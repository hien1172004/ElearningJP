import React from 'react'
import { Toaster } from 'sonner'
import { AppRouter } from '@/routes'

export const App: React.FC = () => {
  return (
    <>
      <AppRouter />
      <Toaster position="top-right" richColors />
    </>
  )
}

export default App
