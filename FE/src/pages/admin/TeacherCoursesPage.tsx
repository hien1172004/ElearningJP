import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Edit3, Trash2, Eye, EyeOff } from 'lucide-react'
import { Table, Tag, Button as AntdButton, Tooltip, Select } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PageHeader, LoadingState, EmptyState, SearchInput, FilterBar } from '@/components/common'
import { CourseFormModal, CourseReviewBadge } from '@/components/course'
import { ConfirmModal } from '@/components/common'
import { useCourses, useDeleteCourse } from '@/hooks'
import { courseService } from '@/services'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { CourseSummary, JlptLevel } from '@/types'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'
import { QUERY_KEYS } from '@/lib/queryKeys'

const PAGE_SIZE = 10

const LEVEL_OPTIONS = (['N5', 'N4', 'N3', 'N2', 'N1'] as const).map((l) => ({ value: l, label: `JLPT ${l}` }))

type PublishFilter = 'published' | 'draft'
const PUBLISH_OPTIONS: { value: PublishFilter; label: string }[] = [
  { value: 'published', label: 'Đã xuất bản' },
  { value: 'draft', label: 'Bản nháp' },
]

// "Chờ duyệt" = giáo viên đã gửi; "Đang soạn" = có thay đổi nhưng chưa ai gửi duyệt
type ChangesFilter = 'all' | 'pending_review' | 'editing'
const CHANGES_OPTIONS: { value: ChangesFilter; label: string }[] = [
  { value: 'all', label: 'Tất cả bản soạn' },
  { value: 'pending_review', label: 'Chờ duyệt' },
  { value: 'editing', label: 'Đang soạn · chưa gửi duyệt' },
]

function statusBadge(course: CourseSummary) {
  if (course.archivedAt) return <Tag className="rounded-md" color="red">Đã lưu trữ</Tag>
  return <CourseReviewBadge course={course} />
}

/** Ảnh bìa khóa học; chưa có ảnh hoặc ảnh lỗi → ô chữ cái đầu của tiêu đề. */
const CourseThumb: React.FC<{ src?: string; title?: string }> = ({ src, title }) => {
  const [failed, setFailed] = useState(false)
  if (src && !failed) {
    return (
      <img
        src={src}
        alt=""
        loading="lazy"
        onError={() => setFailed(true)}
        className="w-10 h-10 rounded object-cover border border-indigo-200 shrink-0"
      />
    )
  }
  return (
    <div className="w-10 h-10 rounded bg-indigo-50 text-indigo-700 font-bold text-sm flex items-center justify-center border border-indigo-200 shrink-0">
      {title?.[0] ?? '日'}
    </div>
  )
}

export const TeacherCoursesPage: React.FC = () => {
  const navigate = useNavigate()
  const qc = useQueryClient()
  // Chỉ ADMIN tạo / sửa / xuất bản / xóa khóa; giáo viên chỉ thấy khóa được giao và vào soạn bài
  const admin = isAdmin(useAuthStore((s) => s.user))
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCourse, setEditingCourse] = useState<CourseSummary | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CourseSummary | null>(null)
  const [publishingId, setPublishingId] = useState<number | null>(null)

  // Bộ lọc của ADMIN theo bản đang soạn: chờ duyệt (đã gửi) / đang soạn (có thay đổi, chưa gửi)
  const [changesFilter, setChangesFilter] = useState<ChangesFilter>('all')
  // Tìm kiếm + lọc + phân trang phía server
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [level, setLevel] = useState<JlptLevel | undefined>(undefined)
  const [publishFilter, setPublishFilter] = useState<PublishFilter | undefined>(undefined)
  const [page, setPage] = useState(1)
  const { data, isLoading, isFetching } = useCourses({
    admin: true,
    page,
    size: PAGE_SIZE,
    pendingChanges: admin && changesFilter === 'editing',
    reviewStatus: !admin
      ? undefined
      : changesFilter === 'pending_review'
        ? 'PENDING_REVIEW'
        : changesFilter === 'editing'
          ? 'EDITING'
          : undefined,
    keyword: keyword || undefined,
    jlptLevel: level,
    published: publishFilter ? publishFilter === 'published' : undefined,
  })
  const filtered = !!keyword || !!level || !!publishFilter
  const deleteCourse = useDeleteCourse()

  const handlePublish = async (course: CourseSummary, publish: boolean) => {
    setPublishingId(course.id)
    try {
      await courseService.publishCourse(course.id, publish)
      toast.success(publish ? 'Đã mở lại khóa học với bản đã duyệt' : 'Đã chuyển về bản nháp')
      qc.invalidateQueries({ queryKey: ['courses'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.all })
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
    // Xóa khóa cuối cùng của trang (không phải trang 1) → lùi về trang trước còn dữ liệu
    if (courses.length === 1 && page > 1) setPage(page - 1)
    setDeleteTarget(null)
  }

  const courses = data?.items ?? data?.content ?? []
  const total = data?.totalElements ?? 0

  const allColumns: ColumnsType<CourseSummary> = [
    {
      title: 'Tiêu đề',
      key: 'title',
      render: (_, record) => (
        <div className="flex items-center gap-3">
          <CourseThumb src={record.thumbnailUrl} title={record.title} />
          <div>
            <p className="font-semibold text-sm text-gray-900">{record.title}</p>
            {/* <p className="text-xs text-gray-400">/{record.slug}</p> */}
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
      width: 200,
      render: (_, record) => statusBadge(record),
    },
    {
      title: 'Hành động',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        // stopPropagation: bấm nút hành động không được kích hoạt click của row
        // (row click → mở trang quản lý khóa học).
        <div className="flex items-center gap-1.5 flex-wrap" onClick={(e) => e.stopPropagation()}>
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
          <Tooltip
            title={
              record.published
                ? 'Chuyển nháp'
                : record.publishedVersionId
                  ? 'Xuất bản (phiên bản đã chốt)'
                  : 'Chưa có phiên bản nào — cần duyệt hoặc lưu ở trang quản lý khóa'
            }
          >
            <AntdButton
              size="small"
              loading={publishingId === record.id}
              disabled={!record.published && !record.publishedVersionId}
              icon={record.published ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              onClick={() => handlePublish(record, !record.published)}
            />
          </Tooltip>
          {/* <Tooltip title="Quản lý bài học">
            <AntdButton
              size="small"
              type="primary"
              onClick={() => navigate(`/teacher/courses/${record.id}`)}
            >
              Quản lý
            </AntdButton>
          </Tooltip> */}
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
  const columns = admin ? allColumns : allColumns.filter((c) => c.key !== 'actions')

  return (
    <div className="space-y-6 pb-12">
      <PageHeader
        // Cùng khung với nội dung bên dưới để nút thao tác thẳng mép phải bảng
        className="max-w-7xl mx-auto px-3 sm:px-4"
        actions={
          admin ? (
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
          ) : undefined
        }
      />

      <div className="max-w-7xl mx-auto px-3 sm:px-4 space-y-4">
        <FilterBar>
          <SearchInput
            placeholder="Tìm theo tên khóa học"
            value={keywordInput}
            onChange={setKeywordInput}
            onSearch={(v) => {
              setKeyword(v)
              setPage(1)
            }}
            className="md:flex-1 md:min-w-60"
          />
          <Select<JlptLevel>
            allowClear
            value={level}
            onChange={(v) => {
              setLevel(v)
              setPage(1)
            }}
            placeholder="Tất cả cấp độ"
            options={LEVEL_OPTIONS}
            className="md:w-44"
            aria-label="Lọc theo cấp độ"
          />
          <Select<PublishFilter>
            allowClear
            value={publishFilter}
            onChange={(v) => {
              setPublishFilter(v)
              setPage(1)
            }}
            placeholder="Mọi trạng thái"
            options={PUBLISH_OPTIONS}
            className="md:w-44"
            aria-label="Lọc theo trạng thái xuất bản"
          />
          {/* Bộ lọc của ADMIN theo bản đang soạn (trước đây là thẻ tab) */}
          {admin && (
            <Select<ChangesFilter>
              value={changesFilter}
              onChange={(v) => {
                setChangesFilter(v)
                setPage(1)
              }}
              options={CHANGES_OPTIONS}
              className="md:w-56"
              aria-label="Lọc theo trạng thái bản đang soạn"
            />
          )}
        </FilterBar>

        {isLoading && <LoadingState message="Đang tải danh sách khóa học..." />}

        {!isLoading && courses.length === 0 && filtered && (
          <EmptyState
            title="Không có khóa học phù hợp"
            description="Thử đổi từ khóa hoặc bộ lọc."
          />
        )}

        {!isLoading && courses.length === 0 && !filtered && changesFilter !== 'all' && admin && (
          <EmptyState
            title={
              changesFilter === 'pending_review' ? 'Không có khóa nào chờ duyệt' : 'Không có khóa nào đang soạn dở'
            }
            description={
              changesFilter === 'pending_review'
                ? 'Chưa có giáo viên nào gửi khóa học lên duyệt.'
                : 'Mọi khóa đều không có thay đổi chưa gửi duyệt / chưa lưu.'
            }
          />
        )}

        {!isLoading && courses.length === 0 && !filtered && !(changesFilter !== 'all' && admin) && (
          <EmptyState
            title={admin ? 'Chưa có khóa học nào' : 'Bạn chưa được giao khóa học nào'}
            description={
              admin
                ? 'Tạo khóa học đầu tiên rồi giao cho giáo viên soạn bài.'
                : 'Quản trị viên sẽ giao khóa học cho bạn. Bạn sẽ nhận được thông báo khi được giao.'
            }
            action={
              admin && (
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
              )
            }
          />
        )}

        {!isLoading && courses.length > 0 && (
          <Table<CourseSummary>
            dataSource={courses}
            rowKey="id"
            loading={isFetching}
            pagination={{
              current: page,
              pageSize: PAGE_SIZE,
              total,
              showSizeChanger: false,
              showTotal: (t) => `${t} khóa học`,
              onChange: setPage,
            }}
            columns={columns}
            // Bấm vào bất kỳ đâu trên row (trừ cột "Hành động") → mở trang quản lý khóa học.
            // Cột hành động đã stopPropagation nên các nút bấm được vẫn hoạt động bình thường.
            onRow={(record) => ({
              onClick: () => navigate(`/teacher/courses/${record.id}`),
              style: { cursor: 'pointer' },
            })}
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
