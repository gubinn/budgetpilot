import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
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
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      token.value = ''
      user.value = null
      localStorage.removeItem('token')
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
    }
  }

  async function changePassword(oldPassword, newPassword) {
    const res = await authApi.changePassword({ oldPassword, newPassword })
    return res
  }

  return { user, token, isLoggedIn, isAdmin, nickname, login, verifyOtp, resendOtp, logout, fetchUser, changePassword }
})
