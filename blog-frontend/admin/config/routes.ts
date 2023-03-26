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

export default [
  {
    path: '/workplace',
    name: 'workplace',
    icon: 'smile',
    access: 'isLoggedIn',
    component: './workplace',
  },
  {
    path: '/articles',
    layout: false,
    access: 'isLoggedIn',
    routes: [
      {
        title: '写文章',
        path: '/articles/write',
        component: './articles/write',
      },
      {
        title: '写文章-富文本',
        path: '/articles/write-rich-text',
        component: './articles/write/rich-text',
      },
      {
        title: '修改文章',
        path: '/articles/:id/modify',
        component: './articles/modify',
      },
      {
        title: '修改文章-富文本',
        path: '/articles/:id/modify-rich-text',
        component: './articles/modify/rich-text',
      }
    ]
  },
  { // 博客
    name: 'blog',
    icon: 'profile',
    path: '/blog',
    access: 'isLoggedIn',
    routes: [
      {
        name: 'articles',
        icon: 'profile',
        path: '/blog/articles',
        component: './articles',
      },
      {
        name: 'categories',
        icon: 'profile',
        path: '/blog/categories',
        component: './articles/categories',
      },
      {
        name: 'labels',
        icon: 'profile',
        path: '/blog/labels',
        component: './articles/labels',
      },
      {
        name: 'comments',
        icon: 'smile',
        path: '/blog/comments',
        component: './articles/comments',
      },
    ]
  },
  {
    name: 'attachment',
    icon: 'profile',
    access: 'isLoggedIn',
    path: '/attachment',
    routes: [
      {
        name: 'list',
        icon: 'smile',
        path: '/attachment/list',
        component: './attachment',
      },
    ],
  },
  {
    name: 'user',
    path: '/user',
    icon: 'user',
    access: 'isLoggedIn',
    routes: [
      {
        layout: false,
        access: 'none',
        path: '/user/login',
        component: './user/login',
      },
      {
        name: 'list',
        icon: 'user',
        path: '/user/list',
        access: 'isLoggedIn',
        component: './user/list',
      },
    ],
  },
  {
    name: 'system',
    path: '/system',
    icon: 'setting',
    access: 'isLoggedIn',
    routes: [
      {
        name: 'account',
        path: '/system/account',
        component: './system/account',
      },
      {
        name: 'logging',
        icon: 'log',
        path: '/system/logging',
        component: './system/logging',
      },
      {
        name: 'options',
        icon: 'options',
        path: '/system/options',
        component: './system/options',
      },
    ],
  },
  {
    path: '/',
    redirect: '/workplace',
    access: 'isLoggedIn',
  },
  {
    layout: false,
    path: "/not-found",
    component: './404',
  },
  {
    path: '/**',
    redirect: '/not-found',
  },

];
