import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { adminScenarioService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  AdminScenario,
  AdminScenarioListParams,
  AdminScenarioPayload,
  ApiResponse,
  PageResponse,
} from '@/types'

const EMPTY_PAGE: PageResponse<AdminScenario> = {
  page: 1,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  items: [],
}

/** Mã lỗi BE (ErrorCode) → thông báo tiếng Việt. */
const ERROR_MESSAGES: Record<number, string> = {
  6001: 'Không tìm thấy kịch bản.',
  6004: 'Bạn chỉ được quản lý kịch bản do chính mình tạo.',
}

export const toScenarioErrorMessage = (
  error: AxiosError<ApiResponse<unknown>>,
  fallback: string,
): string => {
  const code = error.response?.data?.code
  return (
    (code != null ? ERROR_MESSAGES[code] : undefined) ?? error.response?.data?.message ?? fallback
  )
}

/** Danh sách kịch bản cho trang quản lý (TEACHER chỉ nhận kịch bản của mình). */
export function useAdminScenarios(params: AdminScenarioListParams) {
  return useQuery<PageResponse<AdminScenario>>({
    queryKey: QUERY_KEYS.adminScenario.list({ ...params }),
    queryFn: async () => {
      const res = await adminScenarioService.getScenarios(params)
      return res.data ?? EMPTY_PAGE
    },
    placeholderData: keepPreviousData,
  })
}

/** Chi tiết một kịch bản để sửa; tắt khi chưa có id (trang tạo mới). */
export function useAdminScenario(id: number | null) {
  return useQuery<AdminScenario | null>({
    queryKey: id ? QUERY_KEYS.adminScenario.detail(id) : QUERY_KEYS.adminScenario.detailDisabled,
    queryFn: async () => {
      if (!id) return null
      const res = await adminScenarioService.getScenario(id)
      return res.data ?? null
    },
    enabled: !!id,
  })
}

export function useCreateScenario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: AdminScenarioPayload) => adminScenarioService.createScenario(payload),
    onSuccess: (res) => {
      toast.success('Đã tạo kịch bản', { description: res.data?.title })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminScenario.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Tạo kịch bản thất bại', {
        description: toScenarioErrorMessage(error, 'Vui lòng thử lại.'),
      })
    },
  })
}

export function useUpdateScenario(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: AdminScenarioPayload) => adminScenarioService.updateScenario(id, payload),
    onSuccess: (res) => {
      toast.success('Đã lưu kịch bản', { description: res.data?.title })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminScenario.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Lưu kịch bản thất bại', {
        description: toScenarioErrorMessage(error, 'Vui lòng thử lại.'),
      })
    },
  })
}

export function useDeleteScenario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => adminScenarioService.deleteScenario(id),
    onSuccess: () => {
      toast.success('Đã xóa kịch bản')
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminScenario.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Xóa kịch bản thất bại', {
        description: toScenarioErrorMessage(error, 'Vui lòng thử lại.'),
      })
    },
  })
}
