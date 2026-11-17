import { useBodyScrollLock } from '@/hooks/useBodyScrollLock'
import { toast } from 'sonner'
import React, { useState, useMemo } from 'react'
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useLocation, useNavigate } from 'react-router-dom'
import { BackButton } from '@/components/common'
import { examService } from '@/services'
import type { ExamSummary, ExamDetail, JlptLevel, AttemptType } from '@/types'
import { useAuthStore, useSubscriptionStore } from '@/stores'
import { isTeacherOrAdmin as checkTeacherOrAdmin } from '@/utils/roles'
import { 
  Award, Clock, ChevronRight, Flame, BookOpen,
  X, Loader2, Edit3, Crown, FileText, Lock, Eye
} from 'lucide-react'
import { Pagination } from 'antd'
import { LeaderboardWidget } from '@/components'

type StatusFilter = 'ALL' | 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

const STATUS_TABS: { key: StatusFilter; label: string }[] = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'NOT_STARTED', label: 'Chưa làm' },
  { key: 'IN_PROGRESS', label: 'Đang làm' },
  { key: 'COMPLETED', label: 'Đã xong' },
]

export const ExamLobbyPage: React.FC = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  // Trang gọi tới có thể gửi kèm state.backTo để hiện nút quay lại (vd: quản lý đề thi)
  const backTo = (useLocation().state as { backTo?: string } | null)?.backTo
  const user = useAuthStore((s) => s.user)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isTeacherOrAdmin = checkTeacherOrAdmin(user)
  // Cho phép Admin/Teacher chuyển sang chế độ học viên thường để test/kiểm tra đề thi bị khóa PRO
  const [adminSimulateFreeLearner, setAdminSimulateFreeLearner] = useState<boolean>(false)
  const userHasPro = useSubscriptionStore((s) => s.isPro) || Boolean(user?.isPro)
  const isPro = adminSimulateFreeLearner ? false : (userHasPro || isTeacherOrAdmin)
  const openPaywall = useSubscriptionStore((s) => s.openPaywall)

  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | 'ALL'>('N5')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')

  // Phân trang chuẩn Ant Design (mặc định 12 đề = lưới 3x4)
  const [currentPage, setCurrentPage] = useState<number>(1)
  const [pageSize, setPageSize] = useState<number>(12)

  const examsQuery = useQuery({
    queryKey: QUERY_KEYS.exam.published({ level: selectedLevel, page: currentPage, size: pageSize }),
    queryFn: () =>
      examService.getPublishedExams({
        level: selectedLevel === 'ALL' ? undefined : selectedLevel,
        page: currentPage - 1,
        size: pageSize,
      }),
    placeholderData: keepPreviousData,
  })
  const exams = useMemo<ExamSummary[]>(
    () => examsQuery.data?.data?.content || examsQuery.data?.data?.items || [],
    [examsQuery.data],
  )
  const totalItems = examsQuery.data?.data?.totalElements ?? exams.length
  const loading = examsQuery.isLoading
  const error = examsQuery.error
    ? (examsQuery.error as any)?.response?.data?.message || 'Không thể tải danh sách đề thi'
    : null

  // Lịch sử làm bài để định danh trạng thái Chưa làm / Đang làm / Đạt / Chưa đạt (lỗi thì bỏ qua)
  const { data: myHistory } = useQuery({
    queryKey: QUERY_KEYS.exam.myHistory(user?.id),
    queryFn: examService.getMyExamHistory,
    // Chờ có user.id, nếu không key đổi từ undefined → id làm gọi API 2 lần
    enabled: isAuthenticated && !!user?.id,
  })

  // State cho Modal đăng nhập
  const [showLoginModal, setShowLoginModal] = useState(false)

  // State cho Modal chọn chế độ thi
  const [selectedExam, setSelectedExam] = useState<ExamDetail | null>(null)
  // Khoá cuộn trang phía sau khi lớp phủ tự dựng đang mở
  useBodyScrollLock(!!selectedExam || showLoginModal)
  const [loadingDetailId, setLoadingDetailId] = useState<number | null>(null)
  const [attemptType, setAttemptType] = useState<AttemptType>('FULL_EXAM')
  const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null)
  const [starting, setStarting] = useState(false)

  const levels: (JlptLevel | 'ALL')[] = ['ALL', 'N5', 'N4', 'N3', 'N2', 'N1']

  // Map trạng thái làm bài mới nhất của từng đề thi
  const userExamStatusMap = useMemo(() => {
    const map: Record<number, { status: 'IN_PROGRESS' | 'PASSED' | 'FAILED' | 'NOT_STARTED'; attemptId: number; score?: number }> = {}
    if (!isAuthenticated || !myHistory || myHistory.length === 0) return map

    for (const attempt of myHistory) {
      const existing = map[attempt.examId]
      if (attempt.status === 'IN_PROGRESS') {
        map[attempt.examId] = { status: 'IN_PROGRESS', attemptId: attempt.attemptId }
      } else if (!existing || existing.status !== 'IN_PROGRESS') {
        map[attempt.examId] = {
          status: attempt.passed ? 'PASSED' : 'FAILED',
          attemptId: attempt.attemptId,
          score: attempt.totalScaledScore,
        }
      }
    }
    return map
  }, [isAuthenticated, myHistory])

  // Lọc đề thi theo tab trạng thái (Tất cả / Chưa làm / Đang làm / Đã xong)
  const displayedExams = useMemo(() => {
    let list = exams
    if (statusFilter !== 'ALL') {
      list = list.filter((exam) => {
        const st = userExamStatusMap[exam.id]?.status ?? 'NOT_STARTED'
        if (statusFilter === 'NOT_STARTED') return st === 'NOT_STARTED'
        if (statusFilter === 'IN_PROGRESS') return st === 'IN_PROGRESS'
        if (statusFilter === 'COMPLETED') return st === 'PASSED' || st === 'FAILED'
        return true
      })
    }
    // Sắp xếp: Đề Miễn phí (Free) lên trước, Đề Premium (PRO) ra sau; trong từng nhóm sắp xếp theo ID tăng dần (Đề 01, 02, 03, ...)
    return list.slice().sort((a, b) => {
      const aPrem = Boolean(a.isPremium ?? (a as any).premium)
      const bPrem = Boolean(b.isPremium ?? (b as any).premium)
      if (aPrem !== bPrem) return aPrem ? 1 : -1
      return a.id - b.id
    })
  }, [exams, statusFilter, userExamStatusMap])

  const handleOpenStartModal = async (examSummary: ExamSummary) => {
    // Nếu chưa đăng nhập thì hiển thị modal yêu cầu đăng nhập
    if (!isAuthenticated) {
      setShowLoginModal(true)
      return
    }

    try {
      setLoadingDetailId(examSummary.id)
      const res = await examService.getExamDetail(examSummary.id)
      if (res.data) {
        setSelectedExam(res.data)
        setAttemptType('FULL_EXAM')
        setSelectedSectionId(null)
      }
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Lỗi khi tải chi tiết đề thi')
    } finally {
      setLoadingDetailId(null)
    }
  }

  const handleUnlockPremium = (exam: ExamSummary) => {
    if (!isAuthenticated) {
      setShowLoginModal(true)
      return
    }
    openPaywall(
      `Đề thi "${exam.title}" thuộc gói hội viên PRO. Nâng cấp ngay để mở khóa toàn bộ kho đề thi JLPT N5 ~ N1 chuẩn hóa!`,
    )
  }

  const handleStartExam = async () => {
    if (!selectedExam) return
    if (attemptType === 'PRACTICE_SECTION' && !selectedSectionId) {
      toast.warning('Vui lòng chọn phần thi bạn muốn luyện tập!')
      return
    }

    try {
      setStarting(true)
      const res = await examService.startExam(selectedExam.id, {
        attemptType,
        sectionId: attemptType === 'PRACTICE_SECTION' ? selectedSectionId || undefined : undefined,
      })
      if (res.data) {
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.exam.myHistory(user?.id) })
        navigate(`/exams/${selectedExam.id}/taking/${res.data.attemptId}`, {
          state: { takingData: res.data },
        })
      }
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Không thể bắt đầu bài thi')
    } finally {
      setStarting(false)
    }
  }

  return (
    <div className="pb-10 max-w-[1650px] mx-auto">
      {/* Mở từ trang quản lý đề thi ("Xem phòng thi") → cho quay lại trang đó */}
      {backTo && <BackButton to={backTo} className="mb-3" />}

      {/* Grid 2 Cột: Cột Trái Đề Thi + Cột Phải Bảng Xếp Hạng */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 items-start">
        {/* CỘT TRÁI (8 CỘT TRÊN DESKTOP): BỘ LỌC + DANH SÁCH BÀI THI */}
        <div className="lg:col-span-8 space-y-4">
          {/* HÀNG 1: CHỌN CẤP ĐỘ JLPT (N5 ~ N1) & NÚT CHỨC NĂNG */}
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="text-xs font-bold text-nibi uppercase tracking-wider mr-1">Cấp độ:</span>
              {levels.map((lvl) => (
                <button
                  key={lvl}
                  type="button"
                  aria-pressed={selectedLevel === lvl}
                  onClick={() => {
                    setSelectedLevel(lvl)
                    setCurrentPage(1)
                  }}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                    selectedLevel === lvl
                      ? 'bg-blue-600 text-white shadow-xs'
                      : 'bg-surface hover:bg-suna/30 text-sumi border border-suna shadow-2xs'
                  }`}
                >
                  {lvl === 'ALL' ? 'Tất cả' : lvl}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => navigate('/exam-history')}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-surface border border-suna text-sumi hover:text-blue-600 font-medium text-xs shadow-2xs transition-all cursor-pointer"
              >
                <Clock className="w-3.5 h-3.5 text-blue-600" />
                <span>Lịch sử thi</span>
              </button>
              {isTeacherOrAdmin && (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setAdminSimulateFreeLearner((prev) => !prev)}
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-semibold shadow-2xs transition-all cursor-pointer ${
                      adminSimulateFreeLearner
                        ? 'bg-amber-500 hover:bg-amber-600 text-white border-amber-600'
                        : 'bg-surface border-suna text-sumi hover:text-amber-600 hover:border-amber-300'
                    }`}
                    title="Bật/Tắt chế độ xem thử giao diện học viên miễn phí (để kiểm tra đề khóa PRO)"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>{adminSimulateFreeLearner ? 'Chế độ: Học viên thường' : 'Xem như học viên'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/admin/exams')}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-surface border border-suna text-sumi hover:text-blue-600 font-medium text-xs shadow-2xs transition-all cursor-pointer"
                  >
                    <Edit3 className="w-3.5 h-3.5 text-blue-600" />
                    <span>Quản lý</span>
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* HÀNG 2: TAB TRẠNG THÁI (TẤT CẢ / CHƯA LÀM / ĐANG LÀM / ĐÃ XONG) + LEGEND */}
          <div className="flex items-center justify-between gap-3 flex-wrap pt-0.5">
            {/* Filter Tabs */}
            <div className="flex items-center gap-1.5">
              {STATUS_TABS.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  aria-pressed={statusFilter === tab.key}
                  onClick={() => setStatusFilter(tab.key)}
                  className={`px-3.5 sm:px-4 py-1.5 rounded-xl text-xs transition-all cursor-pointer ${
                    statusFilter === tab.key
                      ? 'bg-blue-50 text-blue-600 font-bold border border-blue-200/80 shadow-2xs'
                      : 'bg-surface hover:bg-suna/30 text-nibi hover:text-sumi border border-suna font-medium'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {/* Legend (Đang làm / Đạt / Chưa đạt) */}
            <div className="flex items-center gap-3 text-xs font-medium text-nibi shrink-0">
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-600" />
                <span>Đang làm</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                <span>Đạt</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500" />
                <span>Chưa đạt</span>
              </div>
            </div>
          </div>

          {/* Danh sách đề thi */}
          {loading ? (
            <div className="flex flex-col items-center justify-center py-16 text-nibi bg-surface rounded-2xl border border-suna">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-3" />
              <p className="font-semibold text-sm">Đang tải danh sách đề thi...</p>
            </div>
          ) : error ? (
            <div className="p-5 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 rounded-2xl text-rose-600 dark:text-rose-400 text-sm text-center font-medium">
              {error}
            </div>
          ) : displayedExams.length === 0 ? (
            <div className="text-center py-14 bg-surface rounded-2xl border border-suna shadow-2xs p-6">
              <BookOpen className="w-12 h-12 text-nibi/50 mx-auto mb-3" />
              <h3 className="text-base font-bold text-sumi mb-1">
                {statusFilter === 'ALL'
                  ? 'Chưa có đề thi nào ở cấp độ này'
                  : 'Không có đề thi nào phù hợp với bộ lọc hiện tại'}
              </h3>
              <p className="text-xs sm:text-sm text-nibi max-w-md mx-auto">
                Hệ thống đang liên tục cập nhật thêm đề thi chuẩn mới. Bạn vui lòng chọn cấp độ hoặc bộ lọc khác để luyện tập!
              </p>
            </div>
          ) : (
            <>
              {/* LƯỚI THẺ ĐỀ THI 3 CỘT (MATCH CHUẨN DESIGN YÊU CẦU) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3.5 pt-1">
                {displayedExams.map((exam) => {
                  const userStatus = userExamStatusMap[exam.id]?.status ?? 'NOT_STARTED'
                  const isPremiumExam = Boolean(exam.isPremium ?? (exam as any).premium)
                  const isLocked = Boolean(isPremiumExam && !isPro)
                  const isInProgress = userStatus === 'IN_PROGRESS'

                  return (
                    <div
                      key={exam.id}
                      className={`relative bg-surface rounded-2xl border p-4 sm:p-4.5 transition-all duration-200 overflow-hidden flex flex-col justify-between min-h-[145px] shadow-2xs ${
                        isInProgress
                          ? 'border-blue-500 ring-1 ring-blue-500/20 shadow-xs'
                          : isLocked
                            ? 'border-suna hover:border-blue-300'
                            : 'border-suna hover:border-suna/80'
                      }`}
                    >
                      {/* Watermark JLPT lớn chìm phía sau nền */}
                      <div className="absolute -bottom-2 -left-1 text-5xl font-black text-suna/30 select-none pointer-events-none uppercase tracking-wider">
                        JLPT
                      </div>

                      {/* Header thẻ: Icon đề + Tiêu đề + Chấm trạng thái + Huy hiệu PRO */}
                      <div className="flex items-start justify-between gap-2 relative z-10">
                        <div className="flex items-center gap-2 min-w-0">
                          <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 ${
                            isLocked
                              ? 'bg-amber-50 dark:bg-amber-950/40 text-amber-600'
                              : 'bg-suna/30 text-nibi'
                          }`}>
                            <FileText className="w-3.5 h-3.5" />
                          </div>
                          <div className="min-w-0">
                            <div className="flex items-center gap-1.5">
                              <h4 className="font-bold text-sm text-sumi truncate" title={exam.title}>
                                {exam.title}
                              </h4>
                              {userStatus === 'IN_PROGRESS' && (
                                <span className="w-2.5 h-2.5 rounded-full bg-blue-600 shrink-0" title="Đang làm dở" />
                              )}
                              {userStatus === 'PASSED' && (
                                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shrink-0" title="Đã thi - Đạt" />
                              )}
                              {userStatus === 'FAILED' && (
                                <span className="w-2.5 h-2.5 rounded-full bg-rose-500 shrink-0" title="Đã thi - Chưa đạt" />
                              )}
                            </div>
                          </div>
                        </div>

                        {/* Huy hiệu vương miện PRO tròn chuẩn hình mẫu */}
                        {isPremiumExam && (
                          <div
                            className={`w-6 h-6 rounded-full flex items-center justify-center shadow-2xs shrink-0 ${
                              isLocked ? 'bg-blue-500 text-white' : 'bg-blue-600 text-white'
                            }`}
                            title={isLocked ? 'Đề thi VIP - Yêu cầu tài khoản PRO' : 'Đề thi VIP (Đã mở khóa)'}
                          >
                            <Crown className="w-3.5 h-3.5 fill-current" />
                          </div>
                        )}
                      </div>

                      {/* Thông số đề thi: Số câu hỏi | Thời lượng làm bài */}
                      <div className="mt-2 text-xs text-nibi font-medium relative z-10">
                        <span>{exam.totalQuestions} câu</span>
                        <span className="mx-1.5 text-suna">|</span>
                        <span>{exam.totalDurationMinutes} phút</span>
                      </div>

                      {/* Nút hành động hoặc Trạng thái Mở khóa Premium */}
                      <div className="mt-4 flex items-center justify-end relative z-10">
                        {isLocked ? (
                          <button
                            type="button"
                            onClick={() => handleUnlockPremium(exam)}
                            className="text-xs font-semibold text-sumi hover:text-blue-600 flex items-center gap-1.5 transition-colors cursor-pointer group"
                            title="Nhấn để nâng cấp tài khoản PRO mở khóa toàn bộ đề thi"
                          >
                            <span className="group-hover:underline">Mở khoá với premium</span>
                            <Lock className="w-3.5 h-3.5 text-sumi group-hover:text-blue-600" />
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleOpenStartModal(exam)}
                            disabled={loadingDetailId === exam.id}
                            className="px-5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white text-xs font-semibold shadow-xs transition-colors cursor-pointer disabled:opacity-60"
                          >
                            {loadingDetailId === exam.id ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin mx-auto" />
                            ) : isInProgress ? (
                              'Làm tiếp'
                            ) : (
                              'Bắt đầu'
                            )}
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>

              {/* Phân trang chuẩn Ant Design */}
              {totalItems > 0 && (
                <div className="flex items-center justify-center pt-6">
                  <Pagination
                    current={currentPage}
                    pageSize={pageSize}
                    total={totalItems}
                    onChange={(page, newPageSize) => {
                      setCurrentPage(page)
                      if (newPageSize && newPageSize !== pageSize) {
                        setPageSize(newPageSize)
                        setCurrentPage(1)
                      }
                    }}
                    onShowSizeChange={(_current, newPageSize) => {
                      setPageSize(newPageSize)
                      setCurrentPage(1)
                    }}
                    showSizeChanger={true}
                    pageSizeOptions={['6', '12', '24', '48']}
                    locale={{ items_per_page: '/ trang' }}
                  />
                </div>
              )}
            </>
          )}
        </div>

        {/* CỘT PHẢI (4 CỘT TRÊN DESKTOP): BẢNG XẾP HẠNG GIAO DIỆN MỚI */}
        <div className="lg:col-span-4 lg:sticky-panel" id="leaderboard">
          <LeaderboardWidget
            initialLevel={selectedLevel === 'ALL' ? 'N5' : selectedLevel}
            variant="lobby"
            // Chưa có API ghi nhận yêu cầu của học viên → chỉ giáo viên/admin thấy nút (mở trang tạo đề)
            onRequestMoreExams={isTeacherOrAdmin ? () => navigate('/admin/exams/create') : undefined}
          />
        </div>
      </div>

      {/* Modal Chọn Chế Độ Thi (Full Exam hoặc Practice Section) */}
      {selectedExam && (
        <div role="dialog" aria-modal="true" aria-label="Chọn chế độ làm bài" className="fixed inset-0 overscroll-contain z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-2xl max-w-lg w-full overflow-hidden shadow-xl border border-suna animate-in fade-in zoom-in-95 duration-200">
            <div className="px-5 py-4 border-b border-suna/80 flex items-center justify-between bg-kinari/50">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-ai">
                  JLPT {selectedExam.jlptLevel}
                </span>
                <h3 className="text-base font-bold text-sumi line-clamp-1">
                  {selectedExam.title}
                </h3>
              </div>
              <button
                onClick={() => setSelectedExam(null)}
                className="w-8 h-8 rounded-full bg-suna/40 hover:bg-suna flex items-center justify-center text-nibi hover:text-sumi transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              <div>
                <label className="block text-xs font-bold text-nibi uppercase tracking-wider mb-2">
                  1. Chọn Chế độ làm bài:
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setAttemptType('FULL_EXAM')
                      setSelectedSectionId(null)
                    }}
                    className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                      attemptType === 'FULL_EXAM'
                        ? 'border-ai bg-kamenozoki/40 text-sumi ring-1 ring-ai shadow-2xs font-semibold'
                        : 'border-suna hover:border-ai/40 bg-surface text-sumi'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-bold text-sm mb-1 text-ai">
                      <Flame className="w-4 h-4 text-shu" />
                      <span>Thi cả bài</span>
                    </div>
                    <p className="text-xs text-nibi font-normal">
                      Mô phỏng 100% như thi thật ({selectedExam.totalDurationMinutes} phút, đủ các ca).
                    </p>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setAttemptType('PRACTICE_SECTION')
                      if (selectedExam.sections.length > 0) {
                        setSelectedSectionId(selectedExam.sections[0].id)
                      }
                    }}
                    className={`p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                      attemptType === 'PRACTICE_SECTION'
                        ? 'border-ai bg-kamenozoki/40 text-sumi ring-1 ring-ai shadow-2xs font-semibold'
                        : 'border-suna hover:border-ai/40 bg-surface text-sumi'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-bold text-sm mb-1 text-ai">
                      <Award className="w-4 h-4 text-emerald-600" />
                      <span>Luyện từng phần</span>
                    </div>
                    <p className="text-xs text-nibi font-normal">
                      Tùy chọn ôn luyện riêng lẻ Từ vựng, Ngữ pháp - Đọc hiểu hoặc Nghe hiểu.
                    </p>
                  </button>
                </div>
              </div>

              {attemptType === 'PRACTICE_SECTION' && (
                <div>
                  <label className="block text-xs font-bold text-nibi uppercase tracking-wider mb-2">
                    2. Chọn Phần thi:
                  </label>
                  <div className="space-y-2">
                    {selectedExam.sections.map((sec) => (
                      <button
                        key={sec.id}
                        type="button"
                        onClick={() => setSelectedSectionId(sec.id)}
                        className={`w-full p-3 rounded-xl border flex items-center justify-between transition-all cursor-pointer ${
                          selectedSectionId === sec.id
                            ? 'border-ai bg-kamenozoki/30 text-ai font-bold ring-1 ring-ai'
                            : 'border-suna hover:border-ai/30 text-sumi bg-surface'
                        }`}
                      >
                        <div className="flex items-center gap-2.5">
                          <div
                            className={`w-2 h-2 rounded-full ${
                              selectedSectionId === sec.id ? 'bg-ai' : 'bg-suna'
                            }`}
                          />
                          <span className="text-xs">{sec.sectionTitle}</span>
                        </div>
                        <span className="text-xs text-nibi font-normal">
                          {sec.durationMinutes} phút
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div className="pt-2 border-t border-suna/70 flex items-center justify-between text-xs text-nibi">
                <span>Số câu hỏi: <strong className="text-sumi">{selectedExam.totalQuestions}</strong> câu</span>
                <span>Thang điểm: <strong className="text-sumi">{selectedExam.totalScaledMaxScore}</strong> điểm</span>
              </div>
            </div>

            <div className="px-5 py-3.5 bg-kinari/60 border-t border-suna/80 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setSelectedExam(null)}
                className="px-4 py-2 rounded-lg text-xs font-medium text-nibi hover:text-sumi hover:bg-suna/30 transition-colors cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleStartExam}
                disabled={starting}
                className="px-5 py-2 rounded-lg text-xs font-semibold bg-ai hover:bg-ai/90 active:scale-98 text-white shadow-2xs transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-70"
              >
                {starting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang khởi tạo...</span>
                  </>
                ) : (
                  <>
                    <span>Bắt đầu tính giờ</span>
                    <ChevronRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Yêu Cầu Đăng Nhập Trước Khi Vào Thi */}
      {showLoginModal && (
        <div role="dialog" aria-modal="true" aria-label="Yêu cầu đăng nhập" className="fixed inset-0 overscroll-contain z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-2xl max-w-md w-full overflow-hidden shadow-xl border border-suna p-6 text-center animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-full bg-kamenozoki text-ai flex items-center justify-center mx-auto mb-3 shadow-2xs">
              <Award className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-sumi mb-2">
              Yêu cầu đăng nhập
            </h3>
            <p className="text-xs sm:text-sm text-nibi leading-relaxed mb-6 font-normal">
              Để kết quả bài làm được lưu lại, chấm điểm Scaled Score chuẩn JLPT và <strong className="text-ai font-semibold">ghi danh lên Bảng xếp hạng</strong>, bạn vui lòng đăng nhập trước khi bắt đầu!
            </p>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setShowLoginModal(false)}
                className="flex-1 py-2.5 px-4 rounded-lg text-xs font-medium text-nibi bg-suna/30 hover:bg-suna/50 transition-colors cursor-pointer"
              >
                Để sau
              </button>
              <button
                type="button"
                onClick={() => navigate('/login?redirect=/exams')}
                className="flex-1 py-2.5 px-4 rounded-lg text-xs font-medium bg-ai hover:bg-ai/90 text-white shadow-2xs transition-all cursor-pointer"
              >
                Đăng nhập ngay
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
