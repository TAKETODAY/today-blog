<template>
  <div>
    <!-- search -->
    <search-header/>
    <!-- 文章 -->
    <article-pagination title="最新文章" :pageChange="pageChange"
                        :article="article" :loading="articlesLoading"/>

    <mobile-footer/>
    <!-- navigation -->
    <navigation/>
  </div>
</template>

<script>
import { articleService } from "services";
import { Divider, Pagination, Search, Toast } from "vant";
import { ArticleList, ArticlePagination, MobileFooter, SearchHeader } from "src/components";

export default {
  components: {
    SearchHeader,
    ArticlePagination,
    MobileFooter,
    ArticleList,
    [Search.name]: Search,
    [Divider.name]: Divider,
    [Pagination.name]: Pagination
  },
  data: function () {
    return {
      articlesLoading: true,
      article: {
        "all": 0,
        "current": 1,
        "data": [],
        "num": 0,
        "size": 10
      },
    };
  },
  computed: {},
  mounted() {
    this.load(1)
  },
  methods: {
    pageChange(e) {
      this.load(e)
    },
    load(current) {
      this.articlesLoading = true

      articleService.getIndexArticles(current).then(res => {
        this.article = res.data;
      }).catch(() => {
        Toast("首页文章加载失败")
      }).finally(() => this.articlesLoading = false)
    },
  },
};
</script>

<style lang="less">

</style>
