import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { examService } from '../../services/exam.service'
import type { ExamResult } from '../../types/exam.types'
import {
  Award,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  ArrowRight,
  RotateCcw,
  History,
  FileText,
  Calendar,
  Layers,
} from 'lucide-react'

export const ExamResultPage: React.FC = () => {
  const { attemptId } = useParams<{ attemptId: string }>()
  const navigate = useNavigate()
  const [result, setResult] = useState<ExamResult | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!attemptId) return
    const fetchResult = async () => {
      try {
        setLoading(true)
        const data = await examService.getExamResult(Number(attemptId))
        setResult(data)
      } catch (err: any) {
        setError(err.response?.data?.message || 'Không thể tải kết quả thi')
      } finally {
        setLoading(false)
      }
    }
    fetchResult()
  }, [attemptId])

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-600 dark:text-slate-400 font-medium">Đang tổng hợp phiếu điểm...</p>
        </div>
      </div>
    )
  }

  if (error || !result) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex items-center justify-center p-4">
        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8 max-w-md w-full text-center">
          <XCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-slate-800 dark:text-white mb-2">Lỗi tải kết quả</h2>
          <p className="text-slate-600 dark:text-slate-400 text-sm mb-6">{error || 'Không tìm thấy dữ liệu kết quả bài thi.'}</p>
          <button
            onClick={() => navigate('/exams')}
            className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-xl transition"
          >
            Quay lại sảnh thi
          </button>
        </div>
      </div>
    )
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins} phút ${secs} giây`
  }

  const getGroupName = (code: string) => {
    switch (code) {
      case 'LANGUAGE_KNOWLEDGE':
        return 'Kiến thức ngôn ngữ (Từ vựng - Ngữ pháp)'
      case 'READING':
        return 'Đọc hiểu'
      case 'LANGUAGE_KNOWLEDGE_READING':
        return 'Kiến thức ngôn ngữ & Đọc hiểu'
      case 'LISTENING':
        return 'Nghe hiểu'
      default:
        return code
    }
  }

  const isFullExam = result.attemptType === 'FULL_EXAM'

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-slate-100 to-indigo-50/30 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950 py-10 px-4">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Certificate Card Header */}
        <div className="bg-white dark:bg-slate-800 rounded-3xl shadow-xl border border-slate-200 dark:border-slate-700/60 overflow-hidden relative">
          {/* Top banner accent */}
          <div
            className={`h-3 w-full ${
              result.passed
                ? 'bg-gradient-to-r from-emerald-500 to-teal-400'
                : 'bg-gradient-to-r from-rose-500 to-amber-500'
            }`}
          />

          <div className="p-6 md:p-10">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 pb-8 border-b border-slate-100 dark:border-slate-700">
              <div>
                <div className="flex items-center gap-2 mb-2">
                  <span className="px-3 py-1 bg-indigo-50 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 rounded-lg text-xs font-bold uppercase tracking-wider">
                    {result.jlptLevel} • {isFullExam ? 'THI THỬ CHUẨN JLPT' : 'LUYỆN TẬP PHÂN HỆ'}
                  </span>
                  <span className="text-xs text-slate-400 flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5" />
                    {new Date(result.submittedAt).toLocaleDateString('vi-VN')}
                  </span>
                </div>
                <h1 className="text-2xl md:text-3xl font-extrabold text-slate-900 dark:text-white">
                  {result.examTitle}
                </h1>
                <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-slate-400" />
                  Thời gian làm bài: <span className="font-semibold text-slate-700 dark:text-slate-300">{formatTime(result.timeSpentSeconds)}</span>
                </p>
              </div>

              {/* Status Badge */}
              <div
                className={`flex items-center gap-3.5 px-5 py-3.5 md:px-6 md:py-4 rounded-2xl border shrink-0 whitespace-nowrap shadow-xs ${
                  result.passed
                    ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800/60 text-emerald-700 dark:text-emerald-300'
                    : 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800/60 text-rose-700 dark:text-rose-300'
                }`}
              >
                {result.passed ? (
                  <CheckCircle2 className="w-8 h-8 text-emerald-500 shrink-0" />
                ) : (
                  <XCircle className="w-8 h-8 text-rose-500 shrink-0" />
                )}
                <div className="shrink-0">
                  <div className="text-xs font-semibold uppercase tracking-wider opacity-80 whitespace-nowrap">
                    Kết quả tổng thể
                  </div>
                  <div className="text-xl md:text-2xl font-black whitespace-nowrap">
                    {result.passed ? 'ĐẠT (合格)' : 'KHÔNG ĐẠT (不合格)'}
                  </div>
                </div>
              </div>
            </div>

            {/* Threshold failed alert */}
            {result.hasThresholdFailed && (
              <div className="mt-6 p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800/60 rounded-2xl flex items-start gap-3.5">
                <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-amber-900 dark:text-amber-200">
                  <span className="font-bold">Cảnh báo điểm liệt:</span> Tổng điểm của bạn có thể đạt hoặc gần ngưỡng đỗ, nhưng bạn có phân môn không đạt mức điểm sàn tối thiểu quy định theo chuẩn JLPT.
                </div>
              </div>
            )}

            {/* Score Breakdown Section */}
            <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Main Scaled Score Card */}
              <div className="md:col-span-1 bg-gradient-to-br from-indigo-600 to-indigo-800 text-white rounded-2xl p-6 shadow-md flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between opacity-80 text-xs font-semibold uppercase tracking-wider mb-2">
                    <span>Điểm chuẩn hóa</span>
                    <Award className="w-4 h-4" />
                  </div>
                  <div className="text-4xl md:text-5xl font-black">
                    {result.totalScaledScore}
                    <span className="text-xl font-normal opacity-70"> / {result.totalScaledMaxScore}</span>
                  </div>
                  <p className="text-xs text-indigo-200 mt-2">
                    Điểm sàn yêu cầu: <span className="font-semibold text-white">{result.overallPassingScore} điểm</span>
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-indigo-500/50">
                  <div className="text-xs text-indigo-200">Điểm thô (Số câu đúng):</div>
                  <div className="text-xl font-bold mt-0.5">{result.totalRawScore} câu</div>
                </div>
              </div>

              {/* Group scores breakdown */}
              <div className="md:col-span-2 space-y-4">
                <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider flex items-center gap-2">
                  <Layers className="w-4 h-4 text-indigo-500" />
                  Điểm theo từng phần thi
                </h3>

                <div className="space-y-3">
                  {result.groupScores.map((group) => {
                    const percent = Math.round((group.scaledScore / group.maxScaledScore) * 100)
                    return (
                      <div
                        key={group.scoreGroupId}
                        className="p-4 bg-slate-50 dark:bg-slate-900/60 rounded-2xl border border-slate-200 dark:border-slate-700"
                      >
                        <div className="flex items-center justify-between text-sm mb-1.5">
                          <span className="font-semibold text-slate-800 dark:text-slate-200">
                            {getGroupName(group.groupCode)}
                          </span>
                          <span className="font-bold text-slate-900 dark:text-white">
                            {group.scaledScore} / {group.maxScaledScore} điểm
                          </span>
                        </div>

                        {/* Progress Bar */}
                        <div className="w-full bg-slate-200 dark:bg-slate-700 h-2.5 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all duration-500 ${
                              group.groupPassed ? 'bg-indigo-600' : 'bg-rose-500'
                            }`}
                            style={{ width: `${Math.min(100, Math.max(0, percent))}%` }}
                          />
                        </div>

                        <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400 mt-2">
                          <span>
                            Đúng: <strong className="text-slate-700 dark:text-slate-300">{group.rawCorrectCount} / {group.rawTotalCount} câu</strong>
                          </span>
                          <span>
                            Điểm liệt: <strong className="text-rose-500 font-semibold">&lt; {group.minRequiredScore}đ</strong> • {group.groupPassed ? (
                              <span className="text-emerald-600 dark:text-emerald-400 font-medium">Qua ngưỡng</span>
                            ) : (
                              <span className="text-rose-600 font-bold">Dưới ngưỡng liệt</span>
                            )}
                          </span>
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="mt-10 pt-6 border-t border-slate-100 dark:border-slate-700 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-3 w-full sm:w-auto">
                <button
                  onClick={() => navigate('/exam-history')}
                  className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 font-medium hover:bg-slate-50 dark:hover:bg-slate-700 transition"
                >
                  <History className="w-4 h-4 text-slate-500" />
                  Lịch sử thi
                </button>
                <button
                  onClick={() => navigate('/exams')}
                  className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 font-medium hover:bg-slate-50 dark:hover:bg-slate-700 transition"
                >
                  <RotateCcw className="w-4 h-4 text-slate-500" />
                  Sảnh đề thi
                </button>
              </div>

              <button
                onClick={() => navigate(`/exams/review/${result.attemptId}`)}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold shadow-lg shadow-indigo-600/30 transition group"
              >
                <FileText className="w-5 h-5" />
                <span>Xem lại chi tiết bài làm & Lời giải</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
export default ExamResultPage
