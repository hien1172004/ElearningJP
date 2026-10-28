import React, { useState } from 'react'
import { Button } from 'antd'
import { MessageSquare, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { useComments, useCreateComment, extractErrorMessage } from '@/hooks/useComments'
import { CommentComposer } from './CommentComposer'
import { CommentItem } from './CommentItem'
import type { CommentTargetType } from '@/types'

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
  pageSize = 20,
  title = 'Bình luận',
  className,
}) => {
  const [page, setPage] = useState(0)

  // Đổi target (vd: chọn từ vựng khác trong trang tra cứu) → về trang đầu.
  // Dùng pattern "adjust state during render" của React thay vì useEffect
  // để tránh render thừa.
  const [prevTarget, setPrevTarget] = useState({ targetType, targetId })
  if (prevTarget.targetType !== targetType || prevTarget.targetId !== targetId) {
    setPrevTarget({ targetType, targetId })
    setPage(0)
  }

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
      setPage(0)
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
      className={`bg-white rounded-md border border-gray-200/80 shadow-xs p-4 md:p-5 ${className ?? ''}`}
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
              />
            </div>
          ))}
        </div>
      )}

      {/* ---------------- Phân trang ---------------- */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between gap-3 mt-4 pt-3 border-t border-gray-100">
          <Button
            size="small"
            icon={<ChevronLeft className="w-3.5 h-3.5" />}
            disabled={page <= 0 || isFetching}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="!rounded-md !font-semibold"
          >
            Trước
          </Button>

          <span className="text-xs text-gray-500">
            Trang {page + 1} / {totalPages}
          </span>

          <Button
            size="small"
            disabled={page >= totalPages - 1 || isFetching}
            onClick={() => setPage((p) => p + 1)}
            className="!rounded-md !font-semibold"
          >
            Sau
            <ChevronRight className="w-3.5 h-3.5 ml-1" />
          </Button>
        </div>
      )}
    </section>
  )
}
