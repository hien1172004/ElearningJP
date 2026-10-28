import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/stores'
import { isTeacherOrAdmin } from '@/utils/roles'

export const AdminRoute: React.FC = () => {
  const { isAuthenticated, user } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!isTeacherOrAdmin(user)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
