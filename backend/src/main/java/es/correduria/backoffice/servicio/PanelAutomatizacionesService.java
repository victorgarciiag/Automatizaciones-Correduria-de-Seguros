package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Automatizacion;
import es.correduria.backoffice.dominio.Ejecucion;
import es.correduria.backoffice.repositorio.AutomatizacionRepository;
import es.correduria.backoffice.repositorio.EjecucionRepository;
import es.correduria.backoffice.repositorio.RevisionRepository;
import es.correduria.backoffice.dominio.Revision;
import es.correduria.backoffice.web.dto.AutomatizacionDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Calcula las métricas del panel: ejecuciones, éxito, coste y horas ahorradas. */
@Service
@RequiredArgsConstructor
public class PanelAutomatizacionesService {

    private final AutomatizacionRepository automatizaciones;
    private final EjecucionRepository ejecuciones;
    private final RevisionRepository revisiones;

    @Value("${app.coste-hora-empleado:18}")
    private double costeHoraEmpleado;

    public AutomatizacionDtos.Panel panel(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(dias - 1L);
        List<Ejecucion> lista = ejecuciones.findByInicioAfter(desde.atStartOfDay());

        Map<String, List<Ejecucion>> porAutomatizacion = lista.stream()
                .collect(Collectors.groupingBy(Ejecucion::getAutomatizacion));

        List<AutomatizacionDtos.Estadisticas> stats = automatizaciones.findAllByOrderByOrdenAsc().stream()
                .map(a -> estadisticas(a, porAutomatizacion.getOrDefault(a.getCodigo(), List.of())))
                .toList();

        Map<LocalDate, List<Ejecucion>> porFecha = lista.stream()
                .collect(Collectors.groupingBy(e -> e.getInicio().toLocalDate()));
        List<AutomatizacionDtos.Dia> porDia = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hoy); d = d.plusDays(1)) {
            var delDia = porFecha.getOrDefault(d, List.of());
            porDia.add(new AutomatizacionDtos.Dia(d,
                    contar(delDia, Ejecucion.Resultado.OK),
                    contar(delDia, Ejecucion.Resultado.REVISION),
                    contar(delDia, Ejecucion.Resultado.ERROR),
                    horas(delDia)));
        }

        long total = lista.size();
        double horas = horas(lista);
        return new AutomatizacionDtos.Panel(dias, total, tasaExito(lista), coste(lista), horas,
                redondear(horas * costeHoraEmpleado), revisiones.countByEstado(Revision.Estado.PENDIENTE),
                lista.stream().anyMatch(Ejecucion::isDemo) || ejecuciones.existsByDemoTrue(),
                porDia, stats);
    }

    private AutomatizacionDtos.Estadisticas estadisticas(Automatizacion a, List<Ejecucion> lista) {
        BigDecimal coste = coste(lista);
        double costeMedio = lista.isEmpty() ? 0
                : coste.divide(BigDecimal.valueOf(lista.size()), 5, RoundingMode.HALF_UP).doubleValue();
        LocalDateTime ultima = lista.stream().map(Ejecucion::getInicio).max(Comparator.naturalOrder()).orElse(null);
        return new AutomatizacionDtos.Estadisticas(a.getCodigo(), a.getNombre(), a.getDescripcion(), a.getArea(),
                a.getEstado(), a.getMinutosManuales(), a.getOrden(), lista.size(),
                contar(lista, Ejecucion.Resultado.OK), contar(lista, Ejecucion.Resultado.REVISION),
                contar(lista, Ejecucion.Resultado.ERROR), tasaExito(lista), coste, horas(lista), costeMedio, ultima);
    }

    private static long contar(List<Ejecucion> lista, Ejecucion.Resultado resultado) {
        return lista.stream().filter(e -> e.getResultado() == resultado).count();
    }

    /** Éxito = terminó sin error (incluye las que fueron a revisión humana, que es el comportamiento esperado). */
    private static double tasaExito(List<Ejecucion> lista) {
        if (lista.isEmpty()) {
            return 0;
        }
        return redondear(100.0 * (lista.size() - contar(lista, Ejecucion.Resultado.ERROR)) / lista.size());
    }

    private static BigDecimal coste(List<Ejecucion> lista) {
        return lista.stream().map(Ejecucion::getCosteEur).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static double horas(List<Ejecucion> lista) {
        double minutos = lista.stream().map(Ejecucion::getMinutosAhorrados).mapToDouble(BigDecimal::doubleValue).sum();
        return redondear(minutos / 60.0);
    }

    private static double redondear(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
