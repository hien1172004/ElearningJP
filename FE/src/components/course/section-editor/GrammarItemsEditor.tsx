import React, { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, BookOpen, Eye, Search, Link2, X as XIcon, Volume2, Star } from 'lucide-react'
import { Modal, Form, Input, InputNumber, App, Select, Spin, Empty, Tag } from 'antd'
import { z } from 'zod'
import {
  type LessonGrammarItem,
  type LessonGrammarItemCreateRequest,
  type LessonGrammarItemUpdateRequest,
  type LessonGrammarExampleCreateRequest,
  type LessonGrammarExampleUpsertRequest,
  type GrammarDetail,
} from '@/types'
import { useGrammarItemMutations, useGrammarItemDetail } from '@/hooks'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'

// ============================================================================
// Form schema
// ============================================================================
const baseSchema = z.object({
  grammarId: z.number().int().positive('Vui lòng chọn 1 ngữ pháp từ từ điển'),
  customStructure: z.string().max(500).optional(),
  customMeaning: z.string().max(2000).optional(),
  note: z.string().max(2000).optional(),
  orderIndex: z.number().int().min(1).optional(),
})

type FormValues = z.infer<typeof baseSchema>

// ============================================================================
// Example row
// ============================================================================
interface ExampleRow {
  id?: number
  exampleJp: string
  exampleVi: string
  orderIndex?: number
}

const emptyExample = (idx: number): ExampleRow => ({
  exampleJp: '',
  exampleVi: '',
  orderIndex: idx + 1,
})

/**
 * Chuẩn hoá `examplesJson` từ BE (có thể là array, object, hoặc null)
 * về dạng mảng các item { jp, vi, romaji }.
 * Một số BE trả về: mảng, object lồng nhau, hoặc chuỗi JSON encoded.
 */
const normalizeExamplesJson = (raw: unknown): Array<{ jp?: string; vi?: string; romaji?: string }> => {
  if (!raw) return []
  // Trường hợp là string JSON
  if (typeof raw === 'string') {
    try {
      return normalizeExamplesJson(JSON.parse(raw))
    } catch {
      return []
    }
  }
  // Trường hợp là array
  if (Array.isArray(raw)) {
    return raw.filter(
      (x): x is { jp?: string; vi?: string; romaji?: string } =>
        x != null && typeof x === 'object',
    )
  }
  // Trường hợp là object (BE có thể trả về { items: [...] } hoặc { examples: [...] })
  if (typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    if (Array.isArray(obj.items)) return normalizeExamplesJson(obj.items)
    if (Array.isArray(obj.examples)) return normalizeExamplesJson(obj.examples)
    if (Array.isArray(obj.data)) return normalizeExamplesJson(obj.data)
  }
  return []
}

// ============================================================================
// Sub-component: modal xem chi tiết 1 grammar item
// ============================================================================
const GrammarItemDetailModal: React.FC<{
  open: boolean
  item: LessonGrammarItem | null
  onClose: () => void
}> = ({ open, item, onClose }) => {
  const { data: detail, isLoading } = useGrammarItemDetail(
    open && item ? item.id : undefined,
  )

  // Lấy examplesJson từ dictionary detail
  const [grammarDict, setGrammarDict] = useState<GrammarDetail | null>(null)
  useEffect(() => {
    if (!open || !item?.grammarId) {
      setGrammarDict(null)
      return
    }
    let cancelled = false
    dictionaryService
      .getGrammarDetail(item.grammarId)
      .then((res) => {
        if (!cancelled) setGrammarDict((res.data as GrammarDetail) ?? null)
      })
      .catch(() => {
        if (!cancelled) setGrammarDict(null)
      })
    return () => {
      cancelled = true
    }
  }, [open, item?.grammarId])

  if (!item) return null

  const merged = detail ?? item
  const displayTitle = item.title ?? ''
  const displayStructure = item.displayStructure ?? item.structure ?? ''
  const displayMeaning = item.displayMeaning ?? item.meaningVi ?? ''

  const examples = merged.examples ?? []
  const dictExamples = normalizeExamplesJson(grammarDict?.examplesJson)

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      zIndex={1100}
      width={880}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <Eye className="w-5 h-5 text-purple-600" />
          <span>Chi tiết ngữ pháp</span>
          {displayTitle && (
            <span className="text-lg text-purple-700 ml-2">
              {displayTitle}
            </span>
          )}
        </div>
      }
      destroyOnHidden
    >
      {isLoading ? (
        <div className="py-12 flex justify-center">
          <Spin />
        </div>
      ) : (
        <div className="py-2 space-y-4">
          {/* ============================================================ */}
          {/* Thông tin chính: Title + Structure + Meaning + JLPT */}
          {/* ============================================================ */}
          <div className="rounded-md border-2 border-purple-200 bg-purple-50/50 p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex-1 min-w-0 space-y-2">
                {displayStructure && (
                  <div className="inline-block bg-white border-2 border-purple-200 rounded px-4 py-2 shadow-inner">
                    <p className="text-base font-bold text-gray-900 font-mono">
                      {displayStructure}
                    </p>
                  </div>
                )}
                {displayMeaning && (
                  <p className="text-base font-semibold text-purple-800">
                    📖 {displayMeaning}
                  </p>
                )}
                <div className="flex flex-wrap gap-1.5">
                  {merged.jlptLevel && (
                    <Tag color="purple" className="!m-0">
                      JLPT {merged.jlptLevel}
                    </Tag>
                  )}
                </div>
              </div>
              {/* Nút nghe phát âm — dùng title làm text fallback TTS */}
              {displayTitle && (
                <button
                  type="button"
                  onClick={() => playSmartAudio({ text: displayTitle })}
                  className="shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-purple-600 text-white text-xs font-bold hover:bg-purple-700 hover:scale-105 active:scale-95 transition"
                  title="Nghe phát âm mẫu ngữ pháp"
                >
                  <Volume2 className="w-4 h-4" />
                  Nghe
                </button>
              )}
            </div>
            {item.usageNotes && (
              <div className="mt-3 pt-3 border-t border-purple-200/60">
                <p className="text-[11px] uppercase text-purple-700 font-bold mb-1">
                  Cách dùng
                </p>
                <p className="text-sm text-gray-700 whitespace-pre-wrap">
                  {item.usageNotes}
                </p>
              </div>
            )}
          </div>

          {/* ============================================================ */}
          {/* Thông tin bổ sung: Note, Override */}
          {/* ============================================================ */}
          {(item.note || item.customMeaning || item.customStructure) && (
            <div className="rounded-md border border-amber-200 bg-amber-50/50 p-3 text-sm space-y-1.5">
              {item.customStructure && (
                <div>
                  <p className="text-[11px] uppercase text-amber-700 font-bold">
                    Cấu trúc tuỳ chỉnh (giáo viên)
                  </p>
                  <p className="text-gray-800 whitespace-pre-wrap font-mono text-sm">
                    {item.customStructure}
                  </p>
                </div>
              )}
              {item.customMeaning && (
                <div>
                  <p className="text-[11px] uppercase text-amber-700 font-bold">
                    Nghĩa tuỳ chỉnh (giáo viên)
                  </p>
                  <p className="text-gray-800 whitespace-pre-wrap">
                    {item.customMeaning}
                  </p>
                </div>
              )}
              {item.note && (
                <div>
                  <p className="text-[11px] uppercase text-amber-700 font-bold">
                    Ghi chú
                  </p>
                  <p className="text-gray-700 whitespace-pre-wrap">{item.note}</p>
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* Examples */}
          {/* ============================================================ */}
          <div>
            <p className="text-[11px] uppercase text-gray-400 font-bold mb-2">
              Ví dụ minh hoạ ({examples.length})
            </p>
            {examples.length > 0 ? (
              <div className="space-y-1.5">
                {examples.map((ex, idx) => (
                  <div
                    key={ex.id ?? idx}
                    className="rounded border border-gray-200 bg-white p-2.5"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[10px] font-bold text-purple-600">
                        #{idx + 1}
                      </span>
                      {ex.exampleJp && (
                        <button
                          type="button"
                          onClick={() =>
                            playSmartAudio({ text: ex.exampleJp ?? '' })
                          }
                          className="shrink-0 inline-flex items-center gap-1 px-2 py-1 rounded-md bg-purple-50 text-purple-600 hover:bg-purple-100 hover:scale-105 active:scale-95 transition"
                          title="Nghe phát âm câu ví dụ"
                        >
                          <Volume2 className="w-3.5 h-3.5" />
                          <span className="text-[10px] font-bold">Nghe</span>
                        </button>
                      )}
                    </div>
                    <p className="text-base font-semibold text-gray-900 mt-1">
                      {ex.exampleJp}
                    </p>
                    {ex.exampleRomaji && (
                      <p className="text-sm text-gray-500 italic mt-0.5">
                        {ex.exampleRomaji}
                      </p>
                    )}
                    {ex.exampleVi && (
                      <p className="text-sm text-purple-950 font-medium mt-0.5">
                        {ex.exampleVi}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            ) : dictExamples.length > 0 ? (
              // Fallback: hiển thị ví dụ thô từ từ điển (BE lưu examplesJson)
              <div className="space-y-1.5">
                {dictExamples.map((ex, idx) => (
                  <div
                    key={idx}
                    className="rounded border border-dashed border-gray-300 bg-gray-50/60 p-2.5"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[10px] font-bold text-gray-500">
                        #{idx + 1} (từ từ điển)
                      </span>
                      {ex.jp && (
                        <button
                          type="button"
                          onClick={() => playSmartAudio({ text: ex.jp ?? '' })}
                          className="shrink-0 inline-flex items-center gap-1 px-2 py-1 rounded-md bg-purple-50 text-purple-600 hover:bg-purple-100 transition"
                          title="Nghe phát âm"
                        >
                          <Volume2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                    {ex.jp && (
                      <p className="text-base font-semibold text-gray-900 mt-1">
                        {ex.jp}
                      </p>
                    )}
                    {ex.romaji && (
                      <p className="text-sm text-gray-500 italic mt-0.5">
                        {ex.romaji}
                      </p>
                    )}
                    {ex.vi && (
                      <p className="text-sm text-purple-950 font-medium mt-0.5">
                        {ex.vi}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-gray-400 italic">
                Chưa có ví dụ cho mẫu ngữ pháp này.
              </p>
            )}
          </div>
        </div>
      )}
    </Modal>
  )
}

// ============================================================================
// Props
// ============================================================================
interface GrammarItemsEditorProps {
  lessonId: number
  sectionId: number
  items: LessonGrammarItem[]
}

export const GrammarItemsEditor: React.FC<GrammarItemsEditorProps> = ({
  lessonId,
  sectionId,
  items,
}) => {
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [isOpen, setIsOpen] = useState(false)
  const [editing, setEditing] = useState<LessonGrammarItem | null>(null)
  const [examples, setExamples] = useState<ExampleRow[]>([])
  /** Item đang xem chi tiết (mở modal view, không edit) */
  const [viewingItem, setViewingItem] = useState<LessonGrammarItem | null>(null)
  /** Đang fetch chi tiết từ điển sau khi user chọn grammar (lấy examplesJson) */
  const [loadingGrammarDetail, setLoadingGrammarDetail] = useState(false)

  const { createGrammarItem, updateGrammarItem, deleteGrammarItem } =
    useGrammarItemMutations(lessonId, sectionId)

  // ========================================================================
  // Autocomplete search grammar từ từ điển
  // ========================================================================
  const [searchKw, setSearchKw] = useState('')
  const [searchOptions, setSearchOptions] = useState<
    Array<{
      id: number
      title: string
      structure?: string
      jlptLevel?: string
      meaningVi?: string
    }>
  >([])
  const [searching, setSearching] = useState(false)
  /** Grammar vừa được chọn — lưu để hiển thị panel confirm */
  const [selectedGrammar, setSelectedGrammar] = useState<{
    id: number
    title: string
    structure?: string
    jlptLevel?: string
    meaningVi?: string
  } | null>(null)

  useEffect(() => {
    if (editing || searchKw.trim().length === 0) {
      setSearchOptions([])
      return
    }
    let cancelled = false
    setSearching(true)
    dictionaryService
      .searchGrammars({ keyword: searchKw.trim(), size: 15 })
      .then((res) => {
        if (cancelled) return
        const data: any = res.data
        const itemsArr = data?.items ?? data?.content ?? []
        setSearchOptions(
          itemsArr.map((it: any) => ({
            id: it.id,
            title: it.title,
            structure: it.structure,
            jlptLevel: it.jlptLevel,
            meaningVi: it.meaningVi,
          })),
        )
      })
      .catch(() => {
        if (!cancelled) setSearchOptions([])
      })
      .finally(() => !cancelled && setSearching(false))
    return () => {
      cancelled = true
    }
  }, [searchKw, editing])

  /**
   * Khi user chọn 1 grammar từ autocomplete:
   *  1) Gọi API lấy chi tiết ngữ pháp để có examplesJson (BE lưu dạng JSON)
   *  2) Tự điền các field custom (structure, meaning)
   *  3) Auto-fill examples vào khung "ví dụ minh hoạ" — giáo viên vẫn có thể
   *     thêm/xoá/sửa ví dụ như bình thường (thiết kế cũ giữ nguyên)
   */
  const handleSelectGrammar = async (grammarId: number) => {
    const g = searchOptions.find((o) => o.id === grammarId)
    if (!g) return

    // Optimistic: set selected + fill các field cơ bản ngay để UI không bị giật
    form.setFieldsValue({
      grammarId: g.id,
      customStructure: g.structure ?? '',
      customMeaning: g.meaningVi ?? '',
    })
    setSelectedGrammar(g)
    setSearchOptions([])
    setSearchKw('')

    // Fetch chi tiết để lấy examplesJson.
    // Dùng cờ `cancelled` để tránh race condition khi user click chọn nhanh.
    let cancelled = false
    setLoadingGrammarDetail(true)
    try {
      const res = await dictionaryService.getGrammarDetail(g.id)
      if (cancelled) return
      const detail = (res.data as GrammarDetail) ?? null
      if (!detail) return

      // Cập nhật lại selectedGrammar với data đầy đủ
      setSelectedGrammar((prev) =>
        prev && prev.id === g.id
          ? {
              ...prev,
              structure: detail.structure ?? prev.structure,
              jlptLevel: detail.jlptLevel ?? prev.jlptLevel,
              meaningVi: detail.meaningVi ?? prev.meaningVi,
            }
          : prev,
      )

      // Fill examples từ examplesJson vào state
      if (cancelled) return
      const dictExamples = normalizeExamplesJson(detail.examplesJson)
      if (dictExamples.length > 0) {
        setExamples(
          dictExamples.map((ex, idx) => ({
            id: undefined, // ví dụ từ từ điển, chưa gắn vào lesson item
            exampleJp: ex.jp ?? '',
            exampleVi: ex.vi ?? '',
            exampleRomaji: ex.romaji ?? '',
            orderIndex: idx + 1,
          })),
        )
      }
    } catch {
      if (cancelled) return
      // Lỗi fetch detail → vẫn giữ grammar đã chọn, examples để trống
      message.warning(
        'Không tải được chi tiết ngữ pháp. Ví dụ minh hoạ sẽ để trống.',
      )
    } finally {
      if (!cancelled) setLoadingGrammarDetail(false)
    }
  }

  /** Cho phép user đổi sang grammar khác */
  const handleClearGrammar = () => {
    setSelectedGrammar(null)
    form.setFieldsValue({
      grammarId: undefined,
      customStructure: '',
      customMeaning: '',
    })
    // Reset examples khi đổi từ — tránh ví dụ của mẫu cũ còn lẫn vào
    setExamples([])
  }

  // ========================================================================
  // Modal lifecycle
  // ========================================================================
  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setExamples([])
    setSelectedGrammar(null)
    setSearchKw('')
    setSearchOptions([])
    setLoadingGrammarDetail(false)
    setIsOpen(true)
  }

  const openEdit = (item: LessonGrammarItem) => {
    setEditing(item)
    form.setFieldsValue({
      grammarId: item.grammarId,
      customStructure: item.customStructure ?? '',
      customMeaning: item.customMeaning ?? '',
      note: item.note ?? '',
      orderIndex: item.orderIndex,
    })
    setExamples(
      (item.examples ?? []).map((ex, idx) => ({
        id: ex.id,
        exampleJp: ex.exampleJp ?? '',
        exampleVi: ex.exampleVi ?? '',
        orderIndex: ex.orderIndex ?? idx + 1,
      })),
    )
    if (item.grammarId) {
      setSelectedGrammar({
        id: item.grammarId,
        title: item.title ?? '',
        structure: item.structure,
        jlptLevel: item.jlptLevel,
        meaningVi: item.meaningVi,
      })
    }
    setIsOpen(true)
  }

  const closeModal = () => {
    setIsOpen(false)
    setEditing(null)
    form.resetFields()
    setExamples([])
    setSelectedGrammar(null)
    setSearchKw('')
    setSearchOptions([])
    setLoadingGrammarDetail(false)
  }

  // ========================================================================
  // Submit
  // ========================================================================
  const handleSubmit = async () => {
    try {
      const values = (await form.validateFields()) as FormValues
      const v = baseSchema.safeParse(values)
      if (!v.success) {
        message.error(v.error.issues[0].message)
        return
      }
      const data = v.data

      // Validate examples
      const cleanedExamples: ExampleRow[] = examples
        .filter((e) => e.exampleJp?.trim() || e.exampleVi?.trim())
        .map((e, idx) => ({ ...e, orderIndex: e.orderIndex ?? idx + 1 }))

      for (const e of cleanedExamples) {
        if (!e.exampleJp?.trim()) {
          message.error('Mỗi ví dụ cần có câu tiếng Nhật')
          return
        }
        if (!e.exampleVi?.trim()) {
          message.error(`Ví dụ "${e.exampleJp}" cần có bản dịch tiếng Việt`)
          return
        }
      }

      if (editing) {
        const payload: LessonGrammarItemUpdateRequest = {
          orderIndex: data.orderIndex,
          customStructure: data.customStructure,
          customMeaning: data.customMeaning,
          note: data.note,
          examples: cleanedExamples.map<LessonGrammarExampleUpsertRequest>((e) => ({
            id: e.id,
            exampleJp: e.exampleJp,
            exampleVi: e.exampleVi,
            orderIndex: e.orderIndex,
          })),
        }
        await updateGrammarItem.mutateAsync({ id: editing.id, payload })
      } else {
        const payload: LessonGrammarItemCreateRequest = {
          grammarId: data.grammarId,
          orderIndex: data.orderIndex,
          customStructure: data.customStructure,
          customMeaning: data.customMeaning,
          note: data.note,
          ...(cleanedExamples.length > 0
            ? {
                examples: cleanedExamples.map<LessonGrammarExampleCreateRequest>((e) => ({
                  exampleJp: e.exampleJp,
                  exampleVi: e.exampleVi,
                  orderIndex: e.orderIndex,
                })),
              }
            : {}),
        }
        await createGrammarItem.mutateAsync(payload)
      }
      closeModal()
    } catch (err) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axErr = err as any
        const msg = axErr.response?.data?.message ?? 'Yêu cầu không hợp lệ'
        message.error(Array.isArray(msg) ? msg.join(', ') : String(msg))
      }
    }
  }

  // ========================================================================
  // Examples CRUD
  // ========================================================================
  const addExample = () => {
    setExamples((prev) => [...prev, emptyExample(prev.length)])
  }
  const updateExample = (idx: number, patch: Partial<ExampleRow>) => {
    setExamples((prev) => prev.map((e, i) => (i === idx ? { ...e, ...patch } : e)))
  }
  const removeExample = (idx: number) => {
    setExamples((prev) => prev.filter((_, i) => i !== idx))
  }

  const sorted = [...(items ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="font-semibold text-xs text-purple-700 uppercase tracking-wider">
          Ngữ pháp ({items?.length ?? 0})
        </h4>
        <button
          type="button"
          onClick={openCreate}
          className="inline-flex items-center gap-1 px-2 py-1 rounded bg-purple-600 text-white text-xs font-bold hover:bg-purple-700 transition"
        >
          <Plus className="w-3.5 h-3.5" /> Thêm ngữ pháp
        </button>
      </div>

      {sorted.length === 0 && (
        <div className="py-4 text-center text-xs text-gray-400 border border-dashed border-gray-200 rounded-md">
          Chưa có ngữ pháp nào.
        </div>
      )}

      <div className="space-y-2">
        {sorted.map((item) => (
          <div
            key={item.id}
            className="flex items-center gap-3 p-2.5 bg-white rounded-md border border-gray-200/80 hover:border-purple-300 transition group"
          >
            <div className="flex-1 min-w-0">
              <p className="font-bold text-sm text-gray-900">{item.title}</p>
              {item.displayStructure && (
                <p className="text-xs text-purple-600 font-medium">{item.displayStructure}</p>
              )}
              {item.displayMeaning && (
                <p className="text-xs text-gray-500 mt-0.5">{item.displayMeaning}</p>
              )}
              {item.examples && item.examples.length > 0 && (
                <p className="text-[10px] text-gray-400 mt-0.5">
                  {item.examples.length} ví dụ
                </p>
              )}
            </div>
            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition">
              <button
                type="button"
                onClick={() => setViewingItem(item)}
                className="p-1 text-gray-400 hover:text-purple-600 rounded transition"
                title="Xem chi tiết"
              >
                <Eye className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() => openEdit(item)}
                className="p-1 text-gray-400 hover:text-indigo-600 rounded"
                title="Sửa"
              >
                <Pencil className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() => deleteGrammarItem.mutate(item.id)}
                className="p-1 text-gray-400 hover:text-rose-600 rounded"
                title="Xoá"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>

      <Modal
        open={isOpen}
        onCancel={closeModal}
        onOk={handleSubmit}
        width={880}
        title={
          <div className="flex items-center gap-2 font-bold text-gray-900">
            <BookOpen className="w-5 h-5 text-purple-600" />
            {editing ? 'Sửa ngữ pháp' : 'Thêm ngữ pháp mới'}
          </div>
        }
        okText={editing ? 'Lưu' : 'Thêm'}
        cancelText="Huỷ"
        confirmLoading={createGrammarItem.isPending || updateGrammarItem.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" className="pt-2">
          {/* ============================================================== */}
          {/* CREATE MODE: Tìm grammar từ từ điển */}
          {/* ============================================================== */}
          {!editing && !selectedGrammar && (
            <div className="rounded-md border border-purple-200 bg-purple-50/50 p-3 mb-3 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-purple-700 uppercase tracking-wider">
                <Search className="w-3.5 h-3.5" /> Tìm ngữ pháp từ từ điển
              </div>
              <Select
                showSearch
                size="large"
                className="!w-full"
                placeholder="Gõ để tìm (VD: てください, làm ơn, te form...)"
                filterOption={false}
                searchValue={searchKw}
                onSearch={setSearchKw}
                onChange={handleSelectGrammar}
                value={null}
                notFoundContent={
                  searching ? (
                    <Spin size="small" />
                  ) : searchKw.trim() ? (
                    <Empty description="Không tìm thấy" />
                  ) : (
                    <span className="text-xs text-gray-400">Gõ ít nhất 1 ký tự...</span>
                  )
                }
              >
                {searchOptions.map((o) => (
                  <Select.Option key={o.id} value={o.id}>
                    <span className="font-semibold">{o.title}</span>
                    {o.jlptLevel && (
                      <span className="ml-1 text-[10px] px-1 py-0.5 bg-purple-100 text-purple-700 rounded">
                        {o.jlptLevel}
                      </span>
                    )}
                    {o.structure && (
                      <span className="text-gray-400 ml-1">{o.structure}</span>
                    )}
                    {o.meaningVi && (
                      <span className="text-gray-500 ml-1">— {o.meaningVi}</span>
                    )}
                  </Select.Option>
                ))}
              </Select>
              <p className="text-[11px] text-purple-700/70">
                💡 Sau khi chọn, các ô bên dưới sẽ tự điền từ từ điển. Bạn có thể sửa từng ô nếu muốn dùng giá trị riêng.
              </p>
              <Form.Item name="grammarId" hidden>
                <InputNumber />
              </Form.Item>
            </div>
          )}

          {/* ============================================================== */}
          {/* ĐÃ CHỌN GRAMMAR (cả create + edit): Hiển thị panel confirm */}
          {/* ============================================================== */}
          {selectedGrammar && (
            <div className="rounded-md border-2 border-purple-300 bg-purple-50 p-3 mb-3">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2 text-xs font-bold text-purple-700 uppercase tracking-wider">
                  <Link2 className="w-3.5 h-3.5" /> Ngữ pháp đã chọn
                </div>
                {!editing && (
                  <button
                    type="button"
                    onClick={handleClearGrammar}
                    className="text-[11px] text-purple-700 hover:text-rose-600 inline-flex items-center gap-1"
                  >
                    <XIcon className="w-3 h-3" /> Đổi ngữ pháp khác
                  </button>
                )}
              </div>
              <div className="flex items-baseline gap-2 flex-wrap">
                <p className="text-base font-bold text-gray-900">{selectedGrammar.title}</p>
                {selectedGrammar.jlptLevel && (
                  <span className="text-[10px] px-1.5 py-0.5 bg-purple-100 text-purple-700 rounded font-bold">
                    {selectedGrammar.jlptLevel}
                  </span>
                )}
              </div>
              {selectedGrammar.structure && (
                <p className="text-sm text-purple-600 font-medium mt-1">
                  {selectedGrammar.structure}
                </p>
              )}
              {selectedGrammar.meaningVi && (
                <p className="text-sm text-gray-700 mt-1">📖 {selectedGrammar.meaningVi}</p>
              )}
              <Form.Item name="grammarId" hidden>
                <InputNumber />
              </Form.Item>
            </div>
          )}

          {/* ============================================================== */}
          {/* CUSTOM OVERRIDE FIELDS */}
          {/* ============================================================== */}
          <div className="rounded-md border border-amber-200 bg-amber-50/50 p-3 mb-3 space-y-2">
            <div className="flex items-center gap-2 text-xs font-bold text-amber-700 uppercase tracking-wider">
              <Pencil className="w-3.5 h-3.5" /> Tuỳ chỉnh (giáo viên tự sửa)
            </div>
            <p className="text-[11px] text-amber-700/70 mb-1">
              Để trống = dùng giá trị mặc định từ từ điển.
            </p>
            <div className="grid grid-cols-2 gap-2">
              <Form.Item
                name="customStructure"
                label="Cấu trúc"
                className="!mb-0"
              >
                <Input.TextArea rows={2} placeholder="VD: Verb て-form + ください" />
              </Form.Item>
              <Form.Item name="customMeaning" label="Nghĩa tiếng Việt" className="!mb-0">
                <Input.TextArea rows={2} placeholder="VD: Làm ơn ~" />
              </Form.Item>
            </div>
          </div>

          {/* ============================================================== */}
          {/* NOTE + ORDER */}
          {/* ============================================================== */}
          <div className={`grid ${editing ? 'grid-cols-3' : 'grid-cols-1'} gap-2`}>
            <Form.Item
              name="note"
              label="Ghi chú"
              className={`!${editing ? 'col-span-2' : 'col-span-full'} !mb-0`}
            >
              <Input.TextArea rows={2} placeholder="Ghi chú của giáo viên..." />
            </Form.Item>
            {editing && (
              <Form.Item name="orderIndex" label="Thứ tự" className="!mb-0">
                <InputNumber min={1} className="!w-full" placeholder="Tự gán" />
              </Form.Item>
            )}
          </div>

          {/* ============================================================== */}
          {/* EXAMPLES */}
          {/* ============================================================== */}
          <div className="rounded-md border border-indigo-200 bg-indigo-50/50 p-3 mt-3 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-bold text-indigo-700 uppercase tracking-wider">
                <BookOpen className="w-3.5 h-3.5" /> Ví dụ minh hoạ ({examples.length})
                {loadingGrammarDetail && (
                  <span className="ml-1 text-[10px] font-normal text-indigo-500 normal-case inline-flex items-center gap-1">
                    <Spin size="small" /> đang tải từ từ điển...
                  </span>
                )}
              </div>
              <button
                type="button"
                onClick={addExample}
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-indigo-600 text-white text-[11px] font-bold hover:bg-indigo-700"
              >
                <Plus className="w-3 h-3" /> Thêm ví dụ
              </button>
            </div>

            {examples.length === 0 ? (
              <p className="text-[11px] text-indigo-700/70 italic">
                Không có ví dụ. Bấm "Thêm ví dụ" để tạo, hoặc để trống để BE tự lấy từ từ điển.
              </p>
            ) : (
              <div className="space-y-2">
                {examples.map((ex, idx) => (
                  <div
                    key={idx}
                    className="rounded border border-gray-200 bg-white p-2 space-y-1.5"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-bold text-indigo-600 uppercase">
                        #{idx + 1}
                      </span>
                      <button
                        type="button"
                        onClick={() => removeExample(idx)}
                        className="text-gray-400 hover:text-rose-600 p-0.5"
                      >
                        <XIcon className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    <Input
                      size="small"
                      placeholder="Câu tiếng Nhật *"
                      value={ex.exampleJp}
                      onChange={(e) => updateExample(idx, { exampleJp: e.target.value })}
                      status={ex.exampleJp?.trim() === '' ? 'error' : ''}
                    />
                    <Input
                      size="small"
                      placeholder="Bản dịch tiếng Việt *"
                      value={ex.exampleVi}
                      onChange={(e) => updateExample(idx, { exampleVi: e.target.value })}
                      status={ex.exampleVi?.trim() === '' ? 'error' : ''}
                    />
                    <InputNumber
                      size="small"
                      min={1}
                      placeholder="Thứ tự"
                      value={ex.orderIndex}
                      onChange={(v) => updateExample(idx, { orderIndex: v ?? undefined })}
                      className="!w-24"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        </Form>
      </Modal>

      {/* Modal xem chi tiết grammar item */}
      <GrammarItemDetailModal
        open={!!viewingItem}
        item={viewingItem}
        onClose={() => setViewingItem(null)}
      />
    </div>
  )
}
