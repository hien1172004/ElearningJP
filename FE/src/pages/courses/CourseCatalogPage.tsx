import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { BookOpen } from 'lucide-react'
import { Pagination } from 'antd'
import { LoadingState } from '@/components/common'
import { CourseCard, CourseFilterBar } from '@/components/course'
import { useCourses } from '@/hooks'
import { useAuthStore } from '@/stores'
import type { JlptLevel } from '@/types'

export const CourseCatalogPage: React.FC = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const [keyword, setKeyword] = useState('')
  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | undefined>(undefined)
  const [debouncedKeyword, setDebouncedKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(12)

  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedKeyword(keyword.trim())
      setPage(1)
    }, 300)
    return () => clearTimeout(t)
  }, [keyword])

  const { data, isLoading, isError } = useCourses({
    jlptLevel: selectedLevel,
    page,
    size: pageSize,
  })

  const courses = data?.items ?? []
  const filtered = debouncedKeyword
    ? courses.filter((c) =>
      c.title.toLowerCase().includes(debouncedKeyword.toLowerCase()),
    )
    : courses

  return (
    <div className="space-y-6 pb-12">
      {/* Header trang Khóa học */}
      <div className="max-w-7xl mx-auto px-3 sm:px-4 pt-2">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 pb-4 border-b border-suna/80">
          <div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-kamenozoki text-ai text-xs font-bold mb-2 border border-ai/20 shadow-2xs">
              <BookOpen className="w-3.5 h-3.5 text-ai" />
              <span>Khóa học JLPT chuẩn hóa</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-sumi tracking-tight">
              Khóa học tiếng Nhật
            </h1>
            <p className="text-xs sm:text-sm text-nibi mt-1 leading-relaxed">
              Lộ trình bài bản từ N5 đến N1 kèm từ vựng, ngữ pháp, chữ Hán và bài tập củng cố.
            </p>
          </div>
          {!isAuthenticated && (
            <Link
              to="/login"
              className="px-5 py-2.5 rounded-lg bg-ai hover:bg-[#254b70] text-white text-xs sm:text-sm font-semibold shadow-2xs transition inline-flex items-center gap-2 shrink-0 select-none"
            >
              Đăng nhập để bắt đầu học
            </Link>
          )}
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-3 sm:px-4 space-y-4">
        <CourseFilterBar
          keyword={keyword}
          onKeywordChange={setKeyword}
          selectedLevel={selectedLevel}
          onLevelChange={(lvl) => {
            setSelectedLevel(lvl)
            setPage(1)
          }}
        />

        {isLoading && <LoadingState message="Đang tải danh sách khóa học..." />}

        {isError && (
          <div className="text-center py-16 text-sm text-shu">
            Không thể tải danh sách khóa học. Vui lòng thử lại.
          </div>
        )}

        {!isLoading && !isError && filtered.length === 0 && (
          <div className="bg-surface rounded-2xl border border-suna/90 shadow-2xs text-center py-16 px-4 space-y-3">
            <div className="w-14 h-14 rounded-2xl bg-kamenozoki text-ai flex items-center justify-center mx-auto border border-ai/20 shadow-2xs">
              <BookOpen className="w-7 h-7" />
            </div>
            <p className="font-bold text-base text-sumi">
              Chưa tìm thấy khóa học nào
            </p>
            <p className="text-xs sm:text-sm text-nibi max-w-md mx-auto leading-relaxed">
              {selectedLevel
                ? `Không có khóa học ${selectedLevel} nào phù hợp với bộ lọc hiện tại.`
                : 'Danh sách khóa học đang được cập nhật.'}
            </p>
          </div>
        )}

        {!isLoading && !isError && filtered.length > 0 && (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
              {filtered.map((course) => (
                <CourseCard key={course.id} course={course} />
              ))}
            </div>

            {data && data.totalElements > 0 && (
              <div className="flex items-center justify-center pt-8">
                <Pagination
                  current={page}
                  pageSize={pageSize}
                  total={data.totalElements}
                  onChange={(newPage, newPageSize) => {
                    setPage(newPage)
                    if (newPageSize && newPageSize !== pageSize) {
                      setPageSize(newPageSize)
                      setPage(1)
                    }
                  }}
                  onShowSizeChange={(_current, newPageSize) => {
                    setPageSize(newPageSize)
                    setPage(1)
                  }}
                  showSizeChanger={true}
                  pageSizeOptions={['8', '12', '24', '48']}
                  locale={{ items_per_page: '/ trang' }}
                />
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
