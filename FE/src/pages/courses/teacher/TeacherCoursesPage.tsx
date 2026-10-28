import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Edit3, Trash2, Eye, EyeOff } from 'lucide-react'
import { Table, Tag, Button as AntdButton, Tooltip } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PageHeader, LoadingState, EmptyState } from '@/components/common'
import { CourseFormModal } from '@/components/course'
import { ConfirmModal } from '@/components/common'
import { useCourses, useDeleteCourse } from '@/hooks'
import { courseService } from '@/services'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuthStore } from '@/stores'
import type { CourseSummary } from '@/types'

function statusBadge(published: boolean, archivedAt?: string | null) {
  if (archivedAt) return <Tag className="rounded-md" color="red">Đã lưu trữ</Tag>
  if (published) return <Tag className="rounded-md" color="green">Đã xuất bản</Tag>
  return <Tag className="rounded-md" color="default">Bản nháp</Tag>
}

export const TeacherCoursesPage: React.FC = () => {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const user = useAuthStore((s) => s.user)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCourse, setEditingCourse] = useState<CourseSummary | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CourseSummary | null>(null)
  const [publishingId, setPublishingId] = useState<number | null>(null)

  const { data, isLoading } = useCourses({ admin: true })
  const deleteCourse = useDeleteCourse()

  const handlePublish = async (course: CourseSummary, publish: boolean) => {
    setPublishingId(course.id)
    try {
      await courseService.publishCourse(course.id, publish)
      toast.success(publish ? 'Đã xuất bản khóa học' : 'Đã chuyển về bản nháp')
      qc.invalidateQueries({ queryKey: ['courses'] })
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'Không thể thay đổi trạng thái.'
      toast.error('Cập nhật trạng thái thất bại', { description: msg })
    } finally {
      setPublishingId(null)
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    await deleteCourse.mutateAsync(deleteTarget.id)
    setDeleteTarget(null)
  }

  const courses = data?.items ?? []

  const columns: ColumnsType<CourseSummary> = [
    {
      title: 'Tiêu đề',
      key: 'title',
      render: (_, record) => (
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded bg-indigo-50 text-indigo-700 font-bold text-sm flex items-center justify-center border border-indigo-200 shrink-0">
            {record.title?.[0] ?? '日'}
          </div>
          <div>
            <p className="font-semibold text-sm text-gray-900">{record.title}</p>
            <p className="text-xs text-gray-400">/{record.slug}</p>
          </div>
        </div>
      ),
    },
    {
      title: 'Cấp độ',
      dataIndex: 'jlptLevel',
      width: 80,
      render: (lvl) => <span className="font-bold text-sm text-indigo-600">{lvl}</span>,
    },
    {
      title: 'Bài học',
      dataIndex: 'totalLessons',
      width: 80,
      render: (n) => n ?? 0,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'published',
      key: 'status',
      width: 130,
      render: (pub, record) => statusBadge(pub, record.archivedAt),
    },
    {
      title: 'Hành động',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        <div className="flex items-center gap-1.5 flex-wrap">
          <Tooltip title="Sửa khóa học">
            <AntdButton
              size="small"
              icon={<Edit3 className="w-4 h-4" />}
              onClick={() => {
                setEditingCourse(record)
                setIsModalOpen(true)
              }}
            />
          </Tooltip>
          <Tooltip title={record.published ? 'Gỡ xuất bản' : 'Xuất bản'}>
            <AntdButton
              size="small"
              loading={publishingId === record.id}
              icon={record.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              onClick={() => handlePublish(record, !record.published)}
            />
          </Tooltip>
          <Tooltip title="Quản lý bài học">
            <AntdButton
              size="small"
              type="primary"
              onClick={() => navigate(`/teacher/courses/${record.id}`)}
            >
              Quản lý
            </AntdButton>
          </Tooltip>
          <Tooltip title="Xóa">
            <AntdButton
              size="small"
              danger
              icon={<Trash2 className="w-4 h-4" />}
              onClick={() => setDeleteTarget(record)}
            />
          </Tooltip>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6 pb-12">
      <PageHeader
        badge="Quản lý khóa học"
        title="Khoá học của tôi"
        subtitle={`Xin chào ${user?.fullName || user?.email || 'Giáo viên'}! Quản lý danh sách khóa học của bạn.`}
        actions={
          <AntdButton
            type="primary"
            size="large"
            icon={<Plus className="w-5 h-5" />}
            onClick={() => {
              setEditingCourse(null)
              setIsModalOpen(true)
            }}
            className="!rounded-md !font-bold"
          >
            Tạo khóa học mới
          </AntdButton>
        }
      />

      <div className="max-w-7xl mx-auto px-3 sm:px-4">
        {isLoading && <LoadingState message="Đang tải danh sách khóa học..." />}

        {!isLoading && courses.length === 0 && (
          <EmptyState
            title="Chưa có khóa học nào"
            description="Tạo khóa học đầu tiên để bắt đầu giảng dạy."
            action={
              <AntdButton
                type="primary"
                size="large"
                icon={<Plus className="w-5 h-5" />}
                onClick={() => {
                  setEditingCourse(null)
                  setIsModalOpen(true)
                }}
                className="!rounded-md !font-bold"
              >
                Tạo khóa học đầu tiên
              </AntdButton>
            }
          />
        )}

        {!isLoading && courses.length > 0 && (
          <Table<CourseSummary>
            dataSource={courses}
            rowKey="id"
            pagination={{ pageSize: 10, showSizeChanger: false }}
            columns={columns}
          />
        )}
      </div>

      <CourseFormModal
        open={isModalOpen}
        course={editingCourse}
        onClose={() => {
          setIsModalOpen(false)
          setEditingCourse(null)
        }}
        onSuccess={() => {
          setIsModalOpen(false)
          setEditingCourse(null)
        }}
      />

      <ConfirmModal
        open={!!deleteTarget}
        title="Xác nhận xóa khóa học"
        content={
          <p className="text-sm text-gray-600">
            Bạn có chắc muốn xóa khóa học{' '}
            <strong>"{deleteTarget?.title}"</strong>? Hành động này không thể hoàn tác.
          </p>
        }
        okText="Xóa"
        danger
        isLoading={deleteCourse.isPending}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
