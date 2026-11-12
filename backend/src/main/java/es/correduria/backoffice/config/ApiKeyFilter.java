package es.correduria.backoffice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege los endpoints que usan las automatizaciones (/api/integracion/**) con una API key.
 * Principio de mínimo privilegio: n8n solo puede usar estos endpoints y nada más.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-API-Key";

    private final byte[] apiKey;

    public ApiKeyFilter(@Value("${app.integracion.api-key}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // getServletPath() viene decodificado y normalizado (getRequestURI no): así "/api/%69ntegracion/..."
        // no se salta el filtro y luego llega igualmente al controlador.
        return !request.getServletPath().startsWith("/api/integracion/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(CABECERA);
        boolean valida = recibida != null
                && MessageDigest.isEqual(apiKey, recibida.getBytes(StandardCharsets.UTF_8));
        if (!valida) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"API key ausente o no válida (cabecera X-API-Key)\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
