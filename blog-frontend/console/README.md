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

开发服务运行期间避免同时执行 `max setup`、安装依赖或生产构建，这些操作可能重建 `.umi`，
导致正在编译的服务短暂无法解析框架别名或生成模块。测试命令仅在生成文件缺失时执行 setup；
修改框架配置后，先停止开发服务，再重新启动以生成最新配置。

## Umi 4 迁移说明

单元测试集中放在 `tests/unit/`，按 `components/`、`utils/`、`umi/` 分类，不放入 `src/`。
组件测试可运行 `pnpm test:component`；测试初始化及 E2E 辅助脚本保留在 `tests/`。

- 框架 API 从 `@umijs/max` 导入，不直接引用 `.umi` 内部实现。
- 动态路由参数使用 `useParams()`；查询参数从 `history.location.search` 解析。
- `initialState`、model、access、request、locale、layout、Dva 插件均保留。
- Umi 的 request 改为基于 Axios，默认仍返回响应体；现有 `@/utils/http` 请求封装继续保留。
- 默认按路由分包，不再强制将生产代码合并成一个 chunk。
- 单元测试与 Puppeteer E2E 分开配置，浏览器测试位于 `tests/e2e/`。

### 浏览器回归测试

运行 `pnpm test:e2e`，默认在随机端口启动开发服务并在结束后清理自己的服务进程。
E2E 服务使用独立的 `src/.umi-e2e/` 生成目录，不再与日常开发服务共用 `.umi`。
不要同时启动多个 E2E 服务；修改框架配置后仍需重启日常开发服务。
需要安装 Chrome，可用 `CHROME_PATH` 指定浏览器路径；也可用 `E2E_BASE_URL`
连接已有的独立测试服务，此时不会启动或停止服务。

测试拦截 API 并使用固定数据，不需要后端，不验证真实登录凭据或后端鉴权。
当前覆盖独立登录页、匿名访问限制、Markdown 编辑器加载/输入/刷新，
以及三种导航布局的设置切换、持久化和入口可用性；未预期 API 请求与页面异常会使测试失败。
未引用的旧 Radar 模板已清理，类型检查不再依赖 BizCharts/G2。

### 仍需业务联调

升级后应联调：登录/退出、登录后返回原页面、权限、工作台统计、文章编辑与保存、上传、列表筛选分页，
以及 `/blog-admin/#/...` 下的直接访问和刷新。
