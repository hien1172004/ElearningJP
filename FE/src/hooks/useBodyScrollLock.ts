import { useEffect } from 'react'

/**
 * Khoá cuộn trang (body) khi một lớp phủ tự dựng (modal / drawer / lightbox `fixed inset-0`) đang mở,
 * để lăn chuột trên nền mờ không làm trang phía sau cuộn theo.
 *
 * - Dùng bộ đếm toàn cục: nhiều modal mở chồng nhau (vd: Chi tiết Kanji → Luyện viết) chỉ mở khoá
 *   khi modal CUỐI CÙNG đóng.
 * - Bù `padding-right` bằng độ rộng thanh cuộn để nội dung không bị giật sang phải khi thanh cuộn ẩn.
 * - Modal antd (Modal/Drawer) đã tự khoá cuộn — không cần dùng hook này.
 */
let lockCount = 0
let savedOverflow = ''
let savedPaddingRight = ''

const lock = () => {
  if (lockCount === 0) {
    const body = document.body
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth
    savedOverflow = body.style.overflow
    savedPaddingRight = body.style.paddingRight
    body.style.overflow = 'hidden'
    if (scrollbarWidth > 0) {
      const current = parseFloat(window.getComputedStyle(body).paddingRight) || 0
      body.style.paddingRight = `${current + scrollbarWidth}px`
    }
  }
  lockCount += 1
}

const unlock = () => {
  lockCount = Math.max(0, lockCount - 1)
  if (lockCount === 0) {
    document.body.style.overflow = savedOverflow
    document.body.style.paddingRight = savedPaddingRight
  }
}

export const useBodyScrollLock = (active: boolean) => {
  useEffect(() => {
    if (!active) return
    lock()
    return unlock
  }, [active])
}
