import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { queryClient } from '@/lib/queryClient'
import { AntdProvider } from '@/lib/antd'
import { AuthInitializer } from '@/components/auth/AuthInitializer'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AntdProvider>
        <AuthInitializer>
          <App />
        </AuthInitializer>
      </AntdProvider>
    </QueryClientProvider>
  </StrictMode>,
)
