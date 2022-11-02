<template>
  <div>
    <nav-header title="登录"/>
    <div>
      <div style="padding-top: 70px;">
        <van-cell-group>
          <van-field placeholder="请输入邮箱"
                     v-model="email"
                     :error-message="emailError"
                     label="邮箱"
          />
          <van-field placeholder="请输入密码"
                     v-model="password"
                     :error-message="passwordError"
                     label="密码"
                     type="password"
          />
        </van-cell-group>
        <div style="margin: 10px;">
          <van-button size="large" type="info" @click="login">登录</van-button>
        </div>


<!--        <van-panel title="友情提示">
          <van-cell>1.</van-cell>
          <van-cell>2.</van-cell>
        </van-panel>-->
      </div>
    </div>
  </div>
</template>

<script>
  import { Toast } from "vant";
  import { mapActions } from 'vuex'
  import { userService } from "src/services";
  import { handleValidationError } from "src/utils";

  export default {

    data() {
      return {
        email: '',
        password: '',
        // email: 'taketoday@foxmail.com',
        // password: '666',
        emailError: '',
        passwordError: ''
      }
    },
    methods: {
      ...mapActions({
        updateSession: 'user/updateSession',
      }),
      login() {
        userService.login({
          email: this.email,
          password: this.password
        }).then(res => {
          if (res.data.success) {
            this.updateSession(res.data.data)
            const { hash } = this.$route
            const { forward } = this.$route.query
            this.$router.push({ hash, path: forward ? forward : '/user' })
          }
          else {
            Toast(res.data.message)
          }
        }).catch(err => {
          handleValidationError(err, validation => {
            this.emailError = validation.email
            this.passwordError = validation.password
          })
        })
      }
    }

  }
</script>

<style>

</style>
