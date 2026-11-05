import { confirmDialog } from '@/components/common/ConfirmModal'
import React, { useState, useEffect } from 'react'
import { useParams, Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Pencil, Plus, BookOpen, Eye, EyeOff, Trash2, Clock, Edit3, Megaphone } from 'lucide-react'
import { Button as AntdButton, Modal } from 'antd'
import { toast } from 'sonner'
import { LoadingState, EmptyState, BackButton } from '@/components/common'
import {
  CourseFormModal,
  LessonFormModal,
  CourseOutlineSidebar,
  CourseStudentsPanel,
  LessonWorkspacePanel,
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
import type { ExamSummary } from '@/types/exam.types'
import { examService } from '@/services/exam.service'

const CARD = 'bg-white rounded-xl border border-suna shadow-xs'

/**
 * Không gian soạn khóa học (giáo viên) — bố cục 2 cột:
 * trái là mục lục (Tổng quan · Bài học → mục/quiz · Bài kiểm tra · Học viên · Cài đặt),
 * phải là nội dung của mục đang chọn. Mục đang chọn lưu trên URL (xem `workspaceSelection`).
 */
export const TeacherCourseEditorPage: React.FC = () => {
  const { courseId } = useParams<{ courseId: string }>()
  const navigate = useNavigate()
  const id = Number(courseId)

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

  // Đề thi & bài kiểm tra của khóa học
  const [courseExams, setCourseExams] = useState<ExamSummary[]>([])
  const [loadingExams, setLoadingExams] = useState(false)

  const { data: course, isLoading, isFetching: courseFetching } = useCourseDetail(id)
  const deleteCourse = useDeleteCourse()
  const publishCourse = usePublishCourse(id)
  const deleteLesson = useDeleteLesson(id)
  const reorderLessons = useReorderLessons(id)

  const lessons = course?.lessons ?? []
  const openLessonId = selection.view === 'lesson' ? selection.lessonId : undefined
  // Chỉ tải bài thuộc khóa này (tránh mở bài của khóa khác qua URL)
  const openLessonInCourse = openLessonId != null && lessons.some((l) => l.id === openLessonId)
  const { data: openLesson, isLoading: openLessonLoading } = useLessonDetail(
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
        toast.info('Đã thu hồi đề thi về bản nháp')
      } else {
        await examService.publishExam(examId)
        toast.success('Đã xuất bản đề thi cho học viên làm bài')
      }
      loadExams()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái đề thi')
    }
  }

  const handleDeleteExam = async (examId: number, examTitle: string) => {
    const confirmed = await confirmDialog({
      title: `Xóa đề thi "${examTitle}"?`,
      content: 'Thao tác này không thể hoàn tác. Toàn bộ câu hỏi và kết quả thi liên quan sẽ bị xóa vĩnh viễn.',
      okText: 'Xóa đề thi',
      cancelText: 'Hủy',
      danger: true,
    })
    if (!confirmed) return
    try {
      await examService.deleteExam(examId)
      toast.success('Đã xóa đề thi thành công')
      loadExams()
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Không thể xóa đề thi lúc này')
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
    await reorderLessons.mutateAsync(newOrder.map((l) => l.id))
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

  const statusBadge = (pub: boolean) =>
    pub ? (
      <span className="px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-700 text-xs font-bold border border-emerald-200">
        Đã xuất bản
      </span>
    ) : (
      <span className="px-2.5 py-1 rounded-md bg-gray-100 text-gray-600 text-xs font-bold border border-gray-200">
        Bản nháp
      </span>
    )

  // ======================== Nội dung cột phải ========================

  const overviewPanel = (
    <div className={`${CARD} p-6 space-y-4`}>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
        <div
          onClick={() => (lessons[0] ? select({ view: 'lesson', lessonId: lessons[0].id }) : openCreateLesson())}
          className="p-3.5 rounded-xl bg-indigo-50/50 border border-indigo-100 hover:border-indigo-300 hover:bg-indigo-50 transition cursor-pointer"
        >
          <p className="text-2xl font-black text-indigo-600">{lessons.length}</p>
          <p className="text-xs text-gray-500 mt-1">Bài học</p>
        </div>
        <div className="p-3.5 rounded-xl bg-emerald-50/50 border border-emerald-100">
          <p className="text-2xl font-black text-emerald-600">{course.totalEnrollments ?? 0}</p>
          <p className="text-xs text-gray-500 mt-1">Học viên</p>
        </div>
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
      {lessons.length === 0 && (
        <EmptyState
          title="Chưa có bài học nào"
          description="Thêm bài học đầu tiên để bắt đầu giảng dạy."
          action={
            <AntdButton type="primary" icon={<Plus className="w-4 h-4" />} onClick={openCreateLesson} className="!rounded-md">
              Thêm bài học đầu tiên
            </AntdButton>
          }
        />
      )}
      <p className="text-xs text-slate-500">
        Chọn một bài ở mục lục bên trái để soạn chữ Hán, từ vựng, ngữ pháp và quiz.
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
                className="bg-white rounded-xl border border-gray-200/80 hover:border-indigo-300 hover:shadow-sm p-4 shadow-xs transition flex flex-col justify-between gap-3 cursor-pointer"
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
                        Sửa đề
                      </AntdButton>
                    </Link>
                    <AntdButton
                      size="small"
                      onClick={() => handleTogglePublishExam(exam.id, isPublished)}
                      className="!rounded-md"
                    >
                      {isPublished ? 'Gỡ xuất bản' : 'Xuất bản'}
                    </AntdButton>
                  </div>

                  <AntdButton
                    size="small"
                    danger
                    icon={<Trash2 className="w-3.5 h-3.5" />}
                    onClick={() => handleDeleteExam(exam.id, exam.title)}
                    className="!rounded-md"
                  />
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
            <p className="font-semibold text-sm text-gray-900">Xuất bản khóa học</p>
            <p className="text-xs text-gray-500 mt-0.5">
              {course.published
                ? 'Khóa học đang công khai. Học viên có thể đăng ký.'
                : 'Khóa học đang ẩn. Chỉ bạn mới thấy được.'}
            </p>
          </div>
          <AntdButton
            type={course.published ? 'default' : 'primary'}
            icon={course.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            loading={publishCourse.isPending}
            onClick={() => publishCourse.mutate(!course.published)}
            className="!rounded-md !font-semibold"
          >
            {course.published ? 'Gỡ xuất bản' : 'Xuất bản ngay'}
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
        <LessonWorkspacePanel
          lessonId={selection.lessonId}
          lessonNumber={lessonIndex + 1}
          section={selection.section}
          lesson={openLesson}
          isLoading={openLessonLoading}
          onSelect={select}
          onEditLesson={() => openEditLesson(openLesson ?? lessons[lessonIndex])}
        />
      )
    ) : selection.view === 'exams' ? (
      examsPanel
    ) : selection.view === 'students' ? (
      studentsPanel
    ) : selection.view === 'settings' ? (
      settingsPanel
    ) : (
      overviewPanel
    )

  return (
    <div className="max-w-7xl mx-auto px-3 sm:px-4 space-y-4 pb-12">
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
          {statusBadge(course.published)}
          <Link to={`/courses/${id}`}>
            <AntdButton icon={<BookOpen className="w-4 h-4" />} className="!rounded-md">
              Xem trước
            </AntdButton>
          </Link>
          <AntdButton
            type={course.published ? 'default' : 'primary'}
            onClick={() => publishCourse.mutate(!course.published)}
            loading={publishCourse.isPending}
            icon={course.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            className="!rounded-md !font-semibold"
          >
            {course.published ? 'Chuyển nháp' : 'Xuất bản'}
          </AntdButton>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[18rem_minmax(0,1fr)] gap-4 items-start">
        <aside className="lg:sticky lg:top-20 lg:max-h-[calc(100vh-6rem)] lg:overflow-y-auto">
          <CourseOutlineSidebar
            lessons={lessons}
            selection={selection}
            onSelect={select}
            openLesson={openLesson}
            openLessonLoading={openLessonLoading}
            examCount={courseExams.length}
            studentCount={course.totalEnrollments ?? 0}
            onAddLesson={openCreateLesson}
            onEditLesson={openEditLesson}
            onDeleteLesson={setDeleteLessonTarget}
            onMoveLesson={handleReorder}
            reorderPending={reorderLessons.isPending}
          />
        </aside>
        <main className="min-w-0">{mainPanel}</main>
      </div>

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
  )
}
