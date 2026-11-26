# TODAY BLOG

![Java17](https://img.shields.io/badge/JDK-17+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-Harry_Yang-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f18d51f79f1e40d7b590ba85a70ae783)](https://app.codacy.com/gh/TAKETODAY/today-blog?utm_source=github.com&utm_medium=referral&utm_content=TAKETODAY/today-blog&utm_campaign=Badge_Grade)

后台使用 https://github.com/TAKETODAY/today-infrastructure 作为基础框架，该项目也是其最佳实践。

一个完整的博客系统，包含公开 PC 博客、后台管理端，以及一个自研框架驱动的 REST API 后端。

## ✨ 技术栈

- **后端**：Java 17+（推荐 JDK 25） + [TODAY Infrastructure](https://github.com/TAKETODAY/today-infrastructure)（自研框架，Netty
  嵌入式服务器、虚拟线程、Flyway 迁移、FreeMarker、Caffeine 缓存）、MySQL
- **前端**：
    1. 公开 PC 博客使用 React + Vite
    2. 后台管理使用 React + Ant Design（Umi 4）

## 📁 项目结构

```text
today-blog-opensource
├── today-blog-core/                 # 公共基础能力（密码编码、日期、字符串和通用返回值）
├── today-blog-server/               # 服务器实现（公开 API /console/* 管理 API）
├── today-blog-cli/                  # 独立运维 CLI
├── today-blog-upgrade-protocol/     # API 与 CLI 共用的升级协议
├── today-blog-frontend/             # 前端 Gradle 子项目（通过 pnpm 构建）
│   ├── console/               # 后台管理（React 18 + Umi 4 + Ant Design 4）
│   ├── desktop/               # PC 端公开博客（React 18 + Vite）
│   └── common/                # 前端共享模块
├── buildSrc/                  # Gradle 自定义插件
└── resources/                 # 站点静态资源
```

`today-blog-core` 提供服务器与 CLI 可复用的基础能力，仅依赖 `infra-core`，不依赖 Web、数据库或应用模块。
服务器业务、持久化模型、附件配置、HTTP 路由与启动装配集中在 `today-blog-server`；升级协议仍独立维护。
只有出现明确的服务器公共能力复用需求时，才增加 `today-blog-server-core`，不预先创建接口转发层。
模块重命名不改变应用标识 `today-blog-api`、启动器 `blog-api`、发行包内的 `api/` 目录及 HTTP 路径。

## 🧰 环境要求

| 依赖    | 版本                                               | 说明                                                                                |
|---------|----------------------------------------------------|-------------------------------------------------------------------------------------|
| JDK     | **17+**（最低），推荐 **25**（SDKMAN `25-librca`） | 后端编译与运行                                                                      |
| Gradle  | 随 wrapper 自动下载（9.8.x）                       | 无需手动安装，使用 `./gradlew`                                                      |
| MySQL   | 8.x                                                | 本地开发数据库，`localhost:3306`                                                    |
| Node.js | 18 及以上（建议 Node LTS）                         | Console 仍使用旧版 webpack，相关脚本已内置 `NODE_OPTIONS=--openssl-legacy-provider` |
| pnpm    | 12.x（已验证）                                      | 前端依赖安装与构建                                                                  |

> 测试无需 MySQL：后端测试通过内嵌 **MariaDB4j**（端口 3307）运行。

## 🚀 快速开始

### 1. 初始化数据库

创建数据库（后端默认连接 `root/88888888@localhost:3306/today`，见 `application-dev.yaml`）：

```sql
CREATE DATABASE IF NOT EXISTS today DEFAULT CHARACTER SET utf8mb4;
```

执行初始化脚本（建库 + 全量建表）：

```bash
mysql -uroot -ppassword today < today-blog-server/src/main/resources/db/schema.sql
```

可选：导入演示数据：

```bash
mysql -uroot -ppassword today < today-blog-server/src/main/resources/db/data.sql
```

启动后端时 Flyway 会自动完成增量迁移（`repair → baseline(3.1.0) → migrate`），无需手动处理后续版本。

### 2. 启动后端

```bash
./gradlew :today-blog-server:run
```

服务默认监听 `http://localhost:8080`，默认激活 `dev` profile。

### 3. 启动前端

在仓库根目录执行以下命令，按各自的锁文件安装前端依赖：

```bash
./gradlew :today-blog-frontend:installFrontendDependencies
```

| 端                  | 目录                    | 命令         | 访问地址              | 代理目标                   |
|---------------------|-------------------------|--------------|-----------------------|----------------------------|
| Console（管理后台） | `today-blog-frontend/console` | `pnpm dev`   | http://localhost:8000 | `/api` `/upload` → `:8080` |
| Desktop（PC 博客）  | `today-blog-frontend/desktop` | `pnpm start` | http://localhost:3000 | → `:8080`                  |

开发时请保持后端先运行于 `:8080`，前端通过代理转发请求。

## ⚙️ 配置文件说明

后端配置位于 `today-blog-server/src/main/resources/`：

| 文件                    | 说明                                                                                                 |
|-------------------------|------------------------------------------------------------------------------------------------------|
| `application.yaml`      | 通用配置：默认 profile（`dev`）、Netty/虚拟线程、FreeMarker、Jackson、Flyway baseline（3.1.0）、日志 |
| `application-dev.yaml`  | 开发环境：MySQL 连接（`root/pwd@localhost:3306/today`）、站点 host、调试日志                         |
| `application-prod.yaml` | 生产环境：MySQL、`site.host`、阿里云 OSS、CDN、附件目录                                              |
| `infra.properties`      | SQL 输出与控制台着色                                                                                 |

常用配置段：

- `datasource`：数据源（用户名 / 密码 / JDBC URL，默认 HikariCP）
- `site`：站点信息（`host`、`subTitle` 等）
- `aliyun.oss`：阿里云 OSS（默认关闭 `enabled: false`）
- `mail`：SMTP 邮件（评论通知等）
- `attachment`：附件存储位置 / CDN 前缀

切换环境 profile：

```bash
./gradlew :today-blog-server:run -Dinfra.profiles.active=prod
# 或
JAVA_OPTS="-Dinfra.profiles.active=prod" ./today-blog-server/build/install/blog-api/bin/blog-api
```

## 🧪 测试

```bash
./gradlew :today-blog-core:test :today-blog-upgrade-protocol:test :today-blog-cli:test :today-blog-server:test
# 单个服务器测试类
./gradlew :today-blog-server:test --tests "cn.taketoday.blog.web.http.ArticleHttpHandlerTests"
```

- 测试使用内嵌 MariaDB4j（端口 3307）， **无需本地 MySQL**，用例默认 `@Transactional` 自动回滚。
- API 测试使用 `@WebAPITest` 元注解，自动登录并在每个请求注入认证头。

## 📦 生产构建与部署

在仓库根目录一键构建完整发行包（API、Console、Desktop 和独立运维 CLI）：

```bash
./gradlew release
```

产物为 `build/today-blog-<version>-<build>.zip` 和 `build/dist/today-blog-cli.jar`。
也可以单独构建前端或 API：

```bash
./gradlew :today-blog-frontend:buildReleaseFrontend
./gradlew :today-blog-server:installDist
```

API 发行目录为 `today-blog-server/build/install/blog-api/`，包含可直接运行的脚本：

```bash
start.sh     # 后台启动（写入 app.pid）
stop.sh      # 停止
restart.sh   # 重启
find.sh      # 查看进程
```

生产环境以 `prod` profile 启动（记得先用 `application-prod.yaml` 配置真实数据源）：

```bash
./gradlew :today-blog-server:run -Dinfra.profiles.active=prod
# 或在前台运行发行包（日志输出到控制台）
JAVA_OPTS=-Dinfra.profiles.active=prod ./today-blog-server/build/install/blog-api/bin/blog-api
```

生产环境后台运行请使用[部署文档](src/docs/deployment.md)中的 `today-blog start` 或 systemd 托管。

可选：构建 GraalVM Native Image（需 GraalVM 及 `-PincludeAOT`）：

```bash
./gradlew :today-blog-server:clean nativeBuild -PincludeAOT
```

## 📚 API 文档

项目提供 OpenAPI 3.0 规范文件，可直接导入 Swagger UI / Redoc 等工具浏览：

- **规范文件**：`today-blog-server/docs/openapi.yaml`
- **本地预览**：将文件导入 https://editor.swagger.io 或本地 Swagger UI 即可查看全部接口与鉴权说明。

其余入口：

- **代码即文档**：公开 API 位于 `today-blog-server/src/main/java/cn/taketoday/blog/web/http/`，管理后台 API 位于 `web/console/`（路径前缀 `/console/*`
  ），直接阅读 `@RestController` 即可。
- **Console 生成**：在 `today-blog-frontend/console` 下执行 `pnpm openapi`（umi openapi）可基于后端接口生成类型定义。
- **鉴权**：登录 / 博主接口支持浏览器会话（`SESSION` Cookie，博主接口需博主账号）或 API Key，见下文「API Key」章节。

## 🔑 API Key

博主登录后可以通过管理接口创建 API Key。创建响应中的 `value` 只返回一次，请妥善保存。

```http
POST /api/console/api-keys
Content-Type: application/json

{"name":"automation"}
```

使用 API Key 调用需要登录或博主权限的接口时，支持以下两种请求头：

```http
Authorization: Bearer tbk_...
```

或：

```http
X-API-Key: tbk_...
```

管理 API Key：

```http
GET    /api/console/api-keys
DELETE /api/console/api-keys/{id}
POST   /api/console/api-keys/{id}/revoke
```

API Key 数据库存储的是 SHA-256 摘要。撤销后可以删除但不可恢复；API Key 的权限与所属用户一致；如果所属用户被禁用，API Key 也无法认证。

API Key 每个请求重新验证，仅设置当前请求身份，不创建或修改登录 Session。
携带 Bearer 或 `X-API-Key` 时优先使用密钥认证；无效、空白或已撤销的密钥返回 401，不回退到已有登录会话。

## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [TODAY Infrastructure](https://github.com/TAKETODAY/today-infrastructure) 基础设施库
* [JetBrains](https://www.jetbrains.com/?from=https://github.com/TAKETODAY/today-blog) 感谢 JetBrains 提供免费 IDE 开源授权

## 📄 开源协议

使用 [GPLv3](https://github.com/TAKETODAY/today-infrastructure/blob/master/LICENSE) 开源协议

# 部署与在线升级

完整发行包构建、systemd 首次部署和后台升级说明见 [部署文档](src/docs/deployment.md)。当前版本仅校验文件完整性，不验证安装包来源。
