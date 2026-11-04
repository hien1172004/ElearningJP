import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { commentService } from '@/services'
import type { ApiResponse, CommentReportPayload } from '@/types'

/** Mã lỗi BE → thông báo tiếng Việt. */
const ERROR_MESSAGES: Record<number, string> = {
  4000: 'Bình luận không còn tồn tại.',
  4004: 'Trả lời không còn tồn tại.',
  4007: 'Bạn đã báo cáo nội dung này rồi.',
  4008: 'Bạn không thể báo cáo nội dung của chính mình.',
}

/** Báo cáo vi phạm một bình luận (`kind = 'comment'`) hoặc trả lời (`kind = 'reply'`). */
export function useReportComment() {
  return useMutation({
    mutationFn: (vars: { kind: 'comment' | 'reply'; id: number; payload: CommentReportPayload }) =>
      vars.kind === 'reply'
        ? commentService.reportReply(vars.id, vars.payload)
        : commentService.reportComment(vars.id, vars.payload),
    onSuccess: () => {
      toast.success('Đã gửi báo cáo', {
        description: 'Cảm ơn bạn! Người kiểm duyệt sẽ xem xét nội dung này.',
      })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const code = error.response?.data?.code
      toast.error('Không gửi được báo cáo', {
        description:
          (code != null ? ERROR_MESSAGES[code] : undefined) ??
          error.response?.data?.message ??
          'Vui lòng thử lại.',
      })
    },
  })
}
