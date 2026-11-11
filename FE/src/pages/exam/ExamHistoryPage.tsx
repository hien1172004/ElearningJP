import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BackButton } from '@/components/common'
import { examService } from '../../services/exam.service'
import type { ExamAttemptHistory } from '../../types/exam.types'
import type { JlptLevel } from '@/types'
import { JLPT_LEVEL_BADGE } from '@/utils/jlpt'
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
  ClipboardList,
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
    <div className="pb-8">
      <div className="max-w-6xl mx-auto space-y-4">
        {/* Header: nút quay lại + hành động */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <BackButton to="/exams" />

          <button
            onClick={() => navigate('/exams')}
            className="inline-flex items-center gap-1.5 h-9 px-4 rounded-lg bg-ai hover:bg-ai/90 text-white text-xs font-medium shadow-2xs transition-all cursor-pointer self-start md:self-auto select-none"
          >
            <BookOpen className="w-4 h-4" />
            <span>Vào sảnh đề mới</span>
          </button>
        </div>

        {/* Quick Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="bg-surface rounded-2xl p-4 border border-suna/90 shadow-xs flex items-center justify-between">
            <div>
              <div className="text-[11px] font-semibold text-nibi uppercase tracking-wide">
                Tổng số bài thi
              </div>
              <div className="text-xl font-extrabold text-sumi mt-0.5">
                {totalAttempts} bài
              </div>
            </div>
            <div className="w-9 h-9 rounded-lg bg-kamenozoki text-ai border border-ai/20 flex items-center justify-center shadow-2xs">
              <FileText className="w-4 h-4" />
            </div>
          </div>

          <div className="bg-surface rounded-2xl p-4 border border-suna/90 shadow-xs flex items-center justify-between">
            <div>
              <div className="text-[11px] font-semibold text-nibi uppercase tracking-wide">
                Tỉ lệ đỗ (Pass rate)
              </div>
              <div className="text-xl font-extrabold text-emerald-700 mt-0.5">
                {passRate}%
              </div>
            </div>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center justify-center shadow-2xs">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>

          <div className="bg-surface rounded-2xl p-4 border border-suna/90 shadow-xs flex items-center justify-between">
            <div>
              <div className="text-[11px] font-semibold text-nibi uppercase tracking-wide">
                Điểm trung bình
              </div>
              <div className="text-xl font-extrabold text-amber-700 mt-0.5">
                {avgScore} điểm
              </div>
            </div>
            <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-700 border border-amber-200 flex items-center justify-center shadow-2xs">
              <Award className="w-4 h-4" />
            </div>
          </div>
        </div>

        {/* Filters and Search Bar */}
        <div className="bg-surface rounded-2xl p-3 border border-suna/90 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
          {/* Level Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
            {['ALL', 'N5', 'N4', 'N3', 'N2', 'N1'].map((lvl) => (
              <button
                key={lvl}
                onClick={() => setSelectedLevel(lvl)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer whitespace-nowrap ${
                  selectedLevel === lvl
                    ? 'bg-ai text-white shadow-2xs font-bold'
                    : 'text-nibi hover:text-ai hover:bg-kamenozoki/50'
                }`}
              >
                {lvl === 'ALL' ? 'Tất cả cấp độ' : lvl}
              </button>
            ))}
          </div>

          {/* Search box */}
          <div className="relative w-full md:w-72">
            <Search className="w-4 h-4 text-nibi absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Tìm kiếm đề thi..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-3 h-9 bg-surface border border-suna rounded-lg text-xs text-sumi focus:outline-none focus:border-ai transition-colors placeholder:text-nibi/50"
            />
          </div>
        </div>

        {/* History List Table / Cards */}
        {loading ? (
          <div className="p-10 text-center bg-surface rounded-2xl border border-suna">
            <div className="w-8 h-8 border-3 border-ai border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-nibi text-xs">Đang tải lịch sử thi...</p>
          </div>
        ) : filteredHistory.length === 0 ? (
          <div className="p-10 text-center bg-surface rounded-2xl border border-suna shadow-xs">
            <History className="w-10 h-10 text-nibi/40 mx-auto mb-3" />
            <h3 className="text-base font-bold text-sumi">Chưa có lượt thi nào</h3>
            <p className="text-nibi text-xs mt-1">
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

              const levelBadgeStyle =
                JLPT_LEVEL_BADGE[item.jlptLevel as JlptLevel] ||
                'bg-slate-100 text-nibi border border-suna'

              return (
                <div
                  key={item.attemptId}
                  className="bg-surface rounded-2xl p-5 border border-suna/90 hover:border-ai/40 shadow-xs transition-all duration-200 flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider ${levelBadgeStyle}`}>
                        JLPT {item.jlptLevel}
                      </span>
                      <span className="px-2.5 py-0.5 rounded-full bg-slate-100 text-nibi text-xs font-medium border border-suna">
                        {isFull ? 'Thi Full chuẩn đề' : `Luyện tập: ${item.sectionTitle || 'Phân hệ'}`}
                      </span>
                      <span className="text-xs text-nibi flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5 text-ai" />
                        {dateStr}
                      </span>
                    </div>

                    <h3 className="text-base font-bold text-sumi">
                      {item.examTitle}
                    </h3>

                    <div className="flex items-center gap-4 text-xs text-nibi">
                      <span>
                        Số câu đúng: <strong className="text-sumi">{item.totalRawScore ?? 0} câu</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Thời gian: <strong className="text-sumi">{Math.round((item.timeSpentSeconds || 0) / 60)} phút</strong>
                      </span>
                    </div>
                  </div>

                  {/* Score & Badge */}
                  <div className="flex items-center gap-5 justify-between md:justify-end border-t md:border-t-0 pt-3 md:pt-0 border-gray-100">
                    <div className="text-right">
                      <div className="text-xl font-bold text-sumi font-mono">
                        {item.totalScaledScore ?? 0}
                        <span className="text-xs font-normal text-nibi">/{item.totalScaledMaxScore || 180}</span>
                      </div>
                      <div className="mt-0.5">
                        {item.passed ? (
                          <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 inline-flex items-center gap-1 shadow-2xs">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Đỗ (合格)
                          </span>
                        ) : (
                          <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-shu border border-rose-200 inline-flex items-center gap-1 shadow-2xs">
                            <XCircle className="w-3.5 h-3.5" />
                            Trượt (不合格)
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Action buttons */}
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => navigate(`/exams/result/${item.attemptId}`)}
                        className="inline-flex items-center gap-1.5 h-8 px-3 rounded-lg border border-suna hover:border-ai/50 text-sumi hover:text-ai bg-surface text-xs font-medium shadow-2xs transition-all cursor-pointer whitespace-nowrap"
                        title="Xem phiếu điểm"
                      >
                        <ClipboardList className="w-3.5 h-3.5 text-ai" />
                        <span>Phiếu điểm</span>
                      </button>
                      <button
                        onClick={() => navigate(`/exams/review/${item.attemptId}`)}
                        className="inline-flex items-center gap-1.5 h-8 px-3.5 rounded-lg bg-ai hover:bg-ai/90 text-white text-xs font-medium shadow-2xs transition-all cursor-pointer whitespace-nowrap select-none"
                      >
                        <FileText className="w-3.5 h-3.5" />
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
