package joomidang.papersummary.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        Info info = new Info()
                .title("Paper Summary API")
                .version("1.0.0")
                .description("API documentation for the Paper Summary application");

        // ApiResponse 객체의 기본 스키마 정의
        Schema apiResponseSchema = new Schema<>()
                .type("object")
                .addProperty("code", new Schema<String>().type("string").description("응답 코드"))
                .addProperty("message", new Schema<String>().type("string").description("응답 메시지"))
                .addProperty("data", new Schema<>().type("object").description("응답 데이터 (선택적)"));

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization"))
                        .addSchemas("ApiResponse", apiResponseSchema))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .info(info);
    }
}