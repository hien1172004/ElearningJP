import React, { useState } from 'react'
import { Button as AntdButton, Modal, Popconfirm } from 'antd'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Copy, FileWarning, RefreshCw, X } from 'lucide-react'
import { useConcurrentEditStore } from '@/stores/useConcurrentEditStore'
import { isRichText, type UnsavedEntry } from '@/lib/unsavedContent'
import { sanitizeJapaneseHtml } from '@/components/editor/utils/sanitize'

const toPlainText = (html: string) =>
  new DOMParser().parseFromString(html, 'text/html').body.textContent ?? ''

/** Chép 1 đoạn; đoạn HTML chép cả dạng rich text để dán vào trình soạn thảo giữ định dạng (ruby, in đậm…). */
async function copyEntry(value: string) {
  try {
    if (isRichText(value) && typeof ClipboardItem !== 'undefined' && navigator.clipboard.write) {
      const html = sanitizeJapaneseHtml(value)
      await navigator.clipboard.write([
        new ClipboardItem({
          'text/html': new Blob([html], { type: 'text/html' }),
          'text/plain': new Blob([toPlainText(html)], { type: 'text/plain' }),
        }),
      ])
    } else {
      await navigator.clipboard.writeText(value)
    }
    toast.success('Đã chép')
  } catch {
    toast.error('Không chép được — hãy bôi đen đoạn chữ rồi chép thủ công')
  }
}

const EntryView: React.FC<{ entry: UnsavedEntry }> = ({ entry }) => (
  <li className="rounded-lg border border-gray-200 bg-gray-50 p-2.5">
    <div className="mb-1 flex items-center justify-between gap-2">
      <span className="text-xs font-semibold text-gray-600">{entry.label}</span>
      <AntdButton size="small" type="text" icon={<Copy className="h-3.5 w-3.5" />} onClick={() => copyEntry(entry.value)}>
        Chép
      </AntdButton>
    </div>
    {isRichText(entry.value) ? (
      <div
        className="text-sm text-gray-900 break-words"
        // Nội dung do chính người dùng nhập, vẫn sanitize trước khi hiển thị
        dangerouslySetInnerHTML={{ __html: sanitizeJapaneseHtml(entry.value) }}
      />
    ) : (
      <p className="whitespace-pre-wrap break-words text-sm text-gray-900">{entry.value}</p>
    )}
  </li>
)

/**
 * Xử lý người lưu sau khi 2 người cùng sửa (BE 409 CONCURRENT_MODIFICATION), gắn 1 lần ở gốc ứng dụng:
 * - Modal chỉ có nút "Tải bản mới nhất" (không hiện lỗi 409, không có lựa chọn ghi đè).
 * - Bấm nút: tải lại dữ liệu đang mở, form sửa đang mở tự đóng ({@code useOnConcurrentReload}),
 *   nội dung vừa gửi lưu hiện ở panel "Bản chưa lưu của bạn" để chép lại từng đoạn rồi lưu như bình thường.
 */
export const ConcurrentEditModal: React.FC = () => {
  const qc = useQueryClient()
  const conflictOpen = useConcurrentEditStore((s) => s.conflictOpen)
  const pendingCount = useConcurrentEditStore((s) => s.pending.length)
  const unsaved = useConcurrentEditStore((s) => s.unsaved)
  const reloadLatest = useConcurrentEditStore((s) => s.reloadLatest)
  const dismissUnsaved = useConcurrentEditStore((s) => s.dismissUnsaved)
  const [reloading, setReloading] = useState(false)

  const handleReload = async () => {
    setReloading(true)
    reloadLatest()
    try {
      // Trang soạn dùng nhiều query (khóa, bài, section, item, quiz…) → làm mới mọi query đang mở
      await qc.invalidateQueries()
    } finally {
      setReloading(false)
    }
  }

  return (
    <>
      <Modal
        open={conflictOpen}
        closable={false}
        maskClosable={false}
        keyboard={false}
        centered
        title={
          <div className="flex items-center gap-2 font-bold text-gray-900">
            <RefreshCw className="h-5 w-5 text-indigo-600" />
            Nội dung vừa được người khác cập nhật
          </div>
        }
        footer={
          <AntdButton
            type="primary"
            loading={reloading}
            icon={<RefreshCw className="h-4 w-4" />}
            onClick={handleReload}
          >
            Tải bản mới nhất
          </AntdButton>
        }
      >
        <p className="text-sm text-gray-700">
          Có người đã lưu nội dung này trước bạn nên thay đổi của bạn chưa được lưu. Hãy tải bản mới nhất rồi
          sửa tiếp.
        </p>
        {pendingCount > 0 && (
          <p className="mt-2 text-sm text-gray-700">
            Phần bạn vừa nhập sẽ được giữ ở mục <strong>“Bản chưa lưu của bạn”</strong> để chép lại.
          </p>
        )}
      </Modal>

      {unsaved.length > 0 && (
        <aside
          aria-label="Bản chưa lưu của bạn"
          className="fixed bottom-4 left-4 right-4 z-[999] flex max-h-[60vh] flex-col overflow-hidden rounded-xl border border-amber-300 bg-surface shadow-lg sm:right-auto sm:w-[26rem]"
        >
          <div className="flex items-start gap-2 border-b border-amber-200 bg-amber-50 p-3">
            <FileWarning className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-bold text-gray-900">Bản chưa lưu của bạn</p>
              <p className="text-xs text-gray-600">
                Mở lại mục cần sửa (đã là bản mới nhất), chép từng đoạn dưới đây vào rồi lưu như bình thường.
              </p>
            </div>
            <Popconfirm
              title="Bỏ bản chưa lưu?"
              description="Các đoạn này sẽ mất, không khôi phục được."
              okText="Bỏ"
              cancelText="Giữ lại"
              onConfirm={dismissUnsaved}
            >
              <AntdButton size="small" type="text" aria-label="Bỏ bản chưa lưu" icon={<X className="h-4 w-4" />} />
            </Popconfirm>
          </div>
          <ul className="space-y-2 overflow-y-auto p-3">
            {unsaved.map((entry, i) => (
              <EntryView key={i} entry={entry} />
            ))}
          </ul>
        </aside>
      )}
    </>
  )
}
