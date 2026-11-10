import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  Broadcast,
  BroadcastHistoryParams,
  BroadcastPayload,
  PageResponse,
  RecipientCountParams,
} from '@/types'

/**
 * Gửi thông báo hàng loạt — BE: BroadcastController `/admin/broadcasts`.
 * ADMIN: mọi đối tượng; TEACHER: chỉ học viên khóa của mình (BE trả 403 nếu không phải).
 */
export const adminBroadcastService = {
  countRecipients: async (params: RecipientCountParams): Promise<ApiResponse<number>> => {
    return axiosClient.get('/admin/broadcasts/recipient-count', { params })
  },

  send: async (payload: BroadcastPayload): Promise<ApiResponse<Broadcast>> => {
    return axiosClient.post('/admin/broadcasts', payload)
  },

  getHistory: async (params: BroadcastHistoryParams): Promise<ApiResponse<PageResponse<Broadcast>>> => {
    return axiosClient.get('/admin/broadcasts', { params })
  },
}
