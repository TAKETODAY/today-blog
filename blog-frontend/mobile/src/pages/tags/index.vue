<template>
  <div>

    <search-header/>

    <van-tabs v-model="activeKey">
      <van-tab v-for="tag in tags" :key="tag.id"
               :to='`/tags/${tag.name}`' :title="tag.name"/>
    </van-tabs>

    <article-pagination title="同标签文章" :pageChange="pageChange"
                        :article="article" :loading="loading"/>

    <mobile-footer/>

    <navigation/>
  </div>
</template>

<script>
import { Search, Toast } from "vant";
import { ArticlePagination, MobileFooter, SearchHeader } from 'src/components'
import { articleService } from "src/services";
import { isEmpty, isNotEmpty } from "src/utils";
import { handleError } from "utils/error-handler";

export default {
  name: "tags",
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
      tags: [],
      loading: true,
    };
  },
  methods: {
    isEmpty,
    isNotEmpty,
    pageChange(page) {
      this.load(this.getTagName(this.$route), page)
    },
    load(name, page = 1) {
      if (isNotEmpty(name)) {
        let found = false
        this.tags.forEach((category, idx) => {
          if (category.name === name) {
            this.activeKey = idx
            found = true
          }
        })

        if (found) {
          this.loading = true;
          articleService.getByTagName(name, page).then(data => {
            this.article = data.data;
          }).catch(handleError).finally(() => this.loading = false)
        }
        else {
          this.$router.push({ path: '/not-found', query: { forward: '/tags' } })
        }
      }
    },
    getTagName($route) {
      const { name } = $route.params;
      if (isNotEmpty(name)) {
        return name
      }
      else if (isNotEmpty(this.tags)) {
        return this.tags[0].name
      }
      else {
        Toast("标签错误")
      }
    },
  },

  beforeRouteUpdate(to, from, next) {
    this.load(this.getTagName(to))
    next()
  },
  beforeRouteLeave(to, from, next) {
    this.load(this.getTagName(to))
    next()
  },
  mounted() {
    articleService.getAllTags().then(res => {
      this.tags = res.data
      this.load(this.getTagName(this.$route));
    }).catch(() => {
      Toast("分类加载失败")
    })
  }
};
</script>

<style lang="less">

</style>
