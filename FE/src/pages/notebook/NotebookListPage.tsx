import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Input,
  Dropdown,
  Modal,
  message,
  Switch,
  Tooltip,
  Button,
  Select,
} from 'antd'
import type { MenuProps } from 'antd'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  FilterOutlined,
  FileTextOutlined,
  FolderAddOutlined,
  PlusCircleOutlined,
  CheckOutlined,
  ReadOutlined,
  InfoCircleOutlined,
  FolderOpenOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import {
  useMyNotebooks,
  useMyGroups,
  useNotebookMutations,
  useNotebookStatsOverview,
} from '@/hooks/useNotebooks'
import { NotebookCard } from '@/components/notebook/NotebookCard'
import { NotebookGroupCard } from '@/components/notebook/NotebookGroupCard'
import { CreateNotebookSimpleModal } from '@/components/notebook/CreateNotebookSimpleModal'
import { CreateGroupWithNotebookModal } from '@/components/notebook/CreateGroupWithNotebookModal'
import { ImportShareSimpleModal } from '@/components/notebook/ImportShareSimpleModal'
import { ShareNotebookModal } from '@/components/notebook/ShareNotebookModal'
import { ReviewModeSelectModal } from '@/components/notebook/ReviewModeSelectModal'
import { notebookService } from '@/services/notebook.service'
import type { Notebook, NotebookGroup } from '@/types'

type SortOption = 'newest' | 'oldest' | 'nameAsc' | 'nameDesc'

const SORT_LABELS: Record<SortOption, string> = {
  newest: 'Mới nhất',
  oldest: 'Cũ nhất',
  nameAsc: 'Theo tên ( A → Z )',
  nameDesc: 'Theo tên ( Z → A )',
}

export const NotebookListPage: React.FC = () => {
  const navigate = useNavigate()

  // Queries & Mutations
  const { data: notebooks = [], isLoading: notebooksLoading, refetch: refetchNotebooks } = useMyNotebooks()
  const { data: rawGroups = [], isLoading: groupsLoading, refetch: refetchGroups } = useMyGroups()
  const { data: stats, refetch: refetchStats } = useNotebookStatsOverview()
  const {
    createNotebook,
    deleteNotebook,
    moveToGroup,
    createGroup,
    updateGroup,
    deleteGroup,
  } = useNotebookMutations()

  // State điều hướng nhóm (Nếu null: Xem ở cấp ngoài Root; Nếu có: Xem bên trong nhóm - Ảnh 3)
  const [currentGroup, setCurrentGroup] = useState<NotebookGroup | null>(null)
  const [isSyncing, setIsSyncing] = useState(false)

  // State tìm kiếm & sắp xếp
  const [keyword, setKeyword] = useState('')
  const [sort, setSort] = useState<SortOption>('newest')
  const [srsEnabled, setSrsEnabled] = useState(true)

  // Modals state
  const [createNotebookOpen, setCreateNotebookOpen] = useState(false)
  const [createGroupOpen, setCreateGroupOpen] = useState(false)
  const [importShareOpen, setImportShareOpen] = useState(false)
  const [reviewSelectOpen, setReviewSelectOpen] = useState(false)
  const [sharingNotebook, setSharingNotebook] = useState<Notebook | null>(null)
  const [movingNotebook, setMovingNotebook] = useState<Notebook | null>(null)
  const [selectedTargetGroupId, setSelectedTargetGroupId] = useState<number | null>(null)

  // Đổi tên nhóm
  const [editingGroup, setEditingGroup] = useState<NotebookGroup | null>(null)
  const [newGroupName, setNewGroupName] = useState('')

  const handleSyncAll = async (silent = false) => {
    try {
      setIsSyncing(true)
      const results = await Promise.all([
        refetchNotebooks(),
        refetchGroups(),
        refetchStats(),
      ])
      const hasError = results.some((r) => r.isError)
      if (hasError) {
        if (!silent) message.error('Không thể đồng bộ dữ liệu sổ tay')
      } else if (!silent) {
        message.success('Đã đồng bộ toàn bộ dữ liệu sổ tay và tiến độ SRS mới nhất!')
      }
    } catch {
      if (!silent) {
        message.error('Không thể đồng bộ dữ liệu sổ tay')
      }
    } finally {
      setIsSyncing(false)
    }
  }

  // 1. Thêm mới Sổ tay (Nếu đang ở trong nhóm thì tự động gán vào nhóm đó)
  const handleCreateNotebook = async (name: string) => {
    try {
      await createNotebook.mutateAsync({
        name,
        groupId: currentGroup ? currentGroup.id : null,
      })
      message.success('Đã tạo sổ tay mới thành công!')
      handleSyncAll(true)
    } catch {
      message.error('Không thể tạo sổ tay!')
    }
  }

  // 2. Thêm mới Nhóm sổ tay kèm 1 sổ tay ban đầu (Ảnh 3)
  const handleCreateGroupWithNotebook = async (
    groupName: string,
    notebookName: string
  ) => {
    try {
      const groupRes = await createGroup.mutateAsync({ name: groupName })
      const groupId = groupRes.data?.id

      await createNotebook.mutateAsync({
        name: notebookName,
        groupId,
      })
      message.success(`Đã tạo nhóm "${groupName}" và sổ tay "${notebookName}" thành công!`)
      handleSyncAll(true)
    } catch {
      message.error('Không thể tạo nhóm sổ tay!')
    }
  }

  // 3. Nhập mã chia sẻ (Ảnh 4)
  const handleImportShareCode = async (shareCode: string) => {
    try {
      const res = await notebookService.cloneNotebookByShareCode(
        shareCode,
        currentGroup ? currentGroup.id : null
      )
      if (res.data) {
        message.success(`Đã nhân bản sổ tay "${res.data.name}" thành công!`)
        handleSyncAll(true)
        navigate(`/notebooks/${res.data.id}`)
      }
    } catch {
      message.error('Mã sổ tay không tồn tại hoặc tính năng chia sẻ đã bị tắt!')
    }
  }

  // Chuyển nhóm cho sổ tay
  const handleStartMoveNotebook = (nb: Notebook) => {
    setMovingNotebook(nb)
    setSelectedTargetGroupId(nb.groupId ?? null)
  }

  const handleConfirmMoveNotebook = async () => {
    if (!movingNotebook) return
    try {
      await moveToGroup.mutateAsync({
        id: movingNotebook.id,
        targetGroupId: selectedTargetGroupId,
      })
      message.success('Đã chuyển nhóm cho sổ tay thành công!')
      setMovingNotebook(null)
      handleSyncAll(true)
    } catch {
      message.error('Không thể chuyển nhóm sổ tay')
    }
  }

  // Xóa sổ tay
  const handleDeleteNotebook = (nb: Notebook) => {
    Modal.confirm({
      title: `Xóa sổ tay "${nb.name}"`,
      content: 'Toàn bộ từ vựng bên trong sẽ bị xóa theo. Bạn có chắc không?',
      okText: 'Xóa',
      okType: 'danger',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          await deleteNotebook.mutateAsync(nb.id)
          message.success('Đã xóa sổ tay thành công')
          handleSyncAll(true)
        } catch {
          message.error('Không thể xóa sổ tay')
        }
      },
    })
  }

  // Xóa nhóm
  const handleDeleteGroup = (g: NotebookGroup) => {
    Modal.confirm({
      title: `Xóa nhóm "${g.name}"`,
      content: 'Các sổ tay bên trong nhóm này sẽ được chuyển ra danh sách ngoài. Bạn có chắc không?',
      okText: 'Xóa nhóm',
      okType: 'danger',
      cancelText: 'Hủy',
      onOk: async () => {
        try {
          await deleteGroup.mutateAsync(g.id)
          message.success('Đã xóa nhóm sổ tay')
          if (currentGroup?.id === g.id) {
            setCurrentGroup(null)
          }
          handleSyncAll(true)
        } catch {
          message.error('Không thể xóa nhóm')
        }
      },
    })
  }

  // Đổi tên nhóm
  const handleStartRenameGroup = (g: NotebookGroup) => {
    setEditingGroup(g)
    setNewGroupName(g.name)
  }

  const handleConfirmRenameGroup = async () => {
    if (!editingGroup || !newGroupName.trim()) return
    try {
      await updateGroup.mutateAsync({
        id: editingGroup.id,
        data: { name: newGroupName.trim() },
      })
      message.success('Đã đổi tên nhóm thành công!')
      if (currentGroup?.id === editingGroup.id) {
        setCurrentGroup({ ...currentGroup, name: newGroupName.trim() })
      }
      setEditingGroup(null)
      handleSyncAll(true)
    } catch {
      message.error('Không thể đổi tên nhóm')
    }
  }

  // Dropdown menu cho nút "Thêm mới +" ở danh sách ngoài (Ảnh 1)
  const rootAddMenuItems: MenuProps['items'] = [
    {
      key: 'add-notebook',
      label: <span className="font-medium text-xs py-1">Thêm mới Sổ tay</span>,
      icon: <FileTextOutlined className="text-gray-600" />,
      onClick: () => setCreateNotebookOpen(true),
    },
    {
      key: 'add-group',
      label: <span className="font-medium text-xs py-1">Thêm mới Nhóm sổ tay</span>,
      icon: <FolderAddOutlined className="text-gray-600" />,
      onClick: () => setCreateGroupOpen(true),
    },
    {
      key: 'import-share',
      label: <span className="font-medium text-xs py-1">Nhập Sổ tay</span>,
      icon: <PlusCircleOutlined className="text-gray-600" />,
      onClick: () => setImportShareOpen(true),
    },
  ]

  // Dropdown menu cho nút Bộ lọc sắp xếp (Ảnh 5)
  const sortMenuItems: MenuProps['items'] = (
    ['newest', 'oldest', 'nameAsc', 'nameDesc'] as SortOption[]
  ).map((key) => ({
    key,
    label: (
      <div className="flex items-center justify-between w-44 py-1 text-xs">
        <span>{SORT_LABELS[key]}</span>
        {sort === key && <CheckOutlined className="text-blue-600 text-xs" />}
      </div>
    ),
    onClick: () => setSort(key),
  }))

  // Tính số lượng sổ tay của từng nhóm
  const groupsWithCount = rawGroups.map((g) => ({
    ...g,
    notebookCount: notebooks.filter((n) => n.groupId === g.id).length,
  }))

  // Sổ tay ngoài cùng (chưa gán vào nhóm nào)
  const ungroupedNotebooks = notebooks.filter((n) => !n.groupId)

  // Sổ tay thuộc nhóm hiện tại (nếu đang ở bên trong nhóm - Ảnh 3)
  const currentGroupNotebooks = currentGroup
    ? notebooks.filter((n) => n.groupId === currentGroup.id)
    : []

  // Xử lý tìm kiếm & sắp xếp cho sổ tay hiển thị
  const filterAndSortNotebooks = (list: Notebook[]) => {
    let res = [...list]
    if (keyword.trim()) {
      const q = keyword.trim().toLowerCase()
      res = res.filter(
        (n) =>
          n.name.toLowerCase().includes(q) ||
          (n.description && n.description.toLowerCase().includes(q))
      )
    }
    res.sort((a, b) => {
      if (sort === 'newest') return b.id - a.id
      if (sort === 'oldest') return a.id - b.id
      if (sort === 'nameAsc') return a.name.localeCompare(b.name)
      if (sort === 'nameDesc') return b.name.localeCompare(a.name)
      return 0
    })
    return res
  }

  const displayedUngroupedNotebooks = filterAndSortNotebooks(ungroupedNotebooks)
  const displayedGroupNotebooks = filterAndSortNotebooks(currentGroupNotebooks)

  // Lọc và sắp xếp nhóm theo từ khóa & bộ lọc
  const displayedGroups = groupsWithCount
    .filter((g) => (keyword.trim() ? g.name.toLowerCase().includes(keyword.trim().toLowerCase()) : true))
    .sort((a, b) => {
      if (sort === 'newest') return b.id - a.id
      if (sort === 'oldest') return a.id - b.id
      if (sort === 'nameAsc') return a.name.localeCompare(b.name)
      if (sort === 'nameDesc') return b.name.localeCompare(a.name)
      return 0
    })

  const isLoading = notebooksLoading || groupsLoading

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
      {/* 1. Header Tab Navigation: Sổ tay của tôi | Khám phá (Chỉ hiện khi ở ngoài Root) */}
      {!currentGroup && (
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="px-5 py-2 rounded-xl bg-blue-600 text-white font-medium text-sm shadow-xs cursor-pointer"
          >
            Sổ tay của tôi
          </button>
          <button
            type="button"
            onClick={() => navigate('/notebooks/explore')}
            className="px-5 py-2 rounded-xl bg-white hover:bg-gray-50 text-gray-600 font-medium text-sm border border-gray-100 transition-colors cursor-pointer"
          >
            Khám phá
          </button>
        </div>
      )}

      {/* 2. Khối Tổng quan (SRS Spaced Repetition Overview) - Chỉ hiện khi ở ngoài Root */}
      {!currentGroup && (
        <div className="bg-white border border-gray-100 rounded-3xl p-6 shadow-2xs space-y-4">
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div className="flex items-center gap-3">
              <h2 className="text-base font-bold text-gray-800">Tổng quan</h2>
              <div className="h-4 w-px bg-gray-200" />
              <div className="flex items-center gap-1.5 text-xs text-gray-600">
                <span>Học lặp lại ngắt quãng</span>
                <Tooltip title="Phương pháp Spaced Repetition (SM-2) tự động tối ưu chu kỳ ôn tập giúp ghi nhớ từ vựng sâu vào trí nhớ dài hạn.">
                  <InfoCircleOutlined className="text-gray-400 cursor-pointer text-xs" />
                </Tooltip>
                <Switch
                  size="small"
                  checked={srsEnabled}
                  onChange={setSrsEnabled}
                  className={srsEnabled ? 'bg-blue-600' : ''}
                />
              </div>
            </div>

            <button
              type="button"
              onClick={() => {
                if (notebooks.length > 0) {
                  setReviewSelectOpen(true)
                } else {
                  message.info('Vui lòng tạo hoặc thêm từ vào sổ tay trước khi ôn tập!')
                }
              }}
              className="inline-flex items-center gap-2 px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs shadow-xs transition-colors cursor-pointer"
            >
              <ReadOutlined className="text-sm" />
              <span>Ôn tập sổ tay</span>
            </button>
          </div>

          {/* 4 Thẻ mức độ ghi nhớ */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-1">
            <div className="bg-white border border-red-100/80 rounded-2xl p-4 shadow-2xs relative overflow-hidden">
              <div className="absolute left-0 top-3 bottom-3 w-1 bg-red-500 rounded-r" />
              <div className="pl-2">
                <div className="text-xs text-gray-500 font-medium">Chưa thuộc</div>
                <div className="text-2xl font-bold text-gray-900 mt-1">
                  {stats?.notLearnedCount ?? stats?.unlearnedCount ?? 0}{' '}
                  <span className="text-xs font-normal text-gray-400">từ</span>
                </div>
              </div>
            </div>

            <div className="bg-white border border-amber-100/80 rounded-2xl p-4 shadow-2xs relative overflow-hidden">
              <div className="absolute left-0 top-3 bottom-3 w-1 bg-amber-500 rounded-r" />
              <div className="pl-2">
                <div className="text-xs text-gray-500 font-medium">Thuộc lơ mơ</div>
                <div className="text-2xl font-bold text-gray-900 mt-1">
                  {stats?.barelyCount ?? 0}{' '}
                  <span className="text-xs font-normal text-gray-400">từ</span>
                </div>
              </div>
            </div>

            <div className="bg-white border border-blue-100/80 rounded-2xl p-4 shadow-2xs relative overflow-hidden">
              <div className="absolute left-0 top-3 bottom-3 w-1 bg-blue-500 rounded-r" />
              <div className="pl-2">
                <div className="text-xs text-gray-500 font-medium">Thuộc kha khá</div>
                <div className="text-2xl font-bold text-gray-900 mt-1">
                  {stats?.moderateCount ?? 0}{' '}
                  <span className="text-xs font-normal text-gray-400">từ</span>
                </div>
              </div>
            </div>

            <div className="bg-white border border-emerald-100/80 rounded-2xl p-4 shadow-2xs relative overflow-hidden">
              <div className="absolute left-0 top-3 bottom-3 w-1 bg-emerald-500 rounded-r" />
              <div className="pl-2">
                <div className="text-xs text-gray-500 font-medium">Thuộc làu làu</div>
                <div className="text-2xl font-bold text-gray-900 mt-1">
                  {stats?.masteredCount ?? 0}{' '}
                  <span className="text-xs font-normal text-gray-400">từ</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 3. Nút QUAY LẠI khi đang ở bên trong nhóm (Đúng như Ảnh 3) */}
      {currentGroup && (
        <div>
          <button
            type="button"
            onClick={() => setCurrentGroup(null)}
            className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold transition-colors cursor-pointer"
          >
            <ArrowLeftOutlined /> Quay lại
          </button>
        </div>
      )}

      {/* 4. Khối Danh sách Sổ tay (Khung lớn bo tròn) */}
      <div className="bg-white border border-gray-100 rounded-3xl p-6 shadow-2xs space-y-6">
        {/* Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-gray-100">
          <div className="flex items-center gap-3">
            {/* Tiêu đề: Nếu đang ở trong nhóm thì hiện tên nhóm to (Ảnh 3: d1); nếu ở ngoài thì hiện "Danh sách sổ tay" */}
            <h2 className="text-base font-bold text-gray-800">
              {currentGroup ? currentGroup.name : 'Danh sách sổ tay'}
            </h2>

            {!currentGroup && (
              <button
                type="button"
                disabled={isSyncing}
                onClick={() => handleSyncAll(false)}
                className="flex items-center gap-1.5 text-xs text-gray-600 hover:text-blue-600 bg-gray-50 hover:bg-blue-50 px-3 py-1.5 rounded-xl border border-gray-200/80 transition-all cursor-pointer disabled:opacity-60 shadow-2xs"
                title="Đồng bộ danh sách sổ tay, thư mục và thống kê SRS từ máy chủ"
              >
                <ReloadOutlined className={`text-xs ${isSyncing ? 'animate-spin text-blue-600' : ''}`} />
                <span>{isSyncing ? 'Đang đồng bộ...' : 'Đồng bộ sổ tay'}</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            {/* Ô tìm kiếm */}
            <Input
              prefix={<SearchOutlined className="text-gray-400 text-sm" />}
              placeholder="Tìm kiếm..."
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-48 sm:w-56 h-9 rounded-xl border-gray-200 text-xs"
            />

            {/* Nút Thêm mới: Nếu đang ở trong nhóm thì bấm thẳng vào tạo sổ tay vào nhóm đó (Ảnh 3: nút +); nếu ở ngoài thì mở dropdown 3 mục (Ảnh 1) */}
            {currentGroup ? (
              <button
                type="button"
                onClick={() => setCreateNotebookOpen(true)}
                className="w-9 h-9 rounded-xl bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center text-sm shadow-2xs transition-colors cursor-pointer"
                title="Thêm sổ tay vào nhóm này"
              >
                <PlusOutlined />
              </button>
            ) : (
              <Dropdown menu={{ items: rootAddMenuItems }} trigger={['click']} placement="bottomRight">
                <button
                  type="button"
                  className="h-9 pl-3.5 pr-1 bg-white hover:bg-gray-50 border border-gray-200 rounded-full flex items-center gap-2 text-xs font-medium text-gray-700 shadow-2xs transition-all cursor-pointer"
                >
                  <span>Thêm mới</span>
                  <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs shadow-2xs">
                    <PlusOutlined />
                  </span>
                </button>
              </Dropdown>
            )}

            {/* Nút Bộ lọc sắp xếp icon tròn (Ảnh 5) */}
            <Dropdown menu={{ items: sortMenuItems }} trigger={['click']} placement="bottomRight">
              <button
                type="button"
                className="w-9 h-9 rounded-full bg-white hover:bg-gray-50 border border-gray-200 flex items-center justify-center text-gray-600 hover:text-blue-600 transition-colors cursor-pointer shadow-2xs"
                title="Sắp xếp danh sách"
              >
                <FilterOutlined className="text-sm" />
              </button>
            </Dropdown>
          </div>
        </div>

        {/* 5. NỘI DUNG LƯỚI CARD (Grid 3 Cột) */}
        {isLoading ? (
          <div className="py-20 text-center text-gray-400 text-xs">
            Đang nạp danh sách...
          </div>
        ) : currentGroup ? (
          /* TRƯỜNG HỢP 1: Đang xem bên trong nhóm (Ảnh 3) */
          displayedGroupNotebooks.length === 0 ? (
            <div className="py-16 text-center text-gray-400 text-xs space-y-3">
              <div>Nhóm này chưa có cuốn sổ tay nào.</div>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => setCreateNotebookOpen(true)}
                className="rounded-xl"
              >
                Thêm sổ tay đầu tiên vào nhóm
              </Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {displayedGroupNotebooks.map((nb) => (
                <NotebookCard
                  key={nb.id}
                  notebook={nb}
                  onClick={(item) => navigate(`/notebooks/${item.id}`)}
                  onEdit={(item) => navigate(`/notebooks/${item.id}`)}
                  onDelete={handleDeleteNotebook}
                  onShare={(item) => setSharingNotebook(item)}
                  onMove={handleStartMoveNotebook}
                />
              ))}
            </div>
          )
        ) : (
          /* TRƯỜNG HỢP 2: Đang ở danh sách ngoài Root (Ảnh 2) */
          displayedGroups.length === 0 && displayedUngroupedNotebooks.length === 0 ? (
            <div className="py-20 flex flex-col items-center justify-center text-center">
              <div className="w-16 h-16 rounded-3xl bg-gray-50 flex items-center justify-center text-gray-300 mb-4 border border-gray-100">
                <FolderOpenOutlined className="text-3xl" />
              </div>
              <h3 className="font-semibold text-gray-700 text-sm">Chưa có sổ tay nào</h3>
              <p className="text-xs text-gray-400 mt-1 max-w-sm">
                Tạo sổ tay hoặc nhóm sổ tay đầu tiên của bạn để bắt đầu lưu từ vựng và ôn tập.
              </p>
              <button
                type="button"
                onClick={() => setCreateNotebookOpen(true)}
                className="mt-4 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs shadow-xs transition-colors cursor-pointer"
              >
                Tạo sổ tay ngay
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* PHẦN A: Các Card Nhóm sổ tay màu xanh dương 📁 (Ví dụ: d1 - 1 mục như Ảnh 2) */}
              {displayedGroups.map((g) => (
                <NotebookGroupCard
                  key={`group-${g.id}`}
                  group={g}
                  notebookCount={g.notebookCount}
                  onClick={(selectedG) => setCurrentGroup(selectedG as NotebookGroup)}
                  onEdit={(selectedG) => handleStartRenameGroup(selectedG as NotebookGroup)}
                  onDelete={(selectedG) => handleDeleteGroup(selectedG as NotebookGroup)}
                />
              ))}

              {/* PHẦN B: Các Card Sổ tay độc lập màu vàng cam 📒 (Ví dụ: ffgf - 1 từ như Ảnh 2) */}
              {displayedUngroupedNotebooks.map((nb) => (
                <NotebookCard
                  key={`nb-${nb.id}`}
                  notebook={nb}
                  onClick={(item) => navigate(`/notebooks/${item.id}`)}
                  onEdit={(item) => navigate(`/notebooks/${item.id}`)}
                  onDelete={handleDeleteNotebook}
                  onShare={(item) => setSharingNotebook(item)}
                  onMove={handleStartMoveNotebook}
                />
              ))}
            </div>
          )
        )}
      </div>

      {/* 6. Các Modals chuẩn Mazii */}
      {/* Modal 1: Tạo sổ tay (Ảnh 2) */}
      <CreateNotebookSimpleModal
        open={createNotebookOpen}
        loading={createNotebook.isPending}
        onClose={() => setCreateNotebookOpen(false)}
        onSubmit={handleCreateNotebook}
      />

      {/* Modal 2: Tạo mới nhóm sổ tay (Ảnh 3) */}
      <CreateGroupWithNotebookModal
        open={createGroupOpen}
        loading={createGroup.isPending || createNotebook.isPending}
        onClose={() => setCreateGroupOpen(false)}
        onSubmit={handleCreateGroupWithNotebook}
      />

      {/* Modal 3: Nhập mã sổ tay (Ảnh 4) */}
      <ImportShareSimpleModal
        open={importShareOpen}
        onClose={() => setImportShareOpen(false)}
        onConfirm={handleImportShareCode}
      />

      {/* Modal 4: Chia sẻ sổ tay */}
      <ShareNotebookModal
        open={Boolean(sharingNotebook)}
        notebook={sharingNotebook}
        onClose={() => setSharingNotebook(null)}
        onToggleSharing={async (enabled) => {
          if (sharingNotebook) {
            const res = await notebookService.toggleSharing(sharingNotebook.id, enabled)
            if (res.data) setSharingNotebook(res.data)
            handleSyncAll(true)
          }
        }}
      />

      {/* Modal 5: Đổi tên nhóm */}
      <Modal
        title="Đổi tên nhóm sổ tay"
        open={Boolean(editingGroup)}
        onCancel={() => setEditingGroup(null)}
        onOk={handleConfirmRenameGroup}
        confirmLoading={updateGroup.isPending}
        okText="Lưu thay đổi"
        cancelText="Hủy"
        destroyOnClose
        centered
      >
        <div className="py-3 space-y-2">
          <label className="block text-xs font-semibold text-gray-700">
            Tên nhóm sổ tay mới
          </label>
          <Input
            value={newGroupName}
            onChange={(e) => setNewGroupName(e.target.value)}
            onPressEnter={handleConfirmRenameGroup}
            autoFocus
            className="h-10 rounded-xl"
          />
        </div>
      </Modal>

      {/* Modal 6: Chuyển nhóm cho sổ tay */}
      <Modal
        title={`Chuyển nhóm cho "${movingNotebook?.name}"`}
        open={Boolean(movingNotebook)}
        onCancel={() => setMovingNotebook(null)}
        onOk={handleConfirmMoveNotebook}
        confirmLoading={moveToGroup.isPending}
        okText="Chuyển nhóm"
        cancelText="Hủy"
        destroyOnClose
        centered
      >
        <div className="py-3 space-y-3">
          <label className="block text-xs font-semibold text-gray-700">
            Chọn nhóm đích:
          </label>
          <Select
            className="w-full h-11"
            value={selectedTargetGroupId}
            onChange={setSelectedTargetGroupId}
            options={[
              { label: '📁 (Không thuộc nhóm nào - Để ngoài cùng)', value: null },
              ...rawGroups.map((g) => ({
                label: `📁 ${g.name}`,
                value: g.id,
              })),
            ]}
          />
        </div>
      </Modal>

      {/* Modal 7: Chọn chế độ luyện tập Ôn tập sổ tay (Chuẩn Mazii) */}
      <ReviewModeSelectModal
        open={reviewSelectOpen}
        onClose={() => setReviewSelectOpen(false)}
        notebooks={notebooks}
        srsEnabled={srsEnabled}
        onToggleSrs={setSrsEnabled}
      />
    </div>
  )
}
export default NotebookListPage
