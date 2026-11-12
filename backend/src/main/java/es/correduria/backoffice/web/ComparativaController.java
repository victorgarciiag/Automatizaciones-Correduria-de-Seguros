package es.correduria.backoffice.web;

import es.correduria.backoffice.servicio.ComparativaService;
import es.correduria.backoffice.web.dto.ComparativaDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comparativas")
@RequiredArgsConstructor
public class ComparativaController {

    private final ComparativaService comparativas;

    @GetMapping
    public List<ComparativaDtos.Resumen> listar() {
        return comparativas.listar();
    }

    @GetMapping("/{id}")
    public ComparativaDtos.Resumen detalle(@PathVariable Long id) {
        return comparativas.obtener(id);
    }

    /** Solo para pruebas: borra todas las comparativas. */
    @DeleteMapping
    public Map<String, Long> limpiar() {
        return Map.of("comparativas", comparativas.limpiarPruebas());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ComparativaDtos.Resumen crear(@RequestParam String clienteNombre,
                                         @RequestParam String clienteEmail,
                                         @RequestParam String vehiculo,
                                         @RequestParam(required = false) String creadaPor,
                                         @RequestParam("pdfs") List<MultipartFile> pdfs) {
        return comparativas.crear(clienteNombre, clienteEmail, vehiculo, creadaPor, pdfs);
    }

    @PostMapping("/{id}/aprobar")
    public ComparativaDtos.Resumen aprobar(@PathVariable Long id, @Valid @RequestBody ComparativaDtos.Aprobacion aprobacion) {
        return comparativas.aprobar(id, aprobacion);
    }

    @PostMapping("/{id}/rechazar")
    public ComparativaDtos.Resumen rechazar(@PathVariable Long id, @Valid @RequestBody ComparativaDtos.Rechazo rechazo) {
        return comparativas.rechazar(id, rechazo);
    }
}
