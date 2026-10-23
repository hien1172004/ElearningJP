/**
 * Tính tuần ISO ở phía FE — phải khớp chính xác với BE (`LeaderboardWeekUtil`).
 *
 * <p><b>Vì sao không dùng `Date` UTC?</b> `Date` mặc định dùng múi giờ của máy
 * người dùng (GMT+7 ở VN, nhưng có thể là khác ở nơi khác), và các hàm như
 * `getDay()` trả về theo múi giờ local → dễ lệch 1 ngày so với BE (vốn neo
 * cứng `Asia/Ho_Chi_Minh`).</p>
 *
 * <p>Vì vậy ở đây ta làm đúng 2 việc:</p>
 * <ol>
 *   <li>Lấy "hôm nay" theo múi giờ nghiệp vụ <b>Asia/Ho_Chi_Minh</b> — dùng
 *       `toLocaleDateString('en-CA', ...)` cho ra đúng định dạng `yyyy-MM-dd`.</li>
 *   <li>Sau đó dùng <b>phép tính ngày lịch thuần</b> (thuần ISO: thứ 2 → chủ nhật,
 *       tuần chứa ngày 4/1 là tuần 1) trên chuỗi ngày đó → không còn phụ thuộc
 *       múi giờ nữa.</li>
 * </ol>
 *
 * <p>Chuẩn ISO 8601: tuần bắt đầu thứ 2. Tuần đầu tiên của năm là tuần chứa
 * ngày 4/1 (ngày đầu tiên có ít nhất 4 ngày trong năm đó).</p>
 */

/** Múi giờ nghiệp vụ — phải khớp `TimeConfig.APP_ZONE` ở BE. */
export const APP_TIMEZONE = 'Asia/Ho_Chi_Minh'

/** Số mili giây trong 1 ngày. */
const MS_PER_DAY = 86_400_000

/** Thứ trong tuần theo ISO: thứ 2 = 1 … chủ nhật = 7. */
function isoDayOfWeek(date: Date): number {
  // `getUTCDay()`: 0 = CN … 6 = T7 → chuyển sang 1 = T2 … 7 = CN
  const day = date.getUTCDay()
  return day === 0 ? 7 : day
}

/**
 * "Hôm nay" theo múi giờ {@link APP_TIMEZONE}, dạng `yyyy-MM-dd`.
 *
 * <p>Dùng `en-CA` vì locale này xuất ngày theo thứ tự `yyyy-MM-dd` — không cần
 * tự ghép chuỗi.</p>
 */
export function todayIso(): string {
  return new Date().toLocaleDateString('en-CA', {
    timeZone: APP_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

/**
 * Ngày thứ 2 đầu tuần ISO chứa {@code isoDate}.
 *
 * @param isoDate ngày bất kỳ dạng `yyyy-MM-dd`
 * @return ngày thứ 2 dạng `yyyy-MM-dd`
 */
export function isoWeekStart(isoDate: string): string {
  const date = parseIsoDate(isoDate)
  const dow = isoDayOfWeek(date)
  // Thứ 2 của tuần đó = ngày hiện tại lùi (dow - 1) ngày
  const monday = new Date(date.getTime() - (dow - 1) * MS_PER_DAY)
  return formatIsoDate(monday)
}

/**
 * Tuần ISO chứa {@code isoDate}.
 *
 * @param isoDate ngày bất kỳ dạng `yyyy-MM-dd`
 * @return weekKey dạng `YYYY-'W'ww` (vd `2026-W42`)
 */
export function isoWeekKey(isoDate: string): string {
  const monday = isoWeekStart(isoDate)
  const mondayDate = parseIsoDate(monday)

  // Ngày thứ 4 của tuần đó luôn nằm trong cùng ISO week → dùng làm mốc.
  // (Đây là bản tương đương của cách BE neo vào `LocalDate.of(year, 1, 4)`.)
  const thursday = new Date(mondayDate.getTime() + 3 * MS_PER_DAY)
  const year = thursday.getUTCFullYear()

  // Ngày 1 tháng 1 của `year` nằm ở tuần nào → tính offset tuần.
  const jan4 = new Date(Date.UTC(year, 0, 4))
  const jan4Dow = isoDayOfWeek(jan4)
  const jan4Monday = new Date(jan4.getTime() - (jan4Dow - 1) * MS_PER_DAY)

  const week = Math.round(
    (mondayDate.getTime() - jan4Monday.getTime()) / (7 * MS_PER_DAY),
  ) + 1

  return `${year}-W${String(week).padStart(2, '0')}`
}

/**
 * Dịch weekKey `delta` tuần.
 *
 * <p>Dịch qua ngày thứ 2 của tuần đó rồi tính lại → xử lý đúng trường hợp
 * giao năm (vd `2020-W53` → `2021-W01`).</p>
 *
 * @param weekKey tuần gốc, vd `2026-W42`
 * @param delta   số tuần cần dịch (âm = tuần trước, dương = tuần sau)
 * @return weekKey mới
 */
export function shiftWeekKey(weekKey: string, delta: number): string {
  const monday = parseIsoDate(isoWeekStart(weekKeyToMonday(weekKey)))
  const shifted = new Date(monday.getTime() + delta * 7 * MS_PER_DAY)
  return isoWeekKey(formatIsoDate(shifted))
}

/**
 * Ngày thứ 2 đầu tuần từ weekKey.
 *
 * @param weekKey tuần dạng `YYYY-'W'ww`
 * @return ngày thứ 2 dạng `yyyy-MM-dd`
 */
export function weekKeyToMonday(weekKey: string): string {
  const match = /^(\d{4})-W(\d{2})$/.exec(weekKey.trim())
  if (!match) {
    throw new Error(`Invalid week key: ${weekKey}`)
  }
  const year = Number(match[1])
  const week = Number(match[2])

  // Neo vào 4/1 — ngày này luôn nằm trong ISO week 1 của week-based-year đó.
  const jan4 = new Date(Date.UTC(year, 0, 4))
  const jan4Dow = isoDayOfWeek(jan4)
  const jan4Monday = new Date(jan4.getTime() - (jan4Dow - 1) * MS_PER_DAY)

  const monday = new Date(jan4Monday.getTime() + (week - 1) * 7 * MS_PER_DAY)
  return formatIsoDate(monday)
}

/**
 * Tuần ISO hiện tại (theo múi giờ nghiệp vụ).
 */
export function currentWeekKey(): string {
  return isoWeekKey(todayIso())
}

/**
 * Tuần có phải tuần hiện tại không.
 */
export function isCurrentWeek(weekKey: string): boolean {
  return weekKey === currentWeekKey()
}

/**
 * So sánh 2 weekKey — trả về số âm nếu {@code a} sớm hơn {@code b}.
 */
export function compareWeekKeys(a: string, b: string): number {
  return weekKeyToMonday(a).localeCompare(weekKeyToMonday(b))
}

// ============================================================================
// Tháng lịch — dùng cho lịch học (Khoi 2)
// ============================================================================

/**
 * Tháng hiện tại theo múi giờ {@link APP_TIMEZONE}, dạng `yyyy-MM`.
 *
 * <p>Không dùng `new Date().getMonth()` vì lấy theo múi giờ máy người dùng →
 * lệch với BE (vốn neo cứng `Asia/Ho_Chi_Minh`) quanh đêm.</p>
 */
export function currentMonthInTz(): string {
  return todayIso().slice(0, 7)
}

/**
 * Dịch chuỗi tháng `delta` tháng.
 *
 * <p>Dịch qua ngày 1 của tháng đó (luôn an toàn vì mọi tháng đều có ngày 1) rồi
 * ghi lại thành `yyyy-MM`.</p>
 *
 * @param month tháng gốc, vd `2026-10`
 * @param delta số tháng cần dịch (âm = tháng trước, dương = tháng sau)
 * @return tháng mới, vd `2026-11`
 *
 * @example
 * shiftMonth('2026-01', -1) // '2025-12'
 */
export function shiftMonth(month: string, delta: number): string {
  const [y, m] = month.split('-').map(Number)
  // Date.UTC tự chuẩn hoá tháng ngoài 0..11 (tháng 0 = tháng 12 năm trước).
  const shifted = new Date(Date.UTC(y, (m ?? 1) - 1 + delta, 1))
  const year = shifted.getUTCFullYear()
  const mon = String(shifted.getUTCMonth() + 1).padStart(2, '0')
  return `${year}-${mon}`
}

/**
 * Label hiển thị cho tuần, vd `Tuần 42 · 12/10 – 18/10`.
 *
 * @param weekKey tuần dạng `YYYY-'W'ww`
 */
export function formatWeekLabel(weekKey: string): string {
  const monday = weekKeyToMonday(weekKey)
  const sundayDate = parseIsoDate(monday)
  const sunday = new Date(sundayDate.getTime() + 6 * MS_PER_DAY)
  const week = /W(\d{2})$/.exec(weekKey)?.[1] ?? ''
  return `Tuần ${week} · ${formatShortDate(monday)} – ${formatShortDate(formatIsoDate(sunday))}`
}

// --------------------------------------------------------------------------
// Helpers nội bộ
// --------------------------------------------------------------------------

/** Parse `yyyy-MM-dd` thành `Date` (UTC midnight) — không dùng `new Date(str)`. */
function parseIsoDate(isoDate: string): Date {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(Date.UTC(y, (m ?? 1) - 1, d ?? 1))
}

/** Format `Date` (UTC) → `yyyy-MM-dd`. */
function formatIsoDate(date: Date): string {
  const y = date.getUTCFullYear()
  const m = String(date.getUTCMonth() + 1).padStart(2, '0')
  const d = String(date.getUTCDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** Format `yyyy-MM-dd` → `dd/MM`. */
function formatShortDate(isoDate: string): string {
  const [, m, d] = isoDate.split('-')
  return `${d}/${m}`
}
