import { axiosClient } from './api/axiosClient'
import type { ApiResponse, PushSubscriptionPayload } from '@/types'

/**
 * Service cho Web Push — lưu/xóa subscription ở backend.
 */
export const webPushService = {
  subscribe: async (payload: PushSubscriptionPayload): Promise<ApiResponse<void>> => {
    return axiosClient.post('/push/subscribe', payload)
  },

  unsubscribe: async (endpoint: string): Promise<ApiResponse<void>> => {
    return axiosClient.delete('/push/unsubscribe', { params: { endpoint } })
  },
}
