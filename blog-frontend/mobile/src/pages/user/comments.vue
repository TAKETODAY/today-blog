<template>
  <div>
    <nav-header title="我的评论" fix/>


    <skeleton v-if="isLoading"/>
    <van-empty v-else-if="listIsEmpty" description="暂无"/>
    <div v-else class="comment">
      <div v-for="(comment, idx) in comment.data" :key="idx" class="comment-item">
        <!-- 列表 -->
        <div class="content" v-html="comment.content"></div>
        <van-tag plain type="primary" v-if="'CHECKED'===comment.status">通过审核</van-tag>
        <van-tag plain type="primary" v-if="'RECYCLE'===comment.status">已经丢入垃圾桶</van-tag>
        <van-tag plain type="primary" v-if="'CHECKING'===comment.status">等待审核</van-tag>
        <div class="date">{{ moment(comment.id).fromNow() }}</div>
      </div>
      <!-- 分页 -->
      <van-pagination @change="load"
                      v-model="comment.current"
                      :total-items="comment.all"
                      :page-count="comment.num" :items-per-page="comment.size"/>
    </div>

    <mobile-footer/>

  </div>

</template>

<script>
import { MobileFooter } from 'src/components'

import { userService } from "services";
import { isEmpty, requireLogin } from "utils";
import moment from "moment";

export default {
  name: "index",
  components: {
    MobileFooter
  },
  data() {
    return {
      user: {},
      loading: true,
      comment: {
        "all": 0,
        "current": 1,
        "data": [],
        "num": 0,
        "size": 10
      }
    }
  },
  computed: {

    listIsEmpty() {
      return isEmpty(this.comment.data)
    },
    isLoading() {
      return this.loading
    }
  },
  methods: {
    moment,
    load(page = 1) {
      this.loading = true
      userService.getUserComments(page).then(res => {
        this.comment = res.data
      }).catch(err => {
        requireLogin(this, err)
      }).finally(() => this.loading = false)
    }
  },
  async created() {
    this.user = await userService.getSession()
    this.load()
  }
}
</script>

<style lang="less">

.date {
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.comment {
  display: flex;
  flex-direction: column;
  padding: 10px;
  border-bottom: 1px solid #F2F6FC;
  word-wrap: break-word !important;
  white-space: normal;
  background: #FFFFFF;

  .comment-item {
    margin-bottom: 10px;
    border-bottom: 1px dashed #c8dde4;
  }
}
</style>
