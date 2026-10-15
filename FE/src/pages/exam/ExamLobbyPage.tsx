import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { examService } from '@/services'
import type { ExamSummary, ExamDetail, JlptLevel, AttemptType } from '@/types'
import { 
  Award, Clock, HelpCircle, CheckCircle2, ChevronRight, 
  Flame, BookOpen, Headphones, Play, X, Loader2, Sparkles, Filter
} from 'lucide-react'

export const ExamLobbyPage: React.FC = () => {
  const navigate = useNavigate()
  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | 'ALL'>('ALL')
  const [exams, setExams] = useState<ExamSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // State cho Modal chọn chế độ thi
  const [selectedExam, setSelectedExam] = useState<ExamDetail | null>(null)
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
    try {
      setLoadingDetailId(examSummary.id)
      const res = await examService.getExamDetail(examSummary.id)
      if (res.data) {
        setSelectedExam(res.data)
        setAttemptType('FULL_EXAM')
        setSelectedSectionId(null)
      }
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Lỗi khi tải chi tiết đề thi')
    } finally {
      setLoadingDetailId(null)
    }
  }

  const handleStartExam = async () => {
    if (!selectedExam) return
    if (attemptType === 'PRACTICE_SECTION' && !selectedSectionId) {
      alert('Vui lòng chọn phần thi bạn muốn luyện tập!')
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
      alert(err?.response?.data?.message || 'Không thể bắt đầu bài thi')
    } finally {
      setStarting(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Banner Giới thiệu */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-indigo-900 via-indigo-800 to-indigo-700 text-white p-8 md:p-12 mb-10 shadow-xl">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/10 backdrop-blur-md text-indigo-200 text-xs font-bold uppercase tracking-wider mb-4 border border-white/10">
            <Sparkles className="w-4 h-4 text-amber-400" />
            <span>Phòng Thi Thử JLPT Trực Tuyến Chuẩn Quốc Tế</span>
          </div>
          <h1 className="text-3xl md:text-5xl font-black tracking-tight mb-4 leading-tight">
            Chinh Phục Kỳ Thi JLPT <br className="hidden md:inline" />
            <span className="text-amber-300">Tự Tin Đạt Điểm Cao</span>
          </h1>
          <p className="text-indigo-100/90 text-sm md:text-base leading-relaxed mb-6 font-medium">
            Hệ thống mô phỏng đề thi thật 100%: đầy đủ các ca thi, tính giờ độc lập, chống dính điểm liệt, 
            bài đọc tờ rơi đồ họa sắc nét và nghe lại từng câu kèm transcript chi tiết.
          </p>
          <div className="flex flex-wrap gap-4 text-xs md:text-sm font-semibold">
            <div className="flex items-center gap-2 bg-white/10 px-3.5 py-2 rounded-xl border border-white/10">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Chấm điểm Scaled Score chuẩn</span>
            </div>
            <div className="flex items-center gap-2 bg-white/10 px-3.5 py-2 rounded-xl border border-white/10">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Kiểm soát điểm liệt từng phần</span>
            </div>
          </div>
        </div>
        <div className="absolute right-0 bottom-0 opacity-10 md:opacity-20 translate-x-12 translate-y-12">
          <Award className="w-96 h-96 text-white" />
        </div>
      </div>

      {/* Tabs Lọc Cấp Độ N1 - N5 */}
      <div className="flex items-center justify-between mb-8 flex-wrap gap-4">
        <div className="flex items-center gap-2 bg-white p-1.5 rounded-2xl border border-gray-200 shadow-xs">
          <div className="px-3 text-xs font-bold text-gray-400 flex items-center gap-1.5 uppercase">
            <Filter className="w-3.5 h-3.5" />
            <span>Cấp độ:</span>
          </div>
          {levels.map((lvl) => (
            <button
              key={lvl}
              onClick={() => setSelectedLevel(lvl)}
              className={`px-4 py-2 rounded-xl text-sm font-bold transition-all ${
                selectedLevel === lvl
                  ? 'bg-indigo-600 text-white shadow-sm scale-102'
                  : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-100'
              }`}
            >
              {lvl === 'ALL' ? 'Tất cả' : lvl}
            </button>
          ))}
        </div>

        <button
          onClick={() => navigate('/exam-history')}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white border border-gray-200 text-gray-700 hover:text-indigo-600 hover:border-indigo-300 font-bold text-sm shadow-xs transition"
        >
          <Clock className="w-4 h-4 text-indigo-500" />
          <span>Lịch sử thi của tôi</span>
        </button>
      </div>

      {/* Danh sách đề thi */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <Loader2 className="w-10 h-10 animate-spin text-indigo-600 mb-4" />
          <p className="font-semibold text-sm">Đang tải danh sách đề thi...</p>
        </div>
      ) : error ? (
        <div className="p-6 bg-red-50 border border-red-200 rounded-2xl text-red-600 text-center font-medium">
          {error}
        </div>
      ) : exams.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-3xl border border-gray-200/80 shadow-xs p-8">
          <BookOpen className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-bold text-gray-800 mb-1">Chưa có đề thi nào ở cấp độ này</h3>
          <p className="text-sm text-gray-500 max-w-md mx-auto">
            Hệ thống đang cập nhật thêm đề thi mới. Bạn vui lòng chọn cấp độ khác để trải nghiệm!
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {exams.map((exam) => (
            <div
              key={exam.id}
              className="bg-white rounded-3xl border border-gray-200/90 hover:border-indigo-300 hover:shadow-xl transition-all duration-300 flex flex-col justify-between overflow-hidden group"
            >
              <div className="p-6">
                <div className="flex items-center justify-between gap-2 mb-4">
                  <span className={`px-3 py-1 rounded-xl text-xs font-black uppercase tracking-wider ${
                    exam.jlptLevel === 'N5' ? 'bg-emerald-100 text-emerald-800' :
                    exam.jlptLevel === 'N4' ? 'bg-teal-100 text-teal-800' :
                    exam.jlptLevel === 'N3' ? 'bg-blue-100 text-blue-800' :
                    exam.jlptLevel === 'N2' ? 'bg-purple-100 text-purple-800' :
                    'bg-rose-100 text-rose-800'
                  }`}>
                    JLPT {exam.jlptLevel}
                  </span>
                  <span className="text-xs font-bold text-gray-400">
                    Thang điểm: {exam.totalScaledMaxScore}đ
                  </span>
                </div>

                <h3 className="text-base font-bold text-gray-900 group-hover:text-indigo-600 transition-colors line-clamp-2 mb-3">
                  {exam.title}
                </h3>

                <div className="grid grid-cols-2 gap-3 text-xs font-semibold text-gray-600 bg-slate-50 p-3.5 rounded-2xl mb-4">
                  <div className="flex items-center gap-2">
                    <Clock className="w-4 h-4 text-indigo-500" />
                    <span>{exam.totalDurationMinutes} phút</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <HelpCircle className="w-4 h-4 text-emerald-500" />
                    <span>{exam.totalQuestions} câu hỏi</span>
                  </div>
                  <div className="flex items-center gap-2 col-span-2">
                    <Award className="w-4 h-4 text-amber-500" />
                    <span>Điểm đỗ: {exam.overallPassingScore}/{exam.totalScaledMaxScore} điểm</span>
                  </div>
                </div>
              </div>

              <div className="px-6 pb-6 pt-0">
                <button
                  onClick={() => handleOpenStartModal(exam)}
                  disabled={loadingDetailId === exam.id}
                  className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white rounded-2xl font-bold text-sm shadow-xs hover:shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70"
                >
                  {loadingDetailId === exam.id ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Play className="w-4 h-4 fill-current" />
                  )}
                  <span>{loadingDetailId === exam.id ? 'Đang chuẩn bị...' : 'Vào thi ngay'}</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Chọn Chế Độ Thi (Full Exam hoặc Practice Section) */}
      {selectedExam && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full overflow-hidden shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-200">
            <div className="p-6 border-b border-gray-100 flex items-center justify-between bg-slate-50">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
                  JLPT {selectedExam.jlptLevel}
                </span>
                <h3 className="text-lg font-black text-gray-900 line-clamp-1">
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

            <div className="p-6 space-y-5">
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
                    className={`p-4 rounded-2xl border text-left transition-all ${
                      attemptType === 'FULL_EXAM'
                        ? 'border-indigo-600 bg-indigo-50/70 text-indigo-950 ring-2 ring-indigo-600'
                        : 'border-gray-200 hover:border-gray-300 bg-white text-gray-700'
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
                    className={`p-4 rounded-2xl border text-left transition-all ${
                      attemptType === 'PRACTICE_SECTION'
                        ? 'border-indigo-600 bg-indigo-50/70 text-indigo-950 ring-2 ring-indigo-600'
                        : 'border-gray-200 hover:border-gray-300 bg-white text-gray-700'
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
                      const viName = sec.sessionType === 'VOCABULARY'
                        ? 'Chữ Hán - Từ vựng'
                        : sec.sessionType === 'GRAMMAR_READING'
                        ? 'Ngữ pháp & Đọc hiểu'
                        : 'Nghe hiểu'

                      const jpName = sec.sessionType === 'VOCABULARY'
                        ? '言語知識（文字・語彙）'
                        : sec.sessionType === 'GRAMMAR_READING'
                        ? '言語知識（文法）・読解'
                        : '聴解'

                      return (
                        <button
                          key={sec.id}
                          type="button"
                          onClick={() => setSelectedSectionId(sec.id)}
                          className={`w-full p-4 rounded-2xl border text-left flex items-center justify-between transition-all ${
                            selectedSectionId === sec.id
                              ? 'border-indigo-600 bg-indigo-50/70 text-indigo-950 font-bold ring-2 ring-indigo-500/20 shadow-xs'
                              : 'border-gray-200 bg-white text-gray-700 font-medium hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex items-center gap-3.5">
                            <div className="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
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

            <div className="p-6 bg-slate-50 border-t border-gray-100 flex items-center justify-end gap-3">
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
    </div>
  )
}
