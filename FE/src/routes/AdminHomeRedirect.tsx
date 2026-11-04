import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'

/**
 * Điểm vào khu quản trị (`/admin`, `/admin/dashboard`):
 * ADMIN → trang thống kê; TEACHER → danh sách đề thi (TEACHER không có quyền xem thống kê).
 * Được bọc trong ProtectedRoute ADMIN/TEACHER nên ở đây chỉ cần phân nhánh theo role.
 */
export const AdminHomeRedirect: React.FC = () => {
  const user = useAuthStore((s) => s.user)
  return <Navigate to={isAdmin(user) ? '/admin/stats' : '/admin/exams'} replace />
}
