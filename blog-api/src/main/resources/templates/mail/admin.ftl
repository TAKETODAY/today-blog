<#ftl output_format="HTML" auto_esc=true>
<!DOCTYPE html>
<html lang="zh-CN">
<head><meta charset="UTF-8"><title>评论待审核</title></head>
<body>
<h2>${site.name!"博客"} 有新的评论待审核</h2>
<p>文章：${comment.articleTitle!""}</p>
<p>评论者：${comment.commenter!"访客"}</p>
<blockquote style="white-space: pre-wrap">${comment.content!""}</blockquote>
<p>请登录管理后台审核此评论。</p>
<p><a href="${site.host!""}">访问网站</a></p>
</body>
</html>
