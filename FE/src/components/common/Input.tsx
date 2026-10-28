import React, { forwardRef } from 'react'
import { Input as AntdInput } from 'antd'
import type { InputProps as AntdInputProps, InputRef } from 'antd'
import { clsx } from 'clsx'

export interface InputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size' | 'prefix'> {
  label?: string
  error?: string
  hint?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
  containerClassName?: string
  size?: 'sm' | 'md' | 'lg'
}

const sizeToAntd: Record<NonNullable<InputProps['size']>, AntdInputProps['size']> =
  {
    sm: 'small',
    md: 'middle',
    lg: 'large',
  }

/**
 * Input dự án — wrap antd Input.
 * Giữ nguyên API: label / error / hint / leftIcon / rightIcon / isPassword (auto từ type).
 * Vẫn forward ref xuống antd InputRef.
 */
export const Input = forwardRef<InputRef, InputProps>(
  (
    {
      label,
      error,
      hint,
      leftIcon,
      rightIcon,
      type = 'text',
      containerClassName,
      className,
      disabled,
      id,
      size = 'md',
      ...rest
    },
    ref,
  ) => {
    const generatedId = React.useId()
    const inputId = id ?? generatedId

    return (
      <div className={clsx('w-full', containerClassName)}>
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-semibold text-gray-700 mb-1.5"
          >
            {label}
          </label>
        )}
        <AntdInput
          ref={ref}
          id={inputId}
          type={type}
          disabled={disabled}
          status={error ? 'error' : undefined}
          prefix={leftIcon ? <span className="inline-flex">{leftIcon}</span> : undefined}
          suffix={rightIcon ? <span className="inline-flex">{rightIcon}</span> : undefined}
          size={sizeToAntd[size]}
          className={className}
          {...(rest as unknown as AntdInputProps)}
        />
        {error ? (
          <p className="mt-1 text-xs font-medium text-rose-600">{error}</p>
        ) : hint ? (
          <p className="mt-1 text-xs text-gray-500">{hint}</p>
        ) : null}
      </div>
    )
  },
)

Input.displayName = 'Input'

/**
 * PasswordInput — wrap antd Input.Password.
 * Giữ nguyên tên "type='password'" ở các page cũ: nếu dùng type="password" thì
 * bên trong Input ta chuyển sang AntdInput.Password để có nút toggle mắt.
 */
const PasswordInput = forwardRef<InputRef, Omit<InputProps, 'type' | 'rightIcon'>>(
  (props, ref) => {
    return (
      <Input
        ref={ref}
        type="password"
        {...props}
        rightIcon={undefined}
      />
    )
  },
)
PasswordInput.displayName = 'PasswordInput'

/**
 * Tiện ích: thay vì tạo component mới, ta xuất luôn `Password`
 * từ antd để các page muốn dùng có sẵn toggle mắt.
 */
export { PasswordInput }
