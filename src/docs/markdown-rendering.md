# Markdown 渲染约定

文章 Markdown 的解析、代码高亮、HTML 清理和资源地址处理统一由
`today-blog-frontend/common/article-markdown.js` 提供。

## 使用入口

- 控制台 Markdown 编辑器预览调用 `renderArticleMarkdown`。
- 控制台保存 Markdown 文章时，用同一函数生成兼容历史数据的 `content` 字段。
- 公开文章详情优先使用 `markdown` 字段重新渲染；Markdown 为空时，清理并显示已有的 `content` 字段。
- 富文本文章仍使用保存后的 HTML，但公开详情会经过同一套 HTML 清理和资源地址处理。
  富文本保存时清空 Markdown 字段，避免公开详情重新渲染过时的 Markdown。

后续新增 Markdown 语法或调整安全策略时，只修改共用模块及其测试，不在控制台和公开前端分别创建解析器。

## 解析规则

共用解析器使用 `markdown-it`，启用 URL 自动识别和 Emoji 扩展；代码块使用
`highlight.js` 的按需语言集合。当前高亮语言包括 Bash、CSS、Diff、Groovy、Java、
JavaScript、JSON、Kotlin、Markdown、Plain Text、Python、SQL、TypeScript、XML 和 YAML。
未知语言按普通代码块转义显示。

普通段落中的单个换行按 CommonMark 软换行处理；行尾两个空格或显式硬换行生成
`<br>`。目录、脚注等未启用的扩展语法按普通 Markdown 处理。

Markdown 中允许的 HTML 会先经过 DOMPurify 清理。脚本、表单、内嵌框架、对象、嵌入和
样式标签会被移除，事件属性和不安全资源地址不会进入最终 HTML。链接、图片和其他资源
地址以文章详情地址为基准解析；只保留 `http`、`https`，以及链接使用的 `mailto` 和
`tel` 协议。Markdown 生成的 `srcset` 和任意内联样式会被移除，表格单元格的
`text-align` 样式除外。

## 图片和历史文章

新插入的图片只保存最终 URL。历史文章中使用 `data-original` 或 `data-echo` 延迟加载
属性的图片会在展示时恢复为 `src`，并使用浏览器原生的 `loading="lazy"`；不再依赖页面
级别的懒加载初始化。

保存后的 Markdown 仍保留在 `markdown` 字段，生成的 HTML 保留在 `content` 字段，以便
旧客户端和 Markdown 为空的历史文章继续展示。公开详情对两种字段都执行清理和相对地址
转换。
