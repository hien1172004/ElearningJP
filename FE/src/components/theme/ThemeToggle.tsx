import React from 'react'
import { Dropdown } from 'antd'
import { Monitor, Moon, Sun } from 'lucide-react'
import { useThemeStore, type ThemeMode } from '@/stores'

const OPTIONS: { key: ThemeMode; label: string; icon: React.ReactNode }[] = [
  { key: 'light', label: 'Sáng', icon: <Sun className="w-4 h-4" /> },
  { key: 'dark', label: 'Tối', icon: <Moon className="w-4 h-4" /> },
  { key: 'system', label: 'Theo hệ thống', icon: <Monitor className="w-4 h-4" /> },
]

/** Nút chọn giao diện Sáng / Tối / Theo hệ thống (TopBar). */
export const ThemeToggle: React.FC = () => {
  const mode = useThemeStore((s) => s.mode)
  const resolved = useThemeStore((s) => s.resolved)
  const setMode = useThemeStore((s) => s.setMode)

  return (
    <Dropdown
      trigger={['click']}
      menu={{
        selectable: true,
        selectedKeys: [mode],
        items: OPTIONS.map((o) => ({ key: o.key, label: o.label, icon: o.icon })),
        onClick: ({ key }) => setMode(key as ThemeMode),
      }}
    >
      <button
        type="button"
        aria-label="Chọn giao diện sáng/tối"
        title="Giao diện"
        className="inline-flex items-center justify-center w-9 h-9 rounded-md text-gray-600 hover:bg-gray-100"
      >
        {resolved === 'dark' ? <Moon className="w-[18px] h-[18px]" /> : <Sun className="w-[18px] h-[18px]" />}
      </button>
    </Dropdown>
  )
}
