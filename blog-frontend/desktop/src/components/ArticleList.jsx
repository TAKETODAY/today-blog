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

import { Empty, Skeleton } from 'antd';
import React from 'react';
import { Link, withRouter } from "react-router-dom";

import { arrayNotEquals, getRandLabel, isNotEmpty } from 'core';
import { Image } from './';
import { connect } from "react-redux";
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import HttpError from "./http/HttpError";

class ArticleList extends React.Component {

  shouldComponentUpdate(nextProps, nextState) {
    return arrayNotEquals(this.props.articles, nextProps.articles)
  }

  render() {
    let { error, errorTitle, title } = this.props;
    if (error) {

      if (!errorTitle) {
        if (title) {
          errorTitle = title + "加载失败"
        }
        else {
          errorTitle = "文章列表加载失败"
        }
      }
      return <HttpError {...error} title={errorTitle}/>
    }
    const { articles } = this.props;
    if (!articles) {
      return <Skeleton active/>
    }
    if (articles.length === 0) {
      return <Empty style={{ marginTop: '10px' }} description="暂无文章"/>
    }
    return (<>
      {
        articles.map((article, idx) => {
          return (
            <div key={article.uri} className="article-list">
              <h2>
                <Link to={`/articles/${article.uri}`} dangerouslySetInnerHTML={{ __html: article.title }}/>
              </h2>
              {isNotEmpty(article.tags) &&
                <span className="tags">{article.tags.map((label, idx) => {
                  return <Link key={idx} to={`/tags/${label}`} className={getRandLabel()} title={label}>{label}</Link>
                })}
                </span>
              }
              <span className="summary" dangerouslySetInnerHTML={{ __html: article.summary }}
                    onClick={() => this.props.history.push(`/articles/${article.uri}`)}/>
              {isNotEmpty(article.cover) &&
                <span className="img">
                  <Image alt={article.summary} src={article.cover} original={false}/>
                </span>
              }
              <span className="info">
                  {new Date(article.createAt).toLocaleString()} | <span className="read-num"> 阅读数 <span
                className="num">{article.pv}</span></span>
                </span>
              <hr/>
            </div>
          )
        })
      }
    </>)
  }
}

export default connect(
  userSessionMapStateToProps, { updateUserSession }
)(withRouter(ArticleList))
