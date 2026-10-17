import React from 'react'
import { Toaster } from 'sonner'
import { AppRouter } from '@/routes'
import { ErrorBoundary } from './components'

export const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <AppRouter />
      <Toaster position="top-right" richColors closeButton />
    </ErrorBoundary>
  )
}

export default App
