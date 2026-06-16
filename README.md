# modern-boot-lab

一个为你量身定制的 **2026 Spring Boot 教学项目**。
它假设你已经熟练：多模块 Maven、构造器注入、`Result` 统一响应、PO/VO 分层、MyBatis、`@Validated`、Docker 打包。
所以本项目**只教你栈里缺的那一代东西**（你现在停在 Boot 2.5 / Java 8 / `javax` / fastjson / springfox）。

完整的「为什么这么写」在 **[LESSONS.md](./LESSONS.md)**，学习进度与续学指引在 **[学习进度与计划.md](./学习进度与计划.md)**，建议配合源码一起读。

## 技术栈

| 维度 | 本项目（2026） | 你现在 |
|---|---|---|
| 构建 | Gradle Kotlin DSL + Wrapper + toolchain | Maven |
| 语言 | Java 21（record / 虚拟线程 / pattern matching） | Java 8 |
| 框架 | Spring Boot 3.5.14 / Spring 6（`jakarta.*`） | Boot 2.5 / Spring 5（`javax.*`） |
| API 文档 | springdoc-openapi（OpenAPI 3） | springfox（Swagger 2，已死） |
| JSON | Jackson | fastjson 1.x（有 RCE 史） |
| 错误响应 | RFC 9457 `ProblemDetail` | 自定义 `Results.fail()` |
| ORM | Spring Data JPA（曾并排对比 MyBatis-Plus 后选定，见 LESSONS 第 10 节） | tk.mybatis |
| 数据库迁移 | Flyway（版本化、可追溯、幂等） | 手改 SQL / `ddl-auto` |
| 并发 | 虚拟线程（一行开启） | 传统线程池 |
| 集成测试 | Testcontainers（真 Postgres，用完即焚） | （多为 H2 / 手测） |
| 可观测性 | Actuator + Micrometer（可接 OTel） | 仅 Sentry |
| CI/CD | GitHub Actions：构建→测试→打镜像→推 GHCR（不可变 `:sha` 标签） | Jenkins 网页点配置 |

## 怎么跑

```bash
# 不需要本机装 Gradle，用 wrapper（版本锁在仓库里）
./gradlew bootRun
```

默认用 **H2 内存库**，开箱即跑、零外部依赖（**可复现原则**：提交版默认就能在任何干净环境起来）。
想连本地真 Postgres：设环境变量 `SPRING_PROFILES_ACTIVE=postgres`（见 `application-postgres.yml`），不写进 git。

起来后：

| 地址 | 作用 |
|---|---|
| http://localhost:8080/ | 首页：验证配置绑定 + 虚拟线程（`isVirtualThread: true`） |
| http://localhost:8080/swagger-ui.html | Swagger UI（springdoc 自动生成） |
| http://localhost:8080/v3/api-docs | OpenAPI 3 JSON |
| http://localhost:8080/h2-console | H2 可视化（jdbc:h2:mem:lab，用户 sa 空密码） |
| http://localhost:8080/actuator/health | 健康检查 |

### 试接口

```bash
# 创建（注意返回 201 + Location 头）
curl -i -X POST localhost:8080/api/jpa/widgets \
  -H 'Content-Type: application/json' \
  -d '{"name":"红宝石","sku":"RUBY-001","quantity":10}'

# 列表 / 各种查询
curl localhost:8080/api/jpa/widgets
curl localhost:8080/api/jpa/widgets/latest                 # 派生查询
curl 'localhost:8080/api/jpa/widgets/in-stock?min=1'       # @Query (JPQL)
curl 'localhost:8080/api/jpa/widgets/search?minQty=1'      # Specification 动态查询
curl localhost:8080/api/jpa/widgets/summaries              # 接口投影

# 校验失败：看 RFC 9457 的 application/problem+json
curl -X POST localhost:8080/api/jpa/widgets \
  -H 'Content-Type: application/json' \
  -d '{"name":"","sku":"bad sku","quantity":-3}'
```

## 代码地图

```
src/main/java/com/mega/lab/
├─ ModernBootLabApplication.java   主类（@ConfigurationPropertiesScan）
├─ HomeController.java             配置绑定 + 虚拟线程自检
├─ common/                        异常 + RFC 9457 全局处理
│  ├─ GlobalExceptionHandler.java   ← ProblemDetail 收口（替代 Results.fail）
│  ├─ ResourceNotFoundException.java
│  └─ DuplicateResourceException.java
├─ config/
│  ├─ OpenApiConfig.java           springdoc 元信息（替代 springfox Docket）
│  └─ LabProperties.java           @ConfigurationProperties 用 record
└─ widget/                        业务实现（JPA）
   ├─ Widget.java / WidgetStatus.java / WidgetRepository.java
   ├─ WidgetService.java / WidgetController.java
   └─ dto/ CreateWidgetRequest.java(record+校验) / WidgetResponse.java / WidgetSummary.java

src/main/resources/
├─ application.yml                 默认 H2；Flyway / Actuator / 虚拟线程配置
├─ application-postgres.yml        postgres profile（SPRING_PROFILES_ACTIVE=postgres 启用）
└─ db/migration/                   Flyway 版本化迁移脚本
   ├─ V1__init.sql                 建表
   └─ V2__add_status_index.sql     加索引

src/test/java/.../
├─ ModernBootLabApplicationTests.java   上下文冒烟测试（H2，随 build 跑）
└─ widget/WidgetIntegrationTest.java    Testcontainers 真 Postgres 集成测试（@Tag("integration")）

.github/workflows/ci.yml           CI 流水线（见下）
Dockerfile / .dockerignore         多阶段构建（JDK 编译 → JRE 运行）
```

## CI/CD

每次 push 到 `main` / 提 PR，GitHub Actions 自动跑 [`.github/workflows/ci.yml`](./.github/workflows/ci.yml)：

```
push → [build] 装JDK21(缓存) → ./gradlew build(编译+单测) → 上传测试报告
          └─(needs 门禁)─ [docker-image] 登录GHCR → 打标签 → 多阶段构建镜像 → 推送
                                                                       ↓
                              ghcr.io/daleiguo666-sys/modern-boot-lab:sha-xxxx (不可变)
                                                                      :latest    (指向 main 最新)
```

要点：**Pipeline as Code**（流水线进 git）、依赖缓存、`needs` 任务门禁、不可变镜像（`:sha` 绑定提交，可精确回滚）、`GITHUB_TOKEN` 临时令牌登录（不硬编码密码）。

本地构建 / 运行镜像：

```bash
docker build -t modern-boot-lab:dev .
docker run --rm -p 8080:8080 modern-boot-lab:dev    # 默认 H2，独立可跑
curl localhost:8080/actuator/health
```

> 国内环境一次性配置（已固化在本机 colima）：DNS 用 `colima start --dns 223.5.5.5 --dns 8.8.8.8`；镜像加速器在 `~/.colima/default/colima.yaml` 配 `registry-mirrors`（daocloud + 1ms）。

## 还没做、留作下一步

- [ ] **微服务那套**（下一阶段重点）：服务拆分、服务间调用、配置中心、消息队列、分布式缓存
- [ ] 演进成多模块（**有真实理由时**才拆，见 LESSONS 第 13 节）
- [ ] 跑通 3.5 → Boot 4.0 迁移（见 LESSONS 第 14 节）
- [ ] CI 里用 `services: postgres` 把 `@Tag("integration")` 集成测试也跑进流水线
- [ ] 流水线加安全扫描（依赖漏洞 SCA + 镜像扫描 Trivy）
```
