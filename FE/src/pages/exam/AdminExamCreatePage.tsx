import React, { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { examService } from '@/services/exam.service'
import type { JlptLevel, SessionType, CourseSummary } from '@/types'
import { courseService } from '@/services/course.service'
import { useAuthStore } from '@/stores'
import {
  FileText,
  Sparkles,
  ArrowRight,
  Clock,
  Layers,
  BookOpen,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Award,
} from 'lucide-react'

interface InitialSectionConfig {
  sessionType: SessionType
  sectionTitle: string
  orderIndex: number
  durationMinutes: number
}

// Cấu hình các phần thi chuẩn theo cấp độ JLPT
const getStandardSections = (level: JlptLevel): InitialSectionConfig[] => {
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

const LEVEL_COLORS: Record<JlptLevel, { bg: string; text: string; border: string; desc: string }> = {
  N5: { bg: 'bg-emerald-50 dark:bg-emerald-950/30', text: 'text-emerald-700 dark:text-emerald-400', border: 'border-emerald-500', desc: 'Sơ cấp 1 (Cơ bản)' },
  N4: { bg: 'bg-blue-50 dark:bg-blue-950/30', text: 'text-blue-700 dark:text-blue-400', border: 'border-blue-500', desc: 'Sơ cấp 2 (Giao tiếp cơ bản)' },
  N3: { bg: 'bg-indigo-50 dark:bg-indigo-950/30', text: 'text-indigo-700 dark:text-indigo-400', border: 'border-indigo-500', desc: 'Trung cấp (Đời sống)' },
  N2: { bg: 'bg-purple-50 dark:bg-purple-950/30', text: 'text-purple-700 dark:text-purple-400', border: 'border-purple-500', desc: 'Trung - Cao cấp (Công việc)' },
  N1: { bg: 'bg-rose-50 dark:bg-rose-950/30', text: 'text-rose-700 dark:text-rose-400', border: 'border-rose-500', desc: 'Cao cấp (Chuyên sâu)' },
}

export const AdminExamCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const urlCourseId = searchParams.get('courseId')
  const urlLevel = searchParams.get('level') as JlptLevel | null

  // 1. Thông tin cơ bản đề thi
  const [title, setTitle] = useState(urlCourseId ? `Bài kiểm tra khóa học - JLPT ${urlLevel || 'N5'}` : '')
  const [jlptLevel, setJlptLevel] = useState<JlptLevel>(urlLevel || 'N4')
  const [examType, setExamType] = useState<'JLPT_OFFICIAL_MOCK' | 'MINI_TEST'>(
    urlCourseId ? 'MINI_TEST' : 'JLPT_OFFICIAL_MOCK'
  )
  const [courseId, setCourseId] = useState<string>(urlCourseId || '')

  // 2. Cấu hình phần thi khởi tạo
  const [sections, setSections] = useState<InitialSectionConfig[]>(() => getStandardSections(urlLevel || 'N4'))

  // 3. User & Courses cho chế độ MINI_TEST
  const user = useAuthStore((s) => s.user)
  const [courses, setCourses] = useState<CourseSummary[]>([])
  const [loadingCourses, setLoadingCourses] = useState(false)

  // 4. Trạng thái submit
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Cập nhật cấu hình phần thi khi đổi Level
  useEffect(() => {
    setSections(getStandardSections(jlptLevel))
  }, [jlptLevel])

  // Tải danh sách khóa học nếu chọn MINI_TEST
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
        if (!isAdmin && isTeacher && user?.id) {
          setCourses(allCourses.filter((c) => c.createdByUserId === user.id))
        } else {
          setCourses(allCourses)
        }
      } catch (err) {
        console.error('Lỗi tải khóa học:', err)
      } finally {
        setLoadingCourses(false)
      }
    }

    fetchCourses()
  }, [examType, user])

  // Tổng thời gian
  const totalDurationMinutes = sections.reduce((sum, s) => sum + (s.durationMinutes || 0), 0)

  // Gợi ý tiêu đề nhanh
  const handleSuggestTitle = () => {
    const now = new Date()
    const month = now.getMonth() >= 6 ? '12' : '07'
    const year = now.getFullYear()
    setTitle(`Đề thi thử JLPT ${jlptLevel} - Kỳ thi Tháng ${month}/${year}`)
  }

  // Cập nhật số phút của từng phần
  const handleSectionDurationChange = (idx: number, mins: number) => {
    const updated = [...sections]
    updated[idx] = { ...updated[idx], durationMinutes: Math.max(5, mins) }
    setSections(updated)
  }

  // Submit tạo đề thi
  const handleCreateExam = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const trimmedTitle = title.trim()
    if (!trimmedTitle) {
      setError('Vui lòng nhập tiêu đề đề thi.')
      return
    }

    if (examType === 'MINI_TEST' && !courseId) {
      setError('Vui lòng chọn khóa học áp dụng cho bài kiểm tra này.')
      return
    }

    try {
      setIsSubmitting(true)

      const payload = {
        title: trimmedTitle,
        jlptLevel,
        examType,
        totalDurationMinutes,
        sourceFileName: 'Manual_Or_AI_Editor_Flow',
        courseId: examType === 'MINI_TEST' && courseId ? Number(courseId) : undefined,
        sections: sections.map((sec, sIdx) => ({
          sessionType: sec.sessionType,
          sectionTitle: `${sec.sectionTitle} (${sec.durationMinutes} phút)`,
          orderIndex: sIdx + 1,
          durationMinutes: sec.durationMinutes,
          questions: [], // Khởi tạo rỗng để vào trang soạn thảo điền bằng tay hoặc quét AI
        })),
      }

      const newExamId = await examService.importExam(payload)
      navigate(`/admin/exams/${newExamId}/preview`)
    } catch (err: any) {
      console.error('Lỗi tạo đề thi:', err)
      setError(err.response?.data?.message || err.message || 'Không thể tạo đề thi. Vui lòng kiểm tra lại.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50/70 dark:bg-slate-950 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header điều hướng */}
        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => navigate('/admin/exams')}
            className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white transition"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại danh sách đề thi</span>
          </button>

          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
            <Sparkles className="w-3.5 h-3.5" />
            Khởi tạo nhanh & Soạn thảo linh hoạt
          </span>
        </div>

        {/* Tiêu đề trang */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800 shadow-sm">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 rounded-2xl bg-linear-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white shadow-md shadow-indigo-500/20 shrink-0">
              <FileText className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
                Tạo Đề Thi Mới
              </h1>
              <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
                Nhập thông tin cơ bản để khởi tạo cấu trúc bài thi chuẩn JLPT. Sau khi khởi tạo, bạn có thể tự do{' '}
                <strong className="text-slate-700 dark:text-slate-200">soạn câu hỏi bằng tay</strong> hoặc{' '}
                <strong className="text-indigo-600 dark:text-indigo-400">sử dụng AI quét tự động</strong> ngay trong phòng biên soạn.
              </p>
            </div>
          </div>
        </div>

        {/* Thông báo lỗi nếu có */}
        {error && (
          <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-rose-700 dark:text-rose-300 text-sm flex items-start gap-3 shadow-xs">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5 text-rose-600 dark:text-rose-400" />
            <div className="space-y-1">
              <p className="font-semibold">Đã xảy ra lỗi:</p>
              <p>{error}</p>
            </div>
          </div>
        )}

        {/* Form nhập thông tin cơ bản */}
        <form onSubmit={handleCreateExam} className="space-y-6">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800 shadow-sm space-y-6">
            <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2 pb-3 border-b border-slate-100 dark:border-slate-800">
              <Award className="w-5 h-5 text-indigo-600" />
              Thông tin đề thi
            </h2>

            {/* Cấp độ JLPT */}
            <div className="space-y-2.5">
              <label className="block text-sm font-bold text-slate-800 dark:text-slate-200">
                Cấp độ JLPT <span className="text-rose-500">*</span>
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                {(['N5', 'N4', 'N3', 'N2', 'N1'] as JlptLevel[]).map((level) => {
                  const isSelected = jlptLevel === level
                  const meta = LEVEL_COLORS[level]
                  return (
                    <button
                      key={level}
                      type="button"
                      onClick={() => setJlptLevel(level)}
                      className={`relative p-3.5 rounded-xl border-2 text-left transition-all cursor-pointer flex flex-col justify-between ${
                        isSelected
                          ? `${meta.border} ${meta.bg} shadow-md shadow-indigo-500/10 scale-[1.02]`
                          : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-white dark:bg-slate-900'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className={`text-lg font-black ${isSelected ? meta.text : 'text-slate-800 dark:text-slate-200'}`}>
                          {level}
                        </span>
                        {isSelected && <CheckCircle2 className="w-4 h-4 text-indigo-600" />}
                      </div>
                      <span className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-1">
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
                <label className="block text-sm font-bold text-slate-800 dark:text-slate-200">
                  Tiêu đề đề thi <span className="text-rose-500">*</span>
                </label>
                <button
                  type="button"
                  onClick={handleSuggestTitle}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 flex items-center gap-1 cursor-pointer"
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
                className="w-full px-4 py-3 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 text-sm font-medium transition"
              />
            </div>

            {/* Loại đề thi */}
            <div className="space-y-2.5">
              <label className="block text-sm font-bold text-slate-800 dark:text-slate-200">
                Loại đề thi <span className="text-rose-500">*</span>
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setExamType('JLPT_OFFICIAL_MOCK')}
                  className={`p-4 rounded-xl border-2 text-left transition cursor-pointer flex items-start gap-3 ${
                    examType === 'JLPT_OFFICIAL_MOCK'
                      ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/20 shadow-xs'
                      : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
                  }`}
                >
                  <BookOpen className={`w-5 h-5 shrink-0 mt-0.5 ${examType === 'JLPT_OFFICIAL_MOCK' ? 'text-indigo-600' : 'text-slate-400'}`} />
                  <div>
                    <p className="text-sm font-bold text-slate-900 dark:text-white">
                      Đề thi thử JLPT chính thức
                    </p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                      Đầy đủ các phần thi theo chuẩn JLPT Nhật Bản, dùng cho kỳ thi thử công khai hoặc toàn hệ thống.
                    </p>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setExamType('MINI_TEST')}
                  className={`p-4 rounded-xl border-2 text-left transition cursor-pointer flex items-start gap-3 ${
                    examType === 'MINI_TEST'
                      ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/20 shadow-xs'
                      : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
                  }`}
                >
                  <Layers className={`w-5 h-5 shrink-0 mt-0.5 ${examType === 'MINI_TEST' ? 'text-indigo-600' : 'text-slate-400'}`} />
                  <div>
                    <p className="text-sm font-bold text-slate-900 dark:text-white">
                      Bài kiểm tra theo khóa học
                    </p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                      Gắn trực tiếp vào một khóa học cụ thể, chỉ học viên đã tham gia khóa học mới được làm bài.
                    </p>
                  </div>
                </button>
              </div>
            </div>

            {/* Chọn khóa học nếu là MINI_TEST */}
            {examType === 'MINI_TEST' && (
              <div className="space-y-2 p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700">
                <label className="block text-sm font-bold text-slate-800 dark:text-slate-200">
                  Khóa học áp dụng bài thi này <span className="text-rose-500">*</span>
                </label>
                {loadingCourses ? (
                  <p className="text-xs text-slate-500">Đang tải danh sách khóa học...</p>
                ) : courses.length === 0 ? (
                  <p className="text-xs text-amber-600">Bạn chưa có khóa học nào hoặc không có quyền gán đề thi.</p>
                ) : (
                  <select
                    value={courseId}
                    onChange={(e) => setCourseId(e.target.value)}
                    required
                    className="w-full px-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-sm font-medium focus:ring-2 focus:ring-indigo-500/20"
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

          {/* Cấu trúc phần thi sẽ khởi tạo */}
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800 shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
              <div className="space-y-0.5">
                <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Layers className="w-5 h-5 text-indigo-600" />
                  Cấu trúc các phần thi tự động khởi tạo
                </h2>
                <p className="text-xs text-slate-500">
                  Hệ thống tự động tạo sẵn {sections.length} phần thi chuẩn theo quy định của JLPT {jlptLevel}. Bạn có thể tùy chỉnh thời lượng từng phần.
                </p>
              </div>

              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 text-xs font-bold shrink-0">
                <Clock className="w-4 h-4 text-slate-500" />
                <span>Tổng: {totalDurationMinutes} phút</span>
              </div>
            </div>

            <div className="space-y-3">
              {sections.map((sec, idx) => (
                <div
                  key={sec.sessionType}
                  className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="flex items-center gap-3">
                    <span className="w-7 h-7 rounded-lg bg-indigo-100 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 font-bold text-xs flex items-center justify-center shrink-0">
                      {idx + 1}
                    </span>
                    <div>
                      <p className="text-sm font-bold text-slate-900 dark:text-white">
                        {sec.sectionTitle}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        Mã phần thi: <code className="font-mono text-indigo-600 dark:text-indigo-400">{sec.sessionType}</code>
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
                        onChange={(e) => handleSectionDurationChange(idx, Number(e.target.value))}
                        className="w-16 px-2.5 py-1 text-center font-bold text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white"
                      />
                      <span className="text-xs text-slate-500">phút</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="p-3.5 rounded-xl bg-indigo-50/60 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/50 text-xs text-indigo-800 dark:text-indigo-300 flex items-center gap-2">
              <HelpCircle className="w-4 h-4 text-indigo-600 shrink-0" />
              <span>
                Sau khi bấm <strong>"Khởi tạo đề thi"</strong>, bạn sẽ được chuyển ngay sang giao diện biên soạn chi tiết. Tại đó, mỗi phần thi đều có sẵn nút <strong>"Thêm câu hỏi bằng tay"</strong>, <strong>"Quét câu hỏi bằng AI"</strong>, và <strong>"Nạp đáp án 正答表"</strong>.
              </span>
            </div>
          </div>

          {/* Hành động chính */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => navigate('/admin/exams')}
              disabled={isSubmitting}
              className="px-5 py-3 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-bold text-sm hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer disabled:opacity-50"
            >
              Hủy
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-3 rounded-xl bg-linear-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-bold text-sm transition shadow-md shadow-indigo-500/20 flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Đang khởi tạo đề thi...</span>
                </>
              ) : (
                <>
                  <span>Khởi tạo đề thi & Bắt đầu soạn nội dung</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AdminExamCreatePage
