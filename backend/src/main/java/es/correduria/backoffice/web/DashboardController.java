package es.correduria.backoffice.web;

import es.correduria.backoffice.dominio.Poliza;
import es.correduria.backoffice.dominio.Recibo;
import es.correduria.backoffice.repositorio.ClienteRepository;
import es.correduria.backoffice.repositorio.PolizaRepository;
import es.correduria.backoffice.repositorio.ReciboRepository;
import es.correduria.backoffice.repositorio.SiniestroRepository;
import es.correduria.backoffice.servicio.PanelAutomatizacionesService;
import es.correduria.backoffice.servicio.RevisionService;
import es.correduria.backoffice.servicio.SiniestroService;
import es.correduria.backoffice.web.dto.DashboardDto;
import es.correduria.backoffice.web.dto.PolizaDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ClienteRepository clientes;
    private final PolizaRepository polizas;
    private final ReciboRepository recibos;
    private final SiniestroRepository siniestros;
    private final SiniestroService siniestroService;
    private final RevisionService revisionService;
    private final PanelAutomatizacionesService panelService;

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        LocalDate hoy = LocalDate.now();
        var porRamo = polizas.carteraPorRamo(Poliza.Estado.VIGENTE).stream()
                .map(f -> new DashboardDto.RamoCartera((Poliza.Ramo) f[0], (Long) f[1], (BigDecimal) f[2]))
                .sorted(Comparator.comparing(DashboardDto.RamoCartera::polizas).reversed())
                .toList();
        var vencimientos = polizas.findTop8ByEstadoAndFechaVencimientoBetweenOrderByFechaVencimientoAsc(
                        Poliza.Estado.VIGENTE, hoy, hoy.plusDays(30)).stream()
                .map(PolizaDtos.Resumen::de).toList();
        var revisionesPendientes = revisionService.pendientes();

        return new DashboardDto(
                clientes.count(),
                polizas.countByEstado(Poliza.Estado.VIGENTE),
                polizas.sumaPrimas(Poliza.Estado.VIGENTE),
                siniestros.countByEstadoIn(SiniestroService.ACTIVOS),
                recibos.countByEstado(Recibo.Estado.DEVUELTO),
                recibos.sumaImportes(Recibo.Estado.DEVUELTO),
                revisionesPendientes.size(),
                polizas.countByEstadoAndFechaVencimientoBetween(Poliza.Estado.VIGENTE, hoy, hoy.plusDays(30)),
                porRamo,
                vencimientos,
                siniestroService.tablero().stream().limit(6).toList(),
                revisionesPendientes.stream().limit(5).toList(),
                panelService.panel(14));
    }
}
