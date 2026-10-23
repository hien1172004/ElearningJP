import React, { useMemo, useState } from 'react'
import { BarChart3, Target, Flame, Sparkles } from 'lucide-react'
import {
  LeaderboardBlock,
  ProgressSummaryBlock,
  StudyCalendarBlock,
  TopLookupsBlock,
  type SummaryRangePreset,
} from '@/components/progress'
import { useProgressSummary } from '@/hooks/useProgressSummary'

/**
 * ProgressPage — trang "Tiến độ học tập" (Mazii-style).
 *
 * Bố cục (2 khối trên mỗi hàng ở desktop):
 * <pre>
 * ┌─────────────────────┬─────────────────────┐
 * │ Khối 1: Tổng hợp   │ Khối 2: Lịch học   │
 * │ (4 thẻ + mascot)   │ & chuỗi ngày       │
 * ├─────────────────────┼─────────────────────┤
 * │ Khối 3: Từ đã tra  │ Khối 4: Bảng xếp   │
 * │ nhiều nhất          │ hạng tuần          │
 * ├─────────────────────┴─────────────────────┤
 * │ Khối 5: Biểu đồ (placeholder)               │
 * └──────────────────────────────────────────────┘
 * </pre>
 *
 * Route: `/progress` (protected — cần đăng nhập).
 */
export const ProgressPage: React.FC = () => {
  const [range, setRange] = useState<{
    preset: SummaryRangePreset
    from?: string
    to?: string
  }>({ preset: '30d' })

  // Chỉ truyền from/to khi user chọn preset cụ thể (không phải mặc định).
  const queryParams = useMemo(
    () => ({ from: range.from, to: range.to }),
    [range.from, range.to],
  )

  const { data, isLoading, isError, refetch } = useProgressSummary(queryParams)

  return (
    <div className="space-y-4 pb-12">
      {/* ======================= HEADER ======================= */}
      <header className="bg-gradient-to-br from-indigo-700 via-indigo-800 to-slate-900 rounded-md px-5 py-4 text-white shadow-lg">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-indigo-200 text-xs font-medium mb-0.5">
              Hành trình chinh phục tiếng Nhật
            </p>
            <div className="text-xl md:text-2xl font-extrabold tracking-tight flex items-center gap-2">
              <BarChart3 className="w-6 h-6" />
              <span className="hidden md:inline">Tiến độ học tập</span>
            </div>
            <p className="text-indigo-100/80 text-xs mt-1 max-w-xl">
              Theo dõi số từ đã học, thời gian học mỗi ngày và chuỗi ngày liên tụp
              của bạn.
            </p>
          </div>

          {/* Quick stats tổng quan (nếu đã có data) */}
          {data && (
            <div className="flex gap-4 sm:gap-6">
              <div className="text-center">
                <div className="flex items-center justify-center gap-1.5 text-amber-300">
                  <Flame className="w-4 h-4" />
                  <span className="text-xl font-extrabold tabular-nums">
                    {data.currentStreak}
                  </span>
                </div>
                <p className="text-[10px] text-indigo-200 font-semibold mt-0.5">
                  ngày liên tiếp
                </p>
              </div>
              {/* <div className="text-center">
                <div className="flex items-center justify-center gap-1.5 text-emerald-300">
                  <Target className="w-4 h-4" />
                  <span className="text-xl font-extrabold tabular-nums">
                    {data.goalMetDays}
                  </span>
                </div>
                <p className="text-[10px] text-indigo-200 font-semibold mt-0.5">
                  ngày đạt goal
                </p>
              </div> */}
              <div className="text-center">
                <div className="flex items-center justify-center gap-1.5 text-sky-300">
                  <Sparkles className="w-4 h-4" />
                  <span className="text-xl font-extrabold tabular-nums">
                    {data.xpEarned}
                  </span>
                </div>
                <p className="text-[10px] text-indigo-200 font-semibold mt-0.5">
                  XP kỳ này
                </p>
              </div>
            </div>
          )}
        </div>
      </header>

      {/* ======================= KHỐI 1 & 2 — TỔNG HỢP + LỊCH ======================= */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4 items-start">
        <ProgressSummaryBlock
          summary={data}
          isLoading={isLoading}
          isError={isError}
          onRefetch={() => void refetch()}
          from={range.from}
          to={range.to}
          onRangeChange={setRange}
        />

        <StudyCalendarBlock />
      </div>

      {/* ======================= KHỐI 3 & 4 — TỪ ĐÃ TRA + XẾP HẠNG ======================= */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4 items-start">
        <TopLookupsBlock />
        <LeaderboardBlock />
      </div>

      {/* ======================= KHỐI 5 — BIỂU ĐỒ (chưa làm) ======================= */}
      <div className="bg-white rounded-md border border-dashed border-gray-300 px-4 py-3 flex items-center gap-3 opacity-60">
        <span className="w-9 h-9 rounded-md bg-gray-100 text-gray-400 flex items-center justify-center shrink-0">
          <BarChart3 className="w-4 h-4" />
        </span>
        <div className="min-w-0">
          <p className="font-bold text-gray-700 text-sm">Biểu đồ tiến độ</p>
          <p className="text-xs text-gray-500">
            Đường biểu diễn số từ học mỗi ngày.
          </p>
        </div>
        <span className="ml-auto text-[11px] text-indigo-500 font-semibold shrink-0">
          Sắp ra mắt
        </span>
      </div>
    </div>
  )
}

export default ProgressPage