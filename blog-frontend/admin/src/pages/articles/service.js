import { http } from '@/utils'
import { request } from 'umi';

export async function queryArticles(params, sort) {
  const { pageSize, current, ...rest } = params
  params = { ...rest, ...sort, size: pageSize, page: current }
  return request('/api/articles/admin', {
    method: 'GET',
    params
  }).then(res => {
    return {
      ...res,
      total: res.all,
      pageSize: res.size,
      current: res.current
    }
  })
}

export async function getCategories() {
  return http.get('/api/categories')
}

export async function toggleArticleStatus(id, status) {
  return http.put(`/api/articles/${ id }/status/${ status }`)
}


export async function create(data) {
  return http.post('/api/books', data)
}

export async function update(book) {
  return http.put(`/api/books/${ book.id }`, book)
}

export async function deleteArticle(article) {
  return http.delete(`/api/articles/${ article.id }`)
}

export async function toggleAlreadySold(id) {
  return http.put(`/api/books/${ id }/toggle-already-sold`)
}
