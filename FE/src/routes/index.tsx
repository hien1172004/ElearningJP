import React from 'react'
import { createBrowserRouter, Outlet, RouterProvider } from 'react-router-dom'
import { MainLayout } from '@/layouts'
import {
  HomePage,
  ForgotPasswordPage,
  ResetPasswordPage,
  GoogleCallbackPage,
  DictionaryPage,
  CourseCatalogPage,
  CourseDetailPage,
  TeacherCoursesPage,
  TeacherCourseEditorPage,
  LessonViewerPage,
  ProgressPage,
} from '@/pages'
import { ProtectedRoute } from './ProtectedRoute'

// Wrapper để apply MainLayout cho protected routes
const WithMainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) =>
  React.createElement(MainLayout, null, children)

// Outlet wrapper for nested routes inside ProtectedRoute
const ProtectedOutlet: React.FC = () => <Outlet />

export const router = createBrowserRouter([
  // ====================================================================
  // Public pages under MainLayout
  // ====================================================================
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'dictionary', element: <DictionaryPage /> },
      { path: 'courses', element: <CourseCatalogPage /> },
      { path: 'courses/:id', element: <CourseDetailPage /> },
    ],
  },

  // ====================================================================
  // Auth — Đăng nhập / Đăng ký giờ dùng MODAL (AuthModal) trong MainLayout.
  // Riêng forgot/reset password vẫn là trang riêng vì cần URL riêng cho email.
  // ====================================================================
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },

  // Callback Google OAuth: BE redirect về đây với ?token=...
  { path: '/auth/google/callback', element: <GoogleCallbackPage /> },

  // ====================================================================
  // Protected: Teacher / Admin pages (with MainLayout)
  // ====================================================================
  {
    path: '/teacher',
    element: (
      <ProtectedRoute roles={['TEACHER', 'ADMIN']}>
        <WithMainLayout>
          <ProtectedOutlet />
        </WithMainLayout>
      </ProtectedRoute>
    ),
    children: [
      { path: 'courses', element: <TeacherCoursesPage /> },
      { path: 'courses/:courseId', element: <TeacherCourseEditorPage /> },
    ],
  },

  // ====================================================================
  // Protected: Progress page (tiến độ học tập — with MainLayout)
  // ====================================================================
  {
    path: '/progress',
    element: (
      <ProtectedRoute>
        <WithMainLayout>
          <ProtectedOutlet />
        </WithMainLayout>
      </ProtectedRoute>
    ),
    children: [
      {index: true, element: <ProgressPage />},
    ],
  },

  // ====================================================================
  // Protected: Student routes (xem bài học - with MainLayout)
  // ====================================================================
  {
    path: '/learn',
    element: (
      <ProtectedRoute>
        <WithMainLayout>
          <ProtectedOutlet />
        </WithMainLayout>
      </ProtectedRoute>
    ),
    children: [
      { path: ':courseSlug/:lessonSlug', element: <LessonViewerPage /> },
    ],
  },

  // 404 fallback
  {
    path: '*',
    element: (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
        <div className="text-center space-y-3">
          <h1 className="text-3xl font-extrabold text-gray-900">404</h1>
          <p className="text-gray-500">Trang bạn tìm không tồn tại.</p>
          <a
            href="/"
            className="inline-block mt-3 px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-semibold hover:bg-indigo-700"
          >
            Về trang chủ
          </a>
        </div>
      </div>
    ),
  },
])

export const AppRouter: React.FC = () => {
  return <RouterProvider router={router} />
}
