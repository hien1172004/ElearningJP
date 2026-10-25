import React, { useState } from 'react'
import { Volume2, Languages, Loader2, Sparkles, User, RotateCcw } from 'lucide-react'
import { FuriganaText } from './FuriganaText'
import { GrammarCorrectionCard } from './GrammarCorrectionCard'
import { MaziikoMascot } from './MaziikoMascot'
import { speakingService } from '@/services/speaking.service'
import type { AiMessage } from '@/types/speaking.types'

interface SpeakingChatBubbleProps {
  message: AiMessage
  aiRoleName?: string
  onRepeatPrompt?: (text: string) => void
  showRomaji?: boolean
}

export const SpeakingChatBubble: React.FC<SpeakingChatBubbleProps> = ({
  message,
  aiRoleName = 'Maziiko',
  onRepeatPrompt,
  showRomaji = true,
}) => {
  const isAI = message.role === 'AI'
  const [isPlayingAudio, setIsPlayingAudio] = useState(false)
  const [isTranslating, setIsTranslating] = useState(false)
  const [showTranslation, setShowTranslation] = useState(Boolean(message.textVi))
  const [translatedText, setTranslatedText] = useState(message.textVi || '')
  const [audioUrl, setAudioUrl] = useState(message.audioUrl || '')

  const handlePlayAudio = async () => {
    if (isPlayingAudio) return

    try {
      setIsPlayingAudio(true)

      let targetUrl = audioUrl
      if (!targetUrl) {
        // Gọi backend sinh file TTS
        const ttsRes = await speakingService.generateTts(message.textJa, 'ja', message.id)
        targetUrl = ttsRes.audioUrl
        setAudioUrl(targetUrl)
      }

      // Format URL nếu là relative path
      const fullUrl = targetUrl.startsWith('http')
        ? targetUrl
        : `${import.meta.env.VITE_API_BASE_URL?.replace('/api/v1', '') || 'http://localhost:8080'}${targetUrl}`

      const audio = new Audio(fullUrl)
      audio.onended = () => setIsPlayingAudio(false)
      audio.onerror = () => {
        setIsPlayingAudio(false)
        // Fallback sang Web Speech Synthesis của trình duyệt nếu file audio máy chủ gặp sự cố mạng
        if ('speechSynthesis' in window) {
          const utter = new SpeechSynthesisUtterance(message.textJa)
          utter.lang = 'ja-JP'
          window.speechSynthesis.speak(utter)
        }
      }
      await audio.play()
    } catch (err) {
      console.warn('Lỗi phát âm thanh:', err)
      setIsPlayingAudio(false)
      if ('speechSynthesis' in window) {
        const utter = new SpeechSynthesisUtterance(message.textJa)
        utter.lang = 'ja-JP'
        window.speechSynthesis.speak(utter)
      }
    }
  }

  const handleToggleTranslate = async () => {
    if (showTranslation) {
      setShowTranslation(false)
      return
    }

    if (translatedText) {
      setShowTranslation(true)
      return
    }

    try {
      setIsTranslating(true)
      const res = await speakingService.translate(message.textJa, message.id)
      setTranslatedText(res.translatedText)
      setShowTranslation(true)
    } catch (err) {
      console.warn('Lỗi dịch tin nhắn:', err)
    } finally {
      setIsTranslating(false)
    }
  }

  if (!isAI) {
    // Tin nhắn của người học (User - Căn phải)
    return (
      <div className="flex justify-end items-start gap-2.5 my-3 animate-fade-in">
        <div className="max-w-[85%] sm:max-w-[75%] rounded-2xl rounded-tr-xs bg-indigo-600 text-white px-4 py-3 shadow-md shadow-indigo-500/10 text-left">
          <div className="text-sm font-medium text-indigo-100 mb-1 flex items-center justify-end gap-1">
            <span>Bạn</span>
          </div>
          <FuriganaText
            text={message.textJa}
            showRomaji={showRomaji}
            size="md"
            className="text-white"
          />
        </div>
        <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0 shadow-xs border border-indigo-200">
          <User className="w-4 h-4" />
        </div>
      </div>
    )
  }

  // Tin nhắn của AI (Căn trái)
  return (
    <div className="flex justify-start items-start gap-2.5 my-3.5 animate-fade-in group">
      <div className="shrink-0 mt-0.5">
        <MaziikoMascot size={32} />
      </div>

      <div className="max-w-[85%] sm:max-w-[80%] rounded-2xl rounded-tl-xs bg-white border border-slate-200/80 px-4 py-3 shadow-xs hover:border-indigo-200 transition-all text-left">
        {/* Tên AI / Nhân vật đóng vai */}
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-xs font-semibold text-indigo-600 tracking-wide flex items-center gap-1">
            {aiRoleName}
          </span>
        </div>

        {/* Nội dung câu nói tiếng Nhật (kèm Furigana & Romaji) */}
        <FuriganaText
          text={message.textJa}
          showRomaji={showRomaji}
          size="md"
          className="text-slate-800"
        />

        {/* Hiệu ứng con trỏ nhấp nháy khi đang stream từng từ */}
        {message.isStreaming && (
          <span className="inline-block w-2 h-4 ml-1 bg-indigo-600 animate-pulse rounded-xs" />
        )}

        {/* Khung bản dịch tiếng Việt khi bấm Xem nghĩa */}
        {showTranslation && translatedText && (
          <div className="mt-2.5 pt-2 border-t border-slate-100 text-slate-600 text-xs sm:text-sm font-normal bg-slate-50/70 p-2 rounded-lg">
            🇻🇳 {translatedText}
          </div>
        )}

        {/* Khung sửa lỗi ngữ pháp (nếu người học nói câu trước đó có lỗi) */}
        {message.correction && <GrammarCorrectionCard correction={message.correction} />}

        {/* Thanh công cụ tương tác: Phát âm, Dịch, Luyện nói lại */}
        <div className="flex items-center gap-2 mt-3 pt-2 border-t border-slate-100 text-slate-500">
          <button
            onClick={handlePlayAudio}
            disabled={isPlayingAudio}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
              isPlayingAudio
                ? 'bg-indigo-50 text-indigo-600 font-semibold'
                : 'hover:bg-slate-100 hover:text-slate-700 text-slate-500'
            }`}
            title="Nghe phát âm tiếng Nhật chuẩn"
          >
            {isPlayingAudio ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-600" />
            ) : (
              <Volume2 className="w-3.5 h-3.5" />
            )}
            <span>{isPlayingAudio ? 'Đang đọc...' : 'Phát âm'}</span>
          </button>

          <button
            onClick={handleToggleTranslate}
            disabled={isTranslating}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
              showTranslation
                ? 'bg-amber-50 text-amber-700 font-semibold'
                : 'hover:bg-slate-100 hover:text-slate-700 text-slate-500'
            }`}
            title="Dịch câu sang tiếng Việt"
          >
            {isTranslating ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-amber-600" />
            ) : (
              <Languages className="w-3.5 h-3.5" />
            )}
            <span>{showTranslation ? 'Ẩn nghĩa' : 'Xem nghĩa'}</span>
          </button>

          {onRepeatPrompt && (
            <button
              onClick={() => onRepeatPrompt(message.textJa)}
              className="flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium hover:bg-slate-100 hover:text-slate-700 text-slate-500 transition-all"
              title="Điền câu này vào ô nói để luyện tập nhại lại (Shadowing)"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Luyện câu này</span>
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
