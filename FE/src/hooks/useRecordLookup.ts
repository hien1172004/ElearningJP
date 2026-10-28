import { useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { useAuthStore } from '@/stores/useAuthStore'
import type { DictionaryLookupPayload, LookupTargetType } from '@/types'

/**
 * useRecordLookup — ghi nhận 1 lần tra từ điển (Bước 2).
 *
 * ## Vì sao cần hook riêng?
 *
 * Gộp 3 điều kiện gọi API lạc lõng ở nhiều chỗ:
 *
 * 1. Chỉ gửi khi user đã đăng nhập.
 * 2. Fire-and-forget — không đợi response, không throw (lỗi mạng không được
 *    làm hỏng UX khi user đang tra từ).
 * 3. Invalidate cache Progress sau khi ghi nhận, để lần mở trang Progress
 *    kế tiếp hiển thị số liệu mới nhất.
 *
 * ## Mỗi lần tra đều được ghi — KHÔNG dedupe ở client
 *
 * Trước đây hook có cửa sổ 60 giây: cùng 1 từ trong 60s chỉ ghi 1 lần.
 * Điều đó làm sai số liệu "Số lần tra từ" (thực tế user đã tra 5 lần thì
 * chỉ ghi 1).
 *
 * Nay **không còn dedupe window**:
 *
 * - `lookups` (Số lần tra từ) = đếm **mọi** lần tra → phải ghi đủ.
 * - `new_items_learned` (Từ đã học) = chỉ đếm lần **đầu tiên** → BE tự
 *   chống trùng qua bảng `user_learned_items` (UNIQUE + ON CONFLICT DO NOTHING).
 *
 * Tách việc chống trùng sang BE là đúng chỗ hơn: BE mới là nơi duy nhất biết
 * "mục này đã học chưa", và làm thế client không thể vô tình bỏ sót lượt tra
 * (vd: user tra 1 từ, đóng tab, mở lại sau 5 phút → vẫn phải cộng lượt tra).
 *
 * ```tsx
 * const recordLookup = useRecordLookup()
 *
 * // Khi mở chi tiết 1 từ vựng
 * recordLookup({ targetType: 'VOCAB', targetId: vocab.id })
 *
 * // Khi mở chi tiết 1 chữ Hán (lưu ký tự vào sourceRef)
 * recordLookup({ targetType: 'KANJI', sourceRef: '日本語' })
 * ```
 */
export function useRecordLookup() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const queryClient = useQueryClient()

  return useCallback(
    (payload: DictionaryLookupPayload) => {
      if (!isAuthenticated) return
      if (!payload.targetType) return

      // Fire-and-forget — lỗi không ảnh hưởng UX
      progressService
        .recordLookup({
          targetType: payload.targetType as LookupTargetType,
          targetId: payload.targetId ?? null,
          sourceRef: payload.sourceRef ?? null,
        })
        .then(() => {
          // Đã ghi nhận 1 lượt tra → số liệu Progress thay đổi → invalidate cache
          // để lần mở trang Progress kế tiếp hiển thị số mới nhất.
          void queryClient.invalidateQueries({ queryKey: ['progress'] })
        })
        .catch(() => undefined)
    },
    [isAuthenticated, queryClient],
  )
}
