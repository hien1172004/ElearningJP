import React from 'react'
import { Toaster } from 'sonner'
import { AppRouter } from '@/routes'
import { ErrorBoundary } from './components'
import { useThemeStore } from '@/stores'

export const App: React.FC = () => {
  const theme = useThemeStore((s) => s.resolved)
  return (
    <ErrorBoundary>
      <AppRouter />
      <Toaster position="top-right" richColors closeButton theme={theme} />
    </ErrorBoundary>
  )
}

export default App
