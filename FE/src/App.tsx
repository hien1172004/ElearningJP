import React from 'react'
import { Toaster } from 'sonner'
import { AppRouter } from '@/routes'
import { ConcurrentEditModal, ErrorBoundary } from './components'
import { useThemeStore } from '@/stores'

export const App: React.FC = () => {
  const theme = useThemeStore((s) => s.resolved)
  return (
    <ErrorBoundary>
      <AppRouter />
      <ConcurrentEditModal />
      {/* Kênh toast duy nhất của app: luôn góc phải, ngay dưới TopBar (không che chuông / tài khoản) */}
      <Toaster
        position="top-right"
        richColors
        closeButton
        theme={theme}
        offset={{ top: 'calc(var(--topbar-h) + 8px)', right: 16 }}
        mobileOffset={{ top: 'calc(var(--topbar-h) + 8px)', right: 12, left: 12 }}
      />
    </ErrorBoundary>
  )
}

export default App
