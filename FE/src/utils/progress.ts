/**
 * Format helpers cho trang Progress.
 *
 * Tách riêng khỏi component để tái dùng cho các khối 2-5 và test độc lập.
 */

import type { ChartGranularity } from '@/types'

/**
 * Các preset kỳ dùng chung cho bộ lọc của Khối 1 (và Khối 3 — biểu đồ).
 *
 * <p>Đặt ở đây (utils) thay vì trong component để cả Khối 1 lẫn biểu đồ đều
 * import từ 1 nguồn, không tạo vòng phụ thuộc utils ↔ components.</p>
 */
export type SummaryRangePreset = '7d' | '30d' | '90d' | 'month' | 'year'

/**
 * Suy ra đơn vị bucket của biểu đồ từ preset kỳ.
 *
 * <ul>
 *   <li>{@code 7d} / {@code 30d} / {@code month} → theo ngày (≤ 31 cột)</li>
 *   <li>{@code 90d} → theo tuần ISO (≤ 13 cột)</li>
 *   <li>{@code year} → theo tháng (≤ 12 cột)</li>
 * </ul>
 *
 * @param preset preset kỳ đang chọn
 */
export function chartGranularityForPreset(
  preset: SummaryRangePreset,
): ChartGranularity {
  switch (preset) {
    case '90d':
      return 'week'
    case 'year':
      return 'month'
    case '7d':
    case '30d':
    case 'month':
    default:
      return 'day'
  }
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
