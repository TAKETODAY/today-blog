import { Empty, Result, Skeleton } from 'antd';
import React from 'react';
import { Link, } from "react-router-dom";
import { withRouter } from "react-router";

import { arrayNotEquals, getRandLabel, isNotEmpty } from '../utils';
import { Image } from './';
import { connect } from "react-redux";
import { userSessionMapStateToProps } from "../redux/action-types";
import { updateUserSession } from "../redux/actions";
import HttpError from "./http/HttpError";

class ArticleList extends React.Component {

  shouldComponentUpdate(nextProps, nextState) {
    return arrayNotEquals(this.props.articles, nextProps.articles)
  }

  render() {
    let { error, errorTitle, title } = this.props;
    if (error) {

      if (!errorTitle) {
        if (title) {
          errorTitle = title + "加载失败"
        }
        else {
          errorTitle = "文章列表加载失败"
        }
      }
      return <HttpError { ...error } title={ errorTitle }/>
    }
    const { articles } = this.props;
    if (!articles) {
      return <Skeleton active/>
    }
    if (articles.length === 0) {
      return <Empty style={ { marginTop: '10px' } } description="暂无文章"/>
    }
    return (<>
      {
        articles.map((article, idx) => {
          return (
              <div key={ article.id } className="article-list">
                <h2><Link to={ `/articles/${ article.id }` } dangerouslySetInnerHTML={ { __html: article.title } }/></h2>
                { isNotEmpty(article.labels) &&
                <span className="tags">{ article.labels.map((label, idx) => {
                  return <Link key={ idx } to={ `/tags/${ label.name }` } className={ getRandLabel() } title={ label.name }>{ label.name }</Link>
                }) }
                </span>
                }
                <span className="summary" dangerouslySetInnerHTML={ { __html: article.summary } }
                      onClick={ () => this.props.history.push(`/articles/${ article.id }`) }/>
                { isNotEmpty(article.image) &&
                <span className="img">
                  <Image alt={ article.summary } src={ article.image } original={ false }/>
                </span>
                }
                <span className="info">
                  { new Date(article.id).toLocaleString() } | <span className="read-num"> 阅读数 <span className="num">{ article.pv }</span></span>
                </span>
                <hr/>
              </div>
          )
        })
      }
    </>)
  }
}

export default connect(
    userSessionMapStateToProps, { updateUserSession }
)(withRouter(ArticleList))
