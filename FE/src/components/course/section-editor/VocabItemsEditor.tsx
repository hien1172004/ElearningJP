import React, { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, BookOpen, Eye, Search, Link2, X as XIcon, Volume2, Star } from 'lucide-react'
import { Modal, Form, Input, InputNumber, App, Select, Spin, Empty, Tag } from 'antd'
import { z } from 'zod'
import {
  type LessonVocabularyItem,
  type LessonVocabularyItemCreateRequest,
  type LessonVocabularyItemUpdateRequest,
  type LessonVocabularyExampleCreateRequest,
  type LessonVocabularyExampleUpsertRequest,
  type VocabularyDetail,
} from '@/types'
import { useVocabItemMutations, useVocabItemDetail } from '@/hooks'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'

// ============================================================================
// Form schema (client-side validation trước khi gửi BE)
// ============================================================================
const baseSchema = z.object({
  vocabId: z.number().int().positive('Vui lòng chọn 1 từ vựng từ từ điển'),
  customHiragana: z.string().max(100).optional(),
  customRomaji: z.string().max(150).optional(),
  customAudioUrl: z.string().max(500).optional(),
  customMeaning: z.string().max(5000).optional(),
  imageUrl: z.string().max(500).optional(),
  note: z.string().max(2000).optional(),
  orderIndex: z.number().int().min(1).optional(),
})

type FormValues = z.infer<typeof baseSchema>

// ============================================================================
// Shape for a single example row trong form (dùng cho cả create và update)
// ============================================================================
interface ExampleRow {
  /** id từ BE - dùng cho update để biết example nào đang edit */
  id?: number
  exampleJp: string
  exampleVi: string
  exampleRomaji?: string
  audioUrl?: string
  orderIndex?: number
  isPrimary?: boolean
}

const emptyExample = (idx: number): ExampleRow => ({
  exampleJp: '',
  exampleVi: '',
  exampleRomaji: '',
  audioUrl: '',
  orderIndex: idx + 1,
  isPrimary: idx === 0, // mặc định example đầu tiên là primary
})

// ============================================================================
// Sub-component: modal xem chi tiết 1 vocab item (dùng cả data từ lesson item
// và fetch thêm từ /lesson-vocabulary-items/vocabulary-items/{id} để chắc chắn
// có examples đầy đủ — phòng trường hợp BE trả về thiếu).
// ============================================================================
const VocabItemDetailModal: React.FC<{
  open: boolean
  item: LessonVocabularyItem | null
  onClose: () => void
}> = ({ open, item, onClose }) => {
  // Fetch chi tiết từ BE để chắc chắn có examples đầy đủ + vocab từ từ điển
  const { data: detail, isLoading } = useVocabItemDetail(
    open && item ? item.id : undefined,
  )

  // Senses + kanji components lấy từ dictionary detail
  const [vocabDict, setVocabDict] = useState<VocabularyDetail | null>(null)
  useEffect(() => {
    if (!open || !item?.vocabId) {
      setVocabDict(null)
      return
    }
    let cancelled = false
    dictionaryService
      .getVocabularyDetail(item.vocabId)
      .then((res) => {
        if (!cancelled) setVocabDict((res.data as VocabularyDetail) ?? null)
      })
      .catch(() => {
        if (!cancelled) setVocabDict(null)
      })
    return () => {
      cancelled = true
    }
  }, [open, item?.vocabId])

  if (!item) return null

  // Ưu tiên data từ detail (đầy đủ) → fallback từ list item
  const merged = detail ?? item
  const displayWord = item.word ?? ''
  const displayHiragana =
    item.displayHiragana ?? item.hiragana ?? vocabDict?.hiragana ?? ''
  const displayRomaji = item.displayRomaji ?? item.romaji ?? ''
  const displayMeaning =
    item.displayMeaning ?? item.meaningVi ?? vocabDict?.meaningVi ?? ''

  const examples = merged.examples ?? []
  const senses = vocabDict?.senses ?? []
  const kanjiComponents = vocabDict?.kanjiComponents ?? []

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      zIndex={1100}
      width={880}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <Eye className="w-5 h-5 text-emerald-600" />
          <span>Chi tiết từ vựng</span>
          {displayWord && (
            <span className="text-xl text-emerald-700 ml-2">
              {displayWord}
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
          {/* Thông tin chính */}
          {/* ============================================================ */}
          <div className="rounded-md border-2 border-emerald-200 bg-emerald-50/50 p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-baseline gap-3 flex-wrap flex-1 min-w-0">
                <span className="text-2xl font-bold text-gray-900">
                  {displayWord || '—'}
                </span>
                {displayHiragana && (
                  <span className="text-base text-gray-500">
                    【{displayHiragana}】
                  </span>
                )}
                {displayRomaji && (
                  <span className="text-sm text-gray-400 italic">
                    {displayRomaji}
                  </span>
                )}
                {item.hanViet && (
                  <span className="text-sm text-gray-500">
                    ({item.hanViet})
                  </span>
                )}
              </div>
              {/* Nút nghe phát âm từ chính — ưu tiên audioUrl từ từ điển, fallback TTS */}
              {displayWord && (
                <button
                  type="button"
                  onClick={() =>
                    playSmartAudio({
                      text: displayWord,
                      audioUrl:
                        vocabDict?.audioUrl ||
                        item.displayAudioUrl ||
                        item.customAudioUrl ||
                        undefined,
                    })
                  }
                  className="shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 hover:scale-105 active:scale-95 transition"
                  title="Nghe phát âm từ vựng"
                >
                  <Volume2 className="w-4 h-4" />
                  Nghe
                </button>
              )}
            </div>
            {displayMeaning && (
              <p className="mt-2 text-base font-semibold text-emerald-800">
                📖 {displayMeaning}
              </p>
            )}
            <div className="mt-2 flex flex-wrap gap-1.5">
              {merged.jlptLevel && (
                <Tag color="emerald" className="!m-0">
                  JLPT {merged.jlptLevel}
                </Tag>
              )}
              {merged.partOfSpeech && (
                <Tag color="blue" className="!m-0">
                  {merged.partOfSpeech}
                </Tag>
              )}
            </div>
          </div>

          {/* ============================================================ */}
          {/* Thông tin bổ sung: Note, Override */}
          {/* ============================================================ */}
          {(item.note || item.customMeaning) && (
            <div className="rounded-md border border-amber-200 bg-amber-50/50 p-3 text-sm space-y-1.5">
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
                  <p className="text-gray-700 whitespace-pre-wrap">
                    {item.note}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* ============================================================ */}
          {/* Senses (từ từ điển) */}
          {/* ============================================================ */}
          {senses.length > 0 && (
            <div>
              <p className="text-[11px] uppercase text-gray-400 font-bold mb-2">
                Các nghĩa ({senses.length})
              </p>
              <div className="space-y-1.5">
                {senses.map((s, idx) => (
                  <div
                    key={s.id ?? idx}
                    className="rounded border border-gray-200 bg-white p-2.5 text-sm"
                  >
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] font-bold text-emerald-600">
                        #{s.senseNo}
                      </span>
                      {s.partOfSpeech && (
                        <span className="text-[10px] px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded">
                          {s.partOfSpeech}
                        </span>
                      )}
                    </div>
                    {s.meaningVi && (
                      <p className="text-gray-800 font-medium mt-0.5">
                        {s.meaningVi}
                      </p>
                    )}
                    {s.meaningEn && (
                      <p className="text-xs text-gray-500 mt-0.5">
                        EN: {s.meaningEn}
                      </p>
                    )}
                    {s.usageNotes && (
                      <p className="text-[11px] text-gray-400 italic mt-1">
                        {s.usageNotes}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ============================================================ */}
          {/* Kanji components (chữ Hán cấu thành) */}
          {/* ============================================================ */}
          {kanjiComponents.length > 0 && (
            <div>
              <p className="text-[11px] uppercase text-gray-400 font-bold mb-2">
                Chữ Hán cấu thành ({kanjiComponents.length})
              </p>
              <div className="flex flex-wrap gap-1.5">
                {kanjiComponents.map((k, idx) => (
                  <div
                    key={k.id ?? idx}
                    className="rounded border border-amber-200 bg-amber-50/40 px-2.5 py-1.5 text-sm"
                  >
                    <span className="font-serif font-bold text-lg text-amber-900 mr-1.5">
                      {k.character}
                    </span>
                    {k.hanViet && (
                      <span className="text-xs text-gray-600">
                        {k.hanViet}
                      </span>
                    )}
                    {k.meaningVi && (
                      <span className="text-xs text-gray-500 ml-1">
                        — {k.meaningVi}
                      </span>
                    )}
                    {k.jlptLevel && (
                      <span className="text-[10px] font-bold text-amber-600 ml-1">
                        {k.jlptLevel}
                      </span>
                    )}
                  </div>
                ))}
              </div>
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
                    className={`rounded border p-2.5 ${
                      ex.isPrimary
                        ? 'border-amber-300 bg-amber-50/60'
                        : 'border-gray-200 bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-baseline gap-2 flex-wrap">
                        <span className="text-[10px] font-bold text-indigo-600">
                          #{idx + 1}
                        </span>
                        {ex.isPrimary && (
                          <span className="text-[10px] px-1.5 py-0.5 bg-amber-500 text-white rounded font-bold">
                            PRIMARY
                          </span>
                        )}
                      </div>
                      {/* Nút nghe câu ví dụ — ưu tiên audioUrl của example, fallback TTS */}
                      {ex.exampleJp && (
                        <button
                          type="button"
                          onClick={() =>
                            playSmartAudio({
                              text: ex.exampleJp ?? '',
                              audioUrl: ex.audioUrl || undefined,
                            })
                          }
                          className="shrink-0 inline-flex items-center gap-1 px-2 py-1 rounded-md bg-indigo-50 text-indigo-600 hover:bg-indigo-100 hover:scale-105 active:scale-95 transition"
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
                      <p className="text-sm text-indigo-950 font-medium mt-0.5">
                        {ex.exampleVi}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-gray-400 italic">
                Chưa có ví dụ cho từ vựng này.
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
interface VocabItemsEditorProps {
  lessonId: number
  sectionId: number
  items: LessonVocabularyItem[]
}

export const VocabItemsEditor: React.FC<VocabItemsEditorProps> = ({
  lessonId,
  sectionId,
  items,
}) => {
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [isOpen, setIsOpen] = useState(false)
  const [editing, setEditing] = useState<LessonVocabularyItem | null>(null)
  /** Item đang xem chi tiết (mở modal view, không edit) */
  const [viewingItem, setViewingItem] = useState<LessonVocabularyItem | null>(null)
  const [examples, setExamples] = useState<ExampleRow[]>([])

  const { createVocabItem, updateVocabItem, deleteVocabItem } =
    useVocabItemMutations(lessonId, sectionId)

  // ========================================================================
  // Autocomplete search vocab từ từ điển
  // ========================================================================
  const [searchKw, setSearchKw] = useState('')
  const [searchOptions, setSearchOptions] = useState<
    Array<{
      id: number
      word: string
      hiragana?: string
      romaji?: string
      hanViet?: string
      meaningVi?: string
      meaningEn?: string
      audioUrl?: string
      partOfSpeech?: string
      jlptLevel?: string
    }>
  >([])
  const [searching, setSearching] = useState(false)
  /** Đang fetch chi tiết từ điển sau khi user chọn vocab (lấy examples) */
  const [loadingVocabDetail, setLoadingVocabDetail] = useState(false)
  /** Vocab vừa được chọn — lưu để hiển thị panel confirm */
  const [selectedVocab, setSelectedVocab] = useState<{
    id: number
    word: string
    hiragana?: string
    romaji?: string
    hanViet?: string
    meaningVi?: string
    meaningEn?: string
    audioUrl?: string
  } | null>(null)

  useEffect(() => {
    if (editing || searchKw.trim().length === 0) {
      setSearchOptions([])
      return
    }
    let cancelled = false
    setSearching(true)
    dictionaryService
      .searchVocabularies({ keyword: searchKw.trim(), size: 15 })
      .then((res) => {
        if (cancelled) return
        const data: any = res.data
        const itemsArr = data?.items ?? data?.content ?? []
        setSearchOptions(
          itemsArr.map((it: any) => ({
            id: it.id,
            word: it.word,
            hiragana: it.hiragana,
            romaji: it.romaji,
            hanViet: it.hanViet,
            meaningVi: it.meaningVi,
            meaningEn: it.meaningEn,
            audioUrl: it.audioUrl,
            partOfSpeech: it.partOfSpeech,
            jlptLevel: it.jlptLevel,
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
   * Khi user chọn 1 vocab từ autocomplete:
   *  1) Gọi API lấy chi tiết từ vựng để có examples đầy đủ
   *  2) Tự điền các field custom (hiragana, romaji, audio, meaning)
   *  3) Auto-fill examples vào khung "ví dụ minh hoạ" — giáo viên vẫn có thể
   *     thêm/xoá/sửa ví dụ như bình thường (thiết kế cũ giữ nguyên)
   */
  const handleSelectVocab = async (vocabId: number) => {
    const v = searchOptions.find((o) => o.id === vocabId)
    if (!v) return

    // Optimistic: set selected + fill các field cơ bản ngay để UI không bị giật
    form.setFieldsValue({
      vocabId: v.id,
      customHiragana: v.hiragana ?? '',
      customRomaji: v.romaji ?? '',
      customAudioUrl: v.audioUrl ?? '',
      customMeaning: v.meaningVi ?? '',
    })
    setSelectedVocab(v)
    setSearchOptions([])
    setSearchKw('')

    // Fetch chi tiết để lấy examples (BE trả về { ..., examples: [...] }).
    // Dùng cờ `cancelled` để tránh race condition khi user click chọn nhanh
    // nhiều vocab liên tiếp.
    let cancelled = false
    setLoadingVocabDetail(true)
    try {
      const res = await dictionaryService.getVocabularyDetail(v.id)
      if (cancelled) return
      const detail = (res.data as VocabularyDetail) ?? null
      if (!detail) return

      // Cập nhật lại selectedVocab với data đầy đủ
      setSelectedVocab((prev) =>
        prev && prev.id === v.id
          ? {
              ...prev,
              ...detail,
              meaningVi: detail.meaningVi ?? prev.meaningVi,
            }
          : prev,
      )

      // Fill examples vào state — KHÔNG ghi đè nếu user đã chọn vocab khác rồi
      if (cancelled) return
      const dictExamples = detail.examples ?? []
      if (dictExamples.length > 0) {
        setExamples(
          dictExamples.map((ex, idx) => ({
            id: undefined, // đây là example từ từ điển, chưa gắn vào lesson item
            exampleJp: ex.exampleJp ?? '',
            exampleVi: ex.exampleVi ?? '',
            exampleRomaji: ex.exampleRomaji ?? '',
            audioUrl: ex.audioUrl ?? '',
            orderIndex: idx + 1,
            isPrimary: idx === 0,
          })),
        )
      }
    } catch {
      if (cancelled) return
      // Lỗi fetch detail → vẫn giữ vocab đã chọn, examples để trống
      message.warning(
        'Không tải được chi tiết từ vựng. Ví dụ minh hoạ sẽ để trống.',
      )
    } finally {
      if (!cancelled) setLoadingVocabDetail(false)
    }
  }

  /** Cho phép user đổi sang vocab khác */
  const handleClearVocab = () => {
    setSelectedVocab(null)
    form.setFieldsValue({
      vocabId: undefined,
      customHiragana: '',
      customRomaji: '',
      customAudioUrl: '',
      customMeaning: '',
    })
    // Reset examples khi đổi từ — để tránh ví dụ của từ cũ còn lẫn vào
    setExamples([])
  }

  // ========================================================================
  // Modal lifecycle
  // ========================================================================
  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setExamples([])
    setSelectedVocab(null)
    setSearchKw('')
    setSearchOptions([])
    setLoadingVocabDetail(false)
    setIsOpen(true)
  }

  const openEdit = (item: LessonVocabularyItem) => {
    setEditing(item)
    form.setFieldsValue({
      vocabId: item.vocabId,
      customHiragana: item.customHiragana ?? '',
      customRomaji: item.customRomaji ?? '',
      customAudioUrl: item.customAudioUrl ?? '',
      customMeaning: item.customMeaning ?? '',
      imageUrl: item.imageUrl ?? '',
      note: item.note ?? '',
      orderIndex: item.orderIndex,
    })
    setExamples(
      (item.examples ?? []).map((ex, idx) => ({
        id: ex.id,
        exampleJp: ex.exampleJp ?? '',
        exampleVi: ex.exampleVi ?? '',
        exampleRomaji: ex.exampleRomaji ?? '',
        audioUrl: ex.audioUrl ?? '',
        orderIndex: ex.orderIndex ?? idx + 1,
        isPrimary: ex.isPrimary ?? false,
      })),
    )
    // Hiển thị từ vựng đang edit trên panel (lấy từ item)
    if (item.vocabId) {
      setSelectedVocab({
        id: item.vocabId,
        word: item.word ?? '',
        hiragana: item.hiragana,
        romaji: item.romaji,
        hanViet: item.hanViet,
        meaningVi: item.meaningVi,
        meaningEn: item.meaningEn,
        audioUrl: item.displayAudioUrl,
      })
    }
    setIsOpen(true)
  }

  const closeModal = () => {
    setIsOpen(false)
    setEditing(null)
    form.resetFields()
    setExamples([])
    setSelectedVocab(null)
    setSearchKw('')
    setSearchOptions([])
    setLoadingVocabDetail(false)
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

      // Validate examples (nếu có thì phải đủ JP+VI)
      const cleanedExamples: ExampleRow[] = examples
        .filter((e) => e.exampleJp?.trim() || e.exampleVi?.trim())
        .map((e, idx) => ({
          ...e,
          orderIndex: e.orderIndex ?? idx + 1,
        }))

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
        // Update
        const payload: LessonVocabularyItemUpdateRequest = {
          orderIndex: data.orderIndex,
          customHiragana: data.customHiragana,
          customRomaji: data.customRomaji,
          customAudioUrl: data.customAudioUrl,
          customMeaning: data.customMeaning,
          imageUrl: data.imageUrl,
          note: data.note,
          // Map example rows → upsert request (giữ id cũ nếu có)
          examples: cleanedExamples.map<LessonVocabularyExampleUpsertRequest>((e) => ({
            id: e.id,
            exampleJp: e.exampleJp,
            exampleVi: e.exampleVi,
            exampleRomaji: e.exampleRomaji,
            audioUrl: e.audioUrl,
            orderIndex: e.orderIndex,
            isPrimary: e.isPrimary,
          })),
        }
        await updateVocabItem.mutateAsync({ id: editing.id, payload })
      } else {
        // Create
        const payload: LessonVocabularyItemCreateRequest = {
          vocabId: data.vocabId,
          orderIndex: data.orderIndex,
          customHiragana: data.customHiragana,
          customRomaji: data.customRomaji,
          customAudioUrl: data.customAudioUrl,
          customMeaning: data.customMeaning,
          imageUrl: data.imageUrl,
          note: data.note,
          // Nếu user tự nhập examples → dùng; nếu không → KHÔNG gửi để BE auto-copy từ từ điển
          ...(cleanedExamples.length > 0
            ? {
                examples: cleanedExamples.map<LessonVocabularyExampleCreateRequest>((e) => ({
                  exampleJp: e.exampleJp,
                  exampleVi: e.exampleVi,
                  exampleRomaji: e.exampleRomaji,
                  audioUrl: e.audioUrl,
                  orderIndex: e.orderIndex,
                  isPrimary: e.isPrimary,
                })),
              }
            : {}),
        }
        await createVocabItem.mutateAsync(payload)
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
  // Examples CRUD trong form
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
  const setPrimary = (idx: number) => {
    setExamples((prev) => prev.map((e, i) => ({ ...e, isPrimary: i === idx })))
  }

  const sorted = [...(items ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="font-semibold text-xs text-emerald-700 uppercase tracking-wider">
          Từ vựng ({items?.length ?? 0})
        </h4>
        <button
          type="button"
          onClick={openCreate}
          className="inline-flex items-center gap-1 px-2 py-1 rounded bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition"
        >
          <Plus className="w-3.5 h-3.5" /> Thêm từ
        </button>
      </div>

      {sorted.length === 0 && (
        <div className="py-4 text-center text-xs text-gray-400 border border-dashed border-gray-200 rounded-md">
          Chưa có từ vựng nào.
        </div>
      )}

      <div className="space-y-2">
        {sorted.map((item) => (
          <div
            key={item.id}
            className="flex items-center gap-3 p-2.5 bg-white rounded-md border border-gray-200/80 hover:border-emerald-300 transition group"
          >
            <div className="flex-1 min-w-0">
              <p className="font-bold text-sm text-gray-900">{item.word}</p>
              {item.displayHiragana && (
                <p className="text-xs text-gray-500">【{item.displayHiragana}】</p>
              )}
              {item.displayMeaning && (
                <p className="text-xs text-gray-600 mt-0.5">{item.displayMeaning}</p>
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
                className="p-1 text-gray-400 hover:text-emerald-600 rounded transition"
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
                onClick={() => deleteVocabItem.mutate(item.id)}
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
            <BookOpen className="w-5 h-5 text-emerald-600" />
            {editing ? 'Sửa từ vựng' : 'Thêm từ vựng mới'}
          </div>
        }
        okText={editing ? 'Lưu' : 'Thêm'}
        cancelText="Huỷ"
        confirmLoading={createVocabItem.isPending || updateVocabItem.isPending}
        destroyOnHidden
        okButtonProps={{ disabled: !editing && examples.length === 0 && false }}
      >
        <Form form={form} layout="vertical" className="pt-2">
          {/* ============================================================== */}
          {/* CREATE MODE: Tìm vocab từ từ điển */}
          {/* ============================================================== */}
          {!editing && !selectedVocab && (
            <div className="rounded-md border border-emerald-200 bg-emerald-50/50 p-3 mb-3 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-emerald-700 uppercase tracking-wider">
                <Search className="w-3.5 h-3.5" /> Tìm từ vựng từ từ điển
              </div>
              <Select
                showSearch
                size="large"
                className="!w-full"
                placeholder="Gõ để tìm (VD: 食べる, ăn, tabe...)"
                filterOption={false}
                searchValue={searchKw}
                onSearch={setSearchKw}
                onChange={handleSelectVocab}
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
                    <span className="font-semibold">{o.word}</span>
                    {o.hiragana && (
                      <span className="text-gray-400 ml-1">【{o.hiragana}】</span>
                    )}
                    {o.meaningVi && (
                      <span className="text-gray-500 ml-1">— {o.meaningVi}</span>
                    )}
                  </Select.Option>
                ))}
              </Select>
              <p className="text-[11px] text-emerald-700/70">
                💡 Sau khi chọn, các ô bên dưới sẽ tự điền từ từ điển. Bạn có thể sửa từng ô nếu muốn dùng giá trị riêng.
              </p>
              <Form.Item name="vocabId" hidden>
                <InputNumber />
              </Form.Item>
            </div>
          )}

          {/* ============================================================== */}
          {/* ĐÃ CHỌN VOCAB (cả create + edit): Hiển thị panel confirm */}
          {/* ============================================================== */}
          {selectedVocab && (
            <div className="rounded-md border-2 border-emerald-300 bg-emerald-50 p-3 mb-3">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2 text-xs font-bold text-emerald-700 uppercase tracking-wider">
                  <Link2 className="w-3.5 h-3.5" /> Từ vựng đã chọn
                </div>
                {!editing && (
                  <button
                    type="button"
                    onClick={handleClearVocab}
                    className="text-[11px] text-emerald-700 hover:text-rose-600 inline-flex items-center gap-1"
                  >
                    <XIcon className="w-3 h-3" /> Đổi từ khác
                  </button>
                )}
              </div>
              <div className="flex items-baseline gap-2">
                <p className="text-base font-bold text-gray-900">{selectedVocab.word}</p>
                {selectedVocab.hiragana && (
                  <p className="text-sm text-gray-500">【{selectedVocab.hiragana}】</p>
                )}
                {selectedVocab.hanViet && (
                  <p className="text-xs text-gray-400">({selectedVocab.hanViet})</p>
                )}
              </div>
              {selectedVocab.romaji && (
                <p className="text-xs text-gray-500 mt-0.5 italic">
                  {selectedVocab.romaji}
                </p>
              )}
              {(selectedVocab.meaningVi) && (
                <p className="text-sm text-gray-700 mt-1">
                  📖 {selectedVocab.meaningVi}
                </p>
              )}
              <Form.Item name="vocabId" hidden>
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
              <Form.Item name="customHiragana" label="Hiragana" className="!mb-0">
                <Input placeholder="VD: たべる" />
              </Form.Item>
              <Form.Item name="customRomaji" label="Romaji" className="!mb-0">
                <Input placeholder="VD: taberu" />
              </Form.Item>
              <Form.Item
                name="customAudioUrl"
                label={
                  <span className="flex items-center gap-1">
                    <Volume2 className="w-3 h-3" /> URL Audio
                  </span>
                }
                className="!mb-0"
              >
                <Input placeholder="https://..." />
              </Form.Item>
              <Form.Item name="imageUrl" label="URL ảnh minh hoạ" className="!mb-0">
                <Input placeholder="https://..." />
              </Form.Item>
              <Form.Item
                name="customMeaning"
                label="Nghĩa (VI)"
                className="!mb-0 !col-span-2"
              >
                <Input.TextArea rows={2} placeholder="VD: ăn, thưởng thức" />
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
                {loadingVocabDetail && (
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
                    className={`rounded border p-2 space-y-1.5 ${
                      ex.isPrimary
                        ? 'border-amber-300 bg-amber-50/80'
                        : 'border-gray-200 bg-white'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-bold text-indigo-600 uppercase">
                          #{idx + 1}
                        </span>
                        <button
                          type="button"
                          onClick={() => setPrimary(idx)}
                          className={`flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded ${
                            ex.isPrimary
                              ? 'bg-amber-500 text-white'
                              : 'bg-gray-100 text-gray-500 hover:bg-amber-100'
                          }`}
                          title={ex.isPrimary ? 'Đang là primary' : 'Đặt làm primary'}
                        >
                          <Star
                            className={`w-3 h-3 ${ex.isPrimary ? 'fill-current' : ''}`}
                          />
                          Primary
                        </button>
                      </div>
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
                      status={
                        ex.exampleJp?.trim() === '' ? 'error' : ''
                      }
                    />
                    <Input
                      size="small"
                      placeholder="Bản dịch tiếng Việt *"
                      value={ex.exampleVi}
                      onChange={(e) => updateExample(idx, { exampleVi: e.target.value })}
                      status={
                        ex.exampleVi?.trim() === '' ? 'error' : ''
                      }
                    />
                    <div className="grid grid-cols-2 gap-1.5">
                      <Input
                        size="small"
                        placeholder="Romaji (tuỳ chọn)"
                        value={ex.exampleRomaji}
                        onChange={(e) =>
                          updateExample(idx, { exampleRomaji: e.target.value })
                        }
                      />
                      <Input
                        size="small"
                        placeholder="Audio URL"
                        value={ex.audioUrl}
                        onChange={(e) => updateExample(idx, { audioUrl: e.target.value })}
                      />
                    </div>
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

      {/* Modal xem chi tiết vocab item */}
      <VocabItemDetailModal
        open={!!viewingItem}
        item={viewingItem}
        onClose={() => setViewingItem(null)}
      />
    </div>
  )
}
