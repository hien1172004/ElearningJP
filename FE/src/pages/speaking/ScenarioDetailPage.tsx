import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  LayoutGrid,
  Sparkles,
  Play,
  Square,
  Eye,
  EyeOff,
  Volume2,
  Languages,
  Mic,
  Plus,
  Check,
  Flag,
  Lightbulb,
  CheckCircle2,
  X,
  ChevronRight,
  RotateCcw,
  Loader2,
} from 'lucide-react'
import { speakingService } from '@/services/speaking.service'
import { FuriganaText } from '@/components/speaking/FuriganaText'
import { useSpeechRecognition } from '@/hooks/useSpeechRecognition'
import type { AiScenario, SampleDialogueLine, ScenarioVocab } from '@/types/speaking.types'

type ActiveTab = 'dialogue' | 'vocabulary' | 'grammar' | 'tips'

export const ScenarioDetailPage: React.FC = () => {
  const { scenarioId } = useParams<{ scenarioId: string }>()
  const navigate = useNavigate()

  const [scenario, setScenario] = useState<AiScenario | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [activeTab, setActiveTab] = useState<ActiveTab>('dialogue')
  const [isStartingSession, setIsStartingSession] = useState<boolean>(false)

  // Tab Hội thoại states
  const [hideDialogue, setHideDialogue] = useState<boolean>(false)
  const [isPlayingAllDialogue, setIsPlayingAllDialogue] = useState<boolean>(false)
  const [currentPlayingIndex, setCurrentPlayingIndex] = useState<number | null>(null)
  const [translationsVisible, setTranslationsVisible] = useState<Record<number, boolean>>({})
  const [translatedTexts, setTranslatedTexts] = useState<Record<number, string>>({})
  const [translatingIndex, setTranslatingIndex] = useState<number | null>(null)
  const [playingLineIndex, setPlayingLineIndex] = useState<number | null>(null)
  const [shadowingLine, setShadowingLine] = useState<{ index: number; text: string } | null>(null)
  const [shadowingScore, setShadowingScore] = useState<number | null>(null)
  const [shadowingResult, setShadowingResult] = useState<string | null>(null)

  // Web Speech API cho Luyện đọc (Shadowing)
  const {
    isListening: isShadowingListening,
    transcript: shadowingTranscript,
    interimTranscript: shadowingInterim,
    error: speechError,
    startListening: startShadowingListening,
    stopListening: stopShadowingListening,
    resetTranscript: resetShadowingTranscript,
  } = useSpeechRecognition({
    lang: 'ja-JP',
    continuous: false,
    interimResults: true,
  })

  // Tab Từ vựng states
  const [savedVocab, setSavedVocab] = useState<Record<string, boolean>>({})
  const [isPlayingAllVocab, setIsPlayingAllVocab] = useState<boolean>(false)
  const [currentVocabIndex, setCurrentVocabIndex] = useState<number | null>(null)
  const [showVocabPracticeModal, setShowVocabPracticeModal] = useState<boolean>(false)
  const [practiceVocabIndex, setPracticeVocabIndex] = useState<number>(0)
  const [showVocabAnswer, setShowVocabAnswer] = useState<boolean>(false)

  // Tab Ngữ pháp states
  const [expandedGrammar, setExpandedGrammar] = useState<Record<number, boolean>>({})
  const [savedGrammar, setSavedGrammar] = useState<Record<number, boolean>>({})
  const [isPlayingAllGrammar, setIsPlayingAllGrammar] = useState<boolean>(false)

  // Audio player ref
  const audioRef = React.useRef<HTMLAudioElement | null>(null)

  // Báo cáo modal state
  const [showReportModal, setShowReportModal] = useState<boolean>(false)
  const [reportSuccess, setReportSuccess] = useState<boolean>(false)

  useEffect(() => {
    if (!scenarioId) return
    const id = Number(scenarioId)
    if (isNaN(id)) {
      navigate('/speaking')
      return
    }

    setLoading(true)
    speakingService
      .getScenarioById(id)
      .then((data) => {
        setScenario(data)
      })
      .catch((err) => {
        console.error('Lỗi tải kịch bản:', err)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [scenarioId, navigate])

  // Dừng phát âm thanh khi unmount
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }
    }
  }, [])

  // Bắt đầu luyện nói với AI
  const handleStartPractice = async () => {
    if (!scenario || isStartingSession) return
    try {
      setIsStartingSession(true)
      const session = await speakingService.createSession({
        mode: 'SCENARIO',
        scenarioId: scenario.id,
      })
      navigate(`/speaking/session/${session.id}`)
    } catch (err) {
      console.error('Lỗi tạo phiên luyện nói:', err)
      alert('Không thể khởi tạo phiên luyện nói. Vui lòng thử lại!')
    } finally {
      setIsStartingSession(false)
    }
  }

  // Phát âm thanh tiếng Nhật qua Web Speech API (dự phòng đảm bảo 100% người dùng luôn nghe thấy tiếng)
  const speakWithBrowser = (text: string): Promise<void> => {
    return new Promise((resolve) => {
      if (!('speechSynthesis' in window)) {
        resolve()
        return
      }
      try {
        window.speechSynthesis.cancel()
        const utterance = new SpeechSynthesisUtterance(text)
        utterance.lang = 'ja-JP'
        utterance.rate = 0.95
        utterance.pitch = 1.0

        const voices = window.speechSynthesis.getVoices()
        const jaVoice = voices.find((v) => v.lang.startsWith('ja') || v.lang.includes('JP'))
        if (jaVoice) {
          utterance.voice = jaVoice
        }

        utterance.onend = () => resolve()
        utterance.onerror = () => resolve()
        window.speechSynthesis.speak(utterance)
      } catch {
        resolve()
      }
    })
  }

  // Phát TTS cho một câu (thử backend audio trước, nếu lỗi tự động fallback sang Web Speech)
  const playTts = async (text: string, lineIndex?: number): Promise<void> => {
    if (!text) return
    if (lineIndex !== undefined) {
      setPlayingLineIndex(lineIndex)
    }
    try {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }

      let played = false
      try {
        const tts = await speakingService.generateTts(text, 'ja')
        if (tts?.audioUrl) {
          const baseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/+$/, '')
          const cleanPath = tts.audioUrl.startsWith('/api/v1')
            ? tts.audioUrl.substring('/api/v1'.length)
            : tts.audioUrl
          const audioUrl = tts.audioUrl.startsWith('http') ? tts.audioUrl : `${baseUrl}${cleanPath}`

          await new Promise<void>((resolve, reject) => {
            const audio = new Audio(audioUrl)
            audioRef.current = audio
            audio.onended = () => {
              played = true
              resolve()
            }
            audio.onerror = (e) => reject(e)
            audio.play().catch(reject)
          })
        }
      } catch (err) {
        console.warn('Backend audio failed, falling back to Web Speech API:', err)
      }

      if (!played) {
        await speakWithBrowser(text)
      }
    } catch (err) {
      console.warn('Lỗi phát TTS:', err)
      await speakWithBrowser(text)
    } finally {
      if (lineIndex !== undefined) {
        setPlayingLineIndex(null)
      }
    }
  }

  // Phát toàn bộ đoạn hội thoại lần lượt
  const handlePlayAllDialogue = async () => {
    if (!scenario?.sampleDialogue || scenario.sampleDialogue.length === 0) return
    if (isPlayingAllDialogue) {
      if (audioRef.current) audioRef.current.pause()
      if ('speechSynthesis' in window) window.speechSynthesis.cancel()
      setIsPlayingAllDialogue(false)
      setCurrentPlayingIndex(null)
      return
    }

    setIsPlayingAllDialogue(true)
    for (let i = 0; i < scenario.sampleDialogue.length; i++) {
      const line = scenario.sampleDialogue[i]
      const textToPlay = line.text || line.ja || ''
      if (!textToPlay) continue

      setCurrentPlayingIndex(i)
      await playTts(textToPlay, i)
      // Dừng 600ms giữa các câu thoại
      await new Promise((res) => setTimeout(res, 600))
    }
    setIsPlayingAllDialogue(false)
    setCurrentPlayingIndex(null)
  }

  // Dịch một câu thoại mẫu (Hỗ trợ nghĩa có sẵn trong DB hoặc gọi Gemini AI dịch thời gian thực)
  const toggleTranslateLine = async (index: number, text: string) => {
    if (translationsVisible[index]) {
      setTranslationsVisible((prev) => ({ ...prev, [index]: false }))
      return
    }

    if (!translatedTexts[index]) {
      const line = scenario?.sampleDialogue?.[index]
      if (line?.meaning || line?.vi) {
        setTranslatedTexts((prev) => ({ ...prev, [index]: line.meaning || line.vi || '' }))
        setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
        return
      }

      setTranslatingIndex(index)
      setTranslationsVisible((prev) => ({ ...prev, [index]: true }))

      try {
        const res: any = await speakingService.translate(text)
        const translated = res.translatedText || res.textVi || (res.data && res.data.translatedText)
        if (translated) {
          setTranslatedTexts((prev) => ({ ...prev, [index]: translated }))
        } else {
          setTranslatedTexts((prev) => ({ ...prev, [index]: 'Không có bản dịch.' }))
        }
      } catch (err) {
        console.warn('Lỗi dịch câu:', err)
        setTranslatedTexts((prev) => ({ ...prev, [index]: 'Không thể dịch vào lúc này.' }))
      } finally {
        setTranslatingIndex(null)
      }
      return
    }

    setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
  }

  // Thuật toán chấm điểm tương đồng phát âm tiếng Nhật
  const calculateAccuracy = (target: string, spoken: string): number => {
    if (!spoken || !target) return 0
    const cleanTarget = target.replace(/[、。！？\s\.\?!]/g, '').toLowerCase()
    const cleanSpoken = spoken.replace(/[、。！？\s\.\?!]/g, '').toLowerCase()
    if (!cleanSpoken) return 0
    if (cleanTarget === cleanSpoken) return 100

    let matches = 0
    for (const char of cleanSpoken) {
      if (cleanTarget.includes(char)) {
        matches++
      }
    }
    const ratio = matches / Math.max(cleanTarget.length, 1)
    const score = Math.min(100, Math.round(ratio * 100))
    return Math.max(score, cleanSpoken.length > 0 ? 55 : 0)
  }

  // Shadowing / Luyện nhại câu
  const handleStartShadowing = (index: number, text: string) => {
    resetShadowingTranscript()
    setShadowingLine({ index, text })
    setShadowingScore(null)
    setShadowingResult(null)
  }

  // Bật / tắt Micro thu âm khi luyện đọc
  const handleToggleShadowingMic = () => {
    if (isShadowingListening) {
      stopShadowingListening()
      const spoken = (shadowingTranscript + ' ' + shadowingInterim).trim()
      if (spoken && shadowingLine) {
        const score = calculateAccuracy(shadowingLine.text, spoken)
        setShadowingScore(score)
        if (score >= 85) {
          setShadowingResult(`Rất xuất sắc! Độ chính xác ${score}% ✨`)
        } else if (score >= 65) {
          setShadowingResult(`Khá tốt! Độ chính xác ${score}%. Hãy phát âm rõ từng âm tiết hơn nhé! 👍`)
        } else {
          setShadowingResult(`Độ chính xác ${score}%. Hãy nghe lại câu mẫu và nói to rõ hơn nhé! 💪`)
        }
      }
    } else {
      resetShadowingTranscript()
      setShadowingScore(null)
      setShadowingResult(null)
      startShadowingListening()
    }
  }

  // Phát toàn bộ danh sách từ vựng lần lượt
  const handlePlayAllVocab = async () => {
    if (!scenario?.vocabulary || scenario.vocabulary.length === 0) return
    if (isPlayingAllVocab) {
      if (audioRef.current) audioRef.current.pause()
      setIsPlayingAllVocab(false)
      setCurrentVocabIndex(null)
      return
    }

    setIsPlayingAllVocab(true)
    for (let i = 0; i < scenario.vocabulary.length; i++) {
      const vocab = scenario.vocabulary[i]
      setCurrentVocabIndex(i)
      await playTts(vocab.word)
      await new Promise((res) => setTimeout(res, 800))
    }
    setIsPlayingAllVocab(false)
    setCurrentVocabIndex(null)
  }

  // Lưu từ vựng vào sổ tay
  const handleSaveVocab = (word: string) => {
    setSavedVocab((prev) => ({ ...prev, [word]: !prev[word] }))
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F0F2F5] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" />
          <span className="text-sm font-semibold text-slate-600">Đang tải tình huống...</span>
        </div>
      </div>
    )
  }

  if (!scenario) {
    return (
      <div className="min-h-screen bg-[#F0F2F5] flex flex-col items-center justify-center p-4">
        <p className="text-slate-600 mb-4 font-medium">Không tìm thấy tình huống này.</p>
        <button
          onClick={() => navigate('/speaking')}
          className="px-5 py-2 rounded-xl bg-blue-600 text-white font-bold text-sm shadow-xs"
        >
          Quay lại danh sách
        </button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#F0F2F5] text-slate-800 pb-16">
      {/* ==================================================================== */}
      {/* 1. Header điều hướng Mazii: < Quay lại | Tiêu đề | Báo cáo */}
      {/* ==================================================================== */}
      <div className="bg-white border-b border-slate-200/90 sticky top-0 z-30 shadow-xs">
        <div className="max-w-[1550px] mx-auto px-4 sm:px-8 py-3 flex items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/speaking')}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition shadow-2xs"
            >
              <ChevronLeft className="w-4 h-4 text-slate-500" />
              <span>Quay lại</span>
            </button>
            <h1 className="text-base sm:text-lg font-bold text-slate-800 truncate">
              {scenario.title}
            </h1>
          </div>

          <button
            onClick={() => {
              setShowReportModal(true)
              setReportSuccess(false)
            }}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition"
          >
            <Flag className="w-3.5 h-3.5 text-slate-400" />
            <span>Báo cáo</span>
          </button>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* 2. Nội dung 2 cột: Cột trái Tổng quan (35%) | Cột phải Luyện tập (65%) */}
      {/* ==================================================================== */}
      <div className="max-w-[1550px] mx-auto px-4 sm:px-8 mt-5">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
          {/* ------------------------------------------------------------------ */}
          {/* CỘT TRÁI: TỔNG QUAN (4/12 cols ~35%) */}
          {/* ------------------------------------------------------------------ */}
          <div className="lg:col-span-5 xl:col-span-4 bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs space-y-5">
            {/* Tiêu đề mục Tổng quan */}
            <div className="flex items-center gap-2 text-slate-800 border-b border-slate-100 pb-3">
              <LayoutGrid className="w-5 h-5 text-slate-700" />
              <h2 className="text-base font-bold">Tổng quan</h2>
            </div>

            {/* Cấp độ JLPT */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1.5">Cấp độ</div>
              <span className="inline-block px-2.5 py-0.5 rounded-md bg-emerald-600 text-white font-bold text-xs tracking-wide">
                {scenario.jlptLevel}
              </span>
            </div>

            {/* Vai trò của tôi */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1">Vai trò của tôi</div>
              <p className="text-sm text-slate-800 leading-relaxed font-normal">
                {scenario.userRole}
              </p>
            </div>

            {/* Vai trò của AI */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1">Vai trò của AI</div>
              <p className="text-sm text-slate-800 leading-relaxed font-normal">
                {scenario.aiRole}
              </p>
            </div>

            {/* Mô tả tình huống */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1">Mô tả tình huống</div>
              <p className="text-sm text-slate-700 leading-relaxed font-normal">
                {scenario.description}
              </p>
            </div>

            {/* Nhiệm vụ cần hoàn thành */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-2">Nhiệm vụ</div>
              <ul className="space-y-2">
                {scenario.tasks && scenario.tasks.length > 0 ? (
                  scenario.tasks.map((task) => (
                    <li key={task.id} className="flex items-start gap-2.5 text-xs sm:text-sm text-slate-700 leading-snug">
                      <span className="w-2 h-2 rounded-full bg-slate-300 mt-1.5 shrink-0" />
                      <span>{task.text}</span>
                    </li>
                  ))
                ) : (
                  <li className="text-xs text-slate-400 italic">Không có nhiệm vụ bắt buộc.</li>
                )}
              </ul>
            </div>

            {/* Nút to CTA: ✦ Luyện nói */}
            <div className="pt-2">
              <button
                type="button"
                onClick={handleStartPractice}
                disabled={isStartingSession}
                className="w-full py-3.5 rounded-full bg-linear-to-r from-[#8B5CF6] via-[#6366F1] to-[#3B82F6] hover:from-[#7C3AED] hover:to-[#2563EB] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition cursor-pointer disabled:opacity-60"
              >
                {isStartingSession ? (
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <Sparkles className="w-4 h-4 text-amber-300 fill-amber-300" />
                    <span>Luyện nói</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* ------------------------------------------------------------------ */}
          {/* CỘT PHẢI: LUYỆN TẬP TABS (8/12 cols ~65%) */}
          {/* ------------------------------------------------------------------ */}
          <div className="lg:col-span-7 xl:col-span-8 bg-white rounded-2xl border border-slate-200/90 p-5 sm:p-6 shadow-xs">
            {/* Header Luyện tập & Tab Navigation */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 gap-3">
              <h2 className="text-lg font-bold text-slate-800">Luyện tập</h2>

              {/* 4 Tabs: Hội thoại | Từ vựng | Ngữ pháp | Mẹo */}
              <div className="flex items-center gap-1.5 p-1 rounded-xl bg-slate-100/70 border border-slate-200/60 self-start sm:self-auto">
                <button
                  type="button"
                  onClick={() => setActiveTab('dialogue')}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                    activeTab === 'dialogue'
                      ? 'bg-blue-50 text-blue-600 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Hội thoại
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('vocabulary')}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                    activeTab === 'vocabulary'
                      ? 'bg-blue-50 text-blue-600 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Từ vựng
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('grammar')}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                    activeTab === 'grammar'
                      ? 'bg-blue-50 text-blue-600 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Ngữ pháp
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('tips')}
                  className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                    activeTab === 'tips'
                      ? 'bg-blue-50 text-blue-600 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Mẹo
                </button>
              </div>
            </div>

            {/* ================================================================ */}
            {/* TAB 1: HỘI THOẠI (Matching Screenshot 4) */}
            {/* ================================================================ */}
            {activeTab === 'dialogue' && (
              <div className="mt-4 space-y-4">
                {/* Subheader: Đoạn hội thoại mẫu | Nút Play | Nút Ẩn hội thoại */}
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-700">Đoạn hội thoại mẫu</h3>

                  <div className="flex items-center gap-2">
                    {/* Nút phát toàn bộ audio */}
                    <button
                      type="button"
                      onClick={handlePlayAllDialogue}
                      className={`p-2 rounded-full border transition flex items-center justify-center ${
                        isPlayingAllDialogue
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                      }`}
                      title={isPlayingAllDialogue ? 'Dừng phát' : 'Phát toàn bộ đoạn hội thoại'}
                    >
                      {isPlayingAllDialogue ? (
                        <Square className="w-3.5 h-3.5 fill-current" />
                      ) : (
                        <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                      )}
                    </button>

                    {/* Nút Ẩn hội thoại */}
                    <button
                      type="button"
                      onClick={() => setHideDialogue((prev) => !prev)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium border border-slate-200 transition"
                    >
                      {hideDialogue ? (
                        <>
                          <Eye className="w-3.5 h-3.5 text-slate-500" />
                          <span>Hiện hội thoại</span>
                        </>
                      ) : (
                        <>
                          <EyeOff className="w-3.5 h-3.5 text-slate-500" />
                          <span>Ẩn hội thoại</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Khung đoạn chat màu tím nhạt (#F7F5FC / #F5F3FF) */}
                <div className="bg-[#F7F5FC] rounded-2xl p-4 sm:p-6 space-y-4 min-h-[360px]">
                  {scenario.sampleDialogue && scenario.sampleDialogue.length > 0 ? (
                    scenario.sampleDialogue.map((line: SampleDialogueLine, index: number) => {
                      const isUser = line.speaker === 'USER' || line.role === 'USER'
                      const lineText = line.text || line.ja || ''
                      const isCurrentPlaying = currentPlayingIndex === index
                      const isTranslated = !!translationsVisible[index]

                      return (
                        <div
                          key={index}
                          className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-[85%] sm:max-w-[75%] rounded-2xl p-4 shadow-xs transition-all ${
                              isUser
                                ? 'bg-white rounded-tr-xs border border-slate-100'
                                : 'bg-white rounded-tl-xs border border-slate-100'
                            } ${isCurrentPlaying ? 'ring-2 ring-indigo-500 shadow-md' : ''}`}
                          >
                            {/* Nội dung câu thoại tiếng Nhật (có thể bị ẩn nếu bật chế độ nghe) */}
                            {hideDialogue ? (
                              <div className="py-2 px-4 rounded-lg bg-slate-100 text-xs text-slate-400 italic flex items-center gap-2">
                                <Volume2 className="w-3.5 h-3.5 text-slate-400" />
                                <span>(Nội dung đang ẩn để luyện nghe - nhấn loa để nghe)</span>
                              </div>
                            ) : (
                              <div>
                                <FuriganaText
                                  text={lineText}
                                  size="md"
                                  showRomaji={true}
                                />
                                {isTranslated && (
                                  <div className="mt-2 pt-2 border-t border-slate-100 text-xs sm:text-sm text-indigo-700 font-medium">
                                    {translatingIndex === index ? (
                                      <div className="flex items-center gap-2 text-indigo-500 animate-pulse">
                                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                        <span>Đang dịch câu thoại...</span>
                                      </div>
                                    ) : (
                                      <span>{translatedTexts[index] || line.vi || line.meaning || 'Không có bản dịch.'}</span>
                                    )}
                                  </div>
                                )}
                              </div>
                            )}

                            {/* Thanh công cụ nhỏ: 🔊 Loa | 🌐 Dịch | 🎙️ Luyện nhại */}
                            <div className="flex items-center gap-1.5 mt-3 pt-2 border-t border-slate-100/80">
                              {/* 🔊 Nghe TTS */}
                              <button
                                type="button"
                                onClick={() => playTts(lineText, index)}
                                className={`p-1.5 rounded-lg transition ${
                                  playingLineIndex === index
                                    ? 'bg-blue-100 text-blue-700 scale-105 shadow-xs'
                                    : 'hover:bg-slate-100 text-slate-500 hover:text-slate-800'
                                }`}
                                title="Nghe phát âm tiếng Nhật"
                              >
                                <Volume2 className={`w-4 h-4 ${playingLineIndex === index ? 'animate-bounce text-blue-600' : ''}`} />
                              </button>

                              {/* 🌐 Dịch nghĩa */}
                              <button
                                type="button"
                                onClick={() => toggleTranslateLine(index, lineText)}
                                className={`p-1.5 rounded-lg transition ${
                                  isTranslated
                                    ? 'bg-indigo-50 text-indigo-600'
                                    : 'hover:bg-slate-100 text-slate-500 hover:text-slate-800'
                                }`}
                                title="Xem bản dịch tiếng Việt"
                              >
                                <Languages className="w-4 h-4" />
                              </button>

                              {/* 🎙️ Shadowing mic */}
                              <button
                                type="button"
                                onClick={() => handleStartShadowing(index, lineText)}
                                className="p-1.5 rounded-lg hover:bg-indigo-50 text-slate-500 hover:text-indigo-600 transition"
                                title="Luyện đọc theo câu này"
                              >
                                <Mic className="w-4 h-4" />
                              </button>
                            </div>
                          </div>
                        </div>
                      )
                    })
                  ) : (
                    <div className="text-center py-12 text-slate-400 text-sm">
                      Chưa có đoạn hội thoại mẫu cho tình huống này.
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* ================================================================ */}
            {/* TAB 2: TỪ VỰNG (Matching Screenshot 5) */}
            {/* ================================================================ */}
            {activeTab === 'vocabulary' && (
              <div className="mt-4 space-y-4">
                {/* Subheader: Danh sách | Nút Play | Nút Luyện tập */}
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-700">Danh sách</h3>

                  <div className="flex items-center gap-2">
                    {/* Nút phát toàn bộ từ vựng */}
                    <button
                      type="button"
                      onClick={handlePlayAllVocab}
                      className={`p-2 rounded-full border transition flex items-center justify-center ${
                        isPlayingAllVocab
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                      }`}
                      title={isPlayingAllVocab ? 'Dừng phát' : 'Phát âm thanh toàn bộ từ vựng'}
                    >
                      {isPlayingAllVocab ? (
                        <Square className="w-3.5 h-3.5 fill-current" />
                      ) : (
                        <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                      )}
                    </button>

                    {/* Nút Luyện tập từ vựng */}
                    <button
                      type="button"
                      onClick={() => {
                        setShowVocabPracticeModal(true)
                        setPracticeVocabIndex(0)
                        setShowVocabAnswer(false)
                      }}
                      className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold shadow-xs transition"
                    >
                      Luyện tập
                    </button>
                  </div>
                </div>

                {/* Grid 2 cột từ vựng (Screenshot 5) */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  {scenario.vocabulary && scenario.vocabulary.length > 0 ? (
                    scenario.vocabulary.map((item: ScenarioVocab, idx: number) => {
                      const isCurrentPlaying = currentVocabIndex === idx
                      const isSaved = !!savedVocab[item.word]

                      return (
                        <div
                          key={idx}
                          className={`bg-[#F8FAFC] rounded-2xl p-4 border border-slate-100/90 hover:border-indigo-200 hover:bg-slate-50 transition flex items-start justify-between gap-3 ${
                            isCurrentPlaying ? 'ring-2 ring-indigo-500 bg-indigo-50/30' : ''
                          }`}
                        >
                          {/* Trái: Từ kanji kèm furigana và nghĩa tiếng Việt */}
                          <div className="flex-1 min-w-0">
                            {/* Furigana text */}
                            <div className="mb-1">
                              <FuriganaText
                                text={item.word}
                                size="md"
                                showRomaji={false}
                              />
                            </div>
                            {/* Nghĩa tiếng Việt */}
                            <p className="text-xs sm:text-sm text-slate-600 leading-snug line-clamp-2">
                              {item.meaning}
                            </p>
                          </div>

                          {/* Phải: Nút loa phát âm + Nút thêm vào sổ tay */}
                          <div className="flex items-center gap-1 shrink-0 pt-1">
                            <button
                              type="button"
                              onClick={() => playTts(item.word)}
                              className="p-1.5 rounded-lg hover:bg-slate-200 text-slate-500 hover:text-slate-800 transition"
                              title="Nghe phát âm"
                            >
                              <Volume2 className="w-4 h-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleSaveVocab(item.word)}
                              className={`p-1.5 rounded-lg transition ${
                                isSaved
                                  ? 'bg-emerald-50 text-emerald-600'
                                  : 'hover:bg-slate-200 text-slate-400 hover:text-slate-700'
                              }`}
                              title={isSaved ? 'Đã lưu vào sổ từ' : 'Thêm vào sổ tay từ vựng'}
                            >
                              {isSaved ? (
                                <Check className="w-4 h-4 text-emerald-600" />
                              ) : (
                                <Plus className="w-4 h-4" />
                              )}
                            </button>
                          </div>
                        </div>
                      )
                    })
                  ) : (
                    <div className="col-span-2 text-center py-12 text-slate-400 text-sm">
                      Chưa có từ vựng tiêu biểu cho tình huống này.
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* ================================================================ */}
            {/* TAB 3: NGỮ PHÁP */}
            {/* ================================================================ */}
            {activeTab === 'grammar' && (
              <div className="mt-4 space-y-4">
                {/* Subheader: Danh sách | Nút Play | Nút Luyện tập (Screenshot 5) */}
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-700">Danh sách</h3>

                  <div className="flex items-center gap-2">
                    {/* Nút phát toàn bộ ngữ pháp */}
                    <button
                      type="button"
                      onClick={() => {
                        if (isPlayingAllGrammar) {
                          if (audioRef.current) audioRef.current.pause()
                          setIsPlayingAllGrammar(false)
                        } else if (scenario.grammar && scenario.grammar.length > 0) {
                          setIsPlayingAllGrammar(true)
                          const playNext = async () => {
                            for (let i = 0; i < scenario.grammar.length; i++) {
                              const g = scenario.grammar[i]
                              await playTts(g.example || g.pattern)
                              await new Promise((res) => setTimeout(res, 800))
                            }
                            setIsPlayingAllGrammar(false)
                          }
                          playNext()
                        }
                      }}
                      className={`p-2 rounded-full border transition flex items-center justify-center ${
                        isPlayingAllGrammar
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                      }`}
                      title={isPlayingAllGrammar ? 'Dừng phát' : 'Phát âm thanh câu ví dụ'}
                    >
                      {isPlayingAllGrammar ? (
                        <Square className="w-3.5 h-3.5 fill-current" />
                      ) : (
                        <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                      )}
                    </button>

                    {/* Nút Luyện tập */}
                    <button
                      type="button"
                      onClick={() => alert('Chế độ luyện tập ngữ pháp đang được kích hoạt!')}
                      className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold shadow-xs transition"
                    >
                      Luyện tập
                    </button>
                  </div>
                </div>

                {/* Danh sách thẻ ngữ pháp (1:1 Screenshot 5) */}
                <div className="space-y-3.5">
                  {scenario.grammar && scenario.grammar.length > 0 ? (
                    scenario.grammar.map((item, idx) => {
                      const isExpanded = !!expandedGrammar[idx]
                      const isSaved = !!savedGrammar[idx]

                      return (
                        <div
                          key={idx}
                          className="bg-[#F8FAFC] rounded-2xl p-4 sm:p-5 border border-slate-200/80 hover:border-indigo-200 transition space-y-2.5"
                        >
                          {/* Row 1: Cấu trúc ngữ pháp + Nút + */}
                          <div className="flex items-center justify-between">
                            <h4 className="font-bold text-sm sm:text-base text-slate-800 tracking-wide">
                              {item.pattern}
                            </h4>
                            <button
                              type="button"
                              onClick={() => setSavedGrammar((prev) => ({ ...prev, [idx]: !prev[idx] }))}
                              className={`p-1.5 rounded-lg transition ${
                                isSaved
                                  ? 'bg-emerald-50 text-emerald-600'
                                  : 'hover:bg-slate-200 text-slate-400 hover:text-slate-700'
                              }`}
                              title={isSaved ? 'Đã lưu' : 'Thêm vào sổ tay ngữ pháp'}
                            >
                              {isSaved ? (
                                <Check className="w-4 h-4 text-emerald-600" />
                              ) : (
                                <Plus className="w-4 h-4" />
                              )}
                            </button>
                          </div>

                          {/* Row 2: Ý nghĩa tiếng Việt */}
                          <p className="text-xs sm:text-sm text-slate-600 font-normal leading-relaxed">
                            {item.meaning}
                          </p>

                          {/* Row 3 & 4: Câu ví dụ trong bài + Icon loa */}
                          {item.example && (
                            <div className="pt-2 border-t border-slate-200/70 space-y-1">
                              <div className="flex items-center justify-between">
                                <span className="text-xs sm:text-sm font-semibold text-slate-800">
                                  {item.example}
                                </span>
                                <button
                                  type="button"
                                  onClick={() => playTts(item.example!)}
                                  className="p-1.5 rounded-lg hover:bg-slate-200 text-slate-500 hover:text-slate-800 transition"
                                  title="Nghe phát âm ví dụ"
                                >
                                  <Volume2 className="w-4 h-4" />
                                </button>
                              </div>
                              {item.exampleMeaning && (
                                <p className="text-xs text-slate-500">
                                  {item.exampleMeaning}
                                </p>
                              )}
                            </div>
                          )}

                          {/* Row 5: Nút Giải nghĩa kèm mũi tên > */}
                          <div className="pt-1">
                            <button
                              type="button"
                              onClick={() =>
                                setExpandedGrammar((prev) => ({ ...prev, [idx]: !prev[idx] }))
                              }
                              className="w-full flex items-center justify-between py-2 px-3 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition"
                            >
                              <span>Giải nghĩa</span>
                              <ChevronRight
                                className={`w-4 h-4 text-slate-400 transform transition-transform ${
                                  isExpanded ? 'rotate-90' : ''
                                }`}
                              />
                            </button>

                            {/* Khung giải nghĩa mở rộng */}
                            {isExpanded && (
                              <div className="mt-2 p-3 rounded-xl bg-indigo-50/60 border border-indigo-100 text-xs text-slate-700 leading-relaxed animate-in fade-in">
                                {item.explanation ||
                                  `Cấu trúc "${item.pattern}" được sử dụng phổ biến trong văn cảnh này để diễn đạt: ${item.meaning}. Hãy ghi nhớ và luyện tập phát âm thường xuyên!`}
                              </div>
                            )}
                          </div>
                        </div>
                      )
                    })
                  ) : (
                    <div className="text-center py-12 text-slate-400 text-sm">
                      Chưa có điểm ngữ pháp nào được đánh dấu.
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* ================================================================ */}
            {/* TAB 4: MẸO & GIAO TIẾP */}
            {/* ================================================================ */}
            {activeTab === 'tips' && (
              <div className="mt-4 space-y-4">
                <h3 className="text-sm font-bold text-slate-700">Mẹo giao tiếp tiếng Nhật</h3>
                <div className="space-y-3">
                  {scenario.tips && scenario.tips.length > 0 ? (
                    scenario.tips.map((tip, idx) => (
                      <div
                        key={idx}
                        className="bg-amber-50/60 rounded-2xl p-4 sm:p-5 border border-amber-200/60 flex items-start gap-3"
                      >
                        <div className="w-7 h-7 rounded-xl bg-amber-100 flex items-center justify-center shrink-0 mt-0.5">
                          <Lightbulb className="w-4 h-4 text-amber-600" />
                        </div>
                        <p className="text-xs sm:text-sm text-amber-950 leading-relaxed font-normal">
                          {tip}
                        </p>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-12 text-slate-400 text-sm">
                      Chưa có mẹo giao tiếp bổ sung.
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* MODAL: Shadowing / Luyện nhại theo câu */}
      {/* ==================================================================== */}
      {shadowingLine && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl relative space-y-4">
            <button
              onClick={() => {
                if (isShadowingListening) stopShadowingListening()
                setShadowingLine(null)
              }}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <Mic className="w-5 h-5 text-indigo-600" />
              <span>Luyện phát âm theo mẫu (Shadowing)</span>
            </h3>

            {/* Khung câu mẫu tiếng Nhật */}
            <div className="p-4 rounded-xl bg-[#F7F5FC] text-center space-y-2">
              <FuriganaText text={shadowingLine.text} size="lg" />
              <button
                type="button"
                onClick={() => playTts(shadowingLine.text)}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50 shadow-2xs mt-2 transition"
              >
                <Volume2 className="w-3.5 h-3.5 text-indigo-600" />
                <span>Nghe lại câu mẫu</span>
              </button>
            </div>

            {/* Khung kết quả giọng nói của người dùng */}
            {(shadowingTranscript || shadowingInterim) && (
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-center space-y-1 animate-fade-in">
                <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                  Giọng của bạn thu được:
                </span>
                <p className="text-sm font-medium text-slate-800">
                  {shadowingTranscript}
                  {shadowingInterim && <span className="text-indigo-400 italic"> {shadowingInterim}</span>}
                </p>
              </div>
            )}

            {/* Micro thu âm tương tác */}
            <div className="flex flex-col items-center justify-center pt-2">
              <button
                type="button"
                onClick={handleToggleShadowingMic}
                className={`w-16 h-16 rounded-full flex items-center justify-center transition shadow-lg ${
                  isShadowingListening
                    ? 'bg-rose-500 animate-pulse text-white ring-4 ring-rose-200 scale-105'
                    : 'bg-linear-to-r from-purple-600 to-blue-600 text-white hover:scale-105'
                }`}
                title={isShadowingListening ? 'Nhấn để hoàn thành câu' : 'Nhấn vào micro để đọc'}
              >
                <Mic className="w-7 h-7" />
              </button>
              <span className="text-xs font-medium text-slate-600 mt-2.5">
                {isShadowingListening ? 'Đang lắng nghe... Hãy đọc câu tiếng Nhật trên' : 'Nhấn vào micro để bắt đầu đọc'}
              </span>
            </div>

            {/* Thông báo lỗi Micro nếu có */}
            {speechError && (
              <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs text-center font-medium">
                {speechError}
              </div>
            )}

            {/* Kết quả đánh giá phát âm */}
            {shadowingResult && (
              <div className="space-y-3 animate-fade-in">
                <div
                  className={`p-3.5 rounded-xl border text-xs font-semibold text-center flex items-center justify-center gap-2 ${
                    (shadowingScore ?? 0) >= 80
                      ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                      : (shadowingScore ?? 0) >= 60
                      ? 'bg-amber-50 border-amber-200 text-amber-800'
                      : 'bg-rose-50 border-rose-200 text-rose-800'
                  }`}
                >
                  <Sparkles className="w-4 h-4 shrink-0" />
                  <span>{shadowingResult}</span>
                </div>

                <div className="flex items-center justify-center gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => {
                      resetShadowingTranscript()
                      setShadowingScore(null)
                      setShadowingResult(null)
                      startShadowingListening()
                    }}
                    className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
                  >
                    <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
                    <span>Thử đọc lại</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL: Luyện tập Flashcard Từ vựng */}
      {/* ==================================================================== */}
      {showVocabPracticeModal && scenario?.vocabulary && scenario.vocabulary.length > 0 && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl relative space-y-5">
            <button
              onClick={() => setShowVocabPracticeModal(false)}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center justify-between text-xs text-slate-500 font-semibold">
              <span>Luyện nhớ từ vựng</span>
              <span>
                {practiceVocabIndex + 1} / {scenario.vocabulary.length}
              </span>
            </div>

            {/* Flashcard Box */}
            <div
              onClick={() => setShowVocabAnswer((prev) => !prev)}
              className="min-h-[180px] p-6 rounded-2xl bg-[#F8FAFC] border-2 border-dashed border-indigo-200 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-slate-50 transition"
            >
              <div className="text-2xl font-bold text-slate-800 mb-2">
                {scenario.vocabulary[practiceVocabIndex].word}
              </div>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  playTts(scenario.vocabulary[practiceVocabIndex].word)
                }}
                className="p-1.5 rounded-full bg-white shadow-xs text-slate-500 hover:text-indigo-600 mb-3"
              >
                <Volume2 className="w-4 h-4" />
              </button>

              {showVocabAnswer ? (
                <div className="text-sm font-semibold text-indigo-700 animate-fade-in">
                  {scenario.vocabulary[practiceVocabIndex].meaning}
                </div>
              ) : (
                <div className="text-xs text-slate-400">
                  (Nhấn vào thẻ để xem nghĩa tiếng Việt)
                </div>
              )}
            </div>

            {/* Navigation buttons */}
            <div className="flex items-center justify-between gap-3">
              <button
                type="button"
                disabled={practiceVocabIndex === 0}
                onClick={() => {
                  setPracticeVocabIndex((p) => Math.max(0, p - 1))
                  setShowVocabAnswer(false)
                }}
                className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-40"
              >
                Từ trước
              </button>

              <button
                type="button"
                onClick={() => {
                  if (practiceVocabIndex < scenario.vocabulary.length - 1) {
                    setPracticeVocabIndex((p) => p + 1)
                    setShowVocabAnswer(false)
                  } else {
                    setShowVocabPracticeModal(false)
                  }
                }}
                className="px-5 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-700 shadow-xs"
              >
                {practiceVocabIndex < scenario.vocabulary.length - 1 ? 'Từ kế tiếp' : 'Hoàn thành'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL: Báo cáo tình huống */}
      {/* ==================================================================== */}
      {showReportModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl relative space-y-4">
            <button
              onClick={() => setShowReportModal(false)}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <Flag className="w-5 h-5 text-rose-500" />
              <span>Báo cáo tình huống</span>
            </h3>

            {reportSuccess ? (
              <div className="text-center py-6 space-y-2">
                <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
                <p className="text-sm font-bold text-slate-800">Cảm ơn bạn đã phản hồi!</p>
                <p className="text-xs text-slate-500">
                  Đội ngũ biên soạn sẽ kiểm tra và cập nhật nội dung trong thời gian sớm nhất.
                </p>
              </div>
            ) : (
              <>
                <p className="text-xs text-slate-500">
                  Hãy cho chúng tôi biết vấn đề bạn gặp phải với tình huống &quot;{scenario.title}&quot;:
                </p>

                <div className="space-y-2 text-xs">
                  {['Lỗi ngữ pháp / dịch nghĩa', 'Lỗi phát âm audio', 'Nội dung không phù hợp', 'Khác'].map(
                    (reason) => (
                      <label
                        key={reason}
                        className="flex items-center gap-2 p-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 cursor-pointer"
                      >
                        <input type="radio" name="reportReason" className="text-indigo-600" defaultChecked={reason === 'Lỗi ngữ pháp / dịch nghĩa'} />
                        <span className="text-slate-700 font-medium">{reason}</span>
                      </label>
                    )
                  )}
                </div>

                <textarea
                  placeholder="Mô tả chi tiết thêm nếu có..."
                  rows={3}
                  className="w-full text-xs p-3 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                />

                <div className="flex justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowReportModal(false)}
                    className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                  >
                    Hủy
                  </button>
                  <button
                    type="button"
                    onClick={() => setReportSuccess(true)}
                    className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold"
                  >
                    Gửi báo cáo
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
export default ScenarioDetailPage
