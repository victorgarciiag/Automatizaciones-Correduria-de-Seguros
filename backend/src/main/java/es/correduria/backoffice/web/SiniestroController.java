package es.correduria.backoffice.web;

import es.correduria.backoffice.servicio.SiniestroService;
import es.correduria.backoffice.web.dto.SiniestroDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/siniestros")
@RequiredArgsConstructor
public class SiniestroController {

    private final SiniestroService siniestros;

    /** Siniestros activos + cerrados en los últimos 30 días (para el tablero). */
    @GetMapping
    public List<SiniestroDtos.Resumen> tablero() {
        return siniestros.tablero();
    }

    @GetMapping("/{id}")
    public SiniestroDtos.Resumen detalle(@PathVariable Long id) {
        return siniestros.obtener(id);
    }

    @PatchMapping("/{id}/estado")
    public SiniestroDtos.Resumen cambiarEstado(@PathVariable Long id, @Valid @RequestBody SiniestroDtos.CambioEstado cambio) {
        return siniestros.cambiarEstado(id, cambio.estado());
    }
}
