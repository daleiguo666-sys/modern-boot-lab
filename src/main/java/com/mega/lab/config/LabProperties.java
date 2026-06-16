package com.mega.lab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 类型安全的配置绑定 —— 用 record 写 @ConfigurationProperties，这是 2026 的标准姿势。
 *
 * 对比你过去的写法：
 *  - 不再到处 @Value("${...}") 散落字段，集中成一个不可变配置对象。
 *  - record 天然不可变，配置一旦绑定就不会被改，线程安全。
 *  - 配合 spring-boot-configuration-processor（已在依赖里），IDE 里写 application.yml 有自动补全。
 *
 * 启用方式：在主类或某个 @Configuration 上加 @ConfigurationPropertiesScan（本项目已在主类加）。
 *
 * 对应 application.yml：
 *   lab:
 *     welcome-message: "..."
 *     cache-ttl: 30s
 */
@ConfigurationProperties(prefix = "lab")
public record LabProperties(
        String welcomeMessage,
        Duration cacheTtl              // Boot 自动把 "30s" / "5m" 解析成 Duration，不用自己 parse
) {
}
