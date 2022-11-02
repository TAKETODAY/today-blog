import { getCacheable, http } from 'src/utils'

export default class ArticleService {

  getAllCategories() {
    return getCacheable("/api/categories")
  }

  getAllTags() {
    return getCacheable("/api/tags")
  }

  getByTagName(name, page = 1) {
    return getCacheable(`/api/articles/tags/${ name }?page=${ page }`)
  }

  getByCategoryName(name, page = 1) {
    return getCacheable(`/api/articles/categories/${ name }?page=${ page }`)
  }

  getById(articleId, key) {
    return http.get(`/api/articles/${ articleId }${ key ? '?key=' + key : '' }`)
  }

  getIndexArticles(current) {
    return getCacheable(`/api/articles?page=${ current }`)
  }

  updatePV(id) {
    return http.post(`/api/articles/${ id }/pv`)
  }

  switchFavorite(id) {
    return http.put(`/api/books/${ id }/favorite`)
  }

  create(form) {
    return http.post(`/api/books`, form)
  }

  getUserFavorite(page, size) {
    return http.get(`/api/books?type=favorite&current=${ page }&size=${ size }`)
  }

  getUserPublish(page, size) {
    return http.get(`/api/books?type=publish&current=${ page }&size=${ size }`)
  }

  deletePublish(bookId) {
    return http.delete(`/api/books/${ bookId }`)
  }

  getByUser(id, page, size) {
    return http.get(`/api/books/users/${ id }?current=${ page }&size=${ size }`)
  }

  listByAuthor(author) {
    return http.get(`/api/books/author/${ author }`)
  }

  publishingHouse(publishing) {
    return http.get(`/api/books/publishing/${ publishing }`)
  }

  search(search, page = 1) {
    return http.get(`/api/articles/search?q=${ search }&page=${ page }`)
  }

}

