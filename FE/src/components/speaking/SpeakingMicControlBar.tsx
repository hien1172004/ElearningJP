import React, { useState, useEffect } from 'react'
import { Mic, MicOff, Send, Loader2, Keyboard } from 'lucide-react'
import { useSpeechRecognition } from '@/hooks/useSpeechRecognition'

interface SpeakingMicControlBarProps {
  onSendMessage: (message: string) => void
  disabled?: boolean
  isProcessing?: boolean
  initialPromptText?: string
}

export const SpeakingMicControlBar: React.FC<SpeakingMicControlBarProps> = ({
  onSendMessage,
  disabled = false,
  isProcessing = false,
  initialPromptText = '',
}) => {
  const [inputText, setInputText] = useState('')
  const [isKeyboardMode, setIsKeyboardMode] = useState(false)

  const {
    isListening,
    transcript,
    interimTranscript,
    error: speechError,
    isSupported,
    startListening,
    stopListening,
    resetTranscript,
    setTranscript,
  } = useSpeechRecognition({
    lang: 'ja-JP',
  })

  // Nếu cha truyền initialPromptText (ví dụ bấm nút 'Luyện câu này'), điền vào text
  useEffect(() => {
    if (initialPromptText) {
      setInputText(initialPromptText)
      setTranscript(initialPromptText)
      setIsKeyboardMode(true)
    }
  }, [initialPromptText, setTranscript])

  // Cập nhật inputText theo transcript ghi âm
  useEffect(() => {
    if (transcript) {
      setInputText(transcript)
    }
  }, [transcript])

  const handleToggleMic = () => {
    if (disabled || isProcessing) return

    if (isListening) {
      stopListening()
    } else {
      resetTranscript()
      setInputText('')
      startListening()
    }
  }

  const handleSend = () => {
    const textToSend = (inputText || transcript).trim()
    if (!textToSend || disabled || isProcessing) return

    if (isListening) {
      stopListening()
    }

    onSendMessage(textToSend)
    setInputText('')
    resetTranscript()
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="bg-white/95 backdrop-blur-md border-t border-slate-200/90 p-4 sticky bottom-0 z-20 shadow-lg">
      <div className="max-w-3xl mx-auto">
        {/* Banner thông báo trạng thái hoặc lỗi microphone */}
        {speechError && (
          <div className="mb-3 px-3 py-1.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg text-center animate-fade-in flex items-center justify-center gap-1.5">
            <span>⚠️ {speechError}</span>
            <button
              onClick={() => setIsKeyboardMode(true)}
              className="underline font-semibold hover:text-rose-900 ml-1"
            >
              Chuyển sang gõ chữ
            </button>
          </div>
        )}

        {/* Trực tiếp hiển thị những gì người dùng đang nói */}
        {(isListening || interimTranscript) && (
          <div className="mb-3 p-3 bg-indigo-50/80 border border-indigo-200/60 rounded-xl text-center animate-fade-in">
            <div className="flex items-center justify-center gap-2 text-indigo-700 text-xs font-semibold mb-1">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-500"></span>
              </span>
              <span>Đang lắng nghe giọng tiếng Nhật của bạn...</span>
            </div>
            <p className="text-sm sm:text-base font-medium text-slate-800 tracking-wide min-h-[24px]">
              {transcript || interimTranscript || 'Hãy nói vào micro...'}
            </p>
          </div>
        )}

        {/* Chế độ nhập chữ bàn phím */}
        {isKeyboardMode ? (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setIsKeyboardMode(false)}
              className="p-2.5 rounded-xl border border-slate-200 text-slate-500 hover:text-indigo-600 hover:bg-slate-50 transition-all"
              title="Chuyển sang chế độ nói bằng micro"
            >
              <Mic className="w-5 h-5" />
            </button>

            <div className="relative flex-1">
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Nhập câu tiếng Nhật của bạn (ví dụ: こんにちは)..."
                disabled={disabled || isProcessing}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 text-sm sm:text-base outline-none transition-all pr-10"
                autoFocus
              />
            </div>

            <button
              type="button"
              onClick={handleSend}
              disabled={!inputText.trim() || disabled || isProcessing}
              className="px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-200 disabled:text-slate-400 text-white font-medium text-sm flex items-center gap-1.5 transition-all shadow-md shadow-indigo-500/20"
            >
              {isProcessing ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Send className="w-4 h-4" />
              )}
              <span className="hidden sm:inline">Gửi</span>
            </button>
          </div>
        ) : (
          /* Chế độ Micro chính */
          <div className="flex flex-col items-center justify-center">
            <div className="flex items-center gap-4 sm:gap-6">
              {/* Nút bật gõ phím */}
              <button
                type="button"
                onClick={() => setIsKeyboardMode(true)}
                className="p-3 rounded-full border border-slate-200 text-slate-500 hover:text-indigo-600 hover:bg-slate-50 transition-all shadow-xs"
                title="Gõ chữ thay vì nói"
              >
                <Keyboard className="w-5 h-5" />
              </button>

              {/* Nút Micro lớn ở trung tâm */}
              <div className="relative flex items-center justify-center">
                {isListening && (
                  <>
                    <div className="absolute w-20 h-20 rounded-full bg-rose-400/30 animate-ping pointer-events-none" />
                    <div className="absolute w-16 h-16 rounded-full bg-rose-300/40 animate-pulse pointer-events-none" />
                  </>
                )}

                <button
                  type="button"
                  onClick={handleToggleMic}
                  disabled={disabled || isProcessing || !isSupported}
                  className={`relative z-10 w-16 h-16 rounded-full flex items-center justify-center shadow-lg transition-all transform active:scale-95 ${
                    isListening
                      ? 'bg-rose-500 text-white shadow-rose-500/40 hover:bg-rose-600 scale-105'
                      : isProcessing
                      ? 'bg-slate-300 text-slate-500 cursor-not-allowed shadow-none'
                      : 'bg-indigo-600 text-white shadow-indigo-600/30 hover:bg-indigo-700 hover:scale-105'
                  }`}
                  title={
                    isListening
                      ? 'Bấm để dừng ghi âm và gửi'
                      : isProcessing
                      ? 'Đang xử lý...'
                      : 'Bấm để bắt đầu nói tiếng Nhật'
                  }
                >
                  {isProcessing ? (
                    <Loader2 className="w-7 h-7 animate-spin" />
                  ) : isListening ? (
                    <MicOff className="w-7 h-7 animate-bounce" />
                  ) : (
                    <Mic className="w-7 h-7" />
                  )}
                </button>
              </div>

              {/* Nút gửi khi có transcript */}
              {transcript.trim() ? (
                <button
                  type="button"
                  onClick={handleSend}
                  disabled={disabled || isProcessing}
                  className="p-3 rounded-full bg-indigo-600 text-white hover:bg-indigo-700 transition-all shadow-md shadow-indigo-600/20"
                  title="Gửi câu vừa nói"
                >
                  <Send className="w-5 h-5" />
                </button>
              ) : (
                <div className="w-11 h-11" /> // Placeholder giữ cân xứng
              )}
            </div>

            {/* Hướng dẫn ngắn bên dưới nút mic */}
            <div className="mt-2.5 text-xs text-slate-500 font-medium">
              {isListening ? (
                <span className="text-rose-600 font-semibold animate-pulse">
                  Đang ghi âm... Bấm lại nút mic khi nói xong
                </span>
              ) : isProcessing ? (
                <span className="text-indigo-600 font-medium flex items-center gap-1 justify-center">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" /> AI đang trả lời...
                </span>
              ) : (
                <span>Bấm Micro và nói câu tiếng Nhật của bạn</span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
