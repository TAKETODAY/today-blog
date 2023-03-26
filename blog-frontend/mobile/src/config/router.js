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

import Vue from 'vue';
import Router from 'vue-router';
import { http, setTitle, startNProgress, stopNProgress } from "utils";
import { userService } from "services";

Vue.use(Router);

const routes = [
  {
    name: 'home',
    path: '/',
    component: () => import('../pages/index'),
    meta: {
      title: '首页',
    }
  },
  {
    path: '/articles/:id',
    component: () => import('../pages/article/detail'),
    meta: {
      title: '文章详情'
    }
  },
  {
    name: 'search',
    path: '/search',
    component: () => import('../pages/article/search'),
    meta: {
      title: '搜索'
    }
  },
  {
    name: 'categories',
    path: '/categories',
    component: () => import('src/pages/categories'),
    meta: {
      title: '分类',
    }
  },
  {
    path: '/categories/:name',
    component: () => import('src/pages/categories'),
    meta: {
      title: '分类',
    }
  },
  {
    name: 'tags',
    path: '/tags',
    component: () => import('src/pages/tags'),
    meta: {
      title: '标签',
    }
  },
  {
    path: '/tags/:name',
    component: () => import('src/pages/tags'),
    meta: {
      title: '标签',
    }
  },
  {
    name: 'user',
    path: '/user',
    component: () => import('../pages/user'),
    meta: {
      title: '用户中心',
      requireAuth: true,
    }
  },
  {
    name: 'user-info',
    path: '/user/info',
    component: () => import('../pages/user/info/detail'),
    meta: {
      title: '个人信息',
      requireAuth: true,
    }
  },
  {
    nme: 'users-comments',
    path: '/user/comments',
    component: () => import('../pages/user/comments'),
    meta: {
      title: '我的评论',
      requireAuth: true,
    }
  },
  {
    path: '/login',
    component: () => import('src/pages/account/login'),
    meta: {
      title: '登录'
    }
  },
  {
    path: '/change-password',
    component: () => import('src/pages/account/password'),
    meta: {
      title: '修改密码'
    }
  },
  {
    path: '/not-found',
    component: () => import('src/pages/error/not-found'),
    meta: {
      title: '页面不存在',
    }
  },
  {
    path: '/bad-request',
    component: () => import('src/pages/error/bad-request'),
    meta: {
      title: '错误请求',
    }
  },
  {
    path: '/internal-server-error',
    component: () => import('src/pages/error/internal-server-error'),
    meta: {
      title: '服务器内部错误',
    }
  },
  {
    path: '/access-forbidden',
    component: () => import('src/pages/error/access-forbidden'),
    meta: {
      title: '没有权限',
    }
  },
  {
    path: '/method-not-allowed',
    component: () => import('src/pages/error/method-not-allowed'),
    meta: {
      title: '您请求的方式不正确',
    }
  },
  {
    path: '*',
    component: () => import('src/pages/error/not-found'),
    meta: {
      title: '页面不存在',
    }
    // redirect: '/not-found'
  }
]

// add route path
// routes.forEach(route => {
//   route.path = route.path || '/' + (route.name || '');
// });

const router = new Router({
  routes,
  mode: 'history',
  base: '/m/',
  scrollBehavior(to, from, savedPosition) {
    if (to.hash) {
      return {
        selector: to.hash
      }
    }
    return savedPosition ? savedPosition : { x: 0, y: 0 }
  }
})

router.beforeEach(async (to, from, next) => {
  startNProgress()
  const { meta } = to;
  const { requireAuth } = meta
  if (requireAuth && !await userService.isLoggedIn()) {
    await router.push({ path: '/login', query: { forward: to.path } })
    return
  }
  const { title } = meta
  if (title) {
    setTitle(title)
  }
  // 继续路由
  next()
});

router.afterEach((/*to, from*/) => {
  stopNProgress()

  setTimeout(() => {
    http.post("/api/pv?referer=" + encodeURIComponent(location.href))
  }, 1500)

  // next()
})

// const originalPush = Router.prototype.push
// Router.prototype.push = function push(location) {
//   return originalPush.call(this, location).catch(err => err)
// }

export {
  router
};
