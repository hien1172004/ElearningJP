import React, { useState } from 'react'
import { Button as AntdButton, Select } from 'antd'
import { UserMinus, UserPlus } from 'lucide-react'
import { EmptyState, LoadingState } from '@/components/common'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { useAdminUsers, useAssignCourseTeacher, useCourseTeachers, useUnassignCourseTeacher } from '@/hooks'
import type { CourseTeacher } from '@/types'

interface CourseTeachersPanelProps {
  courseId: number
  /** ADMIN được giao / gỡ giáo viên; giáo viên chỉ xem danh sách đồng nghiệp. */
  canManage: boolean
}

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'

const displayName = (t: Pick<CourseTeacher, 'fullName' | 'email'>) => t.fullName || t.email

/** Giáo viên được giao khóa học: mọi giáo viên được giao có quyền ngang nhau trên bài học của khóa. */
export const CourseTeachersPanel: React.FC<CourseTeachersPanelProps> = ({ courseId, canManage }) => {
  const { data: teachers = [], isLoading, isError } = useCourseTeachers(courseId)
  const assign = useAssignCourseTeacher(courseId)
  const unassign = useUnassignCourseTeacher(courseId)
  const [keyword, setKeyword] = useState('')
  const [selected, setSelected] = useState<number | null>(null)

  // Ứng viên: tài khoản TEACHER đang hoạt động (chỉ ADMIN mới tải được danh sách người dùng)
  const { data: candidates, isFetching: searching } = useAdminUsers(
    { role: 'TEACHER', status: 'ACTIVE', keyword: keyword || undefined, page: 1, size: 20 },
    { enabled: canManage },
  )
  const assignedIds = new Set(teachers.map((t) => t.teacherId))
  const options = (candidates?.items ?? [])
    .filter((u) => !assignedIds.has(u.id))
    .map((u) => ({ value: u.id, label: `${u.fullName || u.email} · ${u.email}` }))

  const handleAssign = () => {
    if (selected == null) return
    assign.mutate(selected, { onSuccess: () => setSelected(null) })
  }

  const handleUnassign = async (t: CourseTeacher) => {
    const ok = await confirmDialog({
      title: `Gỡ "${displayName(t)}" khỏi khóa học?`,
      content: 'Giáo viên sẽ không còn quyền chỉnh sửa khóa học. Bài học họ đã tạo vẫn được giữ nguyên.',
      okText: 'Gỡ giáo viên',
      cancelText: 'Hủy',
      danger: true,
    })
    if (ok) unassign.mutate(t.teacherId)
  }

  return (
    <div className="space-y-4">
      <div className={`${CARD} p-4 space-y-3`}>
        <div>
          <h3 className="font-bold text-sm text-gray-800">Giáo viên được giao</h3>
          <p className="text-xs text-gray-500 mt-0.5">
            Mọi giáo viên được giao đều có quyền soạn và sửa tất cả bài học, quiz và bài kiểm tra của khóa.
          </p>
        </div>
        {canManage && (
          <div className="flex flex-col sm:flex-row gap-2">
            <Select
              showSearch
              allowClear
              value={selected ?? undefined}
              placeholder="Tìm giáo viên theo tên hoặc email"
              filterOption={false}
              onSearch={setKeyword}
              onChange={(v: number | undefined) => setSelected(v ?? null)}
              loading={searching}
              options={options}
              notFoundContent={searching ? 'Đang tìm...' : 'Không có giáo viên phù hợp'}
              className="flex-1 min-w-0"
            />
            <AntdButton
              type="primary"
              icon={<UserPlus className="w-4 h-4" />}
              disabled={selected == null}
              loading={assign.isPending}
              onClick={handleAssign}
              className="!rounded-md"
            >
              Giao cho khóa học
            </AntdButton>
          </div>
        )}
      </div>

      {isLoading ? (
        <LoadingState message="Đang tải danh sách giáo viên..." />
      ) : isError ? (
        <EmptyState card title="Không tải được danh sách giáo viên" description="Vui lòng thử lại sau." />
      ) : teachers.length === 0 ? (
        <EmptyState
          card
          title="Chưa có giáo viên nào được giao"
          description={canManage ? 'Chọn giáo viên ở trên để giao soạn khóa học này.' : undefined}
        />
      ) : (
        <ul className={`${CARD} divide-y divide-gray-100`}>
          {teachers.map((t) => (
            <li key={t.teacherId} className="flex items-center gap-3 px-4 py-3">
              {t.avatarUrl ? (
                <img src={t.avatarUrl} alt="" className="w-9 h-9 rounded-full object-cover shrink-0" />
              ) : (
                <div className="w-9 h-9 rounded-full bg-indigo-50 text-indigo-700 flex items-center justify-center text-sm font-bold shrink-0">
                  {displayName(t).charAt(0).toUpperCase()}
                </div>
              )}
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-gray-900 truncate">{displayName(t)}</p>
                <p className="text-xs text-gray-500 truncate">{t.email}</p>
              </div>
              {t.assignedAt && (
                <span className="hidden sm:inline text-xs text-gray-400 shrink-0">
                  Giao ngày {new Date(t.assignedAt).toLocaleDateString('vi-VN')}
                </span>
              )}
              {canManage && (
                <AntdButton
                  size="small"
                  danger
                  icon={<UserMinus className="w-3.5 h-3.5" />}
                  loading={unassign.isPending && unassign.variables === t.teacherId}
                  onClick={() => handleUnassign(t)}
                  className="!rounded-md"
                >
                  Gỡ
                </AntdButton>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
