<template>
  <div>
    <error desc="错误请求 !"/>
  </div>
</template>

<script>
import Error from "./error";

export default {
  name: "bad-request",
  components: { Error },
}
</script>

<style lang="less">
</style>
