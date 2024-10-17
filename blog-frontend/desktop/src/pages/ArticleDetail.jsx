/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import { LockOutlined } from '@ant-design/icons';
import { Form, Input, message, Skeleton } from 'antd';
import React from 'react';
import { Link, withRouter } from 'react-router-dom';
import zone from '../assets/images/share/zone.png';
import { AdminLink, ArticleComment, HttpError } from 'src/components';
import { articleService } from 'src/services';
import {
  applySEO,
  getArticleId,
  getRandLabel,
  getSummary,
  isEmpty,
  isNotEmpty,
  logging,
  scrollTop,
  shareQQZone
} from 'core';

import { setTitle } from "../utils/common"
import { connect } from "react-redux";
import { navigationsUserSessionMapStateToProps } from "src/redux/action-types";
import { updateNavigations } from "src/redux/actions";
import { store } from "src/redux/store";
import lazyload from "core/lazyload";
import { http, startNProgress, stopNProgress } from "core";

const passwordRules = [
  { required: true, message: '请输入访问密码!' }
]

function buildOptions(state) {
  const { article } = state
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


class ArticleDetail extends React.Component {

  state = {
    key: null,
    article: null,
    needPassword: false,
  }

  componentDidMount() {
    this.loadArticle(this.props.match.params.articleId)
    this.updatePageView()
  }

  loadArticle(articleId, key = null) {
    articleId = getArticleId(articleId)

    super.setState({ article: null, comments: null });
    scrollTop()
    if (key == null) {
      key = sessionStorage.getItem("article-password:" + articleId)
    }
    articleService.getById(articleId, key).then(res => {
      const article = res.data;
      super.setState({ article, needPassword: false });
      setTitle(article.title)
      setSEO(article)
      this.updateNavigations()
      setTimeout(() => {
        articleService.updatePageView(article.id)
      }, 1500);
      if (key) {
        sessionStorage.setItem("article-password:" + articleId, key)
      }
      try {
        lazyload.init({
          offset: 100,
          throttle: 250,
          unload: false,
        })
      }
      catch (e) {
        logging("懒加载出错")
      }
    }).catch(err => {
      const { status } = err.response || { status: 0 }

      const error = {
        status: err.response ? err.response.status : 500,
        title: "文章加载失败",
        subTitle: err.message
      };

      super.setState({ error })

      message.error(err.response.data.message)
      if (status === 403) {
        this.setState({ needPassword: true })
      }
      else if (status === 404) {
        this.props.history.push("/not-found");
      }
    })
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.match.params.articleId) {
      const { articleId } = this.props.match.params
      if (articleId !== nextProps.match.params.articleId) {
        this.loadArticle(nextProps.match.params.articleId)
      }
    }
  }

  updatePageView() {
    setTimeout(() => {
      startNProgress()
      http.post("/api/pv?referer=" + encodeURIComponent(location.href))
          .then(stopNProgress)
    }, 1500)
  }

  componentDidUpdate(prevProps) {
    console.log(prevProps.location, this.props.location)
    const locationChanged =
        this.props.location !== prevProps.location;

    if (locationChanged) {
      console.log("路由发生了变化")
      this.updatePageView()
    }
  }

  updateNavigations() {

    const navigations = [
      { name: '全部博客', url: '/' },
      { name: this.state.article.category, url: `/categories/${this.state.article.category}` },
      { name: this.state.article.title, url: `/articles/${this.state.article.uri}` }
    ]

    this.props.updateNavigations(navigations)
  }

  requirePassword = (formData) => {
    const { articleId } = this.props.match.params
    this.loadArticle(articleId, formData.key)
  }

  shareQQZone = () => {
    shareQQZone(buildOptions(this.state))
  }

  renderPassword() {
    return (<>
      <div className="shadow-box">
        <Form name="requirePassword" onFinish={this.requirePassword}>
          <Form.Item name="key" rules={passwordRules} validateStatus="validating">
            <Input autoFocus size="large" placeholder="请输入访问密码" prefix={<LockOutlined/>}/>
          </Form.Item>
        </Form>
      </div>
    </>)
  }

  render() {
    const { article, needPassword, error } = this.state
    if (needPassword) {
      return this.renderPassword()
    }

    if (error) {
      return <HttpError {...error}/>
    }

    if (isEmpty(article)) { // loading
      return <Skeleton active/>
    }
    const { userSession } = this.props

    return (<>

      <div className="shadow-box">
        <article className="article-content">
          <h1 className="title">{article.title}</h1>
          <div className="property">
            <span>发布于 {new Date(article.createAt).toLocaleString()}</span> |
            <span> 更新 {new Date(article.updateAt).toLocaleString()}</span> |
            <span> 浏览 {article.pv} </span> |
            <span> <Link to={`/categories/${article.category}`}
                         title={`点击查看分类 ${article.category}`}>{article.category}</Link></span> |
            {userSession?.blogger && <>
            <span>
                <AdminLink href={`/articles/${article.id}/${isNotEmpty(article.markdown) ? "modify" : "modify-rich-text"}`}
                           target='_blank'>
                  &nbsp;编辑
                </AdminLink>
            </span> |
              <span style={{ cursor: "pointer" }}>
              <img onClick={this.shareQQZone} className="share" title="分享到QQ空间" src={zone} width="18" alt="分享到QQ空间"/>
            </span>
            </>}
          </div>
          <div className="markdown contentTxt" dangerouslySetInnerHTML={{ __html: article.content }}/>
        </article>
        {isNotEmpty(article.labels) &&
            <div className="article-tags" id="tagcloud">
              {article.labels.map((label, idx) => {
                return <Link key={idx} to={`/tags/${label.name}`} className={getRandLabel()}>{label.name}</Link>
              })}
            </div>
        }
        <div className="article-copyright">
          {article.copyright || '本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。'}
        </div>
      </div>
      <ArticleComment articleId={article.id}/>
    </>)
  }
}

export default connect(
    navigationsUserSessionMapStateToProps, { updateNavigations }
)(withRouter(ArticleDetail))


