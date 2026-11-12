package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Automatizacion;
import es.correduria.backoffice.dominio.Revision;
import es.correduria.backoffice.repositorio.AutomatizacionRepository;
import es.correduria.backoffice.repositorio.RevisionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.correduria.backoffice.web.dto.AltaPolizaDtos;
import es.correduria.backoffice.web.dto.RevisionDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Bandeja de revisión humana: la IA propone, una persona decide.
 * Al resolver, si la revisión trae callbackUrl (p. ej. el resumeUrl de un nodo Wait de n8n),
 * se avisa para que el flujo continúe con la decisión.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevisionService {

    private final RevisionRepository revisiones;
    private final AutomatizacionRepository automatizaciones;
    private final RestClient restClient;
    private final AltaPolizaValidacionService validacionAltas;
    private final AltaPolizaService altasPoliza;
    private final ObjectMapper objectMapper;

    /**
     * n8n genera el resumeUrl con "http://localhost:5678". Dentro de Docker el backend no llega a n8n por
     * localhost, así que se sustituye por la URL interna (p. ej. http://n8n:5678). Vacío = no se toca.
     */
    @Value("${app.n8n.url-interna:}")
    private String n8nUrlInterna;

    public List<RevisionDtos.Resumen> pendientes() {
        return mapear(revisiones.findByEstadoOrderByCreadaEnDesc(Revision.Estado.PENDIENTE));
    }

    public List<RevisionDtos.Resumen> resueltas() {
        return mapear(revisiones.findTop50ByEstadoNotOrderByResueltaEnDesc(Revision.Estado.PENDIENTE));
    }

    public RevisionDtos.Resumen crear(RevisionDtos.Alta alta) {
        if (!automatizaciones.existsById(alta.automatizacion())) {
            throw new IllegalArgumentException("Automatización desconocida: " + alta.automatizacion());
        }
        var r = new Revision();
        r.setAutomatizacion(alta.automatizacion());
        r.setTitulo(alta.titulo());
        r.setMotivo(alta.motivo());
        r.setDatosPropuestos(alta.datosPropuestos());
        r.setConfianza(alta.confianza());
        r.setCallbackUrl(alta.callbackUrl());
        r.setEstado(Revision.Estado.PENDIENTE);
        r.setCreadaEn(LocalDateTime.now());
        return mapear(revisiones.save(r));
    }

    public RevisionDtos.Resumen obtener(Long id) {
        return mapear(buscar(id));
    }

    public RevisionDtos.Resumen aprobar(Long id, RevisionDtos.Resolucion resolucion) {
        return resolver(id, Revision.Estado.APROBADA, resolucion);
    }

    public RevisionDtos.Resumen rechazar(Long id, RevisionDtos.Resolucion resolucion) {
        return resolver(id, Revision.Estado.RECHAZADA, resolucion);
    }

    private RevisionDtos.Resumen resolver(Long id, Revision.Estado decision, RevisionDtos.Resolucion resolucion) {
        var r = buscar(id);
        if (r.getEstado() != Revision.Estado.PENDIENTE) {
            throw new IllegalStateException("La revisión " + id + " ya está " + r.getEstado());
        }
        Map<String, Object> datosFinales = decision == Revision.Estado.APROBADA
                ? (resolucion.datosFinales() != null ? resolucion.datosFinales() : r.getDatosPropuestos())
                : null;

        // Alta de póliza: aprobar = dar de alta con los datos (corregidos) tras volver a validarlos.
        if (decision == Revision.Estado.APROBADA && "alta-poliza".equals(r.getAutomatizacion())) {
            datosFinales = darDeAltaPoliza(r, datosFinales, resolucion.revisor());
        }

        r.setEstado(decision);
        r.setRevisor(resolucion.revisor());
        r.setComentario(resolucion.comentario());
        r.setDatosFinales(datosFinales);
        r.setResueltaEn(LocalDateTime.now());
        r = revisiones.save(r);

        // Se avisa DESPUÉS de guardar: si n8n consulta la revisión al despertar, ya ve la decisión.
        if (r.getCallbackUrl() != null && !r.getCallbackUrl().isBlank()) {
            r.setCallbackEstado(avisar(r));
            r = revisiones.save(r);
        }
        return mapear(r);
    }

    /**
     * Vuelve a pasar las reglas del backend con lo que ha corregido la persona.
     * Si sigue sin cuadrar, la aprobación falla (la revisión queda pendiente) y se muestra el motivo.
     */
    private Map<String, Object> darDeAltaPoliza(Revision r, Map<String, Object> datos, String revisor) {
        AltaPolizaDtos.SolicitudAlta solicitud;
        try {
            solicitud = objectMapper.convertValue(datos, AltaPolizaDtos.SolicitudAlta.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Los datos corregidos no tienen el formato esperado: " + e.getMessage());
        }
        var respuesta = validacionAltas.darDeAlta(solicitud);
        if (!"CREADA".equals(respuesta.resultado())) {
            throw new IllegalStateException("No se puede aprobar, los datos siguen sin cuadrar: "
                    + String.join("; ", respuesta.motivos()));
        }
        Object numeroOriginal = r.getDatosPropuestos() == null ? null : r.getDatosPropuestos().get("numero_poliza");
        altasPoliza.marcarCreadaTrasRevision(numeroOriginal == null ? null : numeroOriginal.toString(),
                respuesta.polizaNumero(), revisor);

        Map<String, Object> resultado = new LinkedHashMap<>(datos);
        resultado.put("polizaIdCreada", respuesta.polizaId());
        resultado.put("clienteNuevo", respuesta.clienteNuevo());
        return resultado;
    }

    private String avisar(Revision r) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("revisionId", r.getId());
        cuerpo.put("decision", r.getEstado().name());
        cuerpo.put("datos", r.getDatosFinales());
        cuerpo.put("revisor", r.getRevisor());
        cuerpo.put("comentario", r.getComentario());
        try {
            var respuesta = restClient.post().uri(URI.create(urlCallback(r.getCallbackUrl())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            return "OK " + respuesta.getStatusCode().value();
        } catch (Exception e) {
            log.warn("No se pudo avisar a {}: {}", r.getCallbackUrl(), e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    private String urlCallback(String url) {
        if (n8nUrlInterna == null || n8nUrlInterna.isBlank()) {
            return url;
        }
        return url.replaceFirst("^https?://(localhost|127\\.0\\.0\\.1):5678", n8nUrlInterna);
    }

    private Revision buscar(Long id) {
        return revisiones.findById(id).orElseThrow(() -> new NoSuchElementException("Revisión no encontrada: " + id));
    }

    private Map<String, String> nombres() {
        return automatizaciones.findAll().stream()
                .collect(Collectors.toMap(Automatizacion::getCodigo, Automatizacion::getNombre));
    }

    private RevisionDtos.Resumen mapear(Revision r) {
        return RevisionDtos.Resumen.de(r, nombres().getOrDefault(r.getAutomatizacion(), r.getAutomatizacion()));
    }

    private List<RevisionDtos.Resumen> mapear(List<Revision> lista) {
        var nombres = nombres();
        return lista.stream()
                .map(r -> RevisionDtos.Resumen.de(r, nombres.getOrDefault(r.getAutomatizacion(), r.getAutomatizacion())))
                .toList();
    }
}
