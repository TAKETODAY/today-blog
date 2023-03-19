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

import React from 'react';
import { Switch } from 'react-router';
import { Redirect, Route } from 'react-router-dom';
import { Footer, Header } from './components';
import { Layout } from 'antd';
import { ArticleLayout, SearchLayout } from './layouts';
import './App.css';
import {
  Article, CategoriesDetail, Error, Home,
  LabelsDetail, Login, Search, UserInfo, UserSettings
} from './pages';

const { Content } = Layout;

export default class App extends React.Component {

  render() {
    return (<>
      <Layout>
        <Header/>
        <Content>
          <Switch>
            <Route path="/login" exact component={Login}/>
            <Route path='/BadRequest' exact>
              <Error status='400'/>
            </Route>
            <Route path={['/NotFound', "/not-found"]} exact>
              <Error status='404'/>
            </Route>
            <Route path={['/AccessForbidden', "/access-forbidden"]} exact>
              <Error status='403'/>
            </Route>
            <Route path={["/MethodNotAllowed", "/method-not-allowed"]} exact>
              <Error status='405'/>
            </Route>
            <Route path={['/InternalServerError', "/internal-server-error"]} exact>
              <Error status='500'/>
            </Route>
            <Route exact path="/search">
              <SearchLayout>
                <Search/>
              </SearchLayout>
            </Route>
            <Route path='/user/info' exact component={UserInfo}/>
            <Route path='/user/settings' exact component={UserSettings}/>
            <Route>
              <ArticleLayout>
                <Switch>
                  <Route exact path='/' component={Home}/>
                  <Route exact path="/tags/:tagsId" component={LabelsDetail}/>
                  <Route exact path="/articles/:articleId" component={Article}/>
                  <Route exact path="/categories/:categoryId" component={CategoriesDetail}/>
                  <Redirect to="/not-found"/>
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
