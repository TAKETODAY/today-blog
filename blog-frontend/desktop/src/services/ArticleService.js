import { getCacheable, http } from '../utils';

export default class ArticleService {

  getById(articleId, key) {
    return http.get(`/api/articles/${ articleId }${ key ? '?key=' + key : '' }`)
  }

  popularArticles() {
    return getCacheable("/api/articles/popular")
  }

  search(query) {
    return http.get(`/api/articles/search?q=${ query }`)
  }

  searchPageable(query, page = 1, size = 10) {
    return http.get(`/api/articles/search?q=${ query }&page=${ page }&size=${ size }`)
  }

  updatePageView(articleId) {
    return http.post(`/api/articles/${ articleId }/pv`)
  }

  fetchHomeArticles(page, size) {
    return http.get(`/api/articles?page=${ page }&size=${ size }`)
  }

  getTag(tagsId, page, size) {
    return http.get(`/api/articles/tags/${ tagsId }?page=${ page }&size=${ size }`)
  }

}

