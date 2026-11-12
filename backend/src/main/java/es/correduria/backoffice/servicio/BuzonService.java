package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Cliente;
import es.correduria.backoffice.dominio.EmailEntrante;
import es.correduria.backoffice.dominio.EmailEntrante.Categoria;
import es.correduria.backoffice.dominio.EmailEntrante.Estado;
import es.correduria.backoffice.dominio.EmailEntrante.Urgencia;
import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.repositorio.ClienteRepository;
import es.correduria.backoffice.repositorio.EmailEntranteRepository;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.web.dto.EmailDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clasificador de emails (automatización 5).
 *
 * 1. n8n recibe el email y el primer LLM lo clasifica y extrae datos.
 * 2. Aquí se cruzan esos datos con la cartera (cliente y póliza) y se asigna la persona por reglas.
 *    Se devuelve ese contexto para que el segundo LLM redacte la respuesta con datos reales.
 * 3. n8n guarda el borrador → PENDIENTE_REVISION → una persona aprueba o descarta en el Buzón.
 * 4. n8n envía la respuesta y confirma → RESPONDIDO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuzonService {

    private static final Pattern EMAIL_ENTRE_ANGULOS = Pattern.compile("<([^>]+)>");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private final EmailEntranteRepository emails;
    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final RestClient restClient;

    @Value("${app.n8n.url-interna:}")
    private String n8nUrlInterna;

    // ------------------------------------------------------------------ lectura (app)

    @Transactional(readOnly = true)
    public List<EmailDtos.Resumen> listar() {
        return emails.findTop100ByOrderByCreadoEnDesc().stream().map(EmailDtos.Resumen::de).toList();
    }

    @Transactional(readOnly = true)
    public EmailDtos.Resumen obtener(Long id) {
        return EmailDtos.Resumen.de(buscar(id));
    }

    /** SOLO PARA PRUEBAS: borra todos los emails del buzón. */
    @Transactional
    public long limpiarPruebas() {
        long total = emails.count();
        emails.deleteAll();
        return total;
    }

    // ------------------------------------------------------------------ desde n8n

    /** Registra el email clasificado, identifica al cliente, asigna la persona y devuelve el contexto. */
    @Transactional
    public EmailDtos.Contexto registrar(EmailDtos.Entrada entrada) {
        var c = entrada.clasificacion();
        Map<String, Object> datos = c.datos() != null ? c.datos() : Map.of();

        var e = new EmailEntrante();
        e.setRemitente(recortar(entrada.remitente().trim(), 200));
        e.setRemitenteEmail(extraerEmail(entrada.remitente()));
        e.setAsunto(recortar(entrada.asunto(), 300));
        e.setTexto(recortar(entrada.texto(), 10000));
        e.setRecibidoEn(parsearFecha(entrada.fecha()));
        e.setCategoria(enumOr(Categoria.class, c.categoria(), Categoria.OTRO));
        e.setUrgencia(enumOr(Urgencia.class, c.urgencia(), Urgencia.MEDIA));
        e.setConfianza(c.confianza() == null ? null : c.confianza().max(BigDecimal.ZERO).min(BigDecimal.ONE));
        e.setResumen(recortar(c.resumen(), 500));
        e.setDatos(new LinkedHashMap<>(datos));

        List<String> avisos = new ArrayList<>();
        identificar(e, datos, avisos);
        e.setAsignadoA(asignar(e.getCategoria(), ramo(e, datos)));
        e.setAvisos(avisos.isEmpty() ? null : recortar(String.join("\n", avisos), 1000));
        e.setEstado(Estado.CLASIFICADO);
        e.setCreadoEn(LocalDateTime.now());
        emails.save(e); // entidad nueva: save() hace persist sobre el mismo objeto

        List<EmailDtos.PolizaInfo> otras = e.getCliente() == null ? List.of()
                : polizas.findByClienteIdOrderByFechaVencimientoDesc(e.getCliente().getId()).stream()
                  .filter(p -> e.getPoliza() == null || !p.getId().equals(e.getPoliza().getId()))
                  .map(EmailDtos.PolizaInfo::de)
                  .toList();

        return new EmailDtos.Contexto(e.getId(), e.getCategoria().name(), e.getUrgencia().name(), e.getAsignadoA(),
                e.getCliente() != null, e.getIdentificadoPor(), EmailDtos.ClienteInfo.de(e.getCliente()),
                EmailDtos.PolizaInfo.de(e.getPoliza()), otras, avisos);
    }

    @Transactional
    public EmailDtos.Resumen guardarBorrador(Long id, EmailDtos.Borrador borrador) {
        var e = buscarEnEstado(id, Estado.CLASIFICADO);
        e.setRespuestaAsunto(borrador.asunto() != null && !borrador.asunto().isBlank()
                ? recortar(borrador.asunto(), 300)
                : "Re: " + Objects.requireNonNullElse(e.getAsunto(), "tu consulta"));
        e.setRespuesta(recortar(borrador.respuesta(), 5000));
        e.setCallbackUrl(borrador.callbackUrl());
        e.setEstado(Estado.PENDIENTE_REVISION);
        e.setBorradorEn(LocalDateTime.now());
        return EmailDtos.Resumen.de(e);
    }

    @Transactional
    public EmailDtos.Resumen marcarEnviado(Long id) {
        var e = buscarEnEstado(id, Estado.APROBADO);
        e.setEstado(Estado.RESPONDIDO);
        e.setEnviadoEn(LocalDateTime.now());
        return EmailDtos.Resumen.de(e);
    }

    @Transactional
    public EmailDtos.Resumen marcarError(Long id, String mensaje) {
        var e = buscar(id);
        if (e.getEstado() == Estado.RESPONDIDO || e.getEstado() == Estado.DESCARTADO) {
            throw new IllegalStateException("El email " + id + " ya está " + e.getEstado());
        }
        e.setEstado(Estado.ERROR);
        e.setError(recortar(mensaje, 500));
        return EmailDtos.Resumen.de(e);
    }

    // ------------------------------------------------------------------ desde la app

    @Transactional
    public EmailDtos.Resumen aprobar(Long id, EmailDtos.Aprobacion aprobacion) {
        var e = buscarEnEstado(id, Estado.PENDIENTE_REVISION);
        if (aprobacion.asunto() != null && !aprobacion.asunto().isBlank()) {
            e.setRespuestaAsunto(recortar(aprobacion.asunto(), 300));
        }
        e.setRespuesta(recortar(aprobacion.respuesta(), 5000));
        e.setRevisor(aprobacion.revisor());
        e.setResueltoEn(LocalDateTime.now());
        e.setError(null);
        // Primero se avisa a n8n: si falla, la transacción no cambia nada y se puede reintentar.
        try {
            avisarN8n(e, "APROBADA");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo avisar a n8n para enviar la respuesta: " + ex.getMessage());
        }
        e.setEstado(Estado.APROBADO);
        return EmailDtos.Resumen.de(e);
    }

    @Transactional
    public EmailDtos.Resumen descartar(Long id, EmailDtos.Descarte descarte) {
        var e = buscarEnEstado(id, Estado.PENDIENTE_REVISION);
        e.setEstado(Estado.DESCARTADO);
        e.setRevisor(descarte.revisor());
        e.setError(recortar(descarte.motivo(), 500));
        e.setResueltoEn(LocalDateTime.now());
        try {
            avisarN8n(e, "RECHAZADA"); // para que el flujo no se quede esperando para siempre
        } catch (Exception ex) {
            log.warn("No se pudo avisar a n8n del descarte del email {}: {}", id, ex.getMessage());
        }
        return EmailDtos.Resumen.de(e);
    }

    // ------------------------------------------------------------------ reglas

    /**
     * Busca al cliente con lo más fiable primero: NIF → email del remitente → nº de póliza → matrícula.
     * La IA solo propone los datos; quién es el cliente lo decide la cartera.
     */
    private void identificar(EmailEntrante e, Map<String, Object> datos, List<String> avisos) {
        String nif = texto(datos.get("nif"));
        String numeroPoliza = texto(datos.get("numero_poliza"));
        String matricula = texto(datos.get("matricula"));

        Cliente cliente = null;
        String por = null;

        if (nif != null) {
            cliente = clientes.findByNifIgnoreCase(nif.replaceAll("[\\s.-]", "")).orElse(null);
            if (cliente != null) {
                por = "NIF";
            } else {
                avisos.add("El NIF " + nif + " no corresponde a ningún cliente.");
            }
        }
        if (cliente == null && e.getRemitenteEmail() != null) {
            var porEmail = clientes.findByEmailIgnoreCase(e.getRemitenteEmail());
            if (porEmail.size() == 1) {
                cliente = porEmail.get(0);
                por = "EMAIL";
            }
        }

        Poliza poliza = null;
        if (numeroPoliza != null) {
            poliza = polizas.findByNumeroIgnoreCase(numeroPoliza.trim()).orElse(null);
            if (poliza == null) {
                avisos.add("La póliza " + numeroPoliza + " no existe en la cartera.");
            }
        }
        if (poliza == null && matricula != null) {
            var porMatricula = polizas.findByMatriculaIgnoreCase(matricula.replaceAll("[\\s-]", ""));
            poliza = porMatricula.stream().filter(p -> p.estaEnVigor(LocalDate.now())).findFirst()
                    .orElse(porMatricula.isEmpty() ? null : porMatricula.get(0));
            if (poliza == null) {
                avisos.add("La matrícula " + matricula + " no está asegurada en la cartera.");
            }
        }

        if (poliza != null) {
            if (cliente == null) {
                cliente = poliza.getCliente();
                por = numeroPoliza != null && poliza.getNumero().equalsIgnoreCase(numeroPoliza.trim()) ? "POLIZA" : "MATRICULA";
            } else if (!poliza.getCliente().getId().equals(cliente.getId())) {
                avisos.add("La póliza " + poliza.getNumero() + " es de otro cliente (" + poliza.getCliente().getNombre()
                        + "). Compruébalo antes de responder.");
                poliza = null;
            }
        }

        // Cliente identificado pero sin póliza concreta: si solo tiene una del ramo del que habla, es esa.
        if (cliente != null && poliza == null) {
            Poliza.Ramo ramo = ramoDeTexto(texto(datos.get("ramo")));
            if (ramo != null) {
                var delRamo = polizas.findByClienteIdOrderByFechaVencimientoDesc(cliente.getId()).stream()
                        .filter(p -> p.getRamo() == ramo && p.getEstado() == Poliza.Estado.VIGENTE).toList();
                if (delRamo.size() == 1) {
                    poliza = delRamo.get(0);
                }
            }
        }

        if (cliente == null) {
            avisos.add("No se ha podido identificar al cliente: puede ser un cliente nuevo.");
        } else if (!"EMAIL".equals(por) && e.getRemitenteEmail() != null
                && !e.getRemitenteEmail().equalsIgnoreCase(Objects.requireNonNullElse(cliente.getEmail(), ""))) {
            avisos.add("El remitente (" + e.getRemitenteEmail() + ") no es el email registrado del cliente.");
        }
        if (poliza != null && !poliza.estaEnVigor(LocalDate.now())) {
            avisos.add("La póliza " + poliza.getNumero() + " no está en vigor.");
        }

        e.setCliente(cliente);
        e.setPoliza(poliza);
        e.setIdentificadoPor(por);
    }

    /** Quién atiende cada tipo de email (reglas fijas, no las decide la IA). */
    static String asignar(Categoria categoria, Poliza.Ramo ramo) {
        return switch (categoria) {
            case SINIESTRO -> SiniestroService.tramitadorPorRamo(ramo);
            case PRESUPUESTO, BAJA -> "Lucía Moreno";
            case RECIBOS -> "Rocío Benítez";
            case MODIFICACION, DOCUMENTACION -> "Javier Ortega";
            case OTRO -> "Sara Gil";
        };
    }

    private Poliza.Ramo ramo(EmailEntrante e, Map<String, Object> datos) {
        if (e.getPoliza() != null) {
            return e.getPoliza().getRamo();
        }
        Poliza.Ramo ramo = ramoDeTexto(texto(datos.get("ramo")));
        if (ramo == null && texto(datos.get("matricula")) != null) {
            return Poliza.Ramo.AUTO;
        }
        return ramo;
    }

    /** "Autos", "auto", "coche" → AUTO; "hogar" → HOGAR... (la IA no siempre escribe igual). */
    static Poliza.Ramo ramoDeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase();
        if (t.contains("AUTO") || t.contains("COCHE") || t.contains("VEHIC") || t.contains("MOTO")) return Poliza.Ramo.AUTO;
        if (t.contains("HOGAR") || t.contains("CASA") || t.contains("VIVIENDA")) return Poliza.Ramo.HOGAR;
        if (t.contains("COMERCIO") || t.contains("NEGOCIO")) return Poliza.Ramo.COMERCIO;
        if (t.contains("VIDA")) return Poliza.Ramo.VIDA;
        if (t.contains("DECESO")) return Poliza.Ramo.DECESOS;
        if (t.contains("SALUD") || t.contains("MEDIC")) return Poliza.Ramo.SALUD;
        return null;
    }

    // ------------------------------------------------------------------ n8n

    private void avisarN8n(EmailEntrante e, String decision) {
        if (e.getCallbackUrl() == null || e.getCallbackUrl().isBlank()) {
            throw new IllegalStateException("El email no tiene callbackUrl de n8n");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("emailId", e.getId());
        cuerpo.put("decision", decision);
        cuerpo.put("para", e.getRemitenteEmail());
        cuerpo.put("asunto", e.getRespuestaAsunto());
        cuerpo.put("respuesta", e.getRespuesta());
        cuerpo.put("revisor", e.getRevisor());
        cuerpo.put("asignadoA", e.getAsignadoA());
        restClient.post().uri(URI.create(urlInterna(e.getCallbackUrl())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    private String urlInterna(String url) {
        if (n8nUrlInterna == null || n8nUrlInterna.isBlank()) {
            return url;
        }
        return url.replaceFirst("^https?://(localhost|127\\.0\\.0\\.1):5678", n8nUrlInterna);
    }

    // ------------------------------------------------------------------ utilidades

    /** "Víctor García <victor@correo.example>" → "victor@correo.example". */
    static String extraerEmail(String remitente) {
        if (remitente == null) {
            return null;
        }
        Matcher m = EMAIL_ENTRE_ANGULOS.matcher(remitente);
        String email = m.find() ? m.group(1) : remitente;
        email = email.trim().toLowerCase();
        return email.contains("@") ? email : null;
    }

    /** Fecha de la cabecera del email ("Tue, 10 Nov 2026 18:35:09 +0100") en hora de Madrid. */
    static LocalDateTime parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return LocalDateTime.now();
        }
        String limpia = fecha.replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
        try {
            return ZonedDateTime.parse(limpia, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(MADRID).toLocalDateTime();
        } catch (Exception ex1) {
            try {
                return ZonedDateTime.parse(limpia).withZoneSameInstant(MADRID).toLocalDateTime();
            } catch (Exception ex2) {
                return LocalDateTime.now();
            }
        }
    }

    private static <E extends Enum<E>> E enumOr(Class<E> tipo, String valor, E porDefecto) {
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return porDefecto;
        }
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.toString().trim();
        return t.isEmpty() || t.equalsIgnoreCase("null") ? null : t;
    }

    private static String recortar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
    }

    private EmailEntrante buscarEnEstado(Long id, Estado esperado) {
        var e = buscar(id);
        if (e.getEstado() != esperado) {
            throw new IllegalStateException("El email " + id + " está " + e.getEstado() + " (se esperaba " + esperado + ")");
        }
        return e;
    }

    private EmailEntrante buscar(Long id) {
        return emails.findById(id).orElseThrow(() -> new NoSuchElementException("Email no encontrado: " + id));
    }
}
