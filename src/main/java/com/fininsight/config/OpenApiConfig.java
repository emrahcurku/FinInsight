package com.fininsight.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI 3.0 / Swagger configuration for FinInsight REST API.
 * Accessible at /swagger-ui/index.html and /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI finInsightOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinInsight REST API")
                        .description("Production-grade AI-powered personal finance management API with multi-dimensional analytics, budget threshold tracking, distributed caching, and event-driven architecture.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FinInsight Engineering")
                                .url("https://github.com/FinInsight/FinInsight"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("/").description("Current Environment Server")
                ))
                .tags(List.of(
                        new Tag().name("Authentication").description("User registration, authentication, JWT token refresh, and profile endpoints"),
                        new Tag().name("Transactions").description("Financial income and expense transaction management with dynamic filtering"),
                        new Tag().name("Categories").description("System default and custom category management"),
                        new Tag().name("Budgets").description("Monthly budget thresholds, pacing tracking, and limit enforcement"),
                        new Tag().name("Dashboard").description("Composite overview aggregating KPIs, monthly cash flow trends, and budget health"),
                        new Tag().name("Analytics").description("Multi-dimensional financial reporting and category distribution analytics"),
                        new Tag().name("AI Insights").description("Contextual AI-powered behavioral synthesis, habit optimization, and legal disclaimer")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide your JWT access token. Format: `Bearer <token>`")));
    }
}
