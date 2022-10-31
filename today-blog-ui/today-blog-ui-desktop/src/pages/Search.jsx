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
      <div className="data_list" style={ { marginTop: '120px' } }>
        { renderSearch() }
      </div>
    </>)
  }
  return (<>
    <div className="data_list">
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
