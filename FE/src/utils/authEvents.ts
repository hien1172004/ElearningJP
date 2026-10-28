/**
 * Custom event cho auth flow — dùng để axiosClient (chạy ngoài React tree)
 * giao tiếp với AuthModal (chạy trong React tree) mà không cần dependency vòng.
 *
 * Ví dụ:
 *   // axiosClient khi gặp 401 không refresh được:
 *   emitAuthExpired(currentPath)
 *
 *   // Trong AuthModal (hoặc MainLayout) lắng nghe:
 *   useEffect(() => {
 *     const handler = (e) => openLogin(e.detail.redirectTo)
 *     window.addEventListener('elearning:auth:expired', handler)
 *     return () => window.removeEventListener('elearning:auth:expired', handler)
 *   }, [openLogin])
 */

export const AUTH_EXPIRED_EVENT = 'elearning:auth:expired'

export interface AuthExpiredDetail {
  /** Path hiện tại để sau khi login quay lại */
  redirectTo: string
  /** Lý do: "expired" (token hết) | "refresh_failed" */
  reason: 'expired' | 'refresh_failed'
}

export function emitAuthExpired(detail: AuthExpiredDetail): void {
  if (typeof window === 'undefined') return
  try {
    window.dispatchEvent(
      new CustomEvent<AuthExpiredDetail>(AUTH_EXPIRED_EVENT, { detail }),
    )
  } catch {
    /* ignore */
  }
}
