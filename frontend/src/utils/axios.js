import axios from 'axios'

axios.interceptors.response.use(
  (response) => {
    // 业务层错误码 80001 = 未登录/登录过期
    if (response.data?.code === 80001) {
      localStorage.removeItem('token')
      const redirect = new URLSearchParams(window.location.search).get('redirect')
      window.location.href = '/login?redirect=' + encodeURIComponent(redirect || window.location.pathname)
      return Promise.reject(new Error('未登录'))
    }
    return response
  },
  (error) => {
    // HTTP 401
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
      return Promise.reject(new Error('未登录'))
    }
    return Promise.reject(error)
  }
)
