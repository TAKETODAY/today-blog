# 任务跟踪：GitHub

本仓库的任务和规格记录在 GitHub Issues 中。所有操作均使用 `gh` CLI。

## 仓库

- GitHub 仓库：`TAKETODAY/today-blog`
- 是否将拉取请求（PR）作为分诊请求来源：否

## 操作约定

- **创建任务**：`gh issue create --title "..." --body "..."`。多行正文使用 heredoc。
- **读取任务**：`gh issue view <number> --json number,title,body,labels,comments`。
- **列出任务**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，按需使用 `--label` 和 `--state` 筛选。
- **发表评论**：`gh issue comment <number> --body "..."`。
- **添加或移除标签**：`gh issue edit <number> --add-label "..."` / `gh issue edit <number> --remove-label "..."`。
- **关闭任务**：`gh issue close <number> --comment "..."`。

命令中的 `<number>` 表示任务编号。

## 技能要求“发布到任务跟踪器”时

创建一个 GitHub Issue。

## 技能要求“获取相关任务”时

使用 `gh issue view <number> --json number,title,body,labels,comments` 读取任务。

## 路径规划操作

以下约定供 `/wayfinder` 技能使用。

- **规划图**：使用一个带 `wayfinder:map` 标签的任务，正文包含笔记、已作决策和待明确事项。
- **子任务**：优先使用 GitHub 子任务功能；不可用时，在子任务正文顶部添加 `Part of #<parent>`，其中 `<parent>` 为父任务编号。
- **阻塞关系**：优先使用 GitHub 原生任务依赖；不可用时，在正文顶部添加 `Blocked by: #<n>, #<n>`，其中 `<n>` 为阻塞任务编号。
- **领取任务**：将任务指派给当前用户。
- **完成任务**：发表评论记录答案，关闭任务，再向规划图的“已作决策”部分补充相关上下文链接。
