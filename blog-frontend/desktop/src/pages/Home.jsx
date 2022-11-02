import { Pagination } from 'antd';
import React from 'react';
import ArticleList from '../components/ArticleList';
import { applySEO, scrollTop, setTitle } from '../utils';
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

    const articles = this.state.articles
    articles['data'] = undefined

    super.setState({ articles: articles })
    articleService.fetchHomeArticles(page, size)
        .then(res => {
          super.setState({ articles: res.data });
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

  componentWillReceiveProps(nextProps) {
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
      <div className="data_list" id="test1">
        <div className="data_list_title">最新文章</div>
        <ArticleList articles={ articles.data } error={ error } title="最新文章"/>
        <div align='center' style={ { padding: '20px' } }>
          <Pagination
              showQuickJumper
              showSizeChanger
              total={ articles.all }
              onChange={ this.updateArticles.bind(this) }
              onShowSizeChange={ this.updateArticles.bind(this) }
              current={ articles.current }
              showTotal={ n => <><b className='red'>{ n }</b>篇文章</> }
          />
        </div>
      </div>
    </>);
  }
}

export default connect(
    navigationsMapStateToProps, { updateNavigations }
)(Home)
