import React, { useState } from 'react'
import { Button, Input } from 'antd'
import { MessageSquare, Send, LogIn } from 'lucide-react'
import { useAuthStore } from '@/stores/useAuthStore'
import { useAuthModalStore } from '@/stores/useAuthModalStore'
import { cn } from '@/utils/cn'

/** BE giới hạn `@Size(max = 5000)` cho nội dung bình luận. */
const MAX_CONTENT_LENGTH = 5000

interface CommentComposerProps {
  /** Nội dung đang nhập (dùng cho reply nội tuyến). */
  value?: string
  onChange?: (value: string) => void
  /** Gọi khi submit thành công. */
  onSubmit: (content: string) => void | Promise<void>
  /** Đang gửi → disable nút. */
  isSubmitting?: boolean
  placeholder?: string
  submitLabel?: string
  /** Ẩn nút đăng nhập (dùng khi đã biết chắc user đã đăng nhập). */
  compact?: boolean
  autoFocus?: boolean
  className?: string
}

/**
 * CommentComposer — ô nhập bình luận / reply.
 *
 * Khi chưa đăng nhập: hiện nút "Đăng nhập để bình luận" (mở modal login
 * của app qua `useAuthModalStore`).
 */
export const CommentComposer: React.FC<CommentComposerProps> = ({
  value,
  onChange,
  onSubmit,
  isSubmitting = false,
  placeholder = 'Chia sẻ suy nghĩ của bạn...',
  submitLabel = 'Gửi bình luận',
  compact = false,
  autoFocus = false,
  className,
}) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)

  // Nội dung nội bộ khi component không được điều khiển từ bên ngoài.
  const [internalValue, setInternalValue] = useState('')
  const isControlled = value !== undefined
  const currentValue = isControlled ? value : internalValue

  const handleChange = (next: string) => {
    if (!isControlled) setInternalValue(next)
    onChange?.(next)
  }

  const handleSubmit = async () => {
    const content = currentValue.trim()
    if (!content || isSubmitting) return
    try {
      await onSubmit(content)
    } catch {
      // Lỗi đã được xử lý + hiển thị toast ở lớp gọi.
      // Giữ nguyên nội dung để user không phải gõ lại.
      return
    }
    if (!isControlled) setInternalValue('')
  }

  // ------------------------------------------------------------------------
  // Chưa đăng nhập
  // ------------------------------------------------------------------------
  if (!isAuthenticated) {
    return (
      <div
        className={cn(
          'flex flex-col sm:flex-row sm:items-center gap-3 rounded-md border border-dashed border-indigo-200 bg-indigo-50/50 p-4',
          className,
        )}
      >
        <span className="w-10 h-10 rounded-md bg-white text-indigo-600 border border-indigo-100 flex items-center justify-center shrink-0">
          <MessageSquare className="w-5 h-5" />
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-gray-900">
            Đăng nhập để tham gia bình luận
          </p>
          <p className="text-xs text-gray-500 mt-0.5">
            Chia sẻ kiến thức, hỏi đáp thắc mắc với người học khác.
          </p>
        </div>
        <Button
          type="primary"
          icon={<LogIn className="w-4 h-4" />}
          onClick={() =>
            openLogin(
              typeof window !== 'undefined'
                ? window.location.pathname + window.location.search
                : null,
            )
          }
          className="!rounded-md !font-semibold shrink-0"
        >
          Đăng nhập để bình luận
        </Button>
      </div>
    )
  }

  // ------------------------------------------------------------------------
  // Đã đăng nhập
  // ------------------------------------------------------------------------
  const trimmed = currentValue.trim()
  const isTooLong = currentValue.length > MAX_CONTENT_LENGTH

  return (
    <div className={cn('space-y-2', className)}>
      <Input.TextArea
        value={currentValue}
        onChange={(e) => handleChange(e.target.value)}
        placeholder={placeholder}
        autoFocus={autoFocus}
        autoSize={{ minRows: compact ? 2 : 3, maxRows: 8 }}
        maxLength={MAX_CONTENT_LENGTH}
        showCount
        disabled={isSubmitting}
        className="!rounded-md"
      />

      {isTooLong && (
        <p className="text-[11px] text-rose-500 font-medium">
          Nội dung vượt quá {MAX_CONTENT_LENGTH} ký tự.
        </p>
      )}

      <div className="flex items-center justify-between gap-2 mt-3">
        <p className="text-[11px] text-gray-400">
          Bình luận sẽ hiển thị công khai cho mọi người.
        </p>
        <Button
          type="primary"
          icon={<Send className="w-3.5 h-3.5" />}
          onClick={handleSubmit}
          loading={isSubmitting}
          disabled={!trimmed || isTooLong}
          className="!rounded-md !font-semibold shrink-0"
        >
          {submitLabel}
        </Button>
      </div>
    </div>
  )
}
