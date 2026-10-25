import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { examService } from '../../services/exam.service'
import type { ExamAttemptHistory } from '../../types/exam.types'
import {
  History,
  Calendar,
  CheckCircle2,
  XCircle,
  FileText,
  Search,
  TrendingUp,
  Award,
  BookOpen,
} from 'lucide-react'

export const ExamHistoryPage: React.FC = () => {
  const navigate = useNavigate()
  const [historyList, setHistoryList] = useState<ExamAttemptHistory[]>([])
  const [loading, setLoading] = useState<boolean>(true)

  // Filters
  const [selectedLevel, setSelectedLevel] = useState<string>('ALL')
  const [searchTerm, setSearchTerm] = useState<string>('')

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        setLoading(true)
        const data = await examService.getMyExamHistory()
        setHistoryList(Array.isArray(data) ? data : [])
      } catch (err: any) {
        console.error('Lỗi khi tải lịch sử thi:', err)
        setHistoryList([])
      } finally {
        setLoading(false)
      }
    }
    fetchHistory()
  }, [])

  const safeList = Array.isArray(historyList) ? historyList : []

  const filteredHistory = safeList.filter((item) => {
    const matchesLevel = selectedLevel === 'ALL' || item.jlptLevel === selectedLevel
    const matchesSearch =
      searchTerm.trim() === '' ||
      (item.examTitle && item.examTitle.toLowerCase().includes(searchTerm.toLowerCase()))
    return matchesLevel && matchesSearch
  })

  // Quick stats
  const totalAttempts = safeList.length
  const passedAttempts = safeList.filter((h) => h.passed).length
  const passRate = totalAttempts > 0 ? Math.round((passedAttempts / totalAttempts) * 100) : 0
  const avgScore =
    totalAttempts > 0
      ? Math.round(
          safeList.reduce((acc, curr) => acc + (curr.totalScaledScore || 0), 0) / totalAttempts
        )
      : 0

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 py-8 px-4 lg:px-8">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header Title */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
                <History className="w-5 h-5" />
              </span>
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                Hồ sơ học tập
              </span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-slate-900 dark:text-white">
              Lịch sử thi thử JLPT
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
              Xem lại toàn bộ kết quả, phiếu điểm và bảng đối chiếu đáp án chi tiết các kỳ thi đã làm.
            </p>
          </div>

          <button
            onClick={() => navigate('/exams')}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold shadow-md shadow-indigo-600/20 transition self-start md:self-auto"
          >
            <BookOpen className="w-4 h-4" />
            <span>Vào sảnh đề mới</span>
          </button>
        </div>

        {/* Quick Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200 dark:border-slate-800 shadow-sm flex items-center justify-between">
            <div>
              <div className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
                Tổng số bài thi
              </div>
              <div className="text-2xl font-black text-slate-900 dark:text-white mt-1">
                {totalAttempts} bài
              </div>
            </div>
            <div className="w-12 h-12 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 flex items-center justify-center text-indigo-600">
              <FileText className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200 dark:border-slate-800 shadow-sm flex items-center justify-between">
            <div>
              <div className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
                Tỉ lệ đỗ (Pass rate)
              </div>
              <div className="text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-1">
                {passRate}%
              </div>
            </div>
            <div className="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 flex items-center justify-center text-emerald-600">
              <TrendingUp className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200 dark:border-slate-800 shadow-sm flex items-center justify-between">
            <div>
              <div className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
                Điểm trung bình
              </div>
              <div className="text-2xl font-black text-amber-600 dark:text-amber-400 mt-1">
                {avgScore} điểm
              </div>
            </div>
            <div className="w-12 h-12 rounded-xl bg-amber-50 dark:bg-amber-950/50 flex items-center justify-center text-amber-600">
              <Award className="w-6 h-6" />
            </div>
          </div>
        </div>

        {/* Filters and Search Bar */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl p-4 border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
          {/* Level Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
            {['ALL', 'N1', 'N2', 'N3', 'N4', 'N5'].map((lvl) => (
              <button
                key={lvl}
                onClick={() => setSelectedLevel(lvl)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                  selectedLevel === lvl
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                {lvl === 'ALL' ? 'Tất cả cấp độ' : lvl}
              </button>
            ))}
          </div>

          {/* Search box */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Tìm kiếm đề thi..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>

        {/* History List Table / Cards */}
        {loading ? (
          <div className="p-12 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800">
            <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-slate-500 text-sm">Đang tải lịch sử thi...</p>
          </div>
        ) : filteredHistory.length === 0 ? (
          <div className="p-12 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800">
            <History className="w-12 h-12 text-slate-400 mx-auto mb-3" />
            <h3 className="text-base font-bold text-slate-800 dark:text-slate-200">Chưa có lượt thi nào</h3>
            <p className="text-slate-500 text-xs mt-1">
              {searchTerm || selectedLevel !== 'ALL'
                ? 'Không tìm thấy kết quả phù hợp với bộ lọc.'
                : 'Bạn chưa tham gia thi thử hoặc luyện tập bài nào.'}
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredHistory.map((item) => {
              const isFull = item.attemptType === 'FULL_EXAM'
              const dateStr = item.submittedAt
                ? new Date(item.submittedAt).toLocaleDateString('vi-VN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                  })
                : 'Chưa hoàn thành'

              return (
                <div
                  key={item.attemptId}
                  className="bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200/80 dark:border-slate-800 hover:border-indigo-300 dark:hover:border-indigo-800 shadow-sm transition flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="px-2.5 py-0.5 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 text-xs font-black">
                        {item.jlptLevel}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 text-xs font-medium">
                        {isFull ? 'Thi Full chuẩn đề' : `Luyện tập: ${item.sectionTitle || 'Phân hệ'}`}
                      </span>
                      <span className="text-xs text-slate-400 flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5" />
                        {dateStr}
                      </span>
                    </div>

                    <h3 className="text-base font-bold text-slate-900 dark:text-white">
                      {item.examTitle}
                    </h3>

                    <div className="flex items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
                      <span>
                        Số câu đúng: <strong className="text-slate-700 dark:text-slate-300">{item.totalRawScore ?? 0} câu</strong>
                      </span>
                      <span>
                        Thời gian: <strong className="text-slate-700 dark:text-slate-300">{Math.round((item.timeSpentSeconds || 0) / 60)} phút</strong>
                      </span>
                    </div>
                  </div>

                  {/* Score & Badge */}
                  <div className="flex items-center gap-5 justify-between md:justify-end border-t md:border-t-0 pt-3 md:pt-0 border-slate-100 dark:border-slate-800">
                    <div className="text-right">
                      <div className="text-2xl font-black text-slate-900 dark:text-white">
                        {item.totalScaledScore ?? 0}
                        <span className="text-sm font-normal text-slate-400">/{item.totalScaledMaxScore || 180}</span>
                      </div>
                      <div
                        className={`text-xs font-bold inline-flex items-center gap-1 ${
                          item.passed ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
                        }`}
                      >
                        {item.passed ? (
                          <>
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Đỗ (合格)
                          </>
                        ) : (
                          <>
                            <XCircle className="w-3.5 h-3.5" />
                            Trượt (不合格)
                          </>
                        )}
                      </div>
                    </div>

                    {/* Action buttons */}
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => navigate(`/exams/result/${item.attemptId}`)}
                        className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-semibold transition"
                        title="Xem phiếu điểm"
                      >
                        Phiếu điểm
                      </button>
                      <button
                        onClick={() => navigate(`/exams/review/${item.attemptId}`)}
                        className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-sm transition"
                      >
                        <FileText className="w-4 h-4" />
                        <span>Xem lại bài làm</span>
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
export default ExamHistoryPage
