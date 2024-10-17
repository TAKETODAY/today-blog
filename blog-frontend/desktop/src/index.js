import { ConfigProvider } from 'antd';
import zhCN from 'antd/es/locale/zh_CN';
import moment from 'moment';
import 'moment/locale/zh-cn';
import 'nprogress/nprogress.css';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './assets/css/main.css';
import { store } from './redux/store';
//import * as serviceWorker from './serviceWorker';
import initApplication from './init-app'

initApplication.forEach(init => {
  init(store)
})

moment.locale('zh-cn');

ReactDOM.render((
    <BrowserRouter>
      <ConfigProvider locale={ zhCN }>
        <Provider store={ store }>
          <App/>
        </Provider>
      </ConfigProvider>
    </BrowserRouter>
), document.getElementById('app'));

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
//serviceWorker.register();
//serviceWorker.unregister();
