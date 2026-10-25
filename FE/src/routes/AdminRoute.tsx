import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores'

export const AdminRoute: React.FC = () => {
  const { isAuthenticated, user } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  const role = user?.role || ''
  const isAdminOrTeacher = role.includes('ADMIN') || role.includes('TEACHER')
  if (!isAdminOrTeacher) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
