import React, { useEffect, useState } from 'react'
import { Pencil, Trash2, Plus, BookOpen, X as XIcon, Check, Eye, ChevronUp, ChevronDown } from 'lucide-react'
import { Modal, App, Button, Input, InputNumber } from 'antd'
import { toast } from 'sonner'
import { useLessonDetail, useLessonQuizManage, useQuizItemMutations, useReorderQuizQuestions } from '@/hooks'
import { QUIZ_TYPE_LABEL } from './constants'
import type { LessonQuizItem } from '@/hooks/useQuizItemMutations'
import type { QuizQuestionWithAnswer } from '@/types'
import {
  MatchingPairsEditor,
  type MatchingData,
} from './MatchingPairsEditor'
import { shuffleMatchingRight } from './matchingFromLesson'

interface QuizPanelProps {
  lessonId: number
  /** Tên lesson để hiển thị trong title các modal. */
  lessonTitle?: string
}

export const QuizPanel: React.FC<QuizPanelProps> = ({ lessonId, lessonTitle: _lessonTitle }) => {
  const { message } = App.useApp()
  // Trang này dành cho Admin/Teacher → dùng endpoint /quiz/manage
  // (trả kèm đáp án đúng, explanation). Học viên dùng /quiz (ẩn đáp án).
  const { data: quiz, isLoading, error } = useLessonQuizManage(lessonId)
  // createQuizItem/updateQuizItem được dùng trong QuestionFormModal (qua cùng hook)
  const { deleteQuizItem } = useQuizItemMutations(lessonId)
  // Hook reorder (gọi POST /lessons/{id}/quiz/questions/reorder)
  const reorderQuiz = useReorderQuizQuestions(lessonId)

  const [isEditorOpen, setIsEditorOpen] = useState(false)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isRenameOpen, setIsRenameOpen] = useState(false)
  const [renameValue, setRenameValue] = useState('')
  const [editingQuestion, setEditingQuestion] = useState<LessonQuizItem | null>(null)
  const [questionFormOpen, setQuestionFormOpen] = useState(false)
  const [viewingQuestion, setViewingQuestion] = useState<QuizQuestionWithAnswer | null>(null)

  // Cast sang shape có đáp án đúng (QuizQuestionWithAnswer) để hiển thị review.
  // Khi truyền sang QuestionFormModal, cast lại về LessonQuizItem (chỉ cần field cơ bản).
  const questionsFull = (quiz?.questions ?? []) as QuizQuestionWithAnswer[]
  const questions = questionsFull as unknown as LessonQuizItem[]
  const total = questions.length
  const quizTitle = quiz?.title ?? ''
  const isNotFound = (error as any)?.response?.status === 404

  // ==========================================================================
  // HANDLERS
  // ==========================================================================
  const openEditor = () => setIsEditorOpen(true)
  const closeEditor = () => {
    setIsEditorOpen(false)
    setEditingQuestion(null)
  }

  const openCreate = () => setIsCreateOpen(true)
  const closeCreate = () => setIsCreateOpen(false)

  const openRename = () => {
    setRenameValue(quizTitle)
    setIsRenameOpen(true)
  }
  const closeRename = () => setIsRenameOpen(false)
  const handleRename = () => {
    // BE chưa có PUT /lessons/{id}/quiz
    message.warning('BE chưa hỗ trợ đổi tiêu đề quiz.')
    closeRename()
  }

  const handleDeleteAll = () => {
    if (total === 0) {
      message.info('Quiz chưa có câu hỏi nào.')
      return
    }
    Modal.confirm({
      title: 'Xoá toàn bộ câu hỏi?',
      content: `Sắp xoá ${total} câu hỏi. Thao tác này không thể hoàn tác.`,
      okText: 'Xoá tất cả',
      cancelText: 'Huỷ',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          for (const q of questions) {
            await deleteQuizItem.mutateAsync(q.id)
          }
          toast.success('Đã xoá toàn bộ câu hỏi')
        } catch {
          /* lỗi đã toast trong hook */
        }
      },
    })
  }

  /**
   * Reorder 1 câu hỏi lên/xuống 1 vị trí bằng nút ↑/↓.
   * Dùng cùng pattern với SectionList.handleReorder:
   *   - Tính danh sách active sort theo orderIndex
   *   - Swap 2 phần tử liền kề
   *   - Gọi BE POST /lessons/{id}/quiz/questions/reorder với itemIds theo thứ tự mới
   *
   * <p>BE dùng thuật toán adjacent swap chain (xem QuizService.swapQuestionToTarget)
   * nên không bị UNIQUE conflict kể cả khi swap nhiều bước.</p>
   */
  const handleReorderQuestion = async (direction: 'up' | 'down', qId: number) => {
    if (!quiz) return
    const arr = [...questionsFull]
      .sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0))
    const idx = arr.findIndex((q) => q.id === qId)
    if (idx < 0) return
    const targetIndex = direction === 'up' ? idx - 1 : idx + 1
    if (targetIndex < 0 || targetIndex >= arr.length) return
    const newOrder = [...arr]
    ;[newOrder[idx], newOrder[targetIndex]] = [newOrder[targetIndex], newOrder[idx]]
    try {
      await reorderQuiz.mutateAsync(newOrder.map((q) => q.id))
    } catch {
      /* lỗi đã toast trong hook */
    }
  }

  return (
    <div className="flex items-center gap-3 p-3 rounded-md border border-cyan-200/80 bg-cyan-50/40 hover:border-cyan-300 transition">
      {/* Icon ? */}
      <div className="shrink-0 w-9 h-9 rounded-sm bg-cyan-600 text-white font-bold text-base flex items-center justify-center">
        <BookOpen className="w-5 h-5" />
      </div>

      {/* Title */}
      <div className="flex-1 min-w-0">
        {isLoading ? (
          <p className="text-xs text-gray-400">Đang tải quiz...</p>
        ) : isNotFound ? (
          <p className="text-sm font-semibold text-gray-500 italic">(Chưa có quiz)</p>
        ) : (
          <div className="flex items-center gap-1.5">
            <p className="font-semibold text-sm text-gray-900 truncate">
              {quizTitle || 'Bài kiểm tra'}
            </p>
            {quiz && (
              <button
                type="button"
                onClick={openRename}
                className="p-0.5 text-gray-400 hover:text-cyan-600 rounded shrink-0"
                title="Đổi tiêu đề"
              >
                <Pencil className="w-3 h-3" />
              </button>
            )}
            {total > 0 && (
              <span className="text-[10px] font-bold text-cyan-700 bg-cyan-100 px-1.5 py-0.5 rounded">
                {total} câu
              </span>
            )}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center gap-1.5 shrink-0">
        {isNotFound ? (
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-cyan-600 text-white text-xs font-bold hover:bg-cyan-700 transition"
          >
            <Plus className="w-3.5 h-3.5" /> Tạo câu hỏi đầu tiên
          </button>
        ) : (
          <>
            <button
              type="button"
              onClick={openEditor}
              disabled={isLoading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-cyan-600 text-white text-xs font-bold hover:bg-cyan-700 transition disabled:opacity-50"
            >
              <Pencil className="w-3.5 h-3.5" /> Quản lý câu hỏi
            </button>
            {quiz && total > 0 && (
              <button
                type="button"
                onClick={handleDeleteAll}
                className="p-1.5 text-gray-400 hover:text-rose-600 rounded shrink-0"
                title="Xoá toàn bộ câu hỏi"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            )}
          </>
        )}
      </div>

      {/* ============== Modal: quản lý câu hỏi (CRUD inline) ============== */}
      <Modal
        open={isEditorOpen}
        onCancel={closeEditor}
        footer={null}
        title={
          <div className="flex items-center gap-2 font-bold text-gray-900">
            <BookOpen className="w-5 h-5 text-cyan-600" />
            Quản lý câu hỏi — {quizTitle || 'Bài kiểm tra'}
          </div>
        }
        width={920}
        destroyOnHidden
      >
        <div className="py-2 space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold text-cyan-700 uppercase tracking-wider">
              Danh sách câu hỏi ({questions.length})
            </p>
            <Button
              type="primary"
              size="small"
              icon={<Plus className="w-3 h-3" />}
              onClick={() => {
                setEditingQuestion(null)
                setQuestionFormOpen(true)
              }}
              className="!rounded"
            >
              Thêm câu hỏi
            </Button>
          </div>

          {questions.length === 0 && (
            <div className="py-4 text-center text-xs text-gray-400 border border-dashed border-gray-200 rounded-md">
              Chưa có câu hỏi nào. Bấm "Thêm câu hỏi" để tạo.
            </div>
          )}

          <div className="space-y-2 max-h-[420px] overflow-y-auto pr-1">
            {questionsFull.map((q, idx) => {
              const qType = (q.questionType ?? 'MULTIPLE_CHOICE') as string
              const correctOpt = q.options?.find((o) => o.correct)
              // idx trong mảng đã sort theo orderIndex từ BE (findByQuizIdAndDeletedFalseOrderByOrderIndexAsc)
              // → dùng idx trực tiếp để enable/disable nút ↑/↓
              const isFirst = idx === 0
              const isLast = idx === questionsFull.length - 1
              const isReordering = reorderQuiz.isPending
              return (
                <div
                  key={q.id}
                  className="flex items-start gap-2 p-2.5 bg-white rounded-md border border-gray-200 hover:border-cyan-300 transition group"
                >
                  <div className="shrink-0 w-7 h-7 rounded bg-cyan-50 text-cyan-700 font-bold text-xs flex items-center justify-center">
                    #{q.orderIndex ?? '?'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-sm text-gray-900 line-clamp-2">
                      {q.questionText}
                    </p>
                    <p className="text-[11px] text-gray-500 mt-0.5">
                      {QUIZ_TYPE_LABEL[qType] ?? qType}
                      {qType === 'MULTIPLE_CHOICE' && correctOpt && (
                        <span className="ml-1.5 inline-flex items-center px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-700 font-bold">
                          ✓ {correctOpt.optionText}
                        </span>
                      )}
                      {qType === 'FILL_IN_THE_BLANK' && q.correctTextAnswer && (
                        <span className="ml-1.5 inline-flex items-center px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-700 font-bold">
                          ✓ {q.correctTextAnswer}
                        </span>
                      )}
                    </p>
                  </div>
                  {/* Nút ↑/↓ reorder — luôn hiển thị (không hover) để dễ thao tác.
                      Enable/disable theo vị trí đầu/cuối. */}
                  {/* <div className="flex flex-col gap-0.5 shrink-0">
                    <button
                      type="button"
                      onClick={() => handleReorderQuestion('up', q.id)}
                      disabled={isFirst || isReordering}
                      className="p-0.5 text-gray-400 hover:text-cyan-600 rounded disabled:opacity-30 disabled:cursor-not-allowed"
                      title="Di chuyển lên"
                    >
                      <ChevronUp className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleReorderQuestion('down', q.id)}
                      disabled={isLast || isReordering}
                      className="p-0.5 text-gray-400 hover:text-cyan-600 rounded disabled:opacity-30 disabled:cursor-not-allowed"
                      title="Di chuyển xuống"
                    >
                      <ChevronDown className="w-3.5 h-3.5" />
                    </button>
                  </div> */}
                  <div className="flex gap-0.5 opacity-0 group-hover:opacity-100 transition shrink-0">
                    <button
                      type="button"
                      onClick={() => setViewingQuestion(q)}
                      className="p-1 text-gray-400 hover:text-cyan-600 rounded"
                      title="Xem chi tiết"
                    >
                      <Eye className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setEditingQuestion(q as unknown as LessonQuizItem)
                        setQuestionFormOpen(true)
                      }}
                      className="p-1 text-gray-400 hover:text-indigo-600 rounded"
                      title="Sửa"
                    >
                      <Pencil className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        Modal.confirm({
                          title: 'Xoá câu hỏi?',
                          content: 'Hành động này không thể hoàn tác.',
                          okText: 'Xoá',
                          cancelText: 'Huỷ',
                          okButtonProps: { danger: true },
                          onOk: () => deleteQuizItem.mutate(q.id),
                        })
                      }}
                      className="p-1 text-gray-400 hover:text-rose-600 rounded"
                      title="Xoá"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </Modal>

      {/* ============== Modal: xem chi tiết 1 câu hỏi ============== */}
      <QuestionViewModal
        question={viewingQuestion}
        onClose={() => setViewingQuestion(null)}
      />

      {/* ============== Modal: form tạo/sửa 1 câu hỏi ============== */}
      <QuestionFormModal
        open={questionFormOpen}
        lessonId={lessonId}
        editing={editingQuestion}
        onClose={() => {
          setQuestionFormOpen(false)
          setEditingQuestion(null)
        }}
      />

      {/* ============== Modal: tạo câu hỏi đầu tiên (khi 404) ============== */}
      <QuestionFormModal
        open={isCreateOpen}
        lessonId={lessonId}
        editing={null}
        onClose={closeCreate}
      />

      {/* ============== Modal: đổi tiêu đề ============== */}
      <Modal
        open={isRenameOpen}
        onCancel={closeRename}
        onOk={handleRename}
        title="Đổi tiêu đề Quiz"
        okText="Lưu"
        cancelText="Huỷ"
        destroyOnHidden
      >
        <div className="py-2 space-y-2">
          <label className="text-xs font-semibold text-gray-700">Tiêu đề mới</label>
          <Input
            value={renameValue}
            onChange={(e) => setRenameValue(e.target.value)}
            placeholder="VD: Bài kiểm tra cuối khóa"
            size="large"
          />
          <p className="text-[11px] text-amber-600">
            ⚠ Tính năng này cần BE hỗ trợ PUT /lessons/{'{id}'}/quiz
          </p>
        </div>
      </Modal>
    </div>
  )
}

// ============================================================================
// Sub: modal xem chi tiết 1 câu hỏi (chỉ đọc, dành cho admin/teacher)
// ============================================================================
interface QuestionViewModalProps {
  question: QuizQuestionWithAnswer | null
  onClose: () => void
}

const QuestionViewModal: React.FC<QuestionViewModalProps> = ({ question, onClose }) => {
  if (!question) return null

  const qType = (question.questionType ?? 'MULTIPLE_CHOICE') as string

  return (
    <Modal
      open={!!question}
      onCancel={onClose}
      footer={null}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <Eye className="w-5 h-5 text-cyan-600" />
          Chi tiết câu hỏi #{question.orderIndex ?? '?'}
        </div>
      }
      width={820}
      destroyOnHidden
    >
      <div className="space-y-3 pt-2">
        {/* Header: loại + thứ tự */}
        <div className="flex items-center gap-2 flex-wrap">
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold bg-cyan-100 text-cyan-700">
            {QUIZ_TYPE_LABEL[qType] ?? qType}
          </span>
          {typeof question.orderIndex === 'number' && (
            <span className="text-[11px] text-gray-500">
              Thứ tự: <span className="font-bold text-gray-700">#{question.orderIndex}</span>
            </span>
          )}
        </div>

        {/* Nội dung câu hỏi */}
        <div className="rounded-md border border-gray-200 bg-gray-50/50 p-3">
          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">
            Nội dung câu hỏi
          </p>
          <p className="text-sm font-semibold text-gray-900 whitespace-pre-wrap">
            {question.questionText || (
              <span className="italic text-gray-400 font-normal">(Trống)</span>
            )}
          </p>
        </div>

        {/* MCQ: hiển thị các option + đánh dấu đáp án đúng */}
        {qType === 'MULTIPLE_CHOICE' && (
          <div className="rounded-md border border-cyan-200 bg-cyan-50/30 p-3 space-y-2">
            <p className="text-xs font-bold text-cyan-700 uppercase tracking-wider">
              Các đáp án
            </p>
            <div className="space-y-1.5">
              {(question.options ?? []).map((o, idx) => {
                const isCorrect = !!o.correct
                return (
                  <div
                    key={o.id ?? idx}
                    className={`flex items-start gap-2 p-2 rounded border-2 transition ${
                      isCorrect
                        ? 'border-emerald-400 bg-emerald-50'
                        : 'border-gray-200 bg-white'
                    }`}
                  >
                    <div
                      className={`shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-[11px] font-bold ${
                        isCorrect
                          ? 'bg-emerald-500 text-white'
                          : 'bg-gray-200 text-gray-600'
                      }`}
                    >
                      {isCorrect ? <Check className="w-3.5 h-3.5" /> : String.fromCharCode(65 + idx)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-gray-900">{o.optionText}</p>
                    </div>
                    {isCorrect && (
                      <span className="shrink-0 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-700">
                        ĐÚNG
                      </span>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {/* FILL_IN_THE_BLANK: ô input read-only hiển thị correctTextAnswer */}
        {qType === 'FILL_IN_THE_BLANK' && (
          <div className="rounded-md border border-amber-200 bg-amber-50/30 p-3 space-y-2">
            <label className="block text-xs font-bold text-amber-700 uppercase tracking-wider">
              Đáp án đúng (correctTextAnswer)
            </label>
            <input
              type="text"
              value={question.correctTextAnswer ?? ''}
              readOnly
              placeholder="(Chưa đặt)"
              className="w-full px-3 py-2 text-sm font-bold text-amber-900 bg-white border-2 border-amber-300 rounded-md focus:outline-none cursor-default"
            />
            <p className="text-[11px] text-amber-700/70">
              Hệ thống so sánh không phân biệt hoa/thường, bỏ qua khoảng trắng đầu/cuối.
            </p>
          </div>
        )}

        {/* MATCHING: bảng 2 cột left/right + ô correctTextAnswer bên dưới */}
        {qType === 'MATCHING' && (
          <div className="rounded-md border border-indigo-200 bg-indigo-50/30 p-3 space-y-3">
            <p className="text-xs font-bold text-indigo-700 uppercase tracking-wider">
              Dữ liệu ghép cặp
            </p>

            {(() => {
              const data = (question as any).matchingData as
                | { left: Array<{ key: string; label: string }>; right: Array<{ key: string; label: string }> }
                | undefined

              if (!data?.left?.length && !data?.right?.length) {
                return (
                  <p className="text-xs italic text-gray-400">
                    (Chưa có dữ liệu ghép cặp)
                  </p>
                )
              }

              // Căn số hàng = max(left, right) để không mất dòng nào
              const maxRows = Math.max(data?.left?.length ?? 0, data?.right?.length ?? 0)
              const left = data?.left ?? []
              const right = data?.right ?? []

              return (
                <div className="rounded-md border border-indigo-200 overflow-hidden bg-white mt-1">
                  {/* Header */}
                  <div className="grid grid-cols-2 bg-indigo-100/60 border-b border-indigo-200">
                    <div className="px-3 py-1.5 text-[10px] font-bold text-indigo-700 uppercase tracking-wider border-r border-indigo-200">
                      Vế trái
                    </div>
                    <div className="px-3 py-1.5 text-[10px] font-bold text-indigo-700 uppercase tracking-wider">
                      Vế phải
                    </div>
                  </div>
                  {/* Rows */}
                  <div className="divide-y divide-indigo-100">
                    {Array.from({ length: maxRows }).map((_, i) => {
                      const l = left[i]
                      const r = right[i]
                      return (
                        <div
                          key={i}
                          className="grid grid-cols-2 hover:bg-indigo-50/40 transition"
                        >
                          {/* Left cell */}
                          <div className="px-3 py-2 border-r border-indigo-100 flex items-center gap-2 min-h-[40px]">
                            {/* <span className="shrink-0 inline-flex items-center justify-center w-6 h-6 rounded text-[10px] font-bold bg-indigo-100 text-indigo-600">
                              {l?.key ?? '—'}
                            </span> */}
                            <span className="text-sm text-gray-900 font-semibold">
                              {l?.label || (
                                <span className="italic text-gray-400 font-normal">(trống)</span>
                              )}
                            </span>
                          </div>
                          {/* Right cell */}
                          <div className="px-3 py-2 flex items-center gap-2 min-h-[40px]">
                            {/* <span className="shrink-0 inline-flex items-center justify-center w-6 h-6 rounded text-[10px] font-bold bg-indigo-100 text-indigo-600">
                              {r?.key ?? '—'}
                            </span> */}
                            <span className="text-sm text-gray-900 font-semibold">
                              {r?.label || (
                                <span className="italic text-gray-400 font-normal">(trống)</span>
                              )}
                            </span>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })()}

            {/* Danh sách cặp đúng in thẳng từng dòng (không JSON) */}
            <div className="space-y-1.5 pt-1">
              <label className="block text-xs font-bold text-indigo-700 uppercase tracking-wider">
                Đáp án đúng
              </label>
              {(() => {
                const map = question.correctMatchingAnswers
                if (!map || Object.keys(map).length === 0) {
                  return (
                    <p className="text-xs italic text-gray-400">(Chưa có đáp án đúng)</p>
                  )
                }
                return (
                  <div className="rounded-md border-2 border-indigo-200 bg-white divide-y divide-indigo-100 overflow-hidden">
                    {Object.entries(map).map(([matchKey, label]) => (
                      <div
                        key={matchKey}
                        className="px-3 py-1.5 text-sm text-indigo-900 font-semibold hover:bg-indigo-50/40"
                      >
                        {label}
                      </div>
                    ))}
                  </div>
                )
              })()}
              <p className="text-[11px] text-indigo-700/70">
                Mỗi entry là 1 cặp đúng <code className="bg-indigo-100 px-1 rounded">L_key→R_key</code> = nhãn ghép.
                Hệ thống chấm dựa trên <code className="bg-indigo-100 px-1 rounded">matchKey</code> của từng option, không dựa vào nhãn.
              </p>
            </div>
          </div>
        )}

        {/* Giải thích (nếu có) */}
        {question.explanation && (
          <div className="rounded-md border border-blue-200 bg-blue-50/30 p-3 space-y-1">
            <p className="text-xs font-bold text-blue-700 uppercase tracking-wider">
              Giải thích
            </p>
            <p className="text-sm text-gray-800 whitespace-pre-wrap">
              {question.explanation}
            </p>
          </div>
        )}
      </div>
    </Modal>
  )
}

// ============================================================================
// Sub: modal form CRUD 1 câu hỏi (dùng cho cả tạo + sửa + tạo đầu tiên)
// ============================================================================
interface McOptionRow {
  optionKey: number
  optionText: string
  correct: boolean
}

interface QuestionFormModalProps {
  open: boolean
  lessonId: number
  editing: LessonQuizItem | null
  onClose: () => void
}

const QuestionFormModal: React.FC<QuestionFormModalProps> = ({
  open,
  lessonId,
  editing,
  onClose,
}) => {
  const { message } = App.useApp()
  const { createQuizItem, updateQuizItem } = useQuizItemMutations(lessonId)

  // Lấy dữ liệu lesson (kèm sections + items) để làm candidates cho MATCHING.
  // Hook có sẵn sẽ tự cache 60s; nếu lesson chưa load thì vẫn cho phép nhập tay.
  const { data: lesson } = useLessonDetail(lessonId)

  const [questionText, setQuestionText] = useState('')
  const [explanation, setExplanation] = useState('')
  const [orderIndex, setOrderIndex] = useState<number | undefined>(undefined)
  const [questionType, setQuestionType] = useState<string>('MULTIPLE_CHOICE')
  const [mcOptions, setMcOptions] = useState<McOptionRow[]>([
    { optionKey: 1, optionText: '', correct: true },
    { optionKey: 2, optionText: '', correct: false },
  ])
  const [correctText, setCorrectText] = useState('')
  const [matchingData, setMatchingData] = useState<MatchingData>({
    left: [
      { key: 'L1', label: '' },
      { key: 'L2', label: '' },
    ],
    right: [
      { key: 'R1', label: '' },
      { key: 'R2', label: '' },
    ],
  })

  /**
   * Parse matchingData an toàn từ nhiều shape.
   * BE đang lưu shape `{left: [...], right: [...]}`, nhưng giữ fallback
   * cho shape cũ `{pairs: [...]}` phòng khi BE đổi.
   */
  const parseMatching = (raw: unknown): MatchingData => {
    const empty: MatchingData = { left: [], right: [] }
    if (!raw) return empty
    const anyRaw = raw as any
    // Shape chính: { left: [...], right: [...] }
    if (Array.isArray(anyRaw.left) && Array.isArray(anyRaw.right)) {
      return {
        left: anyRaw.left.filter((s: any) => s && s.key),
        right: anyRaw.right.filter((s: any) => s && s.key),
      }
    }
    // Shape cũ: { pairs: [...] } → tách ngược về left/right
    if (Array.isArray(anyRaw.pairs) && anyRaw.pairs.length > 0) {
      return {
        left: anyRaw.pairs.map((p: any, i: number) => ({
          key: p.leftKey ?? `L${i + 1}`,
          label: p.leftLabel ?? '',
        })),
        right: anyRaw.pairs.map((p: any, i: number) => ({
          key: p.rightKey ?? `R${i + 1}`,
          label: p.rightLabel ?? '',
        })),
      }
    }
    return empty
  }

  const seedMatching = (): MatchingData => ({
    left: [
      { key: 'L1', label: '' },
      { key: 'L2', label: '' },
    ],
    right: [
      { key: 'R1', label: '' },
      { key: 'R2', label: '' },
    ],
  })

  useEffect(() => {
    if (!open) return
    if (editing) {
      setQuestionText(editing.questionText ?? '')
      setExplanation(editing.explanation ?? '')
      setOrderIndex(editing.orderIndex ?? undefined)
      const qType = (editing.questionType ?? 'MULTIPLE_CHOICE') as string
      setQuestionType(qType)
      if (qType === 'MULTIPLE_CHOICE') {
        const opts = (editing.options ?? []).map((o: any, i: number) => ({
          optionKey: o.optionKey ?? i + 1,
          optionText: o.optionText ?? '',
          correct: o.correct ?? false,
        }))
        setMcOptions(opts.length >= 2 ? opts : [
          { optionKey: 1, optionText: '', correct: true },
          { optionKey: 2, optionText: '', correct: false },
        ])
      } else if (qType === 'FILL_IN_THE_BLANK') {
        setCorrectText(editing.correctTextAnswer ?? '')
      } else if (qType === 'MATCHING') {
        const parsed = parseMatching((editing as any).matchingData)
        setMatchingData(
          parsed.left.length >= 2 && parsed.right.length >= 2 ? parsed : seedMatching(),
        )
      }
    } else {
      setQuestionText('')
      setExplanation('')
      setOrderIndex(undefined)
      setQuestionType('MULTIPLE_CHOICE')
      setMcOptions([
        { optionKey: 1, optionText: '', correct: true },
        { optionKey: 2, optionText: '', correct: false },
      ])
      setCorrectText('')
      setMatchingData(seedMatching())
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing])

  const handleOk = async () => {
    if (!questionText.trim()) {
      message.error('Vui lòng nhập nội dung câu hỏi')
      return
    }
    const payload: any = {
      questionText: questionText.trim(),
      questionType,
      explanation: explanation.trim() || undefined,
      orderIndex,
    }
    if (questionType === 'MULTIPLE_CHOICE') {
      if (mcOptions.length < 2) {
        message.error('Câu trắc nghiệm cần ít nhất 2 đáp án')
        return
      }
      if (mcOptions.some((o) => !o.optionText.trim())) {
        message.error('Vui lòng nhập nội dung cho tất cả đáp án')
        return
      }
      if (mcOptions.filter((o) => o.correct).length !== 1) {
        message.error('Vui lòng chọn đúng 1 đáp án đúng')
        return
      }
      payload.options = mcOptions.map((o) => ({
        optionKey: o.optionKey,
        optionText: o.optionText.trim(),
        correct: o.correct,
      }))
    } else if (questionType === 'FILL_IN_THE_BLANK') {
      if (!correctText.trim()) {
        message.error('Vui lòng nhập đáp án đúng cho câu điền')
        return
      }
      payload.correctTextAnswer = correctText.trim()
    } else if (questionType === 'MATCHING') {
      if (matchingData.left.length < 2 || matchingData.right.length < 2) {
        message.error('Câu ghép cặp cần ít nhất 2 cặp (mỗi cột 2 phần tử)')
        return
      }
      if (matchingData.left.length !== matchingData.right.length) {
        message.error('Số lượng vế trái phải bằng số lượng vế phải')
        return
      }
      if (
        matchingData.left.some((s) => !s.label.trim()) ||
        matchingData.right.some((s) => !s.label.trim())
      ) {
        message.error('Vui lòng nhập đầy đủ nội dung cho cả 2 vế')
        return
      }
      const leftLabels = matchingData.left.map((s) => s.label.trim().toLowerCase())
      const rightLabels = matchingData.right.map((s) => s.label.trim().toLowerCase())
      if (new Set(leftLabels).size !== leftLabels.length) {
        message.error('Các vế trái bị trùng nội dung. Mỗi vế trái phải khác nhau.')
        return
      }
      if (new Set(rightLabels).size !== rightLabels.length) {
        message.error('Các vế phải bị trùng nội dung. Mỗi vế phải phải khác nhau.')
        return
      }
      payload.matchingData = matchingData

      // ========================================================================
      // MATCHING — gửi cặp đúng + cột phải đã shuffle
      // ----------------------------------------------------------------------
      // Cơ chế chấm của BE (xem QuizService.gradeQuiz):
      //   - Duyệt tất cả options, lấy (matchKey, optionText) làm cặp đúng.
      //   - So sánh userPairs[matchKey] === optionText (string match, ignore case).
      //
      // Cấu trúc matchKey (đồng bộ với QuizStudyView khi submit):
      //   matchKey   = "L{leftKey}=R{rightKey}"  (dùng KEY thuần, không theo index)
      //   optionText = "leftLabel = rightLabel"
      //
      // Tại sao dùng KEY thuần thay vì index?
      // - Lưc save: left/right ở cặp đúng (vd L1↔R1, L2↔R2) → matchKey là
      //   chuỗi cố định "L1=R1", "L2=R2", ...
      // - DB: matchingData.right được shuffle để hiển thị khác thứ tự, nhưng
      //   key của từng item vẫn giữ nguyên (R1, R2, R3, ...) — vì shuffle
      //   chỉ đổi vị trí, không đổi key.
      // - Submit: học viên chọn (leftKey, rightKey) dựa trên key, không dựa
      //   trên index hiển thị. Nên matchKey phải dùng key thuần để bất biến
      //   với shuffle.
      //
      // Ví dụ: cặp đúng gốc L1↔R1, L2↔R2, L3↔R3
      //   Sau shuffle: matchingData.right = [R3, R1, R2] (key giữ nguyên)
      //   Viewer hiển thị cột B: "ba, 3" (R3), "một, 1" (R1), "hai, 2" (R2)
      //   options lưu trong DB:
      //     optionKey=1, optionText="一 = một, 1", matchKey="L1=R1"
      //     optionKey=2, optionText="二 = hai, 2", matchKey="L2=R2"
      //     optionKey=3, optionText="三 = ba, 3", matchKey="L3=R3"
      //   Học viên ghép L1→R1: submit { "L1=R1": "一 = một, 1" } → BE khớp ✅
      // ========================================================================

      // Bước 1: dựng options[] theo cặp đúng (left[i] ↔ right[i] CÙNG INDEX
      // trong matchingData GỐC — trước khi shuffle).
      payload.options = matchingData.left.map((l, i) => {
        const right = matchingData.right[i]
        return {
          optionKey: i + 1,
          optionText: `${l.label.trim()} = ${right.label.trim()}`,
          correct: true,
          // Dùng KEY thuần (không phải i+1) → bất biến với shuffle right.
          matchKey: `${l.key}=${right.key}`,
        }
      })

      // Bước 2: shuffle mảng right để cột phải hiển thị thứ tự khác nhau mỗi
      // lần save. KHÔNG shuffle left. options[] không bị ảnh hưởng (đã build
      // ở bước 1 từ cặp đúng cùng index, key vẫn giữ nguyên).
      payload.matchingData = {
        ...matchingData,
        right: shuffleMatchingRight(matchingData.right),
      }
    }

    try {
      if (editing) {
        await updateQuizItem.mutateAsync({
          id: editing.id,
          orderIndex: editing.orderIndex,
          payload,
        })
      } else {
        await createQuizItem.mutateAsync(payload)
      }
      onClose()
    } catch {
      /* lỗi đã toast trong hook */
    }
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <Check className="w-5 h-5 text-cyan-600" />
          {editing ? 'Sửa câu hỏi' : 'Thêm câu hỏi mới'}
        </div>
      }
      okText={editing ? 'Lưu' : 'Thêm'}
      cancelText="Huỷ"
      confirmLoading={createQuizItem.isPending || updateQuizItem.isPending}
      destroyOnHidden
      width={820}
    >
      <div className="space-y-3 pt-2">
        {/* Loại câu hỏi */}
        <div>
          <label className="text-xs font-semibold text-gray-700 mb-1 block">Loại câu hỏi</label>
          <div className="grid grid-cols-3 gap-2">
            {(['MULTIPLE_CHOICE', 'FILL_IN_THE_BLANK', 'MATCHING'] as const).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setQuestionType(t)}
                disabled={!!editing}
                className={`px-3 py-2 rounded text-xs font-bold border-2 transition ${
                  questionType === t
                    ? 'border-cyan-500 bg-cyan-50 text-cyan-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-cyan-300'
                } ${editing ? 'opacity-60 cursor-not-allowed' : ''}`}
              >
                {QUIZ_TYPE_LABEL[t]}
              </button>
            ))}
          </div>
        </div>

        {/* Nội dung + giải thích + thứ tự */}
        <div className="rounded-md border border-emerald-200 bg-emerald-50/30 p-3 space-y-2">
          <div>
            <label className="text-xs font-semibold text-gray-700 mb-1 block">Nội dung câu hỏi</label>
            <Input.TextArea
              rows={2}
              value={questionText}
              onChange={(e) => setQuestionText(e.target.value)}
              placeholder="VD: Nghĩa của từ '勉強' là gì?"
            />
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div className="col-span-2">
              <label className="text-xs font-semibold text-gray-700 mb-1 block">Giải thích (tuỳ chọn)</label>
              <Input
                value={explanation}
                onChange={(e) => setExplanation(e.target.value)}
                placeholder="Hiển thị sau khi HV nộp bài"
              />
            </div>
            <div>
              <label className="text-xs font-semibold text-gray-700 mb-1 block">Thứ tự</label>
              <InputNumber
                min={1}
                value={orderIndex}
                onChange={(v) => setOrderIndex(v ?? undefined)}
                className="!w-full"
                placeholder="Tự gán"
              />
            </div>
          </div>
        </div>

        {/* Options cho MCQ */}
        {questionType === 'MULTIPLE_CHOICE' && (
          <div className="rounded-md border border-cyan-200 bg-cyan-50/30 p-3 space-y-2">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-cyan-700">Các đáp án (chọn 1 đáp án đúng)</p>
              <Button
                size="small"
                onClick={() =>
                  setMcOptions((prev) => [
                    ...prev,
                    { optionKey: prev.length + 1, optionText: '', correct: false },
                  ])
                }
                icon={<Plus className="w-3 h-3" />}
              >
                Thêm đáp án
              </Button>
            </div>
            {mcOptions.map((opt, idx) => (
              <div key={idx} className="flex items-center gap-2">
                <input
                  type="radio"
                  name="mc-correct"
                  checked={opt.correct}
                  onChange={() =>
                    setMcOptions((prev) => prev.map((o, i) => ({ ...o, correct: i === idx })))
                  }
                />
                <span className="text-xs font-bold text-gray-500 w-5">
                  {String.fromCharCode(65 + idx)}.
                </span>
                <Input
                  size="small"
                  value={opt.optionText}
                  onChange={(e) =>
                    setMcOptions((prev) =>
                      prev.map((o, i) => (i === idx ? { ...o, optionText: e.target.value } : o))
                    )
                  }
                  placeholder={`Đáp án ${String.fromCharCode(65 + idx)}`}
                  className="!flex-1"
                />
                {mcOptions.length > 2 && (
                  <button
                    type="button"
                    onClick={() =>
                      setMcOptions((prev) =>
                        prev
                          .filter((_, i) => i !== idx)
                          .map((o, i) => ({ ...o, optionKey: i + 1 }))
                      )
                    }
                    className="text-gray-400 hover:text-rose-600 p-0.5 shrink-0"
                  >
                    <XIcon className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Đáp án cho FILL_IN_THE_BLANK */}
        {questionType === 'FILL_IN_THE_BLANK' && (
          <div className="rounded-md border border-amber-200 bg-amber-50/30 p-3 space-y-2">
            <p className="text-xs font-bold text-amber-700">Đáp án đúng</p>
            <Input
              value={correctText}
              onChange={(e) => setCorrectText(e.target.value)}
              placeholder="VD: べんきょう"
            />
            <p className="text-[11px] text-amber-700/70">
              So sánh không phân biệt hoa/thường, bỏ qua khoảng trắng đầu/cuối.
            </p>
          </div>
        )}

        {/* MATCHING — 2 cột nối nhau */}
        {questionType === 'MATCHING' && (
          <div className="rounded-md border border-indigo-200 bg-indigo-50/30 p-3 space-y-2">
            <div className="flex items-center justify-between">
              <p className="text-xs font-bold text-indigo-700 uppercase tracking-wider">
                Câu ghép cặp
              </p>
              <span className="text-[10px] text-indigo-500 italic">
                Mỗi hàng = 1 cặp (vế trái ↔ vế phải)
              </span>
            </div>
            <MatchingPairsEditor
              mode="editor"
              value={matchingData}
              onChange={setMatchingData}
              lesson={lesson}
              onAutoFilled={(count) => {
                toast.success(`Đã tạo ${count} cặp ghép từ dữ liệu lesson`)
              }}
            />
          </div>
        )}
      </div>
    </Modal>
  )
}


