<template>
  <div>
    <nav-header title="修改密码"/>

    <div class="background">
      <div style="padding-top: 70px;">
        <van-cell-group>
          <van-field label="旧密码" type="password" placeholder="请输入旧密码" v-model="form.password" :error="error.password"/>
          <van-field label="新密码" type="password" placeholder="请输入新密码" v-model="form.newPassword" :error="error.newPassword"/>
          <van-field label="重复新密码" type="password" placeholder="请重复新密码" v-model="form.rePassword" :error="error.rePassword"/>
        </van-cell-group>
        <div style="margin: 10px;">
          <van-button size="large" type="info" @click="submit">确认</van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

  import { Toast } from "vant";
  import { userService } from "src/services";
  import { handleValidationError, requireLogin, isNotEmpty } from "src/utils";

  export default {
    data() {
      return {
        form: {
          password: null,
          rePassword: null,
          newPassword: null,
        },
        error: {
          password: null,
          rePassword: null,
          newPassword: null,
        }
      }
    },
    methods: {
      submit() {
        const form = this.form
        if (form.rePassword !== form.newPassword) {
          Toast("两次密码不一致")
          return
        }
        if (isNotEmpty(form.password) ||
            isNotEmpty(form.rePassword) ||
            isNotEmpty(form.newPassword)) {

          userService.updatePassword(form).then(res => {
            this.$router.back()
            Toast("密码修改成功")
          }).catch(err => {
            requireLogin(this, err)
            handleValidationError(err, v => {
              this.error = v
            })
          })
        }
      }
    }
  }
</script>

<style>
  .background {
    background-size: 161px;
  }
</style>
