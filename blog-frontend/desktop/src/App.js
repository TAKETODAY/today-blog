import React from 'react';
import { Switch } from 'react-router';
import { Redirect, Route } from 'react-router-dom';
import './App.css';
import { Footer, Header } from './components';
import { ArticleLayout, SearchLayout } from './layouts';
import { Article, CategoriesDetail, Error, Home, LabelsDetail, Login, Search, UserInfo, UserSettings } from './pages';
import { Layout } from 'antd';

const { Content } = Layout;

export default class App extends React.Component {

  render() {
    return (<>
      <Layout>
        <Header/>
        <Content>
          <Switch>
            <Route path="/login" exact component={ Login }/>
            <Route path='/BadRequest' exact>
              <Error status='400'/>
            </Route>
            <Route path='/NotFound' exact>
              <Error status='404'/>
            </Route>
            <Route path='/AccessForbidden' exact>
              <Error status='403'/>
            </Route>
            <Route path='/MethodNotAllowed' exact>
              <Error status='405'/>
            </Route>
            <Route path='/InternalServerError' exact>
              <Error status='500'/>
            </Route>
            <Route exact path="/search">
              <SearchLayout>
                <Search/>
              </SearchLayout>
            </Route>
            <Route path='/user/info' exact component={ UserInfo }/>
            <Route path='/user/settings' exact component={ UserSettings }/>
            <Route>
              <ArticleLayout>
                <Switch>
                  <Route exact path='/' component={ Home }/>
                  <Route exact path="/tags/:tagsId" component={ LabelsDetail }/>
                  <Route exact path="/articles/:articleId" component={ Article }/>
                  <Route exact path="/categories/:categoryId" component={ CategoriesDetail }/>
                  <Redirect to="/NotFound"/>
                </Switch>
              </ArticleLayout>
            </Route>
          </Switch>
        </Content>
        <Footer/>
      </Layout>
    </>)
  }
}
