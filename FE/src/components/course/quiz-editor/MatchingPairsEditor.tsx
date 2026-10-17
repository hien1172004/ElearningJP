import React, { useEffect, useMemo, useState } from 'react'
import { Plus, X as XIcon, Link2, Shuffle, Sparkles, Wand2 } from 'lucide-react'
import { Input, Select, Button, AutoComplete, Tag, App } from 'antd'
import type { LessonDetail } from '@/types'
import {
  buildCandidatesFromLesson,
  autoPairCandidates,
  filterCandidatesByKind,
  getKindTagColor,
  getKindLabel,
  type MatchingCandidate,
  type CandidateKind,
} from './matchingFromLesson'

/**
 * ============================================================================
 * MATCHING — 2 cột nối nhau
 * ============================================================================
 *
 * Cấu trúc dữ liệu (khớp với BE QuizQuestionCreateRequest.matchingData):
 *   matchingData = {
 *     left:  [ { key, label }, ... ],   // cột trái
 *     right: [ { key, label }, ... ],   // cột phải
 *   }
 *
 * Đáp án đúng: left[i] ↔ right[i] (cùng index). Khi xáo, chỉ xáo
 * CỘT PHẢI cho học viên; giáo viên thấy cặp đúng theo index.
 *
 * Có 2 mode:
 * 1. EDITOR (`mode="editor"`):
 *    - Giáo viên nhập 2 cột song song. Mỗi dòng = 1 cặp theo index.
 *    - Hai cột nối nhau bằng icon 🔗 + đường kẻ giữa.
 *    - Nút "+ Thêm cặp" (thêm 1 dòng mới cho cả 2 cột).
 *
 * 2. VIEWER (`mode="viewer"`):
 *    - Học viên thấy cột trái cố định, cột phải đã được shuffle
 *      (BE tự shuffle qua options, hoặc FE tự shuffle ở client).
 *    - Với mỗi vế trái, chọn 1 vế phải qua <Select>.
 *    - Cột phải có thể shuffle lại bằng nút "Xáo cột phải".
 *    - Đáp án đúng suy ra từ left[i].key ↔ right[i].key.
 *
 * Cấu trúc matchKey BE dùng để chấm:
 *   matchKey = "L{i}=R{j}"  (i = index vế trái, j = index vế phải của cặp đúng)
 *   optionText = "leftLabel = rightLabel"
 *   userPairs[matchKey] === optionText ⇒ đúng.
 *
 * Khi hiển thị cho học viên, mảng `right` đã được shuffle sẵn lúc save (mỗi
 * lần lưu có 1 thứ tự khác nhau) → viewer chỉ cần duyệt `right` theo index
 * tự nhiên, không cần `displayOrder` riêng.
 * ============================================================================
 */

/** Cấu trúc BE đang lưu trong JsonNode. */
export interface MatchingSide {
  key: string
  label: string
}

export interface MatchingData {
  left: MatchingSide[]
  right: MatchingSide[]
}

interface MatchingPairsEditorProps {
  value?: MatchingData
  onChange?: (data: MatchingData) => void
  /** userAnswers dành cho viewer mode: { leftKey: rightKey } */
  userAnswers?: Record<string, string>
  /** Báo cho parent biết userAnswers đã đổi. */
  onUserAnswerChange?: (answers: Record<string, string>) => void
  /** editor = giáo viên tạo/sửa; viewer = học viên làm bài. */
  mode: 'editor' | 'viewer'
  /** Trạng thái viewer: cho biết đáp án đúng để tô màu. */
  showCorrect?: boolean
  /**
   * (MỚI — chỉ dùng ở editor mode)
   * Lesson hiện tại. Khi truyền vào, editor sẽ trích tất cả items
   * (Kanji + Vocab + Grammar) thành danh sách candidate để:
   *   - Hiển thị dropdown search trong mỗi dòng.
   *   - Cho phép 1 click "Tự động lấy tất cả" để sinh cặp ghép.
   */
  lesson?: LessonDetail | null
  /** Báo cho parent biết đã auto-fill (vd: để hiển thị toast). */
  onAutoFilled?: (pairCount: number) => void
}

// ============================================================================
// Helpers
// ============================================================================
const emptySide = (key: string, label = ''): MatchingSide => ({ key, label })

/** Tạo cặp mới (1 dòng cho cả left và right). */
const newPair = (n: number): { left: MatchingSide; right: MatchingSide } => ({
  left: emptySide(`L${n}`, ''),
  right: emptySide(`R${n}`, ''),
})

/** Shuffle mảng (Fisher-Yates) — dùng cho viewer để xáo cột phải. */
function shuffle<T>(arr: T[]): T[] {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

// ============================================================================
// Component chính
// ============================================================================
export const MatchingPairsEditor: React.FC<MatchingPairsEditorProps> = ({
  value,
  onChange,
  userAnswers = {},
  onUserAnswerChange,
  mode,
  showCorrect = false,
  lesson,
  onAutoFilled,
}) => {
  const { message } = App.useApp()
  const left = value?.left ?? []
  const right = value?.right ?? []
  const [shuffledRight, setShuffledRight] = useState<MatchingSide[]>([])

  // Filter loại candidate: ALL / KANJI / VOCAB / GRAMMAR
  const [kindFilter, setKindFilter] = useState<'ALL' | CandidateKind>('ALL')

  // Tất cả candidate từ lesson (memoized theo lesson)
  const allCandidates = useMemo<MatchingCandidate[]>(
    () => (mode === 'editor' ? buildCandidatesFromLesson(lesson) : []),
    [lesson, mode],
  )

  // Candidate theo filter hiện tại
  const filteredCandidates = useMemo<MatchingCandidate[]>(
    () => filterCandidatesByKind(allCandidates, kindFilter),
    [allCandidates, kindFilter],
  )

  /**
   * Map từ `key` của left/right (vd: "K_42") → candidate gốc.
   * Giúp PairRow biết cần "khóa" ô đó thành dropdown hay cho gõ tay.
   */
  const candidateByKey = useMemo(() => {
    const m = new Map<string, MatchingCandidate>()
    for (const c of allCandidates) m.set(c.key, c)
    return m
  }, [allCandidates])

  // Khi vào viewer, shuffle cột phải 1 lần dựa trên data hiện tại.
  useEffect(() => {
    if (mode === 'viewer') {
      setShuffledRight(shuffle(right))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode, right.length])

  /**
   * 1 click → tạo cặp left/right từ tất cả items trong lesson.
   * - Mặc định: word-to-meaning (từ/ký tự ↔ nghĩa Việt).
   * - Nếu filter là 1 loại cụ thể → chỉ lấy loại đó.
   * - KHÔNG shuffle `right` ở đây — editor hiển thị theo cặp đúng
   *   (L1↔R1, L2↔R2, ...) để teacher dễ kiểm tra. Việc shuffle mảng right
   *   để hiển thị thứ tự xáo cho học viên được làm ở QuizPanel khi save.
   */
  const handleAutoFill = () => {
    if (filteredCandidates.length < 2) {
      message.warning('Cần ít nhất 2 mục trong lesson để tạo cặp ghép.')
      return
    }
    const paired = autoPairCandidates(filteredCandidates, 'word-to-meaning')
    onChange?.(paired)
    onAutoFilled?.(paired.left.length)
    message.success(
      `Đã tạo ${paired.left.length} cặp ghép từ ${
        kindFilter === 'ALL' ? 'tất cả' : getKindLabel(kindFilter).toLowerCase()
      } trong lesson.`,
    )
  }

  // ==========================================================================
  // EDITOR MODE
  // ==========================================================================
  if (mode === 'editor') {
    const rowCount = Math.max(left.length, right.length)
    // Tạo mảng dòng hiển thị — zip theo index
    const rows = Array.from({ length: rowCount }, (_, i) => ({
      left: left[i] ?? emptySide(`L${i + 1}`, ''),
      right: right[i] ?? emptySide(`R${i + 1}`, ''),
    }))

    const updateRow = (idx: number, updated: { left: MatchingSide; right: MatchingSide }) => {
      const newLeft = [...left]
      const newRight = [...right]
      // pad nếu cần
      while (newLeft.length <= idx) newLeft.push(emptySide(`L${newLeft.length + 1}`, ''))
      while (newRight.length <= idx) newRight.push(emptySide(`R${newRight.length + 1}`, ''))
      newLeft[idx] = updated.left
      newRight[idx] = updated.right
      onChange?.({ left: newLeft, right: newRight })
    }

    const addRow = () => {
      const n = Math.max(left.length, right.length) + 1
      const row = newPair(n)
      onChange?.({
        left: [...left, row.left],
        right: [...right, row.right],
      })
    }

    const removeRow = (idx: number) => {
      const newLeft = left.filter((_, i) => i !== idx).map((s, i) => ({ ...s, key: `L${i + 1}` }))
      const newRight = right.filter((_, i) => i !== idx).map((s, i) => ({ ...s, key: `R${i + 1}` }))
      onChange?.({ left: newLeft, right: newRight })
    }

    return (
      <div className="space-y-2">
        {/* ============================================================ */}
        {/* Toolbar: lấy dữ liệu từ lesson (MỚI) */}
        {/* ============================================================ */}
        {allCandidates.length > 0 && (
          <div className="rounded-md border border-indigo-200 bg-gradient-to-r from-indigo-50/60 to-violet-50/40 p-2.5 space-y-2">
            <div className="flex items-center gap-2 flex-wrap">
              <Wand2 className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
              <span className="text-[11px] font-bold text-indigo-700">
                Dữ liệu từ bài học:
              </span>
              <div className="flex items-center gap-1 flex-wrap">
                {(['ALL', 'KANJI', 'VOCAB', 'GRAMMAR'] as const).map((k) => {
                  const count =
                    k === 'ALL'
                      ? allCandidates.length
                      : allCandidates.filter((c) => c.kind === k).length
                  if (k !== 'ALL' && count === 0) return null
                  const active = kindFilter === k
                  return (
                    <button
                      key={k}
                      type="button"
                      onClick={() => setKindFilter(k)}
                      className={`px-2 py-0.5 rounded text-[10px] font-bold border transition ${
                        active
                          ? 'border-indigo-500 bg-indigo-500 text-white'
                          : 'border-indigo-200 bg-white text-indigo-600 hover:border-indigo-400'
                      }`}
                    >
                      {k === 'ALL' ? 'Tất cả' : getKindLabel(k as CandidateKind)} ({count})
                    </button>
                  )
                })}
              </div>
            </div>
            <div className="flex items-center justify-between gap-2">
              <p className="text-[10px] text-indigo-600/80 italic">
                Click "Tự động lấy" sẽ tạo <b>{filteredCandidates.length}</b> cặp ghép
                từ <b>{kindFilter === 'ALL' ? 'tất cả' : getKindLabel(kindFilter).toLowerCase()}</b> trong lesson.
                Mỗi cặp: vế trái = từ/ký tự/mẫu câu, vế phải = nghĩa.
              </p>
              <Button
                size="small"
                type="primary"
                icon={<Sparkles className="w-3 h-3" />}
                onClick={handleAutoFill}
                className="!bg-indigo-600 hover:!bg-indigo-700 shrink-0"
              >
                Tự động lấy
              </Button>
            </div>
          </div>
        )}

        {allCandidates.length === 0 && lesson !== undefined && (
          <div className="rounded-md border border-dashed border-amber-200 bg-amber-50/40 p-2 text-[11px] text-amber-700">
            ⚠ Lesson này chưa có Hán tự / Từ vựng / Ngữ pháp nào. Bạn vẫn có thể
            nhập cặp ghép thủ công bên dưới.
          </div>
        )}

        <div className="flex items-center justify-between">
          <p className="text-xs font-bold text-indigo-700">
            Các cặp ghép ({rows.length})
          </p>
          <Button
            size="small"
            icon={<Plus className="w-3 h-3" />}
            onClick={addRow}
          >
            Thêm cặp
          </Button>
        </div>

        {rows.length === 0 && (
          <div className="py-3 text-center text-xs text-gray-400 border border-dashed border-gray-200 rounded-md">
            Chưa có cặp nào. Bấm "Thêm cặp" hoặc "Tự động lấy" để tạo.
          </div>
        )}

        <div className="space-y-1.5">
          {rows.map((row, idx) => (
            <PairRow
              key={idx}
              idx={idx}
              total={rows.length}
              left={row.left}
              right={row.right}
              candidates={filteredCandidates}
              candidateByKey={candidateByKey}
              onChange={(updated) => updateRow(idx, updated)}
              onRemove={() => removeRow(idx)}
            />
          ))}
        </div>
      </div>
    )
  }

  // ==========================================================================
  // VIEWER MODE — học viên chọn vế phải cho từng vế trái
  // ==========================================================================
  const rightOptions = shuffledRight.map((r) => ({
    value: r.key,
    label: r.label,
  }))

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-xs font-bold text-indigo-700">
          Ghép {left.length} cặp từ
        </p>
        <button
          type="button"
          onClick={() => setShuffledRight(shuffle(right))}
          className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
        >
          <Shuffle className="w-3 h-3" /> Xáo cột phải
        </button>
      </div>

      <div className="space-y-1.5">
        {left.map((l, idx) => {
          // Đáp án đúng: left[idx] ↔ right[idx] (cùng index)
          const correctRightKey = right[idx]?.key ?? ''
          const correctRightLabel = right[idx]?.label ?? ''
          const selectedRightKey = userAnswers[l.key] ?? ''
          const isCorrect = selectedRightKey && selectedRightKey === correctRightKey
          const isWrong = showCorrect && selectedRightKey && selectedRightKey !== correctRightKey

          return (
            <div
              key={l.key}
              className={`grid grid-cols-[1fr_auto_1fr] items-center gap-2 p-2 rounded border ${
                isCorrect
                  ? 'border-emerald-300 bg-emerald-50/50'
                  : isWrong
                    ? 'border-rose-300 bg-rose-50/50'
                    : 'border-gray-200 bg-white'
              }`}
            >
              {/* Vế trái */}
              <div className="flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-indigo-100 text-indigo-700 text-[10px] font-bold flex items-center justify-center shrink-0">
                  {idx + 1}
                </span>
                <span className="font-semibold text-sm text-gray-900 truncate">
                  {l.label || <em className="text-gray-400">(trống)</em>}
                </span>
              </div>

              {/* Dây nối */}
              <div className="flex flex-col items-center text-indigo-400 shrink-0">
                <div className="w-6 h-px bg-indigo-300" />
                <Link2 className="w-3 h-3 -mt-0.5" />
                <div className="w-6 h-px bg-indigo-300" />
              </div>

              {/* Vế phải — <Select> */}
              <div className="flex items-center gap-1.5">
                <Select
                  size="small"
                  className="!flex-1"
                  placeholder="Chọn nghĩa..."
                  value={selectedRightKey || undefined}
                  onChange={(v) =>
                    onUserAnswerChange?.({ ...userAnswers, [l.key]: v })
                  }
                  options={rightOptions}
                  status={isWrong ? 'error' : undefined}
                  disabled={showCorrect}
                />
                {isCorrect && (
                  <span className="text-[10px] font-bold text-emerald-600">✓</span>
                )}
                {isWrong && (
                  <span className="text-[10px] font-bold text-rose-600">✗</span>
                )}
                {showCorrect && selectedRightKey !== correctRightKey && (
                  <span className="text-[10px] text-gray-500 truncate max-w-[80px]">
                    → {correctRightLabel}
                  </span>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// ============================================================================
// Sub: 1 dòng cặp trong editor mode
// ============================================================================
interface PairRowProps {
  idx: number
  total: number
  left: MatchingSide
  right: MatchingSide
  candidates: MatchingCandidate[]
  candidateByKey: Map<string, MatchingCandidate>
  onChange: (updated: { left: MatchingSide; right: MatchingSide }) => void
  onRemove: () => void
}

/**
 * Sub-component input cho 1 cột (trái hoặc phải).
 * - Nếu key hiện tại trùng với 1 candidate trong lesson → dùng AutoComplete
 *   để teacher chọn lại từ danh sách (vẫn cho gõ tay nếu muốn).
 * - Ngược lại (key do teacher tự đặt vd: "L1", "L2") → dùng Input thường.
 */
const ColumnInput: React.FC<{
  value: MatchingSide
  candidates: MatchingCandidate[]
  candidateByKey: Map<string, MatchingCandidate>
  side: 'left' | 'right'
  placeholder: string
  onChange: (side: MatchingSide) => void
}> = ({ value, candidates, candidateByKey, side: _side, placeholder, onChange }) => {
  const linked = candidateByKey.get(value.key)

  // Có candidate liên kết → dùng AutoComplete (search + tag loại)
  if (linked) {
    return (
      <AutoComplete
        size="small"
        className="!flex-1"
        value={value.label}
        placeholder={placeholder}
        filterOption={(input, option) => {
          const c = (option as { candidate: MatchingCandidate }).candidate
          const q = input.toLowerCase()
          return (
            c.display.toLowerCase().includes(q) ||
            c.hint.toLowerCase().includes(q)
          )
        }}
        onChange={(v) => onChange({ ...value, label: v })}
        onSelect={(_v, option) => {
          const c = (option as { candidate: MatchingCandidate }).candidate
          onChange({ key: c.key, label: c.display })
        }}
        options={candidates.map((c) => ({
          value: c.display,
          label: (
            <div className="flex items-center gap-1.5">
              <Tag color={getKindTagColor(c.kind)} className="!m-0 !text-[9px] !leading-4 !px-1.5">
                {getKindLabel(c.kind)}
              </Tag>
              <span className="font-semibold text-gray-900">{c.display}</span>
              <span className="text-[11px] text-gray-500 truncate">— {c.hint}</span>
            </div>
          ),
          candidate: c,
        }))}
      >
        <Input
          size="small"
          value={value.label}
          onChange={(e) => onChange({ ...value, label: e.target.value })}
          placeholder={placeholder}
        />
      </AutoComplete>
    )
  }

  // Không có candidate liên kết → Input thường
  return (
    <Input
      size="small"
      value={value.label}
      onChange={(e) => onChange({ ...value, label: e.target.value })}
      placeholder={placeholder}
      className="!flex-1"
    />
  )
}

const PairRow: React.FC<PairRowProps> = ({
  idx,
  total,
  left,
  right,
  candidates,
  candidateByKey,
  onChange,
  onRemove,
}) => (
  <div className="grid grid-cols-[1fr_auto_1fr_auto] items-center gap-2 p-2 rounded border border-indigo-100 bg-indigo-50/30">
    {/* Vế trái */}
    <div className="flex items-center gap-1.5">
      <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] font-bold flex items-center justify-center shrink-0">
        {idx + 1}
      </span>
      <ColumnInput
        value={left}
        candidates={candidates}
        candidateByKey={candidateByKey}
        side="left"
        placeholder="Vế trái (VD: 勉強)"
        onChange={(v) => onChange({ left: v, right })}
      />
      {candidateByKey.get(left.key) && (
        <Tag
          color={getKindTagColor(candidateByKey.get(left.key)!.kind)}
          className="!m-0 !text-[9px] shrink-0"
        >
          {getKindLabel(candidateByKey.get(left.key)!.kind)}
        </Tag>
      )}
    </div>

    {/* Dây nối */}
    <div className="flex flex-col items-center text-indigo-500 shrink-0">
      <div className="w-6 h-px bg-indigo-300" />
      <Link2 className="w-3.5 h-3.5" />
      <div className="w-6 h-px bg-indigo-300" />
    </div>

    {/* Vế phải */}
    <div className="flex items-center gap-1.5">
      <span className="text-xs font-bold text-indigo-600 w-5">→</span>
      <ColumnInput
        value={right}
        candidates={candidates}
        candidateByKey={candidateByKey}
        side="right"
        placeholder="Vế phải (VD: Học tập)"
        onChange={(v) => onChange({ left, right: v })}
      />
      {candidateByKey.get(right.key) && (
        <Tag
          color={getKindTagColor(candidateByKey.get(right.key)!.kind)}
          className="!m-0 !text-[9px] shrink-0"
        >
          {getKindLabel(candidateByKey.get(right.key)!.kind)}
        </Tag>
      )}
    </div>

    {/* Nút xoá */}
    {total > 2 ? (
      <button
        type="button"
        onClick={onRemove}
        className="p-1 text-gray-400 hover:text-rose-600 rounded shrink-0"
        title="Xoá cặp này"
      >
        <XIcon className="w-3.5 h-3.5" />
      </button>
    ) : (
      <div className="w-5 shrink-0" />
    )}
  </div>
)
