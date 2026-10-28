import { create } from 'zustand'
import type { User } from '@/types'
import { storage } from '@/utils/storage'

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  isRememberMe: boolean
  isInitialized: boolean
  setAuth: (params: {
    user: User | null
    accessToken: string
    refreshToken?: string | null
    remember: boolean
  }) => void
  setUser: (user: User | null) => void
  updateAccessToken: (token: string) => void
  clearAuth: () => void
  setInitialized: (value: boolean) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: storage.getAccessToken(),
  refreshToken: storage.getRefreshToken(),
  isAuthenticated: !!storage.getAccessToken(),
  isRememberMe: storage.getRemember(),
  isInitialized: false,

  setAuth: ({ user, accessToken, refreshToken, remember }) => {
    storage.setTokens({
      access: accessToken,
      refresh: refreshToken ?? undefined,
      remember,
    })
    set({
      user,
      accessToken,
      refreshToken: refreshToken ?? null,
      isAuthenticated: true,
      isRememberMe: remember,
      isInitialized: true,
    })
  },

  setUser: (user) => {
    set({ user })
  },

  updateAccessToken: (token) => {
    const remember = storage.getRemember()
    storage.setAccessToken(token, remember)
    set({ accessToken: token })
  },

  clearAuth: () => {
    storage.clearAll()
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isRememberMe: false,
    })
  },

  setInitialized: (value) => {
    set({ isInitialized: value })
  },
}))
