# AGENTS.md

## 两套独立的构建系统

- **后端（`blog-api/`）**: Gradle 9.4.1（wrapper），Java 21。根项目名 `today-blog`。
- **前端（`blog-frontend/`）**: pnpm 10.33 monorepo，4 个子包：`console/`、`desktop/`、`mobile/`、`common/`。
- 两套系统独立运作，没有统一的编排器。

## 后端框架：TODAY Infrastructure（不是 Spring Boot）

后端使用的是自研框架 **TODAY Infrastructure**（`cn.taketoday:infra-*`，版本 `5.0-Draft.6-SNAPSHOT`）。注解和类名看起来很像 Spring Boot（`@InfraApplication`、`@InfraTest`、`MockMvcTester`、`RestTestClient`、`EntityManager`），但**全部来自 `infra.*` 包**。**严禁导入 `org.springframework.*`。**

框架详细文档（Context7 LLM 可读）: https://context7.com/taketoday/today-infrastructure/llms.txt

### 框架核心概念

| 概念 | 对应 API |
|------|----------|
| 应用入口 | `infra.app.Application.run()` + `@InfraApplication`（组合了 `@Configuration`、`@EnableAutoConfiguration`、`@ComponentScan`） |
| 控制器 | `@RestController`（组合了 `@Controller` + `@ResponseBody`）+ `@GET`/`@POST`/`@PUT`/`@DELETE` |
| 请求映射 | `@RequestMapping`、`@PathVariable`、`@RequestParam`、`@RequestBody` |
| 依赖注入 | `@Autowired`（构造器/字段/方法注入）、`@Qualifier`、`@Configuration` + `@Bean` |
| 事件 | `@EventListener` + `ApplicationEventPublisher`，支持异步（`@Async`）、条件（SpEL）、排序（`@Order`） |
| 异常处理 | `@ExceptionHandler`（控制器内）或 `@RestControllerAdvice`（全局） |
| 事务 | `@Transactional`（`infra.transaction.annotation.Transactional`），支持隔离级别、传播行为、回滚规则 |
| 数据库 | `JdbcTemplate`（`infra.jdbc.core.JdbcTemplate`）或 `EntityManager`（项目自定义 JDBC ORM） |
| 缓存 | `@EnableCaching` + Caffeine（`infra.cache.support.CaffeineCacheManager`） |
| 会话 | `@EnableSession` + `SessionManager`，支持 `HeaderSessionIdResolver` |
| 服务器 | Netty 嵌入式（不是 Tomcat/Jetty） |
| 模板 | FreeMarker（`.ftl` 文件在 `templates/`） |
| DB 迁移 | Flyway（通过 `infra.flyway.config.FlywayMigrationStrategy` 配置） |

### 项目核心文件

- 入口类: `blog-api/src/main/java/cn/taketoday/blog/BlogApplication.java`
- 自动配置声明: `META-INF/config/infra.context.annotation.config.AutoConfiguration.imports`（指向 `AppConfig`）
- 主配置: `cn.taketoday.blog.config.AppConfig`（缓存、会话、FreeMarker 视图、Flyway 策略）
- 自定义注解: `@ConfigBinding`（配置前缀绑定，如 `prefix "site."`）

## 后端常用命令

```bash
./gradlew :blog-api:build          # 编译 + 测试 + 打包
./gradlew :blog-api:test           # 只运行测试
./gradlew :blog-api:run            # 启动开发服务器
./gradlew :blog-api:clean installInfraDist  # 生产构建
./gradlew clean                    # 清理所有

# 运行单个测试类
./gradlew :blog-api:test --tests "cn.taketoday.blog.web.http.ArticleHttpHandlerTests"

# AOT 处理（GraalVM Native Image 准备，测试中禁用）
./gradlew :blog-api:processAot
# Native Image 构建
./gradlew :blog-api:nativeBuild
```

## 后端测试注意事项

- 测试使用 **MariaDB4j**（内嵌 MariaDB，端口 3307），**不是 H2**。需有 `infra-mariadb4j` 依赖。
- 测试元注解 `@WebAPITest`（位于 `cn.taketoday.blog.web`）组合了 `@AutoLogin`、`@AutoConfigureMockMvc`、`@AutoConfigureRestTestClient`、`@ActiveProfiles("test")`、`@InfraTest`。建议写 API 测试时直接用 `@WebAPITest`。
- 测试自动登录：`SessionConfig` 在测试环境中自动创建带 Blogger 和 User 的会话，通过 `HeaderSessionIdResolver` 注入 header key。`MockMvcBuilderCustomizer` 自动为每个请求添加认证头。
- 测试配置: `src/test/resources/application-test.yaml`。
- 测试是 `@Transactional` 的（自动回滚）。AOT 处理在测试中禁用（`processTestAot { enabled = false }`）。
- 测试中可同时注入 `MockMvcTester` 和 `RestTestClient`。

## 本地开发数据库

启动后端前需创建 MySQL 数据库：

```sql
CREATE DATABASE IF NOT EXISTS today DEFAULT CHARACTER SET utf8mb4;
```

开发环境配置（`application-dev.yaml`）要求 MySQL 在 `localhost:3306`，用户 `root`，密码 `88888888`，数据库 `today`。
Flyway 在启动时按顺序执行：`repair()` → `baseline()` → `migrate()`（在 `AppConfig` 中通过 `FlywayMigrationStrategy` Bean 配置）。
Flyway 迁移文件位于 `src/main/resources/db/migration/`，基线版本 `3.1.0`。

## 前端命令

```bash
# Console（管理后台）— React 17 + UmiJS 3 + Ant Design Pro 4
cd blog-frontend/console
pnpm dev          # 开发服务器（代理到 localhost:8080）
pnpm build        # 生产构建
pnpm lint         # lint（会自动执行 umi g tmp 生成临时文件）
pnpm tsc          # TypeScript 类型检查
pnpm test         # 单元测试
pnpm test:e2e     # E2E 测试（Puppeteer）

# Desktop（公开博客）— React 16 + CRA + react-app-rewired
cd blog-frontend/desktop
pnpm start        # 开发服务器（代理到 localhost:8080）
pnpm build        # 生产构建

# Mobile（公开博客）— Vue 2 + vue-cli-service
cd blog-frontend/mobile
pnpm serve        # 开发服务器
pnpm build        # 生产构建
```

## 前端陷阱

- **`NODE_OPTIONS=--openssl-legacy-provider`**：console 和 desktop 构建时需要（老版本 webpack + 新版 Node.js）。
- Console lint 依赖 Umi 生成文件：lint 脚本会自动执行 `umi g tmp` 生成临时文件后再 lint。
- Desktop 通过 `react-app-rewired` + `customize-cra` 覆盖 webpack 配置。**不要 eject。**
- Console 测试使用 Puppeteer 环境（`tests/PuppeteerEnvironment.js`），E2E 测试需先启动开发服务器。

## Gradle buildSrc 自定义插件

`buildSrc/` 中定义了两个自定义 Gradle 插件：
- `cn.taketoday.blog.conventions` — 共享约定（目前较空）。
- `cn.taketoday.blog.optional-dependencies` — 添加 `optional` 配置，依赖不传递泄漏到下游项目。

## 架构要点

- 后端有两条 API 层：公开 API（`web/http/`）和管理后台 API（`web/console/`）。管理 API 路径为 `/console/*`。
- 拦截器处理登录校验和频率限制（`web/interceptor/`）。
- 项目使用 `EntityManager`（自定义 JDBC-based ORM）而非 JPA/Hibernate。`JdbcTemplate` 也可直接使用。
- `@ConfigBinding` 是项目自定义注解，用于绑定配置前缀（如 `prefix "site."` 绑定站点配置）。
- GraalVM Native Image 运行时提示：`AppConfig.CaffeineCacheHints` 实现 `RuntimeHintsRegistrar` 注册 Caffeine 内部类型。
