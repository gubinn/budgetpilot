import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

// 响应拦截
api.interceptors.response.use(
  res => {
    // 业务层错误码 80001 = 未登录/登录过期
    if (res.data?.code === 80001) {
      localStorage.removeItem('token')
      const redirect = new URLSearchParams(window.location.search).get('redirect')
      window.location.href = '/login?redirect=' + encodeURIComponent(redirect || window.location.pathname)
      return Promise.reject(new Error('未登录'))
    }
    if (res.data.code !== 0) {
      console.error('API Error:', res.data.message)
      throw new Error(res.data.message)
    }
    return res.data
  },
  err => {
    // HTTP 401
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
      return Promise.reject(new Error('未登录'))
    }
    console.error('HTTP Error:', err)
    throw err
  }
)

export default api
