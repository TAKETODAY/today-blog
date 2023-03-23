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
import React, { useEffect, useState } from 'react';
import { ArticleList } from '../components';
import { getQuery, scrollTop, setTitle } from '../utils';
import { articleService } from "../services";
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";
import { useHistory, useLocation } from "react-router-dom";

const { Search } = Input;

export default connect(
    navigationsMapStateToProps, { updateNavigations }
)(props => {

  const history = useHistory();
  const location = useLocation();

  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [articles, setArticles] = useState([])
  const [query, setQuery] = useState(getQuery("q"))
  const [value, setValue] = useState('')

  const updateNavigations = () => {
    const navigations = [
      { name: '搜索', url: '/search' }
    ]
    query && navigations.push({ name: query, url: `/search?q=${ query }` })
    props.updateNavigations(navigations)
  }
  setTitle()

  const updateArticles = (page = 1, size = 10) => {
    scrollTop()
    if (query) {
      setArticles([])
      setLoading(true)
      articleService.searchPageable(query, page, size).then(res => {
        setArticles(res.data)
        setTitle(`搜索 '${ query }' 的文章`)
      }).catch(err => {
        setError({
          status: err.response ? err.response.status : 500,
          title: err.message
        })
      }).finally(() => setLoading(false))
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
    history.push(`/search?q=${ query }`);
  }, [query])

  const renderSearch = () => (
      <Search
          value={ value }
          allowClear
          defaultValue={ query }
          loading={ loading }
          placeholder="Search TODAY"
          onSearch={ setQuery }
          onChange={ (event) => {
            setValue(event.target.value)
          } }
          autoFocus={ true }
      />
  )

  if (!query) {
    return (<>
      <div className="shadow-box" style={ { marginTop: '120px' } }>
        { renderSearch() }
      </div>
    </>)
  }
  return (<>
    <div className="shadow-box">
      <div style={ { marginBottom: "10px" } }>
        { renderSearch() }
      </div>
      <div className="data_list_title">关于 <em>{ query }</em> 的搜索结果</div>
      <div className="datas">
        <ArticleList articles={ articles.data } error={ error } title={ `关于 '${ query }' 的搜索结果` }/>
      </div>
      <div align='center' style={ { padding: '20px' } }>
        <Pagination
            showQuickJumper
            showSizeChanger
            total={ articles.all }
            onChange={ updateArticles }
            onShowSizeChange={ updateArticles }
            current={ articles.current }
            showTotal={ n => <><b className='red'>{ n }</b>篇文章</> }
        />
      </div>
    </div>
  </>);
})
