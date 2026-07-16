import api from './index'

export const userApi = {
  me: () =>
    api.get('/users/me'),

  profile: (id) =>
    api.get(`/users/${id}`),

  updateProfile: (data) =>
    api.put('/users/profile', data),

  changePassword: (data) =>
    api.put('/users/password', data),

  follow: (id) =>
    api.post(`/users/${id}/follow`),

  unfollow: (id) =>
    api.delete(`/users/${id}/follow`),

  block: (id) =>
    api.post(`/users/${id}/block`),

  unblock: (id) =>
    api.delete(`/users/${id}/block`),

  following: (id, page = 1, size = 20) =>
    api.get(`/users/${id}/following`, { params: { page, size } }),

  followers: (id, page = 1, size = 20) =>
    api.get(`/users/${id}/followers`, { params: { page, size } }),
}

export const messageApi = {
  send: (data) =>
    api.post('/messages', data),

  conversations: () =>
    api.get('/messages/conversations'),

  chatHistory: (userId) =>
    api.get(`/messages/chat/${userId}`),

  unreadCount: () =>
    api.get('/messages/unread-count'),

  systemMessages: () =>
    api.get('/messages/system'),

  readSystemMessage: (id) =>
    api.put(`/messages/system/${id}/read`),

  broadcast: (data) =>
    api.post('/messages/system/broadcast', data),
}

export const fileApi = {
  upload: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
