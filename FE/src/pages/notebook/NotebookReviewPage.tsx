import React, { useState, useEffect, useRef, useMemo } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import {
  Button,
  Tag,
  Spin,
  Alert,
  Tooltip,
  message,
} from 'antd'
import {
  SoundOutlined,
  CheckCircleOutlined,
  TrophyOutlined,
  ReloadOutlined,
  AudioOutlined,
  EditOutlined,
  RetweetOutlined,
  SearchOutlined,
  SwapOutlined,
  CaretRightOutlined,
  PauseOutlined,
  UndoOutlined,
  CloseOutlined,
  QuestionOutlined,
  CheckOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import {
  useNotebookDetail,
  useNotebookReviewItems,
  useSubmitReviewMutation,
} from '@/hooks/useNotebooks'
import { HanziWriterPractice } from '@/components/dictionary/HanziWriterPractice'
import { DictionaryWordDetailModal, KanjiPracticeModal } from '@/components/dictionary'
import { useKanjiDetail } from '@/hooks'
import { NOTEBOOK_ITEM_TYPES, NOTEBOOK_ITEM_TYPE_META, itemTypeOf } from '@/components/notebook/itemTypeMeta'
import { useSidebarStore } from '@/stores'
import type { NotebookItemType, NotebookReviewItem, SrsRating } from '@/types'

type ReviewMode = 'flashcard' | 'quiz' | 'speaking' | 'writing'

// Mascot shiba của hệ thống (/mascot/standing.svg)
const SystemMascot: React.FC<{ size?: number; className?: string }> = ({
  size = 140,
  className = '',
}) => (
  <img
    src="/mascot/standing.svg"
    alt="Mascot"
    style={{ width: size, height: size }}
    className={`object-contain select-none pointer-events-none shrink-0 ${className}`}
  />
)

// Huy hiệu kết quả Đúng/Sai kèm mascot shiba
const QuizResultBadge: React.FC<{ isCorrect: boolean }> = ({ isCorrect }) => (
  <div className="relative shrink-0 w-16 h-16 md:w-20 md:h-20 flex items-center justify-center">
    <div
      className={`w-14 h-14 md:w-16 md:h-16 rounded-2xl flex items-center justify-center p-1.5 border-2 shadow-xs transition-colors ${
        isCorrect
          ? 'bg-emerald-50 border-emerald-400'
          : 'bg-red-50 border-red-400'
      }`}
    >
      <img
        src="/mascot/standing.svg"
        alt="Mascot"
        className="w-full h-full object-contain select-none pointer-events-none"
      />
    </div>
    <div
      className={`absolute -top-1 -right-1 w-6 h-6 md:w-7 md:h-7 rounded-full flex items-center justify-center text-white text-xs md:text-sm font-black shadow-md border-2 border-white ${
        isCorrect ? 'bg-emerald-500' : 'bg-red-500'
      }`}
    >
      {isCorrect ? '✓' : '✕'}
    </div>
  </div>
)

// Làm sạch cách đọc Furigana (loại bỏ các biến thể trùng lặp do từ điển trả về ví dụ: "そんしょう そんしょぅ")
const formatReading = (reading?: string | null): string => {
  if (!reading) return ''
  const parts = reading.trim().split(/[\s,、;/]+/).filter(Boolean)
  return parts[0] || ''
}

/** Nút mở modal luyện viết & chấm nét cho thẻ Chữ Hán (nạp nét chuẩn từ từ điển khi cần). */
const KanjiPracticeLauncher: React.FC<{ item: NotebookReviewItem }> = ({ item }) => {
  const [open, setOpen] = useState(false)
  const { data: kanjiDetail, isLoading } = useKanjiDetail(open ? item.word : undefined)
  return (
    <>
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation()
          setOpen(true)
        }}
        className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-semibold shadow-2xs transition-colors cursor-pointer"
      >
        <EditOutlined /> Luyện viết
      </button>
      {open && (
        <div onClick={(e) => e.stopPropagation()}>
          <KanjiPracticeModal
            isOpen={open && !isLoading}
            onClose={() => setOpen(false)}
            character={item.word}
            meaningVi={item.meaning ?? undefined}
            hanViet={item.sinoVietnamese ?? undefined}
            strokes={kanjiDetail?.strokes ?? []}
          />
        </div>
      )}
    </>
  )
}

export const NotebookReviewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const sidebarCollapsed = useSidebarStore((s) => s.collapsed)
  const notebookId = id ? parseInt(id, 10) : 0
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  // Chế độ học từ URL param (default là flashcard)
  const paramMode = (searchParams.get('mode') as ReviewMode) || 'flashcard'
  const mode: ReviewMode = paramMode
  const [subPracticeTab, setSubPracticeTab] = useState<'speaking' | 'writing'>(
    paramMode === 'writing' ? 'writing' : 'speaking'
  )

  // Danh sách từ và vị trí hiện tại
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isFlipped, setIsFlipped] = useState(false)
  const [isCompleted, setIsCompleted] = useState(false)
  const [autoPlay, setAutoPlay] = useState(false)
  const autoPlayTimerRef = useRef<any>(null)

  // State Trắc nghiệm & Điền chỗ trống
  const [selectedOption, setSelectedOption] = useState<string | null>(null)
  const [isAnswerChecked, setIsAnswerChecked] = useState(false)
  const [isQuizCorrect, setIsQuizCorrect] = useState<boolean | null>(null)
  const [correctCount, setCorrectCount] = useState(0)

  // Tùy chọn Quizz từ URL params
  const typesParam = searchParams.get('types')
  // Loại kiến thức cần ôn (VOCAB,KANJI,GRAMMAR) — bỏ trống = tất cả
  const itemsParam = searchParams.get('items')
  const activeItemTypes = useMemo<NotebookItemType[] | undefined>(() => {
    if (!itemsParam) return undefined
    const list = itemsParam
      .split(',')
      .filter((t): t is NotebookItemType => (NOTEBOOK_ITEM_TYPES as string[]).includes(t))
    return list.length > 0 ? list : undefined
  }, [itemsParam])
  const audioParam = searchParams.get('audio')
  const srsParam = searchParams.get('srs')
  const isAudioParamEnabled = audioParam !== 'false'
  const isSrsEnabled = srsParam !== 'false'

  const activeQuizTypes = useMemo(() => {
    if (!typesParam) return ['multiple_choice']
    const validKeys = new Set(['multiple_choice', 'listening', 'writing', 'matching'])
    const list = typesParam.split(',').filter((k) => validKeys.has(k))
    return list.length > 0 ? list : ['multiple_choice']
  }, [typesParam])

  // State dạng bài Luyện viết trong Quizz
  const [typedInput, setTypedInput] = useState('')
  const [writingChecked, setWritingChecked] = useState(false)
  const [isWritingCorrect, setIsWritingCorrect] = useState<boolean | null>(null)

  // State dạng bài Nối từ trong Quizz
  const [matchingWordList, setMatchingWordList] = useState<string[]>([])
  const [matchingMeaningList, setMatchingMeaningList] = useState<string[]>([])
  const [selectedMatchWord, setSelectedMatchWord] = useState<string | null>(null)
  const [selectedMatchMeaning, setSelectedMatchMeaning] = useState<string | null>(null)
  const [matchedPairs, setMatchedPairs] = useState<Record<string, string>>({})
  const [mismatchedWord, setMismatchedWord] = useState<string | null>(null)
  const [mismatchedMeaning, setMismatchedMeaning] = useState<string | null>(null)

  // State Luyện nói (Web Speech API)
  const [isListening, setIsListening] = useState(false)
  const [transcript, setTranscript] = useState('')
  const [speechAccuracy, setSpeechAccuracy] = useState<number | null>(null)

  // Modal tra cứu nhanh
  const [quickSearchOpen, setQuickSearchOpen] = useState(false)

  // Query & mutation
  const { data: notebook } = useNotebookDetail(notebookId)
  const {
    data: rawItems = [],
    isLoading,
    refetch,
  } = useNotebookReviewItems(notebookId, 25, activeItemTypes)
  const submitReview = useSubmitReviewMutation(notebookId)

  // State danh sách từ có thể xáo trộn (shuffle)
  const [items, setItems] = useState<NotebookReviewItem[]>([])

  useEffect(() => {
    if (rawItems.length > 0 && items.length === 0) {
      setItems(rawItems)
    }
  }, [rawItems, items.length])

  const currentItem: NotebookReviewItem | undefined = items[currentIndex]

  // Hàm gửi đánh giá SRS (chỉ lưu tiến độ khi người dùng bật chế độ SRS)
  const sendReviewRating = (wordId: number, rating: SrsRating) => {
    if (isSrsEnabled) {
      submitReview.mutate({ wordId, rating })
    }
  }

  // Dạng câu hỏi của từ hiện tại: xoay vòng giữa các dạng bài từ vựng được chọn
  const currentQuizType = useMemo(() => {
    if (activeQuizTypes.length === 0) return 'multiple_choice'
    const type = activeQuizTypes[currentIndex % activeQuizTypes.length]
    // Thẻ Ngữ pháp: nghe / gõ lại tên mẫu câu không có ý nghĩa → chuyển sang trắc nghiệm
    if (currentItem && itemTypeOf(currentItem) === 'GRAMMAR' && (type === 'listening' || type === 'writing')) {
      return 'multiple_choice'
    }
    return type
  }, [activeQuizTypes, currentIndex, currentItem])

  // Reset khi chuyển câu
  useEffect(() => {
    setIsFlipped(false)
    setSelectedOption(null)
    setIsAnswerChecked(false)
    setIsQuizCorrect(null)
    setTranscript('')
    setSpeechAccuracy(null)
    setTypedInput('')
    setWritingChecked(false)
    setIsWritingCorrect(null)

    if (!currentItem) return

    // Tự động phân bổ ngẫu nhiên dạng bài Luyện nói hoặc Luyện viết cho từng từ
    if (mode === 'speaking' || mode === 'writing') {
      // Ngữ pháp không luyện viết nét → chỉ luyện phát âm
      const chars =
        itemTypeOf(currentItem) === 'GRAMMAR' ? [] : (currentItem.word || '').match(/[\u4e00-\u9faf]/g) || []
      if (chars.length === 0) {
        // Từ thuần Kana không chứa chữ Hán -> chỉ Luyện phát âm
        setSubPracticeTab('speaking')
      } else {
        // Từ có chữ Hán -> ngẫu nhiên / đan xen 50/50 giữa phát âm và viết Kanji
        const isWriting = ((currentItem.id ?? 0) + currentIndex) % 2 === 1
        setSubPracticeTab(isWriting ? 'writing' : 'speaking')
      }
    }

    // Tự động phát âm thanh ở dạng bài Nghe nếu người dùng bật âm thanh
    if (mode === 'quiz' && currentQuizType === 'listening' && isAudioParamEnabled) {
      playAudio(currentItem)
    }

    // Thiết lập danh sách Nối từ
    if (mode === 'quiz' && currentQuizType === 'matching') {
      if (items.length > 0) {
        const startIdx = Math.floor(currentIndex / 6) * 6
        let pool = items.slice(startIdx, startIdx + 6)
        if (pool.length < 3) {
          pool = items.slice(0, Math.min(items.length, 6))
        }
        setMatchingWordList([...pool.map((p) => p.word)].sort(() => Math.random() - 0.5))
        setMatchingMeaningList(
          [...pool.map((p) => p.meaning || '')].sort(() => Math.random() - 0.5)
        )
        setMatchedPairs({})
        setSelectedMatchWord(null)
        setSelectedMatchMeaning(null)
        setMismatchedWord(null)
        setMismatchedMeaning(null)
      }
    }
  }, [currentIndex, mode, currentQuizType, isAudioParamEnabled, items])

  // Phát âm tiếng Nhật
  const playAudio = (item?: NotebookReviewItem) => {
    if (!item) return
    if (item.audioUrl) {
      const audio = new Audio(item.audioUrl)
      audio.play().catch(() => playSpeech(item.word))
    } else {
      playSpeech(item.word)
    }
  }

  const playSpeech = (text: string) => {
    if (!('speechSynthesis' in window)) return
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'ja-JP'
    utterance.rate = 0.85
    window.speechSynthesis.speak(utterance)
  }

  // Tự động phát âm khi hiện từ mới (nếu đang ở chế độ autoPlay)
  useEffect(() => {
    if (autoPlay && currentItem && !isCompleted) {
      playAudio(currentItem)
      autoPlayTimerRef.current = setTimeout(() => {
        setIsFlipped(true)
        autoPlayTimerRef.current = setTimeout(() => {
          handleRating('GOOD')
        }, 2200)
      }, 2500)
    }
    return () => {
      if (autoPlayTimerRef.current) clearTimeout(autoPlayTimerRef.current)
    }
  }, [autoPlay, currentIndex, isCompleted])

  // Xử lý gửi đánh giá SRS (Flashcard)
  const handleRating = async (rating: SrsRating) => {
    if (!currentItem) return
    try {
      if (isSrsEnabled) {
        await submitReview.mutateAsync({
          wordId: currentItem.id,
          rating,
        })
      }
      if (currentIndex + 1 < items.length) {
        setCurrentIndex((prev) => prev + 1)
      } else {
        setIsCompleted(true)
      }
    } catch {
      message.error('Không thể lưu tiến độ ôn tập!')
    }
  }

  // Quay lại từ trước đó (Undo button)
  const handlePreviousWord = () => {
    if (currentIndex > 0) {
      setCurrentIndex((prev) => prev - 1)
    }
  }

  // Xáo trộn danh sách từ
  const handleShuffle = () => {
    const shuffled = [...items].sort(() => Math.random() - 0.5)
    setItems(shuffled)
    setCurrentIndex(0)
    message.success('Đã xáo trộn danh sách từ vựng!')
  }

  // Phím tắt bàn phím cho Flashcard
  useEffect(() => {
    if (mode !== 'flashcard' || isCompleted || !currentItem) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space') {
        e.preventDefault()
        setIsFlipped((prev) => !prev)
      } else if (isFlipped) {
        if (e.key === '1') handleRating('AGAIN')
        if (e.key === '2') handleRating('HARD')
        if (e.key === '3') handleRating('GOOD')
        if (e.key === '4') handleRating('EASY')
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [mode, isFlipped, isCompleted, currentItem])

  const advanceQuizNext = () => {
    setIsAnswerChecked(false)
    setIsQuizCorrect(null)
    setSelectedOption(null)
    if (currentIndex + 1 < items.length) {
      setCurrentIndex((prev) => prev + 1)
    } else {
      setIsCompleted(true)
    }
  }

  const advanceMatching = () => {
    const nextIdx = Math.floor(currentIndex / 6) * 6 + 6
    if (nextIdx < items.length) {
      setCurrentIndex(nextIdx)
    } else {
      setIsCompleted(true)
    }
  }

  // Xử lý chọn đáp án Quizz (chỉ đánh dấu chọn, chưa kiểm tra, KHÔNG tự ý nhảy câu)
  const handleSelectQuizOption = (opt: string) => {
    if (isAnswerChecked) return
    setSelectedOption(opt)
  }

  // Xử lý bấm nút "Kiểm tra" để kiểm tra đáp án
  const handleCheckQuizAnswer = () => {
    if (isAnswerChecked || !selectedOption || !currentItem) return

    let targetAnswer = currentItem.meaning || ''
    if (currentQuizType === 'listening') {
      targetAnswer = currentItem.word
    } else if (currentQuizType === 'multiple_choice') {
      const isMeaningPrompt =
        currentIndex % 2 === 0 &&
        currentItem.quizWordOptions &&
        currentItem.quizWordOptions.length >= 2
      targetAnswer = isMeaningPrompt ? currentItem.word : (currentItem.meaning || '')
    }

    const isCorrect = selectedOption === targetAnswer
    setIsAnswerChecked(true)
    setIsQuizCorrect(isCorrect)

    if (isCorrect) {
      setCorrectCount((prev) => prev + 1)
      sendReviewRating(currentItem.id, 'GOOD')
    } else {
      sendReviewRating(currentItem.id, 'AGAIN')
    }
  }

  // Phím tắt bàn phím cho Quizz (Enter để Kiểm tra hoặc Tiếp tục)
  useEffect(() => {
    if (mode !== 'quiz' || isCompleted || !currentItem) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return
      }

      if (e.key === 'Enter') {
        e.preventDefault()
        if (currentQuizType === 'multiple_choice' || currentQuizType === 'listening') {
          if (!isAnswerChecked) {
            if (selectedOption) {
              handleCheckQuizAnswer()
            }
          } else {
            advanceQuizNext()
          }
        }
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [mode, isCompleted, currentItem, isAnswerChecked, selectedOption, currentQuizType, isQuizCorrect])

  // Xử lý Luyện viết trong Quizz
  const handleCheckWriting = (inputOverride?: string) => {
    if (writingChecked || !currentItem) return
    const input = (inputOverride !== undefined ? inputOverride : typedInput).trim().toLowerCase()
    const wordTarget = currentItem.word.toLowerCase()
    const validReadings = (currentItem.reading || '')
      .split(/[\s,、;/]+/)
      .map((r) => r.trim().toLowerCase())
      .filter(Boolean)

    const isCorrect = input === wordTarget || validReadings.includes(input)
    setWritingChecked(true)
    setIsWritingCorrect(isCorrect)

    if (isCorrect) {
      setCorrectCount((prev) => prev + 1)
      message.success('Chính xác!')
      sendReviewRating(currentItem.id, 'GOOD')
    } else {
      const cleanReading = formatReading(currentItem.reading)
      message.error(
        `Chưa chính xác! Đáp án đúng là: ${currentItem.word}${cleanReading ? ` (${cleanReading})` : ''}`
      )
      sendReviewRating(currentItem.id, 'AGAIN')
    }
  }

  // Khi bấm "Không biết đáp án?": Điền từ đúng vào ô, hiện đáp án đúng, KHÔNG tự ý nhảy câu
  const handleRevealWritingAnswer = () => {
    if (writingChecked || !currentItem) return
    setTypedInput(currentItem.word)
    setWritingChecked(true)
    setIsWritingCorrect(false)
    sendReviewRating(currentItem.id, 'AGAIN')
  }

  // Xử lý Nối từ trong Quizz
  const handleSelectMatchingWord = (w: string) => {
    if (matchedPairs[w] || mismatchedWord) return
    if (selectedMatchWord === w) {
      setSelectedMatchWord(null)
      return
    }
    setSelectedMatchWord(w)
    if (selectedMatchMeaning) {
      checkMatchPair(w, selectedMatchMeaning)
    }
  }

  const handleSelectMatchingMeaning = (m: string) => {
    if (Object.values(matchedPairs).includes(m) || mismatchedMeaning) return
    if (selectedMatchMeaning === m) {
      setSelectedMatchMeaning(null)
      return
    }
    setSelectedMatchMeaning(m)
    if (selectedMatchWord) {
      checkMatchPair(selectedMatchWord, m)
    }
  }

  const checkMatchPair = (w: string, m: string) => {
    const matchedItem = items.find((it) => it.word === w)
    const isCorrect = matchedItem?.meaning === m

    if (isCorrect) {
      if (matchedItem) {
        sendReviewRating(matchedItem.id, 'GOOD')
      }
      const nextPairs = { ...matchedPairs, [w]: m }
      setMatchedPairs(nextPairs)
      setSelectedMatchWord(null)
      setSelectedMatchMeaning(null)

      if (Object.keys(nextPairs).length === matchingWordList.length) {
        setCorrectCount((prev) => prev + 1)
        setTimeout(() => advanceMatching(), 1000)
      }
    } else {
      // Khi chọn 2 ô không khớp: nháy đỏ 2 ô đó lên trong 650ms, không hiện thông báo
      setMismatchedWord(w)
      setMismatchedMeaning(m)
      if (matchedItem) {
        sendReviewRating(matchedItem.id, 'AGAIN')
      }
      setTimeout(() => {
        setMismatchedWord(null)
        setMismatchedMeaning(null)
        setSelectedMatchWord(null)
        setSelectedMatchMeaning(null)
      }, 650)
    }
  }

  // Web Speech API - Luyện nói
  const startSpeechRecognition = () => {
    const SpeechRecognition =
      (window as unknown as { SpeechRecognition?: any; webkitSpeechRecognition?: any })
        .SpeechRecognition ||
      (window as unknown as { SpeechRecognition?: any; webkitSpeechRecognition?: any })
        .webkitSpeechRecognition

    if (!SpeechRecognition) {
      message.warning('Trình duyệt của bạn chưa bật hoặc không hỗ trợ Web Speech API!')
      return
    }

    const recognition = new SpeechRecognition()
    recognition.lang = 'ja-JP'
    recognition.interimResults = false
    recognition.maxAlternatives = 1

    setIsListening(true)
    setTranscript('')
    setSpeechAccuracy(null)

    recognition.onresult = (event: any) => {
      const speechText = event.results[0][0].transcript
      setTranscript(speechText)
      setIsListening(false)

      if (currentItem) {
        const simWord = calculateStringSimilarity(speechText.trim(), currentItem.word.trim())
        const targetReading = formatReading(currentItem.reading)
        const simReading = targetReading
          ? calculateStringSimilarity(speechText.trim(), targetReading.trim())
          : 0
        const accuracy = Math.max(simWord, simReading)
        setSpeechAccuracy(accuracy)

        if (accuracy >= 80) {
          message.success('Phát âm rất tốt!')
          sendReviewRating(currentItem.id, 'GOOD')
          setTimeout(() => {
            if (currentIndex + 1 < items.length) {
              setCurrentIndex((prev) => prev + 1)
            } else {
              setIsCompleted(true)
            }
          }, 1500)
        } else if (accuracy >= 50) {
          message.info('Phát âm tương đối gần đúng!')
          sendReviewRating(currentItem.id, 'HARD')
        } else {
          message.warning('Cần cải thiện phát âm!')
          sendReviewRating(currentItem.id, 'AGAIN')
        }
      }
    }

    recognition.onerror = () => {
      setIsListening(false)
      message.error('Không nhận diện được giọng nói hoặc quyền Micro bị từ chối!')
    }

    recognition.onend = () => {
      setIsListening(false)
    }

    try {
      recognition.start()
    } catch {
      setIsListening(false)
    }
  }

  const calculateStringSimilarity = (s1: string, s2: string): number => {
    if (s1 === s2) return 100
    const len1 = s1.length
    const len2 = s2.length
    if (len1 === 0 || len2 === 0) return 0
    const matrix: number[][] = []
    for (let i = 0; i <= len1; i++) matrix[i] = [i]
    for (let j = 0; j <= len2; j++) matrix[0][j] = j
    for (let i = 1; i <= len1; i++) {
      for (let j = 1; j <= len2; j++) {
        const cost = s1[i - 1] === s2[j - 1] ? 0 : 1
        matrix[i][j] = Math.min(
          matrix[i - 1][j] + 1,
          matrix[i][j - 1] + 1,
          matrix[i - 1][j - 1] + cost
        )
      }
    }
    const dist = matrix[len1][len2]
    const maxLen = Math.max(len1, len2)
    return Math.round(((maxLen - dist) / maxLen) * 100)
  }

  const kanjiChars = currentItem?.word
    ? (currentItem.word.match(/[\u4e00-\u9faf]/g) || [])
    : []

  if (isLoading) {
    return (
      <div className="min-h-[500px] flex items-center justify-center">
        <Spin size="large" />
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="max-w-xl mx-auto py-16 px-4 text-center">
        <CheckCircleOutlined className="text-5xl text-emerald-500 mb-4" />
        <h2 className="text-2xl font-bold text-gray-800">Sổ tay này chưa có từ vựng!</h2>
        <p className="text-gray-500 mt-2 mb-6">
          Vui lòng thêm từ vựng vào sổ tay trước khi bắt đầu luyện tập.
        </p>
        <Button
          type="primary"
          onClick={() => navigate(notebookId ? `/notebooks/${notebookId}` : '/notebooks')}
          className="bg-blue-600 rounded-xl"
        >
          Quay lại sổ tay
        </Button>
      </div>
    )
  }

  // Màn hình kết thúc buổi ôn tập
  if (isCompleted) {
    return (
      <div className="max-w-lg mx-auto py-12 px-4 text-center space-y-6">
        <div className="w-20 h-20 mx-auto bg-amber-50 rounded-full flex items-center justify-center border-2 border-amber-200 shadow-inner">
          <TrophyOutlined className="text-4xl text-amber-500" />
        </div>

        <div>
          <h2 className="text-2xl font-extrabold text-gray-800">
            Hoàn thành buổi ôn tập!
          </h2>
          <p className="text-gray-500 text-sm mt-1">
            Bạn đã ôn tập xong {items.length} từ trong đợt này.
          </p>
        </div>

        {mode === 'quiz' && (
          <div className="bg-blue-50/70 p-4 rounded-2xl border border-blue-100 flex items-center justify-around">
            <div>
              <div className="text-xs text-blue-600 font-semibold uppercase">Đúng</div>
              <div className="text-2xl font-bold text-blue-700">{correctCount}</div>
            </div>
            <div className="h-8 w-px bg-blue-200" />
            <div>
              <div className="text-xs text-gray-500 font-semibold uppercase">Tổng câu</div>
              <div className="text-2xl font-bold text-gray-700">{items.length}</div>
            </div>
            <div className="h-8 w-px bg-blue-200" />
            <div>
              <div className="text-xs text-emerald-600 font-semibold uppercase">Độ chính xác</div>
              <div className="text-2xl font-bold text-emerald-600">
                {Math.round((correctCount / items.length) * 100)}%
              </div>
            </div>
          </div>
        )}

        <div className="flex justify-center gap-3 pt-2">
          <Button
            size="large"
            icon={<ReloadOutlined />}
            onClick={() => {
              setCurrentIndex(0)
              setIsCompleted(false)
              setCorrectCount(0)
              refetch()
            }}
            className="rounded-xl"
          >
            Ôn tập tiếp
          </Button>
          <Button
            type="primary"
            size="large"
            onClick={() => navigate(notebookId ? `/notebooks/${notebookId}` : '/notebooks')}
            className="bg-blue-600 hover:bg-blue-700 rounded-xl"
          >
            Quay lại sổ tay
          </Button>
        </div>
      </div>
    )
  }

  const formatStepNumber = (idx: number, total: number) => {
    const currentStr = (idx + 1).toString().padStart(2, '0')
    const totalStr = total.toString().padStart(2, '0')
    return `${currentStr}/${totalStr}`
  }

  return (
    <div className="min-h-[calc(100vh-5rem)] bg-gray-100/70 pb-4 flex flex-col justify-between rounded-2xl">
      {/* 1. THANH TIẾN ĐỘ Ở RẤT ĐẦU TRANG */}
      <div className="w-full h-1 bg-gray-200">
        <div
          className="h-full bg-blue-600 transition-all duration-300"
          style={{ width: `${Math.round(((currentIndex + 1) / items.length) * 100)}%` }}
        />
      </div>

      {/* 2. CHẾ ĐỘ 1: FLASHCARD (Chuẩn Mazii) */}
      {mode === 'flashcard' && currentItem && (
        <div className="max-w-2xl w-full mx-auto px-4 py-3 space-y-4 flex-1 flex flex-col justify-between">
          {/* Header trên cùng: [✕ Thoát] bên trái, FlashCard - 01/25 ở giữa, Báo lỗi bên phải (Chuẩn Mazii) */}
          <div className="flex items-center justify-between border-b border-gray-200/80 pb-3">
            <button
              type="button"
              onClick={() => navigate(notebookId ? `/notebooks/${notebookId}` : '/notebooks')}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-700 hover:text-gray-900 bg-surface border border-gray-200 px-3.5 py-1.5 rounded-xl shadow-2xs transition-colors cursor-pointer"
            >
              <CloseOutlined className="text-xs" />
              <span>Thoát</span>
            </button>

            <div className="text-base font-bold text-blue-600">
              FlashCard - {formatStepNumber(currentIndex, items.length)}
            </div>

            <button
              type="button"
              onClick={() => message.info('Cảm ơn bạn! Đội ngũ sẽ tiếp tục hoàn thiện dữ liệu bài học.')}
              className="inline-flex items-center gap-1 text-xs font-medium text-gray-500 hover:text-gray-800 transition-colors cursor-pointer"
            >
              <ExclamationCircleOutlined className="text-xs text-amber-500" />
              <span>Báo lỗi / Góp ý</span>
            </button>
          </div>

          {/* Thanh sub-toolbar trên thẻ: [Tra cứu từ] bên trái, [Xáo trộn] & [Tự động] bên phải */}
          <div className="flex items-center justify-between pt-1">
            <button
              type="button"
              onClick={() => {
                setQuickSearchOpen(true)
              }}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-surface border border-gray-200 text-xs font-semibold text-gray-700 hover:text-blue-600 hover:border-blue-400 shadow-2xs transition-all cursor-pointer"
            >
              <SearchOutlined className="text-gray-400" />
              <span>Tra cứu từ</span>
            </button>

            <div className="flex items-center gap-2">
              <Tooltip title="Xáo trộn thứ tự từ">
                <button
                  type="button"
                  onClick={handleShuffle}
                  className="w-9 h-9 rounded-xl bg-surface border border-gray-200 text-gray-600 hover:text-blue-600 hover:border-blue-400 flex items-center justify-center shadow-2xs transition-all cursor-pointer text-sm"
                >
                  <SwapOutlined />
                </button>
              </Tooltip>

              <Tooltip title={autoPlay ? 'Tạm dừng tự động' : 'Tự động phát âm & chuyển thẻ'}>
                <button
                  type="button"
                  onClick={() => setAutoPlay(!autoPlay)}
                  className={`w-9 h-9 rounded-xl border flex items-center justify-center shadow-2xs transition-all cursor-pointer text-sm ${
                    autoPlay
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'bg-surface border-gray-200 text-gray-600 hover:text-blue-600'
                  }`}
                >
                  {autoPlay ? <PauseOutlined /> : <CaretRightOutlined />}
                </button>
              </Tooltip>
            </div>
          </div>

          {/* Khung Thẻ Flashcard chính to hơn, nổi bật hơn (Chuẩn Mazii) */}
          <div className="flex-1 flex items-center justify-center my-1">
            <div
              onClick={() => setIsFlipped(!isFlipped)}
              className="w-full max-w-[520px] min-h-[480px] md:min-h-[520px] bg-surface border-4 border-blue-500 rounded-[36px] p-8 md:p-10 shadow-sm flex flex-col justify-between items-center text-center cursor-pointer select-none transition-all hover:shadow-md relative"
            >
              {/* Vị trí câu nhỏ góc trên trái */}
              <div className="absolute top-5 left-7 text-xs text-gray-400 font-semibold">
                {currentIndex + 1} / {items.length}
              </div>
              <Tag
                color={NOTEBOOK_ITEM_TYPE_META[itemTypeOf(currentItem)].tagColor}
                className="!absolute top-4 right-6 !m-0 font-semibold"
              >
                {NOTEBOOK_ITEM_TYPE_META[itemTypeOf(currentItem)].label}
              </Tag>

              {/* Nội dung trung tâm thẻ */}
              <div className="my-auto space-y-4 w-full">
                {!isFlipped ? (
                  <>
                    {/* Mặt trước theo loại thẻ: Từ vựng (từ + cách đọc), Chữ Hán (chữ + On/Kun), Ngữ pháp (mẫu + cấu trúc) */}
                    {itemTypeOf(currentItem) === 'GRAMMAR' ? (
                      <>
                        <div className="text-3xl md:text-4xl font-black text-rose-600 break-words">
                          {currentItem.word}
                        </div>
                        {(currentItem.reading || currentItem.grammarStructure) && (
                          <div className="inline-block bg-rose-50 border border-rose-200 rounded-xl px-4 py-2 text-sm md:text-base font-bold text-gray-800 font-mono break-words">
                            {currentItem.reading || currentItem.grammarStructure}
                          </div>
                        )}
                      </>
                    ) : (
                      <>
                        <div
                          className={`font-black tracking-wide break-words ${
                            itemTypeOf(currentItem) === 'KANJI'
                              ? 'text-7xl md:text-8xl font-serif text-amber-600'
                              : 'text-5xl md:text-6xl text-blue-600'
                          }`}
                        >
                          {currentItem.word}
                        </div>

                        {/* Hán Việt */}
                        {(currentItem.sinoVietnamese || currentItem.hanViet) && (
                          <div className="text-xs md:text-sm font-bold text-gray-400 tracking-wider">
                            「 {currentItem.sinoVietnamese || currentItem.hanViet} 」
                          </div>
                        )}

                        {itemTypeOf(currentItem) === 'KANJI' &&
                        (currentItem.onyomi?.length || currentItem.kunyomi?.length) ? (
                          <div className="text-sm md:text-base font-semibold text-gray-600 space-y-0.5">
                            {currentItem.onyomi && currentItem.onyomi.length > 0 && (
                              <div>On: {currentItem.onyomi.join('、')}</div>
                            )}
                            {currentItem.kunyomi && currentItem.kunyomi.length > 0 && (
                              <div>Kun: {currentItem.kunyomi.join('、')}</div>
                            )}
                          </div>
                        ) : (
                          formatReading(currentItem.reading) && (
                            <div className="text-base md:text-lg font-semibold text-gray-600">
                              「 {formatReading(currentItem.reading)} 」
                            </div>
                          )
                        )}
                      </>
                    )}

                    {/* Loa phát âm */}
                    <div className="pt-2">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          playAudio(currentItem)
                        }}
                        className="w-12 h-12 rounded-full hover:bg-blue-50 text-gray-600 hover:text-blue-600 flex items-center justify-center mx-auto transition-colors cursor-pointer text-xl"
                        title="Nghe phát âm"
                      >
                        <SoundOutlined />
                      </button>
                    </div>

                    <div className="text-xs text-gray-400 pt-3 flex items-center justify-center gap-1.5">
                      <RetweetOutlined /> Chạm vào thẻ hoặc ấn Space để xem nghĩa
                    </div>
                  </>
                ) : (
                  <>
                    <div className="text-2xl md:text-3xl font-extrabold text-gray-900 leading-snug break-words">
                      {currentItem.meaning}
                    </div>

                    {currentItem.partOfSpeech && itemTypeOf(currentItem) === 'VOCAB' && (
                      <div className="text-xs text-gray-400 font-medium italic">
                        {currentItem.partOfSpeech}
                      </div>
                    )}

                    {itemTypeOf(currentItem) === 'GRAMMAR' && currentItem.usageNotes && (
                      <div className="text-xs md:text-sm text-gray-600 max-w-md mx-auto text-left whitespace-pre-line">
                        <span className="font-semibold text-gray-800">Cách dùng: </span>
                        {currentItem.usageNotes}
                      </div>
                    )}

                    {itemTypeOf(currentItem) === 'KANJI' && (
                      <div className="flex items-center justify-center gap-2 text-xs text-gray-500">
                        {currentItem.strokeCount != null && <span>{currentItem.strokeCount} nét</span>}
                        <KanjiPracticeLauncher item={currentItem} />
                      </div>
                    )}

                    {currentItem.exampleSentence && (
                      <div className="bg-gray-50/80 p-4 rounded-2xl text-left text-xs md:text-sm space-y-1.5 border border-gray-100 max-w-md mx-auto mt-3">
                        <div className="text-gray-900 font-semibold">
                          {currentItem.exampleSentence}
                        </div>
                        {currentItem.exampleMeaning && (
                          <div className="text-gray-500">
                            {currentItem.exampleMeaning}
                          </div>
                        )}
                      </div>
                    )}

                    {currentItem.userNote && (
                      <div className="text-xs text-amber-800 bg-amber-50 px-3.5 py-1.5 rounded-full inline-block border border-amber-200/60">
                        Ghi chú: {currentItem.userNote}
                      </div>
                    )}
                  </>
                )}
              </div>

              {/* Logo watermark nhỏ góc dưới phải chuẩn Ảnh 1 */}
              <div className="self-end text-[11px] font-bold text-blue-500/90 bg-blue-50 px-2.5 py-0.5 rounded-md">
                {notebook?.name || 'ElearningJP'}
              </div>
            </div>
          </div>

          {/* 4 Nút hành động to hơn bên dưới thẻ (Có Tooltip khi hover) */}
          <div className="flex items-center justify-center gap-5 md:gap-6 py-2">
            {/* 1. Nút Hoàn tác / Lùi từ trước đó */}
            <Tooltip title="Quay lại từ trước">
              <span className="inline-block">
                <button
                  type="button"
                  onClick={handlePreviousWord}
                  disabled={currentIndex === 0}
                  className="w-14 h-14 md:w-16 md:h-16 rounded-full bg-surface hover:bg-gray-50 border border-gray-200 text-gray-600 flex items-center justify-center shadow-xs text-xl md:text-2xl transition-all hover:scale-105 active:scale-95 cursor-pointer disabled:opacity-40 disabled:hover:scale-100 disabled:cursor-not-allowed"
                >
                  <UndoOutlined />
                </button>
              </span>
            </Tooltip>

            {/* 2. Nút Chưa thuộc (Màu đỏ tròn có dấu X) */}
            <Tooltip title="Chưa thuộc (Học lại ngay)">
              <button
                type="button"
                onClick={() => handleRating('AGAIN')}
                className="w-14 h-14 md:w-16 md:h-16 rounded-full bg-red-50 hover:bg-red-100 border border-red-200 text-red-600 flex items-center justify-center shadow-xs text-xl md:text-2xl transition-all hover:scale-105 active:scale-95 cursor-pointer"
              >
                <CloseOutlined />
              </button>
            </Tooltip>

            {/* 3. Nút Thuộc lơ mơ (Màu cam vàng tròn có dấu ?) */}
            <Tooltip title="Thuộc lơ mơ (Cần ôn lại sớm)">
              <button
                type="button"
                onClick={() => handleRating('HARD')}
                className="w-14 h-14 md:w-16 md:h-16 rounded-full bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-500 flex items-center justify-center shadow-xs text-xl md:text-2xl transition-all hover:scale-105 active:scale-95 cursor-pointer"
              >
                <QuestionOutlined />
              </button>
            </Tooltip>

            {/* 4. Nút Đã thuộc (Màu xanh lá tròn có dấu tích - Chỉ hiện tooltip khi hover) */}
            <Tooltip title="Đã thuộc (Nhớ tốt)">
              <button
                type="button"
                onClick={() => handleRating('GOOD')}
                className="w-14 h-14 md:w-16 md:h-16 rounded-full bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 text-emerald-600 flex items-center justify-center shadow-xs text-xl md:text-2xl transition-all hover:scale-105 active:scale-95 cursor-pointer"
              >
                <CheckOutlined />
              </button>
            </Tooltip>
          </div>
        </div>
      )}

      {/* 3. CHẾ ĐỘ 2: LUYỆN NÓI, VIẾT (Chuẩn Ảnh 4) */}
      {(mode === 'speaking' || mode === 'writing') && currentItem && (
        <div className="max-w-2xl w-full mx-auto px-4 py-4 space-y-6 flex-1 flex flex-col justify-between">
          {/* Header trên cùng: [✕ Thoát] bên trái, Luyện nói, viết - 01/25 ở giữa, Báo lỗi bên phải */}
          <div className="flex items-center justify-between border-b border-gray-200/80 pb-3">
            <button
              type="button"
              onClick={() => navigate(notebookId ? `/notebooks/${notebookId}` : '/notebooks')}
              className="inline-flex items-center gap-1 text-xs font-semibold text-gray-700 hover:text-gray-900 bg-surface border border-gray-200 px-3 py-1.5 rounded-xl shadow-2xs transition-colors cursor-pointer"
            >
              <CloseOutlined className="text-xs" />
              <span>Thoát</span>
            </button>

            <div className="text-sm font-bold text-gray-800">
              {subPracticeTab === 'speaking' ? 'Luyện phát âm' : 'Luyện viết Kanji'} - {formatStepNumber(currentIndex, items.length)}
            </div>

            <button
              type="button"
              onClick={() => message.info('Cảm ơn bạn! Đội ngũ sẽ tiếp tục hoàn thiện dữ liệu bài học.')}
              className="inline-flex items-center gap-1 text-xs font-medium text-gray-500 hover:text-gray-800 transition-colors cursor-pointer"
            >
              <ExclamationCircleOutlined className="text-xs text-amber-500" />
              <span>Báo lỗi / Góp ý</span>
            </button>
          </div>

          {/* Huy hiệu chỉ định dạng bài ngẫu nhiên cho từ hiện tại + Nút tùy chọn đổi nếu muốn */}
          <div className="flex items-center justify-center gap-3">
            <span
              className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold shadow-2xs ${
                subPracticeTab === 'speaking'
                  ? 'bg-blue-50 text-blue-700 border border-blue-200'
                  : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
              }`}
            >
              {subPracticeTab === 'speaking' ? <AudioOutlined /> : <EditOutlined />}
              <span>{subPracticeTab === 'speaking' ? 'Dạng bài: Luyện phát âm' : 'Dạng bài: Luyện viết chữ Hán'}</span>
            </span>

            {kanjiChars.length > 0 && (
              <button
                type="button"
                onClick={() => setSubPracticeTab((prev) => (prev === 'speaking' ? 'writing' : 'speaking'))}
                className="text-xs text-gray-400 hover:text-blue-600 underline cursor-pointer transition-colors"
                title="Bấm nếu bạn muốn đổi sang dạng bài còn lại cho từ này"
              >
                Đổi sang {subPracticeTab === 'speaking' ? 'viết Kanji' : 'phát âm'}
              </button>
            )}
          </div>

          {/* NỘI DUNG A: LUYỆN NÓI (Chuẩn Ảnh 4) */}
          {subPracticeTab === 'speaking' && (
            <div className="space-y-6 text-center my-auto">
              <h3 className="text-sm font-semibold text-gray-800">Nói lại từ ở dưới</h3>

              {/* Mascot shiba */}
              <SystemMascot size={150} className="mx-auto" />

              {/* Từ vựng + Loa nghe + Nghĩa */}
              <div className="space-y-1">
                <div className="inline-flex items-center justify-center gap-2 text-2xl font-black text-gray-900">
                  <button
                    type="button"
                    onClick={() => playAudio(currentItem)}
                    className="text-blue-600 hover:text-blue-700 text-lg transition-transform hover:scale-110 cursor-pointer"
                    title="Nghe phát âm mẫu"
                  >
                    <SoundOutlined />
                  </button>
                  <span>{currentItem.word}</span>
                </div>
                <div className="text-xs text-gray-500">
                  Nghĩa: {currentItem.meaning}
                </div>
              </div>

              {/* Nút to màu xám bo tròn: [🎤 Bấm và trả lời câu hỏi] */}
              <div className="max-w-md mx-auto pt-2 space-y-3">
                <button
                  type="button"
                  onClick={startSpeechRecognition}
                  disabled={isListening}
                  className={`w-full py-4 rounded-2xl flex items-center justify-center gap-2 font-semibold text-sm transition-all cursor-pointer shadow-2xs ${
                    isListening
                      ? 'bg-red-500 text-white animate-pulse'
                      : 'bg-surface hover:bg-blue-50/70 border border-gray-200 text-gray-800 hover:text-blue-600'
                  }`}
                >
                  <AudioOutlined className={isListening ? 'text-white' : 'text-blue-600'} />
                  <span>{isListening ? 'Đang lắng nghe bạn nói...' : 'Bấm và trả lời câu hỏi'}</span>
                </button>

                {/* Kết quả nhận diện giọng nói */}
                {transcript && (
                  <div className="bg-surface p-3 rounded-2xl border border-gray-200 text-xs space-y-1">
                    <div className="text-gray-400">Bạn vừa nói: <strong className="text-gray-800">{transcript}</strong></div>
                    {speechAccuracy !== null && (
                      <div>
                        Độ chuẩn xác: <Tag color={speechAccuracy >= 70 ? 'success' : 'warning'}>{speechAccuracy}%</Tag>
                      </div>
                    )}
                  </div>
                )}

                {/* Nút Bỏ qua / Tiếp tục */}
                <div>
                  <button
                    type="button"
                    onClick={() => {
                      if (currentIndex + 1 < items.length) {
                        setCurrentIndex((prev) => prev + 1)
                      } else {
                        setIsCompleted(true)
                      }
                    }}
                    className={`text-xs font-semibold cursor-pointer transition-colors ${
                      transcript
                        ? 'text-blue-600 hover:text-blue-700 bg-blue-50 px-4 py-1.5 rounded-full shadow-2xs'
                        : 'text-gray-400 hover:text-gray-600'
                    }`}
                  >
                    {transcript ? 'Chuyển sang từ tiếp theo →' : 'Bỏ qua'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* NỘI DUNG B: LUYỆN VIẾT KANJI */}
          {subPracticeTab === 'writing' && (
            <div className="bg-surface rounded-3xl border border-gray-200 p-6 text-center space-y-4 my-auto">
              <div className="space-y-1">
                <div className="text-lg font-bold text-gray-900">{currentItem.word}</div>
                <div className="text-xs text-gray-500">Nghĩa: {currentItem.meaning}</div>
              </div>

              {kanjiChars.length > 0 ? (
                <div className="flex flex-wrap items-center justify-center gap-4 py-2">
                  {kanjiChars.map((char, index) => (
                    <div key={index} className="flex flex-col items-center">
                      <HanziWriterPractice
                        character={char}
                        size={220}
                        onCompleteQuiz={(summary) => {
                          message.success(`Đã viết xong "${char}"! Chuẩn: ${summary.accuracyScore}%`)
                          sendReviewRating(
                            currentItem.id,
                            summary.totalMistakes === 0 ? 'EASY' : 'GOOD'
                          )
                        }}
                      />
                    </div>
                  ))}
                </div>
              ) : (
                <Alert
                  type="info"
                  message="Từ vựng này không chứa chữ Hán Kanji (viết bằng Kana)."
                  description="Bạn có thể chuyển sang tab Luyện phát âm để thực hành nói."
                />
              )}

              <div className="pt-2">
                <Button
                  type="primary"
                  onClick={() => {
                    if (currentIndex + 1 < items.length) {
                      setCurrentIndex((prev) => prev + 1)
                    } else {
                      setIsCompleted(true)
                    }
                  }}
                  className="bg-blue-600 rounded-xl"
                >
                  Chuyển từ tiếp theo
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 4. CHẾ ĐỘ 3: QUIZZ TRẮC NGHIỆM CHUẨN MAZII (Theo ảnh đính kèm của người dùng) */}
      {mode === 'quiz' && currentItem && (
        <div className="max-w-2xl w-full mx-auto px-4 py-4 pb-32 md:pb-36 space-y-4 flex-1 flex flex-col justify-between">
          {/* Header Quizz chuẩn Ảnh 4: [✕ Thoát] bên trái, Quizz - 01/25 màu xanh ở giữa, [Báo lỗi / Góp ý] bên phải */}
          <div className="flex items-center justify-between border-b border-gray-200/80 pb-3">
            <button
              type="button"
              onClick={() => navigate(notebookId ? `/notebooks/${notebookId}` : '/notebooks')}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-700 hover:text-gray-900 bg-surface border border-gray-200 px-3.5 py-1.5 rounded-xl shadow-2xs transition-colors cursor-pointer"
            >
              <CloseOutlined className="text-xs" />
              <span>Thoát</span>
            </button>

            <div className="text-base font-bold text-blue-600">
              Quizz - {formatStepNumber(currentIndex, items.length)}
            </div>

            <button
              type="button"
              onClick={() => message.info('Cảm ơn bạn! Đội ngũ sẽ tiếp tục hoàn thiện dữ liệu bài học.')}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-gray-600 hover:text-gray-900 transition-colors cursor-pointer"
            >
              <ExclamationCircleOutlined className="text-xs text-gray-700" />
              <span>Báo lỗi / Góp ý</span>
            </button>
          </div>

          <div className="my-auto space-y-4">
            {/* DẠNG 1: CHỌN ĐÁP ÁN THEO TỪ VỰNG (multiple_choice) - Chuẩn Ảnh Mazii */}
            {currentQuizType === 'multiple_choice' && (() => {
              const isMeaningPrompt =
                currentIndex % 2 === 0 &&
                currentItem.quizWordOptions &&
                currentItem.quizWordOptions.length >= 2
              const targetAnswer = isMeaningPrompt ? currentItem.word : currentItem.meaning

              return (
                <div className="space-y-4 text-center">
                  <h3 className="text-base font-medium text-gray-800">
                    Chọn đáp án tương ứng với từ sau
                  </h3>

                  {/* Mascot shiba kèm Bong bóng thoại */}
                  <div className="flex items-center justify-center gap-4 py-2">
                    <SystemMascot size={140} />
                    <div className="relative bg-surface border border-gray-200/90 rounded-2xl px-6 py-3 shadow-xs flex items-center gap-2 select-none before:content-[''] before:absolute before:-left-2.5 before:top-1/2 before:-translate-y-1/2 before:border-y-[8px] before:border-y-transparent before:border-r-[10px] before:border-r-white before:z-10 after:content-[''] after:absolute after:-left-[11px] after:top-1/2 after:-translate-y-1/2 after:border-y-[9px] after:border-y-transparent after:border-r-[11px] after:border-r-gray-200/90">
                      <span
                        className={
                          isMeaningPrompt
                            ? 'text-base font-bold text-gray-800 tracking-wide'
                            : 'text-2xl font-black text-gray-900 tracking-wide'
                        }
                      >
                        {isMeaningPrompt ? currentItem.meaning : currentItem.word}
                      </span>
                    </div>
                  </div>

                  {/* Đường kẻ mỏng ngăn cách (Chuẩn Ảnh Mazii) */}
                  <div className="w-full max-w-xl mx-auto h-px bg-gray-200/80 my-3" />

                  {/* 4 Lựa chọn phương án xếp chồng theo hàng dọc */}
                  <div className="flex flex-col gap-3 max-w-xl mx-auto w-full pt-1">
                    {isMeaningPrompt
                      ? (currentItem.quizWordOptions || [currentItem.word]).map((wordOpt, idx) => {
                          const isSelected = selectedOption === wordOpt
                          const isCorrect = wordOpt === targetAnswer

                          let btnClass = 'bg-surface border-gray-200 hover:border-blue-300 text-gray-800 shadow-2xs'
                          let furiganaClass = 'text-gray-400'

                          if (isSelected && !isAnswerChecked) {
                            btnClass = 'bg-blue-50/20 border-2 border-blue-500 shadow-xs text-blue-900 font-semibold'
                            furiganaClass = 'text-blue-500'
                          } else if (isAnswerChecked) {
                            if (isCorrect) {
                              btnClass = 'bg-emerald-50/40 border-2 border-emerald-500 text-emerald-700 font-bold shadow-xs'
                              furiganaClass = 'text-emerald-600 font-medium'
                            } else if (isSelected) {
                              btnClass = 'bg-red-50/40 border-2 border-red-500 text-red-600 font-bold shadow-xs'
                              furiganaClass = 'text-red-500 font-medium'
                            } else {
                              btnClass = 'opacity-40 border-gray-200 bg-surface text-gray-400'
                            }
                          }

                          const reading =
                            wordOpt === currentItem.word
                              ? currentItem.reading
                              : items.find((it) => it.word === wordOpt)?.reading

                          return (
                            <button
                              key={idx}
                              type="button"
                              onClick={() => handleSelectQuizOption(wordOpt)}
                              disabled={isAnswerChecked}
                              className={`w-full py-3.5 px-6 rounded-2xl border text-center transition-all shadow-2xs flex flex-col items-center justify-center relative cursor-pointer active:scale-[0.99] ${btnClass}`}
                            >
                              {reading && reading !== wordOpt && (
                                <span className={`text-xs mb-0.5 ${furiganaClass}`}>{reading}</span>
                              )}
                              <span className="text-base font-bold text-gray-900">{wordOpt}</span>
                            </button>
                          )
                        })
                      : (currentItem.quizMeaningOptions && currentItem.quizMeaningOptions.length >= 2
                          ? currentItem.quizMeaningOptions
                          : [currentItem.meaning || '']
                        ).map((opt, idx) => {
                          const isSelected = selectedOption === opt
                          const isCorrect = opt === targetAnswer

                          let btnClass = 'bg-surface border-gray-200 hover:border-blue-300 text-gray-800 shadow-2xs'

                          if (isSelected && !isAnswerChecked) {
                            btnClass = 'bg-blue-50/20 border-2 border-blue-500 shadow-xs text-blue-900 font-semibold'
                          } else if (isAnswerChecked) {
                            if (isCorrect) {
                              btnClass = 'bg-emerald-50/40 border-2 border-emerald-500 text-emerald-700 font-bold shadow-xs'
                            } else if (isSelected) {
                              btnClass = 'bg-red-50/40 border-2 border-red-500 text-red-600 font-bold shadow-xs'
                            } else {
                              btnClass = 'opacity-40 border-gray-200 bg-surface text-gray-400'
                            }
                          }

                          return (
                            <button
                              key={idx}
                              type="button"
                              onClick={() => handleSelectQuizOption(opt)}
                              disabled={isAnswerChecked}
                              className={`w-full py-4 px-6 rounded-2xl border text-center font-medium text-sm md:text-base transition-all shadow-2xs flex items-center justify-center relative cursor-pointer active:scale-[0.99] ${btnClass}`}
                            >
                              <span>{opt}</span>
                            </button>
                          )
                        })}
                  </div>
                </div>
              )
            })()}

            {/* DẠNG 2: NGHE VÀ CHỌN CÂU TRẢ LỜI ĐÚNG (listening) - Chuẩn Ảnh Mazii */}
            {currentQuizType === 'listening' && (
              <div className="space-y-4 text-center">
                <h3 className="text-base font-medium text-gray-800">
                  Nghe và chọn câu trả lời đúng
                </h3>

                {/* Mascot shiba kèm Bong bóng thoại chứa biểu tượng âm thanh */}
                <div className="flex items-center justify-center gap-4 py-2">
                  <SystemMascot size={140} />
                  <button
                    type="button"
                    onClick={() => playAudio(currentItem)}
                    className="relative bg-surface border border-gray-200/90 hover:border-blue-300 rounded-2xl px-6 py-4 shadow-xs flex items-center gap-3 cursor-pointer select-none active:scale-95 transition before:content-[''] before:absolute before:-left-2.5 before:top-1/2 before:-translate-y-1/2 before:border-y-[8px] before:border-y-transparent before:border-r-[10px] before:border-r-white before:z-10 after:content-[''] after:absolute after:-left-[11px] after:top-1/2 after:-translate-y-1/2 after:border-y-[9px] after:border-y-transparent after:border-r-[11px] after:border-r-gray-200/90"
                    title="Bấm để nghe lại"
                  >
                    <SoundOutlined className="text-2xl text-blue-600" />
                    <div className="flex items-center gap-1.5 h-6">
                      <span className="w-1.5 h-3 bg-blue-500 rounded-full animate-pulse" />
                      <span className="w-1.5 h-6 bg-blue-600 rounded-full" />
                      <span className="w-1.5 h-4 bg-blue-500 rounded-full animate-pulse" />
                      <span className="w-1.5 h-2.5 bg-blue-400 rounded-full" />
                    </div>
                  </button>
                </div>

                {/* Đường kẻ mỏng ngăn cách (Chuẩn Ảnh Mazii) */}
                <div className="w-full max-w-xl mx-auto h-px bg-gray-200/80 my-3" />

                {/* 4 Lựa chọn từ tiếng Nhật với Furigana trên đầu xếp chồng theo hàng dọc */}
                <div className="flex flex-col gap-3 max-w-xl mx-auto w-full pt-1">
                  {(currentItem.quizWordOptions && currentItem.quizWordOptions.length >= 2
                    ? currentItem.quizWordOptions
                    : [currentItem.word]
                  ).map((wordOpt, idx) => {
                    const isSelected = selectedOption === wordOpt
                    const isCorrect = wordOpt === currentItem.word

                    let btnClass = 'bg-surface border-gray-200 hover:border-blue-300 text-gray-800 shadow-2xs'
                    let furiganaClass = 'text-gray-400'

                    if (isSelected && !isAnswerChecked) {
                      btnClass = 'bg-blue-50/20 border-2 border-blue-500 shadow-xs text-blue-900 font-semibold'
                      furiganaClass = 'text-blue-500'
                    } else if (isAnswerChecked) {
                      if (isCorrect) {
                        btnClass = 'bg-emerald-50/40 border-2 border-emerald-500 text-emerald-700 font-bold shadow-xs'
                        furiganaClass = 'text-emerald-600 font-medium'
                      } else if (isSelected) {
                        btnClass = 'bg-red-50/40 border-2 border-red-500 text-red-600 font-bold shadow-xs'
                        furiganaClass = 'text-red-500 font-medium'
                      } else {
                        btnClass = 'opacity-40 border-gray-200 bg-surface text-gray-400'
                      }
                    }

                    const reading =
                      wordOpt === currentItem.word
                        ? currentItem.reading
                        : items.find((it) => it.word === wordOpt)?.reading

                    return (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => handleSelectQuizOption(wordOpt)}
                        disabled={isAnswerChecked}
                        className={`w-full py-3.5 px-6 rounded-2xl border text-center transition-all shadow-2xs flex flex-col items-center justify-center relative cursor-pointer active:scale-[0.99] ${btnClass}`}
                      >
                        {reading && reading !== wordOpt && (
                          <span className={`text-xs mb-0.5 ${furiganaClass}`}>{reading}</span>
                        )}
                        <span className="text-base font-bold text-gray-900">{wordOpt}</span>
                      </button>
                    )
                  })}
                </div>
              </div>
            )}

            {/* DẠNG 3: ĐIỀN ĐÁP ÁN ĐÚNG / LUYỆN GÕ TỪ (writing) - Chuẩn Ảnh 3 */}
            {currentQuizType === 'writing' && (
              <div className="space-y-4 text-center">
                <h3 className="text-base font-medium text-gray-800">
                  Điền đáp án đúng cho từ dưới đây
                </h3>

                {/* Mascot shiba kèm Bong bóng thoại chứa ý nghĩa từ vựng (Chuẩn Ảnh 3) */}
                <div className="flex items-center justify-center gap-4 py-2">
                  <SystemMascot size={140} />
                  <div className="relative bg-surface border border-gray-200/90 rounded-2xl px-6 py-3 shadow-xs flex items-center gap-2 select-none before:content-[''] before:absolute before:-left-2.5 before:top-1/2 before:-translate-y-1/2 before:border-y-[8px] before:border-y-transparent before:border-r-[10px] before:border-r-white before:z-10 after:content-[''] after:absolute after:-left-[11px] after:top-1/2 after:-translate-y-1/2 after:border-y-[9px] after:border-y-transparent after:border-r-[11px] after:border-r-gray-200/90">
                    <span className="text-base font-bold text-gray-800">{currentItem.meaning}</span>
                  </div>
                </div>

                {/* Đường kẻ mỏng ngăn cách (Chuẩn Ảnh 3) */}
                <div className="w-full max-w-xl mx-auto h-px bg-gray-200/80 my-3" />

                {/* Ô gõ đáp án tối giản + Nút "Không biết đáp án?" bên phải (Chuẩn Ảnh 3) */}
                <div className="max-w-xl mx-auto w-full pt-2 space-y-4 text-left">
                  <div className="border-b border-gray-300 focus-within:border-blue-500 pb-2 transition-colors">
                    <input
                      type="text"
                      value={typedInput}
                      onChange={(e) => setTypedInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          if (!writingChecked) handleCheckWriting()
                          else advanceQuizNext()
                        }
                      }}
                      disabled={writingChecked}
                      placeholder="Nhập đáp án..."
                      className="w-full bg-transparent text-base text-gray-900 outline-hidden placeholder:text-gray-400 font-medium"
                      autoFocus
                    />
                  </div>

                  <div className="flex items-center justify-end">
                    {!writingChecked ? (
                      <button
                        type="button"
                        onClick={handleRevealWritingAnswer}
                        className="text-xs font-semibold text-blue-600 hover:text-blue-800 cursor-pointer"
                      >
                        Không biết đáp án?
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={advanceQuizNext}
                        className="px-6 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-2xs transition cursor-pointer"
                      >
                        Tiếp theo →
                      </button>
                    )}
                  </div>

                  {writingChecked && (
                    <div
                      className={`p-3.5 rounded-2xl border text-sm font-medium ${
                        isWritingCorrect
                          ? 'bg-emerald-50 border-emerald-300 text-emerald-800'
                          : 'bg-amber-50 border-amber-300 text-amber-800'
                      }`}
                    >
                      <div className="font-bold">
                        {isWritingCorrect
                          ? '✓ Chính xác!'
                          : `Đáp án đúng: ${currentItem.word} ${formatReading(currentItem.reading) ? `(「 ${formatReading(currentItem.reading)} 」)` : ''}`}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* DẠNG 4: NỐI TỪ THÀNH CẶP ĐÚNG (matching) - Chuẩn Ảnh 4 */}
            {currentQuizType === 'matching' && (
              <div className="space-y-4">
                <h3 className="text-base font-medium text-gray-800 text-center">
                  Nối các từ dưới đây thành 1 cặp đúng
                </h3>

                {/* Bảng nối từ 2 cột với các thẻ bo tròn trắng (Chuẩn Ảnh 4) */}
                <div className="max-w-2xl mx-auto w-full pt-2">
                  <div className="grid grid-cols-2 gap-3.5">
                    {/* Cột 1: Từ tiếng Nhật */}
                    <div className="space-y-3">
                      {matchingWordList.map((w, idx) => {
                        const isMatched = !!matchedPairs[w]
                        const isSelected = selectedMatchWord === w
                        const isMismatched = mismatchedWord === w

                        let btnClass = 'bg-surface border-gray-200 hover:border-blue-400 text-gray-800 cursor-pointer shadow-2xs hover:shadow-xs'
                        if (isMatched) {
                          btnClass = 'bg-emerald-50 border-2 border-emerald-500 text-emerald-700 opacity-60 cursor-not-allowed shadow-none'
                        } else if (isMismatched) {
                          btnClass = 'bg-red-50 border-2 border-red-500 text-red-600 shadow-xs animate-shake'
                        } else if (isSelected) {
                          btnClass = 'bg-blue-50 border-2 border-blue-500 text-blue-700 ring-2 ring-blue-200 shadow-xs'
                        }

                        return (
                          <button
                            key={idx}
                            type="button"
                            disabled={isMatched || !!mismatchedWord}
                            onClick={() => handleSelectMatchingWord(w)}
                            className={`w-full min-h-[58px] p-3 rounded-2xl border text-center font-bold text-sm md:text-base transition-all flex items-center justify-center relative active:scale-[0.98] ${btnClass}`}
                          >
                            <span>{w}</span>
                          </button>
                        )
                      })}
                    </div>

                    {/* Cột 2: Nghĩa tiếng Việt */}
                    <div className="space-y-3">
                      {matchingMeaningList.map((m, idx) => {
                        const isMatched = Object.values(matchedPairs).includes(m)
                        const isSelected = selectedMatchMeaning === m
                        const isMismatched = mismatchedMeaning === m

                        let btnClass = 'bg-surface border-gray-200 hover:border-blue-400 text-gray-800 cursor-pointer shadow-2xs hover:shadow-xs'
                        if (isMatched) {
                          btnClass = 'bg-emerald-50 border-2 border-emerald-500 text-emerald-700 opacity-60 cursor-not-allowed shadow-none'
                        } else if (isMismatched) {
                          btnClass = 'bg-red-50 border-2 border-red-500 text-red-600 shadow-xs animate-shake'
                        } else if (isSelected) {
                          btnClass = 'bg-blue-50 border-2 border-blue-500 text-blue-700 ring-2 ring-blue-200 shadow-xs'
                        }

                        return (
                          <button
                            key={idx}
                            type="button"
                            disabled={isMatched || !!mismatchedMeaning}
                            onClick={() => handleSelectMatchingMeaning(m)}
                            className={`w-full min-h-[58px] p-3 rounded-2xl border text-center font-medium text-xs md:text-sm transition-all flex items-center justify-center relative active:scale-[0.98] ${btnClass}`}
                          >
                            <span>{m}</span>
                          </button>
                        )
                      })}
                    </div>
                  </div>

                  {/* Nút "Bỏ qua" ở góc dưới bên phải (Chuẩn Ảnh 4) */}
                  <div className="flex justify-end pt-6">
                    <button
                      type="button"
                      onClick={advanceMatching}
                      className="px-6 py-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold text-sm transition cursor-pointer shadow-2xs"
                    >
                      Bỏ qua
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
          {/* THANH ĐIỀU KHIỂN & KẾT QUẢ ĐÁY MÀN HÌNH CHUẨN MAZII (Ảnh đính kèm của người dùng) */}
          {(currentQuizType === 'multiple_choice' || currentQuizType === 'listening') && (
            <>
              {/* 1. Khi chưa kiểm tra: Nút "Kiểm tra" ở góc dưới bên phải */}
              {!isAnswerChecked && (
                <div
                  className={`fixed bottom-0 right-0 py-4 px-6 md:px-12 bg-surface/95 backdrop-blur-xs border-t border-gray-200/80 z-20 transition-[left] duration-300 ease-in-out ${
                    sidebarCollapsed ? 'left-0 md:left-16' : 'left-0 md:left-60'
                  }`}
                >
                  <div className="max-w-3xl mx-auto w-full flex items-center justify-end">
                    <button
                      type="button"
                      disabled={!selectedOption}
                      onClick={handleCheckQuizAnswer}
                      className={`font-bold rounded-xl px-8 py-3 text-sm md:text-base transition-all shadow-xs ${
                        selectedOption
                          ? 'bg-blue-600 hover:bg-blue-700 text-white cursor-pointer active:scale-95 shadow-blue-500/20'
                          : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                      }`}
                    >
                      Kiểm tra
                    </button>
                  </div>
                </div>
              )}

              {/* 2. Khi đã kiểm tra: Thanh banner kết quả tràn viền đáy màn hình */}
              {isAnswerChecked && isQuizCorrect !== null && (
                <div
                  className={`fixed bottom-0 right-0 py-4 px-6 md:px-12 bg-surface border-t-2 z-30 shadow-lg animate-in slide-in-from-bottom duration-200 transition-[left] duration-300 ease-in-out ${
                    sidebarCollapsed ? 'left-0 md:left-16' : 'left-0 md:left-60'
                  } ${
                    isQuizCorrect ? 'border-emerald-400' : 'border-red-400'
                  }`}
                >
                  <div className="max-w-3xl mx-auto w-full flex items-center justify-between gap-4">
                    {/* Bên trái: Huy hiệu mascot (✓ hoặc ✕) + Đúng/Sai + Chi tiết từ */}
                    <div className="flex items-center gap-4 min-w-0">
                      <QuizResultBadge isCorrect={isQuizCorrect} />

                      <div className="flex flex-col text-left min-w-0">
                        <span
                          className={`text-xl md:text-2xl font-black tracking-wide ${
                            isQuizCorrect ? 'text-emerald-700' : 'text-red-600'
                          }`}
                        >
                          {isQuizCorrect ? 'Đáp án đúng' : 'Đáp án sai'}
                        </span>

                        <div className="flex flex-col mt-0.5 truncate">
                          {formatReading(currentItem.reading) && (
                            <span className="text-xs text-gray-500 font-medium truncate">
                              {formatReading(currentItem.reading)}
                            </span>
                          )}
                          <span className="text-base font-bold text-gray-900 leading-tight truncate">
                            {currentItem.word}
                          </span>
                          <span className="text-xs md:text-sm text-gray-600 font-normal mt-0.5 truncate">
                            {currentItem.meaning}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Bên phải: Nút Tiếp tục */}
                    <button
                      type="button"
                      onClick={advanceQuizNext}
                      className={`shrink-0 text-white font-bold px-8 py-3 rounded-xl shadow-md cursor-pointer transition active:scale-95 text-sm md:text-base ${
                        isQuizCorrect
                          ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/20'
                          : 'bg-red-600 hover:bg-red-700 shadow-red-600/20'
                      }`}
                    >
                      Tiếp tục
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* MODAL TRA CỨU CHI TIẾT TỪ VỰNG CHUẨN MAZII (Ảnh đính kèm của người dùng) */}
      {currentItem && (
        <DictionaryWordDetailModal
          open={quickSearchOpen}
          onClose={() => setQuickSearchOpen(false)}
          word={currentItem.word}
          vocabId={currentItem.vocabularyId || undefined}
        />
      )}
    </div>
  )
}
export default NotebookReviewPage
