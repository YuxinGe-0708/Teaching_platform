import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),
  getters: {
    isLoggedIn: (state) => state.user !== null,
    isStudent: (state) => state.user?.role === 'student',
    isTeacher: (state) => state.user?.role === 'teacher',
  },
  actions: {
    async login(username, password) {
      const res = await authApi.login(username, password)
      this.user = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      return res.data
    },
    async register(username, password, role, name) {
      const res = await authApi.register(username, password, role, name)
      return res.data
    },
    async fetchUser() {
      const res = await authApi.me()
      if (res.code === 200 && res.data) {
        this.user = res.data
        localStorage.setItem('user', JSON.stringify(res.data))
      }
      return this.user
    },
    async logout() {
      try { await authApi.logout() } catch (e) { /* ignore */ }
      this.user = null
      localStorage.removeItem('user')
    }
  }
})
