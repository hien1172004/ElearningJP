import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { examService } from '@/services/exam.service'
import type {
  AdminExamPreview,
  AdminTakingSection,
  AdminTakingQuestion,
  AdminTakingOption,
  UpdateQuestionInlinePayload,
  CreateQuestionPayload,
  TakingPassage,
} from '@/types'
import { formatJapanesePassageHtml, cleanQuestionPrompt } from '@/utils/format'
import {
  getMondaisBySessionType,
  getMondaiByCode,
  type JlptMondaiDefinition,
} from '@/data/jlptMondaiTypes'
import { QuestionAudioPlayer } from '@/components/exam/QuestionAudioPlayer'
import { AudioSegmentTrimmer } from '@/components/exam/AudioSegmentTrimmer'
import { StarOrderingQuestionForm } from '@/components/exam/forms/StarOrderingQuestionForm'
import { VocabGrammarStandardForm } from '@/components/exam/forms/VocabGrammarStandardForm'
import { PassageQuestionGroupForm } from '@/components/exam/forms/PassageQuestionGroupForm'
import { ListeningQuestionForm } from '@/components/exam/forms/ListeningQuestionForm'
import { FileUploadButton } from '@/components/exam/FileUploadButton'
import {
  FileText,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  PlusCircle,
  Edit3,
  Trash2,
  Save,
  Volume2,
  BookOpen,
  ArrowLeft,
  X,
  Share2,
  Headphones,
  Music,
  Star,
  Image as ImageIcon,
  Check,
  Layers,
  FileCheck,
  UploadCloud,
  EyeOff,
  AlertTriangle,
  RefreshCw,
  RotateCcw,
  Camera,
  Copy,
  Lock,
  ChevronDown,
  SlidersHorizontal,
  ArrowRight,
} from 'lucide-react'

export const AdminExamPreviewPage: React.FC = () => {
  const { examId } = useParams<{ examId: string }>()
  const navigate = useNavigate()

  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [previewData, setPreviewData] = useState<AdminExamPreview | null>(null)

  // Tab section đang chọn
  const [activeSectionIdx, setActiveSectionIdx] = useState<number>(0)

  // Trạng thái phát hành
  const [publishing, setPublishing] = useState<boolean>(false)
  const [publishSuccess, setPublishSuccess] = useState<boolean>(false)

  // Section Audio URL editing
  const [sectionAudioUrlInput, setSectionAudioUrlInput] = useState<string>('')
  const [isUpdatingAudio, setIsUpdatingAudio] = useState<boolean>(false)

  // Inline Question Editing
  const [editingQuestionId, setEditingQuestionId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<UpdateQuestionInlinePayload>({
    questionText: '',
  })
  const [savingQuestion, setSavingQuestion] = useState<boolean>(false)

  // Modal: Thêm câu hỏi thủ công theo đặc thù từng phần & Mondai
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false)
  const [addingQuestion, setAddingQuestion] = useState<boolean>(false)
  const [selectedMondaiCode, setSelectedMondaiCode] = useState<string>('')
  const [newQuestionForm, setNewQuestionForm] = useState<CreateQuestionPayload>({
    mondaiCode: '',
    questionText: '',
    explanationVi: '',
    points: 1,
    starPosition: undefined,
    hasVisualOptions: false,
    imageUrl: '',
    audioUrl: '',
    audioStartTime: undefined,
    audioEndTime: undefined,
    transcriptJp: '',
    transcriptVi: '',
    passageTitle: '',
    passageContentJp: '',
    passageTranslationVi: '',
    passageImageUrl: '',
    options: [
      { optionKey: 1, optionText: '', optionImageUrl: '', isCorrect: true },
      { optionKey: 2, optionText: '', optionImageUrl: '', isCorrect: false },
      { optionKey: 3, optionText: '', optionImageUrl: '', isCorrect: false },
      { optionKey: 4, optionText: '', optionImageUrl: '', isCorrect: false },
    ],
  })

  // Modal: Trích xuất AI cho riêng Section này
  const [isAiSectionModalOpen, setIsAiSectionModalOpen] = useState<boolean>(false)
  const [aiSectionLoading, setAiSectionLoading] = useState<boolean>(false)
  const [aiSectionError, setAiSectionError] = useState<string | null>(null)
  const [aiAudioFile, setAiAudioFile] = useState<File | null>(null)
  const [aiPdfFiles, setAiPdfFiles] = useState<File[]>([])
  const [aiAnswerFile, setAiAnswerFile] = useState<File | null>(null)
  const [isBatchApplyingAnswerKey, setIsBatchApplyingAnswerKey] = useState<boolean>(false)
  const [aiExtractedQuestions, setAiExtractedQuestions] = useState<any[]>([])
  const [aiExtractedPassage, setAiExtractedPassage] = useState<any | null>(null)
  const [aiSaveMode, setAiSaveMode] = useState<'OVERWRITE' | 'APPEND'>('OVERWRITE')
  const [zoomImageUrl, setZoomImageUrl] = useState<string | null>(null)

  // Snapshot / Khôi phục bản lưu trước đó
  const [snapshotTimestamp, setSnapshotTimestamp] = useState<string | null>(null)
  const [isRollbackModalOpen, setIsRollbackModalOpen] = useState<boolean>(false)
  const [isRollingBack, setIsRollingBack] = useState<boolean>(false)
  const [isTakingSnapshot, setIsTakingSnapshot] = useState<boolean>(false)

  // Dropdown công cụ nâng cao
  const [isToolsDropdownOpen, setIsToolsDropdownOpen] = useState<boolean>(false)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as HTMLElement
      if (!target.closest('#tools-dropdown-container')) {
        setIsToolsDropdownOpen(false)
      }
    }
    if (isToolsDropdownOpen) {
      document.addEventListener('click', handleClickOutside)
    }
    return () => document.removeEventListener('click', handleClickOutside)
  }, [isToolsDropdownOpen])

  useEffect(() => {
    if (!examId) return
    const fetchPreview = async () => {
      try {
        setLoading(true)
        const data = await examService.getAdminExamPreview(Number(examId))
        setPreviewData(data)
        if (data.sections?.[0]?.audioUrl) {
          setSectionAudioUrlInput(data.sections[0].audioUrl)
        }

        // Tự động ghi nhớ bản Snapshot ban đầu khi vừa mở trang (nếu chưa có trong phiên này)
        const cachedSnapTime = sessionStorage.getItem(`exam_snapshot_time_${examId}`)
        if (!cachedSnapTime) {
          const nowStr = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
          sessionStorage.setItem(`exam_snapshot_${examId}`, JSON.stringify(data))
          sessionStorage.setItem(`exam_snapshot_time_${examId}`, nowStr)
          setSnapshotTimestamp(nowStr)
          examService.saveSnapshot(Number(examId), data).catch(() => {})
        } else {
          setSnapshotTimestamp(cachedSnapTime)
        }
      } catch (err: any) {
        setError(err.response?.data?.message || 'Không thể tải đề thi để xem trước')
      } finally {
        setLoading(false)
      }
    }
    fetchPreview()
  }, [examId])

  const activeSection: AdminTakingSection | undefined = previewData?.sections?.[activeSectionIdx]

  useEffect(() => {
    if (activeSection?.audioUrl) {
      setSectionAudioUrlInput(activeSection.audioUrl)
    } else {
      setSectionAudioUrlInput('')
    }
  }, [activeSectionIdx, activeSection])

  // Danh sách Mondai khả dụng cho Section này (lọc theo cấp độ JLPT chuẩn)
  const availableMondais = getMondaisBySessionType(activeSection?.sessionType, previewData?.jlptLevel)

  // Danh sách các đoạn văn đã có trong Section để dùng chung (tránh trùng lặp)
  const existingPassagesInActiveSection = React.useMemo(() => {
    if (!activeSection) return []
    const map = new Map<number, TakingPassage>()
    activeSection.questions.forEach((q) => {
      if (q.passage && q.passage.id) {
        map.set(q.passage.id, q.passage)
      }
    })
    return Array.from(map.values())
  }, [activeSection])

  // Cập nhật Audio URL cho cả phần thi nghe
  const handleSaveSectionAudio = async () => {
    if (!examId || !activeSection) return
    try {
      setIsUpdatingAudio(true)
      await examService.updateSectionAudio(Number(examId), activeSection.id, sectionAudioUrlInput.trim())
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
      alert('Đã cập nhật file Audio cho phần thi thành công!')
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi cập nhật Audio')
    } finally {
      setIsUpdatingAudio(false)
    }
  }

  // Bắt đầu sửa câu hỏi inline
  const handleStartEdit = (q: AdminTakingQuestion) => {
    setEditingQuestionId(q.id)
    // Rule: Mỗi câu chỉ có DUY NHẤT 1 đáp án đúng
    const correctOpts = q.options.filter((opt: AdminTakingOption) => opt.correct)
    const singleCorrectKey = correctOpts.length > 1
      ? (correctOpts.find((o) => o.optionKey > 1)?.optionKey || correctOpts[0]?.optionKey || 1)
      : (correctOpts[0]?.optionKey || 1)

    setEditForm({
      mondaiCode: q.mondaiCode || '',
      questionText: q.questionText,
      imageUrl: q.imageUrl || '',
      audioUrl: q.audioUrl || '',
      audioStartTime: q.audioStartTime,
      audioEndTime: q.audioEndTime,
      transcriptJp: q.transcriptJp,
      transcriptVi: q.transcriptVi,
      explanationVi: q.explanationVi,
      starPosition: q.starPosition,
      hasVisualOptions: q.hasVisualOptions || false,
      options: q.options.map((opt: AdminTakingOption) => ({
        id: opt.id,
        optionKey: opt.optionKey,
        optionText: opt.optionText || '',
        optionImageUrl: opt.optionImageUrl || '',
        correct: opt.optionKey === singleCorrectKey,
        isCorrect: opt.optionKey === singleCorrectKey,
      })),
      correctOptionKey: singleCorrectKey,
    })
  }

  // Lưu chỉnh sửa câu hỏi inline
  const handleSaveQuestion = async (qId: number) => {
    if (!examId) return
    try {
      setSavingQuestion(true)
      await examService.updateQuestionInline(Number(examId), qId, editForm)
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
      setEditingQuestionId(null)

      if (previewData?.totalAttempts && previewData.totalAttempts > 0) {
        alert(
          `✅ Đã lưu cập nhật câu hỏi thành công!\n\n📌 LƯU Ý NGHIỆP VỤ: Đề thi này đã có ${previewData.totalAttempts} bài thi đã nộp. Nếu bạn vừa thay đổi đáp án đúng hoặc điểm số, hãy nhấn nút [Chấm lại ${previewData.totalAttempts} bài thi] ở đầu trang để hệ thống tự động cập nhật lại điểm số cho các thí sinh cũ.`
        )
      }
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi lưu thay đổi câu hỏi')
    } finally {
      setSavingQuestion(false)
    }
  }

  // Xóa câu hỏi với cảnh báo an toàn khi đã có lượt thi hoặc đang có thí sinh làm bài
  const handleDeleteQuestion = async (qId: number) => {
    if (!examId) return

    if (previewData?.inProgressAttempts && previewData.inProgressAttempts > 0) {
      alert(
        `⛔ KHÔNG THỂ XÓA CÂU HỎI!\n\nHiện đang có ${previewData.inProgressAttempts} thí sinh đang trực tiếp làm bài thi này. Việc xóa câu hỏi sẽ làm hỏng phiên thi và mất bài làm của thí sinh.\n\nVui lòng chờ thí sinh hoàn thành hoặc sử dụng tính năng "Nhân bản đề (Bản mới)".`
      )
      return
    }

    if (previewData?.totalAttempts && previewData.totalAttempts > 0) {
      if (
        !window.confirm(
          `⚠️ CẢNH BÁO: Đề thi này đã có ${previewData.totalAttempts} lượt nộp bài!\n\nNếu xóa câu hỏi này, thí sinh thi sau sẽ bị lệch số câu so với thí sinh thi trước.\n\nKhuyến nghị: Bạn nên dùng nút "Nhân bản đề (Bản mới)" ở trên để tạo đề mới an toàn.\n\nBạn có chắc chắn VẪN MUỐN XÓA câu này?`
        )
      ) {
        return
      }
    } else {
      if (!window.confirm('Bạn có chắc chắn muốn xóa câu hỏi này khỏi đề thi không?')) return
    }

    try {
      await examService.deleteQuestion(Number(examId), qId)
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi xóa câu hỏi')
    }
  }

  // Nhân bản đề thi sang bản mới (Clone Exam)
  const [isCloning, setIsCloning] = useState<boolean>(false)
  const handleCloneExam = async () => {
    if (!examId) return
    if (!window.confirm('Bạn có muốn nhân bản đề thi này sang một bản mới (Draft) để biên tập/chỉnh sửa an toàn mà không ảnh hưởng kết quả cũ?')) return

    try {
      setIsCloning(true)
      const newExamId = await examService.cloneExam(Number(examId))
      alert('Đã nhân bản đề thi thành công! Đang chuyển hướng sang bản mới...')
      navigate(`/admin/exams/${newExamId}/preview`)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi nhân bản đề thi')
    } finally {
      setIsCloning(false)
    }
  }

  // Chấm lại toàn bộ bài thi đã nộp (Re-grade Submissions)
  const [isRegrading, setIsRegrading] = useState<boolean>(false)
  const handleRegradeExam = async () => {
    if (!examId) return
    const attempts = previewData?.totalAttempts || 0
    if (
      !window.confirm(
        `Bạn có chắc chắn muốn chấm lại điểm cho tất cả ${attempts} bài thi đã nộp theo các đáp án hiện tại không? Điểm số của học viên sẽ được tự động tính toán lại.`
      )
    )
      return

    try {
      setIsRegrading(true)
      const count = await examService.regradeExam(Number(examId))
      alert(`✓ Đã chấm lại thành công cho ${count} lượt làm bài!`)
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi chấm lại bài thi')
    } finally {
      setIsRegrading(false)
    }
  }

  // Quản lý sửa bài đọc dùng chung
  const [editingPassageId, setEditingPassageId] = useState<number | null>(null)
  const [passageEditForm, setPassageEditForm] = useState<{
    title: string
    contentJp: string
    translationVi: string
    imageUrl: string
  }>({
    title: '',
    contentJp: '',
    translationVi: '',
    imageUrl: '',
  })
  const [savingPassage, setSavingPassage] = useState(false)

  const handleStartEditPassage = (p: TakingPassage) => {
    setEditingPassageId(p.id || -1)
    setPassageEditForm({
      title: p.title || '',
      contentJp: p.contentJp || '',
      translationVi: p.translationVi || '',
      imageUrl: p.imageUrl || '',
    })
  }

  const handleSavePassage = async (passageId: number) => {
    if (!examId) return
    try {
      setSavingPassage(true)
      await examService.updatePassage(Number(examId), passageId, passageEditForm)
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
      setEditingPassageId(null)
      alert('Đã cập nhật bài đọc thành công!')
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi cập nhật bài đọc')
    } finally {
      setSavingPassage(false)
    }
  }

  const handleDeletePassage = async (passageId: number, questions: AdminTakingQuestion[]) => {
    if (!examId) return
    if (!window.confirm(`Bạn có chắc chắn muốn xóa bài đọc này và toàn bộ ${questions.length} câu hỏi liên quan không?`)) return
    try {
      await examService.deletePassage(Number(examId), passageId)
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
      alert('Đã xóa bài đọc và các câu hỏi liên quan thành công!')
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi xóa bài đọc')
    }
  }

  // Tạo điểm lưu thủ công (Manual Snapshot / Checkpoint)
  const handleCreateManualSnapshot = async () => {
    if (!examId || !previewData) return
    try {
      setIsTakingSnapshot(true)
      const nowStr = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
      sessionStorage.setItem(`exam_snapshot_${examId}`, JSON.stringify(previewData))
      sessionStorage.setItem(`exam_snapshot_time_${examId}`, nowStr)
      setSnapshotTimestamp(nowStr)
      await examService.saveSnapshot(Number(examId), previewData)
      alert(`📸 Đã tạo điểm lưu đề thi thành công vào lúc ${nowStr}! Bạn có thể yên tâm chỉnh sửa tiếp.`)
    } catch (err: any) {
      alert(err.message || 'Lỗi khi tạo điểm lưu')
    } finally {
      setIsTakingSnapshot(false)
    }
  }

  // Thực hiện Rollback về trạng thái snapshot
  const handleExecuteRollback = async () => {
    if (!examId) return
    try {
      setIsRollingBack(true)
      let snapshotPayload: any = null
      const cachedJson = sessionStorage.getItem(`exam_snapshot_${examId}`)
      if (cachedJson) {
        try {
          snapshotPayload = JSON.parse(cachedJson)
        } catch {}
      }

      const res: any = await examService.rollbackExam(Number(examId), snapshotPayload)
      const restoredData = res?.data || (await examService.getAdminExamPreview(Number(examId)))
      setPreviewData(restoredData)
      setIsRollbackModalOpen(false)
      alert(`⏪ Đã hoàn tác thành công đề thi về trạng thái lúc ${snapshotTimestamp || 'ban đầu'}!`)
    } catch (err: any) {
      alert(err.response?.data?.message || err.message || 'Lỗi khi khôi phục đề thi về bản lưu trước')
    } finally {
      setIsRollingBack(false)
    }
  }

  // Mở modal thêm câu hỏi với Mondai được chỉ định
  const handleOpenAddModal = (targetMondaiCode?: string, presetPassage?: TakingPassage) => {
    const defaultMondai = targetMondaiCode || availableMondais[0]?.code || ''
    const mondaiDef = getMondaiByCode(defaultMondai)
    const isListening = activeSection?.sessionType === 'LISTENING'

    setSelectedMondaiCode(defaultMondai)
    setNewQuestionForm({
      mondaiCode: defaultMondai,
      questionText: isListening ? 'Câu hỏi nghe hiểu' : '',
      explanationVi: '',
      points: 1,
      starPosition: mondaiDef?.questionType === 'STAR' ? 3 : undefined,
      hasVisualOptions: false,
      imageUrl: '',
      audioUrl: '',
      audioStartTime: isListening ? 0 : undefined,
      audioEndTime: isListening ? 30 : undefined,
      transcriptJp: '',
      transcriptVi: '',
      passageTitle: presetPassage?.title || (mondaiDef?.hasPassage ? 'Bài đọc hiểu' : ''),
      passageContentJp: presetPassage?.contentJp || '',
      passageTranslationVi: presetPassage?.translationVi || '',
      passageImageUrl: presetPassage?.imageUrl || '',
      options: [
        { optionKey: 1, optionText: '', optionImageUrl: '', isCorrect: true },
        { optionKey: 2, optionText: '', optionImageUrl: '', isCorrect: false },
        { optionKey: 3, optionText: '', optionImageUrl: '', isCorrect: false },
        { optionKey: 4, optionText: '', optionImageUrl: '', isCorrect: false },
      ],
    })
    setIsAddModalOpen(true)
  }

  // Đổi Mondai trong modal thêm câu hỏi
  const handleMondaiChangeInModal = (code: string) => {
    setSelectedMondaiCode(code)
    const mondaiDef = getMondaiByCode(code)
    const optCount = mondaiDef?.defaultOptionsCount || 4

    setNewQuestionForm((prev) => ({
      ...prev,
      mondaiCode: code,
      starPosition: mondaiDef?.questionType === 'STAR' ? 3 : undefined,
      passageTitle: mondaiDef?.hasPassage && !prev.passageTitle ? 'Bài đọc hiểu mới' : prev.passageTitle,
      options: Array.from({ length: optCount }, (_, i) => ({
        optionKey: i + 1,
        optionText: prev.options[i]?.optionText || '',
        optionImageUrl: prev.options[i]?.optionImageUrl || '',
        isCorrect: i === 0,
      })),
    }))
  }

  // Submit thêm câu hỏi
  const handleAddQuestionSubmit = async () => {
    if (!examId || !activeSection) return
    if (!newQuestionForm.questionText.trim()) {
      alert('Vui lòng nhập nội dung câu hỏi')
      return
    }

    try {
      setAddingQuestion(true)
      await examService.addQuestion(Number(examId), activeSection.id, newQuestionForm)
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
      setIsAddModalOpen(false)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi thêm câu hỏi mới')
    } finally {
      setAddingQuestion(false)
    }
  }

  // Xử lý AI Section Extractor (Trích xuất cho riêng phần này)
  const handleRunAiSectionExtraction = async () => {
    if (!activeSection) return
    setAiSectionLoading(true)
    setAiSectionError(null)

    const isListening = activeSection.sessionType === 'LISTENING'
    const aiBase = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000'

    try {
      if (isListening) {
        if (!aiAudioFile && (!aiPdfFiles || aiPdfFiles.length === 0)) {
          throw new Error('Vui lòng chọn ít nhất file âm thanh MP3 hoặc file PDF đề thi nghe (hoặc cả hai)')
        }
        const formData = new FormData()
        if (aiAudioFile) {
          formData.append('audio_file', aiAudioFile)
        }
        if (aiPdfFiles && aiPdfFiles.length > 0) {
          aiPdfFiles.forEach((f) => {
            formData.append('pdf_files', f)
          })
          formData.append('pdf_file', aiPdfFiles[0])
        }

        if (aiAnswerFile) {
          formData.append('answer_file', aiAnswerFile)
        }

        const res = await fetch(`${aiBase}/api/v1/ai/extract-section?section_type=LISTENING&jlpt_level=${previewData?.jlptLevel || 'N5'}`, {
          method: 'POST',
          body: formData,
        })
        if (!res.ok) {
          const errData = await res.json().catch(() => null)
          throw new Error(errData?.detail || 'AI không thể xử lý file audio/PDF đề nghe. Vui lòng kiểm tra lại.')
        }
        const result = await res.json()
        const items = result.questions || []
        setAiExtractedQuestions(items)
        // Lưu audio_url tổng của section nghe để tự động fill vào section khi apply
        if (result.audio_url) {
          setSectionAudioUrlInput(result.audio_url)
        }
      } else {
        if (!aiPdfFiles || aiPdfFiles.length === 0) {
          throw new Error('Vui lòng chọn ít nhất 1 file PDF đề thi (ví dụ: file ngữ pháp hoặc bài đọc)')
        }
        const formData = new FormData()
        aiPdfFiles.forEach((f) => {
          formData.append('pdf_files', f)
        })
        formData.append('pdf_file', aiPdfFiles[0])
        if (aiAnswerFile) {
          formData.append('answer_file', aiAnswerFile)
        }

        const secType = activeSection.sessionType === 'KNOWLEDGE_READING'
          ? 'KNOWLEDGE_READING'
          : activeSection.sessionType === 'GRAMMAR_READING'
          ? 'GRAMMAR_READING'
          : 'VOCABULARY'
        const res = await fetch(`${aiBase}/api/v1/ai/extract-section?section_type=${secType}&jlpt_level=${previewData?.jlptLevel || 'N5'}`, {
          method: 'POST',
          body: formData,
        })
        if (!res.ok) {
          const errData = await res.json().catch(() => null)
          throw new Error(errData?.detail || 'AI không thể trích xuất file PDF.')
        }
        const result = await res.json()
        if (result.passage) {
          setAiExtractedPassage(result.passage)
        }
        setAiExtractedQuestions(result.questions || [])
      }
    } catch (err: any) {
      setAiSectionError(err.message || 'Lỗi khi phân tích bằng AI')
    } finally {
      setAiSectionLoading(false)
    }
  }

  // Áp dụng các câu hỏi vừa trích xuất bằng AI vào phần thi hiện tại
  const handleApplyAiQuestionsToSection = async () => {
    if (!examId || !activeSection) return
    try {
      setAiSectionLoading(true)
      const isListening = activeSection.sessionType === 'LISTENING'

      // Nếu người dùng chọn GHI ĐÈ và phần thi này đang có câu hỏi -> Xóa sạch các câu cũ
      if (aiSaveMode === 'OVERWRITE' && (activeSection.questions?.length || 0) > 0) {
        try {
          await examService.clearSectionQuestions(Number(examId), activeSection.id)
        } catch (clearErr) {
          console.warn('clearSectionQuestions API failed, falling back to sequential delete:', clearErr)
          for (const oldQ of activeSection.questions) {
            await examService.deleteQuestion(Number(examId), oldQ.id).catch(() => {})
          }
        }
      }

      if (isListening && aiExtractedQuestions.length > 0) {
        for (const q of aiExtractedQuestions) {
          // Rule: Mỗi câu chỉ có DUY NHẤT 1 đáp án đúng
          const opts = q.options || []
          const correctIndices = opts
            .map((o: any, i: number) => (Boolean(o.is_correct ?? o.correct ?? o.isCorrect) ? i : -1))
            .filter((i: number) => i !== -1)
          let chosenCorrectIdx = 0
          if (correctIndices.length === 1) {
            chosenCorrectIdx = correctIndices[0]
          } else if (correctIndices.length > 1) {
            chosenCorrectIdx = correctIndices.find((i: number) => i > 0) ?? correctIndices[0]
          }

          await examService.addQuestion(Number(examId), activeSection.id, {
            mondaiCode: q.mondai_code || 'TASK_BASED',
            questionText: q.question_text || `Câu hỏi nghe số ${q.question_number || ''}`,
            imageUrl: q.image_url || undefined,
            hasVisualOptions: Boolean(q.has_visual_options),
            audioUrl: q.audio_url || undefined,
            audioStartTime: q.audio_start_time !== undefined ? q.audio_start_time : 0,
            audioEndTime: q.audio_end_time !== undefined ? q.audio_end_time : 30,
            transcriptJp: q.transcript_jp || '',
            explanationVi: q.explanation_vi || 'Nghe kịch bản và chọn câu trả lời chính xác.',
            points: 1,
            options: opts.map((opt: any, idx: number) => ({
              optionKey: opt.option_key || idx + 1,
              optionText: opt.option_text || '選択肢',
              optionImageUrl: opt.option_image_url || opt.image_url || undefined,
              isCorrect: idx === chosenCorrectIdx,
            })),
          })
        }
        // Tự động cập nhật audio tổng cho section nghe nếu có
        if (sectionAudioUrlInput.trim()) {
          try {
            await examService.updateSectionAudio(Number(examId), activeSection.id, sectionAudioUrlInput.trim())
          } catch (audioErr) {
            console.warn('Auto-save section audio URL failed:', audioErr)
          }
        }
      } else if (aiExtractedQuestions.length > 0) {
        for (const q of aiExtractedQuestions) {
          const isPassageMondai = ['TEXT_GRAMMAR', 'SHORT_PASSAGE', 'MEDIUM_PASSAGE', 'LONG_PASSAGE', 'INFORMATION_RETRIEVAL'].includes(
            (q.mondai_code || '').toUpperCase()
          )
          const pass = isPassageMondai ? (q.passage || aiExtractedPassage) : undefined

          // Rule: Mỗi câu chỉ có DUY NHẤT 1 đáp án đúng
          const opts = q.options || []
          const correctIndices = opts
            .map((o: any, i: number) => (Boolean(o.is_correct ?? o.correct ?? o.isCorrect) ? i : -1))
            .filter((i: number) => i !== -1)
          let chosenCorrectIdx = 0
          if (correctIndices.length === 1) {
            chosenCorrectIdx = correctIndices[0]
          } else if (correctIndices.length > 1) {
            // Nếu có nhiều đáp án đúng (ví dụ opt 1 và opt 4), ưu tiên đáp án khác 0 từ file đáp án
            chosenCorrectIdx = correctIndices.find((i: number) => i > 0) ?? correctIndices[0]
          }

          await examService.addQuestion(Number(examId), activeSection.id, {
            mondaiCode: q.mondai_code || 'CONTEXTUAL_USE',
            questionText: q.question_text || 'Câu hỏi trắc nghiệm',
            imageUrl: q.image_url || undefined,
            starPosition: q.star_position || undefined,
            passageTitle: pass?.title,
            passageContentJp: pass?.content_jp,
            passageTranslationVi: pass?.translation_vi,
            passageImageUrl: pass?.image_url,
            explanationVi: q.explanation_vi,
            points: 1,
            options: opts.map((opt: any, idx: number) => ({
              optionKey: opt.option_key || idx + 1,
              optionText: opt.option_text || (typeof opt === 'string' ? opt : `Đáp án ${idx + 1}`),
              optionImageUrl: opt.option_image_url || opt.image_url || undefined,
              isCorrect: idx === chosenCorrectIdx,
            })),
          })
        }
      }

      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
      setIsAiSectionModalOpen(false)
      setAiExtractedQuestions([])
      setAiExtractedPassage(null)
      alert(
        aiSaveMode === 'OVERWRITE' && (activeSection.questions?.length || 0) > 0
          ? 'Đã ghi đè và nạp mới toàn bộ câu hỏi vào phần thi này thành công!'
          : 'Đã bổ sung thành công các câu hỏi trích xuất từ AI vào phần này!'
      )
    } catch (err: any) {
      alert(err.message || 'Lỗi khi lưu câu hỏi AI vào phần thi')
    } finally {
      setAiSectionLoading(false)
    }
  }

  const [unpublishing, setUnpublishing] = useState<boolean>(false)

  // Phát hành đề thi
  const handlePublishExam = async () => {
    if (!examId) return
    if (!window.confirm('Bạn có chắc chắn muốn phát hành đề thi này cho tất cả học viên không?')) return

    try {
      setPublishing(true)
      await examService.publishExam(Number(examId))
      setPublishSuccess(true)
      setPreviewData((prev) => (prev ? { ...prev, published: true } : prev))
      setTimeout(() => {
        navigate('/admin/dashboard')
      }, 1200)
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi phát hành đề thi')
    } finally {
      setPublishing(false)
    }
  }

  // Thu hồi đề thi về Bản Nháp
  const handleUnpublishExam = async () => {
    if (!examId) return
    if (
      !window.confirm(
        'Bạn có chắc chắn muốn thu hồi đề thi này về Bản Nháp? Học viên sẽ không còn nhìn thấy hay làm bài thi này nữa.'
      )
    )
      return

    try {
      setUnpublishing(true)
      await examService.unpublishExam(Number(examId))
      setPreviewData((prev) => (prev ? { ...prev, published: false } : prev))
      setPublishSuccess(false)
      alert('Đã chuyển đề thi về Bản Nháp thành công!')
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi thu hồi đề thi')
    } finally {
      setUnpublishing(false)
    }
  }

  // Tự động sắp xếp và đánh số lại câu hỏi (mỗi phần bắt đầu từ 1) theo chuẩn JLPT
  const [isReordering, setIsReordering] = useState<boolean>(false)
  const handleReorderQuestions = async () => {
    if (!examId) return
    if (
      !window.confirm(
        'Hệ thống sẽ tự động sắp xếp lại các câu hỏi theo đúng trình tự Mondai chuẩn JLPT và đánh số mỗi phần thi bắt đầu từ 1. Bạn có muốn thực hiện không?'
      )
    )
      return

    try {
      setIsReordering(true)
      const refreshed = await examService.reorderQuestions(Number(examId))
      setPreviewData(refreshed)
      alert('Đã sắp xếp và đánh số lại câu hỏi (mỗi phần bắt đầu từ câu 1) thành công!')
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi đánh số lại câu hỏi')
    } finally {
      setIsReordering(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-600 font-medium">Đang tải cấu trúc đề thi để biên tập...</p>
        </div>
      </div>
    )
  }

  if (error || !previewData) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-200 max-w-md w-full text-center space-y-4">
          <AlertCircle className="w-12 h-12 text-rose-500 mx-auto" />
          <h2 className="text-lg font-bold text-slate-800">Không thể tải đề thi</h2>
          <p className="text-xs text-slate-500">{error || 'Không tìm thấy dữ liệu đề thi này.'}</p>
          <button
            onClick={() => navigate('/exams')}
            className="px-5 py-2.5 bg-indigo-600 text-white font-semibold rounded-xl text-xs hover:bg-indigo-700 transition"
          >
            Quay lại danh sách đề thi
          </button>
        </div>
      </div>
    )
  }

  const isCurrentSectionListening = activeSection?.sessionType === 'LISTENING'

  // Gom nhóm câu hỏi theo Mondai trong Section (Sắp xếp theo thứ tự số câu hỏi tăng dần 1 -> N)
  const questionsList = [...(activeSection?.questions || [])].sort(
    (a, b) => (a.questionNumber ?? 0) - (b.questionNumber ?? 0)
  )

  const mondaiGroupsMap = new Map<string, { mondaiDef?: JlptMondaiDefinition; questions: AdminTakingQuestion[] }>()

  // Khởi tạo các nhóm Mondai chuẩn của session
  availableMondais.forEach((mDef) => {
    mondaiGroupsMap.set(mDef.code, { mondaiDef: mDef, questions: [] })
  })

  // Phân bổ câu hỏi vào các nhóm Mondai tương ứng
  const unassignedQuestions: AdminTakingQuestion[] = []
  questionsList.forEach((q) => {
    const qCode = q.mondaiCode ? q.mondaiCode.toUpperCase() : ''
    // Tìm mondaiDef tương ứng
    let matchedCode = ''
    for (const m of availableMondais) {
      if (m.code === qCode || qCode.includes(m.code) || (q.mondaiNameVi && q.mondaiNameVi.includes(m.nameVi))) {
        matchedCode = m.code
        break
      }
    }

    // Nếu không khớp availableMondais nhưng có định nghĩa chuẩn trong hệ thống JLPT
    if (!matchedCode && q.mondaiCode) {
      const def = getMondaiByCode(q.mondaiCode)
      if (def) {
        matchedCode = def.code
        if (!mondaiGroupsMap.has(matchedCode)) {
          mondaiGroupsMap.set(matchedCode, { mondaiDef: def, questions: [] })
        }
      }
    }

    if (matchedCode && mondaiGroupsMap.has(matchedCode)) {
      mondaiGroupsMap.get(matchedCode)!.questions.push(q)
    } else {
      unassignedQuestions.push(q)
    }
  })

  // Sắp xếp các câu hỏi trong từng Mondai theo questionNumber tăng dần
  mondaiGroupsMap.forEach((group) => {
    group.questions.sort((a, b) => (a.questionNumber ?? 0) - (b.questionNumber ?? 0))
  })
  unassignedQuestions.sort((a, b) => (a.questionNumber ?? 0) - (b.questionNumber ?? 0))

  // Thứ tự hiển thị Mondai cố định chuẩn theo quy cách đề thi JLPT: Mondai 1 -> Mondai 2 -> Mondai 3...
  const mondaiGroupEntries = Array.from(mondaiGroupsMap.entries()).sort(([, a], [, b]) => {
    return (a.mondaiDef?.mondaiNumber ?? 99) - (b.mondaiDef?.mondaiNumber ?? 99)
  })

  const activeMondaiDef = getMondaiByCode(selectedMondaiCode)

  // Audio URL hiệu lực của Section nghe (ưu tiên audioUrl của section, fallback sang câu hỏi có audio)
  const effectiveSectionAudio =
    activeSection?.audioUrl ||
    activeSection?.questions?.find((x) => x.audioUrl)?.audioUrl ||
    previewData?.sections?.find((s) => s.sessionType === 'LISTENING')?.audioUrl


  // Nạp bảng đáp án chính thức (正答表) cho toàn bộ câu hỏi hiện tại trong Section
  const handleApplyAnswerKeyToExistingQuestions = async (file: File) => {
    if (!examId || !activeSection) return
    try {
      setIsBatchApplyingAnswerKey(true)
      const aiBase = import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8000'
      const formData = new FormData()
      formData.append('file', file)

      const res = await fetch(`${aiBase}/api/v1/ai/parse-answer-key?jlpt_level=${previewData?.jlptLevel || 'N5'}`, {
        method: 'POST',
        body: formData,
      })
      if (!res.ok) {
        throw new Error('Không thể phân tích file đáp án. Vui lòng kiểm tra lại ảnh/PDF.')
      }
      const data = await res.json()
      const answerData = data.answer_data || {}
      const secMap = data.sections || answerData.sections || {}
      const flatMap = data.flat_answers || answerData.flat_answers || {}
      const matchedMap = data.matched_answers || {}

      let targetMap: Record<string, number> = {}
      if (secMap[activeSection.sessionType]) {
        targetMap = secMap[activeSection.sessionType]
      } else if (Object.keys(secMap).length === 1) {
        targetMap = Object.values(secMap)[0] as Record<string, number>
      } else {
        targetMap = flatMap
      }

      let updatedCount = 0
      for (const q of activeSection.questions) {
        const qNum = (q.questionNumber || 0).toString()
        const correctOptKey = matchedMap[qNum] || targetMap[qNum] || flatMap[qNum]
        if (correctOptKey !== undefined) {
          await examService.updateQuestionInline(Number(examId), q.id, {
            correctOptionKey: Number(correctOptKey),
          })
          updatedCount++
        }
      }

      alert(`Đã tự động cập nhật đáp án đúng thành công cho ${updatedCount} câu hỏi từ bảng 正答表!`)
      // Tải lại preview
      const refreshed = await examService.getAdminExamPreview(Number(examId))
      setPreviewData(refreshed)
    } catch (err: any) {
      alert(err.message || 'Lỗi khi nạp bảng đáp án')
    } finally {
      setIsBatchApplyingAnswerKey(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50/50 pb-20">
      {/* Header Bar */}
      <div className="sticky top-0 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800 shadow-xs px-4 sm:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/admin/dashboard')}
              className="p-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
              title="Quay lại danh sách quản lý đề thi"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <span className="px-2.5 py-0.5 rounded-lg text-xs font-black bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-400 border border-indigo-200/60 dark:border-indigo-800 uppercase">
                  {previewData.jlptLevel}
                </span>
                <span className="px-2.5 py-0.5 rounded-lg text-xs font-semibold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                  {previewData.examType === 'JLPT_OFFICIAL_MOCK'
                    ? 'Đề thi thử JLPT'
                    : 'Bài thi khóa học'}
                </span>
                {previewData.published ? (
                  <span className="px-2.5 py-0.5 rounded-lg text-xs font-bold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-400 flex items-center gap-1.5 border border-emerald-200 dark:border-emerald-800">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                    Đã xuất bản
                  </span>
                ) : (
                  <span className="px-2.5 py-0.5 rounded-lg text-xs font-bold bg-amber-50 text-amber-800 dark:bg-amber-950/50 dark:text-amber-400 flex items-center gap-1.5 border border-amber-200 dark:border-amber-800">
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                    Bản nháp (Đã lưu tự động)
                  </span>
                )}
              </div>
              <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white line-clamp-1 mt-0.5">
                {previewData.title}
              </h1>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2">
            {/* Lưu nháp & Thoát */}
            <button
              type="button"
              onClick={() => navigate('/admin/dashboard')}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-bold transition border border-slate-200 dark:border-slate-700 cursor-pointer shadow-xs"
              title="Tất cả thay đổi đã được tự động lưu. Bấm để trở về danh sách đề thi."
            >
              <Save className="w-4 h-4 text-slate-500" />
              <span className="hidden sm:inline">Lưu & Thoát</span>
            </button>

            {/* Dropdown Công cụ nâng cao */}
            <div id="tools-dropdown-container" className="relative">
              <button
                type="button"
                onClick={() => setIsToolsDropdownOpen(!isToolsDropdownOpen)}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-bold transition border border-slate-200 dark:border-slate-700 cursor-pointer shadow-xs"
              >
                <SlidersHorizontal className="w-4 h-4 text-indigo-600" />
                <span>Công cụ</span>
                <ChevronDown className={`w-3.5 h-3.5 text-slate-400 transition-transform ${isToolsDropdownOpen ? 'rotate-180' : ''}`} />
              </button>

              {isToolsDropdownOpen && (
                <div className="absolute right-0 mt-2 w-64 bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200/80 dark:border-slate-800 p-2 z-50 space-y-1 animate-in fade-in-50 zoom-in-95 duration-150">
                  <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Bản lưu & Phục hồi
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setIsToolsDropdownOpen(false)
                      handleCreateManualSnapshot()
                    }}
                    disabled={isTakingSnapshot}
                    className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition text-left cursor-pointer disabled:opacity-50"
                  >
                    <Camera className="w-4 h-4 text-indigo-600" />
                    <span>{isTakingSnapshot ? 'Đang lưu...' : 'Tạo điểm lưu (Snapshot)'}</span>
                  </button>

                  {snapshotTimestamp && (
                    <button
                      type="button"
                      onClick={() => {
                        setIsToolsDropdownOpen(false)
                        setIsRollbackModalOpen(true)
                      }}
                      className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-amber-700 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 transition text-left cursor-pointer"
                    >
                      <RotateCcw className="w-4 h-4 text-amber-600" />
                      <span>Hoàn tác ({snapshotTimestamp})</span>
                    </button>
                  )}

                  <div className="my-1 border-t border-slate-100 dark:border-slate-800" />
                  <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                    Quản lý câu hỏi & Phiên bản
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setIsToolsDropdownOpen(false)
                      handleReorderQuestions()
                    }}
                    disabled={isReordering}
                    className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition text-left cursor-pointer disabled:opacity-50"
                  >
                    <RefreshCw className={`w-4 h-4 text-violet-600 ${isReordering ? 'animate-spin' : ''}`} />
                    <span>{isReordering ? 'Đang sắp xếp...' : 'Sắp xếp số câu (1➔N)'}</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setIsToolsDropdownOpen(false)
                      handleCloneExam()
                    }}
                    disabled={isCloning}
                    className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition text-left cursor-pointer disabled:opacity-50"
                  >
                    <Copy className={`w-4 h-4 text-teal-600 ${isCloning ? 'animate-spin' : ''}`} />
                    <span>{isCloning ? 'Đang nhân bản...' : 'Nhân bản đề (Bản mới)'}</span>
                  </button>

                  {(previewData.totalAttempts ?? 0) > 0 && (
                    <button
                      type="button"
                      onClick={() => {
                        setIsToolsDropdownOpen(false)
                        handleRegradeExam()
                      }}
                      disabled={isRegrading}
                      className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-orange-700 dark:text-orange-400 hover:bg-orange-50 dark:hover:bg-orange-950/40 transition text-left cursor-pointer disabled:opacity-50"
                    >
                      <RefreshCw className={`w-4 h-4 text-orange-600 ${isRegrading ? 'animate-spin' : ''}`} />
                      <span>{isRegrading ? 'Đang chấm lại...' : `Chấm lại (${previewData.totalAttempts} bài)`}</span>
                    </button>
                  )}

                  {previewData.published && (
                    <button
                      type="button"
                      onClick={() => {
                        setIsToolsDropdownOpen(false)
                        handleUnpublishExam()
                      }}
                      disabled={unpublishing}
                      className="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition text-left cursor-pointer disabled:opacity-50"
                    >
                      <EyeOff className="w-4 h-4 text-rose-600" />
                      <span>{unpublishing ? 'Đang thu hồi...' : 'Thu hồi về Bản Nháp'}</span>
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Thêm câu hỏi mới */}
            <button
              type="button"
              onClick={() => handleOpenAddModal()}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 text-xs font-bold transition shadow-xs cursor-pointer"
            >
              <PlusCircle className="w-4 h-4 text-indigo-600" />
              <span>+ Thêm câu hỏi</span>
            </button>

            {/* Phát hành đề thi */}
            {!previewData.published ? (
              <button
                type="button"
                onClick={handlePublishExam}
                disabled={publishing || publishSuccess}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-linear-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 active:scale-95 text-white text-xs font-bold transition shadow-md shadow-emerald-600/20 cursor-pointer disabled:opacity-50"
              >
                <Share2 className="w-4 h-4" />
                <span>{publishing ? 'Đang phát hành...' : publishSuccess ? 'Đã phát hành!' : 'Phát hành đề thi'}</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={handleUnpublishExam}
                disabled={unpublishing}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 hover:bg-rose-100 text-xs font-bold transition shadow-xs cursor-pointer disabled:opacity-50"
                title="Chuyển đề thi này về bản nháp để chỉnh sửa thêm"
              >
                <EyeOff className="w-4 h-4 text-rose-600" />
                <span>{unpublishing ? 'Đang thu hồi...' : 'Thu hồi về Bản Nháp'}</span>
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-8 mt-6 space-y-6">
        {/* Banner cảnh báo an toàn phiên làm bài & xung đột trực tiếp */}
        {(previewData.inProgressAttempts ?? 0) > 0 ? (
          <div className="bg-gradient-to-r from-amber-500/10 via-rose-500/10 to-amber-500/10 border-2 border-rose-300 rounded-3xl p-5 shadow-sm backdrop-blur-sm">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-start gap-3.5">
                <div className="relative mt-0.5">
                  <span className="flex h-3.5 w-3.5 relative">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-3.5 w-3.5 bg-rose-600"></span>
                  </span>
                </div>
                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-bold text-rose-900 text-sm sm:text-base flex items-center gap-1.5">
                      <AlertTriangle className="w-4 h-4 text-rose-600" />
                      Đang có {previewData.inProgressAttempts} thí sinh làm bài thi trực tiếp!
                    </h3>
                    <span className="px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-rose-100 text-rose-800 border border-rose-200">
                      Live Concurrency Mode
                    </span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-600 mt-1 leading-relaxed">
                    <strong>Khuyến nghị an toàn:</strong> Không nên xóa hay thêm câu hỏi lúc này vì sẽ làm lệch phiên thi của học viên. Nếu cần cập nhật lớn, hãy dùng tính năng <strong>[Nhân bản đề (Bản mới)]</strong> hoặc <strong>[Thu hồi về Bản Nháp]</strong>.
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                <button
                  type="button"
                  onClick={handleCloneExam}
                  disabled={isCloning}
                  className="px-3 py-1.5 rounded-xl bg-white hover:bg-rose-50 text-rose-700 text-xs font-bold border border-rose-200 shadow-xs transition"
                >
                  Nhân bản đề mới
                </button>
              </div>
            </div>
          </div>
        ) : (previewData.totalAttempts ?? 0) > 0 ? (
          <div className="bg-blue-50/80 border border-blue-200 rounded-2xl p-4 shadow-xs">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs sm:text-sm text-blue-900">
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                <span>
                  Đề thi này đã có <strong>{previewData.totalAttempts}</strong> lượt nộp bài. Nếu bạn chỉnh sửa nội dung hoặc đáp án đúng, hãy nhấn <strong>[Chấm lại bài thi]</strong> để cập nhật điểm tự động cho các bài thi cũ.
                </span>
              </div>
              <button
                type="button"
                onClick={handleRegradeExam}
                disabled={isRegrading}
                className="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs transition shrink-0 self-end sm:self-center"
              >
                Chấm lại {previewData.totalAttempts} bài thi
              </button>
            </div>
          </div>
        ) : null}

        {/* Navigation Tabs cho từng Section trong đề thi */}
        <div className="bg-slate-200/60 dark:bg-slate-800/60 p-1.5 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 grid grid-cols-1 sm:grid-cols-3 gap-2">
          {previewData.sections.map((sec: AdminTakingSection, idx: number) => {
            const isActive = idx === activeSectionIdx
            const cleanTitle = sec.sectionTitle.replace(/\s*\(\d+\s*phút\)/i, '').trim()
            const duration = sec.durationMinutes || (sec.sessionType === 'LISTENING' ? 35 : 30)

            return (
              <button
                key={sec.id}
                type="button"
                onClick={() => setActiveSectionIdx(idx)}
                className={`relative flex items-center justify-between px-4 py-3 rounded-xl font-bold text-xs sm:text-sm transition-all duration-150 cursor-pointer ${
                  isActive
                    ? 'bg-white dark:bg-slate-900 text-indigo-700 dark:text-indigo-400 shadow-sm border border-slate-200/80 dark:border-slate-700'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-white/60 dark:hover:bg-slate-800/60'
                }`}
              >
                <div className="flex items-center gap-3 truncate">
                  <div
                    className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 transition ${
                      isActive
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
                    }`}
                  >
                    {sec.sessionType === 'LISTENING' ? (
                      <Headphones className="w-4 h-4" />
                    ) : sec.sessionType === 'GRAMMAR_READING' ? (
                      <BookOpen className="w-4 h-4" />
                    ) : (
                      <FileText className="w-4 h-4" />
                    )}
                  </div>
                  <div className="text-left truncate">
                    <p className="truncate leading-tight">{cleanTitle}</p>
                    <p className={`text-[11px] font-normal mt-0.5 ${isActive ? 'text-indigo-500/80 dark:text-indigo-400/80' : 'text-slate-400'}`}>
                      {duration} phút
                    </p>
                  </div>
                </div>

                <span
                  className={`text-xs font-mono font-bold px-2.5 py-0.5 rounded-full shrink-0 ml-2 ${
                    isActive
                      ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200/50 dark:border-indigo-800/50'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {sec.questions.length} câu
                </span>
              </button>
            )
          })}
        </div>

        {/* Thanh công cụ quản lý Section hiện tại */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 shadow-sm border border-slate-200/80 dark:border-slate-800 space-y-5">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-[11px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-md bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-400 border border-indigo-200/60 dark:border-indigo-800">
                  {activeSection?.sessionType}
                </span>
                <h2 className="text-lg font-black text-slate-900 dark:text-white">
                  {activeSection?.sectionTitle.replace(/\s*\(\d+\s*phút\)/i, '').trim()}
                </h2>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Thời lượng phần này: <strong className="text-slate-700 dark:text-slate-300">{activeSection?.durationMinutes || 30} phút</strong> • Tổng số: <strong className="text-slate-700 dark:text-slate-300">{activeSection?.questions.length} câu hỏi</strong> • Gồm <strong className="text-slate-700 dark:text-slate-300">{availableMondais.length} Mondai chuẩn JLPT</strong>
              </p>
            </div>

            {/* Nút công cụ thêm câu & quét AI cho riêng Section này */}
            <div className="flex items-center gap-2 flex-wrap">
              <label
                className={`px-3.5 py-2 rounded-xl border border-emerald-300 dark:border-emerald-700 hover:bg-emerald-50 dark:hover:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 text-xs font-bold transition flex items-center gap-1.5 shadow-xs cursor-pointer ${
                  isBatchApplyingAnswerKey ? 'opacity-50 pointer-events-none' : ''
                }`}
                title="Tải ảnh hoặc PDF bảng đáp án chuẩn (正答表) để tự động khớp đáp án cho các câu hỏi hiện tại"
              >
                <FileCheck className="w-4 h-4 text-emerald-600" />
                <span>{isBatchApplyingAnswerKey ? 'Đang khớp đáp án...' : '📥 Nạp đáp án từ ảnh/PDF 正答表'}</span>
                <input
                  type="file"
                  accept=".pdf,image/*"
                  disabled={isBatchApplyingAnswerKey}
                  onChange={(e) => {
                    const f = e.target.files?.[0]
                    if (f) handleApplyAnswerKeyToExistingQuestions(f)
                    e.target.value = ''
                  }}
                  className="hidden"
                />
              </label>

              <button
                type="button"
                onClick={() => handleOpenAddModal()}
                className="px-3.5 py-2 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-bold transition flex items-center gap-1.5 cursor-pointer shadow-xs"
              >
                <PlusCircle className="w-4 h-4 text-indigo-600" />
                <span>+ Thêm câu hỏi</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setAiSectionError(null)
                  setAiExtractedQuestions([])
                  setAiSaveMode((activeSection?.questions?.length || 0) > 0 ? 'OVERWRITE' : 'APPEND')
                  setIsAiSectionModalOpen(true)
                }}
                className="px-4 py-2 rounded-xl bg-linear-to-r from-amber-500 via-orange-500 to-indigo-600 hover:from-amber-600 hover:to-indigo-700 text-white text-xs font-bold transition shadow-sm shadow-orange-500/10 flex items-center gap-1.5 cursor-pointer active:scale-95"
              >
                <Sparkles className="w-4 h-4 text-amber-200" />
                <span>🤖 Quét & Thêm phần này bằng AI</span>
              </button>
            </div>
          </div>

          {/* Dành riêng cho phần Nghe hiểu: Cập nhật Audio tổng ca nghe */}
          {isCurrentSectionListening && (
            <div className="pt-4 border-t border-slate-100 dark:border-slate-800 space-y-2.5">
              <div className="flex items-center justify-between flex-wrap gap-2">
                <label className="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                  <Volume2 className="w-4 h-4 text-indigo-600" />
                  <span>Audio tổng hợp của toàn bộ phần nghe:</span>
                </label>
                <FileUploadButton
                  accept="audio"
                  folder="jlpt_exam/audios"
                  buttonText="Tải file MP3 tổng từ máy"
                  onUploadSuccess={(url: string) => setSectionAudioUrlInput(url)}
                />
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  value={sectionAudioUrlInput}
                  onChange={(e) => setSectionAudioUrlInput(e.target.value)}
                  placeholder="https://.../ca_nghe_tong_hop.mp3 hoặc bấm tải file từ máy tính"
                  className="flex-1 px-3.5 py-2.5 text-xs border border-slate-300 dark:border-slate-700 rounded-xl bg-slate-50 dark:bg-slate-800/50 text-slate-900 dark:text-white focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 font-mono transition"
                />
                <button
                  type="button"
                  onClick={handleSaveSectionAudio}
                  disabled={isUpdatingAudio || !sectionAudioUrlInput.trim()}
                  className="px-4 py-2 bg-slate-900 hover:bg-black dark:bg-indigo-600 dark:hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition cursor-pointer disabled:opacity-40 shrink-0"
                >
                  {isUpdatingAudio ? 'Đang lưu...' : 'Lưu Audio tổng'}
                </button>
              </div>

              {activeSection?.audioUrl && (
                <div className="mt-2.5">
                  <QuestionAudioPlayer
                    audioUrl={activeSection.audioUrl}
                    title="Audio tổng hợp toàn bộ phần nghe"
                  />
                </div>
              )}
            </div>
          )}
        </div>

        {/* ========================================================= */}
        {/* DANH SÁCH CÂU HỎI THEO TỪNG MONDAI (CHUẨN HÓA JLPT ĐÍCH THỰC) */}
        {/* ========================================================= */}
        {activeSection?.questions.length === 0 ? (
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-8 sm:p-12 shadow-sm space-y-8">
            <div className="text-center max-w-md mx-auto space-y-2">
              <div className="w-14 h-14 rounded-2xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mx-auto shadow-sm">
                <Sparkles className="w-7 h-7" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                Phần này chưa có câu hỏi nào
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                Chọn cách bạn muốn soạn thảo: Sử dụng AI để tự động trích xuất từ đề thi có sẵn, hoặc tự tay soạn từng câu hỏi chuẩn theo format JLPT.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-4xl mx-auto">
              {/* Card 1: AI Trích xuất */}
              <button
                type="button"
                onClick={() => {
                  setAiSectionError(null)
                  setAiExtractedQuestions([])
                  setAiSaveMode('OVERWRITE')
                  setIsAiSectionModalOpen(true)
                }}
                className="group relative p-6 rounded-2xl border-2 border-indigo-200/80 dark:border-indigo-800/80 hover:border-indigo-500 bg-linear-to-b from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 text-left transition-all duration-200 hover:shadow-lg hover:shadow-indigo-500/10 hover:-translate-y-0.5 cursor-pointer flex flex-col justify-between space-y-4"
              >
                <div className="space-y-3">
                  <div className="w-10 h-10 rounded-xl bg-linear-to-tr from-amber-500 to-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-500/20">
                    <Sparkles className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h4 className="text-sm font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 transition">
                        Quét & Trích xuất bằng AI
                      </h4>
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-black uppercase bg-amber-100 text-amber-800">
                        Khuyên dùng
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                      Tải lên file PDF hoặc Audio, AI sẽ tự động đọc đề, cắt tranh và điền câu hỏi trong vài giây.
                    </p>
                  </div>
                </div>
                <div className="text-xs font-bold text-indigo-600 dark:text-indigo-400 flex items-center gap-1 group-hover:gap-2 transition-all">
                  <span>Mở công cụ quét AI</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              </button>

              {/* Card 2: Soạn thủ công */}
              <button
                type="button"
                onClick={() => handleOpenAddModal()}
                className="group p-6 rounded-2xl border border-slate-200 dark:border-slate-800 hover:border-slate-400 bg-white dark:bg-slate-900 text-left transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 cursor-pointer flex flex-col justify-between space-y-4"
              >
                <div className="space-y-3">
                  <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-300">
                    <PlusCircle className="w-5 h-5 text-indigo-600" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 transition">
                      Soạn câu hỏi thủ công
                    </h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                      Tự chọn dạng Mondai (Từ vựng, Ngữ pháp, Đọc hiểu, Nghe hiểu) và nhập câu hỏi, hình ảnh, đáp án đúng.
                    </p>
                  </div>
                </div>
                <div className="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1 group-hover:gap-2 transition-all">
                  <span>+ Thêm câu hỏi đầu tiên</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              </button>

              {/* Card 3: Nạp bảng đáp án */}
              <label
                className="group p-6 rounded-2xl border border-slate-200 dark:border-slate-800 hover:border-emerald-500 bg-white dark:bg-slate-900 text-left transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 cursor-pointer flex flex-col justify-between space-y-4"
              >
                <div className="space-y-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 flex items-center justify-center text-emerald-600">
                    <FileCheck className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-900 dark:text-white group-hover:text-emerald-600 transition">
                      Nạp bảng đáp án 正答表
                    </h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                      Tải file ảnh hoặc PDF bảng đáp án chính thức để AI tự động khớp đáp án đúng 100% khi có câu hỏi.
                    </p>
                  </div>
                </div>
                <div className="text-xs font-bold text-emerald-600 dark:text-emerald-400 flex items-center gap-1 group-hover:gap-2 transition-all">
                  <span>{isBatchApplyingAnswerKey ? 'Đang khớp...' : 'Tải file đáp án lên'}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
                <input
                  type="file"
                  accept=".pdf,image/*"
                  disabled={isBatchApplyingAnswerKey}
                  onChange={(e) => {
                    const f = e.target.files?.[0]
                    if (f) handleApplyAnswerKeyToExistingQuestions(f)
                    e.target.value = ''
                  }}
                  className="hidden"
                />
              </label>
            </div>
          </div>
        ) : (
          <div className="space-y-8">
            {mondaiGroupEntries.map(([mCode, group]) => {
              const mondaiDef = group.mondaiDef
              const qCount = group.questions.length

              return (
                <div
                  key={mCode}
                  className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden"
                >
                  {/* Header của từng Mondai */}
                  <div className="bg-slate-100/80 px-6 py-4 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="px-2.5 py-0.5 rounded-lg bg-indigo-600 text-white font-black text-xs">
                          {mondaiDef?.nameVi?.split(':')[0] || mCode}
                        </span>
                        <h3 className="text-sm font-extrabold text-slate-900">
                          {mondaiDef?.nameVi?.split(':')?.[1] || mondaiDef?.nameVi || mCode}
                        </h3>
                        <span className="text-[11px] font-mono px-2 py-0.5 bg-slate-200 text-slate-700 rounded-full font-bold">
                          {qCount} câu
                        </span>
                      </div>
                      {mondaiDef?.instructionJp && (
                        <p className="text-xs font-serif text-slate-600 pt-1">
                          {mondaiDef.instructionJp}
                        </p>
                      )}
                      {mondaiDef?.instructionVi && (
                        <p className="text-[11px] text-slate-400 italic">
                          ({mondaiDef.instructionVi})
                        </p>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => handleOpenAddModal(mCode)}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-white border border-slate-300 text-slate-700 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200 text-xs font-bold transition shadow-2xs cursor-pointer"
                    >
                      <PlusCircle className="w-3.5 h-3.5 text-indigo-600" />
                      <span>+ Thêm câu vào Mondai này</span>
                    </button>
                  </div>

                  {/* Danh sách câu hỏi trong Mondai này */}
                  <div className="p-6 space-y-5">
                    {qCount === 0 ? (
                      <div className="py-6 text-center text-slate-400 text-xs border border-dashed border-slate-200 rounded-2xl">
                        Mondai này chưa có câu hỏi nào.{' '}
                        <button
                          type="button"
                          onClick={() => handleOpenAddModal(mCode)}
                          className="text-indigo-600 font-bold hover:underline"
                        >
                          Bấm để thêm ngay
                        </button>
                      </div>
                    ) : (
                      (() => {
                        // Nhóm các câu hỏi dùng chung passage (1 đoạn văn nhiều câu hỏi con)
                        type ClusterItem =
                          | { type: 'PASSAGE'; passage: TakingPassage; questions: AdminTakingQuestion[] }
                          | { type: 'SINGLE'; question: AdminTakingQuestion }

                        const clusters: ClusterItem[] = []
                        let curPassageGroup: { type: 'PASSAGE'; passage: TakingPassage; questions: AdminTakingQuestion[] } | null = null

                        group.questions.forEach((q) => {
                          const isPassageMondai = ['TEXT_GRAMMAR', 'SHORT_PASSAGE', 'MEDIUM_PASSAGE', 'LONG_PASSAGE', 'INFORMATION_RETRIEVAL'].includes(
                            (q.mondaiCode || '').toUpperCase()
                          )
                          const hasPassageData = isPassageMondai && q.passage && (q.passage.contentJp || q.passage.imageUrl)
                          if (hasPassageData && q.passage) {
                            const isSamePassage = curPassageGroup && (
                              (curPassageGroup.passage.id && q.passage.id && curPassageGroup.passage.id === q.passage.id) ||
                              (Boolean(q.passage.contentJp) && curPassageGroup.passage.contentJp?.trim() === q.passage.contentJp?.trim()) ||
                              (Boolean(q.passage.imageUrl) && curPassageGroup.passage.imageUrl?.trim() === q.passage.imageUrl?.trim())
                            )
                            if (isSamePassage && curPassageGroup) {
                              curPassageGroup.questions.push(q)
                            } else {
                              curPassageGroup = {
                                type: 'PASSAGE',
                                passage: q.passage,
                                questions: [q],
                              }
                              clusters.push(curPassageGroup)
                            }
                          } else {
                            curPassageGroup = null
                            clusters.push({ type: 'SINGLE', question: q })
                          }
                        })

                        return (
                          <div className="space-y-6">
                            {clusters.map((item, cIdx) => {
                              if (item.type === 'PASSAGE') {
                                const p = item.passage
                                const subQs = item.questions
                                return (
                                  <div
                                    key={`passage-${p.id || cIdx}`}
                                    className="bg-sky-50/50 rounded-3xl border-2 border-sky-200/90 p-5 space-y-4"
                                  >
                                    {/* Header Bài đọc dùng chung */}
                                    <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-sky-200">
                                      <div className="flex items-center gap-2.5">
                                        <div className="p-2 rounded-xl bg-sky-600 text-white shadow-2xs">
                                          <BookOpen className="w-4 h-4" />
                                        </div>
                                        <div>
                                          <div className="flex items-center gap-2">
                                            <span className="text-xs font-black text-sky-950 uppercase tracking-wide">
                                              Bài đọc hiểu / Đục lỗ dùng chung
                                            </span>
                                            <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-sky-100 text-sky-800 border border-sky-300">
                                              {subQs.length} câu hỏi con (Câu {subQs[0].questionNumber}{subQs.length > 1 ? ` ~ Câu ${subQs[subQs.length - 1].questionNumber}` : ''})
                                            </span>
                                          </div>
                                          <h4 className="text-sm font-bold text-slate-800">
                                            {p.title || 'Đoạn văn đọc hiểu'}
                                          </h4>
                                        </div>
                                      </div>

                                      <div className="flex items-center gap-2">
                                        {/* Nút sửa bài đọc trực tiếp */}
                                        <button
                                          type="button"
                                          onClick={() => handleStartEditPassage(p)}
                                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-indigo-300 bg-white hover:bg-indigo-50 text-indigo-700 text-xs font-bold transition shadow-2xs cursor-pointer"
                                          title="Chỉnh sửa nội dung, tiêu đề hoặc ảnh tờ rơi của bài đọc này"
                                        >
                                          <Edit3 className="w-3.5 h-3.5 text-indigo-600" />
                                          <span>Sửa bài đọc</span>
                                        </button>

                                        {/* Nút thêm câu hỏi con cho bài đọc này */}
                                        <button
                                          type="button"
                                          onClick={() => handleOpenAddModal(mCode, p)}
                                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sky-600 hover:bg-sky-700 text-white text-xs font-bold transition shadow-xs cursor-pointer"
                                          title="Thêm câu hỏi con tiếp theo sử dụng chung đoạn văn này"
                                        >
                                          <PlusCircle className="w-3.5 h-3.5" />
                                          <span>+ Thêm câu hỏi</span>
                                        </button>

                                        {/* Nút xóa toàn bộ bài đọc và câu hỏi liên quan */}
                                        {p.id && (
                                          <button
                                            type="button"
                                            onClick={() => handleDeletePassage(p.id!, subQs)}
                                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-rose-200 bg-white hover:bg-rose-50 text-rose-600 text-xs font-bold transition shadow-2xs cursor-pointer"
                                            title="Xóa bài đọc này và tất cả các câu hỏi liên quan khỏi đề thi"
                                          >
                                            <Trash2 className="w-3.5 h-3.5" />
                                            <span>Xóa bài đọc</span>
                                          </button>
                                        )}
                                      </div>
                                    </div>

                                    {/* Khối Nội dung / Khối Chỉnh sửa bài đọc */}
                                    {editingPassageId === p.id ? (
                                      <div className="bg-white rounded-2xl p-5 border-2 border-indigo-400 shadow-md space-y-4">
                                        <div className="flex items-center justify-between border-b pb-2.5">
                                          <span className="text-xs font-extrabold text-indigo-900 uppercase flex items-center gap-1.5">
                                            <Edit3 className="w-4 h-4 text-indigo-600" />
                                            Chỉnh sửa bài đọc dùng chung
                                          </span>
                                          <div className="flex items-center gap-2">
                                            <button
                                              type="button"
                                              onClick={() => setEditingPassageId(null)}
                                              className="px-3 py-1 text-xs border border-slate-300 rounded-lg text-slate-600 hover:bg-slate-50 cursor-pointer font-medium"
                                            >
                                              Hủy
                                            </button>
                                            <button
                                              type="button"
                                              disabled={savingPassage}
                                              onClick={() => p.id && handleSavePassage(p.id)}
                                              className="px-4 py-1 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg shadow-sm cursor-pointer disabled:opacity-50"
                                            >
                                              {savingPassage ? 'Đang lưu...' : 'Lưu bài đọc'}
                                            </button>
                                          </div>
                                        </div>

                                        <div>
                                          <label className="block text-xs font-bold text-slate-700 mb-1">
                                            Tiêu đề bài đọc:
                                          </label>
                                          <input
                                            type="text"
                                            value={passageEditForm.title}
                                            onChange={(e) => setPassageEditForm({ ...passageEditForm, title: e.target.value })}
                                            placeholder="Ví dụ: スーパーあさひ屋 ポイントカードのご案内"
                                            className="w-full p-2 text-xs border border-slate-300 rounded-xl bg-white font-medium"
                                          />
                                        </div>

                                        <div>
                                          <label className="block text-xs font-bold text-slate-700 mb-1">
                                            Nội dung văn bản tiếng Nhật / Bảng biểu HTML:
                                          </label>
                                          <textarea
                                            rows={6}
                                            value={passageEditForm.contentJp}
                                            onChange={(e) => setPassageEditForm({ ...passageEditForm, contentJp: e.target.value })}
                                            placeholder="Nội dung bài đọc tiếng Nhật..."
                                            className="w-full p-2 text-xs border border-slate-300 rounded-xl font-serif bg-white leading-relaxed"
                                          />
                                        </div>

                                        <div>
                                          <label className="block text-xs font-bold text-slate-700 mb-1">
                                            Bản dịch tiếng Việt tham khảo (tùy chọn):
                                          </label>
                                          <textarea
                                            rows={2}
                                            value={passageEditForm.translationVi}
                                            onChange={(e) => setPassageEditForm({ ...passageEditForm, translationVi: e.target.value })}
                                            placeholder="Bản dịch nghĩa tham khảo..."
                                            className="w-full p-2 text-xs border border-slate-300 rounded-xl bg-white"
                                          />
                                        </div>
                                      </div>
                                    ) : (
                                      /* Nội dung bài đọc (Hiển thị xem trước) */
                                      <div className="bg-white rounded-2xl p-4 border border-sky-100 shadow-2xs space-y-2.5">
                                        {p.contentJp && (
                                          <div
                                            className="text-xs md:text-sm text-slate-800 dark:text-slate-200 leading-relaxed select-text"
                                            dangerouslySetInnerHTML={{ __html: formatJapanesePassageHtml(p.contentJp) }}
                                          />
                                        )}
                                        {p.translationVi && (
                                          <p className="text-[11px] text-slate-500 italic pt-2 border-t border-slate-100">
                                            Bản dịch: {p.translationVi}
                                          </p>
                                        )}
                                      </div>
                                    )}

                                    {/* Danh sách các câu hỏi con thuộc bài đọc này */}
                                    <div className="space-y-3 pt-1">
                                      <span className="text-xs font-extrabold text-sky-950 block">
                                        Các câu hỏi của bài đọc này ({subQs.length} câu):
                                      </span>
                                      {subQs.map((subQ) => (
                                        <QuestionItemComponent
                                          key={subQ.id}
                                          q={subQ}
                                          mondaiDef={mondaiDef}
                                          hidePassageContent={true}
                                          isCurrentSectionListening={isCurrentSectionListening}
                                          activeSectionAudioUrl={effectiveSectionAudio}
                                          editingQuestionId={editingQuestionId}
                                          setEditingQuestionId={setEditingQuestionId}
                                          editForm={editForm}
                                          setEditForm={setEditForm}
                                          savingQuestion={savingQuestion}
                                          handleStartEdit={handleStartEdit}
                                          handleSaveQuestion={handleSaveQuestion}
                                          handleDeleteQuestion={handleDeleteQuestion}
                                          handleStartEditPassage={handleStartEditPassage}
                                          hasInProgress={Boolean((previewData?.inProgressAttempts ?? 0) > 0)}
                                          hasSubmissions={Boolean((previewData?.totalAttempts ?? 0) > 0)}
                                          onZoomImage={(url) => setZoomImageUrl(url)}
                                        />
                                      ))}
                                    </div>
                                  </div>
                                )
                              }

                              return (
                                <QuestionItemComponent
                                  key={item.question.id}
                                  q={item.question}
                                  mondaiDef={mondaiDef}
                                  isCurrentSectionListening={isCurrentSectionListening}
                                  activeSectionAudioUrl={effectiveSectionAudio}
                                  editingQuestionId={editingQuestionId}
                                  setEditingQuestionId={setEditingQuestionId}
                                  editForm={editForm}
                                  setEditForm={setEditForm}
                                  savingQuestion={savingQuestion}
                                  handleStartEdit={handleStartEdit}
                                  handleSaveQuestion={handleSaveQuestion}
                                  handleDeleteQuestion={handleDeleteQuestion}
                                  handleStartEditPassage={handleStartEditPassage}
                                  hasInProgress={Boolean((previewData?.inProgressAttempts ?? 0) > 0)}
                                  hasSubmissions={Boolean((previewData?.totalAttempts ?? 0) > 0)}
                                  onZoomImage={(url) => setZoomImageUrl(url)}
                                />
                              )
                            })}
                          </div>
                        )
                      })()
                    )}
                  </div>
                </div>
              )
            })}

            {/* Các câu hỏi chưa gán Mondai chuẩn */}
            {unassignedQuestions.length > 0 && (
              <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
                <div className="bg-slate-100/70 px-6 py-3 border-b border-slate-200 font-bold text-xs text-slate-600">
                  Câu hỏi độc lập khác ({unassignedQuestions.length} câu)
                </div>
                <div className="p-6 space-y-4">
                  {unassignedQuestions.map((q) => (
                    <QuestionItemComponent
                      key={q.id}
                      q={q}
                      isCurrentSectionListening={isCurrentSectionListening}
                      activeSectionAudioUrl={effectiveSectionAudio}
                      editingQuestionId={editingQuestionId}
                      setEditingQuestionId={setEditingQuestionId}
                      editForm={editForm}
                      setEditForm={setEditForm}
                      savingQuestion={savingQuestion}
                      handleStartEdit={handleStartEdit}
                      handleSaveQuestion={handleSaveQuestion}
                      handleDeleteQuestion={handleDeleteQuestion}
                      handleStartEditPassage={handleStartEditPassage}
                      hasInProgress={Boolean((previewData?.inProgressAttempts ?? 0) > 0)}
                      hasSubmissions={Boolean((previewData?.totalAttempts ?? 0) > 0)}
                      onZoomImage={(url) => setZoomImageUrl(url)}
                    />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* ========================================================= */}
      {/* MODAL THÊM CÂU HỎI MỚI (PHÂN HÓA HOÀN TOÀN THEO TỪNG MONDAI) */}
      {/* ========================================================= */}
      {isAddModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-3xl w-full p-6 shadow-2xl space-y-4 my-8 border border-slate-200 max-h-[92vh] overflow-y-auto">
            {/* Header Modal */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2 text-indigo-700">
                <PlusCircle className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-extrabold text-slate-900">
                  Thêm câu hỏi mới vào phần thi: {activeSection?.sectionTitle}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsAddModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 text-xs">
              {/* BƯỚC 1: CHỌN MONDAI SẼ THÊM CÂU HỎI VÀO */}
              <div className="p-3.5 bg-indigo-50/70 border border-indigo-200 rounded-2xl space-y-2">
                <label className="block text-xs font-bold text-indigo-950 flex items-center gap-1.5">
                  <Layers className="w-4 h-4 text-indigo-600" />
                  <span>Chọn Mondai (Bài thi thành phần): *</span>
                </label>
                <select
                  value={selectedMondaiCode}
                  onChange={(e) => handleMondaiChangeInModal(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-indigo-200 bg-white font-bold text-xs text-indigo-900 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                >
                  {availableMondais.map((mDef) => (
                    <option key={mDef.code} value={mDef.code}>
                      {mDef.nameVi}
                    </option>
                  ))}
                </select>
                {activeMondaiDef?.instructionJp && (
                  <p className="text-[11px] font-serif text-slate-600 italic">
                    Chỉ dẫn: {activeMondaiDef.instructionJp}
                  </p>
                )}
              </div>

              {/* BƯỚC 2: FORM CHUYÊN BIỆT HÓA CHUẨN JLPT CHO TỪNG LOẠI MONDAI */}
              {activeMondaiDef?.questionType === 'STAR' ? (
                <StarOrderingQuestionForm
                  data={{
                    questionText: newQuestionForm.questionText,
                    options: newQuestionForm.options.map((o) => ({
                      optionKey: o.optionKey,
                      optionText: o.optionText,
                      isCorrect: o.isCorrect,
                    })),
                    starPosition: newQuestionForm.starPosition || 3,
                    explanationVi: newQuestionForm.explanationVi,
                    points: newQuestionForm.points,
                  }}
                  onChange={(updated) => {
                    setNewQuestionForm((prev) => ({
                      ...prev,
                      questionText: updated.questionText,
                      starPosition: updated.starPosition,
                      explanationVi: updated.explanationVi,
                      options: updated.options.map((o) => ({
                        optionKey: o.optionKey,
                        optionText: o.optionText,
                        optionImageUrl: '',
                        isCorrect: o.isCorrect,
                      })),
                    }))
                  }}
                />
              ) : activeMondaiDef?.hasPassage || activeMondaiDef?.questionType === 'READING_PASSAGE' || activeMondaiDef?.questionType === 'CLOZE_TEXT' ? (
                <PassageQuestionGroupForm
                  data={{
                    questionText: newQuestionForm.questionText,
                    passageTitle: newQuestionForm.passageTitle,
                    passageContentJp: newQuestionForm.passageContentJp,
                    passageTranslationVi: newQuestionForm.passageTranslationVi,
                    passageImageUrl: newQuestionForm.passageImageUrl,
                    options: newQuestionForm.options.map((o) => ({
                      optionKey: o.optionKey,
                      optionText: o.optionText,
                      isCorrect: o.isCorrect,
                    })),
                    explanationVi: newQuestionForm.explanationVi,
                    points: newQuestionForm.points,
                  }}
                  existingPassages={existingPassagesInActiveSection}
                  isClozeText={activeMondaiDef?.questionType === 'CLOZE_TEXT'}
                  onChange={(updated) => {
                    setNewQuestionForm((prev) => ({
                      ...prev,
                      questionText: updated.questionText,
                      passageTitle: updated.passageTitle,
                      passageContentJp: updated.passageContentJp,
                      passageTranslationVi: updated.passageTranslationVi,
                      passageImageUrl: updated.passageImageUrl,
                      explanationVi: updated.explanationVi,
                      options: updated.options.map((o) => ({
                        optionKey: o.optionKey,
                        optionText: o.optionText,
                        optionImageUrl: '',
                        isCorrect: o.isCorrect,
                      })),
                    }))
                  }}
                />
              ) : isCurrentSectionListening || activeMondaiDef?.questionType === 'LISTENING_TASK' || activeMondaiDef?.questionType === 'LISTENING_STANDARD' ? (
                <ListeningQuestionForm
                  data={{
                    questionText: newQuestionForm.questionText,
                    audioUrl: newQuestionForm.audioUrl,
                    audioStartTime: newQuestionForm.audioStartTime,
                    audioEndTime: newQuestionForm.audioEndTime,
                    imageUrl: newQuestionForm.imageUrl,
                    hasVisualOptions: newQuestionForm.hasVisualOptions,
                    transcriptJp: newQuestionForm.transcriptJp,
                    transcriptVi: newQuestionForm.transcriptVi,
                    explanationVi: newQuestionForm.explanationVi,
                    options: newQuestionForm.options.map((o) => ({
                      optionKey: o.optionKey,
                      optionText: o.optionText,
                      optionImageUrl: o.optionImageUrl,
                      isCorrect: o.isCorrect,
                    })),
                  }}
                  activeSectionAudioUrl={effectiveSectionAudio}
                  defaultOptionCount={activeMondaiDef?.defaultOptionsCount || 4}
                  supportsVisuals={activeMondaiDef?.supportsVisuals}
                  onChange={(updated) => {
                    setNewQuestionForm((prev) => ({
                      ...prev,
                      questionText: updated.questionText,
                      audioUrl: updated.audioUrl,
                      audioStartTime: updated.audioStartTime,
                      audioEndTime: updated.audioEndTime,
                      imageUrl: updated.imageUrl,
                      hasVisualOptions: updated.hasVisualOptions,
                      transcriptJp: updated.transcriptJp,
                      transcriptVi: updated.transcriptVi,
                      explanationVi: updated.explanationVi,
                      options: updated.options.map((o) => ({
                        optionKey: o.optionKey,
                        optionText: o.optionText,
                        optionImageUrl: o.optionImageUrl,
                        isCorrect: o.isCorrect,
                      })),
                    }))
                  }}
                />
              ) : (
                <VocabGrammarStandardForm
                  data={{
                    questionText: newQuestionForm.questionText,
                    options: newQuestionForm.options.map((o) => ({
                      optionKey: o.optionKey,
                      optionText: o.optionText,
                      isCorrect: o.isCorrect,
                    })),
                    explanationVi: newQuestionForm.explanationVi,
                    points: newQuestionForm.points,
                  }}
                  mondaiHint={activeMondaiDef?.instructionVi}
                  onChange={(updated) => {
                    setNewQuestionForm((prev) => ({
                      ...prev,
                      questionText: updated.questionText,
                      explanationVi: updated.explanationVi,
                      options: updated.options.map((o) => ({
                        optionKey: o.optionKey,
                        optionText: o.optionText,
                        optionImageUrl: '',
                        isCorrect: o.isCorrect,
                      })),
                    }))
                  }}
                />
              )}
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsAddModalOpen(false)}
                className="px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleAddQuestionSubmit}
                disabled={addingQuestion}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-sm cursor-pointer disabled:opacity-50"
              >
                {addingQuestion ? 'Đang thêm...' : 'Lưu câu hỏi vào Mondai'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL TRÍCH XUẤT AI DÀNH RIÊNG CHO PHẦN THI NÀY */}
      {/* ========================================================= */}
      {isAiSectionModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl space-y-4 my-8 border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2 text-indigo-700">
                <Sparkles className="w-5 h-5 text-amber-500" />
                <h3 className="text-base font-extrabold text-slate-900">
                  Trích xuất AI cho phần: {activeSection?.sectionTitle}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setIsAiSectionModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {aiSectionError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{aiSectionError}</span>
              </div>
            )}

            <div className="space-y-4 text-xs">
              <p className="text-slate-600">
                Hệ thống AI sẽ chỉ phân tích dữ liệu cho riêng phần này (
                <strong>{isCurrentSectionListening ? 'Nghe hiểu' : 'Kiến thức / Đọc hiểu'}</strong>
                ), tự động gom vào từng Mondai chuẩn JLPT.
              </p>

              {/* Nếu là Nghe hiểu: Cho phép Upload cả Audio MP3 và file PDF đề thi nghe */}
              {isCurrentSectionListening ? (
                <div className="space-y-3">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {/* Audio MP3 */}
                    <div className="border-2 border-dashed border-indigo-200 rounded-2xl p-4 text-center space-y-2 bg-indigo-50/30">
                      <Headphones className="w-8 h-8 text-indigo-600 mx-auto" />
                      <p className="font-bold text-slate-800 text-xs">1. File Audio bài nghe (.mp3)</p>
                      <div>
                        <label className="inline-block px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-xs cursor-pointer transition">
                          <span>{aiAudioFile ? 'Đổi file Audio' : 'Chọn file MP3 ca nghe'}</span>
                          <input
                            type="file"
                            accept="audio/*,.mp3,.wav,.m4a"
                            onChange={(e) => setAiAudioFile(e.target.files?.[0] || null)}
                            className="hidden"
                          />
                        </label>
                        {aiAudioFile && (
                          <div className="mt-2 flex items-center justify-between p-1.5 bg-white rounded-xl border border-indigo-200 text-left">
                            <span className="text-[11px] font-bold text-indigo-700 truncate">{aiAudioFile.name}</span>
                            <button
                              type="button"
                              onClick={() => setAiAudioFile(null)}
                              className="text-slate-400 hover:text-rose-600 ml-1 cursor-pointer"
                            >
                              <X className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        )}
                      </div>
                      <p className="text-[10px] text-slate-400">
                        Cắt mốc câu và trích xuất kịch bản audio
                      </p>
                    </div>

                    {/* PDF đề nghe */}
                    <div className="border-2 border-dashed border-sky-200 rounded-2xl p-4 text-center space-y-2 bg-sky-50/30">
                      <FileText className="w-8 h-8 text-sky-600 mx-auto" />
                      <p className="font-bold text-slate-800 text-xs">2. File PDF đề nghe (Tranh & Lựa chọn)</p>
                      <div>
                        <label className="inline-block px-3 py-1.5 bg-sky-600 hover:bg-sky-700 text-white font-bold rounded-xl text-xs cursor-pointer transition">
                          <span>+ Chọn file PDF đề nghe</span>
                          <input
                            type="file"
                            multiple
                            accept=".pdf"
                            onChange={(e) => {
                              if (e.target.files && e.target.files.length > 0) {
                                const newFiles = Array.from(e.target.files)
                                setAiPdfFiles((prev) => {
                                  const existingNames = new Set(prev.map((f) => f.name))
                                  return [...prev, ...newFiles.filter((f) => !existingNames.has(f.name))]
                                })
                              }
                              e.target.value = ''
                            }}
                            className="hidden"
                          />
                        </label>

                        {aiPdfFiles.length > 0 && (
                          <div className="mt-2 space-y-1 max-h-20 overflow-y-auto">
                            {aiPdfFiles.map((file, idx) => (
                              <div
                                key={idx}
                                className="flex items-center justify-between p-1.5 bg-white rounded-lg border border-sky-200 text-left text-[11px]"
                              >
                                <span className="font-semibold text-slate-800 truncate">{file.name}</span>
                                <button
                                  type="button"
                                  onClick={() => setAiPdfFiles((prev) => prev.filter((_, i) => i !== idx))}
                                  className="text-slate-400 hover:text-rose-600 ml-1 cursor-pointer"
                                >
                                  <X className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      <p className="text-[10px] text-slate-400">
                        Tranh vẽ tình huống Mondai 1 & 4, lựa chọn Mondai 2
                      </p>
                    </div>
                  </div>

                  <p className="text-[11px] text-indigo-700 bg-indigo-50/50 p-2 rounded-xl">
                    💡 <strong>Gợi ý:</strong> Bạn có thể nạp cả file Audio MP3 và file PDF đề nghe để AI kết hợp chính xác cả tranh vẽ và mốc nghe.
                  </p>
                </div>
              ) : (
                /* Nếu là Đọc hiểu hoặc Kiến thức ngôn ngữ: Upload PDF (Hỗ trợ nhiều file như Ngữ pháp + Bài đọc) */
                <div className="border-2 border-dashed border-sky-200 rounded-2xl p-6 text-center space-y-3 bg-sky-50/30">
                  <FileText className="w-10 h-10 text-sky-600 mx-auto" />
                  <div>
                    <label className="inline-block px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white font-bold rounded-xl text-xs cursor-pointer transition shadow-sm">
                      <span>+ Chọn các file PDF (Có thể chọn nhiều file)</span>
                      <input
                        type="file"
                        multiple
                        accept=".pdf"
                        onChange={(e) => {
                          if (e.target.files && e.target.files.length > 0) {
                            const newFiles = Array.from(e.target.files)
                            setAiPdfFiles((prev) => {
                              // Tránh trùng file cùng tên
                              const existingNames = new Set(prev.map((f) => f.name))
                              return [...prev, ...newFiles.filter((f) => !existingNames.has(f.name))]
                            })
                          }
                          // Reset input để có thể chọn lại file cùng tên nếu muốn
                          e.target.value = ''
                        }}
                        className="hidden"
                      />
                    </label>

                    {/* Danh sách các file PDF đã chọn */}
                    {aiPdfFiles.length > 0 && (
                      <div className="mt-3 space-y-1.5 max-h-36 overflow-y-auto">
                        {aiPdfFiles.map((file, idx) => (
                          <div
                            key={idx}
                            className="flex items-center justify-between p-2 bg-white rounded-xl border border-sky-200 text-left"
                          >
                            <div className="flex items-center gap-2 overflow-hidden">
                              <span className="w-5 h-5 rounded-md bg-sky-100 text-sky-700 flex items-center justify-center font-bold text-[10px] shrink-0">
                                {idx + 1}
                              </span>
                              <div className="truncate">
                                <p className="text-xs font-semibold text-slate-800 truncate">{file.name}</p>
                                <p className="text-[10px] text-slate-400">
                                  {(file.size / 1024 / 1024).toFixed(2)} MB
                                </p>
                              </div>
                            </div>
                            <button
                              type="button"
                              onClick={() => setAiPdfFiles((prev) => prev.filter((_, i) => i !== idx))}
                              className="p-1 rounded-md text-slate-400 hover:text-rose-600 hover:bg-rose-50 cursor-pointer transition shrink-0 ml-2"
                              title="Bỏ file này"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <p className="text-[11px] text-slate-500">
                    💡 Hỗ trợ nạp cùng lúc nhiều file PDF (ví dụ: 1 file ngữ pháp + 1 file bài đọc hiểu riêng). AI sẽ tự ghép nối và đánh số câu hỏi liên tục.
                  </p>
                </div>
              )}

              {/* Nạp Bảng Đáp Án Chính Thức (Tùy chọn) */}
              <div className="p-3.5 bg-emerald-50/60 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800 rounded-2xl space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-emerald-900 dark:text-emerald-300 flex items-center gap-1.5">
                    <FileCheck className="w-4 h-4 text-emerald-600" />
                    <span>Bảng đáp án chính thức (正答表 - PDF hoặc Ảnh):</span>
                  </label>
                  <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 dark:bg-emerald-900/60 px-2 py-0.5 rounded-full">
                    Khuyên dùng ★
                  </span>
                </div>
                <p className="text-[11px] text-slate-500">
                  Tải lên file ảnh hoặc PDF bảng đáp án (正答表) của kỳ thi để AI tự động điền đáp án đúng 100% cho từng câu hỏi, tránh việc đề thi bị để trống đáp án.
                </p>

                {aiAnswerFile ? (
                  <div className="flex items-center justify-between p-2.5 bg-white dark:bg-slate-800 rounded-xl border border-emerald-300">
                    <div className="flex items-center gap-2 truncate">
                      <FileCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                      <span className="text-xs font-bold text-slate-800 dark:text-slate-200 truncate">
                        {aiAnswerFile.name}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        ({(aiAnswerFile.size / 1024 / 1024).toFixed(2)} MB)
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setAiAnswerFile(null)}
                      className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg cursor-pointer transition"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <label className="block p-3 border-2 border-dashed border-emerald-300 dark:border-emerald-700 hover:border-emerald-500 rounded-xl text-center cursor-pointer bg-white/70 dark:bg-slate-800/70 transition">
                    <div className="flex items-center justify-center gap-2 text-xs font-bold text-emerald-700 dark:text-emerald-300">
                      <UploadCloud className="w-4 h-4 text-emerald-600" />
                      <span>+ Chọn file Bảng đáp án chính thức (PDF / Ảnh JPG, PNG)</span>
                    </div>
                    <input
                      type="file"
                      accept=".pdf,image/*"
                      onChange={(e) => {
                        const file = e.target.files?.[0]
                        if (file) setAiAnswerFile(file)
                      }}
                      className="hidden"
                    />
                  </label>
                )}
              </div>

              <button
                type="button"
                onClick={handleRunAiSectionExtraction}
                disabled={aiSectionLoading || (isCurrentSectionListening ? (!aiAudioFile && aiPdfFiles.length === 0) : aiPdfFiles.length === 0)}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shadow transition flex items-center justify-center gap-2 cursor-pointer disabled:opacity-40"
              >
                {aiSectionLoading ? (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <Sparkles className="w-4 h-4 text-amber-300" />
                )}
                <span>{aiSectionLoading ? 'AI đang phân tích...' : 'Bắt đầu phân tích bằng AI'}</span>
              </button>

              {/* Kết quả trích xuất được */}
              {aiExtractedQuestions.length > 0 && (
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-2xl space-y-2">
                  <span className="font-bold text-slate-800">
                    Kết quả AI trích xuất: {aiExtractedQuestions.length} câu hỏi
                  </span>
                  <div className="max-h-40 overflow-y-auto space-y-1.5 text-[11px]">
                    {aiExtractedQuestions.map((q, idx) => (
                      <div key={idx} className="p-2 bg-white rounded-lg border border-slate-200">
                        {isCurrentSectionListening ? (
                          <div>
                            <strong>Câu {q.question_number || idx + 1}:</strong> Mốc{' '}
                            {Math.floor(q.audio_start_time || q.start_time || 0)}s ➔ {Math.ceil(q.audio_end_time || q.end_time || 30)}s
                            {q.transcript_jp && (
                              <p className="text-slate-500 italic truncate mt-0.5">
                                "{q.transcript_jp}"
                              </p>
                            )}
                          </div>
                        ) : (
                          <div>
                            <strong>Câu {idx + 1}:</strong> {q.question_text}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>

                  {/* Tùy chọn Ghi đè hoặc Thêm tiếp nối nếu phần thi đã có câu hỏi */}
                  {activeSection && (activeSection.questions?.length || 0) > 0 && (
                    <div className="p-3 bg-amber-50/80 border border-amber-200 rounded-xl space-y-2 mt-2">
                      <div className="flex items-center gap-1.5 text-xs font-bold text-amber-900">
                        <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                        <span>Phần thi này hiện đã có {activeSection.questions.length} câu hỏi. Chọn cách lưu:</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                        <label
                          className={`p-2.5 rounded-xl border cursor-pointer flex items-start gap-2 transition ${
                            aiSaveMode === 'OVERWRITE'
                              ? 'bg-amber-100/90 border-amber-400 font-bold text-amber-950 shadow-2xs'
                              : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                          }`}
                        >
                          <input
                            type="radio"
                            name="aiSaveMode"
                            value="OVERWRITE"
                            checked={aiSaveMode === 'OVERWRITE'}
                            onChange={() => setAiSaveMode('OVERWRITE')}
                            className="mt-0.5 accent-amber-600"
                          />
                          <div>
                            <div className="flex items-center gap-1 font-bold">
                              <RefreshCw className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                              <span>Ghi đè (Khuyên dùng)</span>
                            </div>
                            <p className="text-[10px] text-slate-500 font-normal mt-0.5 leading-relaxed">
                              Xóa sạch {activeSection.questions.length} câu cũ, thay thế bằng {aiExtractedQuestions.length} câu mới vừa quét
                            </p>
                          </div>
                        </label>

                        <label
                          className={`p-2.5 rounded-xl border cursor-pointer flex items-start gap-2 transition ${
                            aiSaveMode === 'APPEND'
                              ? 'bg-indigo-50 border-indigo-400 font-bold text-indigo-950 shadow-2xs'
                              : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                          }`}
                        >
                          <input
                            type="radio"
                            name="aiSaveMode"
                            value="APPEND"
                            checked={aiSaveMode === 'APPEND'}
                            onChange={() => setAiSaveMode('APPEND')}
                            className="mt-0.5 accent-indigo-600"
                          />
                          <div>
                            <div className="flex items-center gap-1 font-bold">
                              <PlusCircle className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
                              <span>Thêm tiếp nối</span>
                            </div>
                            <p className="text-[10px] text-slate-500 font-normal mt-0.5 leading-relaxed">
                              Giữ nguyên câu cũ, nối thêm {aiExtractedQuestions.length} câu mới vào cuối danh sách
                            </p>
                          </div>
                        </label>
                      </div>
                    </div>
                  )}

                  <button
                    type="button"
                    onClick={handleApplyAiQuestionsToSection}
                    disabled={aiSectionLoading}
                    className={`w-full mt-2 py-2.5 text-white font-bold rounded-xl text-xs shadow transition flex items-center justify-center gap-1.5 cursor-pointer ${
                      aiSaveMode === 'OVERWRITE' && (activeSection?.questions?.length || 0) > 0
                        ? 'bg-amber-600 hover:bg-amber-700'
                        : 'bg-emerald-600 hover:bg-emerald-700'
                    }`}
                  >
                    {aiSaveMode === 'OVERWRITE' && (activeSection?.questions?.length || 0) > 0 ? (
                      <RefreshCw className="w-4 h-4" />
                    ) : (
                      <CheckCircle2 className="w-4 h-4" />
                    )}
                    <span>
                      {aiSaveMode === 'OVERWRITE' && (activeSection?.questions?.length || 0) > 0
                        ? `Ghi đè & Lưu ${aiExtractedQuestions.length} câu hỏi mới vào phần này`
                        : `Lưu ${aiExtractedQuestions.length} câu hỏi vào phần thi`}
                    </span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* MODAL PHÓNG TO ẢNH TỜ RƠI / BẢNG BIỂU */}
      {zoomImageUrl && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in cursor-zoom-out"
          onClick={() => setZoomImageUrl(null)}
        >
          <div
            className="relative max-w-5xl max-h-[92vh] bg-white rounded-3xl p-3 shadow-2xl flex flex-col items-center"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setZoomImageUrl(null)}
              className="absolute -top-3 -right-3 w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center hover:bg-slate-700 shadow-lg cursor-pointer z-10"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={zoomImageUrl}
              alt="Zoomed flyer"
              className="max-h-[85vh] w-auto object-contain rounded-2xl"
            />
          </div>
        </div>
      )}

      {/* MODAL XÁC NHẬN HOÀN TÁC (ROLLBACK TO SNAPSHOT) */}
      {isRollbackModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
                <RotateCcw className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-extrabold text-slate-900">
                  Khôi phục về trạng thái ban đầu?
                </h3>
                <p className="text-xs text-slate-500">
                  Điểm lưu được ghi nhận lúc: <strong>{snapshotTimestamp || 'Ban đầu'}</strong>
                </p>
              </div>
            </div>

            <div className="p-3.5 bg-amber-50 rounded-2xl border border-amber-200 text-xs text-amber-950 leading-relaxed space-y-1.5">
              <p className="font-semibold flex items-center gap-1">
                <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                <span>Cảnh báo hoàn tác dữ liệu:</span>
              </p>
              <p className="text-slate-600">
                Mọi thao tác thêm câu hỏi mới, xóa câu hỏi, chỉnh sửa nội dung hoặc quét đè bằng AI trong phiên này sẽ bị <strong>hủy bỏ hoàn toàn</strong>. Đề thi sẽ được trả về đúng nguyên trạng của điểm lưu trước đó.
              </p>
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsRollbackModalOpen(false)}
                disabled={isRollingBack}
                className="px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer"
              >
                Giữ các thay đổi
              </button>
              <button
                type="button"
                onClick={handleExecuteRollback}
                disabled={isRollingBack}
                className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-bold transition shadow-sm cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
              >
                <RotateCcw className="w-4 h-4" />
                <span>{isRollingBack ? 'Đang khôi phục...' : 'Xác nhận Hoàn tác'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// =========================================================================
// COMPONENT PHỤ: QUESTION ITEM COMPONENT
// =========================================================================
interface QuestionItemComponentProps {
  q: AdminTakingQuestion
  mondaiDef?: JlptMondaiDefinition
  isCurrentSectionListening: boolean
  activeSectionAudioUrl?: string
  editingQuestionId: number | null
  setEditingQuestionId: (id: number | null) => void
  hidePassageContent?: boolean
  editForm: UpdateQuestionInlinePayload
  setEditForm: (form: UpdateQuestionInlinePayload) => void
  savingQuestion: boolean
  handleStartEdit: (q: AdminTakingQuestion) => void
  handleSaveQuestion: (id: number) => void
  handleDeleteQuestion: (id: number) => void
  handleStartEditPassage?: (p: TakingPassage) => void
  hasInProgress?: boolean
  hasSubmissions?: boolean
  onZoomImage?: (url: string) => void
}

const QuestionItemComponent: React.FC<QuestionItemComponentProps> = ({
  q,
  mondaiDef,
  hidePassageContent = false,
  isCurrentSectionListening,
  activeSectionAudioUrl,
  editingQuestionId,
  setEditingQuestionId,
  editForm,
  setEditForm,
  savingQuestion,
  handleStartEdit,
  handleSaveQuestion,
  handleDeleteQuestion,
  handleStartEditPassage,
  hasInProgress = false,
  hasSubmissions = false,
  onZoomImage,
}) => {
  const isEditing = editingQuestionId === q.id
  const qMondaiDef = mondaiDef || getMondaiByCode(q.mondaiCode)
  const isStarQuestion = qMondaiDef?.questionType === 'STAR' || (q.starPosition && q.starPosition > 0)
  const hasPassage = Boolean(
    (q.passage && (q.passage.contentJp || q.passage.imageUrl)) ||
    qMondaiDef?.hasPassage ||
    qMondaiDef?.questionType === 'READING_PASSAGE' ||
    qMondaiDef?.questionType === 'CLOZE_TEXT'
  )

  return (
    <div className="bg-slate-50/60 rounded-2xl border border-slate-200 p-4 space-y-3 relative transition hover:border-slate-300">
      {/* 1. Header Câu hỏi */}
      <div className="flex items-center justify-between pb-2.5 border-b border-slate-200">
        <div className="flex flex-wrap items-center gap-2">
          <span className="w-7 h-7 rounded-lg bg-indigo-600 text-white font-black flex items-center justify-center text-xs shadow-xs">
            {q.questionNumber}
          </span>

          {/* Dấu sao: CHỈ HIỂN THỊ KHI CÂU HỎI THỰC SỰ LÀ CÂU SAO */}
          {isStarQuestion && (
            <span className="text-xs font-bold px-2.5 py-0.5 bg-amber-100 text-amber-900 border border-amber-300 rounded-md flex items-center gap-1">
              <Star className="w-3.5 h-3.5 text-amber-600 fill-amber-500" />
              Câu sắp xếp dấu sao (Vị trí #{q.starPosition || 3})
            </span>
          )}

          {/* Audio riêng badge */}
          {isCurrentSectionListening && q.audioUrl && (
            <span className="text-xs font-bold px-2 py-0.5 bg-purple-100 text-purple-800 border border-purple-200 rounded-md flex items-center gap-1">
              <Music className="w-3.5 h-3.5 text-purple-600" />
              Audio riêng
            </span>
          )}

          {/* Visual Options badge */}
          {q.hasVisualOptions && (
            <span className="text-xs font-bold px-2 py-0.5 bg-sky-100 text-sky-800 border border-sky-200 rounded-md flex items-center gap-1">
              <ImageIcon className="w-3.5 h-3.5 text-sky-600" />
              Đáp án tranh vẽ
            </span>
          )}
        </div>

        {!isEditing ? (
          <div className="flex items-center gap-2">
            <button
              onClick={() => handleStartEdit(q)}
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl border border-slate-300 bg-white text-slate-700 hover:bg-slate-100 text-xs font-bold transition cursor-pointer shadow-2xs"
            >
              <Edit3 className="w-3.5 h-3.5 text-indigo-600" />
              Sửa
            </button>
            <button
              onClick={() => handleDeleteQuestion(q.id)}
              disabled={hasInProgress}
              className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl border border-rose-200 bg-white text-rose-600 text-xs font-bold transition shadow-2xs ${
                hasInProgress
                  ? 'opacity-40 cursor-not-allowed hover:bg-white'
                  : 'hover:bg-rose-50 cursor-pointer'
              }`}
              title={hasInProgress ? 'Không thể xóa khi đang có thí sinh làm bài trực tiếp' : 'Xóa câu hỏi khỏi đề thi'}
            >
              <Trash2 className="w-3.5 h-3.5" />
              Xóa
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <button
              onClick={() => setEditingQuestionId(null)}
              className="px-3 py-1 rounded-xl border border-slate-300 bg-white text-slate-600 text-xs font-semibold hover:bg-slate-50 cursor-pointer"
            >
              Hủy
            </button>
            <button
              onClick={() => handleSaveQuestion(q.id)}
              disabled={savingQuestion}
              className="inline-flex items-center gap-1.5 px-4 py-1 rounded-xl bg-indigo-600 text-white text-xs font-bold shadow hover:bg-indigo-700 transition cursor-pointer"
            >
              <Save className="w-3.5 h-3.5" />
              {savingQuestion ? 'Đang lưu...' : 'Lưu'}
            </button>
          </div>
        )}
      </div>

      {/* 2. NỘI DUNG ĐẶC THÙ: BÀI ĐỌC HIỂU (Nếu có đoạn văn) */}
      {hasPassage && !hidePassageContent && !isEditing && (
        <div className="p-3 bg-sky-50 border border-sky-200 rounded-xl space-y-1.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 text-xs font-bold text-sky-950">
              <BookOpen className="w-3.5 h-3.5 text-sky-600" />
              <span>{q.passage?.title || 'Đoạn văn đọc hiểu'}</span>
            </div>
            {q.passage && handleStartEditPassage && (
              <button
                type="button"
                onClick={() => handleStartEditPassage(q.passage!)}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg border border-indigo-200 bg-white hover:bg-indigo-50 text-indigo-700 text-[11px] font-bold transition shadow-2xs cursor-pointer"
                title="Chỉnh sửa nội dung hoặc ảnh của bài đọc này"
              >
                <Edit3 className="w-3 h-3 text-indigo-600" />
                <span>Sửa bài đọc</span>
              </button>
            )}
          </div>
          <div
            className="text-xs text-slate-800 dark:text-slate-200 leading-relaxed select-text bg-white dark:bg-slate-900 p-3 rounded-xl border border-sky-100 dark:border-sky-900"
            dangerouslySetInnerHTML={{ __html: formatJapanesePassageHtml(q.passage?.contentJp) }}
          />
          {q.passage?.translationVi && (
            <p className="text-[11px] text-slate-500 italic">
              Dịch: {q.passage.translationVi}
            </p>
          )}
          {q.passage?.imageUrl && (
            <img
              src={q.passage.imageUrl}
              alt="Passage graphic"
              className="max-h-40 rounded border border-slate-200 object-contain bg-white"
            />
          )}
        </div>
      )}

      {/* 3. NỘI DUNG ĐẶC THÙ: AUDIO PLAYER HOÀN CHỈNH CHO CÂU HỎI NGHE HIỂU */}
      {isCurrentSectionListening && !isEditing && (
        <div className="space-y-2">
          {/* Tranh tình huống nếu câu hỏi nghe có ảnh */}
          {q.imageUrl && (
            <div className="p-2 border border-indigo-200 rounded-xl bg-white max-w-md shadow-xs">
              <span className="text-[10px] font-bold text-slate-400 block mb-1">
                Tranh vẽ đề bài (tình huống):
              </span>
              <img
                src={q.imageUrl}
                alt="Question illustration"
                className="max-h-56 object-contain rounded mx-auto cursor-pointer hover:opacity-90 transition"
                onClick={() => onZoomImage?.(q.imageUrl!)}
              />
            </div>
          )}

          {/* Mini Audio Player có thanh kéo (seekbar), Play/Pause, tua 5s */}
          <QuestionAudioPlayer
            audioUrl={q.audioUrl}
            masterAudioUrl={activeSectionAudioUrl}
            startTime={q.audioStartTime}
            endTime={q.audioEndTime}
            title={`Audio câu ${q.questionNumber}`}
          />

          {/* Kịch bản nghe (Scripts) */}
          {q.transcriptJp && (
            <div className="bg-white p-2.5 rounded-xl border border-indigo-100 text-xs text-slate-700 font-serif">
              <span className="font-bold text-indigo-700 block mb-1">
                Kịch bản thoại tiếng Nhật (Scripts):
              </span>
              {q.transcriptJp}
            </div>
          )}
        </div>
      )}

      {/* 4. CHẾ ĐỘ CHỈNH SỬA (INLINE EDIT) */}
      {isEditing ? (
        <div className="space-y-3.5 pt-1">
          {/* Banner cảnh báo khóa nội dung câu hỏi khi đề đã có bài nộp */}
          {hasSubmissions && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-900 flex items-start gap-2.5 shadow-2xs">
              <Lock className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-bold">Đề thi đã có bài nộp — Nội dung câu hỏi & lựa chọn đang ở chế độ Chỉ đọc (Locked)</p>
                <p className="text-[11px] text-amber-700 mt-0.5 leading-relaxed">
                  Để đảm bảo tính nhất quán và công bằng khi học viên xem lại bài thi cũ (Exam Review), câu chữ của câu hỏi và các đáp án gốc đã được khóa. Bạn chỉ có thể cập nhật <strong>Lời giải thích chi tiết</strong> hoặc <strong>Bản dịch/Audio</strong>. Nếu cần thay đổi toàn diện câu hỏi, vui lòng dùng tính năng <strong>[Nhân bản đề (Bản mới)]</strong> ở đầu trang.
                </p>
              </div>
            </div>
          )}

          {/* Nếu là Nghe hiểu: Chỉnh sửa Audio riêng, mốc giây, ảnh câu hỏi */}
          {isCurrentSectionListening && (
            <div className="p-3 bg-indigo-50/70 border border-indigo-200 rounded-xl space-y-2.5">
              <span className="text-xs font-bold text-indigo-900 flex items-center gap-1">
                <Headphones className="w-3.5 h-3.5 text-indigo-600" />
                Cấu hình âm thanh & Tranh vẽ cho câu {q.questionNumber}:
              </span>

              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1 flex items-center justify-between">
                  <span className="flex items-center gap-1">
                    <Music className="w-3.5 h-3.5 text-indigo-600" />
                    File Audio riêng cho câu này (URL MP3 - Tùy chọn):
                  </span>
                  <FileUploadButton
                    accept="audio"
                    folder="jlpt_exam/audios"
                    buttonText="Tải MP3 riêng cho câu này"
                    onUploadSuccess={(url: string) => setEditForm({ ...editForm, audioUrl: url })}
                  />
                </label>
                <input
                  type="text"
                  value={editForm.audioUrl || ''}
                  onChange={(e) => setEditForm({ ...editForm, audioUrl: e.target.value })}
                  placeholder="https://.../cau_so.mp3 hoặc tải từ máy tính"
                  className="w-full p-2 border border-slate-300 rounded-lg text-xs bg-white font-mono"
                />
              </div>

              {/* Thanh kéo chọn mốc âm thanh trực quan kèm nút gắn mốc */}
              {(editForm.audioUrl || activeSectionAudioUrl || q.audioUrl) ? (
                <div className="pt-1">
                  <AudioSegmentTrimmer
                    audioUrl={editForm.audioUrl}
                    masterAudioUrl={activeSectionAudioUrl || q.audioUrl}
                    startTime={editForm.audioStartTime ?? 0}
                    endTime={editForm.audioEndTime ?? 30}
                    onChangeStartTime={(val) =>
                      setEditForm({
                        ...editForm,
                        audioStartTime: val,
                      })
                    }
                    onChangeEndTime={(val) =>
                      setEditForm({
                        ...editForm,
                        audioEndTime: val,
                      })
                    }
                    title={`Kéo chỉnh đoạn âm thanh (Câu ${q.questionNumber})`}
                  />

                  {/* Nhập số chính xác thủ công nếu cần */}
                  <div className="grid grid-cols-2 gap-2 pt-2">
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                        Bắt đầu (giây - nhập số):
                      </label>
                      <input
                        type="number"
                        step="0.1"
                        value={editForm.audioStartTime ?? 0}
                        onChange={(e) =>
                          setEditForm({
                            ...editForm,
                            audioStartTime: parseFloat(e.target.value) || 0,
                          })
                        }
                        className="w-full p-1.5 border border-slate-300 rounded-lg text-xs bg-white font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                        Kết thúc (giây - nhập số):
                      </label>
                      <input
                        type="number"
                        step="0.1"
                        value={editForm.audioEndTime ?? 0}
                        onChange={(e) =>
                          setEditForm({
                            ...editForm,
                            audioEndTime: parseFloat(e.target.value) || 0,
                          })
                        }
                        className="w-full p-1.5 border border-slate-300 rounded-lg text-xs bg-white font-mono"
                      />
                    </div>
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Bắt đầu (giây):
                    </label>
                    <input
                      type="number"
                      step="0.5"
                      value={editForm.audioStartTime ?? 0}
                      onChange={(e) =>
                        setEditForm({
                          ...editForm,
                          audioStartTime: parseFloat(e.target.value) || 0,
                        })
                      }
                      className="w-full p-1.5 border border-slate-300 rounded-lg text-xs bg-white font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Kết thúc (giây):
                    </label>
                    <input
                      type="number"
                      step="0.5"
                      value={editForm.audioEndTime ?? 0}
                      onChange={(e) =>
                        setEditForm({
                          ...editForm,
                          audioEndTime: parseFloat(e.target.value) || 0,
                        })
                      }
                      className="w-full p-1.5 border border-slate-300 rounded-lg text-xs bg-white font-mono"
                    />
                  </div>
                </div>
              )}

              {/* Tranh vẽ tình huống câu hỏi */}
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1 flex items-center justify-between">
                  <span className="flex items-center gap-1">
                    <ImageIcon className="w-3.5 h-3.5 text-indigo-600" />
                    Hình ảnh tình huống câu hỏi (Tranh đề bài - Tùy chọn):
                  </span>
                  <FileUploadButton
                    accept="image"
                    folder="jlpt_exam/images"
                    buttonText="Tải tranh đề bài"
                    onUploadSuccess={(url: string) => setEditForm({ ...editForm, imageUrl: url })}
                  />
                </label>
                <input
                  type="text"
                  value={editForm.imageUrl || ''}
                  onChange={(e) => setEditForm({ ...editForm, imageUrl: e.target.value })}
                  placeholder="https://.../tranh_de_bai.png hoặc tải từ máy tính"
                  className="w-full p-1.5 border border-slate-300 rounded-lg text-xs bg-white font-mono"
                />
                {editForm.imageUrl && (
                  <div className="mt-2 p-1.5 border border-indigo-200 rounded-lg bg-white inline-flex items-center gap-2">
                    <img src={editForm.imageUrl} alt="Preview" className="h-16 rounded object-contain" />
                    <button
                      type="button"
                      onClick={() => setEditForm({ ...editForm, imageUrl: '' })}
                      className="text-[10px] text-rose-600 font-bold hover:underline"
                    >
                      Xóa ảnh
                    </button>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Kịch bản thoại tiếng Nhật (Scripts):
                </label>
                <textarea
                  rows={2}
                  value={editForm.transcriptJp || ''}
                  onChange={(e) => setEditForm({ ...editForm, transcriptJp: e.target.value })}
                  className="w-full p-1.5 border border-slate-300 rounded-lg text-xs font-serif bg-white"
                />
              </div>
            </div>
          )}

          {/* Dấu sao ★: CHỈ HIỂN THỊ KHI CÂU NÀY LÀ CÂU SAO */}
          {isStarQuestion && (
            <div className="p-3 bg-amber-50 border border-amber-300 rounded-xl space-y-1.5">
              <span className="text-xs font-bold text-amber-950 flex items-center gap-1">
                <Star className="w-3.5 h-3.5 text-amber-600 fill-amber-500" />
                Vị trí dấu sao ★ trong câu:
              </span>
              <div className="flex gap-2">
                {[1, 2, 3, 4].map((pos) => (
                  <button
                    key={pos}
                    type="button"
                    onClick={() => setEditForm({ ...editForm, starPosition: pos })}
                    className={`px-3 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                      editForm.starPosition === pos
                        ? 'bg-amber-500 text-white shadow'
                        : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    ★ Vị trí {pos}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Nội dung câu hỏi */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-bold text-slate-700">
                Câu hỏi:
              </label>
              {hasSubmissions && (
                <span className="text-[11px] font-semibold text-amber-700 flex items-center gap-1">
                  <Lock className="w-3 h-3 text-amber-600" />
                  Đã khóa sửa chữ (Đã có bài nộp)
                </span>
              )}
            </div>
            <textarea
              rows={2}
              value={editForm.questionText}
              disabled={hasSubmissions}
              onChange={(e) => setEditForm({ ...editForm, questionText: e.target.value })}
              className={`w-full p-2 border rounded-xl text-xs font-serif ${
                hasSubmissions
                  ? 'bg-slate-100/90 border-slate-200 text-slate-500 cursor-not-allowed select-none'
                  : 'bg-white border-slate-300'
              }`}
            />
          </div>

          {/* 4 Lựa chọn trắc nghiệm (Hỗ trợ tranh vẽ hoặc chữ) */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">
                {editForm.hasVisualOptions ? '4 Lựa chọn đáp án dạng tranh vẽ:' : 'Các đáp án lựa chọn:'}
              </label>
              <label className="flex items-center gap-1 cursor-pointer text-indigo-700 text-[11px] font-semibold">
                <input
                  type="checkbox"
                  checked={editForm.hasVisualOptions}
                  onChange={(e) => {
                    const checked = e.target.checked
                    const nextOptions = editForm.options?.map((opt) => ({
                      ...opt,
                      optionText: checked ? '' : opt.optionText,
                    }))
                    setEditForm({ ...editForm, hasVisualOptions: checked, options: nextOptions })
                  }}
                  className="w-3.5 h-3.5 text-indigo-600 rounded"
                />
                <span>Đáp án dạng tranh vẽ (Không có chữ)</span>
              </label>
            </div>

            {/* NẾU LÀ TRANH VẼ: ẨN Ô CHỮ, HIỂN THỊ 4 THẺ TRANH KÈM TẢI ẢNH VÀ NÚT CHỌN ĐÁP ÁN ĐÚNG */}
            {editForm.hasVisualOptions ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {editForm.options?.map((opt: AdminTakingOption, idx: number) => (
                  <div
                    key={opt.id || idx}
                    className={`p-3 rounded-2xl border-2 transition relative flex flex-col justify-between gap-2.5 ${
                      opt.correct
                        ? 'border-emerald-500 bg-emerald-50/70 shadow-2xs'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <label className="flex items-center gap-2 cursor-pointer font-bold text-xs text-slate-800">
                        <input
                          type="radio"
                          name={`edit-opt-${q.id}`}
                          checked={opt.correct}
                          onChange={() => {
                            const updated = editForm.options?.map((o, i) => ({
                              ...o,
                              correct: i === idx,
                              isCorrect: i === idx,
                            }))
                            setEditForm({
                              ...editForm,
                              options: updated,
                              correctOptionKey: opt.optionKey,
                            })
                          }}
                          className="w-4 h-4 text-emerald-600 cursor-pointer"
                        />
                        <span className="w-5 h-5 rounded-md bg-indigo-100 text-indigo-800 flex items-center justify-center font-bold text-xs">
                          {opt.optionKey}
                        </span>
                        <span>Tranh {opt.optionKey} {opt.correct && '(Đáp án đúng)'}</span>
                      </label>

                      <FileUploadButton
                        accept="image"
                        folder="jlpt_exam/options"
                        buttonText="Tải tranh"
                        onUploadSuccess={(url: string) => {
                          const updated = [...(editForm.options || [])]
                          updated[idx].optionImageUrl = url
                          updated[idx].optionText = ''
                          setEditForm({ ...editForm, options: updated })
                        }}
                      />
                    </div>

                    {/* Preview Tranh */}
                    <div className="border border-dashed border-slate-300 rounded-xl bg-slate-50/70 min-h-[90px] flex items-center justify-center p-1.5 overflow-hidden">
                      {opt.optionImageUrl ? (
                        <img
                          src={opt.optionImageUrl}
                          alt={`Tranh ${opt.optionKey}`}
                          className="max-h-24 w-auto object-contain rounded-lg shadow-2xs"
                        />
                      ) : (
                        <span className="text-[11px] text-slate-400 italic">
                          Chưa có tranh. Bấm "Tải tranh" hoặc dán URL
                        </span>
                      )}
                    </div>

                    {/* URL text input phụ */}
                    <input
                      type="text"
                      value={opt.optionImageUrl || ''}
                      onChange={(e) => {
                        const updated = [...(editForm.options || [])]
                        updated[idx].optionImageUrl = e.target.value
                        updated[idx].optionText = ''
                        setEditForm({ ...editForm, options: updated })
                      }}
                      placeholder={`URL tranh ${opt.optionKey}`}
                      className="w-full p-1 border border-slate-200 rounded-md text-[10px] font-mono text-slate-600 bg-white"
                    />
                  </div>
                ))}
              </div>
            ) : (
              /* NẾU LÀ ĐÁP ÁN CHỮ BÌNH THƯỜNG */
              <div className="space-y-2">
                {editForm.options?.map((opt: AdminTakingOption, idx: number) => (
                  <div
                    key={opt.id || idx}
                    className={`p-2.5 rounded-xl border flex items-center gap-2 ${
                      opt.correct ? 'border-emerald-500 bg-emerald-50/60' : 'border-slate-200 bg-white'
                    }`}
                  >
                    <input
                      type="radio"
                      name={`edit-opt-${q.id}`}
                      checked={opt.correct}
                      onChange={() => {
                        const updated = editForm.options?.map((o, i) => ({
                          ...o,
                          correct: i === idx,
                          isCorrect: i === idx,
                        }))
                        setEditForm({
                          ...editForm,
                          options: updated,
                          correctOptionKey: opt.optionKey,
                        })
                      }}
                      className="w-4 h-4 text-emerald-600 cursor-pointer"
                    />
                    <span className="w-5 h-5 rounded-md bg-slate-100 flex items-center justify-center font-bold text-xs text-slate-700">
                      {opt.optionKey}
                    </span>
                    <input
                      type="text"
                      value={opt.optionText}
                      disabled={hasSubmissions}
                      onChange={(e) => {
                        const updated = [...(editForm.options || [])]
                        updated[idx].optionText = e.target.value
                        setEditForm({ ...editForm, options: updated })
                      }}
                      placeholder={`Lựa chọn ${opt.optionKey}`}
                      className={`flex-1 p-1.5 border rounded-lg text-xs font-serif ${
                        hasSubmissions
                          ? 'bg-slate-100/90 border-slate-200 text-slate-500 cursor-not-allowed select-none'
                          : 'bg-white border-slate-300'
                      }`}
                    />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Giải thích */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              Giải thích đáp án (tùy chọn):
            </label>
            <textarea
              rows={2}
              value={editForm.explanationVi || ''}
              onChange={(e) => setEditForm({ ...editForm, explanationVi: e.target.value })}
              className="w-full p-1.5 border border-slate-300 rounded-lg text-xs bg-white"
            />
          </div>
        </div>
      ) : (
        /* 5. CHẾ ĐỘ HIỂN THỊ XEM TRƯỚC BÌNH THƯỜNG */
        <div className="space-y-3">
          <div
            className="text-sm font-semibold text-slate-900 font-serif leading-relaxed [&_u]:underline [&_u]:underline-offset-4 [&_u]:decoration-2 [&_u]:font-bold [&_strong]:font-bold"
            dangerouslySetInnerHTML={{
              __html: cleanQuestionPrompt(q.questionText)
            }}
          />

          {/* Đáp án trắc nghiệm (Hỗ trợ tranh vẽ đáp án, lọc bỏ đáp án trống) */}
          {(() => {
            const isThreeOptionsMondai = q.mondaiCode === 'UTTERANCE_EXPRESSIONS' || q.mondaiCode === 'QUICK_RESPONSE'
            const visibleOptions = q.options.filter((opt: AdminTakingOption) => {
              if (isThreeOptionsMondai && opt.optionKey > 3) return false
              if (q.hasVisualOptions) {
                return Boolean(opt.optionImageUrl || (opt.optionText && opt.optionText.trim().length > 0))
              }
              if (opt.optionImageUrl || (opt.optionText && opt.optionText.trim().length > 0)) {
                return true
              }
              const othersHaveContent = q.options.some((o: AdminTakingOption) => o.id !== opt.id && (Boolean(o.optionText?.trim()) || Boolean(o.optionImageUrl)))
              if (othersHaveContent) return false
              return isThreeOptionsMondai ? opt.optionKey <= 3 : opt.optionKey <= 4
            })

            // Rule: Mỗi câu chỉ có DUY NHẤT 1 đáp án đúng hiển thị trên giao diện
            const correctKeys = visibleOptions.filter((o: AdminTakingOption) => o.correct).map((o: AdminTakingOption) => o.optionKey)
            const singleCorrectKey = correctKeys.length > 1
              ? (correctKeys.find((k: number) => k > 1) ?? correctKeys[0])
              : (correctKeys[0] ?? null)

            return (
              <div className={`grid gap-2 ${visibleOptions.length === 3 ? 'grid-cols-1 sm:grid-cols-3' : 'grid-cols-1 sm:grid-cols-2'}`}>
                {visibleOptions.map((opt: AdminTakingOption) => {
                  const isOptionCorrect = Boolean(opt.correct && (singleCorrectKey === null || opt.optionKey === singleCorrectKey))
                  return (
                    <div
                      key={opt.id}
                      className={`p-2.5 rounded-xl border flex flex-col justify-between gap-2 text-xs ${
                        isOptionCorrect
                          ? 'border-emerald-500 bg-emerald-50/70 text-emerald-950 font-bold'
                          : 'border-slate-200 bg-white text-slate-700'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <span
                          className={`w-5 h-5 rounded-md flex items-center justify-center font-bold text-xs shrink-0 ${
                            isOptionCorrect ? 'bg-emerald-600 text-white' : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {opt.optionKey}
                        </span>
                        {opt.optionText && opt.optionText.trim() !== String(opt.optionKey) && !opt.optionText.startsWith('Lựa chọn') && !(q.hasVisualOptions && opt.optionText.startsWith('選択肢')) && (
                          <span
                            className="flex-1 font-serif [&_u]:underline [&_u]:underline-offset-4 [&_u]:decoration-2 [&_u]:font-bold"
                            dangerouslySetInnerHTML={{ __html: opt.optionText }}
                          />
                        )}
                        {isOptionCorrect && (
                          <span className="text-[10px] font-bold text-emerald-600 flex items-center gap-0.5 shrink-0 ml-auto">
                            <Check className="w-3.5 h-3.5" /> Đúng
                          </span>
                        )}
                      </div>

                    {/* Nếu đáp án có tranh vẽ */}
                    {opt.optionImageUrl && (
                      <div className="p-1 rounded-lg border border-slate-200 bg-white w-full mx-auto overflow-hidden">
                        <img
                          src={opt.optionImageUrl}
                          alt={`Lựa chọn ${opt.optionKey}`}
                          className="max-h-28 object-contain rounded mx-auto cursor-pointer hover:opacity-90 transition"
                          onClick={() => opt.optionImageUrl && onZoomImage?.(opt.optionImageUrl)}
                        />
                      </div>
                    )}
                  </div>
                  )
                })}
              </div>
            )
          })()}

          {/* Lời giải thích */}
          {q.explanationVi && (
            <div className="p-2.5 bg-white border border-slate-200 rounded-xl text-xs text-slate-600">
              <strong className="text-slate-800">Giải thích:</strong> {q.explanationVi}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default AdminExamPreviewPage
