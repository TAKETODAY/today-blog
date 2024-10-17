/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { http } from 'core';

export default class CommentService {

  getComments(articleId, page) {
    return http.get(`/api/comments/articles/${articleId}${page ? '?page=' + page : ''}`)
  }

  fetchComments(articleId, page) {
    return http.get(`/api/comments?articleId=${articleId}`)
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

