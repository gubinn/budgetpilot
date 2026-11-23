import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

function readReadonly() {
  try {
    return JSON.parse(localStorage.getItem('readonly') || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const readonly = ref(readReadonly())

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

  return {
    user, token, readonly, isLoggedIn, isAdmin, isReadOnly,
    readonlyOwnerName, readonlyShareCode, nickname,
    login, verifyOtp, resendOtp, logout, fetchUser, changePassword,
    setReadonly, clearReadonly
  }
})
