import api from './index'

export const postApi = {
  list: (board, page = 1, size = 10) =>
    api.get('/posts/public/list', { params: { board, page, size } }),

  detail: (id) =>
    api.get(`/posts/public/${id}`),

  create: (data) =>
    api.post('/posts', data),

  delete: (id) =>
    api.delete(`/posts/${id}`),

  share: (id) =>
    api.post(`/posts/${id}/share`),

  userPosts: (userId, page = 1, size = 10) =>
    api.get(`/posts/user/${userId}`, { params: { page, size } }),

  history: (page = 1, size = 10) =>
    api.get('/posts/history', { params: { page, size } }),

  pending: (page = 1, size = 10) =>
    api.get('/posts/pending', { params: { page, size } }),

  review: (id, action) =>
    api.post(`/posts/${id}/review`, null, { params: { action } }),
}

export const commentApi = {
  list: (postId) =>
    api.get(`/comments/post/${postId}`),

  create: (data) =>
    api.post('/comments', data),

  delete: (id) =>
    api.delete(`/comments/${id}`),
}

export const likeApi = {
  toggle: (targetType, targetId) =>
    api.post(`/likes/${targetType}/${targetId}`),
}

export const reportApi = {
  create: (data) =>
    api.post('/reports', data),

  pending: (page = 1, size = 20) =>
    api.get('/reports/pending', { params: { page, size } }),

  handle: (data) =>
    api.post('/reports/handle', data),
}
