package es.correduria.backoffice.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Automatizacion {

    public enum Estado { PLANIFICADA, EN_DESARROLLO, ACTIVA, PAUSADA }

    @Id
    private String codigo;

    private String nombre;
    private String descripcion;
    private String area;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    /** Minutos que tarda una persona en hacer esta tarea a mano (estimación). */
    private int minutosManuales;

    private int orden;
}
