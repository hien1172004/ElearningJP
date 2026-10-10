/**
 * Module âm thanh Hybrid cho ElearningJP:
 * 1. Ưu tiên phát file âm thanh MP3 từ hệ thống (Cloudinary / Server audio_url).
 * 2. Tự động chuyển sang Web Speech API tiếng Nhật (ja-JP) nếu không có audio_url hoặc file audio lỗi.
 */

let currentAudio: HTMLAudioElement | null = null
let currentUtterance: SpeechSynthesisUtterance | null = null

export interface PlayAudioOptions {
  text?: string          // Chữ tiếng Nhật để đọc (dùng khi fallback Web Speech)
  audioUrl?: string      // File âm thanh MP3 từ hệ thống (Cloudinary / server URL)
  onEnd?: () => void     // Callback khi phát xong
  rate?: number          // Tốc độ đọc (mặc định 0.9)
}

/**
 * Dừng mọi âm thanh đang phát (cả file MP3 hệ thống lẫn giọng đọc Web Speech)
 */
export const stopSpeaking = (): void => {
  if (currentAudio) {
    currentAudio.pause()
    currentAudio.currentTime = 0
    currentAudio = null
  }

  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    window.speechSynthesis.cancel()
    currentUtterance = null
  }
}

/**
 * Phát âm thanh thông minh: Ưu tiên file MP3 hệ thống -> Dự phòng Web Speech
 */
export const playSmartAudio = ({
  text,
  audioUrl,
  onEnd,
  rate = 0.9,
}: PlayAudioOptions): void => {
  stopSpeaking()

  // 1. Nếu có file âm thanh của hệ thống (audio_url hợp lệ)
  if (audioUrl && audioUrl.trim().length > 0) {
    try {
      const audio = new Audio(audioUrl)
      currentAudio = audio

      audio.onended = () => {
        currentAudio = null
        onEnd?.()
      }

      audio.onerror = () => {
        console.warn(`[Audio] File hệ thống lỗi (${audioUrl}), tự động chuyển sang Web Speech dự phòng.`)
        currentAudio = null
        if (text) {
          fallbackSpeech(text, onEnd, rate)
        } else {
          onEnd?.()
        }
      }

      audio.play().catch((err) => {
        console.warn('[Audio] Không thể phát audio hệ thống:', err)
        if (text) {
          fallbackSpeech(text, onEnd, rate)
        } else {
          onEnd?.()
        }
      })
      return
    } catch (e) {
      console.warn('[Audio] Khởi tạo Audio thất bại:', e)
    }
  }

  // 2. Dự phòng: Phát bằng Web Speech API tiếng Nhật
  if (text) {
    fallbackSpeech(text, onEnd, rate)
  } else {
    onEnd?.()
  }
}

/**
 * Phát âm tiếng Nhật qua Web Speech API
 */
const fallbackSpeech = (text: string, onEnd?: () => void, rate = 0.9): void => {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    console.warn('Trình duyệt không hỗ trợ Web Speech API.')
    onEnd?.()
    return
  }

  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'ja-JP'
  utterance.rate = rate
  utterance.pitch = 1.0

  const voices = window.speechSynthesis.getVoices()
  const japaneseVoice = voices.find((v) => v.lang.startsWith('ja') || v.lang.includes('JP'))
  if (japaneseVoice) {
    utterance.voice = japaneseVoice
  }

  utterance.onend = () => {
    currentUtterance = null
    onEnd?.()
  }

  utterance.onerror = (e) => {
    console.warn('Lỗi phát âm Web Speech:', e)
    currentUtterance = null
    onEnd?.()
  }

  currentUtterance = utterance
  window.speechSynthesis.speak(utterance)
}

/**
 * Hàm gọi nhanh tương thích ngược
 */
export const speakJapanese = (text: string, onEnd?: () => void, rate = 0.9): void => {
  playSmartAudio({ text, onEnd, rate })
}
