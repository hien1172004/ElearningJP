import React, { useState, useEffect, useCallback, useRef } from 'react'
import { Modal, Button, Spin, Select, message } from 'antd'
import {
  ReloadOutlined,
  LeftOutlined,
  RightOutlined,
  FilePdfOutlined,
  DownloadOutlined,
} from '@ant-design/icons'
import { notebookService } from '@/services/notebook.service'

interface KanjiPdfPreviewModalProps {
  open: boolean
  notebookId: number
  notebookName: string
  onClose: () => void
}

export const KanjiPdfPreviewModal: React.FC<KanjiPdfPreviewModalProps> = ({
  open,
  notebookId,
  notebookName,
  onClose,
}) => {
  const [currentPage, setCurrentPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [imageLoading, setImageLoading] = useState(true)
  const [hasError, setHasError] = useState(false)
  const [pageCache, setPageCache] = useState<Record<number, string>>({})
  const [downloading, setDownloading] = useState(false)

  // Tải ảnh cho một trang cụ thể
  const loadPage = useCallback(
    async (pageNum: number) => {
      if (!notebookId) return

      // Nếu trang đã có trong cache, chuyển trang tức thì
      if (pageCache[pageNum]) {
        setCurrentPage(pageNum)
        setImageLoading(false)
        setHasError(false)
        return
      }

      setImageLoading(true)
      setHasError(false)
      setCurrentPage(pageNum)

      try {
        const url = await notebookService.fetchKanjiPdfPreviewUrl(notebookId, pageNum)
        setPageCache((prev) => ({
          ...prev,
          [pageNum]: url,
        }))
      } catch (err) {
        console.error('Failed to load PDF preview page:', err)
        setHasError(true)
      } finally {
        setImageLoading(false)
      }
    },
    [notebookId, pageCache]
  )

  // Khởi tạo khi mở modal: Lấy tổng số trang và tải trang 1
  useEffect(() => {
    if (!open || !notebookId) return

    let isMounted = true

    const initPreview = async () => {
      setImageLoading(true)
      setHasError(false)
      setCurrentPage(1)

      try {
        // 1. Lấy thông tin số trang thực tế của PDF
        const metaRes = await notebookService.getKanjiPdfMeta(notebookId).catch(() => null)
        const pages = metaRes?.data?.totalPages || 1
        if (isMounted) {
          setTotalPages(pages)
        }

        // 2. Tải ảnh trang 1
        const url = await notebookService.fetchKanjiPdfPreviewUrl(notebookId, 1)
        if (isMounted) {
          setPageCache({ 1: url })
        }
      } catch (err) {
        console.error('Error initializing PDF preview:', err)
        if (isMounted) {
          setHasError(true)
        }
      } finally {
        if (isMounted) {
          setImageLoading(false)
        }
      }
    }

    initPreview()

    return () => {
      isMounted = false
    }
  }, [open, notebookId])

  const pageCacheRef = useRef<Record<number, string>>({})

  useEffect(() => {
    pageCacheRef.current = pageCache
  }, [pageCache])

  // Giải phóng toàn bộ Blob URL khi component unmount
  useEffect(() => {
    return () => {
      Object.values(pageCacheRef.current).forEach((url) => {
        window.URL.revokeObjectURL(url)
      })
    }
  }, [])

  const handleModalClose = () => {
    Object.values(pageCacheRef.current).forEach((url) => {
      window.URL.revokeObjectURL(url)
    })
    setPageCache({})
    setCurrentPage(1)
    setTotalPages(1)
    setHasError(false)
    onClose()
  }

  // Hỗ trợ phím mũi tên Trái / Phải để lật trang
  useEffect(() => {
    if (!open) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft' && currentPage > 1 && !imageLoading) {
        loadPage(currentPage - 1)
      } else if (e.key === 'ArrowRight' && currentPage < totalPages && !imageLoading) {
        loadPage(currentPage + 1)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [open, currentPage, totalPages, imageLoading, loadPage])

  const handleDownload = async () => {
    try {
      setDownloading(true)
      await notebookService.downloadKanjiPdf(notebookId, notebookName)
      message.success('Đã tải xuống vở luyện viết Kanji!')
    } catch {
      message.error('Không thể tải xuống tệp PDF!')
    } finally {
      setDownloading(false)
    }
  }

  const currentImageUrl = pageCache[currentPage]

  return (
    <Modal
      title={
        <div className="flex flex-wrap items-center justify-between gap-2 pr-6">
          <div className="flex items-center gap-2">
            <FilePdfOutlined className="text-red-500 text-lg" />
            <span className="text-base font-bold text-gray-900">
              Xem trước PDF: {notebookName}
            </span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-50 text-blue-600 border border-blue-200">
              Trang {currentPage} / {totalPages}
            </span>
          </div>

          {/* Thanh chuyển trang trên Header */}
          {totalPages > 1 && (
            <div className="flex items-center gap-1.5 bg-gray-100 p-1 rounded-xl">
              <Button
                size="small"
                icon={<LeftOutlined />}
                disabled={currentPage <= 1 || imageLoading}
                onClick={() => loadPage(currentPage - 1)}
                className="rounded-lg h-7 px-2 border-none shadow-none text-xs font-medium hover:bg-white"
              >
                Trước
              </Button>

              <Select
                size="small"
                value={currentPage}
                onChange={(val) => loadPage(val)}
                disabled={imageLoading}
                className="w-24 text-xs font-medium"
                options={Array.from({ length: totalPages }, (_, i) => ({
                  label: `Trang ${i + 1}`,
                  value: i + 1,
                }))}
              />

              <Button
                size="small"
                icon={<RightOutlined />}
                disabled={currentPage >= totalPages || imageLoading}
                onClick={() => loadPage(currentPage + 1)}
                className="rounded-lg h-7 px-2 border-none shadow-none text-xs font-medium hover:bg-white"
              >
                Sau
              </Button>
            </div>
          )}
        </div>
      }
      open={open}
      onCancel={handleModalClose}
      width={1000}
      centered
      className="mazii-modal"
      footer={
        <div className="flex items-center justify-between pt-2">
          <div className="text-xs text-gray-500">
            {totalPages > 1 ? (
              <span>
                Dùng phím mũi tên <kbd className="px-1 py-0.5 bg-gray-100 border rounded text-[11px]">←</kbd>{' '}
                <kbd className="px-1 py-0.5 bg-gray-100 border rounded text-[11px]">→</kbd> để lật trang nhanh
              </span>
            ) : (
              <span>Vở luyện viết Kanji khổ dọc A4</span>
            )}
          </div>

          <div className="flex items-center gap-3">
            <Button
              size="large"
              onClick={handleModalClose}
              className="h-10 px-6 rounded-xl bg-gray-100 hover:bg-gray-200 border-none text-gray-700 font-medium"
            >
              Đóng
            </Button>
            <Button
              type="primary"
              size="large"
              icon={<DownloadOutlined />}
              loading={downloading}
              onClick={handleDownload}
              className="h-10 px-6 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm"
            >
              Tải xuống toàn bộ ({totalPages} trang)
            </Button>
          </div>
        </div>
      }
      destroyOnClose
    >
      <div className="my-2">
        <div className="relative min-h-[500px] max-h-[72vh] overflow-y-auto overflow-x-hidden bg-gray-50 rounded-xl p-4 flex flex-col items-center border border-gray-200">
          {imageLoading && (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-gray-50/80 z-20 rounded-xl">
              <Spin size="large" />
              <div className="text-xs text-gray-600 font-medium mt-3">
                Đang kết xuất xem trước trang {currentPage}/{totalPages}...
              </div>
            </div>
          )}

          {hasError ? (
            <div className="text-center py-12 space-y-3">
              <div className="text-gray-500 text-sm">
                Không thể tải trang xem trước (sổ tay có thể chưa có từ vựng chữ Hán nào).
              </div>
              <Button
                size="middle"
                icon={<ReloadOutlined />}
                onClick={() => loadPage(currentPage)}
                className="rounded-xl"
              >
                Thử lại
              </Button>
            </div>
          ) : (
            currentImageUrl && (
              <>
                <img
                  src={currentImageUrl}
                  alt={`Kanji Practice Preview Page ${currentPage}`}
                  className="w-full max-w-[920px] h-auto rounded-lg shadow-sm bg-white border border-gray-200 block"
                />

                {/* Thanh chuyển trang dính ở đáy khung xem */}
                {totalPages > 1 && (
                  <div className="sticky bottom-2 mt-4 bg-white/95 backdrop-blur-md px-4 py-2 rounded-xl shadow-lg border border-gray-200 flex items-center gap-3 z-10">
                    <Button
                      size="middle"
                      icon={<LeftOutlined />}
                      disabled={currentPage <= 1 || imageLoading}
                      onClick={() => loadPage(currentPage - 1)}
                      className="rounded-lg h-9 font-medium"
                    >
                      Trang trước
                    </Button>
                    <span className="text-sm font-semibold text-gray-800 px-2">
                      Trang {currentPage} / {totalPages}
                    </span>
                    <Button
                      size="middle"
                      icon={<RightOutlined />}
                      disabled={currentPage >= totalPages || imageLoading}
                      onClick={() => loadPage(currentPage + 1)}
                      className="rounded-lg h-9 font-medium"
                    >
                      Trang sau
                    </Button>
                  </div>
                )}
              </>
            )
          )}
        </div>
      </div>
    </Modal>
  )
}

export default KanjiPdfPreviewModal
