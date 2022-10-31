<template>
  <div class="search-list">

    <van-search v-model="keyword"
                placeholder="请输入搜索关键词" show-action @search="onSearch" @cancel="onCancel"/>

    <article-pagination v-if="isNotEmpty(keyword)"
                        :title="title" :pageChange="pageChange"
                        :article="article" :loading="loading"/>

    <mobile-footer/>

  </div>
</template>

<script>
import { Pagination, Search } from "vant";
import { ArticlePagination, MobileFooter, Skeleton } from "src/components";
import { articleService } from "src/services";
import { isNotEmpty, setTitle } from "src/utils";
import debounce from "lodash/debounce";

export default {
  components: {
    MobileFooter,
    ArticlePagination,
    Skeleton,
    [Search.name]: Search,
    [Pagination.name]: Pagination
  },
  data() {
    return {
      title: "",
      keyword: "",
      article: {
        "all": 0,
        "current": 1,
        "data": [],
        "num": 0,
        "size": 10
      },
      loading: true
    };
  },
  methods: {
    isNotEmpty,
    search: debounce(function (q, current = 1) {
      if (isNotEmpty(q)) {
        this.loading = true
        this.title = `关于: ${ q } 的文章`

        articleService.search(q, current).then(res => {
          this.article = res.data
          setTitle(this.title)
        }).finally(() => this.loading = false)
      }
    }, 300),
    pageChange(e) {
      this.search(this.keyword, e)
    },
    onSearch(e) {
      this.search(this.keyword)
    },
    onCancel() {
      this.$router.push("/")
    }
  },
  mounted() {
    const { q } = this.$route.query
    this.keyword = q
  },
  watch: {
    keyword(newVal) {
      this.search(newVal)
    }
  }
};
</script>

<style lang="less">

</style>
