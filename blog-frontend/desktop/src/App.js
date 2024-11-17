/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import React, { useEffect, useState } from 'react';
import { Redirect, Route, Switch, useLocation } from 'react-router-dom';

import { Footer, Header } from './components';
import { http } from 'core';
import { Layout } from 'antd';
import { ArticleLayout, SearchLayout } from './layouts';
import './App.css';
import { ArticleDetail, CategoriesDetail, ErrorPage, Home, LabelsDetail, Login, Search, UserInfo, UserSettings } from 'src/pages';
import { useUserSession } from "./components/hooks";

const { Content } = Layout;

export default () => {
  const location = useLocation();
  const [userSession] = useUserSession();
  const [previousLocation, setPreviousLocation] = useState(null);

  useEffect(() => {
    // 在 effect 中更新 previousLocation
    setPreviousLocation(prevLocation => {
      // 只有在 location 发生变化时才更新 previousLocation
      if (prevLocation !== location) {
        return location
      }
      return prevLocation
    })

    if (!userSession) {
      const referer = previousLocation ? (window.location.origin + previousLocation.pathname) : ''
      const id = setTimeout(() => {
        http.post(`/api/pv?referer=${referer}`)
      }, 3000)
      return () => clearTimeout(id)
    }
    //eslint-disable-next-line
  }, [location])

  return (
    <>
      <Layout>
        <Header/>
        <Content>
          <Switch>
            <Route path="/login" exact component={Login}/>
            <Route path={['/BadRequest', "/bad-request"]} exact>
              <ErrorPage status='400'/>
            </Route>
            <Route path={['/NotFound', "/not-found"]} exact>
              <ErrorPage status='404'/>
            </Route>
            <Route path={['/AccessForbidden', "/access-forbidden"]} exact>
              <ErrorPage status='403'/>
            </Route>
            <Route path={["/MethodNotAllowed", "/method-not-allowed"]} exact>
              <ErrorPage status='405'/>
            </Route>
            <Route path={['/InternalServerError', "/internal-server-error"]} exact>
              <ErrorPage status='500'/>
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
                  <Route exact path="/articles/:articleId" component={ArticleDetail}/>
                  <Route exact path="/categories/:categoryId" component={CategoriesDetail}/>
                  <Redirect to="/not-found"/>
                </Switch>
              </ArticleLayout>
            </Route>
          </Switch>
        </Content>
        <Footer/>
      </Layout>
    </>
  );
}

