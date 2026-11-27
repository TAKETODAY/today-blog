/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { LockOutlined } from '@ant-design/icons';
import { Form, Input, message, Skeleton } from 'antd';
import 'antd/es/form/style';
import 'antd/es/input/style';
import 'antd/es/message/style';
import 'antd/es/skeleton/style';
import React, { useCallback, useMemo } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import zone from '../assets/images/share/zone.png';
import AdminLink from '../components/AdminLink';
import ArticleComment from '../components/comment/ArticleComment';
import HttpError from '../components/http/HttpError';
import { articleService } from 'src/services';
import { applySEO, extractData, getArticleId, getRandLabel, getSummary, isEmpty, isNotEmpty, logging, scrollTop, shareQQZone } from 'core';

import { setTitle } from "../utils/common"
import { store } from "src/redux/store";
import { articleBaseUrl, renderArticleBody } from 'core/article-markdown';
import 'core/article-markdown.css';
import 'core/markdown-highlight.css';
import { useRequest, useSessionStorageState } from "ahooks";

import { useBreadcrumb, useQueryParams, useUserSession } from "../components/hooks";
import { formatDate } from 'core';
import { translate } from '../locales';

function buildOptions(article) {
  const host = store.getState().options["site.host"];
  return {
    url: `${host}/articles/${article.uri}`,
    desc: article.title,
    cover: article.cover,
    summary: article.summary,
  }
}

function setSEO(article) {
  try {
    let content = `${article.title},${article.category}`
    if (isNotEmpty(article.labels)) {
      article.labels.forEach(label => {
        content += ',' + label.name
      })
    }
    applySEO(content, isEmpty(article.summary) ? getSummary(article.content) : article.summary)
  }
  catch (e) {
    logging(e)
  }
}

export default () => {
  const navigate = useNavigate()
  const [, setNav] = useBreadcrumb()
  const [userSession] = useUserSession()
  const { key } = useQueryParams()

  const articleId = getArticleId(useParams().articleId)
  const [password, setPassword] = useSessionStorageState(`article-password:${articleId}`, {
    defaultValue: key
  })

  const { data: article, error, loading, refresh } = useRequest(() => {
    scrollTop()

    return articleService.getById(articleId, password).then(extractData).then(article => {
      setTitle(article.title)
      setSEO(article)

      setNav([
        { labelKey: 'navigation.allArticles', url: '/' },
        { name: article.category, url: `/categories/${article.category}` },
        { name: article.title, url: `/articles/${article.uri}` }
      ])

      if (!userSession?.blogger) {
        setTimeout(() => {
          articleService.updatePageView(article.id)
        }, 1500);
      }

      return article
    }).catch(err => {
      const { status } = err.response || { status: 0 }

      const error = {
        status: err.response ? err.response.status : 500,
        subTitle: err.message,
        needPassword: status === 403 && (!err.response.data?.code
          || err.response.data.code === 'article.password_required'
          || err.response.data.code === 'article.password_invalid')
      }

      if (status === 404) {
        navigate("/not-found")
      }

      if (err.response) {
        message.error(err.response.data?.detail || err.response.data?.message || err.message)
      }

      throw error
    })
  }, {
    refreshDeps: [articleId, password]
  });

  const shareZone = useCallback(() => {
    shareQQZone(buildOptions(article))
  }, [article])

  const siteHost = store.getState().options['site.host'];
  const articleBody = useMemo(() => {
    if (!article) {
      return '';
    }
    return renderArticleBody(article, articleBaseUrl(siteHost, article.uri || articleId));
  }, [article, articleId, siteHost]);


  if (error) {
    if (error.needPassword) {
      const requirePassword = formData => {
        setPassword(formData.key)
        //refresh()
      }

      return (<>
        <div className="shadow-box">
          <Form name="requirePassword" onFinish={requirePassword}>
            <Form.Item name="key" rules={[{ required: true, message: translate('articles.enterTheArticlePassword') }]}
                       validateStatus="validating">
              <Input autoFocus size="large" placeholder={translate('articles.enterTheArticlePassword')} prefix={<LockOutlined/>}/>
            </Form.Item>
          </Form>
        </div>
      </>)
    }

    return <HttpError {...error} title={translate('articles.couldNotLoadThisArticle')}/>
  }

  if (loading) { // loading
    return <Skeleton active/>
  }

  return (<>
    <div className="shadow-box">
      <article className="article-content">
        <h1 className="title">{article.title}</h1>
        <div className="property">
          <span>{translate('articles.published')} {formatDate(article.createAt)}</span> |
          <span> {translate('articles.updated')} {formatDate(article.updateAt)}</span> |
          <span> {translate('articles.views')} {article.pv} </span> |
          <span> <Link to={`/categories/${article.category}`}
                       title={translate('articles.browseCategory', { category: article.category })}>{article.category}</Link></span> |
          {userSession?.blogger && <>
            <span>
                <AdminLink href={`/articles/${article.id}/${isNotEmpty(article.markdown) ? "modify" : "modify-rich-text"}`}
                           target='_blank'>
                  &nbsp;{translate('articles.edit')}
                </AdminLink>
            </span> |
            <span style={{ cursor: "pointer" }}>
              <img onClick={shareZone} className="share" title={translate('articles.shareOnQzone')}
                   src={zone} width="18" alt={translate('articles.shareOnQzone')}/>
            </span>
          </>}
        </div>
        <div className="markdown contentTxt" dangerouslySetInnerHTML={{ __html: articleBody }}/>
      </article>
      {isNotEmpty(article.labels) &&
          <div className="article-tags" id="tagcloud">
            {article.labels.map((label, idx) => {
              return <Link key={idx} to={`/tags/${label.name}`} className={getRandLabel()}>{label.name}</Link>
            })}
          </div>
      }
      <div className="article-copyright">
        {article.copyright || translate('articles.thisIsAnOriginalArticle')}
      </div>
    </div>
    <ArticleComment articleId={article.id}/>
  </>)
}
