/**
 * Types cho module SRS (Spaced Repetition System) — Flashcard + SM-2.
 *
 * Đối chiếu với BE (SrsController):
 *  - SrsDueItemResponse      → SrsDueItem
 *  - SrsStatsResponse        → SrsStats
 *  - SrsReviewRequest        → SrsReviewRequest
 *  - SrsReviewResultResponse → SrsReviewResult
 */

/** Đánh giá chất lượng SM-2 (điểm q = 1/3/4/5). */
export type SrsRating = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'

/** Trạng thái thẻ. */
export type SrsStatus = 'LEARNING' | 'REVIEWING' | 'MASTERED'

/** Loại nội dung thẻ (polymorphic target). */
export type SrsItemType = 'VOCABULARY' | 'CHARACTER' | 'GRAMMAR'

/** Kiểu thẻ (hiện tại backend luôn tạo STANDARD). */
export type SrsCardType = 'STANDARD' | 'RECOGNITION' | 'RECALL' | 'WRITING'

/** Một thẻ trong hàng đợi ôn tập (GET /srs/due). */
export interface SrsDueItem {
  srsItemId: number
  cardType: SrsCardType
  itemType: SrsItemType
  status: SrsStatus
  intervalDays: number
  repetitionCount: number
  /** Hệ số dễ dãi SM-2 (mặc định 2.50). */
  easeFactor: number
  nextReviewAt: string
  /** vocabId / characterId / grammarId gốc. */
  referenceId: number
  /** Mặt trước: từ / chữ Hán / điểm ngữ pháp. */
  frontText: string
  /** Mặt sau: nghĩa tiếng Việt. */
  backMeaning: string
  /** Furigana / Romaji / On-Kun / cấu trúc ngữ pháp. */
  reading: string
  audioUrl?: string | null
  exampleSentence?: string | null
  exampleTranslation?: string | null
}

/** Payload nộp kết quả lật thẻ (POST /srs/items/{id}/review). */
export interface SrsReviewRequest {
  rating: SrsRating
  responseTimeMs?: number
  /** UUID sinh ở client — chống gửi trùng khi retry. */
  clientEventId?: string
}

/** Kết quả sau khi nộp đánh giá (SM-2). */
export interface SrsReviewResult {
  srsItemId: number
  rating: SrsRating
  previousInterval: number
  newInterval: number
  previousEf: number
  newEf: number
  newStatus: SrsStatus
  nextReviewAt: string
  xpEarned: number
}

/** Thống kê SRS (GET /srs/stats). */
export interface SrsStats {
  totalItems: number
  dueTodayCount: number
  learningCount: number
  reviewingCount: number
  masteredCount: number
}

/** Payload thêm thẻ lẻ (POST /srs/items). */
export interface SrsAddItemRequest {
  vocabId?: number
  characterId?: number
  grammarId?: number
}

/** Payload thêm hàng loạt (POST /srs/batch). */
export interface SrsBatchAddRequest {
  lessonId?: number
  vocabIds?: number[]
  characterIds?: number[]
  grammarIds?: number[]
}

/** Một ngày trong dự báo ôn tập SRS. */
export interface SrsForecastDay {
  /** Ngày (yyyy-MM-dd). */
  date: string
  /** Số thẻ đến hạn trong ngày đó (hôm nay gồm cả thẻ quá hạn). */
  count: number
  /** true nếu là ngày hôm nay. */
  today: boolean
}

/** Dự báo số thẻ cần ôn: hôm nay + 4 ngày tiếp theo. */
export interface SrsForecast {
  days: SrsForecastDay[]
}

/** Tập hợp các ID đã được lưu vào SRS của học viên. */
export interface SrsSavedIds {
  vocabIds: number[]
  characterIds: number[]
  grammarIds: number[]
}