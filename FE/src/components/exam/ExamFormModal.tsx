import React, { useState, useEffect } from 'react'
import {
  X,
  Clock,
  Award,
  BookOpen,
  Layers,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Check,
} from 'lucide-react'
import { examService } from '@/services/exam.service'
import { courseService } from '@/services/course.service'
import { useAuthStore } from '@/stores'
import { isAdmin as checkIsAdmin, isTeacher as checkIsTeacher } from '@/utils/roles'
import { JLPT_LEVEL_BORDER, JLPT_LEVEL_TEXT } from '@/utils/jlpt'
import type { ExamSummary, JlptLevel, ExamType, SessionType, CourseSummary } from '@/types'

interface SectionConfig {
  id?: number
  sessionType: SessionType
  sectionTitle: string
  orderIndex: number
  durationMinutes: number
}

// Cấu hình các phần thi chuẩn theo cấp độ JLPT
const getStandardSections = (level: JlptLevel): SectionConfig[] => {
  if (level === 'N1' || level === 'N2') {
    const d1 = level === 'N1' ? 110 : 105
    const d2 = level === 'N1' ? 60 : 50
    return [
      {
        sessionType: 'KNOWLEDGE_READING',
        sectionTitle: `Phần 1: Kiến thức ngôn ngữ (Từ vựng, Ngữ pháp) & Đọc hiểu`,
        orderIndex: 1,
        durationMinutes: d1,
      },
      {
        sessionType: 'LISTENING',
        sectionTitle: `Phần 2: Nghe hiểu`,
        orderIndex: 2,
        durationMinutes: d2,
      },
    ]
  }

  // N3, N4, N5: 3 phần thi độc lập
  const d1 = level === 'N3' || level === 'N4' ? 30 : 25
  const d2 = level === 'N3' ? 70 : level === 'N4' ? 60 : 50
  const d3 = level === 'N3' ? 40 : level === 'N4' ? 35 : 30
  return [
    {
      sessionType: 'VOCABULARY',
      sectionTitle: `Phần 1: Kiến thức ngôn ngữ (Từ vựng)`,
      orderIndex: 1,
      durationMinutes: d1,
    },
    {
      sessionType: 'GRAMMAR_READING',
      sectionTitle: `Phần 2: Ngữ pháp & Đọc hiểu`,
      orderIndex: 2,
      durationMinutes: d2,
    },
    {
      sessionType: 'LISTENING',
      sectionTitle: `Phần 3: Nghe hiểu`,
      orderIndex: 3,
      durationMinutes: d3,
    },
  ]
}

// Màu cấp độ lấy từ utils/jlpt
const LEVEL_COLORS: Record<JlptLevel, { bg: string; text: string; border: string; desc: string }> = {
  N5: { bg: 'bg-surface', text: JLPT_LEVEL_TEXT.N5, border: JLPT_LEVEL_BORDER.N5, desc: 'Sơ cấp 1 (Cơ bản)' },
  N4: { bg: 'bg-surface', text: JLPT_LEVEL_TEXT.N4, border: JLPT_LEVEL_BORDER.N4, desc: 'Sơ cấp 2 (Giao tiếp cơ bản)' },
  N3: { bg: 'bg-surface', text: JLPT_LEVEL_TEXT.N3, border: JLPT_LEVEL_BORDER.N3, desc: 'Trung cấp (Đời sống)' },
  N2: { bg: 'bg-surface', text: JLPT_LEVEL_TEXT.N2, border: JLPT_LEVEL_BORDER.N2, desc: 'Trung - Cao cấp (Công việc)' },
  N1: { bg: 'bg-surface', text: JLPT_LEVEL_TEXT.N1, border: JLPT_LEVEL_BORDER.N1, desc: 'Cao cấp (Chuyên sâu)' },
}

interface ExamFormModalProps {
  open: boolean
  exam: ExamSummary | null
  onClose: () => void
  onSuccess: (updated: ExamSummary) => void
}

export const ExamFormModal: React.FC<ExamFormModalProps> = ({
  open,
  exam,
  onClose,
  onSuccess,
}) => {
  const user = useAuthStore((s) => s.user)

  // Form states
  const [title, setTitle] = useState('')
  const [jlptLevel, setJlptLevel] = useState<JlptLevel>('N5')
  const [examType, setExamType] = useState<ExamType>('JLPT_OFFICIAL_MOCK')
  const [courseId, setCourseId] = useState<string>('')
  const [sections, setSections] = useState<SectionConfig[]>([])
  const [overallPassingScore, setOverallPassingScore] = useState<number>(90)

  // Khóa học khi chọn MINI_TEST
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [loadingCourses, setLoadingCourses] = useState(false)

  // Trạng thái submit & lỗi
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Tải danh sách khóa học
  useEffect(() => {
    if (!open || examType !== 'MINI_TEST') return

    const fetchCourses = async () => {
      try {
        setLoadingCourses(true)
        const isAdmin = checkIsAdmin(user)
        const isTeacher = checkIsTeacher(user)

        let res
        try {
          res = await courseService.getAllCoursesAdmin({ size: 100 })
        } catch {
          res = await courseService.getCourses({ size: 100 })
        }

        const allCourses: CourseSummary[] = res.data?.items || []
        if (!isAdmin && isTeacher && user?.id) {
          setCourses(allCourses.filter((c) => c.createdByUserId === user.id))
        } else {
          setCourses(allCourses)
        }
      } catch (err) {
        console.error('Lỗi tải danh sách khóa học:', err)
      } finally {
        setLoadingCourses(false)
      }
    }

    fetchCourses()
  }, [open, examType, user])

  // Khởi tạo dữ liệu từ exam hiện tại
  useEffect(() => {
    if (!exam || !open) return

    setTitle(exam.title || '')
    setJlptLevel(exam.jlptLevel || 'N5')
    setExamType(exam.examType || 'JLPT_OFFICIAL_MOCK')
    setCourseId(exam.courseId ? String(exam.courseId) : '')
    setOverallPassingScore(exam.overallPassingScore || exam.passingScore || 90)
    setErrorMessage(null)

    // Tải cấu trúc phần thi thực tế của đề thi
    examService
      .getAdminExamPreview(exam.id)
      .then((preview: any) => {
        const rawSections = preview?.data?.sections || preview?.sections || []
        if (rawSections.length > 0) {
          setSections(
            rawSections.map((s: any, idx: number) => ({
              id: s.id,
              sessionType: s.sessionType,
              sectionTitle: s.sectionTitle,
              orderIndex: s.orderIndex || idx + 1,
              durationMinutes: s.durationMinutes || 30,
            }))
          )
        } else {
          setSections(getStandardSections(exam.jlptLevel || 'N5'))
        }
      })
      .catch(() => {
        setSections(getStandardSections(exam.jlptLevel || 'N5'))
      })
  }, [exam, open])

  if (!open || !exam) return null

  // Khi người dùng bấm đổi Cấp độ (Level)
  const handleLevelChange = (newLevel: JlptLevel) => {
    setJlptLevel(newLevel)
    setSections(getStandardSections(newLevel))
  }

  // Tổng thời lượng
  const totalDurationMinutes = sections.reduce((sum, s) => sum + (s.durationMinutes || 0), 0)

  // Gợi ý tiêu đề mẫu
  const handleSuggestTitle = () => {
    const now = new Date()
    const month = now.getMonth() >= 6 ? '12' : '07'
    const year = now.getFullYear()
    setTitle(`Đề thi thử JLPT ${jlptLevel} - Kỳ thi Tháng ${month}/${year}`)
  }

  // Đổi số phút từng phần
  const handleSectionDurationChange = (idx: number, mins: number) => {
    const updated = [...sections]
    updated[idx] = { ...updated[idx], durationMinutes: Math.max(5, mins) }
    setSections(updated)
  }

  // Submit cập nhật thông tin đề thi
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    const trimmedTitle = title.trim()
    if (!trimmedTitle) {
      setErrorMessage('Vui lòng nhập tiêu đề đề thi.')
      return
    }

    if (examType === 'MINI_TEST' && !courseId) {
      setErrorMessage('Vui lòng chọn khóa học áp dụng cho bài kiểm tra này.')
      return
    }

    try {
      setIsSubmitting(true)
      const updated = await examService.updateExamBasic(exam.id, {
        title: trimmedTitle,
        jlptLevel,
        examType,
        totalDurationMinutes,
        overallPassingScore,
        courseId: examType === 'MINI_TEST' && courseId ? Number(courseId) : null,
        sections: sections.map((sec) => ({
          id: sec.id,
          sessionType: sec.sessionType,
          sectionTitle: sec.sectionTitle,
          durationMinutes: sec.durationMinutes,
        })),
      } as any)

      onSuccess(updated)
      onClose()
    } catch (err: any) {
      console.error('Lỗi cập nhật đề thi:', err)
      setErrorMessage(
        err?.response?.data?.message || err?.message || 'Không thể cập nhật đề thi. Vui lòng kiểm tra lại.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 overscroll-contain z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-3 sm:p-5">
      <div className="bg-surface rounded-2xl max-w-4xl w-full max-h-[92vh] overflow-y-auto shadow-2xl border border-suna animate-in fade-in zoom-in-95 duration-200 text-sumi flex flex-col">
        {/* Modal Header */}
        <div className="sticky top-0 z-10 bg-surface/95 backdrop-blur-xs px-5 py-4 border-b border-suna flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-indigo-50 border border-indigo-200/80 text-indigo-600 flex items-center justify-center">
              <Award className="w-4 h-4 text-indigo-600" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Chỉnh sửa thông tin đề thi #{exam.id}
              </h3>
              <p className="text-xs text-slate-500">
                Cập nhật cấp độ JLPT, loại bài thi và cấu trúc thời lượng các phần thi
              </p>
            </div>
          </div>

          <button
            type="button"
            disabled={isSubmitting}
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Content Form */}
        <form onSubmit={handleSubmit} className="p-5 sm:p-6 space-y-5">
          {/* Thông báo lỗi nếu có */}
          {errorMessage && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs sm:text-sm flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="space-y-0.5">
                <p className="font-bold">Đã xảy ra lỗi:</p>
                <p>{errorMessage}</p>
              </div>
            </div>
          )}

          {/* KHỐI 1: Thông tin cơ bản đề thi (Chuẩn thiết kế AdminExamCreatePage) */}
          <div className="bg-surface rounded-xl p-5 border border-suna shadow-xs space-y-5">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2 pb-3 border-b border-slate-100">
              <Award className="w-4 h-4 text-indigo-600" />
              Thông tin đề thi
            </h2>

            {/* Cấp độ JLPT */}
            <div className="space-y-2.5">
              <label className="block text-sm font-bold text-slate-800">
                Cấp độ <span className="text-rose-500">*</span>
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                {(['N5', 'N4', 'N3', 'N2', 'N1'] as JlptLevel[]).map((level) => {
                  const isSelected = jlptLevel === level
                  const meta = LEVEL_COLORS[level]
                  return (
                    <button
                      key={level}
                      type="button"
                      onClick={() => handleLevelChange(level)}
                      className={`relative p-3 rounded-lg border-2 text-left transition-all cursor-pointer flex flex-col justify-between ${
                        isSelected
                          ? `${meta.border} ${meta.bg} shadow-xs`
                          : 'border-slate-200 hover:border-slate-300 bg-surface'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className={`text-lg font-black ${isSelected ? meta.text : 'text-slate-800'}`}>
                          {level}
                        </span>
                        {isSelected && <CheckCircle2 className="w-4 h-4 text-indigo-600" />}
                      </div>
                      <span className="text-[11px] text-slate-500 line-clamp-1">
                        {meta.desc}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Tiêu đề đề thi */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="block text-sm font-bold text-slate-800">
                  Tiêu đề đề thi <span className="text-rose-500">*</span>
                </label>
                <button
                  type="button"
                  onClick={handleSuggestTitle}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 flex items-center gap-1 cursor-pointer"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  Gợi ý tên mẫu
                </button>
              </div>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={`Ví dụ: Đề thi thử JLPT ${jlptLevel} - Đề số 1`}
                required
                className="w-full px-3.5 py-2 rounded-lg border border-slate-300 bg-surface text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 text-sm font-medium transition"
              />
            </div>

            {/* Loại đề thi */}
            <div className="space-y-2.5">
              <label className="block text-sm font-bold text-slate-800">
                Loại đề thi <span className="text-rose-500">*</span>
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setExamType('JLPT_OFFICIAL_MOCK')}
                  className={`p-4 rounded-xl border-2 text-left transition cursor-pointer flex items-start gap-3 ${
                    examType === 'JLPT_OFFICIAL_MOCK'
                      ? 'border-indigo-600 bg-indigo-50/50 shadow-xs'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <BookOpen
                    className={`w-5 h-5 shrink-0 mt-0.5 ${
                      examType === 'JLPT_OFFICIAL_MOCK' ? 'text-indigo-600' : 'text-slate-400'
                    }`}
                  />
                  <div>
                    <p className="text-sm font-bold text-slate-900">
                      Đề thi thử JLPT chính thức
                    </p>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Đầy đủ các phần thi theo chuẩn JLPT Nhật Bản, dùng cho kỳ thi thử công khai hoặc toàn hệ thống.
                    </p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setExamType('MINI_TEST')}
                  className={`p-4 rounded-xl border-2 text-left transition cursor-pointer flex items-start gap-3 ${
                    examType === 'MINI_TEST'
                      ? 'border-indigo-600 bg-indigo-50/50 shadow-xs'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <Layers
                    className={`w-5 h-5 shrink-0 mt-0.5 ${
                      examType === 'MINI_TEST' ? 'text-indigo-600' : 'text-slate-400'
                    }`}
                  />
                  <div>
                    <p className="text-sm font-bold text-slate-900">
                      Bài kiểm tra theo khóa học
                    </p>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Gắn trực tiếp vào một khóa học cụ thể, chỉ học viên đã tham gia khóa học mới được làm bài.
                    </p>
                  </div>
                </button>
              </div>
            </div>

            {/* Chọn khóa học nếu là MINI_TEST */}
            {examType === 'MINI_TEST' && (
              <div className="space-y-2 p-4 rounded-xl bg-slate-50 border border-slate-200">
                <label className="block text-sm font-bold text-slate-800">
                  Khóa học áp dụng bài thi này <span className="text-rose-500">*</span>
                </label>
                {loadingCourses ? (
                  <p className="text-xs text-slate-500">Đang tải danh sách khóa học...</p>
                ) : courses.length === 0 ? (
                  <p className="text-xs text-amber-600">
                    Bạn chưa có khóa học nào hoặc không có quyền gán đề thi.
                  </p>
                ) : (
                  <select
                    value={courseId}
                    onChange={(e) => setCourseId(e.target.value)}
                    required
                    className="w-full px-3 py-2.5 rounded-xl border border-slate-300 bg-surface text-sm font-medium focus:ring-2 focus:ring-indigo-500/20"
                  >
                    <option value="">-- Chọn khóa học --</option>
                    {courses.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.title} (Cấp độ {c.jlptLevel})
                      </option>
                    ))}
                  </select>
                )}
              </div>
            )}
          </div>

          {/* KHỐI 2: Cấu trúc các phần thi tự động khởi tạo / cập nhật (Chuẩn thiết kế AdminExamCreatePage) */}
          <div className="bg-surface rounded-xl p-5 border border-suna shadow-xs space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="space-y-0.5">
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Layers className="w-4 h-4 text-indigo-600" />
                  Cấu trúc các phần thi & thời lượng
                </h2>
                <p className="text-xs text-slate-500">
                  Hệ thống thiết lập {sections.length} phần thi theo chuẩn JLPT {jlptLevel}. Bạn có thể tùy chỉnh thời lượng từng phần.
                </p>
              </div>

              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 text-slate-700 text-xs font-bold shrink-0">
                <Clock className="w-4 h-4 text-slate-500" />
                <span>Tổng: {totalDurationMinutes} phút</span>
              </div>
            </div>

            <div className="space-y-3">
              {sections.map((sec, idx) => (
                <div
                  key={sec.sessionType}
                  className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="flex items-center gap-3">
                    <span className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-700 font-bold text-xs flex items-center justify-center shrink-0">
                      {idx + 1}
                    </span>
                    <div>
                      <p className="text-sm font-bold text-slate-900">
                        {sec.sectionTitle}
                      </p>
                      <p className="text-xs text-slate-500">
                        Mã phần thi:{' '}
                        <code className="font-mono text-indigo-600">
                          {sec.sessionType}
                        </code>
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 self-end sm:self-auto">
                    <label className="text-xs text-slate-500 font-medium">Thời lượng:</label>
                    <div className="flex items-center gap-1">
                      <input
                        type="number"
                        min="5"
                        max="180"
                        value={sec.durationMinutes}
                        onChange={(e) =>
                          handleSectionDurationChange(idx, Number(e.target.value))
                        }
                        className="w-16 px-2.5 py-1 text-center font-bold text-sm rounded-lg border border-slate-300 bg-surface text-slate-900"
                      />
                      <span className="text-xs text-slate-500">phút</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="p-3.5 rounded-xl bg-indigo-50/60 border border-indigo-100 text-xs text-indigo-800 flex items-center gap-2">
              <HelpCircle className="w-4 h-4 text-indigo-600 shrink-0" />
              <span>
                Sau khi bấm <strong>"Lưu thay đổi"</strong>, cấu trúc thời lượng các phần thi và thông tin đề sẽ được cập nhật đồng bộ. Bạn có thể bấm vào đề thi để soạn chi tiết câu hỏi.
              </span>
            </div>
          </div>

          {/* Modal Footer Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-lg border border-suna text-slate-700 font-bold text-sm hover:bg-slate-100 transition cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm transition shadow-xs flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Đang lưu thay đổi...</span>
                </>
              ) : (
                <>
                  <span>Lưu thay đổi</span>
                  <Check className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
