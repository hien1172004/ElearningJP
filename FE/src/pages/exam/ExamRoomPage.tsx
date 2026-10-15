import React, { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { examService } from '@/services'
import type { ExamTakingData, TakingQuestion, TakingSection } from '@/types'
import { cleanJapaneseText, cleanQuestionPrompt, formatMondaiDisplay, formatSectionName } from '@/utils/format'
import { 
  Clock, AlertCircle, Flag, Send, ZoomIn, 
  X, Loader2, Star, ChevronRight, CheckCircle2,
  Headphones
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

  // Bộ đếm thời gian cho CA HIỆN TẠI (seconds)
  const [timeLeft, setTimeLeft] = useState<number>(30 * 60)
  const [submitting, setSubmitting] = useState(false)
  const [confirmSubmitOpen, setConfirmSubmitOpen] = useState(false)
  const [nextSectionModalOpen, setNextSectionModalOpen] = useState(false)

  // Lightbox xem ảnh tờ rơi phóng to
  const [lightboxImage, setLightboxImage] = useState<string | null>(null)

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
    const isLastSection = currentSectionIndex >= takingData.sections.length - 1
    if (!isLastSection) {
      // Tự động chuyển ca tiếp theo
      setNextSectionModalOpen(true)
    } else {
      // Hết giờ ca cuối -> nộp bài
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
  const currentQuestions: TakingQuestion[] = currentSection?.questions || []
  const isLastSection = takingData ? currentSectionIndex >= takingData.sections.length - 1 : true

  // Thống kê số câu đã làm trong ca hiện tại
  const currentAnsweredCount = currentQuestions.filter(q => !!answers[q.id]).length
  const totalQuestionsAllSections = takingData?.sections.flatMap(s => s.questions).length || 0
  const totalAnsweredAllSections = Object.keys(answers).length

  // Xử lý chọn đáp án (câu hỏi thường & câu dấu sao ★ chọn đáp án vào vị trí sao)
  const handleSelectOption = (questionId: number, optionId: number) => {
    setAnswers(prev => ({ ...prev, [questionId]: optionId }))

    if (attemptId) {
      examService.saveAnswer(Number(attemptId), {
        questionId,
        selectedOptionId: optionId,
      }).catch(err => console.error('Auto-save failed', err))
    }
  }

  // Đánh dấu cờ
  const toggleFlagQuestion = (questionId: number) => {
    setFlaggedQuestions(prev => {
      const next = new Set(prev)
      if (next.has(questionId)) next.delete(questionId)
      else next.add(questionId)
      return next
    })
  }

  // Cuộn mượt đến câu hỏi
  const scrollToQuestion = (questionId: number) => {
    const element = document.getElementById(`question-${questionId}`)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
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
      <div className="flex flex-col items-center justify-center min-h-[70vh]">
        <Loader2 className="w-12 h-12 text-indigo-600 animate-spin mb-4" />
        <h2 className="text-lg font-bold text-gray-700">Đang chuẩn bị phòng thi chuẩn JLPT...</h2>
        <p className="text-sm text-gray-400">Vui lòng không tải lại trang</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-slate-100/80 pb-24 font-sans text-gray-900">
      {/* 1. Header Trang Thi Chuẩn JLPT */}
      <header className="bg-white border-b border-gray-300/80 sticky top-0 z-40 px-4 md:px-8 py-3 shadow-xs">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="px-3 py-1.5 rounded-xl bg-slate-900 text-white text-xs font-black tracking-wider uppercase shrink-0">
              JLPT {takingData.jlptLevel}
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-md border border-indigo-200 shrink-0 whitespace-nowrap">
                  Phần {currentSection.orderIndex} / {takingData.sections.length}
                </span>
                <h1 className="text-sm md:text-base font-extrabold text-gray-900 truncate">
                  {currentSection.sessionType === 'VOCABULARY'
                    ? 'Từ vựng (文字・語彙)'
                    : currentSection.sessionType === 'GRAMMAR_READING'
                    ? 'Ngữ pháp & Đọc hiểu (文法・読解)'
                    : currentSection.sessionType === 'LISTENING'
                    ? 'Nghe hiểu (聴解)'
                    : currentSection.sectionTitle}
                </h1>
              </div>
              <p className="text-xs text-gray-500 mt-0.5 truncate">
                Tiến độ: <span className="font-bold text-indigo-600">{currentAnsweredCount}</span> / {currentQuestions.length} câu
                {takingData.sections.length > 1 && (
                  <span className="text-gray-400 ml-2">
                    (Toàn bài: {totalAnsweredAllSections}/{totalQuestionsAllSections})
                  </span>
                )}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2.5 md:gap-3.5 shrink-0">
            {/* Bộ đếm thời gian cho Ca hiện tại */}
            <div className={`flex items-center gap-2 px-3 py-2 md:px-3.5 md:py-2 rounded-xl font-mono text-sm md:text-base font-black transition-all shadow-xs shrink-0 whitespace-nowrap ${
              timeLeft < 300 
                ? 'bg-rose-50 text-rose-600 border border-rose-300 animate-pulse ring-2 ring-rose-200' 
                : timeLeft < 600
                ? 'bg-amber-50 text-amber-700 border border-amber-300'
                : 'bg-indigo-50 text-indigo-900 border border-indigo-200'
            }`}>
              <Clock className="w-4 h-4 md:w-5 md:h-5 text-current shrink-0" />
              <span>{formatTime(timeLeft)}</span>
            </div>

            {/* Nút Chuyển Ca hoặc Nộp Bài */}
            {!isLastSection ? (
              <button
                onClick={() => setNextSectionModalOpen(true)}
                className="px-3.5 py-2 md:px-5 md:py-2.5 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white rounded-xl font-bold text-xs md:text-sm shadow-xs transition flex items-center gap-1.5 shrink-0 whitespace-nowrap"
              >
                <span>Xong Phần {currentSection.orderIndex}</span>
                <ChevronRight className="w-4 h-4 shrink-0" />
              </button>
            ) : (
              <button
                onClick={() => setConfirmSubmitOpen(true)}
                className="px-3.5 py-2 md:px-5 md:py-2.5 bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white rounded-xl font-bold text-xs md:text-sm shadow-xs transition flex items-center gap-1.5 shrink-0 whitespace-nowrap"
              >
                <Send className="w-4 h-4 shrink-0" />
                <span>Nộp toàn bộ bài</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* 2. Thân đề thi & Bảng điều hướng số câu */}
      <div className="max-w-7xl mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* CỘT TRÁI (9/12): ĐỀ THI CA HIỆN TẠI (PHONG CÁCH TỜ THI THẬT JLPT) */}
        <div className="lg:col-span-9 space-y-8">
          {/* Header Bảng đề thi của Ca */}
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-gray-300 shadow-sm relative overflow-hidden">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-gray-200 pb-5 mb-6">
              <div>
                <span className="text-xs font-black uppercase tracking-widest text-indigo-600 mb-1 block">
                  KỲ THI NĂNG LỰC TIẾNG NHẬT JLPT {takingData.jlptLevel}
                </span>
                <h2 className="text-xl md:text-2xl font-black text-gray-900">
                  {currentSection.sectionTitle}
                </h2>
              </div>
              <div className="flex items-center gap-3 text-xs font-bold text-gray-600 bg-slate-50 px-4 py-2.5 rounded-2xl border border-gray-200 self-start md:self-auto">
                <Clock className="w-4 h-4 text-indigo-600" />
                <span>Thời gian làm bài ca này: {currentSection.durationMinutes} phút</span>
              </div>
            </div>

            {/* Trình phát âm thanh phần nghe nếu là Ca 3 */}
            {currentSection.sessionType === 'LISTENING' && currentSection.audioUrl && (
              <div className="p-4 bg-indigo-50/80 rounded-2xl border border-indigo-200 flex items-center gap-4 mb-6">
                <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center shrink-0">
                  <Headphones className="w-5 h-5" />
                </div>
                <div className="grow">
                  <span className="text-xs font-bold text-indigo-900 block mb-1">
                    File Âm Thanh Toàn Bộ Ca Nghe (Nghe liên tục không ngắt)
                  </span>
                  <audio
                    ref={audioRef}
                    controls
                    src={currentSection.audioUrl}
                    className="w-full h-8"
                  />
                </div>
              </div>
            )}

            {/* Danh sách câu hỏi của Ca hiện tại */}
            <div className="space-y-8">
              {currentQuestions.map((q, idx) => {
                const isFlagged = flaggedQuestions.has(q.id)
                const selectedOptId = answers[q.id]
                // Hiển thị Mondai Header nếu là câu đầu tiên của Mondai đó
                const prevQ = idx > 0 ? currentQuestions[idx - 1] : null
                const isNewMondai = !prevQ || prevQ.mondaiCode !== q.mondaiCode

                return (
                  <div key={q.id} className="space-y-4">
                    {/* MONDAI HEADER (ĐỀ MỤC BÀI THI CHUẨN JLPT) */}
                    {isNewMondai && (
                      <div className="bg-slate-50 border-l-4 border-indigo-600 rounded-r-2xl p-4 md:p-5 border border-gray-200">
                        {(() => {
                          const mInfo = formatMondaiDisplay(q.mondaiCode, q.mondaiNameVi);
                          return (
                            <div className="flex items-center gap-2 mb-1">
                              <span className="px-3 py-1 rounded-lg bg-indigo-600 text-white text-xs font-black tracking-wide">
                                {mInfo.jp}
                              </span>
                              <h3 className="text-sm md:text-base font-extrabold text-gray-900">
                                {mInfo.vi}
                              </h3>
                            </div>
                          );
                        })()}
                        {q.instructionJp && (
                          <p className="text-xs md:text-sm text-gray-700 font-serif mt-1.5 leading-relaxed font-medium">
                            {cleanJapaneseText(q.instructionJp)}
                          </p>
                        )}
                        {q.instructionVi && (
                          <p className="text-xs text-gray-500 mt-0.5">
                            ({q.instructionVi})
                          </p>
                        )}
                      </div>
                    )}

                    {/* KHUNG CÂU HỎI */}
                    <div
                      id={`question-${q.id}`}
                      className={`p-6 rounded-2xl border transition-all ${
                        isFlagged 
                          ? 'border-amber-400 bg-amber-50/30 ring-1 ring-amber-400' 
                          : selectedOptId 
                          ? 'border-indigo-200 bg-white shadow-xs' 
                          : 'border-gray-200 bg-white hover:border-gray-300'
                      }`}
                    >
                      {/* Tiêu đề câu & Nút cờ */}
                      <div className="flex items-start justify-between gap-4 mb-4">
                        <div className="flex items-center gap-2.5">
                          <span className="w-8 h-8 rounded-xl bg-slate-900 text-white flex items-center justify-center font-black text-sm">
                            {q.questionNumber}
                          </span>
                          {q.starPosition && (
                            <span className="text-xs font-black text-amber-800 bg-amber-100 border border-amber-300 px-2.5 py-1 rounded-lg flex items-center gap-1">
                              <Star className="w-3.5 h-3.5 fill-current" />
                              <span>Vị trí dấu sao ★</span>
                            </span>
                          )}
                        </div>

                        <button
                          onClick={() => toggleFlagQuestion(q.id)}
                          className={`p-2 rounded-xl transition ${
                            isFlagged
                              ? 'bg-amber-100 text-amber-600'
                              : 'text-gray-400 hover:text-amber-500 hover:bg-gray-100'
                          }`}
                          title="Đánh dấu câu này để kiểm tra lại"
                        >
                          <Flag className={`w-4 h-4 ${isFlagged ? 'fill-current' : ''}`} />
                        </button>
                      </div>

                      {/* Bài đọc / Tờ rơi nếu có (Tự động gom nhóm cho nhiều câu hỏi liên tiếp) */}
                      {q.passage && (
                        (() => {
                          const isSamePassageAsPrev = prevQ && prevQ.passage && prevQ.passage.id === q.passage.id;
                          if (isSamePassageAsPrev) {
                            return (
                              <div className="mb-5 p-3 rounded-xl bg-indigo-50/60 border border-indigo-200/80 flex items-center justify-between text-xs text-indigo-900">
                                <div className="flex items-center gap-2">
                                  <span className="font-bold">📍 Câu hỏi liên kết:</span>
                                  <span>Sử dụng dữ liệu từ bài đọc phía trên để trả lời câu {q.questionNumber}.</span>
                                </div>
                                <a 
                                  href={`#question-${prevQ.id}`}
                                  className="text-indigo-600 hover:text-indigo-800 underline font-bold"
                                >
                                  Xem lại bài đọc
                                </a>
                              </div>
                            );
                          }

                          // Tìm xem có bao nhiêu câu hỏi tiếp theo dùng chung passage này
                          let lastRelatedQNum = q.questionNumber;
                          for (let i = idx + 1; i < currentQuestions.length; i++) {
                            if (currentQuestions[i].passage?.id === q.passage.id) {
                              lastRelatedQNum = currentQuestions[i].questionNumber;
                            } else {
                              break;
                            }
                          }
                          const isMultiQuestions = lastRelatedQNum > q.questionNumber;

                          return (
                            <div className="mb-6 p-6 rounded-2xl bg-slate-50 border-2 border-indigo-100 shadow-xs">
                              <div className="flex items-center justify-between gap-3 border-b border-indigo-100 pb-3 mb-4">
                                <div className="flex items-center gap-2">
                                  <span className="px-2.5 py-1 rounded-md bg-indigo-600 text-white text-xs font-black uppercase tracking-wider">
                                    BÀI ĐỌC HIỂU
                                  </span>
                                  <h4 className="text-sm md:text-base font-bold text-gray-900">
                                    {q.passage.title || 'Dữ liệu đọc hiểu'}
                                  </h4>
                                </div>
                                {isMultiQuestions && (
                                  <span className="text-xs font-bold px-2.5 py-1 bg-amber-100 text-amber-900 rounded-lg border border-amber-200">
                                    Dùng cho Câu {q.questionNumber} ~ Câu {lastRelatedQNum}
                                  </span>
                                )}
                              </div>

                              {q.passage.imageUrl && (
                                <div className="relative mb-4 group/img">
                                  <img
                                    src={q.passage.imageUrl}
                                    alt="Tờ rơi quảng cáo"
                                    className="w-full max-h-96 object-contain rounded-xl bg-white border border-gray-200"
                                  />
                                  <button
                                    onClick={() => setLightboxImage(q.passage!.imageUrl!)}
                                    className="absolute bottom-3 right-3 px-3 py-1.5 rounded-lg bg-black/70 hover:bg-black/90 text-white text-xs font-bold backdrop-blur-xs flex items-center gap-1.5 transition shadow-lg"
                                  >
                                    <ZoomIn className="w-4 h-4" />
                                    <span>Phóng to xem rõ</span>
                                  </button>
                                </div>
                              )}

                              {q.passage.contentJp && (
                                q.passage.contentJp.includes('<table') || q.passage.contentJp.includes('<div') ? (
                                  <div 
                                    className="text-gray-900 leading-relaxed font-sans"
                                    dangerouslySetInnerHTML={{ __html: cleanJapaneseText(q.passage.contentJp) }}
                                  />
                                ) : (
                                  <div className="font-serif text-base md:text-lg leading-loose text-gray-900 tracking-normal p-2 bg-white rounded-xl border border-gray-200 shadow-2xs">
                                    {cleanJapaneseText(q.passage.contentJp)}
                                  </div>
                                )
                              )}
                            </div>
                          );
                        })()
                      )}

                      {/* Tranh minh họa nghe nếu có */}
                      {q.imageUrl && (
                        <div className="mb-4 text-center">
                          <img
                            src={q.imageUrl}
                            alt="Tranh tình huống"
                            className="max-h-72 mx-auto rounded-xl border border-gray-200 object-contain shadow-xs"
                          />
                        </div>
                      )}

                      {/* NỘI DUNG CÂU HỎI (Render HTML chuẩn có gạch chân <u>...) */}
                      <div 
                        className="text-base md:text-lg font-medium text-gray-900 mb-5 font-serif leading-relaxed [&_u]:underline [&_u]:underline-offset-8 [&_u]:decoration-2 [&_u]:font-bold [&_u]:text-indigo-950"
                        dangerouslySetInnerHTML={{ __html: cleanQuestionPrompt(q.questionText, q.instructionJp) }}
                      />

                      {/* GỢI Ý CHO CÂU DẤU SAO (★) - CHỈ CẦN CHỌN ĐÁP ÁN CHO VỊ TRÍ SAO */}
                      {q.starPosition && (
                        <div className="mb-4 p-3.5 bg-amber-50 rounded-xl border border-amber-300 text-xs md:text-sm text-amber-950 flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg bg-amber-500 text-white flex items-center justify-center font-black shrink-0">
                            ★
                          </div>
                          <div>
                            <span className="font-bold block text-amber-900">Quy cách làm bài sắp xếp dấu sao (★):</span>
                            <span>Sắp xếp 4 phương án vào chỗ trống. <strong>Bạn chỉ cần chọn đáp án (1, 2, 3 hoặc 4) nằm ở vị trí dấu sao ★</strong>.</span>
                          </div>
                        </div>
                      )}

                      {/* CÁC PHƯƠNG ÁN LỰA CHỌN */}
                      {q.hasVisualOptions ? (
                        /* Giao diện Lưới tranh 2x2 cho câu nghe chọn tranh */
                        <div className="grid grid-cols-2 gap-4">
                          {q.options.map(opt => {
                            const isSelected = selectedOptId === opt.id
                            return (
                              <button
                                key={opt.id}
                                type="button"
                                onClick={() => handleSelectOption(q.id, opt.id)}
                                className={`p-3 rounded-2xl border transition-all text-left ${
                                  isSelected
                                    ? 'border-indigo-600 bg-indigo-50/60 ring-2 ring-indigo-600 shadow-md'
                                    : 'border-gray-200 hover:border-gray-300 bg-white'
                                }`}
                              >
                                <div className="flex items-center gap-2 mb-2 font-bold text-sm text-gray-700">
                                  <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                                    isSelected ? 'bg-indigo-600 text-white' : 'bg-gray-200 text-gray-600'
                                  }`}>
                                    {opt.optionKey}
                                  </span>
                                  <span>Tranh {opt.optionKey}</span>
                                </div>
                                {opt.optionImageUrl && (
                                  <img
                                    src={opt.optionImageUrl}
                                    alt={`Đáp án ${opt.optionKey}`}
                                    className="w-full h-40 object-cover rounded-xl border border-gray-100"
                                  />
                                )}
                              </button>
                            )
                          })}
                        </div>
                      ) : (
                        /* Giao diện 4 lựa chọn (Áp dụng cho cả câu thường & câu sao ★) */
                        <div className="space-y-2.5">
                          {q.options.map(opt => {
                            const isSelected = selectedOptId === opt.id
                            return (
                              <button
                                key={opt.id}
                                type="button"
                                onClick={() => handleSelectOption(q.id, opt.id)}
                                className={`w-full p-3.5 rounded-xl border text-left flex items-center justify-between transition-all ${
                                  isSelected
                                    ? 'border-indigo-600 bg-indigo-50/80 text-indigo-950 font-bold ring-2 ring-indigo-500/20 shadow-xs'
                                    : 'border-gray-200 hover:border-gray-300 hover:bg-slate-50/80 bg-white text-gray-800 font-medium'
                                }`}
                              >
                                <div className="flex items-center gap-3">
                                  <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 transition ${
                                    isSelected
                                      ? 'bg-indigo-600 text-white'
                                      : 'bg-gray-100 text-gray-700 border border-gray-300'
                                  }`}>
                                    {opt.optionKey}
                                  </span>
                                  <span className="text-sm md:text-base font-serif">{cleanJapaneseText(opt.optionText)}</span>
                                </div>

                                {isSelected && (
                                  <span className="text-xs font-bold text-indigo-600 flex items-center gap-1 shrink-0">
                                    <CheckCircle2 className="w-4 h-4" />
                                    <span>{q.starPosition ? 'Đáp án cho ★' : 'Đã chọn'}</span>
                                  </span>
                                )}
                              </button>
                            )
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Nút cuối trang: Chuyển ca hoặc nộp bài */}
            <div className="pt-8 border-t border-gray-200 mt-10 flex justify-end">
              {!isLastSection ? (
                <button
                  type="button"
                  onClick={() => setNextSectionModalOpen(true)}
                  className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold text-sm shadow-md transition flex items-center gap-2"
                >
                  <span>Hoàn thành Ca {currentSection.orderIndex} & Tiếp tục Ca {currentSection.orderIndex + 1}</span>
                  <ChevronRight className="w-4 h-4" />
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => setConfirmSubmitOpen(true)}
                  className="px-8 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-sm shadow-md transition flex items-center gap-2"
                >
                  <Send className="w-4 h-4" />
                  <span>Nộp bài & Xem kết quả</span>
                </button>
              )}
            </div>
          </div>
        </div>

        {/* CỘT PHẢI (3/12): BẢNG SỐ CÂU CỦA CA HIỆN TẠI (STICKY PALETTE) */}
        <div className="lg:col-span-3">
          <div className="sticky top-[88px] bg-white rounded-3xl p-5 border border-gray-200 shadow-sm space-y-5">
            <div>
              <div className="flex items-center justify-between mb-1">
                <h3 className="font-bold text-sm text-gray-900">Bảng số câu - Ca {currentSection.orderIndex}</h3>
                <span className="text-xs font-bold text-indigo-600">
                  {currentAnsweredCount}/{currentQuestions.length}
                </span>
              </div>
              <p className="text-xs text-gray-400">
                Click số câu để chuyển nhanh đến câu đó
              </p>
            </div>

            {/* Chú thích màu sắc */}
            <div className="grid grid-cols-3 gap-1.5 text-xs font-semibold text-gray-500 border-y border-gray-100 py-3">
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-md bg-emerald-500 shrink-0" />
                <span>Đã làm</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-md bg-white border border-gray-300 shrink-0" />
                <span>Chưa làm</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-md bg-amber-400 shrink-0" />
                <span>Gắn cờ</span>
              </div>
            </div>

            {/* Lưới các số câu của Ca hiện tại */}
            <div className="grid grid-cols-4 gap-2 max-h-[45vh] overflow-y-auto pr-1">
              {currentQuestions.map(q => {
                const isAnswered = !!answers[q.id]
                const isFlagged = flaggedQuestions.has(q.id)

                return (
                  <button
                    key={q.id}
                    onClick={() => scrollToQuestion(q.id)}
                    className={`h-9 rounded-xl font-bold text-xs transition-all flex items-center justify-center ${
                      isFlagged
                        ? 'bg-amber-400 text-white shadow-xs font-black'
                        : isAnswered
                        ? 'bg-emerald-500 text-white shadow-xs'
                        : 'bg-white border border-gray-300 text-gray-700 hover:border-indigo-400 hover:text-indigo-600'
                    }`}
                  >
                    {q.questionNumber}
                  </button>
                )
              })}
            </div>

            {/* Nút hành động */}
            {!isLastSection ? (
              <button
                onClick={() => setNextSectionModalOpen(true)}
                className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white rounded-xl font-bold text-sm shadow-xs transition flex items-center justify-center gap-2"
              >
                <span>Xong Ca {currentSection.orderIndex}</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                onClick={() => setConfirmSubmitOpen(true)}
                className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 active:scale-98 text-white rounded-xl font-bold text-sm shadow-xs transition flex items-center justify-center gap-2"
              >
                <Send className="w-4 h-4" />
                <span>Nộp bài ngay</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Modal Chuyển Ca Thi (Next Section Modal) */}
      {nextSectionModalOpen && takingData && currentSectionIndex < takingData.sections.length - 1 && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-indigo-100 text-indigo-600 flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <h3 className="text-xl font-black text-gray-900 text-center mb-1">
              Hoàn thành Ca {currentSection.orderIndex}!
            </h3>
            <p className="text-xs text-gray-500 text-center mb-5">
              Bạn đã hoàn thành thời gian làm bài của {currentSection.sectionTitle}.
            </p>

            <div className="bg-slate-50 p-4 rounded-2xl border border-gray-200 text-xs space-y-2.5 mb-6">
              <div className="font-bold text-gray-700 uppercase tracking-wider text-[11px]">
                Ca thi tiếp theo:
              </div>
              <div className="text-sm font-black text-indigo-600">
                {takingData.sections[currentSectionIndex + 1]?.sectionTitle}
              </div>
              <div className="flex justify-between text-gray-600 pt-2 border-t border-gray-200">
                <span>Thời gian ca tiếp theo:</span>
                <span className="font-bold text-gray-900">
                  {takingData.sections[currentSectionIndex + 1]?.durationMinutes} phút
                </span>
              </div>
              <div className="flex justify-between text-gray-600">
                <span>Số câu hỏi:</span>
                <span className="font-bold text-gray-900">
                  {takingData.sections[currentSectionIndex + 1]?.questions.length} câu
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setNextSectionModalOpen(false)}
                className="py-3 rounded-xl font-bold text-sm text-gray-600 bg-gray-100 hover:bg-gray-200 transition"
              >
                Kiểm tra lại
              </button>
              <button
                type="button"
                onClick={handleProceedToNextSection}
                className="py-3 rounded-xl font-bold text-sm text-white bg-indigo-600 hover:bg-indigo-700 transition flex items-center justify-center gap-1.5"
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
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center mx-auto mb-4">
              <AlertCircle className="w-6 h-6" />
            </div>

            <h3 className="text-xl font-black text-gray-900 text-center mb-2">
              Bạn có chắc chắn muốn nộp bài?
            </h3>
            <p className="text-xs text-gray-500 text-center mb-5">
              Bài thi sẽ được kết thúc và chấm điểm tự động theo chuẩn Scaled Score JLPT.
            </p>

            <div className="bg-slate-50 p-4 rounded-2xl text-xs font-semibold text-gray-600 space-y-2 mb-6 border border-gray-200">
              <div className="flex justify-between">
                <span>Tổng số câu đã làm:</span>
                <span className="font-bold text-emerald-600">
                  {totalAnsweredAllSections} / {totalQuestionsAllSections} câu
                </span>
              </div>
              <div className="flex justify-between">
                <span>Số câu chưa làm:</span>
                <span className="font-bold text-rose-500">
                  {totalQuestionsAllSections - totalAnsweredAllSections} câu
                </span>
              </div>
              <div className="flex justify-between">
                <span>Thời gian còn lại của ca:</span>
                <span className="font-bold text-indigo-600">{formatTime(timeLeft)}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setConfirmSubmitOpen(false)}
                className="py-3 rounded-xl font-bold text-sm text-gray-600 bg-gray-100 hover:bg-gray-200 transition"
              >
                Làm tiếp
              </button>
              <button
                type="button"
                disabled={submitting}
                onClick={() => {
                  setConfirmSubmitOpen(false)
                  handleSubmitExam()
                }}
                className="py-3 rounded-xl font-bold text-sm text-white bg-emerald-600 hover:bg-emerald-700 transition flex items-center justify-center gap-2"
              >
                {submitting ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <span>Xác nhận nộp</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox Phóng To Ảnh Tờ Rơi */}
      {lightboxImage && (
        <div 
          className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4 cursor-zoom-out"
          onClick={() => setLightboxImage(null)}
        >
          <div className="relative max-w-4xl max-h-[90vh]">
            <button
              onClick={() => setLightboxImage(null)}
              className="absolute -top-12 right-0 p-2 text-white hover:text-gray-300 transition"
            >
              <X className="w-8 h-8" />
            </button>
            <img
              src={lightboxImage}
              alt="Phóng to tờ rơi"
              className="max-h-[85vh] w-auto mx-auto rounded-2xl shadow-2xl"
            />
          </div>
        </div>
      )}
    </div>
  )
}
export default ExamRoomPage
