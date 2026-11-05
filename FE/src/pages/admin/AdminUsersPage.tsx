import React, { useState } from 'react'
import { Lock, Unlock, UserPlus, Users } from 'lucide-react'
import { Avatar, Button as AntdButton, Select, Table, Tag, Tooltip } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { EmptyState, SearchInput, confirmDialog } from '@/components/common'
import { CreateAccountModal } from '@/components/admin'
import { useAdminUsers, useUpdateUserStatus } from '@/hooks'
import { useAuthStore } from '@/stores'
import { formatDate } from '@/utils'
import { normalizeRole } from '@/utils/roles'
import type { AdminUser, AssignableRole, UserStatus } from '@/types'

const PAGE_SIZE = 20

const ROLE_META: Record<AssignableRole, { label: string; color: string }> = {
  ADMIN: { label: 'Quản trị viên', color: 'red' },
  TEACHER: { label: 'Giáo viên', color: 'blue' },
  USER: { label: 'Học viên', color: 'default' },
}

const STATUS_META: Record<UserStatus, { label: string; color: string }> = {
  ACTIVE: { label: 'Hoạt động', color: 'green' },
  INACTIVE: { label: 'Vô hiệu hóa', color: 'orange' },
  BANNED: { label: 'Đã khóa', color: 'red' },
  PENDING: { label: 'Chờ xác minh', color: 'default' },
}

const isAssignableRole = (role: string): role is AssignableRole => role in ROLE_META

export const AdminUsersPage: React.FC = () => {
  const currentUserId = useAuthStore((s) => s.user?.id)

  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [role, setRole] = useState<AssignableRole | undefined>()
  const [status, setStatus] = useState<UserStatus | undefined>()
  const [page, setPage] = useState(1)

  const [createOpen, setCreateOpen] = useState(false)

  const { data, isLoading, isFetching, isError, refetch } = useAdminUsers({
    keyword: keyword || undefined,
    role,
    status,
    page,
    size: PAGE_SIZE,
  })
  const updateStatus = useUpdateUserStatus()

  const users = data?.items ?? data?.content ?? []

  const handleToggleLock = async (user: AdminUser) => {
    const locking = user.status === 'ACTIVE'
    const ok = await confirmDialog({
      title: locking ? 'Khóa tài khoản này?' : 'Mở khóa tài khoản này?',
      content: locking
        ? `${user.email} sẽ bị đăng xuất khỏi mọi thiết bị và không thể đăng nhập cho tới khi được mở khóa.`
        : `${user.email} sẽ có thể đăng nhập lại bình thường.`,
      okText: locking ? 'Khóa tài khoản' : 'Mở khóa',
      danger: locking,
    })
    if (!ok) return
    updateStatus.mutate({ id: user.id, status: locking ? 'BANNED' : 'ACTIVE' })
  }

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage(pagination.current ?? 1)
  }

  const columns: ColumnsType<AdminUser> = [
    {
      title: 'Người dùng',
      key: 'user',
      render: (_, u) => (
        <div className="flex items-center gap-3 min-w-0">
          <Avatar src={u.avatarUrl || undefined} className="shrink-0 bg-indigo-100 text-indigo-700">
            {u.fullName?.charAt(0)?.toUpperCase()}
          </Avatar>
          <div className="min-w-0">
            <div className="font-medium text-slate-800 truncate">
              {u.fullName}
              {u.id === currentUserId && (
                <span className="ml-2 text-xs text-slate-400">(bạn)</span>
              )}
            </div>
            <div className="text-xs text-slate-500 truncate">{u.email}</div>
          </div>
        </div>
      ),
    },
    {
      title: 'Vai trò',
      key: 'roles',
      render: (_, u) => {
        const roles = u.roles.map(normalizeRole).filter(isAssignableRole)
        // Hiển thị role cao nhất trước
        const ordered = (['ADMIN', 'TEACHER', 'USER'] as const).filter((r) => roles.includes(r))
        return (
          <div className="flex flex-wrap gap-1">
            {ordered.map((r) => (
              <Tag key={r} color={ROLE_META[r].color} className="rounded-md">
                {ROLE_META[r].label}
              </Tag>
            ))}
          </div>
        )
      },
    },
    {
      title: 'Đăng nhập',
      dataIndex: 'authProvider',
      key: 'authProvider',
      responsive: ['md'],
      render: (p: AdminUser['authProvider']) => (
        <span className="text-sm text-slate-600">{p === 'GOOGLE' ? 'Google' : 'Email'}</span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (s: UserStatus) => (
        <Tag color={STATUS_META[s]?.color ?? 'default'} className="rounded-md">
          {STATUS_META[s]?.label ?? s}
        </Tag>
      ),
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      key: 'createdAt',
      responsive: ['lg'],
      render: (d?: string) => (
        <span className="text-sm text-slate-600">{d ? formatDate(d) : '—'}</span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      align: 'right',
      render: (_, u) => {
        const isSelf = u.id === currentUserId
        const selfHint = 'Không thể thay đổi tài khoản của chính bạn'
        const locking = u.status === 'ACTIVE'
        return (
          <div className="flex justify-end gap-2">
            <Tooltip title={isSelf ? selfHint : locking ? 'Khóa tài khoản' : 'Mở khóa'}>
              <AntdButton
                size="small"
                danger={locking}
                icon={locking ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />}
                disabled={isSelf}
                loading={updateStatus.isPending && updateStatus.variables?.id === u.id}
                onClick={() => handleToggleLock(u)}
                aria-label={locking ? 'Khóa tài khoản' : 'Mở khóa'}
              />
            </Tooltip>
          </div>
        )
      },
    },
  ]

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 space-y-5">

      <div className="flex flex-col md:flex-row gap-3">
        <SearchInput
          placeholder="Tìm theo email hoặc họ tên"
          value={keywordInput}
          onChange={setKeywordInput}
          onSearch={(v) => {
            setKeyword(v)
            setPage(1)
          }}
          className="md:max-w-sm"
        />
        <Select
          allowClear
          placeholder="Tất cả vai trò"
          value={role}
          onChange={(v?: AssignableRole) => {
            setRole(v)
            setPage(1)
          }}
          options={(['ADMIN', 'TEACHER', 'USER'] as const).map((r) => ({
            value: r,
            label: ROLE_META[r].label,
          }))}
          className="md:w-48"
        />
        <Select
          allowClear
          placeholder="Tất cả trạng thái"
          value={status}
          onChange={(v?: UserStatus) => {
            setStatus(v)
            setPage(1)
          }}
          options={(['ACTIVE', 'INACTIVE', 'BANNED'] as const).map((s) => ({
            value: s,
            label: STATUS_META[s].label,
          }))}
          className="md:w-48"
        />
        <AntdButton
          type="primary"
          icon={<UserPlus className="w-4 h-4" />}
          onClick={() => setCreateOpen(true)}
          className="md:ml-auto"
        >
          Tạo tài khoản
        </AntdButton>
      </div>

      {isError ? (
        <EmptyState
          icon={<Users className="w-10 h-10 text-rose-400" />}
          title="Không tải được danh sách người dùng"
          description="Kiểm tra kết nối hoặc quyền truy cập rồi thử lại."
          action={<AntdButton onClick={() => refetch()}>Thử lại</AntdButton>}
        />
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 overflow-x-auto">
          <Table<AdminUser>
            rowKey="id"
            columns={columns}
            dataSource={users}
            loading={isLoading || isFetching}
            onChange={handleTableChange}
            pagination={{
              current: data?.page ?? page,
              pageSize: PAGE_SIZE,
              total: data?.totalElements ?? 0,
              showSizeChanger: false,
              showTotal: (total) => `${total} người dùng`,
            }}
            locale={{
              emptyText: (
                <EmptyState
                  icon={<Users className="w-10 h-10 text-slate-300" />}
                  title="Không có người dùng phù hợp"
                  description="Thử đổi từ khóa hoặc bộ lọc."
                />
              ),
            }}
          />
        </div>
      )}

      <CreateAccountModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </div>
  )
}
