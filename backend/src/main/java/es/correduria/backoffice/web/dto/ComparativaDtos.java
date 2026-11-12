package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Comparativa;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ComparativaDtos {

    private ComparativaDtos() {
    }

    public record Resumen(Long id, String clienteNombre, String clienteEmail, String vehiculo, int numPresupuestos,
                          Comparativa.Estado estado, List<Map<String, Object>> datos, String emailAsunto,
                          String emailHtml, String error, String creadaPor, String revisor,
                          LocalDateTime creadaEn, LocalDateTime borradorEn, LocalDateTime resueltaEn,
                          LocalDateTime enviadaEn) {

        public static Resumen de(Comparativa c) {
            return new Resumen(c.getId(), c.getClienteNombre(), c.getClienteEmail(), c.getVehiculo(),
                    c.getNumPresupuestos(), c.getEstado(), c.getDatos(), c.getEmailAsunto(), c.getEmailHtml(),
                    c.getError(), c.getCreadaPor(), c.getRevisor(), c.getCreadaEn(), c.getBorradorEn(),
                    c.getResueltaEn(), c.getEnviadaEn());
        }
    }

    /** Lo que envía n8n cuando Claude ha terminado el borrador. */
    public record Borrador(List<Map<String, Object>> datos, String emailAsunto, @NotBlank String emailHtml,
                           @NotBlank String callbackUrl) {
    }

    /** La persona aprueba, pudiendo corregir el asunto y el HTML. */
    public record Aprobacion(@NotBlank String revisor, String emailAsunto, @NotBlank String emailHtml) {
    }

    public record Rechazo(@NotBlank String revisor, String motivo) {
    }

    public record ErrorN8n(@NotBlank String mensaje) {
    }
}
