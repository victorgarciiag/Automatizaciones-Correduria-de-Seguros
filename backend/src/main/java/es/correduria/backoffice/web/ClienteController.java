package es.correduria.backoffice.web;

import es.correduria.backoffice.dominio.Cliente;
import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.repositorio.ClienteRepository;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.repositorio.SiniestroRepository;
import es.correduria.backoffice.web.dto.ClienteDtos;
import es.correduria.backoffice.web.dto.Pagina;
import es.correduria.backoffice.web.dto.PolizaDtos;
import es.correduria.backoffice.web.dto.SiniestroDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteController {

    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final SiniestroRepository siniestros;

    @GetMapping
    public Pagina<ClienteDtos.Resumen> listar(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Cliente.Tipo tipo,
                                              @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamano) {
        Specification<Cliente> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String patron = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(root.get("nif")), patron),
                    cb.like(cb.lower(root.get("email")), patron),
                    cb.like(root.get("telefono"), patron)));
        }
        if (tipo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        var page = clientes.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), Sort.by("nombre")));
        return Pagina.de(page, ClienteDtos.Resumen::de);
    }

    @GetMapping("/{id}")
    public ClienteDtos.Detalle detalle(@PathVariable Long id) {
        var cliente = clientes.findById(id).orElseThrow(() -> new NoSuchElementException("Cliente no encontrado: " + id));
        var lista = polizas.findByClienteIdOrderByFechaVencimientoDesc(id);
        var vigentes = lista.stream().filter(p -> p.getEstado() == Poliza.Estado.VIGENTE).toList();
        var primaTotal = vigentes.stream().map(Poliza::getPrimaAnual).reduce(BigDecimal.ZERO, BigDecimal::add);
        var siniestrosCliente = lista.stream()
                .flatMap(p -> siniestros.findByPolizaIdOrderByFechaAperturaDesc(p.getId()).stream())
                .map(SiniestroDtos.Resumen::de)
                .sorted(Comparator.comparing(SiniestroDtos.Resumen::fechaApertura).reversed())
                .toList();
        return new ClienteDtos.Detalle(ClienteDtos.Resumen.de(cliente),
                lista.stream().map(PolizaDtos.Resumen::de).toList(), primaTotal, vigentes.size(), siniestrosCliente);
    }
}
