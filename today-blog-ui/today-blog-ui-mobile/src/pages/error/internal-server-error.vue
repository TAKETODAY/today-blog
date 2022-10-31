<template>
  <div>
    <error desc="服务器出错啦 !"/>
  </div>
</template>

<script>

import Error from "./error";

export default {
  name: "internal-server-error",
  components: { Error },
}
</script>

<style>

</style>
