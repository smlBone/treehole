import api from './index'

export const authApi = {
  sendCode: (email, purpose = 'REGISTER') =>
    api.post('/auth/send-code', { email, purpose }),

  register: (data) =>
    api.post('/auth/register', data),

  login: (data) =>
    api.post('/auth/login', data),

  resetPassword: (data) =>
    api.post('/auth/reset-password', data),

  logout: () =>
    api.post('/auth/logout'),
}
