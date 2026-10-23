import { ConfigProvider } from 'antd';
import zhCN from 'antd/es/locale/zh_CN';
import moment from 'moment';
import 'moment/locale/zh-cn';
import 'nprogress/nprogress.css';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './assets/antd.less';
import './assets/css/main.css';
import { store } from './redux/store';
import initApplication from './init-app'

initApplication.forEach(init => {
  init(store)
})

moment.locale('zh-cn');

createRoot(document.getElementById('app')).render((
  <BrowserRouter future={{
    v7_startTransition: true,
    v7_relativeSplatPath: true
  }}>
    <ConfigProvider locale={zhCN}>
      <Provider store={store}>
        <App/>
      </Provider>
    </ConfigProvider>
  </BrowserRouter>
));
