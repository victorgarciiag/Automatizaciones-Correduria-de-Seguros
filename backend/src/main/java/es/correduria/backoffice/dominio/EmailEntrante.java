package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** Email del buzón general, clasificado por la IA y asignado a una persona (automatización 5). */
@Entity
@Getter
@Setter
public class EmailEntrante {

    public enum Categoria { SINIESTRO, PRESUPUESTO, RECIBOS, MODIFICACION, BAJA, DOCUMENTACION, OTRO }

    public enum Urgencia { ALTA, MEDIA, BAJA }

    /**
     * CLASIFICADO (n8n está redactando la respuesta) → PENDIENTE_REVISION → (persona) → APROBADO → (n8n envía) → RESPONDIDO
     *                                                                       └→ DESCARTADO
     * ERROR si algo falla en el flujo.
     */
    public enum Estado { CLASIFICADO, PENDIENTE_REVISION, APROBADO, RESPONDIDO, DESCARTADO, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String remitente;
    private String remitenteEmail;
    private String asunto;
    private String texto;
    private LocalDateTime recibidoEn;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    private Urgencia urgencia;

    private BigDecimal confianza;
    private String resumen;

    /** Datos que extrajo la IA (nif, matrícula, póliza...), tal cual. */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> datos;

    /** Lo que ha encontrado el backend al cruzar esos datos con la cartera. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    private Poliza poliza;

    private String identificadoPor;
    private String avisos;
    /** Con nombre explícito: Spring no separa con "_" una mayúscula final (asignadoA → asignadoa). */
    @Column(name = "asignado_a")
    private String asignadoA;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private String respuestaAsunto;
    private String respuesta;

    /** resumeUrl del nodo Wait de n8n. */
    private String callbackUrl;

    private String revisor;
    private String error;
    private LocalDateTime creadoEn;
    private LocalDateTime borradorEn;
    private LocalDateTime resueltoEn;
    private LocalDateTime enviadoEn;
}
