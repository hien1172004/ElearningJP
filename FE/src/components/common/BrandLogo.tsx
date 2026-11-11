import React from 'react'
import { Link } from 'react-router-dom'

interface BrandLogoProps {
  /** Chỉ hiển thị biểu tượng con dấu Hanko (không kèm chữ) */
  iconOnly?: boolean
  /** Ẩn chữ trên màn hình nhỏ (< 640px) */
  hideTextOnMobile?: boolean
  /** Kích thước: 'sm' (header nhỏ/mobile), 'md' (TopBar mặc định), 'lg' (trang đăng nhập/modal) */
  size?: 'sm' | 'md' | 'lg'
  /** Có bọc thẻ Link về trang chủ '/' hay không. Mặc định: true */
  withLink?: boolean
  /** Variant giao diện: 'default' (nền sáng thông thường) | 'light' (dùng trên nền tối/banner xanh indigo đậm) */
  variant?: 'default' | 'light'
  /** Sự kiện click (ví dụ đóng menu mobile) */
  onClick?: () => void
  className?: string
  textClassName?: string
}

/**
 * Biểu tượng con dấu Hanko & Vầng thái dương (Hinomaru)
 * Trích xuất chuẩn xác 100% từ mẫu thiết kế Mẫu 1 (Hanko & Hinomaru)
 */
export const HankoSealIcon: React.FC<{
  sizePx?: number
  className?: string
  variant?: 'default' | 'light'
}> = ({ sizePx = 34, className = '', variant = 'default' }) => {
  return (
    <img
      src="/logo-seal.png"
      alt="ElearningJP Seal"
      width={sizePx}
      height={sizePx}
      className={`shrink-0 select-none object-contain rounded-full transition-transform duration-200 group-hover:scale-105 ${
        variant === 'light' ? 'drop-shadow-md' : 'drop-shadow-xs'
      } ${className}`}
      style={{ width: `${sizePx}px`, height: `${sizePx}px` }}
      loading="eager"
      decoding="async"
      draggable={false}
    />
  )
}

/**
 * Brand Logo chính thức của ElearningJP — Phong cách "Hanko & Hinomaru" (Mẫu 1)
 */
export const BrandLogo: React.FC<BrandLogoProps> = ({
  iconOnly = false,
  hideTextOnMobile = false,
  size = 'md',
  withLink = true,
  variant = 'default',
  onClick,
  className = '',
  textClassName = '',
}) => {
  const isLight = variant === 'light'

  const sizeConfig = {
    sm: { icon: 26, text: 'text-base sm:text-lg', gap: 'gap-2' },
    md: { icon: 34, text: 'text-xl sm:text-2xl', gap: 'gap-2.5' },
    lg: { icon: 44, text: 'text-2xl sm:text-3xl', gap: 'gap-3' },
  }[size]

  const textClasses = [
    'font-bold',
    sizeConfig.text,
    'leading-none',
    hideTextOnMobile ? 'hidden sm:inline-flex' : 'inline-flex',
    'items-baseline',
    'tracking-tight',
    textClassName,
  ]
    .filter(Boolean)
    .join(' ')

  const content = (
    <div
      className={`inline-flex items-center ${sizeConfig.gap} select-none ${className}`}
    >
      <HankoSealIcon sizePx={sizeConfig.icon} variant={variant} />
      {!iconOnly && (
        <span className={textClasses}>
          <span
            className={
              isLight
                ? 'text-white tracking-tight'
                : 'text-[#12304f] dark:text-slate-100 tracking-tight'
            }
          >
            Elearning
          </span>
          <span
            className={`font-bold ml-0.5 ${
              isLight ? 'text-[#ff6b6b]' : 'text-[#be3933]'
            }`}
          >
            JP
          </span>
        </span>
      )}
    </div>
  )

  if (withLink) {
    return (
      <Link
        to="/"
        onClick={onClick}
        className="group inline-flex items-center hover:opacity-95 transition-opacity cursor-pointer"
        title="ElearningJP - Trang chủ"
      >
        {content}
      </Link>
    )
  }

  return (
    <div onClick={onClick} className={onClick ? 'cursor-pointer' : undefined}>
      {content}
    </div>
  )
}

export default BrandLogo
