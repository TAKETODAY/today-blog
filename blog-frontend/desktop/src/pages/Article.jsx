import { LockOutlined } from '@ant-design/icons';
import { Form, Input, message, Skeleton } from 'antd';
import React from 'react';
import { Link, withRouter } from 'react-router-dom';
import qq from '../assets/images/share/qq.png';
import weibo from '../assets/images/share/weibo.png';
import zone from '../assets/images/share/zone.png';
import { ArticleComment } from '../components';
import { articleService } from '../services';
import {
  applySEO,
  getArticleId,
  getRandLabel,
  getSummary,
  isEmpty,
  isNotEmpty,
  logging,
  scrollTop,
  setTitle,
  shareQQ,
  shareQQZone,
  shareWeiBo
} from '../utils';
import { connect } from "react-redux";
import { navigationsUserSessionMapStateToProps } from "src/redux/action-types";
import { updateNavigations } from "../redux/actions";
import { store } from "../redux/store";
import lazyload from "src/utils/lazyload";

const passwordRules = [
  { required: true, message: '请输入访问密码!' }
]

function buildOptions(state) {
  const { article } = state
  const host = store.getState().options["site.host"];
  return {
    url: `${ host }/articles/${ article.id }`,
    desc: article.title,
    image: article.image,
    summary: article.summary,
  }
}

function setSEO(article) {
  try {
    let content = `${ article.title },${ article.category }`
    if (isNotEmpty(article.labels)) {
      article.labels.forEach(label => {
        content += ',' + label.name
      })
    }
    applySEO(content, isEmpty(article.summary) ? getSummary(article.content) : article.summary)
  }
  catch (e) {
    logging(e)
  }
}


class Article extends React.Component {

  /**
   {
      url: null,
      title: null,
      category: null,
      content: null,
      copyRight: null,
      image: null,
      labels: [],
      lastModify: 0,
      markdown: null,
      pv: 0,
      summary: null
    }
   */
  state = {
    key: null,
    article: null,
    needPassword: false,
  }

  componentDidMount() {
    this.loadArticle(this.props.match.params.articleId)
  }

  loadArticle(articleId, key = null) {
    articleId = getArticleId(articleId)

    super.setState({ article: null, comments: null });
    scrollTop()
    if (key == null) {
      key = sessionStorage.getItem("article-password:" + articleId)
    }
    articleService.getById(articleId, key).then(res => {
      const article = res.data;
      super.setState({ article, needPassword: false });
      setTitle(article.title)
      setSEO(article)
      this.updateNavigations()
      setTimeout(() => {
        articleService.updatePageView(articleId)
      }, 1500);
      if (key) {
        sessionStorage.setItem("article-password:" + articleId, key)
      }
      try {
        lazyload.init({
          offset: 100,
          throttle: 250,
          unload: false,
        })
      }
      catch (e) {
        logging("懒加载出错")
      }
    }).catch(err => {
      const { status } = err.response || { status: 0 }
      if (status === 403) {
        message.error(err.response.data.message)
        this.setState({ needPassword: true })
      }
      else if (status === 404) {
        this.props.history.push("/NotFound");
      }
      else {
        this.props.history.push("/InternalServerError");
      }
    })
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.match.params.articleId) {
      const { articleId } = this.props.match.params
      if (articleId !== nextProps.match.params.articleId) {
        this.loadArticle(nextProps.match.params.articleId)
      }
    }
  }

  updateNavigations() {

    const navigations = [
      { name: '全部博客', url: '/' },
      { name: this.state.article.category, url: `/categories/${ this.state.article.category }` },
      { name: this.state.article.title, url: `/articles/${ this.state.article.id }` }
    ]

    this.props.updateNavigations(navigations)
  }

  requirePassword = (formData) => {
    const { articleId } = this.props.match.params
    this.loadArticle(articleId, formData.key)
  }

  shareQQ = () => {
    shareQQ(buildOptions(this.state))
  }

  shareQQZone = () => {
    shareQQZone(buildOptions(this.state))
  }

  shareWeiBo = () => {
    shareWeiBo(buildOptions(this.state))
  }

  renderPassword() {
    return (<>
      <div className="data_list">
        <Form name="requirePassword" onFinish={ this.requirePassword }>
          <Form.Item name="key" rules={ passwordRules } validateStatus="validating">
            <Input autoFocus size="large" placeholder="请输入访问密码" prefix={ <LockOutlined/> }/>
          </Form.Item>
        </Form>
      </div>
    </>)
  }

  render() {
    const { article, needPassword } = this.state
    if (needPassword) {
      return this.renderPassword()
    }
    if (isEmpty(article)) { // loading
      return <Skeleton active/>
    }
    const { userSession } = this.props
    return (<>

      <div className="data_list">
        <article className="articleContent blog_content">
          <h1 className="title" style={ { margin: '0px 0 50px 0' } }>{ article.title }</h1>
          <div className="property">
            <span>发布于: { new Date(article.id).toLocaleString() }</span> |
            <span> 分类: <Link to={ `/categories/${ article.category }` } title={ article.category }>{ article.category }</Link></span> |
            <span> 浏览: { article.pv } </span>
            { userSession?.blogger && <>
              | <span><a href={ `/blog-admin/#/articles/${ article.id }/modify` }
                         target='_blank'>编辑此页</a> </span></>
            }
          </div>
          <div className="markdown" id="contentTxt" dangerouslySetInnerHTML={ { __html: article.content } }/>
        </article>
        { isNotEmpty(article.labels) &&
        <div className="blog_keyWord" id="tagcloud">
          <strong>标签：</strong>
          { article.labels.map((label, idx) => {
            return <Link key={ idx } to={ `/tags/${ label.name }` } className={ getRandLabel() }>{ label.name }</Link>
          }) }
        </div>
        }
        <div id="article_copyRight">{ article.copyRight }
          <div style={ { textAlign: "right" } }>
            分享： <img onClick={ this.shareQQ } className="share" title="分享到QQ好友" src={ qq } width="18" alt="分享到QQ好友"/>
            <img onClick={ this.shareQQZone } className="share" title="分享到QQ空间" src={ zone } width="20" alt="分享到QQ空间"/>
            <img onClick={ this.shareWeiBo } className="share" title="分享到微博" src={ weibo } width="20" alt="分享到微博"/>
          </div>
        </div>
      </div>
      <ArticleComment articleId={ this.props.match.params.articleId }/>
    </>)
  }
}

export default connect(
    navigationsUserSessionMapStateToProps, { updateNavigations }
)(withRouter(Article))


