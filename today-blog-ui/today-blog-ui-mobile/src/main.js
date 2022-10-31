import Vue from 'vue';
import App from './App.vue';
import { router } from 'src/config/router';
import components from 'src/config/components';
import { Dialog, Lazyload, Pagination, Toast } from 'vant';

import store from 'src/store'
import initApplication from 'src/utils/init-app'
import VueQrcode from '@chenfengyuan/vue-qrcode';

import moment from 'moment';
import 'moment/locale/zh-cn';

moment.locale('zh-cn');

Vue.component(VueQrcode.name, VueQrcode);

Vue.config.productionTip = false

Vue.use(Dialog);
Vue.use(Lazyload);
Vue.use(components);
Vue.use(Pagination);

initApplication.forEach(init => {
  init(store)
})

new Vue({
  store,
  router,
  el: '#app',
  filters: {},
  render: h => h(App)
});

