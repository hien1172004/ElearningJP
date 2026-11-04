import { useMutation, useQueryClient } from '@tanstack/react-query'
import { srsService } from '@/services/srs.service'

/**
 * useArchiveSrsItem / useRestoreSrsItem — xoá (lưu trữ) và khôi phục thẻ SRS.
 * Invalidate toàn bộ prefix `['srs']` (items / stats / due / forecast / saved-ids)
 * để danh sách, badge và nút "Thêm vào SRS" ở từ điển cập nhật ngay.
 */
export function useArchiveSrsItem() {
  const queryClient = useQueryClient()
  return useMutation<unknown, unknown, number>({
    mutationFn: (id) => srsService.archiveItem(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['srs'] })
    },
  })
}

export function useRestoreSrsItem() {
  const queryClient = useQueryClient()
  return useMutation<unknown, unknown, number>({
    mutationFn: (id) => srsService.restoreItem(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['srs'] })
    },
  })
}
