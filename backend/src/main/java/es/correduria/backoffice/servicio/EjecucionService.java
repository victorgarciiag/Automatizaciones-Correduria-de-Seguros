package es.correduria.backoffice.servicio;

import es.correduria.backoffice.dominio.Ejecucion;
import es.correduria.backoffice.repositorio.AutomatizacionRepository;
import es.correduria.backoffice.repositorio.EjecucionRepository;
import es.correduria.backoffice.web.dto.AutomatizacionDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EjecucionService {

    private final EjecucionRepository ejecuciones;
    private final AutomatizacionRepository automatizaciones;

    public AutomatizacionDtos.EjecucionResumen registrar(AutomatizacionDtos.AltaEjecucion alta) {
        var automatizacion = automatizaciones.findById(alta.automatizacion())
                .orElseThrow(() -> new IllegalArgumentException("Automatización desconocida: " + alta.automatizacion()));

        // Si n8n no indica el ahorro, se estima con los minutos manuales del catálogo (solo si terminó bien).
        BigDecimal minutos = alta.minutosAhorrados() != null ? alta.minutosAhorrados()
                : alta.resultado() == Ejecucion.Resultado.OK
                ? BigDecimal.valueOf(automatizacion.getMinutosManuales())
                : BigDecimal.ZERO;

        var e = new Ejecucion();
        e.setAutomatizacion(alta.automatizacion());
        e.setInicio(alta.inicio() != null ? alta.inicio() : LocalDateTime.now());
        e.setDuracionMs(alta.duracionMs());
        e.setResultado(alta.resultado());
        e.setTokensEntrada(alta.tokensEntrada());
        e.setTokensSalida(alta.tokensSalida());
        e.setCosteEur(alta.costeEur() != null ? alta.costeEur() : BigDecimal.ZERO);
        e.setMinutosAhorrados(minutos);
        e.setReferencia(alta.referencia());
        e.setDetalle(alta.detalle());
        e.setN8nExecutionId(alta.n8nExecutionId());
        return AutomatizacionDtos.EjecucionResumen.de(ejecuciones.save(e));
    }
}
