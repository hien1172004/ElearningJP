import { useBodyScrollLock } from '@/hooks/useBodyScrollLock'
import React, { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  FileText,
  Clock,
  Sparkles,
  BookOpen,
  CheckCircle2,
  ExternalLink,
  Edit3,
  Search,
  Grid3X3,
  List as ListIcon,
  Trash2,
  Send,
  Eye,
  EyeOff,
  Copy,
  AlertTriangle,
  RotateCw,
  Award,
  Check,
  X,
  Plus,
  GraduationCap,
  ChevronDown,
} from 'lucide-react'
import { examService } from '@/services'
import type { ExamSummary } from '@/types'

// Dải màu định danh cho từng cấp độ JLPT
const JLPT_BADGES: Record<
  string,
  { bg: string; text: string; border: string; label: string }
> = {
  N1: {
    bg: 'bg-rose-50 text-rose-700',
    text: 'text-rose-700',
    border: 'border-rose-200',
    label: 'N1 • Thượng cấp',
  },
  N2: {
    bg: 'bg-purple-50 text-purple-700',
    text: 'text-purple-700',
    border: 'border-purple-200',
    label: 'N2 • Trung cao cấp',
  },
  N3: {
    bg: 'bg-blue-50 text-blue-700',
    text: 'text-blue-700',
    border: 'border-blue-200',
    label: 'N3 • Trung cấp',
  },
  N4: {
    bg: 'bg-emerald-50 text-emerald-700',
    text: 'text-emerald-700',
    border: 'border-emerald-200',
    label: 'N4 • Sơ trung cấp',
  },
  N5: {
    bg: 'bg-amber-50 text-amber-700',
    text: 'text-amber-700',
    border: 'border-amber-200',
    label: 'N5 • Sơ cấp khởi đầu',
  },
}

export const AdminExamsPage: React.FC = () => {
  const navigate = useNavigate()

  // State dữ liệu
  const [exams, setExams] = useState<ExamSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)

  // Bộ lọc & Tìm kiếm
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedLevel, setSelectedLevel] = useState<string>('ALL')
  const [selectedStatus, setSelectedStatus] = useState<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL')
  const [selectedExamType, setSelectedExamType] = useState<string>('ALL')
  const [sortBy, setSortBy] = useState<
    'NEWEST' | 'OLDEST' | 'TITLE_ASC' | 'TITLE_DESC' | 'DURATION_DESC' | 'QUESTIONS_DESC'
  >('NEWEST')
  const [viewMode, setViewMode] = useState<'GRID' | 'TABLE'>('GRID')

  // Trạng thái thao tác
  const [publishingId, setPublishingId] = useState<number | null>(null)
  const [cloningId, setCloningId] = useState<number | null>(null)
  const [deleteModalExam, setDeleteModalExam] = useState<ExamSummary | null>(null)
  // Khoá cuộn trang phía sau khi lớp phủ tự dựng đang mở
  useBodyScrollLock(!!deleteModalExam)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [copiedId, setCopiedId] = useState<number | null>(null)
  const [toastMessage, setToastMessage] = useState<{ text: string; type: 'success' | 'info' | 'error' } | null>(null)

  // Tự ẩn toast sau 3.5s
  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(null), 3500)
      return () => clearTimeout(timer)
    }
  }, [toastMessage])

  // Lấy dữ liệu đề thi
  const fetchExams = async (showRefreshIndicator = false) => {
    try {
      if (showRefreshIndicator) setRefreshing(true)
      else setLoading(true)

      let items: ExamSummary[] = []
      try {
        const response: any = await examService.getAllAdminExams({ page: 0, size: 100 })
        items = response?.data?.content || response?.content || []
      } catch (adminErr) {
        const response: any = await examService.getPublishedExams({ page: 0, size: 100 })
        items = response?.data?.content || response?.content || []
      }

      setExams(items)
    } catch (err) {
      console.error('Failed to load exams for admin dashboard:', err)
      setToastMessage({ text: 'Không thể tải danh sách đề thi. Vui lòng thử lại!', type: 'error' })
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    fetchExams()
  }, [])

  // Thao tác Xuất bản đề thi
  const handlePublishExam = async (examId: number, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      setPublishingId(examId)
      await examService.publishExam(examId)
      setToastMessage({ text: 'Đã xuất bản đề thi thành công! Học viên đã có thể thi thử.', type: 'success' })
      setExams((prev) =>
        prev.map((ex) => (ex.id === examId ? { ...ex, published: true, status: 'PUBLISHED' } : ex))
      )
    } catch (err: any) {
      console.error('Failed to publish exam:', err)
      setToastMessage({
        text: err?.response?.data?.message || 'Có lỗi khi xuất bản đề thi.',
        type: 'error',
      })
    } finally {
      setPublishingId(null)
    }
  }

  // Thao tác Thu hồi đề thi
  const handleUnpublishExam = async (examId: number, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      setPublishingId(examId)
      await examService.unpublishExam(examId)
      setToastMessage({ text: 'Đã thu hồi đề thi về bản nháp (DRAFT). Học viên sẽ không nhìn thấy đề này.', type: 'info' })
      setExams((prev) =>
        prev.map((ex) => (ex.id === examId ? { ...ex, published: false, status: 'DRAFT' } : ex))
      )
    } catch (err: any) {
      console.error('Failed to unpublish exam:', err)
      setToastMessage({
        text: err?.response?.data?.message || 'Có lỗi khi thu hồi đề thi.',
        type: 'error',
      })
    } finally {
      setPublishingId(null)
    }
  }

  // Thao tác Nhân bản đề thi (Clone)
  const handleCloneExam = async (examId: number, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      setCloningId(examId)
      const newExamId = await examService.cloneExam(examId)
      setToastMessage({ text: 'Nhân bản đề thi thành công!', type: 'success' })
      await fetchExams()
      navigate(`/admin/exams/${newExamId}/preview`)
    } catch (err: any) {
      console.error('Failed to clone exam:', err)
      setToastMessage({ text: err?.response?.data?.message || 'Không thể nhân bản đề thi.', type: 'error' })
    } finally {
      setCloningId(null)
    }
  }

  // Thao tác Xóa đề thi
  const confirmDeleteExam = async () => {
    if (!deleteModalExam) return
    try {
      setDeletingId(deleteModalExam.id)
      await examService.deleteExam(deleteModalExam.id)
      setToastMessage({ text: `Đã xóa đề thi "${deleteModalExam.title}" thành công.`, type: 'success' })
      setExams((prev) => prev.filter((ex) => ex.id !== deleteModalExam.id))
      setDeleteModalExam(null)
    } catch (err: any) {
      console.error('Failed to delete exam:', err)
      setToastMessage({
        text: err?.response?.data?.message || 'Không thể xóa đề thi lúc này (có thể do đang có thí sinh thi).',
        type: 'error',
      })
    } finally {
      setDeletingId(null)
    }
  }

  // Sao chép liên kết đề thi
  const handleCopyLink = (examId: number, e: React.MouseEvent) => {
    e.stopPropagation()
    const url = `${window.location.origin}/exam/lobby/${examId}`
    navigator.clipboard.writeText(url)
    setCopiedId(examId)
    setToastMessage({ text: 'Đã sao chép link đề thi vào bộ nhớ tạm!', type: 'info' })
    setTimeout(() => setCopiedId(null), 2000)
  }

  // Thống kê nhanh
  const stats = useMemo(() => {
    const total = exams.length
    const published = exams.filter((e) => e.published === true || e.status === 'PUBLISHED').length
    const draft = total - published

    const levelCounts: Record<string, number> = { N1: 0, N2: 0, N3: 0, N4: 0, N5: 0 }
    exams.forEach((e) => {
      if (e.jlptLevel && levelCounts[e.jlptLevel] !== undefined) {
        levelCounts[e.jlptLevel]++
      }
    })

    return { total, published, draft, levelCounts }
  }, [exams])

  // Lọc và Sắp xếp danh sách đề
  const filteredExams = useMemo(() => {
    let result = [...exams]

    // Lọc tìm kiếm từ khóa
    if (searchTerm.trim()) {
      const q = searchTerm.toLowerCase().trim()
      result = result.filter(
        (e) =>
          e.title.toLowerCase().includes(q) ||
          e.id.toString().includes(q) ||
          e.jlptLevel.toLowerCase().includes(q) ||
          (e.courseId && e.courseId.toString().includes(q))
      )
    }

    // Lọc theo Cấp độ JLPT
    if (selectedLevel !== 'ALL') {
      result = result.filter((e) => e.jlptLevel === selectedLevel)
    }

    // Lọc theo Trạng thái xuất bản
    if (selectedStatus === 'PUBLISHED') {
      result = result.filter((e) => e.published === true || e.status === 'PUBLISHED')
    } else if (selectedStatus === 'DRAFT') {
      result = result.filter((e) => e.published === false || e.status === 'DRAFT')
    }

    // Lọc theo Loại bài thi
    if (selectedExamType !== 'ALL') {
      result = result.filter((e) => e.examType === selectedExamType)
    }

    // Sắp xếp
    result.sort((a, b) => {
      if (sortBy === 'NEWEST') return b.id - a.id
      if (sortBy === 'OLDEST') return a.id - b.id
      if (sortBy === 'TITLE_ASC') return a.title.localeCompare(b.title, 'vi')
      if (sortBy === 'TITLE_DESC') return b.title.localeCompare(a.title, 'vi')
      if (sortBy === 'DURATION_DESC') return b.totalDurationMinutes - a.totalDurationMinutes
      if (sortBy === 'QUESTIONS_DESC') return b.totalQuestions - a.totalQuestions
      return 0
    })

    return result
  }, [exams, searchTerm, selectedLevel, selectedStatus, selectedExamType, sortBy])

  return (
    <div className="space-y-5 pb-20 font-sans text-slate-900">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-in fade-in slide-in-from-bottom-5 duration-200">
          <div
            className={`px-4 py-3 rounded-2xl shadow-xl border text-xs sm:text-sm font-semibold flex items-center gap-3 backdrop-blur-md ${
              toastMessage.type === 'success'
                ? 'bg-slate-900 text-white border-slate-700'
                : toastMessage.type === 'error'
                ? 'bg-rose-900/95 text-rose-100 border-rose-700'
                : 'bg-indigo-950 text-indigo-100 border-indigo-800'
            }`}
          >
            {toastMessage.type === 'success' && <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />}
            {toastMessage.type === 'error' && <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />}
            {toastMessage.type === 'info' && <Sparkles className="w-4 h-4 text-indigo-400 shrink-0" />}
            <span>{toastMessage.text}</span>
            <button
              onClick={() => setToastMessage(null)}
              className="text-white/60 hover:text-white ml-2 cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* 1. Header & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-1">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Quản lý Đề thi JLPT
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Biên tập nội dung, đóng gói Snapshot bất biến và phát hành các bộ đề thi N1 ~ N5 chuẩn JLPT.
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => fetchExams(true)}
            disabled={refreshing}
            title="Làm mới danh sách"
            className="p-2 border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 rounded-lg transition shadow-2xs cursor-pointer disabled:opacity-50"
          >
            <RotateCw className={`w-4 h-4 ${refreshing ? 'animate-spin text-indigo-600' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => navigate('/exams', { state: { backTo: '/admin/exams' } })}
            className="px-3 py-2 border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-medium transition shadow-2xs flex items-center gap-1.5 cursor-pointer"
          >
            <Eye className="w-3.5 h-3.5 text-slate-500" />
            <span>Phòng thi học viên</span>
            <ExternalLink className="w-3 h-3 text-slate-400" />
          </button>

          <button
            type="button"
            onClick={() => navigate('/admin/exams/create')}
            className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white rounded-lg text-xs font-semibold shadow-xs transition flex items-center gap-1.5 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo đề thi mới</span>
          </button>
        </div>
      </div>

      {/* 2. COMPACT KPI SUMMARY CARDS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* KPI 1: Tổng số đề */}
        <div
          onClick={() => {
            setSelectedStatus('ALL')
            setSelectedLevel('ALL')
          }}
          className={`p-4 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
            selectedStatus === 'ALL' && selectedLevel === 'ALL'
              ? 'bg-white border-indigo-500 ring-2 ring-indigo-500/10 shadow-xs'
              : 'bg-white border-slate-200/80 hover:border-slate-300 shadow-2xs'
          }`}
        >
          <div>
            <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Tổng số đề thi</div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.total}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">Tất cả trong kho đề</div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-slate-100 text-slate-600 flex items-center justify-center shrink-0">
            <FileText className="w-5 h-5" />
          </div>
        </div>

        {/* KPI 2: Đang phát hành */}
        <div
          onClick={() => setSelectedStatus('PUBLISHED')}
          className={`p-4 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
            selectedStatus === 'PUBLISHED'
              ? 'bg-emerald-50/40 border-emerald-500 ring-2 ring-emerald-500/10 shadow-xs'
              : 'bg-white border-slate-200/80 hover:border-emerald-300 shadow-2xs'
          }`}
        >
          <div>
            <div className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              Đang phát hành
            </div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.published}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">Học viên có thể thi thử</div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        </div>

        {/* KPI 3: Bản nháp */}
        <div
          onClick={() => setSelectedStatus('DRAFT')}
          className={`p-4 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
            selectedStatus === 'DRAFT'
              ? 'bg-amber-50/40 border-amber-500 ring-2 ring-amber-500/10 shadow-xs'
              : 'bg-white border-slate-200/80 hover:border-amber-300 shadow-2xs'
          }`}
        >
          <div>
            <div className="text-[11px] font-semibold text-amber-700 uppercase tracking-wider">
              Bản nháp đang soạn
            </div>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.draft}</div>
            <div className="text-[11px] text-slate-400 mt-0.5">Chờ đóng gói & xuất bản</div>
          </div>
          <div className="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
            <Clock className="w-5 h-5" />
          </div>
        </div>

        {/* KPI 4: Phân loại cấp độ JLPT */}
        <div className="p-4 rounded-xl bg-white border border-slate-200/80 shadow-2xs flex flex-col justify-between">
          <div className="flex items-center justify-between text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
            <span>Phân bố cấp độ</span>
            <span className="text-indigo-600 font-mono font-bold">N1 ~ N5</span>
          </div>
          <div className="flex items-center gap-1.5 pt-1.5">
            {(['N1', 'N2', 'N3', 'N4', 'N5'] as const).map((lvl) => {
              const count = stats.levelCounts[lvl] || 0
              const active = selectedLevel === lvl
              return (
                <button
                  key={lvl}
                  type="button"
                  onClick={() => setSelectedLevel(active ? 'ALL' : lvl)}
                  className={`flex-1 py-1 px-1 rounded-md text-center transition cursor-pointer border ${
                    active
                      ? 'bg-slate-900 text-white border-slate-900 shadow-2xs font-semibold'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-700 border-slate-200/70'
                  }`}
                  title={`Lọc theo cấp độ ${lvl} (${count} đề)`}
                >
                  <div className="text-[10px] font-bold leading-none">{lvl}</div>
                  <div className={`text-[10px] font-mono mt-0.5 leading-none ${active ? 'text-slate-300' : 'text-slate-400'}`}>
                    {count}
                  </div>
                </button>
              )
            })}
          </div>
        </div>
      </div>

      {/* 3. BỘ LỌC & THANH CÔNG CỤ TÌM KIẾM TINH GỌN */}
      <div className="bg-white p-3.5 rounded-xl border border-slate-200/80 shadow-2xs space-y-3">
        {/* Hàng 1: Search box + Dropdowns loại đề & sắp xếp + View Mode Switcher */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          {/* Ô tìm kiếm thông minh */}
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo tên đề thi, ID, cấp độ, khóa học..."
              className="w-full pl-9 pr-8 py-1.5 text-xs sm:text-sm bg-slate-50/80 border border-slate-200 rounded-lg focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition placeholder:text-slate-400"
            />
            {searchTerm && (
              <button
                type="button"
                onClick={() => setSearchTerm('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Bộ lọc select & View mode */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Pulldown 1: Phân loại đề thi */}
            <div className="relative inline-flex items-center">
              <select
                value={selectedExamType}
                onChange={(e) => setSelectedExamType(e.target.value)}
                className={`appearance-none text-xs font-medium rounded-lg pl-3 pr-7 py-1.5 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 cursor-pointer border transition shadow-2xs ${
                  selectedExamType !== 'ALL'
                    ? 'bg-indigo-50/80 border-indigo-200 text-indigo-900'
                    : 'bg-white hover:bg-slate-50 border-slate-200 text-slate-700'
                }`}
                title="Lọc theo phân loại đề thi"
              >
                <option value="ALL">Tất cả loại bài thi</option>
                <option value="JLPT_OFFICIAL_MOCK">Đề thi thử JLPT chuẩn</option>
                <option value="MINI_TEST">Mini-test / Khóa học</option>
              </select>
              <ChevronDown className="w-3.5 h-3.5 absolute right-2 text-slate-400 pointer-events-none" />
            </div>

            {/* Pulldown 2: Sắp xếp danh sách */}
            <div className="relative inline-flex items-center">
              <select
                value={sortBy}
                onChange={(e: any) => setSortBy(e.target.value)}
                className={`appearance-none text-xs font-medium rounded-lg pl-3 pr-7 py-1.5 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 cursor-pointer border transition shadow-2xs ${
                  sortBy !== 'NEWEST'
                    ? 'bg-indigo-50/80 border-indigo-200 text-indigo-900'
                    : 'bg-white hover:bg-slate-50 border-slate-200 text-slate-700'
                }`}
                title="Sắp xếp danh sách đề thi"
              >
                <option value="NEWEST">Mới nhất (ID giảm dần)</option>
                <option value="OLDEST">Cũ nhất (ID tăng dần)</option>
                <option value="TITLE_ASC">Tên đề (A → Z)</option>
                <option value="TITLE_DESC">Tên đề (Z → A)</option>
                <option value="DURATION_DESC">Thời lượng dài nhất</option>
                <option value="QUESTIONS_DESC">Số câu hỏi nhiều nhất</option>
              </select>
              <ChevronDown className="w-3.5 h-3.5 absolute right-2 text-slate-400 pointer-events-none" />
            </div>

            {/* Toggle Chế độ xem (Grid vs Table) */}
            <div className="flex items-center p-0.5 bg-slate-100 rounded-lg border border-slate-200">
              <button
                type="button"
                onClick={() => setViewMode('GRID')}
                className={`p-1.5 rounded-md transition cursor-pointer ${
                  viewMode === 'GRID'
                    ? 'bg-white text-indigo-600 shadow-2xs font-bold'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
                title="Xem dạng thẻ (Grid View)"
              >
                <Grid3X3 className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() => setViewMode('TABLE')}
                className={`p-1.5 rounded-md transition cursor-pointer ${
                  viewMode === 'TABLE'
                    ? 'bg-white text-indigo-600 shadow-2xs font-bold'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
                title="Xem dạng bảng (Table View)"
              >
                <ListIcon className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Hàng 2: Segmented Pills chọn Cấp độ & Trạng thái */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2.5 border-t border-slate-100">
          {/* Cấp độ JLPT pills */}
          <div className="flex flex-wrap items-center gap-1">
            <span className="text-[11px] font-semibold text-slate-400 mr-1">Cấp độ:</span>

            <button
              type="button"
              onClick={() => setSelectedLevel('ALL')}
              className={`px-2.5 py-1 rounded-md text-xs transition cursor-pointer ${
                selectedLevel === 'ALL'
                  ? 'bg-slate-900 text-white font-semibold shadow-2xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-600 font-medium'
              }`}
            >
              Tất cả ({exams.length})
            </button>

            {(['N1', 'N2', 'N3', 'N4', 'N5'] as const).map((lvl) => {
              const count = stats.levelCounts[lvl] || 0
              const active = selectedLevel === lvl

              return (
                <button
                  key={lvl}
                  type="button"
                  onClick={() => setSelectedLevel(lvl)}
                  className={`px-2.5 py-1 rounded-md text-xs border transition cursor-pointer flex items-center gap-1 ${
                    active
                      ? 'bg-indigo-600 text-white border-indigo-600 font-semibold shadow-2xs'
                      : 'bg-white hover:bg-slate-50 text-slate-700 border-slate-200 font-medium'
                  }`}
                >
                  <span>{lvl}</span>
                  <span className={`text-[10px] font-mono px-1 rounded-full ${active ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-500'}`}>
                    {count}
                  </span>
                </button>
              )
            })}
          </div>

          {/* Trạng thái xuất bản pills */}
          <div className="flex items-center gap-1 text-xs">
            <span className="text-[11px] font-semibold text-slate-400 mr-1">Trạng thái:</span>
            <button
              type="button"
              onClick={() => setSelectedStatus('ALL')}
              className={`px-2.5 py-1 rounded-md transition cursor-pointer ${
                selectedStatus === 'ALL'
                  ? 'bg-indigo-50 text-indigo-700 font-semibold border border-indigo-200/70'
                  : 'text-slate-500 hover:text-slate-800 font-medium'
              }`}
            >
              Tất cả
            </button>
            <button
              type="button"
              onClick={() => setSelectedStatus('PUBLISHED')}
              className={`px-2.5 py-1 rounded-md transition cursor-pointer flex items-center gap-1.5 ${
                selectedStatus === 'PUBLISHED'
                  ? 'bg-emerald-50 text-emerald-700 font-semibold border border-emerald-200/70'
                  : 'text-slate-500 hover:text-slate-800 font-medium'
              }`}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              Đã phát hành
            </button>
            <button
              type="button"
              onClick={() => setSelectedStatus('DRAFT')}
              className={`px-2.5 py-1 rounded-md transition cursor-pointer ${
                selectedStatus === 'DRAFT'
                  ? 'bg-amber-50 text-amber-800 font-semibold border border-amber-200/70'
                  : 'text-slate-500 hover:text-slate-800 font-medium'
              }`}
            >
              Bản nháp
            </button>

            {(searchTerm || selectedLevel !== 'ALL' || selectedStatus !== 'ALL' || selectedExamType !== 'ALL' || sortBy !== 'NEWEST') && (
              <button
                type="button"
                onClick={() => {
                  setSearchTerm('')
                  setSelectedLevel('ALL')
                  setSelectedStatus('ALL')
                  setSelectedExamType('ALL')
                  setSortBy('NEWEST')
                }}
                className="ml-2 text-xs text-indigo-600 hover:underline cursor-pointer"
              >
                Đặt lại
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 4. DANH SÁCH ĐỀ THI (LOADING / EMPTY / GRID / TABLE) */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="bg-surface rounded-2xl p-5 border border-slate-200 animate-pulse space-y-3 shadow-2xs"
            >
              <div className="flex items-center justify-between">
                <div className="w-14 h-5 bg-slate-200 rounded-md" />
                <div className="w-20 h-5 bg-slate-200 rounded-full" />
              </div>
              <div className="h-5 bg-slate-200 rounded-md w-3/4" />
              <div className="grid grid-cols-3 gap-2 pt-2">
                <div className="h-9 bg-slate-100 rounded-lg" />
                <div className="h-9 bg-slate-100 rounded-lg" />
                <div className="h-9 bg-slate-100 rounded-lg" />
              </div>
              <div className="h-8 bg-slate-100 rounded-xl" />
            </div>
          ))}
        </div>
      ) : filteredExams.length === 0 ? (
        <div className="bg-surface rounded-2xl border border-slate-200/80 p-10 text-center max-w-lg mx-auto space-y-4 shadow-xs">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto">
            <BookOpen className="w-6 h-6 text-indigo-500" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-slate-900">Không tìm thấy đề thi phù hợp</h3>
            <p className="text-xs text-slate-500 max-w-xs mx-auto leading-relaxed">
              {searchTerm || selectedLevel !== 'ALL' || selectedStatus !== 'ALL' || selectedExamType !== 'ALL' || sortBy !== 'NEWEST'
                ? 'Thử điều chỉnh lại từ khóa tìm kiếm hoặc các tiêu chí bộ lọc.'
                : 'Chưa có đề thi nào trong hệ thống. Hãy khởi tạo ngay một đề thi JLPT chuẩn.'}
            </p>
          </div>
          <div className="flex items-center justify-center gap-2 pt-1">
            {(searchTerm || selectedLevel !== 'ALL' || selectedStatus !== 'ALL' || selectedExamType !== 'ALL' || sortBy !== 'NEWEST') && (
              <button
                type="button"
                onClick={() => {
                  setSearchTerm('')
                  setSelectedLevel('ALL')
                  setSelectedStatus('ALL')
                  setSelectedExamType('ALL')
                  setSortBy('NEWEST')
                }}
                className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition cursor-pointer"
              >
                Đặt lại bộ lọc
              </button>
            )}
            <button
              type="button"
              onClick={() => navigate('/admin/exams/create')}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition shadow-xs flex items-center gap-1.5 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Tạo đề thi mới</span>
            </button>
          </div>
        </div>
      ) : viewMode === 'GRID' ? (
        /* CHẾ ĐỘ 1: CARD GRID VIEW (LƯỚI THẺ CAO CẤP) */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredExams.map((exam) => {
            const isPublished = exam.published === true || exam.status === 'PUBLISHED'
            const badgeStyle = JLPT_BADGES[exam.jlptLevel] || JLPT_BADGES['N3']
            const isPublishing = publishingId === exam.id
            const isCloning = cloningId === exam.id

            return (
              <div
                key={exam.id}
                onClick={() => navigate(`/admin/exams/${exam.id}/preview`)}
                className="group bg-surface rounded-xl border border-slate-200/90 hover:border-indigo-300 hover:shadow-md transition-all duration-200 p-4 sm:p-5 flex flex-col justify-between cursor-pointer relative"
              >
                <div className="space-y-3">
                  {/* Card Header: Level badge + Exam type + Status badge */}
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5">
                      <span
                        className={`px-2 py-0.5 rounded-md text-xs font-bold border tracking-wide ${badgeStyle.bg} ${badgeStyle.border}`}
                      >
                        {exam.jlptLevel}
                      </span>
                      <span className="px-2 py-0.5 bg-slate-100 text-slate-600 text-[11px] font-medium rounded-md">
                        {exam.examType === 'JLPT_OFFICIAL_MOCK'
                          ? 'JLPT Chuẩn'
                          : exam.examType === 'MINI_TEST'
                          ? 'Mini Test'
                          : exam.examType}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5">
                      {isPublished ? (
                        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200/80 rounded-full text-[11px] font-semibold">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                          <span>Đã phát hành</span>
                          {exam.versionNo && (
                            <span className="font-mono text-[10px] text-emerald-600 bg-emerald-100/60 px-1 rounded">
                              v{exam.versionNo}
                            </span>
                          )}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-2 py-0.5 bg-amber-50 text-amber-700 border border-amber-200/80 rounded-full text-[11px] font-semibold">
                          <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                          <span>Bản nháp</span>
                        </span>
                      )}

                      <button
                        type="button"
                        onClick={(e) => handleCopyLink(exam.id, e)}
                        title="Sao chép link làm bài"
                        className="p-1 text-slate-400 hover:text-indigo-600 hover:bg-slate-100 rounded-md transition cursor-pointer"
                      >
                        {copiedId === exam.id ? (
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                        ) : (
                          <Copy className="w-3.5 h-3.5" />
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Tiêu đề & Thông tin khóa học */}
                  <div>
                    <h3 className="text-sm sm:text-base font-semibold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2 leading-snug">
                      {exam.title}
                    </h3>
                    {exam.courseId && (
                      <div className="flex items-center gap-1 text-[11px] font-medium text-indigo-600 mt-1">
                        <GraduationCap className="w-3.5 h-3.5 text-indigo-500" />
                        <span>Khóa học #{exam.courseId}</span>
                      </div>
                    )}
                  </div>

                  {/* Thông số kỹ thuật (Inline specs bar thanh thoát) */}
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500 pt-2 border-t border-slate-100">
                    <span className="inline-flex items-center gap-1.5 font-medium text-slate-700">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{exam.totalDurationMinutes} phút</span>
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="inline-flex items-center gap-1.5 font-medium text-slate-700">
                      <FileText className="w-3.5 h-3.5 text-slate-400" />
                      <span>{exam.totalQuestions} câu</span>
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="inline-flex items-center gap-1.5 font-medium text-slate-700">
                      <Award className="w-3.5 h-3.5 text-amber-500" />
                      <span>Đạt {exam.overallPassingScore || exam.passingScore || 90}/180</span>
                    </span>
                  </div>
                </div>

                {/* Card Footer: Thao tác chính */}
                <div className="mt-3.5 pt-2.5 flex items-center justify-between gap-1.5 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation()
                      navigate(`/admin/exams/${exam.id}/preview`)
                    }}
                    className="py-1.5 px-3 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-lg text-xs font-semibold transition flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>Biên soạn</span>
                  </button>

                  <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                    {/* Xuất bản / Thu hồi */}
                    {!isPublished ? (
                      <button
                        type="button"
                        disabled={isPublishing}
                        onClick={(e) => handlePublishExam(exam.id, e)}
                        className="py-1.5 px-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold transition flex items-center gap-1 cursor-pointer shadow-2xs"
                      >
                        {isPublishing ? <RotateCw className="w-3 h-3 animate-spin" /> : <Send className="w-3 h-3" />}
                        <span>Xuất bản</span>
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={isPublishing}
                        onClick={(e) => handleUnpublishExam(exam.id, e)}
                        className="py-1.5 px-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium transition flex items-center gap-1 cursor-pointer"
                        title="Thu hồi đề thi về bản nháp"
                      >
                        {isPublishing ? <RotateCw className="w-3 h-3 animate-spin" /> : <EyeOff className="w-3 h-3" />}
                        <span>Thu hồi</span>
                      </button>
                    )}

                    {/* Nhân bản (Clone) */}
                    <button
                      type="button"
                      disabled={isCloning}
                      onClick={(e) => handleCloneExam(exam.id, e)}
                      title="Nhân bản đề thi này"
                      className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-slate-100 rounded-lg transition cursor-pointer"
                    >
                      <Copy className={`w-3.5 h-3.5 ${isCloning ? 'animate-spin text-indigo-600' : ''}`} />
                    </button>

                    {/* Xóa đề thi */}
                    <button
                      type="button"
                      onClick={() => setDeleteModalExam(exam)}
                      title="Xóa đề thi này"
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      ) : (
        /* CHẾ ĐỘ 2: TABLE VIEW (BẢNG QUẢN TRỊ DENSE) */
        <div className="bg-surface rounded-xl border border-slate-200/90 shadow-2xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="py-3 px-4">Cấp độ</th>
                  <th className="py-3 px-4">Tên đề thi</th>
                  <th className="py-3 px-4">Loại đề</th>
                  <th className="py-3 px-4">Thời lượng</th>
                  <th className="py-3 px-4">Số câu</th>
                  <th className="py-3 px-4">Điểm đỗ</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs sm:text-sm">
                {filteredExams.map((exam) => {
                  const isPublished = exam.published === true || exam.status === 'PUBLISHED'
                  const badgeStyle = JLPT_BADGES[exam.jlptLevel] || JLPT_BADGES['N3']

                  return (
                    <tr
                      key={exam.id}
                      onClick={() => navigate(`/admin/exams/${exam.id}/preview`)}
                      className="hover:bg-slate-50/70 transition cursor-pointer"
                    >
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-0.5 rounded-md text-xs font-bold border ${badgeStyle.bg} ${badgeStyle.border}`}
                        >
                          {exam.jlptLevel}
                        </span>
                      </td>

                      <td className="py-3 px-4 max-w-xs">
                        <div className="font-semibold text-slate-900 line-clamp-1 hover:text-indigo-600">
                          {exam.title}
                        </div>
                        {exam.courseId && (
                          <div className="text-[10px] text-indigo-600 font-medium">
                            Khóa học #{exam.courseId}
                          </div>
                        )}
                      </td>

                      <td className="py-3 px-4 text-xs text-slate-600">
                        {exam.examType === 'JLPT_OFFICIAL_MOCK' ? 'JLPT Chuẩn' : exam.examType}
                      </td>

                      <td className="py-3 px-4 text-xs font-medium text-slate-700">
                        {exam.totalDurationMinutes} phút
                      </td>

                      <td className="py-3 px-4 text-xs font-medium text-slate-700">
                        {exam.totalQuestions} câu
                      </td>

                      <td className="py-3 px-4 text-xs font-semibold text-amber-700">
                        {exam.overallPassingScore || exam.passingScore || 90}/180
                      </td>

                      <td className="py-3 px-4">
                        {isPublished ? (
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200/80 rounded-full text-[11px] font-semibold">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                            <span>Đã phát hành</span>
                            {exam.versionNo && (
                              <span className="font-mono text-[10px] text-emerald-600 bg-emerald-100/60 px-1 rounded">
                                v{exam.versionNo}
                              </span>
                            )}
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 bg-amber-50 text-amber-700 border border-amber-200/80 rounded-full text-[11px] font-semibold">
                            <span className="w-1.5 h-1.5 rounded-full bg-amber-500" />
                            <span>Bản nháp</span>
                          </span>
                        )}
                      </td>

                      <td className="py-3 px-4 text-right">
                        <div
                          className="flex items-center justify-end gap-1.5"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <button
                            type="button"
                            onClick={() => navigate(`/admin/exams/${exam.id}/preview`)}
                            className="px-2.5 py-1 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold rounded-lg text-xs transition cursor-pointer"
                          >
                            Biên soạn
                          </button>

                          {!isPublished ? (
                            <button
                              type="button"
                              onClick={(e) => handlePublishExam(exam.id, e)}
                              className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-lg text-xs transition cursor-pointer shadow-2xs"
                            >
                              Xuất bản
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={(e) => handleUnpublishExam(exam.id, e)}
                              className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-lg text-xs transition cursor-pointer"
                              title="Thu hồi đề thi về bản nháp"
                            >
                              Thu hồi
                            </button>
                          )}

                          <button
                            type="button"
                            onClick={() => setDeleteModalExam(exam)}
                            className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition cursor-pointer"
                            title="Xóa đề thi"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 5. MODAL XÁC NHẬN XÓA ĐỀ THI AN TOÀN */}
      {deleteModalExam && (
        <div className="fixed inset-0 overscroll-contain z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-3xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6 text-rose-600" />
            </div>

            <div className="text-center space-y-1.5">
              <h3 className="text-lg font-black text-slate-900">Xác nhận xóa đề thi?</h3>
              <p className="text-xs text-slate-500 leading-relaxed">
                Bạn sắp xóa đề thi{' '}
                <strong className="text-slate-900 font-bold">"{deleteModalExam.title}"</strong> (
                {deleteModalExam.jlptLevel}). Toàn bộ câu hỏi, bài đọc và cấu trúc đề thi này sẽ bị xóa khỏi hệ thống.
              </p>
            </div>

            <div className="p-3 bg-rose-50/70 border border-rose-200 rounded-2xl text-[11px] text-rose-700 font-medium leading-relaxed">
              ⚠️ <strong>Lưu ý:</strong> Hành động này không thể hoàn tác. Các lượt đang thi trực tiếp sẽ bị gián đoạn.
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                disabled={deletingId !== null}
                onClick={() => setDeleteModalExam(null)}
                className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                disabled={deletingId !== null}
                onClick={confirmDeleteExam}
                className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-xl shadow-xs transition flex items-center justify-center gap-2 cursor-pointer"
              >
                {deletingId ? (
                  <RotateCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Trash2 className="w-4 h-4" />
                )}
                <span>Xác nhận xóa</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminExamsPage
