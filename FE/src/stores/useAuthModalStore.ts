import { create } from 'zustand'

export type AuthModalMode = 'login' | 'register'

interface AuthModalState {
  open: boolean
  mode: AuthModalMode
  /**
   * Redirect path sau khi đăng nhập/đăng ký thành công.
   * Nếu null thì về trang hiện tại.
   */
  redirectTo: string | null
  openLogin: (redirectTo?: string | null) => void
  openRegister: (redirectTo?: string | null) => void
  setMode: (mode: AuthModalMode) => void
  close: () => void
}

export const useAuthModalStore = create<AuthModalState>((set) => ({
  open: false,
  mode: 'login',
  redirectTo: null,
  openLogin: (redirectTo = null) =>
    set({ open: true, mode: 'login', redirectTo }),
  openRegister: (redirectTo = null) =>
    set({ open: true, mode: 'register', redirectTo }),
  setMode: (mode) => set({ mode }),
  close: () => set({ open: false }),
}))
