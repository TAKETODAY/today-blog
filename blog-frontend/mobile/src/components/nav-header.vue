<template>
  <van-nav-bar :title="title" left-arrow @click-left="onBack"/>

</template>

<script>
  import { NavBar } from "vant";

  export default {
    name: "nav-header",
    components: {
      [NavBar.name]: NavBar
    },
    props: {
      title: String,
      backTo: String
    },
    methods: {
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
</style>
