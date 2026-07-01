import { defineStore } from 'pinia'
import request from '@/api/request'
import type { ApiResult } from '@/api/types'

/** 登录用户信息（对应后端 LoginVO/UserVO 的子集） */
export interface AuthUser {
  token: string
  userId: number
  username: string
  nickname?: string
  email?: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('recall_user') || 'null') as AuthUser | null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.user,
    token: (state) => state.user?.token || '',
    currentUser: (state) => state.user,
  },
  actions: {
    async login(username: string, passwordHash: string) {
      const res = await request.post<any, ApiResult<any>>('/api/auth/login', {
        account: username,
        password: passwordHash
      })
      
      const vo = res.data
      const authUser: AuthUser = {
        token: vo.token,
        userId: Number(vo.userId),
        username: vo.username,
        nickname: vo.nickname || '',
        email: vo.email || ''
      }
      
      this.setAuth(authUser)
      return authUser
    },

    async register(username: string, email: string, nickname: string, passwordHash: string) {
      const res = await request.post<any, ApiResult<any>>('/api/auth/register', {
        username,
        email,
        password: passwordHash,
        confirmPassword: passwordHash
      })
      
      const vo = res.data
      const authUser: AuthUser = {
        token: vo.token,
        userId: Number(vo.userId),
        username: vo.username,
        nickname: vo.nickname || '',
        email: vo.email || ''
      }
      
      // Temporarily authenticate to update custom nickname if needed
      this.setAuth(authUser)
      if (nickname && nickname !== username) {
        try {
          await request.put('/api/users/profile', { nickname, email })
        } catch (e) {
          console.error('Failed to update register nickname:', e)
        }
      }
      
      // Clear token/session immediately so user is not logged in directly
      this.clear()
    },

    setAuth(user: AuthUser) {
      this.user = user
      localStorage.setItem('recall_token', user.token)
      localStorage.setItem('recall_user', JSON.stringify(user))
    },

    async updateProfile(nickname: string, email: string) {
      if (!this.user) throw new Error('Not logged in')
      const res = await request.put<any, ApiResult<any>>('/api/users/profile', { nickname, email })
      const vo = res.data
      
      const authUser = {
        ...this.user,
        nickname: vo.nickname || '',
        email: vo.email || ''
      }
      this.setAuth(authUser)
    },

    async updatePassword(oldPasswordHash: string, newPasswordHash: string) {
      if (!this.user) throw new Error('Not logged in')
      await request.put('/api/users/password', {
        oldPassword: oldPasswordHash,
        newPassword: newPasswordHash
      })
    },

    async logout() {
      try {
        await request.post('/api/auth/logout')
      } catch (e) {
        console.error('Logout error:', e)
      } finally {
        this.clear()
      }
    },

    clear() {
      this.user = null
      localStorage.removeItem('recall_token')
      localStorage.removeItem('recall_user')
    },

    /** 拉取当前用户信息刷新本地 */
    async fetchMe() {
      if (!this.user) return null
      try {
        const res = await request.get<any, ApiResult<any>>('/api/auth/me')
        const vo = res.data
        const authUser: AuthUser = {
          token: this.token,
          userId: Number(vo.id),
          username: vo.username,
          nickname: vo.nickname || '',
          email: vo.email || ''
        }
        this.setAuth(authUser)
        return authUser
      } catch (e) {
        console.error('Failed to fetch user me:', e)
        return null
      }
    },
  },
})
