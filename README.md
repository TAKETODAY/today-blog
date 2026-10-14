# TODAY BLOG

![Java17](https://img.shields.io/badge/JDK-17+-success.svg)
[![GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](./LICENSE)
[![Author](https://img.shields.io/badge/Author-Harry_Yang-blue.svg)](https://github.com/TAKETODAY)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f18d51f79f1e40d7b590ba85a70ae783)](https://app.codacy.com/gh/TAKETODAY/today-blog?utm_source=github.com&utm_medium=referral&utm_content=TAKETODAY/today-blog&utm_campaign=Badge_Grade)

后台使用 https://github.com/TAKETODAY/today-infrastructure 作为基础框架，该项目也是其最佳实践

前端

1. PC 端使用 React
2. 移动端使用 Vue
3. 后台管理 使用 React + Ant Design

## 🙏 鸣谢

本项目的诞生离不开以下项目：

* [TODAY Infrastructure](https://github.com/TAKETODAY/today-infrastructure) 基础设施库
* [Jetbrains](https://www.jetbrains.com/?from=https://github.com/TAKETODAY/today-blog) 感谢 Jetbrains 提供免费开源授权

## 📄 开源协议

使用 [GPLv3](https://github.com/TAKETODAY/today-infrastructure/blob/master/LICENSE) 开源协议

## API Key

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
```

API Key 数据库存储的是 SHA-256 摘要，撤销后不可恢复。API Key 的权限与所属用户一致；如果所属用户被禁用，API Key 也无法认证。

API Key 每个请求重新验证，仅设置当前请求身份，不创建或修改登录 Session。
携带 Bearer 或 `X-API-Key` 时优先使用密钥认证；无效、空白或已撤销的密钥返回 401，不回退到已有登录会话。
