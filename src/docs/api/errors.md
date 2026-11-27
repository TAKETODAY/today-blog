# API 错误协议

## 响应约定

控制器异常、应用的兜底错误（`/error` 和服务器 `sendError`）、默认维护拒绝和限流使用 RFC 9457 Problem Details，媒体类型为
`application/problem+json`。HTTP 状态与 `status` 相同；成功响应不增加错误包装。

兜底错误按 HTTP 状态选择通用错误码和本地化文案，不返回框架内部消息或 HTML 错误页。连接在应用接管前被拒绝、响应已提交或由代理生成错误时，不保证采用此格式。

```json
{
  "type": "urn:today-blog:problem:maintenance.under_maintenance",
  "title": "系统维护中",
  "status": 503,
  "detail": "系统维护中，请稍后重试。",
  "code": "maintenance.under_maintenance"
}
```

- `type`：稳定的问题类型 URI，不承诺可访问的文档地址。
- `code`：机器标识，小写、点号分层、层内下划线。
- `title`：该类型的简短标题。
- `detail`：安全、可展示的本次错误说明，不作为程序判断条件。
- `instance`：目前不主动生成，也未新增请求追踪系统。

完整错误码用于登录、文章密码、维护等特殊动作。分类必须使用带点号的前缀，
例如 `maintenance.upgrade.`，不能认为同一前缀下的错误都应采取相同动作。
未知错误码使用通用展示，不能要求客户端穷举。

### 错误码格式

格式为 `域[.子域...].错误标识`，`.` 分隔域、各级子域和最终错误标识，支持多个子域：

| 示例 | 域 | 子域 | 错误标识 |
| --- | --- | --- | --- |
| `article.password_required` | `article` | 无 | `password_required` |
| `maintenance.upgrade.package.not_found` | `maintenance` | `upgrade`、`package` | `not_found` |
| `notification.mail.not_retryable` | `notification` | `mail` | `not_retryable` |

各段以小写字母开头，使用小写字母、数字和下划线；当前协议以下划线分隔段内单词，
不混用连字符。可按业务语义继续增加子域，不要求固定段数，也不要为增加层级而机械拆开词组。
已经发布的错误码和默认派生的类型 URI 都是协议的一部分，改名需考虑客户端兼容性。

## 定义位置

统一契约：`today-blog-server/src/main/java/cn/taketoday/blog/problem/Problem.java`。
各领域枚举实现 `Problem`，定义 `code()` 和 `status()`；`type()` 默认从 code 派生。
异常与响应仅依赖接口，不要求业务枚举继承集中式类型。
错误码不需要集中登记；协议一致性测试从编译产物中发现领域枚举，并纳入维护模块的问题常量。
不要在 properties 中配置状态或 URI。

| 定义 | 覆盖范围 |
| --- | --- |
| `problem/CommonProblems` | HTTP 通用请求、分页、日期、认证及内部错误 |
| `service/ArticleProblems` | 文章存在性、可见性、访问密码、置顶与必填项 |
| `service/ContentProblems` | 分类、标签、评论、附件、邮件重试 |
| `service/UserProblems` | 用户存在性、账号状态、密码修改 |
| `maintenance/MaintenanceProblems` | 维护状态、升级包、记录、映射、环境及任务提交（接口常量） |
| `maintenance/backup/BackupProblems` | 备份存在性、执行器可用性 |

以下为通用码及需特殊处理的代表性错误；完整列表以各领域定义为准。

| code | 默认状态 | 含义 |
| --- | ---: | --- |
| `request.invalid` | 400 | 通用请求格式或参数问题 |
| `request.validation_failed` | 400 | 参数校验失败 |
| `request.parameter_missing` | 400 | 必要参数缺失，模板参数为参数名 |
| `request.method_not_allowed` | 405 | 方法不支持 |
| `request.not_acceptable` | 406 | 无法提供要求的响应格式 |
| `request.payload_too_large` | 413 | 明确的请求大小超限 |
| `request.unsupported_media_type` | 415 | 请求媒体类型不支持 |
| `request.rate_limit_exceeded` | 429 | 请求限流 |
| `auth.required` | 401 | 需要登录 |
| `auth.invalid_credentials` | 401 | 账号不存在或密码错误，统一提示以避免泄漏账号存在性 |
| `auth.access_denied` | 403 | 权限不足 |
| `resource.not_found` | 404 | 通用资源不存在 |
| `resource.conflict` | 409 | 通用资源状态冲突 |
| `article.password_required` | 403 | 需要文章密码 |
| `article.password_invalid` | 403 | 文章密码错误 |
| `maintenance.under_maintenance` | 503 | 主动维护 |
| `system.service_unavailable` | 503 | 服务暂不可用，不表示主动维护 |
| `system.internal_error` | 500 | 内部错误，原因只记录日志 |

旧异常根据状态选择通用 code，但保留原 HTTP 状态，不能从通用 code 推断实际状态。
需要精确协议的业务应直接使用 `ProblemException`，不要依靠中文消息猜测类型。

## 消息与国际化

- 消息按领域放在 `today-blog-server/src/main/resources/problems/<领域>/`，不再集中在 `i18n/errors`。
- 每个领域的默认简体中文文件为 `messages.properties`，英文文件为 `messages_en.properties`。
- 所有资源显式按 UTF-8 加载，正文直接使用完整 `code` 作为键，标题使用 `code.title`。
  这只影响资源键，HTTP 响应仍使用 `title` 和 `detail` 字段。
- 参数使用 `MessageFormat` 的 `{0}` 等占位符；单引号需按其规则转义。
- `Accept-Language` 按语言范围和权重匹配简体中文、英文；缺失、非法或不支持时回退中文。
- 禁止回退服务器系统 Locale。响应包含 `Content-Language` 和 `Vary: Accept-Language`。
- 目标语言缺键时先回退默认资源；加载或格式化失败时记录告警并使用安全兜底，不能输出消息键。
- 维护响应继续使用 `Retry-After` 和 `Cache-Control: no-store`。

### 领域资源目录

| 目录（相对于 `src/main/resources/`） | 维护内容 |
| --- | --- |
| `problems/common/` | 通用请求、认证、资源与系统错误 |
| `problems/article/` | 文章与文章访问密码 |
| `problems/user/` | 用户资料、密码与账号状态 |
| `problems/content/` | 分类、标签、评论、附件与邮件重试 |
| `problems/maintenance/` | 维护状态与在线升级 |
| `problems/backup/` | 备份问题 |
| `problems/validation/` | 字段校验消息 |

文件内保留完整消息键，例如 `maintenance.upgrade.package.not_found`。
目录按领域职责组织，不要求与错误码每一级子域一一对应。
`ProblemResponses` 在启动时直接扫描 `classpath*:problems/**/messages.properties`，
自动提取 basename，按名称排序后统一加载并按完整键查找；支持目录及 JAR 中的资源和多级领域目录。
业务代码不需要知道资源路径，也不根据错误码动态推导文件位置。
新增领域目录只需同时提供中英文文件；扫描只在初始化时执行，不支持运行时热发现。
扫描失败、没有发现默认消息文件或不同类路径位置使用相同 basename 时启动失败，避免静默覆盖。
JAR 应保留 `problems/` 目录条目；Native Image 还需在构建时包含消息资源与资源包，不能假定 JVM 扫描测试代表原生镜像已验证。
不同领域之间不得重复定义消息键，同一文件内也不得重复，以免依赖加载顺序覆盖文案。

## 使用方法

### 抛出现有业务问题

优先复用领域问题定义，在发现业务失败的位置抛出异常：

```java
throw new ProblemException(ArticleProblems.PASSWORD_INVALID);
```

对可能为空的查询结果，可使用空值检查工具；非空时不会抛出异常，该方法不返回查询对象：

```java
ProblemException.notNull(article, ArticleProblems.NOT_FOUND);
```

### 在 Service 中使用

`Problem` 和 `ProblemException` 位于公共 `problem` 包，Service 可以直接使用。
以下为 Service 内部的示意方法，其中 `getById` 为既有查询方法：

```java
public Article obtainById(long id) {
  Article article = getById(id);
  ProblemException.notNull(article, ArticleProblems.NOT_FOUND);
  return article;
}
```

Service 决定具体业务问题并向上抛出；Controller 通常不必捕获或重新包装，
`ExceptionHandling` 会统一交给 `ProblemResponses` 转为本地化 HTTP 响应。
不要为了生成错误响应而向 Service 传入 `ServerHttpExchange`、响应对象或已经翻译的消息。

当前 `Problem` 包含 `HttpStatusCode`，属于带 HTTP 语义的应用错误契约，
不是完全独立于传输协议的领域模型。它可以在进程内部跨层传播，
但不能直接当作 RPC 错误报文或序列化 Java 异常跨进程传递。

### 使用默认实现，不定义枚举

无需领域枚举时，可通过静态工厂创建不可变的默认实现 `DefaultProblem`：

```java
Problem problem = Problem.of("request.parameter_missing", HttpStatus.BAD_REQUEST);
throw new ProblemException(problem, "email");
```

静态工厂不自动注册错误码。新增稳定业务码仍需补齐两种语言的消息资源；优先复用已有领域定义。

复用默认实现时，建议把问题保存为 `static final Problem` 常量，避免在每次请求中重复构造。
也可以使用常量类组织领域问题，不要求所有问题都以枚举表示。
协议一致性测试自动发现枚举，并单独收集 `MaintenanceProblems` 的常量；
若新增其他常量类，需显式将其纳入测试。

### 传递消息模板参数

例如分页大小超过限制时，传入允许公开的最大值：

```java
throw new ProblemException(CommonProblems.PAGE_SIZE_EXCEEDED, 100);
```

中文资源 `problems/common/messages.properties`：

```properties
request.page_size_exceeded.title=分页大小超出限制
request.page_size_exceeded=每页大小不能超过 {0}。
```

英文资源 `problems/common/messages_en.properties`：

```properties
request.page_size_exceeded.title=Page size limit exceeded
request.page_size_exceeded=The page size must not exceed {0}.
```

`{0}` 对应异常构造器中第一个消息参数，而不是错误码或 HTTP 状态。
英文请求将按模板返回 `The page size must not exceed 100.`；消息在响应阶段才解析。

新增带参数的类型时，同时在两种资源中配置兼容模板，只传允许公开的参数。
不可传入 SQL、凭据、内部异常或连接配置。

### 携带请求级 detail

当 detail 来自限流注解、维护配置或其他已经确认可展示的请求输入时，使用
`ProblemException.of(problem, detail, arguments)` 携带稳定错误码和本次请求说明：

```java
throw ProblemException.of(CommonProblems.RATE_LIMIT_EXCEEDED, errorMessage);
```

请求级 detail 优先于问题默认消息模板，但不修改共享的 `Problem` 定义。它首先作为消息资源键按请求的
`Accept-Language` 在响应阶段解析；消息键不存在时将传入值作为原文返回，因此也兼容直接传入已经确认安全的文案。
detail 使用的模板参数与默认消息模板共用异常中的参数。detail 只能包含安全、可展示内容，不能传入 SQL、凭据、内部异常或连接配置。
即使问题状态为 5xx，只要传入了自定义 detail，系统也会使用该 detail；调用方必须确保其内容安全、可展示。

### 字段校验消息

校验注解使用资源键，而不是硬编码某一种语言的消息，例如：

```java
@NotEmpty(message = "{validation.email.required}")
@Email(message = "{validation.email.invalid}")
public String email;
```

对应的 `validation.email.required` 和 `validation.email.invalid` 必须在
`problems/validation/messages.properties` 和 `messages_en.properties` 中定义。
请求触发校验失败后，顶层错误码为 `request.validation_failed`，
具体字段信息位于 `errors` 数组，不包含被拒绝的输入值。

### 客户端判断与展示

精确错误码用于决定业务动作，带点号的域前缀用于分类；不要匹配 `detail` 文案：

```javascript
const code = problem.code || '';
if (code === 'article.password_required' || code === 'article.password_invalid') {
  // 展示文章密码输入界面。
}
else if (code.startsWith('maintenance.upgrade.package.')) {
  // 将错误归类到升级包相关问题，具体动作仍需检查完整错误码。
}
```

通用提示优先读取 `detail`，再兼容旧响应的 `message`，最后使用本地通用提示。
未知错误码也必须能够展示，不能因为没有匹配分支而丢失错误提示。

### 新增业务问题检查清单

1. 选择稳定的域、子域和错误标识，确认不存在语义相同的已有问题。
2. 在所属领域定义 `Problem`，明确默认 HTTP 状态；不要仅按状态猜测业务错误码。
3. 在所属领域的两种语言资源中补齐 `code` 正文和 `code.title` 标题，保持模板参数兼容；新增目录会在启动时自动发现。
4. 若新增非枚举常量类，将其纳入协议一致性测试；枚举无需登记。
5. 在真实业务判断点抛出 `ProblemException`，补充错误码、状态及响应测试。
6. 只有需要特殊交互时才新增客户端分支，其余错误使用通用展示。

## 迁移范围与兼容

- 业务异常使用 `ProblemException.of(problem, detail, arguments)` 携带稳定业务码；自定义 `detail` 按请求语言解析为消息，找不到资源键时保留原文。
- 普通 `ResponseStatusException` 保留状态和响应头，原因使用安全的通用模板。
- 未明确为客户端输入错误的 `IllegalArgumentException` 按内部错误处理；业务校验应使用类型化异常。
- OSS、数据库、空指针等异常对外统一安全提示，完整异常留在日志中。
- 校验失败返回 `errors` 数组，包含可用的 `field`、校验器 `code` 和 `detail`，不公开拒绝值。
  字段消息从原始 `{validation.*}` 约束模板按请求语言解析；标准规则使用 `validation.<规则名>`，未知规则安全兜底。
  校验器 code 不是顶层业务错误码。
- 前端通用错误提示和桌面端直接读取错误的路径优先读取 `detail`，再兼容旧 `message`。
- 现有文章、分类、标签、评论、附件、账号、邮件重试、分页、统计日期、升级与备份的业务错误调用已指定领域码。
- 资源不存在统一为 404；重复分类/标签、附件重复同步、文章置顶状态、升级包状态等冲突为 409。
  无变更等输入问题仍为 400。博主鉴权仍使用 404 隐藏资源存在性。
- 登录与博主拦截器的旧 JSON/纯文本失败改为统一问题响应。
- 内部技术异常仍通过安全通用码处理，不为每种 IO、SQL、加密实现故障建立业务枚举。
- 后台任务记录的执行结果、环境就绪信息不是 HTTP 请求失败，保留其业务数据语义。

维护公告、限流注解或配置中的自定义消息可以填写消息资源键并按请求语言翻译；找不到键时原样展示。
5xx 状态同样使用自定义消息，因此调用方必须确保传入内容不会泄露敏感信息。
维护消息为空时使用资源默认提示；已有状态保存的非空默认中文也会视为原文保留。

## 验证

`ProblemResponsesTests` 检查错误码唯一性、命名格式、各领域翻译键完整性、文件内及跨领域重复键、
目录与 JAR 资源发现、模板参数、语言协商和 JSON 扩展展开。
`ExceptionHandlingTests` 检查业务兼容、服务器信息脱敏、HTTP 头保留和类型化响应。
`MaintenanceFilterTests` 覆盖早期过滤器的本地化与维护放行规则。
