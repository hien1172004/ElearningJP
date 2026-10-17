import React from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Users, GraduationCap } from 'lucide-react'
import type { CourseSummary } from '@/types'

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
      ? 'bg-rose-50 text-rose-700 border-rose-200'
      : 'bg-emerald-50 text-emerald-700 border-emerald-200'
    : 'bg-gray-100 text-gray-600 border-gray-200'

  return (
    <div className="group relative bg-white rounded-md border border-gray-200/80 hover:border-indigo-400 hover:shadow-md transition-all duration-200 overflow-hidden flex flex-col">
      {/* Thumbnail */}
      <Link to={target} className="block">
        <div className="relative aspect-[16/9] bg-gradient-to-br from-indigo-100 via-indigo-200 to-purple-200 overflow-hidden">
          {course.thumbnailUrl ? (
            <img
              src={course.thumbnailUrl}
              alt={course.title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-6xl font-serif font-black text-white/60 select-none">
                {course.title?.[0] ?? '日'}
              </span>
            </div>
          )}
          <div className="absolute top-3 left-3 flex items-center gap-1.5">
            <span className="px-2.5 py-1 rounded-md bg-indigo-600 text-white text-xs font-bold shadow-sm">
              {course.jlptLevel}
            </span>
            {showStatus && (
              <span
                className={`px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider border ${statusColor}`}
              >
                {statusLabel}
              </span>
            )}
          </div>
        </div>
      </Link>

      {/* Body */}
      <div className="p-4 flex-1 flex flex-col">
        <Link to={target} className="block">
          <h3 className="font-bold text-base text-gray-900 line-clamp-2 group-hover:text-indigo-600 transition">
            {course.title}
          </h3>
        </Link>

        {course.description && (
          <p className="text-sm text-gray-500 line-clamp-2 mt-1.5 leading-relaxed">
            {course.description}
          </p>
        )}

        <div className="mt-auto pt-4 flex items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3 text-gray-500">
            <span className="inline-flex items-center gap-1" title="Số bài học">
              <BookOpen className="w-3.5 h-3.5" />
              <strong className="text-gray-700">
                {course.totalLessons ?? 0}
              </strong>{' '}
              bài
            </span>
            {course.totalEnrollments != null && (
              <span className="inline-flex items-center gap-1" title="Học viên">
                <Users className="w-3.5 h-3.5" />
                <strong className="text-gray-700">
                  {course.totalEnrollments}
                </strong>
              </span>
            )}
          </div>
          {course.createdByName && (
            <span className="inline-flex items-center gap-1 text-gray-400 truncate max-w-[120px]">
              <GraduationCap className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{course.createdByName}</span>
            </span>
          )}
        </div>

        {action && <div className="pt-3 mt-3 border-t border-gray-100">{action}</div>}
      </div>
    </div>
  )
}

// Re-export tiện ích slugify
export { slugify }
