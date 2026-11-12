package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.dominio.Siniestro;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.repositorio.SiniestroRepository;
import es.correduria.backoffice.web.dto.SiniestroDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SiniestroService {

    public static final EnumSet<Siniestro.Estado> ACTIVOS =
            EnumSet.of(Siniestro.Estado.ABIERTO, Siniestro.Estado.DOCUMENTACION_PENDIENTE, Siniestro.Estado.EN_TRAMITE);

    private final SiniestroRepository siniestros;
    private final PolizaRepository polizas;

    @Transactional(readOnly = true)
    public List<SiniestroDtos.Resumen> tablero() {
        return siniestros.tablero(ACTIVOS, LocalDateTime.now().minusDays(30)).stream()
                .map(SiniestroDtos.Resumen::de).toList();
    }

    @Transactional(readOnly = true)
    public SiniestroDtos.Resumen obtener(Long id) {
        return SiniestroDtos.Resumen.de(buscar(id));
    }

    @Transactional
    public SiniestroDtos.Resumen cambiarEstado(Long id, Siniestro.Estado estado) {
        var s = buscar(id);
        s.setEstado(estado);
        if (estado == Siniestro.Estado.EN_TRAMITE || estado == Siniestro.Estado.CERRADO) {
            s.setDocumentosPendientes(null);
        }
        return SiniestroDtos.Resumen.de(s);
    }

    /** Alta desde una automatización. Valida que la póliza exista y esté en vigor en la fecha del siniestro. */
    @Transactional
    public SiniestroDtos.Resumen alta(SiniestroDtos.Alta alta) {
        Poliza poliza = resolverPoliza(alta);
        if (!poliza.estaEnVigor(alta.fechaOcurrencia())) {
            throw new IllegalStateException("La póliza " + poliza.getNumero()
                    + " no estaba en vigor el " + alta.fechaOcurrencia());
        }
        boolean faltanDocs = alta.documentosPendientes() != null && !alta.documentosPendientes().isEmpty();

        var s = new Siniestro();
        s.setNumero("TMP-" + UUID.randomUUID());
        s.setPoliza(poliza);
        s.setTipo(alta.tipo());
        s.setDescripcion(alta.descripcion());
        s.setFechaOcurrencia(alta.fechaOcurrencia());
        s.setFechaApertura(LocalDateTime.now());
        s.setCanal(alta.canal() != null ? alta.canal() : Siniestro.Canal.TELEGRAM);
        s.setOrigen(Siniestro.Origen.AUTOMATIZACION);
        s.setTramitador(alta.tramitador() != null ? alta.tramitador() : tramitadorPorRamo(poliza.getRamo()));
        s.setDocumentosPendientes(faltanDocs ? String.join(";", alta.documentosPendientes()) : null);
        s.setEstado(faltanDocs ? Siniestro.Estado.DOCUMENTACION_PENDIENTE : Siniestro.Estado.ABIERTO);
        s.setImporteEstimado(alta.importeEstimado());
        s = siniestros.save(s);
        s.setNumero(numero(s));
        return SiniestroDtos.Resumen.de(s);
    }

    public static String numero(Siniestro s) {
        return "S-%d-%05d".formatted(s.getFechaApertura().getYear(), s.getId());
    }

    public static String tramitadorPorRamo(Poliza.Ramo ramo) {
        return ramo == Poliza.Ramo.AUTO ? "Manuel Pérez" : "Elena Castro";
    }

    private Poliza resolverPoliza(SiniestroDtos.Alta alta) {
        if (alta.polizaId() != null) {
            return polizas.findById(alta.polizaId())
                    .orElseThrow(() -> new NoSuchElementException("Póliza no encontrada: " + alta.polizaId()));
        }
        if (alta.numeroPoliza() != null) {
            return polizas.findByNumeroIgnoreCase(alta.numeroPoliza())
                    .orElseThrow(() -> new NoSuchElementException("Póliza no encontrada: " + alta.numeroPoliza()));
        }
        throw new IllegalArgumentException("Indica polizaId o numeroPoliza");
    }

    private Siniestro buscar(Long id) {
        return siniestros.findById(id).orElseThrow(() -> new NoSuchElementException("Siniestro no encontrado: " + id));
    }
}
