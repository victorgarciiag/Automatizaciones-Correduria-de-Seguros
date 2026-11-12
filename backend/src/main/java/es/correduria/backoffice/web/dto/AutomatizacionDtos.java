package es.correduria.backoffice.web.dto;

import es.correduria.backoffice.dominio.Automatizacion;
import es.correduria.backoffice.dominio.Ejecucion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AutomatizacionDtos {

    private AutomatizacionDtos() {
    }

    public record EjecucionResumen(Long id, String automatizacion, LocalDateTime inicio, int duracionMs,
                                   Ejecucion.Resultado resultado, int tokensEntrada, int tokensSalida,
                                   BigDecimal costeEur, BigDecimal minutosAhorrados, String referencia,
                                   String detalle, String n8nExecutionId, boolean demo) {

        public static EjecucionResumen de(Ejecucion e) {
            return new EjecucionResumen(e.getId(), e.getAutomatizacion(), e.getInicio(), e.getDuracionMs(),
                    e.getResultado(), e.getTokensEntrada(), e.getTokensSalida(), e.getCosteEur(),
                    e.getMinutosAhorrados(), e.getReferencia(), e.getDetalle(), e.getN8nExecutionId(), e.isDemo());
        }
    }

    /**
     * Registro de una ejecución desde n8n. Si no se indican minutosAhorrados, se usan los minutos
     * manuales del catálogo (solo cuando el resultado es OK).
     */
    public record AltaEjecucion(@NotBlank String automatizacion, LocalDateTime inicio, @Min(0) int duracionMs,
                                @NotNull Ejecucion.Resultado resultado, @Min(0) int tokensEntrada,
                                @Min(0) int tokensSalida, BigDecimal costeEur, BigDecimal minutosAhorrados,
                                String referencia, String detalle, String n8nExecutionId) {
    }

    public record Estadisticas(String codigo, String nombre, String descripcion, String area,
                               Automatizacion.Estado estado, int minutosManuales, int orden,
                               long ejecuciones, long ok, long revision, long error, double tasaExito,
                               BigDecimal coste, double horasAhorradas, double costeMedio,
                               LocalDateTime ultimaEjecucion) {
    }

    public record Dia(LocalDate fecha, long ok, long revision, long error, double horasAhorradas) {
    }

    public record Panel(int dias, long ejecuciones, double tasaExito, BigDecimal coste, double horasAhorradas,
                        double ahorroEur, long revisionesPendientes, boolean hayDatosDemo,
                        List<Dia> porDia, List<Estadisticas> automatizaciones) {
    }
}
