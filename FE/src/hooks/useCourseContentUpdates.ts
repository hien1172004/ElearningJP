import { useCallback, useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { subscribeStomp } from '@/lib/stompSubscriptions'
import { useAuthStore } from '@/stores/useAuthStore'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { isAdmin } from '@/utils/roles'

/** Khớp {@code CourseContentPublisher.ContentChangedMessage} ở BE. */
interface CourseContentChangedMessage {
  courseId: number
  /**
   * DRAFT: bản đang soạn đổi (kèm draftKind = phía nháp bị đổi; null = phần dùng chung như cấu trúc khóa);
   * PUBLISHED: bản học viên đang học đổi;
   * LOCK: khóa sửa khóa học (theo phía, V65) bắt đầu / kết thúc / bị tiếp quản (nội dung không đổi);
   * REVIEW: chỉ trạng thái duyệt đổi (gửi / rút / từ chối / bỏ nháp) — nội dung người xem không đổi.
   */
  scope: 'DRAFT' | 'PUBLISHED' | 'LOCK' | 'REVIEW'
  actorId?: number | null
  draftKind?: 'ADMIN' | 'TEACHER' | null
}

/** editor: người soạn (ADMIN / giáo viên) — nhận mọi thay đổi; learner: học viên — chỉ khi bản đang học đổi. */
export type CourseContentAudience = 'editor' | 'learner'

/**
 * Nghe realtime "nội dung khóa học vừa đổi" (STOMP {@code /topic/courses/{id}/content}).
 * Thay đổi do chính mình thì bỏ qua. Trả {@code stale} = có bản mới hơn đang hiển thị và
 * {@code reload} = tải lại dữ liệu đang mở (không reload cả trang, giữ nguyên vị trí).
 */
export function useCourseContentUpdates(courseId: number | undefined, audience: CourseContentAudience) {
  const qc = useQueryClient()
  const currentUserId = useAuthStore((s) => s.user?.id)
  // Phía nháp người xem đang soạn (V65): ADMIN → nháp ADMIN, còn lại → nháp TEACHER
  const mySide = useAuthStore((s) => (isAdmin(s.user) ? 'ADMIN' : 'TEACHER'))
  const [stale, setStale] = useState(false)

  useEffect(() => {
    if (!courseId || courseId <= 0) return
    return subscribeStomp(`/topic/courses/${courseId}/content`, (message) => {
      const payload = JSON.parse(message.body) as CourseContentChangedMessage
      if (payload.scope === 'LOCK') {
        // Chỉ cập nhật trạng thái khóa sửa (banner "X đang sửa"), không nhắc tải lại nội dung
        if (audience === 'editor') {
          void qc.invalidateQueries({ queryKey: QUERY_KEYS.courseEditLock.detail(courseId) })
          void qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
        }
        return
      }
      if (payload.actorId != null && payload.actorId === currentUserId) return
      if (audience === 'learner' && payload.scope !== 'PUBLISHED') return
      // Chỉ trạng thái duyệt đổi, hoặc nháp của phía kia đổi (mình không đọc nháp đó): tự làm mới nhãn trạng thái /
      // nút Duyệt, Gửi duyệt... — không nhắc tải lại nội dung
      const otherSideDraft = payload.scope === 'DRAFT' && payload.draftKind != null && payload.draftKind !== mySide
      if (payload.scope === 'REVIEW' || otherSideDraft) {
        void qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
        void qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
        return
      }
      // Người soạn: làm mới trạng thái duyệt ngay (chỉ nhãn / cờ "nháp đã cũ", không đổi nội dung đang sửa) để
      // thông báo biết bài đang mở cần "Cập nhật bản mới" thay vì chỉ tải lại
      if (audience === 'editor') void qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
      setStale(true)
    })
  }, [courseId, audience, currentUserId, mySide, qc])

  const reload = useCallback(async () => {
    setStale(false)
    // Trang soạn / học dùng nhiều query (khóa, bài, section, item, quiz, đề, duyệt...) → làm mới mọi query đang mở
    await qc.invalidateQueries()
  }, [qc])

  return { stale, reload }
}
