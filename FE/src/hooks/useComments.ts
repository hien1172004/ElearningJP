import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { commentService } from '@/services/comment.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  CommentItem,
  CommentListResponse,
  CommentReactionState,
  CommentReplyItem,
  CommentReplyListResponse,
  CommentTargetType,
} from '@/types'

/**
 * Hooks cho module bình luận.
 *
 * Bình luận được gắn theo cặp `(targetType, targetId)`:
 *  - LESSON    → id bài học
 *  - VOCABULARY→ id từ vựng
 *  - KANJI     → id của `writing_characters`
 *  - GRAMMAR   → id điểm ngữ pháp
 */

// ============================================================================
// Queries
// ============================================================================

/**
 * Danh sách bình luận của 1 target.
 *
 * @param page trang hiện tại (0-based)
 */
export function useComments(
  targetType: CommentTargetType,
  targetId: number | undefined | null,
  page = 0,
  options?: { enabled?: boolean; size?: number },
) {
  const safeId = targetId ?? 0
  const size = options?.size ?? 20
  const enabled =
    (options?.enabled ?? true) && safeId > 0 && !Number.isNaN(safeId)

  return useQuery<CommentListResponse, AxiosError>({
    queryKey: enabled
      ? QUERY_KEYS.comment.list(targetType, safeId, page)
      : ['comments', 'list', 'disabled'],
    queryFn: async () => {
      const res = await commentService.getComments(targetType, safeId, page, size)
      return (res?.data ?? {
        items: [],
        total: 0,
        page,
        size,
        totalPages: 0,
      }) as CommentListResponse
    },
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    retry: 1,
  })
}

/**
 * Danh sách reply của 1 bình luận.
 *
 * Mặc định **không** tự tải (`enabled = false`) — chỉ tải khi user bấm
 * "X trả lời" để tránh gọi quá nhiều request khi có nhiều bình luận.
 */
export function useCommentReplies(
  commentId: number | undefined | null,
  page = 0,
  options?: { enabled?: boolean },
) {
  const safeId = commentId ?? 0
  const enabled =
    (options?.enabled ?? false) && safeId > 0 && !Number.isNaN(safeId)

  return useQuery<CommentReplyListResponse, AxiosError>({
    queryKey: enabled
      ? QUERY_KEYS.comment.replies(safeId, page)
      : ['comments', 'replies', 'disabled'],
    queryFn: async () => {
      const res = await commentService.getReplies(safeId, page)
      return (res?.data ?? {
        items: [],
        total: 0,
        page,
        size: 50,
        totalPages: 0,
      }) as CommentReplyListResponse
    },
    enabled,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
    retry: 1,
  })
}

// ============================================================================
// Helpers (nội bộ)
// ============================================================================

/** Invalidate toàn bộ cache bình luận của 1 target. */
function useInvalidateTarget() {
  const queryClient = useQueryClient()
  return (targetType: CommentTargetType, targetId: number) =>
    queryClient.invalidateQueries({
      queryKey: ['comments', 'list', targetType, targetId],
    })
}

/** Invalidate toàn bộ cache reply. */
function useInvalidateReplies() {
  const queryClient = useQueryClient()
  return (commentId: number) =>
    queryClient.invalidateQueries({
      queryKey: ['comments', 'replies', commentId],
    })
}

// ============================================================================
// Mutations
// ============================================================================

/** Tạo bình luận mới. */
export function useCreateComment(
  targetType: CommentTargetType,
  targetId: number,
) {
  const invalidate = useInvalidateTarget()
  return useMutation<
    CommentItem,
    any,
    { content: string }
  >({
    mutationFn: async ({ content }) => {
      const res = await commentService.createComment(targetType, targetId, content)
      return res?.data as CommentItem
    },
    onSuccess: () => {
      void invalidate(targetType, targetId)
    },
  })
}

/** Sửa nội dung bình luận (chủ sở hữu). */
export function useUpdateComment(
  targetType: CommentTargetType,
  targetId: number,
) {
  const invalidate = useInvalidateTarget()
  return useMutation<
    CommentItem,
    any,
    { commentId: number; content: string }
  >({
    mutationFn: async ({ commentId, content }) => {
      const res = await commentService.updateComment(commentId, content)
      return res?.data as CommentItem
    },
    onSuccess: () => {
      void invalidate(targetType, targetId)
    },
  })
}

/** Xoá bình luận (chủ sở hữu). */
export function useDeleteComment(
  targetType: CommentTargetType,
  targetId: number,
) {
  const invalidate = useInvalidateTarget()
  return useMutation<void, any, { commentId: number }>({
    mutationFn: async ({ commentId }) => {
      await commentService.deleteComment(commentId)
    },
    onSuccess: () => {
      void invalidate(targetType, targetId)
    },
  })
}

/** Tạo reply (có thể reply lồng nhau). */
export function useAddReply(
  targetType: CommentTargetType,
  targetId: number,
) {
  const invalidateTarget = useInvalidateTarget()
  const invalidateReplies = useInvalidateReplies()
  return useMutation<
    CommentReplyItem,
    any,
    { commentId: number; content: string; parentReplyId?: number }
  >({
    mutationFn: async ({ commentId, content, parentReplyId }) => {
      const res = await commentService.addReply(commentId, content, parentReplyId)
      return res?.data as CommentReplyItem
    },
    onSuccess: (_data, { commentId }) => {
      void invalidateReplies(commentId)
      void invalidateTarget(targetType, targetId)
    },
  })
}

/** Xoá reply (chủ sở hữu). */
export function useDeleteReply(
  targetType: CommentTargetType,
  targetId: number,
) {
  const invalidateTarget = useInvalidateTarget()
  const invalidateReplies = useInvalidateReplies()
  return useMutation<void, any, { replyId: number; commentId: number }>({
    mutationFn: async ({ replyId }) => {
      await commentService.deleteReply(replyId)
    },
    onSuccess: (_data, { commentId }) => {
      void invalidateReplies(commentId)
      void invalidateTarget(targetType, targetId)
    },
  })
}

// ============================================================================
// Like / Dislike
// ============================================================================

type ToggleKind = 'like' | 'dislike'

/**
 * Context của optimistic update — lưu cache cũ để rollback khi lỗi.
 */
interface ReactionContext {
  previous: Array<[readonly unknown[], unknown]>
}

/**
 * Toggle like/dislike cho bình luận.
 *
 * **Vì sao cần optimistic update + server response?**
 *
 * 1. Optimistic (`onMutate`): đổi cờ + số đếm ngay khi bấm → UI phản hồi tức thì.
 * 2. Server response (`onSuccess`): BE trả về trạng thái **chính xác** sau toggle
 *    (`liked`, `disliked`, `likeCount`, `dislikeCount`) → ta ghi đè lên cache.
 *    Đây là nguồn sự thật, loại bỏ mọi lệch số đếm do optimistic tính sai
 *    (vd: bấm like 2 lần liên tiếp, hoặc server đã có sẵn reaction).
 * 3. `onError`: rollback về cache cũ.
 * 4. `onSettled`: invalidate để đồng bộ với các client khác.
 *
 * Nhờ bước 2, tag like/dislike hiển thị đúng ngay sau khi bấm — **không cần
 * reload trang**.
 */
export function useToggleCommentReaction(
  targetType: CommentTargetType,
  targetId: number,
  kind: ToggleKind,
) {
  const queryClient = useQueryClient()
  const invalidate = useInvalidateTarget()

  return useMutation<
    CommentReactionState,
    any,
    { commentId: number },
    ReactionContext
  >(
    {
      mutationFn: async ({ commentId }) => {
        const res =
          kind === 'like'
            ? await commentService.toggleCommentLike(commentId)
            : await commentService.toggleCommentDislike(commentId)
        return (res?.data ?? {
          liked: false,
          disliked: false,
          likeCount: 0,
          dislikeCount: 0,
        }) as CommentReactionState
      },
      onMutate: async ({ commentId }) => {
        await queryClient.cancelQueries({
          queryKey: ['comments', 'list', targetType, targetId],
        })
        const previous = queryClient.getQueriesData<CommentListResponse>({
          queryKey: ['comments', 'list', targetType, targetId],
        })

        queryClient.setQueriesData<CommentListResponse>(
          { queryKey: ['comments', 'list', targetType, targetId] },
          (old) => {
            if (!old?.items) return old
            return {
              ...old,
              items: old.items.map((c: CommentItem) =>
                c.id === commentId ? applyReaction(c, kind) : c,
              ),
            }
          },
        )

        return { previous }
      },
      onSuccess: (state, { commentId }) => {
        // Ghi đè bằng trạng thái chính xác từ server.
        queryClient.setQueriesData<CommentListResponse>(
          { queryKey: ['comments', 'list', targetType, targetId] },
          (old) => {
            if (!old?.items) return old
            return {
              ...old,
              items: old.items.map((c: CommentItem) =>
                c.id === commentId ? applyServerState(c, state) : c,
              ),
            }
          },
        )
      },
      onError: (_err, _vars, context) => {
        // Rollback về trạng thái trước khi optimistic update.
        context?.previous?.forEach(([key, data]) => {
          queryClient.setQueryData(key, data)
        })
      },
      onSettled: () => {
        void invalidate(targetType, targetId)
      },
    },
  )
}

/** Toggle like/dislike cho reply (optimistic update trên cache replies). */
export function useToggleReplyReaction(
  targetType: CommentTargetType,
  targetId: number,
  kind: ToggleKind,
) {
  const queryClient = useQueryClient()
  const invalidateTarget = useInvalidateTarget()
  const invalidateReplies = useInvalidateReplies()

  return useMutation<
    CommentReactionState,
    any,
    { replyId: number; commentId: number },
    ReactionContext
  >(
    {
      mutationFn: async ({ replyId }) => {
        const res =
          kind === 'like'
            ? await commentService.toggleReplyLike(replyId)
            : await commentService.toggleReplyDislike(replyId)
        return (res?.data ?? {
          liked: false,
          disliked: false,
          likeCount: 0,
          dislikeCount: 0,
        }) as CommentReactionState
      },
      onMutate: async ({ replyId }) => {
        await queryClient.cancelQueries({ queryKey: ['comments', 'replies'] })
        const previous = queryClient.getQueriesData<CommentReplyListResponse>({
          queryKey: ['comments', 'replies'],
        })

        queryClient.setQueriesData<CommentReplyListResponse>(
          { queryKey: ['comments', 'replies'] },
          (old) => {
            if (!old?.items) return old
            return {
              ...old,
              items: old.items.map((r: CommentReplyItem) =>
                r.id === replyId ? applyReplyReaction(r, kind) : r,
              ),
            }
          },
        )

        return { previous }
      },
      onSuccess: (state, { replyId }) => {
        // Ghi đè bằng trạng thái chính xác từ server.
        queryClient.setQueriesData<CommentReplyListResponse>(
          { queryKey: ['comments', 'replies'] },
          (old) => {
            if (!old?.items) return old
            return {
              ...old,
              items: old.items.map((r: CommentReplyItem) =>
                r.id === replyId ? applyServerState(r, state) : r,
              ),
            }
          },
        )
      },
      onError: (_err, _vars, context) => {
        context?.previous?.forEach(([key, data]) => {
          queryClient.setQueryData(key, data)
        })
      },
      onSettled: (_data, { commentId }) => {
        void invalidateReplies(commentId)
        void invalidateTarget(targetType, targetId)
      },
    },
  )
}

/**
 * Ghi đè trạng thái reaction bằng dữ liệu chính xác từ server.
 *
 * <p>Áp dụng cho cả comment và reply — shape giống nhau
 * (`liked` / `disliked` / `likeCount` / `dislikeCount`).</p>
 */
function applyServerState<
  T extends {
    liked: boolean
    disliked: boolean
    likeCount: number
    dislikeCount: number
  },
>(item: T, state: CommentReactionState): T {
  return {
    ...item,
    liked: state.liked,
    disliked: state.disliked,
    likeCount: state.likeCount,
    dislikeCount: state.dislikeCount,
  }
}

/** Tính lại cờ + số đếm sau khi toggle like/dislike. */
function applyReaction(
  comment: CommentItem,
  kind: ToggleKind,
): CommentItem {
  if (kind === 'like') {
    const liked = !comment.liked
    return {
      ...comment,
      liked,
      disliked: liked ? false : comment.disliked,
      likeCount: Math.max(0, comment.likeCount + (liked ? 1 : -1)),
      dislikeCount:
        liked && comment.disliked
          ? Math.max(0, comment.dislikeCount - 1)
          : comment.dislikeCount,
    }
  }
  const disliked = !comment.disliked
  return {
    ...comment,
    disliked,
    liked: disliked ? false : comment.liked,
    dislikeCount: Math.max(0, comment.dislikeCount + (disliked ? 1 : -1)),
    likeCount:
      disliked && comment.liked
        ? Math.max(0, comment.likeCount - 1)
        : comment.likeCount,
  }
}

/** Tương tự {@link applyReaction} nhưng cho reply. */
function applyReplyReaction(
  reply: CommentReplyItem,
  kind: ToggleKind,
): CommentReplyItem {
  if (kind === 'like') {
    const liked = !reply.liked
    return {
      ...reply,
      liked,
      disliked: liked ? false : reply.disliked,
      likeCount: Math.max(0, reply.likeCount + (liked ? 1 : -1)),
      dislikeCount:
        liked && reply.disliked
          ? Math.max(0, reply.dislikeCount - 1)
          : reply.dislikeCount,
    }
  }
  const disliked = !reply.disliked
  return {
    ...reply,
    disliked,
    liked: disliked ? false : reply.liked,
    dislikeCount: Math.max(0, reply.dislikeCount + (disliked ? 1 : -1)),
    likeCount:
      disliked && reply.liked
        ? Math.max(0, reply.likeCount - 1)
        : reply.likeCount,
  }
}

/**
 * Lấy message lỗi từ axios error (BE trả `{ code, message }`).
 */
export function extractErrorMessage(err: any, fallback: string): string {
  return err?.response?.data?.message ?? err?.message ?? fallback
}
