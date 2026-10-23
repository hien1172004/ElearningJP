import React, { useEffect, useMemo, useRef, useState } from 'react'
import { CheckCircle2, XCircle, RotateCcw, ChevronLeft, ChevronRight } from 'lucide-react'
import { Button as AntdButton, Input } from 'antd'
import { useLessonQuiz, useSubmitQuiz } from '@/hooks'
import { LoadingState } from '@/components/common'
import type {
  QuizSubmitResponse,
  QuizDetailItem,
  LessonQuizDetail,
  QuizQuestionApi,
  QuizMatchingData,
} from '@/types'

interface QuizStudyViewProps {
  lessonId: number
  /** Gọi khi quiz đạt yêu cầu (passed) — dùng để đánh dấu lesson hoàn thành. */
  onComplete?: () => void
  /** Báo cho parent là section QUIZ đã "xong" (chỉ khi passed). */
  onSectionDone?: () => void
}

// ============================================================================
// Helpers
// ============================================================================

/** Lấy questionType an toàn (BE có thể trả về string lạ). */
function getQuestionType(q: QuizQuestionApi): string {
  return (q.questionType ?? 'MULTIPLE_CHOICE').toUpperCase()
}

/**
 * Lấy cấu trúc matchingData, fallback rỗng nếu BE không trả.
 *
 * <p>Mảng {@code right} đã được shuffle sẵn lúc teacher save (mỗi save là 1
 * thứ tự khác nhau) → viewer chỉ cần duyệt theo index tự nhiên.</p>
 */
function getMatchingData(q: QuizQuestionApi): QuizMatchingData | null {
  return q.matchingData ?? null
}

// ============================================================================
// Component
// ============================================================================

export const QuizStudyView: React.FC<QuizStudyViewProps> = ({ lessonId, onComplete, onSectionDone }) => {
  const { data: quiz, isLoading, isError } = useLessonQuiz(lessonId)
  const submitQuiz = useSubmitQuiz(lessonId)

  // Câu trả lời dạng MCQ / MATCHING: chọn optionId
  const [answers, setAnswers] = useState<Record<number, number[]>>({})
  // Câu trả lời dạng FILL_IN_THE_BLANK: text tự do
  const [textAnswers, setTextAnswers] = useState<Record<number, string>>({})
  // State riêng cho matching: {[questionId]: {[leftKey]: rightKey}}
  const [matchingState, setMatchingState] = useState<Record<number, Record<string, string>>>({})

  const [result, setResult] = useState<QuizSubmitResponse | null>(null)
  const [page, setPage] = useState(0)

  // ⚠️ Phải gọi useMemo TRƯỚC mọi conditional return
  // vì useMemo = hook. Di chuyển lên đây, trước các early return.
  const questions = useMemo(() => quiz?.questions ?? [], [quiz])
  const total = questions.length

  /** Tính số câu đã trả lời (áp dụng cho mọi dạng). */
  const answeredCount = useMemo(() => {
    return questions.reduce((acc, q) => {
      const t = getQuestionType(q)
      if (t === 'FILL_IN_THE_BLANK') {
        return acc + (textAnswers[q.id]?.trim() ? 1 : 0)
      }
      if (t === 'MATCHING') {
        const m = matchingState[q.id] ?? {}
        const total = getMatchingData(q)?.left.length ?? 0
        return acc + (Object.keys(m).length >= total && total > 0 ? 1 : 0)
      }
      return acc + ((answers[q.id] ?? []).length > 0 ? 1 : 0)
    }, 0)
  }, [questions, answers, textAnswers, matchingState])

  const allAnswered = answeredCount === total

  // Cần computed values ở đây để dùng trong JSX
  const safePage = Math.max(0, Math.min(page, total - 1))
  const currentQ = questions[safePage]

  if (isLoading) return <LoadingState message="Đang tải bài kiểm tra..." fullHeight />
  if (isError || !quiz) {
    return (
      <div className="text-center py-16 space-y-3">
        <XCircle className="w-12 h-12 text-gray-300 mx-auto" />
        <p className="text-base font-semibold text-gray-600">Bài học này chưa có bài kiểm tra.</p>
        <p className="text-sm text-gray-400">Bạn có thể đánh dấu hoàn thành bài học.</p>
      </div>
    )
  }

  if (total === 0) {
    return (
      <div className="text-center py-16 space-y-3">
        <p className="text-base font-semibold text-gray-600">Đề thi chưa có câu hỏi nào.</p>
      </div>
    )
  }

  /** MULTIPLE_CHOICE: chỉ chọn 1 đáp án. Click lại vào đáp án đã chọn → bỏ chọn. */
  const selectMcqOption = (questionId: number, optionId: number) => {
    if (result) return
    setAnswers((prev) => {
      const cur = prev[questionId] ?? []
      // Click vào đáp án đang chọn → bỏ chọn
      if (cur.includes(optionId)) {
        return { ...prev, [questionId]: [] }
      }
      // MCQ chỉ giữ 1 đáp án → thay thế hoàn toàn
      return { ...prev, [questionId]: [optionId] }
    })
  }

  /** MATCHING: chọn 1 rightKey cho 1 leftKey. Truyền null để xoá cặp. */
  const setMatchingAnswer = (questionId: number, leftKey: string, rightKey: string | null) => {
    if (result) return
    setMatchingState((prev) => {
      const cur = { ...(prev[questionId] ?? {}) }
      if (rightKey === null) {
        delete cur[leftKey]
      } else {
        cur[leftKey] = rightKey
      }
      return { ...prev, [questionId]: cur }
    })
  }

  const handleSubmit = async () => {
    // Build payload theo QuizAnswerItem (BE):
    //  - MULTIPLE_CHOICE: selectedOptionId
    //  - MATCHING:        matchingAnswers = { "L{i}=R{j}": "leftLabel = rightLabel", ... }
    //  - FILL_IN_THE_BLANK: textAnswer
    const payload = {
      answers: questions.map((q) => {
        const t = getQuestionType(q)
        if (t === 'FILL_IN_THE_BLANK') {
          return {
            questionId: q.id,
            textAnswer: textAnswers[q.id] ?? '',
          }
        }
        if (t === 'MATCHING') {
          const md = getMatchingData(q)
          const userMap = matchingState[q.id] ?? {}
          // Cấu trúc matchKey đồng bộ với QuizPanel payload khi save:
          //   matchKey   = "L{leftKey}=R{rightKey}"  (dùng KEY thuần, không theo index)
          //   optionText = "leftLabel = rightLabel"
          // BE so sánh userPairs[matchKey] === optionText (string match) để chấm.
          //
          // Lưu ý: KHÔNG dùng `L{leftIdx+1}=R{rightIdx+1}` vì `right` đã được
          // shuffle ở DB. Lúc shuffle, key "R2" có thể nằm ở index 4 → nếu
          // dùng index sẽ build matchKey sai thành "L2=R5". Dùng `leftKey` +
          // `rightKey` thuần (vd "L2=R2") là bất biến với shuffle.
          const matchingAnswers: Record<string, string> = {}
          if (md) {
            Object.entries(userMap).forEach(([leftKey, rightKey]) => {
              const left = md.left.find((l) => l.key === leftKey)
              const right = md.right.find((r) => r.key === rightKey)
              if (!left || !right) return
              // Tìm option BE theo optionText để chắc chắn khớp format.
              // Lưu ý: q.options có thể undefined (BE ẩn đáp án với student) —
              // nếu vậy, dùng fallback dựng từ label left/right.
              const opt = q.options?.find((o) => {
                const parts = o.optionText.split('=').map((s) => s.trim())
                return parts.length === 2 && parts[0] === left.label && parts[1] === right.label
              })
              // KEY: dùng leftKey/rightKey thuần (không theo index) để khớp
              // với matchKey mà BE đã build lúc save (cũng theo key).
              const matchKey = opt?.matchKey ?? `${leftKey}=${rightKey}`
              const optionText = opt?.optionText ?? `${left.label} = ${right.label}`
              matchingAnswers[matchKey] = optionText
            })
          }
          return {
            questionId: q.id,
            matchingAnswers,
          }
        }
        // MULTIPLE_CHOICE (mặc định) — gửi option đầu tiên nếu user chọn nhiều (BE chỉ nhận 1)
        const picked = answers[q.id] ?? []
        return {
          questionId: q.id,
          selectedOptionId: picked[0],
        }
      }),
    }
    const res = await submitQuiz.mutateAsync(payload)
    setResult(res)
  }

  const handleRetake = () => {
    setAnswers({})
    setTextAnswers({})
    setMatchingState({})
    setResult(null)
    setPage(0)
  }

  // Kết quả view
  if (result) {
    // "Tiếp tục học" = đóng kết quả nhưng KHÔNG complete lesson.
    // Nếu đã đạt, giữ nguyên kết quả để user có thể bấm "Hoàn thành bài học".
    const handleContinue = () => {
      if (result.passed) return // không cho đóng nếu đã đạt
      setResult(null)
    }
    return (
      <QuizResultView
        result={result}
        quiz={quiz}
        textAnswers={textAnswers}
        matchingState={matchingState}
        onRetake={handleRetake}
        onComplete={onComplete}
        onContinue={handleContinue}
        onSectionDone={onSectionDone}
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-br from-indigo-50 to-blue-50 rounded-md border-2 border-indigo-200/80 p-6 md:p-8 shadow-sm">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <h3 className="text-xl md:text-2xl font-extrabold text-indigo-800">
              {quiz.title || 'Bài kiểm tra'}
            </h3>
            {quiz.description && (
              <p className="text-sm text-gray-600 mt-1">{quiz.description}</p>
            )}
            {quiz.passingScorePercent != null && (
              <p className="text-xs text-gray-500 mt-2">
                Điểm đạt: {quiz.passingScorePercent}%
              </p>
            )}
          </div>
          <div className="text-right">
            <p className="text-xs text-gray-500">Đã trả lời</p>
            <p className="text-2xl font-extrabold text-indigo-700 tabular-nums">
              {answeredCount}/{total}
            </p>
          </div>
        </div>
      </div>

      {/* Câu hỏi hiện tại */}
      <QuestionCard
        question={currentQ}
        index={safePage}
        selectedOptionIds={answers[currentQ.id] ?? []}
        textValue={textAnswers[currentQ.id] ?? ''}
        matchingState={matchingState[currentQ.id] ?? {}}
        onToggleOption={(optId) => selectMcqOption(currentQ.id, optId)}
        onChangeText={(v) =>
          setTextAnswers((prev) => ({ ...prev, [currentQ.id]: v }))
        }
        onChangeMatching={(lKey, rKey) => setMatchingAnswer(currentQ.id, lKey, rKey)}
        disabled={!!result}
      />

      {/* Footer phân trang + nộp bài */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-2">
        <button
          type="button"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={safePage <= 0}
          className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-md border border-gray-200 bg-white text-sm font-semibold text-gray-700 hover:border-indigo-300 hover:text-indigo-600 transition disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <ChevronLeft className="w-4 h-4" />
          Câu trước
        </button>

        {safePage < total - 1 ? (
          <button
            type="button"
            onClick={() => setPage((p) => Math.min(total - 1, p + 1))}
            className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-md bg-indigo-600 text-white text-sm font-semibold hover:bg-indigo-700 transition"
          >
            Câu tiếp theo
            <ChevronRight className="w-4 h-4" />
          </button>
        ) : (
          <AntdButton
            type="primary"
            size="large"
            onClick={handleSubmit}
            disabled={!allAnswered}
            loading={submitQuiz.isPending}
            className="!rounded-md !font-bold"
          >
            {allAnswered ? 'Nộp bài' : `Trả lời ${total - answeredCount} câu nữa`}
          </AntdButton>
        )}
      </div>
    </div>
  )
}

// ============================================================================
// QuestionCard — render theo từng dạng câu hỏi
// ============================================================================

interface QuestionCardProps {
  question: QuizQuestionApi
  index: number
  selectedOptionIds: number[]
  textValue: string
  matchingState: Record<string, string>
  onToggleOption: (optionId: number) => void
  onChangeText: (value: string) => void
  onChangeMatching: (leftKey: string, rightKey: string | null) => void
  disabled?: boolean
}

const QuestionCard: React.FC<QuestionCardProps> = ({
  question,
  index,
  selectedOptionIds,
  textValue,
  matchingState,
  onToggleOption,
  onChangeText,
  onChangeMatching,
  disabled,
}) => {
  const t = getQuestionType(question)
  return (
    <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 md:p-6 space-y-4">
      <div className="flex items-start gap-3">
        <span className="w-8 h-8 rounded-md bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-sm shrink-0">
          {index + 1}
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-base md:text-lg font-bold text-gray-900">
            {question.questionText}
          </p>
          {t === 'FILL_IN_THE_BLANK' && (
            <p className="text-xs text-gray-500 mt-1 italic">
              (Điền đáp án vào ô bên dưới)
            </p>
          )}
          {t === 'MATCHING' && (
            <p className="text-xs text-gray-500 mt-1 italic">
              (Ghép từng mục bên trái với một mục tương ứng bên phải)
            </p>
          )}
        </div>
      </div>

      {/* FILL_IN_THE_BLANK */}
      {t === 'FILL_IN_THE_BLANK' && (
        <div className="pl-0 sm:pl-11">
          <Input.TextArea
            value={textValue}
            onChange={(e) => onChangeText(e.target.value)}
            placeholder="Nhập câu trả lời của bạn..."
            autoSize={{ minRows: 2, maxRows: 6 }}
            disabled={disabled}
            className="!text-base !rounded-md"
          />
        </div>
      )}

      {/* MATCHING */}
      {t === 'MATCHING' && (
        <MatchingView
          question={question}
          matchingState={matchingState}
          onChange={onChangeMatching}
          disabled={disabled}
        />
      )}

      {/* MULTIPLE_CHOICE (mặc định) */}
      {t !== 'FILL_IN_THE_BLANK' && t !== 'MATCHING' && (
        <div className="space-y-2 pl-0 sm:pl-11">
          {(question.options ?? []).map((opt) => {
            const selected = selectedOptionIds.includes(opt.id)
            return (
              <button
                key={opt.id}
                type="button"
                onClick={() => onToggleOption(opt.id)}
                className={`w-full text-left p-4 rounded-md border-2 transition flex items-center gap-3 ${selected
                    ? 'border-indigo-500 bg-indigo-50'
                    : 'border-gray-200 hover:border-indigo-300 hover:bg-gray-50'
                  }`}
              >
                <span
                  className={`w-7 h-7 rounded-full border-2 flex items-center justify-center text-sm font-bold shrink-0 ${selected
                      ? 'border-indigo-500 bg-indigo-500 text-white'
                      : 'border-gray-300 text-gray-500'
                    }`}
                >
                  {opt.optionKey}
                </span>
                <span className="flex-1 text-sm text-gray-800 font-medium">
                  {opt.optionText}
                </span>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ============================================================================
// MatchingView — giao diện ghép cặp với đường nối SVG
// ============================================================================
//
// Cơ chế chọn (cả desktop & mobile):
// 1. User bấm vào 1 item ở Cột A (activeLeft = leftKey).
// 2. User bấm vào 1 item ở Cột B → ghép cặp (leftKey, rightKey).
// 3. Bấm lại vào Cột A đã chọn hoặc bấm "Xoá" để huỷ.
// 4. Bấm vào 1 cặp đã ghép trên đường nối → huỷ cặp đó.
//
// Hiển thị:
// - Hai cột dọc song song (Cột A | Cột B).
// - SVG overlay vẽ đường cong Bezier nối từ giữa cạnh phải của Cột A
//   đến giữa cạnh trái của Cột B cho mỗi cặp đã ghép.

interface MatchingViewProps {
  question: QuizQuestionApi
  matchingState: Record<string, string> // {[leftKey]: rightKey}
  onChange: (leftKey: string, rightKey: string | null) => void
  disabled?: boolean
}

interface AnchorRect {
  leftKey: string
  rightKey: string
  x1: number // tâm cạnh phải của left
  y1: number
  x2: number // tâm cạnh trái của right
  y2: number
}

const ANCHOR_OFFSET = 0 // anchor nằm ngay giữa chiều cao item

const MatchingView: React.FC<MatchingViewProps> = ({
  question,
  matchingState,
  onChange,
  disabled,
}) => {
  const md = getMatchingData(question)

  // Refs cho từng item để tính toạ độ vẽ đường nối
  const leftRefs = useRef<Record<string, HTMLDivElement | null>>({})
  const rightRefs = useRef<Record<string, HTMLDivElement | null>>({})
  const containerRef = useRef<HTMLDivElement | null>(null)

  // Item đang được chọn ở Cột A (chờ bấm Cột B để ghép)
  const [activeLeft, setActiveLeft] = useState<string | null>(null)

  // Anchor list (cặp ghép → toạ độ tuyệt đối trong container)
  const [anchors, setAnchors] = useState<AnchorRect[]>([])

  // Tính lại toạ độ sau khi render / khi state đổi / khi resize
  const recomputeAnchors = () => {
    if (!md) return
    const container = containerRef.current
    if (!container) return
    const containerRect = container.getBoundingClientRect()

    const next: AnchorRect[] = []
    for (const [leftKey, rightKey] of Object.entries(matchingState)) {
      if (!rightKey) continue
      const lEl = leftRefs.current[leftKey]
      const rEl = rightRefs.current[rightKey]
      if (!lEl || !rEl) continue
      const lRect = lEl.getBoundingClientRect()
      const rRect = rEl.getBoundingClientRect()
      next.push({
        leftKey,
        rightKey,
        x1: lRect.right - containerRect.left,
        y1: lRect.top - containerRect.top + ANCHOR_OFFSET + lRect.height / 2,
        x2: rRect.left - containerRect.left,
        y2: rRect.top - containerRect.top + ANCHOR_OFFSET + rRect.height / 2,
      })
    }
    setAnchors(next)
  }

  // Recompute khi matchingState thay đổi
  useEffect(() => {
    // Đợi DOM commit xong
    const id = requestAnimationFrame(() => recomputeAnchors())
    return () => cancelAnimationFrame(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [matchingState, md?.left, md?.right])

  // Recompute khi resize
  useEffect(() => {
    const onResize = () => recomputeAnchors()
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [md])

  if (!md || md.left.length === 0 || md.right.length === 0) {
    return (
      <p className="text-sm text-rose-500 italic">
        Câu hỏi này chưa có dữ liệu ghép cặp.
      </p>
    )
  }

  // Mảng md.right đã được shuffle sẵn lúc teacher save (mỗi save là 1 thứ tự
  // khác nhau) → chỉ cần duyệt theo index tự nhiên. Không cần displayOrder.

  const isLeftMatched = (leftKey: string) => !!matchingState[leftKey]
  const isRightMatched = (rightKey: string) =>
    Object.values(matchingState).includes(rightKey)
  const getMatchedLeftOf = (rightKey: string) =>
    Object.entries(matchingState).find(([, rk]) => rk === rightKey)?.[0]

  // Tap vào Cột A
  const handleLeftClick = (leftKey: string) => {
    if (disabled) return
    // Nếu đã ghép → tap để xoá
    if (isLeftMatched(leftKey)) {
      onChange(leftKey, null)
      return
    }
    // Nếu đang có activeLeft khác → chuyển sang cái mới
    setActiveLeft((prev) => (prev === leftKey ? null : leftKey))
  }

  // Tap vào Cột B
  const handleRightClick = (rightKey: string) => {
    if (disabled) return
    // Nếu rightKey đã được ghép → tap để xoá cặp đó
    if (isRightMatched(rightKey)) {
      const matchedLeft = getMatchedLeftOf(rightKey)
      if (matchedLeft) onChange(matchedLeft, null)
      return
    }
    // Nếu đang có activeLeft → ghép cặp
    if (activeLeft && !isLeftMatched(activeLeft)) {
      onChange(activeLeft, rightKey)
      setActiveLeft(null)
    }
  }

  // Tap vào đường nối → xoá cặp
  const handleLineClick = (leftKey: string) => {
    if (disabled) return
    onChange(leftKey, null)
  }

  const clearAll = () => {
    if (disabled) return
    for (const lk of Object.keys(matchingState)) onChange(lk, null)
    setActiveLeft(null)
  }

  const matchedCount = Object.values(matchingState).filter(Boolean).length
  const totalPairs = md.left.length

  return (
    <div className="pl-0 sm:pl-11 space-y-3">
      {/* Thanh trạng thái */}
      <div className="flex items-center justify-between text-xs">
        <p className="text-gray-500">
          Đã ghép: <strong className="text-indigo-600">{matchedCount}</strong> / {totalPairs}
          {activeLeft && (
            <span className="ml-2 text-amber-600 font-semibold">
              · Đang chọn "{md.left.find((l) => l.key === activeLeft)?.label}" — bấm vào Cột B để nối
            </span>
          )}
        </p>
        {matchedCount > 0 && (
          <button
            type="button"
            onClick={clearAll}
            disabled={disabled}
            className="text-xs font-semibold text-rose-600 hover:text-rose-700 disabled:opacity-50"
          >
            Xoá tất cả
          </button>
        )}
      </div>

      {/* Khu vực 2 cột + SVG đường nối */}
      <div
        ref={containerRef}
        className="relative grid grid-cols-2 gap-8 sm:gap-16"
      >
        {/* SVG overlay chứa các đường nối */}
        <svg
          className="absolute inset-0 pointer-events-none"
          style={{ overflow: 'visible' }}
        >
          <defs>
            <marker
              id={`arrow-${question.id}`}
              viewBox="0 0 10 10"
              refX="8"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#6366f1" />
            </marker>
          </defs>
          {anchors.map((a) => {
            const midX = (a.x1 + a.x2) / 2
            const path = `M ${a.x1} ${a.y1} C ${midX} ${a.y1}, ${midX} ${a.y2}, ${a.x2} ${a.y2}`
            return (
              <g key={`${a.leftKey}-${a.rightKey}`} className="pointer-events-auto">
                {/* Đường nối trong suốt, rộng hơn để dễ bấm */}
                <path
                  d={path}
                  stroke="transparent"
                  strokeWidth={16}
                  fill="none"
                  style={{ cursor: disabled ? 'not-allowed' : 'pointer' }}
                  onClick={() => handleLineClick(a.leftKey)}
                />
                {/* Đường nối hiển thị */}
                <path
                  d={path}
                  stroke="#6366f1"
                  strokeWidth={2.5}
                  strokeLinecap="round"
                  fill="none"
                  markerEnd={`url(#arrow-${question.id})`}
                  className="pointer-events-none"
                />
              </g>
            )
          })}
        </svg>

        {/* Cột A */}
        <div className="space-y-2">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
            Cột A
          </p>
          {md.left.map((l) => {
            const matched = isLeftMatched(l.key)
            const active = activeLeft === l.key
            const rightLabel = matched
              ? md.right.find((r) => r.key === matchingState[l.key])?.label
              : null
            return (
              <div
                key={l.key}
                ref={(el) => {
                  leftRefs.current[l.key] = el
                }}
                onClick={() => handleLeftClick(l.key)}
                className={`relative h-[56px] flex items-center justify-center px-3 rounded-md border-2 font-bold text-base transition select-none ${matched
                    ? 'border-emerald-400 bg-emerald-50 text-emerald-800'
                    : active
                      ? 'border-indigo-500 bg-indigo-50 text-indigo-800 ring-2 ring-indigo-200'
                      : 'border-gray-200 bg-white text-gray-900 hover:border-indigo-300'
                  } ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
              >
                <span>{l.label}</span>
                {matched && rightLabel && (
                  <span className="absolute -bottom-2 right-2 text-[10px] font-semibold text-emerald-700 bg-white px-1.5 rounded">
                    = {rightLabel}
                  </span>
                )}
                {/* Anchor point cho đường nối (vô hình) */}
                <span className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-2 h-2 rounded-full bg-indigo-500 opacity-0" />
              </div>
            )
          })}
        </div>

        {/* Cột B — md.right đã được shuffle sẵn ở DB (mỗi save là 1 thứ tự khác) */}
        <div className="space-y-2">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
            Cột B
          </p>
          {md.right.map((r) => {
            const matched = isRightMatched(r.key)
            const leftLabel = matched
              ? md.left.find((l) => l.key === getMatchedLeftOf(r.key))?.label
              : null
            return (
              <div
                key={r.key}
                ref={(el) => {
                  rightRefs.current[r.key] = el
                }}
                onClick={() => handleRightClick(r.key)}
                className={`relative h-[56px] flex items-center justify-center px-3 rounded-md border-2 text-sm font-medium transition select-none ${matched
                    ? 'border-emerald-400 bg-emerald-50 text-emerald-800'
                    : 'border-gray-200 bg-white text-gray-700 hover:border-indigo-300'
                  } ${disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'}`}
              >
                <span>{r.label}</span>
                {matched && leftLabel && (
                  <span className="absolute -bottom-2 left-2 text-[10px] font-semibold text-emerald-700 bg-white px-1.5 rounded">
                    {leftLabel} =
                  </span>
                )}
                <span className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-1/2 w-2 h-2 rounded-full bg-indigo-500 opacity-0" />
              </div>
            )
          })}
        </div>
      </div>

      {/* Hướng dẫn nhỏ */}
      <p className="text-xs text-gray-400 italic">
        Mẹo: bấm vào mục Cột A, sau đó bấm vào mục Cột B tương ứng để vẽ đường nối. Bấm vào đường nối hoặc bấm lại vào mục đã ghép để xoá.
      </p>
    </div>
  )
}

// ============================================================================
// QuizResultView
// ============================================================================

interface QuizResultViewProps {
  result: QuizSubmitResponse
  quiz: LessonQuizDetail
  textAnswers: Record<number, string>
  matchingState: Record<number, Record<string, string>>
  onRetake: () => void
  onComplete?: () => void
  /** Nút "Tiếp tục học" khi chưa đạt (chỉ đóng kết quả, không complete). */
  onContinue?: () => void
  onSectionDone?: () => void
}

// Hiển thị danh sách ghép cặp của học viên.
// BE trả Map với key dạng "L_key=R_key" (format mới ưu tiên) hoặc
// "L_key→R_key" / "L_key+R_key" (cũ) để tương thích ngược.
// Value dạng "leftLabel = rightLabel" → tách lấy phần sau "=".
// → Chuẩn hóa về map{leftKey: rightLabel}.
function parseMatchingMap(
  raw: Record<string, string> | undefined,
): Record<string, string> {
  const out: Record<string, string> = {}
  if (!raw) return out
  Object.entries(raw).forEach(([k, v]) => {
    // Tách key theo "=" (format mới), "→" (arrow cũ), hoặc "+" (plus cũ).
    // Ví dụ "L1=R2" → ["L1", "R2"], "L1→R2" → ["L1", "R2"], "L1+R2" → ["L1", "R2"]
    const [leftKey] = k.split(/=|→|\+/).map((s) => s.trim())
    if (!leftKey) return
    // value có dạng "leftLabel = rightLabel" → lấy phần sau dấu "="
    const parts = String(v).split('=').map((s) => s.trim())
    const rightLabel = parts.length === 2 ? parts[1] : String(v)
    out[leftKey] = rightLabel
  })
  return out
}

const MatchingAnswerList: React.FC<{
  quiz: LessonQuizDetail
  questionId: number
  /** Map từ BE (chưa parse) hoặc map đã chuẩn hóa leftKey -> rightLabel */
  answers: Record<string, string>
  isCorrect: boolean
  /** Đáp án đúng từ BE (sau khi parse): leftKey -> rightLabel chuẩn hoá */
  correctByLeftKey?: Record<string, string>
}> = ({ quiz, questionId, answers, isCorrect, correctByLeftKey }) => {
  const q = quiz.questions.find((x) => x.id === questionId)
  if (!q) return null
  const md = getMatchingData(q)
  if (!md) return null

  // Nếu value chứa dấu "=" → dùng parser; nếu không thì coi như đã chuẩn hóa
  const looksLikeRaw = Object.values(answers).some(
    (v) => typeof v === 'string' && v.includes('='),
  )
  const normalized = looksLikeRaw ? parseMatchingMap(answers) : { ...answers }

  // Convert từ matchingState (key form) sang label form
  const finalMap: Record<string, string> = {}
  Object.entries(normalized).forEach(([leftKey, val]) => {
    if (md.left.some((l) => l.key === leftKey) && md.right.some((r) => r.label === val)) {
      // val là rightKey → convert sang label
      const rightItem = md.right.find((r) => r.key === val || r.label === val)
      finalMap[leftKey] = rightItem?.label ?? val
    } else {
      finalMap[leftKey] = val
    }
  })

  // Helper so sánh: cặp (leftKey, userRightLabel) có khớp với đáp án đúng không?
  // Lấy đáp án đúng từ `correctByLeftKey` (đã parse) — nếu không có, fallback coi
  // toàn bộ là sai (vì thiếu dữ liệu để so sánh).
  const isPairCorrect = (leftKey: string, userRightLabel: string): boolean => {
    if (!correctByLeftKey) return false
    const correctLabel = correctByLeftKey[leftKey]
    if (!correctLabel) return false
    return correctLabel.trim() === (userRightLabel ?? '').trim()
  }

  return (
    <ul className="space-y-0.5 text-sm">
      {md.left.map((l) => {
        const txt = finalMap[l.key]
        // Câu đúng → tất cả xanh. Câu sai → so sánh từng cặp với đáp án đúng.
        const pairOk = isCorrect || (txt ? isPairCorrect(l.key, txt) : false)
        return (
          <li key={l.key} className="flex items-center gap-2">
            <span className="font-semibold text-gray-700">{l.label}</span>
            <span className="text-gray-400">→</span>
            {txt ? (
              <span
                className={
                  pairOk
                    ? 'text-emerald-700 font-semibold'
                    : 'text-rose-700 line-through'
                }
              >
                {txt}
              </span>
            ) : (
              <em className="text-gray-400">—</em>
            )}
          </li>
        )
      })}
    </ul>
  )
}

// Hiển thị đáp án đúng cho câu MATCHING.
// BE trả về Map với key = "L_key+R_key" (vd "L1+R3") và value = "leftLabel = rightLabel" (vd "学 = học")
// → Parse để dựng map{leftKey: rightLabel} cho UI.
const CorrectMatchingList: React.FC<{
  detail: QuizDetailItem
  quiz: LessonQuizDetail
  questionId: number
  q?: QuizQuestionApi
}> = ({ detail, quiz, questionId, q }) => {
  const question = q ?? quiz.questions.find((x) => x.id === questionId)
  if (!question) return null
  const md = getMatchingData(question)
  if (!md) return null

  // 1) Ưu tiên map từ BE
  let parsed = parseMatchingMap(detail.correctMatchingAnswers)

  // 2) Fallback: parse từ quiz.questions.options (optionText dạng "学 = học")
  if (Object.keys(parsed).length === 0) {
    question.options?.forEach((opt) => {
      const parts = opt.optionText.split('=').map((s) => s.trim())
      if (parts.length === 2) {
        const [leftLabel, rightLabel] = parts
        const left = md.left.find((l) => l.label === leftLabel)
        if (left) parsed[left.key] = rightLabel
      }
    })
  }

  return (
    <ul className="space-y-0.5 text-sm">
      {md.left.map((l) => (
        <li key={l.key} className="flex items-center gap-2">
          <span className="font-semibold text-gray-700">{l.label}</span>
          <span className="text-gray-400">→</span>
          <span className="text-emerald-700 font-semibold">{parsed[l.key] ?? '—'}</span>
        </li>
      ))}
    </ul>
  )
}

const QuizResultView: React.FC<QuizResultViewProps> = ({
  result,
  quiz,
  textAnswers,
  matchingState,
  onRetake,
  onComplete,
  onContinue,
  onSectionDone,
}) => {
  // BE đang trả `details` (QuizDetailItemResponse) — chấp nhận cả 2 để tương thích
  const rawResults = (result as any).results ?? (result as any).details ?? []
  const normalizedResults: QuizDetailItem[] = rawResults.map((r: any) => ({
    questionId: r.questionId,
    questionText: r.questionText,
    questionType: r.questionType,
    correct: r.isCorrect ?? r.correct ?? false,
    isCorrect: r.isCorrect,
    selectedOptionId: r.selectedOptionId,
    selectedOptionText: r.selectedOptionText,
    userTextAnswer: r.userTextAnswer,
    userMatchingAnswers: r.userMatchingAnswers,
    correctOptionId: r.correctOptionId,
    correctOptionText: r.correctOptionText,
    correctTextAnswer: r.correctTextAnswer,
    correctMatchingAnswers: r.correctMatchingAnswers,
    explanation: r.explanation,
  }))
  const passed = result.passed
  // Báo "section QUIZ đã xong" khi đạt yêu cầu (chỉ phát 1 lần)
  const reportedQuizDoneRef = React.useRef(false)
  React.useEffect(() => {
    if (passed && !reportedQuizDoneRef.current) {
      reportedQuizDoneRef.current = true
      onSectionDone?.()
    }
  }, [passed, onSectionDone])
  return (
    <div className="space-y-6">
      <div
        className={`rounded-md border-2 p-8 md:p-10 shadow-sm text-center space-y-3 ${passed
            ? 'bg-gradient-to-br from-emerald-50 to-teal-50 border-emerald-200'
            : 'bg-gradient-to-br from-rose-50 to-pink-50 border-rose-200'
          }`}
      >
        {passed ? (
          <CheckCircle2 className="w-16 h-16 text-emerald-500 mx-auto" />
        ) : (
          <XCircle className="w-16 h-16 text-rose-500 mx-auto" />
        )}
        <h3 className={`text-2xl md:text-3xl font-extrabold ${passed ? 'text-emerald-800' : 'text-rose-800'}`}>
          {passed ? 'Chúc mừng! Bạn đã đạt' : 'Chưa đạt, hãy thử lại'}
        </h3>
        <p className="text-base text-gray-700">
          Đúng <strong className="tabular-nums">{result.correctCount}</strong> / {result.totalQuestions} câu
          {' · '}
          <strong className="tabular-nums">{result.scorePercent}%</strong>
        </p>
      </div>

      <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 md:p-6 space-y-3">
        <h4 className="font-bold text-base text-gray-900">Chi tiết từng câu</h4>
        <div className="space-y-2">
          {normalizedResults.map((r) => {
            const detail = r
            const q = quiz.questions.find((x) => x.id === r.questionId)
            const qType = q ? getQuestionType(q) : 'MULTIPLE_CHOICE'
            const isCorrect = (r as any).isCorrect ?? r.correct

            // Lấy text đáp án đúng + đáp án học viên từ 3 nguồn (BE detail > local state > q.options)
            const correctOptionText =
              detail.correctOptionText ??
              q?.options?.find((o) => o.id === detail.correctOptionId)?.optionText
            const selectedOptionText =
              detail.selectedOptionText ??
              q?.options?.find((o) => o.id === detail.selectedOptionId)?.optionText
            const userText = detail.userTextAnswer ?? textAnswers[r.questionId] ?? ''
            const correctText = detail.correctTextAnswer ?? ''

            return (
              <div
                key={r.questionId}
                className={`p-4 rounded-md border space-y-2 ${isCorrect
                    ? 'border-emerald-200 bg-emerald-50/40'
                    : 'border-rose-200 bg-rose-50/40'
                  }`}
              >
                {/* Header: icon + câu hỏi */}
                <div className="flex items-start gap-3">
                  {isCorrect ? (
                    <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
                  ) : (
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                  )}
                  <p className="text-sm font-semibold text-gray-900 flex-1 min-w-0">
                    {detail.questionText ?? q?.questionText ?? `Câu #${r.questionId}`}
                  </p>
                </div>

                {/* MCQ */}
                {qType === 'MULTIPLE_CHOICE' && (
                  <div className="ml-8 space-y-1.5 text-sm">
                    <p className="flex items-center gap-2">
                      <span className="text-gray-500">Bạn chọn:</span>
                      {selectedOptionText ? (
                        <span
                          className={`font-semibold ${isCorrect
                              ? 'text-emerald-700'
                              : 'text-rose-700 line-through'
                            }`}
                        >
                          {selectedOptionText}
                        </span>
                      ) : (
                        <em className="text-gray-400">(chưa chọn)</em>
                      )}
                    </p>
                    {!isCorrect && correctOptionText && (
                      <p className="flex items-center gap-2">
                        <span className="text-gray-500">Đáp án đúng:</span>
                        <span className="font-semibold text-emerald-700">
                          {correctOptionText}
                        </span>
                      </p>
                    )}
                  </div>
                )}

                {/* FILL_IN_THE_BLANK */}
                {qType === 'FILL_IN_THE_BLANK' && (
                  <div className="ml-8 space-y-1.5 text-sm">
                    <p className="flex items-center gap-2">
                      <span className="text-gray-500">Bạn trả lời:</span>
                      {userText ? (
                        <span
                          className={`font-semibold ${isCorrect
                              ? 'text-emerald-700'
                              : 'text-rose-700 line-through'
                            }`}
                        >
                          {userText}
                        </span>
                      ) : (
                        <em className="text-gray-400">(trống)</em>
                      )}
                    </p>
                    {!isCorrect && correctText && (
                      <p className="flex items-center gap-2">
                        <span className="text-gray-500">Đáp án đúng:</span>
                        <span className="font-semibold text-emerald-700">
                          {correctText}
                        </span>
                      </p>
                    )}
                  </div>
                )}

                {/* MATCHING */}
                {qType === 'MATCHING' && (
                  <div className="ml-8 space-y-2 text-sm">
                    <p className="text-gray-500 text-xs uppercase tracking-wide font-semibold">
                      Đáp án của bạn
                    </p>
                    <MatchingAnswerList
                      quiz={quiz}
                      questionId={r.questionId}
                      answers={
                        detail.userMatchingAnswers ??
                        Object.fromEntries(
                          Object.entries(matchingState[r.questionId] ?? {}).map(
                            ([lk, rk]) => {
                              const md = q ? getMatchingData(q) : null
                              const rItem = md?.right.find((x) => x.key === rk)
                              return [lk, rItem?.label ?? rk]
                            },
                          ),
                        )
                      }
                      isCorrect={isCorrect}
                      // Truyền đáp án đúng (đã parse về leftKey -> rightLabel)
                      // để MatchingAnswerList so sánh từng cặp: đúng → xanh,
                      // sai → đỏ gạch ngang. Trước đây cả cụm bị gạch khi
                      // câu sai dù có cặp đúng.
                      correctByLeftKey={
                        detail.correctMatchingAnswers
                          ? parseMatchingMap(detail.correctMatchingAnswers)
                          : undefined
                      }
                    />
                    {!isCorrect && (
                      <>
                        <p className="text-gray-500 text-xs uppercase tracking-wide font-semibold pt-1">
                          Đáp án đúng
                        </p>
                        <CorrectMatchingList
                          detail={detail}
                          quiz={quiz}
                          questionId={r.questionId}
                          q={q}
                        />
                      </>
                    )}
                  </div>
                )}

                {/* Explanation / giải thích */}
                {detail.explanation && (
                  <div className="ml-8 mt-1 text-xs text-gray-600 italic bg-amber-50 border border-amber-200 rounded px-3 py-2">
                    <span className="font-semibold not-italic text-amber-800">Gợi ý:</span>{' '}
                    {detail.explanation}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>

      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-end gap-3 pt-2">
        {!passed && (
          <>
            {/* {onContinue && (
              <AntdButton
                onClick={onContinue}
                className="!rounded-md !font-semibold"
              >
                Tiếp tục học
              </AntdButton>
            )} */}
            <AntdButton
              icon={<RotateCcw className="w-4 h-4" />}
              onClick={onRetake}
              className="!rounded-md !font-semibold"
            >
              Làm lại
            </AntdButton>
          </>
        )}
        {passed && onComplete && (
          <AntdButton
            type="primary"
            size="large"
            onClick={onComplete}
            className="!rounded-md !font-bold"
          >
            Hoàn thành bài học
          </AntdButton>
        )}
      </div>
    </div>
  )
}
