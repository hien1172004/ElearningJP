import type { ReportReason } from '@/types'

/** Nhãn tiếng Việt cho lý do báo cáo vi phạm — dùng ở modal báo cáo và trang kiểm duyệt. */
export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  SPAM: 'Spam / quảng cáo',
  OFFENSIVE: 'Ngôn từ xúc phạm, thiếu văn hóa',
  WRONG_INFO: 'Thông tin sai lệch',
  OFF_TOPIC: 'Không liên quan đến bài học',
  OTHER: 'Lý do khác',
}
