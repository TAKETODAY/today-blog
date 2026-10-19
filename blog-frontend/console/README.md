# 博客后台管理 UI


## 环境与技术栈

- Node.js >= 20.19，推荐 Node.js 22。
- Umi Max 4.7.25、React 18.3、Ant Design 4；保留现有 ProComponents 独立包与 Dva model。
- Console 是独立的 pnpm 项目，请在本目录执行命令。

## 常用命令

```shell
pnpm install
pnpm dev                 # 开发服务器，API 代理见 config/proxy.ts
pnpm build               # 生产构建，输出 dist/
pnpm test --runInBand     # Jest 单元测试（不再使用 umi test）
pnpm tsc                 # TypeScript 检查
```

安装后会自动运行 `max setup` 生成 `src/.umi/`，无需提交生成文件。生产资源路径仍为
`/blog-admin/`，路由仍使用 Hash 模式。服务端部署时需要保留完整的 dist 目录，包括异步分包。
新工具链不再需要 `--openssl-legacy-provider`，不再支持 IE 11。

## Umi 4 迁移说明

单元测试集中放在 `tests/unit/`，按 `components/`、`utils/`、`umi/` 分类，不放入 `src/`。
组件测试可运行 `pnpm test:component`；测试初始化及 E2E 辅助脚本保留在 `tests/`。

- 框架 API 从 `@umijs/max` 导入，不直接引用 `.umi` 内部实现。
- 动态路由参数使用 `useParams()`；查询参数从 `history.location.search` 解析。
- `initialState`、model、access、request、locale、layout、Dva 插件均保留。
- Umi 的 request 改为基于 Axios，默认仍返回响应体；现有 `@/utils/http` 请求封装继续保留。
- 默认按路由分包，不再强制将生产代码合并成一个 chunk。
- 单元测试与旧 Puppeteer E2E 分开配置；E2E 依赖本地 Chrome 和后端服务，尚未完成新版运行验证。

### 已知遗留问题

`pnpm tsc` 仍会报告未被业务页面引用的 `src/pages/workplace/components/Radar/index.tsx`
缺少 `bizcharts` 和 `G2` 类型。这是旧模板组件的依赖缺失；本次保留其源码，不为无引用组件引入旧图表库。
生产构建不包含该组件。

升级后应联调：登录/退出、登录后返回原页面、权限、工作台统计、文章编辑与保存、上传、列表筛选分页，
以及 `/blog-admin/#/...` 下的直接访问和刷新。
