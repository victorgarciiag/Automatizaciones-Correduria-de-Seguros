package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Recibo;
import es.correduria.backoffice.repositorio.ReciboRepository;
import es.correduria.backoffice.web.dto.ReciboDtos;
import es.correduria.backoffice.web.dto.ReciboDtos.Accion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Reglas de la automatización "Recibos devueltos" (sin IA).
 *
 * Calendario de avisos, contando desde el aviso anterior:
 *   AVISO_1  amable                → en cuanto se detecta la devolución
 *   AVISO_2  recordatorio          → 6 días después del aviso 1   (≈ día 7)
 *   AVISO_3  posible anulación     → 8 días después del aviso 2   (≈ día 15)
 *   ESCALAR  pasa a administración → 5 días después del aviso 3   (≈ día 20)
 *
 * El backend decide; n8n solo pregunta qué toca, lo envía y confirma.
 */
@Service
@RequiredArgsConstructor
public class AvisoRecibosService {

    private static final int DIAS_HASTA_AVISO_2 = 6;
    private static final int DIAS_HASTA_AVISO_3 = 8;
    private static final int DIAS_HASTA_ESCALAR = 5;
    /** Tras escalar (4 intentos) ya no hay más avisos automáticos: lo lleva una persona. */
    private static final int INTENTOS_MAXIMOS = 4;

    private final ReciboRepository recibos;

    /** Recibos devueltos a los que hoy les toca algún aviso. */
    @Transactional(readOnly = true)
    public List<ReciboDtos.Aviso> pendientes(int limite) {
        LocalDateTime ahora = LocalDateTime.now();
        return recibos.findByEstadoOrderByFechaEmisionAsc(Recibo.Estado.DEVUELTO).stream()
                .map(r -> {
                    Accion accion = accionQueToca(r, ahora);
                    return accion == null ? null : ReciboDtos.Aviso.de(r, accion);
                })
                .filter(Objects::nonNull)
                .limit(limite)
                .toList();
    }

    /** n8n lo llama cuando ya ha enviado el aviso: suma un intento y guarda la fecha. */
    @Transactional
    public ReciboDtos.Aviso registrarAviso(Long id) {
        var r = recibos.findById(id).orElseThrow(() -> new NoSuchElementException("Recibo no encontrado: " + id));
        if (r.getEstado() != Recibo.Estado.DEVUELTO) {
            throw new IllegalStateException("El recibo " + r.getNumero() + " ya no está devuelto (" + r.getEstado() + ")");
        }
        if (r.getIntentosReclamacion() >= INTENTOS_MAXIMOS) {
            throw new IllegalStateException("El recibo " + r.getNumero() + " ya se pasó a administración");
        }
        LocalDateTime ahora = LocalDateTime.now();
        r.setIntentosReclamacion(r.getIntentosReclamacion() + 1);
        r.setFechaUltimoAviso(ahora);
        return ReciboDtos.Aviso.de(r, accionQueToca(r, ahora));
    }

    /** La regla: según los intentos y los días desde el último aviso. null = hoy no toca nada. */
    public Accion accionQueToca(Recibo recibo, LocalDateTime ahora) {
        long dias = recibo.getFechaUltimoAviso() == null
                ? Long.MAX_VALUE
                : ChronoUnit.DAYS.between(recibo.getFechaUltimoAviso(), ahora);

        return switch (recibo.getIntentosReclamacion()) {
            case 0 -> Accion.AVISO_1;
            case 1 -> dias >= DIAS_HASTA_AVISO_2 ? Accion.AVISO_2 : null;
            case 2 -> dias >= DIAS_HASTA_AVISO_3 ? Accion.AVISO_3 : null;
            case 3 -> dias >= DIAS_HASTA_ESCALAR ? Accion.ESCALAR : null;
            default -> null;
        };
    }
}
