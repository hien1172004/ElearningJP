import { create } from 'zustand'
import type { User } from '@/types'
import { storage } from '@/utils/storage'

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  setAuth: (user: User, token: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: storage.getToken(),
  isAuthenticated: !!storage.getToken(),

  setAuth: (user, token) => {
    storage.setToken(token)
    set({ user, token, isAuthenticated: true })
  },

  clearAuth: () => {
    storage.clearToken()
    set({ user: null, token: null, isAuthenticated: false })
  },
}))
