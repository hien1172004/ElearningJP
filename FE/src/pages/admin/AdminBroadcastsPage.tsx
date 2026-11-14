import React, { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Megaphone, Send, Users } from 'lucide-react'
import { Button as AntdButton, Input, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { z } from 'zod'
import { toast } from 'sonner'
import { BackButton, EmptyState, SearchInput, confirmDialog, FilterBar, RequiredMark } from '@/components/common'
import { useBroadcastHistory, useCourses, useRecipientCount, useSendBroadcast } from '@/hooks'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'
import { formatRelativeTime } from '@/utils/format'
import type { Broadcast, BroadcastAudience, RecipientCountParams } from '@/types'

const HISTORY_PAGE_SIZE = 10
const TITLE_MAX = 120
const BODY_MAX = 1000

const AUDIENCE_LABELS: Record<BroadcastAudience, string> = {
  ALL_USERS: 'Tất cả người dùng',
  ROLE: 'Theo vai trò',
  COURSE_STUDENTS: 'Học viên một khóa học',
}

const ROLE_OPTIONS = [
  { value: 'USER', label: 'Học viên' },
  { value: 'TEACHER', label: 'Giáo viên' },
  { value: 'ADMIN', label: 'Quản trị viên' },
]

const roleLabel = (role?: string | null) => ROLE_OPTIONS.find((r) => r.value === role)?.label ?? role ?? ''

// Khớp validation BE (BroadcastRequest)
const schema = z.object({
  title: z.string().trim().min(1, 'Vui lòng nhập tiêu đề').max(TITLE_MAX, `Tiêu đề tối đa ${TITLE_MAX} ký tự`),
  body: z.string().trim().min(1, 'Vui lòng nhập nội dung').max(BODY_MAX, `Nội dung tối đa ${BODY_MAX} ký tự`),
  link: z
    .string()
    .trim()
    .max(300, 'Đường dẫn tối đa 300 ký tự')
    .regex(/^$|^\/(?!\/)\S*$/, 'Đường dẫn phải bắt đầu bằng / (trang trong ứng dụng)'),
})

export const AdminBroadcastsPage: React.FC = () => {
  const admin = isAdmin(useAuthStore((s) => s.user))
  const [searchParams] = useSearchParams()
  const presetCourseId = Number(searchParams.get('courseId')) || undefined

  // TEACHER chỉ gửi được cho học viên khóa của mình
  const [audience, setAudience] = useState<BroadcastAudience>(
    admin && !presetCourseId ? 'ALL_USERS' : 'COURSE_STUDENTS',
  )
  const [role, setRole] = useState<string>('USER')
  const [courseId, setCourseId] = useState<number | undefined>(presetCourseId)
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [link, setLink] = useState('')
  const [historyPage, setHistoryPage] = useState(1)
  // Bộ lọc lịch sử gửi
  const [historyKeywordInput, setHistoryKeywordInput] = useState('')
  const [historyKeyword, setHistoryKeyword] = useState('')
  const [historyAudience, setHistoryAudience] = useState<BroadcastAudience | undefined>(undefined)

  // ADMIN: mọi khóa; TEACHER: khóa do mình tạo (BE đã lọc)
  const { data: coursePage, isLoading: coursesLoading } = useCourses({ admin: true, size: 100 })
  const courses = coursePage?.items ?? coursePage?.content ?? []

  const countParams: RecipientCountParams | null = useMemo(() => {
    if (audience === 'ROLE') return { audience, role }
    if (audience === 'COURSE_STUDENTS') return courseId ? { audience, courseId } : null
    return { audience }
  }, [audience, role, courseId])
  const { data: recipientCount, isFetching: counting } = useRecipientCount(countParams)

  const send = useSendBroadcast()
  const history = useBroadcastHistory({
    page: historyPage,
    size: HISTORY_PAGE_SIZE,
    keyword: historyKeyword || undefined,
    audience: historyAudience,
  })
  const historyFiltered = !!historyKeyword || !!historyAudience
  const historyRows = history.data?.items ?? history.data?.content ?? []

  const handleSend = async () => {
    const parsed = schema.safeParse({ title, body, link })
    if (!parsed.success) {
      toast.error('Thông báo chưa hợp lệ', { description: parsed.error.issues[0]?.message })
      return
    }
    if (!countParams) {
      toast.error('Vui lòng chọn khóa học')
      return
    }
    const count = recipientCount ?? 0
    if (count === 0) {
      toast.error('Không có người nhận nào phù hợp')
      return
    }
    const ok = await confirmDialog({
      title: `Gửi thông báo tới ${count.toLocaleString('vi-VN')} người?`,
      content: 'Thông báo sẽ xuất hiện ngay trong mục thông báo của người nhận và không thể thu hồi.',
      okText: 'Gửi ngay',
    })
    if (!ok) return
    send.mutate(
      {
        ...parsed.data,
        link: parsed.data.link || undefined,
        audience,
        role: audience === 'ROLE' ? role : undefined,
        courseId: audience === 'COURSE_STUDENTS' ? courseId : undefined,
      },
      {
        onSuccess: () => {
          setTitle('')
          setBody('')
          setLink('')
        },
      },
    )
  }

  const columns: ColumnsType<Broadcast> = [
    {
      title: 'Thông báo',
      key: 'content',
      render: (_, b) => (
        <div className="min-w-0 max-w-md">
          <div className="font-medium text-slate-800 truncate">{b.title}</div>
          <div className="text-xs text-slate-500 line-clamp-2 whitespace-pre-wrap">{b.body}</div>
        </div>
      ),
    },
    {
      title: 'Gửi tới',
      key: 'audience',
      render: (_, b) => (
        <span className="text-sm text-slate-600">
          {b.audience === 'ROLE'
            ? `Vai trò: ${roleLabel(b.role)}`
            : b.audience === 'COURSE_STUDENTS'
              ? `Học viên: ${b.courseTitle ?? `khóa #${b.courseId}`}`
              : AUDIENCE_LABELS[b.audience]}
        </span>
      ),
    },
    {
      title: 'Người nhận',
      dataIndex: 'recipientCount',
      key: 'recipientCount',
      align: 'right',
      render: (n: number) => <span className="tabular-nums">{n.toLocaleString('vi-VN')}</span>,
    },
    ...(admin
      ? [
        {
          title: 'Người gửi',
          key: 'sender',
          responsive: ['md'],
          render: (_: unknown, b: Broadcast) => <span className="text-sm text-slate-600">{b.senderName ?? '—'}</span>,
        } satisfies ColumnsType<Broadcast>[number],
      ]
      : []),
    {
      title: 'Thời gian',
      dataIndex: 'createdAt',
      key: 'createdAt',
      responsive: ['md'],
      render: (d: string) => <span className="text-sm text-slate-500">{formatRelativeTime(d)}</span>,
    },
  ]

  const audienceOptions = (admin
    ? (['ALL_USERS', 'ROLE', 'COURSE_STUDENTS'] as const)
    : (['COURSE_STUDENTS'] as const)
  ).map((a) => ({ value: a, label: AUDIENCE_LABELS[a] }))

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 space-y-5">
      {/* Mở từ trang học viên (có ?courseId) → cho quay lại trang đó */}
      {presetCourseId && <BackButton to={`/teacher/students?courseId=${presetCourseId}`} />}

      {/* ======================== Soạn thông báo ======================== */}
      <section className="bg-surface rounded-xl border border-slate-200 p-4 space-y-4">
        <div className="grid md:grid-cols-2 gap-3">
          {/* Không có ô phụ (vai trò / khóa học) → chiếm trọn chiều ngang khung */}
          <label className={audience === 'ALL_USERS' ? 'space-y-1 md:col-span-2' : 'space-y-1'}>
            <span className="text-sm font-medium text-slate-700">Gửi tới<RequiredMark /></span>
            <Select
              value={audience}
              onChange={(v: BroadcastAudience) => setAudience(v)}
              options={audienceOptions}
              className="w-full"
              disabled={!admin}
            />
          </label>
          {audience === 'ROLE' && (
            <label className="space-y-1">
              <span className="text-sm font-medium text-slate-700">Vai trò<RequiredMark /></span>
              <Select value={role} onChange={setRole} options={ROLE_OPTIONS} className="w-full" />
            </label>
          )}
          {audience === 'COURSE_STUDENTS' && (
            <label className="space-y-1">
              <span className="text-sm font-medium text-slate-700">Khóa học<RequiredMark /></span>
              <Select
                value={courseId}
                onChange={(v: number) => setCourseId(v)}
                placeholder={courses.length || coursesLoading ? 'Chọn khóa học' : 'Bạn chưa có khóa học nào'}
                loading={coursesLoading}
                showSearch
                optionFilterProp="label"
                options={courses.map((c) => ({ value: c.id, label: `${c.title} (${c.jlptLevel})` }))}
                className="w-full"
              />
            </label>
          )}
        </div>

        <label className="block space-y-1">
          <span className="text-sm font-medium text-slate-700">Tiêu đề<RequiredMark /></span>
          <Input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={TITLE_MAX}
            showCount
            placeholder="VD: Lịch kiểm tra giữa khóa N4"
          />
        </label>
        <label className="block space-y-1">
          <span className="text-sm font-medium text-slate-700">Nội dung<RequiredMark /></span>
          <Input.TextArea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            maxLength={BODY_MAX}
            showCount
            autoSize={{ minRows: 3, maxRows: 8 }}
            placeholder="Nội dung người nhận sẽ thấy trong mục thông báo"
          />
        </label>
        <label className="block space-y-1">
          <span className="text-sm font-medium text-slate-700">
            Đường dẫn khi bấm thông báo
          </span>
          <Input
            value={link}
            onChange={(e) => setLink(e.target.value)}
            maxLength={300}
            placeholder={audience === 'COURSE_STUDENTS' ? 'Để trống = mở trang khóa học' : 'VD: /exams'}
          />
        </label>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
          <span className="inline-flex items-center gap-2 text-sm text-slate-600">
            <Users className="w-4 h-4 text-slate-400" />
            {!countParams
              ? 'Chọn khóa học để xem số người nhận'
              : counting
                ? 'Đang đếm người nhận...'
                : `Sẽ gửi tới ${(recipientCount ?? 0).toLocaleString('vi-VN')} người`}
          </span>
          <AntdButton
            type="primary"
            icon={<Send className="w-4 h-4" />}
            loading={send.isPending}
            disabled={!countParams || counting}
            onClick={handleSend}
          >
            Gửi thông báo
          </AntdButton>
        </div>
      </section>

      {/* ======================== Lịch sử ======================== */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-slate-800">Lịch sử gửi</h2>
        <FilterBar>
          <SearchInput
            placeholder="Tìm theo tiêu đề hoặc nội dung"
            value={historyKeywordInput}
            onChange={setHistoryKeywordInput}
            onSearch={(v) => {
              setHistoryKeyword(v)
              setHistoryPage(1)
            }}
            className="md:flex-1 md:min-w-60"
          />
          {/* Giáo viên chỉ gửi được cho học viên khóa của mình → không cần lọc đối tượng */}
          {admin && (
            <Select<BroadcastAudience>
              allowClear
              value={historyAudience}
              onChange={(v) => {
                setHistoryAudience(v)
                setHistoryPage(1)
              }}
              placeholder="Tất cả đối tượng"
              options={(['ALL_USERS', 'ROLE', 'COURSE_STUDENTS'] as const).map((a) => ({
                value: a,
                label: AUDIENCE_LABELS[a],
              }))}
              className="md:w-56"
              aria-label="Lọc theo đối tượng nhận"
            />
          )}
        </FilterBar>
        {history.isError ? (
          <EmptyState
            icon={<Megaphone className="w-10 h-10 text-rose-400" />}
            title="Không tải được lịch sử gửi"
            description="Kiểm tra kết nối rồi thử lại."
            action={<AntdButton onClick={() => void history.refetch()}>Thử lại</AntdButton>}
          />
        ) : !history.isLoading && historyRows.length === 0 ? (
          // Rỗng → hiện ngoài bảng: ô trống trong bảng có cột mở rộng (expandable) bị lệch, không phủ hết các cột
          <EmptyState
            card
            icon={<Megaphone className="w-10 h-10 text-slate-300" />}
            title={historyFiltered ? 'Không có thông báo phù hợp' : 'Chưa gửi thông báo nào'}
            description={historyFiltered ? 'Thử đổi từ khóa hoặc bộ lọc.' : 'Các thông báo đã gửi sẽ hiện ở đây.'}
          />
        ) : (
          <div className="bg-surface rounded-xl border border-slate-200 overflow-x-auto">
            <Table<Broadcast>
              rowKey="id"
              columns={columns}
              dataSource={historyRows}
              loading={history.isLoading || history.isFetching}
              onChange={(p) => setHistoryPage(p.current ?? 1)}
              pagination={{
                current: history.data?.page ?? historyPage,
                pageSize: HISTORY_PAGE_SIZE,
                total: history.data?.totalElements ?? 0,
                showSizeChanger: false,
                showTotal: (total) => `${total} lần gửi`,
              }}
              expandable={{
                expandedRowRender: (b) => (
                  <div className="text-sm text-slate-700 whitespace-pre-wrap space-y-1">
                    <p>{b.body}</p>
                    {b.link && (
                      <p className="text-xs text-slate-500">
                        Đường dẫn: <Tag className="rounded-md">{b.link}</Tag>
                      </p>
                    )}
                  </div>
                ),
              }}
            />
          </div>
        )}
      </section>
    </div>
  )
}
