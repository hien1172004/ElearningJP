import React from 'react'
import { ConfigProvider, App as AntdApp, theme as antdTheme } from 'antd'
import { useThemeStore } from '@/stores/useThemeStore'
import viVN from 'antd/locale/vi_VN'
// antd v6 already supports React 19, no patch needed.

const themeToken = {
  // Đồng bộ với chủ đề 和色 (藍 Ai trên nền 生成り Kinari) — xem index.css @theme
  colorPrimary: '#2e5c8a', // 藍 ai
  colorInfo: '#2e5c8a',
  colorSuccess: '#10b981', // emerald-500
  colorWarning: '#f59e0b', // amber-500
  colorError: '#d9534f', // 朱 shu
  colorLink: '#2e5c8a',

  // Bo góc mềm mại như design hiện tại
  borderRadius: 12, // rounded-md
  borderRadiusLG: 16,
  borderRadiusSM: 8,

  // Font
  fontFamily:
    "'Inter', system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
  fontSize: 14,

  // Wireframe dịu hơn
  wireframe: false,
}

/** Nền / viền / chữ theo chế độ — đồng bộ token .dark trong index.css. */
const surfaceTokens = {
  light: {
    colorBgLayout: '#faf7f0', // 生成り kinari
    colorBgContainer: '#ffffff', // 白 shiro
    colorBgElevated: '#ffffff',
    colorBorder: '#e7e0d2', // 砂 suna
    colorBorderSecondary: '#efe9de',
    colorText: '#1f2430', // 墨 sumi
    colorTextSecondary: '#6b6f78', // 鈍 nibi
  },
  dark: {
    colorPrimary: '#4f82b3', // ai sáng hơn cho nền tối
    colorInfo: '#4f82b3',
    colorLink: '#6b95bd',
    colorBgLayout: '#14171f',
    colorBgContainer: '#1c2029',
    colorBgElevated: '#232833',
    colorBorder: '#343946',
    colorBorderSecondary: '#2a2f3a',
    colorText: '#ece7dd',
    colorTextSecondary: '#a3a29f',
  },
} as const

const componentsTheme = {
  Button: {
    controlHeight: 38,
    controlHeightSM: 30,
    controlHeightLG: 46,
    fontWeight: 600,
  },
  Input: {
    controlHeight: 38,
    borderRadius: 12,
    paddingBlock: 9,
  },
  Card: {
    borderRadiusLG: 16,
  },
  Modal: {
    borderRadiusLG: 16,
  },
  Tag: {
    borderRadiusSM: 6,
  },
}

interface AntdProviderProps {
  children: React.ReactNode
}

export const AntdProvider: React.FC<AntdProviderProps> = ({ children }) => {
  const mode = useThemeStore((s) => s.resolved)
  return (
    <ConfigProvider
      locale={viVN}
      theme={{
        algorithm: mode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
        token: { ...themeToken, ...surfaceTokens[mode] },
        components: componentsTheme,
      }}
    >
      <AntdApp>{children}</AntdApp>
    </ConfigProvider>
  )
}
