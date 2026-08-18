package com.d0w0b.phytotrack.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 設定
 *
 * 產生 /v3/api-docs 與 Swagger UI，作為 API 規格的單一來源（Single Source of Truth）：
 *   - 人看：Swagger UI（/swagger-ui.html）
 *   - 程式看：前端以 openapi-typescript 由此規格產生 TypeScript 型別
 *
 * 此處定義 Bearer JWT 安全機制，讓 Swagger UI 可帶 token 試打受保護的 API。
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "PhytoTrack API", version = "1.0.0",
        description = "農作物病蟲害診斷諮詢服務系統 REST API"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {
}
