package com.mega.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan          // 扫描并注册所有 @ConfigurationProperties record（如 LabProperties）
public class ModernBootLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModernBootLabApplication.class, args);
    }
}
