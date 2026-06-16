# modern-boot-lab

一个为你量身定制的 **2026 Spring Boot 教学项目**。
它假设你已经熟练：多模块 Maven、构造器注入、`Result` 统一响应、PO/VO 分层、MyBatis、`@Validated`、Docker 打包。
所以本项目**只教你栈里缺的那一代东西**（你现在停在 Boot 2.5 / Java 8 / `javax` / fastjson / springfox）。

完整的「为什么这么写」在 **[LESSONS.md](./LESSONS.md)**，建议配合源码一起读。

## 技术栈

| 维度 | 本项目（2026） | 你现在 |
|---|---|---|
| 构建 | Gradle Kotlin DSL + Wrapper + toolchain | Maven |
| 语言 | Java 21（record / 虚拟线程 / pattern matching） | Java 8 |
| 框架 | Spring Boot 3.5.14 / Spring 6（`jakarta.*`） | Boot 2.5 / Spring 5（`javax.*`） |
| API 文档 | springdoc-openapi（OpenAPI 3） | springfox（Swagger 2，已死） |
| JSON | Jackson | fastjson 1.x（有 RCE 史） |
| 错误响应 | RFC 9457 `ProblemDetail` | 自定义 `Results.fail()` |
| ORM | Spring Data JPA **和** MyBatis-Plus（并排对比） | tk.mybatis |
| 并发 | 虚拟线程（一行开启） | 传统线程池 |
| 集成测试 | Testcontainers（真 Postgres） | （多为 H2 / 手测） |
| 可观测性 | Actuator + Micrometer（可接 OTel） | 仅 Sentry |

## 怎么跑

```bash
# 不需要本机装 Gradle，用 wrapper（版本锁在仓库里）
./gradlew bootRun
```

默认用 H2 内存库，开箱即跑。起来后：

| 地址 | 作用 |
|---|---|
| http://localhost:8080/ | 首页：验证配置绑定 + 虚拟线程（`isVirtualThread: true`） |
| http://localhost:8080/swagger-ui.html | Swagger UI（springdoc 自动生成） |
| http://localhost:8080/v3/api-docs | OpenAPI 3 JSON |
| http://localhost:8080/h2-console | H2 可视化（jdbc:h2:mem:lab，用户 sa 空密码） |
| http://localhost:8080/actuator/health | 健康检查 |

### 试接口

```bash
# JPA 版创建（注意返回 201 + Location 头）
curl -i -X POST localhost:8080/api/jpa/widgets \
  -H 'Content-Type: application/json' \
  -d '{"name":"红宝石","sku":"RUBY-001","quantity":10}'

# MyBatis-Plus 版读取同一张表 —— 两套 ORM 操作同一份数据
curl localhost:8080/api/mp/widgets

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
│  ├─ MyBatisPlusConfig.java       MP 拦截器（分页 + 乐观锁）
│  └─ LabProperties.java           @ConfigurationProperties 用 record
├─ widget/                        ★ JPA 实现
│  ├─ Widget.java / WidgetStatus.java / WidgetRepository.java
│  ├─ WidgetService.java / WidgetController.java
│  └─ dto/ CreateWidgetRequest.java(record+校验) / WidgetResponse.java(record)
└─ mp/                            ★ MyBatis-Plus 实现（同一张 widget 表）
   ├─ MpWidget.java / MpWidgetMapper.java
   └─ MpWidgetService.java / MpWidgetController.java

src/test/java/.../WidgetIntegrationTest.java   Testcontainers 真 Postgres 集成测试
```

> `widget/`（JPA）和 `mp/`（MyBatis-Plus）**操作同一张表、暴露平行接口**，并排打开就能对比两套 ORM 的代码量与风格。

## 还没做、留作下一步

- [ ] 演进成多模块（**有真实理由时**才拆，见 LESSONS 第 13 节）
- [ ] 跑通 3.5 → Boot 4.0 迁移（见 LESSONS 第 14 节）
- [ ] 实跑 Testcontainers（需本机装 Docker）
