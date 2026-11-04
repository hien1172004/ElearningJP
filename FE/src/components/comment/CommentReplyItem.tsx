import React, { useEffect, useRef, useState } from 'react'
import { Avatar, Tooltip } from 'antd'
import { ThumbsUp, ThumbsDown, Trash2, CornerUpLeft, Flag } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/utils/cn'
import { formatRelativeTime } from '@/utils/format'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { FOCUS_HIGHLIGHT_MS } from '@/utils/commentFocus'
import { useAuthStore } from '@/stores/useAuthStore'
import { useAuthModalStore } from '@/stores/useAuthModalStore'
import {
  useDeleteReply,
  useToggleReplyReaction,
} from '@/hooks/useComments'
import { ReportCommentModal } from './ReportCommentModal'
import type {
  CommentReplyItem as CommentReplyItemType,
  CommentTargetType,
} from '@/types'

interface CommentReplyItemProps {
  reply: CommentReplyItemType
  targetType: CommentTargetType
  targetId: number
  /** Độ sâu lồng nhau (0 = reply trực tiếp của comment, 1 = reply của reply). */
  depth?: number
  /** Xoá reply thành công → gọi callback (để refetch replies + comment). */
  onDeleted?: () => void
  /** Mở ô nhập reply lồng nhau (parentReplyId = reply này). */
  onReply?: (reply: CommentReplyItemType) => void
  /** Reply được mở từ thông báo → cuộn tới + highlight. */
  focused?: boolean
  onFocusDone?: () => void
}

/** Số cấp lồng nhau tối đa được hiển thị (reply cấp 3 sẽ phẳng về cấp 2). */
const MAX_VISUAL_DEPTH = 1

/**
 * CommentReplyItem — 1 dòng reply trong cây trả lời.
 *
 * Hiển thị: avatar nhỏ, tên, thời gian tương đối, nội dung, like/dislike,
 * nút "Trả lời", nút xoá (chủ sở hữu / admin) và nút báo cáo vi phạm (người khác).
 */
export const CommentReplyItem: React.FC<CommentReplyItemProps> = ({
  reply,
  targetType,
  targetId,
  depth = 0,
  onDeleted,
  onReply,
  focused = false,
  onFocusDone,
}) => {
  const rootRef = useRef<HTMLDivElement>(null)
  const [isReportOpen, setIsReportOpen] = useState(false)

  useEffect(() => {
    if (!focused) return
    rootRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    const timer = window.setTimeout(() => onFocusDone?.(), FOCUS_HIGHLIGHT_MS)
    return () => window.clearTimeout(timer)
  }, [focused, onFocusDone])

  const likeMutation = useToggleReplyReaction('like')
  const dislikeMutation = useToggleReplyReaction('dislike')
  const deleteMutation = useDeleteReply(targetType, targetId)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)

  const isReactionLocked =
    likeMutation.isPending ||
    dislikeMutation.isPending ||
    deleteMutation.isPending

  const handleReaction = (kind: 'like' | 'dislike') => {
    // Khách chưa đăng nhập: mở modal đăng nhập thay vì gọi API (tránh 401).
    if (!isAuthenticated) {
      openLogin(window.location.pathname + window.location.search)
      return
    }
    const mutation = kind === 'like' ? likeMutation : dislikeMutation
    mutation.mutate({ replyId: reply.id, commentId: reply.commentId })
  }

  const handleReport = () => {
    if (!isAuthenticated) {
      openLogin(window.location.pathname + window.location.search)
      return
    }
    setIsReportOpen(true)
  }

  const handleDelete = async () => {
    const confirmed = await confirmDialog({
      title: 'Xoá trả lời này?',
      content: 'Thao tác này không thể hoàn tác.',
      okText: 'Xoá',
      cancelText: 'Huỷ',
      danger: true,
    })
    if (!confirmed) return

    try {
      await deleteMutation.mutateAsync({
        replyId: reply.id,
        commentId: reply.commentId,
      })
      toast.success('Đã xoá trả lời')
      onDeleted?.()
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ?? 'Không xoá được trả lời. Vui lòng thử lại.',
      )
    }
  }

  const visualDepth = Math.min(depth, MAX_VISUAL_DEPTH)

  return (
    <div
      ref={rootRef}
      className={cn(
        'flex gap-2.5 rounded-md transition-colors duration-700',
        focused && 'bg-amber-50 ring-2 ring-amber-200 ring-offset-2',
        visualDepth > 0 &&
          'ml-5 sm:ml-8 border-l-2 border-gray-100 pl-3',
      )}
    >
      <Avatar
        size={visualDepth > 0 ? 24 : 28}
        src={reply.user?.avatarUrl ?? undefined}
        className="shrink-0 bg-indigo-100 text-indigo-600 text-[11px]"
      >
        {reply.user?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
      </Avatar>

      <div className="flex-1 min-w-0">
        {/* Header */}
        <div className="flex items-baseline gap-2 flex-wrap">
          <span className="text-xs font-bold text-gray-900">
            {reply.user?.fullName ?? `User #${reply.user?.id ?? '?'}`}
          </span>
          <span className="text-[10px] text-gray-400">
            {formatRelativeTime(reply.createdAt)}
          </span>
        </div>

        {/* Nội dung */}
        <p className="text-sm text-gray-700 mt-0.5 whitespace-pre-wrap break-words">
          {reply.content}
        </p>

        {/* Hành động */}
        <div className="flex items-center gap-1 mt-1.5">
          <Tooltip title="Hữu ích">
            <button
              type="button"
              disabled={isReactionLocked}
              onClick={() => handleReaction('like')}
              className={cn(
                'inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed',
                reply.liked
                  ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                  : 'text-gray-500 hover:bg-gray-100 hover:text-gray-700',
              )}
            >
              <ThumbsUp className="w-3.5 h-3.5" />
              {reply.likeCount > 0 && <span>{reply.likeCount}</span>}
            </button>
          </Tooltip>

          <Tooltip title="Không hữu ích">
            <button
              type="button"
              disabled={isReactionLocked}
              onClick={() => handleReaction('dislike')}
              className={cn(
                'inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed',
                reply.disliked
                  ? 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                  : 'text-gray-500 hover:bg-gray-100 hover:text-gray-700',
              )}
            >
              <ThumbsDown className="w-3.5 h-3.5" />
              {reply.dislikeCount > 0 && <span>{reply.dislikeCount}</span>}
            </button>
          </Tooltip>

          {onReply && (
            <button
              type="button"
              onClick={() => onReply(reply)}
              className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-500 hover:bg-gray-100 hover:text-gray-700 transition"
            >
              <CornerUpLeft className="w-3.5 h-3.5" />
              Trả lời
            </button>
          )}

          {(reply.deletable ?? reply.owner) && (
            <Tooltip title="Xoá trả lời">
              <button
                type="button"
                onClick={handleDelete}
                className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-400 hover:bg-rose-50 hover:text-rose-600 transition"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </Tooltip>
          )}

          {!reply.owner && (
            <Tooltip title="Báo cáo vi phạm">
              <button
                type="button"
                onClick={handleReport}
                aria-label="Báo cáo vi phạm"
                className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold text-gray-400 hover:bg-amber-50 hover:text-amber-700 transition"
              >
                <Flag className="w-3.5 h-3.5" />
              </button>
            </Tooltip>
          )}
        </div>
      </div>

      <ReportCommentModal
        open={isReportOpen}
        kind="reply"
        targetId={reply.id}
        onClose={() => setIsReportOpen(false)}
      />
    </div>
  )
}
