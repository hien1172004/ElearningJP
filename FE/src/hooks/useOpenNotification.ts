import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { useMarkAsRead } from '@/hooks/useNotifications'
import { useResolveCommentLink, extractErrorMessage } from '@/hooks/useComments'
import type { AppNotification, NotificationData } from '@/types'

/** Chỉ nhận đường dẫn trong ứng dụng ("/..."), chặn "//domain" (mở sang trang ngoài). */
const safeLink = (link: unknown): string | null =>
  typeof link === 'string' && link.startsWith('/') && !link.startsWith('//') ? link : null

/** Link dự phòng khi không mở thẳng được bình luận: tab tra cứu / danh sách khóa học. */
const commentFallback = (targetType: NotificationData['targetType']): string => {
  switch (targetType) {
    case 'VOCABULARY':
      return '/dictionary?tab=vocab'
    case 'KANJI':
      return '/dictionary?tab=kanji'
    case 'GRAMMAR':
      return '/dictionary?tab=grammar'
    default:
      return '/courses'
  }
}

/**
 * Trang đích của 1 thông báo — nguồn duy nhất cho chuông, toast real-time và trang /notifications.
 * Trả null khi không có gì để mở (vd bình luận đã bị ẩn).
 */
export async function resolveNotificationLink(
  n: AppNotification,
  resolveCommentLink: (commentId: number, replyId?: number) => Promise<string | null | undefined>,
): Promise<string | null> {
  const data = n.dataJson
  switch (n.type) {
    case 'SRS_DUE':
    case 'STREAK_WARNING':
      return '/srs'
    case 'EXAM_RESULT':
      // Một lần đính chính có thể chấm lại nhiều lượt thi của cùng người → mở lịch sử thi
      return '/exam-history'
    case 'EXAM_UPDATED':
      return '/exams'
    case 'ANNOUNCEMENT':
    case 'COURSE_STATUS_CHANGED':
      return safeLink(data?.link) ?? (data?.courseId ? `/courses/${data.courseId}` : null)
    case 'COMMUNITY_REPLY':
    case 'COMMUNITY_LIKE': {
      if (!data?.targetType) return null
      if (!data.commentId) return commentFallback(data.targetType)
      try {
        return (await resolveCommentLink(data.commentId, data.replyId)) ?? commentFallback(data.targetType)
      } catch (err) {
        toast.error(extractErrorMessage(err, 'Bình luận không còn tồn tại hoặc bạn không có quyền xem.'))
        return null
      }
    }
    case 'COMMENT_MODERATED':
      return null
    default:
      // Giao khóa / duyệt khóa / duyệt bài / bản nháp cũ: link BE đính kèm (bị gỡ khỏi khóa: không có link)
      return safeLink(data?.link)
  }
}

/** Đánh dấu đã đọc rồi mở trang đích của thông báo (nếu có). */
export function useOpenNotification() {
  const navigate = useNavigate()
  const resolveCommentLink = useResolveCommentLink()
  const { mutate: markAsRead } = useMarkAsRead()

  return useCallback(
    async (n: AppNotification) => {
      if (!n.read) markAsRead(n.id)
      const link = await resolveNotificationLink(n, resolveCommentLink)
      if (link) navigate(link)
    },
    [navigate, resolveCommentLink, markAsRead],
  )
}
