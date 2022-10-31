<template>
  <div>
    <div class="user-profile">
      <div>
        <van-icon class="setting" name="setting" size="24"/>
      </div>
      <van-cell class="details" to="/user/info" :label="userProfile">
        <template #icon>
          <span class="avatar">
            <img :src="user.image" :alt="user.name"/>
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
