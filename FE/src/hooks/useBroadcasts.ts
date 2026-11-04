import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { adminBroadcastService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  ApiResponse,
  Broadcast,
  BroadcastPayload,
  PageResponse,
  RecipientCountParams,
} from '@/types'

/** Mã lỗi BE (ErrorCode) → thông báo tiếng Việt. */
const ERROR_MESSAGES: Record<number, string> = {
  1100: 'Không tìm thấy khóa học.',
  6102: 'Đối tượng nhận không hợp lệ.',
  6103: 'Bạn chỉ được gửi thông báo cho học viên khóa học của mình.',
  6104: 'Không có người nhận nào phù hợp.',
}

const toErrorMessage = (error: AxiosError<ApiResponse<unknown>>) => {
  const code = error.response?.data?.code
  return (code != null ? ERROR_MESSAGES[code] : undefined) ?? error.response?.data?.message ?? 'Vui lòng thử lại.'
}

/** Số người sẽ nhận — tắt khi chưa chọn đủ đối tượng. */
export function useRecipientCount(params: RecipientCountParams | null) {
  return useQuery<number>({
    queryKey: params ? QUERY_KEYS.adminBroadcast.recipientCount({ ...params }) : QUERY_KEYS.adminBroadcast.recipientCountDisabled,
    queryFn: async () => {
      if (!params) return 0
      const res = await adminBroadcastService.countRecipients(params)
      return res.data ?? 0
    },
    enabled: !!params,
  })
}

export function useBroadcastHistory(page: number, size = 10) {
  return useQuery<PageResponse<Broadcast>>({
    queryKey: QUERY_KEYS.adminBroadcast.history(page, size),
    queryFn: async () => {
      const res = await adminBroadcastService.getHistory({ page, size })
      return res.data ?? { page: 1, size, totalElements: 0, totalPages: 0, items: [] }
    },
    placeholderData: keepPreviousData,
  })
}

export function useSendBroadcast() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: BroadcastPayload) => adminBroadcastService.send(payload),
    onSuccess: (res) => {
      toast.success(res.message ?? 'Đã gửi thông báo')
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminBroadcast.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Gửi thông báo thất bại', { description: toErrorMessage(error) })
    },
  })
}
