import { useState, useEffect, useRef, useCallback } from 'react'

interface SpeechRecognitionHookOptions {
  lang?: string
  continuous?: boolean
  interimResults?: boolean
  onResult?: (transcript: string) => void
  onError?: (error: string) => void
}

interface IWindowWithSpeech extends Window {
  SpeechRecognition?: any
  webkitSpeechRecognition?: any
}

export function useSpeechRecognition({
  lang = 'ja-JP',
  continuous = false,
  interimResults = true,
  onResult,
  onError,
}: SpeechRecognitionHookOptions = {}) {
  const [isListening, setIsListening] = useState(false)
  const [transcript, setTranscript] = useState('')
  const [interimTranscript, setInterimTranscript] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSupported, setIsSupported] = useState(true)

  const recognitionRef = useRef<any>(null)

  useEffect(() => {
    const win = window as IWindowWithSpeech
    const SpeechRecognitionClass = win.SpeechRecognition || win.webkitSpeechRecognition

    if (!SpeechRecognitionClass) {
      setIsSupported(false)
      setError('Trình duyệt của bạn chưa hỗ trợ nhận diện giọng nói Web Speech API.')
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
        setError(null)
      }

      recognition.onresult = (event: any) => {
        let currentInterim = ''
        let finalResult = ''

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const res = event.results[i]
          const text = res[0]?.transcript || ''
          if (res.isFinal) {
            finalResult += text
          } else {
            currentInterim += text
          }
        }

        if (finalResult) {
          setTranscript((prev) => {
            const next = prev ? `${prev} ${finalResult}`.trim() : finalResult.trim()
            if (onResult) onResult(next)
            return next
          })
          setInterimTranscript('')
        } else {
          setInterimTranscript(currentInterim)
        }
      }

      recognition.onerror = (event: any) => {
        let msg = 'Đã xảy ra sự cố khi nhận diện giọng nói.'
        switch (event.error) {
          case 'not-allowed':
          case 'service-not-allowed':
            msg = 'Bạn đã từ chối quyền truy cập Micro. Hãy cấp quyền trong cài đặt trình duyệt để nói.'
            break
          case 'no-speech':
            msg = 'Không nghe thấy giọng nói. Hãy nói to và gần micro hơn.'
            break
          case 'network':
            msg = 'Lỗi kết nối mạng nhận diện giọng nói.'
            break
          case 'audio-capture':
            msg = 'Không tìm thấy thiết bị thu âm (Microphone).'
            break
        }
        setError(msg)
        setIsListening(false)
        if (onError) onError(msg)
      }

      recognition.onend = () => {
        setIsListening(false)
      }

      recognitionRef.current = recognition
    } catch (e: any) {
      setIsSupported(false)
      setError(`Không thể khởi tạo Web Speech: ${e.message}`)
    }

    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort()
        } catch (_) {}
      }
    }
  }, [lang, continuous, interimResults, onResult, onError])

  const startListening = useCallback(() => {
    if (!recognitionRef.current) return
    setError(null)
    setInterimTranscript('')
    try {
      recognitionRef.current.start()
    } catch (e: any) {
      // Nếu recognition đang chạy thì dừng và chạy lại
      if (e.name === 'InvalidStateError') {
        recognitionRef.current.stop()
        setTimeout(() => {
          try {
            recognitionRef.current.start()
          } catch (_) {}
        }, 100)
      }
    }
  }, [])

  const stopListening = useCallback(() => {
    if (!recognitionRef.current) return
    try {
      recognitionRef.current.stop()
    } catch (_) {}
  }, [])

  const resetTranscript = useCallback(() => {
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
    resetTranscript,
    setTranscript,
  }
}
