import type { ApiResponse } from './api.types'

// ============================================================================
// Lookup / heartbeat — dùng cho instrumentation (Bước 2 + 3)
// ============================================================================

/** Loại đối tượng người dùng tra cứu. Khớp enum BE `LookupTargetType`. */
export type LookupTargetType = 'VOCAB' | 'KANJI' | 'GRAMMAR'

/** Payload gửi `POST /dictionary/lookup`. */
export interface DictionaryLookupPayload {
  targetType: LookupTargetType
  targetId?: number | null
  sourceRef?: string | null
}

/** Response `POST /dictionary/lookup`. */
export interface DictionaryLookupResult {
  wordsLearnedToday: number
  goalWords: number
  goalMet: boolean
  xpAwarded: number
  statDate: string
}

// ============================================================================
// Heartbeat — Bước 3
// ============================================================================

/** Payload gửi `POST /study/heartbeat`. */
export interface StudyHeartbeatPayload {
  /** Số giây học kể từ heartbeat trước. BE chỉ nhận 1-60. */
  seconds: number
  /** Session uuid của tab leader. */
  sessionId: string
}

/** Response `POST /study/heartbeat`. */
export interface StudyHeartbeatResult {
  /** false nếu tab khác đang giữ quyền đếm thời gian. */
  accepted: boolean
  studySecondsToday: number
  statDate: string
  reason?: string | null
}

// ============================================================================
// Bước 4 — Summary (Khối 1)
// ============================================================================

/** Tổng hợp tiến độ học tập theo kỳ. */
export interface ProgressSummary {
  /** Ngày bắt đầu kỳ (inclusive), format yyyy-MM-dd. */
  from: string
  /** Ngày kết thúc kỳ (inclusive), format yyyy-MM-dd. */
  to: string
  /** Số từ đã học = số lần tra từ điển trong kỳ. */
  wordsLearned: number
  /** Tổng thời gian học (giây). */
  studySeconds: number
  /** Tổng XP kiếm được trong kỳ. */
  xpEarned: number
  /** Số ngày có hoạt động (>= 1 lần tra từ). */
  activeDays: number
  /** Số ngày đạt mục tiêu. */
  goalMetDays: number
  /** Chuỗi ngày học liên tiếp hiện tại. */
  currentStreak: number
  /** Chuỗi ngày học liên tiếp dài nhất. */
  longestStreak: number
  /** % thay đổi wordsLearned so với kỳ trước. null nếu kỳ trước = 0. */
  wordsLearnedDeltaPct: number | null
  /** % thay đổi studySeconds so với kỳ trước. */
  studySecondsDeltaPct: number | null
  /** Mục tiêu từ/ngày (mặc định 5). */
  goalWords: number
  /** Hôm nay theo múi giờ nghiệp vụ (Asia/Ho_Chi_Minh), `yyyy-MM-dd`. */
  today: string
  /** Số từ đã tra HÔM NAY (độc lập với kỳ đã chọn). */
  wordsLearnedToday: number
  /** Hôm nay đã đủ số từ mục tiêu chưa (`wordsLearnedToday >= goalWords`). */
  goalMetToday: boolean
}

export type ProgressSummaryResponse = ApiResponse<ProgressSummary>

// ============================================================================
// Khối 2 — Lịch học + streak
// ============================================================================

/** 1 ngày có hoạt động trong lịch. */
export interface CalendarDay {
  /** Ngày `yyyy-MM-dd`. */
  date: string
  /** Số lần tra từ điển trong ngày. */
  lookups: number
  /** Số bài học hoàn thành trong ngày. */
  lessonsCompleted: number
  /** Thời gian học trong ngày (giây). */
  studySeconds: number
  /** XP kiếm được trong ngày. */
  xpEarned: number
  /** Ngày có học gì không. */
  hasActivity: boolean
  /** Ngày có đủ số từ mục tiêu không. */
  goalMet: boolean
}

/**
 * Lịch học + streak theo tháng.
 *
 * <p>`days` chỉ chứa ngày CÓ hoạt động → ngày trống phải tự điền khi dựng lưới.</p>
 */
export interface ProgressCalendar {
  /** Tháng đang xem `yyyy-MM`. */
  month: string
  monthStart: string
  monthEnd: string
  /** Hôm nay theo múi giờ nghiệp vụ. */
  today: string
  goalWords: number
  currentStreak: number
  longestStreak: number
  activeDays: number
  goalMetDays: number
  totalLookups: number
  days: CalendarDay[]
}

// ============================================================================
// Khối 4 — Bảng từ đã tra nhiều nhất
// ============================================================================

/** 1 dòng trong bảng "Từ đã tra nhiều nhất". */
export interface TopLookup {
  /** `VOCAB` | `GRAMMAR` | `KANJI`. */
  targetType: LookupTargetType
  /** ID đối tượng (null với KANJI). */
  targetId: number | null
  /** Ký tự Hán (chỉ có với KANJI). */
  kanjiCharacter: string | null
  /** Từ viết / tên ngữ pháp / ký tự Hán. */
  label: string
  /** Cách đọc kana (VOCAB) hoặc cấu trúc (GRAMMAR). */
  subLabel: string | null
  meaningVi: string | null
  /** Hán Việt (VOCAB) hoặc JLPT dạng chữ (KANJI / GRAMMAR). */
  extraLabel: string | null
  /** Số lần đã tra. */
  lookupCount: number
  imageUrl: string | null
  jlptLevel: string | null
}

// ============================================================================
// Khối 5 — Bảng xếp hạng tuần (Leaderboard)
// ============================================================================

/**
 * Một dòng trong bảng xếp hạng tuần.
 *
 * <p>Quy ước field (phải khớp BE `LeaderboardEntryResponse`):</p>
 * <ul>
 *   <li>{@code score} = điểm xếp hạng =
 *       `lookups_scored * lookup_points + lessons_completed * lesson_points`</li>
 *   <li>{@code totalLookups} = tổng lần tra trong tuần (KHÔNG trần)</li>
 *   <li>{@code totalXp} = tổng XP trong tuần</li>
 * </ul>
 */
export interface LeaderboardEntry {
  /** Hạng trong tuần (bắt đầu từ 1). Cùng điểm → cùng hạng (1, 1, 3). */
  rank: number
  userId: number
  displayName: string
  avatarUrl: string | null
  /** Điểm xếp hạng trong tuần. */
  score: number
  /** Tổng số lần tra từ điển trong tuần (không trần). */
  totalLookups: number
  /** Tổng XP kiếm được trong tuần. */
  totalXp: number
  /** Tuần xếp hạng (ISO: `YYYY-'W'ww`), vd `2026-W42`. */
  weekKey: string
}

/**
 * Wrapper bảng xếp hạng tuần.
 *
 * <p>Có wrapper vì bảng rỗng vẫn phải trả `weekKey` để FE điều hướng tuần
 * (biết đang ở tuần nào, tuần trước/sau là tuần nào).</p>
 */
export interface LeaderboardResponse {
  weekKey: string
  /** Ngày thứ 2 đầu tuần (`yyyy-MM-dd`). */
  weekStart: string
  /** Ngày chủ nhật cuối tuần (`yyyy-MM-dd`). */
  weekEnd: string
  /** Tuần này có phải tuần hiện tại không. */
  isCurrentWeek: boolean
  /** Tổng số người có điểm trong tuần. */
  totalPlayers: number
  /** Danh sách xếp hạng (tối đa 5 dòng). */
  entries: LeaderboardEntry[]
}

/** Hạng cá nhân của user hiện tại trong tuần. */
export interface MyRank {
  /** Hạng của user (bắt đầu từ 1). Cùng điểm → cùng hạng. */
  rank: number | null
  /** Điểm xếp hạng của user trong tuần. */
  score: number | null
  /** Tổng số người có điểm trong tuần. */
  totalPlayers: number | null
  weekKey: string
  userId: number
  displayName: string
  avatarUrl: string | null
  /** User có nằm trong bảng top 5 không. */
  inTop: boolean
}

// ============================================================================
// Re-export convenience
// ============================================================================

export type LookupResponse = ApiResponse<DictionaryLookupResult>
export type HeartbeatResponse = ApiResponse<StudyHeartbeatResult>