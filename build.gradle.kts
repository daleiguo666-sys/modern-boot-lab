plugins {
    java
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.mega.lab"
version = "0.0.1-SNAPSHOT"

java {
    // 用 toolchain 锁定 JDK 版本：谁来构建都用 Java 21，跟本机默认 JDK 无关。
    // 这是 2026 的标准做法 —— 你老项目里 <java.version>1.8</java.version> 只是“编译目标”，
    // toolchain 连“用哪个 JDK 编译”都钉死了，团队/CI 不再因 JDK 不一致翻车。
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // springdoc：2026 的 OpenAPI/Swagger 标准方案。
    // 你老项目的 springfox（io.springfox:springfox-boot-starter）早已停更、不支持 Boot 3，弃用。
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    // Flyway：版本化数据库迁移（替代 ddl-auto，生产标准做法）。
    // Flyway 10+ 把各数据库支持拆成独立模块——连 Postgres 也要单独引入 flyway-database-postgresql，
    // 否则启动报 "Unsupported Database: PostgreSQL"。版本由 Spring Boot 依赖管理统一托管，不用自己写。
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Testcontainers：2026 集成测试标准 —— 真起一个 Postgres 容器跑测试，而非 H2 假装。
    // 版本由 Spring Boot 的依赖管理统一托管，不用自己写。
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

// 默认 test 任务只跑单元测试，排除 @Tag("integration")（需要 Docker 的那些）。
// 好处：本地/CI 没 Docker 也能 ./gradlew build；集成测试单独按需跑。
tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

// 专门跑集成测试（需本机有 Docker）： ./gradlew integrationTest
tasks.register<Test>("integrationTest") {
    description = "运行 @Tag(\"integration\") 的测试（Testcontainers，需要 Docker）"
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    // ---- colima 适配 ----
    // Testcontainers 默认假设 docker socket 在 /var/run/docker.sock；colima 的 socket 在别处，
    // 这条配在 ~/.testcontainers.properties 的 docker.host（含本机用户路径，不写进构建文件）。
    // 下面两条是通用的（不含本机路径），固化在这里：
    //  1. ryuk（清理容器）要把宿主 socket 挂进容器，colima 的 vz/virtiofs 不支持 → 直接禁用 ryuk。
    //     代价：测试容器靠 colima 生命周期回收，不靠 ryuk 即时清。学习场景完全够。
    //  2. 万一别处仍需挂 socket，告诉 Testcontainers 用 VM 内的标准路径，而非宿主 colima 路径。
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
    shouldRunAfter(tasks.test)
}
