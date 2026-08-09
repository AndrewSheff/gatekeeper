package com.ashevtsov.gatekeeper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI / Swagger UI.
 * Bearer JWT авторизация, группировка по тегам, красивый заголовок.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatekeeperOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GateKeeper API")
                        .description("OAuth2/OIDC Authorization Server + API Gateway с rate limiting и аналитикой")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Andrey Shevtsov")
                                .email("andrey@ashevtsov.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT-токен, полученный через OAuth2 Authorization Server"));
    }
}
