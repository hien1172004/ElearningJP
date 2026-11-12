export { useLogin } from './useLogin'
export { useRegister } from './useRegister'
export { useLogout } from './useLogout'
export { useForgotPassword } from './useForgotPassword'
export { useResetPassword } from './useResetPassword'
export { useValidateResetToken } from './useValidateResetToken'
export { useUserProfile, useUpdateProfile, useChangeAvatar, USER_PROFILE_QUERY_KEY } from './useUserProfile'
export { useChangePassword } from './useChangePassword'
export { useAdminUsers, useUpdateUserStatus, useCreateUser } from './useAdminUsers'
export { useAdminDashboard } from './useAdminDashboard'
export {
  useAdminScenarios,
  useAdminScenario,
  useCreateScenario,
  useUpdateScenario,
  useDeleteScenario,
} from './useAdminScenarios'
export { useModerationQueue, useModerationBrowse, useModerationAction } from './useAdminModeration'
export { useReportComment } from './useReportComment'
export { useRecipientCount, useBroadcastHistory, useSendBroadcast } from './useBroadcasts'
export { useCourseStudents, useStudentCourseDetail } from './useCourseStudents'
export { useCourseTeachers, useAssignCourseTeacher, useUnassignCourseTeacher } from './useCourseTeachers'
export { useSaveCourseVersion, useCourseChanges } from './useCourseReview'
export {
  useLessonReviewStates,
  useLessonReviewState,
  useLessonReviewHistory,
  useLessonReviewQueue,
  useSubmitLessonReviews,
  useWithdrawLessonReview,
  useApproveLessonReview,
  useRejectLessonReview,
  useSaveLessonToLive,
} from './useLessonReview'
export { useCourseEditLock, useCourseEditSession, useLessonDraftActions, useArchivedLessonDraft, useLessonReviewRequestContent } from './useCourseEditSession'
export { useFormAutosave, type AutosaveSnapshot, type FormAutosave } from './useFormAutosave'

// Đăng nhập bằng Google (GSI idToken + luồng OAuth2 redirect)
export { useGoogleLogin, useGoogleCallback } from './useGoogleLogin'

// Course / Lesson / Enrollment
export { useCourses } from './useCourses'
export { useCourseDetail, useFetchFreshCourseDetail, useInvalidateCourseDraftState } from './useCourseDetail'
export { useCourseContentUpdates, type CourseContentAudience } from './useCourseContentUpdates'
export { useCreateCourse } from './useCreateCourse'
export { useUpdateCourse } from './useUpdateCourse'
export { useDeleteCourse } from './useDeleteCourse'
export { usePublishCourse } from './usePublishCourse'
export { useEnrollCourse } from './useEnrollCourse'
export { useMyEnrollments } from './useMyEnrollments'
export { useLessonsByCourse } from './useLessonsByCourse'
export { useLessonDetail } from './useLessonDetail'
export { useCreateLesson } from './useCreateLesson'
export { useUpdateLesson } from './useUpdateLesson'
export { useDeleteLesson } from './useDeleteLesson'
export { useReorderLessons } from './useReorderLessons'
export { useOnConcurrentReload } from './useOnConcurrentReload'
export { useStartLesson } from './useStartLesson'
export { useCompleteLesson } from './useCompleteLesson'
export { useSectionMutations, useKanjiItemMutations, useVocabItemMutations, useGrammarItemMutations } from './useSectionMutations'
export { useSectionImport, useSectionExport } from './useSectionImport'
export { useKanjiItemDetail } from './useKanjiItemDetail'
export { useKanjiDetail } from './useKanjiDetail'
export { useVocabItemDetail } from './useVocabItemDetail'
export { useGrammarItemDetail } from './useGrammarItemDetail'
export { useLessonQuiz, useSubmitQuiz } from './useLessonQuiz'
export { useLessonQuizManage } from './useLessonQuizManage'
export { useQuizItemMutations } from './useQuizItemMutations'
export { useReorderQuizQuestions } from './useReorderQuizQuestions'

// Progress page — Bước 2 & 3 (ghi sự kiện học tập)
export { useRecordLookup } from './useRecordLookup'
export { useStudyTimer } from './useStudyTimer'

// Progress page — Bước 4 (đọc dữ liệu tiến độ)
export { useProgressGoal, useUpdateProgressGoal } from './useProgressGoal'
export { useProgressSummary } from './useProgressSummary'
export type { UseProgressSummaryParams } from './useProgressSummary'

// Progress page — Khối 2 (lịch học + streak) & Khối 4 (top từ đã tra)
export { useProgressCalendar } from './useProgressCalendar'
export { useTopLookups } from './useTopLookups'
export type { UseTopLookupsParams } from './useTopLookups'

// Progress page — Khối 3 (biểu đồ tiến độ)
export { useProgressChart } from './useProgressChart'
export type { UseProgressChartParams } from './useProgressChart'

// Progress page — Khối 5 (bảng xếp hạng tuần)
export { useLeaderboard, useMyRank, useRefreshLeaderboard } from './useLeaderboard'

// Module bình luận (tra cứu từ điển + bài học)
export {
  useComments,
  useCommentReplies,
  useCreateComment,
  useUpdateComment,
  useDeleteComment,
  useAddReply,
  useDeleteReply,
  useToggleCommentReaction,
  useToggleReplyReaction,
  useResolveCommentLink,
} from './useComments'

// Module thông báo (chuông báo + real-time STOMP)
export {
  useUnreadCount,
  useRecentNotifications,
  useNotifications,
  useMarkAsRead,
  useMarkAllAsRead,
} from './useNotifications'
export { useNotificationSocket } from './useNotificationSocket'

// Module SRS (Spaced Repetition System)
export { useSrsDue } from './useSrsDue'
export { useSrsStats } from './useSrsStats'
export { useSrsReview } from './useSrsReview'
export { useSrsItems } from './useSrsItems'
export { useArchiveSrsItem, useRestoreSrsItem } from './useSrsItemActions'
export { useSrsForecast } from './useSrsForecast'
export { useAddToSrs } from './useAddToSrs'
export { useSrsSavedIds } from './useSrsSavedIds'

// Module Web Push Notification
export { useWebPush } from './useWebPush'

// Khoá cuộn trang khi modal / drawer tự dựng đang mở
export { useBodyScrollLock } from './useBodyScrollLock'
