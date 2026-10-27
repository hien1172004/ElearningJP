import { useState, useEffect, useRef, useCallback } from 'react'
import { cleanDuplicateSpeech } from '@/utils/speech'

export { cleanDuplicateSpeech }

export interface SpeechRecognitionHookOptions {
  lang?: string
  continuous?: boolean
  interimResults?: boolean
  silenceTimeoutMs?: number // 0 = tắt tự gửi (người dùng tự bấm gửi thủ công)
  onResult?: (transcript: string) => void
  onError?: (error: string) => void
}

interface IWindowWithSpeech extends Window {
  SpeechRecognition?: any
  webkitSpeechRecognition?: any
}

export function useSpeechRecognition({
  lang = 'ja-JP',
  continuous = true,
  interimResults = true,
  silenceTimeoutMs = 0, // Mặc định 0: không tự động gửi, người dùng kiểm tra câu nói rồi tự bấm gửi
  onResult,
  onError,
}: SpeechRecognitionHookOptions = {}) {
  const [isListening, setIsListening] = useState(false)
  const [transcript, setTranscript] = useState('')
  const [interimTranscript, setInterimTranscript] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSupported, setIsSupported] = useState(true)

  const recognitionRef = useRef<any>(null)
  const onResultRef = useRef(onResult)
  const onErrorRef = useRef(onError)
  const silenceTimerRef = useRef<any>(null)
  const fullTranscriptRef = useRef<string>('')
  const prefixTextRef = useRef<string>('')
  const isListeningRef = useRef<boolean>(false)

  useEffect(() => {
    onResultRef.current = onResult
  }, [onResult])

  useEffect(() => {
    onErrorRef.current = onError
  }, [onError])

  useEffect(() => {
    const win = typeof window !== 'undefined' ? (window as IWindowWithSpeech) : ({} as IWindowWithSpeech)
    const SpeechRecognitionClass = win.SpeechRecognition || win.webkitSpeechRecognition

    if (!SpeechRecognitionClass) {
      setIsSupported(false)
      setError('Trình duyệt của bạn chưa hỗ trợ nhận diện giọng nói Web Speech (Khuyên dùng Chrome/Edge).')
      return
    }

    try {
      const recognition = new SpeechRecognitionClass()
      recognition.lang = lang
      recognition.continuous = continuous
      recognition.interimResults = interimResults
      recognition.maxAlternatives = 1

      recognition.onstart = () => {
        setIsListening(true)
        isListeningRef.current = true
        setError(null)
      }

      recognition.onresult = (event: any) => {
        let finalAccumulated = ''
        let interimAccumulated = ''

        for (let i = 0; i < event.results.length; i++) {
          const res = event.results[i]
          const text = res[0]?.transcript || ''
          if (res.isFinal) {
            finalAccumulated += text
          } else {
            interimAccumulated += text
          }
        }

        // Khử trùng lặp: Nếu interimTranscript trùng hoặc là đuôi của finalAccumulated thì xóa interim
        const finalTrimmed = finalAccumulated.trim()
        const interimTrimmed = interimAccumulated.trim()
        if (
          interimTrimmed &&
          (finalTrimmed.endsWith(interimTrimmed) ||
            interimTrimmed === finalTrimmed ||
            finalTrimmed.includes(interimTrimmed))
        ) {
          interimAccumulated = ''
        }

        const prefix = prefixTextRef.current ? prefixTextRef.current + ' ' : ''
        const rawFull = (prefix + finalAccumulated + (interimAccumulated ? ' ' + interimAccumulated : '')).trim()
        const currentFull = cleanDuplicateSpeech(rawFull)
        fullTranscriptRef.current = currentFull
        setTranscript(cleanDuplicateSpeech((prefix + finalAccumulated).trim()))
        setInterimTranscript(interimAccumulated.trim())

        // Xóa timer nếu có
        if (silenceTimerRef.current) {
          clearTimeout(silenceTimerRef.current)
          silenceTimerRef.current = null
        }

        // Chế độ single-shot (continuous = false)
        if (!continuous && finalAccumulated.trim()) {
          const text = (prefix + finalAccumulated).trim()
          try {
            recognition.stop()
          } catch (_) {}
          setIsListening(false)
          isListeningRef.current = false
          if (onResultRef.current) {
            onResultRef.current(text)
          }
          return
        }

        // Chỉ hẹn giờ tự gửi nếu silenceTimeoutMs > 0
        if (continuous && currentFull && silenceTimeoutMs > 0) {
          silenceTimerRef.current = setTimeout(() => {
            const textToSend = fullTranscriptRef.current.trim()
            if (textToSend) {
              try {
                recognition.stop()
              } catch (_) {}
              setIsListening(false)
              isListeningRef.current = false
              if (onResultRef.current) {
                onResultRef.current(textToSend)
              }
            }
          }, silenceTimeoutMs)
        }
      }

      recognition.onerror = (event: any) => {
        if (silenceTimerRef.current) {
          clearTimeout(silenceTimerRef.current)
          silenceTimerRef.current = null
        }

        let msg = 'Đã xảy ra sự cố khi nhận diện giọng nói.'
        switch (event.error) {
          case 'not-allowed':
          case 'service-not-allowed':
            msg = 'Trình duyệt chưa được cấp quyền Microphone. Hãy bấm vào biểu tượng Micro/Ổ khóa trên thanh địa chỉ để Cho phép.'
            break
          case 'no-speech':
            // Khi bật continuous, nếu chưa nói gì thì không báo lỗi ngắt
            if (!fullTranscriptRef.current) {
              return
            }
            msg = 'Chưa nghe thấy giọng nói rõ ràng.'
            break
          case 'network':
            msg = 'Lỗi kết nối mạng nhận diện giọng nói (Google Speech Service).'
            break
          case 'audio-capture':
            msg = 'Không tìm thấy thiết bị thu âm (Microphone).'
            break
          default:
            msg = `Lỗi nhận diện: ${event.error}`
            break
        }
        setError(msg)
        setIsListening(false)
        isListeningRef.current = false
        if (onErrorRef.current) onErrorRef.current(msg)
      }

      recognition.onend = () => {
        if (!silenceTimerRef.current) {
          setIsListening(false)
          isListeningRef.current = false
        }
      }

      recognitionRef.current = recognition
    } catch (e: any) {
      setIsSupported(false)
      setError(`Không thể khởi tạo Web Speech: ${e.message}`)
    }

    return () => {
      if (silenceTimerRef.current) {
        clearTimeout(silenceTimerRef.current)
        silenceTimerRef.current = null
      }
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort()
        } catch (_) {}
      }
    }
  }, [lang, continuous, interimResults, silenceTimeoutMs])

  const startListening = useCallback(async (append = false) => {
    setError(null)
    if (!append) {
      prefixTextRef.current = ''
      fullTranscriptRef.current = ''
      setTranscript('')
      setInterimTranscript('')
    } else {
      prefixTextRef.current = fullTranscriptRef.current
      setInterimTranscript('')
    }

    if (silenceTimerRef.current) {
      clearTimeout(silenceTimerRef.current)
      silenceTimerRef.current = null
    }

    // 1. Kiểm tra trình duyệt
    const win = typeof window !== 'undefined' ? (window as IWindowWithSpeech) : ({} as IWindowWithSpeech)
    const SpeechRecognitionClass = win.SpeechRecognition || win.webkitSpeechRecognition
    if (!SpeechRecognitionClass) {
      const msg = 'Trình duyệt không hỗ trợ nhận diện giọng nói. Vui lòng mở trang web trên Google Chrome hoặc Microsoft Edge.'
      setError(msg)
      if (onErrorRef.current) onErrorRef.current(msg)
      return
    }

    // 2. Chủ động yêu cầu quyền truy cập Micro nếu cần
    if (navigator?.mediaDevices?.getUserMedia) {
      try {
        await navigator.mediaDevices.getUserMedia({ audio: true })
      } catch (err: any) {
        const msg = 'Trình duyệt chưa được cấp quyền Microphone. Vui lòng cho phép quyền Micro trên thanh địa chỉ trình duyệt để luyện nói!'
        setError(msg)
        if (onErrorRef.current) onErrorRef.current(msg)
        return
      }
    }

    // 3. Khởi chạy bộ nhận diện
    if (!recognitionRef.current) return

    try {
      isListeningRef.current = true
      recognitionRef.current.start()
    } catch (e: any) {
      if (e.name === 'InvalidStateError') {
        try {
          recognitionRef.current.stop()
        } catch (_) {}
        setTimeout(() => {
          try {
            isListeningRef.current = true
            recognitionRef.current.start()
          } catch (_) {}
        }, 150)
      } else {
        console.warn('Lỗi bật nhận diện giọng nói:', e)
      }
    }
  }, [])

  // Dừng nghe và gửi ngay lập tức nội dung đã nhận diện
  const stopAndSend = useCallback(() => {
    if (silenceTimerRef.current) {
      clearTimeout(silenceTimerRef.current)
      silenceTimerRef.current = null
    }

    const textToSend = fullTranscriptRef.current.trim()
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop()
      } catch (_) {}
    }
    setIsListening(false)
    isListeningRef.current = false

    if (textToSend && onResultRef.current) {
      onResultRef.current(textToSend)
    }
  }, [])

  // Hủy phiên nghe và xóa dữ liệu đã nói (không gửi)
  const cancelListening = useCallback(() => {
    if (silenceTimerRef.current) {
      clearTimeout(silenceTimerRef.current)
      silenceTimerRef.current = null
    }
    prefixTextRef.current = ''
    fullTranscriptRef.current = ''
    setTranscript('')
    setInterimTranscript('')
    if (recognitionRef.current) {
      try {
        recognitionRef.current.abort()
      } catch (_) {}
    }
    setIsListening(false)
    isListeningRef.current = false
  }, [])

  const stopListening = useCallback(() => {
    if (silenceTimerRef.current) {
      clearTimeout(silenceTimerRef.current)
      silenceTimerRef.current = null
    }
    if (!recognitionRef.current) return
    try {
      recognitionRef.current.stop()
    } catch (_) {}
    setIsListening(false)
    isListeningRef.current = false
  }, [])

  const resetTranscript = useCallback(() => {
    if (silenceTimerRef.current) {
      clearTimeout(silenceTimerRef.current)
      silenceTimerRef.current = null
    }
    prefixTextRef.current = ''
    fullTranscriptRef.current = ''
    setTranscript('')
    setInterimTranscript('')
    setError(null)
  }, [])

  return {
    isListening,
    transcript,
    interimTranscript,
    error,
    isSupported,
    startListening,
    stopListening,
    stopAndSend,
    cancelListening,
    resetTranscript,
    setTranscript,
  }
}
