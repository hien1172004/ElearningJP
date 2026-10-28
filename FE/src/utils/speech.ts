/**
 * Module âm thanh Hybrid cho ElearningJP:
 * 1. Ưu tiên phát file âm thanh MP3 từ hệ thống (Cloudinary / Server audio_url).
 * 2. Tự động kết nối AI Edge-TTS (ja-JP-NanamiNeural / ja-JP-KeitaNeural) cho giọng nói tự nhiên chuẩn người bản xứ.
 * 3. Tự động chuyển sang Web Speech API tiếng Nhật (ja-JP) chất lượng cao (Natural/Online) nếu offline.
 */

const AI_API_URL = import.meta.env.VITE_AI_API_URL || 'http://localhost:8000'

let currentAudio: HTMLAudioElement | null = null
let currentUtterance: SpeechSynthesisUtterance | null = null
void currentUtterance

export interface PlayAudioOptions {
  text?: string          // Chữ tiếng Nhật để đọc (AI TTS / Web Speech)
  audioUrl?: string      // File âm thanh MP3 từ hệ thống (Cloudinary / server URL)
  voice?: 'female' | 'male' | string // Giọng đọc: 'female' (Nanami) hoặc 'male' (Keita)
  onEnd?: () => void     // Callback khi phát xong
  rate?: number          // Tốc độ đọc (mặc định 0.9)
}

/**
 * Chọn giọng tiếng Nhật tự nhiên nhất trong trình duyệt (ưu tiên giọng Online / Natural / Google)
 */
export const getBestJapaneseVoice = (): SpeechSynthesisVoice | null => {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) return null

  const voices = window.speechSynthesis.getVoices()
  const jaVoices = voices.filter((v) => v.lang.startsWith('ja') || v.lang.includes('JP'))
  if (jaVoices.length === 0) return null

  // 1. Ưu tiên các giọng Neural / Natural / Online (Microsoft Nanami / Keita Online)
  const naturalVoice = jaVoices.find(
    (v) =>
      v.name.includes('Natural') ||
      v.name.includes('Online') ||
      v.name.includes('Neural') ||
      v.name.includes('Nanami') ||
      v.name.includes('Keita')
  )
  if (naturalVoice) return naturalVoice

  // 2. Ưu tiên giọng Google tiếng Nhật (trên Chrome)
  const googleVoice = jaVoices.find((v) => v.name.includes('Google') || v.name.includes('日本語'))
  if (googleVoice) return googleVoice

  // 3. Giọng tiếng Nhật đầu tiên sẵn có
  return jaVoices[0]
}

/**
 * Dừng mọi âm thanh đang phát (cả file MP3 hệ thống, AI TTS lẫn giọng đọc Web Speech)
 */
export const stopSpeaking = (): void => {
  if (currentAudio) {
    currentAudio.pause()
    currentAudio.currentTime = 0
    currentAudio = null
  }

  if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
    if (currentUtterance || window.speechSynthesis.speaking) {
      window.speechSynthesis.cancel()
    }
    currentUtterance = null
  }
}

/**
 * Phát âm thanh thông minh:
 * Ưu tiên 1: File MP3 hệ thống (nếu có audioUrl)
 * Ưu tiên 2: Dịch vụ AI Edge-TTS (giọng Nanami/Keita chuẩn người thật)
 * Dự phòng: Web Speech API (đã lọc giọng Natural)
 */
export const playSmartAudio = ({
  text,
  audioUrl,
  voice = 'female',
  onEnd,
  rate = 0.9,
}: PlayAudioOptions): void => {
  stopSpeaking()

  // 1. Nếu có file âm thanh của hệ thống (audio_url hợp lệ)
  if (audioUrl && audioUrl.trim().length > 0) {
    try {
      const audio = new Audio(audioUrl)
      audio.playbackRate = rate
      currentAudio = audio

      audio.onended = () => {
        currentAudio = null
        onEnd?.()
      }

      audio.onerror = () => {
        console.warn(`[Audio] File hệ thống lỗi (${audioUrl}), tự động chuyển sang AI TTS / Web Speech.`)
        currentAudio = null
        playAiOrFallback(text, voice, onEnd, rate)
      }

      audio.play().catch((err) => {
        console.warn('[Audio] Không thể phát audio hệ thống:', err)
        playAiOrFallback(text, voice, onEnd, rate)
      })
      return
    } catch (e) {
      console.warn('[Audio] Khởi tạo Audio thất bại:', e)
    }
  }

  // 2. Phát qua AI Edge-TTS -> Dự phòng Web Speech API
  playAiOrFallback(text, voice, onEnd, rate)
}

/**
 * Phát qua AI Edge-TTS service, tự fallback sang Web Speech nếu máy chủ AI offline
 */
const playAiOrFallback = (text?: string, voice = 'female', onEnd?: () => void, rate = 0.9): void => {
  if (!text || !text.trim()) {
    onEnd?.()
    return
  }

  const cleanText = text.trim()
  const aiVoiceParam = voice === 'male' || voice === 'keita' ? 'ja-JP-KeitaNeural' : 'ja-JP-NanamiNeural'
  const aiTtsUrl = `${AI_API_URL}/api/v1/tts/synthesize?text=${encodeURIComponent(cleanText)}&voice=${aiVoiceParam}`

  try {
    const audio = new Audio(aiTtsUrl)
    audio.playbackRate = rate
    currentAudio = audio

    audio.onended = () => {
      currentAudio = null
      onEnd?.()
    }

    audio.onerror = () => {
      console.warn('[Audio] Dịch vụ AI TTS không phản hồi, tự động dùng Web Speech trình duyệt.')
      currentAudio = null
      fallbackSpeech(cleanText, onEnd, rate)
    }

    audio.play().catch((err) => {
      console.warn('[Audio] Trình duyệt chặn tự động phát AI TTS:', err)
      currentAudio = null
      fallbackSpeech(cleanText, onEnd, rate)
    })
  } catch (err) {
    console.warn('[Audio] Lỗi phát AI TTS:', err)
    fallbackSpeech(cleanText, onEnd, rate)
  }
}

/**
 * Phát âm tiếng Nhật qua Web Speech API (Dự phòng với thuật toán chọn giọng tối ưu)
 */
export const fallbackSpeech = (text: string, onEnd?: () => void, rate = 0.9): void => {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    console.warn('Trình duyệt không hỗ trợ Web Speech API.')
    onEnd?.()
    return
  }

  try {
    window.speechSynthesis.cancel()

    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'ja-JP'
    utterance.rate = rate
    utterance.pitch = 1.0

    const bestVoice = getBestJapaneseVoice()
    if (bestVoice) {
      utterance.voice = bestVoice
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
  } catch (err) {
    console.warn('Lỗi khởi tạo Web Speech:', err)
    onEnd?.()
  }
}

/**
 * Hàm gọi nhanh tương thích ngược
 */
export const speakJapanese = (text: string, onEnd?: () => void, rate = 0.9): void => {
  playSmartAudio({ text, onEnd, rate })
}

/**
 * Khử trùng lặp câu nói giọng nói (Deduplicate Speech Transcripts).
 * Khắc phục triệt để lỗi Web Speech API / microphone thu âm bị nhân đôi câu
 * (Ví dụ: "こんにちは。こんにちは。" -> "こんにちは。")
 */
export const cleanDuplicateSpeech = (rawText: string): string => {
  if (!rawText) return ''
  let text = rawText.trim()
  if (!text) return ''

  // 1. Kiểm tra lặp đôi nguyên văn toàn bộ chuỗi (Half string duplicate)
  // Ví dụ: "こんにちは。こんにちは。" hoặc "こんにちは こんにちは" hoặc "A。A。"
  const len = text.length
  if (len >= 4) {
    for (let split = Math.floor(len / 2) - 2; split <= Math.floor(len / 2) + 2; split++) {
      if (split <= 0 || split >= len) continue
      const part1 = text.slice(0, split).trim().replace(/[。！？!?.,\s]+$/, '')
      const part2 = text.slice(split).trim().replace(/[。！？!?.,\s]+$/, '')
      if (part1 && part1 === part2) {
        const punctuation = text.slice(split).match(/[。！？!?]+$/)?.[0] || '。'
        return part1 + punctuation
      }
    }
  }

  // 2. Khử trùng lặp các câu liên tiếp theo dấu ngắt câu Nhật/Quốc tế
  const segments = text.split(/([。！？!?\n]+)/)
  const cleanedParts: string[] = []
  let lastNormSentence = ''

  for (let i = 0; i < segments.length; i += 2) {
    const sentence = (segments[i] || '').trim()
    const punct = segments[i + 1] || ''
    if (!sentence) continue

    const norm = sentence.replace(/[\s\u3000]+/g, '').toLowerCase()
    if (norm && norm === lastNormSentence) {
      continue
    }

    lastNormSentence = norm
    cleanedParts.push(sentence + (punct || (i + 2 < segments.length ? '。' : '')))
  }

  if (cleanedParts.length > 0) {
    text = cleanedParts.join('')
  }

  // 3. Khử trùng lặp từ/cụm từ ngắn phân cách bằng khoảng trắng
  const tokens = text.split(/\s+/)
  if (tokens.length >= 2) {
    const dedupedTokens: string[] = []
    for (let i = 0; i < tokens.length; i++) {
      const cur = tokens[i].trim()
      const prev = dedupedTokens[dedupedTokens.length - 1]
      const curNorm = cur.replace(/[。！？!?.,]/g, '')
      const prevNorm = prev ? prev.replace(/[。！？!?.,]/g, '') : ''
      if (curNorm && curNorm === prevNorm) {
        continue
      }
      dedupedTokens.push(cur)
    }
    text = dedupedTokens.join(' ')
  }

  return text.trim()
}

