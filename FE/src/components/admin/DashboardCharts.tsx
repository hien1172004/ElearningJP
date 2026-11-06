import React from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { cn } from '@/utils/cn'

// Một series / biểu đồ → một màu (slot 1 của bảng màu dataviz, đã kiểm tra ≥ 3:1 trên nền trắng).
const SERIES_COLOR = '#2a78d6'
const GRID_COLOR = '#eef0f3'
const AXIS_TEXT = '#6b7280'

// ============================================================================
// Thẻ KPI
// ============================================================================

interface StatTileProps {
  label: string
  value: React.ReactNode
  /** Giá trị kỳ hiện tại & kỳ trước (số thô) để tính % thay đổi. Bỏ qua nếu không so sánh. */
  current?: number
  previous?: number
  hint?: React.ReactNode
}

/** % thay đổi so với kỳ trước; kỳ trước = 0 thì không tính được tỉ lệ. */
function DeltaBadge({ current, previous }: { current: number; previous: number }) {
  if (previous === 0) {
    return current > 0 ? (
      <span className="text-xs text-slate-500">Kỳ trước: 0</span>
    ) : null
  }
  const pct = Math.round(((current - previous) / previous) * 100)
  const up = pct > 0
  const flat = pct === 0
  return (
    <span
      className={cn(
        'text-xs font-medium',
        flat ? 'text-slate-500' : up ? 'text-emerald-700' : 'text-rose-700',
      )}
    >
      {flat ? '→' : up ? '▲' : '▼'} {Math.abs(pct)}% so với kỳ trước
    </span>
  )
}

export const StatTile: React.FC<StatTileProps> = ({ label, value, current, previous, hint }) => (
  <div className="bg-surface rounded-xl border border-slate-200 p-4 flex flex-col gap-1 min-w-0">
    <span className="text-sm text-slate-500 truncate">{label}</span>
    <span className="text-2xl font-semibold text-slate-900 tabular-nums">{value}</span>
    {current !== undefined && previous !== undefined && (
      <DeltaBadge current={current} previous={previous} />
    )}
    {hint && <span className="text-xs text-slate-500">{hint}</span>}
  </div>
)

// ============================================================================
// Khung biểu đồ
// ============================================================================

export const ChartCard: React.FC<{
  title: string
  subtitle?: React.ReactNode
  children: React.ReactNode
  className?: string
}> = ({ title, subtitle, children, className }) => (
  <section className={cn('bg-surface rounded-xl border border-slate-200 p-4', className)}>
    <h3 className="text-sm font-semibold text-slate-800">{title}</h3>
    {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
    <div className="mt-3">{children}</div>
  </section>
)

// ============================================================================
// Tooltip
// ============================================================================

interface TooltipPayloadItem {
  value?: number | string | null
  payload?: Record<string, unknown>
}

interface ChartTooltipProps {
  active?: boolean
  payload?: TooltipPayloadItem[]
  label?: string | number
  formatLabel?: (label: string) => string
  formatValue: (value: number) => string
  valueLabel: string
}

const ChartTooltip: React.FC<ChartTooltipProps> = ({
  active,
  payload,
  label,
  formatLabel,
  formatValue,
  valueLabel,
}) => {
  if (!active || !payload?.length) return null
  const raw = payload[0]?.value
  const text = typeof raw === 'number' ? formatValue(raw) : '—'
  const labelText = label === undefined ? '' : formatLabel ? formatLabel(String(label)) : String(label)
  return (
    <div className="bg-surface border border-slate-200 rounded-lg shadow-sm px-3 py-2 text-xs">
      <div className="font-medium text-slate-800">{labelText}</div>
      <div className="mt-1 flex items-center gap-2 text-slate-600">
        <span className="inline-block w-2.5 h-2.5 rounded-sm" style={{ background: SERIES_COLOR }} />
        {valueLabel}: <span className="font-semibold text-slate-900 tabular-nums">{text}</span>
      </div>
    </div>
  )
}

const defaultFormat = (v: number) => v.toLocaleString('vi-VN')

// ============================================================================
// Biểu đồ đường theo ngày (1 series)
// ============================================================================

interface DailyLineChartProps {
  data: object[]
  xKey: string
  yKey: string
  valueLabel: string
  formatX: (value: string) => string
  formatTooltipLabel?: (value: string) => string
  formatValue?: (value: number) => string
  height?: number
}

export function DailyLineChart({
  data,
  xKey,
  yKey,
  valueLabel,
  formatX,
  formatTooltipLabel,
  formatValue = defaultFormat,
  height = 220,
}: DailyLineChartProps) {
  return (
    <div style={{ height }} role="img" aria-label={`Biểu đồ ${valueLabel} theo ngày`}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 12, bottom: 0, left: -12 }}>
          <CartesianGrid stroke={GRID_COLOR} vertical={false} />
          <XAxis
            dataKey={xKey}
            tickFormatter={(v) => formatX(String(v))}
            tick={{ fontSize: 11, fill: AXIS_TEXT }}
            axisLine={{ stroke: GRID_COLOR }}
            tickLine={false}
            minTickGap={16}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fontSize: 11, fill: AXIS_TEXT }}
            axisLine={false}
            tickLine={false}
            width={44}
          />
          <Tooltip
            cursor={{ stroke: '#94a3b8', strokeWidth: 1, strokeDasharray: '3 3' }}
            content={
              <ChartTooltip
                formatLabel={formatTooltipLabel ?? formatX}
                formatValue={formatValue}
                valueLabel={valueLabel}
              />
            }
          />
          <Line
            type="monotone"
            dataKey={yKey}
            stroke={SERIES_COLOR}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4, stroke: '#ffffff', strokeWidth: 2 }}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

// ============================================================================
// Biểu đồ cột (1 series, danh mục có thứ tự)
// ============================================================================

interface SimpleBarChartProps {
  data: object[]
  xKey: string
  yKey: string
  valueLabel: string
  formatX?: (value: string) => string
  formatValue?: (value: number) => string
  /** Domain trục Y cố định, vd [0, 1] cho tỉ lệ. */
  yDomain?: [number, number]
  formatY?: (value: number) => string
  height?: number
}

export function SimpleBarChart({
  data,
  xKey,
  yKey,
  valueLabel,
  formatX,
  formatValue = defaultFormat,
  yDomain,
  formatY,
  height = 220,
}: SimpleBarChartProps) {
  return (
    <div style={{ height }} role="img" aria-label={`Biểu đồ cột ${valueLabel}`}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 12, bottom: 0, left: -12 }}>
          <CartesianGrid stroke={GRID_COLOR} vertical={false} />
          <XAxis
            dataKey={xKey}
            tickFormatter={formatX ? (v) => formatX(String(v)) : undefined}
            tick={{ fontSize: 11, fill: AXIS_TEXT }}
            axisLine={{ stroke: GRID_COLOR }}
            tickLine={false}
            minTickGap={4}
          />
          <YAxis
            allowDecimals={!!yDomain}
            domain={yDomain}
            tickFormatter={formatY}
            tick={{ fontSize: 11, fill: AXIS_TEXT }}
            axisLine={false}
            tickLine={false}
            width={44}
          />
          <Tooltip
            cursor={{ fill: '#f1f5f9' }}
            content={
              <ChartTooltip formatLabel={formatX} formatValue={formatValue} valueLabel={valueLabel} />
            }
          />
          <Bar
            dataKey={yKey}
            fill={SERIES_COLOR}
            radius={[4, 4, 0, 0]}
            maxBarSize={36}
            isAnimationActive={false}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
