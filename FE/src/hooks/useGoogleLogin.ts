import { useCallback, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  decodeJwt,
  extractEmailFromToken,
  extractRolesFromToken,
  extractUserIdFromToken,
} from '@/utils/jwt'
import type { ApiResponse, LoginResponse, User } from '@/types'

// ============================================================================
// Types
// ============================================================================

interface UseGoogleLoginOptions {
  redirectTo?: string
}

/**
 * Shape tối thiểu của Google Identity Services mà hook cần dùng.
 * Không khai báo `declare global` để tránh xung đột với khai báo `Window.google`
 * ở các file khác (vd: LoginPage) — ta dùng cast cục bộ thay vì augment global.
 */
interface GoogleIdentityServices {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string
        callback: (response: { credential: string }) => void
      }) => void
      prompt: (
        notificationCallback?: (notification: {
          isNotDisplayed: () => boolean
          isSkippedMoment: () => boolean
        }) => void,
      ) => void
      renderButton: (
        parent: HTMLElement,
        options: {
          type?: 'standard' | 'icon'
          theme?: 'outline' | 'filled_blue' | 'filled_black'
          size?: 'large' | 'medium' | 'small'
          text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin'
          shape?: 'rectangular' | 'pill' | 'circle' | 'square'
          width?: number
        },
      ) => void
    }
  }
}

interface GoogleGlobal {
  google?: GoogleIdentityServices
}

function getWindowGoogle(): GoogleGlobal {
  return window as unknown as GoogleGlobal
}

// ============================================================================
// Google Identity Services loader
// ============================================================================

const GSI_SCRIPT_ID = 'google-gsi-script'
const GSI_SCRIPT_SRC = 'https://accounts.google.com/gsi/client'
const DEFAULT_GOOGLE_CLIENT_ID =
  '142602742944-vskbcvgq85k9kgl1469k0f5k2d5h73m0.apps.googleusercontent.com'

function getGoogleClientId(): string {
  return import.meta.env.VITE_GOOGLE_CLIENT_ID || DEFAULT_GOOGLE_CLIENT_ID
}

/** Inject script GSI (idempotent) và chờ tới khi `window.google.accounts.id` sẵn sàng. */
function loadGoogleIdentityServices(): Promise<GoogleIdentityServices> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') {
      reject(new Error('Google Identity Services chỉ chạy trong trình duyệt.'))
      return
    }

    const w = getWindowGoogle()
    if (w.google?.accounts?.id) {
      resolve(w.google)
      return
    }

    if (!document.getElementById(GSI_SCRIPT_ID)) {
      const script = document.createElement('script')
      script.id = GSI_SCRIPT_ID
      script.src = GSI_SCRIPT_SRC
      script.async = true
      script.defer = true
      document.body.appendChild(script)
    }

    const startedAt = Date.now()
    const poll = () => {
      const current = getWindowGoogle()
      if (current.google?.accounts?.id) {
        resolve(current.google)
        return
      }
      if (Date.now() - startedAt > 10000) {
        reject(new Error('Timeout khi tải Google Identity Services.'))
        return
      }
      setTimeout(poll, 50)
    }
    poll()
  })
}

/**
 * Dự phòng khi One Tap không hiển thị (trình duyệt chặn, user từng chặn, v.v.):
 * render nút GSI ở ngoài màn hình rồi tự click.
 *
 * Re-initialize GSI với callback riêng: khi GSI trả credential (do click
 * nút vừa render), callback này resolve promise với `idToken`.
 */
function clickFallbackGoogleButton(
  gsi: GoogleIdentityServices,
  clientId: string,
): Promise<string> {
  return new Promise((resolve, reject) => {
    const host = document.createElement('div')
    host.style.cssText =
      'position:fixed;left:-10000px;top:0;width:320px;height:60px;'
    document.body.appendChild(host)

    const cleanup = () => host.remove()

    gsi.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => {
        if (response.credential) {
          resolve(response.credential)
          setTimeout(cleanup, 0)
        } else {
          cleanup()
          reject(new Error('Không nhận được credential từ Google.'))
        }
      },
    })

    try {
      gsi.accounts.id.renderButton(host, {
        type: 'standard',
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
      })
    } catch (e) {
      cleanup()
      reject(e instanceof Error ? e : new Error('Không render được nút Google.'))
      return
    }

    const tryClick = (attempt = 0) => {
      const button = host.querySelector('[role="button"]') as HTMLElement | null
      if (button) {
        button.click()
        return
      }
      if (attempt > 40) {
        cleanup()
        reject(new Error('Không mở được hộp thoại đăng nhập Google.'))
        return
      }
      setTimeout(() => tryClick(attempt + 1), 50)
    }
    tryClick()
  })
}

/**
 * Lấy `idToken` từ Google mà không cần user tương tác trực tiếp:
 *  1. Ưu tiên One Tap (`accounts.id.prompt`).
 *  2. Nếu One Tap không hiển thị → fallback render + click nút GSI.
 */
async function requestGoogleIdToken(
  gsi: GoogleIdentityServices,
  clientId: string,
): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    let settled = false

    const finish = (credential: string) => {
      if (settled) return
      settled = true
      resolve(credential)
    }

    const fail = (message: string) => {
      if (settled) return
      settled = true
      reject(new Error(message))
    }

    gsi.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => {
        if (response.credential) {
          finish(response.credential)
        } else {
          fail('Không nhận được credential từ Google.')
        }
      },
    })

    try {
      gsi.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          // One Tap không dùng được → thử nút GSI dự phòng.
          clickFallbackGoogleButton(gsi, clientId).then(finish, (e) =>
            fail(
              e instanceof Error
                ? e.message
                : 'Không thể đăng nhập bằng Google.',
            ),
          )
        }
      })
    } catch {
      clickFallbackGoogleButton(gsi, clientId).then(finish, (e) =>
        fail(e instanceof Error ? e.message : 'Không thể đăng nhập bằng Google.'),
      )
    }
  })
}

// ============================================================================
// useGoogleLogin
// ============================================================================

/**
 * Hook đăng nhập bằng Google — hỗ trợ cả 2 kiểu gọi:
 *
 * 1. **Kiểu mutation** (trang `/login` cũ):
 *    ```tsx
 *    const { mutate, isPending } = useGoogleLogin({ redirectTo })
 *    mutate(idToken)   // idToken lấy từ Google Identity Services
 *    ```
 *
 * 2. **Kiểu nút bấm** (UI mới — `AuthModal` vẽ nút Google tự vẽ):
 *    ```tsx
 *    const { startGoogleLogin } = useGoogleLogin()
 *    startGoogleLogin()  // tự mở Google, lấy idToken, đổi access token
 *    ```
 *
 * Cả hai đều dùng chung endpoint `POST /auth/google` của BE (nhận `idToken`),
 * nên không phụ thuộc vào việc BE có bật luồng OAuth2 redirect hay không.
 *
 * Lý do cần 2 hook (useGoogleLogin + useGoogleCallback):
 *   - useGoogleLogin: gọi khi click (ở modal) hoặc khi GSI trả idToken.
 *   - useGoogleCallback: xử lý token từ URL khi BE redirect về
 *     (dùng cho luồng OAuth2 redirect nếu BE bật endpoint đó).
 */
export function useGoogleLogin(options: UseGoogleLoginOptions = {}) {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  // Đích điều hướng override theo từng lần gọi (AuthModal lưu trong
  // sessionStorage trước khi bấm nút Google).
  const redirectOverride = useRef<string | null>(null)

  const mutation = useMutation({
    mutationFn: async (idToken: string) => {
      return authService.googleLogin(idToken)
    },
    onSuccess: (res) => {
      const data: LoginResponse | undefined = res?.data
      if (!data?.accessToken) {
        toast.error('Đăng nhập Google thất bại', {
          description: data?.error || 'Phản hồi không hợp lệ từ máy chủ.',
        })
        return
      }

      const scopeRoles = extractRolesFromToken(data.accessToken)
      const userId = extractUserIdFromToken(data.accessToken)
      const email = extractEmailFromToken(data.accessToken)

      const user: User = {
        id: userId ?? 0,
        email,
        fullName: '',
        role: scopeRoles[0] ?? 'STUDENT',
        roles: scopeRoles,
      }

      setAuth({
        user,
        accessToken: data.accessToken,
        refreshToken: null,
        remember: true,
      })

      toast.success('Đăng nhập Google thành công', {
        description: 'Chào mừng bạn đến với ElearningJP!',
      })

      const redirectTo = redirectOverride.current || options.redirectTo || '/'
      redirectOverride.current = null
      navigate(redirectTo, { replace: true })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ||
        'Đăng nhập Google thất bại. Vui lòng thử lại.'
      toast.error('Đăng nhập Google thất bại', { description: msg })
    },
  })

  const [isGoogleLoading, setIsGoogleLoading] = useState(false)

  /**
   * Bắt đầu luồng đăng nhập Google từ nút bấm (không cần GSI render sẵn).
   */
  const startGoogleLogin = useCallback(async () => {
    setIsGoogleLoading(true)
    try {
      const clientId = getGoogleClientId()
      const gsi = await loadGoogleIdentityServices()
      const idToken = await requestGoogleIdToken(gsi, clientId)

      // Ưu tiên đích điều hướng do AuthModal lưu trong sessionStorage.
      try {
        redirectOverride.current =
          sessionStorage.getItem('elearning_google_redirect') || null
      } catch {
        redirectOverride.current = null
      }

      await mutation.mutateAsync(idToken)
    } catch (error) {
      // Lỗi HTTP đã được `onError` xử lý; chỉ toast các lỗi phía GSI.
      if (!(error instanceof AxiosError)) {
        toast.error('Đăng nhập Google thất bại', {
          description:
            error instanceof Error
              ? error.message
              : 'Không thể kết nối với Google. Vui lòng thử lại.',
        })
      }
    } finally {
      setIsGoogleLoading(false)
    }
  }, [mutation])

  return {
    // --- API kiểu mutation (trang /login cũ) ---
    mutate: mutation.mutate,
    isPending: mutation.isPending,

    // --- API kiểu nút bấm (UI mới — AuthModal) ---
    startGoogleLogin,
    isLoading: isGoogleLoading || mutation.isPending,
  }
}

/**
 * Hook xử lý khi BE redirect về FE với ?token=...
 * Trả về object chứa state + handler để route có thể consume.
 */
export function useGoogleCallback() {
  const setAuth = useAuthStore((s) => s.setAuth)

  const handle = useCallback(
    (token: string | null): { ok: boolean; error?: string } => {
      if (!token) {
        return { ok: false, error: 'Không nhận được token từ Google.' }
      }

      const payload = decodeJwt(token)
      const roles = extractRolesFromToken(token)
      const userId = extractUserIdFromToken(token)
      const email = extractEmailFromToken(token) || payload?.sub || ''

      if (!userId && roles.length === 0) {
        // Token không hợp lệ / không đọc được
        return { ok: false, error: 'Token Google không hợp lệ.' }
      }

      const user: User = {
        id: userId ?? 0,
        email,
        fullName: '',
        role: roles[0] ?? 'STUDENT',
        roles,
      }

      // Google flow: mặc định ghi nhớ đăng nhập.
      const remember = true

      setAuth({
        user,
        accessToken: token,
        refreshToken: null,
        remember,
      })

      toast.success('Đăng nhập Google thành công', {
        description: 'Chào mừng bạn quay lại với ElearningJP!',
      })

      return { ok: true }
    },
    [setAuth],
  )

  return { handle }
}
