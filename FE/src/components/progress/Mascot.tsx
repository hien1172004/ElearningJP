import React from 'react'
import { cn } from '@/utils/cn'

/**
 * Mascot — placeholder dùng emoji + lucide-react icon.
 *
 * Quyết định đã chốt ở plan: **placeholder**, chưa cần asset thật.
 * Sau này có thể thay bằng file PNG/SVG mà không đổi API của component.
 */
export type MascotMood =
  | 'happy' // 😄 đạt mục tiêu
  | 'proud' // 🎉 vượt mục tiêu
  | 'neutral' // 🙂 bình thường
  | 'sleepy' // 😴 chưa học gì hôm nay
  | 'cheer' // 📣 động viên

const MOOD_MAP: Record<MascotMood, { emoji: string; label: string }> = {
  happy: { emoji: '😄', label: 'Hôm nay bạn đã đạt mục tiêu!' },
  proud: { emoji: '🎉', label: 'Wow, bạn vượt mục tiêu rồi!' },
  neutral: { emoji: '🙂', label: 'Cùng học thêm một chút nhé!' },
  sleepy: { emoji: '😴', label: 'Hôm nay chưa học gì. Bắt đầu thôi!' },
  cheer: { emoji: '📣', label: 'Kiên trì là chìa khóa!' },
}

export interface MascotProps {
  mood?: MascotMood
  /** Kích thước box (px). */
  size?: number
  className?: string
  /** Hiện dòng chữ động viên bên dưới. */
  showMessage?: boolean
  message?: string
}

export const Mascot: React.FC<MascotProps> = ({
  mood = 'neutral',
  size = 72,
  className,
  showMessage = false,
  message,
}) => {
  const cfg = MOOD_MAP[mood]
  const text = message ?? cfg.label

  return (
    <div
      className={cn('flex flex-col items-center gap-2', className)}
      role="img"
      aria-label={text}
    >
      <div
        className="rounded-full bg-gradient-to-br from-indigo-100 to-sky-100 flex items-center justify-center shadow-sm"
        style={{ width: size, height: size }}
      >
        <span
          className="leading-none select-none"
          style={{ fontSize: Math.round(size * 0.55) }}
          aria-hidden="true"
        >
          {cfg.emoji}
        </span>
      </div>
      {showMessage && (
        <p className="text-sm text-gray-600 font-medium text-center max-w-xs">
          {text}
        </p>
      )}
    </div>
  )
}

export default Mascot