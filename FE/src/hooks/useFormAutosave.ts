import { useCallback, useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { lessonAutosaveService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { extractUnsavedEntries } from '@/lib/unsavedContent'
import { useConcurrentEditStore } from '@/stores/useConcurrentEditStore'
import type { LessonAutosave } from '@/types'

/** Gõ xong 1,5 giây thì tự lưu. */
const DEBOUNCE_MS = 1500

/** Nội dung tự lưu của 1 form. */
export interface AutosaveSnapshot<V = unknown, U = unknown> {
  /** version bản ghi lúc tự lưu (form sửa); không có = form tạo mới. */
  baseVersion?: number
  /** Phần người dùng gõ — dùng để chép lại từng đoạn khi bản ghi đã bị người khác sửa. */
  values: V
  /** State giao diện phụ (lựa chọn từ điển…) — chỉ dùng khi khôi phục cả form. */
  ui?: U
}

export type AutosaveStatus = 'idle' | 'saving' | 'saved' | 'error'

/** Lệnh tự lưu đang chờ, chụp lúc lên lịch (lúc đóng form, form có thể đã bị reset). */
interface Job<V, U> {
  lessonId: number
  formKey: string
  /** Lượt mở form tạo ra lệnh (hiển thị trạng thái đúng lượt). */
  session: string
  snapshot: AutosaveSnapshot<V, U>
}

interface FormAutosaveOptions<V, U> {
  lessonId: number
  /** null = form đang đóng. Vd "vocab:345" (sửa), "vocab:new:12" (tạo mới trong section 12). */
  formKey: string | null
  /** version của bản ghi đang mở (form sửa) — lệch với bản tự lưu → chỉ mở để chép lại, không điền đè. */
  currentVersion?: number
  getSnapshot: () => AutosaveSnapshot<V, U>
  /** State riêng ngoài input (vd danh sách ví dụ) — đổi sau khi đã gõ thì tự lưu lại. */
  changeSignal?: unknown
  onRestore: (snapshot: AutosaveSnapshot<V, U>) => void
}

/**
 * Tự lưu form đang soạn dở lên server theo người + bài (docs/lesson-review-design.md mục 5).
 * - Chỉ lưu sau khi người dùng thực sự gõ ({@code markDirty} — gắn vào onInputCapture / onChangeCapture của form),
 *   không lưu dữ liệu vừa nạp vào form.
 * - Đóng form / rời trang khi còn lệnh chờ → lưu ngay; lưu thành công → {@code clear()} (xóa bản tự lưu).
 * - Mở lại form có bản tự lưu → {@code offer}: khôi phục cả form nếu bản ghi chưa đổi, còn không thì mở panel
 *   "Bản chưa lưu của bạn" để chép lại từng đoạn; hoặc bỏ.
 */
export function useFormAutosave<V, U = unknown>({
  lessonId,
  formKey,
  currentVersion,
  getSnapshot,
  changeSignal,
  onRestore,
}: FormAutosaveOptions<V, U>) {
  const qc = useQueryClient()
  const open = formKey != null && lessonId > 0
  const listKey = QUERY_KEYS.lessonAutosave.list(lessonId)
  const { data: saved = [] } = useQuery<LessonAutosave[]>({
    queryKey: listKey,
    queryFn: async () => (await lessonAutosaveService.list(lessonId)).data ?? [],
    enabled: open,
    staleTime: 0,
  })

  const [tick, setTick] = useState(0)
  // Mỗi lần mở form là 1 lượt mới (kể cả mở lại đúng form cũ): ghi nhận lúc render, không reset bằng effect
  const [prevOpenKey, setPrevOpenKey] = useState<string | null>(null)
  const [openSeq, setOpenSeq] = useState(0)
  const openKey = open ? formKey : null
  if (openKey !== prevOpenKey) {
    setPrevOpenKey(openKey)
    if (openKey) setOpenSeq((n) => n + 1)
  }
  const session = openKey ? `${openKey}#${openSeq}` : null
  // State gắn với lượt mở: lượt mới thì tự về ban đầu
  const [dismissedFor, setDismissedFor] = useState<string | null>(null)
  /** Bản hiển thị của dirtyRef (render không đọc ref). */
  const [dirtyFor, setDirtyFor] = useState<string | null>(null)
  const [saveState, setSaveState] = useState<{ session: string; status: AutosaveStatus; at: Date | null } | null>(null)
  const dismissed = session != null && dismissedFor === session
  const dirty = session != null && dirtyFor === session
  const status: AutosaveStatus = saveState && saveState.session === session ? saveState.status : 'idle'
  const savedAt = saveState && saveState.session === session ? saveState.at : null
  const dirtyRef = useRef(false)
  const clearedRef = useRef(false)
  const timerRef = useRef<number | null>(null)
  const pendingRef = useRef<Job<V, U> | null>(null)
  /** Lệnh lưu đang gửi — lệnh xóa phải chạy sau nó, kẻo bản tự lưu đã bỏ lại "sống lại" trên server. */
  const inflightRef = useRef<Promise<unknown> | null>(null)
  const getSnapshotRef = useRef(getSnapshot)
  const onRestoreRef = useRef(onRestore)
  useEffect(() => {
    getSnapshotRef.current = getSnapshot
    onRestoreRef.current = onRestore
  })

  const writeNow = useCallback(
    (job: Job<V, U>) => {
      setSaveState({ session: job.session, status: 'saving', at: null })
      const request = lessonAutosaveService
        .save(job.lessonId, job.formKey, job.snapshot)
        .then(() => {
          setSaveState({ session: job.session, status: 'saved', at: new Date() })
          if (clearedRef.current) return // vừa lưu form thành công → đừng đưa lại vào danh sách
          qc.setQueryData<LessonAutosave[]>(QUERY_KEYS.lessonAutosave.list(job.lessonId), (old = []) => [
            { formKey: job.formKey, content: job.snapshot, updatedAt: new Date().toISOString() },
            ...old.filter((a) => a.formKey !== job.formKey),
          ])
        })
        .catch(() => setSaveState({ session: job.session, status: 'error', at: null }))
      inflightRef.current = request
    },
    [qc],
  )

  const flush = useCallback(() => {
    if (timerRef.current != null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
    const job = pendingRef.current
    pendingRef.current = null
    if (job && !clearedRef.current) writeNow(job)
  }, [writeNow])

  // Lượt mở mới: bắt đầu lại; đóng / rời trang: lưu ngay phần còn chờ
  useEffect(() => {
    if (!session) return
    dirtyRef.current = false
    clearedRef.current = false
    return flush
  }, [session, flush])

  // Đã gõ → lên lịch tự lưu (chụp nội dung ngay lúc này)
  const signalJson = JSON.stringify(changeSignal ?? null)
  useEffect(() => {
    if (!open || !formKey || !session || !dirtyRef.current || clearedRef.current) return
    pendingRef.current = { lessonId, formKey, session, snapshot: getSnapshotRef.current() }
    if (timerRef.current != null) window.clearTimeout(timerRef.current)
    timerRef.current = window.setTimeout(flush, DEBOUNCE_MS)
  }, [tick, signalJson, open, formKey, session, lessonId, flush])

  const markDirty = useCallback(() => {
    if (!session) return
    dirtyRef.current = true
    setDirtyFor(session)
    setTick((t) => t + 1)
  }, [session])

  const entry = open ? saved.find((a) => a.formKey === formKey) : undefined
  const snapshot = entry?.content as AutosaveSnapshot<V, U> | undefined
  const stale =
    snapshot?.baseVersion != null && currentVersion != null && snapshot.baseVersion !== currentVersion
  const offer = entry && !dismissed && !dirty ? { updatedAt: entry.updatedAt ?? null, stale } : null

  const removeOnServer = useCallback(
    (key: string) => {
      const dropFromCache = () =>
        qc.setQueryData<LessonAutosave[]>(listKey, (old = []) => old.filter((a) => a.formKey !== key))
      dropFromCache()
      // Xóa SAU lệnh lưu đang gửi (nếu có); còn sót thì job dọn sau 30 ngày
      void (inflightRef.current ?? Promise.resolve())
        .catch(() => undefined)
        .then(() => lessonAutosaveService.remove(lessonId, key))
        .then(dropFromCache)
        .catch(() => undefined)
    },
    [lessonId, listKey, qc],
  )

  /** Lưu thành công: xóa bản tự lưu, không lưu lại phần còn chờ. Gọi TRƯỚC khi đóng form. */
  const clear = useCallback(() => {
    clearedRef.current = true
    pendingRef.current = null
    if (timerRef.current != null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
    if (formKey) removeOnServer(formKey)
  }, [formKey, removeOnServer])

  /** Bản ghi đã đổi → chép lại từng đoạn ở panel "Bản chưa lưu của bạn" (không điền đè). */
  const openForCopy = useCallback(() => {
    if (!snapshot) return
    useConcurrentEditStore.getState().showUnsaved(extractUnsavedEntries(snapshot.values))
    setDismissedFor(session)
  }, [snapshot, session])

  const restore = useCallback(() => {
    if (!snapshot) return
    if (stale) {
      openForCopy()
      return
    }
    onRestoreRef.current(snapshot)
    setDismissedFor(session)
    dirtyRef.current = true // sửa tiếp sau khi khôi phục cũng được tự lưu
    setDirtyFor(session)
  }, [snapshot, stale, openForCopy, session])

  const discard = useCallback(() => {
    if (formKey) removeOnServer(formKey)
    setDismissedFor(session)
  }, [formKey, session, removeOnServer])

  return { offer, restore, discard, clear, markDirty, status, savedAt }
}

export type FormAutosave = ReturnType<typeof useFormAutosave>
