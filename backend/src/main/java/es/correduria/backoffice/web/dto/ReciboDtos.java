package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.dominio.Recibo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ReciboDtos {

    private ReciboDtos() {
    }

    public record Resumen(Long id, String numero, Long polizaId, String polizaNumero, Poliza.Ramo ramo,
                          Long clienteId, String clienteNombre, String clienteTelefono, String aseguradoraNombre,
                          BigDecimal importe, LocalDate fechaEmision, LocalDate fechaCobro,
                          Recibo.Estado estado, int intentosReclamacion) {

        public static Resumen de(Recibo r) {
            var p = r.getPoliza();
            var c = p.getCliente();
            return new Resumen(r.getId(), r.getNumero(), p.getId(), p.getNumero(), p.getRamo(),
                    c.getId(), c.getNombre(), c.getTelefono(), p.getAseguradora().getNombre(),
                    r.getImporte(), r.getFechaEmision(), r.getFechaCobro(), r.getEstado(),
                    r.getIntentosReclamacion());
        }
    }

    /** Qué aviso le toca hoy a un recibo devuelto. */
    public enum Accion { AVISO_1, AVISO_2, AVISO_3, ESCALAR }

    /** Recibo devuelto con lo que necesita n8n para enviar el aviso. */
    public record Aviso(Long id, String numero, BigDecimal importe, LocalDate fechaEmision, String polizaNumero,
                        Poliza.Ramo ramo, String aseguradoraNombre, String clienteNombre, String clienteEmail,
                        String clienteTelefono, int intentosReclamacion, LocalDateTime fechaUltimoAviso,
                        Accion accion) {

        public static Aviso de(Recibo r, Accion accion) {
            var p = r.getPoliza();
            var c = p.getCliente();
            return new Aviso(r.getId(), r.getNumero(), r.getImporte(), r.getFechaEmision(), p.getNumero(),
                    p.getRamo(), p.getAseguradora().getNombre(), c.getNombre(), c.getEmail(), c.getTelefono(),
                    r.getIntentosReclamacion(), r.getFechaUltimoAviso(), accion);
        }
    }

    public record Totales(long pendientes, long devueltos, BigDecimal importeDevuelto) {
    }
}
