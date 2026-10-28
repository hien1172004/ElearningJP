import { useQuery } from '@tanstack/react-query'
import { dictionaryService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { KanjiDetail } from '@/types'

/**
 * Lấy chi tiết Hán tự từ từ điển (kèm bộ nét vẽ SVG chuẩn KanjiVG).
 *
 * Dùng cho tính năng "Luyện viết AI" trong phần Kanji của bài học:
 * `LessonKanjiItem` không chứa field `strokes`, nên cần gọi endpoint
 *   GET /api/v1/dictionary/kanji/{character}
 * để lấy `strokes` làm nét mờ chuẩn cho modal `KanjiPracticeModal`.
 *
 * @param character Ký tự Hán cần tra (ví dụ: '学'). Rỗng/undefined → hook không gọi API.
 * @param enabled   Cho phép tắt hook thủ công (VD: chỉ fetch khi user mở modal).
 */
export function useKanjiDetail(character: string | undefined, enabled = true) {
  const char = character?.trim() ?? ''
  const isEnabled = enabled && char.length > 0

  return useQuery<KanjiDetail | null>({
    queryKey: isEnabled ? QUERY_KEYS.dictionary.kanji(char) : ['dictionary', 'kanji', 'disabled'],
    queryFn: async () => {
      const res = await dictionaryService.getKanjiDetail(char)
      return res.data ?? null
    },
    enabled: isEnabled,
    // Nét chuẩn hiếm khi thay đổi → cache lâu để đổi trang chữ Hán không gọi lại API.
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    retry: (failureCount, error: any) => {
      const status = error?.response?.status
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}
