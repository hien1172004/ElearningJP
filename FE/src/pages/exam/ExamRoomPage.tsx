import React, { useState, useEffect, useRef, useMemo } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { examService } from '@/services'
import type { ExamTakingData, TakingQuestion, TakingSection, TakingPassage } from '@/types'
import { cleanJapaneseText, cleanQuestionPrompt, formatMondaiDisplay, formatJapanesePassageHtml } from '@/utils/format'
import { 
  Clock, AlertCircle, Flag, Send, 
  X, Loader2, Star, ChevronRight, CheckCircle2, Sparkles,
  Headphones, BookOpen, Check, ArrowRight
} from 'lucide-react'

export const ExamRoomPage: React.FC = () => {
  const { examId, attemptId } = useParams<{ examId: string; attemptId: string }>()
  const navigate = useNavigate()
  const location = useLocation()

  const [takingData, setTakingData] = useState<ExamTakingData | null>(location.state?.takingData || null)
  const [loading, setLoading] = useState(!location.state?.takingData)
  
  // Ca thi hiện tại (Session 0: Ca 1, Session 1: Ca 2, Session 2: Ca 3)
  const [currentSectionIndex, setCurrentSectionIndex] = useState<number>(0)

  // State lưu đáp án: questionId -> selectedOptionId
  const [answers, setAnswers] = useState<Record<number, number>>({})
  // State lưu câu đánh dấu cờ để xem lại: Set of questionIds
  const [flaggedQuestions, setFlaggedQuestions] = useState<Set<number>>(new Set())

  const flagStorageKey = attemptId ? `exam_flags_${attemptId}` : null

  // Tải trạng thái cắm cờ từ localStorage khi mở bài thi
  useEffect(() => {
    if (!flagStorageKey) return
    try {
      const saved = localStorage.getItem(flagStorageKey)
      if (saved) {
        const parsed = JSON.parse(saved)
        if (Array.isArray(parsed)) {
          setFlaggedQuestions(new Set(parsed))
        }
      }
    } catch (e) {
      console.error('Failed to load saved flagged questions', e)
    }
  }, [flagStorageKey])

  // Bộ đếm thời gian cho CA HIỆN TẠI (seconds)
  const [timeLeft, setTimeLeft] = useState<number>(30 * 60)
  const [submitting, setSubmitting] = useState(false)
  const [confirmSubmitOpen, setConfirmSubmitOpen] = useState(false)
  const [nextSectionModalOpen, setNextSectionModalOpen] = useState(false)

  // Lightbox xem ảnh tờ rơi phóng to
  const [lightboxImage, setLightboxImage] = useState<string | null>(null)

  // Câu hỏi đang được focus / kích hoạt khi tương tác với ô trống
  const [activeQuestionId, setActiveQuestionId] = useState<number | null>(null)
  // Bật/tắt hiển thị phiên âm Furigana
  const [showFurigana, setShowFurigana] = useState<boolean>(true)

  // Ref audio player
  const audioRef = useRef<HTMLAudioElement | null>(null)

  useEffect(() => {
    if (!takingData && examId && attemptId) {
      examService.startExam(Number(examId), { attemptType: 'FULL_EXAM' })
        .then(res => {
          if (res.data) setTakingData(res.data)
        })
        .catch(err => {
          alert('Không thể tải bài thi: ' + (err?.response?.data?.message || err.message))
          navigate('/exams')
        })
        .finally(() => setLoading(false))
    }
  }, [examId, attemptId])

  // Cập nhật thời gian khi bắt đầu hoặc chuyển Ca thi
  useEffect(() => {
    if (takingData && takingData.sections[currentSectionIndex]) {
      const sec = takingData.sections[currentSectionIndex]
      const durationMin = sec.durationMinutes || 30
      setTimeLeft(durationMin * 60)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }, [takingData, currentSectionIndex])

  // Timer đếm ngược cho Ca thi hiện tại
  useEffect(() => {
    if (timeLeft <= 0) {
      handleTimeExpired()
      return
    }

    const timer = setInterval(() => {
      setTimeLeft(prev => prev - 1)
    }, 1000)

    return () => clearInterval(timer)
  }, [timeLeft])

  // Xử lý khi hết giờ của Ca hiện tại
  const handleTimeExpired = () => {
    if (!takingData) return
    const isLastSec = currentSectionIndex >= takingData.sections.length - 1
    if (!isLastSec) {
      setNextSectionModalOpen(true)
    } else {
      handleSubmitExam()
    }
  }

  // Format time mm:ss
  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60)
    const s = secs % 60
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  const currentSection: TakingSection | undefined = takingData?.sections[currentSectionIndex]
  const currentQuestions: TakingQuestion[] = useMemo(() => {
    if (!currentSection?.questions) return []
    return [...currentSection.questions].sort((a, b) => (a.questionNumber ?? 0) - (b.questionNumber ?? 0))
  }, [currentSection])
  const isLastSection = takingData ? currentSectionIndex >= takingData.sections.length - 1 : true

  // Thống kê số câu đã làm trong ca hiện tại
  const currentAnsweredCount = currentQuestions.filter(q => !!answers[q.id]).length
  const currentProgressPercent = currentQuestions.length > 0 
    ? Math.round((currentAnsweredCount / currentQuestions.length) * 100) 
    : 0

  const currentSectionFlaggedCount = useMemo(() => {
    return currentQuestions.filter(q => flaggedQuestions.has(q.id)).length
  }, [currentQuestions, flaggedQuestions])

  const totalQuestionsAllSections = takingData?.sections.flatMap(s => s.questions).length || 0
  const totalAnsweredAllSections = Object.keys(answers).length

  // Bấm vào số chỗ trống 【 21 】 trong bài đọc để cuộn mượt đến câu hỏi tương ứng
  const handlePassageBlankClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const target = (e.target as HTMLElement).closest('[data-blank-num]')
    if (target) {
      const blankNum = target.getAttribute('data-blank-num')
      if (blankNum) {
        const foundQ = currentQuestions.find(item => item.questionNumber === Number(blankNum))
        if (foundQ) {
          scrollToQuestion(foundQ.id)
        }
      }
    }
  }

  // Xử lý chọn đáp án
  const handleSelectOption = (questionId: number, optionId: number) => {
    setAnswers(prev => ({ ...prev, [questionId]: optionId }))

    if (attemptId) {
      examService.saveAnswer(Number(attemptId), {
        questionId,
        selectedOptionId: optionId,
      }).catch(err => console.error('Auto-save failed', err))
    }
  }

  // Đánh dấu cờ và đồng bộ với localStorage
  const toggleFlagQuestion = (questionId: number) => {
    setFlaggedQuestions(prev => {
      const next = new Set(prev)
      if (next.has(questionId)) next.delete(questionId)
      else next.add(questionId)
      if (flagStorageKey) {
        try {
          localStorage.setItem(flagStorageKey, JSON.stringify(Array.from(next)))
        } catch (e) {
          console.error('Failed to save flagged questions to storage', e)
        }
      }
      return next
    })
  }

  // Cuộn mượt đến câu hỏi và kích hoạt nổi bật
  const scrollToQuestion = (questionId: number) => {
    setActiveQuestionId(questionId)
    const element = document.getElementById(`question-${questionId}`)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  // Phân cụm các câu hỏi theo bài đọc (1 đoạn văn có nhiều câu hỏi con)
  type ExamRoomItem =
    | {
        type: 'PASSAGE_GROUP'
        passage: TakingPassage
        mondaiCode?: string
        mondaiNameVi?: string
        instructionJp?: string
        instructionVi?: string
        questions: TakingQuestion[]
      }
    | {
        type: 'SINGLE'
        question: TakingQuestion
      }

  const examRoomItems: ExamRoomItem[] = useMemo(() => {
    const items: ExamRoomItem[] = []
    let curPassageGroup: {
      type: 'PASSAGE_GROUP'
      passage: TakingPassage
      mondaiCode?: string
      mondaiNameVi?: string
      instructionJp?: string
      instructionVi?: string
      questions: TakingQuestion[]
    } | null = null

    currentQuestions.forEach((q) => {
      const hasPassageData = q.passage && (q.passage.contentJp || q.passage.imageUrl)
      if (hasPassageData && q.passage) {
        const isSamePassage =
          curPassageGroup &&
          ((curPassageGroup.passage.id && q.passage.id && curPassageGroup.passage.id === q.passage.id) ||
            (Boolean(q.passage.contentJp) && curPassageGroup.passage.contentJp?.trim() === q.passage.contentJp?.trim()) ||
            (Boolean(q.passage.imageUrl) && curPassageGroup.passage.imageUrl?.trim() === q.passage.imageUrl?.trim()))

        if (isSamePassage && curPassageGroup) {
          curPassageGroup.questions.push(q)
        } else {
          curPassageGroup = {
            type: 'PASSAGE_GROUP',
            passage: q.passage,
            mondaiCode: q.mondaiCode,
            mondaiNameVi: q.mondaiNameVi,
            instructionJp: q.instructionJp,
            instructionVi: q.instructionVi,
            questions: [q],
          }
          items.push(curPassageGroup)
        }
      } else {
        curPassageGroup = null
        items.push({ type: 'SINGLE', question: q })
      }
    })

    return items
  }, [currentQuestions])

  // RENDER QUESTION CARD (Đã tối ưu hóa thanh thoát, chuẩn typography)
  const renderQuestionCard = (q: TakingQuestion, isNestedInPassage: boolean = false) => {
    const isFlagged = flaggedQuestions.has(q.id)
    const selectedOptId = answers[q.id]
    const isActive = activeQuestionId === q.id

    return (
      <div
        key={q.id}
        id={`question-${q.id}`}
        className={`transition-all duration-200 rounded-2xl bg-white border ${
          isActive
            ? 'border-indigo-500 ring-2 ring-indigo-500/20 shadow-md'
            : isFlagged
            ? 'border-amber-300 ring-1 ring-amber-300/60 shadow-xs'
            : selectedOptId
            ? 'border-slate-200 shadow-xs'
            : 'border-slate-200/90 hover:border-slate-300 shadow-xs'
        } ${isNestedInPassage ? 'p-4 sm:p-5' : 'p-5 sm:p-6'}`}
      >
        {/* Header câu hỏi: Số thứ tự + Cờ đánh dấu + Badge sao */}
        <div className="flex items-center justify-between gap-3 mb-3">
          <div className="flex items-center gap-2.5">
            <span className="w-8 h-8 rounded-xl bg-slate-900 text-white flex items-center justify-center font-bold text-xs tracking-tight shadow-2xs shrink-0">
              {q.questionNumber}
            </span>
            {q.starPosition && (
              <span className="text-[11px] font-bold text-amber-900 bg-amber-50 border border-amber-300 px-2.5 py-0.5 rounded-lg flex items-center gap-1.5 shadow-2xs">
                <Star className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                <span>Vị trí dấu sao ★</span>
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={() => toggleFlagQuestion(q.id)}
            className={`px-2.5 py-1 rounded-xl transition cursor-pointer flex items-center gap-1.5 text-xs font-bold border ${
              isFlagged
                ? 'bg-amber-50 border-amber-300 text-amber-800 shadow-2xs hover:bg-amber-100'
                : 'bg-slate-50 border-slate-200 text-slate-500 hover:text-amber-700 hover:border-amber-300 hover:bg-amber-50/50'
            }`}
            title={isFlagged ? 'Bỏ cắm cờ câu này' : 'Cắm cờ để xem lại câu này trước khi nộp bài'}
          >
            <Flag className={`w-3.5 h-3.5 ${isFlagged ? 'fill-amber-500 text-amber-500' : 'text-slate-400'}`} />
            <span>{isFlagged ? 'Đã cắm cờ' : 'Cắm cờ'}</span>
          </button>
        </div>

        {/* Tranh minh họa đề bài */}
        {q.imageUrl && (
          <div className="mb-4 text-center">
            <img
              src={q.imageUrl}
              alt="Tranh minh họa đề bài"
              onClick={() => setLightboxImage(q.imageUrl || null)}
              className="max-h-64 mx-auto rounded-xl border border-slate-200 object-contain shadow-xs cursor-zoom-in hover:opacity-95 transition"
            />
          </div>
        )}

        {/* Nội dung câu hỏi (Question Prompt) */}
        {(() => {
          const cleanedPrompt = cleanQuestionPrompt(q.questionText, q.instructionJp)
          const isJustBlank = /^[【\[(（]?\s*\d+\s*[】\])）]?$/.test(cleanedPrompt.trim())

          if (isJustBlank && isNestedInPassage) {
            return null
          }

          if (!cleanedPrompt) return null

          return (
            <div
              className="text-base sm:text-[17px] font-medium text-slate-900 mb-4 font-serif leading-relaxed [&_u]:underline [&_u]:underline-offset-8 [&_u]:decoration-2 [&_u]:font-bold [&_u]:text-indigo-950"
              dangerouslySetInnerHTML={{ __html: cleanedPrompt }}
            />
          )
        })()}

        {/* Trợ thị trực quan cho câu hỏi dấu sao ★: Mô hình 4 vị trí gạch dưới */}
        {Boolean(q.starPosition && q.starPosition > 0) && (
          <div className="mb-4 p-2.5 rounded-xl bg-amber-50/70 border border-amber-200/90 text-xs flex flex-wrap items-center gap-2">
            <span className="font-bold text-amber-950 flex items-center gap-1">
              <Star className="w-3.5 h-3.5 fill-amber-500 text-amber-600" />
              Sắp xếp:
            </span>
            <div className="flex items-center gap-1.5 font-mono">
              {[1, 2, 3, 4].map((pos) => {
                const isTarget = pos === q.starPosition
                return (
                  <span
                    key={pos}
                    className={`px-2.5 py-0.5 rounded-md text-xs font-bold transition ${
                      isTarget
                        ? 'bg-amber-400 text-slate-900 shadow-2xs ring-1 ring-amber-500'
                        : 'bg-white text-slate-500 border border-amber-200'
                    }`}
                  >
                    {isTarget ? '★ (Cần chọn)' : `___`}
                  </span>
                )
              })}
            </div>
          </div>
        )}

        {/* CÁC PHƯƠNG ÁN LỰA CHỌN (OPTIONS) */}
        {(() => {
          // Lọc bỏ đáp án trống (nếu câu chỉ có 3 đáp án hoặc đáp án thứ 4 bị rỗng)
          const isThreeOptionsMondai = q.mondaiCode === 'UTTERANCE_EXPRESSIONS' || q.mondaiCode === 'QUICK_RESPONSE'
          const visibleOptions = q.options.filter((opt) => {
            if (isThreeOptionsMondai && opt.optionKey > 3) return false
            if (q.hasVisualOptions) {
              return Boolean(opt.optionImageUrl || (opt.optionText && opt.optionText.trim().length > 0))
            }
            if (opt.optionImageUrl || (opt.optionText && opt.optionText.trim().length > 0)) {
              return true
            }
            const othersHaveContent = q.options.some((o) => o.id !== opt.id && (Boolean(o.optionText?.trim()) || Boolean(o.optionImageUrl)))
            if (othersHaveContent) return false
            return isThreeOptionsMondai ? opt.optionKey <= 3 : opt.optionKey <= 4
          })

          // Nếu đáp án là hình ảnh (Visual Options)
          if (q.hasVisualOptions) {
            return (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                {visibleOptions.map((opt) => {
                  const isSelected = selectedOptId === opt.id
                  return (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => handleSelectOption(q.id, opt.id)}
                      className={`p-3 rounded-2xl border transition-all text-left cursor-pointer flex flex-col justify-between gap-2.5 ${
                        isSelected
                          ? 'border-indigo-600 bg-indigo-50/70 ring-2 ring-indigo-500/20 shadow-xs'
                          : 'border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50/60'
                      }`}
                    >
                      <div className="flex items-center justify-between font-bold text-xs">
                        <div className="flex items-center gap-2 text-slate-800">
                          <span
                            className={`w-6 h-6 rounded-lg flex items-center justify-center text-xs font-bold transition ${
                              isSelected ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-700'
                            }`}
                          >
                            {opt.optionKey}
                          </span>
                          <span>Tranh {opt.optionKey}</span>
                        </div>
                        {isSelected && <Check className="w-4 h-4 text-indigo-600" />}
                      </div>
                      {opt.optionImageUrl && (
                        <img
                          src={opt.optionImageUrl}
                          alt={`Đáp án ${opt.optionKey}`}
                          className="w-full h-36 object-contain rounded-xl bg-slate-50 border border-slate-100"
                        />
                      )}
                    </button>
                  )
                })}
              </div>
            )
          }

          // Kiểm tra xem phương án có ngắn không để chia 2 cột cho thoáng
          const isShortOptions = visibleOptions.every((o) => (o.optionText || '').trim().length <= 26 && !o.optionImageUrl)
          const useTwoCols = (isNestedInPassage || isShortOptions) && visibleOptions.length === 4 && isShortOptions

          return (
            <div className={useTwoCols ? 'grid grid-cols-1 sm:grid-cols-2 gap-2.5' : 'space-y-2'}>
              {visibleOptions.map((opt) => {
                const isSelected = selectedOptId === opt.id
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => handleSelectOption(q.id, opt.id)}
                    className={`w-full ${useTwoCols ? 'p-3' : 'p-3.5'} rounded-xl border text-left flex items-center justify-between transition-all cursor-pointer ${
                      isSelected
                        ? 'border-indigo-600 bg-indigo-50/80 text-indigo-950 font-bold ring-2 ring-indigo-500/20 shadow-xs'
                        : 'border-slate-200 hover:border-indigo-300 hover:bg-slate-50/60 bg-white text-slate-800 font-medium'
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <span
                        className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shrink-0 transition ${
                          isSelected
                            ? 'bg-indigo-600 text-white shadow-2xs'
                            : 'bg-slate-100 text-slate-600 border border-slate-200'
                        }`}
                      >
                        {opt.optionKey}
                      </span>
                      <span
                        className="text-sm md:text-base font-serif truncate sm:whitespace-normal [&_u]:underline [&_u]:underline-offset-4 [&_u]:decoration-2 [&_u]:font-bold [&_u]:text-indigo-950"
                        dangerouslySetInnerHTML={{ __html: cleanJapaneseText(opt.optionText) }}
                      />
                    </div>

                    {isSelected && (
                      <span className="text-xs font-bold text-indigo-600 flex items-center gap-1 shrink-0 ml-2">
                        <CheckCircle2 className="w-4 h-4" />
                      </span>
                    )}
                  </button>
                )
              })}
            </div>
          )
        })()}
      </div>
    )
  }

  // Chuyển sang ca thi tiếp theo
  const handleProceedToNextSection = () => {
    setNextSectionModalOpen(false)
    if (takingData && currentSectionIndex < takingData.sections.length - 1) {
      setCurrentSectionIndex(prev => prev + 1)
    }
  }

  // Nộp bài thi
  const handleSubmitExam = async () => {
    if (!attemptId || submitting) return

    try {
      setSubmitting(true)
      const payloadAnswers = Object.entries(answers).map(([qId, optId]) => ({
        questionId: Number(qId),
        selectedOptionId: optId,
      }))

      const res = await examService.submitExam(Number(attemptId), {
        answers: payloadAnswers,
      })

      if (res.data) {
        if (flagStorageKey) {
          try {
            localStorage.removeItem(flagStorageKey)
          } catch (_) {}
        }
        navigate(`/exams/result/${attemptId}`, {
          state: { result: res.data },
        })
      }
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Có lỗi xảy ra khi nộp bài thi!')
      setSubmitting(false)
    }
  }

  if (loading || !takingData || !currentSection) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[70vh] bg-slate-50 font-sans">
        <Loader2 className="w-10 h-10 text-indigo-600 animate-spin mb-3" />
        <h2 className="text-base font-bold text-slate-800">Đang chuẩn bị phòng thi chuẩn JLPT...</h2>
        <p className="text-xs text-slate-400 mt-1">Vui lòng không tải lại trang</p>
      </div>
    )
  }

  return (
    <div className={`min-h-screen bg-slate-50/70 pb-20 font-sans text-slate-900 selection:bg-indigo-100 selection:text-indigo-900 ${showFurigana ? '' : 'hide-furigana'}`}>
      {/* 1. ZEN TOPBAR (Thanh công cụ làm bài tinh tế, cố định trên cùng) */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 lg:px-8 py-2.5 shadow-2xs">
        <div className="max-w-[1560px] mx-auto flex items-center justify-between gap-4">
          
          {/* Cụm trái: Huy hiệu JLPT & Bộ chuyển phần thi (Section Switcher) */}
          <div className="flex items-center gap-3 min-w-0">
            <span className="px-3 py-1 rounded-lg bg-slate-900 text-white text-xs font-black tracking-wider shrink-0 shadow-2xs">
              JLPT {takingData.jlptLevel}
            </span>

            {/* Các tab ca thi (Pills hiển thị tiến trình ca thi) */}
            <div className="hidden sm:flex items-center gap-1.5 p-1 bg-slate-100/90 rounded-xl border border-slate-200/60">
              {takingData.sections.map((sec, idx) => {
                const isActive = idx === currentSectionIndex
                const isPast = idx < currentSectionIndex
                const secAnsCount = sec.questions.filter(q => !!answers[q.id]).length

                return (
                  <div
                    key={sec.id || idx}
                    className={`px-3 py-1 rounded-lg text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
                      isActive
                        ? 'bg-white text-indigo-700 shadow-2xs'
                        : isPast
                        ? 'text-emerald-700 hover:text-emerald-800'
                        : 'text-slate-500'
                    }`}
                  >
                    {isPast && <Check className="w-3.5 h-3.5 text-emerald-600" />}
                    <span>Phần {sec.orderIndex}: {sec.sessionType === 'LISTENING' ? 'Nghe hiểu' : sec.sessionType === 'VOCABULARY' ? 'Từ vựng' : sec.sessionType === 'GRAMMAR_READING' ? 'Ngữ pháp - Đọc' : 'Kiến thức ngôn ngữ'}</span>
                    <span className="text-[10px] opacity-75 font-mono">({secAnsCount}/{sec.questions.length})</span>
                  </div>
                )
              })}
            </div>

            {/* Tiêu đề ngắn gọn trên mobile */}
            <span className="sm:hidden text-xs font-bold text-slate-800 truncate">
              Phần {currentSection.orderIndex}: {currentSection.sectionTitle}
            </span>
          </div>

          {/* Cụm phải: Đồng hồ đếm ngược + Nút Nộp bài / Chuyển ca */}
          <div className="flex items-center gap-2.5 shrink-0">
            {/* Nút bật/tắt hiển thị Furigana */}
            <button
              type="button"
              onClick={() => setShowFurigana(!showFurigana)}
              className={`hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition border cursor-pointer ${
                showFurigana
                  ? 'bg-indigo-50 border-indigo-200 text-indigo-700 hover:bg-indigo-100'
                  : 'bg-slate-100 border-slate-200 text-slate-500 hover:bg-slate-200'
              }`}
              title={showFurigana ? 'Đang bật phiên âm Furigana (Bấm để ẩn)' : 'Đang ẩn Furigana (Bấm để hiện)'}
            >
              <Sparkles className="w-3.5 h-3.5 text-current" />
              <span>ルビ: {showFurigana ? 'Bật' : 'Tắt'}</span>
            </button>
            {/* Bộ đếm thời gian (Zen Timer Widget) */}
            <div
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl font-mono text-sm sm:text-base font-black transition-all shadow-2xs ${
                timeLeft < 180
                  ? 'bg-rose-50 text-rose-600 border border-rose-300 ring-2 ring-rose-200 animate-pulse'
                  : timeLeft < 600
                  ? 'bg-amber-50 text-amber-700 border border-amber-300'
                  : 'bg-slate-100 text-slate-800 border border-slate-200'
              }`}
            >
              <Clock className="w-4 h-4 text-current shrink-0" />
              <span>{formatTime(timeLeft)}</span>
            </div>

            {/* Nút Chuyển ca hoặc Nộp toàn bộ bài */}
            {!isLastSection ? (
              <button
                type="button"
                onClick={() => setNextSectionModalOpen(true)}
                className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white rounded-xl font-bold text-xs sm:text-sm shadow-xs transition flex items-center gap-1.5 cursor-pointer"
              >
                <span>Xong Phần {currentSection.orderIndex}</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmSubmitOpen(true)}
                className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white rounded-xl font-bold text-xs sm:text-sm shadow-xs transition flex items-center gap-1.5 cursor-pointer"
              >
                <Send className="w-4 h-4" />
                <span>Nộp bài thi</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* 2. BODY CHÍNH (Giao diện 2 cột thông thoáng, loại bỏ hoàn toàn các khung lồng nhau thừa thãi) */}
      <div className="max-w-[1560px] mx-auto px-4 sm:px-6 lg:px-8 py-6 grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8 items-start">
        
        {/* CỘT TRÁI (9/12): DANH SÁCH BÀI ĐỌC VÀ CÂU HỎI */}
        <main className="lg:col-span-9 space-y-6">

          {/* Thanh Audio toàn bài nghe (nếu là phần Listening) */}
          {currentSection.sessionType === 'LISTENING' && currentSection.audioUrl && (
            <div className="p-4 bg-indigo-900 text-white rounded-2xl shadow-sm flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-600 text-white flex items-center justify-center shrink-0">
                  <Headphones className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 className="text-xs font-bold text-indigo-100">Âm thanh Ca thi Nghe hiểu (Choukai Audio Track)</h3>
                  <p className="text-[11px] text-indigo-300">File âm thanh phát xuyên suốt ca thi theo chuẩn JLPT</p>
                </div>
              </div>
              <audio
                ref={audioRef}
                controls
                src={currentSection.audioUrl}
                className="w-full sm:w-80 h-8 rounded-lg"
              />
            </div>
          )}

          {/* Render danh sách đề thi */}
          <div className="space-y-6">
            {examRoomItems.map((item, itemIdx) => {
              const curMondaiCode = item.type === 'SINGLE' ? item.question.mondaiCode : item.mondaiCode
              const curMondaiNameVi = item.type === 'SINGLE' ? item.question.mondaiNameVi : item.mondaiNameVi
              const curInstructionJp = item.type === 'SINGLE' ? item.question.instructionJp : item.instructionJp
              const curInstructionVi = item.type === 'SINGLE' ? item.question.instructionVi : item.instructionVi

              const prevItem = itemIdx > 0 ? examRoomItems[itemIdx - 1] : null
              const prevMondaiCode = prevItem ? (prevItem.type === 'SINGLE' ? prevItem.question.mondaiCode : prevItem.mondaiCode) : null
              const isNewMondai = !prevItem || prevMondaiCode !== curMondaiCode

              return (
                <div 
                  key={item.type === 'SINGLE' ? `single-${item.question.id}` : `passage-${item.passage.id || itemIdx}`} 
                  className="space-y-4"
                >
                  {/* MONDAI HEADER (Đề mục phân loại thanh thoát kiểu tạp chí hiện đại) */}
                  {isNewMondai && (
                    <div className="pt-2">
                      <div className="p-4 sm:p-5 rounded-2xl bg-gradient-to-r from-slate-100 via-white to-slate-50 border border-slate-200/90 shadow-2xs">
                        <div className="flex items-center gap-2 mb-1.5">
                          {(() => {
                            const mInfo = formatMondaiDisplay(curMondaiCode, curMondaiNameVi)
                            return (
                              <>
                                <span className="px-2.5 py-1 rounded-md bg-indigo-600 text-white text-xs font-black tracking-wide shrink-0">
                                  {mInfo.jp}
                                </span>
                                <h3 className="text-sm sm:text-base font-extrabold text-slate-900">
                                  {mInfo.vi}
                                </h3>
                              </>
                            )
                          })()}
                        </div>
                        {curInstructionJp && (
                          <p className="text-xs sm:text-sm text-slate-700 font-serif leading-relaxed mt-1">
                            {cleanJapaneseText(curInstructionJp)}
                          </p>
                        )}
                        {curInstructionVi && (
                          <p className="text-xs text-slate-400 mt-0.5">
                            ({curInstructionVi})
                          </p>
                        )}

                        {/* Hướng dẫn dấu sao - Thanh nhã, không choáng ngợp */}
                        {(curMondaiCode === 'SENTENCE_COMPOSITION' || 
                          curMondaiCode?.includes('STAR') || 
                          curMondaiNameVi?.includes('dấu sao') ||
                          curMondaiNameVi?.includes('文の組み立て')) && (
                          <div className="mt-3 p-3 bg-amber-50/80 rounded-xl border border-amber-200/80 text-xs text-amber-950 flex items-start gap-2.5">
                            <span className="w-5 h-5 rounded-md bg-amber-500 text-white flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">
                              ★
                            </span>
                            <div className="leading-relaxed">
                              <span className="font-bold text-amber-900">Quy cách làm bài dấu sao: </span>
                              <span>Sắp xếp 4 phương án vào 4 vị trí gạch dưới. <strong>Bạn chỉ cần chọn đáp án nằm ở vị trí dấu sao ★</strong>.</span>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {/* NỘI DUNG CHÍNH: SINGLE QUESTION HOẶC BÀI ĐỌC PASSAGE */}
                  {item.type === 'SINGLE' ? (
                    renderQuestionCard(item.question, false)
                  ) : (
                    /* KHỐI BÀI ĐỌC DOKKAI (Biên tập thanh thoát, dễ đọc) */
                    <div className="space-y-4">
                      {/* Thẻ bài đọc hiểu ở trên */}
                      <article className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
                        {/* Header bài đọc */}
                        <div className="bg-slate-50/80 border-b border-slate-200 px-5 py-3 flex items-center justify-between gap-3">
                          <div className="flex items-center gap-2.5">
                            <BookOpen className="w-4 h-4 text-indigo-600" />
                            <h4 className="text-sm sm:text-base font-bold text-slate-900 font-serif">
                              {item.passage.title || 'Bài đọc hiểu'}
                            </h4>
                          </div>
                          <span className="text-xs font-bold px-2.5 py-0.5 bg-white text-slate-700 border border-slate-200 rounded-lg shadow-2xs">
                            Câu {item.questions[0].questionNumber} ~ {item.questions[item.questions.length - 1].questionNumber} ({item.questions.length} câu)
                          </span>
                        </div>

                        {/* Thân bài đọc hiểu: Font serif chuẩn Nhật, line-height 2.2 thoáng mắt */}
                        <div className="p-6 sm:p-8 bg-white">
                          {item.passage.contentJp && (
                            <div
                              onClick={handlePassageBlankClick}
                              className="text-slate-800 bg-white select-text font-serif text-[16px] sm:text-[17px] leading-[2.2] tracking-wide"
                              title="Bấm vào số chỗ trống 【 21 】 để cuộn nhanh đến câu hỏi tương ứng bên dưới"
                              dangerouslySetInnerHTML={{ __html: formatJapanesePassageHtml(item.passage.contentJp) }}
                            />
                          )}
                        </div>
                      </article>

                      {/* Các câu hỏi con thuộc bài đọc */}
                      <div className="space-y-3.5 pl-0 sm:pl-3 border-l-0 sm:border-l-2 sm:border-indigo-100">
                        {item.questions.map((subQ) => renderQuestionCard(subQ, true))}
                      </div>
                    </div>
                  )}
                </div>
              )
            })}
          </div>

          {/* Thanh chuyển ca / nộp bài cuối trang */}
          <div className="pt-6 border-t border-slate-200 flex justify-end">
            {!isLastSection ? (
              <button
                type="button"
                onClick={() => setNextSectionModalOpen(true)}
                className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold text-sm shadow-xs transition flex items-center gap-2 cursor-pointer"
              >
                <span>Hoàn thành Phần {currentSection.orderIndex} & Tiếp tục</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmSubmitOpen(true)}
                className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-sm shadow-xs transition flex items-center gap-2 cursor-pointer"
              >
                <Send className="w-4 h-4" />
                <span>Nộp bài & Xem kết quả</span>
              </button>
            )}
          </div>
        </main>

        {/* CỘT PHẢI (3/12): QUESTION PALETTE (Bảng điều hướng số câu cố định - không trôi khi cuộn) */}
        <aside className="lg:col-span-3 lg:sticky lg:top-[68px] lg:self-start z-30">
          <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs p-4 sm:p-5 flex flex-col max-h-[calc(100vh-84px)] space-y-3">
            
            {/* Header Palette & Thanh tiến độ */}
            <div className="shrink-0">
              <div className="flex items-center justify-between mb-1.5">
                <h3 className="font-bold text-xs uppercase tracking-wider text-slate-500">
                  Phần {currentSection.orderIndex}: Số câu hỏi
                </h3>
                <span className="text-xs font-black text-indigo-600">
                  {currentAnsweredCount}/{currentQuestions.length} ({currentProgressPercent}%)
                </span>
              </div>
              
              {/* Progress bar mỏng thanh thoát */}
              <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                <div
                  className="bg-indigo-600 h-1.5 rounded-full transition-all duration-300"
                  style={{ width: `${currentProgressPercent}%` }}
                />
              </div>
            </div>

            {/* Chú thích màu sắc (Legend) */}
            <div className="shrink-0 flex items-center justify-between text-[11px] font-semibold text-slate-500 border-y border-slate-100 py-2">
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-indigo-600 shrink-0" />
                <span>Đã làm</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-slate-200 shrink-0" />
                <span>Chưa làm</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3.5 h-3.5 rounded-full bg-amber-100 border border-amber-300 flex items-center justify-center shrink-0">
                  <Flag className="w-2 h-2 fill-amber-500 text-amber-500" />
                </span>
                <span>Cắm cờ</span>
              </div>
            </div>

            {/* Grid các ô số câu hỏi (Cuộn nội bộ êm ái) */}
            <div className="grid grid-cols-4 gap-2 overflow-y-auto pr-1 py-1 flex-1 min-h-[160px]">
              {currentQuestions.map((q) => {
                const isAnswered = !!answers[q.id]
                const isFlagged = flaggedQuestions.has(q.id)
                const isActive = activeQuestionId === q.id

                  return (
                    <button
                      key={q.id}
                      type="button"
                      onClick={() => scrollToQuestion(q.id)}
                      className={`relative h-9 rounded-xl font-bold text-xs transition-all flex items-center justify-center cursor-pointer ${
                        isActive ? 'ring-2 ring-indigo-600 ring-offset-1' : ''
                      } ${
                        isAnswered
                          ? 'bg-indigo-600 text-white shadow-2xs hover:bg-indigo-700'
                          : isFlagged
                          ? 'bg-amber-50 text-amber-900 border-2 border-amber-400 font-black'
                          : 'bg-slate-100/80 text-slate-700 hover:bg-slate-200/80 hover:text-slate-900'
                      }`}
                      title={`Câu ${q.questionNumber}${isAnswered ? ' • Đã làm' : ' • Chưa làm'}${isFlagged ? ' • Đã cắm cờ' : ''}`}
                    >
                      <span>{q.questionNumber}</span>
                      {isFlagged && (
                        <span
                          className={`absolute -top-1 -right-1 w-3.5 h-3.5 rounded-full flex items-center justify-center shadow-2xs ${
                            isAnswered ? 'bg-amber-400 text-slate-900 ring-1 ring-white' : 'bg-amber-500 text-white'
                          }`}
                        >
                          <Flag className="w-2 h-2 fill-current" />
                        </span>
                      )}
                    </button>
                  )
                })}
              </div>

            {/* Cụm nút hành động phía dưới Palette luôn cố định */}
            <div className="shrink-0 space-y-2 pt-1 border-t border-slate-100">
              {/* Nút nhảy nhanh đến câu cắm cờ kế tiếp */}
              {currentSectionFlaggedCount > 0 && (
                <button
                  type="button"
                  onClick={() => {
                    const flaggedList = currentQuestions.filter((q) => flaggedQuestions.has(q.id))
                    const nextFlagged =
                      flaggedList.find((q) => (activeQuestionId ? q.id > activeQuestionId : true)) ||
                      flaggedList[0]
                    if (nextFlagged) {
                      scrollToQuestion(nextFlagged.id)
                    }
                  }}
                  className="w-full py-1.5 bg-amber-50 hover:bg-amber-100/80 border border-amber-300 text-amber-800 rounded-xl font-bold text-[11px] flex items-center justify-center gap-1.5 transition cursor-pointer"
                >
                  <Flag className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                  <span>Xem câu cắm cờ tiếp theo</span>
                </button>
              )}

              {/* Nút hành động chính trong Palette */}
              {!isLastSection ? (
                <button
                  type="button"
                  onClick={() => setNextSectionModalOpen(true)}
                  className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white rounded-xl font-bold text-xs shadow-xs transition flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <span>Xong Phần {currentSection.orderIndex}</span>
                  <ChevronRight className="w-4 h-4" />
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => setConfirmSubmitOpen(true)}
                  className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white rounded-xl font-bold text-xs shadow-xs transition flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Send className="w-4 h-4" />
                  <span>Nộp bài ngay</span>
                </button>
              )}
            </div>
          </div>
        </aside>
      </div>

      {/* Modal Chuyển Ca Thi (Next Section Modal) */}
      {nextSectionModalOpen && takingData && currentSectionIndex < takingData.sections.length - 1 && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-xl border border-slate-200 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto mb-3 shadow-2xs">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-black text-slate-900 text-center mb-1">
              Hoàn thành Phần {currentSection.orderIndex}!
            </h3>
            <p className="text-xs text-slate-500 text-center mb-5">
              Bạn đã hoàn thành thời gian làm bài của {currentSection.sectionTitle}.
            </p>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 text-xs space-y-2 mb-6">
              <div className="font-bold text-slate-700 uppercase tracking-wider text-[11px]">
                Phần thi tiếp theo:
              </div>
              <div className="text-sm font-black text-indigo-600">
                {takingData.sections[currentSectionIndex + 1]?.sectionTitle}
              </div>
              <div className="flex justify-between text-slate-600 pt-2 border-t border-slate-200">
                <span>Thời gian:</span>
                <span className="font-bold text-slate-900">
                  {takingData.sections[currentSectionIndex + 1]?.durationMinutes} phút
                </span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Số câu hỏi:</span>
                <span className="font-bold text-slate-900">
                  {takingData.sections[currentSectionIndex + 1]?.questions.length} câu
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setNextSectionModalOpen(false)}
                className="py-2.5 rounded-xl font-bold text-xs text-slate-600 bg-slate-100 hover:bg-slate-200 transition cursor-pointer"
              >
                Kiểm tra lại
              </button>
              <button
                type="button"
                onClick={handleProceedToNextSection}
                className="py-2.5 rounded-xl font-bold text-xs text-white bg-indigo-600 hover:bg-indigo-700 transition flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
              >
                <span>Bắt đầu ca tiếp</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Xác Nhận Nộp Toàn Bộ Bài */}
      {confirmSubmitOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-xl border border-slate-200 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto mb-3 shadow-2xs">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-black text-slate-900 text-center mb-1">
              Bạn có chắc chắn muốn nộp bài?
            </h3>
            <p className="text-xs text-slate-500 text-center mb-5">
              Sau khi nộp bài, hệ thống sẽ tự động chấm điểm và bạn không thể thay đổi đáp án.
            </p>

            {/* Bảng thống kê tóm tắt trước khi nộp */}
            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 text-xs space-y-2 mb-6">
              <div className="flex justify-between items-center text-slate-700">
                <span>Số câu đã làm:</span>
                <strong className="text-emerald-600 font-bold">
                  {totalAnsweredAllSections} / {totalQuestionsAllSections} câu
                </strong>
              </div>
              <div className="flex justify-between items-center text-slate-700">
                <span>Số câu chưa làm:</span>
                <strong className="text-rose-600 font-bold">
                  {totalQuestionsAllSections - totalAnsweredAllSections} câu
                </strong>
              </div>
              {flaggedQuestions.size > 0 && (
                <div className="flex justify-between items-center text-amber-800 pt-2 border-t border-slate-200">
                  <span className="flex items-center gap-1.5 font-medium">
                    <Flag className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                    <span>Câu đang cắm cờ xem lại:</span>
                  </span>
                  <div className="flex items-center gap-2">
                    <strong className="font-bold">{flaggedQuestions.size} câu</strong>
                    <button
                      type="button"
                      onClick={() => {
                        setConfirmSubmitOpen(false)
                        const firstFlagged = currentQuestions.find(q => flaggedQuestions.has(q.id))
                        if (firstFlagged) {
                          scrollToQuestion(firstFlagged.id)
                        }
                      }}
                      className="text-[11px] underline text-amber-800 hover:text-amber-950 cursor-pointer font-bold"
                    >
                      Xem lại ngay
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setConfirmSubmitOpen(false)}
                className="py-2.5 rounded-xl font-bold text-xs text-slate-600 bg-slate-100 hover:bg-slate-200 transition cursor-pointer"
              >
                Làm tiếp
              </button>
              <button
                type="button"
                disabled={submitting}
                onClick={handleSubmitExam}
                className="py-2.5 rounded-xl font-bold text-xs text-white bg-emerald-600 hover:bg-emerald-700 transition flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang nộp...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>Nộp bài ngay</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox Xem Ảnh Phóng To */}
      {lightboxImage && (
        <div
          onClick={() => setLightboxImage(null)}
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs flex items-center justify-center p-4 cursor-zoom-out"
        >
          <div className="relative max-w-3xl max-h-[90vh]">
            <img
              src={lightboxImage}
              alt="Ảnh phóng to"
              className="max-w-full max-h-[85vh] rounded-2xl object-contain shadow-2xl"
            />
            <button
              onClick={() => setLightboxImage(null)}
              className="absolute -top-3 -right-3 w-8 h-8 rounded-full bg-white text-slate-800 flex items-center justify-center shadow-md font-bold text-xs cursor-pointer hover:bg-slate-100"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
