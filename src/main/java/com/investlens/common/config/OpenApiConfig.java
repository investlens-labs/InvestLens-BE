package com.investlens.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI investLensOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("InvestLens API")
                        .version("v1")
                        .description("뉴스 기반 투자 영향 가능성 분석 API입니다. 투자 조언이나 주가 예측을 제공하지 않습니다."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
