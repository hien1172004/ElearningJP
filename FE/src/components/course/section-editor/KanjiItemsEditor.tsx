import React, { useState, useMemo, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  Plus,
  Pencil,
  Trash2,
  BookOpen,
  Eye,
  Search,
  Link2,
  PenTool,
  X as XIcon,
} from 'lucide-react'
import { Modal, Form, Input, InputNumber, App, Select, Empty, Spin, Tag, Button } from 'antd'
import { z } from 'zod'
import type {
  LessonKanjiItem,
  LessonKanjiItemCreateRequest,
  LessonKanjiItemUpdateRequest,
  LessonKanjiRelatedVocabRequest,
} from '@/types'
import type { KanjiSummary, KanjiDetail } from '@/types'
import { useKanjiItemMutations, useKanjiItemDetail } from '@/hooks'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { dictionaryService } from '@/services'
import { KanjiHandwritingModal } from '@/components/dictionary'

// =============================================================================
// Schema + types
// =============================================================================
const schema = z.object({
  character: z.string().min(1, 'Vui lòng nhập chữ Kanji').max(8),
  note: z.string().optional(),
  orderIndex: z.number().int().min(1).optional(),
})

type FormValues = z.infer<typeof schema>

interface KanjiItemsEditorProps {
  lessonId: number
  sectionId: number
  items: LessonKanjiItem[]
}

// =============================================================================
// Sub-component: modal xem chi tiết 1 kanji item
// =============================================================================
const KanjiItemDetailModal: React.FC<{
  open: boolean
  itemId: number | null
  onClose: () => void
}> = ({ open, itemId, onClose }) => {
  const { data, isLoading } = useKanjiItemDetail(itemId ?? undefined)

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      zIndex={1100}
      width={880}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <Eye className="w-5 h-5 text-amber-600" />
          <span>Chi tiết Kanji</span>
          {data?.character && (
            <span className="text-2xl font-serif text-amber-700 ml-2">
              {data.character}
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
      ) : !data ? (
        <Empty description="Không tìm thấy dữ liệu" />
      ) : (
        <div className="py-2 space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
            <div>
              <p className="text-[11px] text-gray-400 uppercase">Hán Việt</p>
              <p className="font-semibold">{data.hanViet || '—'}</p>
            </div>
            <div>
              <p className="text-[11px] text-gray-400 uppercase">Nghĩa VI</p>
              <p className="font-semibold">{data.meaningVi || '—'}</p>
            </div>
            <div>
              <p className="text-[11px] text-gray-400 uppercase">JLPT</p>
              <p className="font-semibold">{data.jlptLevel || '—'}</p>
            </div>
            <div>
              <p className="text-[11px] text-gray-400 uppercase">Âm On</p>
              <p className="font-semibold">{data.onyomi?.join('、') || '—'}</p>
            </div>
            <div>
              <p className="text-[11px] text-gray-400 uppercase">Âm Kun</p>
              <p className="font-semibold">{data.kunyomi?.join('、') || '—'}</p>
            </div>
            <div>
              <p className="text-[11px] text-gray-400 uppercase">Số nét</p>
              <p className="font-semibold">{data.strokeCount ?? '—'}</p>
            </div>
          </div>

          {data.note && (
            <div className="rounded bg-amber-50 border border-amber-200 p-3 text-sm">
              <p className="text-[11px] text-amber-700 uppercase font-bold mb-1">
                Ghi chú
              </p>
              <p className="text-gray-700 whitespace-pre-wrap">{data.note}</p>
            </div>
          )}

          <div>
            <p className="text-[11px] text-gray-400 uppercase font-bold mb-2">
              Từ vựng liên quan ({data.relatedVocabulary?.length ?? 0})
            </p>
            {data.relatedVocabulary && data.relatedVocabulary.length > 0 ? (
              <div className="space-y-1.5">
                {data.relatedVocabulary.map((rv) => (
                  <div
                    key={`${rv.vocabId}-${rv.id ?? 'auto'}`}
                    className="flex items-center justify-between p-2 rounded border border-gray-200 bg-white"
                  >
                    <div className="flex-1 min-w-0">
                      <span className="font-bold text-sm">{rv.word}</span>
                      {rv.hiragana && (
                        <span className="text-gray-500 text-xs ml-1">
                          【{rv.hiragana}】
                        </span>
                      )}
                      {rv.meaningVi && (
                        <span className="text-gray-600 text-xs ml-1">
                          — {rv.meaningVi}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      {rv.id === null && (
                        <Tag color="default" className="!m-0">
                          auto
                        </Tag>
                      )}
                      {rv.note && (
                        <Tag color="amber" className="!m-0">
                          {rv.note}
                        </Tag>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-gray-400 italic">
                Chưa có từ vựng liên quan.
              </p>
            )}
          </div>
        </div>
      )}
    </Modal>
  )
}

// =============================================================================
// Main editor
// =============================================================================
export const KanjiItemsEditor: React.FC<KanjiItemsEditorProps> = ({
  lessonId,
  sectionId,
  items,
}) => {
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [isOpen, setIsOpen] = useState(false)
  const [editing, setEditing] = useState<LessonKanjiItem | null>(null)
  const [viewingId, setViewingId] = useState<number | null>(null)

  // Kanji đã chọn
  const [selectedKanji, setSelectedKanji] = useState<KanjiDetail | null>(null)

  // Modal vẽ tay để tìm kanji
  const [isHandwritingOpen, setIsHandwritingOpen] = useState(false)

  // Related vocabulary — chọn từ allRelatedWords của kanji (multi-select)
  const [relatedVocabIds, setRelatedVocabIds] = useState<number[]>([])

  // Form tạo vocab mới
  const [newVocab, setNewVocab] = useState<{
    word: string
    hiragana: string
    romaji: string
    meaningVi: string
    meaningEn: string
    partOfSpeech: string[]
  }>({
    word: '',
    hiragana: '',
    romaji: '',
    meaningVi: '',
    meaningEn: '',
    partOfSpeech: [],
  })
  const [showNewVocab, setShowNewVocab] = useState(false)

  // 5 loại từ chuẩn — BE lưu thành 1 string CSV
  const PART_OF_SPEECH_OPTIONS = useMemo(
    () => [
      { value: 'NOUN', label: 'Danh từ (名詞)' },
      { value: 'VERB', label: 'Động từ (動詞)' },
      { value: 'ADJECTIVE', label: 'Tính từ (形容詞)' },
      { value: 'ADVERB', label: 'Phó từ (副詞)' },
      { value: 'CONJUNCTION', label: 'Liên từ (接続詞)' },
    ],
    [],
  )

  const { createKanjiItem, updateKanjiItem, deleteKanjiItem } =
    useKanjiItemMutations(lessonId, sectionId)

  const queryClient = useQueryClient()

  /** Chọn / bỏ chọn 1 vocab từ allRelatedWords (multi-select) */
  const handleToggleRelatedVocab = (vocabId: number) => {
    setRelatedVocabIds((prev) =>
      prev.includes(vocabId)
        ? prev.filter((id) => id !== vocabId)
        : [...prev, vocabId],
    )
  }

  // ========================================================================
  // Search kanji từ từ điển (cho phần chọn kanji ở create)
  // ========================================================================
  const [kanjiSearchKw, setKanjiSearchKw] = useState('')
  const [kanjiSearchOptions, setKanjiSearchOptions] = useState<KanjiSummary[]>([])
  const [kanjiSearching, setKanjiSearching] = useState(false)

  useEffect(() => {
    if (editing || kanjiSearchKw.trim().length === 0) {
      setKanjiSearchOptions([])
      return
    }
    let cancelled = false
    setKanjiSearching(true)
    dictionaryService
      .searchKanjis({ keyword: kanjiSearchKw.trim(), size: 15 })
      .then((res) => {
        if (cancelled) return
        const data: any = res.data
        const itemsArr = data?.items ?? data?.content ?? []
        setKanjiSearchOptions(
          itemsArr.map((it: any) => ({
            id: it.id,
            character: it.character,
            strokeCount: it.strokeCount,
            onyomi: it.onyomi,
            kunyomi: it.kunyomi,
            hanViet: it.hanViet,
            meaningVi: it.meaningVi,
            jlptLevel: it.jlptLevel,
            radicals: it.radicals,
          })),
        )
      })
      .catch(() => {
        if (!cancelled) setKanjiSearchOptions([])
      })
      .finally(() => !cancelled && setKanjiSearching(false))
    return () => {
      cancelled = true
    }
  }, [kanjiSearchKw, editing])

  /** Chọn 1 kanji từ search → load chi tiết */
  const handleSelectKanji = async (kanjiId: number) => {
    const k = kanjiSearchOptions.find((o) => o.id === kanjiId)
    if (!k) return
    form.setFieldsValue({ character: k.character })
    try {
      const res = await dictionaryService.getKanjiDetail(k.character)
      const detail = (res.data as KanjiDetail) ?? null
      setSelectedKanji(detail)
    } catch {
      setSelectedKanji({ ...(k as KanjiDetail), strokes: [] })
    }
    setKanjiSearchOptions([])
    setKanjiSearchKw('')
  }

  /** Nhận ký tự từ modal vẽ tay → load chi tiết kanji */
  const handleHandwritingSelect = async (char: string) => {
    const trimmed = char.trim()
    if (!trimmed) return
    form.setFieldsValue({ character: trimmed[0] })
    try {
      const res = await dictionaryService.getKanjiDetail(trimmed[0])
      const detail = (res.data as KanjiDetail) ?? null
      if (detail) {
        setSelectedKanji(detail)
      } else {
        // Fallback tối thiểu nếu BE không trả về
        setSelectedKanji({
          id: 0,
          character: trimmed[0],
          strokes: [],
        } as unknown as KanjiDetail)
      }
    } catch {
      message.error(`Không tìm thấy chữ "${trimmed[0]}" trong từ điển`)
    }
  }

  const handleClearKanji = () => {
    setSelectedKanji(null)
    form.setFieldsValue({ character: '' })
  }

  // ========================================================================
  // Load related vocab ids khi mở edit (chỉ cần vocabId vì UI render
  // trực tiếp từ selectedKanji.allRelatedWords)
  // ========================================================================
  const loadRelatedVocabIds = (fullItem: LessonKanjiItem) => {
    const allRVs = fullItem.relatedVocabulary ?? []
    const allVocabIds = allRVs
      .map((rv) => rv.vocabId)
      .filter((v): v is number => v != null)
    setRelatedVocabIds(allVocabIds)
  }

  // ========================================================================
  // Modal lifecycle
  // ========================================================================
  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    setRelatedVocabIds([])
    setNewVocab({
      word: '',
      hiragana: '',
      romaji: '',
      meaningVi: '',
      meaningEn: '',
      partOfSpeech: [],
    })
    setShowNewVocab(false)
    setSelectedKanji(null)
    setKanjiSearchKw('')
    setKanjiSearchOptions([])
    setIsOpen(true)
  }

  const openEdit = async (item: LessonKanjiItem) => {
    setEditing(item)
    form.setFieldsValue({
      character: item.character,
      note: item.note,
      orderIndex: item.orderIndex,
    })
    setKanjiSearchKw('')
    setKanjiSearchOptions([])

    // Load kanji detail
    if (item.character) {
      try {
        const res = await dictionaryService.getKanjiDetail(item.character)
        setSelectedKanji((res.data as KanjiDetail) ?? null)
      } catch {
        setSelectedKanji(null)
      }
    } else {
      setSelectedKanji(null)
    }

    // Fetch full item với relatedVocabulary đầy đủ
    let fullItem: LessonKanjiItem = item
    try {
      const fresh = await queryClient.ensureQueryData<LessonKanjiItem | null>({
        queryKey: QUERY_KEYS.lesson.kanjiItem(item.id),
        queryFn: async () => {
          const res = await import('@/services').then((m) =>
            m.kanjiItemService.getItemDetail(item.id),
          )
          return res.data ?? null
        },
        staleTime: 0,
      })
      if (fresh) fullItem = fresh
    } catch {
      // fallback: dùng item trong list
    }

    await loadRelatedVocabIds(fullItem)
    setNewVocab({
      word: '',
      hiragana: '',
      romaji: '',
      meaningVi: '',
      meaningEn: '',
      partOfSpeech: [],
    })
    setShowNewVocab(false)
    setIsOpen(true)
  }

  const closeModal = () => {
    setIsOpen(false)
    setEditing(null)
    setRelatedVocabIds([])
    setNewVocab({
      word: '',
      hiragana: '',
      romaji: '',
      meaningVi: '',
      meaningEn: '',
      partOfSpeech: [],
    })
    setShowNewVocab(false)
    setSelectedKanji(null)
    setKanjiSearchKw('')
    setKanjiSearchOptions([])
    form.resetFields()
  }

  // ========================================================================
  // Submit
  // ========================================================================
  const handleSubmit = async () => {
    try {
      const values = (await form.validateFields()) as FormValues
      const parsed = schema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      const v = parsed.data

      // Build relatedVocabulary payload — 1 entry duy nhất
      const relatedPayload: LessonKanjiRelatedVocabRequest[] = []

      const validVocabIds = relatedVocabIds.filter((id): id is number => id != null && id > 0)
      const hasNewVocab =
        newVocab.word.trim() && newVocab.hiragana.trim()

      if (validVocabIds.length > 0 || hasNewVocab) {
        const entry: LessonKanjiRelatedVocabRequest = {}

        if (validVocabIds.length > 0) {
          entry.vocabIds = validVocabIds
        }

        if (hasNewVocab) {
          entry.newVocab = {
            word: newVocab.word.trim(),
            hiragana: newVocab.hiragana.trim(),
            romaji: newVocab.romaji.trim() || undefined,
            meaningVi: newVocab.meaningVi.trim() || undefined,
            meaningEn: newVocab.meaningEn.trim() || undefined,
            partOfSpeech:
              newVocab.partOfSpeech.length > 0
                ? newVocab.partOfSpeech.join(',')
                : undefined,
          }
        }

        relatedPayload.push(entry)
      }

      if (editing) {
        const payload: LessonKanjiItemUpdateRequest = {
          note: v.note,
          orderIndex: v.orderIndex,
          relatedVocabulary:
            relatedPayload.length > 0 ? relatedPayload : undefined,
        }
        await updateKanjiItem.mutateAsync({ id: editing.id, payload })
      } else {
        const payload: LessonKanjiItemCreateRequest = {
          character: v.character,
          note: v.note,
          orderIndex: v.orderIndex,
          relatedVocabulary:
            relatedPayload.length > 0 ? relatedPayload : undefined,
        }
        await createKanjiItem.mutateAsync(payload)
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

  const sorted = useMemo(
    () =>
      [...(items ?? [])].sort(
        (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0),
      ),
    [items],
  )

  return (
    <div className="space-y-3">
      {/* ============================================================== */}
      {/* Header + list items */}
      {/* ============================================================== */}
      <div className="flex items-center justify-between">
        <h4 className="font-semibold text-xs text-amber-700 uppercase tracking-wider">
          Kanji Items ({items?.length ?? 0})
        </h4>
        <button
          type="button"
          onClick={openCreate}
          className="inline-flex items-center gap-1 px-2 py-1 rounded bg-amber-600 text-white text-xs font-bold hover:bg-amber-700 transition"
        >
          <Plus className="w-3.5 h-3.5" /> Thêm Kanji
        </button>
      </div>

      {sorted.length === 0 && (
        <div className="py-4 text-center text-xs text-gray-400 border border-dashed border-gray-200 rounded-md">
          Chưa có Kanji nào.
        </div>
      )}

      <div className="space-y-2">
        {sorted.map((item) => (
          <div
            key={item.id}
            className="flex items-center gap-3 p-2.5 bg-white rounded-md border border-gray-200/80 hover:border-amber-300 transition group"
          >
            <div className="w-10 h-10 rounded bg-amber-50 text-amber-900 font-serif font-bold text-lg flex items-center justify-center border border-amber-200 shrink-0">
              {item.character ?? '?'}
            </div>
            <div className="flex-1 min-w-0">
              {item.hanViet && (
                <p className="text-xs text-gray-700 font-semibold">
                  {item.hanViet}
                </p>
              )}
              {item.meaningVi && (
                <p className="text-[11px] text-gray-500 truncate">
                  {item.meaningVi}
                </p>
              )}
              {item.note && (
                <p className="text-[11px] text-gray-400 truncate italic">
                  {item.note}
                </p>
              )}
              {(item.relatedVocabulary?.length ?? 0) > 0 && (
                <p className="text-[10px] text-amber-600 mt-0.5">
                  {item.relatedVocabulary!.length} từ vựng liên quan
                </p>
              )}
            </div>
            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition">
              <button
                type="button"
                onClick={() => setViewingId(item.id)}
                className="p-1 text-gray-400 hover:text-amber-600 rounded transition"
                title="Xem chi tiết"
              >
                <Eye className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() => openEdit(item)}
                className="p-1 text-gray-400 hover:text-indigo-600 rounded transition"
                title="Sửa"
              >
                <Pencil className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() => deleteKanjiItem.mutate(item.id)}
                className="p-1 text-gray-400 hover:text-rose-600 rounded transition"
                title="Xoá"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* ============================================================== */}
      {/* Edit / Create modal — 1 trang duy nhất */}
      {/* ============================================================== */}
      <Modal
        open={isOpen}
        onCancel={closeModal}
        onOk={handleSubmit}
        zIndex={1100}
        title={
          <div className="flex items-center gap-2 font-bold text-gray-900">
            <BookOpen className="w-5 h-5 text-amber-600" />
            {editing
              ? `Sửa Kanji ${editing.character ?? ''}`
              : 'Thêm Kanji mới'}
          </div>
        }
        okText={editing ? 'Lưu' : 'Thêm'}
        cancelText="Huỷ"
        confirmLoading={createKanjiItem.isPending || updateKanjiItem.isPending}
        width={880}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" className="pt-2">
          {/* ============================================================== */}
          {/* CREATE MODE: Tìm kanji từ từ điển */}
          {/* ============================================================== */}
          {!editing && !selectedKanji && (
            <div className="rounded-md border border-amber-200 bg-amber-50/50 p-3 mb-3 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-amber-700 uppercase tracking-wider">
                <Search className="w-3.5 h-3.5" /> Tìm Kanji từ từ điển
              </div>
              <div className="flex items-stretch gap-2">
                <Select
                  showSearch
                  size="large"
                  className="!flex-1"
                  placeholder="Gõ để tìm (VD: 日, 学, nhat, nhật...)"
                  filterOption={false}
                  searchValue={kanjiSearchKw}
                  onSearch={setKanjiSearchKw}
                  onChange={handleSelectKanji}
                  value={null}
                  notFoundContent={
                    kanjiSearching ? (
                      <Spin size="small" />
                    ) : kanjiSearchKw.trim() ? (
                      <Empty description="Không tìm thấy" />
                    ) : (
                      <span className="text-xs text-gray-400">
                        Gõ ít nhất 1 ký tự...
                      </span>
                    )
                  }
                >
                  {kanjiSearchOptions.map((o) => (
                    <Select.Option key={o.id} value={o.id}>
                      <span className="font-serif font-bold text-lg mr-2">
                        {o.character}
                      </span>
                      {o.hanViet && (
                        <span className="text-gray-700">{o.hanViet}</span>
                      )}
                      {o.meaningVi && (
                        <span className="text-gray-500 ml-1">— {o.meaningVi}</span>
                      )}
                      {o.jlptLevel && (
                        <span className="ml-2 text-[10px] font-bold text-amber-600 uppercase">
                          {o.jlptLevel}
                        </span>
                      )}
                    </Select.Option>
                  ))}
                </Select>
                <Button
                  type="default"
                  size="large"
                  onClick={() => setIsHandwritingOpen(true)}
                  className="!flex !items-center !gap-1.5 !shrink-0"
                  title="Vẽ tay để tìm kanji"
                >
                  <PenTool className="w-4 h-4 text-amber-600" />
                  <span className="font-semibold">Vẽ Kanji</span>
                </Button>
              </div>
              <p className="text-[11px] text-amber-700/70">
                💡 Gõ chữ / Hán Việt / nghĩa để tìm, hoặc bấm{' '}
                <b>"Vẽ Kanji"</b> để vẽ tay. Sau khi chọn, panel bên dưới sẽ
                hiển thị chi tiết Kanji. Bấm "Đổi Kanji" để chọn lại.
              </p>
            </div>
          )}

          {/* ============================================================== */}
          {/* KANJI ĐÃ CHỌN (cả create + edit) */}
          {/* ============================================================== */}
          {selectedKanji && (
            <div className="rounded-md border-2 border-amber-300 bg-amber-50 p-3 mb-3">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2 text-xs font-bold text-amber-700 uppercase tracking-wider">
                  <Link2 className="w-3.5 h-3.5" /> Kanji đã chọn
                </div>
                <button
                  type="button"
                  onClick={handleClearKanji}
                  className="text-[11px] text-amber-700 hover:text-rose-600 inline-flex items-center gap-1"
                >
                  <XIcon className="w-3 h-3" /> Đổi Kanji
                </button>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-16 h-16 rounded bg-white text-amber-900 font-serif font-bold text-4xl flex items-center justify-center border-2 border-amber-300 shrink-0">
                  {selectedKanji.character}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-baseline gap-2 flex-wrap">
                    {selectedKanji.hanViet && (
                      <p className="text-base font-bold text-gray-900">
                        {selectedKanji.hanViet}
                      </p>
                    )}
                    {selectedKanji.jlptLevel && (
                      <span className="text-[10px] font-bold text-amber-700 uppercase">
                        {selectedKanji.jlptLevel}
                      </span>
                    )}
                    {selectedKanji.strokeCount != null && (
                      <span className="text-[10px] text-gray-500">
                        {selectedKanji.strokeCount} nét
                      </span>
                    )}
                  </div>
                  {selectedKanji.meaningVi && (
                    <p className="text-sm text-gray-700 mt-1">
                      📖 {selectedKanji.meaningVi}
                    </p>
                  )}
                  {(selectedKanji.onyomi?.length ||
                    selectedKanji.kunyomi?.length) && (
                    <div className="flex gap-3 mt-1 text-[11px] text-gray-600">
                      {selectedKanji.onyomi?.length ? (
                        <span>
                          <span className="text-gray-400">On: </span>
                          <span className="font-semibold">
                            {selectedKanji.onyomi.join('、')}
                          </span>
                        </span>
                      ) : null}
                      {selectedKanji.kunyomi?.length ? (
                        <span>
                          <span className="text-gray-400">Kun: </span>
                          <span className="font-semibold">
                            {selectedKanji.kunyomi.join('、')}
                          </span>
                        </span>
                      ) : null}
                    </div>
                  )}
                </div>
              </div>
              <Form.Item name="character" hidden>
                <Input />
              </Form.Item>
            </div>
          )}

          {/* ============================================================== */}
          {/* TỪ VỰNG LIÊN QUAN — multi-select từ allRelatedWords */}
          {/* ============================================================== */}
          <div className="rounded-md border border-indigo-200 bg-indigo-50/50 p-3 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-bold text-indigo-700 uppercase tracking-wider">
                <BookOpen className="w-3.5 h-3.5" /> Từ vựng liên quan
                {relatedVocabIds.length > 0 && (
                  <span className="ml-1 inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full bg-indigo-600 text-white text-[10px]">
                    {relatedVocabIds.length}
                  </span>
                )}
              </div>
              {/* Nút "Thêm từ mới" → toggle form (giữ nguyên logic cũ) */}
              <button
                type="button"
                onClick={() => setShowNewVocab((v) => !v)}
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-indigo-600 text-white text-[11px] font-bold hover:bg-indigo-700"
                title="Thêm vocab mới chưa có trong từ điển"
              >
                <Plus className="w-3 h-3" /> Thêm từ mới
              </button>
            </div>

            {/* Multi-select từ allRelatedWords */}
            {selectedKanji?.allRelatedWords &&
            selectedKanji.allRelatedWords.length > 0 ? (
              <div className="space-y-1.5">
                <div className="text-[11px] text-gray-500 px-1">
                  Chọn các từ vựng liên quan tới chữ Kanji này (
                  {selectedKanji.allRelatedWords.length} tùy chọn):
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 max-h-72 overflow-y-auto pr-1">
                  {selectedKanji.allRelatedWords.map((w) => {
                    const checked = relatedVocabIds.includes(w.vocabId)
                    return (
                      <label
                        key={w.vocabId}
                        className={`flex items-start gap-2 p-2 rounded border cursor-pointer transition select-none ${
                          checked
                            ? 'border-indigo-500 bg-indigo-100/80 ring-1 ring-indigo-300'
                            : 'border-gray-200 bg-white hover:border-indigo-300 hover:bg-indigo-50/40'
                        }`}
                      >
                        <input
                          type="checkbox"
                          className="mt-1 w-4 h-4 accent-indigo-600 shrink-0"
                          checked={checked}
                          onChange={() => handleToggleRelatedVocab(w.vocabId)}
                        />
                        <div className="flex-1 min-w-0">
                          <div className="flex items-baseline gap-1.5 flex-wrap">
                            <span className="font-semibold text-sm text-gray-900">
                              {w.word}
                            </span>
                            {w.hiragana && (
                              <span className="text-gray-500 text-xs">
                                【{w.hiragana}】
                              </span>
                            )}
                            {w.jlptLevel && (
                              <span className="text-[10px] font-bold text-amber-600 uppercase">
                                {w.jlptLevel}
                              </span>
                            )}
                            {w.readingType && (
                              <span className="text-[10px] px-1 rounded bg-gray-100 text-gray-500 uppercase">
                                {w.readingType}
                              </span>
                            )}
                          </div>
                          {w.meaningVi && (
                            <p className="text-[11px] text-gray-600 mt-0.5 line-clamp-2">
                              {w.meaningVi}
                            </p>
                          )}
                        </div>
                      </label>
                    )
                  })}
                </div>
              </div>
            ) : (
              <div className="text-[11px] text-gray-500 italic px-1 py-2">
                Chữ Kanji này chưa có từ vựng liên quan nào trong từ điển.
                Bạn có thể bấm <b>“Thêm từ mới”</b> ở góc trên để tạo mới.
              </div>
            )}

            {/* Form tạo vocab mới — giữ nguyên logic */}
            {showNewVocab && (
              <div className="rounded border border-indigo-300 bg-white p-3 space-y-2">
                <div className="text-[11px] font-bold text-indigo-700 uppercase tracking-wider mb-1">
                  Tạo từ mới
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <Input
                    placeholder="Từ vựng (VD: 学生) *"
                    value={newVocab.word}
                    onChange={(e) =>
                      setNewVocab((v) => ({ ...v, word: e.target.value }))
                    }
                    status={
                      !newVocab.word.trim() && showNewVocab ? 'error' : ''
                    }
                  />
                  <Input
                    placeholder="Hiragana (VD: がくせい) *"
                    value={newVocab.hiragana}
                    onChange={(e) =>
                      setNewVocab((v) => ({ ...v, hiragana: e.target.value }))
                    }
                    status={
                      !newVocab.hiragana.trim() && showNewVocab ? 'error' : ''
                    }
                  />
                  <Input
                    placeholder="Romaji (VD: gakusei)"
                    value={newVocab.romaji}
                    onChange={(e) =>
                      setNewVocab((v) => ({ ...v, romaji: e.target.value }))
                    }
                  />
                  <Select
                    mode="multiple"
                    allowClear
                    size="middle"
                    className="!w-full"
                    placeholder="Chọn loại từ (có thể chọn nhiều)"
                    value={newVocab.partOfSpeech}
                    onChange={(vals: string[]) =>
                      setNewVocab((v) => ({ ...v, partOfSpeech: vals }))
                    }
                    options={PART_OF_SPEECH_OPTIONS}
                    maxTagCount="responsive"
                  />
                  <Input
                    placeholder="Nghĩa VI"
                    value={newVocab.meaningVi}
                    onChange={(e) =>
                      setNewVocab((v) => ({
                        ...v,
                        meaningVi: e.target.value,
                      }))
                    }
                    className="!col-span-2"
                  />
                </div>
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={() =>
                      setNewVocab({
                        word: '',
                        hiragana: '',
                        romaji: '',
                        meaningVi: '',
                        meaningEn: '',
                        partOfSpeech: [],
                      })
                    }
                    className="text-[11px] text-gray-400 hover:text-rose-600"
                  >
                    <XIcon className="w-3 h-3 inline mr-0.5" />
                    Xoá
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* ============================================================== */}
          {/* GHI CHÚ + THỨ TỰ */}
          {/* ============================================================== */}
          <div className={`grid ${editing ? 'grid-cols-3' : 'grid-cols-1'} gap-2 mt-3`}>
            <Form.Item
              name="note"
              label="Ghi chú"
              className={`!${editing ? 'col-span-2' : 'col-span-full'} !mb-0`}
            >
              <Input.TextArea
                rows={2}
                placeholder="Ghi chú của giáo viên..."
              />
            </Form.Item>
            {editing && (
              <Form.Item name="orderIndex" label="Thứ tự" className="!mb-0">
                <InputNumber min={1} className="!w-full" placeholder="Tự gán" />
              </Form.Item>
            )}
          </div>
        </Form>
      </Modal>

      {/* Modal xem chi tiết */}
      <KanjiItemDetailModal
        open={!!viewingId}
        itemId={viewingId}
        onClose={() => setViewingId(null)}
      />

      {/* Modal vẽ tay tìm kanji */}
      <KanjiHandwritingModal
        isOpen={isHandwritingOpen}
        onClose={() => setIsHandwritingOpen(false)}
        onSelectCharacter={(char) => {
          setIsHandwritingOpen(false)
          handleHandwritingSelect(char)
        }}
      />
    </div>
  )
}
