package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Aseguradora;
import es.correduria.backoffice.dominio.Cliente;
import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.repositorio.AseguradoraRepository;
import es.correduria.backoffice.repositorio.ClienteRepository;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.web.dto.AltaPolizaDtos.RespuestaAlta;
import es.correduria.backoffice.web.dto.AltaPolizaDtos.SolicitudAlta;
import es.correduria.backoffice.web.dto.AltaPolizaDtos.Tomador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatización 3: valida los datos que la IA ha extraído de un PDF de póliza y, si todo cuadra, la da de alta.
 * Si algo no cuadra NO se guarda nada: se devuelve REVISION con la lista de motivos para que decida una persona.
 */
@Service
@RequiredArgsConstructor
public class AltaPolizaValidacionService {

    private static final String LETRAS_NIF = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Pattern DNI = Pattern.compile("^(\\d{8})([A-Z])$");
    private static final Pattern NIE = Pattern.compile("^([XYZ])(\\d{7})([A-Z])$");
    private static final Pattern MATRICULA = Pattern.compile("(\\d{4})\\s?([BCDFGHJKLMNPRSTVWXYZ]{3})");
    private static final Pattern LOCALIDAD = Pattern.compile("\\d{5}\\s+(.+)$");

    private final AseguradoraRepository aseguradoras;
    private final ClienteRepository clientes;
    private final PolizaRepository polizas;

    @Transactional
    public RespuestaAlta darDeAlta(SolicitudAlta s) {
        List<String> motivos = new ArrayList<>();
        Tomador t = s.tomador() != null ? s.tomador() : new Tomador(null, null, null, null, null);

        // 1. Campos obligatorios
        obligatorio(s.numeroPoliza(), "Falta el número de póliza", motivos);
        obligatorio(s.aseguradora(), "Falta la aseguradora", motivos);
        obligatorio(t.nombre(), "Falta el nombre del tomador", motivos);
        obligatorio(t.nif(), "Falta el NIF del tomador", motivos);

        // 2. NIF del tomador (letra de control)
        String nif = t.nif() == null ? null : t.nif().replaceAll("[\\s-]", "").toUpperCase();
        if (nif != null && !nif.isBlank()) {
            validarNif(nif).ifPresent(motivos::add);
        }

        // 3. Fechas y prima
        LocalDate efecto = fecha(s.fechaEfecto(), "efecto", motivos);
        LocalDate vencimiento = fecha(s.fechaVencimiento(), "vencimiento", motivos);
        if (efecto != null && vencimiento != null && !vencimiento.isAfter(efecto)) {
            motivos.add("La fecha de vencimiento (" + vencimiento + ") no es posterior a la de efecto (" + efecto + ")");
        }
        if (s.primaAnual() == null || s.primaAnual().compareTo(BigDecimal.ZERO) <= 0) {
            motivos.add("La prima anual falta o no es mayor que 0");
        }

        // 4. Contra los datos de la cartera
        Aseguradora aseguradora = null;
        if (s.aseguradora() != null && !s.aseguradora().isBlank()) {
            aseguradora = aseguradoras.findByNombreIgnoreCase(s.aseguradora().trim()).orElse(null);
            if (aseguradora == null) {
                motivos.add("Aseguradora desconocida: " + s.aseguradora());
            }
        }
        if (s.numeroPoliza() != null && polizas.findByNumeroIgnoreCase(s.numeroPoliza().trim()).isPresent()) {
            motivos.add("La póliza " + s.numeroPoliza() + " ya existe en la cartera");
        }
        Poliza.Ramo ramo = ramo(s.ramo());
        if (ramo == null) {
            motivos.add("Ramo no reconocido: " + s.ramo());
        }
        Poliza.Periodicidad periodicidad = periodicidad(s.formaPago());
        if (periodicidad == null) {
            motivos.add("Forma de pago no reconocida: " + s.formaPago());
        }
        Optional<Cliente> existente = nif == null ? Optional.empty() : clientes.findByNifIgnoreCase(nif);
        if (existente.isPresent() && t.nombre() != null
                && !normalizar(existente.get().getNombre()).equals(normalizar(t.nombre()))) {
            motivos.add("El NIF " + nif + " ya pertenece a otro cliente: " + existente.get().getNombre());
        }

        if (!motivos.isEmpty()) {
            return new RespuestaAlta("REVISION", null, s.numeroPoliza(), existente.map(Cliente::getId).orElse(null),
                    false, motivos);
        }

        // 5. Todo cuadra: cliente (si es nuevo) + póliza
        boolean clienteNuevo = existente.isEmpty();
        Cliente cliente = existente.orElseGet(() -> clientes.save(nuevoCliente(t, nif)));

        var p = new Poliza();
        p.setNumero(s.numeroPoliza().trim());
        p.setCliente(cliente);
        p.setAseguradora(aseguradora);
        p.setRamo(ramo);
        p.setProducto(s.producto() != null ? s.producto() : s.ramo());
        p.setObjetoAsegurado(s.objetoAsegurado() != null ? s.objetoAsegurado() : cliente.getNombre());
        p.setMatricula(ramo == Poliza.Ramo.AUTO ? matricula(s.objetoAsegurado()) : null);
        p.setFechaEfecto(efecto);
        p.setFechaVencimiento(vencimiento);
        p.setPrimaAnual(s.primaAnual());
        p.setPeriodicidad(periodicidad);
        p.setComisionPct(BigDecimal.valueOf(ramo == Poliza.Ramo.AUTO ? 12 : 15));
        p.setEstado(Poliza.Estado.VIGENTE);
        p = polizas.save(p);

        return new RespuestaAlta("CREADA", p.getId(), p.getNumero(), cliente.getId(), clienteNuevo, List.of());
    }

    // ------------------------------------------------------------------ validaciones

    /** DNI y NIE: se comprueba la letra de control. CIF de empresa: solo se acepta el formato (sin dígito de control). */
    static Optional<String> validarNif(String nif) {
        Matcher dni = DNI.matcher(nif);
        if (dni.matches()) {
            char esperada = LETRAS_NIF.charAt(Integer.parseInt(dni.group(1)) % 23);
            return dni.group(2).charAt(0) == esperada ? Optional.empty()
                    : Optional.of("La letra del NIF " + nif + " no es correcta (debería ser " + esperada + ")");
        }
        Matcher nie = NIE.matcher(nif);
        if (nie.matches()) {
            int prefijo = "XYZ".indexOf(nie.group(1));
            char esperada = LETRAS_NIF.charAt(Integer.parseInt(prefijo + nie.group(2)) % 23);
            return nie.group(3).charAt(0) == esperada ? Optional.empty()
                    : Optional.of("La letra del NIE " + nif + " no es correcta (debería ser " + esperada + ")");
        }
        if (nif.matches("^[A-W]\\d{7,8}[0-9A-Z]$")) {
            return Optional.empty();
        }
        return Optional.of("El NIF " + nif + " no tiene un formato válido");
    }

    private static void obligatorio(String valor, String motivo, List<String> motivos) {
        if (valor == null || valor.isBlank()) {
            motivos.add(motivo);
        }
    }

    private static LocalDate fecha(String valor, String nombre, List<String> motivos) {
        if (valor == null || valor.isBlank()) {
            motivos.add("Falta la fecha de " + nombre);
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            motivos.add("Fecha de " + nombre + " no válida: " + valor);
            return null;
        }
    }

    // ------------------------------------------------------------------ traducciones del texto de la IA

    static Poliza.Ramo ramo(String texto) {
        String r = normalizar(texto);
        if (r.isEmpty()) return null;
        if (r.contains("hogar")) return Poliza.Ramo.HOGAR;
        if (r.contains("auto") || r.contains("moto") || r.contains("vehiculo")) return Poliza.Ramo.AUTO;
        if (r.contains("comercio") || r.contains("multirriesgo") || r.contains("pyme")
                || r.contains("responsabilidad")) return Poliza.Ramo.COMERCIO;
        if (r.contains("deceso")) return Poliza.Ramo.DECESOS;
        if (r.contains("vida")) return Poliza.Ramo.VIDA;
        if (r.contains("salud")) return Poliza.Ramo.SALUD;
        return null;
    }

    /** Si la IA no indica forma de pago se asume anual (lo habitual). */
    static Poliza.Periodicidad periodicidad(String texto) {
        String f = normalizar(texto);
        if (f.isEmpty() || f.contains("anual") && !f.contains("semestral")) return Poliza.Periodicidad.ANUAL;
        if (f.contains("semestral")) return Poliza.Periodicidad.SEMESTRAL;
        if (f.contains("trimestral")) return Poliza.Periodicidad.TRIMESTRAL;
        if (f.contains("mensual")) return Poliza.Periodicidad.MENSUAL;
        return null;
    }

    private static Cliente nuevoCliente(Tomador t, String nif) {
        var c = new Cliente();
        boolean empresa = !nif.isEmpty() && Character.isLetter(nif.charAt(0)) && "XYZ".indexOf(nif.charAt(0)) < 0;
        c.setTipo(empresa ? Cliente.Tipo.EMPRESA : Cliente.Tipo.PARTICULAR);
        c.setNombre(t.nombre().trim());
        c.setNif(nif);
        c.setTelefono(t.telefono());
        c.setEmail(t.email());
        c.setDireccion(t.direccion());
        if (t.direccion() != null) {
            Matcher m = LOCALIDAD.matcher(t.direccion().trim());
            c.setLocalidad(m.find() ? m.group(1).trim() : null);
        }
        c.setFechaAlta(LocalDate.now());
        return c;
    }

    private static String matricula(String objeto) {
        if (objeto == null) return null;
        Matcher m = MATRICULA.matcher(objeto.toUpperCase());
        return m.find() ? m.group(1) + m.group(2) : null;
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.toLowerCase().trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
