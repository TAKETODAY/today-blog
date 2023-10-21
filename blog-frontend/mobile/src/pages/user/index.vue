<!--
  - Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
  - Copyright © TODAY & 2017 - 2023 All Rights Reserved.
  -
  - DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
  - along with this program.  If not, see [http://www.gnu.org/licenses/]
  -->

<template>
  <div>
    <div class="user-profile">
      <div>
        <van-icon class="setting" name="setting" size="24"/>
      </div>
      <van-cell class="details" to="/user/info" :label="userProfile">
        <template #icon>
          <span class="avatar">
            <img :src="user.avatar" :alt="user.name"/>
          </span>
        </template>
        <template #title>
          <span class="user-name">{{ user.name }}</span>
          <van-tag type="warning" v-if="user.title">{{ user.title }}</van-tag>
        </template>
      </van-cell>

    </div>

    <van-cell-group class="user-group">
      <van-cell title="我的评论" value="更多" is-link to="/user/comments"/>
    </van-cell-group>

<!--
    <van-cell-group class="user-group">
      <van-cell title="更多工具"/>
      <van-row class="user-links">
        <van-col span="6">
          <router-link to="/user/address">
            <van-icon name="location-o"/>
            <div>收货地址</div>
          </router-link>
        </van-col>
        <van-col span="6">
          <router-link to="/user/publish">
            <van-icon name="shop-o"/>
            <div>我发布的</div>
          </router-link>
        </van-col>
        <van-col span="6">
          <router-link to="/user/publish/orders">
            <van-icon name="logistics"/>
            <div>发货管理</div>
          </router-link>
        </van-col>
      </van-row>
    </van-cell-group>
-->

    <van-cell-group>
      <van-cell title="退出登录" @click="logout"/>
    </van-cell-group>
    <navigation/>
  </div>
</template>

<script>

import { Dialog } from 'vant'
import { requireLogin } from "src/utils"
import { userService } from "src/services"

export default {
  components: {},
  computed: {
    userProfile() {
      return '账号ID: ' + this.user.id
    },
  },
  data() {
    return {
      user: {},
    }
  },
  methods: {
    logout: function () {
      Dialog.confirm({
        title: '确定退出',
        message: '您确定要安全退出吗?',
        cancelButtonText: '手滑了'
      }).then(() => {
        userService.logout().then(() => {
          this.$router.push('/');
        })
      }).catch(() => {
      })
    }
  },
  async created() {
    this.user = await userService.getSession()
  }
};
</script>

<style lang="less">
.user {
  &-profile {
    width: 100%;
    height: 120px;
    background-color: #f1f5fa;
    background-repeat: no-repeat;
    background-size: 100% 100%;

    img {
      border-radius: 50%;
      width: 46px;
    }

    .van-cell__title {
      margin-left: 5px;

      .van-cell__label {
        margin-top: 0;
      }
    }

    .details {
      background-color: transparent;

      padding: 10px;

    }

    .setting {
      font-size: 24px;
      width: 24px;
      margin-left: auto;
      padding: 10px;
      position: relative;
      display: inherit;
    }

    .user-name {
      font-weight: bold;
      font-size: 18px;
    }

  }

  &-group {
    margin-bottom: .3rem;

    .van-cell__value {
      color: #999;
      font-size: 12px;
    }
  }

  &-links {
    padding: 15px 0;
    font-size: 12px;
    text-align: center;
    background-color: #fff;

    span {
      font-size: 30px;
      font-weight: bold;
    }

    .category {
      color: #999;
    }

    .van-icon {
      position: relative;
      width: 24px;
      font-size: 24px;
    }
  }
}
</style>
