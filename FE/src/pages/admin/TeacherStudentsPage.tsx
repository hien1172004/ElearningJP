import React from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { GraduationCap, Megaphone } from 'lucide-react'
import { Button as AntdButton, Select } from 'antd'
import { EmptyState, LoadingState } from '@/components/common'
import { CourseStudentsPanel } from '@/components/course'
import { useCourses } from '@/hooks'

export const TeacherStudentsPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  // ADMIN: mọi khóa; TEACHER: khóa do mình tạo (BE đã lọc)
  const { data: coursePage, isLoading: coursesLoading } = useCourses({ admin: true, size: 100 })
  const courses = coursePage?.items ?? coursePage?.content ?? []
  const paramCourseId = Number(searchParams.get('courseId')) || null
  // Mặc định chọn khóa đầu tiên khi chưa chọn
  const courseId = paramCourseId ?? courses[0]?.id ?? null

  const selectCourse = (id: number) => {
    setSearchParams({ courseId: String(id) })
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 space-y-5">
      {coursesLoading ? (
        <LoadingState message="Đang tải khóa học..." />
      ) : courses.length === 0 ? (
        <EmptyState
          icon={<GraduationCap className="w-10 h-10 text-slate-300" />}
          title="Bạn chưa có khóa học nào"
          description="Tạo khóa học để bắt đầu theo dõi học viên."
          action={<AntdButton onClick={() => navigate('/teacher/courses')}>Quản lý khóa học</AntdButton>}
        />
      ) : (
        <>
          <div className="flex flex-col md:flex-row gap-3">
            <Select
              value={courseId ?? undefined}
              onChange={selectCourse}
              showSearch
              optionFilterProp="label"
              options={courses.map((c) => ({ value: c.id, label: `${c.title} (${c.jlptLevel})` }))}
              className="md:w-80"
              aria-label="Chọn khóa học"
            />
            {courseId && (
              <AntdButton
                icon={<Megaphone className="w-4 h-4" />}
                onClick={() => navigate(`/admin/broadcasts?courseId=${courseId}`)}
                className="md:ml-auto"
              >
                Gửi thông báo cho học viên
              </AntdButton>
            )}
          </div>

          {/* key: đổi khóa thì làm mới trang, tìm kiếm và ngăn chi tiết */}
          {courseId && <CourseStudentsPanel key={courseId} courseId={courseId} />}
        </>
      )}
    </div>
  )
}
