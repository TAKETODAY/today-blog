<template>
  <div>

    <search-header/>

    <van-tabs v-model="activeKey">
      <van-tab v-for="category in categories" :key="category.id"
               :to='`/categories/${category.name}`' :title="`${category.name} (${category.articleCount})`"></van-tab>
    </van-tabs>

    <article-pagination title="同类文章" :pageChange="pageChange"
                        :article="article" :loading="loading"/>

    <mobile-footer/>

    <navigation/>
  </div>
</template>

<script>
import { Search, Toast } from "vant";
import { ArticlePagination, MobileFooter, SearchHeader } from 'src/components'
import { articleService } from "src/services";
import { isEmpty, isNotEmpty, setTitle } from "src/utils";
import { handleError } from "utils/error-handler";

export default {
  name: "categories",
  components: {
    MobileFooter,
    SearchHeader,
    ArticlePagination,
    [Search.name]: Search
  },
  data() {
    return {
      article: {
        "all": 0,
        "current": 1,
        "data": [],
        "num": 0,
        "size": 10
      },
      activeKey: 0,
      categories: [],
      loading: true,
    };
  },
  methods: {
    isEmpty,
    isNotEmpty,
    pageChange(page) {
      this.load(this.getCategoryName(this.$route), page)
    },
    load(name, page = 1) {
      if (isNotEmpty(name)) {
        let found = false
        this.categories.forEach((category, idx) => {
          if (category.name === name) {
            this.activeKey = idx
            found = true
          }
        })

        if (found) {
          this.loading = true;
          articleService.getByCategoryName(name, page).then(data => {
            this.article = data.data;
          }).catch(handleError).finally(() => this.loading = false)
        }
        else {
          this.$router.push({ path: '/not-found', query: { forward: '/categories' } })
        }
      }
    },
    getCategoryName($route) {
      const { name } = $route.params;
      if (isNotEmpty(name)) {
        return name
      }
      else if (isNotEmpty(this.categories)) {
        return this.categories[0].name
      }
      else {
        Toast("分类错误")
      }
    },
  },

  beforeRouteUpdate(to, from, next) {
    this.load(this.getCategoryName(to))
    next()
  },
  beforeRouteLeave(to, from, next) {
    this.load(this.getCategoryName(to))
    next()
  },
  mounted() {
    articleService.getAllCategories().then(data => {
      this.categories = data.data;
      this.load(this.getCategoryName(this.$route));
    }).catch(() => {
      Toast("分类加载失败")
    })
  }
};
</script>

<style lang="less">

</style>
