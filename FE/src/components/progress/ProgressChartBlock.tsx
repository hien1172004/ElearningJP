import React from 'react'
import { Button } from 'antd'
import { BarChart3, CircleAlert, RefreshCw, Sparkles } from 'lucide-react'
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useProgressChart } from '@/hooks/useProgressChart'
import {
  chartGranularityForPreset,
  formatCompactNumber,
  formatIsoDate,
  type SummaryRangePreset,
} from '@/utils/progress'
import type { ChartGranularity, ProgressChartPoint } from '@/types'

/**
 * ProgressChartBlock — Khối 3: biểu đồ tiến độ.
 *
 * <p>Gồm 2 series (đúng như thiết kế):</p>
 * <ul>
 *   <li><b>Cột xanh</b> — số từ MỚI học trong mỗi bucket</li>
 *   <li><b>Đường cam</b> — số từ MỚI học tích lũy (cộng dồn, bắt đầu từ 0
 *       trong kỳ)</li>
 * </ul>
 *
 * <p><b>Dùng chung bộ lọc kỳ với Khối 1</b> — nhận {@code preset}/{@code from}/
 * {@code to} từ {@code ProgressPage} nên 2 khối luôn hiển thị cùng 1 kỳ.</p>
 *
 * <p><b>Đơn vị bucket do BE quyết định</b> (BE tự gộp để quy tắc tuần ISO
 * không lệch 1 ngày giữa 2 phía): FE chỉ suy ra để định dạng nhãn trục X.</p>
 *
 * ```tsx
 * <ProgressChartBlock from={from} to={to} preset="30d" />
 * ```
 */

interface ProgressChartBlockProps {
  /** Ngày bắt đầu `yyyy-MM-dd` (dùng chung với Khối 1). */
  from?: string
  /** Ngày kết thúc `yyyy-MM-dd` (dùng chung với Khối 1). */
  to?: string
  /** Preset kỳ đang chọn — suy ra granularity để gọi BE. */
  preset: SummaryRangePreset
}

// --------------------------------------------------------------------------
// Hằng số hiển thị
// --------------------------------------------------------------------------

/** Màu cột (số từ học trong bucket). */
const BAR_COLOR = '#3b82f6'

/** Màu đường (số từ tích lũy). */
const LINE_COLOR = '#f97316'

/** Nhãn legend. */
const BAR_LABEL = 'Từ mới học'
const LINE_LABEL = 'Từ mới học tích lũy'

// --------------------------------------------------------------------------
// Format nhãn trục X
// --------------------------------------------------------------------------

/** `yyyy-MM-dd` → `dd/MM`. */
function formatDayLabel(isoDate: string): string {
  const parts = isoDate.split('-')
  if (parts.length !== 3) return isoDate
  return `${parts[2]}/${parts[1]}`
}

/** `yyyy-MM-dd` → `MM/yy`. */
function formatMonthLabel(isoDate: string): string {
  const parts = isoDate.split('-')
  if (parts.length !== 3) return isoDate
  return `${parts[1]}/${parts[2].slice(2)}`
}

/**
 * Nhãn trục X theo đơn vị bucket.
 *
 * @param isoDate       ngày đầu bucket `yyyy-MM-dd`
 * @param granularity  đơn vị bucket
 */
function formatXAxisLabel(isoDate: string, granularity: ChartGranularity): string {
  if (granularity === 'month') return formatMonthLabel(isoDate)
  return formatDayLabel(isoDate)
}

/**
 * Nhãn đầy đủ trong tooltip.
 *
 * @param isoDate      ngày đầu bucket `yyyy-MM-dd`
 * @param granularity  đơn vị bucket
 */
function formatTooltipLabel(
  isoDate: string,
  granularity: ChartGranularity,
): string {
  if (granularity === 'day') return formatIsoDate(isoDate)
  if (granularity === 'week') return `Tuần ${formatDayLabel(isoDate)}`
  return `Tháng ${formatMonthLabel(isoDate)}`
}

// --------------------------------------------------------------------------
// Tooltip
// --------------------------------------------------------------------------

interface TooltipPayloadItem {
  dataKey?: string | number
  value?: number | string
}

interface ChartTooltipProps {
  active?: boolean
  payload?: TooltipPayloadItem[]
  label?: string
  granularity: ChartGranularity
}

const ChartTooltip: React.FC<ChartTooltipProps> = ({
  active,
  payload,
  label,
  granularity,
}) => {
  if (!active || !payload || payload.length === 0) return null

  const words = Number(
    payload.find((p) => p.dataKey === 'wordsLearned')?.value ?? 0,
  )
  const cumulative = Number(
    payload.find((p) => p.dataKey === 'cumulativeWords')?.value ?? 0,
  )

  return (
    <div className="rounded-md border border-gray-200 bg-white px-3 py-2 shadow-md">
      <p className="text-[11px] font-bold text-gray-700 mb-1">
        {label ? formatTooltipLabel(label, granularity) : ''}
      </p>
      <p className="text-[11px] text-gray-600 flex items-center gap-1.5">
        <span
          className="inline-block w-2 h-2 rounded-sm shrink-0"
          style={{ backgroundColor: BAR_COLOR }}
        />
        {BAR_LABEL}:{' '}
        <span className="font-bold text-gray-900 tabular-nums">{words}</span>
      </p>
      <p className="text-[11px] text-gray-600 flex items-center gap-1.5">
        <span
          className="inline-block w-2 h-2 rounded-sm shrink-0"
          style={{ backgroundColor: LINE_COLOR }}
        />
        {LINE_LABEL}:{' '}
        <span className="font-bold text-gray-900 tabular-nums">{cumulative}</span>
      </p>
    </div>
  )
}

// --------------------------------------------------------------------------
// Loading / Error / Empty
// --------------------------------------------------------------------------

const SkeletonBlock: React.FC = () => (
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-4">
    <div className="animate-pulse space-y-3">
      <div className="h-5 w-44 bg-gray-200 rounded" />
      <div className="h-[280px] w-full bg-gray-100 rounded-md" />
    </div>
  </div>
)

const ErrorBlock: React.FC<{ onRetry?: () => void; message?: string }> = ({
  onRetry,
  message,
}) => (
  <div className="bg-white rounded-md border border-rose-200 shadow-xs p-6">
    <div className="flex flex-col sm:flex-row items-center gap-4 text-center sm:text-left">
      <span className="flex items-center justify-center w-14 h-14 rounded-full bg-rose-50 text-rose-500 shrink-0">
        <CircleAlert className="w-7 h-7" aria-hidden="true" />
      </span>
      <div className="flex-1">
        <p className="font-bold text-gray-900">Không tải được biểu đồ</p>
        <p className="text-sm text-gray-500 mt-0.5">
          {message ?? 'Có lỗi xảy ra. Vui lòng thử lại sau.'}
        </p>
      </div>
      {onRetry && (
        <Button icon={<RefreshCw className="w-4 h-4" />} onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  </div>
)

const EmptyBlock: React.FC = () => (
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-6">
    <div className="flex flex-col sm:flex-row items-center gap-5 text-center sm:text-left">
      <span className="flex items-center justify-center w-[72px] h-[72px] rounded-full bg-gradient-to-br from-indigo-100 to-sky-100 text-indigo-500 shrink-0">
        <Sparkles className="w-9 h-9" aria-hidden="true" />
      </span>
      <div className="flex-1">
        <p className="font-bold text-gray-900 text-lg">Chưa có dữ liệu biểu đồ</p>
        <p className="text-sm text-gray-500 mt-1 max-w-md">
          Bạn chưa học từ nào mới trong kỳ này. Mỗi lần tra một từ MỚI trong
          từ điển sẽ được ghi nhận vào biểu đồ.
        </p>
      </div>
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Main block
// --------------------------------------------------------------------------

export const ProgressChartBlock: React.FC<ProgressChartBlockProps> = ({
  from,
  to,
  preset,
}) => {
  const granularity = chartGranularityForPreset(preset)

  const { data, isLoading, isError, error, refetch } = useProgressChart({
    from,
    to,
    granularity,
  })

  if (isLoading && !data) return <SkeletonBlock />
  if (isError && !data) {
    return (
      <ErrorBlock
        message={error?.message}
        onRetry={() => void refetch()}
      />
    )
  }

  const points: ProgressChartPoint[] = data?.points ?? []

  // Chưa học gì trong kỳ → không vẽ biểu đồ rỗng.
  if (points.length === 0 || (data?.totalWords ?? 0) === 0) {
    return <EmptyBlock />
  }

  // BE có thể tự cắt kỳ (trần 366 ngày) → hiển thị kỳ THỰC TẾ.
  const rangeLabel = data?.from && data?.to
    ? `${formatIsoDate(data.from)} — ${formatIsoDate(data.to)}`
    : null

  return (
    <section
      className="bg-white rounded-md border border-gray-200/80 shadow-xs p-4 space-y-3"
      aria-label="Biểu đồ tiến độ"
    >
      {/* ---------- Header ---------- */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-indigo-600" />
            Biểu đồ tiến độ
          </h2>
          {rangeLabel && (
            <p className="text-xs text-gray-500 mt-0.5">
              {rangeLabel}
              <span className="ml-1.5 text-gray-400">
                ({formatCompactNumber(data?.totalWords ?? 0)} từ)
              </span>
            </p>
          )}
        </div>

        <Button
          size="small"
          icon={<RefreshCw className="w-3.5 h-3.5" />}
          onClick={() => void refetch()}
          loading={isLoading}
          aria-label="Làm mới biểu đồ"
        >
          Làm mới
        </Button>
      </div>

      {/* ---------- Biểu đồ ---------- */}
      <div className="w-full h-[280px]">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart
            data={points}
            margin={{ top: 8, right: 8, bottom: 0, left: -12 }}
          >
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="#e5e7eb"
              vertical={false}
            />

            <XAxis
              dataKey="bucketStart"
              tickFormatter={(v: string) => formatXAxisLabel(v, granularity)}
              tick={{ fontSize: 11, fill: '#6b7280' }}
              axisLine={{ stroke: '#e5e7eb' }}
              tickLine={false}
              minTickGap={24}
            />

            <YAxis
              tick={{ fontSize: 11, fill: '#6b7280' }}
              axisLine={false}
              tickLine={false}
              width={48}
              tickFormatter={(v: number) => formatCompactNumber(v)}
            />

            <Tooltip
              content={<ChartTooltip granularity={granularity} />}
              cursor={{ fill: 'rgba(59, 130, 246, 0.06)' }}
            />

            <Bar
              dataKey="wordsLearned"
              name={BAR_LABEL}
              fill={BAR_COLOR}
              radius={[3, 3, 0, 0]}
              maxBarSize={28}
            />

            <Line
              type="monotone"
              dataKey="cumulativeWords"
              name={LINE_LABEL}
              stroke={LINE_COLOR}
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4, fill: LINE_COLOR }}
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {/* ---------- Chú giải ---------- */}
      <div className="flex flex-wrap items-center gap-4 text-[11px] font-semibold text-gray-600">
        <span className="inline-flex items-center gap-1.5">
          <span
            className="inline-block w-3 h-3 rounded-sm"
            style={{ backgroundColor: BAR_COLOR }}
          />
          {BAR_LABEL}
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span
            className="inline-block w-3 h-1 rounded-full"
            style={{ backgroundColor: LINE_COLOR }}
          />
          {LINE_LABEL}
        </span>
      </div>
    </section>
  )
}

export default ProgressChartBlock
