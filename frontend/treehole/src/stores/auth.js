import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isReviewer = computed(() => user.value?.role === 'REVIEWER' || isAdmin.value)
  const isSpecial = computed(() => user.value?.role === 'SPECIAL')

  async function login(email, hashedPassword) {
    const res = await authApi.login({ email, password: hashedPassword })
    if (res.code === 200) {
      token.value = res.data
      localStorage.setItem('token', res.data)
      await fetchUser()
    }
    return res
  }

  async function fetchUser() {
    if (!token.value) return null
    try {
      const res = await userApi.me()
      if (res.code === 200) {
        user.value = res.data
      }
      return res.data
    } catch {
      logout()
      return null
    }
  }

  function logout() {
    if (token.value) {
      authApi.logout().catch(() => {})
    }
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    isReviewer,
    isSpecial,
    login,
    fetchUser,
    logout,
  }
})
