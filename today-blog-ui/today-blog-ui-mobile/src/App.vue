<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script>
import './assets/style/common.css';
import './assets/style/reset.css';
import 'nprogress/nprogress.css';
// import Vue from 'vue';
// import VueQrcode from '@chenfengyuan/vue-qrcode';

// Vue.component(VueQrcode.name, VueQrcode);
// <qrcode value="Hello, World!" :options="{ width: 200 }"></qrcode>

export default {
  name: 'app'
}
</script>
