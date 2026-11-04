import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  CommentItem,
  CommentListResponse,
  CommentLocation,
  CommentReactionState,
  CommentReportPayload,
  CommentReplyItem,
  CommentReplyListResponse,
  CommentTargetType,
} from '@/types'

/**
 * commentService — gọi API bình luận của BE.
 *
 * BE dùng path theo target: `/{targetType}/{targetId}/comments`
 * với targetType ∈ LESSON | VOCABULARY | KANJI | GRAMMAR.
 *
 * > Lưu ý: `axiosClient` đã tự đính kèm Bearer token + refresh 401,
 * > nên các method POST/PUT/DELETE chỉ cần gọi thẳng.
 */
export const commentService = {
  /** Danh sách bình luận của 1 target (mới nhất trước). */
  getComments: async (
    targetType: CommentTargetType,
    targetId: number,
    page = 1,
    size = 5,
  ): Promise<ApiResponse<CommentListResponse>> => {
    return axiosClient.get(`/${targetType}/${targetId}/comments`, {
      params: { page, size },
    })
  },

  /** Tạo bình luận mới trên 1 target. */
  createComment: async (
    targetType: CommentTargetType,
    targetId: number,
    content: string,
  ): Promise<ApiResponse<CommentItem>> => {
    return axiosClient.post(`/${targetType}/${targetId}/comments`, { content })
  },

  /** Cập nhật nội dung bình luận (chỉ chủ sở hữu). */
  updateComment: async (
    commentId: number,
    content: string,
  ): Promise<ApiResponse<CommentItem>> => {
    return axiosClient.put(`/comments/${commentId}`, { content })
  },

  /** Xoá mềm bình luận (chỉ chủ sở hữu). */
  deleteComment: async (commentId: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/comments/${commentId}`)
  },

  /** Vị trí của bình luận (target, từ khóa / khóa học, thứ tự) — để mở thẳng từ thông báo. */
  getCommentLocation: async (
    commentId: number,
    replyId?: number | null,
  ): Promise<ApiResponse<CommentLocation>> => {
    return axiosClient.get(`/comments/${commentId}/location`, {
      params: replyId ? { replyId } : undefined,
    })
  },

  /** Danh sách reply của 1 bình luận. */
  getReplies: async (
    commentId: number,
    page = 1,
    size = 50,
  ): Promise<ApiResponse<CommentReplyListResponse>> => {
    return axiosClient.get(`/comments/${commentId}/replies`, {
      params: { page, size },
    })
  },

  /** Tạo reply (có thể reply lồng nhau bằng `parentReplyId`). */
  addReply: async (
    commentId: number,
    content: string,
    parentReplyId?: number,
  ): Promise<ApiResponse<CommentReplyItem>> => {
    return axiosClient.post(`/comments/${commentId}/replies`, {
      content,
      parentReplyId: parentReplyId ?? undefined,
    })
  },

  /** Xoá mềm reply (chỉ chủ sở hữu). */
  deleteReply: async (replyId: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/comment-replies/${replyId}`)
  },

  /** Toggle like bình luận → trả về trạng thái reaction đầy đủ. */
  toggleCommentLike: async (
    commentId: number,
  ): Promise<ApiResponse<CommentReactionState>> => {
    return axiosClient.post(`/comments/${commentId}/like`)
  },

  /** Toggle dislike bình luận → trả về trạng thái reaction đầy đủ. */
  toggleCommentDislike: async (
    commentId: number,
  ): Promise<ApiResponse<CommentReactionState>> => {
    return axiosClient.post(`/comments/${commentId}/dislike`)
  },

  /** Toggle like reply → trả về trạng thái reaction đầy đủ. */
  toggleReplyLike: async (
    replyId: number,
  ): Promise<ApiResponse<CommentReactionState>> => {
    return axiosClient.post(`/comment-replies/${replyId}/like`)
  },

  /** Toggle dislike reply → trả về trạng thái reaction đầy đủ. */
  toggleReplyDislike: async (
    replyId: number,
  ): Promise<ApiResponse<CommentReactionState>> => {
    return axiosClient.post(`/comment-replies/${replyId}/dislike`)
  },

  /** Báo cáo vi phạm bình luận (mỗi người 1 lần, không tự báo cáo — BE trả 409/400). */
  reportComment: async (
    commentId: number,
    payload: CommentReportPayload,
  ): Promise<ApiResponse<void>> => {
    return axiosClient.post(`/comments/${commentId}/report`, payload)
  },

  /** Báo cáo vi phạm trả lời. */
  reportReply: async (
    replyId: number,
    payload: CommentReportPayload,
  ): Promise<ApiResponse<void>> => {
    return axiosClient.post(`/comment-replies/${replyId}/report`, payload)
  },
}
