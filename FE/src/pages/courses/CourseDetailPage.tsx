import React from 'react'
import { useParams, Link } from 'react-router-dom'
import { Users, BookOpen, GraduationCap, ArrowRight, LogIn } from 'lucide-react'
import { Button as AntdButton } from 'antd'
import { PageHeader, LoadingState } from '@/components/common'
import { LessonOutlineList } from '@/components/course'
import { CourseProgressBar } from '@/components/common'
import { useCourseDetail, useEnrollCourse, useMyEnrollments } from '@/hooks'
import { useAuthStore, useAuthModalStore } from '@/stores'
import { toSlug } from '@/utils'

export const CourseDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const courseId = Number(id)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)
  const { data: course, isLoading, isError } = useCourseDetail(courseId)
  const { data: enrollments = [] } = useMyEnrollments(true)
  const enroll = useEnrollCourse()

  const myEnrollment = enrollments.find((e) => e.courseId === courseId)
  const isEnrolled = course?.isEnrolled ?? !!myEnrollment

  if (isLoading) return <LoadingState message="Đang tải thông tin khóa học..." fullHeight />
  if (isError || !course) {
    return (
      <div className="text-center py-20 text-sm text-rose-600">
        Không tìm thấy khóa học này.
        <br />
        <Link to="/courses" className="text-indigo-600 underline mt-2 inline-block">
          Quay lại danh sách
        </Link>
      </div>
    )
  }

  const firstLesson = course.lessons?.[0]
  const lessons = course.lessons ?? []

  return (
    <div className="space-y-6 pb-12">
      {/* Hero */}
      <PageHeader
        badge={`JLPT ${course.jlptLevel}`}
        title={<span>{course.title}</span>}
        subtitle={
          course.description ||
          `Khóa học tiếng Nhật cấp độ ${course.jlptLevel} dành cho người học.`
        }
        variant="indigo"
        actions={
          isAuthenticated && !isEnrolled ? (
            <AntdButton
              type="primary"
              size="large"
              icon={<LogIn className="w-5 h-5" />}
              loading={enroll.isPending}
              onClick={() => enroll.mutate(courseId)}
              className="!rounded-md !font-bold"
            >
              Đăng ký học ngay
            </AntdButton>
          ) : isEnrolled && firstLesson ? (
            <Link to={
              course?.slug
                ? `/learn/${course.slug}/${toSlug(firstLesson.title)}?courseId=${courseId}&lessonId=${firstLesson.id}`
                : `/learn/${courseId}/${firstLesson.id}`
            }>
              <AntdButton
                type="primary"
                size="large"
                icon={<ArrowRight className="w-5 h-5" />}
                className="!rounded-md !font-bold"
              >
                Vào học ngay
              </AntdButton>
            </Link>
          ) : null
        }
      />

      <div className="max-w-5xl mx-auto px-3 sm:px-4 space-y-6">
        {/* Stats + Progress */}
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-6 grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="text-center">
            <p className="text-2xl font-black text-indigo-600">{course.jlptLevel}</p>
            <p className="text-xs text-gray-500 mt-1">Cấp độ JLPT</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-black text-gray-900 flex items-center justify-center gap-1">
              <BookOpen className="w-5 h-5" />
              {course.totalLessons ?? lessons.length}
            </p>
            <p className="text-xs text-gray-500 mt-1">Bài học</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-black text-gray-900 flex items-center justify-center gap-1">
              <Users className="w-5 h-5" />
              {course.totalEnrollments ?? 0}
            </p>
            <p className="text-xs text-gray-500 mt-1">Học viên</p>
          </div>
          <div className="text-center">
            {isEnrolled && course.progressPercent != null ? (
              <CourseProgressBar
                percent={course.progressPercent}
                completedLessons={course.completedLessonsCount}
                totalLessons={course.totalLessons ?? lessons.length}
                size="sm"
              />
            ) : (
              <>
                <p className="text-2xl font-black text-gray-900 flex items-center justify-center gap-1">
                  <GraduationCap className="w-5 h-5" />
                  {course.createdByName ?? '—'}
                </p>
                <p className="text-xs text-gray-500 mt-1">Giáo viên</p>
              </>
            )}
          </div>
        </div>

        {isEnrolled && (
          <div className="bg-indigo-50/60 rounded-md border border-indigo-200/80 p-5 flex items-center justify-between gap-4">
            <div>
              <p className="font-bold text-indigo-800">Tiến độ học tập</p>
              <p className="text-sm text-indigo-600 mt-0.5">
                {course.completedLessonsCount ?? 0}/{course.totalLessons ?? lessons.length} bài hoàn thành
              </p>
            </div>
            <CourseProgressBar
              percent={course.progressPercent ?? 0}
              completedLessons={course.completedLessonsCount}
              totalLessons={course.totalLessons ?? lessons.length}
            />
          </div>
        )}

        {/* Mục lục bài học */}
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-6">
          <h2 className="font-bold text-base text-gray-900 mb-4 flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            Mục lục bài học ({lessons.length})
          </h2>
          <LessonOutlineList
            lessons={lessons}
            courseId={courseId}
            courseSlug={course?.slug ?? ''}
            isEnrolled={isEnrolled}
          />
          {!isEnrolled && (
            <div className="mt-4 p-4 bg-gray-50 rounded-md text-center text-sm text-gray-600">
              <p>Vui lòng đăng ký khóa học để xem chi tiết bài học.</p>
              {!isAuthenticated && (
                <AntdButton
                  type="primary"
                  className="!rounded-md !font-semibold mt-3"
                  onClick={() => openLogin()}
                >
                  Đăng nhập và đăng ký
                </AntdButton>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
