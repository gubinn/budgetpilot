import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

function redirectWhenSessionInvalid() {
  let readonly = null
  try {
    readonly = JSON.parse(localStorage.getItem('readonly') || 'null')
  } catch {
    readonly = null
  }
  localStorage.removeItem('token')
  if (readonly?.shareCode && !window.location.pathname.startsWith('/share/')) {
    // 保留 readonly，分享页据此展示“会话已过期”并允许重新验证
    window.location.href = `/share/${readonly.shareCode}?expired=1`
    return
  }
  const redirect = new URLSearchParams(window.location.search).get('redirect')
  window.location.href = '/login?redirect=' + encodeURIComponent(redirect || window.location.pathname)
}

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
      redirectWhenSessionInvalid()
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
      redirectWhenSessionInvalid()
      return Promise.reject(new Error('未登录'))
    }
    console.error('HTTP Error:', err)
    throw err
  }
)

export default api
