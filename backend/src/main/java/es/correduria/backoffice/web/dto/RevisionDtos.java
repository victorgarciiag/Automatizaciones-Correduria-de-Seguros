package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Revision;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class RevisionDtos {

    private RevisionDtos() {
    }

    public record Resumen(Long id, String automatizacion, String automatizacionNombre, String titulo,
                          String motivo, Map<String, Object> datosPropuestos, Map<String, Object> datosFinales,
                          BigDecimal confianza, Revision.Estado estado, String revisor, String comentario,
                          LocalDateTime creadaEn, LocalDateTime resueltaEn, String callbackEstado, boolean demo) {

        public static Resumen de(Revision r, String nombreAutomatizacion) {
            return new Resumen(r.getId(), r.getAutomatizacion(), nombreAutomatizacion, r.getTitulo(),
                    r.getMotivo(), r.getDatosPropuestos(), r.getDatosFinales(), r.getConfianza(),
                    r.getEstado(), r.getRevisor(), r.getComentario(), r.getCreadaEn(), r.getResueltaEn(),
                    r.getCallbackEstado(), r.isDemo());
        }
    }

    /** Lo que envía n8n para pedir una revisión humana. */
    public record Alta(@NotBlank String automatizacion, @NotBlank String titulo, String motivo,
                       @NotEmpty Map<String, Object> datosPropuestos,
                       @DecimalMin("0") @DecimalMax("1") BigDecimal confianza, String callbackUrl) {
    }

    /** Decisión de la persona. Puede corregir los datos antes de aprobar. */
    public record Resolucion(@NotBlank String revisor, String comentario, Map<String, Object> datosFinales) {
    }
}
