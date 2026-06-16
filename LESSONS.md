# LESSONS —— 从你的 2021 栈到 2026 标准

读法：每节先说**你现在怎么做**，再说**2026 标准怎么做**和**为什么**，最后指到本项目的**具体文件**。
凡是你已经熟练的（构造器注入、MyBatis 基础、多模块拆分机制、Docker 基础、`@Validated`）一律略过。

目录：
1. 构建：Maven → Gradle Kotlin DSL + Wrapper + toolchain
2. Java 8 → 21：record / 虚拟线程 / pattern matching / 文本块
3. `javax.*` → `jakarta.*`（升级 Boot 3 的头号破坏性变更）
4. springfox → springdoc
5. fastjson → Jackson（安全）
6. 错误响应：`Results.fail()` → RFC 9457 `ProblemDetail`
7. 虚拟线程：你 IO 密集后台的最大红利
8. 配置绑定：`@Value` 散落 → `@ConfigurationProperties` record
9. 关掉 OSIV（open-in-view）
10. JPA vs MyBatis-Plus（你点名要对比）
11. Testcontainers 取代 H2 做集成测试
12. 可观测性：只有 Sentry → Actuator + Micrometer + OTel
13. 打包 & DB 迁移：手写 Dockerfile → Buildpacks；`ddl-auto` → Flyway
14. 何时单模块、何时多模块（回答你的疑问）
15. 收尾：Boot 3.5 → 4.0 要改什么

---

## 1. 构建：Maven → Gradle Kotlin DSL + Wrapper + toolchain

你全是 Maven。2026 新项目里 Gradle（尤其 **Kotlin DSL**）已是主流。关键差异不在语法，而在三个习惯：

- **Wrapper（`./gradlew`）**：Gradle 版本锁进仓库（`gradle/wrapper/gradle-wrapper.properties`），任何人 clone 下来用的都是同一个 Gradle，不依赖本机装没装。Maven 也有 wrapper（`./mvnw`），但很多老项目没用——养成「永远提交 wrapper、永远用 wrapper 跑」的习惯。
- **toolchain**：见 `build.gradle.kts` 的
  ```kotlin
  java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
  ```
  这比你 `<java.version>1.8</java.version>` 更强：它连「用哪个 JDK 来编译」都钉死了。本机默认是 JDK 17 也没关系，Gradle 会自动找/下载 JDK 21。CI 和本地从此不会因 JDK 不一致翻车。
- **依赖写法**：`implementation` / `runtimeOnly` / `compileOnly` / `testImplementation` 取代 Maven 的 `<scope>`。`implementation` 比 Maven 默认的 `compile` 更好——它不会把传递依赖泄露给下游模块，编译更快、耦合更低。

→ 看 `build.gradle.kts`、`gradle/wrapper/gradle-wrapper.properties`

> 注：Gradle 和 Maven 都仍是 2026 的「标准」，没有谁淘汰谁。你要是更想留在 Maven 生态，这些理念（wrapper、toolchain、BOM）一样适用。

---

## 2. Java 8 → 21：真正该换肌肉记忆的语法

你写 Java 8。下面这些是 17/21 里**日常会用到**的，按价值排序：

- **record**（最高频）：不可变数据载体。本项目所有 DTO 都是 record，替代你的 `@Data class XxxPO/VO`。
  → `widget/dto/CreateWidgetRequest.java`、`WidgetResponse.java`、`config/LabProperties.java`
  注意边界：**JPA 实体不能用 record**（需要无参构造 + 可变字段做代理），所以 `widget/Widget.java` 仍是类 + Lombok。

- **虚拟线程**：见第 7 节，单独讲。

- **switch 表达式 + 模式匹配**（替代一长串 if-else / 老 switch）：
  ```java
  String label = switch (status) {
      case ACTIVE -> "在售";
      case DISCONTINUED -> "停产";
  };
  // instanceof 模式匹配，不用再强转：
  if (obj instanceof Widget w) { use(w.getName()); }
  ```

- **文本块**（多行字符串，写 SQL/JSON 很爽）：
  ```java
  String sql = """
      select id, name from widget
      where status = 'ACTIVE'
      """;
  ```

- **`String.formatted()` / `var`**：本项目 Service 里用了 `"widget %d 不存在".formatted(id)`，比 `String.format` 顺手。局部变量 `var` 适度用。

- **sealed 类/接口**：限定继承范围，配合 switch 模式匹配做「穷尽」检查。适合做状态机、结果类型。你现在用不到可以先放。

> 关于 Stream：你那条「**一律 for 循环、不用 Stream**」的约定**只适用于 werewolf-server**，不是全局规范。所以本教学项目正常使用 Stream（见 `WidgetService.list()` 里的 `Stream.toList()`）。`Stream.toList()`（Java 16+）替代了 `collect(Collectors.toList())`，更短且返回不可变 List。

---

## 3. `javax.*` → `jakarta.*`

这是从 Boot 2.x 升 3.x **报错最多**的一处，必须知道。Oracle 把 Java EE 捐给 Eclipse 基金会后改名 Jakarta EE，包名从 `javax.*` 全量改成 `jakarta.*`。

本项目里你能看到的对应：
- `jakarta.persistence.*`（JPA 注解）—— 你过去是 `javax.persistence.*`
- `jakarta.validation.*`（校验注解）—— 过去 `javax.validation.*`
- `jakarta.servlet.*` —— 过去 `javax.servlet.*`

升级老项目时的实操：全局把 `javax.persistence/validation/servlet/annotation` 替换成 `jakarta.` 即可。**例外**：`javax.sql.DataSource`、`javax.crypto` 等仍在 JDK 里，不要动。

→ 看 `widget/Widget.java`（`jakarta.persistence`）、`CreateWidgetRequest.java`（`jakarta.validation`）

---

## 4. springfox → springdoc

你用 `io.springfox:springfox-boot-starter`（Swagger 2）。**springfox 已停止维护、不支持 Boot 3**，必须换 **springdoc-openapi**（OpenAPI 3）。

差异：
- 注解换包：`@Api`→`@Tag`，`@ApiOperation`→`@Operation`，`@ApiModelProperty`→`@Schema`（来自 `io.swagger.v3.oas.annotations`）。
- 不用再写一堆 `Docket` Bean，springdoc 自动扫所有 `@RestController`。全局信息用一个 `OpenAPI` Bean 即可。
- UI 地址：`/swagger-ui.html`，JSON：`/v3/api-docs`。

→ 看 `config/OpenApiConfig.java`、各 Controller 上的 `@Tag/@Operation`、`build.gradle.kts` 的 `springdoc-openapi-starter-webmvc-ui`

---

## 5. fastjson → Jackson

你 pom 里是 `fastjson 1.2.83`。**fastjson 1.x 有一长串 RCE（远程代码执行）漏洞史**，2026 的强烈共识是别再用在新项目里。Spring Boot 默认就是 **Jackson**，本项目全程没引 fastjson，序列化/反序列化都走 Jackson。

如果你某些场景离不开 fastjson 的 API，至少迁到 **fastjson2**（重写过，安全性好很多），但默认请用 Jackson。
常用对应：`JSON.toJSONString(o)` → `objectMapper.writeValueAsString(o)`；`JSON.parseObject(s, T.class)` → `objectMapper.readValue(s, T.class)`。

→ 全项目（无 fastjson 依赖）。Boot 自动配置好的 `ObjectMapper` 直接注入即可用。

---

## 6. 错误响应：`Results.fail()` → RFC 9457 `ProblemDetail`

你现在错误也走自定义的 `Result.fail(code, msg)`。2026 标准是 **RFC 9457（原 7807）`ProblemDetail`**，Spring 6 内置。

好处：
- 响应 `Content-Type` 是标准的 `application/problem+json`，固定字段 `type/title/status/detail/instance`，可加扩展字段。
- 不用每个公司自己发明 `{code,msg,data}`；网关、前端、APM 都能按统一标准解析。
- 配合 `@RestControllerAdvice` 全局收口：**Service 只管抛业务异常**，不在业务代码里拼错误响应。

本项目实测输出（校验失败）：
```json
{
  "type": "https://errors.mega.lab/validation-error",
  "title": "参数校验失败",
  "status": 400,
  "detail": "请求体存在非法字段",
  "instance": "/api/jpa/widgets",
  "timestamp": "2026-06-01T01:54:34Z",
  "errors": ["sku: sku 必须是大写字母/数字/横杠，长度 3-64", "..."]
}
```

> 成功响应你仍可保留自己的 `Result<T>` 包装（国内很常见），ProblemDetail 只接管「错误」这一侧。两者可以共存。

→ 看 `common/GlobalExceptionHandler.java`、`common/*Exception.java`

---

## 7. 虚拟线程（Project Loom）—— 对你价值最大

Java 21 正式版特性。开启方式就一行（`application.yml`）：
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
开启后，Tomcat 每个 HTTP 请求跑在一根**虚拟线程**上。虚拟线程极轻量（几 KB），遇到 IO 阻塞时由 JVM 自动挂起、释放底层平台线程。

**为什么对你这种游戏/社交后台是红利**：你的接口大量是「查 DB / 调 Redis / 调融云、阿里云 SDK」这类 IO 等待。传统线程池里一根线程阻塞就占着不放，并发被线程数卡死；虚拟线程让你用「一请求一线程」的简单写法，拿到接近异步的吞吐，**几乎不改业务代码**。

本项目首页 `GET /` 实测返回 `"isVirtualThread": true`，线程名 `VirtualThread[#35,tomcat-handler-0]`。

注意坑：
- `synchronized` 长临界区会「pin」住平台线程，抵消好处 → 换成 `ReentrantLock`。
- 不要再自建大线程池去「控制并发」；需要限流用信号量/限流器，而不是线程数。

→ 看 `application.yml` 的 `spring.threads.virtual`、`HomeController.java`

---

## 8. 配置绑定：`@Value` → `@ConfigurationProperties` record

你大概率到处 `@Value("${xxx}")`。2026 标准是把一组相关配置绑成一个**不可变 record**：

```java
@ConfigurationProperties(prefix = "lab")
public record LabProperties(String welcomeMessage, Duration cacheTtl) {}
```
- 集中、类型安全、不可变（线程安全）。
- `Duration cacheTtl` 能直接把 `"30s"`/`"5m"` 解析成 `Duration`，不用自己 parse。
- 配合 `spring-boot-configuration-processor`（已在依赖里），写 `application.yml` 时 IDE 有自动补全。
- 启用：主类加 `@ConfigurationPropertiesScan`（已加）。

→ 看 `config/LabProperties.java`、`ModernBootLabApplication.java`、`application.yml` 的 `lab:` 段

---

## 9. 关掉 OSIV（open-in-view）

Spring Boot 默认 `spring.jpa.open-in-view=true`，会把 Hibernate Session 一直开到视图渲染完。后果：**懒加载可能在 Controller 层偷偷发 SQL**，连接占用时间变长，问题难定位。2026 共识是**显式关掉**：
```yaml
spring:
  jpa:
    open-in-view: false
```
关掉后，懒加载只能在 `@Transactional` 边界内发生，强迫你在 Service 里把要用的数据查全、转成 DTO 再出去——这正是好习惯。本项目已关。

→ `application.yml`

---

## 10. JPA vs MyBatis-Plus（你点名要对比）

> ⚠️ **更新（2026-06-15）**：对比完成后你选定了 JPA，**MP 代码（`mp/` 包、`MyBatisPlusConfig`、`com.baomidou` 依赖）已从项目移除**，`/api/mp/...` 接口下线。本节作为「曾并排对比过」的知识保留，下面指向 `mp/` 的代码指引仅供回顾，仓库里已无对应文件。

（当初）本项目让两者**操作同一张 `widget` 表**、暴露 `/api/jpa/widgets` 与 `/api/mp/widgets` 两套平行接口，方便并排读。

| 维度 | Spring Data JPA | MyBatis-Plus |
|---|---|---|
| 心智模型 | 面向对象/实体，ORM 帮你生成 SQL | 面向 SQL，框架帮你省掉样板 |
| CRUD | 继承 `JpaRepository` 即得 | 继承 `BaseMapper` 即得 |
| 简单条件查询 | 方法名派生 `findBySku` | `LambdaQueryWrapper.eq(MpWidget::getSku, ...)` |
| 复杂/调优 SQL | `@Query`/Specification/Criteria，较绕 | 直接写 SQL（XML 或注解），最自由 |
| 与你现有技能 | 思路差异大，**最值得学** | 从 tk.mybatis 迁移**几乎零成本** |
| 乐观锁 | `@Version`（JPA 原生） | `@Version` + `OptimisticLockerInnerInterceptor` |

实务建议：
- **新业务、领域模型清晰** → JPA 上手快、样板少。
- **重 SQL、复杂报表、要精细调优**（你游戏后台很多）→ MyBatis-Plus 更顺手，且你团队已有 MyBatis 肌肉记忆。
- 同一项目两者可共存，但**别让两者同时写同一张表**（本项目为教学才这么做；生产里一张表归一个所有者）。

从 **tk.mybatis → MyBatis-Plus** 的对应：`@Table`→`@TableName`，`@Id`→`@TableId`，并白送条件构造器、分页插件、`IService` 模板。tk.mybatis 已基本停更，这是该换的主要原因。

→ JPA 侧：`widget/` 整个包（仍在）。MP 侧 `mp/` 包、`config/MyBatisPlusConfig.java` **已移除**。

---

## 11. Testcontainers 取代 H2 做集成测试

集成测试别再用 H2「假装」数据库——H2 的方言和真 Postgres 有差异，H2 过了不代表生产没事。2026 标准用 **Testcontainers** 在测试时真起一个 Postgres 容器：

```java
@Container @ServiceConnection
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
```
`@ServiceConnection`（Boot 3.1+）自动把容器的 jdbc url/账号密码注入 datasource，连 `@DynamicPropertySource` 都不用写了。

→ 看 `src/test/java/.../WidgetIntegrationTest.java`

**已实跑验证（2026-06-15，待办 D 完成）**：`./gradlew integrationTest` 真起一个临时 `postgres:16-alpine` 容器跑测试，Flyway 在容器里建表，测完容器即焚（`docker ps -a` 看不到残留）。

colima 适配的两个坑（已解决，配置已固化）：
1. **Testcontainers 找不到 docker socket** → `~/.testcontainers.properties` 配 `docker.host=unix://$HOME/.colima/default/docker.sock`。
2. **ryuk 清理容器要把宿主 socket 挂进容器，colima 的 vz/virtiofs 不支持**（`operation not supported`）→ 在 `build.gradle.kts` 的 `integrationTest` 任务里设 `TESTCONTAINERS_RYUK_DISABLED=true`（+ `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`）。
3. 镜像走 DaoCloud 预拉：`docker pull docker.m.daocloud.io/library/postgres:16-alpine && docker tag ... postgres:16-alpine`。

---

## 12. 可观测性：只有 Sentry → Actuator + Micrometer + OTel

你现在错误监控靠 Sentry，但缺**指标**和**链路追踪**。2026 的标准三件套：

- **Actuator**：健康检查 + 指标端点。本项目已开 `health/info/metrics/prometheus`。
  → `application.yml` 的 `management:` 段，访问 `/actuator/health`。
- **Micrometer**：指标门面（类似 SLF4J 之于日志）。加 `micrometer-registry-prometheus` 即可在 `/actuator/prometheus` 暴露 Prometheus 抓取格式。
- **链路追踪**：`micrometer-tracing-bridge-otel` + OTLP exporter，把 trace 发到 OpenTelemetry/Jaeger/Tempo。Sentry 仍可留作异常聚合，但 trace/metrics 用这套标准方案。

这一节本项目只搭好了 Actuator 地基（指标/Prometheus 端点已开），Micrometer-tracing 的接入留作练习——加依赖 + 配 OTLP endpoint 即可，不改业务代码。

---

## 13. 打包 & DB 迁移

**打包**：你用 `spotify/dockerfile-maven-plugin` + 手写 Dockerfile。该插件已废弃。2026 两条标准路：
1. **Cloud Native Buildpacks**（最省心，不用写 Dockerfile）：
   ```bash
   ./gradlew bootBuildImage --imageName=mega/modern-boot-lab:latest
   ```
   Boot 自动构建分层、优化过的 OCI 镜像。
2. 自己写 **分层 Dockerfile**（要精细控制时）：用 Boot 的 layered jar（`dependencies / spring-boot-loader / snapshot-dependencies / application` 四层），让依赖层和代码层分开缓存，改代码时只重建最后一层。

**DB 迁移**：标准是 **Flyway**（或 Liquibase）：把每次 schema 变更写成带版本号的 SQL 脚本，可审计、可回溯、团队一致。生产 `ddl-auto=validate`，schema 全交给 Flyway。

→ **已落地并实跑（2026-06-15，待办 B 完成）**：
- 迁移脚本在 `src/main/resources/db/migration/`：`V1__init.sql`（建 widget 表）、`V2__add_status_index.sql`（加索引）。
- `application.yml`：`ddl-auto: validate` + `spring.flyway`，默认连真 Postgres（colima 容器）。
- 依赖：`flyway-core` + `flyway-database-postgresql`（Flyway 10+ 各数据库支持拆成独立模块，Postgres 必须单独引）。
- 连真库验证过的三个行为：**增量**（重启只跑没跑过的）、`flyway_schema_history` **历史表**（记版本/时间/checksum）、**checksum 拦截**（改了已跑的 V1 → 启动直接失败）。
- 铁律：跑过的 `V*.sql` 一个字都不能改，只能往后加 `V3/V4...`。

---

## 14. 何时单模块、何时多模块（回答你的疑问）

你问「多模块是不是大部分项目的选择」。**不是。**

- **绝大多数单体服务就该是单模块。** 你之所以全是多模块，是因为你做的是微服务集群（一个仓库多个可独立部署的服务，外加共享的 common/sdk）——那是多模块的**正当理由**。
- 常见反模式：把一个单体硬拆成 `api / service / dao` 三个模块。那只是「分包」该干的事，拆成模块只会拖慢构建、加重心智，**没有任何复用或独立部署收益**。
- 2026 共识：**默认单模块，包按「功能」组织（package-by-feature，本项目 `widget/` 就是按功能分包），直到出现真实的「跨服务复用」或「独立部署」需求，再拆模块。**

**什么时候真的该拆**（任一成立）：
1. 同一份代码要被**多个可独立部署的服务**复用 → 抽 `xxx-common` / `xxx-sdk`。
2. 一个仓库里要放**多个服务**（gateway、admin、各 api）→ 每个服务一个模块（你 werewolf-cloud 正是这样）。
3. 要给**外部团队**发布一个 jar（client/SDK）→ 单独模块独立版本化。

**怎么拆**（你已会机制，这里只给 Gradle 版结构，作为「从单到多」的下一步）：
```
modern-boot-lab/
├─ settings.gradle.kts        // include("lab-common", "lab-app")
├─ build.gradle.kts           // subprojects 公共配置
├─ lab-common/                // 纯 library：DTO、异常、ProblemDetail 处理器
└─ lab-app/                   // 可启动服务：依赖 lab-common
```
关键点：只有 `lab-app` 应用 `org.springframework.boot` 插件并打可执行 jar；`lab-common` 只是普通 library（用 `java-library` 插件、`api`/`implementation` 区分依赖可见性）。
> 这一步本项目**尚未做**——等你想动手时我们再把现在的单模块按上面结构拆开，你会亲眼看到「为什么拆 / 拆了什么变了」。

---

## 15. 收尾：Boot 3.5 → 4.0 要改什么

2026 年 Spring Boot **4.0 已经是 GA 默认版**（配套 Spring Framework 7）。我们主线用 3.5（生态最稳），但你该知道下一跳要改什么：

- **基线抬到 Java 17+**（建议直接 21）。
- **Jackson 3.x**：包名从 `com.fasterxml.jackson` 变成 `tools.jackson`（2.x → 3.x 是大版本破坏性变更）。这是升 4.0 改动量最大的点之一。
- **JSpecify 空安全注解**：Spring 7 用 `@Nullable`/`@NonNull`（JSpecify 标准），配合 IDE/工具做编译期空检查。
- **内置 API 版本管理**：`@RequestMapping(version = "1.1")` 之类，框架原生支持接口版本路由。
- **弹性能力收编进核心**：`@Retryable`、`@ConcurrentLimit` 等进了 `spring-core`，不必再单独引 spring-retry。
- **HTTP Interface 客户端增强**：声明式 HTTP 客户端（`@HttpExchange` 接口）更成熟，可作为 Feign 的标准替代。
- 第三方生态要确认 Boot 4 兼容版本（springdoc、MyBatis-Plus 的对应 release）。

迁移建议路线：**先升到 3.5 最新并清掉所有 deprecation 警告 → 再跳 4.0**。Spring 官方的 deprecation 一般跨一个大版本才删，3.5 把警告清干净，4.0 会顺很多。

> 这一步本项目也**尚未做**——等你把主线吃透，我们可以新建一个分支把它升到 Boot 4.0，你能直观看到 Jackson 3 包名替换、配置项变化这些真实改动。

---

## 建议的学习顺序

1. `./gradlew bootRun` 跑起来，把 README 里的 curl 都打一遍，对照响应看现象。
2. 读 `widget/`（JPA）整个包吃透一套 ORM（早期曾并排对比过 MyBatis-Plus，现已选定 JPA 并移除 MP，见第 10 节）。
3. 读 `common/GlobalExceptionHandler.java`，理解 ProblemDetail 怎么收口。
4. 改 `application.yml` 把 `spring.threads.virtual.enabled` 关掉重启，对比首页 `isVirtualThread` 变化。
5. 想继续时，挑「多模块拆分」或「升 Boot 4.0」其中一个，我们动手做。
