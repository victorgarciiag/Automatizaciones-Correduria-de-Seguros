package es.correduria.backoffice.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.correduria.backoffice.dominio.AltaPoliza;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public final class AltaPolizaDtos {

    private AltaPolizaDtos() {
    }

    public record Resumen(Long id, String fichero, AltaPoliza.Estado estado, String polizaNumero,
                          List<String> motivos, String creadaPor, LocalDateTime creadaEn, LocalDateTime resueltaEn) {

        public static Resumen de(AltaPoliza a) {
            List<String> motivos = a.getMotivos() == null || a.getMotivos().isBlank()
                    ? List.of()
                    : Arrays.stream(a.getMotivos().split(";")).map(String::trim).toList();
            return new Resumen(a.getId(), a.getFichero(), a.getEstado(), a.getPolizaNumero(), motivos,
                    a.getCreadaPor(), a.getCreadaEn(), a.getResueltaEn());
        }
    }

    /** Lo que envía n8n al terminar: CREADA, REVISION o ERROR, con el nº de póliza y los motivos. */
    public record Resultado(@NotNull AltaPoliza.Estado estado, String polizaNumero, List<String> motivos) {
    }

    /**
     * Datos extraídos por la IA del PDF de la póliza (tal cual los devuelve Claude, en snake_case).
     * Las fechas llegan como texto: si vienen mal formadas, es un motivo de revisión y no un error 400.
     */
    public record SolicitudAlta(String aseguradora,
                                @JsonProperty("numero_poliza") String numeroPoliza,
                                String ramo,
                                String producto,
                                Tomador tomador,
                                @JsonProperty("objeto_asegurado") String objetoAsegurado,
                                @JsonProperty("fecha_efecto") String fechaEfecto,
                                @JsonProperty("fecha_vencimiento") String fechaVencimiento,
                                @JsonProperty("prima_anual") BigDecimal primaAnual,
                                @JsonProperty("forma_pago") String formaPago) {
    }

    public record Tomador(String nombre, String nif, String direccion, String telefono, String email) {
    }

    /** CREADA (póliza dada de alta) o REVISION (no se ha guardado nada; ver motivos). */
    public record RespuestaAlta(String resultado, Long polizaId, String polizaNumero, Long clienteId,
                                boolean clienteNuevo, List<String> motivos) {
    }
}
