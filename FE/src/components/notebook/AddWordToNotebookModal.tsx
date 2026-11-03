import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react'
import { Modal, Input, message, Tooltip, Select } from 'antd'
import {
  SearchOutlined,
  PlusOutlined,
  LeftOutlined,
  RightOutlined,
  LoadingOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons'
import { ArrowUpDown } from 'lucide-react'
import { MaziiFolderIcon } from './NotebookGroupCard'
import { MaziiNotebookIcon } from './NotebookCard'
import { CreateNotebookSimpleModal } from './CreateNotebookSimpleModal'
import { notebookService } from '@/services/notebook.service'
import type {
  Notebook,
  NotebookGroupWithNotebooks,
  CreateNotebookWordRequest,
} from '@/types'

export interface AddWordTargetData {
  word: string
  reading?: string
  sinoVietnamese?: string
  meaning?: string
  partOfSpeech?: string
  audioUrl?: string
}

interface AddWordToNotebookModalProps {
  open: boolean
  wordData: AddWordTargetData | null
  onClose: () => void
  onSuccess?: (notebookId: number) => void
}

type SortOption = 'date_desc' | 'date_asc' | 'name_asc' | 'name_desc'

export const AddWordToNotebookModal: React.FC<AddWordToNotebookModalProps> = ({
  open,
  wordData,
  onClose,
  onSuccess,
}) => {
  const [groups, setGroups] = useState<NotebookGroupWithNotebooks[]>([])
  const [ungroupedNotebooks, setUngroupedNotebooks] = useState<Notebook[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [sortOption, setSortOption] = useState<SortOption>('date_desc')

  // Thư mục hiện tại đang mở (null = Thư mục gốc)
  const [activeGroup, setActiveGroup] = useState<NotebookGroupWithNotebooks | null>(null)
  const activeGroupRef = useRef<NotebookGroupWithNotebooks | null>(null)
  useEffect(() => {
    activeGroupRef.current = activeGroup
  }, [activeGroup])

  // Trạng thái đang thêm từ vào sổ tay nào
  const [addingNotebookId, setAddingNotebookId] = useState<number | null>(null)

  // Modal tạo sổ tay nhanh
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [creatingNotebook, setCreatingNotebook] = useState(false)

  // Phân trang chuẩn Mazii (mặc định 20/trang)
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)

  // Tải dữ liệu thư mục & sổ tay
  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [groupsRes, ungroupedRes] = await Promise.all([
        notebookService.getMyGroupsWithNotebooks().catch(() => ({ data: [] as NotebookGroupWithNotebooks[] })),
        notebookService.getMyUngroupedNotebooks().catch(() => ({ data: [] as Notebook[] })),
      ])

      const fetchedGroups = groupsRes.data || []
      const fetchedUngrouped = ungroupedRes.data || []

      setGroups(fetchedGroups)
      setUngroupedNotebooks(fetchedUngrouped)

      // Nếu đang mở group, cập nhật lại dữ liệu group đó
      const currentActive = activeGroupRef.current
      if (currentActive) {
        const updatedActive = fetchedGroups.find((g) => g.id === currentActive.id)
        if (updatedActive) {
          setActiveGroup(updatedActive)
        }
      }
    } catch (err) {
      console.error('Lỗi tải danh sách sổ tay:', err)
      message.error('Không thể tải danh sách sổ tay')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (open) {
      setSearchKeyword('')
      setActiveGroup(null)
      setCurrentPage(1)
      loadData()
    }
  }, [open, loadData])

  // Reset trang về 1 khi đổi từ khóa tìm kiếm hoặc đổi thư mục
  useEffect(() => {
    setCurrentPage(1)
  }, [searchKeyword, activeGroup, pageSize])

  // Định dạng ngày theo chuẩn vi-VN (DD/MM/YYYY)
  const formatDate = (isoString?: string) => {
    if (!isoString) return ''
    try {
      const date = new Date(isoString)
      return date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })
    } catch {
      return ''
    }
  }

  // Sắp xếp danh sách
  const sortItems = useCallback(
    <T extends { name: string; createdAt?: string }>(items: T[]): T[] => {
      return [...items].sort((a, b) => {
        if (sortOption === 'name_asc') {
          return a.name.localeCompare(b.name, 'vi')
        }
        if (sortOption === 'name_desc') {
          return b.name.localeCompare(a.name, 'vi')
        }
        const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
        const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
        if (sortOption === 'date_asc') {
          return timeA - timeB
        }
        return timeB - timeA
      })
    },
    [sortOption]
  )

  // Danh sách các mục hiển thị (kết hợp Groups và Notebooks theo ngữ cảnh)
  const displayItems = useMemo(() => {
    const kw = searchKeyword.trim().toLowerCase()

    // Trường hợp đang mở một Thư mục cụ thể: chỉ hiển thị sổ tay thuộc thư mục đó
    if (activeGroup) {
      let nbs = activeGroup.notebooks || []
      if (kw) {
        nbs = nbs.filter((n) => n.name.toLowerCase().includes(kw))
      }
      const sortedNbs = sortItems(nbs)
      return sortedNbs.map((nb) => ({ type: 'notebook' as const, data: nb }))
    }

    // Trường hợp đang ở Thư mục gốc:
    // Nếu có từ khóa tìm kiếm: tìm cả trong group và tất cả notebook
    if (kw) {
      const matchedGroups = groups.filter((g) => g.name.toLowerCase().includes(kw))
      // Gom tất cả notebook (cả ungrouped và trong groups) khớp từ khóa
      const allNotebooks: Notebook[] = [
        ...ungroupedNotebooks,
        ...groups.flatMap((g) => g.notebooks || []),
      ]
      const matchedNotebooks = allNotebooks.filter((n) => n.name.toLowerCase().includes(kw))

      return [
        ...sortItems(matchedGroups).map((g) => ({ type: 'group' as const, data: g })),
        ...sortItems(matchedNotebooks).map((n) => ({ type: 'notebook' as const, data: n })),
      ]
    }

    // Ở thư mục gốc không có search: hiển thị Thư mục + Sổ tay chưa phân nhóm
    return [
      ...sortItems(groups).map((g) => ({ type: 'group' as const, data: g })),
      ...sortItems(ungroupedNotebooks).map((n) => ({ type: 'notebook' as const, data: n })),
    ]
  }, [activeGroup, groups, ungroupedNotebooks, searchKeyword, sortItems])

  // Phân trang
  const totalItems = displayItems.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const safeCurrentPage = Math.min(Math.max(1, currentPage), totalPages)

  const paginatedItems = useMemo(() => {
    const startIndex = (safeCurrentPage - 1) * pageSize
    return displayItems.slice(startIndex, startIndex + pageSize)
  }, [displayItems, safeCurrentPage, pageSize])

  // Xử lý thêm từ vào sổ tay
  const handleSelectNotebook = async (notebook: Notebook) => {
    if (!wordData || !wordData.word || addingNotebookId) return

    setAddingNotebookId(notebook.id)
    try {
      const payload: CreateNotebookWordRequest = {
        word: wordData.word.trim(),
        reading: wordData.reading?.trim() || undefined,
        sinoVietnamese: wordData.sinoVietnamese?.trim() || undefined,
        meaning: wordData.meaning?.trim() || wordData.sinoVietnamese?.trim() || wordData.word.trim(),
        partOfSpeech: wordData.partOfSpeech || 'Từ vựng',
        audioUrl: wordData.audioUrl || undefined,
      }

      await notebookService.addWord(notebook.id, payload)
      message.success(`Đã thêm "${wordData.word}" vào sổ tay "${notebook.name}" thành công!`)
      onSuccess?.(notebook.id)
      onClose()
    } catch (err: any) {
      const errMsg =
        err?.response?.data?.message ||
        (err?.response?.data?.code === 4001 ? 'Từ này đã có trong sổ tay' : 'Không thể thêm từ vào sổ tay')
      message.warning(errMsg)
    } finally {
      setAddingNotebookId(null)
    }
  }

  // Xử lý tạo sổ tay nhanh từ nút +
  const handleCreateNotebookSubmit = async (name: string) => {
    setCreatingNotebook(true)
    try {
      const res = await notebookService.createNotebook({
        name,
        groupId: activeGroup?.id ?? null,
      })
      message.success(`Đã tạo sổ tay "${name}" thành công`)
      setIsCreateModalOpen(false)
      await loadData()

      // Tự động thêm từ vào sổ tay vừa tạo nếu người dùng muốn
      if (res.data?.id && wordData?.word) {
        await handleSelectNotebook(res.data)
      }
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Không thể tạo sổ tay mới')
    } finally {
      setCreatingNotebook(false)
    }
  }

  // Chuyển đổi thứ tự sắp xếp
  const toggleSort = () => {
    setSortOption((prev) => {
      if (prev === 'date_desc') return 'date_asc'
      if (prev === 'date_asc') return 'name_asc'
      if (prev === 'name_asc') return 'name_desc'
      return 'date_desc'
    })
  }

  const sortLabel = {
    date_desc: 'Mới nhất',
    date_asc: 'Cũ nhất',
    name_asc: 'Tên A-Z',
    name_desc: 'Tên Z-A',
  }[sortOption]

  return (
    <>
      <Modal
        open={open}
        onCancel={onClose}
        footer={null}
        width={820}
        centered
        destroyOnClose
        className="add-word-to-notebook-modal"
        styles={{
          body: {
            padding: '4px',
          },
        }}
      >
        <div className="space-y-4">
          {/* 1. Modal Header Title */}
          <div className="flex items-center justify-between pb-1 border-b border-gray-100">
            <div>
              <h2 className="text-lg font-bold text-gray-900">Thêm từ vào sổ tay</h2>
              {wordData?.word && (
                <p className="text-xs text-gray-500 mt-0.5">
                  Từ được thêm: <span className="font-bold text-indigo-600 font-sans">{wordData.word}</span>
                  {wordData.reading ? ` 【${wordData.reading}】` : ''}
                  {wordData.sinoVietnamese ? ` (${wordData.sinoVietnamese})` : ''}
                </p>
              )}
            </div>
          </div>

          {/* 2. Search Bar + Quick Actions */}
          <div className="flex items-center gap-2.5">
            <div className="relative flex-1">
              <Input
                prefix={<SearchOutlined className="text-gray-400 mr-1" />}
                placeholder="Tìm kiếm"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                allowClear
                className="h-10 rounded-full border-gray-200 text-sm pl-4 pr-4 hover:border-gray-300 focus:border-blue-500"
              />
            </div>

            {/* Nút Tạo sổ tay mới */}
            <Tooltip title={activeGroup ? `Tạo sổ tay mới vào thư mục "${activeGroup.name}"` : 'Tạo sổ tay mới'}>
              <button
                type="button"
                onClick={() => setIsCreateModalOpen(true)}
                className="w-10 h-10 rounded-full border border-gray-200 bg-white hover:bg-gray-50 active:scale-95 text-gray-600 flex items-center justify-center transition-all cursor-pointer shadow-2xs"
              >
                <PlusOutlined className="text-base" />
              </button>
            </Tooltip>

            {/* Nút Đổi sắp xếp */}
            <Tooltip title={`Sắp xếp: ${sortLabel}`}>
              <button
                type="button"
                onClick={toggleSort}
                className="w-10 h-10 rounded-full border border-gray-200 bg-white hover:bg-gray-50 active:scale-95 text-gray-600 flex items-center justify-center transition-all cursor-pointer shadow-2xs"
              >
                <ArrowUpDown className="w-4 h-4" />
              </button>
            </Tooltip>
          </div>

          {/* 3. Breadcrumb khi đang duyệt trong thư mục */}
          {activeGroup && (
            <div className="flex items-center justify-between bg-blue-50/60 border border-blue-100 rounded-lg px-3.5 py-2">
              <div className="flex items-center gap-2 text-sm">
                <MaziiFolderIcon className="w-5 h-5 shrink-0" />
                <span className="font-semibold text-blue-950 truncate">{activeGroup.name}</span>
                <span className="text-xs text-blue-600">({activeGroup.notebooks?.length || 0} sổ tay)</span>
              </div>
              <button
                type="button"
                onClick={() => setActiveGroup(null)}
                className="flex items-center gap-1 text-xs font-semibold text-blue-700 hover:text-blue-800 hover:underline cursor-pointer"
              >
                <ArrowLeftOutlined className="text-xs" />
                <span>Quay lại tất cả</span>
              </button>
            </div>
          )}

          {/* 4. Grid danh sách Thư mục & Sổ tay (3 cột như ảnh mẫu) */}
          <div className="min-h-[340px] max-h-[460px] overflow-y-auto pr-1">
            {loading ? (
              <div className="flex flex-col items-center justify-center h-64 text-gray-400 gap-2">
                <LoadingOutlined className="text-2xl text-blue-600" />
                <span className="text-xs">Đang tải danh sách sổ tay...</span>
              </div>
            ) : paginatedItems.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 text-gray-400 gap-2">
                <span className="text-sm">Không tìm thấy thư mục hoặc sổ tay phù hợp</span>
                {searchKeyword && (
                  <button
                    type="button"
                    onClick={() => setSearchKeyword('')}
                    className="text-xs text-blue-600 hover:underline cursor-pointer"
                  >
                    Xóa tìm kiếm
                  </button>
                )}
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                {paginatedItems.map((item) => {
                  if (item.type === 'group') {
                    const group = item.data as NotebookGroupWithNotebooks
                    const count = group.notebooks?.length ?? 0
                    return (
                      <div
                        key={`group-${group.id}`}
                        onClick={() => setActiveGroup(group)}
                        className="bg-white border border-gray-200/90 hover:border-blue-500 hover:shadow-xs rounded-xl p-3.5 transition-all duration-150 cursor-pointer flex flex-col justify-between group"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2.5 min-w-0">
                            <MaziiFolderIcon className="w-6 h-6 shrink-0" />
                            <span
                              className="font-semibold text-sm text-gray-800 truncate group-hover:text-blue-600 transition-colors"
                              title={group.name}
                            >
                              {group.name}
                            </span>
                          </div>
                          <RightOutlined className="text-gray-300 text-xs group-hover:text-blue-500 transition-colors shrink-0" />
                        </div>
                        <div className="mt-4 pt-2 border-t border-gray-100 flex items-center justify-between text-xs text-gray-400">
                          <span>{count} mục</span>
                          <span>{formatDate(group.createdAt)}</span>
                        </div>
                      </div>
                    )
                  }

                  const notebook = item.data as Notebook
                  const isAdding = addingNotebookId === notebook.id

                  return (
                    <div
                      key={`notebook-${notebook.id}`}
                      onClick={() => !isAdding && handleSelectNotebook(notebook)}
                      className={`bg-white border rounded-xl p-3.5 transition-all duration-150 flex flex-col justify-between group ${
                        isAdding
                          ? 'border-amber-400 bg-amber-50/20 cursor-wait'
                          : 'border-gray-200/90 hover:border-amber-500 hover:shadow-xs cursor-pointer'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2.5 min-w-0">
                          <MaziiNotebookIcon className="w-6 h-6 shrink-0" />
                          <span
                            className="font-semibold text-sm text-gray-800 truncate group-hover:text-amber-700 transition-colors"
                            title={notebook.name}
                          >
                            {notebook.name}
                          </span>
                        </div>
                        {isAdding && <LoadingOutlined className="text-amber-500 text-sm shrink-0" />}
                      </div>
                      <div className="mt-4 pt-2 border-t border-gray-100 flex items-center justify-between text-xs text-gray-400">
                        <span className="font-medium text-gray-600">{notebook.wordCount ?? 0} từ</span>
                        <span>{formatDate(notebook.createdAt)}</span>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* 5. Pagination Bar (chuẩn giao diện ảnh mẫu Mazii, ẩn khi không có mục nào) */}
          {totalItems > 0 && (
            <div className="pt-3 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-gray-600 select-none">
              {/* Vế trái: Trang hiện tại */}
              <div className="flex items-center gap-1.5 text-gray-500">
                <span>Trang hiện tại:</span>
                <span className="font-semibold text-gray-800">{safeCurrentPage}</span>
                <span>/</span>
                <span className="font-semibold text-gray-800">{totalPages}</span>
              </div>

              {/* Vế giữa: Các nút chuyển trang */}
              <div className="flex items-center gap-1">
                {/* Về trang đầu */}
                <button
                  type="button"
                  disabled={safeCurrentPage <= 1}
                  onClick={() => setCurrentPage(1)}
                  className="w-7 h-7 rounded border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center text-gray-600 cursor-pointer font-bold text-xs"
                  title="Trang đầu"
                >
                  «
                </button>

                {/* Lùi 1 trang */}
                <button
                  type="button"
                  disabled={safeCurrentPage <= 1}
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  className="w-7 h-7 rounded border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center text-gray-600 cursor-pointer"
                  title="Trang trước"
                >
                  <LeftOutlined style={{ fontSize: 10 }} />
                </button>

                {/* Các số trang */}
                {Array.from({ length: totalPages }, (_, i) => i + 1)
                  .filter((p) => {
                    if (totalPages <= 5) return true
                    return Math.abs(p - safeCurrentPage) <= 1 || p === 1 || p === totalPages
                  })
                  .map((p, idx, arr) => {
                    const showEllipsis = idx > 0 && p - arr[idx - 1] > 1
                    return (
                      <React.Fragment key={p}>
                        {showEllipsis && <span className="px-1 text-gray-400">...</span>}
                        <button
                          type="button"
                          onClick={() => setCurrentPage(p)}
                          className={`w-7 h-7 rounded text-xs font-semibold flex items-center justify-center transition-colors cursor-pointer ${
                            p === safeCurrentPage
                              ? 'border border-blue-500 bg-blue-50 text-blue-600'
                              : 'border border-gray-200 bg-white hover:bg-gray-50 text-gray-700'
                          }`}
                        >
                          {p}
                        </button>
                      </React.Fragment>
                    )
                  })}

                {/* Tiến 1 trang */}
                <button
                  type="button"
                  disabled={safeCurrentPage >= totalPages}
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  className="w-7 h-7 rounded border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center text-gray-600 cursor-pointer"
                  title="Trang kế"
                >
                  <RightOutlined style={{ fontSize: 10 }} />
                </button>

                {/* Đến trang cuối */}
                <button
                  type="button"
                  disabled={safeCurrentPage >= totalPages}
                  onClick={() => setCurrentPage(totalPages)}
                  className="w-7 h-7 rounded border border-gray-200 bg-white hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center text-gray-600 cursor-pointer font-bold text-xs"
                  title="Trang cuối"
                >
                  »
                </button>
              </div>

              {/* Vế phải: Kích thước trang */}
              <div className="flex items-center gap-1.5">
                <Select
                  size="small"
                  value={pageSize}
                  onChange={(val) => setPageSize(val)}
                  options={[
                    { value: 12, label: '12/trang' },
                    { value: 20, label: '20/trang' },
                    { value: 50, label: '50/trang' },
                  ]}
                  className="w-24"
                />
              </div>
            </div>
          )}
        </div>
      </Modal>

      {/* Modal Tạo sổ tay nhanh lồng nhau */}
      <CreateNotebookSimpleModal
        open={isCreateModalOpen}
        loading={creatingNotebook}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={handleCreateNotebookSubmit}
      />
    </>
  )
}
export default AddWordToNotebookModal
