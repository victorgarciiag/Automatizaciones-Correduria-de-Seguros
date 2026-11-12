package es.correduria.backoffice.web;

import es.correduria.backoffice.servicio.BuzonService;
import es.correduria.backoffice.web.dto.EmailDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class BuzonController {

    private final BuzonService buzon;

    @GetMapping
    public List<EmailDtos.Resumen> listar() {
        return buzon.listar();
    }

    @GetMapping("/{id}")
    public EmailDtos.Resumen detalle(@PathVariable Long id) {
        return buzon.obtener(id);
    }

    /** Solo para pruebas: borra todos los emails del buzón. */
    @DeleteMapping
    public Map<String, Long> limpiar() {
        return Map.of("emails", buzon.limpiarPruebas());
    }

    @PostMapping("/{id}/aprobar")
    public EmailDtos.Resumen aprobar(@PathVariable Long id, @Valid @RequestBody EmailDtos.Aprobacion aprobacion) {
        return buzon.aprobar(id, aprobacion);
    }

    @PostMapping("/{id}/descartar")
    public EmailDtos.Resumen descartar(@PathVariable Long id, @Valid @RequestBody EmailDtos.Descarte descarte) {
        return buzon.descartar(id, descarte);
    }
}
