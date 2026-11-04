import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  ModerationAction,
  ModerationBrowseParams,
  ModerationItem,
  ModerationKind,
  ModerationQueueParams,
  PageResponse,
} from '@/types'

const segment = (kind: ModerationKind) => (kind === 'REPLY' ? 'replies' : 'comments')

/**
 * Kiểm duyệt bình luận — BE: CommentModerationController `/admin/moderation`.
 * ADMIN: mọi bình luận; TEACHER: bình luận trong bài học thuộc khóa của mình (ngoài phạm vi → 403).
 */
export const adminModerationService = {
  getReportQueue: async (
    params: ModerationQueueParams,
  ): Promise<ApiResponse<PageResponse<ModerationItem>>> => {
    return axiosClient.get('/admin/moderation/reports', { params })
  },

  browse: async (
    params: ModerationBrowseParams,
  ): Promise<ApiResponse<PageResponse<ModerationItem>>> => {
    return axiosClient.get('/admin/moderation/comments', { params })
  },

  /** Ẩn / khôi phục / xóa / bỏ qua báo cáo. `note` gửi kèm thông báo cho người viết (ẩn, xóa). */
  act: async (
    kind: ModerationKind,
    id: number,
    action: ModerationAction,
    note?: string,
  ): Promise<ApiResponse<void>> => {
    const base = `/admin/moderation/${segment(kind)}/${id}`
    const body = note ? { note } : undefined
    switch (action) {
      case 'hide':
        return axiosClient.post(`${base}/hide`, body)
      case 'restore':
        return axiosClient.post(`${base}/restore`)
      case 'dismiss':
        return axiosClient.post(`${base}/dismiss-reports`)
      case 'delete':
        return axiosClient.delete(base, { data: body })
    }
  },
}
