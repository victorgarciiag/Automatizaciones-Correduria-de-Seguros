package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Cliente;
import es.correduria.backoffice.dominio.EmailEntrante;
import es.correduria.backoffice.dominio.Poliza;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class EmailDtos {

    private EmailDtos() {
    }

    // ------------------------------------------------------------------ desde n8n

    /** Lo que manda n8n tras el primer LLM: el email y su clasificación (mismo formato que el Edit Fields). */
    public record Entrada(@NotBlank String remitente, String asunto, String texto, String fecha,
                          @NotNull @Valid Clasificacion clasificacion) {
    }

    public record Clasificacion(@NotBlank String categoria, String urgencia, BigDecimal confianza, String resumen,
                                Map<String, Object> datos) {
    }

    public record Borrador(String asunto, @NotBlank String respuesta, @NotBlank String callbackUrl) {
    }

    public record ErrorN8n(@NotBlank String mensaje) {
    }

    /** Respuesta para n8n: lo que el backend sabe del email. Es el contexto del segundo LLM. */
    public record Contexto(Long emailId, String categoria, String urgencia, String asignadoA,
                           boolean clienteIdentificado, String identificadoPor, ClienteInfo cliente,
                           PolizaInfo poliza, List<PolizaInfo> otrasPolizas, List<String> avisos) {
    }

    public record ClienteInfo(Long id, String nombre, String nif, String telefono, String email) {
        public static ClienteInfo de(Cliente c) {
            return c == null ? null : new ClienteInfo(c.getId(), c.getNombre(), c.getNif(), c.getTelefono(), c.getEmail());
        }
    }

    public record PolizaInfo(Long id, String numero, Poliza.Ramo ramo, String producto, String aseguradora,
                             String objetoAsegurado, Poliza.Estado estado, boolean enVigor, LocalDate fechaVencimiento) {
        public static PolizaInfo de(Poliza p) {
            return p == null ? null : new PolizaInfo(p.getId(), p.getNumero(), p.getRamo(), p.getProducto(),
                    p.getAseguradora().getNombre(), p.getObjetoAsegurado(), p.getEstado(),
                    p.estaEnVigor(LocalDate.now()), p.getFechaVencimiento());
        }
    }

    // ------------------------------------------------------------------ para la app

    public record Resumen(Long id, String remitente, String remitenteEmail, String asunto, String texto,
                          LocalDateTime recibidoEn, EmailEntrante.Categoria categoria, EmailEntrante.Urgencia urgencia,
                          BigDecimal confianza, String resumen, Map<String, Object> datos, ClienteInfo cliente,
                          PolizaInfo poliza, String identificadoPor, List<String> avisos, String asignadoA,
                          EmailEntrante.Estado estado, String respuestaAsunto, String respuesta, String revisor,
                          String error, LocalDateTime creadoEn, LocalDateTime borradorEn, LocalDateTime resueltoEn,
                          LocalDateTime enviadoEn) {

        public static Resumen de(EmailEntrante e) {
            return new Resumen(e.getId(), e.getRemitente(), e.getRemitenteEmail(), e.getAsunto(), e.getTexto(),
                    e.getRecibidoEn(), e.getCategoria(), e.getUrgencia(), e.getConfianza(), e.getResumen(), e.getDatos(),
                    ClienteInfo.de(e.getCliente()), PolizaInfo.de(e.getPoliza()), e.getIdentificadoPor(),
                    separarAvisos(e.getAvisos()), e.getAsignadoA(), e.getEstado(), e.getRespuestaAsunto(), e.getRespuesta(),
                    e.getRevisor(), e.getError(), e.getCreadoEn(), e.getBorradorEn(), e.getResueltoEn(), e.getEnviadoEn());
        }

        private static List<String> separarAvisos(String texto) {
            return texto == null || texto.isBlank() ? List.of() : Arrays.asList(texto.split("\n"));
        }
    }

    /** La persona aprueba, pudiendo corregir el asunto y el texto de la respuesta. */
    public record Aprobacion(@NotBlank String revisor, String asunto, @NotBlank String respuesta) {
    }

    public record Descarte(@NotBlank String revisor, String motivo) {
    }
}
