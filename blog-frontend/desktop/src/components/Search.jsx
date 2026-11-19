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
import { LoadingOutlined, SearchOutlined } from '@ant-design/icons';
import { Dropdown, Empty, Input, Menu, message, Spin } from 'antd';
import 'antd/es/dropdown/style';
import 'antd/es/empty/style';
import 'antd/es/input/style';
import 'antd/es/menu/style';
import 'antd/es/message/style';
import 'antd/es/spin/style';
import { Link } from "react-router-dom";
import debounce from 'lodash/debounce';

import { articleService } from 'src/services';
import { extractData, isEmpty, translate } from 'core';

class SearchComponent extends React.Component {

  constructor(props) {
    super(props)
    this.searchArticles = debounce(this.searchArticles, 300)
  }

  state = {
    input: null,
    data: null,
    loading: false,
  }

  searchArticles = query => {

    if (isEmpty(query)) {
      return
    }

    this.lastQuery = query
    this.setState({ data: null, loading: true })
    articleService.search(query)
      .then(extractData)
      .then(extractData)
      .then(data => {
        if (query !== this.lastQuery) {
          return
        }
        this.setState({ data, loading: false })
      })
      .catch(err => {
        if (err.response) {
          message.error(err.response.data?.detail || err.response.data?.message || err.message)
          this.setState({ data: null, loading: false })
        }
      })
  }

  renderMenu() {
    const { data, loading } = this.state
    if (loading) {
      return [
        {
          key: 'loading',
          label: (
            <Menu.Item className='textCenter'>
              <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin/>} tip={translate('搜索中…', 'Searching…')}/>
            </Menu.Item>
          )
        }
      ]
    }
    if (data === null) {
      return []
    }
    if (isEmpty(data)) {
      return [
        {
          key: 'empty',
          label: (
            <Menu.Item>
              <Empty style={{ margin: '0px' }} image={Empty.PRESENTED_IMAGE_SIMPLE}
                     description={translate(`暂无关于 ${this.lastQuery} 的文章`, `No articles found for ${this.lastQuery}`)}/>
            </Menu.Item>
          )
        },
      ]
    }
    return data.map((article) => {
      return (
        {
          key: article.id,
          label: (
            <Menu.Item key={article.id}>
              <Link to={`/articles/${article.uri}`} title={article.title}>{article.title}</Link>
            </Menu.Item>
          )
        }
      )
    })
  }

  search = (q) => {
    window.location = `/search?q=${q}`
    // this.props.history.push(`/search?q=${ q }`);
    this.props.onSearch && this.props.onSearch(q)
  }

  render() {
    const { autoFocus, style } = this.props
    const items = this.renderMenu()
    return (
      <Dropdown menu={{ items }} placement="bottomRight" trigger="click">
        <Input
          suffix={<SearchOutlined/>}
          style={style}
          placeholder={translate('搜索文章', 'Search articles')}
          onChange={event => {
            const input = event.target.value;
            this.setState({ input })
            this.searchArticles(input)
          }}
          onPressEnter={_ => {
            this.search(this.state.input)
          }}
          autoFocus={autoFocus}>

        </Input>
      </Dropdown>
    )
  }
}

export default SearchComponent
