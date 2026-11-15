import React from 'react'
import {
  Award,
  Bell,
  CircleCheck,
  CircleX,
  ClipboardCheck,
  FileWarning,
  FileX,
  Flame,
  GraduationCap,
  Layers,
  Megaphone,
  MessageCircle,
  RefreshCw,
  Send,
  ShieldAlert,
  ThumbsUp,
  Undo2,
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
  COMMENT_MODERATED: { Icon: ShieldAlert, tone: 'bg-amber-50 text-amber-600' },
  ANNOUNCEMENT: { Icon: Megaphone, tone: 'bg-violet-50 text-violet-600' },
  COURSE_STATUS_CHANGED: { Icon: GraduationCap, tone: 'bg-sky-50 text-sky-600' },
  COURSE_TEACHER_ASSIGNMENT: { Icon: GraduationCap, tone: 'bg-emerald-50 text-emerald-600' },
  COURSE_REVIEW: { Icon: ClipboardCheck, tone: 'bg-amber-50 text-amber-600' },
  LESSON_SUBMITTED: { Icon: Send, tone: 'bg-amber-50 text-amber-600' },
  LESSON_WITHDRAWN: { Icon: Undo2, tone: 'bg-slate-100 text-slate-600' },
  LESSON_APPROVED: { Icon: CircleCheck, tone: 'bg-emerald-50 text-emerald-600' },
  LESSON_REJECTED: { Icon: CircleX, tone: 'bg-rose-50 text-rose-600' },
  DRAFT_STALE: { Icon: FileWarning, tone: 'bg-orange-50 text-orange-600' },
  LESSON_UPDATED: { Icon: GraduationCap, tone: 'bg-sky-50 text-sky-600' },
  LESSON_DRAFT_DISCARDED: { Icon: FileX, tone: 'bg-rose-50 text-rose-600' },
  COMMUNITY_REPLY: {
    Icon: MessageCircle,
    tone: 'bg-indigo-50 text-indigo-600',
  },
  COMMUNITY_LIKE: {
    Icon: ThumbsUp,
    tone: 'bg-emerald-50 text-emerald-600',
  },
  SRS_DUE: { Icon: Layers, tone: 'bg-sky-50 text-sky-600' },
  STREAK_WARNING: { Icon: Flame, tone: 'bg-amber-50 text-amber-600' },
  EXAM_RESULT: { Icon: Award, tone: 'bg-emerald-50 text-emerald-600' },
  EXAM_UPDATED: { Icon: RefreshCw, tone: 'bg-teal-50 text-teal-600' },
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
