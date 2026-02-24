package com.smartlogix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / Swagger UI configuration for SmartLogix.
 * <p>
 * Configures the OpenAPI 3 specification exposed at {@code /v3/api-docs} and rendered
 * as Swagger UI at {@code /swagger-ui.html}. The spec requires a
 * {@code Authorization: Bearer <token>} header for all protected endpoints, modelled
 * as an HTTP Bearer security scheme named {@code bearerAuth}.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Produces the {@link OpenAPI} bean that describes the SmartLogix REST API.
     * <p>
     * The returned object sets:
     * <ul>
     *   <li>API title, version, and description visible in Swagger UI.</li>
     *   <li>A global security requirement so that the "Authorize" button in Swagger UI
     *       pre-fills the {@code Authorization} header for every request.</li>
     *   <li>An HTTP Bearer JWT security scheme component referenced by the global
     *       security requirement.</li>
     * </ul>
     * </p>
     *
     * @return the fully configured {@link OpenAPI} descriptor
     */
    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("SmartLogix API")
                        .version("1.0.0")
                        .description("Multi-Tenant Order Management Platform API"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
