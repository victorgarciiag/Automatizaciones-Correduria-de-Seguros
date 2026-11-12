package es.correduria.backoffice.web;

import es.correduria.backoffice.repositorio.ClienteRepository;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.servicio.AltaPolizaService;
import es.correduria.backoffice.servicio.AltaPolizaValidacionService;
import es.correduria.backoffice.servicio.AvisoRecibosService;
import es.correduria.backoffice.servicio.BuzonService;
import es.correduria.backoffice.servicio.ComparativaService;
import es.correduria.backoffice.servicio.EjecucionService;
import es.correduria.backoffice.servicio.RevisionService;
import es.correduria.backoffice.servicio.SiniestroService;
import es.correduria.backoffice.web.dto.AltaPolizaDtos;
import es.correduria.backoffice.web.dto.AutomatizacionDtos;
import es.correduria.backoffice.web.dto.ClienteDtos;
import es.correduria.backoffice.web.dto.ComparativaDtos;
import es.correduria.backoffice.web.dto.EmailDtos;
import es.correduria.backoffice.web.dto.PolizaDtos;
import es.correduria.backoffice.web.dto.ReciboDtos;
import es.correduria.backoffice.web.dto.RevisionDtos;
import es.correduria.backoffice.web.dto.SiniestroDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * Endpoints para las automatizaciones (n8n). Requieren la cabecera X-API-Key.
 * Patrón: la IA extrae datos → n8n los valida aquí contra datos reales → actúa o pide revisión.
 */
@RestController
@RequestMapping("/api/integracion")
@RequiredArgsConstructor
@Tag(name = "Integración (n8n)")
@SecurityRequirement(name = "apiKey")
public class IntegracionController {

    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final SiniestroService siniestros;
    private final RevisionService revisiones;
    private final EjecucionService ejecuciones;
    private final AvisoRecibosService avisoRecibos;
    private final ComparativaService comparativas;
    private final AltaPolizaService altasPoliza;
    private final AltaPolizaValidacionService validacionAltas;
    private final BuzonService buzon;

    @Operation(summary = "Buscar clientes por NIF, teléfono o email")
    @GetMapping("/clientes")
    @Transactional(readOnly = true)
    public List<ClienteDtos.Resumen> buscarClientes(@RequestParam(required = false) String nif,
                                                    @RequestParam(required = false) String telefono,
                                                    @RequestParam(required = false) String email) {
        Stream<es.correduria.backoffice.dominio.Cliente> resultado = Stream.empty();
        if (nif != null) {
            resultado = clientes.findByNifIgnoreCase(nif.trim()).stream();
        } else if (telefono != null) {
            resultado = clientes.findByTelefono(normalizarTelefono(telefono)).stream();
        } else if (email != null) {
            resultado = clientes.findByEmailIgnoreCase(email.trim()).stream();
        }
        return resultado.map(ClienteDtos.Resumen::de).toList();
    }

    @Operation(summary = "Buscar pólizas por número, matrícula o NIF del cliente (incluye si está en vigor)")
    @GetMapping("/polizas")
    @Transactional(readOnly = true)
    public List<PolizaDtos.Resumen> buscarPolizas(@RequestParam(required = false) String numero,
                                                  @RequestParam(required = false) String matricula,
                                                  @RequestParam(required = false) String nif) {
        if (numero != null) {
            return polizas.findByNumeroIgnoreCase(numero.trim()).stream().map(PolizaDtos.Resumen::de).toList();
        }
        if (matricula != null) {
            return polizas.findByMatriculaIgnoreCase(matricula.replace(" ", "").replace("-", ""))
                    .stream().map(PolizaDtos.Resumen::de).toList();
        }
        if (nif != null) {
            return clientes.findByNifIgnoreCase(nif.trim()).stream()
                    .flatMap(c -> polizas.findByClienteIdOrderByFechaVencimientoDesc(c.getId()).stream())
                    .map(PolizaDtos.Resumen::de).toList();
        }
        throw new IllegalArgumentException("Indica numero, matricula o nif");
    }

    @Operation(summary = "Abrir un siniestro (valida que la póliza esté en vigor)")
    @PostMapping("/siniestros")
    @ResponseStatus(HttpStatus.CREATED)
    public SiniestroDtos.Resumen abrirSiniestro(@Valid @RequestBody SiniestroDtos.Alta alta) {
        return siniestros.alta(alta);
    }

    @Operation(summary = "Enviar algo a la bandeja de revisión humana")
    @PostMapping("/revisiones")
    @ResponseStatus(HttpStatus.CREATED)
    public RevisionDtos.Resumen pedirRevision(@Valid @RequestBody RevisionDtos.Alta alta) {
        return revisiones.crear(alta);
    }

    @Operation(summary = "Consultar el estado de una revisión")
    @GetMapping("/revisiones/{id}")
    public RevisionDtos.Resumen consultarRevision(@PathVariable Long id) {
        return revisiones.obtener(id);
    }

    @Operation(summary = "Recibos devueltos a los que hoy les toca un aviso (el campo 'accion' dice cuál)")
    @GetMapping("/recibos/avisos-pendientes")
    public List<ReciboDtos.Aviso> avisosPendientes(@RequestParam(defaultValue = "10") int limite) {
        return avisoRecibos.pendientes(Math.max(1, Math.min(limite, 100)));
    }

    @Operation(summary = "Confirmar que se ha enviado el aviso de un recibo")
    @PostMapping("/recibos/{id}/aviso-enviado")
    public ReciboDtos.Aviso avisoEnviado(@PathVariable Long id) {
        return avisoRecibos.registrarAviso(id);
    }

    @Operation(summary = "Comparativa: guardar el borrador del email generado por la IA (pasa a revisión)")
    @PostMapping("/comparativas/{id}/borrador")
    public ComparativaDtos.Resumen borradorComparativa(@PathVariable Long id, @Valid @RequestBody ComparativaDtos.Borrador borrador) {
        return comparativas.guardarBorrador(id, borrador);
    }

    @Operation(summary = "Comparativa: confirmar que el email se ha enviado al cliente")
    @PostMapping("/comparativas/{id}/enviada")
    public ComparativaDtos.Resumen comparativaEnviada(@PathVariable Long id) {
        return comparativas.marcarEnviada(id);
    }

    @Operation(summary = "Comparativa: informar de un error en el flujo")
    @PostMapping("/comparativas/{id}/error")
    public ComparativaDtos.Resumen comparativaError(@PathVariable Long id, @Valid @RequestBody ComparativaDtos.ErrorN8n error) {
        return comparativas.marcarError(id, error.mensaje());
    }

    @Operation(summary = "Alta de póliza: validar los datos extraídos por la IA y, si todo cuadra, dar de alta cliente y póliza")
    @PostMapping("/polizas/alta")
    public AltaPolizaDtos.RespuestaAlta altaPoliza(@RequestBody AltaPolizaDtos.SolicitudAlta solicitud) {
        return validacionAltas.darDeAlta(solicitud);
    }

    @Operation(summary = "Alta de póliza: informar del resultado (CREADA, REVISION o ERROR) de un PDF subido desde la app")
    @PostMapping("/altas-poliza/{id}/resultado")
    public AltaPolizaDtos.Resumen resultadoAltaPoliza(@PathVariable Long id, @Valid @RequestBody AltaPolizaDtos.Resultado resultado) {
        return altasPoliza.registrarResultado(id, resultado);
    }

    @Operation(summary = "Buzón: registrar un email clasificado por la IA (identifica al cliente, asigna persona y devuelve el contexto)")
    @PostMapping("/emails")
    @ResponseStatus(HttpStatus.CREATED)
    public EmailDtos.Contexto registrarEmail(@Valid @RequestBody EmailDtos.Entrada entrada) {
        return buzon.registrar(entrada);
    }

    @Operation(summary = "Buzón: guardar el borrador de respuesta (pasa a revisión)")
    @PostMapping("/emails/{id}/borrador")
    public EmailDtos.Resumen borradorEmail(@PathVariable Long id, @Valid @RequestBody EmailDtos.Borrador borrador) {
        return buzon.guardarBorrador(id, borrador);
    }

    @Operation(summary = "Buzón: confirmar que la respuesta se ha enviado")
    @PostMapping("/emails/{id}/enviado")
    public EmailDtos.Resumen emailEnviado(@PathVariable Long id) {
        return buzon.marcarEnviado(id);
    }

    @Operation(summary = "Buzón: informar de un error en el flujo")
    @PostMapping("/emails/{id}/error")
    public EmailDtos.Resumen emailError(@PathVariable Long id, @Valid @RequestBody EmailDtos.ErrorN8n error) {
        return buzon.marcarError(id, error.mensaje());
    }

    @Operation(summary = "Registrar una ejecución (trazabilidad, coste y ahorro)")
    @PostMapping("/ejecuciones")
    @ResponseStatus(HttpStatus.CREATED)
    public AutomatizacionDtos.EjecucionResumen registrarEjecucion(@Valid @RequestBody AutomatizacionDtos.AltaEjecucion alta) {
        return ejecuciones.registrar(alta);
    }

    /** Los teléfonos se guardan como "6XX XXX XXX". Acepta +34, espacios y guiones. */
    private static String normalizarTelefono(String telefono) {
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.length() == 11 && digitos.startsWith("34")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() != 9) {
            return telefono.trim();
        }
        return digitos.substring(0, 3) + " " + digitos.substring(3, 6) + " " + digitos.substring(6);
    }
}
