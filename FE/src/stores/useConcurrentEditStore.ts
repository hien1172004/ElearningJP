import { create } from 'zustand'
import type { UnsavedEntry } from '@/lib/unsavedContent'

/**
 * Xử lý người "thua" khi 2 người cùng sửa (BE trả 409 CONCURRENT_MODIFICATION):
 * 1. {@link report}: mở modal (không hiện lỗi 409), giữ nội dung vừa gửi lưu.
 * 2. Người dùng bấm "Tải bản mới nhất" → {@link reloadLatest}: đóng modal, chuyển nội dung đó sang
 *    panel "Bản chưa lưu của bạn" và tăng {@code reloadSeq} để các form sửa đang mở (giữ version cũ) tự đóng.
 * 3. Chép lại xong → {@link dismissUnsaved}.
 */
interface ConcurrentEditState {
  conflictOpen: boolean
  /** Nội dung của các lần lưu bị từ chối, chờ người dùng bấm "Tải bản mới nhất". */
  pending: UnsavedEntry[]
  /** Nội dung hiển thị ở panel "Bản chưa lưu của bạn". */
  unsaved: UnsavedEntry[]
  /** Tăng mỗi lần "Tải bản mới nhất" — form sửa nghe để tự đóng. */
  reloadSeq: number
  report: (entries: UnsavedEntry[]) => void
  /** Mở thẳng panel "Bản chưa lưu của bạn" (vd: bản tự lưu của bản ghi đã bị người khác sửa → chép lại từng đoạn). */
  showUnsaved: (entries: UnsavedEntry[]) => void
  reloadLatest: () => void
  dismissUnsaved: () => void
}

export const useConcurrentEditStore = create<ConcurrentEditState>((set) => ({
  conflictOpen: false,
  pending: [],
  unsaved: [],
  reloadSeq: 0,
  // Nhiều lần lưu cùng bị từ chối trước khi bấm nút → gộp lại, không mất đoạn nào
  report: (entries) => set((s) => ({ conflictOpen: true, pending: [...s.pending, ...entries] })),
  reloadLatest: () =>
    set((s) => ({
      conflictOpen: false,
      pending: [],
      unsaved: [...s.unsaved, ...s.pending],
      reloadSeq: s.reloadSeq + 1,
    })),
  showUnsaved: (entries) => set((s) => ({ unsaved: [...s.unsaved, ...entries] })),
  dismissUnsaved: () => set({ unsaved: [] }),
}))
