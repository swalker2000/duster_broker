package com.duster.security

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val bearerSchemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Duster Broker API")
                    .description(
                        """
                        JWT-авторизация: сначала `POST /auth/login` (`deviseId` + `password`),
                        затем Authorize → вставьте `accessToken` (без префикса Bearer).
                        """.trimIndent()
                    )
                    .version("0.0.1")
            )
            .components(
                Components().addSecuritySchemes(
                    bearerSchemeName,
                    SecurityScheme()
                        .name(bearerSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT из ответа /auth/login (поле accessToken)")
                )
            )
            .addSecurityItem(SecurityRequirement().addList(bearerSchemeName))
    }
}
