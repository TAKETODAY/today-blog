export default [
  {
    path: '/workplace',
    name: 'workplace',
    icon: 'smile',
    access: 'canAdmin',
    component: './workplace',
  },
  {
    path: '/articles',
    layout: false,
    access: 'canAdmin',
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
    access: 'canAdmin',
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
    access: 'canAdmin',
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
    access: 'canAdmin',
    routes: [
      {
        layout: false,
        path: '/user/login',
        component: './User/login',
      },
      {
        name: 'list',
        icon: 'user',
        path: '/user/list',
        component: './User/list',
      },
    ],
  },
  {
    name: 'system',
    path: '/system',
    icon: 'setting',
    access: 'canAdmin',
    routes: [
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
  // {
  //   path: '/admin',
  //   name: 'admin',
  //   icon: 'crown',
  //   access: 'canAdmin',
  //   component: './Admin',
  //   routes: [
  //     {
  //       path: '/admin/sub-page',
  //       name: 'sub-page',
  //       icon: 'smile',
  //       component: './workplace',
  //     },
  //   ],
  // },
  {
    path: '/',
    redirect: '/workplace',
    access: 'canAdmin',
  },
  {
    component: './404',
  },
];
