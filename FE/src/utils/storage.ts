/**
 * Storage abstraction cho token và cờ Remember Me.
 *
 * - Nếu `remember = true` -> lưu vào localStorage (giữ qua các session).
 * - Nếu `remember = false` -> lưu vào sessionStorage (mất khi đóng tab).
 *
 * Access token luôn được lưu. Refresh token (nếu có) chỉ lưu khi remember = true.
 */

const ACCESS_KEY = 'elearning_access_token'
const REFRESH_KEY = 'elearning_refresh_token'
const REMEMBER_KEY = 'elearning_remember_flag'

function getStore(remember: boolean): Storage {
  return remember ? localStorage : sessionStorage
}

export const storage = {
  // ---------- Access token ----------
  getAccessToken(): string | null {
    // Ưu tiên localStorage (Remember me) trước, rồi mới tới sessionStorage.
    try {
      return (
        localStorage.getItem(ACCESS_KEY) ?? sessionStorage.getItem(ACCESS_KEY)
      )
    } catch {
      return null
    }
  },

  setAccessToken(token: string, remember: boolean): void {
    try {
      // Xoá ở cả 2 store trước để tránh "kẹt" token cũ ở nơi không mong muốn.
      sessionStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(ACCESS_KEY)
      getStore(remember).setItem(ACCESS_KEY, token)
      this.setRemember(remember)
    } catch {
      // ignore
    }
  },

  // ---------- Refresh token ----------
  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY)
    } catch {
      return null
    }
  },

  setRefreshToken(token: string): void {
    try {
      localStorage.setItem(REFRESH_KEY, token)
    } catch {
      // ignore
    }
  },

  // ---------- Remember flag ----------
  getRemember(): boolean {
    try {
      return localStorage.getItem(REMEMBER_KEY) === '1'
    } catch {
      return false
    }
  },

  setRemember(remember: boolean): void {
    try {
      if (remember) {
        localStorage.setItem(REMEMBER_KEY, '1')
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }
    } catch {
      // ignore
    }
  },

  // ---------- Set cả 2 token một lúc ----------
  setTokens(params: {
    access: string
    refresh?: string | null
    remember: boolean
  }): void {
    this.setAccessToken(params.access, params.remember)
    if (params.refresh) {
      this.setRefreshToken(params.refresh)
    }
  },

  // ---------- Xoá tất cả ----------
  clearAll(): void {
    try {
      localStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(REMEMBER_KEY)
      sessionStorage.removeItem(ACCESS_KEY)
    } catch {
      // ignore
    }
  },

  /** Backward-compat với code cũ (axiosClient đang gọi `storage.getToken()`). */
  getToken(): string | null {
    return this.getAccessToken()
  },
  setToken(token: string): void {
    // Mặc định gọi setToken() cũ sẽ lưu vào localStorage để giữ behavior trước đó.
    this.setAccessToken(token, true)
  },
  clearToken(): void {
    this.clearAll()
  },
}
