/**
 * Dựng lưới lịch tháng + phân loại màu ô ngày.
 *
 * <p>Tách riêng khỏi component để <b>test thuần</b> không cần render React,
 * và để logic ngày trong tháng không lẫn vào JSX.</p>
 */

/** Thứ trong tuần ISO: thứ 2 = 1 … chủ nhật = 7. */
const DOW_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'] as const

/** Số mili giây trong 1 ngày. */
const MS_PER_DAY = 86_400_000

/**
 * Phân loại 1 ô lịch — quyết định màu hiển thị.
 *
 * - {@code goal}   → học và **đạt mục tiêu** → xanh dương (theo yêu cầu thiết kế).
 * - {@code active} → có học nhưng **chưa đạt** → xanh lá.
 * - {@code empty}  → không học → không tô màu.
 * - {@code future} → ngày chưa tới → làm mờ, không tô màu.
 */
export type DayState = 'goal' | 'active' | 'empty' | 'future'

/** 1 ô trong lưới lịch. */
export interface CalendarCell {
  /** `yyyy-MM-dd`. */
  date: string
  /** Ngày trong tháng (1..31) — null cho ô trống đệm. */
  dayNumber: number | null
  state: DayState
  /** Số lần tra từ (null với ô trống / ngày không học). */
  lookups: number | null
  /** Có phải hôm nay không. */
  isToday: boolean
  /**
   * Ngày chưa tới.
   *
   * <p>Cần riêng khỏi {@link state} vì ngày tương lai cũng là `'empty'` —
   * không có flag này thì không phân biệt được "chưa học" với "chưa tới".</p>
   */
  isFuture: boolean
}

export interface BuildCalendarGridParams {
  /** Ngày đầu tháng `yyyy-MM-dd`. */
  monthStart: string
  /** Ngày cuối tháng `yyyy-MM-dd`. */
  monthEnd: string
  /** Hôm nay `yyyy-MM-dd`. */
  today: string
  /** Ngày có hoạt động, dạng map ngày → số lần tra. */
  lookupsByDate: Record<string, number>
  /** Ngày đạt mục tiêu. */
  goalDates: ReadonlySet<string>
}

/**
 * Dựng lưới 7 cột (T2..CN) cho 1 tháng, có ô trống đệm ở đầu/cuối.
 *
 * <p><b>Vì sao luôn bắt đầu từ thứ 2?</b> Chuẩn ISO 8601 (giống tuần của bảng
 * xếp hạng) — tuần bắt đầu bằng thứ 2. Nếu neo theo thứ 7/CN của lịch Việt Nam
 * thì "ngày học liên tụp" của streak sẽ lệch 1 ngày so với BE.</p>
 */
export function buildCalendarGrid({
  monthStart,
  monthEnd,
  today,
  lookupsByDate,
  goalDates,
}: BuildCalendarGridParams): CalendarCell[] {
  const start = parseIso(monthStart)
  const end = parseIso(monthEnd)
  const todayDate = parseIso(today)

  // getUTCDay(): 0 = CN … 6 = T7. Ô đầu tiên của lưới là thứ 2.
  const leadingBlanks = (start.getUTCDay() + 6) % 7

  const cells: CalendarCell[] = []

  for (let i = 0; i < leadingBlanks; i++) {
    cells.push({
      date: '',
      dayNumber: null,
      state: 'empty',
      lookups: null,
      isToday: false,
      isFuture: false,
    })
  }

  const totalDays = Math.round(
    (end.getTime() - start.getTime()) / MS_PER_DAY,
  ) + 1

  for (let i = 0; i < totalDays; i++) {
    const current = new Date(start.getTime() + i * MS_PER_DAY)
    const date = formatIso(current)
    const lookups = lookupsByDate[date] ?? 0
    const isToday = date === today
    const isFuture = current.getTime() > todayDate.getTime()

    let state: DayState
    if (isFuture) {
      // Ngày chưa tới: chưa thể "đạt" hay "chưa đạt" → coi như trống.
      state = 'empty'
    } else if (goalDates.has(date)) {
      state = 'goal'
    } else if (lookups > 0) {
      state = 'active'
    } else {
      state = 'empty'
    }

    cells.push({
      date,
      dayNumber: current.getUTCDate(),
      state,
      lookups,
      isToday,
      isFuture,
    })
  }

  return cells
}

/** Nhãn thứ trong tuần (T2..CN) theo thứ tự cột của lưới. */
export function weekdayLabels(): readonly string[] {
  return DOW_LABELS
}

/** Tháng tiếng Việt không kèm năm — dùng cho nhãn phụ trong card streak. */
export function formatMonthShort(month: string): string {
  return formatMonthLabel(month).split(' / ')[0]
}

/**
 * Tên tháng tiếng Việt — dùng cho tiêu đề "Tháng 10 / 2026".
 *
 * @param month chuỗi `yyyy-MM`
 */
export function formatMonthLabel(month: string): string {
  const [y, m] = month.split('-')
  const names = [
    'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
    'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12',
  ]
  const idx = Number(m) - 1
  const label = names[idx] ?? month
  return y ? `${label} / ${y}` : label
}

// --------------------------------------------------------------------------
// Helpers nội bộ
// --------------------------------------------------------------------------

/** Parse `yyyy-MM-dd` → `Date` (UTC midnight). Không dùng `new Date(str)`. */
function parseIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, (m ?? 1) - 1, d ?? 1))
}

/** Format `Date` (UTC) → `yyyy-MM-dd`. */
function formatIso(date: Date): string {
  const y = date.getUTCFullYear()
  const m = String(date.getUTCMonth() + 1).padStart(2, '0')
  const d = String(date.getUTCDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}