import { create } from 'zustand'

/** Chế độ giao diện người dùng chọn. `system` = theo cài đặt sáng/tối của hệ điều hành. */
export type ThemeMode = 'light' | 'dark' | 'system'
export type ResolvedTheme = 'light' | 'dark'

/** Khoá localStorage — phải trùng với script chống nháy trong index.html. */
export const THEME_STORAGE_KEY = 'elearning-theme'

const media = () =>
  typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)') : null

const readMode = (): ThemeMode => {
  try {
    const v = localStorage.getItem(THEME_STORAGE_KEY)
    return v === 'light' || v === 'dark' || v === 'system' ? v : 'system'
  } catch {
    return 'system'
  }
}

const resolve = (mode: ThemeMode): ResolvedTheme =>
  mode === 'system' ? (media()?.matches ? 'dark' : 'light') : mode

/** Gắn/bỏ class `.dark` trên <html> — toàn bộ token màu trong index.css đổi theo. */
const apply = (theme: ResolvedTheme) => {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('dark', theme === 'dark')
}

interface ThemeState {
  mode: ThemeMode
  /** Giao diện thực tế đang hiển thị (đã giải `system`). */
  resolved: ResolvedTheme
  setMode: (mode: ThemeMode) => void
}

export const useThemeStore = create<ThemeState>((set, get) => {
  const initialMode = readMode()
  const initialResolved = resolve(initialMode)
  apply(initialResolved)

  // Đang theo hệ thống → đổi theo khi người dùng đổi sáng/tối của OS
  media()?.addEventListener('change', () => {
    if (get().mode !== 'system') return
    const resolved = resolve('system')
    apply(resolved)
    set({ resolved })
  })

  return {
    mode: initialMode,
    resolved: initialResolved,
    setMode: (mode) => {
      try {
        localStorage.setItem(THEME_STORAGE_KEY, mode)
      } catch {
        /* ignore */
      }
      const resolved = resolve(mode)
      apply(resolved)
      set({ mode, resolved })
    },
  }
})
