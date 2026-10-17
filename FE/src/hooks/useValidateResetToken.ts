import { useQuery } from '@tanstack/react-query'
import { authService } from '@/services/auth.service'

export function useValidateResetToken(token: string | null) {
  return useQuery({
    queryKey: ['auth', 'validate-reset-token', token],
    enabled: !!token,
    queryFn: async () => {
      const res = await authService.validateResetToken(token as string)
      return res?.data === true
    },
    staleTime: 0,
    retry: 0,
  })
}
