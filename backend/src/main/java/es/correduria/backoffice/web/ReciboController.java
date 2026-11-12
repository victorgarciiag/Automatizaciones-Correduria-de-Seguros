package es.correduria.backoffice.web;

import es.correduria.backoffice.dominio.Recibo;
import es.correduria.backoffice.repositorio.ReciboRepository;
import es.correduria.backoffice.web.dto.Pagina;
import es.correduria.backoffice.web.dto.ReciboDtos;
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
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReciboController {

    private final ReciboRepository recibos;

    @GetMapping
    public Pagina<ReciboDtos.Resumen> listar(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Recibo.Estado estado,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "20") int tamano) {
        Specification<Recibo> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String patron = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                var poliza = root.join("poliza", JoinType.INNER);
                var cliente = poliza.join("cliente", JoinType.INNER);
                return cb.or(
                        cb.like(cb.lower(root.get("numero")), patron),
                        cb.like(cb.lower(poliza.get("numero")), patron),
                        cb.like(cb.lower(cliente.get("nombre")), patron));
            });
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        var page = recibos.findAll(spec, PageRequest.of(pagina, Math.min(tamano, 100),
                Sort.by(Sort.Direction.DESC, "fechaEmision")));
        return Pagina.de(page, ReciboDtos.Resumen::de);
    }

    @GetMapping("/totales")
    public ReciboDtos.Totales totales() {
        return new ReciboDtos.Totales(recibos.countByEstado(Recibo.Estado.PENDIENTE),
                recibos.countByEstado(Recibo.Estado.DEVUELTO), recibos.sumaImportes(Recibo.Estado.DEVUELTO));
    }

    @PostMapping("/{id}/cobrar")
    @Transactional
    public ReciboDtos.Resumen cobrar(@PathVariable Long id) {
        var r = buscar(id);
        if (r.getEstado() == Recibo.Estado.COBRADO || r.getEstado() == Recibo.Estado.ANULADO) {
            throw new IllegalStateException("El recibo " + r.getNumero() + " ya está " + r.getEstado());
        }
        r.setEstado(Recibo.Estado.COBRADO);
        r.setFechaCobro(LocalDate.now());
        return ReciboDtos.Resumen.de(r);
    }

    @PostMapping("/{id}/reclamar")
    @Transactional
    public ReciboDtos.Resumen reclamar(@PathVariable Long id) {
        var r = buscar(id);
        if (r.getEstado() != Recibo.Estado.DEVUELTO) {
            throw new IllegalStateException("Solo se reclaman recibos devueltos");
        }
        r.setIntentosReclamacion(r.getIntentosReclamacion() + 1);
        r.setFechaUltimoAviso(java.time.LocalDateTime.now());
        return ReciboDtos.Resumen.de(r);
    }

    private Recibo buscar(Long id) {
        return recibos.findById(id).orElseThrow(() -> new NoSuchElementException("Recibo no encontrado: " + id));
    }
}
