import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器：添加JWT token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理错误
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // 未授权，清除token并跳转登录
        localStorage.removeItem('token')
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          window.location.href = '/login'
        }
        return Promise.reject({ code: 401, message: data?.message || '请先登录' })
      }
      return Promise.reject({ code: status, message: data?.message || '请求失败' })
    }
    return Promise.reject({ code: 500, message: '网络错误' })
  }
)

export default api
