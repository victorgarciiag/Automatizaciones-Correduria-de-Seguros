package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Poliza;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(long clientes, long polizasVigentes, BigDecimal primasCartera,
                           long siniestrosAbiertos, long recibosDevueltos, BigDecimal importeDevuelto,
                           long revisionesPendientes, long vencimientos30d,
                           List<RamoCartera> carteraPorRamo, List<PolizaDtos.Resumen> proximosVencimientos,
                           List<SiniestroDtos.Resumen> ultimosSiniestros, List<RevisionDtos.Resumen> revisiones,
                           AutomatizacionDtos.Panel automatizaciones) {

    public record RamoCartera(Poliza.Ramo ramo, long polizas, BigDecimal primas) {
    }
}
