import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

function parseReadonly(raw) {
  try {
    return JSON.parse(raw || 'null')
  } catch {
    return null
  }
}

function fallbackReadonly() {
  return {
    ownerName: '共享账本',
    shareId: null,
    expiresAt: null,
    shareCode: '',
    pending: true
  }
}

function initialToken() {
  return localStorage.getItem('token') || ''
}

function initialReadonly(token) {
  const stored = parseReadonly(localStorage.getItem('readonly'))
  if (token && !token.startsWith('ro_')) return null
  if (token.startsWith('ro_')) return stored || fallbackReadonly()
  return stored
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(initialToken())
  const readonly = ref(initialReadonly(token.value))
  // 正常令牌却残留了只读元数据时，顺手清掉；否则路由守卫会把普通会话当成只读。
  if (token.value && !token.value.startsWith('ro_') && localStorage.getItem('readonly')) {
    localStorage.removeItem('readonly')
  }

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isReadOnly = computed(() => !!readonly.value)
  const readonlyOwnerName = computed(() => readonly.value?.ownerName || '共享账本')
  const readonlyShareCode = computed(() => readonly.value?.shareCode || '')
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')

  async function login(username, password) {
    const res = await authApi.login({ username, password })
    // 公网来源不会直接返回 token，而是返回验证码挑战
    if (res.data?.token) {
      applySession(res.data)
    }
    return res
  }

  async function verifyOtp(challengeId, code) {
    const res = await authApi.verifyOtp({ challengeId, code })
    applySession(res.data)
    return res
  }

  async function resendOtp(challengeId) {
    return authApi.resendOtp({ challengeId })
  }

  function applySession(data) {
    token.value = data.token
    user.value = data
    localStorage.setItem('token', token.value)
    clearReadonly()
  }

  function setReadonly(data, shareCode) {
    token.value = data.token
    user.value = null
    localStorage.setItem('token', data.token)
    readonly.value = {
      ownerName: data.ownerName,
      shareId: data.shareId,
      expiresAt: data.expiresAt,
      shareCode
    }
    localStorage.setItem('readonly', JSON.stringify(readonly.value))
  }

  function clearReadonly() {
    readonly.value = null
    localStorage.removeItem('readonly')
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      token.value = ''
      user.value = null
      localStorage.removeItem('token')
      clearReadonly()
    }
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      const res = await authApi.info()
      user.value = res.data
    } catch {
      token.value = ''
      user.value = null
      localStorage.removeItem('token')
      // 只读会话失效时保留 readonly，交给分享页展示过期并重新验证
      if (!readonly.value) {
        clearReadonly()
      }
    }
  }

  async function changePassword(oldPassword, newPassword) {
    const res = await authApi.changePassword({ oldPassword, newPassword })
    return res
  }

  // 同一浏览器多标签页共享 localStorage：一个标签进入只读分享/退出登录后，
  // 其他标签页的内存状态也要同步，否则会出现「页面还能点编辑，写请求却被
  // 服务端只读过滤器拒绝」的割裂状态。
  function syncFromStorage(event) {
    if (event.key === 'token') {
      token.value = event.newValue || ''
      if (token.value && !token.value.startsWith('ro_')) {
        readonly.value = null
      } else if (token.value.startsWith('ro_') && !readonly.value) {
        readonly.value = fallbackReadonly()
      } else if (!token.value && !localStorage.getItem('readonly')) {
        // 退出时 token 和 readonly 两个 key 都会被删除，事件顺序不保证；
        // 两个 key 都没了才清空，避免把“已失效的分享”误当成已退出。
        readonly.value = null
      }
      return
    }
    if (event.key === 'readonly') {
      const stored = parseReadonly(event.newValue)
      if (token.value && !token.value.startsWith('ro_')) {
        readonly.value = null
      } else if (token.value.startsWith('ro_')) {
        readonly.value = stored || fallbackReadonly()
      } else {
        // token 已被清掉但分享元数据还在：保留给分享页展示“会话已失效”
        readonly.value = stored
      }
      return
    }
    if (event.key === null) {
      token.value = ''
      readonly.value = null
    }
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('storage', syncFromStorage)
  }

  return {
    user, token, readonly, isLoggedIn, isAdmin, isReadOnly,
    readonlyOwnerName, readonlyShareCode, nickname,
    login, verifyOtp, resendOtp, logout, fetchUser, changePassword,
    setReadonly, clearReadonly
  }
})
