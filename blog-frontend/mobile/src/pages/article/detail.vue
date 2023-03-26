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
    <div v-if="needPassword" class="password-input">
      <van-nav-bar title="需要访问密码" left-arrow @click-left="onBack" fixed>
        <template #right>
          <van-icon color="#5B6B73" name="share" size="18"/>
        </template>
      </van-nav-bar>
      <van-field v-model="key" center clearable label="访问密码" placeholder="请输入访问密码">
        <template #button>
          <van-button size="small" type="primary" @click="requirePassword">确定</van-button>
        </template>
      </van-field>
    </div>
    <div v-else class="article">
      <van-nav-bar :title="article.title" left-arrow @click-left="onBack" fixed>
        <template #right>
          <van-icon color="#5B6B73" name="share" size="18" @click="share.show = true"/>
        </template>
      </van-nav-bar>
      <van-share-sheet v-model="share.show" title="立即分享给好友" :options="share.options" @select="onShare"/>

      <div class="article-cover">
        <img v-if="article.image" :src="article.image" :alt="article.title"/>
      </div>

      <van-cell-group class="article-meta">
        <van-cell>
          <div class="article-title">
            <h3>{{ article.title }}</h3>
          </div>
          <!--          <van-tag v-for="(label, idx) in article.labels" :key="idx" plain style="margin-right: 10px;">-->
          <router-link v-for="(label, idx) in article.labels" :key="idx" :class="getRandLabel()" :to="`/tags/${label.name}`">{{ label.name }}</router-link>
          <!--          </van-tag>-->
        </van-cell>
        <van-cell title="发布时间" :value="moment(article.id).format('lll')"/>
        <van-cell is-link title="分类" :value="article.category" :to="`/categories/${article.category}`"/>
      </van-cell-group>

      <!-- info details-->
      <div class="article-info">
        <div class="info-title">文章正文</div>
        <div class="details markdown" v-html="article.content"/>
        <div class="copyright">{{ article.copyright }}</div>
        <div class="footer">
          <span>{{ article.pv }}次浏览</span>
        </div>
      </div>

      <!--comments-->
      <div class="comments-info" id="comments">
        <div class="info-title">互动</div>
        <div class="details">
          <comment :comments="comments" :reload="loadComments"/>
          <van-pagination v-if="showCommentsPagination"
                          @change="pageChangeComments"
                          v-model="commentsData.current"
                          :total-items="commentsData.total"
                          :page-count="commentsData.pages" :items-per-page="commentsData.size"/>
        </div>
      </div>
      <!--same categories books-->
      <div class="categories-info">
        <div class="info-title">相似文章</div>
        <div class="details">
          <article-list :articles="categoriesArticles" :loading="categoriesLoading"/>
        </div>
      </div>

    </div>
    <!-- 二维码-->
    <van-overlay v-if="share.overlay" :show="share.overlay" @click="share.overlay = false">
      <div class="qrcode-wrapper">
        <qrcode :value="share.qrcodeValue" :options="{ width: 300 }"/>
      </div>
    </van-overlay>

    <mobile-footer/>
  </div>
</template>

<script>
import Vue from 'vue';
import { Overlay, Rate, ShareSheet, Toast } from "vant";
import { articleService, commentService } from "src/services";
import { ArticleList, Comment, MobileFooter } from "src/components";
import { getRandLabel, isNotEmpty, logging, setClipboard, setTitle, shareQQ, shareQQZone, shareWeiBo } from "src/utils";
import loading from "assets/images/loading.gif";
import "assets/style/github.css";
import { handleError } from "utils/error-handler";
import moment from 'moment';
import zone from "assets/images/zone.png";
import lazyload from "utils/lazyload";

Vue.use(ShareSheet);
Vue.use(Rate);
Vue.use(Overlay);

const defaultArticle = {
  "pv": 0,
  "labels": [],
  "content": "",
  "updateAt": 0,
  "image": loading,
  "title": "正在加载...",
  "summary": "正在加载...",
  "category": "正在加载...",
  "copyright": "版权声明：本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。"
}

function getId(id) {
  if (id && id.endsWith('.html')) {
    return id.substr(0, id.length - '.html'.length)
  }
  return id
}

export default {
  components: {
    MobileFooter,
    ArticleList,
    Comment,
  },
  data() {
    return {
      commentsData: {
        data: [],
        total: 1,
        size: 10,
        pages: 1,
        current: 1
      },
      key: '',
      share: {
        show: false,
        overlay: false,
        qrcodeValue: '',
        options: [
          { name: 'QQ', icon: 'qq', share: shareQQ },
          { name: '微博', icon: 'weibo', share: shareWeiBo },
          { name: 'QQ空间', icon: zone, share: shareQQZone },
          { name: '复制链接', icon: 'link', share: (options) => setClipboard(options.url) },
          {
            name: '二维码', icon: 'qrcode', share: (options) => {
              this.share.overlay = true
              this.share.qrcodeValue = options.url
            }
          },
        ]
      },
      needPassword: false,
      article: defaultArticle,
      categoriesLoading: true,
      categoriesArticles: [],
    };
  },
  computed: {
    showCommentsPagination() {
      return isNotEmpty(this.commentsData.data)
    },
    comments() {
      return this.commentsData.data
    },
  },
  methods: {
    moment,
    getRandLabel,
    onBack() {
      this.$router.push(`/`)
    },
    onShare(option) {
      const article = this.article
      option.share && option.share({
        url: location.href,
        desc: article.title,
        image: article.image,
        summary: article.summary,
      })
      this.share.show = false;
    },
    requirePassword() {
      this.loadById(this.$route.params.id, this.key)
    },
    loadById(id, key = null) {
      id = getId(id)
      if (key == null) {
        key = sessionStorage.getItem("article-password:" + id)
      }
      this.commentsData.current = 1 // 重置加载评论页数，防止重新加载书籍时不存在评论页
      articleService.getById(id, key).then(res => {
        const article = res.data
        this.article = article
        this.needPassword = false
        setTitle(article.title)

        setTimeout(() => {
          articleService.updatePV(id)
        }, 1500);
        if (key) {
          sessionStorage.setItem("article-password:" + id, key)
        }
        this.categoriesLoading = true
        articleService.getByCategoryName(article.category).then(res => {
          this.categoriesArticles = res.data.data
        }).catch(handleError).finally(() => this.categoriesLoading = false)
        try {
          lazyload.init({
            offset: 100,
            throttle: 250,
            unload: false,
          })
        }
        catch (e) {
          logging("懒加载出错")
        }

        this.loadComments(id)
      }).catch((e) => {
        const { status } = e.response || { status: 0 }

        if (status === 403) {
          handleError(e)
          this.needPassword = true
        }
        else if (status === 404) {
          handleError(e)
          this.$router.push('/not-found')
        }
        else {
          this.$router.push('/internal-server-error')
          Toast("文章详情加载失败")
        }
      });
    },
    pageChangeComments(page) {
      this.loadComments()
    },
    loadComments(id = this.$route.params.id) {
      commentService.getComments(id, this.commentsData.current).then((res) => {
        this.commentsData = res.data
      }).catch((e) => {
        Toast("评论加载失败");
      }).finally(() => {

      })
    },
  },
  mounted() {
    this.loadById(this.$route.params.id)
  },
  beforeRouteUpdate(to, from, next) {
    this.article = defaultArticle
    this.loadById(to.params.id)
    next()
  }
};
</script>

<style lang="less">

.qrcode-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.password-input {
  margin: 120px auto;
}

.article {

  .article-meta {
    .collect {
      float: right;
      font-size: 0.4rem;
    }
  }

  .info-title {
    height: 44px;
    line-height: 44px;
    padding-left: 10px;
    border-left: 3px solid #ee0a24;
    font-size: 14px;
    font-weight: 700;
    margin: 10px 0 0 0;
    background: #fff;
  }

  .comments-info {
    background: #fff;

    &-title {
      height: 44px;
      line-height: 44px;
      text-align: center;
      font-size: 14px;
      font-weight: 700;
      margin: 10px;
    }
  }

  .active {
    color: #f44;
  }

  .article-cover {
    position: relative;
    padding: 10px;
    background: #fff;
    margin-top: 46px;
    text-align: center;
    max-width: 100%;
    overflow-x: auto;

    img {
      max-width: 100%;
      vertical-align: middle;
      border: 0;
      height: auto;
      -ms-interpolation-mode: bicubic;
      overflow: hidden;
      font-size: 12px;
    }
  }

  &-tag {
    font-size: 12px;
    border-top: 1px solid #e5e5e5;

    span {
      margin-right: 10px;
    }

    i {
      color: red;
      margin-right: 3px;
    }

    img {
      width: 12px;
      margin-right: 3px;
      margin-top: 6px;
    }
  }

  &-title {
    h1 {
      font-size: 0.28rem;
      line-height: 0.38rem;
      color: #000;
    }
  }

  &-cell-group {
    margin: 15px 0;

    .van-cell__value {
      color: #999;
    }
  }

  &-info-title {
    height: 44px;
    line-height: 44px;
    text-align: center;
    font-size: 14px;
    font-weight: 700;
    margin: 10px;
  }

  &-info {
    background: #fff;

    .details {
      padding: 10px;
      margin-bottom: 10px;
      font-size: 16px;

      word-wrap: break-word !important;
      white-space: normal;
    }

    .copyright {
      padding: 10px;

      font-size: 15px;
      color: #999898;
    }

    .footer {
      font-size: 15px;
      color: #999898;
      padding: 10px;
      text-align: right;
    }
  }
}

</style>
