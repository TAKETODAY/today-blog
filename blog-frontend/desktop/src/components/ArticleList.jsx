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
import { PushpinFilled } from '@ant-design/icons';
import 'antd/es/empty/style';
import 'antd/es/skeleton/style';
import React, { useSyncExternalStore } from 'react';
import { Link } from "react-router-dom";

import { arrayNotEquals, formatDate, getRandLabel, isNotEmpty } from 'core';
import { translate } from '../locales';
import Image from './Image';
import { connect } from "react-redux";
import { withRouter } from "../utils/withRouter";
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import HttpError from "./http/HttpError";
import { preferencesSnapshot, subscribePreferences } from '../../../common/visitor-preferences';

class ArticleList extends React.Component {

  shouldComponentUpdate(nextProps) {
    return arrayNotEquals(this.props.articles, nextProps.articles)
      || this.props.showPinned !== nextProps.showPinned
      || this.props.error !== nextProps.error
      || this.props.title !== nextProps.title
      || this.props.errorTitle !== nextProps.errorTitle
      || this.props.preferences !== nextProps.preferences
  }

  render() {
    let { error, errorTitle, title } = this.props;
    if (error) {

      if (!errorTitle) {
        if (title) {
          errorTitle = translate('articles.loadNamedListFailed', { title })
        }
        else {
          errorTitle = translate('articles.couldNotLoadArticles')
        }
      }
      return <HttpError {...error} title={errorTitle}/>
    }
    const { articles } = this.props;
    if (!articles) {
      return <Skeleton active/>
    }
    if (articles.length === 0) {
      return <Empty style={{ marginTop: '10px' }} description={translate('articles.noArticles')}/>
    }
    return (<>
      {
        articles.map((article, idx) => {
          return (
            <div key={article.uri} className="article-list">
              <h2>
                {this.props.showPinned && article.pinnedAt != null &&
                  <span className="article-pinned-badge">
                     <PushpinFilled aria-hidden="true"/>{translate('articles.pinned')}
                  </span>
                }
                <Link to={`/articles/${article.uri}`} dangerouslySetInnerHTML={{ __html: article.title }}/>
              </h2>
              {isNotEmpty(article.tags) &&
                <span className="tags">{article.tags.map((label, idx) => {
                  return <Link key={idx} to={`/tags/${label}`} className={getRandLabel()} title={label}>{label}</Link>
                })}
                </span>
              }
              <span className="summary" dangerouslySetInnerHTML={{ __html: article.summary }}
                    onClick={() => this.props.navigate(`/articles/${article.uri}`)}/>
              {isNotEmpty(article.cover) &&
                <span className="img article-cover">
                  <Image alt={article.summary} src={article.cover} original={false}
                         loading={idx === 0 ? 'eager' : 'lazy'} decoding="async"/>
                </span>
              }
              <span className="info">
                  {formatDate(article.createAt)} | <span className="read-num"> {translate('articles.viewCount')} <span
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

const ConnectedArticleList = connect(
  userSessionMapStateToProps, { updateUserSession }
)(withRouter(ArticleList))

export default function LocalizedArticleList(props) {
  const preferences = useSyncExternalStore(subscribePreferences, preferencesSnapshot);
  return <ConnectedArticleList {...props} preferences={preferences}/>;
}
