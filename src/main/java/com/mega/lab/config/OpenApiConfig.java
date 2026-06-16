package com.mega.lab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc 的全局元信息。加好依赖后即可访问：
 *   - Swagger UI： http://localhost:8080/swagger-ui.html
 *   - OpenAPI JSON：http://localhost:8080/v3/api-docs
 *
 * 你不用再像 springfox 那样写一堆 Docket 配置，springdoc 自动扫描所有 @RestController。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI labOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Modern Boot Lab API")
                .version("v1")
                .description("2026 Spring Boot 教学项目：JPA 实践"));
    }
}
