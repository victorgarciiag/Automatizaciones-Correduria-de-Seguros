package es.correduria.backoffice.web;

import es.correduria.backoffice.servicio.RevisionService;
import es.correduria.backoffice.web.dto.RevisionDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revisiones")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisiones;

    @GetMapping
    public List<RevisionDtos.Resumen> listar(@RequestParam(defaultValue = "PENDIENTE") String estado) {
        return "PENDIENTE".equalsIgnoreCase(estado) ? revisiones.pendientes() : revisiones.resueltas();
    }

    @GetMapping("/{id}")
    public RevisionDtos.Resumen detalle(@PathVariable Long id) {
        return revisiones.obtener(id);
    }

    @PostMapping("/{id}/aprobar")
    public RevisionDtos.Resumen aprobar(@PathVariable Long id, @Valid @RequestBody RevisionDtos.Resolucion resolucion) {
        return revisiones.aprobar(id, resolucion);
    }

    @PostMapping("/{id}/rechazar")
    public RevisionDtos.Resumen rechazar(@PathVariable Long id, @Valid @RequestBody RevisionDtos.Resolucion resolucion) {
        return revisiones.rechazar(id, resolucion);
    }
}
