package es.correduria.backoffice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Correduría de Seguros · Backoffice API")
                        .version("0.1.0")
                        .description("""
                                API del backoffice de una correduría ficticia.
                                - /api/** la usa el frontend.
                                - /api/integracion/** la usan las automatizaciones (n8n) con la cabecera X-API-Key."""))
                .components(new Components().addSecuritySchemes("apiKey",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name(ApiKeyFilter.CABECERA)));
    }
}
