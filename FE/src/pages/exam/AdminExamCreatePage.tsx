import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { examService } from '@/services/exam.service'
import type { JlptLevel, SessionType, CourseSummary } from '@/types'
import { courseService } from '@/services/course.service'
import { useAuthStore } from '@/stores'
import { getMondaisBySessionType } from '@/data/jlptMondaiTypes'
import { formatJapanesePassageHtml, isQuestionTextOnly } from '@/utils/format'
import {
  FileText,
  UploadCloud,
  Sparkles,
  ArrowRight,
  BookOpen,
  Clock,
  FileCheck,
  AlertCircle,
  PlusCircle,
  Headphones,
  Layers,
  Bookmark,
  CheckCircle2,
  X,
  Trash2,
  Check,
  Edit3,
  Star,
  Image as ImageIcon,
  ZoomIn,
  Maximize2,
} from 'lucide-react'

interface SectionDraft {
  sessionType: SessionType
  sectionTitle: string
  orderIndex: number
  durationMinutes: number
  pdfFiles: File[]
  audioFile: File | null
  audioUrl?: string
  answerFile?: File | null
  isExtracting: boolean
  extractError: string | null
  extractedPassage?: any
  questions: any[]
}

export const AdminExamCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const urlCourseId = searchParams.get('courseId')
  const urlLevel = searchParams.get('level') as JlptLevel | null

  // Mode: SECTION_BY_SECTION | AI_SINGLE | MANUAL
  const [creationMode, setCreationMode] = useState<'SECTION_BY_SECTION' | 'AI_SINGLE' | 'MANUAL'>('SECTION_BY_SECTION')

  // AI Scope for AI_SINGLE mode: FULL | LISTENING | READING | VOCABULARY
  const [aiScope, setAiScope] = useState<'FULL' | 'LISTENING' | 'READING' | 'VOCABULARY'>('FULL')

  // Basic Info Form
  const [title, setTitle] = useState(urlCourseId ? `Bài kiểm tra khóa học - JLPT ${urlLevel || 'N5'}` : '')
  const [jlptLevel, setJlptLevel] = useState<JlptLevel>(urlLevel || 'N5')
  const [examType, setExamType] = useState<'JLPT_OFFICIAL_MOCK' | 'MINI_TEST'>(
    urlCourseId ? 'MINI_TEST' : 'JLPT_OFFICIAL_MOCK'
  )
  const [totalDuration, setTotalDuration] = useState<number>(urlCourseId ? 30 : 105)
  const [courseId, setCourseId] = useState<string>(urlCourseId || '')

  // User & Courses state for MINI_TEST mode
  const user = useAuthStore((s) => s.user)
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [loadingCourses, setLoadingCourses] = useState(false)

  // Tải danh sách khóa học theo phân quyền (Chỉ tải khi examType là MINI_TEST)
  useEffect(() => {
    if (examType !== 'MINI_TEST') {
      setCourseId('')
      return
    }

    const fetchCourses = async () => {
      try {
        setLoadingCourses(true)
        const isAdmin = user?.role === 'ADMIN' || user?.role === 'ROLE_ADMIN' || user?.roles?.includes('ROLE_ADMIN')
        const isTeacher = user?.role === 'TEACHER' || user?.role === 'ROLE_TEACHER' || user?.roles?.includes('ROLE_TEACHER')

        let res
        try {
          res = await courseService.getAllCoursesAdmin({ size: 100 })
        } catch {
          res = await courseService.getCourses({ size: 100 })
        }

        const allCourses: CourseSummary[] = res.data?.items || []
        // Phân quyền: Giảng viên chỉ được gán vào các khóa học do chính mình tạo
        if (!isAdmin && isTeacher && user?.id) {
          const teacherCourses = allCourses.filter((c) => c.createdByUserId === user.id)
          setCourses(teacherCourses)
        } else {
          setCourses(allCourses)
        }
      } catch (err) {
        console.error('Không thể tải danh sách khóa học:', err)
      } finally {
        setLoadingCourses(false)
      }
    }

    fetchCourses()
  }, [examType, user])

  // Single AI Files (for AI_SINGLE mode)
  const [singlePdfFile, setSinglePdfFile] = useState<File | null>(null)
  const [singleAudioFile, setSingleAudioFile] = useState<File | null>(null)
  const [singleAnswerFile, setSingleAnswerFile] = useState<File | null>(null)
  const [isApplyingAnswerKey, setIsApplyingAnswerKey] = useState(false)

  // Sections Drafts (for SECTION_BY_SECTION mode)
  const [sectionDrafts, setSectionDrafts] = useState<SectionDraft[]>([])

  // Modal rà soát & chỉnh sửa câu hỏi cho Section
  const [editingSectionIdx, setEditingSectionIdx] = useState<number | null>(null)

  // Status
  const [loading, setLoading] = useState(false)
  const [statusMessage, setStatusMessage] = useState<string>('')
  const [error, setError] = useState<string | null>(null)

  // Lightbox xem ảnh tờ rơi / bài thi HD
  const [lightboxImage, setLightboxImage] = useState<string | null>(null)

  // Hàm tạo danh sách section mặc định theo cấp độ JLPT
  const createDefaultSections = (level: JlptLevel): SectionDraft[] => {
    if (level === 'N1' || level === 'N2') {
      const d1 = level === 'N1' ? 110 : 105
      const d2 = level === 'N1' ? 60 : 50
      return [
        {
          sessionType: 'KNOWLEDGE_READING',
          sectionTitle: `Phần 1: Kiến thức ngôn ngữ (Từ vựng, Ngữ pháp) & Đọc hiểu (${d1} phút)`,
          orderIndex: 1,
          durationMinutes: d1,
          pdfFiles: [],
          audioFile: null,
          isExtracting: false,
          extractError: null,
          questions: [],
        },
        {
          sessionType: 'LISTENING',
          sectionTitle: `Phần 2: Nghe hiểu (${d2} phút)`,
          orderIndex: 2,
          durationMinutes: d2,
          pdfFiles: [],
          audioFile: null,
          isExtracting: false,
          extractError: null,
          questions: [],
        },
      ]
    }

    // N3, N4, N5: 3 phần riêng biệt
    const d1 = level === 'N3' || level === 'N4' ? 30 : 25
    const d2 = level === 'N3' ? 70 : level === 'N4' ? 60 : 50
    const d3 = level === 'N3' ? 40 : level === 'N4' ? 35 : 30
    return [
      {
        sessionType: 'VOCABULARY',
        sectionTitle: `Phần 1: Kiến thức ngôn ngữ (Từ vựng) (${d1} phút)`,
        orderIndex: 1,
        durationMinutes: d1,
        pdfFiles: [],
        audioFile: null,
        isExtracting: false,
        extractError: null,
        questions: [],
      },
      {
        sessionType: 'GRAMMAR_READING',
        sectionTitle: `Phần 2: Ngữ pháp & Đọc hiểu (${d2} phút)`,
        orderIndex: 2,
        durationMinutes: d2,
        pdfFiles: [],
        audioFile: null,
        isExtracting: false,
        extractError: null,
        questions: [],
      },
      {
        sessionType: 'LISTENING',
        sectionTitle: `Phần 3: Nghe hiểu (${d3} phút)`,
        orderIndex: 3,
        durationMinutes: d3,
        pdfFiles: [],
        audioFile: null,
        isExtracting: false,
        extractError: null,
        questions: [],
      },
    ]
  }

  // Khởi tạo sections khi đổi JLPT level (Bảo toàn câu hỏi và file đã tải lên)
  useEffect(() => {
    setSectionDrafts((prev) => {
      const defaultSecs = createDefaultSections(jlptLevel)
      const calcDur = defaultSecs.reduce((acc, s) => acc + s.durationMinutes, 0)
      setTotalDuration(calcDur)

      // Nếu người dùng đã có câu hỏi hoặc file tải lên, TUYỆT ĐỐI KHÔNG XÓA SẠCH!
      const totalQs = prev.reduce((sum, s) => sum + (s.questions?.length || 0), 0)
      const hasFiles = prev.some((s) => (s.pdfFiles && s.pdfFiles.length > 0) || s.audioFile)
      if (totalQs > 0 || hasFiles) {
        if (prev.length === defaultSecs.length) {
          return prev.map((s, idx) => ({
            ...s,
            sectionTitle: defaultSecs[idx].sectionTitle,
            durationMinutes: defaultSecs[idx].durationMinutes,
          }))
        }
        return prev
      }

      return defaultSecs
    })

    if (!title || title.includes('Đề thi thử JLPT')) {
      setTitle(`Đề thi thử JLPT ${jlptLevel} chuẩn`)
    }
  }, [jlptLevel])

  // Cập nhật 1 section trong state
  const updateSectionDraft = (index: number, updates: Partial<SectionDraft>) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      next[index] = { ...next[index], ...updates }
      return next
    })
  }

  // Xử lý thêm file PDF vào section
  const handleAddPdfFilesToSection = (index: number, newFiles: FileList | null) => {
    if (!newFiles || newFiles.length === 0) return
    const incoming = Array.from(newFiles)
    setSectionDrafts((prev) => {
      const next = [...prev]
      const current = next[index].pdfFiles || []
      const existingNames = new Set(current.map((f) => f.name))
      const merged = [...current, ...incoming.filter((f) => !existingNames.has(f.name))]
      next[index] = { ...next[index], pdfFiles: merged, extractError: null }
      return next
    })
  }

  // Xóa 1 file PDF khỏi section
  const handleRemovePdfFileFromSection = (secIndex: number, fileIndex: number) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const current = [...next[secIndex].pdfFiles]
      current.splice(fileIndex, 1)
      next[secIndex] = { ...next[secIndex], pdfFiles: current }
      return next
    })
  }

  // Trích xuất AI cho 1 Section cụ thể
  const handleExtractSection = async (sectionIdx: number) => {
    const sec = sectionDrafts[sectionIdx]
    if (!sec) return

    const isListening = sec.sessionType === 'LISTENING'
    if (isListening) {
      if (!sec.audioFile && sec.pdfFiles.length === 0) {
        updateSectionDraft(sectionIdx, {
          extractError: 'Vui lòng nạp ít nhất file Audio (.mp3) hoặc file PDF đề thi nghe (.pdf).',
        })
        return
      }
    } else {
      if (sec.pdfFiles.length === 0) {
        updateSectionDraft(sectionIdx, {
          extractError: 'Vui lòng chọn ít nhất 1 file PDF (có thể chọn cả file ngữ pháp và bài đọc riêng).',
        })
        return
      }
    }

    updateSectionDraft(sectionIdx, { isExtracting: true, extractError: null })

    try {
      const aiBase = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000'
      const formData = new FormData()

      if (sec.audioFile) {
        formData.append('audio_file', sec.audioFile)
      }
      if (sec.pdfFiles && sec.pdfFiles.length > 0) {
        sec.pdfFiles.forEach((file) => {
          formData.append('pdf_files', file)
        })
        if (sec.pdfFiles[0]) {
          formData.append('pdf_file', sec.pdfFiles[0])
        }
      }
      if (sec.answerFile) {
        formData.append('answer_file', sec.answerFile)
      }

      const res = await fetch(
        `${aiBase}/api/v1/ai/extract-section?section_type=${sec.sessionType}&jlpt_level=${jlptLevel}`,
        {
          method: 'POST',
          body: formData,
        }
      )

      if (!res.ok) {
        const errJson = await res.json().catch(() => null)
        throw new Error(errJson?.detail || 'Dịch vụ AI không thể phân tích phần thi này.')
      }

      const extractedData = await res.json()
      const extractedQuestions = extractedData.questions || []
      const extractedPassage = extractedData.passage || null
      const extractedAudioUrl = extractedData.audio_url || undefined

      updateSectionDraft(sectionIdx, {
        isExtracting: false,
        questions: extractedQuestions,
        extractedPassage: extractedPassage,
        audioUrl: extractedAudioUrl || sec.audioUrl,
      })

      if (extractedData.detected_jlpt_level && extractedData.detected_jlpt_level !== jlptLevel) {
        setJlptLevel(extractedData.detected_jlpt_level as JlptLevel)
      }

      // Mở modal rà soát ngay lập tức nếu có câu hỏi
      if (extractedQuestions.length > 0) {
        setEditingSectionIdx(sectionIdx)
      }
    } catch (err: any) {
      console.error(err)
      updateSectionDraft(sectionIdx, {
        isExtracting: false,
        extractError: err.message || 'Lỗi khi trích xuất phần thi bằng AI',
      })
    }
  }

  // =========================================================================
  // CÁC HÀM XỬ LÝ CHỈNH SỬA CÂU HỎI TRONG MODAL STUDIO
  // =========================================================================
  const handleUpdateQuestionField = (secIdx: number, qIdx: number, field: string, value: any) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      qs[qIdx] = { ...qs[qIdx], [field]: value }
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleSetCorrectOption = (secIdx: number, qIdx: number, optIdx: number) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const q = { ...qs[qIdx] }
      q.options = (q.options || []).map((opt: any, i: number) => ({
        ...opt,
        is_correct: i === optIdx,
        correct: i === optIdx,
      }))
      qs[qIdx] = q
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleUpdateOptionText = (secIdx: number, qIdx: number, optIdx: number, text: string) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const q = { ...qs[qIdx] }
      const opts = [...(q.options || [])]
      opts[optIdx] = { ...opts[optIdx], option_text: text, optionText: text }
      q.options = opts
      qs[qIdx] = q
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleDeleteQuestion = (secIdx: number, qIdx: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa câu hỏi này khỏi phần thi?')) return
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      qs.splice(qIdx, 1)
      qs.forEach((q, i) => {
        q.question_number = i + 1
      })
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleAddNewQuestion = (secIdx: number) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const newNum = qs.length + 1
      const isListening = next[secIdx].sessionType === 'LISTENING'
      qs.push({
        question_number: newNum,
        mondai_code: isListening ? 'TASK_BASED' : 'SENTENTIAL_GRAMMAR',
        question_text: '',
        questionText: '',
        options: [
          { option_key: 1, option_text: '', is_correct: true },
          { option_key: 2, option_text: '', is_correct: false },
          { option_key: 3, option_text: '', is_correct: false },
          { option_key: 4, option_text: '', is_correct: false },
        ],
        explanation_vi: '',
      })
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }


  // Xử lý chỉnh sửa vị trí dấu sao (Star Position)
  const handleSetStarPosition = (secIdx: number, qIdx: number, starPos: number) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const q = { ...qs[qIdx] }
      q.star_position = starPos
      q.starPosition = starPos

      let text = q.question_text ?? q.questionText ?? ''
      const slots = ['＿＿＿', '＿＿＿', '＿＿＿', '＿＿＿']
      slots[starPos - 1] = '＿★＿'
      const starPattern = slots.join('　')

      const underlineMatches: any[] = Array.from(text.matchAll(/[_＿]{2,}/g))
      if (underlineMatches.length >= 4) {
        const firstMatch = underlineMatches[0].index!
        const lastMatch = underlineMatches[3].index! + underlineMatches[3][0].length
        text = text.substring(0, firstMatch) + starPattern + text.substring(lastMatch)
      } else if (!text.includes('★')) {
        if (text.includes('。')) {
          text = text.replace('。', `　${starPattern}　。`)
        } else {
          text = `${text}　${starPattern}`
        }
      } else {
        text = text.replace(/[_＿]*★[_＿]*/g, '＿＿＿')
        const parts: any[] = Array.from(text.matchAll(/[_＿]{2,}/g))
        if (parts.length >= 4) {
          const firstMatch = parts[0].index!
          const lastMatch = parts[3].index! + parts[3][0].length
          text = text.substring(0, firstMatch) + starPattern + text.substring(lastMatch)
        }
      }
      q.question_text = text
      q.questionText = text
      qs[qIdx] = q
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleInsertStarTemplate = (secIdx: number, qIdx: number) => {
    const q = sectionDrafts[secIdx]?.questions?.[qIdx]
    const currentStar = q?.star_position ?? q?.starPosition ?? 3
    handleSetStarPosition(secIdx, qIdx, currentStar)
  }

  // Xử lý bài đọc hiểu riêng cho từng câu hỏi (Passage per Question)
  const handleInitQuestionPassage = (secIdx: number, qIdx: number) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const q = { ...qs[qIdx] }
      const secPassage = next[secIdx].extractedPassage
      q.passage = secPassage ? { ...secPassage } : {
        title: q.mondai_code === 'TEXT_GRAMMAR' ? 'Ngữ pháp đoạn văn' : 'Bài đọc hiểu',
        content_jp: '',
        translation_vi: '',
        image_url: q.image_url || '',
      }
      qs[qIdx] = q
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleRemoveQuestionPassage = (secIdx: number, qIdx: number) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const q = { ...qs[qIdx] }
      delete q.passage
      qs[qIdx] = q
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleUpdateQuestionPassageField = (secIdx: number, qIdx: number, field: string, value: string) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      const q = { ...qs[qIdx] }
      q.passage = {
        ...(q.passage || { title: 'Bài đọc hiểu', content_jp: '', translation_vi: '' }),
        [field]: value,
      }
      qs[qIdx] = q
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  const handleUpdateClusterPassageField = (
    secIdx: number,
    qIndices: number[],
    field: string,
    value: string
  ) => {
    setSectionDrafts((prev) => {
      const next = [...prev]
      const qs = [...next[secIdx].questions]
      qIndices.forEach((idx) => {
        const q = { ...qs[idx] }
        q.passage = {
          ...(q.passage || { title: 'Bài đọc hiểu', content_jp: '', translation_vi: '' }),
          [field]: value,
        }
        qs[idx] = q
      })
      next[secIdx] = { ...next[secIdx], questions: qs }
      return next
    })
  }

  // Tải bảng đáp án chính thức (正答表) và nạp tự động vào câu hỏi hiện tại
  const handleUploadAnswerKeyToCurrentSection = async (file: File) => {
    if (editingSectionIdx === null) return
    const curSec = sectionDrafts[editingSectionIdx]
    if (!curSec) return

    try {
      setIsApplyingAnswerKey(true)
      const aiBase = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000'
      const formData = new FormData()
      formData.append('file', file)

      const res = await fetch(
        `${aiBase}/api/v1/ai/parse-answer-key?section_type=${curSec.sessionType}&jlpt_level=${jlptLevel}`,
        {
          method: 'POST',
          body: formData,
        }
      )

      if (!res.ok) {
        const errJson = await res.json().catch(() => null)
        throw new Error(errJson?.detail || 'Không thể đọc file đáp án')
      }

      const data = await res.json()
      const answersMap: Record<string, number> = data.matched_answers || data.flat_answers || {}
      
      let matchedCount = 0
      setSectionDrafts((prev) => {
        const next = [...prev]
        const qs = [...next[editingSectionIdx].questions]
        qs.forEach((q) => {
          const qNum = String(q.question_number || '')
          const correctKey = answersMap[qNum]
          if (correctKey !== undefined) {
            matchedCount++
            q.options = (q.options || []).map((opt: any, idx: number) => {
              const optKey = opt.option_key ?? idx + 1
              const isMatch = Number(optKey) === Number(correctKey)
              return {
                ...opt,
                is_correct: isMatch,
                correct: isMatch,
              }
            })
          }
        })
        next[editingSectionIdx] = { ...next[editingSectionIdx], questions: qs }
        return next
      })

      alert(`✓ Đã nhận diện bảng đáp án 正答表 và tự động điền chính xác cho ${matchedCount} câu hỏi!`)
    } catch (err: any) {
      alert(`Lỗi đọc file đáp án: ${err.message}`)
    } finally {
      setIsApplyingAnswerKey(false)
    }
  }

  // Alias để đồng bộ tên gọi hàm xóa câu hỏi
  const handleDeleteSectionQuestion = handleDeleteQuestion

  // Đổi phạm vi trích xuất cho AI_SINGLE mode
  const handleScopeChange = (scope: 'FULL' | 'LISTENING' | 'READING' | 'VOCABULARY', level = jlptLevel) => {
    setAiScope(scope)
    setError(null)
    if (scope === 'FULL') {
      const dur = level === 'N1' ? 170 : level === 'N2' ? 155 : level === 'N3' ? 140 : level === 'N4' ? 125 : 105
      setTotalDuration(dur)
      setTitle(`Đề thi thử JLPT ${level} tổng hợp`)
    } else if (scope === 'LISTENING') {
      const dur = level === 'N1' ? 60 : level === 'N2' ? 50 : level === 'N3' ? 40 : level === 'N4' ? 35 : 30
      setTotalDuration(dur)
      setTitle(`Phần thi Nghe hiểu (Choukai) - JLPT ${level}`)
    } else if (scope === 'READING') {
      const dur = level === 'N1' || level === 'N2' ? 60 : level === 'N3' ? 70 : level === 'N4' ? 60 : 50
      setTotalDuration(dur)
      setTitle(`Phần thi Đọc hiểu (Dokkai) - JLPT ${level}`)
    } else if (scope === 'VOCABULARY') {
      const dur = level === 'N1' || level === 'N2' ? 50 : level === 'N3' ? 30 : level === 'N4' ? 30 : 25
      setTotalDuration(dur)
      setTitle(`Phần thi Kiến thức ngôn ngữ (Từ vựng) - JLPT ${level}`)
    }
  }

  // =========================================================================
  // SUBMIT: LƯU TOÀN BỘ ĐỀ THEO TỪNG PHẦN
  // =========================================================================
  const handleSaveSectionBySection = async () => {
    if (!title.trim()) {
      setError('Vui lòng nhập tiêu đề kỳ thi')
      return
    }

    try {
      setLoading(true)
      setError(null)
      setStatusMessage('Đang lưu cấu trúc đề và các câu hỏi đã trích xuất vào hệ thống...')

      const normalizedSections = sectionDrafts.map((sec, sIdx) => ({
        sessionType: sec.sessionType,
        sectionTitle: sec.sectionTitle,
        orderIndex: sIdx + 1,
        durationMinutes: sec.durationMinutes,
        audioUrl: sec.audioUrl || sec.questions?.find((x: any) => x.audio_url || x.audioUrl)?.audio_url || sec.questions?.find((x: any) => x.audio_url || x.audioUrl)?.audioUrl,
        questions: (sec.questions || []).map((q: any, qIdx: number) => ({
          questionNumber: qIdx + 1,
          mondaiCode: q.mondai_code || q.mondaiCode,
          questionText: q.question_text || q.questionText,
          imageUrl: isQuestionTextOnly(q) ? undefined : (q.image_url || q.imageUrl),
          audioUrl: q.audio_url || q.audioUrl || sec.audioUrl,
          audioStartTime: q.audio_start_time ?? q.audioStartTime,
          audioEndTime: q.audio_end_time ?? q.audioEndTime,
          transcriptJp: q.transcript_jp || q.transcriptJp,
          explanationVi: q.explanation_vi || q.explanationVi,
          starPosition: q.star_position ?? q.starPosition,
          hasVisualOptions: isQuestionTextOnly(q) ? false : Boolean(q.has_visual_options ?? q.hasVisualOptions),
          passage: (() => {
            const isPassageMondai = ['TEXT_GRAMMAR', 'SHORT_PASSAGE', 'MEDIUM_PASSAGE', 'LONG_PASSAGE', 'INFORMATION_RETRIEVAL'].includes(
              (q.mondai_code || q.mondaiCode || '').toUpperCase()
            )
            if (!isPassageMondai) return undefined

            if (q.passage) {
              return {
                title: q.passage.title,
                contentJp: q.passage.content_jp || q.passage.contentJp,
                translationVi: q.passage.translation_vi || q.passage.translationVi,
                imageUrl: (q.mondai_code === 'INFORMATION_RETRIEVAL' || q.mondaiCode === 'INFORMATION_RETRIEVAL')
                  ? (q.passage.image_url || q.passage.imageUrl)
                  : undefined,
              }
            }
            if (q.mondai_code === 'TEXT_GRAMMAR' && sec.extractedPassage) {
              return {
                title: sec.extractedPassage.title,
                contentJp: sec.extractedPassage.content_jp || sec.extractedPassage.contentJp,
                translationVi: sec.extractedPassage.translation_vi || sec.extractedPassage.translationVi,
                imageUrl: undefined,
              }
            }
            return undefined
          })(),
          options: (() => {
            const opts = q.options || []
            const correctIndices = opts
              .map((o: any, i: number) => (Boolean(o.is_correct ?? o.correct ?? o.isCorrect) ? i : -1))
              .filter((i: number) => i !== -1)
            let chosenIdx = 0
            if (correctIndices.length === 1) {
              chosenIdx = correctIndices[0]
            } else if (correctIndices.length > 1) {
              chosenIdx = correctIndices.find((i: number) => i > 0) ?? correctIndices[0]
            }
            return opts.map((opt: any, optIdx: number) => ({
              optionKey: opt.option_key || opt.optionKey || optIdx + 1,
              optionText: opt.option_text ?? opt.optionText ?? '',
              optionImageUrl: isQuestionTextOnly(q) ? undefined : (opt.option_image_url || opt.optionImageUrl),
              isCorrect: optIdx === chosenIdx,
            }))
          })(),
        })),
      }))

      const calcTotalDuration = sectionDrafts.reduce((acc, s) => acc + (s.durationMinutes || 0), 0)

      const payload = {
        title,
        jlptLevel,
        examType,
        totalDurationMinutes: calcTotalDuration || totalDuration,
        sourceFileName: 'Section_By_Section_Import',
        courseId: examType === 'MINI_TEST' && courseId ? Number(courseId) : undefined,
        sections: normalizedSections,
      }

      const newExamId = await examService.importExam(payload)
      setStatusMessage('Tạo đề thi thành công! Đang chuyển hướng sang phòng biên soạn chuyên sâu...')

      setTimeout(() => {
        navigate(`/admin/exams/${newExamId}/preview`)
      }, 600)
    } catch (err: any) {
      console.error(err)
      setError(err.response?.data?.message || err.message || 'Lỗi khi lưu đề thi')
    } finally {
      setLoading(false)
    }
  }

  // =========================================================================
  // SUBMIT 2: TRÍCH XUẤT NHANH TỪ 1 FILE DUY NHẤT (AI_SINGLE)
  // =========================================================================
  const handleCreateWithSingleAI = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) {
      setError('Vui lòng nhập tiêu đề kỳ thi')
      return
    }

    if (aiScope === 'LISTENING' && !singleAudioFile) {
      setError('Vui lòng chọn file Audio MP3 bài nghe')
      return
    }
    if (aiScope !== 'LISTENING' && !singlePdfFile) {
      setError('Vui lòng chọn file PDF đề thi')
      return
    }

    try {
      setLoading(true)
      setError(null)
      setStatusMessage('Đang kết nối AI Service để trích xuất dữ liệu...')

      const aiBase = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000'
      const formData = new FormData()

      let finalSections: any[] = []

      if (aiScope === 'FULL') {
        formData.append('pdf_file', singlePdfFile!)
        if (singleAudioFile) {
          formData.append('audio_file', singleAudioFile)
        }
        if (singleAnswerFile) {
          formData.append('answer_file', singleAnswerFile)
        }

        const aiUrl = `${aiBase}/api/v1/ai/extract-exam?title=${encodeURIComponent(
          title
        )}&jlpt_level=${jlptLevel}`
        const aiRes = await fetch(aiUrl, {
          method: 'POST',
          body: formData,
        })

        if (!aiRes.ok) {
          throw new Error('Dịch vụ AI không thể phân tích đề thi tổng hợp.')
        }

        const extractedData = await aiRes.json()
        finalSections = extractedData.sections || []
      } else {
        const sectionType =
          aiScope === 'LISTENING'
            ? 'LISTENING'
            : aiScope === 'READING'
            ? 'GRAMMAR_READING'
            : 'VOCABULARY'

        if (singleAudioFile) formData.append('audio_file', singleAudioFile)
        if (singlePdfFile) formData.append('pdf_file', singlePdfFile)
        if (singleAnswerFile) {
          formData.append('answer_file', singleAnswerFile)
        }

        const aiUrl = `${aiBase}/api/v1/ai/extract-section?section_type=${sectionType}&jlpt_level=${jlptLevel}`
        const aiRes = await fetch(aiUrl, {
          method: 'POST',
          body: formData,
        })

        if (!aiRes.ok) {
          throw new Error(`Dịch vụ AI không thể phân tích phần thi ${sectionType}.`)
        }

        const extractedData = await aiRes.json()
        finalSections = [
          {
            sessionType: extractedData.section_type || sectionType,
            sectionTitle: extractedData.section_title || title,
            orderIndex: 1,
            durationMinutes: totalDuration,
            audioUrl: extractedData.audio_url || undefined,
            questions: extractedData.questions || [],
          },
        ]
      }

      setStatusMessage('AI trích xuất thành công! Đang lưu vào hệ thống...')

      const normalizedSections = finalSections.map((sec: any) => ({
        sessionType: sec.session_type || sec.sessionType,
        sectionTitle: sec.section_title || sec.sectionTitle,
        orderIndex: sec.order_index ?? sec.orderIndex,
        durationMinutes: sec.duration_minutes ?? sec.durationMinutes,
        audioUrl: sec.audio_url || sec.audioUrl,
        questions: (sec.questions || []).map((q: any, qIdx: number) => ({
          questionNumber: qIdx + 1,
          mondaiCode: q.mondai_code || q.mondaiCode,
          questionText: q.question_text || q.questionText,
          imageUrl: isQuestionTextOnly(q) ? undefined : (q.image_url || q.imageUrl),
          audioUrl: q.audio_url || q.audioUrl,
          audioStartTime: q.audio_start_time ?? q.audioStartTime,
          audioEndTime: q.audio_end_time ?? q.audioEndTime,
          transcriptJp: q.transcript_jp || q.transcriptJp,
          explanationVi: q.explanation_vi || q.explanationVi,
          starPosition: q.star_position ?? q.starPosition,
          hasVisualOptions: isQuestionTextOnly(q) ? false : Boolean(q.has_visual_options ?? q.hasVisualOptions),
          passage: q.passage
            ? {
                title: q.passage.title,
                contentJp: q.passage.content_jp || q.passage.contentJp,
                translationVi: q.passage.translation_vi || q.passage.translationVi,
                imageUrl: q.passage.image_url || q.passage.imageUrl,
              }
            : undefined,
          options: (() => {
            const opts = q.options || []
            const correctIndices = opts
              .map((o: any, i: number) => (Boolean(o.is_correct ?? o.correct ?? o.isCorrect) ? i : -1))
              .filter((i: number) => i !== -1)
            let chosenIdx = 0
            if (correctIndices.length === 1) {
              chosenIdx = correctIndices[0]
            } else if (correctIndices.length > 1) {
              chosenIdx = correctIndices.find((i: number) => i > 0) ?? correctIndices[0]
            }
            return opts.map((opt: any, optIdx: number) => ({
              optionKey: opt.option_key || opt.optionKey || optIdx + 1,
              optionText: opt.option_text ?? opt.optionText,
              optionImageUrl: isQuestionTextOnly(q) ? undefined : (opt.option_image_url || opt.optionImageUrl),
              isCorrect: optIdx === chosenIdx,
            }))
          })(),
        })),
      }))

      const payload = {
        title,
        jlptLevel,
        examType,
        totalDurationMinutes: totalDuration,
        sourceFileName: singlePdfFile ? singlePdfFile.name : singleAudioFile ? singleAudioFile.name : 'AI_Generated',
        courseId: examType === 'MINI_TEST' && courseId ? Number(courseId) : undefined,
        sections: normalizedSections,
      }

      const newExamId = await examService.importExam(payload)
      setStatusMessage('Tạo đề thi thành công! Đang chuyển hướng...')

      setTimeout(() => {
        navigate(`/admin/exams/${newExamId}/preview`)
      }, 600)
    } catch (err: any) {
      console.error(err)
      setError(err.message || 'Lỗi khi tạo đề thi với AI')
    } finally {
      setLoading(false)
    }
  }

  // =========================================================================
  // SUBMIT 3: TẠO KHUNG ĐỀ THỦ CÔNG (MANUAL)
  // =========================================================================
  const handleCreateManual = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) {
      setError('Vui lòng nhập tiêu đề kỳ thi')
      return
    }

    try {
      setLoading(true)
      setError(null)
      setStatusMessage('Đang khởi tạo khung đề thi...')

      const defaultSecs = createDefaultSections(jlptLevel).map((s) => ({
        sessionType: s.sessionType,
        sectionTitle: s.sectionTitle,
        orderIndex: s.orderIndex,
        durationMinutes: s.durationMinutes,
        questions: [],
      }))

      const payload = {
        title,
        jlptLevel,
        examType,
        totalDurationMinutes: totalDuration,
        courseId: examType === 'MINI_TEST' && courseId ? Number(courseId) : undefined,
        sections: defaultSecs,
      }

      const newExamId = await examService.importExam(payload)
      setStatusMessage('Đã tạo khung đề thành công! Tự động mở trang biên soạn...')

      setTimeout(() => {
        navigate(`/admin/exams/${newExamId}/preview`)
      }, 600)
    } catch (err: any) {
      console.error(err)
      setError(err.response?.data?.message || err.message || 'Lỗi khởi tạo đề thi')
    } finally {
      setLoading(false)
    }
  }

  const totalQuestionsExtracted = sectionDrafts.reduce((sum, s) => sum + (s.questions?.length || 0), 0)
  const currentEditingSection = editingSectionIdx !== null ? sectionDrafts[editingSectionIdx] : null
  const editingAvailableMondais = currentEditingSection ? getMondaisBySessionType(currentEditingSection.sessionType, jlptLevel) : []

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-8">
        {/* Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 text-xs font-bold">
            <Sparkles className="w-4 h-4 text-indigo-600" />
            ElearningJP Smart Exam Studio
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Tạo & Nhập Đề Thi Thông Minh
          </h1>
          <p className="text-sm text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
            Hỗ trợ nhập và rà soát từng phần thi độc lập (Từ vựng, Ngữ pháp & Đọc hiểu, Nghe hiểu) với nhiều tệp PDF riêng biệt.
          </p>
        </div>

        {/* 3 Chế độ tạo đề */}
        <div className="flex justify-center">
          <div className="bg-slate-200/80 dark:bg-slate-900 p-1.5 rounded-2xl flex flex-wrap gap-1 border border-slate-300 dark:border-slate-800 shadow-inner">
            <button
              type="button"
              onClick={() => setCreationMode('SECTION_BY_SECTION')}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition cursor-pointer ${
                creationMode === 'SECTION_BY_SECTION'
                  ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              <Layers className="w-4 h-4 text-indigo-600" />
              <span>Nhập theo từng phần & Rà soát (Khuyên dùng)</span>
            </button>
            <button
              type="button"
              onClick={() => setCreationMode('AI_SINGLE')}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition cursor-pointer ${
                creationMode === 'AI_SINGLE'
                  ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>Tạo nhanh từ 1 file</span>
            </button>
            <button
              type="button"
              onClick={() => setCreationMode('MANUAL')}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition cursor-pointer ${
                creationMode === 'MANUAL'
                  ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              <PlusCircle className="w-4 h-4 text-slate-500" />
              <span>Tạo khung đề thủ công</span>
            </button>
          </div>
        </div>

        {/* Global Error Banner */}
        {error && (
          <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center gap-3">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* BƯỚC 1: THÔNG TIN CẤU HÌNH CHUNG CỦA BÀI THI */}
        <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-slate-200 dark:border-slate-800 p-6 sm:p-8 space-y-4">
          <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2 border-b border-slate-100 dark:border-slate-800 pb-2">
            <BookOpen className="w-4 h-4 text-indigo-600" />
            1. Cấu hình thông tin đề thi
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="sm:col-span-2 lg:col-span-4">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                Tiêu đề bài thi <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ví dụ: Đề thi thử JLPT N4 Tháng 7 (Đầy đủ 3 phần)"
                className="w-full text-sm px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                Cấp độ JLPT
              </label>
              <select
                value={jlptLevel}
                onChange={(e: any) => {
                  const newLvl = e.target.value as JlptLevel
                  setJlptLevel(newLvl)
                  handleScopeChange(aiScope, newLvl)
                }}
                className="w-full text-sm px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none font-medium"
              >
                <option value="N5">JLPT N5 (Sơ cấp)</option>
                <option value="N4">JLPT N4 (Sơ trung cấp)</option>
                <option value="N3">JLPT N3 (Trung cấp)</option>
                <option value="N2">JLPT N2 (Thượng trung cấp)</option>
                <option value="N1">JLPT N1 (Cao cấp)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5">
                Loại bài thi
              </label>
              <select
                value={examType}
                onChange={(e: any) => {
                  const val = e.target.value
                  setExamType(val)
                  if (val === 'JLPT_OFFICIAL_MOCK') {
                    setCourseId('')
                  }
                }}
                className="w-full text-sm px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none font-medium"
              >
                <option value="JLPT_OFFICIAL_MOCK">Đề thi thử JLPT chính thức (Mở tự do)</option>
                <option value="MINI_TEST">Mini-test / Bài thi thuộc khóa học</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                Tổng thời gian (Phút)
              </label>
              <input
                type="number"
                min={5}
                max={240}
                value={totalDuration}
                onChange={(e) => setTotalDuration(Number(e.target.value))}
                className="w-full text-sm px-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 focus:ring-2 focus:ring-indigo-500 focus:outline-none font-mono"
              />
            </div>

            <div>
              {examType === 'MINI_TEST' ? (
                <div>
                  <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 mb-1.5 flex items-center justify-between">
                    <span className="flex items-center gap-1">
                      <BookOpen className="w-3.5 h-3.5 text-indigo-500" />
                      Gắn vào khóa học
                    </span>
                    {loadingCourses && <span className="text-[10px] text-indigo-500 animate-pulse">Đang nạp...</span>}
                  </label>
                  <select
                    value={courseId}
                    onChange={(e) => setCourseId(e.target.value)}
                    className="w-full text-sm px-4 py-2.5 rounded-xl border border-indigo-300 dark:border-indigo-700 bg-indigo-50/50 dark:bg-indigo-950/30 focus:ring-2 focus:ring-indigo-500 focus:outline-none font-medium"
                  >
                    <option value="">-- Chọn khóa học bạn phụ trách --</option>
                    {courses.map((c) => (
                      <option key={c.id} value={c.id}>
                        [{c.jlptLevel}] {c.title} {c.createdByName ? `(${c.createdByName})` : ''}
                      </option>
                    ))}
                  </select>
                  <p className="text-[10px] text-slate-500 mt-1">
                    {user?.role === 'TEACHER' || user?.role === 'ROLE_TEACHER'
                      ? '✓ Chỉ hiển thị các khóa học do bạn trực tiếp giảng dạy.'
                      : '✓ Bài kiểm tra sẽ hiển thị trong khóa học đã chọn.'}
                  </p>
                </div>
              ) : (
                <div>
                  <label className="block text-xs font-bold text-slate-400 mb-1.5">
                    Phạm vi đề thi
                  </label>
                  <div className="w-full text-xs px-3 py-2 rounded-xl bg-slate-100 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                    <span>Đề mở tự do (Không thuộc khóa học cụ thể)</span>
                  </div>
                  <p className="text-[10px] text-slate-400 mt-1">
                    Đề thi thử JLPT dành cho mọi học viên luyện thi tự do.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* ========================================================================= */}
        {/* CHẾ ĐỘ 1: NHẬP VÀ PREVIEW THEO TỪNG PHẦN (SECTION BY SECTION) */}
        {/* ========================================================================= */}
        {creationMode === 'SECTION_BY_SECTION' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
                  <Layers className="w-5 h-5 text-indigo-600" />
                  2. Quản lý & Trích xuất theo từng phần thi ({jlptLevel})
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Tải tệp riêng cho từng phần thi (cho phép nhiều file PDF). Sau khi trích xuất, bạn có thể bấm rà soát để chỉnh sửa câu hỏi trực tiếp.
                </p>
              </div>

              <div className="text-right">
                <span className="text-xs font-bold text-slate-500">Đã sẵn sàng: </span>
                <span className="text-sm font-extrabold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-200">
                  {totalQuestionsExtracted} câu hỏi
                </span>
              </div>
            </div>

            {/* Danh sách Section Cards */}
            <div className="space-y-5">
              {sectionDrafts.map((sec, secIdx) => {
                const isListening = sec.sessionType === 'LISTENING'
                const hasExtractedQuestions = sec.questions.length > 0

                return (
                  <div
                    key={sec.orderIndex}
                    className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden transition"
                  >
                    {/* Card Header */}
                    <div className="p-5 sm:p-6 bg-slate-50/70 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800 flex flex-wrap items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <span className="w-8 h-8 rounded-xl bg-indigo-600 text-white font-black text-sm flex items-center justify-center shadow-sm">
                          {sec.orderIndex}
                        </span>
                        <div>
                          <h3 className="font-extrabold text-slate-900 dark:text-white text-sm sm:text-base">
                            {sec.sectionTitle}
                          </h3>
                          <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-500">
                            <span className="font-medium">
                              Thời lượng: {sec.durationMinutes} phút
                            </span>
                            <span>•</span>
                            <span className="font-mono text-[11px] bg-slate-200 dark:bg-slate-700 px-1.5 py-0.5 rounded text-slate-700 dark:text-slate-300">
                              {sec.sessionType}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Trạng thái Section */}
                      <div className="flex items-center gap-2">
                        {hasExtractedQuestions ? (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Đã trích xuất {sec.questions.length} câu
                          </span>
                        ) : sec.isExtracting ? (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 text-amber-700 border border-amber-200 text-xs font-bold animate-pulse">
                            <div className="w-3 h-3 border-2 border-amber-600 border-t-transparent rounded-full animate-spin" />
                            Đang phân tích AI...
                          </span>
                        ) : (sec.pdfFiles.length > 0 || sec.audioFile) ? (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-sky-50 text-sky-700 border border-sky-200 text-xs font-semibold">
                            {isListening
                              ? [sec.audioFile ? '1 Audio MP3' : null, sec.pdfFiles.length > 0 ? `${sec.pdfFiles.length} file PDF đề` : null].filter(Boolean).join(' + ')
                              : `${sec.pdfFiles.length} file PDF đã chọn`}
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-100 text-slate-500 text-xs">
                            Chưa nạp file
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Card Body */}
                    <div className="p-5 sm:p-6 space-y-4">
                      {/* Section Error */}
                      {sec.extractError && (
                        <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs flex items-center gap-2">
                          <AlertCircle className="w-4 h-4 shrink-0" />
                          <span>{sec.extractError}</span>
                        </div>
                      )}

                      {/* KHU VỰC TẢI TỆP */}
                      {isListening ? (
                        /* Section Nghe: Cho phép nạp cả Audio MP3 và file PDF đề thi nghe */
                        <div className="space-y-3">
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            {/* 1. Tải Audio MP3 */}
                            <div className="border-2 border-dashed border-indigo-200 dark:border-indigo-900/50 rounded-2xl p-4 text-center bg-indigo-50/20 dark:bg-indigo-950/20 space-y-2">
                              <Headphones className="w-7 h-7 text-indigo-600 mx-auto" />
                              <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                                1. File Audio bài nghe (.mp3)
                              </p>
                              <div>
                                <label className="inline-block px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-xs cursor-pointer transition shadow-xs">
                                  <span>{sec.audioFile ? 'Đổi file Audio MP3' : 'Chọn file Audio (.mp3)'}</span>
                                  <input
                                    type="file"
                                    accept=".mp3,.wav,.m4a"
                                    onChange={(e) => {
                                      if (e.target.files && e.target.files[0]) {
                                        updateSectionDraft(secIdx, {
                                          audioFile: e.target.files[0],
                                          extractError: null,
                                        })
                                      }
                                    }}
                                    className="hidden"
                                  />
                                </label>
                                {sec.audioFile && (
                                  <div className="mt-2 flex items-center justify-between p-2 bg-white rounded-xl border border-indigo-200 text-left text-xs text-indigo-900 font-bold">
                                    <span className="truncate">{sec.audioFile.name}</span>
                                    <div className="flex items-center gap-1 shrink-0 ml-2">
                                      <span className="text-[10px] font-normal text-slate-400">
                                        {(sec.audioFile.size / 1024 / 1024).toFixed(2)} MB
                                      </span>
                                      <button
                                        type="button"
                                        onClick={() => updateSectionDraft(secIdx, { audioFile: null })}
                                        className="text-slate-400 hover:text-rose-600 cursor-pointer p-0.5"
                                        title="Xóa audio"
                                      >
                                        <X className="w-3.5 h-3.5" />
                                      </button>
                                    </div>
                                  </div>
                                )}
                              </div>
                              <p className="text-[10px] text-slate-400">
                                Dùng để cắt mốc câu và trích xuất kịch bản thoại
                              </p>
                            </div>

                            {/* 2. Tải File PDF Đề Thi Nghe */}
                            <div className="border-2 border-dashed border-sky-200 dark:border-sky-900/50 rounded-2xl p-4 text-center bg-sky-50/20 dark:bg-sky-950/20 space-y-2">
                              <FileText className="w-7 h-7 text-sky-600 mx-auto" />
                              <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                                2. File PDF đề thi nghe (Tranh vẽ & Lựa chọn in)
                              </p>
                              <div>
                                <label className="inline-block px-3.5 py-1.5 bg-sky-600 hover:bg-sky-700 text-white font-bold rounded-xl text-xs cursor-pointer transition shadow-xs">
                                  <span>+ Thêm file PDF đề nghe (.pdf)</span>
                                  <input
                                    type="file"
                                    multiple
                                    accept=".pdf"
                                    onChange={(e) => {
                                      handleAddPdfFilesToSection(secIdx, e.target.files)
                                      e.target.value = ''
                                    }}
                                    className="hidden"
                                  />
                                </label>

                                {sec.pdfFiles.length > 0 && (
                                  <div className="mt-2 space-y-1.5 max-h-24 overflow-y-auto">
                                    {sec.pdfFiles.map((file, fIdx) => (
                                      <div
                                        key={fIdx}
                                        className="flex items-center justify-between p-1.5 bg-white rounded-xl border border-sky-200 text-left text-xs font-semibold text-slate-800"
                                      >
                                        <span className="truncate text-[11px]">{file.name}</span>
                                        <button
                                          type="button"
                                          onClick={() => handleRemovePdfFileFromSection(secIdx, fIdx)}
                                          className="text-slate-400 hover:text-rose-600 cursor-pointer ml-1"
                                          title="Xóa file PDF này"
                                        >
                                          <X className="w-3.5 h-3.5" />
                                        </button>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                              <p className="text-[10px] text-slate-400">
                                Trích xuất hình vẽ tình huống Mondai 1 & Mondai 4, các lựa chọn in sẵn
                              </p>
                            </div>
                          </div>

                          <div className="p-2.5 bg-indigo-50/50 rounded-xl text-[11px] text-indigo-900 flex items-center gap-2">
                            <Sparkles className="w-4 h-4 text-amber-500 shrink-0" />
                            <span>
                              💡 <strong>Gợi ý:</strong> Tải cả <strong>file Audio (.mp3)</strong> và <strong>file PDF đề nghe</strong> sẽ giúp AI lấy đầy đủ cả tranh vẽ câu hỏi, các lựa chọn in sẵn trên giấy thi và mốc thời gian nghe chuẩn xác nhất.
                            </span>
                          </div>
                        </div>
                      ) : (
                        /* Section Đọc / Từ vựng / Ngữ pháp: Tải MULTIPLE PDF FILES */
                        <div className="border-2 border-dashed border-slate-300 dark:border-slate-700 rounded-2xl p-5 bg-slate-50/50 dark:bg-slate-800/30 space-y-3">
                          <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
                            <div className="flex items-center gap-3">
                              <FileText className="w-8 h-8 text-indigo-600 shrink-0" />
                              <div className="text-left">
                                <p className="text-xs font-bold text-slate-800 dark:text-slate-200">
                                  Chọn tệp PDF cho phần này (Hỗ trợ nhiều file)
                                </p>
                                <p className="text-[11px] text-slate-500">
                                  💡 Nếu phần này gồm ngữ pháp và bài đọc riêng, hãy chọn cả 2 file PDF. AI sẽ tự động kết hợp!
                                </p>
                              </div>
                            </div>

                            <label className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-xs cursor-pointer transition shadow-xs shrink-0">
                              <PlusCircle className="w-4 h-4" />
                              <span>+ Thêm file PDF</span>
                              <input
                                type="file"
                                multiple
                                accept=".pdf"
                                onChange={(e) => {
                                  handleAddPdfFilesToSection(secIdx, e.target.files)
                                  e.target.value = ''
                                }}
                                className="hidden"
                              />
                            </label>
                          </div>

                          {/* Danh sách các file PDF đã chọn trong section này */}
                          {sec.pdfFiles.length > 0 ? (
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2">
                              {sec.pdfFiles.map((file, fIdx) => (
                                <div
                                  key={fIdx}
                                  className="flex items-center justify-between p-2.5 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 text-xs shadow-2xs"
                                >
                                  <div className="flex items-center gap-2 overflow-hidden">
                                    <span className="w-5 h-5 rounded-md bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300 flex items-center justify-center font-bold text-[10px] shrink-0">
                                      {fIdx + 1}
                                    </span>
                                    <div className="truncate">
                                      <p className="font-semibold text-slate-800 dark:text-slate-200 truncate">
                                        {file.name}
                                      </p>
                                      <p className="text-[10px] text-slate-400">
                                        {(file.size / 1024 / 1024).toFixed(2)} MB
                                      </p>
                                    </div>
                                  </div>
                                  <button
                                    type="button"
                                    onClick={() => handleRemovePdfFileFromSection(secIdx, fIdx)}
                                    className="p-1 rounded-md text-slate-400 hover:text-rose-600 hover:bg-rose-50 cursor-pointer transition shrink-0 ml-1"
                                    title="Xóa file này"
                                  >
                                    <X className="w-4 h-4" />
                                  </button>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <p className="text-center text-[11px] text-slate-400 italic py-1">
                              Chưa có file PDF nào được chọn cho phần này.
                            </p>
                          )}
                        </div>
                      )}

                      {/* TÙY CHỌN: TẢI FILE BẢNG ĐÁP ÁN CHÍNH THỨC (正答表) */}
                      <div className="p-3.5 bg-emerald-50/60 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800 rounded-2xl space-y-1.5">
                        <div className="flex items-center justify-between">
                          <label className="text-xs font-bold text-emerald-950 dark:text-emerald-300 flex items-center gap-1.5">
                            <FileCheck className="w-4 h-4 text-emerald-600" />
                            <span>File bảng đáp án chính thức (Ảnh / PDF 正答表 - Tùy chọn):</span>
                          </label>
                          {sec.answerFile && (
                            <button
                              type="button"
                              onClick={() => updateSectionDraft(secIdx, { answerFile: null })}
                              className="text-[11px] text-rose-600 hover:underline font-semibold cursor-pointer"
                            >
                              Gỡ file
                            </button>
                          )}
                        </div>
                        <input
                          type="file"
                          accept="image/*,application/pdf"
                          onChange={(e) => {
                            const f = e.target.files?.[0] || null
                            updateSectionDraft(secIdx, { answerFile: f })
                          }}
                          className="text-xs file:mr-2.5 file:py-1 file:px-2.5 file:rounded-lg file:border-0 file:text-xs file:font-bold file:bg-emerald-100 file:text-emerald-800 hover:file:bg-emerald-200 cursor-pointer text-slate-600 dark:text-slate-300"
                        />
                        <p className="text-[11px] text-emerald-700 dark:text-emerald-400">
                          {sec.answerFile ? (
                            <span className="font-bold text-emerald-800 dark:text-emerald-300">
                              ✓ Đã chọn file đáp án: {sec.answerFile.name} (AI sẽ nạp đáp án theo bảng này)
                            </span>
                          ) : (
                            '💡 Nếu tải ảnh/PDF bảng 正答表, AI sẽ khớp đáp án chuẩn 100%. Nếu không tải, AI chỉ render đề thi và để trống đáp án.'
                          )}
                        </p>
                      </div>

                      {/* HÀNG NÚT THAO TÁC CỦA SECTION */}
                      <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => handleExtractSection(secIdx)}
                            disabled={sec.isExtracting || (isListening ? (!sec.audioFile && sec.pdfFiles.length === 0) : sec.pdfFiles.length === 0)}
                            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white font-bold rounded-xl text-xs shadow transition cursor-pointer disabled:opacity-40"
                          >
                            {sec.isExtracting ? (
                              <>
                                <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                <span>Đang trích xuất AI...</span>
                              </>
                            ) : (
                              <>
                                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                                <span>{hasExtractedQuestions ? 'Trích xuất lại AI phần này' : '⚡ Trích xuất AI phần này'}</span>
                              </>
                            )}
                          </button>
                        </div>

                        {hasExtractedQuestions && (
                          <button
                            type="button"
                            onClick={() =>
                              updateSectionDraft(secIdx, {
                                questions: [],
                                extractedPassage: null,
                              })
                            }
                            className="text-xs text-rose-500 hover:text-rose-700 font-medium cursor-pointer"
                          >
                            Xóa kết quả trích xuất
                          </button>
                        )}
                      </div>

                      {/* BẢNG TỔNG KẾT KẾT QUẢ TRÍCH XUẤT (GỌN GÀNG, KHÔNG BỊ CO CỤM) */}
                      {hasExtractedQuestions && (
                        <div className="p-4 bg-emerald-50/80 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800 rounded-2xl flex flex-wrap items-center justify-between gap-3 shadow-xs">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-bold shrink-0 shadow-xs">
                              <CheckCircle2 className="w-5 h-5" />
                            </div>
                            <div>
                              <p className="text-xs sm:text-sm font-extrabold text-slate-900 dark:text-white">
                                Đã trích xuất thành công {sec.questions.length} câu hỏi
                                {sec.extractedPassage ? ' • Kèm bài đọc hiểu' : ''}
                              </p>
                              <p className="text-[11px] text-slate-500">
                                Nhấn nút bên cạnh để mở phòng rà soát: sửa nội dung, chọn lại đáp án đúng hoặc bổ sung câu hỏi dễ dàng.
                              </p>
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => setEditingSectionIdx(secIdx)}
                            className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs sm:text-sm shadow-sm transition cursor-pointer active:scale-95"
                          >
                            <Edit3 className="w-4 h-4" />
                            <span>Mở phòng rà soát & Chỉnh sửa ({sec.questions.length} câu)</span>
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Bottom Finalize Bar */}
            <div className="sticky bottom-4 p-4 sm:p-5 bg-white dark:bg-slate-900 rounded-3xl border border-slate-300 dark:border-slate-700 shadow-xl flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 flex items-center justify-center text-indigo-600">
                  <CheckCircle2 className="w-6 h-6" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-900 dark:text-white">
                    Đã chuẩn bị {sectionDrafts.length} phần thi • Tổng {totalQuestionsExtracted} câu hỏi
                  </p>
                  <p className="text-[11px] text-slate-500">
                    Sau khi lưu, bạn sẽ được đưa đến phòng biên tập chuyên sâu để quản lý audio và phát hành đề.
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => navigate('/exams')}
                  className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-xs font-bold hover:bg-slate-100 transition cursor-pointer"
                >
                  Hủy bỏ
                </button>

                <button
                  type="button"
                  onClick={handleSaveSectionBySection}
                  disabled={loading}
                  className="inline-flex items-center gap-2 px-6 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs sm:text-sm shadow-lg shadow-indigo-600/25 transition cursor-pointer disabled:opacity-50"
                >
                  {loading ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      <span>{statusMessage || 'Đang lưu đề thi...'}</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Lưu toàn bộ đề thi (Tất cả các phần)</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* CHẾ ĐỘ 2: TẠO NHANH TỪ 1 FILE DUY NHẤT (AI_SINGLE) */}
        {/* ========================================================================= */}
        {creationMode === 'AI_SINGLE' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-slate-200 dark:border-slate-800 p-6 sm:p-10">
            <form onSubmit={handleCreateWithSingleAI} className="space-y-6">
              {/* Chọn Scope */}
              <div className="space-y-3 pb-4 border-b border-slate-100 dark:border-slate-800">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Phạm vi trích xuất từ file:
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                  <div
                    onClick={() => handleScopeChange('FULL')}
                    className={`p-3.5 rounded-2xl border-2 cursor-pointer transition flex flex-col justify-between ${
                      aiScope === 'FULL'
                        ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                        : 'border-slate-200 dark:border-slate-800 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <Layers className="w-5 h-5 text-indigo-600" />
                      <span className="font-bold text-xs text-slate-900 dark:text-white">Toàn bộ đề thi</span>
                    </div>
                    <p className="text-[11px] text-slate-500">1 file PDF gồm cả Từ vựng, Đọc hiểu và Audio nghe</p>
                  </div>

                  <div
                    onClick={() => handleScopeChange('VOCABULARY')}
                    className={`p-3.5 rounded-2xl border-2 cursor-pointer transition flex flex-col justify-between ${
                      aiScope === 'VOCABULARY'
                        ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                        : 'border-slate-200 dark:border-slate-800 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <Bookmark className="w-5 h-5 text-emerald-600" />
                      <span className="font-bold text-xs text-slate-900 dark:text-white">Chỉ phần Từ vựng</span>
                    </div>
                    <p className="text-[11px] text-slate-500">1 file PDF phần kiến thức từ vựng & Kanji</p>
                  </div>

                  <div
                    onClick={() => handleScopeChange('READING')}
                    className={`p-3.5 rounded-2xl border-2 cursor-pointer transition flex flex-col justify-between ${
                      aiScope === 'READING'
                        ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                        : 'border-slate-200 dark:border-slate-800 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <BookOpen className="w-5 h-5 text-sky-600" />
                      <span className="font-bold text-xs text-slate-900 dark:text-white">Chỉ phần Đọc hiểu</span>
                    </div>
                    <p className="text-[11px] text-slate-500">1 file PDF phần bài đọc hiểu và ngữ pháp</p>
                  </div>

                  <div
                    onClick={() => handleScopeChange('LISTENING')}
                    className={`p-3.5 rounded-2xl border-2 cursor-pointer transition flex flex-col justify-between ${
                      aiScope === 'LISTENING'
                        ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                        : 'border-slate-200 dark:border-slate-800 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-2">
                      <Headphones className="w-5 h-5 text-purple-600" />
                      <span className="font-bold text-xs text-slate-900 dark:text-white">Chỉ phần Nghe hiểu</span>
                    </div>
                    <p className="text-[11px] text-slate-500">Chỉ nộp file Audio MP3 bài nghe</p>
                  </div>
                </div>
              </div>

              {/* Tải tệp */}
              <div className="space-y-4">
                <h3 className="text-xs font-bold text-slate-700 dark:text-slate-300">
                  Tải tệp tài liệu:
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {aiScope !== 'LISTENING' && (
                    <div className="p-6 border-2 border-dashed border-slate-300 dark:border-slate-700 rounded-2xl flex flex-col items-center justify-center text-center relative hover:border-indigo-500 transition">
                      <input
                        type="file"
                        accept=".pdf"
                        onChange={(e) => {
                          if (e.target.files && e.target.files[0]) {
                            setSinglePdfFile(e.target.files[0])
                          }
                        }}
                        className="absolute inset-0 opacity-0 cursor-pointer"
                      />
                      <FileText className="w-10 h-10 text-indigo-500 mb-2" />
                      <p className="text-xs font-bold text-slate-700 dark:text-slate-300">
                        {singlePdfFile ? singlePdfFile.name : 'Chọn file PDF đề thi (.pdf)'}
                      </p>
                      <p className="text-[11px] text-slate-400 mt-1">
                        {singlePdfFile
                          ? `${(singlePdfFile.size / 1024 / 1024).toFixed(2)} MB`
                          : 'Kéo thả hoặc bấm để chọn'}
                      </p>
                    </div>
                  )}

                  {(aiScope === 'FULL' || aiScope === 'LISTENING') && (
                    <div className="p-6 border-2 border-dashed border-slate-300 dark:border-slate-700 rounded-2xl flex flex-col items-center justify-center text-center relative hover:border-indigo-500 transition">
                      <input
                        type="file"
                        accept=".mp3,.wav,.m4a"
                        onChange={(e) => {
                          if (e.target.files && e.target.files[0]) {
                            setSingleAudioFile(e.target.files[0])
                          }
                        }}
                        className="absolute inset-0 opacity-0 cursor-pointer"
                      />
                      <Headphones className="w-10 h-10 text-purple-500 mb-2" />
                      <p className="text-xs font-bold text-slate-700 dark:text-slate-300">
                        {singleAudioFile ? singleAudioFile.name : 'Chọn file Audio ca nghe (.mp3)'}
                      </p>
                      <p className="text-[11px] text-slate-400 mt-1">
                        {singleAudioFile
                          ? `${(singleAudioFile.size / 1024 / 1024).toFixed(2)} MB`
                          : 'Silero VAD & AI xử lý ca nghe'}
                      </p>
                    </div>
                  )}

                  <div className="p-6 border-2 border-dashed border-emerald-300 dark:border-emerald-700 bg-emerald-50/20 rounded-2xl flex flex-col items-center justify-center text-center relative hover:border-emerald-500 transition">
                    <input
                      type="file"
                      accept="image/*,application/pdf"
                      onChange={(e) => {
                        if (e.target.files && e.target.files[0]) {
                          setSingleAnswerFile(e.target.files[0])
                        }
                      }}
                      className="absolute inset-0 opacity-0 cursor-pointer"
                    />
                    <FileCheck className="w-10 h-10 text-emerald-600 mb-2" />
                    <p className="text-xs font-bold text-slate-700 dark:text-slate-300">
                      {singleAnswerFile ? singleAnswerFile.name : 'Bảng đáp án (Ảnh/PDF 正答表 - Tùy chọn)'}
                    </p>
                    <p className="text-[11px] text-emerald-600 mt-1">
                      {singleAnswerFile
                        ? `${(singleAnswerFile.size / 1024 / 1024).toFixed(2)} MB`
                        : 'Khớp đáp án chính xác 100%'}
                    </p>
                  </div>
                </div>
              </div>

              {loading && (
                <div className="p-4 rounded-2xl bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-900/60 flex items-center gap-3 animate-pulse">
                  <div className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin flex-shrink-0" />
                  <span className="text-xs font-semibold text-indigo-900 dark:text-indigo-200">
                    {statusMessage}
                  </span>
                </div>
              )}

              <div className="pt-4 flex items-center justify-between border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => navigate('/exams')}
                  className="px-5 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-sm font-semibold hover:bg-slate-100 transition cursor-pointer"
                >
                  Quay lại
                </button>

                <button
                  type="submit"
                  disabled={loading}
                  className="inline-flex items-center gap-2 px-8 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm shadow-lg shadow-indigo-600/25 transition disabled:opacity-50 cursor-pointer"
                >
                  <Sparkles className="w-4 h-4" />
                  <span>{loading ? 'Đang trích xuất AI...' : 'Trích xuất & Tạo đề thi'}</span>
                </button>
              </div>
            </form>
          </div>
        )}

        {/* ========================================================================= */}
        {/* CHẾ ĐỘ 3: TẠO KHUNG ĐỀ THỦ CÔNG (MANUAL) */}
        {/* ========================================================================= */}
        {creationMode === 'MANUAL' && (
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-sm border border-slate-200 dark:border-slate-800 p-6 sm:p-10">
            <form onSubmit={handleCreateManual} className="space-y-6">
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-slate-600 text-xs leading-relaxed">
                💡 Hệ thống sẽ khởi tạo khung đề thi chuẩn gồm các phần theo cấp độ JLPT {jlptLevel}. Sau đó bạn sẽ được chuyển đến phòng biên tập để tự thêm câu hỏi thủ công.
              </div>

              <div className="pt-4 flex items-center justify-between border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => navigate('/exams')}
                  className="px-5 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-sm font-semibold hover:bg-slate-100 transition cursor-pointer"
                >
                  Quay lại
                </button>

                <button
                  type="submit"
                  disabled={loading}
                  className="inline-flex items-center gap-2 px-8 py-3 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm shadow-lg shadow-indigo-600/25 transition cursor-pointer"
                >
                  <ArrowRight className="w-4 h-4" />
                  <span>{loading ? 'Đang tạo khung...' : 'Tạo khung đề & Mở trang soạn câu'}</span>
                </button>
              </div>
            </form>
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* MODAL STUDIO: PHÒNG RÀ SOÁT & CHỈNH SỬA CÂU HỎI TOÀN DIỆN */}
      {/* ========================================================================= */}
      {editingSectionIdx !== null && currentEditingSection && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 max-w-5xl w-full max-h-[92vh] flex flex-col overflow-hidden">
            {/* Modal Header */}
            <div className="p-5 sm:p-6 bg-slate-50/80 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-bold shadow-md shadow-indigo-600/25">
                  <Edit3 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white">
                    Phòng Rà Soát & Chỉnh Sửa Câu Hỏi
                  </h3>
                  <p className="text-xs text-slate-500">
                    {currentEditingSection.sectionTitle} • <strong>{currentEditingSection.questions.length} câu hỏi</strong>
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <label className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow transition cursor-pointer">
                  <FileCheck className="w-3.5 h-3.5" />
                  <span>{isApplyingAnswerKey ? 'Đang đọc đáp án...' : '📥 Nạp đáp án từ ảnh/PDF 正答表'}</span>
                  <input
                    type="file"
                    accept="image/*,application/pdf"
                    disabled={isApplyingAnswerKey}
                    className="hidden"
                    onChange={(e) => {
                      const f = e.target.files?.[0]
                      if (f) handleUploadAnswerKeyToCurrentSection(f)
                    }}
                  />
                </label>
                <button
                  type="button"
                  onClick={() => setEditingSectionIdx(null)}
                  className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow transition cursor-pointer"
                >
                  Hoàn tất & Đóng
                </button>
                <button
                  type="button"
                  onClick={() => setEditingSectionIdx(null)}
                  className="p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-5 sm:p-8 space-y-6">
              {(() => {
                type Cluster =
                  | {
                      type: 'PASSAGE'
                      passage: any
                      questions: { q: any; qIdx: number }[]
                    }
                  | {
                      type: 'SINGLE'
                      q: any
                      qIdx: number
                    }

                const clusters: Cluster[] = []
                let curPassageCluster: {
                  type: 'PASSAGE'
                  passage: any
                  questions: { q: any; qIdx: number }[]
                } | null = null

                currentEditingSection.questions.forEach((q: any, qIdx: number) => {
                  const isPassageMondai =
                    currentEditingSection.sessionType !== 'VOCABULARY' &&
                    [
                      'TEXT_GRAMMAR',
                      'SHORT_PASSAGE',
                      'MEDIUM_PASSAGE',
                      'LONG_PASSAGE',
                      'INFORMATION_RETRIEVAL',
                    ].includes(q.mondai_code || '')
                  const pass = q.passage
                  const hasPassage =
                    isPassageMondai &&
                    pass &&
                    Boolean((pass.content_jp || pass.contentJp || '').trim() || pass.image_url || pass.imageUrl)

                  if (hasPassage) {
                    const pTitle = (pass.title || '').trim()
                    const pContent = (pass.content_jp || pass.contentJp || '').trim()
                    const pImg = (pass.image_url || pass.imageUrl || '').trim()

                    const curTitle = (curPassageCluster?.passage?.title || '').trim()
                    const curContent = (
                      curPassageCluster?.passage?.content_jp || curPassageCluster?.passage?.contentJp || ''
                    ).trim()
                    const curImg = (
                      curPassageCluster?.passage?.image_url || curPassageCluster?.passage?.imageUrl || ''
                    ).trim()

                    const isSame =
                      curPassageCluster &&
                      ((pTitle && pTitle === curTitle) ||
                        (pContent && pContent === curContent) ||
                        (pImg && pImg === curImg))

                    if (isSame && curPassageCluster) {
                      curPassageCluster.questions.push({ q, qIdx })
                    } else {
                      curPassageCluster = {
                        type: 'PASSAGE',
                        passage: pass,
                        questions: [{ q, qIdx }],
                      }
                      clusters.push(curPassageCluster)
                    }
                  } else {
                    curPassageCluster = null
                    clusters.push({ type: 'SINGLE', q, qIdx })
                  }
                })

                const renderQuestionItem = (q: any, qIdx: number, isSubQuestion = false) => {
                  const isCloze = q.mondai_code === 'TEXT_GRAMMAR'
                  return (
                    <div
                      key={qIdx}
                      className={`p-4 sm:p-5 rounded-2xl border transition ${
                        isSubQuestion
                          ? 'bg-white dark:bg-slate-800/90 border-sky-200 dark:border-sky-800/70 shadow-xs'
                          : 'bg-slate-50/70 dark:bg-slate-800/40 border-slate-200 dark:border-slate-700 hover:border-slate-300'
                      } space-y-3`}
                    >
                      {/* Header */}
                      <div className="flex flex-wrap items-center justify-between gap-3 pb-2.5 border-b border-slate-200 dark:border-slate-700">
                        <div className="flex items-center gap-2.5">
                          <span className={`w-7 h-7 rounded-lg text-white font-black text-xs flex items-center justify-center shadow-xs ${
                            isCloze ? 'bg-sky-600' : 'bg-indigo-600'
                          }`}>
                            {q.question_number || qIdx + 1}
                          </span>
                          <span className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                            {isCloze ? (
                              <span className="px-2 py-0.5 rounded-md bg-sky-100 text-sky-800 dark:bg-sky-900/60 dark:text-sky-300 font-extrabold border border-sky-300 dark:border-sky-700">
                                Chỗ trống 【 {q.question_number || qIdx + 1} 】
                              </span>
                            ) : (
                              `Câu hỏi #${q.question_number || qIdx + 1}`
                            )}
                          </span>
                          {Boolean(q.has_visual_options || q.hasVisualOptions) && (
                            <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 dark:bg-purple-900/60 dark:text-purple-300 font-extrabold text-[10px] border border-purple-300 dark:border-purple-700 flex items-center gap-1 shadow-2xs">
                              🖼️ 4 Lựa chọn tranh vẽ
                            </span>
                          )}
                          {Boolean((q.image_url || q.imageUrl) && !q.has_visual_options && !q.hasVisualOptions) && (
                            <span className="px-2 py-0.5 rounded-md bg-indigo-100 text-indigo-800 dark:bg-indigo-900/60 dark:text-indigo-300 font-extrabold text-[10px] border border-indigo-300 dark:border-indigo-700 flex items-center gap-1 shadow-2xs">
                              🖼️ Tranh tình huống
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-2.5">
                          <div className="flex items-center gap-1.5">
                            <span className="text-[11px] font-semibold text-slate-500">Mondai:</span>
                            <select
                              value={q.mondai_code || ''}
                              onChange={(e) => handleUpdateQuestionField(editingSectionIdx, qIdx, 'mondai_code', e.target.value)}
                              className="text-xs px-2 py-1 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 font-medium"
                            >
                              {editingAvailableMondais.map((m) => (
                                <option key={m.code} value={m.code}>
                                  {m.nameVi}
                                </option>
                              ))}
                            </select>
                          </div>

                          <button
                            type="button"
                            onClick={() => handleDeleteSectionQuestion(editingSectionIdx, qIdx)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-rose-600 hover:bg-rose-50 border border-rose-200 text-xs font-bold cursor-pointer transition"
                            title="Xóa câu hỏi này"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                            <span>Xóa</span>
                          </button>
                        </div>
                      </div>

                      {/* 1. Bài sao: Star Adjuster */}
                      {(q.mondai_code === 'SENTENCE_COMPOSITION' || q.star_position != null || (q.question_text || '').includes('★')) && (
                        <div className="p-2.5 bg-amber-50/70 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800 rounded-xl flex flex-wrap items-center justify-between gap-2">
                          <div className="flex items-center gap-2">
                            <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                            <span className="text-xs font-bold text-amber-900 dark:text-amber-300">
                              Vị trí dấu sao ★:
                            </span>
                            <div className="flex items-center gap-1 ml-1">
                              {[1, 2, 3, 4].map((pos) => {
                                const isCurrent = Number(q.star_position ?? q.starPosition ?? 3) === pos
                                return (
                                  <button
                                    key={pos}
                                    type="button"
                                    onClick={() => handleSetStarPosition(editingSectionIdx, qIdx, pos)}
                                    className={`w-6 h-6 rounded-md text-xs font-black transition cursor-pointer flex items-center justify-center ${
                                      isCurrent
                                        ? 'bg-amber-500 text-white shadow-xs ring-1 ring-amber-400'
                                        : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-300 dark:border-slate-700 hover:bg-amber-100'
                                    }`}
                                  >
                                    {pos}
                                  </button>
                                )
                              })}
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => handleInsertStarTemplate(editingSectionIdx, qIdx)}
                            className="text-[11px] font-bold text-amber-700 dark:text-amber-400 hover:underline cursor-pointer"
                          >
                            <span>Chèn mẫu 4 gạch (＿＿＿ ＿★＿)</span>
                          </button>
                        </div>
                      )}

                      {/* 2. Đính kèm đoạn văn nếu là câu đứng một mình */}
                      {!isSubQuestion && currentEditingSection.sessionType !== 'VOCABULARY' && (q.passage || ['TEXT_GRAMMAR', 'SHORT_PASSAGE', 'MEDIUM_PASSAGE', 'LONG_PASSAGE', 'INFORMATION_RETRIEVAL'].includes(q.mondai_code)) && (
                        <div className="p-3 bg-sky-50/60 dark:bg-sky-950/20 border border-sky-200 dark:border-sky-800 rounded-xl space-y-2">
                          <div className="flex items-center justify-between pb-1 border-b border-sky-100 dark:border-sky-800">
                            <span className="text-xs font-bold text-sky-900 dark:text-sky-300 flex items-center gap-1.5">
                              <BookOpen className="w-3.5 h-3.5 text-sky-600" />
                              Đoạn văn bài đọc hiểu
                            </span>
                            {q.passage && (
                              <button
                                type="button"
                                onClick={() => handleRemoveQuestionPassage(editingSectionIdx, qIdx)}
                                className="text-[11px] text-rose-600 hover:underline font-semibold cursor-pointer"
                              >
                                Bỏ bài đọc riêng
                              </button>
                            )}
                          </div>
                          {!q.passage ? (
                            <button
                              type="button"
                              onClick={() => handleInitQuestionPassage(editingSectionIdx, qIdx)}
                              className="w-full py-1.5 border border-dashed border-sky-300 text-sky-600 rounded-xl text-xs font-semibold hover:bg-sky-50 cursor-pointer"
                            >
                              + Đính kèm bài đọc riêng
                            </button>
                          ) : (
                            <div className="space-y-2">
                              <input
                                type="text"
                                value={q.passage.title || ''}
                                onChange={(e) => handleUpdateQuestionPassageField(editingSectionIdx, qIdx, 'title', e.target.value)}
                                placeholder="Tiêu đề đoạn văn..."
                                className="w-full text-xs px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800"
                              />
                              <textarea
                                rows={3}
                                value={q.passage.content_jp || ''}
                                onChange={(e) => handleUpdateQuestionPassageField(editingSectionIdx, qIdx, 'content_jp', e.target.value)}
                                placeholder="Nội dung bài đọc tiếng Nhật..."
                                className="w-full text-xs font-serif p-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800"
                              />
                            </div>
                          )}
                        </div>
                      )}

                      {/* 3. Nội dung câu hỏi */}
                      <div>
                        <div className="flex items-center justify-between mb-1">
                          <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                            {isCloze ? 'Đề bài chỗ trống:' : 'Nội dung câu hỏi:'}
                          </label>
                          {!isCloze && q.mondai_code !== 'SENTENCE_COMPOSITION' && (
                            <button
                              type="button"
                              onClick={() => {
                                handleUpdateQuestionField(editingSectionIdx, qIdx, 'mondai_code', 'SENTENCE_COMPOSITION')
                                handleSetStarPosition(editingSectionIdx, qIdx, 3)
                              }}
                              className="text-[11px] text-amber-600 hover:underline font-semibold cursor-pointer flex items-center gap-1"
                            >
                              <Star className="w-3 h-3" /> Đặt làm câu sao ★
                            </button>
                          )}
                        </div>
                        <textarea
                          rows={2}
                          value={q.question_text ?? q.questionText ?? (isCloze ? `【 ${q.question_number || qIdx + 1} 】` : '')}
                          onChange={(e) => {
                            handleUpdateQuestionField(editingSectionIdx, qIdx, 'question_text', e.target.value)
                            handleUpdateQuestionField(editingSectionIdx, qIdx, 'questionText', e.target.value)
                          }}
                          placeholder={isCloze ? `【 ${q.question_number || qIdx + 1} 】` : "Ví dụ: きのう、あたらしい【本】をかいました。"}
                          className="w-full text-xs font-serif px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 focus:ring-1 focus:ring-indigo-500 focus:outline-none resize-y"
                        />
                      </div>

                      {/* Tranh minh họa câu hỏi nếu có */}
                      {(q.image_url || q.imageUrl) && (
                        <div className="p-2 border border-indigo-200 dark:border-indigo-800 rounded-xl bg-white dark:bg-slate-900 inline-flex items-center gap-3">
                          <img
                            src={q.image_url || q.imageUrl}
                            alt="Question illustration"
                            className="max-h-36 rounded object-contain border border-slate-200 dark:border-slate-700"
                          />
                          <span className="text-[11px] text-indigo-700 dark:text-indigo-400 font-bold">
                            🖼️ Tranh minh họa đề bài
                          </span>
                        </div>
                      )}

                      {/* 4. Các phương án trả lời */}
                      <div className="space-y-1.5">
                        <label className="block text-[11px] font-bold text-slate-600 dark:text-slate-400">
                          Các lựa chọn trả lời (Bấm nút tròn để chọn đáp án ĐÚNG):
                        </label>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {(q.options || []).map((opt: any, optIdx: number) => {
                            const isCorrect = Boolean(opt.is_correct || opt.correct)
                            return (
                              <div
                                key={optIdx}
                                className={`p-2 rounded-xl border-2 flex items-center gap-2 transition ${
                                  isCorrect
                                    ? 'border-emerald-500 bg-emerald-50/70 dark:bg-emerald-950/30'
                                    : 'border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800'
                                }`}
                              >
                                <input
                                  type="radio"
                                  name={`correct-opt-${qIdx}`}
                                  checked={isCorrect}
                                  onChange={() => handleSetCorrectOption(editingSectionIdx, qIdx, optIdx)}
                                  className="w-4 h-4 text-emerald-600 focus:ring-emerald-500 cursor-pointer shrink-0"
                                  title="Đánh dấu đáp án này là đúng"
                                />
                                <span
                                  className={`w-5 h-5 rounded-md flex items-center justify-center font-bold text-xs shrink-0 ${
                                    isCorrect ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300'
                                  }`}
                                >
                                  {opt.option_key || optIdx + 1}
                                </span>
                                {(opt.option_image_url || opt.optionImageUrl) && (
                                  <div className="shrink-0 p-0.5 border border-indigo-200 dark:border-indigo-800 rounded bg-white dark:bg-slate-900">
                                    <img
                                      src={opt.option_image_url || opt.optionImageUrl}
                                      alt={`Option ${optIdx + 1}`}
                                      className="h-12 w-auto max-w-[90px] rounded object-contain"
                                    />
                                  </div>
                                )}
                                <input
                                  type="text"
                                  value={opt.option_text ?? opt.optionText ?? ''}
                                  onChange={(e) => handleUpdateOptionText(editingSectionIdx, qIdx, optIdx, e.target.value)}
                                  placeholder={`Nhập đáp án ${optIdx + 1}...`}
                                  className="flex-1 text-xs font-serif p-1 rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 focus:outline-none"
                                />
                                {isCorrect && (
                                  <span className="text-[10px] font-extrabold text-emerald-700 dark:text-emerald-400 flex items-center gap-0.5 shrink-0 pr-1">
                                    <Check className="w-3 h-3" /> Đúng
                                  </span>
                                )}
                              </div>
                            )
                          })}
                        </div>
                      </div>

                      {/* 5. Giải thích */}
                      <div>
                        <input
                          type="text"
                          value={q.explanation_vi || ''}
                          onChange={(e) => handleUpdateQuestionField(editingSectionIdx, qIdx, 'explanation_vi', e.target.value)}
                          placeholder="Giải thích đáp án (tùy chọn)..."
                          className="w-full text-xs px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 focus:outline-none"
                        />
                      </div>
                    </div>
                  )
                }

                return (
                  <div className="space-y-6">
                    {clusters.map((cluster, cIdx) => {
                      if (cluster.type === 'SINGLE') {
                        return renderQuestionItem(cluster.q, cluster.qIdx, false)
                      }

                      // PASSAGE CLUSTER
                      const p = cluster.passage
                      const subQs = cluster.questions
                      const isCloze = subQs[0].q.mondai_code === 'TEXT_GRAMMAR'
                      const isPoster = subQs[0].q.mondai_code === 'INFORMATION_RETRIEVAL'
                      const qIndices = subQs.map((item) => item.qIdx)

                      return (
                        <div
                          key={`passage-group-${cIdx}`}
                          className="p-5 sm:p-6 bg-sky-50/70 dark:bg-sky-950/20 border-2 border-sky-300 dark:border-sky-800/80 rounded-3xl space-y-5 shadow-xs"
                        >
                          {/* Header Cụm Bài đọc */}
                          <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-sky-200 dark:border-sky-800">
                            <div className="flex items-center gap-2.5">
                              <div className="p-2 rounded-xl bg-sky-600 text-white shadow-xs">
                                <BookOpen className="w-5 h-5" />
                              </div>
                              <div>
                                <div className="flex items-center gap-2">
                                  <span className="text-xs font-black text-sky-950 dark:text-sky-300 uppercase tracking-wide">
                                    {isCloze
                                      ? 'Đoạn văn đục lỗ điền từ'
                                      : isPoster
                                      ? 'Tờ rơi / Biểu mẫu tìm kiếm thông tin'
                                      : 'Bài đọc hiểu dùng chung'}
                                  </span>
                                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-sky-100 text-sky-800 dark:bg-sky-900/60 dark:text-sky-200 border border-sky-300 dark:border-sky-700">
                                    {subQs.length} câu hỏi con (Câu {subQs[0].q.question_number || subQs[0].qIdx + 1}
                                    {subQs.length > 1 ? ` ~ Câu ${subQs[subQs.length - 1].q.question_number || subQs[subQs.length - 1].qIdx + 1}` : ''})
                                  </span>
                                </div>
                                <h4 className="text-sm font-bold text-slate-800 dark:text-slate-100">
                                  {p.title || (isCloze ? 'Bài đục lỗ' : 'Đoạn văn đọc hiểu')}
                                </h4>
                              </div>
                            </div>
                          </div>

                          {/* Editor Bài đọc (Chỉ hiện 1 lần cho cả cụm câu hỏi) */}
                          <div className="space-y-3 bg-white dark:bg-slate-800/90 p-4 rounded-2xl border border-sky-200 dark:border-sky-700/60 shadow-2xs">
                            <div>
                              <label className="block text-[11px] font-bold text-slate-700 dark:text-slate-300 mb-1">
                                Tiêu đề bài đọc / Tên tác phẩm:
                              </label>
                              <input
                                type="text"
                                value={p.title || ''}
                                onChange={(e) => handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'title', e.target.value)}
                                placeholder="VD: 「ポチ」 林 明"
                                className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                              />
                            </div>

                            {/* Flyer poster image if present */}
                            {(p.image_url || p.imageUrl || isPoster) && (
                              <div className="p-4 bg-purple-50/60 dark:bg-purple-950/20 border border-purple-200 dark:border-purple-800 rounded-2xl space-y-3">
                                <div className="flex flex-wrap items-center justify-between gap-2">
                                  <label className="text-xs font-bold text-purple-900 dark:text-purple-300 flex items-center gap-1.5">
                                    <ImageIcon className="w-4 h-4 text-purple-600" />
                                    <span>Hình ảnh tờ rơi / Biểu mẫu thông báo gốc:</span>
                                  </label>
                                  {(p.image_url || p.imageUrl) && (
                                    <button
                                      type="button"
                                      onClick={() => setLightboxImage(p.image_url || p.imageUrl)}
                                      className="px-2.5 py-1 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-xs font-bold transition flex items-center gap-1 shadow-xs cursor-pointer"
                                    >
                                      <ZoomIn className="w-3.5 h-3.5" />
                                      <span>🔍 Phóng to xem HD</span>
                                    </button>
                                  )}
                                </div>

                                {(p.image_url || p.imageUrl) ? (
                                  <div
                                    onClick={() => setLightboxImage(p.image_url || p.imageUrl)}
                                    className="group relative max-w-xl mx-auto rounded-2xl overflow-hidden border-2 border-purple-200 dark:border-purple-700 bg-white p-2 cursor-pointer shadow-sm hover:shadow-md transition"
                                    title="Bấm để phóng to xem rõ từng chữ"
                                  >
                                    <img
                                      src={p.image_url || p.imageUrl}
                                      alt="Notice flyer"
                                      className="w-full max-h-[460px] object-contain rounded-xl mx-auto transition group-hover:scale-[1.01]"
                                    />
                                    <div className="absolute inset-0 bg-slate-900/30 opacity-0 group-hover:opacity-100 transition flex items-center justify-center rounded-xl backdrop-blur-2xs">
                                      <span className="px-4 py-2 bg-slate-950/90 text-white text-xs font-bold rounded-xl shadow-lg flex items-center gap-2">
                                        <Maximize2 className="w-4 h-4 text-purple-300" />
                                        <span>Bấm vào để phóng to toàn màn hình HD</span>
                                      </span>
                                    </div>
                                  </div>
                                ) : (
                                  <div className="p-4 border-2 border-dashed border-purple-300 dark:border-purple-700 rounded-xl text-center text-xs text-purple-700 dark:text-purple-300">
                                    Chưa có ảnh tờ rơi. Bạn có thể dán URL hoặc tải ảnh cắt sẵn lên dưới đây.
                                  </div>
                                )}

                                <div className="flex flex-col sm:flex-row items-center gap-2">
                                  <input
                                    type="text"
                                    value={p.image_url || p.imageUrl || ''}
                                    onChange={(e) => {
                                      handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'image_url', e.target.value)
                                      handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'imageUrl', e.target.value)
                                    }}
                                    placeholder="URL ảnh tờ rơi (http://localhost:8000/static/extracted_images/...)"
                                    className="w-full text-xs px-3 py-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 font-mono focus:ring-2 focus:ring-purple-500 focus:outline-none"
                                  />
                                  <label className="shrink-0 px-3 py-2 bg-white dark:bg-slate-800 border border-purple-300 dark:border-purple-700 hover:bg-purple-50 text-purple-800 dark:text-purple-300 text-xs font-bold rounded-xl cursor-pointer transition flex items-center gap-1.5 shadow-2xs">
                                    <UploadCloud className="w-4 h-4 text-purple-600" />
                                    <span>Tải ảnh nét</span>
                                    <input
                                      type="file"
                                      accept="image/*"
                                      className="hidden"
                                      onChange={(e) => {
                                        const file = e.target.files?.[0]
                                        if (file) {
                                          const reader = new FileReader()
                                          reader.onload = (evt) => {
                                            const dataUrl = evt.target?.result as string
                                            handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'image_url', dataUrl)
                                            handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'imageUrl', dataUrl)
                                          }
                                          reader.readAsDataURL(file)
                                        }
                                      }}
                                    />
                                  </label>
                                </div>
                              </div>
                            )}

                            <div>
                              <div className="flex items-center justify-between mb-1.5">
                                <label className="block text-xs font-bold text-slate-800 dark:text-slate-200">
                                  📄 Nội dung bài đọc / Tờ rơi tiếng Nhật (Bản văn bản AI trích xuất rõ nét):
                                </label>
                                {isCloze && (
                                  <span className="text-[11px] text-sky-700 dark:text-sky-300 font-semibold">
                                    Chỗ trống: <code className="font-bold bg-sky-100 dark:bg-sky-900 px-1 py-0.5 rounded">【 21 】</code>
                                  </span>
                                )}
                              </div>
                              <p className="text-[11px] text-slate-500 dark:text-slate-400 mb-1.5">
                                💡 Học viên sẽ đọc bản văn bản này với font chữ to, chuẩn tiếng Nhật trên màn hình thi để làm bài dễ dàng, không bị mờ như khi xem ảnh chụp.
                              </p>
                              <textarea
                                rows={8}
                                value={p.content_jp || p.contentJp || ''}
                                onChange={(e) => {
                                  handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'content_jp', e.target.value)
                                  handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'contentJp', e.target.value)
                                }}
                                placeholder="【公園で遊ぶときの注意】&#10;1. ボール遊びは指定の広場で行ってください。&#10;2. 夜間（午後8時以降）は静かにしてください。..."
                                className="w-full text-sm font-serif leading-relaxed p-3.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                              />
                            </div>

                            {/* Live preview formatted Japanese text */}
                            {(p.content_jp || p.contentJp) && (
                              <div className="p-4 bg-slate-50 dark:bg-slate-900/60 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2">
                                <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                                  <span>👁️ Xem trước hiển thị trong đề thi:</span>
                                </div>
                                <div
                                  className="text-sm font-sans text-slate-800 dark:text-slate-200 leading-relaxed p-3 bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700"
                                  dangerouslySetInnerHTML={{ __html: formatJapanesePassageHtml(p.content_jp || p.contentJp || '') }}
                                />
                              </div>
                            )}

                            <div>
                              <label className="block text-[11px] font-bold text-slate-700 dark:text-slate-300 mb-1">
                                Bản dịch tiếng Việt (Tùy chọn):
                              </label>
                              <textarea
                                rows={2}
                                value={p.translation_vi || p.translationVi || ''}
                                onChange={(e) => {
                                  handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'translation_vi', e.target.value)
                                  handleUpdateClusterPassageField(editingSectionIdx, qIndices, 'translationVi', e.target.value)
                                }}
                                className="w-full text-xs p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-sky-500 focus:outline-none"
                              />
                            </div>
                          </div>

                          {/* Danh sách câu hỏi con */}
                          <div className="space-y-3 pt-1">
                            <span className="text-xs font-black text-sky-950 dark:text-sky-200 block">
                              Các câu hỏi con của bài đọc này ({subQs.length} câu):
                            </span>
                            {subQs.map(({ q, qIdx }) => renderQuestionItem(q, qIdx, true))}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )
              })()}

              {/* Nút thêm câu hỏi mới */}
              <button
                type="button"
                onClick={() => handleAddNewQuestion(editingSectionIdx)}
                className="w-full py-3 border-2 border-dashed border-indigo-300 dark:border-indigo-800 rounded-2xl text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 flex items-center justify-center gap-2 cursor-pointer transition"
              >
                <PlusCircle className="w-4 h-4" />
                <span>+ Thêm câu hỏi mới vào phần này</span>
              </button>
            </div>

            {/* Modal Footer */}
            <div className="p-4 sm:p-5 bg-slate-50 dark:bg-slate-800 border-t border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <span className="text-xs text-slate-500">
                Đang có <strong>{currentEditingSection.questions.length}</strong> câu hỏi trong phần thi này
              </span>

              <button
                type="button"
                onClick={() => setEditingSectionIdx(null)}
                className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs sm:text-sm shadow-sm transition cursor-pointer"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Hoàn tất & Cập nhật</span>
              </button>
            </div>
          </div>
        </div>
      )}
      {/* Lightbox Phóng To Ảnh Tờ Rơi / Minh Họa HD */}
      {lightboxImage && (
        <div
          className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4 cursor-zoom-out animate-in fade-in duration-200"
          onClick={() => setLightboxImage(null)}
        >
          <div
            className="relative max-w-5xl max-h-[92vh] flex flex-col items-center"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-full flex items-center justify-between pb-3 text-white">
              <span className="text-xs font-bold text-slate-300 flex items-center gap-2">
                <ImageIcon className="w-4 h-4 text-purple-400" />
                <span>Xem ảnh tờ rơi / biểu mẫu gốc chất lượng cao</span>
              </span>
              <button
                type="button"
                onClick={() => setLightboxImage(null)}
                className="p-1.5 bg-white/10 hover:bg-white/20 rounded-xl text-white transition cursor-pointer"
                title="Đóng cửa sổ"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            <div className="overflow-auto max-h-[85vh] rounded-2xl bg-white p-2 shadow-2xl">
              <img
                src={lightboxImage}
                alt="Phóng to tờ rơi"
                className="max-h-[82vh] w-auto mx-auto object-contain rounded-xl"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminExamCreatePage
