package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Poliza;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PolizaDtos {

    private PolizaDtos() {
    }

    public record Resumen(Long id, String numero, Long clienteId, String clienteNombre, String clienteNif,
                          Long aseguradoraId, String aseguradoraNombre, String aseguradoraColor,
                          Poliza.Ramo ramo, String producto, String objetoAsegurado, String matricula,
                          LocalDate fechaEfecto, LocalDate fechaVencimiento, BigDecimal primaAnual,
                          Poliza.Periodicidad periodicidad, BigDecimal comisionPct, Poliza.Estado estado,
                          boolean enVigor) {

        public static Resumen de(Poliza p) {
            var c = p.getCliente();
            var a = p.getAseguradora();
            return new Resumen(p.getId(), p.getNumero(), c.getId(), c.getNombre(), c.getNif(),
                    a.getId(), a.getNombre(), a.getColor(), p.getRamo(), p.getProducto(),
                    p.getObjetoAsegurado(), p.getMatricula(), p.getFechaEfecto(), p.getFechaVencimiento(),
                    p.getPrimaAnual(), p.getPeriodicidad(), p.getComisionPct(), p.getEstado(),
                    p.estaEnVigor(LocalDate.now()));
        }
    }

    public record Detalle(Resumen poliza, ClienteDtos.Resumen cliente, List<ReciboDtos.Resumen> recibos,
                          List<SiniestroDtos.Resumen> siniestros) {
    }
}
