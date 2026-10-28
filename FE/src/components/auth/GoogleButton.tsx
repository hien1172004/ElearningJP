import React from 'react'
import { cn } from '@/utils/cn'

interface GoogleButtonProps {
  onClick: () => void
  loading?: boolean
  label?: string
  disabled?: boolean
  className?: string
}

/**
 * Google "G" logo chính hãng (multi-color, không cần Google Identity Services).
 * Dùng cho cả 2 flow: đăng nhập / đăng ký với Google.
 */
const GoogleG: React.FC<{ className?: string }> = ({ className }) => (
  <svg
    className={className}
    width="18"
    height="18"
    viewBox="0 0 48 48"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      fill="#FFC107"
      d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"
    />
    <path
      fill="#FF3D00"
      d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"
    />
    <path
      fill="#4CAF50"
      d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"
    />
    <path
      fill="#1976D2"
      d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"
    />
  </svg>
)

/**
 * Nút "Đăng nhập/Đăng ký với Google".
 *
 * Hành vi:
 *  - onClick: trigger OAuth flow (redirect tới BE)
 *  - loading: disable + spinner overlay
 *  - Có hover/focus/active states theo Google brand guidelines (light theme)
 */
export const GoogleButton: React.FC<GoogleButtonProps> = ({
  onClick,
  loading = false,
  label = 'Tiếp tục với Google',
  disabled = false,
  className,
}) => {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled || loading}
      className={cn(
        'w-full h-11 inline-flex items-center justify-center gap-3',
        'rounded-md border border-gray-300 bg-white',
        'text-sm font-semibold text-gray-700',
        'shadow-sm transition-all',
        'hover:bg-gray-50 hover:border-gray-400 hover:shadow',
        'active:scale-[0.99]',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2',
        'disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-white',
        className,
      )}
    >
      {loading ? (
        <span
          className="inline-block w-4 h-4 border-2 border-gray-300 border-t-indigo-600 rounded-full animate-spin"
          aria-hidden="true"
        />
      ) : (
        <GoogleG className="shrink-0" />
      )}
      <span>{loading ? 'Đang chuyển hướng…' : label}</span>
    </button>
  )
}
