import { useEffect, useRef } from 'react'
import { useConcurrentEditStore } from '@/stores/useConcurrentEditStore'

/**
 * Gọi {@code handler} khi người dùng bấm "Tải bản mới nhất" trong modal xung đột.
 * Form sửa dùng để tự đóng: form đang giữ version cũ, lưu lại sẽ lại bị 409 —
 * mở lại sẽ lấy bản mới nhất; phần vừa nhập nằm ở panel "Bản chưa lưu của bạn".
 */
export function useOnConcurrentReload(handler: () => void) {
  const ref = useRef(handler)
  useEffect(() => {
    ref.current = handler
  })
  useEffect(
    () =>
      useConcurrentEditStore.subscribe((state, prev) => {
        if (state.reloadSeq !== prev.reloadSeq) ref.current()
      }),
    [],
  )
}
