package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Seguimiento de un PDF de póliza subido desde la app para darlo de alta con la automatización. */
@Entity
@Getter
@Setter
public class AltaPoliza {

    /** PROCESANDO → CREADA (alta automática) | REVISION (a la bandeja) | ERROR */
    public enum Estado { PROCESANDO, CREADA, REVISION, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fichero;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private String polizaNumero;

    /** Motivos de revisión o error, separados por ";" */
    private String motivos;

    private String creadaPor;
    private LocalDateTime creadaEn;
    private LocalDateTime resueltaEn;
}
