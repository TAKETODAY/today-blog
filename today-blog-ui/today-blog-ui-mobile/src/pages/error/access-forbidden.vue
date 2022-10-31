<template>
  <div>
    <error desc="服务器拒绝处理您的请求！您可能没有访问此操作的权限 !"/>
  </div>
</template>

<script>
import Error from "./error";

export default {
  name: "access-forbidden",
  components: { Error },
  methods: {

  },
}
</script>

<style lang="less">

</style>
