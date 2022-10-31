import { getCacheable, http } from '@/utils'

export default new class {

  create(article) {
    return http.post('/api/articles', article)
  }

  update(article) {
    return http.put('/api/articles/' + article.id, article)
  }

  getAllCategories() {
    return getCacheable('/api/categories')
  }

  getById(id) {
    return http.get(`/api/articles/${ id }`)
  }

  getAllLabels() {
    return getCacheable('/api/tags')
  }

}()
