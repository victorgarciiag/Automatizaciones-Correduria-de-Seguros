package es.correduria.backoffice.web;

import es.correduria.backoffice.servicio.AltaPolizaService;
import es.correduria.backoffice.web.dto.AltaPolizaDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/altas-poliza")
@RequiredArgsConstructor
public class AltaPolizaController {

    private final AltaPolizaService altas;

    @GetMapping
    public List<AltaPolizaDtos.Resumen> listar() {
        return altas.listar();
    }

    /** Solo para pruebas: deja la pantalla limpia (ver AltaPolizaService.limpiarPruebas). */
    @DeleteMapping
    public Map<String, Long> limpiar() {
        return altas.limpiarPruebas();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<AltaPolizaDtos.Resumen> subir(@RequestParam("pdfs") List<MultipartFile> pdfs,
                                              @RequestParam(required = false) String creadaPor) {
        return altas.subir(pdfs, creadaPor);
    }
}
