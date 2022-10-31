<template>

  <!-- search -->
  <van-search v-model="keyword" placeholder="请输入搜索关键词" show-action @search="onSearch">
    <div slot="action" @click="onSearch">搜索</div>
    <img slot="left" width="50" height="30" alt="Logo" :src="logo" style="margin:0 5px 0 -10px">
  </van-search>

</template>

<script>
import logo from "assets/logo.svg";
import { Search } from "vant";

export default {
  name: "search-header",
  components: {
    [Search.name]: Search,
  },
  props: {
    title: String,
    backTo: String
  },
  data() {
    return {
      logo: logo,
      keyword: "", // 搜索关键词
    }
  },
  methods: {

    onSearch() {
      this.$router.push({
        path: "/search",
        query: { q: this.keyword }
      });
    },
    onBack() {
      if (this.backTo) {
        this.$router.push(this.backTo)
      }
      else {
        history.back();
      }
    }
  }
};
</script>

<style>
.book-list {
  background: #fff;
  padding: 10px;
  margin-bottom: 40px;
}

.category {
  text-align: center;
  padding-bottom: 10px;
  background-color: #fff;
}
</style>
