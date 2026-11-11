import React from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Users, GraduationCap } from 'lucide-react'
import type { CourseSummary } from '@/types'
import { JLPT_LEVEL_BADGE } from '@/utils/jlpt'

interface CourseCardProps {
  course: CourseSummary
  showStatus?: boolean
  to?: string
  action?: React.ReactNode
}

function slugify(input: string): string {
  return input
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .slice(0, 80)
}

export const CourseCard: React.FC<CourseCardProps> = ({
  course,
  showStatus = false,
  to,
  action,
}) => {
  const target = to ?? `/courses/${course.id}`
  const statusLabel = course.published
    ? course.archivedAt
      ? 'Đã lưu trữ'
      : 'Đã xuất bản'
    : 'Bản nháp'

  const statusColor = course.published
    ? course.archivedAt
      ? 'bg-rose-50 text-shu border-rose-200'
      : 'bg-emerald-50 text-emerald-700 border-emerald-200'
    : 'bg-kinari text-nibi border-suna'

  return (
    <div className="group relative bg-surface rounded-2xl border border-suna/90 hover:border-ai/50 hover:shadow-md transition-all duration-300 overflow-hidden flex flex-col shadow-2xs">
      {/* Thumbnail */}
      <Link to={target} className="block">
        <div className="relative aspect-[16/9] bg-[#f4efe6] dark:bg-[#1a1e28] border-b border-suna/80 overflow-hidden">
          {course.thumbnailUrl ? (
            <img
              src={course.thumbnailUrl}
              alt={course.title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center p-6 text-center relative">
              {/* Subtle Japanese watermarking */}
              <span className="absolute -right-3 -bottom-5 text-7xl font-serif font-black text-sumi/5 dark:text-white/5 select-none pointer-events-none">
                {course.jlptLevel}
              </span>
              <div className="w-12 h-12 rounded-xl bg-surface border border-suna/80 shadow-2xs flex items-center justify-center mb-2.5 text-ai group-hover:scale-105 transition-transform">
                <BookOpen className="w-6 h-6" />
              </div>
              <p className="font-serif font-bold text-xs text-sumi/80 line-clamp-1 max-w-[85%]">
                {course.title}
              </p>
            </div>
          )}
          <div className="absolute top-3 left-3 flex items-center gap-1.5">
            <span
              className={`px-2.5 py-0.5 rounded-lg text-xs font-black shadow-2xs backdrop-blur-xs ${
                JLPT_LEVEL_BADGE[course.jlptLevel] || 'bg-surface text-sumi border border-suna'
              }`}
            >
              {course.jlptLevel}
            </span>
            {showStatus && (
              <span
                className={`px-2 py-0.5 rounded-lg text-[10px] font-bold uppercase tracking-wider border shadow-2xs ${statusColor}`}
              >
                {statusLabel}
              </span>
            )}
          </div>
        </div>
      </Link>

      {/* Body */}
      <div className="p-4 sm:p-5 flex-1 flex flex-col justify-between">
        <div>
          <Link to={target} className="block">
            <h3 className="font-bold text-base text-sumi line-clamp-2 group-hover:text-ai transition-colors leading-snug">
              {course.title}
            </h3>
          </Link>

          {course.description && (
            <p className="text-xs sm:text-sm text-nibi line-clamp-2 mt-2 leading-relaxed">
              {course.description}
            </p>
          )}
        </div>

        <div className="pt-4 mt-4 border-t border-suna/60 flex items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3 text-nibi">
            <span className="inline-flex items-center gap-1" title="Số bài học">
              <BookOpen className="w-3.5 h-3.5 text-ai" />
              <strong className="text-sumi font-bold">
                {course.totalLessons ?? 0}
              </strong>{' '}
              bài
            </span>
            {course.totalEnrollments != null && (
              <span className="inline-flex items-center gap-1" title="Học viên">
                <Users className="w-3.5 h-3.5 text-nibi" />
                <strong className="text-sumi font-bold">
                  {course.totalEnrollments}
                </strong>
              </span>
            )}
          </div>
          {course.createdByName && (
            <span className="inline-flex items-center gap-1 text-nibi truncate max-w-[120px]">
              <GraduationCap className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{course.createdByName}</span>
            </span>
          )}
        </div>

        {action && <div className="pt-3 mt-3 border-t border-suna/60">{action}</div>}
      </div>
    </div>
  )
}

// Re-export tiện ích slugify
export { slugify }
