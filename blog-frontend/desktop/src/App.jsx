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

import React, { Suspense, useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';

import { Footer, Header } from './components';
import RouteLoading from './components/RouteLoading';
import { http } from 'core';
import { Layout } from 'antd';
import { ArticleLayout, SearchLayout } from './layouts';
import './App.css';
import { ArticleDetail, CategoriesDetail, ErrorPage, Home, LabelsDetail, Login, Search } from 'src/pages';
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
      const referer = previousLocation ?
          (window.location.origin + previousLocation.pathname + previousLocation.search) : ''

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
          <Suspense fallback={<RouteLoading />}>
            <Routes>
            <Route path="/login" element={<Login/>}/>
            <Route path="/BadRequest" element={<ErrorPage status='400'/>}/>
            <Route path="/bad-request" element={<ErrorPage status='400'/>}/>
            <Route path="/NotFound" element={<ErrorPage status='404'/>}/>
            <Route path="/not-found" element={<ErrorPage status='404'/>}/>
            <Route path="/AccessForbidden" element={<ErrorPage status='403'/>}/>
            <Route path="/access-forbidden" element={<ErrorPage status='403'/>}/>
            <Route path="/MethodNotAllowed" element={<ErrorPage status='405'/>}/>
            <Route path="/method-not-allowed" element={<ErrorPage status='405'/>}/>
            <Route path="/InternalServerError" element={<ErrorPage status='500'/>}/>
            <Route path="/internal-server-error" element={<ErrorPage status='500'/>}/>
            <Route path="/search" element={
              <SearchLayout>
                <Search/>
              </SearchLayout>
            }/>
            <Route path="/*" element={
              <ArticleLayout>
                <Routes>
                  <Route path="/" element={<Home/>}/>
                  <Route path="/tags/:tagsId" element={<LabelsDetail/>}/>
                  <Route path="/articles/:articleId" element={<ArticleDetail/>}/>
                  <Route path="/categories/:categoryId" element={<CategoriesDetail/>}/>
                  <Route path="*" element={<Navigate to="/not-found" replace/>}/>
                </Routes>
              </ArticleLayout>
            }/>
            </Routes>
          </Suspense>
        </Content>
        <Footer/>
      </Layout>
    </>
  );
}
