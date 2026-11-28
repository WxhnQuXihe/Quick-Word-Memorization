package com.shorthand.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shorthandOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shorthand API")
                        .description("神也背单词 / Shorthand 后端接口")
                        .version("0.5.5"));
    }
}
