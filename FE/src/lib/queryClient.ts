import { MutationCache, QueryClient } from '@tanstack/react-query'
import { isAxiosError } from 'axios'

/** Mã lỗi BE `CONCURRENT_MODIFICATION`: bản ghi đã bị người khác sửa (optimistic locking). */
export const CONCURRENT_MODIFICATION_CODE = 9001

export const isConcurrentModification = (error: unknown): boolean =>
  isAxiosError(error) && (error.response?.data as { code?: number } | undefined)?.code === CONCURRENT_MODIFICATION_CODE

export const queryClient: QueryClient = new QueryClient({
  // Xung đột khi lưu (người khác vừa sửa cùng nội dung): tải lại dữ liệu đang hiển thị để người dùng
  // thấy bản mới nhất. Hook vẫn tự hiện toast lỗi (message BE); form không đóng nên giữ phần vừa nhập.
  mutationCache: new MutationCache({
    onError: (error) => {
      if (isConcurrentModification(error)) {
        void queryClient.invalidateQueries()
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
