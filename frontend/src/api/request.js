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
    if (res.data.code !== 0) {
      console.error('API Error:', res.data.message)
      throw new Error(res.data.message)
    }
    return res.data
  },
  err => {
    console.error('HTTP Error:', err)
    throw err
  }
)

export default api
