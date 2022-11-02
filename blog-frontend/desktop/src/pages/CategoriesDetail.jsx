import { Pagination } from 'antd';
import React from 'react';
import ArticleList from '../components/ArticleList';
import { applySEO, getCacheable, isNotEmpty, scrollTop, setTitle } from '../utils';
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";

class CategoriesDetail extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.props.match.params.categoryId !== nextProps.match.params.categoryId
  //     || this.state.articles !== nextState.articles
  // }

  state = {
    articles: {
      all: 0,
      current: 1,
      num: 0,
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

  componentWillReceiveProps(nextProps) {
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

    getCacheable(`/api/articles/categories/${ categoryId }?page=${ page }&size=${ size }`)
        .then(res => {
          const articles = res.data;
          super.setState({ articles });
          const title = `关于分类 '${ categoryId }' 的文章`;
          setTitle(title)
          applySEO(null, title)
        })
  }

  render() {
    const { articles } = this.state
    const { categoryId } = this.props.match.params;
    return (<>
      <div className="data_list" id="test1">
        <div className="data_list_title">关于 <b className='red'>{ categoryId }</b> 的文章</div>
        <div className="datas">
          <ArticleList articles={ articles.data }/>
        </div>
        { isNotEmpty(articles.data) &&
            <div align='center' style={ { padding: '20px' } }>
              <Pagination
                  showQuickJumper
                  showSizeChanger
                  total={ articles.all }
                  onChange={ (page, size) => {
                    this.loadArticles(categoryId, page, size)
                  } }
                  onShowSizeChange={ (page, size) => {
                    this.loadArticles(categoryId, page, size)
                  } }
                  current={ articles.current }
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

