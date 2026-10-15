import React, { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { examService } from '../../services/exam.service'
import type { ExamReviewData, ReviewQuestion } from '../../types/exam.types'
import { cleanJapaneseText, cleanQuestionPrompt, formatMondaiDisplay, formatSectionName } from '../../utils/format'
import {
  ArrowLeft,
  CheckCircle2,
  XCircle,
  Play,
  Pause,
  Volume2,
  Maximize2,
  X,
  HelpCircle,
  Eye,
  EyeOff,
  Star,
} from 'lucide-react'

export const ExamReviewPage: React.FC = () => {
  const { attemptId } = useParams<{ attemptId: string }>()
  const navigate = useNavigate()

  const [reviewData, setReviewData] = useState<ExamReviewData | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  // Tab section
  const [activeSectionIndex, setActiveSectionIndex] = useState<number>(0)

  // Modal zoom image
  const [zoomImageUrl, setZoomImageUrl] = useState<string | null>(null)

  // Toggle Scripts/Transcript theo questionId: Record<number, { showJp: boolean; showVi: boolean }>
  const [transcriptState, setTranscriptState] = useState<Record<number, { showJp: boolean; showVi: boolean }>>({})

  // Audio control for listening questions
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const [playingQuestionId, setPlayingQuestionId] = useState<number | null>(null)
  const currentIntervalRef = useRef<any>(null)

  useEffect(() => {
    if (!attemptId) return
    const fetchReview = async () => {
      try {
        setLoading(true)
        const data = await examService.getExamReview(Number(attemptId))
        setReviewData(data)
      } catch (err: any) {
        setError(err.response?.data?.message || 'Không thể tải dữ liệu xem lại bài thi')
      } finally {
        setLoading(false)
      }
    }
    fetchReview()

    return () => {
      if (currentIntervalRef.current) clearInterval(currentIntervalRef.current)
    }
  }, [attemptId])

  const toggleTranscriptJp = (qId: number) => {
    setTranscriptState((prev) => ({
      ...prev,
      [qId]: {
        showJp: !prev[qId]?.showJp,
        showVi: prev[qId]?.showVi || false,
      },
    }))
  }

  const toggleTranscriptVi = (qId: number) => {
    setTranscriptState((prev) => ({
      ...prev,
      [qId]: {
        showJp: prev[qId]?.showJp || false,
        showVi: !prev[qId]?.showVi,
      },
    }))
  }

  const handlePlayQuestionAudio = (question: ReviewQuestion, audioUrl?: string) => {
    if (!audioUrl || question.audioStartTime === undefined) return

    if (playingQuestionId === question.id) {
      if (audioRef.current) {
        audioRef.current.pause()
      }
      setPlayingQuestionId(null)
      if (currentIntervalRef.current) clearInterval(currentIntervalRef.current)
      return
    }

    if (!audioRef.current) {
      audioRef.current = new Audio(audioUrl)
    } else {
      if (audioRef.current.src !== audioUrl) {
        audioRef.current.src = audioUrl
      }
    }

    const audio = audioRef.current
    audio.currentTime = question.audioStartTime
    audio.play()
    setPlayingQuestionId(question.id)

    if (currentIntervalRef.current) clearInterval(currentIntervalRef.current)

    currentIntervalRef.current = setInterval(() => {
      if (question.audioEndTime && audio.currentTime >= question.audioEndTime) {
        audio.pause()
        setPlayingQuestionId(null)
        clearInterval(currentIntervalRef.current)
      }
    }, 200)

    audio.onended = () => {
      setPlayingQuestionId(null)
      if (currentIntervalRef.current) clearInterval(currentIntervalRef.current)
    }
  }

  const scrollToQuestion = (qNumber: number) => {
    const el = document.getElementById(`review-q-${qNumber}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-600 dark:text-slate-400 font-medium">Đang tải chi tiết bài làm...</p>
        </div>
      </div>
    )
  }

  if (error || !reviewData) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8 max-w-md w-full text-center">
          <XCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-slate-800 dark:text-white mb-2">Lỗi tải dữ liệu</h2>
          <p className="text-slate-600 dark:text-slate-400 text-sm mb-6">{error || 'Không tìm thấy dữ liệu xem lại'}</p>
          <button
            onClick={() => navigate('/exam-history')}
            className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-xl transition"
          >
            Quay lại lịch sử
          </button>
        </div>
      </div>
    )
  }

  const activeSection = reviewData.sections[activeSectionIndex] || reviewData.sections[0]
  const allQuestions = activeSection ? activeSection.questions : []

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-slate-950 flex flex-col font-sans">
      {/* Top sticky review bar */}
      {/* Top sticky review bar */}
      <header className="sticky top-0 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 shadow-xs">
        {/* Hàng 1: Tiêu đề đề thi & Điểm số - Thiết kế gọn gàng trên 1 hàng */}
        <div className="max-w-7xl mx-auto px-4 lg:px-6 py-2.5 flex items-center justify-between gap-3">
          <div className="flex-1 min-w-0 flex items-center gap-2.5">
            <button
              onClick={() => navigate(`/exams/result/${reviewData.attemptId}`)}
              className="p-1.5 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 transition shrink-0"
              title="Quay lại phiếu điểm"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <span className="px-2 py-0.5 bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 rounded text-xs font-black shrink-0">
              {reviewData.jlptLevel}
            </span>
            <span
              className={`text-xs font-bold px-2 py-0.5 rounded shrink-0 ${
                reviewData.passed
                  ? 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400'
                  : 'bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-400'
              }`}
            >
              {reviewData.passed ? 'ĐẠT (合格)' : 'KHÔNG ĐẠT (不合格)'}
            </span>
            <h1
              className="text-sm md:text-base font-bold text-slate-800 dark:text-white truncate"
              title={reviewData.examTitle}
            >
              {reviewData.examTitle}
            </h1>
          </div>

          <div className="flex items-center gap-2.5 shrink-0">
            <div className="hidden sm:flex items-center gap-2.5 text-xs font-semibold bg-slate-100/80 dark:bg-slate-800/80 px-3 py-1 rounded-lg">
              <span className="text-slate-600 dark:text-slate-300">
                Điểm:{' '}
                <strong className="text-indigo-600 dark:text-indigo-400 font-extrabold text-sm">
                  {reviewData.totalScaledScore ?? 0}
                </strong>
                <span className="text-slate-400 font-normal">/{reviewData.totalScaledMaxScore || 180}</span>
              </span>
              <span className="text-slate-300 dark:text-slate-600">•</span>
              <span className="text-slate-600 dark:text-slate-300">
                Đúng:{' '}
                <strong className="text-emerald-600 dark:text-emerald-400 font-bold">
                  {reviewData.totalRawScore ?? 0} câu
                </strong>
              </span>
            </div>
            <button
              onClick={() => navigate('/exam-history')}
              className="text-xs font-semibold px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 transition shrink-0"
            >
              Lịch sử thi
            </button>
          </div>
        </div>

        {/* Hàng 2: Thanh chuyển Phần thi (Section Tabs) siêu gọn gàng */}
        {reviewData.sections.length > 1 && (
          <div className="border-t border-slate-100 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-900/50 px-4 lg:px-6 py-1.5">
            <div className="max-w-7xl mx-auto flex items-center gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mr-1 shrink-0">
                Phần thi:
              </span>
              {reviewData.sections.map((sec, idx) => {
                let compactLabel = `Phần ${idx + 1}`
                if (sec.sessionType === 'VOCABULARY') compactLabel = 'Phần 1: Từ vựng (文字・語彙)'
                else if (sec.sessionType === 'GRAMMAR_READING') compactLabel = 'Phần 2: Ngữ pháp & Đọc hiểu (文法・読解)'
                else if (sec.sessionType === 'LISTENING') compactLabel = 'Phần 3: Nghe hiểu (聴解)'
                else if (sec.sessionType === 'KNOWLEDGE_READING') compactLabel = 'Phần 1: Từ vựng - Ngữ pháp - Đọc'
                else if (sec.sectionTitle) compactLabel = sec.sectionTitle

                return (
                  <button
                    key={sec.id}
                    onClick={() => setActiveSectionIndex(idx)}
                    className={`px-3 py-1 rounded-lg text-xs font-bold whitespace-nowrap shrink-0 transition flex items-center gap-1.5 ${
                      activeSectionIndex === idx
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200/80 dark:border-slate-700'
                    }`}
                  >
                    <span>{compactLabel}</span>
                    <span
                      className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                        activeSectionIndex === idx
                          ? 'bg-indigo-700 text-white'
                          : 'bg-slate-100 dark:bg-slate-700 text-slate-500'
                      }`}
                    >
                      {sec.questions.length} câu
                    </span>
                  </button>
                )
              })}
            </div>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <div className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-6 grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Continuous Paper-Style Questions (3 columns) */}
        <div className="lg:col-span-3 space-y-6">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200/80 dark:border-slate-800 p-4 md:p-6 mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-slate-800 dark:text-white">
                {formatSectionName(activeSection?.sessionType, activeSectionIndex + 1, activeSection?.sectionTitle)}
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Xem lại câu hỏi, đối chiếu đáp án của bạn và nghiên cứu lời giải chi tiết.
              </p>
            </div>
            {activeSection?.audioUrl && (
              <div className="flex items-center gap-2 text-xs font-medium text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-3 py-1.5 rounded-lg border border-indigo-200 dark:border-indigo-800">
                <Volume2 className="w-4 h-4" />
                <span>Có file âm thanh phân đoạn</span>
              </div>
            )}
          </div>

          {/* List of Questions in continuous scroll */}
          {allQuestions.map((q, idx) => {
            const isCorrect = q.studentAnswer?.correct ?? false
            const isAnswered = q.studentAnswer?.selectedOptionId != null || (q.studentAnswer?.orderedOptionIds && q.studentAnswer.orderedOptionIds.length > 0)
            const showJp = transcriptState[q.id]?.showJp
            const showVi = transcriptState[q.id]?.showVi
            const prevQ = idx > 0 ? allQuestions[idx - 1] : null
            const mondaiInfo = formatMondaiDisplay(q.mondaiCode, q.mondaiNameVi)
            const isNewMondai = !prevQ || prevQ.mondaiCode !== q.mondaiCode

            return (
              <div key={q.id} className="space-y-4">
                {/* MONDAI HEADER: Chỉ hiển thị 1 lần duy nhất ở đầu mỗi dạng bài Mondai */}
                {isNewMondai && (
                  <div className="bg-white dark:bg-slate-900 border-l-4 border-indigo-600 rounded-2xl p-4 md:p-5 border border-slate-200 dark:border-slate-800 shadow-xs">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="px-3 py-1 rounded-lg bg-indigo-600 text-white text-xs font-black tracking-wide">
                        {mondaiInfo.jp}
                      </span>
                      <h3 className="text-sm md:text-base font-extrabold text-slate-900 dark:text-white">
                        {mondaiInfo.vi}
                      </h3>
                    </div>
                    {q.instructionJp && (
                      <p className="text-xs md:text-sm text-slate-700 dark:text-slate-300 font-serif mt-1.5 leading-relaxed font-medium">
                        {cleanJapaneseText(q.instructionJp)}
                      </p>
                    )}
                    {q.instructionVi && (
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        ({q.instructionVi})
                      </p>
                    )}
                  </div>
                )}

                {/* KHUNG CÂU HỎI */}
                <div
                  id={`review-q-${q.questionNumber}`}
                  className={`bg-white dark:bg-slate-900 rounded-2xl shadow-sm border transition-all ${
                    !isAnswered
                      ? 'border-slate-300 dark:border-slate-700'
                      : isCorrect
                      ? 'border-emerald-300/80 dark:border-emerald-900/60'
                      : 'border-rose-300/80 dark:border-rose-900/60'
                  } p-5 md:p-7 relative`}
                >
                  {/* Status Ribbon/Badge */}
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2.5">
                      <span className="w-8 h-8 rounded-xl bg-slate-900 text-white font-extrabold flex items-center justify-center text-sm shadow-xs">
                        {q.questionNumber}
                      </span>
                      {q.starPosition && (
                        <span className="text-xs font-black text-amber-800 dark:text-amber-300 bg-amber-100 dark:bg-amber-950/60 border border-amber-300 dark:border-amber-800 px-2.5 py-1 rounded-lg flex items-center gap-1">
                          <Star className="w-3.5 h-3.5 fill-current" />
                          <span>Vị trí dấu sao ★</span>
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      {!isAnswered ? (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                          Chưa trả lời
                        </span>
                      ) : isCorrect ? (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300">
                          <CheckCircle2 className="w-4 h-4" />
                          Chính xác
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300">
                          <XCircle className="w-4 h-4" />
                          Chưa chính xác
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Passage for Reading/Poster (Tự động gom nhóm) */}
                  {q.passage && (
                  (() => {
                    const isSamePassageAsPrev = prevQ && prevQ.passage && prevQ.passage.id === q.passage.id;
                    if (isSamePassageAsPrev) {
                      return (
                        <div className="mb-5 p-3 rounded-xl bg-indigo-50/70 dark:bg-indigo-950/30 border border-indigo-200 dark:border-indigo-800/60 flex items-center justify-between text-xs text-indigo-900 dark:text-indigo-300">
                          <div className="flex items-center gap-2">
                            <span className="font-bold">📍 Câu hỏi liên kết:</span>
                            <span>Sử dụng bài đọc ở câu trên để trả lời câu {q.questionNumber}.</span>
                          </div>
                          <a 
                            href={`#review-q-${prevQ.questionNumber}`}
                            className="text-indigo-600 dark:text-indigo-400 hover:underline font-bold"
                          >
                            Xem lại bài đọc
                          </a>
                        </div>
                      );
                    }

                    return (
                      <div className="mb-6 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl p-5">
                        <div className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400 mb-3 flex items-center justify-between">
                          <span>Bài đọc / Dữ liệu tham chiếu: {q.passage.title}</span>
                        </div>

                        {q.passage.imageUrl && (
                          <div className="mb-4 relative group inline-block">
                            <img
                              src={q.passage.imageUrl}
                              alt={q.passage.title}
                              className="max-h-96 rounded-lg border border-slate-300 dark:border-slate-600 shadow-sm cursor-pointer"
                              onClick={() => setZoomImageUrl(q.passage?.imageUrl || null)}
                            />
                            <button
                              onClick={() => setZoomImageUrl(q.passage?.imageUrl || null)}
                              className="absolute bottom-3 right-3 bg-slate-900/80 hover:bg-slate-900 text-white p-2 rounded-lg text-xs flex items-center gap-1 shadow-md transition"
                            >
                              <Maximize2 className="w-3.5 h-3.5" />
                              Phóng to xem rõ
                            </button>
                          </div>
                        )}

                        {q.passage.contentJp && (
                          q.passage.contentJp.includes('<table') || q.passage.contentJp.includes('<div') ? (
                            <div 
                              className="text-slate-800 dark:text-slate-200 leading-relaxed font-sans"
                              dangerouslySetInnerHTML={{ __html: cleanJapaneseText(q.passage.contentJp) }}
                            />
                          ) : (
                            <div className="text-base text-slate-800 dark:text-slate-200 whitespace-pre-wrap leading-relaxed font-serif p-3 bg-white dark:bg-slate-900/80 rounded-lg border border-slate-200 dark:border-slate-700">
                              {cleanJapaneseText(q.passage.contentJp)}
                            </div>
                          )
                        )}
                      </div>
                    );
                  })()
                )}

                {/* Listening Audio Player for this question */}
                {q.audioStartTime !== undefined && activeSection.audioUrl && (
                  <div className="mb-5 p-3.5 bg-indigo-50/70 dark:bg-indigo-950/30 border border-indigo-200 dark:border-indigo-800/60 rounded-xl flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handlePlayQuestionAudio(q, activeSection.audioUrl)}
                        className={`w-10 h-10 rounded-xl flex items-center justify-center font-semibold transition shadow-sm ${
                          playingQuestionId === q.id
                            ? 'bg-rose-600 hover:bg-rose-700 text-white'
                            : 'bg-indigo-600 hover:bg-indigo-700 text-white'
                        }`}
                      >
                        {playingQuestionId === q.id ? (
                          <Pause className="w-5 h-5" />
                        ) : (
                          <Play className="w-5 h-5 ml-0.5" />
                        )}
                      </button>
                      <div>
                        <div className="text-xs font-bold text-indigo-900 dark:text-indigo-200 flex items-center gap-1.5">
                          <Volume2 className="w-4 h-4 text-indigo-600" />
                          <span>Đoạn ghi âm câu {q.questionNumber}</span>
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400">
                          {q.audioStartTime}s - {q.audioEndTime ?? '?'}s
                        </div>
                      </div>
                    </div>

                    {/* Transcript toggle buttons */}
                    <div className="flex items-center gap-2">
                      {q.transcriptJp && (
                        <button
                          onClick={() => toggleTranscriptJp(q.id)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 border transition ${
                            showJp
                              ? 'bg-indigo-600 text-white border-indigo-600'
                              : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-300 dark:border-slate-600 hover:bg-slate-50'
                          }`}
                        >
                          {showJp ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                          Transcript JP
                        </button>
                      )}
                      {q.transcriptVi && (
                        <button
                          onClick={() => toggleTranscriptVi(q.id)}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 border transition ${
                            showVi
                              ? 'bg-teal-600 text-white border-teal-600'
                              : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-300 dark:border-slate-600 hover:bg-slate-50'
                          }`}
                        >
                          {showVi ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                          Dịch nghĩa VI
                        </button>
                      )}
                    </div>
                  </div>
                )}

                {/* Transcript JP Dropdown */}
                {showJp && q.transcriptJp && (
                  <div className="mb-4 p-4 bg-amber-50/60 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/50 rounded-xl">
                    <div className="text-xs font-bold text-amber-900 dark:text-amber-300 mb-1">
                      Kịch bản hội thoại tiếng Nhật (Transcript JP):
                    </div>
                    <div className="text-sm text-slate-800 dark:text-slate-200 whitespace-pre-wrap leading-relaxed">
                      {cleanJapaneseText(q.transcriptJp)}
                    </div>
                  </div>
                )}

                {/* Transcript VI Dropdown */}
                {showVi && q.transcriptVi && (
                  <div className="mb-4 p-4 bg-teal-50/60 dark:bg-teal-950/20 border border-teal-200 dark:border-teal-900/50 rounded-xl">
                    <div className="text-xs font-bold text-teal-900 dark:text-teal-300 mb-1">
                      Bản dịch hội thoại tiếng Việt (Transcript VI):
                    </div>
                    <div className="text-sm text-slate-800 dark:text-slate-200 whitespace-pre-wrap leading-relaxed">
                      {q.transcriptVi}
                    </div>
                  </div>
                )}

                {/* Question Image (minh họa tình huống) */}
                {q.imageUrl && (
                  <div className="mb-4">
                    <img
                      src={q.imageUrl}
                      alt={`Minh họa câu ${q.questionNumber}`}
                      className="max-h-64 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm"
                    />
                  </div>
                )}

                {/* Question Text */}
                <div 
                  className="text-base md:text-lg font-medium text-slate-900 dark:text-slate-100 mb-5 leading-relaxed [&_u]:underline [&_u]:underline-offset-4 [&_u]:decoration-2 [&_u]:font-bold"
                  dangerouslySetInnerHTML={{ __html: cleanQuestionPrompt(q.questionText, q.instructionJp) }}
                />

                {/* Options List */}
                <div
                  className={`grid gap-3 ${
                    q.hasVisualOptions ? 'grid-cols-1 sm:grid-cols-2' : 'grid-cols-1'
                  }`}
                >
                  {q.options.map((opt) => {
                    const isUserChoice = q.studentAnswer?.selectedOptionId === opt.id
                    const isCorrectAnswer = opt.correct

                    let borderClass = 'border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800'
                    let textClass = 'text-slate-700 dark:text-slate-300'
                    let badgeNode = null

                    if (isCorrectAnswer) {
                      borderClass = 'border-emerald-500 bg-emerald-50/60 dark:bg-emerald-950/30 ring-2 ring-emerald-500/30'
                      textClass = 'text-emerald-900 dark:text-emerald-200 font-semibold'
                      badgeNode = (
                        <span className="ml-auto flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-100 dark:bg-emerald-900/50 px-2.5 py-1 rounded-md">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Đáp án đúng
                        </span>
                      )
                    } else if (isUserChoice && !isCorrectAnswer) {
                      borderClass = 'border-rose-400 bg-rose-50/60 dark:bg-rose-950/30 ring-2 ring-rose-400/30'
                      textClass = 'text-rose-900 dark:text-rose-200 font-medium'
                      badgeNode = (
                        <span className="ml-auto flex items-center gap-1 text-xs font-bold text-rose-600 dark:text-rose-400 bg-rose-100 dark:bg-rose-900/50 px-2.5 py-1 rounded-md">
                          <XCircle className="w-3.5 h-3.5" />
                          Lựa chọn của bạn
                        </span>
                      )
                    }

                    return (
                      <div
                        key={opt.id}
                        className={`p-3.5 rounded-xl border flex items-center gap-3 transition ${borderClass}`}
                      >
                        <span
                          className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold ${
                            isCorrectAnswer
                              ? 'bg-emerald-600 text-white'
                              : isUserChoice
                              ? 'bg-rose-600 text-white'
                              : 'bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          {opt.optionKey}
                        </span>

                        {opt.optionImageUrl ? (
                          <div className="flex-1">
                            <img
                              src={opt.optionImageUrl}
                              alt={`Đáp án ${opt.optionKey}`}
                              className="max-h-36 rounded border border-slate-200 dark:border-slate-700"
                            />
                          </div>
                        ) : (
                          <span className={`text-sm md:text-base ${textClass}`}>
                            {cleanJapaneseText(opt.optionText)}
                          </span>
                        )}

                        {badgeNode}
                      </div>
                    )
                  })}
                </div>

                {/* Star Position question re-ordering analysis */}
                {q.starPosition && q.studentAnswer?.orderedOptionIds && (
                  <div className="mt-4 p-3 bg-slate-50 dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 text-xs">
                    <span className="font-bold text-slate-700 dark:text-slate-300">
                      Thứ tự bạn đã sắp xếp:
                    </span>{' '}
                    {q.studentAnswer.orderedOptionIds.map((optId, idx) => {
                      const opt = q.options.find((o) => o.id === optId)
                      const isStarIdx = idx + 1 === q.starPosition
                      return (
                        <span
                          key={optId}
                          className={`inline-block mx-1 px-2 py-0.5 rounded font-medium ${
                            isStarIdx
                              ? 'bg-amber-100 dark:bg-amber-900/50 text-amber-800 dark:text-amber-300 border border-amber-300'
                              : 'bg-white dark:bg-slate-700 text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          {idx + 1}. {opt?.optionText} {isStarIdx ? ' (★ Vị trí sao)' : ''}
                        </span>
                      )
                    })}
                  </div>
                )}

                {/* Explanation Section */}
                {q.explanationVi && (
                  <div className="mt-5 p-4 bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/40 rounded-xl">
                    <div className="text-xs font-bold text-indigo-700 dark:text-indigo-300 uppercase tracking-wider flex items-center gap-1.5 mb-1.5">
                      <HelpCircle className="w-4 h-4" />
                      Giải thích đáp án
                    </div>
                    <div className="text-sm text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-wrap">
                      {q.explanationVi}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )
        })}
        </div>

        {/* Sticky Question Palette Navigator (1 column) */}
        <div className="lg:col-span-1">
          <div className="sticky top-[88px] bg-white dark:bg-slate-900 rounded-2xl shadow-md border border-slate-200 dark:border-slate-800 p-5 space-y-4">
            <h3 className="text-sm font-bold text-slate-800 dark:text-white uppercase tracking-wider">
              Danh sách câu hỏi
            </h3>

            {/* Legend */}
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-400">
                <span className="w-3.5 h-3.5 rounded-md bg-emerald-500"></span>
                <span>Câu đúng</span>
              </div>
              <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-400">
                <span className="w-3.5 h-3.5 rounded-md bg-rose-500"></span>
                <span>Câu sai</span>
              </div>
              <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-400">
                <span className="w-3.5 h-3.5 rounded-md bg-slate-300 dark:bg-slate-700"></span>
                <span>Chưa làm</span>
              </div>
            </div>

            {/* Grid numbers */}
            <div className="grid grid-cols-5 gap-2 max-h-[60vh] overflow-y-auto pr-1">
              {allQuestions.map((q) => {
                const isCorrect = q.studentAnswer?.correct ?? false
                const isAnswered =
                  q.studentAnswer?.selectedOptionId != null ||
                  (q.studentAnswer?.orderedOptionIds && q.studentAnswer.orderedOptionIds.length > 0)

                let colorClass = 'bg-slate-100 dark:bg-slate-800 text-slate-500 hover:bg-slate-200'
                if (isAnswered) {
                  colorClass = isCorrect
                    ? 'bg-emerald-500 text-white font-bold hover:bg-emerald-600'
                    : 'bg-rose-500 text-white font-bold hover:bg-rose-600'
                }

                return (
                  <button
                    key={q.id}
                    onClick={() => scrollToQuestion(q.questionNumber)}
                    className={`h-9 rounded-xl text-xs flex items-center justify-center transition-all ${colorClass}`}
                    title={`Câu ${q.questionNumber}: ${
                      !isAnswered ? 'Chưa làm' : isCorrect ? 'Đúng' : 'Sai'
                    }`}
                  >
                    {q.questionNumber}
                  </button>
                )
              })}
            </div>

            <div className="pt-3 border-t border-slate-100 dark:border-slate-800">
              <button
                onClick={() => navigate(`/exams/result/${reviewData.attemptId}`)}
                className="w-full py-2.5 px-4 bg-indigo-50 dark:bg-indigo-900/30 hover:bg-indigo-100 dark:hover:bg-indigo-900/50 text-indigo-700 dark:text-indigo-300 font-semibold text-xs rounded-xl transition"
              >
                Xem lại phiếu điểm
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Lightbox zoom modal */}
      {zoomImageUrl && (
        <div
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4"
          onClick={() => setZoomImageUrl(null)}
        >
          <div className="relative max-w-4xl max-h-[90vh] overflow-auto bg-white dark:bg-slate-900 rounded-2xl p-2 shadow-2xl">
            <button
              onClick={() => setZoomImageUrl(null)}
              className="absolute top-4 right-4 bg-slate-900/70 hover:bg-slate-900 text-white p-2 rounded-full transition z-10"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={zoomImageUrl}
              alt="Ảnh phóng to"
              className="w-full h-auto rounded-xl object-contain max-h-[85vh]"
            />
          </div>
        </div>
      )}
    </div>
  )
}
export default ExamReviewPage
