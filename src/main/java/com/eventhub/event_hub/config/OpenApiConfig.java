package com.eventhub.event_hub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("EventHub API")
                .version("0.0.5")
                .description("REST API for managing events and categories.")
                .contact(new Contact()
                        .name("Matheus Barbosa")
                        .email("matheusbarbosasoares7@gmail.com"))
        );
    }
}
