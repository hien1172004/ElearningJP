import React, { useState, useEffect, useRef, useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  LayoutGrid,
  Sparkles,
  Play,
  Square,
  Eye,
  EyeOff,
  Volume2,
  Languages,
  Mic,
  Check,
  CheckCircle2,
  Lightbulb,
  X,
  ChevronRight,
  ChevronLeft,
  RotateCcw,
  Loader2,
  Settings2,
  Copy,
  Target,
  Zap,
  Keyboard,
  ArrowUp,
} from 'lucide-react'
import { toast } from 'sonner'
import { speakingService } from '@/services/speaking.service'
import { FuriganaText } from '@/components/speaking/FuriganaText'
import { GrammarCorrectionCard } from '@/components/speaking/GrammarCorrectionCard'
import { MaziikoMascot } from '@/components/speaking/MaziikoMascot'
import { useSpeechRecognition, cleanDuplicateSpeech } from '@/hooks/useSpeechRecognition'
import { playSmartAudio, getBestJapaneseVoice } from '@/utils'
import type { LessonSpeakingScenario } from '@/types/course.types'
import type { AiScenario, AiMessage, AiSession, SampleDialogueLine, ScenarioTask } from '@/types/speaking.types'

type ActiveTab = 'dialogue' | 'vocabulary' | 'grammar' | 'tips'

interface SpeakingStudyViewProps {
  lessonId: number
  scenario?: LessonSpeakingScenario
  scenarios?: LessonSpeakingScenario[]
  previewMode?: boolean
  onSectionDone?: () => void
  onChatModeChange?: (active: boolean) => void
}

/** Chấm điểm tương đồng ngữ âm tiếng Nhật */
function calculateSimilarity(str1: string, str2: string): number {
  const s1 = str1.replace(/[\s\p{P}、。！？]/gu, '').toLowerCase()
  const s2 = str2.replace(/[\s\p{P}、。！？]/gu, '').toLowerCase()
  if (!s1 || !s2) return 0
  if (s1 === s2) return 100

  let matches = 0
  for (const char of s2) {
    if (s1.includes(char)) matches++
  }
  const ratio = matches / Math.max(s1.length, 1)
  const score = Math.min(100, Math.round(ratio * 100))
  return Math.max(score, s2.length > 0 ? 55 : 0)
}

/** Màu sắc badge tương ứng từng cấp độ JLPT tổng quát N5 -> N1 */
function getJlptBadgeColor(level?: string): string {
  switch (level?.toUpperCase()) {
    case 'N1':
      return 'bg-rose-600 text-white'
    case 'N2':
      return 'bg-purple-600 text-white'
    case 'N3':
      return 'bg-amber-600 text-white'
    case 'N4':
      return 'bg-sky-600 text-white'
    case 'N5':
    default:
      return 'bg-emerald-600 text-white'
  }
}

export const SpeakingStudyView: React.FC<SpeakingStudyViewProps> = ({
  lessonId,
  scenario,
  scenarios,
  previewMode = false,
  onSectionDone,
  onChatModeChange,
}) => {
  const queryClient = useQueryClient()
  // Danh sách kịch bản của bài học
  const scenarioList = useMemo(
    () => (scenarios && scenarios.length > 0 ? scenarios : scenario ? [scenario] : []),
    [scenarios, scenario],
  )
  const [activeScenarioIndex, setActiveScenarioIndex] = useState(0)
  const rawScenario = scenarioList[activeScenarioIndex] || scenarioList[0] || scenario

  // Tải chi tiết kịch bản đầy đủ từ speakingService (đảm bảo đủ từ vựng, ngữ pháp, mẹo, hội thoại mẫu)
  const [fullScenario, setFullScenario] = useState<AiScenario | null>(null)
  const [isLoadingScenario, setIsLoadingScenario] = useState(false)

  useEffect(() => {
    if (!rawScenario?.scenarioId) return
    let isMounted = true
    setIsLoadingScenario(true)
    speakingService
      .getScenarioById(rawScenario.scenarioId)
      .then((data) => {
        if (isMounted) setFullScenario(data)
      })
      .catch((err) => {
        console.warn('Lỗi tải chi tiết kịch bản:', err)
      })
      .finally(() => {
        if (isMounted) setIsLoadingScenario(false)
      })

    return () => {
      isMounted = false
    }
  }, [rawScenario?.scenarioId])

  // Gộp dữ liệu kịch bản giữa lesson payload & full API data
  const currentScenario = useMemo(() => {
    return {
      ...rawScenario,
      ...fullScenario,
      title: fullScenario?.title || rawScenario?.title || 'Luyện nói tình huống',
      description: fullScenario?.description || rawScenario?.description || '',
      userRole: fullScenario?.userRole || rawScenario?.userRole || 'Người học',
      aiRole: fullScenario?.aiRole || rawScenario?.aiRole || 'Đối tác AI',
      jlptLevel: fullScenario?.jlptLevel || rawScenario?.jlptLevel || 'N5',
      tasks: (fullScenario?.tasks && fullScenario.tasks.length > 0) ? fullScenario.tasks : rawScenario?.tasks || [],
      sampleDialogue: (fullScenario?.sampleDialogue && fullScenario.sampleDialogue.length > 0)
        ? fullScenario.sampleDialogue
        : rawScenario?.sampleDialogue || [],
      vocabulary: (fullScenario?.vocabulary && fullScenario.vocabulary.length > 0)
        ? fullScenario.vocabulary
        : rawScenario?.vocabulary || [],
      grammar: (fullScenario?.grammar && fullScenario.grammar.length > 0)
        ? fullScenario.grammar
        : rawScenario?.grammar || [],
      tips: (fullScenario?.tips && fullScenario.tips.length > 0) ? fullScenario.tips : rawScenario?.tips || [],
    }
  }, [rawScenario, fullScenario])

  // Chế độ: false = Xem tài liệu bài học 2 cột | true = Toàn màn hình nói trực tiếp với AI (chuẩn 100% trang Luyện nói)
  const [isAiChatMode, setIsAiChatMode] = useState<boolean>(false)
  const [activeTab, setActiveTab] = useState<ActiveTab>('dialogue')

  useEffect(() => {
    onChatModeChange?.(isAiChatMode)
  }, [isAiChatMode, onChatModeChange])

  // ============================================================================
  // TAB HỘI THOẠI STATES
  // ============================================================================
  const [hideDialogue, setHideDialogue] = useState<boolean>(false)
  const [isPlayingAllDialogue, setIsPlayingAllDialogue] = useState<boolean>(false)
  const [currentPlayingIndex, setCurrentPlayingIndex] = useState<number | null>(null)
  const [translationsVisible, setTranslationsVisible] = useState<Record<number, boolean>>({})
  const [translatedTexts, setTranslatedTexts] = useState<Record<number, string>>({})
  const [translatingIndex, setTranslatingIndex] = useState<number | null>(null)
  const [playingLineIndex, setPlayingLineIndex] = useState<number | null>(null)

  // Shadowing / Luyện nhại theo câu mẫu
  const [shadowingLine, setShadowingLine] = useState<{ index: number; text: string } | null>(null)
  const [shadowingScore, setShadowingScore] = useState<number | null>(null)
  const [shadowingResult, setShadowingResult] = useState<string | null>(null)

  // ============================================================================
  // TAB TỪ VỰNG & NGỮ PHÁP STATES
  // ============================================================================
  const [isPlayingAllVocab, setIsPlayingAllVocab] = useState<boolean>(false)
  const [currentVocabIndex, setCurrentVocabIndex] = useState<number | null>(null)
  const [showVocabPracticeModal, setShowVocabPracticeModal] = useState<boolean>(false)
  const [practiceVocabIndex, setPracticeVocabIndex] = useState<number>(0)
  const [showVocabAnswer, setShowVocabAnswer] = useState<boolean>(false)
  const [expandedGrammar, setExpandedGrammar] = useState<Record<number, boolean>>({})
  const [isPlayingAllGrammar, setIsPlayingAllGrammar] = useState<boolean>(false)

  // ============================================================================
  // AI CHAT SESSION STATES
  // ============================================================================
  const [session, setSession] = useState<AiSession | null>(null)
  const [messages, setMessages] = useState<AiMessage[]>([])
  const [isInitializingSession, setIsInitializingSession] = useState<boolean>(false)
  const [isProcessing, setIsProcessing] = useState<boolean>(false)
  const [inputText, setInputText] = useState('')
  const [completedTasks, setCompletedTasks] = useState<number[]>([])

  // Cài đặt hiển thị phòng nói (Giống hệt Popover Cài đặt của trang Luyện nói - Ảnh 2)
  const [showSettings, setShowSettings] = useState<boolean>(false)
  const [showRomaji, setShowRomaji] = useState<boolean>(() => {
    const saved = localStorage.getItem('speaking_show_romaji')
    return saved !== null ? saved === 'true' : true
  })
  const [showFurigana, setShowFurigana] = useState<boolean>(() => {
    const saved = localStorage.getItem('speaking_show_furigana')
    return saved !== null ? saved === 'true' : true
  })
  const [audioRate, setAudioRate] = useState<number>(1.0)
  const [selectedVoice, setSelectedVoice] = useState<'female' | 'male'>(() => {
    return (localStorage.getItem('speaking_voice_gender') as 'female' | 'male') || 'female'
  })

  // 3 Nút tính năng nổi: 🎯 Nhiệm vụ | ⚡ Dịch nhanh | 💡 Gợi ý
  const [showTasksModal, setShowTasksModal] = useState<boolean>(false)
  const [showInlineSuggestion, setShowInlineSuggestion] = useState<boolean>(false)
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState<boolean>(false)
  const [suggestionList, setSuggestionList] = useState<Array<{ ja: string; vi?: string }>>([])
  const [currentSugIndex, setCurrentSugIndex] = useState<number>(0)
  const [isKeyboardMode, setIsKeyboardMode] = useState<boolean>(false)
  const [copiedId, setCopiedId] = useState<number | string | null>(null)

  // Audio & Message Refs
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const isProcessingRef = useRef(false)
  const lastSentTextRef = useRef('')
  const lastSentTimeRef = useRef(0)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const chatContainerRef = useRef<HTMLDivElement>(null)

  const totalTasks = currentScenario?.tasks?.length || 0
  const userTurns = messages.filter((m) => m.role === 'USER').length
  const isAllTasksCompleted = totalTasks > 0 && completedTasks.length >= totalTasks
  const isGoalReached = totalTasks > 0 ? isAllTasksCompleted : userTurns >= 3

  // ============================================================================
  // SPEECH RECOGNITION (Dùng chung cho cả Chat & Shadowing)
  // ============================================================================
  const {
    isListening,
    transcript,
    interimTranscript,
    startListening,
    stopListening,
    cancelListening,
    resetTranscript,
    error: speechError,
  } = useSpeechRecognition({
    lang: 'ja-JP',
    continuous: isAiChatMode,
    interimResults: true,
  })

  const spokenText = cleanDuplicateSpeech((transcript + (interimTranscript ? ' ' + interimTranscript : '')).trim())

  // Dọn dẹp audio khi unmount
  useEffect(() => {
    return () => {
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current = null
      }
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
    }
  }, [])

  // Tự động cuộn xuống cuối nội bộ bên trong khung chat (KHÔNG cuộn toàn bộ trang web)
  useEffect(() => {
    if (isAiChatMode && chatContainerRef.current) {
      chatContainerRef.current.scrollTo({
        top: chatContainerRef.current.scrollHeight,
        behavior: 'smooth',
      })
    }
  }, [messages, isAiChatMode])

  // Quản lý trạng thái hoàn thành của từng tình huống trong danh sách kịch bản bài học
  const [completedScenarioIds, setCompletedScenarioIds] = useState<Set<number | string>>(() => new Set())

  // Đồng bộ trạng thái đã hoàn thành ban đầu từ backend
  useEffect(() => {
    setCompletedScenarioIds((prev) => {
      const updated = new Set(prev)
      scenarioList.forEach((sc, idx) => {
        if (sc.completed) {
          updated.add(sc.id || sc.scenarioId || idx)
        }
      })
      return updated
    })
  }, [scenarioList])

  // Cập nhật tình huống hiện tại khi người học hoàn thành các nhiệm vụ hoặc số lượt nói
  useEffect(() => {
    if (isGoalReached) {
      const currentKey = rawScenario?.id || rawScenario?.scenarioId || activeScenarioIndex
      setCompletedScenarioIds((prev) => {
        if (prev.has(currentKey)) return prev
        const updated = new Set(prev)
        updated.add(currentKey)
        return updated
      })
      queryClient.invalidateQueries({ queryKey: ['curriculum', 'lessons', lessonId] })
    }
  }, [isGoalReached, rawScenario, activeScenarioIndex, lessonId, queryClient])

  // Toàn bộ các tình huống luyện nói của bài học đã hoàn thành hay chưa
  const allScenariosCompleted = useMemo(() => {
    if (scenarioList.length === 0) return isGoalReached
    return scenarioList.every((sc, idx) => {
      const key = sc.id || sc.scenarioId || idx
      return completedScenarioIds.has(key) || (idx === activeScenarioIndex && isGoalReached)
    })
  }, [scenarioList, completedScenarioIds, activeScenarioIndex, isGoalReached])

  // CHỈ đánh dấu hoàn thành dạng bài Luyện nói khi TẤT CẢ các tình huống trong bài đều đã hoàn thành
  useEffect(() => {
    if (allScenariosCompleted && onSectionDone) {
      onSectionDone()
    }
  }, [allScenariosCompleted, onSectionDone])

  // Khởi tạo hoặc khôi phục AI Chat Session
  useEffect(() => {
    if (!currentScenario?.scenarioId) return

    let isMounted = true
    setIsInitializingSession(true)

    const initSession = async () => {
      try {
        if (rawScenario.activeSessionId) {
          const existing = await speakingService.getSession(rawScenario.activeSessionId)
          if (isMounted) {
            setSession(existing)
            setMessages(existing.messages || [])
            setCompletedTasks(existing.completedTasks || [])
          }
        } else {
          const newSession = await speakingService.createSession({
            mode: 'SCENARIO',
            scenarioId: currentScenario.scenarioId,
            lessonId,
          })
          if (isMounted) {
            setSession(newSession)
            setMessages(newSession.messages || [])
            setCompletedTasks(newSession.completedTasks || [])
          }
        }
      } catch (err) {
        console.warn('Khởi tạo session luyện nói thất bại, sẽ tạo khi user bắt đầu chat:', err)
      } finally {
        if (isMounted) setIsInitializingSession(false)
      }
    }

    initSession()

    return () => {
      isMounted = false
    }
  }, [lessonId, currentScenario?.scenarioId, rawScenario?.activeSessionId])

  // ============================================================================
  // AUDIO & TTS HELPERS
  // ============================================================================
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
        utterance.rate = audioRate || 0.95
        const bestVoice = getBestJapaneseVoice()
        if (bestVoice) utterance.voice = bestVoice
        utterance.onend = () => resolve()
        utterance.onerror = () => resolve()
        window.speechSynthesis.speak(utterance)
      } catch {
        resolve()
      }
    })
  }

  const playTts = async (text: string, lineIndex?: number): Promise<void> => {
    if (!text) return
    if (lineIndex !== undefined) setPlayingLineIndex(lineIndex)
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
        console.warn('Backend audio fallback sang Web Speech:', err)
      }

      if (!played) {
        await speakWithBrowser(text)
      }
    } catch (err) {
      console.warn('Lỗi phát TTS:', err)
      await speakWithBrowser(text)
    } finally {
      if (lineIndex !== undefined) setPlayingLineIndex(null)
    }
  }

  // Phát toàn bộ đoạn hội thoại mẫu tuần tự
  const handlePlayAllDialogue = async () => {
    if (!currentScenario?.sampleDialogue || currentScenario.sampleDialogue.length === 0) return
    if (isPlayingAllDialogue) {
      if (audioRef.current) audioRef.current.pause()
      if ('speechSynthesis' in window) window.speechSynthesis.cancel()
      setIsPlayingAllDialogue(false)
      setCurrentPlayingIndex(null)
      return
    }

    setIsPlayingAllDialogue(true)
    for (let i = 0; i < currentScenario.sampleDialogue.length; i++) {
      const line = currentScenario.sampleDialogue[i]
      const text = line.text || (line as any).ja || ''
      if (!text) continue
      setCurrentPlayingIndex(i)
      await playTts(text, i)
      await new Promise((res) => setTimeout(res, 600))
    }
    setIsPlayingAllDialogue(false)
    setCurrentPlayingIndex(null)
  }

  // Dịch nghĩa câu thoại mẫu
  const toggleTranslateLine = async (index: number, text: string) => {
    if (translationsVisible[index]) {
      setTranslationsVisible((prev) => ({ ...prev, [index]: false }))
      return
    }

    if (!translatedTexts[index]) {
      const line = currentScenario?.sampleDialogue?.[index]
      if (line?.vi || (line as any)?.meaning) {
        setTranslatedTexts((prev) => ({ ...prev, [index]: line.vi || (line as any).meaning || '' }))
        setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
        return
      }

      setTranslatingIndex(index)
      setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
      try {
        const res: any = await speakingService.translate(text)
        const translated = res.translatedText || res.textVi || (res.data && res.data.translatedText)
        setTranslatedTexts((prev) => ({ ...prev, [index]: translated || 'Không có bản dịch.' }))
      } catch {
        setTranslatedTexts((prev) => ({ ...prev, [index]: 'Không thể dịch vào lúc này.' }))
      } finally {
        setTranslatingIndex(null)
      }
      return
    }

    setTranslationsVisible((prev) => ({ ...prev, [index]: true }))
  }

  // Shadowing / Luyện nhại theo câu
  const handleStartShadowing = (index: number, text: string) => {
    resetTranscript()
    setShadowingLine({ index, text })
    setShadowingScore(null)
    setShadowingResult(null)
  }

  const handleToggleShadowingMic = () => {
    if (isListening) {
      stopListening()
      const spoken = cleanDuplicateSpeech((transcript + ' ' + interimTranscript).trim())
      if (spoken && shadowingLine) {
        const score = calculateSimilarity(shadowingLine.text, spoken)
        setShadowingScore(score)
        if (score >= 85) {
          setShadowingResult(`Rất xuất sắc! Độ chính xác ${score}% ✨`)
        } else if (score >= 65) {
          setShadowingResult(`Khá tốt! Độ chính xác ${score}%. Phát âm to rõ hơn nhé! 👍`)
        } else {
          setShadowingResult(`Độ chính xác ${score}%. Hãy nghe lại và thử lại nhé! 💪`)
        }
      }
    } else {
      resetTranscript()
      setShadowingScore(null)
      setShadowingResult(null)
      startListening()
    }
  }

  // Phát toàn bộ từ vựng
  const handlePlayAllVocab = async () => {
    if (!currentScenario?.vocabulary || currentScenario.vocabulary.length === 0) return
    if (isPlayingAllVocab) {
      if (audioRef.current) audioRef.current.pause()
      setIsPlayingAllVocab(false)
      setCurrentVocabIndex(null)
      return
    }

    setIsPlayingAllVocab(true)
    for (let i = 0; i < currentScenario.vocabulary.length; i++) {
      const vocab = currentScenario.vocabulary[i]
      setCurrentVocabIndex(i)
      await playTts(vocab.word)
      await new Promise((res) => setTimeout(res, 800))
    }
    setIsPlayingAllVocab(false)
    setCurrentVocabIndex(null)
  }

  // ============================================================================
  // XỬ LÝ GỬI TIN NHẮN CHAT VỚI AI
  // ============================================================================
  const handleSendMessage = async (textToSend: string) => {
    const cleanText = cleanDuplicateSpeech(textToSend).trim()
    const now = Date.now()
    if (!cleanText || isProcessingRef.current) return

    let currentSession = session
    if (!currentSession) {
      try {
        setIsProcessing(true)
        currentSession = await speakingService.createSession({
          mode: 'SCENARIO',
          scenarioId: currentScenario.scenarioId,
          lessonId,
        })
        setSession(currentSession)
      } catch (err) {
        toast.error('Không thể tạo phiên đối thoại với AI. Vui lòng thử lại!')
        setIsProcessing(false)
        return
      }
    }

    if (isListening) stopListening()
    if (cleanText === lastSentTextRef.current && now - lastSentTimeRef.current < 2000) return

    lastSentTextRef.current = cleanText
    lastSentTimeRef.current = now
    isProcessingRef.current = true
    setIsProcessing(true)

    const userMsg: AiMessage = {
      role: 'USER',
      textJa: cleanText,
      createdAt: new Date().toISOString(),
    }
    const aiPlaceholder: AiMessage = {
      role: 'AI',
      textJa: '',
      isStreaming: true,
      createdAt: new Date().toISOString(),
    }

    setMessages((prev) => [...prev, userMsg, aiPlaceholder])
    setInputText('')
    resetTranscript()

    speakingService.sendStreamMessage(currentSession.id, cleanText, {
      onChunk: (chunk: string) => {
        setMessages((prev) => {
          const next = [...prev]
          const lastIdx = next.length - 1
          if (lastIdx >= 0 && next[lastIdx].role === 'AI') {
            next[lastIdx] = {
              ...next[lastIdx],
              textJa: (next[lastIdx].textJa || '') + chunk,
              isStreaming: true,
            }
          }
          return next
        })
      },
      onComplete: (event: any) => {
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
              next[userIdx] = { ...next[userIdx], id: event.userMessageId }
            }
          }
          return next
        })

        const aiReply = event.response?.reply_ja || (event.response as any)?.replyJa || ''
        if (aiReply) {
          playSmartAudio({ text: aiReply, voice: selectedVoice, rate: audioRate })
        }

        if (event.response?.completed_task_ids) {
          setCompletedTasks((prev) => {
            const set = new Set(prev)
            event.response!.completed_task_ids!.forEach((id: number) => set.add(id))
            return Array.from(set)
          })
        }
      },
      onError: (err: any) => {
        console.error('Lỗi streaming phản hồi:', err)
        isProcessingRef.current = false
        setIsProcessing(false)
        toast.error('AI không phản hồi được. Vui lòng gửi lại câu thoại.')
      },
    })
  }

  // Nút "Dịch cả bài": Bật / tắt bản dịch cho TẤT CẢ câu thoại trong cuộc trò chuyện
  const handleToggleTranslateAll = async () => {
    if (messages.length === 0) {
      toast.info('Chưa có câu thoại nào để dịch.')
      return
    }
    const anyVisible = messages.some((_, i) => translationsVisible[i])
    if (anyVisible) {
      setTranslationsVisible({})
      return
    }

    const newVisibility: Record<number, boolean> = {}
    const newTranslations = { ...translatedTexts }

    for (let i = 0; i < messages.length; i++) {
      newVisibility[i] = true
      if (!newTranslations[i] && !messages[i].textVi) {
        try {
          const res = await speakingService.translate(messages[i].textJa)
          newTranslations[i] = res.translatedText
        } catch {
          // bỏ qua nếu một câu lỗi mạng
        }
      }
    }
    setTranslatedTexts(newTranslations)
    setTranslationsVisible(newVisibility)
  }

  // Nút "Gợi ý": Mở danh sách câu thoại phản xạ thông minh inline
  const handleOpenSuggestions = async () => {
    if (showInlineSuggestion) {
      setShowInlineSuggestion(false)
      return
    }
    setShowInlineSuggestion(true)
    if (suggestionList.length > 0) return

    setIsLoadingSuggestions(true)
    try {
      const hints: Array<{ ja: string; vi?: string }> = []
      if (currentScenario?.sampleDialogue) {
        currentScenario.sampleDialogue
          .filter((d: any) => d.speaker === 'USER' || d.role === 'USER')
          .forEach((d: any) => {
            hints.push({ ja: d.text || d.ja || '', vi: d.vi || d.meaning || '' })
          })
      }
      if (currentScenario?.tasks) {
        currentScenario.tasks.forEach((t: any) => {
          hints.push({ ja: t.text, vi: 'Nhiệm vụ cần đạt' })
        })
      }
      if (hints.length === 0) {
        hints.push({ ja: 'はい、わかりました。', vi: 'Vâng, tôi đã hiểu.' })
        hints.push({ ja: '〜をお願いできますか？', vi: 'Bạn có thể giúp tôi... được không?' })
      }
      setSuggestionList(hints)
      setCurrentSugIndex(0)
    } finally {
      setIsLoadingSuggestions(false)
    }
  }

  const handleCopy = (id: number | string, text: string) => {
    navigator.clipboard.writeText(text)
    setCopiedId(id)
    toast.success('Đã sao chép câu thoại!')
    setTimeout(() => setCopiedId(null), 2000)
  }

  const formatTime = (dateStr?: string) => {
    if (!dateStr) return ''
    try {
      const d = new Date(dateStr)
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
    } catch {
      return ''
    }
  }

  return (
    <div className="w-full space-y-4">
      {/* ==================================================================== */}
      {/* 1. THANH CHUYỂN ĐỔI KỊCH BẢN (CHỈ HIỆN KHI Ở TRANG XEM TÀI LIỆU MẪU) */}
      {/* ==================================================================== */}
      {scenarioList.length > 1 && !isAiChatMode && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 bg-white rounded-xl border border-slate-200/90 shadow-2xs">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider shrink-0">
              Tình huống ({activeScenarioIndex + 1}/{scenarioList.length})
            </span>
            <select
              value={activeScenarioIndex}
              onChange={(e) => {
                if (isListening) stopListening()
                setActiveScenarioIndex(Number(e.target.value))
                setIsAiChatMode(false)
                setSession(null)
                setMessages([])
                setCompletedTasks([])
                setInputText('')
              }}
              className="text-xs sm:text-sm font-semibold text-slate-800 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 focus:outline-hidden focus:ring-2 focus:ring-indigo-500 truncate cursor-pointer max-w-full sm:max-w-md"
            >
              {scenarioList.map((sc, idx) => {
                const key = sc.id || sc.scenarioId || idx
                const isThisDone = sc.completed || completedScenarioIds.has(key) || (idx === activeScenarioIndex && isGoalReached)
                return (
                  <option key={sc.id || idx} value={idx}>
                    #{sc.orderIndex || idx + 1}: {sc.title} {isThisDone ? '✓ (Đã hoàn thành)' : ''}
                  </option>
                )
              })}
            </select>
          </div>

          <div className="flex items-center gap-1.5 self-end sm:self-auto shrink-0">
            <button
              type="button"
              disabled={activeScenarioIndex === 0}
              onClick={() => {
                if (isListening) stopListening()
                setActiveScenarioIndex((p) => Math.max(0, p - 1))
                setIsAiChatMode(false)
                setSession(null)
                setMessages([])
                setCompletedTasks([])
                setInputText('')
              }}
              className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-35 disabled:pointer-events-none transition cursor-pointer"
            >
              ← Trước
            </button>
            <button
              type="button"
              disabled={activeScenarioIndex >= scenarioList.length - 1}
              onClick={() => {
                if (isListening) stopListening()
                setActiveScenarioIndex((p) => Math.min(scenarioList.length - 1, p + 1))
                setIsAiChatMode(false)
                setSession(null)
                setMessages([])
                setCompletedTasks([])
                setInputText('')
              }}
              className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-35 disabled:pointer-events-none transition cursor-pointer"
            >
              Sau →
            </button>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* 2. CHẾ ĐỘ 1: XEM TÀI LIỆU BÀI MẪU (BỐ CỤC 2 CỘT TỔNG QUAN + 4 TABS) */}
      {/* ==================================================================== */}
      {!isAiChatMode ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
          {/* CỘT TRÁI: TỔNG QUAN (~35% -> lg:col-span-5 xl:col-span-4) */}
          <div className="lg:col-span-5 xl:col-span-4 bg-white rounded-2xl border border-slate-200/90 p-4 sm:p-5 shadow-xs space-y-5">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2 text-slate-800">
                <LayoutGrid className="w-5 h-5 text-slate-700" />
                <h2 className="text-base font-bold">Tổng quan</h2>
              </div>
              {previewMode && (
                <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                  Xem trước
                </span>
              )}
            </div>

            {/* Cấp độ JLPT */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1.5">Cấp độ</div>
              <span
                className={`inline-block px-2.5 py-0.5 rounded-md font-bold text-xs tracking-wide ${getJlptBadgeColor(
                  currentScenario.jlptLevel,
                )}`}
              >
                {currentScenario.jlptLevel || 'N5'}
              </span>
            </div>

            {/* Vai trò của tôi */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1">Vai trò của tôi</div>
              <p className="text-sm text-slate-800 leading-relaxed font-medium">
                {currentScenario.userRole || 'Người học'}
              </p>
            </div>

            {/* Vai trò của AI */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1">Vai trò của AI</div>
              <p className="text-sm text-slate-800 leading-relaxed font-medium">
                {currentScenario.aiRole || 'Đối tác hội thoại (Maziiko)'}
              </p>
            </div>

            {/* Mô tả tình huống */}
            <div>
              <div className="text-xs font-semibold text-slate-500 mb-1">Mô tả tình huống</div>
              <p className="text-xs sm:text-sm text-slate-700 leading-relaxed font-normal">
                {currentScenario.description || 'Chưa có mô tả bối cảnh tình huống.'}
              </p>
            </div>

            {/* Nhiệm vụ cần đạt */}
            <div>
              <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
                <span>Nhiệm vụ</span>
                {totalTasks > 0 && (
                  <span className="text-[11px] font-bold text-indigo-600">
                    {completedTasks.length}/{totalTasks} đã đạt
                  </span>
                )}
              </div>
              <ul className="space-y-2">
                {currentScenario.tasks && currentScenario.tasks.length > 0 ? (
                  currentScenario.tasks.map((task) => {
                    const isDone = completedTasks.includes(task.id)
                    return (
                      <li
                        key={task.id}
                        className={`flex items-start gap-2.5 text-xs sm:text-sm leading-snug transition ${
                          isDone ? 'text-emerald-700 font-medium' : 'text-slate-700'
                        }`}
                      >
                        {isDone ? (
                          <CheckCircle2 className="w-4 h-4 text-emerald-500 mt-0.5 shrink-0" />
                        ) : (
                          <span className="w-2 h-2 rounded-full bg-slate-300 mt-1.5 shrink-0" />
                        )}
                        <span className={isDone ? 'line-through opacity-85' : ''}>{task.text}</span>
                      </li>
                    )
                  })
                ) : (
                  <li className="text-xs text-slate-400 italic">Không có nhiệm vụ bắt buộc.</li>
                )}
              </ul>
            </div>

            {/* Nút to CTA: ✨ Luyện nói cùng AI */}
            <div className="pt-2">
              <button
                type="button"
                onClick={() => setIsAiChatMode(true)}
                className="w-full py-2.5 rounded-full bg-linear-to-r from-[#8B5CF6] via-[#6366F1] to-[#3B82F6] hover:from-[#7C3AED] hover:to-[#2563EB] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-md hover:shadow-lg transition cursor-pointer"
              >
                <Sparkles className="w-4 h-4 text-amber-300 fill-amber-300" />
                <span>Luyện nói cùng AI</span>
              </button>
            </div>
          </div>

          {/* CỘT PHẢI: LUYỆN TẬP TABS (8/12 cols ~65%) */}
          <div className="lg:col-span-7 xl:col-span-8 bg-white rounded-2xl border border-slate-200/90 p-4 sm:p-5 shadow-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 gap-3">
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-800">Luyện tập</h2>
                {(isLoadingScenario || isInitializingSession) && (
                  <Loader2 className="w-4 h-4 text-slate-400 animate-spin" />
                )}
              </div>

              {/* 4 Tabs gốc: Hội thoại | Từ vựng | Ngữ pháp | Mẹo */}
              <div className="flex items-center gap-1 p-1 rounded-xl bg-slate-100/80 border border-slate-200/60 self-start sm:self-auto">
                <button
                  type="button"
                  onClick={() => setActiveTab('dialogue')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
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
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
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
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
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
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
                    activeTab === 'tips'
                      ? 'bg-blue-50 text-blue-600 shadow-2xs font-bold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Mẹo
                </button>
              </div>
            </div>

            {/* TAB 1: HỘI THOẠI */}
            {activeTab === 'dialogue' && (
              <div className="mt-4 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-700">Đoạn hội thoại mẫu</h3>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={handlePlayAllDialogue}
                      className={`p-2 rounded-full border transition flex items-center justify-center cursor-pointer ${
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

                    <button
                      type="button"
                      onClick={() => setHideDialogue((prev) => !prev)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium border border-slate-200 transition cursor-pointer"
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

                <div className="bg-[#F7F5FC] rounded-2xl p-3 sm:p-5 space-y-4 min-h-[360px] max-h-[600px] overflow-y-auto">
                  {currentScenario.sampleDialogue && currentScenario.sampleDialogue.length > 0 ? (
                    currentScenario.sampleDialogue.map((line: SampleDialogueLine, index: number) => {
                      const isUser = line.speaker === 'USER' || (line as any).role === 'USER'
                      const lineText = line.text || (line as any).ja || ''
                      const isCurrentPlaying = currentPlayingIndex === index
                      const isTranslated = !!translationsVisible[index]

                      return (
                        <div key={index} className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
                          <div
                            className={`max-w-[88%] sm:max-w-[78%] rounded-2xl p-3.5 shadow-xs transition-all ${
                              isUser
                                ? 'bg-white rounded-tr-xs border border-slate-200/80 text-right'
                                : 'bg-white rounded-tl-xs border border-slate-200/80 text-left'
                            } ${isCurrentPlaying ? 'ring-2 ring-indigo-500 shadow-md' : ''}`}
                          >
                            <div className={`text-[11px] font-semibold text-slate-400 mb-1 ${isUser ? 'text-right' : 'text-left'}`}>
                              {isUser ? currentScenario.userRole : currentScenario.aiRole}
                            </div>

                            {hideDialogue ? (
                              <div className="py-2 px-3 rounded-lg bg-slate-100 text-xs text-slate-400 italic flex items-center gap-2">
                                <Volume2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                                <span>(Đang ẩn chữ để luyện nghe - nhấn loa bên dưới để nghe)</span>
                              </div>
                            ) : (
                              <div className={isUser ? 'text-right' : 'text-left'}>
                                <FuriganaText text={lineText} size="md" showRomaji={showRomaji} />
                                {isTranslated && (
                                  <div className="mt-2 pt-2 border-t border-slate-100 text-xs sm:text-sm text-indigo-700 font-medium text-left">
                                    {translatingIndex === index ? (
                                      <div className="flex items-center gap-2 text-indigo-500 animate-pulse">
                                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                        <span>Đang dịch...</span>
                                      </div>
                                    ) : (
                                      <span>
                                        {translatedTexts[index] || line.vi || (line as any).meaning || 'Không có bản dịch.'}
                                      </span>
                                    )}
                                  </div>
                                )}
                              </div>
                            )}

                            <div className={`flex items-center gap-1 mt-3 pt-2 border-t border-slate-100 ${isUser ? 'justify-end' : 'justify-start'}`}>
                              <button
                                type="button"
                                onClick={() => playTts(lineText, index)}
                                className={`p-1.5 rounded-lg transition cursor-pointer ${
                                  playingLineIndex === index
                                    ? 'bg-blue-100 text-blue-700 scale-105 shadow-2xs'
                                    : 'hover:bg-slate-100 text-slate-500 hover:text-slate-800'
                                }`}
                                title="Nghe phát âm"
                              >
                                <Volume2 className={`w-4 h-4 ${playingLineIndex === index ? 'animate-bounce text-blue-600' : ''}`} />
                              </button>

                              <button
                                type="button"
                                onClick={() => toggleTranslateLine(index, lineText)}
                                className={`p-1.5 rounded-lg transition cursor-pointer ${
                                  isTranslated ? 'bg-indigo-50 text-indigo-600' : 'hover:bg-slate-100 text-slate-500 hover:text-slate-800'
                                }`}
                                title="Dịch nghĩa"
                              >
                                <Languages className="w-4 h-4" />
                              </button>

                              <button
                                type="button"
                                onClick={() => handleStartShadowing(index, lineText)}
                                className="p-1.5 rounded-lg hover:bg-indigo-50 text-slate-500 hover:text-indigo-600 transition cursor-pointer"
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

            {/* TAB 2: TỪ VỰNG */}
            {activeTab === 'vocabulary' && (
              <div className="mt-4 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-700">Danh sách</h3>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={handlePlayAllVocab}
                      className={`p-2 rounded-full border transition flex items-center justify-center cursor-pointer ${
                        isPlayingAllVocab
                          ? 'bg-blue-600 text-white border-blue-600'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                      }`}
                      title={isPlayingAllVocab ? 'Dừng phát' : 'Phát toàn bộ từ vựng'}
                    >
                      {isPlayingAllVocab ? <Square className="w-3.5 h-3.5 fill-current" /> : <Play className="w-3.5 h-3.5 fill-current ml-0.5" />}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowVocabPracticeModal(true)
                        setPracticeVocabIndex(0)
                        setShowVocabAnswer(false)
                      }}
                      className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold shadow-xs transition cursor-pointer"
                    >
                      Luyện tập
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                  {currentScenario.vocabulary && currentScenario.vocabulary.length > 0 ? (
                    (currentScenario.vocabulary as any[]).map((item: any, idx: number) => {
                      const isCurrentPlaying = currentVocabIndex === idx
                      return (
                        <div
                          key={idx}
                          className={`bg-[#F8FAFC] rounded-2xl p-4 border border-slate-200/80 hover:border-indigo-200 hover:bg-slate-50 transition flex items-start justify-between gap-3 ${
                            isCurrentPlaying ? 'ring-2 ring-indigo-500 bg-indigo-50/30' : ''
                          }`}
                        >
                          <div className="flex-1 min-w-0">
                            <div className="mb-1">
                              <FuriganaText text={item.word} size="md" showRomaji={false} />
                            </div>
                            <p className="text-xs sm:text-sm text-slate-600 leading-snug line-clamp-2">{item.meaning}</p>
                          </div>
                          <button
                            type="button"
                            onClick={() => playTts(item.word)}
                            className="p-2 rounded-xl bg-white border border-slate-200/80 hover:bg-indigo-50 hover:text-indigo-600 text-slate-500 transition cursor-pointer shadow-2xs shrink-0"
                            title="Nghe phát âm"
                          >
                            <Volume2 className="w-4 h-4" />
                          </button>
                        </div>
                      )
                    })
                  ) : (
                    <div className="col-span-2 text-center py-12 text-slate-400 text-sm">Chưa có từ vựng tiêu biểu cho tình huống này.</div>
                  )}
                </div>
              </div>
            )}

            {/* TAB 3: NGỮ PHÁP */}
            {activeTab === 'grammar' && (
              <div className="mt-4 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-slate-700">Danh sách</h3>
                  <button
                    type="button"
                    onClick={() => {
                      if (isPlayingAllGrammar) {
                        if (audioRef.current) audioRef.current.pause()
                        setIsPlayingAllGrammar(false)
                      } else if (currentScenario.grammar && currentScenario.grammar.length > 0) {
                        setIsPlayingAllGrammar(true)
                        const playNext = async () => {
                          const grammarList = currentScenario.grammar as any[]
                          for (let i = 0; i < grammarList.length; i++) {
                            const g = grammarList[i]
                            await playTts(g.example || g.pattern)
                            await new Promise((res) => setTimeout(res, 800))
                          }
                          setIsPlayingAllGrammar(false)
                        }
                        playNext()
                      }
                    }}
                    className={`p-2 rounded-full border transition flex items-center justify-center cursor-pointer ${
                      isPlayingAllGrammar ? 'bg-blue-600 text-white border-blue-600' : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200'
                    }`}
                    title={isPlayingAllGrammar ? 'Dừng phát' : 'Phát toàn bộ ví dụ'}
                  >
                    {isPlayingAllGrammar ? <Square className="w-3.5 h-3.5 fill-current" /> : <Play className="w-3.5 h-3.5 fill-current ml-0.5" />}
                  </button>
                </div>

                <div className="space-y-3.5">
                  {currentScenario.grammar && currentScenario.grammar.length > 0 ? (
                    (currentScenario.grammar as any[]).map((item: any, idx: number) => {
                      const isExpanded = !!expandedGrammar[idx]
                      return (
                        <div key={idx} className="bg-[#F8FAFC] rounded-2xl p-4 sm:p-5 border border-slate-200/80 hover:border-indigo-200 transition space-y-2.5">
                          <h4 className="font-bold text-sm text-slate-800 tracking-wide">{item.pattern}</h4>
                          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">{item.meaning}</p>
                          {item.example && (
                            <div className="pt-2 border-t border-slate-200/70 space-y-1">
                              <div className="flex items-center justify-between">
                                <span className="text-xs sm:text-sm font-semibold text-slate-800">{item.example}</span>
                                <button type="button" onClick={() => playTts(item.example!)} className="p-1.5 rounded-lg hover:bg-slate-200 text-slate-500 hover:text-slate-800 transition cursor-pointer" title="Nghe phát âm ví dụ">
                                  <Volume2 className="w-4 h-4" />
                                </button>
                              </div>
                              {item.exampleMeaning && <p className="text-xs text-slate-500">{item.exampleMeaning}</p>}
                            </div>
                          )}
                          <div className="pt-1">
                            <button type="button" onClick={() => setExpandedGrammar((prev) => ({ ...prev, [idx]: !prev[idx] }))} className="w-full flex items-center justify-between py-2 px-3 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition cursor-pointer">
                              <span>Giải nghĩa</span>
                              <ChevronRight className={`w-4 h-4 text-slate-400 transform transition-transform ${isExpanded ? 'rotate-90' : ''}`} />
                            </button>
                            {isExpanded && (
                              <div className="mt-2 p-3 rounded-xl bg-indigo-50/60 border border-indigo-100 text-xs text-slate-700 leading-relaxed">
                                {item.explanation || `Cấu trúc "${item.pattern}" được sử dụng phổ biến trong ngữ cảnh này để diễn đạt: ${item.meaning}. Hãy luyện tập vận dụng thường xuyên!`}
                              </div>
                            )}
                          </div>
                        </div>
                      )
                    })
                  ) : (
                    <div className="text-center py-12 text-slate-400 text-sm">Chưa có điểm ngữ pháp nào được đánh dấu cho tình huống này.</div>
                  )}
                </div>
              </div>
            )}

            {/* TAB 4: MẸO GIAO TIẾP */}
            {activeTab === 'tips' && (
              <div className="mt-4 space-y-4">
                <h3 className="text-sm font-bold text-slate-700">Mẹo giao tiếp tiếng Nhật</h3>
                <div className="space-y-3">
                  {currentScenario.tips && currentScenario.tips.length > 0 ? (
                    currentScenario.tips.map((tip, idx) => (
                      <div key={idx} className="bg-amber-50/60 rounded-2xl p-4 sm:p-5 border border-amber-200/60 flex items-start gap-3">
                        <div className="w-7 h-7 rounded-xl bg-amber-100 flex items-center justify-center shrink-0 mt-0.5">
                          <Lightbulb className="w-4 h-4 text-amber-600" />
                        </div>
                        <p className="text-xs sm:text-sm text-amber-950 leading-relaxed font-normal">{tip}</p>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-12 text-slate-400 text-sm">Chưa có mẹo giao tiếp bổ sung cho tình huống này.</div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      ) : (
        /* ==================================================================== */
        /* 3. CHẾ ĐỘ 2: LUYỆN NÓI CÙNG AI TRỰC TIẾP (HẾT KHUNG, CUỘN NỘI BỘ, KHÔNG GIÃN TRANG) */
        /* ==================================================================== */
        <div className="-m-6 md:-m-8 flex flex-col h-[calc(100vh-140px)] min-h-[580px] bg-[#FAF8F5] rounded-md overflow-hidden relative">
          {/* Header phòng luyện nói: Cố định ở trên cùng */}
          <div className="shrink-0 bg-white/95 backdrop-blur-xs px-5 sm:px-8 py-3.5 border-b border-amber-100/90 flex items-center justify-between gap-4 sticky top-0 z-30 shadow-2xs">
            <button
              type="button"
              onClick={() => {
                if (isListening) stopListening()
                setIsAiChatMode(false)
              }}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs sm:text-sm font-semibold transition cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
              <span>Quay lại bài học</span>
            </button>

            <div className="flex items-center gap-2.5 min-w-0 max-w-lg">
              <span className={`px-2.5 py-0.5 rounded-md text-xs font-bold ${getJlptBadgeColor(currentScenario.jlptLevel)} uppercase shrink-0`}>
                {currentScenario.jlptLevel || 'N5'}
              </span>
              {scenarioList.length > 1 && (
                <span className="px-2 py-0.5 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold shrink-0">
                  Tình huống {activeScenarioIndex + 1}/{scenarioList.length}
                </span>
              )}
              <h2 className="text-sm sm:text-base font-bold text-slate-800 truncate">
                {currentScenario.title}
              </h2>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {scenarioList.length > 1 && activeScenarioIndex < scenarioList.length - 1 && isGoalReached && (
                <button
                  type="button"
                  onClick={() => {
                    if (isListening) stopListening()
                    setActiveScenarioIndex((p) => p + 1)
                    setSession(null)
                    setMessages([])
                    setCompletedTasks([])
                    setInputText('')
                  }}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-xs transition cursor-pointer"
                >
                  <span>Tình huống tiếp</span>
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              )}

              {/* Icon Cài đặt mở Popover */}
              <div className="relative">
              <button
                type="button"
                onClick={() => setShowSettings((p) => !p)}
                className={`p-2 rounded-xl transition cursor-pointer border ${
                  showSettings
                    ? 'bg-blue-50 text-blue-600 border-blue-200 shadow-2xs'
                    : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
                title="Cài đặt phòng nói"
              >
                <Settings2 className="w-4.5 h-4.5" />
              </button>

              {/* POPOVER CÀI ĐẶT PHÒNG NÓI */}
              {showSettings && (
                <div className="absolute right-0 top-11 w-76 sm:w-80 bg-white rounded-2xl p-4 shadow-2xl border border-slate-200/90 z-50 space-y-4 animate-in fade-in zoom-in-95">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-700">Hiện Romaji</span>
                    <button
                      type="button"
                      onClick={() => setShowRomaji((p) => !p)}
                      className={`w-10 h-5.5 flex items-center rounded-full p-0.5 cursor-pointer transition-colors ${
                        showRomaji ? 'bg-blue-600' : 'bg-slate-300'
                      }`}
                    >
                      <div className={`bg-white w-4.5 h-4.5 rounded-full shadow-md transform transition-transform ${showRomaji ? 'translate-x-4.5' : 'translate-x-0'}`} />
                    </button>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-700">Hiện Furigana</span>
                    <button
                      type="button"
                      onClick={() => setShowFurigana((p) => !p)}
                      className={`w-10 h-5.5 flex items-center rounded-full p-0.5 cursor-pointer transition-colors ${
                        showFurigana ? 'bg-blue-600' : 'bg-slate-300'
                      }`}
                    >
                      <div className={`bg-white w-4.5 h-4.5 rounded-full shadow-md transform transition-transform ${showFurigana ? 'translate-x-4.5' : 'translate-x-0'}`} />
                    </button>
                  </div>

                  {/* Tốc độ đọc rùa 🐢 -> thỏ 🐇 */}
                  <div>
                    <span className="text-xs font-semibold text-slate-700 block mb-1.5">Tốc độ đọc</span>
                    <div className="flex items-center gap-2">
                      <span className="text-sm select-none">🐢</span>
                      <input
                        type="range"
                        min="0.7"
                        max="1.3"
                        step="0.1"
                        value={audioRate}
                        onChange={(e) => setAudioRate(parseFloat(e.target.value))}
                        className="flex-1 h-1.5 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                      />
                      <span className="text-sm select-none">🐇</span>
                    </div>
                    <div className="text-center text-[11px] text-slate-400 font-medium mt-1">
                      Tốc độ: x{audioRate.toFixed(1)}
                    </div>
                  </div>

                  {/* Giọng đọc AI: Nữ (Nanami) vs Nam (Keita) */}
                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-xs font-semibold text-slate-700">Giọng đọc AI</span>
                      <span className="text-[11px] text-slate-400">{selectedVoice === 'female' ? 'Nữ (Nanami)' : 'Nam (Keita)'}</span>
                    </div>
                    <div className="grid grid-cols-2 gap-1.5">
                      <button
                        type="button"
                        onClick={() => setSelectedVoice('female')}
                        className={`py-1.5 px-2 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1 transition cursor-pointer ${
                          selectedVoice === 'female' ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                        }`}
                      >
                        <span>👩 Nữ (Nanami)</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setSelectedVoice('male')}
                        className={`py-1.5 px-2 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1 transition cursor-pointer ${
                          selectedVoice === 'male' ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                        }`}
                      >
                        <span>👨 Nam (Keita)</span>
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                    <button
                      type="button"
                      onClick={() => {
                        setShowRomaji(true)
                        setShowFurigana(true)
                        setAudioRate(1.0)
                        setSelectedVoice('female')
                      }}
                      className="text-xs font-medium text-slate-400 hover:text-slate-700 cursor-pointer"
                    >
                      Đặt lại
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowSettings(false)}
                      className="px-4 py-1.5 rounded-xl bg-blue-600 text-white font-bold text-xs hover:bg-blue-700 shadow-xs cursor-pointer"
                    >
                      Áp dụng
                    </button>
                  </div>
                </div>
              )}
            </div>
            </div>
          </div>

          {/* Khung chat trung tâm - Rộng rãi, cuộn nội bộ tự động khi nhiều tin nhắn */}
          <div
            ref={chatContainerRef}
            className="flex-1 min-h-0 w-full overflow-y-auto scroll-smooth pr-1"
          >
            <div className="max-w-3xl mx-auto px-4 sm:px-6 py-6 space-y-4.5">
              {/* THẺ BỐI CẢNH (Context Pill chuẩn Mazii) */}
              {currentScenario.description && (
                <div className="flex flex-col items-center justify-center mb-5 gap-2">
                  <div className="inline-flex items-center gap-2.5 px-5 py-2.5 rounded-full bg-blue-50/90 border border-blue-200/80 text-xs sm:text-sm text-slate-700 font-medium shadow-2xs text-center max-w-xl">
                    <span className="text-blue-600 font-bold text-base select-none shrink-0">💬</span>
                    <span className="leading-snug">{currentScenario.description}</span>
                  </div>
                  {(currentScenario.userRole || currentScenario.aiRole) && (
                    <div className="inline-flex items-center gap-2.5 text-xs text-slate-500 bg-white/80 backdrop-blur-xs px-3.5 py-1 rounded-full border border-slate-200/70 shadow-2xs">
                      <span>👤 Bạn: <strong className="text-slate-800 font-semibold">{currentScenario.userRole || 'Người học'}</strong></span>
                      <span className="text-slate-300">·</span>
                      <span>🤖 AI: <strong className="text-slate-800 font-semibold">{currentScenario.aiRole || 'Maziiko'}</strong></span>
                    </div>
                  )}
                </div>
              )}

              {/* DANH SÁCH TIN NHẮN ĐỐI THOẠI */}
              {messages.length === 0 ? (
                <div className="py-14 flex flex-col items-center justify-center text-center space-y-3">
                  <MaziikoMascot size={72} showAura={true} />
                  <p className="text-sm font-semibold text-slate-700">
                    Hãy bắt đầu bằng một câu chào hoặc lời thoại theo vai của bạn!
                  </p>
                  <p className="text-xs text-slate-400 max-w-sm">
                    Bấm vào micro bên dưới hoặc chuyển sang gõ phím để trò chuyện cùng Maziiko.
                  </p>
                </div>
              ) : (
                messages.map((msg, index) => {
                  const isUser = msg.role === 'USER'
                  const isTranslated = !!translationsVisible[index]
                  const time = formatTime(msg.createdAt)

                  if (!isUser) {
                    // Tin nhắn AI (Maziiko) - Căn trái
                    return (
                      <div key={msg.id || index} className="flex justify-start items-start gap-3 animate-in fade-in">
                        <div className="shrink-0 mt-1">
                          <MaziikoMascot size={36} />
                        </div>

                        <div className="max-w-[85%] sm:max-w-[80%] rounded-2xl rounded-tl-xs bg-white border border-slate-200/90 p-4 shadow-xs text-left space-y-2">
                          {/* Header của tin nhắn AI */}
                          <div className="flex items-center gap-2 text-xs">
                            <span className="font-bold text-slate-800">Maziiko</span>
                            {currentScenario.aiRole && (
                              <span className="text-[11px] text-indigo-600 font-medium bg-indigo-50 px-1.5 py-0.5 rounded-md">
                                {currentScenario.aiRole}
                              </span>
                            )}
                            <span className="text-slate-400 text-[11px]">{time}</span>
                          </div>

                          {/* Nội dung tiếng Nhật */}
                          <FuriganaText
                            text={msg.textJa}
                            size="md"
                            showRomaji={showRomaji}
                            showFurigana={showFurigana}
                            className="text-slate-800 text-sm sm:text-base leading-relaxed"
                          />

                          {msg.isStreaming && (
                            <span className="inline-block w-2 h-4 ml-1 bg-indigo-600 animate-pulse rounded-xs" />
                          )}

                          {/* Bản dịch */}
                          {isTranslated && (
                            <div className="mt-2 pt-2 border-t border-slate-100 text-xs sm:text-sm font-medium text-indigo-700">
                              {translatedTexts[index] || msg.textVi || 'Đang tải bản dịch...'}
                            </div>
                          )}

                          {/* Thẻ gợi ý / sửa lỗi ngữ pháp */}
                          {msg.correction && (
                            <div className="mt-1.5">
                              <GrammarCorrectionCard correction={msg.correction} />
                            </div>
                          )}

                          {/* 3 icon thao tác: 🔊 Loa | 📋 Copy | 🌐 Dịch */}
                          <div className="flex items-center gap-1 mt-2.5 pt-2 border-t border-slate-100 text-slate-500">
                            <button
                              type="button"
                              onClick={() => playSmartAudio({ text: msg.textJa, voice: selectedVoice, rate: audioRate })}
                              className="p-1.5 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer"
                              title="Nghe phát âm"
                            >
                              <Volume2 className="w-4 h-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleCopy(index, msg.textJa)}
                              className="p-1.5 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer"
                              title="Sao chép"
                            >
                              {copiedId === index ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                            </button>
                            <button
                              type="button"
                              onClick={async () => {
                                setTranslationsVisible((p) => ({ ...p, [index]: !p[index] }))
                                if (!translatedTexts[index] && !msg.textVi) {
                                  try {
                                    const res = await speakingService.translate(msg.textJa)
                                    setTranslatedTexts((p) => ({ ...p, [index]: res.translatedText }))
                                  } catch {
                                    toast.error('Không thể dịch câu này.')
                                  }
                                }
                              }}
                              className={`p-1.5 rounded-lg transition cursor-pointer ${
                                isTranslated ? 'bg-indigo-50 text-indigo-600 font-semibold' : 'hover:bg-slate-100 hover:text-slate-800 text-slate-500'
                              }`}
                              title="Dịch nghĩa"
                            >
                              <Languages className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      </div>
                    )
                  }

                  // Tin nhắn người học - Căn phải (Trắng tinh khôi, viền trang nhã như SpeakingPracticePage)
                  return (
                    <div key={msg.id || index} className="flex justify-end items-start gap-2.5 animate-in fade-in">
                      <div className="max-w-[85%] sm:max-w-[75%] rounded-2xl rounded-tr-xs bg-white border border-slate-200/90 p-4 shadow-xs text-left space-y-2">
                        <div className="flex items-center justify-between gap-4 mb-1 text-xs text-slate-400">
                          <span className="font-semibold text-slate-700">{currentScenario.userRole || 'Bạn'}</span>
                          <span className="text-[11px]">{time}</span>
                        </div>

                        <FuriganaText
                          text={msg.textJa}
                          size="md"
                          showRomaji={showRomaji}
                          showFurigana={showFurigana}
                          className="text-slate-800 text-sm sm:text-base leading-relaxed"
                        />

                        {isTranslated && (
                          <div className="mt-2 pt-2 border-t border-slate-100 text-xs sm:text-sm font-medium text-indigo-700">
                            {translatedTexts[index] || msg.textVi || 'Đang tải bản dịch...'}
                          </div>
                        )}

                        <div className="flex items-center gap-1.5 mt-2.5 pt-2 border-t border-slate-100 text-slate-500">
                          <button
                            type="button"
                            onClick={() => playSmartAudio({ text: msg.textJa, voice: selectedVoice, rate: audioRate })}
                            className="p-1.5 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer"
                            title="Phát âm"
                          >
                            <Volume2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleCopy(index, msg.textJa)}
                            className="p-1.5 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer"
                            title="Sao chép"
                          >
                            {copiedId === index ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                          </button>
                          <button
                            type="button"
                            onClick={async () => {
                              setTranslationsVisible((p) => ({ ...p, [index]: !p[index] }))
                              if (!translatedTexts[index] && !msg.textVi) {
                                try {
                                  const res = await speakingService.translate(msg.textJa)
                                  setTranslatedTexts((p) => ({ ...p, [index]: res.translatedText }))
                                } catch {
                                  toast.error('Không thể dịch câu này.')
                                }
                              }
                            }}
                            className={`p-1.5 rounded-lg transition cursor-pointer ${
                              isTranslated ? 'bg-indigo-50 text-indigo-600 font-semibold' : 'hover:bg-slate-100 hover:text-slate-800 text-slate-500'
                            }`}
                            title="Dịch nghĩa"
                          >
                            <Languages className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  )
                })
              )}

              {isProcessing && (
                <div className="flex items-center gap-2 p-3 bg-white border border-slate-200/80 text-slate-600 rounded-2xl w-fit text-xs animate-pulse shadow-2xs">
                  <Loader2 className="w-4 h-4 animate-spin text-indigo-600" />
                  <span>Maziiko đang suy nghĩ...</span>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
          </div>

          {/* ==================================================================== */}
          {/* THANH ĐIỀU KHIỂN NỔI PHÍA DƯỚI (CỐ ĐỊNH, KHÔNG BỊ TRÔI) */}
          {/* ==================================================================== */}
          <div className="shrink-0 pb-5 px-4 sm:px-6 bg-gradient-to-t from-[#FAF8F5] via-[#FAF8F5]/95 to-transparent pt-3 border-t border-amber-100/60">
            <div className="max-w-2xl w-full mx-auto flex flex-col items-center gap-3">
              {/* HÀNG 3 NÚT NỔI: 🎯 Nhiệm vụ | ⚡ Dịch cả bài | 💡 Gợi ý */}
              <div className="flex items-center gap-2 flex-wrap justify-center">
                {currentScenario.tasks && currentScenario.tasks.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setShowTasksModal(true)}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200/90 hover:border-indigo-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-xs transition cursor-pointer"
                  >
                    <Target className="w-3.5 h-3.5 text-indigo-600" />
                    <span>Nhiệm vụ</span>
                    <span className="text-[10px] text-slate-400">
                      ({completedTasks.length}/{totalTasks})
                    </span>
                  </button>
                )}

                <button
                  type="button"
                  onClick={handleToggleTranslateAll}
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200/90 hover:border-indigo-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-xs transition cursor-pointer"
                  title="Bật / tắt bản dịch tiếng Việt cho tất cả các câu thoại"
                >
                  <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-400" />
                  <span>Dịch cả bài</span>
                </button>

                <button
                  type="button"
                  disabled={isLoadingSuggestions}
                  onClick={handleOpenSuggestions}
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200/90 hover:border-indigo-300 hover:bg-slate-50 text-slate-700 font-semibold text-xs shadow-xs transition cursor-pointer disabled:opacity-50"
                >
                  {isLoadingSuggestions ? (
                    <div className="w-3.5 h-3.5 border-2 border-purple-600 border-t-transparent rounded-full animate-spin" />
                  ) : (
                    <Lightbulb className="w-3.5 h-3.5 text-purple-600 fill-purple-100" />
                  )}
                  <span>{isLoadingSuggestions ? 'Đang gợi ý...' : 'Gợi ý'}</span>
                </button>
              </div>

              {/* THẺ GỢI Ý CÂU TRẢ LỜI INLINE KHI BẤM NÚT "GỢI Ý" */}
              {showInlineSuggestion && (
                <div className="w-full bg-white rounded-2xl p-4 border border-slate-200/90 shadow-md space-y-3 animate-in fade-in slide-in-from-bottom-2">
                  {isLoadingSuggestions ? (
                    <div className="flex items-center gap-3 py-2 text-indigo-600 text-sm font-medium">
                      <Loader2 className="w-4 h-4 animate-spin shrink-0" />
                      <span>AI đang gợi ý phản hồi phù hợp...</span>
                    </div>
                  ) : suggestionList.length > 0 ? (
                    <>
                      <div className="flex items-start justify-between gap-3">
                        <div className="space-y-1 pr-2">
                          <div className="text-slate-800 font-semibold text-sm leading-snug">
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
                          className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center shrink-0 shadow-md hover:scale-105 transition cursor-pointer"
                          title="Gửi câu gợi ý này cho AI"
                        >
                          <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                        </button>
                      </div>

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
                              onClick={() => setCurrentSugIndex((prev) => (prev + 1) % suggestionList.length)}
                              className="text-indigo-600 hover:text-indigo-700 font-semibold cursor-pointer"
                            >
                              Câu khác
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => setShowInlineSuggestion(false)}
                            className="hover:text-slate-700 font-medium underline cursor-pointer"
                          >
                            Đóng
                          </button>
                        </div>
                      </div>
                    </>
                  ) : null}
                </div>
              )}

              {/* THANH NHẬP LIỆU CAPSULE HOẶC THẺ THU ÂM */}
              {isListening || !!spokenText ? (
                /* Thẻ thu âm sóng âm khi nói */
                <div className="w-full bg-white rounded-2xl p-4 border border-slate-200/90 shadow-md space-y-3 animate-in fade-in slide-in-from-bottom-2">
                  <div className="text-slate-800 text-sm sm:text-base font-medium min-h-[32px] select-text">
                    {spokenText || (
                      <span className="text-slate-400 italic">
                        {isListening ? 'Đang lắng nghe bạn nói...' : 'Bấm mic để nói tiếp...'}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center justify-between pt-1">
                    <button
                      type="button"
                      onClick={() => {
                        cancelListening()
                        resetTranscript()
                      }}
                      className="w-9 h-9 rounded-full hover:bg-slate-100 text-slate-500 flex items-center justify-center transition cursor-pointer"
                      title="Hủy"
                    >
                      <X className="w-5 h-5" />
                    </button>

                    <div className="flex items-center gap-1 px-2 sm:px-4">
                      {[10, 16, 22, 14, 26, 18, 12, 24, 16, 28, 20, 14, 22, 16, 10, 24, 18, 12, 26, 16, 10].map((h, i) => (
                        <span
                          key={i}
                          className={`w-1 rounded-full transition-all duration-150 ${isListening ? 'bg-indigo-400/80 animate-pulse' : 'bg-slate-200'}`}
                          style={{ height: isListening ? `${h}px` : '6px', animationDelay: `${(i * 60) % 500}ms` }}
                        />
                      ))}
                    </div>

                    <div className="flex items-center gap-2">
                      {isListening ? (
                        <button
                          type="button"
                          onClick={stopListening}
                          className="w-9 h-9 rounded-xl bg-slate-900 hover:bg-slate-800 text-white flex items-center justify-center transition shadow-xs cursor-pointer"
                          title="Dừng thu âm"
                        >
                          <Square className="w-4 h-4 fill-white text-white rounded-xs" />
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => startListening()}
                          className="w-9 h-9 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 flex items-center justify-center transition cursor-pointer"
                          title="Nói tiếp"
                        >
                          <Mic className="w-4 h-4" />
                        </button>
                      )}

                      <button
                        type="button"
                        disabled={isProcessing || !spokenText.trim()}
                        onClick={() => {
                          handleSendMessage(spokenText)
                          resetTranscript()
                        }}
                        className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 text-white flex items-center justify-center transition shadow-md hover:scale-105 disabled:opacity-40 disabled:cursor-not-allowed shrink-0 cursor-pointer"
                        title="Gửi câu thoại"
                      >
                        <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                      </button>
                    </div>
                  </div>
                </div>
              ) : (
                /* Thanh Capsule bình thường - Trắng tinh, đổ bóng chuẩn Mazii */
                <div className="w-full bg-white rounded-full p-2 pl-3.5 pr-2 border border-slate-200/90 shadow-md flex items-center justify-between gap-3 hover:shadow-lg transition-shadow">
                  <button
                    type="button"
                    onClick={() => setIsKeyboardMode((p) => !p)}
                    className={`p-2 rounded-full transition cursor-pointer ${
                      isKeyboardMode
                        ? 'bg-purple-100 text-purple-700'
                        : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100'
                    }`}
                    title={isKeyboardMode ? 'Chuyển sang giọng nói' : 'Chuyển sang gõ phím'}
                  >
                    <Keyboard className="w-5 h-5" />
                  </button>

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
                      placeholder="Hãy thử gõ câu trả lời bằng tiếng Nhật..."
                      autoFocus
                      className="flex-1 bg-transparent border-none text-xs sm:text-sm text-slate-800 placeholder:text-slate-400 focus:outline-hidden"
                    />
                  ) : (
                    <div
                      onClick={() => {
                        resetTranscript()
                        startListening()
                      }}
                      className="flex-1 text-center text-xs sm:text-sm font-medium text-slate-500 hover:text-slate-800 cursor-pointer select-none truncate px-2"
                    >
                      {speechError ? (
                        <span className="text-rose-500 text-xs">⚠️ {speechError}</span>
                      ) : (
                        'Hãy thử nói gì đó'
                      )}
                    </div>
                  )}

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
                      className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white flex items-center justify-center transition shadow-md hover:scale-105 disabled:opacity-40 disabled:cursor-not-allowed shrink-0 cursor-pointer"
                      title="Gửi câu trả lời"
                    >
                      {isProcessing ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
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
                      className="w-10 h-10 rounded-full bg-linear-to-r from-purple-600 via-indigo-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white flex items-center justify-center transition shadow-md hover:scale-105 shrink-0 cursor-pointer"
                      title="Bấm để nói"
                    >
                      <Mic className="w-5 h-5" />
                    </button>
                  )}
                </div>
              )}

              {/* Dòng chữ disclaimer nhỏ */}
              <div className="text-[11px] text-slate-400 font-medium select-none">
                Được tạo ra bởi AI · Có thể có sai sót
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 1: DANH SÁCH NHIỆM VỤ (KHI BẤM NÚT "NHIỆM VỤ (0/3)") */}
      {/* ==================================================================== */}
      {showTasksModal && currentScenario.tasks && currentScenario.tasks.length > 0 && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 shadow-2xl relative space-y-4">
            <button
              onClick={() => setShowTasksModal(false)}
              className="absolute top-4 right-4 p-1 rounded-full hover:bg-slate-100 text-slate-400 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
            <h3 className="text-sm sm:text-base font-bold text-slate-800 flex items-center gap-2">
              <Target className="w-4 h-4 text-indigo-600" />
              <span>Nhiệm vụ trong tình huống ({completedTasks.length}/{totalTasks})</span>
            </h3>
            <ul className="space-y-2.5 pt-1">
              {currentScenario.tasks.map((task: ScenarioTask) => {
                const isCompleted = completedTasks.includes(task.id)
                return (
                  <li
                    key={task.id}
                    className={`flex items-start gap-2.5 p-3 rounded-xl border transition ${
                      isCompleted ? 'bg-emerald-50 border-emerald-200 text-emerald-900' : 'bg-slate-50 border-slate-200 text-slate-700'
                    }`}
                  >
                    <CheckCircle2 className={`w-4 h-4 mt-0.5 shrink-0 ${isCompleted ? 'text-emerald-600' : 'text-slate-300'}`} />
                    <span className="text-xs sm:text-sm font-medium">{task.text}</span>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 2: SHADOWING / LUYỆN ĐỌC THEO CÂU MẪU */}
      {/* ==================================================================== */}
      {shadowingLine && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl relative space-y-4">
            <button
              onClick={() => {
                if (isListening) stopListening()
                setShadowingLine(null)
              }}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-base font-bold text-slate-800 flex items-center gap-2">
              <Mic className="w-5 h-5 text-indigo-600" />
              <span>Luyện phát âm theo mẫu (Shadowing)</span>
            </h3>

            <div className="p-4 rounded-xl bg-[#F7F5FC] text-center space-y-2">
              <FuriganaText text={shadowingLine.text} size="lg" />
              <button
                type="button"
                onClick={() => playTts(shadowingLine.text)}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-white border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50 shadow-2xs mt-2 transition cursor-pointer"
              >
                <Volume2 className="w-3.5 h-3.5 text-indigo-600" />
                <span>Nghe lại câu mẫu</span>
              </button>
            </div>

            {(transcript || interimTranscript) && (
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-center space-y-1">
                <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                  Giọng của bạn thu được:
                </span>
                <p className="text-sm font-medium text-slate-800">
                  {cleanDuplicateSpeech((transcript + ' ' + interimTranscript).trim())}
                </p>
              </div>
            )}

            <div className="flex flex-col items-center justify-center pt-2">
              <button
                type="button"
                onClick={handleToggleShadowingMic}
                className={`w-16 h-16 rounded-full flex items-center justify-center transition shadow-lg cursor-pointer ${
                  isListening
                    ? 'bg-rose-500 animate-pulse text-white ring-4 ring-rose-200 scale-105'
                    : 'bg-linear-to-r from-purple-600 to-blue-600 text-white hover:scale-105'
                }`}
              >
                <Mic className="w-7 h-7" />
              </button>
              <span className="text-xs font-medium text-slate-600 mt-2.5">
                {isListening ? 'Đang lắng nghe... Hãy đọc câu tiếng Nhật trên' : 'Nhấn vào micro để bắt đầu đọc'}
              </span>
            </div>

            {speechError && (
              <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs text-center font-medium">
                {speechError}
              </div>
            )}

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
                      resetTranscript()
                      setShadowingScore(null)
                      setShadowingResult(null)
                      startListening()
                    }}
                    className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition cursor-pointer"
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
      {/* MODAL 3: LUYỆN TẬP FLASHCARD TỪ VỰNG */}
      {/* ==================================================================== */}
      {showVocabPracticeModal && currentScenario.vocabulary && currentScenario.vocabulary.length > 0 && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl relative space-y-5">
            <button
              onClick={() => setShowVocabPracticeModal(false)}
              className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center justify-between text-xs text-slate-500 font-semibold">
              <span>Luyện nhớ từ vựng</span>
              <span>{practiceVocabIndex + 1} / {currentScenario.vocabulary.length}</span>
            </div>

            <div
              onClick={() => setShowVocabAnswer((prev) => !prev)}
              className="min-h-[180px] p-6 rounded-2xl bg-[#F8FAFC] border-2 border-dashed border-indigo-200 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-slate-50 transition"
            >
              <div className="text-2xl font-bold text-slate-800 mb-2">
                {currentScenario.vocabulary[practiceVocabIndex].word}
              </div>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  playTts(currentScenario.vocabulary[practiceVocabIndex].word)
                }}
                className="p-1.5 rounded-full bg-white shadow-xs text-slate-500 hover:text-indigo-600 mb-3 cursor-pointer"
              >
                <Volume2 className="w-4 h-4" />
              </button>

              {showVocabAnswer ? (
                <div className="text-sm font-semibold text-indigo-700 animate-fade-in">
                  {currentScenario.vocabulary[practiceVocabIndex].meaning}
                </div>
              ) : (
                <div className="text-xs text-slate-400">(Nhấn vào thẻ để xem nghĩa tiếng Việt)</div>
              )}
            </div>

            <div className="flex items-center justify-between gap-3">
              <button
                type="button"
                disabled={practiceVocabIndex === 0}
                onClick={() => {
                  setPracticeVocabIndex((p) => Math.max(0, p - 1))
                  setShowVocabAnswer(false)
                }}
                className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-40 cursor-pointer"
              >
                Từ trước
              </button>

              <button
                type="button"
                onClick={() => {
                  if (practiceVocabIndex < currentScenario.vocabulary.length - 1) {
                    setPracticeVocabIndex((p) => p + 1)
                    setShowVocabAnswer(false)
                  } else {
                    setShowVocabPracticeModal(false)
                  }
                }}
                className="px-5 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold hover:bg-blue-700 shadow-xs cursor-pointer"
              >
                {practiceVocabIndex < currentScenario.vocabulary.length - 1 ? 'Từ kế tiếp' : 'Hoàn thành'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default SpeakingStudyView
