import { MutationCache, QueryClient } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { useConcurrentEditStore } from '@/stores/useConcurrentEditStore'
import { extractUnsavedEntries } from '@/lib/unsavedContent'
import { QUERY_KEYS } from '@/lib/queryKeys'

/** Mã lỗi BE `CONCURRENT_MODIFICATION`: bản ghi đã bị người khác sửa (optimistic locking). */
export const CONCURRENT_MODIFICATION_CODE = 9001

/** Mã lỗi phiên sửa bài của ADMIN (1250 ADMIN đang sửa, 1251 cần mở phiên, 1252 ADMIN khác đang sửa). */
const EDIT_LOCK_CODES = new Set([1250, 1251, 1252])

const errorCode = (error: unknown): number | undefined =>
  isAxiosError(error) ? (error.response?.data as { code?: number } | undefined)?.code : undefined

export const isConcurrentModification = (error: unknown): boolean =>
  isAxiosError(error) && (error.response?.data as { code?: number } | undefined)?.code === CONCURRENT_MODIFICATION_CODE

export const queryClient: QueryClient = new QueryClient({
  // Xung đột khi lưu (người khác vừa lưu trước): không hiện lỗi 409 mà mở modal "Tải bản mới nhất"
  // (ConcurrentEditModal), giữ nội dung vừa gửi lưu để chép lại. Hook bỏ qua toast cho lỗi này.
  mutationCache: new MutationCache({
    onError: (error, variables) => {
      if (isConcurrentModification(error)) {
        useConcurrentEditStore.getState().report(extractUnsavedEntries(variables))
      }
      // Bị chặn do phiên sửa bài (vd: phiên của ADMIN đã hết hạn / bị tiếp quản) → nút Sửa bài & banner khớp lại ngay
      const code = errorCode(error)
      if (code != null && EDIT_LOCK_CODES.has(code)) {
        void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
      }
    },
  }),
  defaultOptions: {
    queries: {
      // Ngắn (30s) để sau mỗi mutation, query đang inactive có thể tự coi là stale
      // và refetch ngay khi component mount lại → UI luôn tươi.
      // Các hook cần dữ liệu rất tĩnh (vd: me, dictionary.kana) có thể override riêng.
      staleTime: 30 * 1000,
      gcTime: 30 * 60 * 1000, // 30 phút
      retry: 0, // 401 sẽ được axios interceptor xử lý, không nên retry tự động
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: 0,
    },
  },
})
