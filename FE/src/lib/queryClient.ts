import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
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
