import React from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { MainLayout, AuthLayout } from '@/layouts'
import { HomePage, LoginPage, DictionaryPage } from '@/pages'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'dictionary', element: <DictionaryPage /> },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
    ],
  },
])

export const AppRouter: React.FC = () => {
  return <RouterProvider router={router} />
}
