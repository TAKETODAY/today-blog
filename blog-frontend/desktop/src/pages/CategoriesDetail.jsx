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
import { connect } from "react-redux";
import { ArticleList } from 'src/components';
import { articleService } from "src/services";
import { updateNavigations } from "src/redux/actions";
import { navigationsMapStateToProps } from "src/redux/action-types";
import { applySEO, extractData, isNotEmpty, scrollTop } from 'core';
import { setTitle } from "../utils/common"

class CategoriesDetail extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.props.match.params.categoryId !== nextProps.match.params.categoryId
  //     || this.state.articles !== nextState.articles
  // }

  state = {
    articles: {
      total: 0,
      current: 1,
      pages: 0,
      size: 8,
      data: null
    },
  }

  componentDidMount() {

    if (this.props.match.params.categoryId) {
      this.updateArticles(this.props)
    }
  }

  updateNavigations(navigation) {
    const navigations = [{ name: '全部分类' }]
    if (navigation) {
      navigations.push(navigation)
    }
    this.props.updateNavigations(navigations)
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.match.params.categoryId) {
      const { categoryId } = this.props.match.params
      if (categoryId !== nextProps.match.params.categoryId) {
        this.updateArticles(nextProps)
      }
    }
  }

  updateArticles(props) {
    const { categoryId } = props.match.params
    this.loadArticles(categoryId);
  }

  loadArticles(categoryId, page = 1, size = 10) {
    super.setState({ articles: { data: null } });
    scrollTop()
    this.updateNavigations({ name: categoryId, url: window.location })

    articleService.getArticlesByCategory(categoryId, page, size)
        .then(extractData)
        .then(articles => {
          super.setState({ articles });
          const title = `关于分类 '${categoryId}' 的文章`;
          setTitle(title)
          applySEO(undefined, title)
        })
  }

  render() {
    const { articles } = this.state
    const { categoryId } = this.props.match.params;
    return (<>
      <div className="shadow-box" id="test1">
        <div className="data-list-title">关于 <b className='red'>{categoryId}</b> 的文章</div>
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
                    this.loadArticles(categoryId, page, size)
                  }}
                  onShowSizeChange={(page, size) => {
                    this.loadArticles(categoryId, page, size)
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
)(CategoriesDetail)

