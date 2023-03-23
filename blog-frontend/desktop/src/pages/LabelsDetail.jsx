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

import React from 'react';
import { Pagination } from 'antd';
import { connect } from "react-redux";
import { articleService } from "../services";
import ArticleList from '../components/ArticleList';
import { updateNavigations } from "../redux/actions";
import { navigationsMapStateToProps } from "../redux/action-types";
import { applySEO, extractData, isNotEmpty, scrollTop, setTitle } from '../utils';

class LabelsDetail extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.state.articles !== nextState.articles
  //   // this.props.match.params.tagsId !== nextProps.match.params.tagsId
  // }

  state = {
    articles: {
      total: 0,
      current: 1,
      pages: 0,
      size: 8,
      data: null
    }
  }

  componentDidMount() {
    if (this.props.match.params.tagsId) {
      this.updateArticles(this.props)
    }
  }

  updateNavigations(navigation) {
    const navigations = [{ name: '全部标签' }]
    if (navigation) {
      navigations.push(navigation)
    }
    this.props.updateNavigations(navigations)
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.match.params.tagsId) {
      const { tagsId } = this.props.match.params
      if (tagsId !== nextProps.match.params.tagsId) {
        this.updateArticles(nextProps)
      }
    }
  }

  updateArticles(props) {
    const tag = props.match.params.tagsId;
    this.loadArticles(tag)
  }

  loadArticles(tag, page = 1, size = 10) {
    super.setState({ articles: { data: null } });
    scrollTop()
    this.updateNavigations({ name: tag, url: window.location })
    articleService.getArticlesByTag(tag, page, size)
      .then(extractData)
      .then(articles => {
        super.setState({ articles });
        const title = `关于标签 '${tag}' 的文章`
        setTitle(title)
        applySEO(undefined, title)
      })
  }

  render() {
    const { articles } = this.state
    const { tagsId } = this.props.match.params
    return (<>
      <div className="shadow-box" id="test1">
        <div className="data_list_title">关于 <b className='red'>{tagsId}</b> 的文章</div>
        <div className="datas">
          <ArticleList articles={articles.data}/>
        </div>
        {isNotEmpty(articles.data) &&
          <div align='center' style={{ padding: '20px' }}>
            <Pagination
              showQuickJumper
              showSizeChanger
              total={articles.total}
              onChange={(page, size) => {
                this.loadArticles(tagsId, page, size)
              }}
              onShowSizeChange={(page, size) => {
                this.loadArticles(tagsId, page, size)
              }}
              current={articles.current}
            />
          </div>
        }
      </div>
    </>);
  }
}

export default connect(
  navigationsMapStateToProps, { updateNavigations }
)(LabelsDetail)
