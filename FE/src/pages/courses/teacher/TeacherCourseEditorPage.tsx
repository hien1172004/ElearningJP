import React, { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, Pencil, Plus, BookOpen, Eye, EyeOff, Trash2 } from 'lucide-react'
import { Tabs, Button as AntdButton, Modal } from 'antd'
import { PageHeader, LoadingState, EmptyState } from '@/components/common'
import {
  CourseFormModal,
  LessonFormModal,
  LessonCard,
} from '@/components/course'
import { SectionEditorModal } from '@/components/course/section-editor'
import {
  useCourseDetail,
  useDeleteCourse,
  usePublishCourse,
  useDeleteLesson,
  useReorderLessons,
} from '@/hooks'
import type { LessonDetail } from '@/types'

export const TeacherCourseEditorPage: React.FC = () => {
  const { courseId } = useParams<{ courseId: string }>()
  const navigate = useNavigate()
  const id = Number(courseId)

  const [activeTab, setActiveTab] = useState('overview')
  const [isCourseModalOpen, setIsCourseModalOpen] = useState(false)
  const [isLessonModalOpen, setIsLessonModalOpen] = useState(false)
  const [editingLesson, setEditingLesson] = useState<LessonDetail | null>(null)
  const [deleteLessonTarget, setDeleteLessonTarget] = useState<LessonDetail | null>(null)
  const [sectionLessonId, setSectionLessonId] = useState<number | null>(null)

  const { data: course, isLoading } = useCourseDetail(id)
  const deleteCourse = useDeleteCourse()
  const publishCourse = usePublishCourse(id)
  const deleteLesson = useDeleteLesson(id)
  const reorderLessons = useReorderLessons(id)

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

  const lessons = course.lessons ?? []

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
    setDeleteLessonTarget(null)
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

  return (
    <div className="space-y-6 pb-12">
      <PageHeader
        badge={`JLPT ${course.jlptLevel}`}
        title={<span>{course.title}</span>}
        subtitle={course.description}
        actions={
          <div className="flex flex-wrap items-center gap-2">
            {statusBadge(course.published)}
            <AntdButton
              onClick={() => publishCourse.mutate(!course.published)}
              loading={publishCourse.isPending}
              icon={course.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              className="!rounded-md !font-semibold"
            >
              {course.published ? 'Chuyển nháp' : 'Xuất bản'}
            </AntdButton>
            <Link to={`/courses/${id}`}>
              <AntdButton icon={<BookOpen className="w-4 h-4" />} className="!rounded-md">
                Xem trước
              </AntdButton>
            </Link>
          </div>
        }
      />

      <div className="max-w-5xl mx-auto px-3 sm:px-4">
        <div className="mb-4">
          <Link
            to="/teacher/courses"
            className="inline-flex items-center gap-1.5 text-sm text-gray-600 hover:text-indigo-600 transition"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay lại danh sách khóa học
          </Link>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'overview',
              label: 'Tổng quan',
              children: (
                <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-6 space-y-4">
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
                    <div className="p-4 rounded-md bg-indigo-50/50 border border-indigo-100">
                      <p className="text-2xl font-black text-indigo-600">{lessons.length}</p>
                      <p className="text-xs text-gray-500 mt-1">Bài học</p>
                    </div>
                    <div className="p-4 rounded-md bg-emerald-50/50 border border-emerald-100">
                      <p className="text-2xl font-black text-emerald-600">{course.totalEnrollments ?? 0}</p>
                      <p className="text-xs text-gray-500 mt-1">Học viên</p>
                    </div>
                    <div className="p-4 rounded-md bg-gray-50/50 border border-gray-200">
                      <p className="text-2xl font-black text-gray-600">{course.jlptLevel}</p>
                      <p className="text-xs text-gray-500 mt-1">Cấp độ</p>
                    </div>
                    <div className="p-4 rounded-md bg-gray-50/50 border border-gray-200">
                      <p className="text-2xl font-black text-gray-600">
                        {course.createdAt ? new Date(course.createdAt).toLocaleDateString('vi-VN') : '—'}
                      </p>
                      <p className="text-xs text-gray-500 mt-1">Ngày tạo</p>
                    </div>
                  </div>
                  <div className="pt-2 border-t border-gray-100">
                    <div className="flex items-center justify-between">
                      <p className="text-sm text-gray-600">
                        <strong>Slug:</strong> /courses/{course.slug}
                      </p>
                      <AntdButton
                        size="small"
                        icon={<Pencil className="w-3.5 h-3.5" />}
                        onClick={() => setIsCourseModalOpen(true)}
                      >
                        Sửa thông tin
                      </AntdButton>
                    </div>
                  </div>
                </div>
              ),
            },
            {
              key: 'lessons',
              label: `Bài học (${lessons.length})`,
              children: (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-sm text-gray-800">Danh sách bài học</h3>
                    <AntdButton
                      type="primary"
                      icon={<Plus className="w-4 h-4" />}
                      onClick={() => {
                        setEditingLesson(null)
                        setIsLessonModalOpen(true)
                      }}
                      className="!rounded-md !font-semibold"
                    >
                      Thêm bài học
                    </AntdButton>
                  </div>

                  {lessons.length === 0 && (
                    <EmptyState
                      title="Chưa có bài học nào"
                      description="Thêm bài học đầu tiên để bắt đầu giảng dạy."
                      action={
                        <AntdButton
                          type="primary"
                          icon={<Plus className="w-4 h-4" />}
                          onClick={() => setIsLessonModalOpen(true)}
                          className="!rounded-md"
                        >
                          Thêm bài học đầu tiên
                        </AntdButton>
                      }
                    />
                  )}

                  <div className="space-y-2">
                    {lessons.map((lesson, idx) => (
                      <LessonCard
                        key={lesson.id}
                        lesson={lesson}
                        index={idx}
                        total={lessons.length}
                        onEdit={() => {
                          setEditingLesson(lesson as unknown as LessonDetail)
                          setIsLessonModalOpen(true)
                        }}
                        onDelete={() => setDeleteLessonTarget(lesson as unknown as LessonDetail)}
                        onMoveUp={() => handleReorder(lesson.id, 'up')}
                        onMoveDown={() => handleReorder(lesson.id, 'down')}
                        onOpenSectionEditor={() => setSectionLessonId(lesson.id)}
                      />
                    ))}
                  </div>
                </div>
              ),
            },
            {
              key: 'publish',
              label: 'Xuất bản',
              children: (
                <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-6 space-y-6">
                  <h3 className="font-bold text-base text-gray-900">Quản lý trạng thái</h3>
                  <div className="space-y-4">
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
                        onClick={() => {
                          if (window.confirm(`Xóa khóa học "${course.title}"?`)) {
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
              ),
            },
          ]}
        />
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

      <SectionEditorModal
        open={sectionLessonId !== null}
        lessonId={sectionLessonId}
        onClose={() => setSectionLessonId(null)}
      />
    </div>
  )
}
