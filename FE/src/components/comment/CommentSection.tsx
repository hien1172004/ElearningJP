import React, { useCallback, useState } from 'react'
import { Button, Pagination } from 'antd'
import { MessageSquare, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { useComments, useCreateComment, extractErrorMessage } from '@/hooks/useComments'
import { CommentComposer } from './CommentComposer'
import { CommentItem } from './CommentItem'
import type { CommentTargetType } from '@/types'
import type { CommentFocus } from '@/utils/commentFocus'

interface CommentSectionProps {
  /** Loại đối tượng được bình luận. */
  targetType: CommentTargetType
  /** Id của đối tượng (id bài học / id từ vựng / id hán tự / id ngữ pháp). */
  targetId: number
  /** Số bình luận mỗi trang. */
  pageSize?: number
  /** Tiêu đề tuỳ chỉnh. */
  title?: string
  className?: string
  /**
   * Bình luận cần mở thẳng (từ thông báo). Trang gọi chỉ truyền khi bình luận này
   * thuộc đúng target đang hiển thị. Mở đúng trang, cuộn tới và highlight 1 lần.
   */
  focus?: CommentFocus | null
}

/**
 * CommentSection — khối bình luận dùng chung.
 *
 * Dùng cho:
 *  - Trang tra cứu từ điển (VOCABULARY / KANJI / GRAMMAR)
 *  - Trang bài học (LESSON)
 *
 * ```tsx
 * <CommentSection targetType="VOCABULARY" targetId={vocabId} />
 * ```
 */
export const CommentSection: React.FC<CommentSectionProps> = ({
  targetType,
  targetId,
  pageSize = 5,
  title = 'Bình luận',
  className,
  focus,
}) => {
  const [page, setPage] = useState(1)

  // Đổi target (vd: chọn từ vựng khác trong trang tra cứu) → về trang đầu.
  // Dùng pattern "adjust state during render" của React thay vì useEffect
  // để tránh render thừa.
  const [prevTarget, setPrevTarget] = useState({ targetType, targetId })
  if (prevTarget.targetType !== targetType || prevTarget.targetId !== targetId) {
    setPrevTarget({ targetType, targetId })
    setPage(1)
  }

  // Bình luận cần mở (từ thông báo): nhảy tới trang chứa nó — mỗi focus chỉ áp dụng 1 lần.
  const focusKey = focus
    ? `${targetType}:${targetId}:${focus.commentId}:${focus.replyId ?? ''}`
    : null
  const [appliedFocusKey, setAppliedFocusKey] = useState<string | null>(null)
  const [doneFocusKey, setDoneFocusKey] = useState<string | null>(null)
  if (focus && focusKey && focusKey !== appliedFocusKey) {
    setAppliedFocusKey(focusKey)
    setDoneFocusKey(null)
    setPage(Math.floor(focus.position / pageSize) + 1)
  }
  // Đã cuộn + highlight xong → tắt focus để đổi trang / quay lại không cuộn lần nữa.
  const activeFocus = focus && focusKey !== doneFocusKey ? focus : null
  const handleFocusDone = useCallback(() => setDoneFocusKey(focusKey), [focusKey])

  const { data, isLoading, isError, error, refetch, isFetching } = useComments(
    targetType,
    targetId,
    page,
    { size: pageSize },
  )
  const createMutation = useCreateComment(targetType, targetId)

  const comments = data?.items ?? []
  const total = data?.total ?? 0
  const totalPages = data?.totalPages ?? 0

  const handleCreate = async (content: string) => {
    try {
      await createMutation.mutateAsync({ content })
      toast.success('Đã đăng bình luận')
      // Quay về trang đầu để thấy bình luận vừa đăng.
      setPage(1)
      void refetch()
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ??
          'Không đăng được bình luận. Vui lòng thử lại.',
      )
      throw err
    }
  }

  return (
    <section
      className={`bg-surface rounded-md border border-gray-200/80 shadow-xs p-4 md:p-5 ${className ?? ''}`}
      aria-label={title}
    >
      {/* ---------------- Header ---------------- */}
      <div className="flex items-center justify-between gap-3 pb-3 border-b border-gray-100">
        <h3 className="text-sm font-extrabold text-gray-900 flex items-center gap-2">
          <MessageSquare className="w-4 h-4 text-indigo-600" />
          {title}
          <span className="text-xs font-bold text-gray-400">
            ({total})
          </span>
        </h3>

        <button
          type="button"
          onClick={() => void refetch()}
          title="Tải lại bình luận"
          className="p-1.5 rounded-md text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition"
        >
          <RefreshCw
            className={`w-3.5 h-3.5 ${isFetching ? 'animate-spin' : ''}`}
          />
        </button>
      </div>

      {/* ---------------- Ô viết bình luận ---------------- */}
      <div className="py-4">
        <CommentComposer
          onSubmit={handleCreate}
          isSubmitting={createMutation.isPending}
        />
      </div>

      {/* ---------------- Danh sách bình luận ---------------- */}
      {isLoading && (
        <div className="py-10 text-center text-sm text-gray-400 animate-pulse">
          Đang tải bình luận...
        </div>
      )}

      {isError && !isLoading && (
        <div className="py-8 text-center space-y-2">
          <p className="text-sm text-rose-500">
            {extractErrorMessage(error, 'Không tải được bình luận.')}
          </p>
          <Button size="small" onClick={() => void refetch()}>
            Thử lại
          </Button>
        </div>
      )}

      {!isLoading && !isError && comments.length === 0 && (
        <div className="py-10 text-center space-y-1">
          <span className="inline-flex w-11 h-11 rounded-full bg-indigo-50 text-indigo-400 items-center justify-center">
            <MessageSquare className="w-5 h-5" />
          </span>
          <p className="text-sm font-bold text-gray-900">
            Chưa có bình luận nào
          </p>
          <p className="text-xs text-gray-500">
            Hãy là người đầu tiên chia sẻ kiến thức tại đây.
          </p>
        </div>
      )}

      {comments.length > 0 && (
        <div className="space-y-5 divide-y divide-gray-100">
          {comments.map((comment) => (
            <div key={comment.id} className="pb-5 last:pb-0 last:border-b-0">
              <CommentItem
                comment={comment}
                targetType={targetType}
                targetId={targetId}
                focused={activeFocus?.commentId === comment.id}
                focusReplyId={
                  activeFocus?.commentId === comment.id
                    ? activeFocus.replyId
                    : undefined
                }
                onFocusDone={handleFocusDone}
              />
            </div>
          ))}
        </div>
      )}

      {/* ---------------- Phân trang ---------------- */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 mt-4 pt-3 border-t border-gray-100">
          <Pagination
            size="small"
            current={page}
            total={total}
            pageSize={pageSize}
            onChange={(next) => setPage(next)}
            showSizeChanger={false}
            hideOnSinglePage
            itemRender={(_current, type, originalElement) => {
              if (type === 'prev' || type === 'next') {
                return (
                  <Button
                    size="small"
                    icon={
                      type === 'prev' ? (
                        <ChevronLeft className="w-3.5 h-3.5" />
                      ) : (
                        <ChevronRight className="w-3.5 h-3.5" />
                      )
                    }
                    loading={isFetching}
                    className="!rounded-md !font-semibold"
                  >
                    {type === 'prev' ? 'Trước' : 'Sau'}
                  </Button>
                )
              }
              return originalElement
            }}
          />
        </div>
      )}
    </section>
  )
}
