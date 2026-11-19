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

import { Input, Pagination } from 'antd';
import 'antd/es/input/style';
import 'antd/es/pagination/style';
import React, { useEffect, useState } from 'react';
import ArticleList from '../components/ArticleList';
import { extractData, getQuery, scrollTop, translate } from 'core';
import { articleService } from "../services";
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";
import { useNavigate, useLocation } from "react-router-dom";
import { setTitle } from "../utils/common"

const { Search } = Input;

export default connect(
    navigationsMapStateToProps, { updateNavigations }
)(props => {

  const navigate = useNavigate();
  const location = useLocation();

  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [articles, setArticles] = useState([])
  const [query, setQuery] = useState(getQuery("q"))
  const [value, setValue] = useState('')

  const updateNavigations = () => {
    const navigations = [
      { label: ['搜索', 'Search'], url: '/search' }
    ]
    query && navigations.push({ name: query, url: `/search?q=${query}` })
    props.updateNavigations(navigations)
  }
  const title = query ? translate(`关于「${query}」的搜索结果`, `Search results for “${query}”`) : translate('搜索', 'Search');
  setTitle(title)

  const updateArticles = (page = 1, size = 10) => {
    scrollTop()
    if (query) {
      setArticles([])
      setLoading(true)
      articleService.searchPageable(query, page, size)
          .then(extractData)
          .then(setArticles)
          .catch(err => {
            setError({
              status: err.response ? err.response.status : 500,
              title: err.message
            })
          })
          .finally(() => setLoading(false))
    }
  }

  useEffect(() => {
    const query1 = getQuery("q");
    if (query1) {
      setQuery(query1)
    }
    else {
      setQuery('')
    }
  }, [location])

  useEffect(() => {
    updateNavigations()
    updateArticles()
    setValue(query)
    navigate(`/search?q=${query}`);
  }, [query])

  const renderSearch = () => (
      <Search
          value={value}
          allowClear
          defaultValue={query}
          loading={loading}
          placeholder={translate('搜索文章', 'Search articles')}
          onSearch={setQuery}
          onChange={(event) => {
            setValue(event.target.value)
          }}
          autoFocus={true}
      />
  )

  if (!query) {
    return (<>
      <div className="shadow-box" style={{ marginTop: '120px' }}>
        {renderSearch()}
      </div>
    </>)
  }
  return (<>
    <div className="shadow-box">
      <div style={{ marginBottom: "10px" }}>
        {renderSearch()}
      </div>
      <div className="data-list-title">{title}</div>
      <div className="datas">
        <ArticleList articles={articles.data} error={error} title={title}/>
      </div>
      <div align='center' style={{ padding: '20px' }}>
        <Pagination
            showQuickJumper
            showSizeChanger
            total={articles.total}
            onChange={updateArticles}
            onShowSizeChange={updateArticles}
            current={articles.current}
            showTotal={n => <><b className='red'>{n}</b>{translate('篇文章', n === 1 ? ' article' : ' articles')}</>}
        />
      </div>
    </div>
  </>);
})
