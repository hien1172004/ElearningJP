import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MessageSquareText, Pencil, Plus, Trash2 } from 'lucide-react'
import { Button as AntdButton, Select, Table, Tag, Tooltip } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { EmptyState, SearchInput, confirmDialog, FilterBar } from '@/components/common'
import { SCENARIO_TOPICS, scenarioTopicName } from '@/components/admin'
import { useAdminScenarios, useDeleteScenario } from '@/hooks'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'
import { formatDate } from '@/utils'
import type { AdminScenario, JlptLevel } from '@/types'

const PAGE_SIZE = 20
const LEVELS: JlptLevel[] = ['N5', 'N4', 'N3', 'N2', 'N1']

export const AdminScenariosPage: React.FC = () => {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const admin = isAdmin(user)

  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [topicId, setTopicId] = useState<string | undefined>()
  const [jlptLevel, setJlptLevel] = useState<JlptLevel | undefined>()
  const [page, setPage] = useState(1)

  const { data, isLoading, isFetching, isError, refetch } = useAdminScenarios({
    keyword: keyword || undefined,
    topicId,
    jlptLevel,
    page,
    size: PAGE_SIZE,
  })
  const deleteScenario = useDeleteScenario()

  const scenarios = data?.items ?? data?.content ?? []

  const handleDelete = async (s: AdminScenario) => {
    const ok = await confirmDialog({
      title: 'Xóa kịch bản này?',
      content: `"${s.title}" sẽ bị xóa vĩnh viễn và không còn hiển thị ở sảnh luyện nói. Lịch sử các buổi luyện nói đã dùng kịch bản này vẫn được giữ.`,
      okText: 'Xóa kịch bản',
      danger: true,
    })
    if (ok) deleteScenario.mutate(s.id)
  }

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage(pagination.current ?? 1)
  }

  const columns: ColumnsType<AdminScenario> = [
    {
      title: 'Kịch bản',
      key: 'title',
      render: (_, s) => (
        <div className="min-w-0">
          <div className="font-medium text-slate-800 truncate max-w-md">{s.title}</div>
          <div className="text-xs text-slate-500 truncate max-w-md">
            {s.userRole} ↔ {s.aiRole}
          </div>
        </div>
      ),
    },
    {
      title: 'Chủ đề',
      dataIndex: 'topicId',
      key: 'topicId',
      responsive: ['md'],
      render: (t: string) => <span className="text-sm text-slate-600">{scenarioTopicName(t)}</span>,
    },
    {
      title: 'Cấp độ',
      dataIndex: 'jlptLevel',
      key: 'jlptLevel',
      render: (l: JlptLevel) => (
        <Tag color="blue" className="rounded-md">
          {l}
        </Tag>
      ),
    },
    {
      title: 'Nội dung',
      key: 'content',
      responsive: ['lg'],
      render: (_, s) => (
        <span className="text-xs text-slate-500 tabular-nums">
          {s.tasks?.length ?? 0} nhiệm vụ · {s.sampleDialogue?.length ?? 0} câu thoại ·{' '}
          {s.vocabulary?.length ?? 0} từ
        </span>
      ),
    },
    // Cột người tạo chỉ có ý nghĩa với ADMIN (TEACHER chỉ thấy kịch bản của mình)
    ...(admin
      ? [
        {
          title: 'Người tạo',
          key: 'createdBy',
          responsive: ['md'],
          render: (_: unknown, s: AdminScenario) =>
            s.createdById ? (
              <span className="text-sm text-slate-600">{s.createdByName || s.createdByEmail}</span>
            ) : (
              <Tag className="rounded-md">Hệ thống / khách</Tag>
            ),
        } satisfies ColumnsType<AdminScenario>[number],
      ]
      : []),
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
      render: (_, s) => (
        <div className="flex justify-end gap-2">
          <Tooltip title="Sửa kịch bản">
            <AntdButton
              size="small"
              icon={<Pencil className="w-4 h-4" />}
              onClick={() => navigate(`/admin/scenarios/${s.id}`)}
              aria-label="Sửa kịch bản"
            />
          </Tooltip>
          <Tooltip title="Xóa kịch bản">
            <AntdButton
              size="small"
              danger
              icon={<Trash2 className="w-4 h-4" />}
              loading={deleteScenario.isPending && deleteScenario.variables === s.id}
              onClick={() => handleDelete(s)}
              aria-label="Xóa kịch bản"
            />
          </Tooltip>
        </div>
      ),
    },
  ]

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 space-y-5">
      <FilterBar>
        <SearchInput
          placeholder="Tìm theo tiêu đề hoặc mô tả"
          value={keywordInput}
          onChange={setKeywordInput}
          onSearch={(v) => {
            setKeyword(v)
            setPage(1)
          }}
          className="md:flex-1 md:min-w-60"
        />
        <Select
          allowClear
          placeholder="Tất cả chủ đề"
          value={topicId}
          onChange={(v?: string) => {
            setTopicId(v)
            setPage(1)
          }}
          options={SCENARIO_TOPICS.map((t) => ({ value: t.id, label: t.name }))}
          className="md:w-56"
        />
        <Select
          allowClear
          placeholder="Tất cả cấp độ"
          value={jlptLevel}
          onChange={(v?: JlptLevel) => {
            setJlptLevel(v)
            setPage(1)
          }}
          options={LEVELS.map((l) => ({ value: l, label: l }))}
          className="md:w-40"
        />
        <AntdButton
          type="primary"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => navigate('/admin/scenarios/new')}
          className="md:ml-auto"
        >
          Tạo kịch bản
        </AntdButton>
      </FilterBar>

      {isError ? (
        <EmptyState
          icon={<MessageSquareText className="w-10 h-10 text-rose-400" />}
          title="Không tải được danh sách kịch bản"
          description="Kiểm tra kết nối hoặc quyền truy cập rồi thử lại."
          action={<AntdButton onClick={() => refetch()}>Thử lại</AntdButton>}
        />
      ) : (
        <div className="bg-surface rounded-xl border border-slate-200 overflow-x-auto">
          <Table<AdminScenario>
            rowKey="id"
            columns={columns}
            dataSource={scenarios}
            loading={isLoading || isFetching}
            onChange={handleTableChange}
            pagination={{
              current: data?.page ?? page,
              pageSize: PAGE_SIZE,
              total: data?.totalElements ?? 0,
              showSizeChanger: false,
              showTotal: (total) => `${total} kịch bản`,
            }}
            locale={{
              emptyText: (
                <EmptyState
                  icon={<MessageSquareText className="w-10 h-10 text-slate-300" />}
                  title={keyword || topicId || jlptLevel ? 'Không có kịch bản phù hợp' : 'Chưa có kịch bản nào'}
                  description={
                    keyword || topicId || jlptLevel
                      ? 'Thử đổi từ khóa hoặc bộ lọc.'
                      : 'Bấm "Tạo kịch bản" để soạn kịch bản luyện nói đầu tiên.'
                  }
                />
              ),
            }}
          />
        </div>
      )}
    </div>
  )
}
