import { defineStore } from 'pinia'
import * as authApi from '@/api/modules/auth'

const TOKEN_KEY = 'cts_token'
const USER_KEY = 'cts_user'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persistUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
    loading: false
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.nickname || state.user?.username || '邻居',
    isCertified: (state) => state.user?.realNameStatus === 'VERIFIED'
  },

  actions: {
    setSession(payload) {
      this.token = payload.token
      this.user = payload.user
      localStorage.setItem(TOKEN_KEY, payload.token)
      persistUser(payload.user)
    },

    async login(form) {
      const data = await authApi.login(form)
      this.setSession(data)
      return data
    },

    async register(form) {
      const data = await authApi.register(form)
      this.setSession(data)
      return data
    },

    async loadMe() {
      if (!this.token) return null
      this.loading = true
      try {
        const user = await authApi.getMe()
        this.user = user
        persistUser(user)
        return user
      } finally {
        this.loading = false
      }
    },

    async updateProfile(form) {
      const user = await authApi.updateProfile(form)
      this.user = user
      persistUser(user)
      return user
    },

    async updateAvatar(avatarUrl) {
      const user = await authApi.updateAvatar(avatarUrl)
      this.user = user
      persistUser(user)
      return user
    },

    async changePassword(form) {
      await authApi.changePassword(form)
    },

    async submitRealName(form) {
      const user = await authApi.submitRealName(form)
      this.user = user
      persistUser(user)
      return user
    },

    async logout() {
      try {
        if (this.token) await authApi.logout()
      } catch {
        // 后端不可用时也要允许前端退出
      } finally {
        this.clear()
      }
    },

    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      persistUser(null)
    }
  }
})
