package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Cliente {

    public enum Tipo { PARTICULAR, EMPRESA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    private String nombre;
    private String nif;
    private String telefono;
    private String email;
    private String direccion;
    private String localidad;
    private LocalDate fechaAlta;
}
