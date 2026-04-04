/**
 * @name umi 的路由配置
 * @description 只支持 path,component,routes,redirect,wrappers,name,icon 的配置
 * @param path  path 只支持两种占位符配置，第一种是动态参数 :id 的形式，第二种是 * 通配符，通配符只能出现路由字符串的最后。
 * @param component 配置 location 和 path 匹配后用于渲染的 React 组件路径。可以是绝对路径，也可以是相对路径，如果是相对路径，会从 src/pages 开始找起。
 * @param routes 配置子路由，通常在需要为多个路径增加 layout 组件时使用。
 * @param redirect 配置路由跳转
 * @param wrappers 配置路由组件的包装组件，通过包装组件可以为当前的路由组件组合进更多的功能。 比如，可以用于路由级别的权限校验
 * @param name 配置路由的标题，默认读取国际化文件 menu.ts 中 menu.xxxx 的值，如配置 name 为 login，则读取 menu.ts 中 menu.login 的取值作为标题
 * @param icon 配置路由的图标，取值参考 https://ant.design/components/icon-cn， 注意去除风格后缀和大小写，如想要配置图标为 <StepBackwardOutlined /> 则取值应为 stepBackward 或 StepBackward，如想要配置图标为 <UserOutlined /> 则取值应为 user 或者 User
 * @doc https://umijs.org/docs/guides/routes
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
