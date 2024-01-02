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
  <div class="container">
    <!-- comment input -->
    <div class="comment-input">
      <van-field v-if="isLoggedIn" @keyup.enter="createComment"
                 v-model="inputComment" :placeholder="options['comment.placeholder']">
        <template #button>
          <van-button size="small" type="danger" @click="createComment">留言</van-button>
        </template>
        <template #left-icon>
          <img :src="loginUser.avatar"/>
        </template>
      </van-field>
      <van-button v-else size="small" type="danger" :to="loginBackUrl">请登录后留言</van-button>
    </div>
    <!-- comment list-->
    <van-empty v-if="isEmpty(comments)" description="暂无评论"/>
    <div v-else class="comment" v-for="comment in comments" :key="comment.id">
      <div class="info">
        <img class="avatar" :src="comment.user.avatar" width="36" height="36"/>
        <div class="right">
          <div class="name">
            <a :href="comment.user.site" target="_blank">{{ comment.user.name }}</a>
          </div>
          <div class="date">{{ moment(comment.id).fromNow() }}</div>
        </div>
      </div>
      <div class="content" v-html="comment.content"></div>
      <div class="control">
        <span class="comment-reply" @click="showCommentInput(comment)">
          <span>回复</span>
        </span>
      </div>
      <div class="reply">
        <div class="item" v-for="reply in comment.replies">
          <div class="reply-content">
            <span class="from-name">{{ comment.user.name }}</span><span>: </span>
            <span class="to-name">@{{ reply.user.name }}</span>
            <span v-html="reply.content"></span>
          </div>
          <div class="reply-bottom">
            <span>{{ moment(reply.id).fromNow() }}</span>
            <span class="reply-text" @click="showCommentInput(comment, reply)">
              <i class="iconfont icon-comment"></i>
              <span>回复</span>
            </span>
          </div>
        </div>
        <div class="write-reply" v-if="isNotEmpty(comment.replies)" @click="showCommentInput(comment)">
          <van-icon name="edit"/>
          <span class="add-comment">添加新评论</span>
        </div>
        <transition name="fade">
          <div class="input-wrapper" v-if="replyId === comment.id">
            <van-field class="gray-bg-input"
                       v-model="inputComment"
                       type="textarea"
                       :rows="3"
                       autofocus
                       maxlength="1000"
                       show-word-limit
                       placeholder="写下你的评论"
            />
            <div class="btn-control">
              <span class="cancel" @click="cancel">取消</span>
              <van-button class="btn" type="primary" @click="createComment">确定</van-button>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script>
import {Toast} from "vant";
import {commentService, userService} from 'src/services'
import {isNotEmpty, isEmpty} from 'src/utils'
import moment from "moment";
import {mapGetters} from "vuex";

export default {
  name: "comment",
  props: {
    comments: {
      type: Array,
      required: true
    },
    article: {},
    reload: Function
  },
  data() {
    return {
      loginUser: null,
      inputComment: '',
      replyId: null
    }
  },
  computed: {
    ...mapGetters({
      options: "options/options",
    }),
    loginBackUrl() {
      return `/login?forward=/articles/${this.article.uri}#comments`
    },
    isLoggedIn() {
      return this.loginUser !== null
    },
  },
  methods: {
    isEmpty,
    moment,
    isNotEmpty,
    /**
     * 点击取消按钮
     */
    cancel() {
      this.replyId = null
    },
    /**
     * 提交评论
     */
    createComment() {
      if (isEmpty(this.inputComment)) {
        Toast("留言不能为空")
      }
      else {
        // console.log(this.inputComment)
        commentService.createComment({
          content: this.inputComment,
          commentId: this.replyId,
          articleId: this.article.id,
        }).then(() => {
          this.replyId = null
          this.inputComment = ''
          this.reload(this.article.id)
        }).catch(res => {
          console.log(res)
          Toast("留言失败")
        })
      }
    },
    /**
     * 点击评论按钮显示输入框
     * item: 当前大评论
     * reply: 当前回复的评论
     */
    showCommentInput(item, reply) {
      this.replyId = item.id
      this.inputComment = reply ? `@${reply.user.name} ` : ''
    }
  },
  async mounted() {
    this.loginUser = await userService.getSession()
  }
}
</script>

<style lang="less">
@color-main: #409EFF;
@color-success: #67C23A;
@color-warning: #E6A23C;
@color-danger: #F56C6C;
@color-info: #909399;

@text-main: #303133;
@text-normal: #606266;
@text-minor: #909399; //次要文字
@text-placeholder: #C0C4CC;
@text-333: #333;

.container {
  box-sizing: border-box;

  .van-empty {
    padding: 10px 0;

    .van-empty__image {
      width: 80px;
      height: 80px;
    }
  }

  .comment-input {
    padding: 10px;
    font-size: 14px;
    text-align: center;

    .van-cell {
      background: #F8F8F8;
      padding: 0;
    }

    .van-button {
      border-radius: 0;
    }

    img {
      width: 30px;
    }
  }

  .comment {
    display: flex;
    flex-direction: column;
    padding: 10px;
    border-bottom: 1px solid #F2F6FC;
    word-wrap: break-word !important;
    white-space: normal;

    .info {
      display: flex;
      align-items: center;

      .avatar {
        border-radius: 50%;
      }

      .right {
        display: flex;
        flex-direction: column;
        margin-left: 10px;

        .name {
          font-size: 16px;
          color: @text-main;
          margin-bottom: 5px;
          font-weight: 500;
        }

        .date {
          font-size: 12px;
          color: @text-minor;
        }
      }
    }

    .content {
      font-size: 16px;
      color: @text-main;
      line-height: 20px;
      padding: 10px 0;
    }

    .control {
      display: flex;
      align-items: center;
      font-size: 14px;
      color: @text-minor;

      .like {
        display: flex;
        align-items: center;
        margin-right: 20px;
        cursor: pointer;

        &.active, &:hover {
          color: @color-main;
        }

        .iconfont {
          font-size: 14px;
          margin-right: 5px;
        }
      }

      .comment-reply {
        display: flex;
        align-items: center;
        cursor: pointer;

        &:hover {
          color: @text-333;
        }

        .iconfont {
          font-size: 16px;
          margin-right: 5px;
        }
      }
    }

    .reply {
      margin: 10px 0;
      border-left: 2px solid #DCDFE6;

      .item {
        margin: 0 10px;
        padding: 10px 0;
        border-bottom: 1px dashed #EBEEF5;

        .reply-content {
          display: flex;
          align-items: center;
          font-size: 14px;
          color: @text-main;

          .from-name {
            color: @color-main;
          }

          .to-name {
            color: @color-main;
            margin-left: 5px;
            margin-right: 5px;
          }
        }

        .reply-bottom {
          display: flex;
          align-items: center;
          margin-top: 6px;
          font-size: 12px;
          color: @text-minor;

          .reply-text {
            display: flex;
            align-items: center;
            margin-left: 10px;
            cursor: pointer;

            &:hover {
              color: @text-333;
            }

            .icon-comment {
              margin-right: 5px;
            }
          }
        }
      }

      .write-reply {
        display: flex;
        align-items: center;
        font-size: 14px;
        color: @text-minor;
        padding: 10px;
        cursor: pointer;

        &:hover {
          color: @text-main;
        }

        .el-icon-edit {
          margin-right: 5px;
        }
      }

      .fade-enter-active, fade-leave-active {
        transition: opacity 0.5s;
      }

      .fade-enter, .fade-leave-to {
        opacity: 0;
      }

      .input-wrapper {
        padding: 10px;

        .gray-bg-input, .el-input__inner {
          /*background-color: #67C23A;*/
        }

        .btn-control {
          display: flex;
          justify-content: flex-end;
          align-items: center;
          padding-top: 10px;

          .cancel {
            font-size: 16px;
            color: @text-normal;
            margin-right: 20px;
            cursor: pointer;

            &:hover {
              color: @text-333;
            }
          }

          .confirm {
            font-size: 16px;
          }
        }
      }
    }
  }
}
</style>


