<#ftl output_format="HTML" auto_esc=true>
<!DOCTYPE html>
<html lang="zh-CN">
<head><meta charset="UTF-8"><title>评论审核通过</title></head>
<body>
<p>${comment.commenter!"访客"}，您好！</p>
<p>您在 ${site.name!"博客"} 的文章《${comment.articleTitle!""}》下的评论已审核通过。</p>
<blockquote style="white-space: pre-wrap">${comment.content!""}</blockquote>
<p><a href="${site.host!""}/articles/${comment.articleId?c}">查看文章与评论</a></p>
</body>
</html>
