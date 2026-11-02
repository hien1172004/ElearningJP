import { useMutation, useQueryClient } from '@tanstack/react-query'
import { dictionaryService } from '@/services'
import type { SrsDueItem } from '@/types'

export type SrsTargetType = 'VOCABULARY' | 'KANJI' | 'GRAMMAR'

/**
 * useAddToSrs — thêm 1 mục (từ vựng / chữ Hán / ngữ pháp) vào sổ tay SRS.
 *
 * Sau khi thêm thành công, invalidate toàn bộ query SRS (`stats` / `due` / `items`)
 * để badge số thẻ đến hạn trên Sidebar và các màn hình liên quan cập nhật ngay,
 * không phải chờ hết `staleTime` (30s).
 */
export function useAddToSrs() {
  const queryClient = useQueryClient()
  return useMutation<
    SrsDueItem | undefined,
    unknown,
    { type: SrsTargetType; id: number }
  >({
    mutationFn: async ({ type, id }) => {
      const res = await dictionaryService.addToSrs(type, id)
      return res?.data as SrsDueItem | undefined
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['srs'] })
    },
  })
}
