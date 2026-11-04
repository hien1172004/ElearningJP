import React, { useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { LoadingState } from '@/components/common'
import {
  LeaderboardBlock,
  ProgressChartBlock,
  ProgressSummaryBlock,
  StudyCalendarBlock,
  TopLookupsBlock,
} from '@/components/progress'
import { resolvePresetRange, type ProgressRange } from '@/utils/progress'
import { useProgressGoal } from '@/hooks/useProgressGoal'
import { useProgressSummary } from '@/hooks/useProgressSummary'

/**
 * ProgressDashboard — nội dung trang "Tiến độ học tập" (Mazii-style).
 *
 * Bố cục desktop (không banner đầu trang):
 * <pre>
 * ┌──────────────────────────────┬──────────────┐
 * │ Tổng hợp (bộ lọc kỳ + mascot)│ Lịch streak  │  ← chung 1 thẻ (2xl 7/5, xl 6/6)
 * ├───────────────┬──────────────┼──────────────┤
 * │ Biểu đồ (5)   │ Top từ (3)   │ Xếp hạng (4) │  ← 3 thẻ (2xl); xl: biểu đồ 1 hàng riêng
 * └───────────────┴──────────────┴──────────────┘
 * </pre>
 *
 * Route: `/progress` (protected — cần đăng nhập).
 */
const ProgressDashboard: React.FC = () => {
  const [range, setRange] = useState<ProgressRange>(() => resolvePresetRange('month'))

  const queryParams = useMemo(
    () => ({ from: range.from, to: range.to, allTime: range.preset === 'all' || undefined }),
    [range.from, range.to, range.preset],
  )

  const { data, isLoading, isError, refetch } = useProgressSummary(queryParams)

  return (
    <div className="space-y-4 pb-12">
      {/* ======================= HÀNG 1 — TỔNG HỢP + LỊCH STREAK (chung 1 thẻ, không banner) ======================= */}
      <div className="bg-white rounded-xl border border-suna shadow-xs p-4 sm:p-5 grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        <div className="xl:col-span-6 2xl:col-span-7 min-w-0">
          <ProgressSummaryBlock
            summary={data}
            isLoading={isLoading}
            isError={isError}
            onRefetch={() => void refetch()}
            preset={range.preset}
            from={range.from}
            to={range.to}
            onRangeChange={setRange}
          />
        </div>
        <div className="xl:col-span-6 2xl:col-span-5 min-w-0 pt-5 xl:pt-0 xl:pl-5 border-t xl:border-t-0 xl:border-l border-suna">
          <StudyCalendarBlock />
        </div>
      </div>

      {/* ======================= HÀNG 2 — BIỂU ĐỒ + TỪ ĐÃ TRA + XẾP HẠNG ======================= */}
      {/* Biểu đồ dùng CHUNG bộ lọc kỳ với khối tổng hợp (state `range`). */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-4 items-stretch">
        <div className="xl:col-span-12 2xl:col-span-5 min-w-0">
          <ProgressChartBlock from={range.from} to={range.to} preset={range.preset} />
        </div>
        <div className="xl:col-span-6 2xl:col-span-3 min-w-0 bg-white rounded-xl border border-suna shadow-xs p-4">
          <TopLookupsBlock />
        </div>
        <div className="xl:col-span-6 2xl:col-span-4 min-w-0 bg-white rounded-xl border border-suna shadow-xs p-4">
          <LeaderboardBlock />
        </div>
      </div>
    </div>
  )
}

/**
 * ProgressPage — chưa chọn mục tiêu thì chuyển sang trang chọn mục tiêu.
 * Lỗi khi tải mục tiêu vẫn hiện số liệu, không chặn cả trang.
 */
export const ProgressPage: React.FC = () => {
  const { data: goal, isLoading } = useProgressGoal()

  if (isLoading) return <LoadingState fullHeight />
  if (goal && !goal.configured) return <Navigate to="/progress/goal" replace />
  return <ProgressDashboard />
}

export default ProgressPage