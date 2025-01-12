<!--
  - Copyright 2017 - 2025 the original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with this program. If not, see [https://www.gnu.org/licenses/]
  -->

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
        this.updateSession(res.data)
        const { hash } = this.$route
        const { forward } = this.$route.query
        this.$router.push({ hash, path: forward ? forward : '/user' })
      }).catch(err => {
        Toast(err.message)
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
