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
import 'antd/es/pagination/style';
import React from 'react';
import ArticleList from '../components/ArticleList';
import { applySEO, extractData, scrollTop } from 'core';
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";
import { articleService } from '../services';
import { setTitle } from "../utils/common"
import { withRouter } from "../utils/withRouter"

class Home extends React.Component {

  state = {
    articles: {
      all: 0,
      current: 1,
      num: 0,
      size: 10,
      data: []
    }
  }

  getPagination() {
    const params = new URLSearchParams(this.props.location.search);
    const positiveInteger = (value, fallback) => {
      const number = Number(value);
      return Number.isSafeInteger(number) && number > 0 ? number : fallback;
    };
    return {
      page: positiveInteger(params.get('page'), 1),
      size: positiveInteger(params.get('size'), 10),
    };
  }

  pageURL(page, size) {
    const params = new URLSearchParams(this.props.location.search);
    params.set('page', page);
    params.set('size', size);
    return `${this.props.location.pathname}?${params}`;
  }

  changePage = (page, size) => {
    this.props.navigate(this.pageURL(page, size));
  }

  renderPageItem = (page, type, element) => {
    if (type !== 'page' && type !== 'jump-prev' && type !== 'jump-next') {
      return element;
    }
    return React.cloneElement(element, {
      href: this.pageURL(page, this.getPagination().size),
      rel: undefined,
      onClick: event => {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) {
          event.stopPropagation();
          return;
        }
        event.preventDefault();
      },
    });
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
    const { page, size } = this.getPagination();
    this.updateArticles(page, size)
    this.props.updateNavigations(null)
  }

  componentDidUpdate(prevProps) {
    if (prevProps.location.search !== this.props.location.search) {
      const { page, size } = this.getPagination();
      this.updateArticles(page, size);
    }
  }

  render() {
    const { articles, error } = this.state
    return (<>
      <div className="shadow-box" id="test1">
        <div className="data-list-title">最新文章</div>
        <ArticleList articles={articles.data} error={error} title="最新文章" showPinned/>
        <div align='center' style={{ padding: '20px' }}>
          <Pagination
              showQuickJumper
              showSizeChanger
              total={articles.total}
              onChange={this.changePage}
              itemRender={this.renderPageItem}
              pageSize={this.getPagination().size}
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
)(withRouter(Home))
