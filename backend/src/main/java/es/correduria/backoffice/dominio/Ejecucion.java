package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Ejecucion {

    public enum Resultado { OK, REVISION, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String automatizacion;
    private LocalDateTime inicio;
    private int duracionMs;

    @Enumerated(EnumType.STRING)
    private Resultado resultado;

    private int tokensEntrada;
    private int tokensSalida;
    private BigDecimal costeEur;
    private BigDecimal minutosAhorrados;

    /** Referencia de negocio (nº de siniestro, de póliza...). */
    private String referencia;
    private String detalle;

    @Column(name = "n8n_execution_id")
    private String n8nExecutionId;

    private boolean demo;
}
