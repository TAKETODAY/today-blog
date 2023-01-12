import React from "react"

export default props => {
  const { article, id, children, target, ...rest } = props
  const getArticleId = () => {
    if (article) {
      return article.id
    }
    else {
      return id
    }
  }

  const articleId = getArticleId()
  const targetToUse = target ? target : "_blank"
  return (
    <>
      <a target={targetToUse} href={`/#/articles/${articleId}`} {...rest}>
        {children || article?.title || ''}
      </a>
    </>
  )
}
