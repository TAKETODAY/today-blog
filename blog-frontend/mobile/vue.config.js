const moment = require('moment')
const webpack = require('webpack')
const CompressionPlugin = require('compression-webpack-plugin')
const LodashModuleReplacementPlugin = require('lodash-webpack-plugin')
// const UglifyJsPlugin = require('uglifyjs-webpack-plugin'); //去console插件


const production = process.env.NODE_ENV === 'production'
process.env.VUE_APP_VERSION = `${ moment().format('YYYY.MM.DD') }.${ moment().valueOf() }`

function isProduction() {
  return production
}

const path = require('path')

function resolve(dir) {
  // console.log(__dirname)
  return path.join(__dirname, dir)
}

module.exports = {
  outputDir: 'dist',
  // publicPath: isProduction() ? '/m/' : '/',
  publicPath: '/m/',
  productionSourceMap: false,
  // productionGzip: true,
  devServer: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
      },
      '/upload': {
        target: 'http://localhost:8080',
      },
    }
  },

  chainWebpack: (config) => {
    config.minify = {
      removeComments: true,
      collapseWhitespace: true,
      removeAttributeQuotes: true,
      minifyCSS: true,
      minifyJS: true,
      // more options:
      // https://github.com/kangax/html-minifier#options-quick-reference
    }
    config.resolve.alias
        // .set('@$', resolve('src'))
        .set('src', resolve('src'))
        .set('data', resolve('src/data'))
        .set('pages', resolve('src/pages'))
        .set('utils', resolve('src/utils'))
        .set('store', resolve('src/store'))
        .set('assets', resolve('src/assets'))
        .set('config', resolve('src/config'))
        .set('services', resolve('src/services'))
        .set('components', resolve('src/components'))

  },

  configureWebpack: (config) => {
    if (isProduction()) { //判断是生产环境

      const plugins = [
        new webpack.optimize.LimitChunkCountPlugin({
          maxChunks: 2,
        }),
        new CompressionPlugin({
          filename: '[file].gz[query]',
          algorithm: 'gzip',
          test: /\.(js|css)$/,// 匹配文件名
          threshold: 1024, // 对超过4k的数据压缩
          deleteOriginalAssets: false, // 不删除源文件
          minRatio: 0.8 // 压缩比
        }),
        new webpack.ContextReplacementPlugin(/moment[/\\]locale$/, /zh-cn/),
        new LodashModuleReplacementPlugin(),//优化lodash

        // new UglifyJsPlugin({
        //   uglifyOptions: {
        //     compress: {
        //       warnings: false,
        //       drop_console: true,//console
        //       drop_debugger: true,
        //       pure_funcs: ['console.log']//移除console
        //     },
        //   },
        //   sourceMap: false,
        //   parallel: true,
        // })
      ]

      config.plugins = [...plugins, ...config.plugins]
      config.optimization = {
        splitChunks: {
          cacheGroups: {
            // 提取公共模块
            commons: {
              chunks: 'all',
              minChunks: 2,
              maxInitialRequests: 5,
              minSize: 0,
              name: 'common'
            }
          }
        },
      }

    }

  },
};
