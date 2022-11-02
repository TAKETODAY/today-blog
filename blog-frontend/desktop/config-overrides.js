const { override, fixBabelImports, disableChunk, disableEsLint, addWebpackAlias, addLessLoader } = require('customize-cra');
const path = require('path')
const { addWebpackPlugin } = require("customize-cra");
const CompressionPlugin = require('compression-webpack-plugin')
const webpack = require('webpack')
const LodashModuleReplacementPlugin = require('lodash-webpack-plugin')

const resolveAlias = dir => path.join(__dirname, '.', dir)
//生产环境去除console.* functions
const dropConsole = () => {
  return config => {
    if (config.optimization.minimizer) {
      config.optimization.minimizer.forEach(minimizer => {
        if (minimizer.constructor.name === 'TerserPlugin') {
          minimizer.options.terserOptions.compress.drop_console = true
        }
      })
    }
    return config
  }
}

const rewiredSourceMap = () => config => {
  config.devtool = config.mode === 'development' ? 'cheap-module-source-map' : false;
  return config;
};

process.env.GENERATE_SOURCEMAP = "false"
module.exports = {
  webpack: override(
      rewiredSourceMap(),
      fixBabelImports('import', {
        libraryName: 'antd',
        libraryDirectory: 'es',
        style: true,
      }),
      dropConsole(),
      disableChunk(),
      disableEsLint(),
      addWebpackAlias({
        '@': resolveAlias('src'),
        'src': resolveAlias('src'),
      }),
      addWebpackPlugin(
          new CompressionPlugin({
            filename: '[file].gz[query]',
            algorithm: 'gzip',
            test: /\.(js|css|svg)$/,// 匹配文件名
            threshold: 1024, // 对超过4k的数据压缩
            deleteOriginalAssets: false, // 不删除源文件
            minRatio: 0.8 // 压缩比
          }),
          new webpack.ContextReplacementPlugin(/moment[/\\]locale$/, /zh-cn/),
          new LodashModuleReplacementPlugin(),//优化lodash
      ),
      addLessLoader({
        javascriptEnabled: true,
        modifyVars: {
          '@primary-color': '#337ab7',
          // '@success-color': '#1e8e3e',
          // '@info-color': '@primary-color',
          // '@warning-color': '#ffc440',
          // '@error-color': '#d93026',
          // '@processing-color': '@primary-color',
          // '@text-color': 'fade(#000, 65%)',
          // '@text-color-secondary': 'fade(#000, 45%)',
          // '@border-color-base': '#dedede',
          // '@border-color-split': '#dedede',
          // '@layout-body-background': '#fafafa',
          // '@font-size-base': '12px',
          // '@border-radius-base': '0',
          // '@border-radius-sm': '0',
          // '@outline-width': '0',
          // '@outline-color': '#737373',
          // '@background-color-base': 'hsv(0, 0, 96%)',
          // '@btn-default-bg': '#fafafa',
          // '@btn-default-border': '#dedede',
          // '@radio-button-bg': 'transparent',
          // '@radio-button-checked-bg': 'transparent',
          // '@form-item-margin-bottom': '16px',
          // '@input-height-lg': '36px',
          // '@input-hover-border-color': '#737373',
          // '@progress-radius': '0',
          // '@table-header-bg': '#fafafa',
          // '@table-row-hover-bg': '#fafafa',
          // '@table-padding-vertical': '15px',
          // '@tabs-card-gutter': '-1px',
          // '@tabs-card-tab-active-border-top': '2px solid @primary-color',
          // '@switch-color': '@success-color',
          // '@breadcrumb-base-color': '@text-color',
          // '@breadcrumb-last-item-color': '@text-color-secondary',
          // '@slider-handle-border-width': '1px',
          // '@slider-handle-shadow': '1px 1px 4px 0 rgba(0,0,0,.13)',
          // '@slider-track-background-color': '@primary-color',
          // '@slider-track-background-color-hover': '@primary-color',
          // '@slider-handle-color': '#dedede',
          // '@slider-handle-color-hover': '#dedede',
          // '@slider-handle-color-focus': '#dedede',
          // '@slider-handle-color-focus-shadow': 'transparent',
          // '@slider-handle-color-tooltip-open': '#ddd',
          // '@alert-success-border-color': '#dff4e5',
          // '@alert-success-bg-color': '#dff4e5',
          // '@alert-info-border-color': '#e5f3ff',
          // '@alert-info-bg-color': '#e5f3ff',
          // '@alert-warning-border-color': '#fff7db',
          // '@alert-warning-bg-color': '#fff7db',
          // '@alert-error-border-color': '#fcebea',
          // '@alert-error-bg-color': '#fcebea'
        }
      })
  )
}
