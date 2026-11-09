import { useBodyScrollLock } from '@/hooks/useBodyScrollLock'
import { toast } from 'sonner'
import React, { useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { BackButton } from '@/components/common'
import { examService } from '@/services'
import type { ExamSummary, ExamDetail, JlptLevel, AttemptType } from '@/types'
import { useAuthStore } from '@/stores'
import { isTeacherOrAdmin as checkTeacherOrAdmin } from '@/utils/roles'
import { JLPT_LEVEL_BADGE } from '@/utils/jlpt'
import { 
  Award, Clock, HelpCircle, CheckCircle2, ChevronRight, 
  Flame, BookOpen, Headphones, Play, X, Loader2, Filter, Edit3
} from 'lucide-react'
import { LeaderboardWidget } from '@/components'

export const ExamLobbyPage: React.FC = () => {
  const navigate = useNavigate()
  // Trang gọi tới có thể gửi kèm state.backTo để hiện nút quay lại (vd: quản lý đề thi)
  const backTo = (useLocation().state as { backTo?: string } | null)?.backTo
  const user = useAuthStore((s) => s.user)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isTeacherOrAdmin = checkTeacherOrAdmin(user)

  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | 'ALL'>('ALL')
  const [exams, setExams] = useState<ExamSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

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

  useEffect(() => {
    fetchExams()
  }, [selectedLevel])

  const fetchExams = async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await examService.getPublishedExams({
        level: selectedLevel === 'ALL' ? undefined : selectedLevel,
      })
      const items = res.data?.content || res.data?.items || []
      setExams(items)
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách đề thi')
    } finally {
      setLoading(false)
    }
  }

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
    <div className="pb-8">
      {/* Mở từ trang quản lý đề thi ("Xem phòng thi") → cho quay lại trang đó */}
      {backTo && <BackButton to={backTo} className="mb-3" />}
      {/* Grid 2 Cột Giống Mazii: Cột Trái Đề Thi + Cột Phải Bảng Xếp Hạng */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 lg:gap-5 items-start">
        {/* CỘT TRÁI (8 CỘT TRÊN DESKTOP): BỘ LỌC + DANH SÁCH BÀI THI */}
        <div className="lg:col-span-7 xl:col-span-8 space-y-4">
          {/* Tabs Lọc Cấp Độ N1 - N5 và Nút chức năng */}
          <div className="flex items-center justify-between flex-wrap gap-3 bg-surface p-2.5 rounded-xl border border-suna shadow-xs">
            <div className="flex items-center gap-1.5 flex-wrap">
              <div className="px-2 text-xs font-bold text-gray-400 flex items-center gap-1.5 uppercase">
                <Filter className="w-3.5 h-3.5" />
                <span>Cấp độ:</span>
              </div>
              {levels.map((lvl) => (
                <button
                  key={lvl}
                  onClick={() => setSelectedLevel(lvl)}
                  className={`px-3 py-1 rounded-md text-xs font-bold transition-all cursor-pointer ${
                    selectedLevel === lvl
                      ? 'bg-indigo-600 text-white shadow-xs font-black'
                      : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-100'
                  }`}
                >
                  {lvl === 'ALL' ? 'Tất cả' : lvl}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <button
                onClick={() => navigate('/exam-history')}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-surface border border-gray-200 text-gray-700 hover:text-indigo-600 hover:border-indigo-300 font-bold text-xs shadow-xs transition cursor-pointer"
              >
                <Clock className="w-3.5 h-3.5 text-indigo-500" />
                <span>Lịch sử thi</span>
              </button>
            </div>
          </div>

          {/* Danh sách đề thi */}
          {loading ? (
            <div className="flex flex-col items-center justify-center py-14 text-gray-400 bg-surface rounded-xl border border-suna">
              <Loader2 className="w-8 h-8 animate-spin text-indigo-600 mb-3" />
              <p className="font-semibold text-sm">Đang tải danh sách đề thi...</p>
            </div>
          ) : error ? (
            <div className="p-5 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm text-center font-medium">
              {error}
            </div>
          ) : exams.length === 0 ? (
            <div className="text-center py-12 bg-surface rounded-xl border border-suna shadow-xs p-6">
              <BookOpen className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <h3 className="text-base font-bold text-gray-800 mb-1">Chưa có đề thi nào ở cấp độ này</h3>
              <p className="text-sm text-gray-500 max-w-md mx-auto">
                Hệ thống đang cập nhật thêm đề thi mới. Bạn vui lòng chọn cấp độ khác để trải nghiệm!
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-3 gap-3">
              {exams.map((exam) => (
                <div
                  key={exam.id}
                  className="bg-surface rounded-xl border border-suna hover:border-indigo-300 hover:shadow-md transition-all duration-200 flex flex-col justify-between overflow-hidden group"
                >
                  <div className="p-4">
                    <div className="flex items-center justify-between gap-2 mb-2.5">
                      <span className={`px-2 py-0.5 rounded-md text-[11px] font-black uppercase tracking-wider ${JLPT_LEVEL_BADGE[exam.jlptLevel]}`}>
                        JLPT {exam.jlptLevel}
                      </span>
                      <span className="text-xs font-bold text-gray-400">
                        Thang điểm: {exam.totalScaledMaxScore}đ
                      </span>
                    </div>

                    <h3 className="text-sm font-bold text-gray-900 group-hover:text-indigo-600 transition-colors line-clamp-2 mb-2.5">
                      {exam.title}
                    </h3>

                    <div className="grid grid-cols-2 gap-2 text-xs font-semibold text-gray-600 bg-kinari p-2.5 rounded-lg">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-indigo-500 shrink-0" />
                        <span>{exam.totalDurationMinutes} phút</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <HelpCircle className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                        <span>{exam.totalQuestions} câu hỏi</span>
                      </div>
                      <div className="flex items-center gap-1.5 col-span-2">
                        <Award className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                        <span>Điểm đỗ: {exam.overallPassingScore}/{exam.totalScaledMaxScore} điểm</span>
                      </div>
                    </div>
                  </div>

                  <div className="px-4 pb-4 pt-0 flex items-center gap-2">
                    <button
                      onClick={() => handleOpenStartModal(exam)}
                      disabled={loadingDetailId === exam.id}
                      className="flex-1 py-2 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white rounded-lg font-bold text-sm shadow-xs transition-all flex items-center justify-center gap-2 disabled:opacity-70 cursor-pointer"
                    >
                      {loadingDetailId === exam.id ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <Play className="w-4 h-4 fill-current" />
                      )}
                      <span>{loadingDetailId === exam.id ? 'Đang chuẩn bị...' : 'Vào thi ngay'}</span>
                    </button>
                    {isTeacherOrAdmin && (
                      <button
                        type="button"
                        onClick={() => navigate(`/admin/exams/${exam.id}/preview`)}
                        className="p-2 bg-slate-100 hover:bg-indigo-50 text-slate-700 hover:text-indigo-600 rounded-lg transition border border-gray-200 hover:border-indigo-200 cursor-pointer"
                        title="Biên soạn / Chỉnh sửa đề thi"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* CỘT PHẢI (4-5 CỘT TRÊN DESKTOP): BẢNG XẾP HẠNG MAZII STICKY */}
        <div className="lg:col-span-5 xl:col-span-4 lg:sticky-panel" id="leaderboard">
          <LeaderboardWidget
            initialLevel={selectedLevel === 'ALL' ? 'N5' : selectedLevel}
          />
        </div>
      </div>

      {/* Modal Chọn Chế Độ Thi (Full Exam hoặc Practice Section) */}
      {selectedExam && (
        <div className="fixed inset-0 overscroll-contain z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-xl max-w-lg w-full overflow-hidden shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-200">
            <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between bg-slate-50">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
                  JLPT {selectedExam.jlptLevel}
                </span>
                <h3 className="text-base font-black text-gray-900 line-clamp-1">
                  {selectedExam.title}
                </h3>
              </div>
              <button
                onClick={() => setSelectedExam(null)}
                className="w-8 h-8 rounded-full bg-gray-200/80 hover:bg-gray-300 flex items-center justify-center text-gray-600 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                  1. Chọn Chế độ làm bài:
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setAttemptType('FULL_EXAM')
                      setSelectedSectionId(null)
                    }}
                    className={`p-3 rounded-xl border text-left transition-all ${
                      attemptType === 'FULL_EXAM'
                        ? 'border-indigo-600 bg-indigo-50/70 text-indigo-950 ring-2 ring-indigo-600'
                        : 'border-gray-200 hover:border-gray-300 bg-surface text-gray-700'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-black text-sm mb-1 text-indigo-600">
                      <Flame className="w-4 h-4" />
                      <span>Thi cả bài</span>
                    </div>
                    <p className="text-xs text-gray-500 font-medium">
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
                    className={`p-3 rounded-xl border text-left transition-all ${
                      attemptType === 'PRACTICE_SECTION'
                        ? 'border-indigo-600 bg-indigo-50/70 text-indigo-950 ring-2 ring-indigo-600'
                        : 'border-gray-200 hover:border-gray-300 bg-surface text-gray-700'
                    }`}
                  >
                    <div className="flex items-center gap-2 font-black text-sm mb-1 text-emerald-600">
                      <BookOpen className="w-4 h-4" />
                      <span>Luyện riêng ca</span>
                    </div>
                    <p className="text-xs text-gray-500 font-medium">
                      Chỉ làm riêng 1 phần (Từ vựng, Đọc hiểu, hoặc Nghe).
                    </p>
                  </button>
                </div>
              </div>

              {/* Nếu chọn Luyện riêng ca -> Chọn ca cụ thể */}
              {attemptType === 'PRACTICE_SECTION' && (
                <div>
                  <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                    2. Chọn Phần thi bạn muốn luyện tập:
                  </label>
                  <div className="space-y-2.5">
                    {Array.from(new Map(selectedExam.sections.map((s) => [s.id, s])).values()).map((sec) => {
                      const viName = sec.sessionType === 'KNOWLEDGE_READING'
                        ? 'Kiến thức ngôn ngữ & Đọc hiểu'
                        : sec.sessionType === 'VOCABULARY'
                        ? 'Chữ Hán - Từ vựng'
                        : sec.sessionType === 'GRAMMAR_READING'
                        ? 'Ngữ pháp & Đọc hiểu'
                        : 'Nghe hiểu'

                      const jpName = sec.sessionType === 'KNOWLEDGE_READING'
                        ? '言語知識（文字・語彙・文法）・読解'
                        : sec.sessionType === 'VOCABULARY'
                        ? '言語知識（文字・語彙）'
                        : sec.sessionType === 'GRAMMAR_READING'
                        ? '言語知識（文法）・読解'
                        : '聴解'

                      return (
                        <button
                          key={sec.id}
                          type="button"
                          onClick={() => setSelectedSectionId(sec.id)}
                          className={`w-full p-3 rounded-xl border text-left flex items-center justify-between transition-all ${
                            selectedSectionId === sec.id
                              ? 'border-indigo-600 bg-indigo-50/70 text-indigo-950 font-bold ring-2 ring-indigo-500/20 shadow-xs'
                              : 'border-gray-200 bg-surface text-gray-700 font-medium hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex items-center gap-3.5">
                            <div className="w-9 h-9 rounded-lg bg-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
                              {sec.sessionType === 'LISTENING' ? (
                                <Headphones className="w-5 h-5" />
                              ) : (
                                <BookOpen className="w-5 h-5" />
                              )}
                            </div>
                            <div>
                              <div className="text-sm font-bold text-gray-900 flex items-center gap-2">
                                <span>Phần {sec.orderIndex}: {viName}</span>
                              </div>
                              <div className="text-xs text-gray-500 mt-0.5 flex items-center gap-2">
                                <span className="font-japanese text-indigo-700 font-semibold">{jpName}</span>
                                <span>•</span>
                                <span className="text-indigo-600 font-semibold">{sec.durationMinutes} phút</span>
                                <span>•</span>
                                <span>{sec.questionCount} câu hỏi</span>
                              </div>
                            </div>
                          </div>
                          {selectedSectionId === sec.id && (
                            <CheckCircle2 className="w-5 h-5 text-indigo-600 shrink-0" />
                          )}
                        </button>
                      )
                    })}
                  </div>
                </div>
              )}
            </div>

            <div className="px-5 py-4 bg-slate-50 border-t border-gray-100 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setSelectedExam(null)}
                className="px-5 py-2.5 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-200 transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                disabled={starting}
                onClick={handleStartExam}
                className="px-6 py-2.5 rounded-xl text-sm font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs transition flex items-center gap-2 disabled:opacity-50"
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
        <div className="fixed inset-0 overscroll-contain z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-xl max-w-md w-full overflow-hidden shadow-2xl border border-gray-100 p-6 text-center animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center mx-auto mb-3 shadow-xs">
              <Award className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-black text-gray-900 mb-2">
              Yêu cầu đăng nhập
            </h3>
            <p className="text-xs sm:text-sm text-gray-600 leading-relaxed mb-6 font-medium">
              Để kết quả bài làm được lưu lại, chấm điểm Scaled Score chuẩn JLPT và <strong className="text-indigo-600 font-bold">ghi danh lên Bảng xếp hạng</strong>, bạn vui lòng đăng nhập trước khi bắt đầu!
            </p>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setShowLoginModal(false)}
                className="flex-1 py-2.5 px-4 rounded-lg text-sm font-bold text-gray-600 bg-gray-100 hover:bg-gray-200 transition cursor-pointer"
              >
                Để sau
              </button>
              <button
                type="button"
                onClick={() => navigate('/login?redirect=/exams')}
                className="flex-1 py-2.5 px-4 rounded-lg text-sm font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-md hover:shadow-indigo-500/25 transition cursor-pointer"
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
