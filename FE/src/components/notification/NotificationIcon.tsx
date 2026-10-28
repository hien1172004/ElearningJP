import React from 'react'
import {
  Award,
  CheckSquare,
  Flame,
  Layers,
  MessageCircle,
  ThumbsDown,
  ThumbsUp,
  Trophy,
  Bell,
  type LucideIcon,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import type { NotificationType } from '@/types'

interface NotificationIconProps {
  type: NotificationType
  className?: string
  /** Kích thước icon (Tailwind class, mặc định w-4 h-4). */
  iconClassName?: string
}

interface IconSpec {
  Icon: LucideIcon
  /** Class màu nền + màu icon. */
  tone: string
}

const ICONS: Record<NotificationType, IconSpec> = {
  COMMUNITY_REPLY: {
    Icon: MessageCircle,
    tone: 'bg-indigo-50 text-indigo-600',
  },
  COMMUNITY_LIKE: {
    Icon: ThumbsUp,
    tone: 'bg-emerald-50 text-emerald-600',
  },
  COMMUNITY_DISLIKE: {
    Icon: ThumbsDown,
    tone: 'bg-rose-50 text-rose-600',
  },
  SRS_DUE: { Icon: Layers, tone: 'bg-sky-50 text-sky-600' },
  STREAK_WARNING: { Icon: Flame, tone: 'bg-amber-50 text-amber-600' },
  EXAM_RESULT: { Icon: Award, tone: 'bg-emerald-50 text-emerald-600' },
  BADGE_UNLOCKED: { Icon: Trophy, tone: 'bg-yellow-50 text-yellow-600' },
  TASK_REMINDER: { Icon: CheckSquare, tone: 'bg-slate-100 text-slate-600' },
}

const FALLBACK: IconSpec = { Icon: Bell, tone: 'bg-gray-100 text-gray-600' }

/** Icon tròn theo loại thông báo — dùng trong dropdown và trang danh sách. */
export const NotificationIcon: React.FC<NotificationIconProps> = ({
  type,
  className,
  iconClassName,
}) => {
  const spec = ICONS[type] ?? FALLBACK
  const Icon = spec.Icon

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center w-8 h-8 rounded-full shrink-0',
        spec.tone,
        className,
      )}
      aria-hidden="true"
    >
      <Icon className={cn('w-4 h-4', iconClassName)} />
    </span>
  )
}
