import React from 'react'
import { Button as AntdButton } from 'antd'
import type { ButtonProps as AntdButtonProps } from 'antd'
import { clsx } from 'clsx'

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md' | 'lg'

/**
 * Mapping variant của dự án -> antd.
 * - primary   -> type="primary"
 * - secondary -> type="default"
 * - outline   -> type="default" variant="outlined"
 * - ghost     -> type="text"
 * - danger    -> type="primary" danger
 */
const variantToAntd: Record<
  ButtonVariant,
  Pick<AntdButtonProps, 'type' | 'variant' | 'danger'>
> = {
  primary: { type: 'primary', variant: 'solid' },
  secondary: { type: 'default', variant: 'solid' },
  outline: { type: 'default', variant: 'outlined' },
  ghost: { type: 'text', variant: 'text' },
  danger: { type: 'primary', variant: 'solid', danger: true },
}

const sizeToAntd: Record<ButtonSize, AntdButtonProps['size']> = {
  sm: 'small',
  md: 'middle',
  lg: 'large',
}

export interface ButtonProps
  extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  variant?: ButtonVariant
  size?: ButtonSize
  isLoading?: boolean
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
  fullWidth?: boolean
  block?: boolean
  /** Mirror với htmlType của antd */
  type?: 'button' | 'submit' | 'reset'
}

/**
 * Button dự án — wrap antd Button.
 * Giữ nguyên API cũ: variant / size / isLoading / leftIcon / rightIcon / fullWidth
 * để không phải sửa nơi gọi. Vẫn truyền `className` Tailwind để tinh chỉnh.
 */
export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  fullWidth = false,
  block,
  className,
  disabled,
  type = 'button',
  ...rest
}) => {
  // Bỏ `type` ra khỏi rest để không truyền nhầm xuống antd (antd dùng `type` cho semantic type)
  const { type: _ignoredType, ...restWithoutType } = rest as Record<
    string,
    unknown
  >

  return (
    <AntdButton
      htmlType={type}
      loading={isLoading}
      disabled={disabled || isLoading}
      icon={leftIcon}
      block={block ?? fullWidth}
      {...variantToAntd[variant]}
      size={sizeToAntd[size]}
      className={clsx(
        'font-semibold! inline-flex items-center justify-center',
        className,
      )}
      {...(restWithoutType as unknown as AntdButtonProps)}
    >
      {rightIcon ? (
        <span className="inline-flex items-center gap-2">
          <span>{children}</span>
          <span className="shrink-0 inline-flex">{rightIcon}</span>
        </span>
      ) : (
        children
      )}
    </AntdButton>
  )
}
