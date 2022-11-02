<template>
  <van-tabbar route active-color="#337ab7"> <!--#ee0a24-->
    <van-tabbar-item icon="wap-home-o" to="/">首页</van-tabbar-item>
    <van-tabbar-item icon="wap-nav" to="/categories">分类</van-tabbar-item>
    <van-tabbar-item icon="label-o" to="/tags">标签</van-tabbar-item>
    <van-tabbar-item icon="contact" to="/user">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script>
  import { Tabbar, TabbarItem } from "vant";

  export default {
    name: "navigation",
    components: {
      [Tabbar.name]: Tabbar,
      [TabbarItem.name]: TabbarItem
    }
  }
</script>

