package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.AltaPoliza;
import es.correduria.backoffice.repositorio.AltaPolizaRepository;
import es.correduria.backoffice.repositorio.EmailEntranteRepository;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.repositorio.RevisionRepository;
import es.correduria.backoffice.web.dto.AltaPolizaDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Automatización 3: la app sube PDFs de pólizas; cada PDF se registra (PROCESANDO) y se envía al webhook de n8n.
 * n8n extrae, valida y da de alta (o manda a revisión) y nos devuelve el resultado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AltaPolizaService {

    private static final int MAX_PDFS = 10;

    private final AltaPolizaRepository altas;
    private final RestClient restClient;
    private final PolizaRepository polizas;
    private final RevisionRepository revisiones;
    private final EmailEntranteRepository emails;

    @Value("${app.n8n.webhook-alta-poliza}")
    private String webhookAltaPoliza;

    public List<AltaPolizaDtos.Resumen> listar() {
        return altas.findTop50ByOrderByCreadaEnDesc().stream().map(AltaPolizaDtos.Resumen::de).toList();
    }

    /** Un registro y una llamada a n8n por cada PDF: así cada póliza se procesa y se sigue por separado. */
    public List<AltaPolizaDtos.Resumen> subir(List<MultipartFile> pdfs, String creadaPor) {
        if (pdfs == null || pdfs.isEmpty() || pdfs.size() > MAX_PDFS) {
            throw new IllegalArgumentException("Sube entre 1 y " + MAX_PDFS + " PDFs");
        }
        for (MultipartFile pdf : pdfs) {
            String nombre = pdf.getOriginalFilename() == null ? "" : pdf.getOriginalFilename().toLowerCase();
            if (!nombre.endsWith(".pdf")) {
                throw new IllegalArgumentException("Solo se admiten PDFs: " + pdf.getOriginalFilename());
            }
        }

        List<AltaPolizaDtos.Resumen> resultado = new ArrayList<>();
        for (MultipartFile pdf : pdfs) {
            var a = new AltaPoliza();
            a.setFichero(pdf.getOriginalFilename());
            a.setEstado(AltaPoliza.Estado.PROCESANDO);
            a.setCreadaPor(creadaPor);
            a.setCreadaEn(LocalDateTime.now());
            a = altas.save(a);
            try {
                enviarAN8n(a, pdf);
            } catch (Exception e) {
                log.warn("No se pudo enviar el alta {} a n8n: {}", a.getId(), e.getMessage());
                a.setEstado(AltaPoliza.Estado.ERROR);
                a.setMotivos("No se pudo contactar con n8n: " + e.getMessage());
                a.setResueltaEn(LocalDateTime.now());
                a = altas.save(a);
            }
            resultado.add(AltaPolizaDtos.Resumen.de(a));
        }
        return resultado;
    }

    /** n8n informa del resultado final. */
    public AltaPolizaDtos.Resumen registrarResultado(Long id, AltaPolizaDtos.Resultado resultado) {
        var a = altas.findById(id).orElseThrow(() -> new NoSuchElementException("Alta no encontrada: " + id));
        if (resultado.estado() == AltaPoliza.Estado.PROCESANDO) {
            throw new IllegalArgumentException("El resultado no puede ser PROCESANDO");
        }
        a.setEstado(resultado.estado());
        a.setPolizaNumero(resultado.polizaNumero());
        a.setMotivos(resultado.motivos() == null ? null : String.join(";", resultado.motivos()));
        a.setResueltaEn(LocalDateTime.now());
        return AltaPolizaDtos.Resumen.de(altas.save(a));
    }

    /** Cuando una persona aprueba (y corrige) un alta que estaba en revisión, el documento pasa a CREADA. */
    public void marcarCreadaTrasRevision(String numeroOriginal, String numeroFinal, String revisor) {
        if (numeroOriginal == null) {
            return;
        }
        for (AltaPoliza a : altas.findByEstadoAndPolizaNumeroIgnoreCase(AltaPoliza.Estado.REVISION, numeroOriginal)) {
            a.setEstado(AltaPoliza.Estado.CREADA);
            a.setPolizaNumero(numeroFinal);
            a.setMotivos("Aprobada tras revisión por " + revisor);
            a.setResueltaEn(LocalDateTime.now());
            altas.save(a);
        }
    }

    /**
     * SOLO PARA PRUEBAS: borra el historial de altas, sus revisiones y las pólizas que dio de alta la automatización,
     * para poder volver a subir los mismos PDFs. No toca la cartera ficticia original.
     */
    @Transactional
    public Map<String, Long> limpiarPruebas() {
        long polizasBorradas = 0;
        for (AltaPoliza a : altas.findByEstado(AltaPoliza.Estado.CREADA)) {
            if (a.getPolizaNumero() != null) {
                var poliza = polizas.findByNumeroIgnoreCase(a.getPolizaNumero());
                if (poliza.isPresent()) {
                    // Si algún email del Buzón apunta a esta póliza, se desvincula (si no, la FK impide borrarla).
                    emails.desvincularPoliza(poliza.get());
                    polizas.delete(poliza.get());
                    polizasBorradas++;
                }
            }
        }
        long revisionesBorradas = revisiones.deleteByAutomatizacion("alta-poliza");
        long documentos = altas.count();
        altas.deleteAll();

        Map<String, Long> resultado = new LinkedHashMap<>();
        resultado.put("documentos", documentos);
        resultado.put("revisiones", revisionesBorradas);
        resultado.put("polizas", polizasBorradas);
        return resultado;
    }

    /** POST multipart al webhook: el PDF en el campo "pdf" y el id del alta en la query. */
    private void enviarAN8n(AltaPoliza a, MultipartFile pdf) throws IOException {
        URI uri = UriComponentsBuilder.fromHttpUrl(webhookAltaPoliza)
                .queryParam("altaId", a.getId())
                .queryParam("creadaPor", a.getCreadaPor() == null ? "" : a.getCreadaPor())
                .build()
                .encode()
                .toUri();

        String nombre = pdf.getOriginalFilename() != null ? pdf.getOriginalFilename() : "poliza.pdf";
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("pdf", new ByteArrayResource(pdf.getBytes()) {
            @Override
            public String getFilename() {
                return nombre;
            }
        });
        restClient.post().uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }
}
