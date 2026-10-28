import React, { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  Settings2,
  Volume2,
  Copy,
  Languages,
  Check,
  Target,
  Zap,
  Lightbulb,
  Keyboard,
  Mic,
  X,
  AlertTriangle,
  CheckCircle2,
  ArrowUp,
  Square,
} from 'lucide-react'
import { speakingService } from '@/services/speaking.service'
import { FuriganaText } from '@/components/speaking/FuriganaText'
import { MaziikoMascot } from '@/components/speaking/MaziikoMascot'
import { GrammarCorrectionCard } from '@/components/speaking/GrammarCorrectionCard'
import { useSpeechRecognition, cleanDuplicateSpeech } from '@/hooks/useSpeechRecognition'
import { playSmartAudio } from '@/utils'
import type { AiSession, AiMessage, ScenarioTask } from '@/types/speaking.types'

export const SpeakingPracticePage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()

  const [session, setSession] = useState<AiSession | null>(null)
  const [messages, setMessages] = useState<AiMessage[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isProcessing, setIsProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Settings State (Modal / Popover)
  const [showSettings, setShowSettings] = useState(false)
  const [showRomaji, setShowRomaji] = useState<boolean>(() => {
    const saved = localStorage.getItem('speaking_show_romaji')
    return saved !== null ? saved === 'true' : true
  })
  const [showFurigana, setShowFurigana] = useState<boolean>(() => {
    const saved = localStorage.getItem('speaking_show_furigana')
    return saved !== null ? saved === 'true' : true
  })
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0)
  const [selectedVoice, setSelectedVoice] = useState<'female' | 'male'>(() => {
    return (localStorage.getItem('speaking_voice_gender') as 'female' | 'male') || 'female'
  })

  const handleToggleRomaji = () => {
    setShowRomaji((prev) => {
      const next = !prev
      localStorage.setItem('speaking_show_romaji', String(next))
      return next
    })
  }

  const handleToggleFurigana = () => {
    setShowFurigana((prev) => {
      const next = !prev
      localStorage.setItem('speaking_show_furigana', String(next))
      return next
    })
  }

  const handleSelectVoice = (voice: 'female' | 'male') => {
    setSelectedVoice(voice)
    localStorage.setItem('speaking_voice_gender', voice)
  }

  // Translation per message
  const [translationsVisible, setTranslationsVisible] = useState<Record<number, boolean>>({})
  const [translatedTexts, setTranslatedTexts] = useState<Record<number, string>>({})
  const [copiedId, setCopiedId] = useState<number | string | null>(null)

  // Floating Action Popups: Nhiệm vụ | Gợi ý
  const [showTasksModal, setShowTasksModal] = useState(false)
  const [showInlineSuggestion, setShowInlineSuggestion] = useState(false)
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false)
  const [suggestionList, setSuggestionList] = useState<Array<{ ja: string; vi?: string }>>([])
  const [currentSugIndex, setCurrentSugIndex] = useState(0)

  // Input capsule state
  const [isKeyboardMode, setIsKeyboardMode] = useState(false)
  const [inputText, setInputText] = useState('')

  const messagesEndRef = useRef<HTMLDivElement>(null)
  const isProcessingRef = useRef(false)
  const lastSentTextRef = useRef('')
  const lastSentTimeRef = useRef(0)

  // Speech Recognition hook: silenceTimeoutMs = 0 (tắt tự gửi, hiển thị chữ trực tiếp)
  const {
    isListening,
    transcript,
    interimTranscript,
    error: speechError,
    startListening,
    stopListening,
    cancelListening,
    resetTranscript,
  } = useSpeechRecognition({
    lang: 'ja-JP',
    continuous: true,
    silenceTimeoutMs: 0,
    onError: (err) => {
      console.warn('Speech recognition error:', err)
    },
  })

  // Đoạn text thu âm trực tiếp khi người dùng nói (đã làm sạch và khử trùng lặp câu)
  const spokenText = cleanDuplicateSpeech((transcript + (interimTranscript ? ' ' + interimTranscript : '')).trim())

  // Gửi câu thoại đã nói cho AI khi người dùng bấm nút gửi
  const handleSendSpeech = () => {
    const textToSend = cleanDuplicateSpeech(spokenText)
    if (!textToSend || isProcessingRef.current) return
    stopListening()
    resetTranscript()
    handleSendMessage(textToSend)
  }

  // Hủy phiên thu âm hiện tại
  const handleCancelSpeech = () => {
    cancelListening()
  }

  // Tiếp tục nói thêm
  const handleResumeListening = () => {
    startListening(true)
  }

  const scrollToBottom = (smooth = true) => {
    messagesEndRef.current?.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto' })
  }

  // Tải chi tiết phiên làm việc
  useEffect(() => {
    if (!sessionId) return

    setIsLoading(true)
    setError(null)

    speakingService
      .getSession(Number(sessionId))
      .then((data) => {
        setSession(data)
        setMessages(data.messages || [])

        // Kiểm tra xem có tin nhắn đang chờ gửi từ trang trước không
        const pending = sessionStorage.getItem(`pending_message_${data.id}`)
        if (pending) {
          sessionStorage.removeItem(`pending_message_${data.id}`)
          setTimeout(() => {
            handleSendMessage(pending)
          }, 400)
        }

        setTimeout(() => scrollToBottom(false), 150)
      })
      .catch((err) => {
        console.error('Lỗi tải phiên luyện nói:', err)
        setError('Không thể tải phiên luyện nói. Vui lòng thử lại!')
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [sessionId])

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // Gửi tin nhắn từ người học và stream phản hồi AI
  const handleSendMessage = (textToSend: string) => {
    const cleanText = cleanDuplicateSpeech(textToSend).trim()
    const now = Date.now()
    if (!session || !cleanText || isProcessingRef.current) return

    // Chống gửi trùng lặp câu nói trong vòng 2 giây
    if (cleanText === lastSentTextRef.current && now - lastSentTimeRef.current < 2000) {
      console.warn('Bỏ qua tin nhắn trùng lặp:', cleanText)
      return
    }

    isProcessingRef.current = true
    lastSentTextRef.current = cleanText
    lastSentTimeRef.current = now

    const userMsg: AiMessage = {
      role: 'USER',
      textJa: cleanText,
      createdAt: new Date().toISOString(),
    }

    const tempAiMsg: AiMessage = {
      role: 'AI',
      textJa: '',
      isStreaming: true,
      createdAt: new Date().toISOString(),
    }

    setMessages((prev) => [...prev, userMsg, tempAiMsg])
    setIsProcessing(true)
    setInputText('')

    speakingService.sendStreamMessage(session.id, cleanText, {
      onChunk: (chunk: string) => {
        setMessages((prev) => {
          const next = [...prev]
          const lastIdx = next.length - 1
          if (lastIdx >= 0 && next[lastIdx].role === 'AI') {
            next[lastIdx] = {
              ...next[lastIdx],
              textJa: next[lastIdx].textJa + chunk,
            }
          }
          return next
        })
      },
      onComplete: (event) => {
        isProcessingRef.current = false
        setIsProcessing(false)
        setMessages((prev) => {
          const next = [...prev]
          const lastIdx = next.length - 1
          if (lastIdx >= 0 && next[lastIdx].role === 'AI') {
            next[lastIdx] = {
              ...next[lastIdx],
              id: event.aiMessageId,
              textJa: event.response?.reply_ja || next[lastIdx].textJa,
              textVi: event.response?.reply_vi || event.response?.replyVi || next[lastIdx].textVi,
              correction: event.response?.correction || null,
              isStreaming: false,
            }
          }
          if (event.userMessageId && next.length >= 2) {
            const userIdx = next.length - 2
            if (next[userIdx].role === 'USER') {
              next[userIdx] = {
                ...next[userIdx],
                id: event.userMessageId,
              }
            }
          }
          return next
        })

        // Tự động phát âm thanh câu trả lời của AI ngay khi vừa nhận xong
        const aiReply =
          event.response?.reply_ja ||
          (event.response as any)?.replyJa ||
          ''
        if (aiReply) {
          playSmartAudio({
            text: aiReply,
            voice: selectedVoice,
            rate: playbackSpeed,
          })
        }

        // Cập nhật nhiệm vụ hoàn thành nếu là kịch bản
        if (event.response?.completed_task_ids && session) {
          setSession((prev) => {
            if (!prev) return prev
            const currentTasks = new Set(prev.completedTasks || [])
            event.response!.completed_task_ids!.forEach((id) => currentTasks.add(id))
            return {
              ...prev,
              completedTasks: Array.from(currentTasks),
            }
          })
        }
      },
      onError: (err) => {
        isProcessingRef.current = false
        setIsProcessing(false)
        console.error('Lỗi SSE:', err)
        setMessages((prev) => {
          const next = [...prev]
          const lastIdx = next.length - 1
          if (lastIdx >= 0 && next[lastIdx].role === 'AI') {
            next[lastIdx] = {
              ...next[lastIdx],
              textJa: 'ごめんなさい、ちょっとエラーが発生しました。(Xin lỗi, xảy ra lỗi kết nối. Hãy thử lại nhé!)',
              isStreaming: false,
            }
          }
          return next
        })
      },
    })
  }

  // Phát TTS kèm tốc độ đọc & giọng đọc được chọn (Edge-TTS Nanami/Keita chuẩn người bản xứ)
  const playTts = (text: string) => {
    if (!text || !text.trim()) return
    playSmartAudio({
      text: text.trim(),
      voice: selectedVoice,
      rate: playbackSpeed,
    })
  }

  // Sao chép nội dung vào clipboard
  const handleCopy = (id: number | string, text: string) => {
    navigator.clipboard.writeText(text)
    setCopiedId(id)
    setTimeout(() => setCopiedId(null), 2000)
  }

  // Dịch nghĩa câu
  const handleToggleTranslate = async (index: number, text: string) => {
    if (translationsVisible[index]) {
      setTranslationsVisible((prev) => ({ ...prev, [index]: false }))
      return
    }

    if (!translatedTexts[index]) {
      const msg = messages[index]
      // Nếu tin nhắn đã có sẵn bản dịch tiếng Việt thực sự (khác tiếng Nhật)
      if (msg?.textVi && msg.textVi.trim() !== msg.textJa.trim()) {
        setTranslatedTexts((prev) => ({ ...prev, [index]: msg.textVi! }))
        setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
        return
      }

      try {
        const res = await speakingService.translate(text, msg?.id)
        const translated = res.translatedText || res.textVi || 'Không có bản dịch.'
        setTranslatedTexts((prev) => ({ ...prev, [index]: translated }))
      } catch (err) {
        console.warn('Lỗi dịch:', err)
        setTranslatedTexts((prev) => ({ ...prev, [index]: 'Không thể dịch vào lúc này.' }))
      }
    }
    setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
  }

  // Nút "Dịch nhanh": Dịch câu thoại AI gần nhất
  const handleQuickTranslateLastMessage = () => {
    const lastAiIdx = messages.map((m) => m.role).lastIndexOf('AI')
    if (lastAiIdx >= 0) {
      handleToggleTranslate(lastAiIdx, messages[lastAiIdx].textJa)
    }
  }

  // Nút "Gợi ý": Mở/đóng thẻ gợi ý câu thoại thông minh từ AI (1:1 Screenshot 2)
  const handleOpenSuggestions = async () => {
    if (showInlineSuggestion) {
      setShowInlineSuggestion(false)
      return
    }

    setShowInlineSuggestion(true)
    setIsLoadingSuggestions(true)

    try {
      // 1. Lấy câu thoại gần nhất của AI trong cuộc trò chuyện
      const lastAiMsg = [...messages].reverse().find((m) => m.role === 'AI')?.textJa || ''
      const incompleteTasks =
        session?.scenario?.tasks
          ?.map((t) => t.text || '') || []

      // 2. Gọi AI Suggestion Service thời gian thực
      const suggestions = await speakingService.getSmartSuggestions({
        lastAiMessage: lastAiMsg,
        scenarioTitle: session?.title || session?.scenario?.title || 'Luyện nói tiếng Nhật',
        jlptLevel: session?.scenario?.jlptLevel || 'N4',
        incompleteTasks,
      })

      if (suggestions && suggestions.length > 0) {
        setSuggestionList(suggestions)
        setCurrentSugIndex(0)
      } else if (session?.scenario?.sampleDialogue && session.scenario.sampleDialogue.length > 0) {
        // Fallback sang câu mẫu kịch bản nếu có
        const userLines = session.scenario.sampleDialogue
          .filter((l) => l.speaker === 'USER' || l.role === 'USER')
          .map((l) => ({
            ja: l.text || l.ja || '',
            vi: l.meaning || l.vi || '',
          }))
          .filter((item) => !!item.ja)
        if (userLines.length > 0) {
          setSuggestionList(userLines)
          setCurrentSugIndex(0)
        }
      }
    } catch (err) {
      console.warn('Lỗi lấy AI suggestions:', err)
    } finally {
      setIsLoadingSuggestions(false)
    }
  }

  const formatTime = (dateStr?: string) => {
    if (!dateStr) return '19:27'
    try {
      const d = new Date(dateStr)
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
    } catch {
      return '19:27'
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F0F2F5] flex flex-col items-center justify-center p-4">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-3" />
        <p className="text-slate-600 font-semibold text-sm">Đang tải phòng luyện nói cùng Maziiko...</p>
      </div>
    )
  }

  if (error || !session) {
    return (
      <div className="min-h-screen bg-[#F0F2F5] flex flex-col items-center justify-center p-4">
        <p className="text-slate-600 mb-4 font-semibold">{error || 'Không tìm thấy phiên luyện nói.'}</p>
        <button
          onClick={() => navigate('/speaking')}
          className="px-5 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs shadow-xs"
        >
          Quay lại danh sách
        </button>
      </div>
    )
  }

  const isScenario = session.mode === 'SCENARIO'
  const situationText = session.scenario?.description || (isScenario ? session.title : 'Trò chuyện tự do cùng Maziiko')

  return (
    <div className="min-h-screen bg-[#F0F2F5] text-slate-800 flex flex-col justify-between relative">
      {/* ==================================================================== */}
      {/* 1. Header Mazii: < Quay lại | Tiêu đề | ⚙️ Cài đặt */}
      {/* ==================================================================== */}
      <div className="bg-white border-b border-slate-200/90 sticky top-0 z-30 shadow-xs">
        <div className="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/speaking')}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition shadow-2xs"
            >
              <ChevronLeft className="w-4 h-4 text-slate-500" />
              <span>Quay lại</span>
            </button>
            <h1 className="text-base sm:text-lg font-bold text-slate-800 truncate max-w-xs sm:max-w-md">
              {session.title || 'Gọi món tại quán'}
            </h1>
          </div>

          <div className="relative">
            <button
              onClick={() => setShowSettings((prev) => !prev)}
              className={`p-2 rounded-xl transition ${
                showSettings
                  ? 'bg-blue-50 text-blue-600'
                  : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'
              }`}
              title="Cài đặt phòng nói"
            >
              <Settings2 className="w-5 h-5" />
            </button>

            {/* ============================================================== */}
            {/* Popover Cài đặt (1:1 Screenshot 1) */}
            {/* ============================================================== */}
            {showSettings && (
              <div className="absolute right-0 top-12 w-80 sm:w-88 bg-white rounded-2xl p-5 shadow-2xl border border-slate-100 z-50 space-y-4 animate-in fade-in zoom-in-95">
                {/* Hiện Romaji */}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-700">Hiện Romaji</span>
                  <button
                    type="button"
                    onClick={handleToggleRomaji}
                    className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors ${
                      showRomaji ? 'bg-blue-600' : 'bg-slate-300'
                    }`}
                  >
                    <div
                      className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                        showRomaji ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Hiện Furigana */}
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-700">Hiện Furigana</span>
                  <button
                    type="button"
                    onClick={handleToggleFurigana}
                    className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors ${
                      showFurigana ? 'bg-blue-600' : 'bg-slate-300'
                    }`}
                  >
                    <div
                      className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                        showFurigana ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>

                {/* Tốc độ đọc */}
                <div className="space-y-2">
                  <div className="text-sm font-semibold text-slate-700">Tốc độ đọc</div>
                  <div className="flex items-center gap-3">
                    <span className="text-lg">🐢</span>
                    <input
                      type="range"
                      min="0.6"
                      max="1.4"
                      step="0.1"
                      value={playbackSpeed}
                      onChange={(e) => setPlaybackSpeed(parseFloat(e.target.value))}
                      className="flex-1 accent-blue-600 h-1.5 bg-slate-200 rounded-lg cursor-pointer"
                    />
                    <span className="text-lg">🐇</span>
                  </div>
                  <div className="text-center text-xs font-semibold text-slate-500">
                    Tốc độ: x{playbackSpeed.toFixed(1)}
                  </div>
                </div>

                {/* Giọng đọc AI */}
                <div className="space-y-1.5 pt-1 border-t border-slate-100">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold text-slate-700">Giọng đọc AI</span>
                    <span className="text-xs text-blue-600 font-medium">
                      {selectedVoice === 'female' ? 'Nữ (Nanami)' : 'Nam (Keita)'}
                    </span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => handleSelectVoice('female')}
                      className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl border text-xs font-semibold transition cursor-pointer ${
                        selectedVoice === 'female'
                          ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-xs'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <span>👩 Nữ (Nanami)</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSelectVoice('male')}
                      className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl border text-xs font-semibold transition cursor-pointer ${
                        selectedVoice === 'male'
                          ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-xs'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <span>👨 Nam (Keita)</span>
                    </button>
                  </div>
                </div>

                {/* Footer buttons: Báo lỗi | Đặt lại | Áp dụng */}
                <div className="flex items-center justify-between pt-3 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => alert('Cảm ơn bạn! Đội ngũ sẽ tiếp nhận góp ý.')}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-[11px] font-semibold text-slate-700 transition"
                  >
                    <AlertTriangle className="w-3.5 h-3.5 text-slate-500" />
                    <span>Báo lỗi / Góp ý</span>
                  </button>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        setShowRomaji(true)
                        setShowFurigana(true)
                        setPlaybackSpeed(1.0)
                      }}
                      className="px-2.5 py-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800"
                    >
                      Đặt lại
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowSettings(false)}
                      className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-xs transition"
                    >
                      Áp dụng
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* 2. Khung trò chuyện trung tâm */}
      {/* ==================================================================== */}
      <div className="flex-1 max-w-3xl w-full mx-auto px-4 py-6 space-y-5">
        {/* Bong bóng giới thiệu ngữ cảnh tình huống (Screenshot 1: Tôi cùng một người bạn...) */}
        {situationText && (
          <div className="flex justify-center mb-6">
            <div className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-blue-50/90 border border-blue-200/80 text-xs sm:text-sm text-slate-700 font-medium shadow-2xs text-center max-w-xl">
              <span className="text-blue-600 font-bold text-base">💬</span>
              <span>{situationText}</span>
            </div>
          </div>
        )}

        {/* Danh sách các tin nhắn */}
        {messages.map((msg, index) => {
          const isAI = msg.role === 'AI'
          const time = formatTime(msg.createdAt)
          const isTranslated = !!translationsVisible[index]

          if (!isAI) {
            // Tin nhắn của người học (Căn phải)
            return (
              <div key={index} className="flex justify-end items-start gap-2.5 animate-in fade-in">
                <div className="max-w-[85%] sm:max-w-[75%] rounded-2xl rounded-tr-xs bg-white border border-slate-200/90 p-4 shadow-xs text-left">
                  <div className="flex items-center justify-between mb-1.5 text-xs text-slate-400">
                    <span className="font-semibold text-slate-700">Bạn</span>
                    <span>{time}</span>
                  </div>

                  <FuriganaText
                    text={msg.textJa}
                    showRomaji={showRomaji}
                    showFurigana={showFurigana}
                    size="md"
                    className="text-slate-800"
                  />

                  {/* Bản dịch tiếng Việt nếu bật */}
                  {isTranslated && (
                    <div className="mt-2 pt-2 border-t border-slate-100 text-xs sm:text-sm text-indigo-700 font-medium">
                      {translatedTexts[index] || msg.textVi || 'Đang dịch...'}
                    </div>
                  )}

                  {/* Thanh công cụ nhỏ */}
                  <div className="flex items-center gap-1.5 mt-2.5 pt-2 border-t border-slate-100 text-slate-500">
                    <button
                      type="button"
                      onClick={() => playTts(msg.textJa)}
                      className="p-1.5 rounded-lg hover:bg-slate-100 transition"
                      title="Phát âm"
                    >
                      <Volume2 className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleCopy(index, msg.textJa)}
                      className="p-1.5 rounded-lg hover:bg-slate-100 transition"
                      title="Sao chép"
                    >
                      {copiedId === index ? (
                        <Check className="w-4 h-4 text-emerald-600" />
                      ) : (
                        <Copy className="w-4 h-4" />
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleToggleTranslate(index, msg.textJa)}
                      className="p-1.5 rounded-lg hover:bg-slate-100 transition"
                      title="Dịch nghĩa"
                    >
                      <Languages className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            )
          }

          // Tin nhắn của Maziiko AI (Căn trái 1:1 Screenshot 1)
          return (
            <div key={index} className="flex justify-start items-start gap-3 animate-in fade-in">
              {/* Avatar mèo Maziiko */}
              <div className="shrink-0 mt-1">
                <MaziikoMascot size={36} />
              </div>

              <div className="max-w-[85%] sm:max-w-[80%] rounded-2xl rounded-tl-xs bg-white border border-slate-200/90 p-4 shadow-xs text-left space-y-2">
                {/* Tên & thời gian */}
                <div className="flex items-center gap-2 text-xs">
                  <span className="font-bold text-slate-800">Maziiko</span>
                  <span className="text-slate-400">{time}</span>
                </div>

                {/* Nội dung câu tiếng Nhật có Furigana & Romaji */}
                <FuriganaText
                  text={msg.textJa}
                  showRomaji={showRomaji}
                  showFurigana={showFurigana}
                  size="md"
                  className="text-slate-800"
                />

                {/* Hiệu ứng nhấp nháy khi đang stream */}
                {msg.isStreaming && (
                  <span className="inline-block w-2 h-4 ml-1 bg-indigo-600 animate-pulse rounded-xs" />
                )}

                {/* Bản dịch tiếng Việt nếu bật */}
                {isTranslated && (
                  <div className="mt-2 pt-2 border-t border-slate-100 text-xs sm:text-sm text-indigo-700 font-medium">
                    {translatedTexts[index] || msg.textVi || 'Đang dịch...'}
                  </div>
                )}

                {/* Sửa lỗi ngữ pháp nếu có */}
                {msg.correction && <GrammarCorrectionCard correction={msg.correction} />}

                {/* Thanh công cụ 3 icon: 🔊 Loa | 📋 Copy | 🌐 Dịch (Screenshot 1) */}
                <div className="flex items-center gap-1 mt-2.5 pt-2 border-t border-slate-100 text-slate-500">
                  <button
                    type="button"
                    onClick={() => playTts(msg.textJa)}
                    className="p-1.5 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition"
                    title="Phát âm câu này"
                  >
                    <Volume2 className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={() => handleCopy(index, msg.textJa)}
                    className="p-1.5 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition"
                    title="Sao chép văn bản"
                  >
                    {copiedId === index ? (
                      <Check className="w-4 h-4 text-emerald-600" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => handleToggleTranslate(index, msg.textJa)}
                    className={`p-1.5 rounded-lg transition ${
                      isTranslated
                        ? 'bg-indigo-50 text-indigo-600'
                        : 'hover:bg-slate-100 hover:text-slate-800 text-slate-500'
                    }`}
                    title="Dịch câu sang tiếng Việt"
                  >
                    <Languages className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          )
        })}

        <div ref={messagesEndRef} />
      </div>

      {/* ==================================================================== */}
      {/* 3. Thanh điều khiển nổi phía dưới (Screenshot 1) */}
      {/* ==================================================================== */}
      <div className="sticky bottom-0 z-20 pb-4 px-4 bg-gradient-to-t from-[#F0F2F5] via-[#F0F2F5]/90 to-transparent pt-3">
        <div className="max-w-2xl mx-auto flex flex-col items-center gap-3">
          {/* Hàng 3 nút tính năng nổi: 🎯 Nhiệm vụ | ⚡ Dịch nhanh | 🤖 Gợi ý */}
          <div className="flex items-center gap-2">
            {isScenario && session.scenario?.tasks && session.scenario.tasks.length > 0 && (
              <button
                type="button"
                onClick={() => setShowTasksModal(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200/90 hover:border-indigo-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-xs transition"
              >
                <Target className="w-3.5 h-3.5 text-indigo-600" />
                <span>Nhiệm vụ</span>
                <span className="text-[10px] text-slate-400">
                  ({session.completedTasks?.length || 0}/{session.scenario.tasks.length})
                </span>
              </button>
            )}

            <button
              type="button"
              onClick={handleQuickTranslateLastMessage}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200/90 hover:border-indigo-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-xs transition"
            >
              <Zap className="w-3.5 h-3.5 text-amber-500" />
              <span>Dịch nhanh</span>
            </button>

            <button
              type="button"
              disabled={isLoadingSuggestions}
              onClick={handleOpenSuggestions}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200/90 hover:border-indigo-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-xs transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoadingSuggestions ? (
                <div className="w-3.5 h-3.5 border-2 border-purple-600 border-t-transparent rounded-full animate-spin" />
              ) : (
                <Lightbulb className="w-3.5 h-3.5 text-purple-600" />
              )}
              <span>{isLoadingSuggestions ? 'Đang gợi ý...' : 'Gợi ý'}</span>
            </button>
          </div>

          {/* Thẻ Gợi ý trực quan inline phía trên thanh nhập liệu (1:1 Screenshot 2) */}
          {showInlineSuggestion && (
            <div className="w-full bg-white rounded-2xl p-4 border border-slate-200/90 shadow-md space-y-3 animate-in fade-in slide-in-from-bottom-2">
              {isLoadingSuggestions ? (
                <div className="flex items-center gap-3 py-2 text-indigo-600 text-sm font-medium">
                  <div className="w-4 h-4 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin shrink-0" />
                  <span>AI đang phân tích câu nói và gợi ý phản hồi phù hợp...</span>
                </div>
              ) : suggestionList.length > 0 ? (
                <>
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1 pr-2">
                      <div className="text-slate-800 font-semibold text-sm sm:text-base leading-snug">
                        {suggestionList[currentSugIndex].ja}
                      </div>
                      {suggestionList[currentSugIndex].vi && (
                        <div className="text-slate-500 text-xs sm:text-sm">
                          {suggestionList[currentSugIndex].vi}
                        </div>
                      )}
                    </div>

                    {/* Nút gửi câu gợi ý này */}
                    <button
                      type="button"
                      disabled={isProcessing}
                      onClick={() => {
                        handleSendMessage(suggestionList[currentSugIndex].ja)
                        setShowInlineSuggestion(false)
                      }}
                      className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center shrink-0 shadow-md hover:scale-105 transition disabled:opacity-40"
                      title="Gửi câu gợi ý này cho AI"
                    >
                      <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                    </button>
                  </div>

              {/* Toolbar nhỏ: Phát âm & Sao chép */}
              <div className="flex items-center gap-1.5 pt-1 text-slate-500">
                <button
                  type="button"
                  onClick={() => playTts(suggestionList[currentSugIndex].ja)}
                  className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-600 transition"
                  title="Nghe phát âm"
                >
                  <Volume2 className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() => handleCopy('suggestion', suggestionList[currentSugIndex].ja)}
                  className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-600 transition"
                  title="Sao chép"
                >
                  {copiedId === 'suggestion' ? (
                    <Check className="w-4 h-4 text-emerald-600" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
                </button>
              </div>

              {/* Dòng ngăn cách: Gợi ý ----------- Đóng */}
              <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs text-slate-400">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-slate-700">Gợi ý</span>
                  {suggestionList.length > 1 && (
                    <span className="text-[11px] text-slate-400">
                      ({currentSugIndex + 1}/{suggestionList.length})
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-3">
                  {suggestionList.length > 1 && (
                    <button
                      type="button"
                      onClick={() =>
                        setCurrentSugIndex((prev) => (prev + 1) % suggestionList.length)
                      }
                      className="text-indigo-600 hover:text-indigo-700 font-medium"
                    >
                      Câu khác
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => setShowInlineSuggestion(false)}
                    className="hover:text-slate-700 font-medium underline"
                  >
                    Đóng
                  </button>
                </div>
              </div>
            </>
          ) : null}
        </div>
      )}

          {/* Khung nhập liệu: Khi đang thu âm hoặc có chữ vừa nói thì hiển thị Thẻ Thu Âm (1:1 Screenshot 2) */}
          {isListening || !!spokenText ? (
            <div className="w-full bg-white rounded-2xl p-4 border border-slate-200/90 shadow-md space-y-3 animate-in fade-in slide-in-from-bottom-2">
              {/* Dòng chữ nhận diện trực tiếp khi nói */}
              <div className="text-slate-800 text-sm sm:text-base font-medium min-h-[32px] select-text">
                {spokenText || (
                  <span className="text-slate-400 italic">
                    {isListening ? 'Đang lắng nghe bạn nói...' : 'Bấm mic để nói tiếp...'}
                  </span>
                )}
              </div>

              {/* Hàng điều khiển: [✕]   [Soundwave Bars]   [■ / Mic] [↑] */}
              <div className="flex items-center justify-between pt-1">
                {/* Nút hủy X */}
                <button
                  type="button"
                  onClick={handleCancelSpeech}
                  className="w-9 h-9 rounded-full hover:bg-slate-100 text-slate-500 flex items-center justify-center transition"
                  title="Hủy câu nói"
                >
                  <X className="w-5 h-5" />
                </button>

                {/* Sóng âm thanh Soundwave Visualizer */}
                <div className="flex items-center gap-1 px-2 sm:px-4">
                  {[10, 16, 22, 14, 26, 18, 12, 24, 16, 28, 20, 14, 22, 16, 10, 24, 18, 12, 26, 16, 10].map((h, i) => (
                    <span
                      key={i}
                      className={`w-1 rounded-full transition-all duration-150 ${
                        isListening ? 'bg-indigo-400/80 animate-pulse' : 'bg-slate-200'
                      }`}
                      style={{
                        height: isListening ? `${h}px` : '6px',
                        animationDelay: `${(i * 60) % 500}ms`,
                      }}
                    />
                  ))}
                </div>

                {/* Các nút bên phải: Dừng nói (Square) & Gửi (ArrowUp) */}
                <div className="flex items-center gap-2">
                  {/* Nút Dừng nói hoặc tiếp tục nói */}
                  {isListening ? (
                    <button
                      type="button"
                      onClick={stopListening}
                      className="w-9 h-9 rounded-xl bg-slate-900 hover:bg-slate-800 text-white flex items-center justify-center transition shadow-xs"
                      title="Dừng ghi âm"
                    >
                      <Square className="w-4 h-4 fill-white text-white rounded-xs" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={handleResumeListening}
                      className="w-9 h-9 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 flex items-center justify-center transition"
                      title="Tiếp tục nói"
                    >
                      <Mic className="w-4 h-4" />
                    </button>
                  )}

                  {/* Nút Gửi (Mũi tên lên tím-xanh) */}
                  <button
                    type="button"
                    disabled={isProcessing || !spokenText.trim()}
                    onClick={handleSendSpeech}
                    className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 disabled:opacity-40 disabled:cursor-not-allowed shrink-0"
                    title="Gửi câu nói cho AI"
                  >
                    <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                  </button>
                </div>
              </div>
            </div>
          ) : (
            /* Thanh Capsule bình thường khi ở chế độ chờ hoặc gõ phím */
            <div className="w-full bg-white rounded-full p-2 pl-3.5 pr-2 border border-slate-200/90 shadow-md flex items-center justify-between gap-3">
              {/* Nút bàn phím */}
              <button
                type="button"
                onClick={() => setIsKeyboardMode((prev) => !prev)}
                className={`p-2 rounded-full transition ${
                  isKeyboardMode
                    ? 'bg-purple-100 text-purple-700'
                    : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100'
                }`}
                title="Chuyển sang gõ chữ"
              >
                <Keyboard className="w-5 h-5" />
              </button>

              {/* Input hoặc Placeholder giọng nói */}
              {isKeyboardMode ? (
                <input
                  type="text"
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && inputText.trim()) {
                      handleSendMessage(inputText)
                      setInputText('')
                    }
                  }}
                  placeholder="Hãy thử gõ câu trả lời..."
                  autoFocus
                  className="flex-1 bg-transparent border-none text-sm text-slate-800 placeholder:text-slate-400 focus:outline-hidden"
                />
              ) : (
                <div
                  onClick={() => {
                    resetTranscript()
                    startListening()
                  }}
                  className="flex-1 text-center text-xs sm:text-sm text-slate-500 cursor-pointer select-none truncate px-2"
                >
                  {speechError ? (
                    <span className="text-rose-500 font-medium text-xs px-2" title={speechError}>
                      ⚠️ {speechError}
                    </span>
                  ) : (
                    'Hãy thử nói gì đó'
                  )}
                </div>
              )}

              {/* Nút hành động bên phải */}
              {isKeyboardMode ? (
                <button
                  type="button"
                  disabled={isProcessing || !inputText.trim()}
                  onClick={() => {
                    if (inputText.trim()) {
                      handleSendMessage(inputText)
                      setInputText('')
                    }
                  }}
                  className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                  title="Gửi câu trả lời"
                >
                  {isProcessing ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                  )}
                </button>
              ) : (
                <button
                  type="button"
                  disabled={isProcessing}
                  onClick={() => {
                    resetTranscript()
                    startListening()
                  }}
                  className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 shrink-0"
                  title="Bật microphone để nói"
                >
                  {isProcessing ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <Mic className="w-5 h-5" />
                  )}
                </button>
              )}
            </div>
          )}

          {/* Dòng chữ disclaimer nhỏ */}
          <div className="text-[11px] text-slate-400 font-medium">
            Được tạo ra bởi AI · Có thể có sai sót
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* Modal: Danh sách nhiệm vụ */}
      {/* ==================================================================== */}
      {showTasksModal && session.scenario?.tasks && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl relative space-y-4">
            <button
              onClick={() => setShowTasksModal(false)}
              className="absolute top-4 right-4 p-1 rounded-full hover:bg-slate-100 text-slate-400"
            >
              <X className="w-5 h-5" />
            </button>
            <h3 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <Target className="w-5 h-5 text-indigo-600" />
              <span>Nhiệm vụ trong tình huống</span>
            </h3>
            <ul className="space-y-3 pt-2">
              {session.scenario.tasks.map((task: ScenarioTask) => {
                const isCompleted = session.completedTasks?.includes(task.id)
                return (
                  <li
                    key={task.id}
                    className={`flex items-start gap-2.5 p-3 rounded-xl border transition ${
                      isCompleted
                        ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                        : 'bg-slate-50 border-slate-200 text-slate-700'
                    }`}
                  >
                    <CheckCircle2
                      className={`w-5 h-5 mt-0.5 shrink-0 ${
                        isCompleted ? 'text-emerald-600' : 'text-slate-300'
                      }`}
                    />
                    <span className="text-xs sm:text-sm font-medium">{task.text}</span>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>
      )}
    </div>
  )
}

export default SpeakingPracticePage
