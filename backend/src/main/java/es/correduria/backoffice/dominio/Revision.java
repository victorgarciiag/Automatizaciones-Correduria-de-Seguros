package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
public class Revision {

    public enum Estado { PENDIENTE, APROBADA, RECHAZADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String automatizacion;
    private String titulo;
    private String motivo;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> datosPropuestos;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> datosFinales;

    /** Confianza de la IA entre 0 y 1. */
    private BigDecimal confianza;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    /** URL a la que avisar al resolver (p. ej. el resumeUrl de un nodo Wait de n8n). */
    private String callbackUrl;
    private String callbackEstado;

    private String revisor;
    private String comentario;
    private LocalDateTime creadaEn;
    private LocalDateTime resueltaEn;
    private boolean demo;
}
