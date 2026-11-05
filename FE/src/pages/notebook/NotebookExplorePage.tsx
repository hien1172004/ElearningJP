import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Input,
  Dropdown,
  Button,
  Pagination,
  Spin,
  Avatar,
  message,
  Modal,
  Tag,
  Empty,
} from 'antd'
import type { MenuProps } from 'antd'
import {
  SearchOutlined,
  FilterOutlined,
  HeartOutlined,
  HeartFilled,
  DownloadOutlined,
  UserOutlined,
  BookOutlined,
  CheckOutlined,
} from '@ant-design/icons'
import {
  useExploreNotebooks,
  useFavoriteNotebooks,
  useRecentlyViewedNotebooks,
  useCommunityNotebookMutations,
} from '@/hooks/useNotebooks'
import { notebookService } from '@/services/notebook.service'
import type { Notebook } from '@/types'
import { BackButton } from '@/components/common'

type SubTab = 'explore' | 'favorites' | 'recent'
type SortOption = 'all' | 'downloads' | 'likes' | 'views'

const SORT_LABELS: Record<SortOption, string> = {
  all: 'Tất cả',
  downloads: 'Tải xuống nhiều nhất',
  likes: 'Thích nhiều nhất',
  views: 'Xem nhiều nhất',
}

export const NotebookExplorePage: React.FC = () => {
  const navigate = useNavigate()

  const [activeTab, setActiveTab] = useState<SubTab>('explore')
  const [keyword, setKeyword] = useState('')
  const [sort, setSort] = useState<SortOption>('all')
  const [page, setPage] = useState(0)
  const pageSize = 12

  // Modal xem chi tiết & clone
  const [previewModalOpen, setPreviewModalOpen] = useState(false)
  const [selectedNotebook, setSelectedNotebook] = useState<Notebook | null>(null)
  const [previewWords, setPreviewWords] = useState<any[]>([])
  const [loadingPreview, setLoadingPreview] = useState(false)
  const [cloning, setCloning] = useState(false)

  // React Query data
  const { data: exploreData, isLoading: exploreLoading } = useExploreNotebooks({
    keyword: keyword || undefined,
    sort,
    page,
    size: pageSize,
  })

  const { data: favoriteData, isLoading: favoriteLoading } = useFavoriteNotebooks(
    page,
    pageSize
  )

  const { data: recentData, isLoading: recentLoading } = useRecentlyViewedNotebooks(
    page,
    pageSize
  )

  const { toggleLike, cloneNotebook } = useCommunityNotebookMutations()

  // Format số lượt xem kiểu k (ví dụ 1200 -> 1.2k)
  const formatCount = (count?: number) => {
    if (!count) return '0'
    if (count >= 1000) {
      return `${(count / 1000).toFixed(1)}k`
    }
    return count.toString()
  }

  // Xử lý Thích / Bỏ thích
  const handleLike = async (e: React.MouseEvent, notebook: Notebook) => {
    e.stopPropagation()
    try {
      await toggleLike.mutateAsync(notebook.id)
    } catch {
      message.error('Không thể thực hiện thao tác thích')
    }
  }

  // Xem chi tiết sổ tay
  const handleCardClick = async (notebook: Notebook) => {
    setSelectedNotebook(notebook)
    setPreviewModalOpen(true)
    setLoadingPreview(true)
    try {
      const res = await notebookService.viewPublicNotebookDetail(notebook.id)
      if (res.data) setSelectedNotebook(res.data)

      const wordsRes = await notebookService.getWords(notebook.id, { page: 0, size: 20 })
      setPreviewWords(wordsRes.data?.items ?? [])
    } catch {
      message.error('Không thể tải chi tiết sổ tay')
    } finally {
      setLoadingPreview(false)
    }
  }

  // Clone sổ tay về kho cá nhân
  const handleCloneFromPreview = async () => {
    if (!selectedNotebook?.shareCode) {
      message.warning('Sổ tay này chưa có mã chia sẻ công khai!')
      return
    }

    try {
      setCloning(true)
      const res = await cloneNotebook.mutateAsync({
        shareCode: selectedNotebook.shareCode,
      })
      if (res.data) {
        message.success(`Đã nhân bản "${selectedNotebook.name}" vào sổ tay của bạn!`)
        setPreviewModalOpen(false)
        navigate(`/notebooks/${res.data.id}`)
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể tải sổ tay này về kho!'
      message.error(msg)
    } finally {
      setCloning(false)
    }
  }

  // Sort dropdown menu
  const sortMenuItems: MenuProps['items'] = (
    ['all', 'downloads', 'likes', 'views'] as SortOption[]
  ).map((key) => ({
    key,
    label: (
      <div className="flex items-center justify-between w-44 py-1">
        <span>{SORT_LABELS[key]}</span>
        {sort === key && <CheckOutlined className="text-blue-600 text-xs" />}
      </div>
    ),
    onClick: () => {
      setSort(key)
      setPage(0)
    },
  }))

  // Chọn nguồn dữ liệu hiển thị theo tab
  let currentItems: Notebook[] = []
  let totalElements = 0
  let isCurrentLoading = false

  if (activeTab === 'explore') {
    currentItems = exploreData?.items ?? []
    totalElements = exploreData?.totalElements ?? 0
    isCurrentLoading = exploreLoading
  } else if (activeTab === 'favorites') {
    currentItems = favoriteData?.items ?? []
    totalElements = favoriteData?.totalElements ?? 0
    isCurrentLoading = favoriteLoading
  } else {
    currentItems = recentData?.items ?? []
    totalElements = recentData?.totalElements ?? 0
    isCurrentLoading = recentLoading
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between pb-2 border-b border-gray-100">
        <div className="flex items-center gap-3">
          <BackButton to="/notebooks" />
        </div>
      </div>

      {/* Thanh công cụ: Sub-tabs bên trái & Tìm kiếm / Lọc bên phải (giống ảnh 2) */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* 3 Sub-tabs (đã bỏ Premium) */}
        <div className="flex items-center gap-2 border-b md:border-b-0 border-gray-200 pb-2 md:pb-0">
          <button
            onClick={() => {
              setActiveTab('explore')
              setPage(0)
            }}
            className={`px-4 py-2 font-semibold text-sm rounded-xl transition-all cursor-pointer ${
              activeTab === 'explore'
                ? 'bg-blue-50 text-blue-600 border border-blue-200'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
            }`}
          >
            Khám phá sổ tay
          </button>

          <button
            onClick={() => {
              setActiveTab('favorites')
              setPage(0)
            }}
            className={`px-4 py-2 font-semibold text-sm rounded-xl transition-all cursor-pointer ${
              activeTab === 'favorites'
                ? 'bg-blue-50 text-blue-600 border border-blue-200'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
            }`}
          >
            Yêu thích
          </button>

          <button
            onClick={() => {
              setActiveTab('recent')
              setPage(0)
            }}
            className={`px-4 py-2 font-semibold text-sm rounded-xl transition-all cursor-pointer ${
              activeTab === 'recent'
                ? 'bg-blue-50 text-blue-600 border border-blue-200'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
            }`}
          >
            Đã xem
          </button>
        </div>

        {/* Ô tìm kiếm & Dropdown sắp xếp */}
        <div className="flex items-center gap-2">
          {activeTab === 'explore' && (
            <>
              <Input
                placeholder="Tìm kiếm sổ tay..."
                prefix={<SearchOutlined className="text-gray-400" />}
                value={keyword}
                onChange={(e) => {
                  setKeyword(e.target.value)
                  setPage(0)
                }}
                allowClear
                className="w-64 rounded-xl"
              />

              <Dropdown menu={{ items: sortMenuItems }} trigger={['click']}>
                <Button
                  icon={<FilterOutlined className="text-blue-600" />}
                  className="rounded-xl font-medium"
                >
                  {SORT_LABELS[sort]}
                </Button>
              </Dropdown>
            </>
          )}
        </div>
      </div>

      {/* Lưới danh sách Sổ tay Cộng đồng (Card Grid 3 cột như ảnh 2) */}
      {isCurrentLoading ? (
        <div className="flex items-center justify-center min-h-[360px]">
          <Spin size="large" />
        </div>
      ) : currentItems.length === 0 ? (
        <div className="py-20 text-center bg-white rounded-3xl border border-suna shadow-xs">
          <Empty
            description={
              <span className="text-gray-500 text-sm">
                Không tìm thấy sổ tay nào phù hợp!
              </span>
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {currentItems.map((item) => (
            <div
              key={item.id}
              onClick={() => handleCardClick(item)}
              className="group bg-white rounded-2xl border border-gray-200/90 p-4 shadow-2xs hover:shadow-md hover:border-blue-300 transition-all cursor-pointer flex flex-col justify-between select-none"
            >
              {/* Dòng trên: Tên sổ tay & Nút Thích / Tải */}
              <div className="flex items-start justify-between gap-2 mb-3">
                <div className="flex items-center gap-1.5 flex-1 min-w-0">
                  <h3 className="text-sm font-bold text-gray-900 truncate group-hover:text-blue-600 transition-colors">
                    {item.name}
                  </h3>
                  {item.isOwner && (
                    <Tag color="purple" className="m-0 text-[10px] px-1.5 py-0 rounded-md font-semibold shrink-0">
                      Của bạn
                    </Tag>
                  )}
                  {!item.isOwner && item.isImported && (
                    <Tag color="green" className="m-0 text-[10px] px-1.5 py-0 rounded-md font-semibold shrink-0 flex items-center gap-0.5">
                      <CheckOutlined /> Đã lưu
                    </Tag>
                  )}
                </div>

                <div className="flex items-center gap-2.5 text-xs text-gray-500 shrink-0">
                  {/* Nút Like tim */}
                  <span
                    onClick={(e) => handleLike(e, item)}
                    className="flex items-center gap-1 hover:text-red-500 cursor-pointer transition-colors"
                  >
                    {item.isLiked ? (
                      <HeartFilled className="text-red-500 text-sm" />
                    ) : (
                      <HeartOutlined className="text-sm" />
                    )}
                    <span>{item.likeCount || 0}</span>
                  </span>

                  {/* Lượt tải */}
                  <span className="flex items-center gap-0.5 text-gray-400">
                    <DownloadOutlined className="text-xs" />
                    <span>{item.downloadCount || 0}</span>
                  </span>
                </div>
              </div>

              {/* Dòng dưới: Số từ + Lượt xem & Avatar tác giả */}
              <div className="flex items-center justify-between text-xs text-gray-500 pt-2 border-t border-gray-100/80">
                <div className="flex items-center gap-1.5 font-medium text-gray-600">
                  <span>{item.wordCount || 0} từ</span>
                  <span>•</span>
                  <span>{formatCount(item.viewCount)} lượt xem</span>
                </div>

                <div className="flex items-center gap-1.5 text-gray-700">
                  <Avatar
                    size={22}
                    src={item.authorAvatar}
                    icon={<UserOutlined />}
                    className="bg-amber-400 text-white shrink-0"
                  />
                  <span className="font-medium text-[11px] truncate max-w-[100px]">
                    {item.authorName || 'Ẩn danh'}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Phân trang */}
      {totalElements > pageSize && (
        <div className="flex justify-center pt-4">
          <Pagination
            current={page + 1}
            pageSize={pageSize}
            total={totalElements}
            onChange={(p) => setPage(p - 1)}
            showSizeChanger={false}
          />
        </div>
      )}

      {/* Modal xem trước chi tiết sổ tay cộng đồng & Nút Clone */}
      <Modal
        title={
          <div className="flex items-center gap-2">
            <BookOutlined className="text-blue-600 text-lg" />
            <span>Chi tiết sổ tay cộng đồng</span>
          </div>
        }
        open={previewModalOpen}
        onCancel={() => setPreviewModalOpen(false)}
        width={680}
        footer={[
          <Button key="close" onClick={() => setPreviewModalOpen(false)}>
            Đóng
          </Button>,
          selectedNotebook?.isOwner ? (
            <Button
              key="open-own"
              type="primary"
              onClick={() => {
                setPreviewModalOpen(false)
                navigate(`/notebooks/${selectedNotebook.id}`)
              }}
              className="bg-purple-600 hover:bg-purple-700 font-semibold"
            >
              Mở sổ tay của bạn
            </Button>
          ) : selectedNotebook?.isImported ? (
            <Button
              key="open-imported"
              type="primary"
              icon={<CheckOutlined />}
              onClick={() => {
                setPreviewModalOpen(false)
                navigate(`/notebooks/${selectedNotebook.userNotebookId || selectedNotebook.id}`)
              }}
              className="bg-emerald-600 hover:bg-emerald-700 font-semibold"
            >
              Đã có trong kho - Mở ngay
            </Button>
          ) : (
            <Button
              key="clone"
              type="primary"
              icon={<DownloadOutlined />}
              loading={cloning}
              onClick={handleCloneFromPreview}
              className="bg-blue-600 hover:bg-blue-700 font-semibold"
            >
              Tải về sổ tay của tôi ({selectedNotebook?.wordCount || 0} từ)
            </Button>
          ),
        ]}
        destroyOnClose
      >
        {selectedNotebook && (
          <div className="space-y-4 my-2">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-gray-900">
                  {selectedNotebook.name}
                </h2>
                {selectedNotebook.jlptLevel && (
                  <Tag color="blue">{selectedNotebook.jlptLevel}</Tag>
                )}
                {selectedNotebook.isOwner && (
                  <Tag color="purple">Sổ tay của bạn</Tag>
                )}
                {!selectedNotebook.isOwner && selectedNotebook.isImported && (
                  <Tag color="green">
                    <CheckOutlined /> Đã có trong kho của bạn
                  </Tag>
                )}
              </div>
              <div className="text-xs text-gray-500">
                Tác giả:{' '}
                <span className="font-semibold text-gray-700">
                  {selectedNotebook.authorName || 'Ẩn danh'}
                </span>{' '}
                • {selectedNotebook.wordCount} từ vựng •{' '}
                {selectedNotebook.downloadCount} lượt tải • {selectedNotebook.likeCount} lượt thích
              </div>
              {selectedNotebook.description && (
                <div className="text-xs text-gray-600 italic bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                  "{selectedNotebook.description}"
                </div>
              )}
            </div>

            {/* Danh sách từ xem trước */}
            <div>
              <div className="text-xs font-bold text-gray-700 uppercase mb-2">
                Một số từ vựng tiêu biểu:
              </div>
              {loadingPreview ? (
                <div className="py-8 text-center">
                  <Spin />
                </div>
              ) : previewWords.length === 0 ? (
                <div className="text-xs text-gray-400 py-4 text-center">
                  Sổ tay này chưa có từ vựng nào.
                </div>
              ) : (
                <div className="max-h-60 overflow-y-auto space-y-2 border border-gray-100 rounded-xl p-3 bg-slate-50/50">
                  {previewWords.map((w, idx) => (
                    <div
                      key={idx}
                      className="flex items-center justify-between text-xs bg-white p-2 rounded-lg border border-gray-100"
                    >
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-gray-900">{w.word}</span>
                        {w.reading && (
                          <span className="text-gray-500">({w.reading})</span>
                        )}
                        {(w.sinoVietnamese || w.hanViet) && (
                          <span className="text-emerald-600 font-medium">
                            [{w.sinoVietnamese || w.hanViet}]
                          </span>
                        )}
                      </div>
                      <div className="text-gray-700 font-medium truncate max-w-[240px]">
                        {w.meaning}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
export default NotebookExplorePage
