import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { adminUserService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  AdminUser,
  AdminUserListParams,
  ApiResponse,
  PageResponse,
  UserStatus,
} from '@/types'

const EMPTY_PAGE: PageResponse<AdminUser> = {
  page: 1,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  items: [],
}

/** Mã lỗi BE (ErrorCode) → thông báo tiếng Việt. */
const ERROR_MESSAGES: Record<number, string> = {
  1002: 'Không tìm thấy người dùng.',
  1010: 'Vai trò không hợp lệ.',
  1016: 'Bạn không thể thay đổi trạng thái hoặc vai trò của chính mình.',
}

const toErrorMessage = (error: AxiosError<ApiResponse<unknown>>, fallback: string) => {
  const code = error.response?.data?.code
  return (code != null ? ERROR_MESSAGES[code] : undefined) ?? error.response?.data?.message ?? fallback
}

/** Danh sách người dùng cho trang quản trị (giữ dữ liệu trang cũ khi đổi trang/bộ lọc). */
export function useAdminUsers(params: AdminUserListParams) {
  return useQuery<PageResponse<AdminUser>>({
    queryKey: QUERY_KEYS.adminUser.list({ ...params }),
    queryFn: async () => {
      const res = await adminUserService.getUsers(params)
      return res.data ?? EMPTY_PAGE
    },
    placeholderData: keepPreviousData,
  })
}

export function useUpdateUserStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: UserStatus }) =>
      adminUserService.updateStatus(id, status),
    onSuccess: (res, { status }) => {
      toast.success(status === 'ACTIVE' ? 'Đã mở khóa tài khoản' : 'Đã khóa tài khoản', {
        description: res.data?.email,
      })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminUser.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Cập nhật trạng thái thất bại', {
        description: toErrorMessage(error, 'Vui lòng thử lại.'),
      })
    },
  })
}

export function useUpdateUserRoles() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, roles }: { id: number; roles: string[] }) =>
      adminUserService.updateRoles(id, roles),
    onSuccess: (res) => {
      toast.success('Đã cập nhật vai trò', {
        description: res.data
          ? `${res.data.email} cần đăng nhập lại để áp dụng quyền mới.`
          : undefined,
      })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.adminUser.all })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Cập nhật vai trò thất bại', {
        description: toErrorMessage(error, 'Vui lòng thử lại.'),
      })
    },
  })
}
