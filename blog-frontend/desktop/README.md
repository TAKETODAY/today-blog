# 博客 PC 端 UI

## 安装依赖

```shell
pnpm install
```

## 本地调试

```shell
pnpm start
```

## 首页资源检查

```shell
pnpm build
pnpm check:home
pnpm test
```

`check:home` 根据 Vite manifest 递归统计首页静态依赖（不包含其他页面和编辑器的异步资源），
要求 JS gzip 小于 350 KB、CSS gzip 小于 40 KB，并禁止首页静态包含 CodeMirror。
首页直接加载，其他页面和评论编辑器保持懒加载；Ant Design 样式随使用组件引入，主题色保持 `#337ab7`。

编辑器仅注册常用的 15 种高亮语言；其他语言仍显示代码和转义内容，但不提供语法着色。
新增语言时，在 `src/components/Editor/highlight.js` 中注册并增加测试，避免重新引入全量语言包。

部署时需单独验证（生成 `.gz` 不表示服务器自动使用）：

- JS/CSS 响应包含 `Content-Encoding: gzip`（或由服务器提供 Brotli）和适当的 `Vary: Accept-Encoding`。
- 带 hash 的 `/assets/` 资源设置长期缓存，例如 `Cache-Control: public, max-age=31536000, immutable`。
- `index.html` 应重新验证，不使用与带 hash 资源相同的永久缓存。
- 部署后重新运行桌面端 PageSpeed，比较 LCP、TBT、CLS 和冷启动请求瀑布；构建体积不等同于实测加载时间。

Font Awesome 暂保留原有外部加载，避免破坏导航、文章内容和编辑器中的 `fa-*` 图标。
后续替换或自托管时需要一起核对这些使用场景。
