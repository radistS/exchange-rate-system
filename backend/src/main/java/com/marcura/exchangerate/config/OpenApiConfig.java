package com.marcura.exchangerate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** SpringDoc OpenAPI metadata for Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI exchangeRateOpenApi(
            @Value("${server.port:8080}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("Exchange Rate Management API")
                        .description(
                                "Spread-adjusted exchange rates, usage analytics, historical series, "
                                        + "and AI trend insights (Marcura assessment).")
                        .version("1.0.0")
                        .contact(new Contact().name("Marcura Exchange Rate System"))
                        .license(new License().name("Assessment submission")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Docker Compose (backend service)")));
    }
}
