import React from 'react'
import { Avatar, Tooltip } from 'antd'
import { ThumbsUp, ThumbsDown, Trash2, CornerUpLeft } from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/utils/cn'
import { formatRelativeTime } from '@/utils/format'
import { confirmDialog } from '@/components/common/ConfirmModal'
import {
  useDeleteReply,
  useToggleReplyReaction,
} from '@/hooks/useComments'
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
}

/** Số cấp lồng nhau tối đa được hiển thị (reply cấp 3 sẽ phẳng về cấp 2). */
const MAX_VISUAL_DEPTH = 1

/**
 * CommentReplyItem — 1 dòng reply trong cây trả lời.
 *
 * Hiển thị: avatar nhỏ, tên, thời gian tương đối, nội dung, like/dislike,
 * nút "Trả lời" và nút xoá (chỉ chủ sở hữu).
 */
export const CommentReplyItem: React.FC<CommentReplyItemProps> = ({
  reply,
  targetType,
  targetId,
  depth = 0,
  onDeleted,
  onReply,
}) => {
  const likeMutation = useToggleReplyReaction(targetType, targetId, 'like')
  const dislikeMutation = useToggleReplyReaction(targetType, targetId, 'dislike')
  const deleteMutation = useDeleteReply(targetType, targetId)

  const isPending =
    likeMutation.isPending ||
    dislikeMutation.isPending ||
    deleteMutation.isPending

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
      className={cn(
        'flex gap-2.5',
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
              disabled={isPending}
              onClick={() =>
                likeMutation.mutate({
                  replyId: reply.id,
                  commentId: reply.commentId,
                })
              }
              className={cn(
                'inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold transition',
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
              disabled={isPending}
              onClick={() =>
                dislikeMutation.mutate({
                  replyId: reply.id,
                  commentId: reply.commentId,
                })
              }
              className={cn(
                'inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-semibold transition',
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

          {reply.owner && (
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
        </div>
      </div>
    </div>
  )
}
