import React, { useState } from 'react'
import { Avatar, Button, Input, Tooltip } from 'antd'
import {
  ThumbsUp,
  ThumbsDown,
  Trash2,
  Pencil,
  Save,
  X,
} from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/utils/cn'
import { formatRelativeTime } from '@/utils/format'
import { confirmDialog } from '@/components/common/ConfirmModal'
import {
  useDeleteComment,
  useToggleCommentReaction,
  useUpdateComment,
} from '@/hooks/useComments'
import { CommentReplies } from './CommentReplies'
import type { CommentItem as CommentItemType, CommentTargetType } from '@/types'

interface CommentItemProps {
  comment: CommentItemType
  targetType: CommentTargetType
  targetId: number
}

/** BE giới hạn `@Size(max = 5000)`. */
const MAX_CONTENT_LENGTH = 5000

/**
 * CommentItem — 1 bình luận gốc.
 *
 * Hiển thị: avatar, tên, thời gian tương đối, nội dung, thanh hành động
 * (like / dislike / trả lời / sửa / xoá) và khối reply thu gọn.
 */
export const CommentItem: React.FC<CommentItemProps> = ({
  comment,
  targetType,
  targetId,
}) => {
  const [isEditing, setIsEditing] = useState(false)
  const [editValue, setEditValue] = useState(comment.content)

  const likeMutation = useToggleCommentReaction(targetType, targetId, 'like')
  const dislikeMutation = useToggleCommentReaction(
    targetType,
    targetId,
    'dislike',
  )
  const updateMutation = useUpdateComment(targetType, targetId)
  const deleteMutation = useDeleteComment(targetType, targetId)

  const isPending =
    likeMutation.isPending ||
    dislikeMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending

  const handleStartEdit = () => {
    setEditValue(comment.content)
    setIsEditing(true)
  }

  const handleCancelEdit = () => {
    setEditValue(comment.content)
    setIsEditing(false)
  }

  const handleSaveEdit = async () => {
    const content = editValue.trim()
    if (!content) {
      toast.error('Nội dung bình luận không được để trống')
      return
    }
    try {
      await updateMutation.mutateAsync({ commentId: comment.id, content })
      toast.success('Đã cập nhật bình luận')
      setIsEditing(false)
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ?? 'Không cập nhật được bình luận.',
      )
    }
  }

  const handleDelete = async () => {
    const confirmed = await confirmDialog({
      title: 'Xoá bình luận này?',
      content: 'Thao tác này không thể hoàn tác.',
      okText: 'Xoá',
      cancelText: 'Huỷ',
      danger: true,
    })
    if (!confirmed) return

    try {
      await deleteMutation.mutateAsync({ commentId: comment.id })
      toast.success('Đã xoá bình luận')
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ?? 'Không xoá được bình luận. Vui lòng thử lại.',
      )
    }
  }

  return (
    <article className="flex gap-3">
      {/* Avatar */}
      <Avatar
        size={36}
        src={comment.user?.avatarUrl ?? undefined}
        className="shrink-0 bg-indigo-100 text-indigo-600 text-xs"
      >
        {comment.user?.fullName?.charAt(0)?.toUpperCase() ?? '?'}
      </Avatar>

      <div className="flex-1 min-w-0">
        {/* Header */}
        <div className="flex items-baseline gap-2 flex-wrap">
          <span className="text-sm font-bold text-gray-900">
            {comment.user?.fullName ?? `User #${comment.user?.id ?? '?'}`}
          </span>
          <span className="text-[11px] text-gray-400">
            {formatRelativeTime(comment.createdAt)}
          </span>
          {comment.updatedAt !== comment.createdAt && !isEditing && (
            <span className="text-[10px] text-gray-400 italic">đã sửa</span>
          )}
        </div>

        {/* Nội dung (hoặc ô sửa nội tuyến) */}
        {isEditing ? (
          <div className="mt-2 space-y-2">
            <Input.TextArea
              value={editValue}
              onChange={(e) => setEditValue(e.target.value)}
              autoSize={{ minRows: 2, maxRows: 10 }}
              maxLength={MAX_CONTENT_LENGTH}
              showCount
              disabled={updateMutation.isPending}
              className="!rounded-md"
            />
            <div className="flex items-center gap-2">
              <Button
                size="small"
                type="primary"
                icon={<Save className="w-3.5 h-3.5" />}
                loading={updateMutation.isPending}
                onClick={handleSaveEdit}
                disabled={!editValue.trim()}
                className="!rounded-md !font-semibold"
              >
                Lưu
              </Button>
              <Button
                size="small"
                icon={<X className="w-3.5 h-3.5" />}
                onClick={handleCancelEdit}
                disabled={updateMutation.isPending}
                className="!rounded-md"
              >
                Huỷ
              </Button>
            </div>
          </div>
        ) : (
          <p className="text-sm text-gray-800 mt-1 whitespace-pre-wrap break-words leading-relaxed">
            {comment.content}
          </p>
        )}

        {/* Thanh hành động */}
        {!isEditing && (
          <div className="flex items-center gap-1 mt-2">
            <Tooltip title="Hữu ích">
              <button
                type="button"
                disabled={isPending}
                onClick={() => likeMutation.mutate({ commentId: comment.id })}
                className={cn(
                  'inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold transition',
                  comment.liked
                    ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                    : 'text-gray-500 hover:bg-gray-100 hover:text-gray-700',
                )}
              >
                <ThumbsUp className="w-3.5 h-3.5" />
                {comment.likeCount > 0 && <span>{comment.likeCount}</span>}
              </button>
            </Tooltip>

            <Tooltip title="Không hữu ích">
              <button
                type="button"
                disabled={isPending}
                onClick={() =>
                  dislikeMutation.mutate({ commentId: comment.id })
                }
                className={cn(
                  'inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold transition',
                  comment.disliked
                    ? 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                    : 'text-gray-500 hover:bg-gray-100 hover:text-gray-700',
                )}
              >
                <ThumbsDown className="w-3.5 h-3.5" />
                {comment.dislikeCount > 0 && (
                  <span>{comment.dislikeCount}</span>
                )}
              </button>
            </Tooltip>

            {comment.owner && (
              <>
                <button
                  type="button"
                  onClick={handleStartEdit}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold text-gray-500 hover:bg-gray-100 hover:text-gray-700 transition"
                >
                  <Pencil className="w-3.5 h-3.5" />
                  Sửa
                </button>

                <button
                  type="button"
                  onClick={handleDelete}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-semibold text-gray-400 hover:bg-rose-50 hover:text-rose-600 transition"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  Xoá
                </button>
              </>
            )}
          </div>
        )}

        {/* Khối trả lời */}
        {!isEditing && (
          <CommentReplies
            commentId={comment.id}
            targetType={targetType}
            targetId={targetId}
            replyCount={comment.replyCount}
          />
        )}
      </div>
    </article>
  )
}
