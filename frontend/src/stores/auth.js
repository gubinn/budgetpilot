import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')

  async function login(username, password) {
    const res = await axios.post('/api/auth/login', { username, password })
    if (res.data.code === 0) {
      token.value = res.data.data.token
      user.value = res.data.data
      localStorage.setItem('token', token.value)
    }
    return res.data
  }

  async function logout() {
    try {
      await axios.post('/api/auth/logout', null, {
        headers: { Authorization: token.value }
      })
    } finally {
      token.value = ''
      user.value = null
      localStorage.removeItem('token')
    }
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      const res = await axios.get('/api/auth/info', {
        headers: { Authorization: token.value }
      })
      if (res.data.code === 0) {
        user.value = res.data.data
      }
    } catch {
      token.value = ''
      user.value = null
      localStorage.removeItem('token')
    }
  }

  async function changePassword(oldPassword, newPassword) {
    const res = await axios.post('/api/auth/change-password', { oldPassword, newPassword }, {
      headers: { Authorization: token.value }
    })
    return res.data
  }

  return { user, token, isLoggedIn, isAdmin, nickname, login, logout, fetchUser, changePassword }
})
