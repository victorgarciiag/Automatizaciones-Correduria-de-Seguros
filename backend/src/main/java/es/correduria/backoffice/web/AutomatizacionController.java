package es.correduria.backoffice.web;

import es.correduria.backoffice.repositorio.EjecucionRepository;
import es.correduria.backoffice.repositorio.RevisionRepository;
import es.correduria.backoffice.servicio.PanelAutomatizacionesService;
import es.correduria.backoffice.web.dto.AutomatizacionDtos;
import es.correduria.backoffice.web.dto.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/automatizaciones")
@RequiredArgsConstructor
public class AutomatizacionController {

    private final PanelAutomatizacionesService panel;
    private final EjecucionRepository ejecuciones;
    private final RevisionRepository revisiones;

    @GetMapping("/panel")
    public AutomatizacionDtos.Panel panel(@RequestParam(defaultValue = "30") int dias) {
        return panel.panel(Math.max(1, Math.min(dias, 365)));
    }

    @GetMapping("/ejecuciones")
    public Pagina<AutomatizacionDtos.EjecucionResumen> ejecuciones(@RequestParam(required = false) String automatizacion,
                                                                  @RequestParam(defaultValue = "0") int pagina,
                                                                  @RequestParam(defaultValue = "25") int tamano) {
        var pageable = PageRequest.of(pagina, Math.min(tamano, 100));
        var page = automatizacion == null || automatizacion.isBlank()
                ? ejecuciones.findAllByOrderByInicioDesc(pageable)
                : ejecuciones.findByAutomatizacionOrderByInicioDesc(automatizacion, pageable);
        return Pagina.de(page, AutomatizacionDtos.EjecucionResumen::de);
    }

    /** Borra las ejecuciones y revisiones de demostración para empezar a medir con datos reales. */
    @DeleteMapping("/demo")
    @Transactional
    public Map<String, Long> borrarDatosDemo() {
        return Map.of("ejecuciones", ejecuciones.deleteByDemoTrue(), "revisiones", revisiones.deleteByDemoTrue());
    }
}
