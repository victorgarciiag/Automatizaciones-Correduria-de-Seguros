package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Comparativa de presupuestos: la crea un comercial desde la app y la procesa n8n + Claude. */
@Entity
@Getter
@Setter
public class Comparativa {

    /**
     * PROCESANDO → (n8n envía el borrador) → PENDIENTE_REVISION → (persona) → APROBADA → (n8n envía el email) → ENVIADA
     *                                                              └→ RECHAZADA
     * ERROR si n8n no responde o falla.
     */
    public enum Estado { PROCESANDO, PENDIENTE_REVISION, APROBADA, ENVIADA, RECHAZADA, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clienteNombre;
    private String clienteEmail;
    private String vehiculo;
    private int numPresupuestos;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    /** Datos extraídos por la IA de cada presupuesto. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> datos;

    private String emailAsunto;
    private String emailHtml;

    /** resumeUrl del nodo Wait de n8n: se llama al aprobar o rechazar. */
    private String callbackUrl;

    private String error;
    private String creadaPor;
    private String revisor;
    private LocalDateTime creadaEn;
    private LocalDateTime borradorEn;
    private LocalDateTime resueltaEn;
    private LocalDateTime enviadaEn;
}
