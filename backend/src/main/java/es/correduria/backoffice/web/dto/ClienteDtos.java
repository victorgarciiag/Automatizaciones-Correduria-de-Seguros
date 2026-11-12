package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record Resumen(Long id, Cliente.Tipo tipo, String nombre, String nif, String telefono,
                          String email, String direccion, String localidad, LocalDate fechaAlta) {

        public static Resumen de(Cliente c) {
            return new Resumen(c.getId(), c.getTipo(), c.getNombre(), c.getNif(), c.getTelefono(),
                    c.getEmail(), c.getDireccion(), c.getLocalidad(), c.getFechaAlta());
        }
    }

    public record Detalle(Resumen cliente, List<PolizaDtos.Resumen> polizas, BigDecimal primaTotal,
                          long polizasVigentes, List<SiniestroDtos.Resumen> siniestros) {
    }
}
