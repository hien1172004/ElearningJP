import { useQuery } from '@tanstack/react-query'
import { srsService } from '@/services/srs.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { PageResponse, SrsDueItem, SrsStatus } from '@/types'

export interface UseSrsItemsParams {
  status?: SrsStatus
  page?: number
  size?: number
}

const EMPTY_PAGE: PageResponse<SrsDueItem> = {
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

/**
 * useSrsItems — danh sách toàn bộ thẻ SRS của user (phân trang + lọc trạng thái).
 * Dùng cho trang "Sổ tay SRS".
 */
export function useSrsItems(
  params: UseSrsItemsParams = {},
  options?: { enabled?: boolean },
) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const { status, page = 0, size = 20 } = params

  return useQuery<PageResponse<SrsDueItem>>({
    queryKey: QUERY_KEYS.srs.items({ status, page, size }),
    queryFn: async () => {
      const res = await srsService.getItems({ status, page, size })
      return (res?.data ?? { ...EMPTY_PAGE, page, size }) as PageResponse<SrsDueItem>
    },
    enabled: isAuthenticated && (options?.enabled ?? true),
    staleTime: 30_000,
    retry: 1,
  })
}
