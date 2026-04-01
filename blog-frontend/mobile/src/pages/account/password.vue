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
    <nav-header title="修改密码"/>

    <div class="background">
      <div style="padding-top: 70px;">
        <van-cell-group>
          <van-field label="旧密码" type="password" placeholder="请输入旧密码" v-model="form.oldPassword" :error="error.password"/>
          <van-field label="新密码" type="password" placeholder="请输入新密码" v-model="form.newPassword" :error="error.newPassword"/>
          <van-field label="重复新密码" type="password" placeholder="请重复新密码" v-model="form.confirmNewPassword"
                     :error="error.confirmNewPassword"/>
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
import { handleValidationError, isNotEmpty, requireLogin } from "src/utils";

export default {
  data() {
    return {
      form: {
        oldPassword: null,
        newPassword: null,
        confirmNewPassword: null,
      },
      error: {
        oldPassword: null,
        newPassword: null,
        confirmNewPassword: null,
      }
    }
  },
  methods: {
    submit() {
      const form = this.form
      if (form.confirmNewPassword !== form.newPassword) {
        Toast("两次密码不一致")
        return
      }
      if (isNotEmpty(form.oldPassword) ||
          isNotEmpty(form.confirmNewPassword) ||
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
