import React, { useState, useEffect } from 'react'
import { BookOpen } from 'lucide-react'
import { Button as AntdButton } from 'antd'
import { PageHeader, LoadingState } from '@/components/common'
import { CourseCard, CourseFilterBar } from '@/components/course'
import { useCourses } from '@/hooks'
import { useAuthStore, useAuthModalStore } from '@/stores'
import type { JlptLevel } from '@/types'

export const CourseCatalogPage: React.FC = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)
  const [keyword, setKeyword] = useState('')
  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | undefined>(undefined)
  const [debouncedKeyword, setDebouncedKeyword] = useState('')
  const [page, setPage] = useState(1)

  useEffect(() => {
    const t = setTimeout(() => setDebouncedKeyword(keyword.trim()), 300)
    return () => clearTimeout(t)
  }, [keyword])

  const { data, isLoading, isError } = useCourses({
    jlptLevel: selectedLevel,
    page,
    size: 12,
  })

  const courses = data?.items ?? []
  const filtered = debouncedKeyword
    ? courses.filter((c) =>
        c.title.toLowerCase().includes(debouncedKeyword.toLowerCase()),
      )
    : courses

  return (
    <div className="space-y-6 pb-12">
      <PageHeader
        badge="Khóa học tiếng Nhật"
        title={
          <span>
            Khoá học JLPT{' '}
            <span className="text-indigo-300">từ N5 đến N1</span>
          </span>
        }
        subtitle="Chọn khóa học phù hợp với trình độ của bạn và bắt đầu hành trình chinh phục tiếng Nhật."
        actions={
          !isAuthenticated && (
            <AntdButton
              type="primary"
              size="large"
              className="!rounded-md !font-bold"
              onClick={() => openLogin()}
            >
              Đăng nhập để đăng ký học
            </AntdButton>
          )
        }
      />

      <div className="max-w-7xl mx-auto px-3 sm:px-4 space-y-4">
        <CourseFilterBar
          keyword={keyword}
          onKeywordChange={setKeyword}
          selectedLevel={selectedLevel}
          onLevelChange={setSelectedLevel}
        />

        {isLoading && <LoadingState message="Đang tải danh sách khóa học..." />}

        {isError && (
          <div className="text-center py-16 text-sm text-rose-600">
            Không thể tải danh sách khóa học. Vui lòng thử lại.
          </div>
        )}

        {!isLoading && !isError && filtered.length === 0 && (
          <div className="text-center py-16 space-y-3">
            <div className="w-16 h-16 rounded-md bg-gray-100 text-gray-400 flex items-center justify-center mx-auto">
              <BookOpen className="w-8 h-8" />
            </div>
            <p className="font-semibold text-gray-700">
              Chưa có khóa học nào
            </p>
            <p className="text-sm text-gray-500">
              {selectedLevel
                ? `Không có khóa học N${selectedLevel.slice(1)} nào được công khai.`
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

            {data && data.totalPages > 1 && (
              <div className="flex items-center justify-center gap-3 pt-4">
                <AntdButton
                  size="large"
                  disabled={page <= 1}
                  onClick={() => setPage((p) => p - 1)}
                >
                  Trước
                </AntdButton>
                <span className="text-sm font-medium text-gray-600 px-4">
                  Trang {data.page} / {data.totalPages}
                </span>
                <AntdButton
                  size="large"
                  disabled={page >= data.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Sau
                </AntdButton>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
