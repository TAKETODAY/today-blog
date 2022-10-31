<template>
  <div class="articles-info">
    <div class="info-title">{{ title }}</div>
    <div class="details">
      <article-list :articles="article.data" :loading="loading"/>
      <van-pagination @change="pageChange"
                      v-model="article.current"
                      :total-items="article.all"
                      :page-count="article.num" :items-per-page="article.size"/>

    </div>
  </div>

</template>


<script>
import { ArticleList } from 'src/components'

export default {
  name: "article-pagination",
  components: {
    ArticleList
  },
  props: {
    loading: {
      type: Boolean,
    },
    title: {
      type: String
    },
    article: {
      type: Object,
    },
    pageChange: {
      type: Function
    }
  },
  data() {
    return {}
  },
  computed: {},
  mounted() {

  },
  methods: {}
}
</script>

<style lang="less">

.articles {

  &-info {
    background: #fff;

    .info-title {
      height: 44px;
      line-height: 44px;
      padding-left: 10px;
      border-left: 3px solid #1b809e;
      border-bottom: 1px dashed #c8dde4;

      font-size: 14px;
      font-weight: 700;
      margin: 10px 0 0 0;
      background: #fff;
    }

    .details {
      padding: 10px;
      font-size: 14px;
    }

  }
}
</style>
