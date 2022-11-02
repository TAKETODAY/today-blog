<template>
  <div>
    <nav-header title="个人信息"/>
    <van-cell-group title="基础资料">

      <!--<van-cell title="修改个人信息"  is-link />-->
      <!--<van-cell title="修改登录密码"  is-link />-->
      <!--<van-cell title="修改绑定手机"  is-link />-->
      <!--<van-cell title="关联账号"  is-link />-->
      <!--<van-cell title="切换账号"  is-link to="/login" />-->

      <van-cell title="昵称" :value="user.name" @click="onShowNicknameDialog"/>
      <van-cell title="头像" @click="onImagePreview">
        <van-image width="24px" lazy-load :src="user.image"/>
      </van-cell>

      <van-cell title="更换头像" class="change-avatar">
        <file-upload v-model="form.image" max-count="1" label=" " @change="saveAvatar"/>
      </van-cell>

    </van-cell-group>

    <van-cell-group title="密保资料">
      <!--      <van-cell title="手机号" :value="user.phone"/>-->
      <van-cell title="密码" is-link to="/change-password"/>
    </van-cell-group>

    <van-dialog v-model="showNicknameDialog" :before-close="onShowNicknameDialogClose" show-cancel-button>
      <van-field :value="user.name" label="昵称" placeholder="请输入昵称" @input="inputNickname"/>
    </van-dialog>

  </div>
</template>

<script>
  import { mapActions } from 'vuex'
  import { ImagePreview, Toast } from 'vant'
  import { userService } from 'src/services'
  import { FileUpload } from "src/components"
  import { handleValidationError, requireLogin} from "src/utils"

  export default {
    components: { FileUpload },
    data() {
      return {
        user: {},
        showNicknameDialog: false,
        name: undefined,
        form: {
          avatar: '',
          gender: 1,
        }
      };
    },
    methods: {
      ...mapActions({
        updateSession: 'user/updateSession',
      }),
      inputNickname(value) {
        this.name = value;
      },
      onShowNicknameDialog() {
        this.showNicknameDialog = true;
        this.name = this.user.name;
      },
      onShowNicknameDialogClose(action, done) {
        if (action === 'confirm') {
          const that = this;
          userService.updateInfo(that.user.id, { name: that.name }).then(res => {
            that.user.name = that.name
            this.updateSession(res)
            Toast("修改成功")
          }).catch(err => {
            requireLogin(this, err)
            handleValidationError(err)
          }).finally(() => done())
        }
        else {
          done();
        }
      },
      onImagePreview() {
        ImagePreview([this.user.image]);
      },
      saveAvatar(avatar) {
        console.log(avatar)
        userService.updateAvatar(avatar).then(res => {
          this.user = res.data
          this.updateSession(res.data)
          Toast("修改成功")
        }).catch(err => {
          requireLogin(this, err)
        })
      },
    },
    async mounted() {
      this.user = await userService.getSession()
      this.form.gender = this.user.gender
    }
  }
</script>

<style lang="less">
  .change-avatar {
    padding-right: 0;

    .van-cell__title {
      width: 50px !important;
    }
  }


</style>
