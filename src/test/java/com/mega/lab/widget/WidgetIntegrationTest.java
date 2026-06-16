package com.mega.lab.widget;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 2026 的标准集成测试：用 Testcontainers 真起一个 Postgres，而不是拿 H2 凑合。
 *
 * 为什么这是“代差”：
 *  - H2 的 SQL 方言和真 Postgres 有差异，H2 测过不代表生产没问题。
 *  - @ServiceConnection（Spring Boot 3.1+）自动把容器的 jdbc url/账号密码注入 datasource，
 *    不用再手写 @DynamicPropertySource —— 这是相对早期 Testcontainers 用法的简化。
 *
 * 运行前提：本机要装 Docker（你当前环境没装，所以这个测试我没在这里跑，只保证编译通过）。
 * 装好 Docker 后： ./gradlew test --tests "*WidgetIntegrationTest"
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
@Tag("integration")          // 默认 ./gradlew test 会跳过；用 ./gradlew integrationTest 单独跑（需 Docker）
class WidgetIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;

    @Test
    void createThenFetch_roundTrips() throws Exception {
        mockMvc.perform(post("/api/jpa/widgets")
                        .contentType("application/json")
                        .content("{\"name\":\"测试件\",\"sku\":\"TEST-001\",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("TEST-001"));

        mockMvc.perform(get("/api/jpa/widgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("TEST-001"));
    }

    @Test
    void validationFailure_returnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/jpa/widgets")
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"sku\":\"bad sku\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://errors.mega.lab/validation-error"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
