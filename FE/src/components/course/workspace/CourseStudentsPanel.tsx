import React, { useState } from 'react'
import { GraduationCap } from 'lucide-react'
import { Avatar, Button as AntdButton, Drawer, Progress, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { EmptyState, LoadingState, SearchInput } from '@/components/common'
import { StatTile } from '@/components/admin'
import { useCourseStudents, useStudentCourseDetail } from '@/hooks'
import { formatDate, formatRelativeTime } from '@/utils/format'
import type { LessonProgressStatus, StudentProgress, StudentSort } from '@/types'

const PAGE_SIZE = 20

const SORT_OPTIONS: { value: StudentSort; label: string }[] = [
  { value: 'LAST_ACTIVE', label: 'Học gần đây nhất' },
  { value: 'PROGRESS', label: 'Tiến độ cao nhất' },
  { value: 'NAME', label: 'Tên A → Z' },
  { value: 'ENROLLED', label: 'Đăng ký mới nhất' },
]

const LESSON_STATUS: Record<LessonProgressStatus, { label: string; color: string }> = {
  COMPLETED: { label: 'Hoàn thành', color: 'green' },
  IN_PROGRESS: { label: 'Đang học', color: 'blue' },
  NOT_STARTED: { label: 'Chưa học', color: 'default' },
}

const fmtScore = (v?: number | null) => (v == null ? '—' : v.toLocaleString('vi-VN'))

interface CourseStudentsPanelProps {
  courseId: number
}

/**
 * Học viên của MỘT khóa học: 4 thẻ tổng quan, tìm kiếm + sắp xếp, bảng tiến độ và
 * ngăn chi tiết từng bài. Dùng ở trang "Học viên" và trong không gian soạn khóa học.
 * Đổi khóa học thì truyền `key={courseId}` để làm mới trạng thái (trang, tìm kiếm, ngăn chi tiết).
 */
export const CourseStudentsPanel: React.FC<CourseStudentsPanelProps> = ({ courseId }) => {
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [sort, setSort] = useState<StudentSort>('LAST_ACTIVE')
  const [page, setPage] = useState(1)
  const [detailUserId, setDetailUserId] = useState<number | null>(null)

  const { data, isLoading, isFetching, isError, refetch } = useCourseStudents(courseId, {
    keyword: keyword || undefined,
    sort,
    page,
    size: PAGE_SIZE,
  })
  const detail = useStudentCourseDetail(courseId, detailUserId)

  const columns: ColumnsType<StudentProgress> = [
    {
      title: 'Học viên',
      key: 'student',
      render: (_, s) => (
        <div className="flex items-center gap-3 min-w-0">
          <Avatar src={s.avatarUrl || undefined} className="shrink-0 bg-indigo-100 text-indigo-700">
            {s.fullName?.charAt(0)?.toUpperCase() ?? '?'}
          </Avatar>
          <div className="min-w-0">
            <div className="font-medium text-slate-800 truncate">{s.fullName}</div>
            <div className="text-xs text-slate-500 truncate">{s.email}</div>
          </div>
        </div>
      ),
    },
    {
      title: 'Tiến độ',
      key: 'progress',
      width: 220,
      render: (_, s) => (
        <div className="min-w-[160px]">
          <Progress percent={s.progressPercent} size="small" strokeColor="#2a78d6" />
          <div className="text-xs text-slate-500">
            {s.completedLessons}/{s.totalLessons} bài
            {s.inProgressLessons > 0 && ` · ${s.inProgressLessons} đang học`}
          </div>
        </div>
      ),
    },
    {
      title: 'Điểm kiểm tra',
      dataIndex: 'avgExamScore',
      key: 'avgExamScore',
      align: 'right',
      responsive: ['md'],
      render: (v?: number | null) => <span className="tabular-nums">{fmtScore(v)}</span>,
    },
    {
      title: 'Học gần nhất',
      dataIndex: 'lastActiveAt',
      key: 'lastActiveAt',
      render: (d?: string | null) => (
        <span className="text-sm text-slate-600">{d ? formatRelativeTime(d) : 'Chưa học'}</span>
      ),
    },
    {
      title: 'Ngày đăng ký',
      dataIndex: 'enrolledAt',
      key: 'enrolledAt',
      responsive: ['lg'],
      render: (d?: string | null) => <span className="text-sm text-slate-600">{d ? formatDate(d) : '—'}</span>,
    },
  ]

  const summary = data?.summary
  const students = data?.students.items ?? data?.students.content ?? []

  return (
    <div className="space-y-5">
      {summary && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatTile label="Học viên" value={summary.totalStudents.toLocaleString('vi-VN')} hint={`${data?.totalLessons ?? 0} bài học`} />
          <StatTile label="Tiến độ trung bình" value={`${summary.avgProgressPercent.toLocaleString('vi-VN')}%`} />
          <StatTile
            label={`Học trong ${summary.recentDays} ngày qua`}
            value={summary.activeRecently.toLocaleString('vi-VN')}
          />
          <StatTile
            label="Đã học xong"
            value={summary.finishedStudents.toLocaleString('vi-VN')}
            hint={`${summary.notStartedStudents.toLocaleString('vi-VN')} người chưa bắt đầu`}
          />
        </div>
      )}

      {/* Tìm kiếm + sắp xếp: nằm ngay trên bảng học viên */}
      <div className="flex flex-col md:flex-row gap-3">
        <SearchInput
          placeholder="Tìm theo tên hoặc email"
          value={keywordInput}
          onChange={setKeywordInput}
          onSearch={(v) => {
            setKeyword(v)
            setPage(1)
          }}
          className="md:max-w-xs"
        />
        <Select
          value={sort}
          onChange={(v: StudentSort) => {
            setSort(v)
            setPage(1)
          }}
          options={SORT_OPTIONS}
          className="md:w-48"
          aria-label="Sắp xếp"
        />
      </div>

      {isError ? (
        <EmptyState
          icon={<GraduationCap className="w-10 h-10 text-rose-400" />}
          title="Không tải được danh sách học viên"
          description="Kiểm tra kết nối hoặc quyền truy cập rồi thử lại."
          action={<AntdButton onClick={() => void refetch()}>Thử lại</AntdButton>}
        />
      ) : !isLoading && !isFetching && students.length === 0 ? (
        // Hiện ngoài Table: ô trống của antd chỉ trải qua các cột không có `responsive`
        // nên nội dung bị lệch sang trái.
        <EmptyState
          card
          icon={<GraduationCap className="w-10 h-10 text-slate-300" />}
          title={keyword ? 'Không có học viên phù hợp' : 'Khóa học chưa có học viên'}
          description={keyword ? 'Thử đổi từ khóa tìm kiếm.' : 'Học viên đăng ký khóa sẽ xuất hiện ở đây.'}
        />
      ) : (
        <div className="bg-surface rounded-xl border border-slate-200 overflow-x-auto">
          <Table<StudentProgress>
            rowKey="userId"
            columns={columns}
            dataSource={students}
            loading={isLoading || isFetching}
            onChange={(p) => setPage(p.current ?? 1)}
            onRow={(s) => ({
              onClick: () => setDetailUserId(s.userId),
              className: 'cursor-pointer',
            })}
            pagination={{
              current: data?.students.page ?? page,
              pageSize: PAGE_SIZE,
              total: data?.students.totalElements ?? 0,
              showSizeChanger: false,
              showTotal: (total) => `${total} học viên`,
            }}
          />
        </div>
      )}

      {/* ======================== Chi tiết học viên ======================== */}
      <Drawer
        open={!!detailUserId}
        onClose={() => setDetailUserId(null)}
        title={detail.data?.student.fullName ?? 'Tiến độ học viên'}
        width={520}
        destroyOnHidden
      >
        {detail.isLoading ? (
          <LoadingState message="Đang tải tiến độ..." />
        ) : detail.isError || !detail.data ? (
          <EmptyState title="Không tải được tiến độ" description="Học viên có thể đã hủy đăng ký khóa học." />
        ) : (
          <div className="space-y-4">
            <div className="text-sm text-slate-600 space-y-1">
              <div>{detail.data.student.email}</div>
              <Progress percent={detail.data.student.progressPercent} strokeColor="#2a78d6" />
              <div className="text-xs text-slate-500">
                {detail.data.student.completedLessons}/{detail.data.student.totalLessons} bài hoàn thành · Điểm quiz TB{' '}
                {fmtScore(detail.data.student.avgQuizScore)} · Đăng ký{' '}
                {detail.data.student.enrolledAt ? formatDate(detail.data.student.enrolledAt) : '—'}
              </div>
            </div>
            <ul className="divide-y divide-slate-100 border border-slate-200 rounded-lg">
              {detail.data.lessons.map((l) => (
                <li key={l.lessonId} className="flex items-start justify-between gap-3 px-3 py-2.5">
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-slate-800 truncate">
                      {l.orderIndex}. {l.title}
                    </div>
                    <div className="text-xs text-slate-500">
                      {l.completedAt
                        ? `Hoàn thành ${formatRelativeTime(l.completedAt)}`
                        : l.lastAccessedAt
                          ? `Học gần nhất ${formatRelativeTime(l.lastAccessedAt)}`
                          : 'Chưa mở bài'}
                      {l.bestScore != null && ` · Điểm quiz ${fmtScore(l.bestScore)}`}
                    </div>
                  </div>
                  <Tag color={LESSON_STATUS[l.status].color} className="rounded-md shrink-0 !m-0">
                    {LESSON_STATUS[l.status].label}
                  </Tag>
                </li>
              ))}
            </ul>
          </div>
        )}
      </Drawer>
    </div>
  )
}
