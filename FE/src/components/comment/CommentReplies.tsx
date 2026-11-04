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
  /** Reply cần mở (từ thông báo) → tự mở khối reply, cuộn tới + highlight. */
  focusReplyId?: number
  onFocusDone?: () => void
  /**
   * Ô viết trả lời — do `CommentItem` điều khiển để nút "Trả lời" nằm trên thanh hành động
   * của bình luận. `replyTo` khác null khi trả lời một reply cụ thể.
   */
  composer?: { open: boolean; replyTo: CommentReplyItemType | null }
  /** Mở ô viết trả lời (không truyền reply = trả lời bình luận gốc). */
  onStartReply?: (reply?: CommentReplyItemType) => void
  onCloseComposer?: () => void
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
  props: Pick<CommentRepliesProps, 'targetType' | 'targetId' | 'focusReplyId' | 'onFocusDone'>,
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
        focused={props.focusReplyId === node.reply.id}
        onFocusDone={props.onFocusDone}
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
 * - Mặc định thu gọn, chỉ tải data khi user bấm "Xem N trả lời" (chỉ hiện khi đã có trả lời).
 * - Cây reply lồng nhau dựng từ `parentReplyId`.
 * - Ô viết trả lời hiện ngay khi bấm "Trả lời" (trạng thái nằm ở `CommentItem`);
 *   gửi `parentReplyId` khi trả lời 1 reply khác.
 */
export const CommentReplies: React.FC<CommentRepliesProps> = ({
  commentId,
  targetType,
  targetId,
  replyCount = 0,
  defaultOpen = false,
  focusReplyId,
  onFocusDone,
  composer = { open: false, replyTo: null },
  onStartReply,
  onCloseComposer,
}) => {
  const [isOpen, setIsOpen] = useState(defaultOpen || !!focusReplyId)
  // Thông báo trỏ tới 1 reply khi khối đang đóng → mở ra (mỗi reply 1 lần).
  const [openedForReplyId, setOpenedForReplyId] = useState(focusReplyId)
  if (focusReplyId && focusReplyId !== openedForReplyId) {
    setOpenedForReplyId(focusReplyId)
    setIsOpen(true)
  }
  const replyTo = composer.replyTo

  const { data, isLoading, isError, refetch } = useCommentReplies(
    commentId,
    1,
    { enabled: isOpen },
  )
  const addReplyMutation = useAddReply(targetType, targetId)

  const tree = useMemo(
    () => buildReplyTree(data?.items ?? []),
    [data?.items],
  )

  const handleToggle = () => {
    setIsOpen((prev) => !prev)
  }

  /** Trả lời 1 reply khác → mở ô viết trả lời ngay. */
  const handleStartReply = (reply?: CommentReplyItemType) => {
    onStartReply?.(reply)
  }

  const handleSubmitReply = async (content: string) => {
    try {
      await addReplyMutation.mutateAsync({
        commentId,
        content,
        parentReplyId: replyTo?.id,
      })
      toast.success('Đã gửi trả lời')
      onCloseComposer?.()
      // Mở danh sách để thấy ngay trả lời vừa gửi
      if (isOpen) void refetch()
      else setIsOpen(true)
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ?? 'Không gửi được trả lời. Vui lòng thử lại.',
      )
    }
  }

  const totalReplies = data?.total ?? replyCount
  const hasReplies = totalReplies > 0

  // Chưa có trả lời và chưa mở ô viết → không hiện gì (bỏ chú thích "Chưa có trả lời nào")
  if (!hasReplies && !composer.open) return null

  return (
    <div className="mt-2 space-y-3">
      {/* Nút mở / đóng danh sách trả lời — chỉ khi đã có trả lời */}
      {hasReplies && (
        <button
          type="button"
          onClick={handleToggle}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-700 transition"
        >
          <CornerDownRight className="w-3.5 h-3.5" />
          {isOpen ? 'Ẩn trả lời' : `Xem ${totalReplies} trả lời`}
        </button>
      )}

      {isOpen && hasReplies && (
        <div className="space-y-3">
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

          {tree.length > 0 && (
            <div className="space-y-3">
              {tree.map((node) =>
                renderNode(
                  node,
                  0,
                  { targetType, targetId, focusReplyId, onFocusDone },
                  handleStartReply,
                ),
              )}
            </div>
          )}
        </div>
      )}

      {/* Ô viết trả lời — hiện ngay khi bấm "Trả lời" */}
      {composer.open && (
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-2 rounded-md bg-gray-50 border border-gray-200 px-3 py-2">
            <p className="text-[11px] text-gray-600 truncate">
              Đang trả lời <strong>{replyTo?.user?.fullName ?? 'bình luận'}</strong>
            </p>
            <button
              type="button"
              onClick={() => onCloseComposer?.()}
              className="text-[11px] font-semibold text-gray-400 hover:text-gray-600 transition shrink-0"
            >
              Huỷ
            </button>
          </div>
          <CommentComposer
            // Đổi người được trả lời → tạo lại ô viết để focus đúng chỗ
            key={replyTo?.id ?? 'root'}
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
      )}
    </div>
  )
}
