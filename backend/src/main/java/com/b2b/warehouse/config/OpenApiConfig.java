package com.b2b.warehouse.config;

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

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI b2bWarehouseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("B2B Wholesale & Free Sample Management API")
                        .version("1.0.0")
                        .description("High-Traffic B2B E-Commerce, Multi-Branch Inventory, Store Locator (PostGIS) & Vietnam Payment/Logistics Gateway API")
                        .contact(new Contact()
                                .name("B2B Platform Architecture Team")
                                .email("support@b2b-warehouse.local")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide the short-lived JWT Access Token (15m TTL)")));
    }
}
