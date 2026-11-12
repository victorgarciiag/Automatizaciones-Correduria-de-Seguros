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
public class Recibo {

    public enum Estado { PENDIENTE, COBRADO, DEVUELTO, ANULADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Poliza poliza;

    private BigDecimal importe;
    private LocalDate fechaEmision;
    private LocalDate fechaCobro;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private int intentosReclamacion;
    
    private LocalDateTime fechaUltimoAviso;
}
