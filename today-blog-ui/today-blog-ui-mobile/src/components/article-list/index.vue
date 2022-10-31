<template>
  <div>
    <van-skeleton v-if="loading" title :row="3" class="skeleton"/>
    <van-empty v-else-if="showEmpty" description="暂无文章"/>
    <div v-else class="article-list">
      <div v-for="article in articles" class="article">
        <h2>
          <router-link :to="`/articles/${article.id}`">{{ article.title }}</router-link>
        </h2>
        <div class="tags">
          <router-link :key="idx" v-for="(label ,idx) in article.labels"
                       :class="getRandLabel()" :to="`/tags/${ label.name }`" :title="label.name">{{ label.name }}
          </router-link>
        </div>
        <div class="summary" @click="showDetail(article.id)">
          {{ article.summary }}
        </div>
        <div class="img" v-if="article.image">
          <van-image lazy-load :src="article.image" @click="imagePreview(article.image)">
            <template v-slot:loading>
              <van-loading type="spinner" size="30"/>
            </template>
            <template v-slot:error>加载失败</template>
          </van-image>
        </div>
        <div class="info">
          {{ new Date(article.id).toLocaleString() }} | <span class="read-num"> 阅读数 <span class="num">{{ article.pv }}</span></span>
        </div>
        <hr/>
      </div>
    </div>
  </div>
</template>


<script>
import Vue from 'vue';
import { Image as VanImage, ImagePreview, Tag } from 'vant';

import { getRandLabel, isEmpty, isNotEmpty } from 'src/utils';

Vue.use(Tag);
Vue.use(ImagePreview);
Vue.use(VanImage);

export default {
  name: "article-list",
  components: {},
  props: {
    loading: {
      type: Boolean,
    },
    articles: {
      type: Array,
    }
  },
  data() {
    return {}
  },
  computed: {
    showEmpty() {
      return !this.loading && isEmpty(this.articles);
    },
  },
  mounted() {
  },
  methods: {
    isEmpty,
    isNotEmpty,
    getRandLabel,
    imagePreview(image) {
      ImagePreview([image])
    },
    showDetail(id) {
      this.$router.push(`/articles/${ id }`)
    },
  }
}
</script>

<style lang="less">
.skeleton {
  padding-bottom: 10px;
}

.article-list {
  background: #fff;

  .article {
    padding: 10px;
  }

  .num {
    color: #3399ea;
  }

  .read-num {
    color: #999;
  }

  .info {
    text-align: right;
  }

  .tags {
    margin-bottom: 10px;
  }

  h2 {
    margin-bottom: 10px;
  }

  .img {
    margin: 10px 0 10px 0;
  }

  .summary {
    font-size: 15px;
    color: #6b6b6b;
    word-wrap: break-word !important;
    white-space: normal;
  }

  hr {
    border: none;
    border-top: 1px dashed #c8dde4;
    margin-bottom: 0;
  }
}


</style>
