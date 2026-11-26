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

createRoot(document.getElementById('app')).render((
  <BrowserRouter future={{
    v7_startTransition: true,
    v7_relativeSplatPath: true
  }}>
    <Provider store={store}>
      <App/>
    </Provider>
  </BrowserRouter>
));
