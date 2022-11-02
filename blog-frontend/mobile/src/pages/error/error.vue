<template>
  <div>
    <van-empty :description="desc" :image="head"/>
    <van-button to="/" block>首页</van-button>
    <van-button to="/" type="primary" @click="back()" block>返回</van-button>
  </div>
</template>

<script>
import { isNotEmpty } from "utils";
import head from 'assets/images/error-head.png';

export default {
  name: "Error",
  props: {
    message: {
      type: String
    },
    desc: {
      type: String
    }
  },
  data() {
    return {
      head
    }
  },
  methods: {
    back() {
      const { forward } = this.$route.query
      if (isNotEmpty(forward)) {
        const { hash } = this.$route
        this.$router.push({ hash, path: forward ? forward : '/' })
      }
      else {
        this.$router.back()
      }
    }
  },
}
</script>

<style lang="less" scoped>

div {
  font-size: 16px;
}

</style>
