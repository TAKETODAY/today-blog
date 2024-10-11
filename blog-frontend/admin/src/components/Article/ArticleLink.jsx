import React from "react"

let desktopBase;
if (process.env.NODE_ENV === 'production') {
  desktopBase = ''
}
else {
  desktopBase = 'http://localhost:3000'
}


export default props => {
  const { article, id, children, target, ...rest } = props
  const getArticleId = () => {
    if (article) {
      return article.uri
    }
    else {
      return id
    }
  }

  const articleId = getArticleId()
  const targetToUse = target ? target : "_blank"
  return (
      <>
        <a target={targetToUse} href={`${desktopBase}/articles/${articleId}`} {...rest}>
          {children || article?.title || ''}
        </a>
      </>
  )
}
