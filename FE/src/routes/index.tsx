import React from 'react'
import { createBrowserRouter, Outlet, RouterProvider, Navigate } from 'react-router-dom'
import { MainLayout } from '@/layouts'
import {
  HomePage,
  ForgotPasswordPage,
  ResetPasswordPage,
  DictionaryPage,
  CourseCatalogPage,
  CourseDetailPage,
  TeacherCoursesPage,
  TeacherCourseEditorPage,
  LessonViewerPage,
  ProgressPage,
  GoogleCallbackPage,
  ExamLobbyPage,
  ExamRoomPage,
  ExamResultPage,
  ExamReviewPage,
  ExamHistoryPage,
  AdminExamPreviewPage,
  AdminExamCreatePage,
  AdminDashboardPage,
  SpeakingLobbyPage,
  SpeakingPracticePage,
  ScenarioDetailPage,
  SpeakingFreeTalkPage,
  ProfilePage,
} from '@/pages'
import { AuthGatePage } from '@/pages/auth/AuthGatePage'
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
      { path: 'leaderboard', element: <Navigate to="/exams" replace /> },
      { path: 'exams/leaderboard', element: <Navigate to="/exams" replace /> },
      { path: 'exam-history', element: <ExamHistoryPage /> },
      { path: 'exams/result/:attemptId', element: <ExamResultPage /> },
      { path: 'speaking', element: <SpeakingLobbyPage /> },
      { path: 'speaking/free', element: <SpeakingFreeTalkPage /> },
      { path: 'speaking/scenario/:scenarioId', element: <ScenarioDetailPage /> },
      { path: 'speaking/session/:sessionId', element: <SpeakingPracticePage /> },
    ],
  },

  // Auth: /login & /register nay chỉ là "cửa ngõ" mở AuthModal (không còn trang riêng)
  { path: '/login', element: <AuthGatePage mode="login" /> },
  { path: '/register', element: <AuthGatePage mode="register" /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  // OAuth callback từ Google — không dùng MainLayout
  { path: '/auth/google/callback', element: <GoogleCallbackPage /> },

  // ====================================================================
  // Dedicated fullscreen routes (No header/footer distractions)
  // ====================================================================
  {
    path: '/exams/:examId/taking/:attemptId',
    element: <ExamRoomPage />,
  },
  {
    path: '/exams/review/:attemptId',
    element: <ExamReviewPage />,
  },

  // ====================================================================
  // Protected: User Profile (with MainLayout)
  // ====================================================================
  {
    path: '/profile',
    element: (
      <ProtectedRoute>
        <WithMainLayout>
          <ProtectedOutlet />
        </WithMainLayout>
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <ProfilePage /> },
    ],
  },

  // ====================================================================
  // Protected: Admin Exam Management Routes
  // ====================================================================
  {
    path: '/admin',
    element: (
      <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
        <WithMainLayout>
          <ProtectedOutlet />
        </WithMainLayout>
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: 'dashboard', element: <AdminDashboardPage /> },
      { path: 'exams', element: <AdminDashboardPage /> },
      { path: 'exams/:examId/preview', element: <AdminExamPreviewPage /> },
      { path: 'exams/create', element: <AdminExamCreatePage /> },
    ],
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
  // Protected: Trang tiến độ học tập (BE yêu cầu auth cho /progress/**)
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
      { index: true, element: <ProgressPage /> },
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

export const AppRouter: React.FC = () => <RouterProvider router={router} />
