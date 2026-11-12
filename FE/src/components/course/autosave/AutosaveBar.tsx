import React from 'react'
import { Button as AntdButton } from 'antd'
import { CloudCheck, CloudOff, History, Loader2 } from 'lucide-react'
import type { FormAutosave } from '@/hooks/useFormAutosave'
import { formatRelativeTime } from '@/utils/format'

/**
 * Đầu form soạn bài: mời khôi phục bản tự lưu (nếu có) hoặc hiện trạng thái tự lưu.
 * Bản ghi đã bị người khác sửa → nút "Mở để chép lại" (panel "Bản chưa lưu của bạn"), không điền đè.
 */
export const AutosaveBar: React.FC<{ autosave: FormAutosave }> = ({ autosave }) => {
  const { offer, restore, discard, status, savedAt } = autosave
  if (offer) {
    return (
      <div className="mb-3 flex flex-wrap items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
        <History className="h-4 w-4 shrink-0" />
        <span className="min-w-0 flex-1">
          Bạn có bản chưa lưu {offer.updatedAt ? formatRelativeTime(offer.updatedAt) : ''}.
          {offer.stale && ' Nội dung này đã được người khác sửa sau đó — hãy chép lại từng đoạn cần giữ.'}
        </span>
        <AntdButton size="small" type="primary" onClick={restore}>
          {offer.stale ? 'Mở để chép lại' : 'Khôi phục'}
        </AntdButton>
        <AntdButton size="small" onClick={discard}>
          Bỏ
        </AntdButton>
      </div>
    )
  }
  if (status === 'idle') return null
  return (
    <p className="mb-2 flex items-center gap-1.5 text-xs text-slate-500" aria-live="polite">
      {status === 'saving' && (
        <>
          <Loader2 className="h-3.5 w-3.5 animate-spin" /> Đang tự lưu…
        </>
      )}
      {status === 'saved' && (
        <>
          <CloudCheck className="h-3.5 w-3.5 text-emerald-600" /> Đã tự lưu bản nháp của bạn
          {savedAt && ` lúc ${savedAt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`}
          {' '}(chỉ bạn thấy)
        </>
      )}
      {status === 'error' && (
        <>
          <CloudOff className="h-3.5 w-3.5 text-rose-600" /> Chưa tự lưu được — phần đang gõ vẫn còn trên màn hình
        </>
      )}
    </p>
  )
}

/**
 * Bọc nội dung form: gõ / đổi bất kỳ ô nào bên trong → đánh dấu đã sửa để tự lưu.
 * Vùng có {@code data-autosave-ignore} (vd ô tìm từ điển) không tính là sửa nội dung.
 */
export const AutosaveScope: React.FC<{ autosave: FormAutosave; children: React.ReactNode }> = ({ autosave, children }) => {
  const onEdit = (e: React.SyntheticEvent) => {
    if (e.target instanceof Element && e.target.closest('[data-autosave-ignore]')) return
    autosave.markDirty()
  }
  return (
    <div onInputCapture={onEdit} onChangeCapture={onEdit}>
      {children}
    </div>
  )
}
