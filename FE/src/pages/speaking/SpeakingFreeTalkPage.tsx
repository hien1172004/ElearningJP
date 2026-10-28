import React, { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ChevronLeft,
  Settings2,
  FileText,
  Sparkles,
  Keyboard,
  Mic,
  X,
  AlertTriangle,
  ArrowUp,
  Square,
} from 'lucide-react'
import { MaziikoMascot } from '@/components/speaking/MaziikoMascot'
import { speakingService } from '@/services/speaking.service'
import { useAuthStore } from '@/stores'
import { useSpeechRecognition, cleanDuplicateSpeech } from '@/hooks/useSpeechRecognition'

const FREE_TALK_TOPICS = [
  { id: 'anime', text: '好きなアニメ', sub: 'Anime yêu thích' },
  { id: 'phrases', text: 'よく使うフレーズ', sub: 'Cụm từ hay dùng' },
  { id: 'weekend', text: '私の週末', sub: 'Cuối tuần của tôi' },
  { id: 'food', text: '好きな食べ物', sub: 'Món ăn yêu thích' },
  { id: 'today', text: '今日あったこと', sub: 'Chuyện xảy ra hôm nay' },
]

export const SpeakingFreeTalkPage: React.FC = () => {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const username = user?.fullName || 'bạn'

  const [inputText, setInputText] = useState('')
  const [isKeyboardMode, setIsKeyboardMode] = useState(false)
  const [isStartingSession, setIsStartingSession] = useState(false)
  const [showSettingsPopover, setShowSettingsPopover] = useState(false)
  const [showRomaji, setShowRomaji] = useState<boolean>(() => {
    const saved = localStorage.getItem('speaking_show_romaji')
    return saved !== null ? saved === 'true' : true
  })
  const [showFurigana, setShowFurigana] = useState<boolean>(() => {
    const saved = localStorage.getItem('speaking_show_furigana')
    return saved !== null ? saved === 'true' : true
  })
  const [playbackSpeed, setPlaybackSpeed] = useState(1.0)
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

  // Speech Recognition hook: silenceTimeoutMs = 0 (tắt tự gửi)
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

  const handleSendSpeech = () => {
    const textToSend = cleanDuplicateSpeech(spokenText)
    if (!textToSend || isStartingRef.current) return
    stopListening()
    resetTranscript()
    handleStartWithInput(textToSend)
  }

  const handleCancelSpeech = () => {
    cancelListening()
  }

  const handleResumeListening = () => {
    startListening(true)
  }

  const isStartingRef = useRef(false)

  // Bắt đầu phiên trò chuyện tự do với chủ đề được chọn
  const handleStartSession = async (topic: string, initialUserText?: string) => {
    if (isStartingRef.current || isStartingSession) return
    isStartingRef.current = true
    try {
      setIsStartingSession(true)
      const session = await speakingService.createSession({
        mode: 'FREE',
        topic: topic || '日常会話',
      })

      if (session && session.id) {
        if (initialUserText && initialUserText.trim()) {
          sessionStorage.setItem(`pending_message_${session.id}`, initialUserText.trim())
        }
        navigate(`/speaking/session/${session.id}`)
      } else {
        throw new Error('Không nhận được mã phiên từ máy chủ')
      }
    } catch (err) {
      console.error('Lỗi khởi tạo phiên trò chuyện tự do:', err)
      alert('Không thể kết nối với AI. Vui lòng thử lại!')
    } finally {
      setIsStartingSession(false)
      isStartingRef.current = false
    }
  }

  const handleStartWithInput = (text: string) => {
    const cleaned = cleanDuplicateSpeech(text).trim()
    if (!cleaned) return
    handleStartSession('日常会話', cleaned)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && inputText.trim()) {
      handleStartWithInput(inputText)
    }
  }

  return (
    <div className="min-h-screen bg-[#F0F2F5] text-slate-800 flex flex-col justify-between">
      {/* ==================================================================== */}
      {/* 1. Header Mazii: < Quay lại | Trò chuyện tự do | ⚙️ 📋 */}
      {/* ==================================================================== */}
      <div className="bg-white border-b border-slate-200/90 sticky top-0 z-30 shadow-xs">
        <div className="max-w-[1550px] mx-auto px-4 sm:px-8 py-3 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/speaking')}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition shadow-2xs"
            >
              <ChevronLeft className="w-4 h-4 text-slate-500" />
              <span>Quay lại</span>
            </button>
            <h1 className="text-base sm:text-lg font-bold text-slate-800">
              Trò chuyện tự do
            </h1>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <button
                onClick={() => setShowSettingsPopover((prev) => !prev)}
                className={`p-2 rounded-xl transition ${
                  showSettingsPopover
                    ? 'bg-blue-50 text-blue-600'
                    : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'
                }`}
                title="Cài đặt"
              >
                <Settings2 className="w-5 h-5" />
              </button>

              {/* Popover Cài đặt 1:1 Mazii Screenshot */}
              {showSettingsPopover && (
                <div className="absolute right-0 top-12 w-80 sm:w-88 bg-white rounded-2xl p-5 shadow-2xl border border-slate-100 z-50 space-y-4 animate-in fade-in zoom-in-95 text-left">
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

                  {/* Footer buttons: Báo lỗi / Góp ý | Đặt lại | Áp dụng */}
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
                        onClick={() => setShowSettingsPopover(false)}
                        className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-xs transition"
                      >
                        Áp dụng
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <button
              className="p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition"
              title="Ghi chú & Báo cáo"
            >
              <FileText className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* 2. Nội dung trung tâm: Mèo Maziiko + Lời chào tím + Gợi ý chủ đề */}
      {/* ==================================================================== */}
      <div className="max-w-4xl mx-auto px-4 w-full flex-1 flex flex-col items-center justify-center py-10 text-center">
        {/* Mèo Maziiko với hào quang vũ trụ */}
        <div className="mb-6">
          <MaziikoMascot size={110} showAura={true} showBadge={true} />
        </div>

        {/* Lời chào màu tím đặc trưng của Mazii */}
        <h2 className="text-base sm:text-xl font-bold text-[#8B5CF6] max-w-xl leading-relaxed mb-8">
          Chào {username}! Hôm nay của bạn thế nào? Kể cho Maziiko nghe bằng tiếng Nhật nhé
        </h2>

        {/* Hàng chủ đề gợi ý */}
        <div className="w-full max-w-3xl">
          <div className="text-left text-xs font-semibold text-slate-400 mb-3 px-1">
            Chủ đề gợi ý
          </div>

          <div className="flex items-stretch gap-3 overflow-x-auto pb-3 scrollbar-none">
            {FREE_TALK_TOPICS.map((topic) => (
              <button
                key={topic.id}
                type="button"
                onClick={() => handleStartSession(topic.text)}
                disabled={isStartingSession}
                className="flex flex-col items-start p-4 rounded-2xl bg-white border border-slate-200/90 hover:border-purple-300 hover:shadow-md transition-all text-left min-w-[140px] sm:min-w-[160px] group shrink-0"
              >
                <div className="w-7 h-7 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                  <Sparkles className="w-4 h-4 fill-purple-600" />
                </div>
                <div className="font-bold text-xs sm:text-sm text-slate-800 group-hover:text-purple-600 transition-colors">
                  {topic.text}
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* 3. Capsule Input nổi phía dưới màn hình (1:1 Screenshot 2) */}
      {/* ==================================================================== */}
      <div className="sticky bottom-0 z-20 pb-4 px-4 bg-gradient-to-t from-[#F0F2F5] via-[#F0F2F5]/90 to-transparent pt-4">
        <div className="max-w-2xl mx-auto flex flex-col items-center">
          {/* Capsule Container */}
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

          {/* Card thu âm trực tiếp hoặc Capsule thông thường */}
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
                    disabled={isStartingSession || !spokenText.trim()}
                    onClick={handleSendSpeech}
                    className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 disabled:opacity-40 disabled:cursor-not-allowed shrink-0"
                    title="Bắt đầu trò chuyện"
                  >
                    <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                  </button>
                </div>
              </div>
            </div>
          ) : (
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
                  onKeyDown={handleKeyDown}
                  placeholder="Hãy thử gõ câu bạn muốn nói..."
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

              {/* Các nút hành động bên phải */}
              {isKeyboardMode ? (
                <button
                  type="button"
                  disabled={isStartingSession || !inputText.trim()}
                  onClick={() => handleStartWithInput(inputText)}
                  className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                  title="Bắt đầu trò chuyện"
                >
                  {isStartingSession ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                  )}
                </button>
              ) : (
                <button
                  type="button"
                  disabled={isStartingSession}
                  onClick={() => {
                    resetTranscript()
                    startListening()
                  }}
                  className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 shrink-0"
                  title="Bật microphone để nói"
                >
                  {isStartingSession ? (
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <Mic className="w-5 h-5" />
                  )}
                </button>
              )}
            </div>
          )}
          </div>

          {/* Dòng chữ disclaimer nhỏ */}
          <div className="text-[11px] text-slate-400 mt-2 font-medium">
            Được tạo ra bởi AI · Có thể có sai sót
          </div>
        </div>
      </div>
    </div>
  )
}

export default SpeakingFreeTalkPage
