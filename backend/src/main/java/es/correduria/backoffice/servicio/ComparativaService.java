package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Comparativa;
import es.correduria.backoffice.dominio.Comparativa.Estado;
import es.correduria.backoffice.repositorio.ComparativaRepository;
import es.correduria.backoffice.web.dto.ComparativaDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Comparativa de presupuestos (automatización 2, v2).
 *
 * 1. La app crea la comparativa con los PDFs → se guarda en PROCESANDO y se envía al webhook de n8n.
 * 2. n8n (Claude) devuelve el borrador del email + su resumeUrl → PENDIENTE_REVISION.
 * 3. Una persona aprueba (puede corregir el HTML) o rechaza → se avisa a n8n en la resumeUrl.
 * 4. n8n envía el email y confirma → ENVIADA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComparativaService {

    private static final int MAX_PDFS = 6;

    private final ComparativaRepository comparativas;
    private final RestClient restClient;

    @Value("${app.n8n.webhook-comparativa}")
    private String webhookComparativa;

    /** Ver RevisionService: reescribe http://localhost:5678 cuando el backend corre en Docker. */
    @Value("${app.n8n.url-interna:}")
    private String n8nUrlInterna;

    public List<ComparativaDtos.Resumen> listar() {
        return comparativas.findTop50ByOrderByCreadaEnDesc().stream().map(ComparativaDtos.Resumen::de).toList();
    }

    public ComparativaDtos.Resumen obtener(Long id) {
        return ComparativaDtos.Resumen.de(buscar(id));
    }

    // ------------------------------------------------------------------ desde la app

    /** SOLO PARA PRUEBAS: borra todas las comparativas. */
    public long limpiarPruebas() {
        long total = comparativas.count();
        comparativas.deleteAll();
        return total;
    }

    public ComparativaDtos.Resumen crear(String clienteNombre, String clienteEmail, String vehiculo,
                                         String creadaPor, List<MultipartFile> pdfs) {
        validar(clienteNombre, clienteEmail, vehiculo, pdfs);

        var c = new Comparativa();
        c.setClienteNombre(clienteNombre.trim());
        c.setClienteEmail(clienteEmail.trim());
        c.setVehiculo(vehiculo.trim());
        c.setNumPresupuestos(pdfs.size());
        c.setCreadaPor(creadaPor);
        c.setEstado(Estado.PROCESANDO);
        c.setCreadaEn(LocalDateTime.now());
        c = comparativas.save(c);

        // Se guarda ANTES de llamar a n8n: así, si n8n responde muy rápido, la comparativa ya existe.
        try {
            enviarAN8n(c, pdfs);
        } catch (Exception e) {
            log.warn("No se pudo enviar la comparativa {} a n8n: {}", c.getId(), e.getMessage());
            c.setEstado(Estado.ERROR);
            c.setError("No se pudo contactar con n8n: " + e.getMessage());
            c = comparativas.save(c);
        }
        return ComparativaDtos.Resumen.de(c);
    }

    public ComparativaDtos.Resumen aprobar(Long id, ComparativaDtos.Aprobacion aprobacion) {
        var c = buscarEnEstado(id, Estado.PENDIENTE_REVISION);
        c.setEstado(Estado.APROBADA);
        c.setRevisor(aprobacion.revisor());
        if (aprobacion.emailAsunto() != null && !aprobacion.emailAsunto().isBlank()) {
            c.setEmailAsunto(aprobacion.emailAsunto());
        }
        c.setEmailHtml(aprobacion.emailHtml());
        c.setResueltaEn(LocalDateTime.now());
        c.setError(null);
        c = comparativas.save(c);

        try {
            avisarN8n(c, "APROBADA");
        } catch (Exception e) {
            // Si n8n no recibe la aprobación, se vuelve atrás para poder reintentar.
            c.setEstado(Estado.PENDIENTE_REVISION);
            c.setError("No se pudo avisar a n8n para enviar el email: " + e.getMessage());
            comparativas.save(c);
            throw new IllegalStateException(c.getError());
        }
        return ComparativaDtos.Resumen.de(c);
    }

    public ComparativaDtos.Resumen rechazar(Long id, ComparativaDtos.Rechazo rechazo) {
        var c = buscarEnEstado(id, Estado.PENDIENTE_REVISION);
        c.setEstado(Estado.RECHAZADA);
        c.setRevisor(rechazo.revisor());
        c.setError(rechazo.motivo());
        c.setResueltaEn(LocalDateTime.now());
        c = comparativas.save(c);
        try {
            avisarN8n(c, "RECHAZADA"); // para que el flujo no se quede esperando para siempre
        } catch (Exception e) {
            log.warn("No se pudo avisar a n8n del rechazo de la comparativa {}: {}", id, e.getMessage());
        }
        return ComparativaDtos.Resumen.de(c);
    }

    // ------------------------------------------------------------------ desde n8n

    public ComparativaDtos.Resumen guardarBorrador(Long id, ComparativaDtos.Borrador borrador) {
        var c = buscarEnEstado(id, Estado.PROCESANDO);
        c.setDatos(borrador.datos());
        c.setEmailAsunto(borrador.emailAsunto() != null && !borrador.emailAsunto().isBlank()
                ? borrador.emailAsunto()
                : "Comparativa de seguros para tu " + c.getVehiculo());
        c.setEmailHtml(borrador.emailHtml());
        c.setCallbackUrl(borrador.callbackUrl());
        c.setEstado(Estado.PENDIENTE_REVISION);
        c.setBorradorEn(LocalDateTime.now());
        return ComparativaDtos.Resumen.de(comparativas.save(c));
    }

    public ComparativaDtos.Resumen marcarEnviada(Long id) {
        var c = buscarEnEstado(id, Estado.APROBADA);
        c.setEstado(Estado.ENVIADA);
        c.setEnviadaEn(LocalDateTime.now());
        return ComparativaDtos.Resumen.de(comparativas.save(c));
    }

    public ComparativaDtos.Resumen marcarError(Long id, String mensaje) {
        var c = buscar(id);
        if (c.getEstado() == Estado.ENVIADA || c.getEstado() == Estado.RECHAZADA) {
            throw new IllegalStateException("La comparativa " + id + " ya está " + c.getEstado());
        }
        c.setEstado(Estado.ERROR);
        c.setError(mensaje);
        return ComparativaDtos.Resumen.de(comparativas.save(c));
    }

    // ------------------------------------------------------------------ llamadas a n8n

    /**
     * POST multipart al webhook: un campo "pdfs" por cada fichero.
     * Los datos de la comparativa van en la query (?comparativaId=...): si se mandan como partes de texto
     * del multipart, n8n los interpreta como ficheros .txt.
     */
    private void enviarAN8n(Comparativa c, List<MultipartFile> pdfs) throws IOException {
        URI uri = UriComponentsBuilder.fromHttpUrl(webhookComparativa)
                .queryParam("comparativaId", c.getId())
                .queryParam("clienteNombre", c.getClienteNombre())
                .queryParam("clienteEmail", c.getClienteEmail())
                .queryParam("vehiculo", c.getVehiculo())
                .build()
                .encode()
                .toUri();

        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        for (MultipartFile pdf : pdfs) {
            String nombre = pdf.getOriginalFilename() != null ? pdf.getOriginalFilename() : "presupuesto.pdf";
            cuerpo.add("pdfs", new ByteArrayResource(pdf.getBytes()) {
                @Override
                public String getFilename() {
                    return nombre;
                }
            });
        }
        restClient.post().uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    /** Despierta el nodo Wait de n8n con la decisión y el email final. */
    private void avisarN8n(Comparativa c, String decision) {
        if (c.getCallbackUrl() == null || c.getCallbackUrl().isBlank()) {
            throw new IllegalStateException("La comparativa no tiene callbackUrl de n8n");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("comparativaId", c.getId());
        cuerpo.put("decision", decision);
        cuerpo.put("clienteNombre", c.getClienteNombre());
        cuerpo.put("clienteEmail", c.getClienteEmail());
        cuerpo.put("emailAsunto", c.getEmailAsunto());
        cuerpo.put("emailHtml", c.getEmailHtml());
        cuerpo.put("revisor", c.getRevisor());
        // URI.create: la URL de n8n ya viene codificada (lleva una firma); no hay que volver a codificarla
        restClient.post().uri(URI.create(urlInterna(c.getCallbackUrl())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    private String urlInterna(String url) {
        if (n8nUrlInterna == null || n8nUrlInterna.isBlank()) {
            return url;
        }
        return url.replaceFirst("^https?://(localhost|127\\.0\\.0\\.1):5678", n8nUrlInterna);
    }

    // ------------------------------------------------------------------ utilidades

    private void validar(String clienteNombre, String clienteEmail, String vehiculo, List<MultipartFile> pdfs) {
        if (clienteNombre == null || clienteNombre.isBlank() || vehiculo == null || vehiculo.isBlank()) {
            throw new IllegalArgumentException("Cliente y vehículo son obligatorios");
        }
        if (clienteEmail == null || !clienteEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email del cliente no válido");
        }
        if (pdfs == null || pdfs.isEmpty() || pdfs.size() > MAX_PDFS) {
            throw new IllegalArgumentException("Sube entre 1 y " + MAX_PDFS + " PDFs");
        }
        for (MultipartFile pdf : pdfs) {
            String nombre = pdf.getOriginalFilename() == null ? "" : pdf.getOriginalFilename().toLowerCase();
            if (!nombre.endsWith(".pdf")) {
                throw new IllegalArgumentException("Solo se admiten PDFs: " + pdf.getOriginalFilename());
            }
        }
    }

    private Comparativa buscarEnEstado(Long id, Estado esperado) {
        var c = buscar(id);
        if (c.getEstado() != esperado) {
            throw new IllegalStateException("La comparativa " + id + " está " + c.getEstado()
                    + " (se esperaba " + esperado + ")");
        }
        return c;
    }

    private Comparativa buscar(Long id) {
        return comparativas.findById(id).orElseThrow(() -> new NoSuchElementException("Comparativa no encontrada: " + id));
    }
}
