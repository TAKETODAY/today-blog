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

import { Pagination } from 'antd';
import React from 'react';
import ArticleList from '../components/ArticleList';
import { applySEO, extractData, scrollTop, setTitle } from '../utils';
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";
import { articleService } from '../services';


class Home extends React.Component {

  state = {
    articles: {
      all: 0,
      current: 1,
      num: 0,
      size: 8,
      data: []
    }
  }

  updateArticles(page = 1, size = 10) {
    scrollTop()

    const { articles } = this.state
    articles['data'] = undefined

    super.setState({ articles })
    articleService.fetchHomeArticles(page, size)
      .then(extractData)
      .then(articles => {
        super.setState({ articles });
      })
      .catch(error => {
        const err = {
          status: error.response ? error.response.status : 500,
          subTitle: error.message
        };
        super.setState({ error: err })
      })
  }

  componentDidMount() {
    setTitle("代码是我心中的一首诗")
    applySEO()
    this.updateArticles()
    this.props.updateNavigations(null)
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.match.params.categoryId) {
      const { categoryId } = this.props.match.params
      if (categoryId !== nextProps.match.params.categoryId) {
        this.updateArticles(nextProps)
      }
    }
  }

  render() {
    const { articles, error } = this.state
    return (<>
      <div className="shadow-box" id="test1">
        <div className="data-list-title">最新文章</div>
        <ArticleList articles={articles.data} error={error} title="最新文章"/>
        <div align='center' style={{ padding: '20px' }}>
          <Pagination
            showQuickJumper
            showSizeChanger
            total={articles.total}
            onChange={this.updateArticles.bind(this)}
            onShowSizeChange={this.updateArticles.bind(this)}
            current={articles.current}
            showTotal={n => <><b className='red'>{n}</b>篇文章</>}
          />
        </div>
      </div>
    </>);
  }
}

export default connect(
  navigationsMapStateToProps, { updateNavigations }
)(Home)
