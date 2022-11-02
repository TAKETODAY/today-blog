import { http } from '../utils';

export default class CommentService {

  getComments(articleId, page) {
    return http.get(`/api/comments/articles/${articleId}${page ? '?page=' + page : ''}`)
  }

  createComment(data) {
    return http.post('/api/comments', data)
  }

  getById(commentsId) {
    return http.get(`/api/comments/${commentsId}`)
  }

  getByUser(page, size) {
    return http.get(`/api/comments/users?page=${page}&size=${size}`)
  }

  deleteComment(id) {
    return http.delete(`/api/comments/${id}`)
  }

}

