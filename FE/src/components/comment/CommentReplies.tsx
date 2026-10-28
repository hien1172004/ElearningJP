import React, { useMemo, useState } from 'react'
import { Button } from 'antd'
import { CornerDownRight, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { useCommentReplies, useAddReply } from '@/hooks/useComments'
import { CommentReplyItem } from './CommentReplyItem'
import { CommentComposer } from './CommentComposer'
import type {
  CommentReplyItem as CommentReplyItemType,
  CommentTargetType,
} from '@/types'

interface CommentRepliesProps {
  commentId: number
  targetType: CommentTargetType
  targetId: number
  /** Số reply đã biết (replyCount của comment) — dùng để hiện nút mở. */
  replyCount?: number
  /** Mở sẵn khối reply (vd: khi vừa gửi 1 reply). */
  defaultOpen?: boolean
}

/** Node của cây reply (dựng từ `parentReplyId` — BE trả về danh sách phẳng). */
interface ReplyNode {
  reply: CommentReplyItemType
  children: ReplyNode[]
}

/**
 * Dựng cây reply từ danh sách phẳng.
 * Reply không tìm thấy parent → coi như reply cấp 0 (phòng trường hợp dữ liệu lệch).
 */
function buildReplyTree(
  replies: CommentReplyItemType[],
): ReplyNode[] {
  const byId = new Map<number, ReplyNode>()
  replies.forEach((reply) => {
    byId.set(reply.id, { reply, children: [] })
  })

  const roots: ReplyNode[] = []
  replies.forEach((reply) => {
    const node = byId.get(reply.id)!
    const parentId = reply.parentReplyId
    const parent = parentId != null ? byId.get(parentId) : undefined
    if (parent && parent !== node) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

/** Render 1 node + toàn bộ con (đệ quy, tối đa 2 cấp hiển thị). */
function renderNode(
  node: ReplyNode,
  depth: number,
  props: Omit<CommentRepliesProps, 'defaultOpen'>,
  onReply: (reply: CommentReplyItemType) => void,
): React.ReactNode {
  return (
    <div key={node.reply.id} className="space-y-2.5">
      <CommentReplyItem
        reply={node.reply}
        targetType={props.targetType}
        targetId={props.targetId}
        depth={depth}
        onReply={onReply}
      />
      {node.children.length > 0 && (
        <div className="space-y-2.5">
          {node.children.map((child) =>
            renderNode(child, depth + 1, props, onReply),
          )}
        </div>
      )}
    </div>
  )
}

/**
 * CommentReplies — khối trả lời của 1 bình luận.
 *
 * - Mặc định thu gọn, chỉ tải data khi user bấm "X trả lời".
 * - Cây reply lồng nhau dựng từ `parentReplyId`.
 * - Có ô nhập reply nhanh (gửi `parentReplyId` khi reply 1 reply khác).
 */
export const CommentReplies: React.FC<CommentRepliesProps> = ({
  commentId,
  targetType,
  targetId,
  replyCount = 0,
  defaultOpen = false,
}) => {
  const [isOpen, setIsOpen] = useState(defaultOpen)
  const [isComposerOpen, setIsComposerOpen] = useState(false)
  const [replyTo, setReplyTo] = useState<CommentReplyItemType | null>(null)

  const { data, isLoading, isError, refetch } = useCommentReplies(
    commentId,
    0,
    { enabled: isOpen },
  )
  const addReplyMutation = useAddReply(targetType, targetId)

  const tree = useMemo(
    () => buildReplyTree(data?.items ?? []),
    [data?.items],
  )

  const handleToggle = () => {
    setIsOpen((prev) => !prev)
    if (!isOpen) {
      setIsComposerOpen(false)
      setReplyTo(null)
    }
  }

  /** Mở ô nhập reply (có thể trả lời 1 reply khác). */
  const handleStartReply = (reply?: CommentReplyItemType) => {
    setReplyTo(reply ?? null)
    setIsComposerOpen(true)
  }

  const handleSubmitReply = async (content: string) => {
    try {
      await addReplyMutation.mutateAsync({
        commentId,
        content,
        parentReplyId: replyTo?.id,
      })
      toast.success('Đã gửi trả lời')
      setIsComposerOpen(false)
      setReplyTo(null)
      void refetch()
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ?? 'Không gửi được trả lời. Vui lòng thử lại.',
      )
    }
  }

  const totalReplies = data?.total ?? replyCount

  return (
    <div className="mt-3 border-t border-gray-100 pt-3">
      {/* Nút mở / đóng khối reply */}
      <button
        type="button"
        onClick={handleToggle}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-700 transition"
      >
        <CornerDownRight className="w-3.5 h-3.5" />
        {isOpen
          ? 'Ẩn trả lời'
          : totalReplies > 0
            ? `Xem ${totalReplies} trả lời`
            : 'Trả lời'}
      </button>

      {isOpen && (
        <div className="mt-3 space-y-3">
          {/* Nội dung reply */}
          {isLoading && (
            <div className="flex items-center gap-2 py-3 text-xs text-gray-400">
              <Loader2 className="w-4 h-4 animate-spin" />
              Đang tải trả lời...
            </div>
          )}

          {isError && (
            <div className="flex items-center justify-between gap-2 py-3">
              <p className="text-xs text-rose-500">
                Không tải được danh sách trả lời.
              </p>
              <Button size="small" onClick={() => void refetch()}>
                Thử lại
              </Button>
            </div>
          )}

          {!isLoading && !isError && tree.length === 0 && (
            <p className="text-xs text-gray-400 py-2">
              Chưa có trả lời nào. Hãy là người đầu tiên trả lời!
            </p>
          )}

          {tree.length > 0 && (
            <div className="space-y-3">
              {tree.map((node) =>
                renderNode(node, 0, { commentId, targetType, targetId }, handleStartReply),
              )}
            </div>
          )}

          {/* Ô nhập trả lời */}
          {isComposerOpen ? (
            <div className="space-y-2">
              {replyTo && (
                <div className="flex items-center justify-between gap-2 rounded-md bg-gray-50 border border-gray-200 px-3 py-2">
                  <p className="text-[11px] text-gray-600 truncate">
                    Đang trả lời <strong>{replyTo.user?.fullName ?? 'bình luận'}</strong>
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setReplyTo(null)
                      setIsComposerOpen(false)
                    }}
                    className="text-[11px] font-semibold text-gray-400 hover:text-gray-600 transition shrink-0"
                  >
                    Huỷ
                  </button>
                </div>
              )}
              <CommentComposer
                compact
                autoFocus
                placeholder={
                  replyTo
                    ? `Trả lời ${replyTo.user?.fullName ?? ''}...`
                    : 'Viết trả lời của bạn...'
                }
                submitLabel="Gửi trả lời"
                isSubmitting={addReplyMutation.isPending}
                onSubmit={handleSubmitReply}
              />
            </div>
          ) : (
            <Button
              size="small"
              type="text"
              className="!px-0 !text-xs !font-semibold !text-indigo-600"
              onClick={() => handleStartReply()}
            >
              Viết trả lời
            </Button>
          )}
        </div>
      )}
    </div>
  )
}
