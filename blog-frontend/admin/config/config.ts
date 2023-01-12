// https://umijs.org/config/
import { defineConfig } from 'umi';
import defaultSettings from './defaultSettings';
import proxy from './proxy';
import routes from './routes';

const CompressionPlugin = require('compression-webpack-plugin')

const { REACT_APP_ENV } = process.env;
const production = process.env.NODE_ENV === 'production'

export function isNotNull(object: any) {
  return !(object === null || object === undefined)
}

export default defineConfig({
  hash: true, // 配置是否让生成的文件包含 hash 后缀，通常用于增量发布和避免浏览器加载缓存。
  antd: {},

  dva: {
    hmr: true,
  },
  title: 'TODAY BLOG',
  history: {
    type: 'hash'
  },
  // base: '/blog-admin/',
  // publicPath: '/blog-admin/',
  publicPath: '/',
  layout: {
    // https://umijs.org/zh-CN/plugins/plugin-layout
    locale: true,
    siderWidth: 208,
    ...defaultSettings,
  },
  // https://umijs.org/zh-CN/plugins/plugin-locale
  locale: {
    // default zh-CN
    default: 'zh-CN',
    antd: true,
    // default true, when it is true, will use `navigator.language` overwrite default
    baseNavigator: true,
  },
  targets: {
    ie: 11,
  },
  // umi routes: https://umijs.org/docs/routing
  routes,
  // Theme for antd: https://ant.design/docs/react/customize-theme-cn
  theme: {
    'primary-color': defaultSettings.primaryColor,
  },
  // esbuild is father build tools
  // https://umijs.org/plugins/plugin-esbuild
  esbuild: {},
  ignoreMomentLocale: true,
  proxy: proxy[REACT_APP_ENV || 'dev'],
  manifest: {
    basePath: '/',
  },
  // Fast Refresh 热更新
  fastRefresh: {},
  nodeModulesTransform: { type: 'none' },
  // mfsu: {},
  webpack5: {},
  // exportStatic: {},
  // chunks: ['vendors', 'umi'],
  dynamicImport: {
    loading: '@ant-design/pro-layout/es/PageLoading',
  },
  chainWebpack: function (config, { webpack }) {
    if (isNotNull(this.dynamicImport)) {
      console.log("dynamicImport", this.dynamicImport)
      console.log("ENV", REACT_APP_ENV)
      config.merge({
        optimization: {
          splitChunks: {
            chunks: 'all',
            minSize: 30000,
            minChunks: 2,
            automaticNameDelimiter: '.',
            cacheGroups: {
              vendor: {
                name: 'vendors',
                // @ts-ignore
                test({ resource }) {
                  return /[\\/]node_modules[\\/]/.test(resource);
                },
                priority: 10,
              },
            },
          },
        }
      })

      if (production) {
        config.plugin('LimitChunkCountPlugin').use(require("webpack").optimize.LimitChunkCountPlugin, [
          {
            maxChunks: 1,
          },
        ])
      }
    }

    if (production) {

      //gzip压缩
      config.plugin('compression-webpack-plugin').use(CompressionPlugin, [
        {
          test: /\.js$|\.html$|\.css$/, //匹配文件名
          threshold: 10240, //对超过10k的数据压缩
          deleteOriginalAssets: false, //不删除源文件
        },
      ]);

      //过滤掉momnet的那些不使用的国际化文件
      config.plugin("replace").use(require("webpack").ContextReplacementPlugin).tap(() => {
        return [/moment[/\\]locale$/, /zh-cn/];
      })
    }

  }
});
