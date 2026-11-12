package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.dominio.Siniestro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public final class SiniestroDtos {

    private SiniestroDtos() {
    }

    public record Resumen(Long id, String numero, Long polizaId, String polizaNumero, Poliza.Ramo ramo,
                          String objetoAsegurado, Long clienteId, String clienteNombre, String clienteTelefono,
                          String aseguradoraNombre, String tipo, String descripcion, LocalDate fechaOcurrencia,
                          LocalDateTime fechaApertura, Siniestro.Estado estado, Siniestro.Canal canal,
                          Siniestro.Origen origen, String tramitador, List<String> documentosPendientes,
                          BigDecimal importeEstimado) {

        public static Resumen de(Siniestro s) {
            var p = s.getPoliza();
            var c = p.getCliente();
            List<String> docs = s.getDocumentosPendientes() == null || s.getDocumentosPendientes().isBlank()
                    ? List.of()
                    : Arrays.stream(s.getDocumentosPendientes().split(";")).map(String::trim).toList();
            return new Resumen(s.getId(), s.getNumero(), p.getId(), p.getNumero(), p.getRamo(),
                    p.getObjetoAsegurado(), c.getId(), c.getNombre(), c.getTelefono(),
                    p.getAseguradora().getNombre(), s.getTipo(), s.getDescripcion(), s.getFechaOcurrencia(),
                    s.getFechaApertura(), s.getEstado(), s.getCanal(), s.getOrigen(), s.getTramitador(),
                    docs, s.getImporteEstimado());
        }
    }

    public record CambioEstado(@NotNull Siniestro.Estado estado) {
    }

    /** Alta de siniestro desde una automatización (n8n). Se identifica la póliza por id o por número. */
    public record Alta(Long polizaId, String numeroPoliza, @NotBlank String tipo, @NotBlank String descripcion,
                       @NotNull LocalDate fechaOcurrencia, Siniestro.Canal canal, String tramitador,
                       List<String> documentosPendientes, BigDecimal importeEstimado) {
    }
}
