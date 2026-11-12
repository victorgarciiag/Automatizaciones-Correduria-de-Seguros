package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Siniestro {

    public enum Estado { ABIERTO, DOCUMENTACION_PENDIENTE, EN_TRAMITE, CERRADO, RECHAZADO }

    public enum Canal { TELEFONO, WHATSAPP, TELEGRAM, EMAIL, PRESENCIAL }

    public enum Origen { MANUAL, AUTOMATIZACION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Poliza poliza;

    private String tipo;
    private String descripcion;
    private LocalDate fechaOcurrencia;
    private LocalDateTime fechaApertura;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    @Enumerated(EnumType.STRING)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    private Origen origen;

    private String tramitador;

    /** Documentos que faltan, separados por ";" */
    private String documentosPendientes;

    private BigDecimal importeEstimado;
}
