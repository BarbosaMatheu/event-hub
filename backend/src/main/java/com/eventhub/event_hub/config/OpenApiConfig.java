package com.eventhub.event_hub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI eventHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventHub API")
                        .version("1.0.0")
                        .description("""
                                Event management and ticketing API.

                                Browsing events and categories is public. Buying tickets requires an account
                                (POST /api/v1/auth/register, then /login and use the token with the Authorize button).
                                Admin endpoints require the ADMIN role.""")
                        .contact(new Contact()
                                .name("Matheus Barbosa")
                                .email("matheusbarbosasoares7@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
