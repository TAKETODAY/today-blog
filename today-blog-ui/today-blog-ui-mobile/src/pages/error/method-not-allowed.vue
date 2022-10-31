<template>
  <div>
    <error desc="您请求的方式不正确 !"/>
  </div>
</template>

<script>
import Error from "./error";

export default {
  name: "method-not-allowed",
  components: { Error },
}
</script>

<style>

</style>
