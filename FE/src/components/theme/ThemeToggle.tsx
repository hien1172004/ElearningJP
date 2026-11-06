import React from 'react'
import { Moon, Sun } from 'lucide-react'
import { useThemeStore } from '@/stores'

/**
 * Nút đổi giao diện trên TopBar: mỗi lần bấm chuyển Sáng ↔ Tối, icon đổi theo
 * (đang sáng → hiện ☾ để chuyển sang tối; đang tối → hiện ☀ để chuyển sang sáng).
 * Lần đầu vào (chưa chọn) giao diện theo cài đặt của hệ điều hành.
 */
export const ThemeToggle: React.FC = () => {
  const isDark = useThemeStore((s) => s.resolved === 'dark')
  const setMode = useThemeStore((s) => s.setMode)
  const label = isDark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'

  return (
    <button
      type="button"
      onClick={() => setMode(isDark ? 'light' : 'dark')}
      aria-label={label}
      title={label}
      className="inline-flex items-center justify-center w-9 h-9 rounded-md text-gray-600 hover:bg-gray-100 transition-colors"
    >
      {isDark ? <Sun className="w-[18px] h-[18px]" /> : <Moon className="w-[18px] h-[18px]" />}
    </button>
  )
}
