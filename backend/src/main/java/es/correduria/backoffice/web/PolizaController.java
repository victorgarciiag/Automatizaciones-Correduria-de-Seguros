package es.correduria.backoffice.web;

import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.repositorio.ReciboRepository;
import es.correduria.backoffice.repositorio.SiniestroRepository;
import es.correduria.backoffice.web.dto.ClienteDtos;
import es.correduria.backoffice.web.dto.Pagina;
import es.correduria.backoffice.web.dto.PolizaDtos;
import es.correduria.backoffice.web.dto.ReciboDtos;
import es.correduria.backoffice.web.dto.SiniestroDtos;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/polizas")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolizaController {

    private final PolizaRepository polizas;
    private final ReciboRepository recibos;
    private final SiniestroRepository siniestros;

    @GetMapping
    public Pagina<PolizaDtos.Resumen> listar(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Poliza.Ramo ramo,
                                             @RequestParam(required = false) Poliza.Estado estado,
                                             @RequestParam(required = false) Long aseguradoraId,
                                             @RequestParam(required = false) Integer venceEnDias,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "20") int tamano) {
        Specification<Poliza> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String patron = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                var cliente = root.join("cliente", JoinType.INNER);
                return cb.or(
                        cb.like(cb.lower(root.get("numero")), patron),
                        cb.like(cb.lower(root.get("matricula")), patron),
                        cb.like(cb.lower(root.get("objetoAsegurado")), patron),
                        cb.like(cb.lower(cliente.get("nombre")), patron),
                        cb.like(cb.lower(cliente.get("nif")), patron));
            });
        }
        if (ramo != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ramo"), ramo));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (aseguradoraId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("aseguradora").get("id"), aseguradoraId));
        }
        if (venceEnDias != null) {
            LocalDate hoy = LocalDate.now();
            spec = spec.and((root, query, cb) -> cb.between(root.get("fechaVencimiento"), hoy, hoy.plusDays(venceEnDias)));
        }
        Sort orden = venceEnDias != null ? Sort.by("fechaVencimiento") : Sort.by(Sort.Direction.DESC, "fechaEfecto");
        var page = polizas.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100), orden));
        return Pagina.de(page, PolizaDtos.Resumen::de);
    }

    @GetMapping("/{id}")
    public PolizaDtos.Detalle detalle(@PathVariable Long id) {
        var p = polizas.findById(id).orElseThrow(() -> new NoSuchElementException("Póliza no encontrada: " + id));
        return new PolizaDtos.Detalle(PolizaDtos.Resumen.de(p), ClienteDtos.Resumen.de(p.getCliente()),
                recibos.findByPolizaIdOrderByFechaEmisionDesc(id).stream().map(ReciboDtos.Resumen::de).toList(),
                siniestros.findByPolizaIdOrderByFechaAperturaDesc(id).stream().map(SiniestroDtos.Resumen::de).toList());
    }
}
