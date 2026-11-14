import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Edit3, Trash2, Eye, EyeOff, Crown } from 'lucide-react'
import { Table, Tag, Button as AntdButton, Tooltip, Select } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PageHeader, LoadingState, EmptyState, SearchInput, FilterBar } from '@/components/common'
import { CourseFormModal, CourseReviewBadge } from '@/components/course'
import { ConfirmModal } from '@/components/common'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { PlanChangeConfirmModal } from '@/components/subscription'
import { useCourses, useDeleteCourse } from '@/hooks'
import { courseService } from '@/services'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { CourseSummary, JlptLevel } from '@/types'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'
import { QUERY_KEYS } from '@/lib/queryKeys'

const PAGE_SIZE = 10
/** Tạm ẩn nút xóa khóa học — đổi thành true để hiện lại. */
const COURSE_DELETE_ENABLED = false

const LEVEL_OPTIONS = (['N5', 'N4', 'N3', 'N2', 'N1'] as const).map((l) => ({ value: l, label: `JLPT ${l}` }))

type PublishFilter = 'published' | 'draft'
const PUBLISH_OPTIONS: { value: PublishFilter; label: string }[] = [
  { value: 'published', label: 'Đã xuất bản' },
  { value: 'draft', label: 'Bản nháp' },
]

type PremiumFilter = 'all' | 'premium' | 'free'
const PREMIUM_OPTIONS: { value: PremiumFilter; label: string }[] = [
  { value: 'all', label: 'Tất cả gói' },
  { value: 'premium', label: 'Chỉ gói PRO' },
  { value: 'free', label: 'Chỉ gói Miễn phí' },
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
  const [premiumTarget, setPremiumTarget] = useState<{ course: CourseSummary; targetPremium: boolean } | null>(null)
  const [isUpdatingPremium, setIsUpdatingPremium] = useState(false)

  // Bộ lọc của ADMIN theo bản đang soạn: chờ duyệt (đã gửi) / đang soạn (có thay đổi, chưa gửi)
  const [changesFilter, setChangesFilter] = useState<ChangesFilter>('all')
  // Tìm kiếm + lọc + phân trang phía server
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [level, setLevel] = useState<JlptLevel | undefined>(undefined)
  const [publishFilter, setPublishFilter] = useState<PublishFilter | undefined>(undefined)
  const [premiumFilter, setPremiumFilter] = useState<PremiumFilter>('all')
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
  const filtered = !!keyword || !!level || !!publishFilter || premiumFilter !== 'all'
  const deleteCourse = useDeleteCourse()

  const handlePublish = async (course: CourseSummary, publish: boolean) => {
    const ok = await confirmDialog(
      publish
        ? {
            title: `Xuất bản khóa học "${course.title}"?`,
            content:
              'Học viên sẽ thấy khóa học với phiên bản đã chốt gần nhất (không kèm phần đang soạn). '
              + 'Học viên đã đăng ký sẽ nhận thông báo.',
            okText: 'Xuất bản',
            cancelText: 'Hủy',
          }
        : {
            title: `Chuyển khóa học "${course.title}" về nháp?`,
            content: 'Khóa học sẽ bị ẩn với học viên cho tới khi xuất bản lại. Học viên đã đăng ký sẽ nhận thông báo.',
            okText: 'Chuyển nháp',
            cancelText: 'Hủy',
            danger: true,
          },
    )
    if (!ok) return
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

  const handleConfirmTogglePremium = async () => {
    if (!premiumTarget) return
    setIsUpdatingPremium(true)
    try {
      await courseService.setCoursePremium(premiumTarget.course.id, premiumTarget.targetPremium)
      toast.success(
        premiumTarget.targetPremium
          ? `Đã chuyển khóa học "${premiumTarget.course.title}" sang gói Premium (PRO)`
          : `Đã chuyển khóa học "${premiumTarget.course.title}" sang Miễn phí (Free)`
      )
      qc.invalidateQueries({ queryKey: ['courses'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.all })
      setPremiumTarget(null)
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'Không thể thay đổi trạng thái gói.'
      toast.error('Cập nhật gói thất bại', { description: msg })
    } finally {
      setIsUpdatingPremium(false)
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
  const displayedCourses = courses.filter((c) => {
    if (premiumFilter === 'premium') return Boolean(c.isPremium)
    if (premiumFilter === 'free') return !c.isPremium
    return true
  })
  const total = data?.totalElements ?? 0

  const allColumns: ColumnsType<CourseSummary> = [
    {
      title: 'Tiêu đề',
      key: 'title',
      render: (_, record) => (
        <div className="flex items-center gap-3">
          <CourseThumb src={record.thumbnailUrl} title={record.title} />
          <div>
            <div className="flex items-center gap-1.5 flex-wrap">
              <p className="font-semibold text-sm text-gray-900">{record.title}</p>
              {record.isPremium && (
                <Tag color="gold" className="font-bold text-[10px] rounded-full inline-flex items-center gap-0.5 border-amber-300 px-1.5 py-0 leading-tight">
                  <Crown className="w-2.5 h-2.5 text-amber-600 fill-amber-600" />
                  <span>PRO</span>
                </Tag>
              )}
            </div>
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
      title: 'Gói',
      key: 'isPremium',
      width: 105,
      render: (_, record) => (
        record.isPremium ? (
          <Tag color="gold" className="font-bold text-xs rounded-full inline-flex items-center gap-1 border-amber-300 px-2 py-0.5">
            <Crown className="w-3 h-3 text-amber-600 fill-amber-600" />
            <span>PRO</span>
          </Tag>
        ) : (
          <Tag color="default" className="text-xs rounded-full font-medium text-gray-600 px-2 py-0.5">
            Miễn phí
          </Tag>
        )
      ),
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
          {admin && (
            <Tooltip title={record.isPremium ? 'Đổi sang Miễn phí (Yêu cầu xác nhận)' : 'Nâng lên PRO (Yêu cầu xác nhận)'}>
              <AntdButton
                size="small"
                className={record.isPremium ? '!text-amber-600 !border-amber-300 hover:!text-amber-700' : '!text-gray-500'}
                icon={<Crown className={`w-3.5 h-3.5 ${record.isPremium ? 'text-amber-500 fill-amber-500' : ''}`} />}
                onClick={() => setPremiumTarget({ course: record, targetPremium: !record.isPremium })}
              />
            </Tooltip>
          )}
          {/* <Tooltip title="Quản lý bài học">
            <AntdButton
              size="small"
              type="primary"
              onClick={() => navigate(`/teacher/courses/${record.id}`)}
            >
              Quản lý
            </AntdButton>
          </Tooltip> */}
          {COURSE_DELETE_ENABLED && (
            <Tooltip title="Xóa">
              <AntdButton
                size="small"
                danger
                icon={<Trash2 className="w-4 h-4" />}
                onClick={() => setDeleteTarget(record)}
              />
            </Tooltip>
          )}
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
        title={
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
              Quản lý Khóa học JLPT
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
              Biên tập nội dung, thiết kế lộ trình bài học và phát hành các khóa học N1 ~ N5 chuẩn JLPT.
            </p>
          </div>
        }
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
          <Select<PremiumFilter>
            value={premiumFilter}
            onChange={(v) => {
              setPremiumFilter(v)
              setPage(1)
            }}
            options={PREMIUM_OPTIONS}
            className="md:w-40"
            aria-label="Lọc theo gói truy cập"
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

        {!isLoading && displayedCourses.length > 0 && (
          <Table<CourseSummary>
            dataSource={displayedCourses}
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

      {/* MODAL XÁC NHẬN CHUYỂN GÓI PRO / FREE KHÓA HỌC HIỆN ĐẠI (ZEN / FINTECH) */}
      <PlanChangeConfirmModal
        open={!!premiumTarget}
        itemType="course"
        title={premiumTarget?.course.title ?? ''}
        level={premiumTarget?.course.jlptLevel}
        targetPremium={premiumTarget?.targetPremium ?? false}
        isLoading={isUpdatingPremium}
        onConfirm={handleConfirmTogglePremium}
        onCancel={() => setPremiumTarget(null)}
      />
    </div>
  )
}
