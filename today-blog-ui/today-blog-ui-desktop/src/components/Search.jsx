import { LoadingOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Dropdown, Empty, Input, Menu, message, Spin } from 'antd';
import debounce from 'lodash/debounce';
import React from 'react';
import { Link, withRouter } from "react-router-dom";
import { articleService } from '../services';
import { isEmpty } from '../utils';

const { Search } = Input;
const loadingIcon = <LoadingOutlined style={ { fontSize: 24 } } spin/>

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
        .then(res => {
          if (query !== this.lastQuery) {
            return
          }
          const data = res.data.data.map(article => ({
            id: article.id,
            title: article.title
          }))
          this.setState({ data, loading: false })
        })
        .catch(err => {
          if (err.response) {
            message.error(err.response.data.message)
            this.setState({ data: null, loading: false })
          }
        })
  }

  renderMenu() {
    const { data, loading } = this.state
    if (loading) {
      return (<Menu className='textCenter'>
        <Menu.Item>
          <Spin indicator={ loadingIcon } tip="搜索中..."/>
        </Menu.Item>
      </Menu>)
    }
    if (data === null) {
      return <></>
    }
    if (isEmpty(data)) {
      return (
          <Menu>
            <Menu.Item>
              <Empty
                  style={ { margin: '0px' } }
                  image={ Empty.PRESENTED_IMAGE_SIMPLE }
                  description={ `暂无关于 ${ this.lastQuery } 的文章` }/>
            </Menu.Item>
          </Menu>
      )
    }
    return (
        <Menu>
          {
            data.map((article, idx) => {
              return (
                  <Menu.Item key={ article.id }>
                    <Link to={ `/articles/${ article.id }` }
                          title={ article.title }
                          dangerouslySetInnerHTML={ { __html: article.title } }/>
                  </Menu.Item>
              )
            })
          }
        </Menu>
    )
  }

  search = (q) => {
    window.location = `/search?q=${ q }`
    // this.props.history.push(`/search?q=${ q }`);
    this.props.onSearch && this.props.onSearch(q)
  }

  render() {
    const { autoFocus, style } = this.props
    return (
        <Dropdown overlay={ this.renderMenu() } placement="bottomRight" trigger="click">
          <Input
              suffix={ <SearchOutlined/> }
              style={ style }
              placeholder="Search TODAY"
              onChange={ event => {
                const input = event.target.value;
                this.setState({ input })
                this.searchArticles(input)
              } }
              onPressEnter={ (event) => {
                this.search(this.state.input)
              } }
              autoFocus={ autoFocus }>

          </Input>
        </Dropdown>
    )
  }
}

export default withRouter(SearchComponent)

