import React, { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button,
  Input,
  Dropdown,
  Tag,
  Popconfirm,
  message,
  Spin,
  Segmented,
} from 'antd'
import type { MenuProps } from 'antd'
import {
  PlusOutlined,
  MoreOutlined,
  ReadOutlined,
  SearchOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  ShareAltOutlined,
  DeleteOutlined,
  EditOutlined,
  FolderOutlined,
  CloudDownloadOutlined,
} from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import {
  useNotebookDetail,
  useNotebookWords,
  useNotebookShuffledWords,
  useNotebookWordMutations,
  useNotebookMutations,
  NOTEBOOK_QUERY_KEYS,
} from '@/hooks/useNotebooks'
import { WordTable } from '@/components/notebook/WordTable'
import { WordCardCarousel } from '@/components/notebook/WordCardCarousel'
import { WordFormModal } from '@/components/notebook/WordFormModal'
import { WordImportModal } from '@/components/notebook/WordImportModal'
import { KanjiPdfPreviewModal } from '@/components/notebook/KanjiPdfPreviewModal'
import { ShareNotebookModal } from '@/components/notebook/ShareNotebookModal'
import { NotebookFormModal } from '@/components/notebook/NotebookFormModal'
import { ReviewModeSelectModal } from '@/components/notebook/ReviewModeSelectModal'
import { NOTEBOOK_ITEM_TYPES, NOTEBOOK_ITEM_TYPE_META } from '@/components/notebook/itemTypeMeta'
import { notebookService } from '@/services/notebook.service'
import type {
  NotebookWord,
  CreateNotebookWordRequest,
  UpdateNotebookWordRequest,
  UpdateNotebookRequest,
  NotebookItemType,
} from '@/types'
import { BackButton } from '@/components/common'

export const NotebookDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const notebookId = id ? parseInt(id, 10) : 0
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // State tìm kiếm & phân trang từ vựng
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [selectedWordIds, setSelectedWordIds] = useState<number[]>([])
  // Lọc theo loại thẻ: ALL = cả 3 loại
  const [typeFilter, setTypeFilter] = useState<NotebookItemType | 'ALL'>('ALL')

  // Modal states
  const [wordModalOpen, setWordModalOpen] = useState(false)
  const [editingWord, setEditingWord] = useState<NotebookWord | null>(null)
  const [importModalOpen, setImportModalOpen] = useState(false)
  const [kanjiPdfModalOpen, setKanjiPdfModalOpen] = useState(false)
  const [shareModalOpen, setShareModalOpen] = useState(false)
  const [reviewSelectOpen, setReviewSelectOpen] = useState(false)
  const [editNotebookModalOpen, setEditNotebookModalOpen] = useState(false)

  // React Query hooks
  const { data: notebook, isLoading: notebookLoading } = useNotebookDetail(notebookId)
  const { data: wordsData, isLoading: wordsLoading, refetch: refetchWords } = useNotebookWords(notebookId, {
    keyword: keyword || undefined,
    page,
    size: pageSize,
    sort: 'id,desc',
    itemType: typeFilter === 'ALL' ? undefined : typeFilter,
  })
  const { data: shuffledWords, refetch: refetchShuffled } = useNotebookShuffledWords(notebookId)

  const {
    addWord,
    updateWord,
    deleteWord,
    batchDeleteWords,
    importWordsFromExcel,
  } = useNotebookWordMutations(notebookId)

  const { updateNotebook, deleteNotebook } = useNotebookMutations()

  if (notebookLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Spin size="large" />
      </div>
    )
  }

  if (!notebook) {
    return (
      <div className="p-8 text-center">
        <h2 className="text-xl font-bold text-gray-700">Không tìm thấy sổ tay!</h2>
        <Button className="mt-4" onClick={() => navigate('/notebooks')}>
          Quay lại danh sách
        </Button>
      </div>
    )
  }

  // Xử lý Thêm / Sửa từ vựng
  const handleOpenAddWord = () => {
    setEditingWord(null)
    setWordModalOpen(true)
  }

  const handleOpenEditWord = (word: NotebookWord) => {
    setEditingWord(word)
    setWordModalOpen(true)
  }

  const handleSubmitWord = async (
    values: CreateNotebookWordRequest | UpdateNotebookWordRequest
  ) => {
    try {
      if (editingWord) {
        await updateWord.mutateAsync({
          wordId: editingWord.id,
          data: values as UpdateNotebookWordRequest,
        })
        message.success('Cập nhật từ vựng thành công!')
      } else {
        await addWord.mutateAsync(values as CreateNotebookWordRequest)
        message.success('Đã thêm từ vựng mới vào sổ tay!')
      }
      setWordModalOpen(false)
      refetchWords()
      refetchShuffled()
    } catch {
      message.error('Có lỗi xảy ra khi lưu từ vựng!')
    }
  }

  const handleDeleteWord = async (wordId: number) => {
    try {
      await deleteWord.mutateAsync(wordId)
      message.success('Đã xóa từ vựng khỏi sổ tay!')
      refetchWords()
      refetchShuffled()
    } catch {
      message.error('Không thể xóa từ vựng!')
    }
  }

  const handleBatchDelete = async () => {
    if (selectedWordIds.length === 0) return
    try {
      await batchDeleteWords.mutateAsync(selectedWordIds)
      setSelectedWordIds([])
      message.success(`Đã xóa ${selectedWordIds.length} từ vựng đã chọn!`)
      refetchWords()
      refetchShuffled()
    } catch {
      message.error('Không thể xóa hàng loạt từ vựng!')
    }
  }

  const handleUpdateNote = async (wordId: number, note: string) => {
    try {
      await updateWord.mutateAsync({
        wordId,
        data: { userNote: note },
      })
      message.success('Đã lưu ghi chú')
    } catch {
      message.error('Không thể cập nhật ghi chú')
    }
  }

  // Xuất file Excel an toàn có Bearer Token
  const handleExportExcel = async () => {
    try {
      message.loading({ content: 'Đang chuẩn bị file Excel...', key: 'exportExcel' })
      await notebookService.downloadExcel(notebookId, notebook.name)
      message.success({ content: 'Đã tải xuống file Excel thành công!', key: 'exportExcel' })
    } catch {
      message.error({ content: 'Không thể xuất file Excel!', key: 'exportExcel' })
    }
  }

  // Bật/tắt chia sẻ sổ tay
  const handleToggleSharing = async (enabled: boolean) => {
    await notebookService.toggleSharing(notebookId, enabled)
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.detail(notebookId) })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.myList() })
  }


  // Cập nhật thông tin sổ tay
  const handleUpdateNotebookSubmit = async (values: UpdateNotebookRequest) => {
    try {
      await updateNotebook.mutateAsync({ id: notebookId, data: values })
      setEditNotebookModalOpen(false)
      message.success('Đã cập nhật thông tin sổ tay!')
    } catch {
      message.error('Có lỗi xảy ra khi cập nhật sổ tay!')
    }
  }

  // Xóa sổ tay
  const handleDeleteNotebook = async () => {
    try {
      await deleteNotebook.mutateAsync(notebookId)
      message.success('Đã xóa sổ tay thành công!')
      navigate('/notebooks')
    } catch {
      message.error('Không thể xóa sổ tay!')
    }
  }

  // Menu thao tác nâng cao (⋮)
  const menuItems: MenuProps['items'] = [
    {
      key: 'edit',
      icon: <EditOutlined />,
      label: 'Chỉnh sửa sổ tay',
      onClick: () => setEditNotebookModalOpen(true),
    },
    {
      key: 'share',
      icon: <ShareAltOutlined />,
      label: 'Chia sẻ sổ tay',
      onClick: () => setShareModalOpen(true),
    },
    {
      type: 'divider',
    },
    {
      key: 'import-excel',
      icon: <FileExcelOutlined className="text-emerald-600" />,
      label: 'Nhập từ vựng từ Excel',
      onClick: () => setImportModalOpen(true),
    },
    {
      key: 'export-excel',
      icon: <CloudDownloadOutlined className="text-blue-600" />,
      label: 'Xuất từ vựng ra Excel',
      onClick: handleExportExcel,
    },
    {
      key: 'kanji-pdf',
      icon: <FilePdfOutlined className="text-red-500" />,
      label: 'Vở luyện viết Kanji (PDF)',
      onClick: () => setKanjiPdfModalOpen(true),
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      danger: true,
      icon: <DeleteOutlined />,
      label: (
        <Popconfirm
          title="Xác nhận xóa sổ tay"
          description="Toàn bộ từ vựng bên trong sẽ bị xóa theo. Bạn có chắc không?"
          okText="Xóa"
          cancelText="Hủy"
          okButtonProps={{ danger: true }}
          onConfirm={handleDeleteNotebook}
        >
          <span>Xóa sổ tay này</span>
        </Popconfirm>
      ),
    },
  ]

  const words = wordsData?.items ?? []
  const totalWords = wordsData?.totalElements ?? 0

  // Số thẻ theo loại (sổ cũ chưa có bộ đếm theo loại → coi toàn bộ là từ vựng)
  const typeCounts: Record<NotebookItemType, number> = {
    VOCAB: notebook.vocabCount ?? notebook.wordCount ?? 0,
    KANJI: notebook.kanjiCount ?? 0,
    GRAMMAR: notebook.grammarCount ?? 0,
  }
  const typeOptions = [
    { value: 'ALL', label: `Tất cả (${notebook.wordCount ?? 0})` },
    ...NOTEBOOK_ITEM_TYPES.map((t) => ({
      value: t,
      label: `${NOTEBOOK_ITEM_TYPE_META[t].label} (${typeCounts[t]})`,
      disabled: typeCounts[t] === 0 && typeFilter !== t,
    })),
  ]

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      {/* Thanh tiêu đề & Công cụ chuẩn Mazii */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-gray-100">
        <div className="flex items-center gap-3 min-w-0">
          <BackButton to="/notebooks" className="shrink-0" />

          <h1 className="text-xl sm:text-2xl font-bold text-gray-900 tracking-tight truncate">
            {notebook.name}
          </h1>
          {notebook.jlptLevel && (
            <Tag color="blue" className="rounded-full text-xs font-semibold shrink-0">
              {notebook.jlptLevel}
            </Tag>
          )}
          {(notebook.sharingEnabled || notebook.isPublic) && (
            <Tag color="success" className="rounded-full text-xs font-semibold shrink-0">
              Đã chia sẻ
            </Tag>
          )}
          {notebook.groupName && (
            <span className="text-xs text-gray-400 flex items-center gap-1 shrink-0">
              <FolderOutlined /> {notebook.groupName}
            </span>
          )}
        </div>

        {/* Thanh công cụ tìm kiếm, nút +, nút ⋮, và Ôn tập sổ tay nằm trên cùng 1 hàng */}
        <div className="flex items-center gap-2 shrink-0">
          <Input
            placeholder="Tìm trong sổ tay..."
            prefix={<SearchOutlined className="text-gray-400 text-xs" />}
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value)
              setPage(0)
            }}
            allowClear
            className="w-44 sm:w-56 h-9 rounded-xl border-gray-200 text-xs"
          />

          {/* Nút + Thêm từ bo góc chuẩn Mazii */}
          <button
            type="button"
            onClick={handleOpenAddWord}
            className="h-9 px-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 flex items-center gap-1.5 text-xs font-semibold shadow-2xs transition-colors cursor-pointer whitespace-nowrap"
            title="Thêm từ mới"
          >
            <PlusOutlined className="text-xs" />
            <span>Thêm từ</span>
          </button>

          {/* Nút ⋮ vuông bo góc chuẩn Mazii */}
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <button
              type="button"
              className="w-9 h-9 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 flex items-center justify-center text-base shadow-2xs transition-colors cursor-pointer"
              title="Thao tác khác"
            >
              <MoreOutlined />
            </button>
          </Dropdown>

          {/* Nút Ôn tập sổ tay màu xanh bo góc chuẩn Mazii */}
          <button
            type="button"
            onClick={() => {
              if (notebook && (notebook.wordCount || 0) > 0) {
                setReviewSelectOpen(true)
              } else {
                message.info('Sổ tay này chưa có mục nào để ôn tập!')
              }
            }}
            className="h-9 px-4 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs flex items-center gap-1.5 shadow-xs transition-colors cursor-pointer whitespace-nowrap"
          >
            <ReadOutlined className="text-sm" />
            <span>Ôn tập sổ tay</span>
          </button>
        </div>
      </div>

      {/* Thẻ xem nhanh Flashcard preview (Mặt trước / Mặt sau) */}
      <WordCardCarousel
        words={shuffledWords && shuffledWords.length > 0 ? shuffledWords : words}
        onShuffle={() => refetchShuffled()}
      />

      {/* Danh sách từ vựng */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex flex-wrap items-center gap-3">
            <h2 className="text-lg font-bold text-gray-800">Danh sách</h2>
            <Segmented
              size="small"
              value={typeFilter}
              options={typeOptions}
              onChange={(v) => {
                setTypeFilter(v as NotebookItemType | 'ALL')
                setPage(0)
                setSelectedWordIds([])
              }}
            />
            <span className="text-xs text-gray-400 font-medium">({totalWords} mục)</span>
          </div>

          {selectedWordIds.length > 0 && (
            <Popconfirm
              title="Xác nhận xóa hàng loạt"
              description={`Bạn có chắc muốn xóa ${selectedWordIds.length} từ đã chọn?`}
              okText="Xóa"
              cancelText="Hủy"
              okButtonProps={{ danger: true }}
              onConfirm={handleBatchDelete}
            >
              <Button danger size="small" icon={<DeleteOutlined />}>
                Xóa {selectedWordIds.length} mục đã chọn
              </Button>
            </Popconfirm>
          )}
        </div>

        <WordTable
          words={words}
          loading={wordsLoading}
          total={totalWords}
          currentPage={page}
          pageSize={pageSize}
          selectedWordIds={selectedWordIds}
          onPageChange={(newPage, newPageSize) => {
            setPage(newPage)
            setPageSize(newPageSize)
          }}
          onSelectionChange={setSelectedWordIds}
          onEditWord={handleOpenEditWord}
          onDeleteWord={handleDeleteWord}
          onUpdateNote={handleUpdateNote}
        />
      </div>

      {/* Modals */}
      <WordFormModal
        open={wordModalOpen}
        editingWord={editingWord}
        submitting={addWord.isPending || updateWord.isPending}
        onCancel={() => setWordModalOpen(false)}
        onSubmit={handleSubmitWord}
      />

      <WordImportModal
        open={importModalOpen}
        submitting={importWordsFromExcel.isPending}
        onCancel={() => {
          setImportModalOpen(false)
          refetchWords()
          refetchShuffled()
        }}
        onImport={async (file) => {
          const res = await importWordsFromExcel.mutateAsync(file)
          refetchWords()
          refetchShuffled()
          return res.data
        }}
      />

      <KanjiPdfPreviewModal
        open={kanjiPdfModalOpen}
        notebookId={notebookId}
        notebookName={notebook.name}
        onClose={() => setKanjiPdfModalOpen(false)}
      />

      <ShareNotebookModal
        open={shareModalOpen}
        notebook={notebook}
        onClose={() => setShareModalOpen(false)}
        onToggleSharing={handleToggleSharing}
      />

      <NotebookFormModal
        open={editNotebookModalOpen}
        initialData={notebook}
        loading={updateNotebook.isPending}
        onClose={() => setEditNotebookModalOpen(false)}
        onSubmit={async (values) => {
          await handleUpdateNotebookSubmit(values)
        }}
      />
      <ReviewModeSelectModal
        open={reviewSelectOpen}
        onClose={() => setReviewSelectOpen(false)}
        notebooks={notebook ? [notebook] : []}
        defaultNotebookId={notebookId}
      />
    </div>
  )
}
export default NotebookDetailPage
