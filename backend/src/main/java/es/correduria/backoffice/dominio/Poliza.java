package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Poliza {

    public enum Ramo { AUTO, HOGAR, COMERCIO, VIDA, DECESOS, SALUD }

    public enum Estado { VIGENTE, ANULADA, VENCIDA }

    public enum Periodicidad { ANUAL, SEMESTRAL, TRIMESTRAL, MENSUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Aseguradora aseguradora;

    @Enumerated(EnumType.STRING)
    private Ramo ramo;

    private String producto;
    private String objetoAsegurado;
    private String matricula;
    private LocalDate fechaEfecto;
    private LocalDate fechaVencimiento;
    private BigDecimal primaAnual;

    @Enumerated(EnumType.STRING)
    private Periodicidad periodicidad;

    private BigDecimal comisionPct;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    /** Una póliza está en vigor si está VIGENTE y la fecha indicada cae dentro de su periodo. */
    public boolean estaEnVigor(LocalDate fecha) {
        return estado == Estado.VIGENTE
                && !fecha.isBefore(fechaEfecto)
                && !fecha.isAfter(fechaVencimiento);
    }
}
