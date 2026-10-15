import React from 'react'
import { createBrowserRouter, Outlet, RouterProvider } from 'react-router-dom'
import { MainLayout } from '@/layouts'
import {
  HomePage,
  LoginPage,
  RegisterPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  DictionaryPage,
  CourseCatalogPage,
  CourseDetailPage,
  TeacherCoursesPage,
  TeacherCourseEditorPage,
  LessonViewerPage,
  ExamLobbyPage,
  ExamRoomPage,
  ExamResultPage,
  ExamReviewPage,
  ExamHistoryPage,
  AdminExamPreviewPage,
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
      { path: 'exams', element: <ExamLobbyPage /> },
      { path: 'exam-history', element: <ExamHistoryPage /> },
      { path: 'exams/result/:attemptId', element: <ExamResultPage /> },
    ],
  },

  // Auth pages — không dùng MainLayout
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },

  // ====================================================================
  // Dedicated fullscreen exam routes (No header/footer distractions)
  // ====================================================================
  {
    path: '/exams/:examId/taking/:attemptId',
    element: <ExamRoomPage />,
  },
  {
    path: '/exams/review/:attemptId',
    element: <ExamReviewPage />,
  },
  {
    path: '/admin/exams/:examId/preview',
    element: <AdminExamPreviewPage />,
  },

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
