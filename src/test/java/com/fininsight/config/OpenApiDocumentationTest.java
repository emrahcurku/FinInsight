package com.fininsight.config;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@DisplayName("OpenAPI / Swagger Configuration & Metadata Tests")
class OpenApiDocumentationTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    @DisplayName("OpenAPI bean initializes with valid API metadata and versioning")
    void openApiMetadataShouldBeValid() {
        OpenAPI openAPI = openApiConfig.finInsightOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("FinInsight REST API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getDescription()).contains("AI-powered personal finance");
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("Apache 2.0");
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("FinInsight Engineering");
    }

    @Test
    @DisplayName("OpenAPI config registers JWT Bearer authentication security scheme")
    void openApiShouldContainJwtBearerSecurityScheme() {
        OpenAPI openAPI = openApiConfig.finInsightOpenAPI();

        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME);

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.SECURITY_SCHEME_NAME);
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualToIgnoringCase("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("OpenAPI config registers all core domain tags")
    void openApiShouldContainAllCoreDomainTags() {
        OpenAPI openAPI = openApiConfig.finInsightOpenAPI();

        assertThat(openAPI.getTags()).isNotNull();
        List<String> tagNames = openAPI.getTags().stream().map(Tag::getName).toList();

        assertThat(tagNames).containsExactlyInAnyOrder(
                "Authentication",
                "Transactions",
                "Categories",
                "Budgets",
                "Dashboard",
                "Analytics",
                "AI Insights"
        );
    }
}
