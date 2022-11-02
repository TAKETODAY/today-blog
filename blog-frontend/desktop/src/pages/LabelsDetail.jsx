import { Pagination } from 'antd';
import React from 'react';
import ArticleList from '../components/ArticleList';
import { applySEO, isNotEmpty, scrollTop, setTitle } from '../utils';
import { connect } from "react-redux";
import { navigationsMapStateToProps } from "../redux/action-types";
import { updateNavigations } from "../redux/actions";
import { articleService } from "../services";


class LabelsDetail extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.state.articles !== nextState.articles
  //   // this.props.match.params.tagsId !== nextProps.match.params.tagsId
  // }

  state = {
    articles: {
      all: 0,
      current: 1,
      num: 0,
      size: 8,
      data: null
    }
  }

  componentDidMount() {
    if (this.props.match.params.tagsId) {
      this.updateArticles(this.props)
    }
  }

  updateNavigations(navigation) {
    const navigations = [{ name: '全部标签' }]
    if (navigation) {
      navigations.push(navigation)
    }
    this.props.updateNavigations(navigations)
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.match.params.tagsId) {
      const { tagsId } = this.props.match.params
      if (tagsId !== nextProps.match.params.tagsId) {
        this.updateArticles(nextProps)
      }
    }
  }

  updateArticles(props) {
    const tagsId = props.match.params.tagsId;
    this.loadArticles(tagsId)
  }

  loadArticles(tagsId, page = 1, size = 10) {
    super.setState({ articles: { data: null } });
    scrollTop()
    this.updateNavigations({ name: tagsId, url: window.location })

    articleService.getTag(tagsId, page, size).then(res => {
      const articles = res.data;
      super.setState({ articles });
      const title = `关于标签 '${ tagsId }' 的文章`
      setTitle(title)
      applySEO(null, title)
    })
  }

  render() {
    const { articles } = this.state
    const { tagsId } = this.props.match.params;
    return (<>
      <div className="data_list" id="test1">
        <div className="data_list_title">关于 <b className='red'>{ tagsId }</b> 的文章</div>
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
                    this.loadArticles(tagsId, page, size)
                  } }
                  onShowSizeChange={ (page, size) => {
                    this.loadArticles(tagsId, page, size)
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
)(LabelsDetail)
