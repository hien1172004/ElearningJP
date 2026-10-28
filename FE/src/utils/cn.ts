import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Kết hợp clsx + tailwind-merge để merge class Tailwind an toàn
 * (override class trùng nhau mà không phải lo thứ tự).
 *
 * Ví dụ: cn('p-2', condition && 'p-4', 'text-red-500')
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}
