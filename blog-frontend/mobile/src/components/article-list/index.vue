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
    <van-skeleton v-if="loading" title :row="3" class="skeleton"/>
    <van-empty v-else-if="showEmpty" description="暂无文章"/>
    <div v-else class="article-list">
      <div v-for="article in articles" class="article">
        <h2>
          <router-link :to="`/articles/${article.uri}`">{{ article.title }}</router-link>
        </h2>
        <div class="tags">
          <router-link :key="idx" v-for="(label ,idx) in article.labels"
                       :class="getRandLabel()" :to="`/tags/${ label.name }`" :title="label.name">{{ label.name }}
          </router-link>
        </div>
        <div class="summary" @click="showDetail(article.uri)">
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
          {{ new Date(article.createAt).toLocaleString() }} | <span class="read-num"> 阅读数 <span class="num">{{ article.pv }}</span></span>
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
    showDetail(uri) {
      this.$router.push(`/articles/${ uri }`)
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
