import { confirmDialog } from '@/components/common/ConfirmModal'
import React, { useState, useEffect, useRef } from 'react'
import { useParams, Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Pencil, Plus, BookOpen, Eye, EyeOff, Trash2, Clock, Edit3, Megaphone } from 'lucide-react'
import { Button as AntdButton, Modal } from 'antd'
import { toast } from 'sonner'
import { LoadingState, EmptyState, BackButton } from '@/components/common'
import {
  CourseFormModal,
  LessonFormModal,
  CourseOutlineSidebar,
  CourseLessonList,
  CourseStudentsPanel,
  CourseTeachersPanel,
  CourseReviewBadge,
  CourseReviewPanel,
  CourseEditLockBanner,
  CourseEditLockContext,
  CourseChangesView,
  CourseReviewCommentsPanel,
  courseEditLockReason,
  editLockHint,
  parseSelection,
  toSearchParams,
  type WorkspaceSelection,
} from '@/components/course'
import {
  useCourseDetail,
  useDeleteCourse,
  useLessonDetail,
  usePublishCourse,
  useDeleteLesson,
  useReorderLessons,
} from '@/hooks'
import type { LessonDetail, LessonOutline } from '@/types'
import { SECTION_TYPE_TO_TAB, lessonEditorPath, type LessonEditorTab } from './TeacherLessonEditorPage'
import type { ExamSummary } from '@/types/exam.types'
import { examService } from '@/services/exam.service'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'

/**
 * Không gian soạn khóa học (giáo viên): thanh tab ngang dưới thanh chú thích
 * (Tổng quan · Duyệt & xuất bản · Bài kiểm tra · Học viên · Giáo viên · Cài đặt [ADMIN]; trượt ngang khi màn hình hẹp),
 * bên dưới là nội dung của mục đang chọn. Danh sách bài học (tìm kiếm + phân trang) nằm trong Tổng quan.
 * Mục đang chọn lưu trên URL (xem `workspaceSelection`).
 */
export const TeacherCourseEditorPage: React.FC = () => {
  const { courseId } = useParams<{ courseId: string }>()
  const navigate = useNavigate()
  const id = Number(courseId)
  // Chỉ ADMIN sửa thông tin / xuất bản / xóa khóa; giáo viên được giao soạn bài học
  const currentUser = useAuthStore((s) => s.user)
  const admin = isAdmin(currentUser)

  const [searchParams, setSearchParams] = useSearchParams()
  const selection = parseSelection(searchParams)
  // Số bước lịch sử đã đi trong trang này (mỗi lần chọn mục là 1 bước, lưu ở history state)
  const location = useLocation()
  const workspaceDepth = (location.state as { workspaceDepth?: number } | null)?.workspaceDepth ?? 0
  const select = (next: WorkspaceSelection) =>
    setSearchParams(toSearchParams(next), { state: { workspaceDepth: workspaceDepth + 1 } })

  /**
   * Quay lại trang TRƯỚC khi vào không gian soạn (vd: danh sách khóa học), bỏ qua các bước
   * chuyển mục bên trong. Mở thẳng bằng link (không có trang trước) → về danh sách khóa học.
   */
  const leaveWorkspace = () => {
    const idx = (window.history.state as { idx?: number } | null)?.idx
    if (typeof idx === 'number' && idx - workspaceDepth > 0) navigate(-(workspaceDepth + 1))
    else navigate('/teacher/courses')
  }

  const [isCourseModalOpen, setIsCourseModalOpen] = useState(false)
  const [isLessonModalOpen, setIsLessonModalOpen] = useState(false)
  const [editingLesson, setEditingLesson] = useState<LessonDetail | null>(null)
  const [deleteLessonTarget, setDeleteLessonTarget] = useState<LessonOutline | null>(null)
  // Danh sách bài học trong Tổng quan — ô "Bài học" cuộn tới đây + nháy viền nhẹ
  const lessonListRef = useRef<HTMLDivElement>(null)
  const [lessonListFlash, setLessonListFlash] = useState(false)
  useEffect(() => {
    if (!lessonListFlash) return
    const t = setTimeout(() => setLessonListFlash(false), 1200)
    return () => clearTimeout(t)
  }, [lessonListFlash])

  // Đề thi & bài kiểm tra của khóa học
  const [courseExams, setCourseExams] = useState<ExamSummary[]>([])
  const [loadingExams, setLoadingExams] = useState(false)

  const { data: course, isLoading, isFetching: courseFetching } = useCourseDetail(id)
  const deleteCourse = useDeleteCourse()
  const publishCourse = usePublishCourse(id)
  const deleteLesson = useDeleteLesson(id)
  const reorderLessons = useReorderLessons(id)

  const lessons = course?.lessons ?? []
  // Đang chờ duyệt: giáo viên tạm khóa sửa (ADMIN vẫn sửa được)
  // ADMIN chỉ xem & duyệt; giáo viên bị khóa sửa khi khóa đang chờ duyệt
  const lockReason = courseEditLockReason(course, admin)
  const locked = lockReason !== null
  const lockedTitle = editLockHint(lockReason)
  // Mở lại khóa chỉ được khi đã có bản được duyệt (hiện lại đúng bản đó)
  const canReopen = !!course?.publishedVersionId
  const openLessonId = selection.view === 'lesson' ? selection.lessonId : undefined
  // Chỉ tải bài thuộc khóa này (tránh mở bài của khóa khác qua URL)
  const openLessonInCourse = openLessonId != null && lessons.some((l) => l.id === openLessonId)
  const { data: openLesson } = useLessonDetail(
    openLessonInCourse ? openLessonId : undefined,
  )

  const loadExams = async () => {
    if (!id) return
    try {
      setLoadingExams(true)
      const data = await examService.getCourseExams(id)
      setCourseExams(data || [])
    } catch (err) {
      console.error('Không thể tải bài kiểm tra của khóa học:', err)
    } finally {
      setLoadingExams(false)
    }
  }

  useEffect(() => {
    loadExams()
  }, [id])

  const handleTogglePublishExam = async (examId: number, currentlyPublished?: boolean) => {
    try {
      if (currentlyPublished) {
        await examService.unpublishExam(examId)
        toast.info('Đã ẩn đề thi với học viên')
      } else {
        await examService.publishExam(examId)
        toast.success('Đã hiện lại đề thi (bản đã duyệt) cho học viên')
      }
      loadExams()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái đề thi')
    }
  }

  const handleDeleteExam = async (exam: ExamSummary) => {
    // Đề đã được duyệt: học viên đang thi theo bản đã duyệt → chỉ đánh dấu chờ gỡ
    const approved = !!exam.publishedVersionId
    const confirmed = await confirmDialog(
      approved
        ? {
            title: `Gỡ đề thi "${exam.title}" khỏi khóa?`,
            content:
              'Đề đang có trong bản học viên học nên chưa xóa ngay: học viên vẫn thi được cho tới khi quản trị viên ' +
              'duyệt khóa lần tới, lúc đó đề mới bị gỡ. Bạn có thể hủy yêu cầu gỡ trước khi duyệt.',
            okText: 'Đánh dấu gỡ',
            cancelText: 'Hủy',
            danger: true,
          }
        : {
            title: `Xóa đề thi "${exam.title}"?`,
            content: 'Thao tác này không thể hoàn tác. Toàn bộ câu hỏi và kết quả thi liên quan sẽ bị xóa vĩnh viễn.',
            okText: 'Xóa đề thi',
            cancelText: 'Hủy',
            danger: true,
          },
    )
    if (!confirmed) return
    try {
      const res = await examService.deleteExam(exam.id)
      toast.success(res?.message || (approved ? 'Đã đánh dấu gỡ đề thi' : 'Đã xóa đề thi thành công'))
      loadExams()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Không thể xóa đề thi lúc này')
    }
  }

  const handleCancelExamRemoval = async (examId: number) => {
    try {
      await examService.cancelExamRemoval(examId)
      toast.success('Đã hủy yêu cầu gỡ đề thi')
      loadExams()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Không thể hủy yêu cầu gỡ lúc này')
    }
  }

  if (isLoading) return <LoadingState message="Đang tải thông tin khóa học..." fullHeight />

  if (!course) {
    return (
      <div className="text-center py-20 text-sm text-rose-600">
        Không tìm thấy khóa học.
        <br />
        <Link to="/teacher/courses" className="text-indigo-600 underline">Quay lại</Link>
      </div>
    )
  }

  const handleReorder = async (lessonId: number, direction: 'up' | 'down') => {
    const idx = lessons.findIndex((l) => l.id === lessonId)
    if (idx < 0) return
    const target = direction === 'up' ? idx - 1 : idx + 1
    if (target < 0 || target >= lessons.length) return
    const newOrder = [...lessons]
    ;[newOrder[idx], newOrder[target]] = [newOrder[target], newOrder[idx]]
    await reorderLessons.mutateAsync(newOrder)
  }

  const handleDeleteLesson = async () => {
    if (!deleteLessonTarget) return
    await deleteLesson.mutateAsync(deleteLessonTarget.id)
    // Bài đang mở bị xóa → về tổng quan
    if (deleteLessonTarget.id === openLessonId) select({ view: 'overview' })
    setDeleteLessonTarget(null)
  }

  const openCreateLesson = () => {
    setEditingLesson(null)
    setIsLessonModalOpen(true)
  }

  const openEditLesson = (lesson: LessonOutline | LessonDetail) => {
    setEditingLesson(lesson as unknown as LessonDetail)
    setIsLessonModalOpen(true)
  }

  // ======================== Nội dung cột phải ========================

  const overviewPanel = (
    <div className={`${CARD} p-6 space-y-4`}>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
        {/* Ô "Bài học": cuộn xuống danh sách bên dưới (chưa có bài → mở form thêm bài) */}
        <button
          type="button"
          title={lessons.length ? 'Xem danh sách bài học' : 'Thêm bài học đầu tiên'}
          onClick={() => {
            if (!lessons.length) return openCreateLesson()
            lessonListRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
            setLessonListFlash(true)
          }}
          className="p-3.5 rounded-xl bg-indigo-50/50 border border-indigo-100 hover:border-indigo-300 hover:bg-indigo-50 transition cursor-pointer"
        >
          <p className="text-2xl font-black text-indigo-600">{lessons.length}</p>
          <p className="text-xs text-gray-500 mt-1">Bài học</p>
        </button>
        {/* Ô "Học viên": mở mục Học viên */}
        <button
          type="button"
          title="Xem học viên của khóa"
          onClick={() => select({ view: 'students' })}
          className="p-3.5 rounded-xl bg-emerald-50/50 border border-emerald-100 hover:border-emerald-300 hover:bg-emerald-50 transition cursor-pointer"
        >
          <p className="text-2xl font-black text-emerald-600">{course.totalEnrollments ?? 0}</p>
          <p className="text-xs text-gray-500 mt-1">Học viên</p>
        </button>
        <div className="p-3.5 rounded-xl bg-gray-50/50 border border-gray-200">
          <p className="text-2xl font-black text-gray-600">{course.jlptLevel}</p>
          <p className="text-xs text-gray-500 mt-1">Cấp độ</p>
        </div>
        <div className="p-3.5 rounded-xl bg-gray-50/50 border border-gray-200">
          <p className="text-xl font-black text-gray-600 truncate">
            {course.updatedAt ? new Date(course.updatedAt).toLocaleDateString('vi-VN') : '—'}
          </p>
          <p className="text-xs text-gray-500 mt-1">Cập nhật lần cuối</p>
        </div>
      </div>
      {lessons.length === 0 ? (
        <EmptyState
          title="Chưa có bài học nào"
          description="Thêm bài học đầu tiên để bắt đầu giảng dạy."
          action={
            <AntdButton
              type="primary"
              icon={<Plus className="w-4 h-4" />}
              onClick={openCreateLesson}
              disabled={locked}
              title={lockedTitle}
              className="!rounded-md"
            >
              Thêm bài học đầu tiên
            </AntdButton>
          }
        />
      ) : (
        <div
          ref={lessonListRef}
          className={`scroll-mt-24 rounded-xl transition-shadow duration-500 ${lessonListFlash ? 'ring-2 ring-indigo-300 ring-offset-4 ring-offset-surface' : ''}`}
        >
        <CourseLessonList
          lessons={lessons}
          onOpen={(l) => select({ view: 'lesson', lessonId: l.id })}
          onAdd={openCreateLesson}
          onEdit={openEditLesson}
          onDelete={setDeleteLessonTarget}
          onMove={handleReorder}
          reorderPending={reorderLessons.isPending}
        />
        </div>
      )}
      <p className="text-xs text-slate-500">
        Bấm vào một bài để soạn từ vựng, ngữ pháp, chữ Hán và quiz.
      </p>
    </div>
  )

  const examsPanel = (
    <div className="space-y-4">
      <div className={`${CARD} p-4 flex flex-wrap items-center justify-between gap-3`}>
        <div>
          <h3 className="font-bold text-sm text-gray-800">Đề thi & Bài kiểm tra của khóa học</h3>
          <p className="text-xs text-gray-500 mt-0.5">
            Tạo bài kiểm tra định kỳ (Mini Test, Mid-term, Final Test) dành riêng cho học viên khóa học này.
          </p>
        </div>
        <AntdButton
          type="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => navigate(`/admin/exams/create?courseId=${id}&level=${course.jlptLevel}`)}
          disabled={locked}
          title={lockedTitle}
          className="!rounded-md !font-semibold"
        >
          Tạo bài kiểm tra mới
        </AntdButton>
      </div>

      {loadingExams ? (
        <div className="p-8 text-center text-xs text-gray-500">Đang tải danh sách bài kiểm tra...</div>
      ) : courseExams.length === 0 ? (
        <EmptyState
          card
          title="Chưa có bài kiểm tra nào"
          description="Tạo bài mini test hoặc đề thi giữa/cuối khóa để đánh giá tiến độ của học viên."
          action={
            <AntdButton
              type="primary"
              icon={<Plus className="w-4 h-4" />}
              onClick={() => navigate(`/admin/exams/create?courseId=${id}&level=${course.jlptLevel}`)}
              disabled={locked}
              title={lockedTitle}
              className="!rounded-md"
            >
              Tạo bài kiểm tra đầu tiên
            </AntdButton>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {courseExams.map((exam) => {
            const isPublished = exam.published || exam.status === 'PUBLISHED'
            return (
              <div
                key={exam.id}
                onClick={() => navigate(`/admin/exams/${exam.id}/preview`)}
                className="bg-surface rounded-xl border border-gray-200/80 hover:border-indigo-300 hover:shadow-sm p-4 shadow-xs transition flex flex-col justify-between gap-3 cursor-pointer"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5">
                      <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100">
                        {exam.jlptLevel ? `JLPT ${exam.jlptLevel}` : 'Mini-test'}
                      </span>
                      <span
                        className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${
                          isPublished
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}
                      >
                        {isPublished ? 'Đã phát hành' : 'Bản nháp'}
                      </span>
                      {exam.pendingRemoval && (
                        <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 border border-rose-200">
                          Chờ gỡ khi duyệt
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-gray-400 flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {exam.totalDurationMinutes} phút
                    </span>
                  </div>

                  <h4 className="font-bold text-sm text-gray-900 line-clamp-1">{exam.title}</h4>
                  <p className="text-xs text-gray-500">
                    Số câu hỏi: <strong className="text-gray-700">{exam.totalQuestions}</strong> câu • Điểm tối đa: <strong className="text-gray-700">{exam.totalScaledMaxScore || 180}</strong>
                  </p>
                </div>

                <div
                  className="pt-2 border-t border-gray-100 flex items-center justify-between gap-2"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="flex items-center gap-1.5">
                    <Link to={`/admin/exams/${exam.id}/preview`}>
                      <AntdButton size="small" icon={<Edit3 className="w-3.5 h-3.5" />} className="!rounded-md">
                        {admin ? 'Xem đề' : 'Sửa đề'}
                      </AntdButton>
                    </Link>
                    {/* Đề gắn với khóa: nội dung lên cùng lần duyệt khóa; ADMIN chỉ ẩn / hiện lại bản đã duyệt */}
                    {admin && (isPublished || exam.publishedVersionId) ? (
                      <AntdButton
                        size="small"
                        onClick={() => handleTogglePublishExam(exam.id, isPublished)}
                        className="!rounded-md"
                      >
                        {isPublished ? 'Ẩn đề' : 'Hiện lại'}
                      </AntdButton>
                    ) : admin ? (
                      <span className="text-[11px] text-slate-400">Lên cùng lần duyệt khóa</span>
                    ) : null}
                  </div>

                  {exam.pendingRemoval ? (
                    <AntdButton
                      size="small"
                      onClick={() => handleCancelExamRemoval(exam.id)}
                      disabled={locked}
                      title={lockedTitle}
                      className="!rounded-md"
                    >
                      Hủy gỡ
                    </AntdButton>
                  ) : (
                    <AntdButton
                      size="small"
                      danger
                      icon={<Trash2 className="w-3.5 h-3.5" />}
                      onClick={() => handleDeleteExam(exam)}
                      disabled={locked}
                      title={lockedTitle ?? (exam.publishedVersionId ? 'Gỡ khỏi khóa (khi duyệt lần tới)' : 'Xóa đề')}
                      className="!rounded-md"
                    />
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )

  const studentsPanel = (
    <div className="space-y-4">
      <div className={`${CARD} p-4 flex flex-wrap items-center justify-between gap-3`}>
        <div>
          <h3 className="font-bold text-sm text-gray-800">Học viên của khóa học</h3>
          <p className="text-xs text-gray-500 mt-0.5">
            Theo dõi tiến độ, điểm bài kiểm tra và lần học gần nhất của từng học viên.
          </p>
        </div>
        <AntdButton
          icon={<Megaphone className="w-4 h-4" />}
          onClick={() => navigate(`/admin/broadcasts?courseId=${id}`)}
          className="!rounded-md"
        >
          Gửi thông báo cho học viên
        </AntdButton>
      </div>
      <CourseStudentsPanel key={id} courseId={id} />
    </div>
  )

  const settingsPanel = (
    <div className={`${CARD} p-6 space-y-6`}>
      <h3 className="font-bold text-base text-gray-900">Cài đặt khóa học</h3>
      <div className="space-y-4">
        <div className="flex items-center justify-between p-4 rounded-md border border-gray-200">
          <div>
            <p className="font-semibold text-sm text-gray-900">Thông tin khóa học</p>
            <p className="text-xs text-gray-500 mt-0.5">Tên, cấp độ, mô tả và ảnh bìa.</p>
          </div>
          <AntdButton icon={<Pencil className="w-4 h-4" />} onClick={() => setIsCourseModalOpen(true)} className="!rounded-md">
            Sửa thông tin
          </AntdButton>
        </div>

        <div className="flex items-center justify-between p-4 rounded-md border border-gray-200">
          <div>
            <p className="font-semibold text-sm text-gray-900">Hiển thị với học viên</p>
            <p className="text-xs text-gray-500 mt-0.5">
              {course.published
                ? 'Khóa học đang công khai với bản đã duyệt gần nhất.'
                : canReopen
                  ? 'Khóa học đang ẩn. Mở lại sẽ hiện đúng bản đã duyệt gần nhất (không kèm phần đang soạn).'
                  : 'Khóa học chưa có bản nào được duyệt — giáo viên cần gửi duyệt trước.'}
            </p>
          </div>
          <AntdButton
            type={course.published ? 'default' : 'primary'}
            icon={course.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            loading={publishCourse.isPending}
            disabled={!course.published && !canReopen}
            onClick={() => publishCourse.mutate(!course.published)}
            className="!rounded-md !font-semibold"
          >
            {course.published ? 'Chuyển nháp' : 'Mở lại khóa'}
          </AntdButton>
        </div>

        <div className="flex items-center justify-between p-4 rounded-md border border-rose-100 bg-rose-50/50">
          <div>
            <p className="font-semibold text-sm text-rose-800">Xóa khóa học</p>
            <p className="text-xs text-rose-600 mt-0.5">
              Xóa mềm khóa học. Dữ liệu học viên vẫn được giữ.
            </p>
          </div>
          <AntdButton
            danger
            icon={<Trash2 className="w-4 h-4" />}
            onClick={async () => {
              const ok = await confirmDialog({
                title: `Xóa khóa học "${course.title}"?`,
                content: 'Toàn bộ bài học và nội dung trong khóa học này sẽ bị xóa vĩnh viễn.',
                okText: 'Xóa khóa học',
                cancelText: 'Hủy',
                danger: true,
              })
              if (ok) {
                deleteCourse.mutate(id, {
                  onSuccess: () => navigate('/teacher/courses'),
                })
              }
            }}
            loading={deleteCourse.isPending}
            className="!rounded-md"
          >
            Xóa khóa học
          </AntdButton>
        </div>
      </div>
    </div>
  )

  /** Điều hướng sang trang soạn bài; section cũ (id) → tab tương ứng loại của nó. */
  const lessonRedirect = (section?: number | 'quiz') => {
    if (selection.view !== 'lesson') return null
    let tab: LessonEditorTab | undefined
    if (section === 'quiz') tab = 'quiz'
    else if (typeof section === 'number') {
      if (!openLesson) return null // chờ tải bài để biết loại của section
      const found = openLesson.sections?.find((s) => s.id === section)
      tab = found ? SECTION_TYPE_TO_TAB[found.sectionType] : undefined
    }
    return <Navigate to={lessonEditorPath(id, selection.lessonId, tab)} replace />
  }

  const lessonIndex = openLessonId != null ? lessons.findIndex((l) => l.id === openLessonId) : -1
  const mainPanel =
    selection.view === 'lesson' ? (
      lessonIndex < 0 && courseFetching ? (
        // Bài vừa tạo chưa có trong danh sách cho tới khi khóa học tải lại
        <LoadingState message="Đang tải bài học..." />
      ) : lessonIndex < 0 ? (
        <EmptyState
          card
          title="Không tìm thấy bài học"
          description="Bài học không thuộc khóa này hoặc đã bị xóa."
          action={<AntdButton onClick={() => select({ view: 'overview' })}>Về tổng quan</AntdButton>}
        />
      ) : (
        // Bài học mở ở trang riêng (4 tab Từ vựng · Ngữ pháp · Chữ Hán · Quiz).
        // replace: link cũ ?lesson=..&section=.. không để lại bước lịch sử lặp vòng.
        lessonRedirect(selection.section) ?? <LoadingState message="Đang tải bài học..." />
      )
    ) : selection.view === 'exams' ? (
      examsPanel
    ) : selection.view === 'students' ? (
      studentsPanel
    ) : selection.view === 'review' ? (
      <div className="space-y-4">
        <CourseReviewPanel course={course} admin={admin} currentUserId={currentUser?.id} />
        <CourseReviewCommentsPanel course={course} admin={admin} />
        <CourseChangesView courseId={id} />
      </div>
    ) : selection.view === 'teachers' ? (
      <CourseTeachersPanel courseId={id} canManage={admin} />
    ) : selection.view === 'settings' && admin ? (
      settingsPanel
    ) : (
      overviewPanel
    )

  return (
    <CourseEditLockContext.Provider value={lockReason}>
    <div className="max-w-7xl mx-auto px-3 sm:px-4 space-y-4 pb-12" data-edit-locked={locked || undefined}>
      <BackButton onClick={leaveWorkspace} />

      {/* Header khóa học: luôn thấy trạng thái + Xem trước + Xuất bản */}
      <div className={`${CARD} px-4 py-3 flex flex-wrap items-center justify-between gap-3`}>
        <div className="min-w-0">
          <h1 className="text-xl font-bold text-sumi leading-tight truncate">{course.title}</h1>
          {course.description && (
            <p className="text-sm text-nibi line-clamp-1 max-w-3xl" title={course.description}>
              {course.description}
            </p>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <CourseReviewBadge course={course} />
          <Link to={`/courses/${id}`}>
            <AntdButton icon={<BookOpen className="w-4 h-4" />} className="!rounded-md">
              Xem trước
            </AntdButton>
          </Link>
          {/* ADMIN: nội dung mới chỉ lên qua duyệt yêu cầu; ở đây chỉ ẩn / mở lại bản đã duyệt */}
          {admin && (course.published || canReopen) && (
            <AntdButton
              type={course.published ? 'default' : 'primary'}
              onClick={() => publishCourse.mutate(!course.published)}
              loading={publishCourse.isPending}
              icon={course.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              className="!rounded-md !font-semibold"
            >
              {course.published ? 'Chuyển nháp' : 'Mở lại khóa'}
            </AntdButton>
          )}
        </div>
      </div>

      <CourseEditLockBanner />

      {/* Mục lục dạng thanh tab ngang ngay dưới thanh chú thích, nội dung chiếm toàn bộ chiều ngang */}
      <div className="lg:sticky-panel z-10">
        <CourseOutlineSidebar
          lessonCount={lessons.length}
          selection={selection}
          onSelect={select}
          examCount={courseExams.length}
          studentCount={course.totalEnrollments ?? 0}
          showSettings={admin}
          reviewPending={course.reviewStatus === 'PENDING_REVIEW'}
        />
      </div>
      <main className="min-w-0">{mainPanel}</main>

      {/* Modals */}
      <CourseFormModal
        open={isCourseModalOpen}
        course={course}
        onClose={() => setIsCourseModalOpen(false)}
      />

      <LessonFormModal
        open={isLessonModalOpen}
        courseId={id}
        lesson={editingLesson}
        suggestedOrder={lessons.length + 1}
        onClose={() => {
          setIsLessonModalOpen(false)
          setEditingLesson(null)
        }}
        onSuccess={(saved) => {
          // Tạo bài mới → mở ngay bài đó để soạn nội dung
          if (!editingLesson && saved?.id) select({ view: 'lesson', lessonId: saved.id })
        }}
      />

      <Modal
        open={!!deleteLessonTarget}
        title="Xác nhận xóa bài học"
        onOk={handleDeleteLesson}
        onCancel={() => setDeleteLessonTarget(null)}
        okText="Xóa"
        cancelText="Huỷ"
        okButtonProps={{ danger: true, loading: deleteLesson.isPending }}
        centered
      >
        <p className="text-sm text-gray-600">
          Bạn có chắc muốn xóa bài học <strong>"{deleteLessonTarget?.title}"</strong>?
        </p>
      </Modal>
    </div>
    </CourseEditLockContext.Provider>
  )
}
