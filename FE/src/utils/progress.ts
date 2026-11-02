/**
 * Format helpers cho trang Progress.
 *
 * Tách riêng khỏi component để tái dùng cho các khối 2-5 và test độc lập.
 */

import type { ChartGranularity } from '@/types'
import { isoWeekStart, todayIso } from './week'

/**
 * Các preset kỳ dùng chung cho bộ lọc của Khối 1 (và Khối 3 — biểu đồ).
 *
 * <p>Đặt ở đây (utils) thay vì trong component để cả Khối 1 lẫn biểu đồ đều
 * import từ 1 nguồn, không tạo vòng phụ thuộc utils ↔ components.</p>
 */
export type SummaryRangePreset = 'day' | 'week' | 'month' | 'all' | 'custom'

/** Kỳ đang lọc. `all` không có from/to: BE tự tính từ ngày đầu có dữ liệu. */
export interface ProgressRange {
  preset: SummaryRangePreset
  from?: string
  to?: string
}

/** Số ngày tối đa của kỳ tự chọn. Khớp `ProgressService.MAX_RANGE_DAYS` ở BE. */
export const MAX_CUSTOM_RANGE_DAYS = 366

const MS_PER_DAY = 86_400_000

function parseIso(iso: string): number {
  const [y, m, d] = iso.split('-').map(Number)
  return Date.UTC(y, m - 1, d)
}

function toIso(utcMs: number): string {
  return new Date(utcMs).toISOString().slice(0, 10)
}

/** Kỳ của các lựa chọn có sẵn, theo ngày của múi giờ nghiệp vụ: hôm nay, tuần này (T2–CN), tháng này. */
export function resolvePresetRange(preset: Exclude<SummaryRangePreset, 'custom'>): ProgressRange {
  const today = todayIso()
  switch (preset) {
    case 'day':
      return { preset, from: today, to: today }
    case 'week': {
      const monday = isoWeekStart(today)
      return { preset, from: monday, to: toIso(parseIso(monday) + 6 * MS_PER_DAY) }
    }
    case 'month': {
      const [y, m] = today.split('-').map(Number)
      return { preset, from: toIso(Date.UTC(y, m - 1, 1)), to: toIso(Date.UTC(y, m, 0)) }
    }
    case 'all':
      return { preset }
  }
}

/**
 * Đơn vị bucket của biểu đồ theo kỳ: toàn thời gian theo tháng; kỳ tự chọn
 * theo độ dài (≤ 31 ngày theo ngày, ≤ 180 ngày theo tuần, còn lại theo tháng).
 */
export function chartGranularityForRange({ preset, from, to }: ProgressRange): ChartGranularity {
  if (preset === 'all') return 'month'
  if (preset !== 'custom' || !from || !to) return 'day'
  const days = (parseIso(to) - parseIso(from)) / MS_PER_DAY + 1
  if (days <= 31) return 'day'
  return days <= 180 ? 'week' : 'month'
}

/**
 * Format giây thành chuỗi thời gian dễ đọc.
 *
 * - `< 60 giây`      → "45 giây"
 * - `< 60 phút`      → "12 phút"  (làm tròn xuống)
 * - `< 24 giờ`       → "2 giờ 15 phút"
 * - `>= 24 giờ`      → "5 giờ 30 phút" (bỏ phần ngày, giữ dạng giờ)
 *
 * @param seconds số giây (>= 0)
 * @param compact khi `true`, bỏ phần phút/giây với giá trị lớn
 */
export function formatDuration(seconds: number | null | undefined): string {
  const total = Math.max(0, Math.floor(seconds ?? 0))
  if (total === 0) return '0 giây'

  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const secs = total % 60

  if (hours > 0) {
    return minutes > 0 ? `${hours} giờ ${minutes} phút` : `${hours} giờ`
  }
  if (minutes > 0) {
    return `${minutes} phút`
  }
  return `${secs} giây`
}

/**
 * Format số lớn gọn gàng: 1234 → "1.2K", 1234567 → "1.2M".
 *
 * @param value số cần format
 * @param decimals số chữ số thập phân (mặc định 1)
 */
export function formatCompactNumber(value: number | null | undefined, decimals = 1): string {
  const n = value ?? 0
  if (Math.abs(n) < 1000) return String(n)

  if (Math.abs(n) < 1_000_000) {
    return `${(n / 1000).toFixed(decimals).replace(/\.0$/, '')}K`
  }
  return `${(n / 1_000_000).toFixed(decimals).replace(/\.0$/, '')}M`
}

/**
 * Format ngày ISO `yyyy-MM-dd` → `dd/MM/yyyy` (kiểu Việt Nam).
 *
 * <p>Hàm này **khác** `formatDate` trong `format.ts`: bản đó nhận `Date`
 * và dùng `Intl.DateTimeFormat`; bản này nhận string ISO và không tạo
 * `Date` object (tránh lỗi timezone lệch 1 ngày).</p>
 *
 * <p>Đặt tên `formatIsoDate` để không trùng với `formatDate` đã export
 * từ `format.ts`.</p>
 */
export function formatIsoDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '—'
  const parts = isoDate.split('-')
  if (parts.length !== 3) return isoDate
  const [y, m, d] = parts
  return `${d}/${m}/${y}`
}
