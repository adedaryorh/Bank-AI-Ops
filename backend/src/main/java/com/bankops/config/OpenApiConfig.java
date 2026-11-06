package com.bankops.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bankOpsOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("BankOps AI API")
                .version("v1")
                .description("""
                    Decision-support platform for bank employees. Synthetic data only; this system never
                    approves or rejects customers and is not a production or legally compliant system.
                    """)
                .license(new License().name("Portfolio project — no warranty")))
            .components(new Components().addSecuritySchemes("bearer-jwt",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Keycloak access token (audience bankops-api)")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
