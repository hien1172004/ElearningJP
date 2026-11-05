import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { adminModerationService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  ApiResponse,
  ModerationAction,
  ModerationBrowseParams,
  ModerationItem,
  ModerationKind,
  ModerationQueueParams,
  PageResponse,
} from '@/types'

const EMPTY_PAGE: PageResponse<ModerationItem> = {
  page: 1,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  items: [],
}

/** Mã lỗi BE (ErrorCode) → thông báo tiếng Việt. */
const ERROR_MESSAGES: Record<number, string> = {
  4000: 'Bình luận không còn tồn tại.',
  4004: 'Trả lời không còn tồn tại.',
  4009: 'Bạn chỉ được kiểm duyệt bình luận trong bài học thuộc khóa của mình.',
}

const toErrorMessage = (error: AxiosError<ApiResponse<unknown>>) => {
  const code = error.response?.data?.code
  return (
    (code != null ? ERROR_MESSAGES[code] : undefined) ??
    error.response?.data?.message ??
    'Vui lòng thử lại.'
  )
}

const SUCCESS_MESSAGES: Record<ModerationAction, string> = {
  hide: 'Đã ẩn và thông báo cho người viết',
  restore: 'Đã khôi phục',
  delete: 'Đã xóa và thông báo cho người viết',
  dismiss: 'Đã bỏ qua báo cáo',
}

/** Hàng chờ báo cáo vi phạm (gom theo bình luận). */
export function useModerationQueue(params: ModerationQueueParams) {
  return useQuery<PageResponse<ModerationItem>>({
    queryKey: QUERY_KEYS.adminModeration.queue({ ...params }),
    queryFn: async () => {
      const res = await adminModerationService.getReportQueue(params)
      return res.data ?? EMPTY_PAGE
    },
    placeholderData: keepPreviousData,
  })
}

/**
 * Tra cứu bình luận. Không truyền `kind` → BE gộp cả bình luận gốc lẫn trả lời,
 * phân trang chung theo thời gian (mới nhất trước).
 */
export function useModerationBrowse(params: ModerationBrowseParams) {
  return useQuery<PageResponse<ModerationItem>>({
    queryKey: QUERY_KEYS.adminModeration.browse({ ...params }),
    queryFn: async () => {
      const res = await adminModerationService.browse(params)
      return res.data ?? EMPTY_PAGE
    },
    placeholderData: keepPreviousData,
  })
}

/** Ẩn / khôi phục / xóa / bỏ qua — làm mới hàng chờ, tra cứu và bình luận công khai. */
export function useModerationAction() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { kind: ModerationKind; id: number; action: ModerationAction; note?: string }) =>
      adminModerationService.act(vars.kind, vars.id, vars.action, vars.note),
    onSuccess: (_res, vars) => {
      toast.success(SUCCESS_MESSAGES[vars.action])
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminModeration.all })
      // Danh sách bình luận / trả lời công khai đã đổi
      qc.invalidateQueries({ queryKey: QUERY_KEYS.comment.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Thao tác kiểm duyệt thất bại', { description: toErrorMessage(error) })
    },
  })
}
