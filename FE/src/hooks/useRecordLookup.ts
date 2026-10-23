import { useCallback, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { useAuthStore } from '@/stores/useAuthStore'
import type { DictionaryLookupPayload, LookupTargetType } from '@/types'

/**
 * useRecordLookup — ghi nhận 1 lần tra từ điển (Bước 2).
 *
 * ## Vì sao cần hook riêng?
 *
 * Người dùng có thể tra cùng 1 từ liên tục (vd: mở/đóng panel nhiều lần).
 * Nếu gọi API mỗi lần → spam request. Hook này sẽ:
 *
 * 1. **Chỉ ghi 1 lần cho mỗi target trong 1 khoảng thời gian** (`dedupeWindowMs`).
 * 2. Chỉ gửi khi user đã đăng nhập.
 * 3. Fire-and-forget — không đợi response, không throw.
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
export interface UseRecordLookupOptions {
  /** Trong bao lâu thì coi 2 lần tra cùng 1 target là trùng nhau (ms). Mặc định 60_000. */
  dedupeWindowMs?: number
}

export function useRecordLookup(options: UseRecordLookupOptions = {}) {
  const { dedupeWindowMs = 60_000 } = options
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const queryClient = useQueryClient()

  // key = `${targetType}:${targetId ?? sourceRef}` → timestamp gần nhất
  const lastRecordedRef = useRef<Map<string, number>>(new Map())

  return useCallback(
    (payload: DictionaryLookupPayload) => {
      if (!isAuthenticated) return
      if (!payload.targetType) return

      // Khoá trùng lặp: cùng target trong window → bỏ qua
      const key = `${payload.targetType}:${
        payload.targetId ?? payload.sourceRef ?? 'unknown'
      }`
      const now = Date.now()
      const last = lastRecordedRef.current.get(key)
      if (last && now - last < dedupeWindowMs) {
        return
      }
      lastRecordedRef.current.set(key, now)

      // Dọn bản ghi cũ để Map không phình vô hạn
      if (lastRecordedRef.current.size > 200) {
        const cutoff = now - dedupeWindowMs * 2
        for (const [k, t] of lastRecordedRef.current) {
          if (t < cutoff) lastRecordedRef.current.delete(k)
        }
      }

      // Fire-and-forget — lỗi không ảnh hưởng UX
      progressService
        .recordLookup({
          targetType: payload.targetType as LookupTargetType,
          targetId: payload.targetId ?? null,
          sourceRef: payload.sourceRef ?? null,
        })
        .then(() => {
          // Đã ghi nhận 1 từ → số liệu Progress thay đổi → invalidate cache
          // để lần mở trang Progress kế tiếp hiển thị số mới nhất.
          void queryClient.invalidateQueries({ queryKey: ['progress'] })
        })
        .catch(() => undefined)
    },
    [isAuthenticated, dedupeWindowMs, queryClient],
  )
}