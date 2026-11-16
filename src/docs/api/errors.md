# API 错误协议

## 响应约定

控制器异常、默认维护拒绝和限流使用 RFC 9457 Problem Details，媒体类型为
`application/problem+json`。HTTP 状态与 `status` 相同；成功响应不增加错误包装。

```json
{
  "type": "urn:today-blog:problem:system.maintenance",
  "title": "系统维护中",
  "status": 503,
  "detail": "系统维护中，请稍后重试。",
  "code": "system.maintenance"
}
```

- `type`：稳定的问题类型 URI，不承诺可访问的文档地址。
- `code`：机器标识，小写、点号分层、层内下划线。
- `title`：该类型的简短标题。
- `detail`：安全、可展示的本次错误说明，不作为程序判断条件。
- `instance`：目前不主动生成，也未新增请求追踪系统。

完整错误码用于登录、文章密码、维护等特殊动作。分类必须使用带点号的前缀，
例如 `upgrade.`，不能认为同一前缀下的错误都应采取相同动作。
未知错误码使用通用展示，不能要求客户端穷举。

## 定义位置

统一契约：`blog-api/src/main/java/cn/taketoday/blog/error/Problem.java`。
各领域枚举实现 `Problem`，定义 `code()` 和 `status()`；`type()` 默认从 code 派生。
异常与响应仅依赖接口，不要求业务枚举继承集中式类型。
`ProblemCatalog.all()` 显式汇总当前协议，供完整性检查使用，不参与运行时自动注册。
不要在 properties 中配置状态或 URI。

| 定义 | 覆盖范围 |
| --- | --- |
| `error/CommonProblems` | HTTP 通用请求、分页、日期、认证、维护及内部错误 |
| `service/ArticleProblems` | 文章存在性、可见性、访问密码、置顶与必填项 |
| `service/ContentProblems` | 分类、标签、评论、附件、邮件重试 |
| `service/UserProblems` | 用户存在性、账号状态、密码修改 |
| `maintenance/upgrade/UpgradeProblems` | 升级包、记录、映射、环境及任务提交 |
| `maintenance/backup/BackupProblems` | 备份存在性、执行器可用性 |

以下为通用码及需特殊处理的代表性错误；完整列表以领域枚举及总清单为准。

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
| `system.maintenance` | 503 | 主动维护 |
| `system.unavailable` | 503 | 服务暂不可用，不表示主动维护 |
| `system.internal_error` | 500 | 内部错误，原因只记录日志 |

旧异常根据状态选择通用 code，但保留原 HTTP 状态，不能从通用 code 推断实际状态。
需要精确协议的业务应直接使用 `ProblemException`，不要依靠中文消息猜测类型。

## 消息与国际化

- 默认简体中文：`blog-api/src/main/resources/i18n/errors.properties`。
- 英文：`blog-api/src/main/resources/i18n/errors_en.properties`。
- 所有资源显式按 UTF-8 加载，消息键为 `code.title` 和 `code.detail`。
- 参数使用 `MessageFormat` 的 `{0}` 等占位符；单引号需按其规则转义。
- `Accept-Language` 按语言范围和权重匹配简体中文、英文；缺失、非法或不支持时回退中文。
- 禁止回退服务器系统 Locale。响应包含 `Content-Language` 和 `Vary: Accept-Language`。
- 目标语言缺键时先回退默认资源；加载或格式化失败时记录告警并使用安全兜底，不能输出消息键。
- 维护响应继续使用 `Retry-After` 和 `Cache-Control: no-store`。

新增类型化业务错误示例：

```java
throw new ProblemException(ArticleProblems.PASSWORD_INVALID);
```

无需领域枚举时，可通过静态工厂创建不可变的默认实现 `DefaultProblem`：

```java
Problem problem = Problem.of("request.parameter_missing", HttpStatus.BAD_REQUEST);
throw new ProblemException(problem, "email");
```

静态工厂不自动注册错误码。新增稳定业务码仍需补齐两种语言的消息资源，
并将常量纳入协议清单及相应测试；优先复用已有领域定义。

新增带参数的类型时，同时在两种资源中配置兼容模板，只传允许公开的参数。
不可传入 SQL、凭据、内部异常或连接配置。
新增领域枚举后，将其加入 `ProblemCatalog`；清单测试扫描编译产物中的所有 `Problem` 枚举，防止漏注册。

## 迁移范围与兼容

- 旧 `ErrorMessageException` 的客户端消息保留原文；服务端消息不再直接公开。
- 普通 `ResponseStatusException` 保留状态和响应头，原因使用安全的通用模板。
- 未明确为客户端输入错误的 `IllegalArgumentException` 按内部错误处理；业务校验应使用类型化异常。
- OSS、数据库、空指针等异常对外统一安全提示，完整异常留在日志中。
- 文章访问的密码分支已使用明确类型；旧 `ArticlePasswordException` 暂保留兼容处理。
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

维护公告、限流注解或配置中的自定义消息属于输入内容，原样展示，不自动翻译。
维护消息为空时使用资源默认提示；已有状态保存的非空默认中文也会视为原文保留。

## 验证

`ProblemResponsesTests` 检查错误码唯一性、命名格式、翻译键完整性、模板参数、语言协商和 JSON 扩展展开。
`ExceptionHandlingTests` 检查业务兼容、服务器信息脱敏、HTTP 头保留和类型化响应。
`MaintenanceFilterTests` 覆盖早期过滤器的本地化与维护放行规则。
