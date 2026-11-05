import { create } from 'zustand'
import type { User } from '@/types'
import { storage } from '@/utils/storage'
import { queryClient } from '@/lib/queryClient'
import { QUERY_KEYS } from '@/lib/queryKeys'

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
    // Bình luận tải lúc còn là khách có owner/deletable/moderator/liked = false.
    // Đăng nhập ngay trên trang (modal) → tải lại để hiện đúng nút Sửa/Xoá và ẩn nút báo cáo.
    void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.comment.all })
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
    // Xoá toàn bộ cache server-state của phiên cũ (danh sách khóa học quản lý, profile, SRS…)
    // để tài khoản đăng nhập tiếp theo trên cùng tab không thấy dữ liệu của người trước.
    // Dùng clear() (không refetch) thay vì resetQueries(): refetch khi đã mất token sẽ dính 401
    // → interceptor gọi lại clearAuth → lặp vô hạn.
    queryClient.clear()
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
