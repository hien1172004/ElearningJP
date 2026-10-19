import type { JlptLevel } from './dictionary.types'

// ============================================================================
// ENUMS
// ============================================================================
export type EnrollmentStatus = 'ACTIVE' | 'COMPLETED' | 'DROPPED'
export type ProgressStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'
export type LessonSectionType = 'KANJI' | 'VOCABULARY' | 'GRAMMAR'

// ============================================================================
// COURSE
// ============================================================================
export interface CourseSummary {
  id: number
  title: string
  slug: string
  jlptLevel: JlptLevel
  description?: string
  thumbnailUrl?: string
  published: boolean
  createdByUserId?: number
  createdByName?: string
  totalLessons?: number
  totalEnrollments?: number
  createdAt?: string
  updatedAt?: string
  archivedAt?: string | null
}

export interface CourseDetail extends CourseSummary {
  isEnrolled?: boolean | null
  progressPercent?: number
  completedLessonsCount?: number
  totalEnrollments?: number
  lessons?: LessonOutline[]
}

export interface CourseCreateRequest {
  title: string
  slug: string
  jlptLevel: JlptLevel
  description?: string
  thumbnailUrl?: string
  published?: boolean
}

export interface CourseUpdateRequest {
  title?: string
  slug?: string
  jlptLevel?: JlptLevel
  description?: string
  thumbnailUrl?: string
}

// ============================================================================
// ENROLLMENT
// ============================================================================
export interface Enrollment {
  id: number
  userId: number
  courseId: number
  courseTitle?: string
  courseSlug?: string
  courseThumbnailUrl?: string
  courseJlptLevel?: JlptLevel
  status: EnrollmentStatus
  progressPercent?: number
  enrolledAt?: string
  completedAt?: string
}

// ============================================================================
// LESSON
// ============================================================================
export interface LessonSummary {
  id: number
  courseId: number
  title: string
  orderIndex: number
  hasQuiz?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface LessonOutline {
  id: number
  title: string
  orderIndex: number
  hasQuiz: boolean
  status?: ProgressStatus | null
  bestScore?: number | null
}

export interface LessonDetail {
  id: number
  courseId: number
  courseTitle?: string
  title: string
  orderIndex: number
  hasQuiz: boolean
  status?: ProgressStatus | null
  bestScore?: number | null
  lastAccessedAt?: string
  completedAt?: string
  sections?: LessonSection[]
}

export interface LessonCreateRequest {
  title: string
  orderIndex: number
}

export interface LessonUpdateRequest {
  title?: string
  orderIndex?: number
}

export interface LessonReorderRequest {
  lessonIds: number[]
}

// ============================================================================
// LESSON SECTIONS
// ============================================================================
export interface LessonSection {
  id: number
  sectionType: LessonSectionType
  title: string
  orderIndex: number
  kanjiItems?: LessonKanjiItem[]
  vocabularyItems?: LessonVocabularyItem[]
  grammarItems?: LessonGrammarItem[]
}

export interface LessonSectionCreateRequest {
  sectionType: LessonSectionType
  title: string
  orderIndex: number
}

export interface LessonSectionUpdateRequest {
  title?: string
  orderIndex?: number
}

export interface LessonSectionReorderRequest {
  sectionIds: number[]
}

// ============================================================================
// LESSON KANJI ITEM
// ============================================================================
// Type này match với BE LessonKanjiItemResponse (trả về trong lesson.sections[].kanjiItems[]).
// Bao gồm luôn character, hanViet, onyomi, kunyomi, relatedVocabulary...
// → Không cần gọi thêm API GET /lesson-kanji-items/{id} khi đã có lesson detail.
export interface LessonKanjiItem {
  id: number
  orderIndex?: number
  note?: string
  characterId?: number
  character?: string
  hanViet?: string
  meaningVi?: string
  meaningEn?: string
  strokeCount?: number
  onyomi?: string[]
  kunyomi?: string[]
  romaji?: string
  jlptLevel?: string
  radicals?: string[]
  imageUrl?: string
  relatedVocabulary?: LessonKanjiRelatedVocab[]
}

export interface LessonKanjiItemCreateRequest {
  /**
   * Chữ Hán (1 hoặc nhiều ký tự). BE dùng field name là "character".
   */
  character?: string
  /** @deprecated BE dùng "character". Để tương thích ngược nếu ai đó đang dùng. */
  kanjiCharacter?: string
  kanjiId?: number
  note?: string
  orderIndex?: number
  /**
   * Optional danh sách related vocab nhúng trong payload.
   * Match với BE LessonKanjiRelatedVocabRequest:
   *   { vocabIds?: [Long, ...], newVocab?: { word, hiragana, ... }, note? }
   * Mỗi entry có thể chọn 1+ vocab có sẵn (vocabIds) HOẶC tạo mới (newVocab) HOẶC cả 2.
   * Nếu không gửi → BE tự derive từ bảng vocabulary_characters.
   */
  relatedVocabulary?: LessonKanjiRelatedVocabRequest[]
}

export interface LessonKanjiItemUpdateRequest {
  note?: string
  orderIndex?: number
  /**
   * Nếu truyền → thay thế toàn bộ related vocab của item.
   * Truyền mảng rỗng [] để xoá hết (BE sẽ fallback derive).
   * Không truyền → BE giữ nguyên related vocab hiện tại.
   */
  relatedVocabulary?: LessonKanjiRelatedVocabRequest[]
}

/**
 * Payload gửi lên BE cho mỗi entry relatedVocabulary.
 * BE yêu cầu: { vocabIds?: [Long, ...], newVocab?: { word, hiragana (NotBlank), ... } }
 * - Có thể có vocabIds, có thể có newVocab, hoặc cả 2
 * - newVocab.word và newVocab.hiragana là bắt buộc (NotBlank)
 * - newVocab chỉ tạo được 1 vocab mới mỗi entry
 */
export interface LessonKanjiRelatedVocabRequest {
  /** Chọn nhiều vocab có sẵn từ từ điển theo id. Mỗi id tạo 1 bản ghi related vocab. */
  vocabIds?: number[]
  /** Tạo 1 vocab mới trong từ điển rồi gắn vào. word + hiragana bắt buộc. */
  newVocab?: {
    word: string
    hiragana: string
    romaji?: string
    hanViet?: string
    meaningVi?: string
    meaningEn?: string
    partOfSpeech?: string
  }
  /** Ghi chú chung cho cả entry này. */
  note?: string
}

// ============================================================================
// LESSON VOCAB ITEM
// ============================================================================
// Type này match với BE LessonVocabularyItemResponse (trả về trong lesson.sections[].vocabularyItems[]).
// Bao gồm word, hiragana, hanViet, meaningVi, examples, custom* fields, display* fields...
// → Không cần gọi thêm API khi đã có lesson detail.
export interface LessonVocabularyItem {
  id: number
  orderIndex?: number
  note?: string
  vocabId?: number
  word?: string
  hiragana?: string
  romaji?: string
  hanViet?: string
  meaningVi?: string
  meaningEn?: string
  partOfSpeech?: string
  jlptLevel?: string
  customHiragana?: string
  customRomaji?: string
  customAudioUrl?: string
  customMeaning?: string
  imageUrl?: string
  displayHiragana?: string
  displayRomaji?: string
  displayAudioUrl?: string
  displayMeaning?: string
  examples?: LessonVocabularyExample[]
}

export interface LessonVocabularyExample {
  id?: number
  orderIndex?: number
  exampleJp?: string
  exampleVi?: string
  exampleRomaji?: string
  audioUrl?: string
  isPrimary?: boolean
}

export interface LessonVocabularyItemCreateRequest {
  /** ID vocab trong từ điển (BẮT BUỘC theo BE). */
  vocabId: number
  orderIndex?: number
  /** Override fields — chỉ gửi khi giáo viên muốn dùng giá trị khác từ điển */
  customHiragana?: string
  customRomaji?: string
  customAudioUrl?: string
  customMeaning?: string
  imageUrl?: string
  note?: string
  /**
   * Semantics:
   * - null (không gửi) HOẶC [] → BE auto-copy tất cả examples từ vocabulary.examples
   * - [...] → dùng danh sách này (giáo viên kiểm soát hoàn toàn, không auto-copy)
   */
  examples?: LessonVocabularyExampleCreateRequest[]
}

export interface LessonVocabularyItemUpdateRequest {
  orderIndex?: number
  /** Override fields — 3-state semantics: null = giữ, "" = xoá override, value = update */
  customHiragana?: string | null
  customRomaji?: string | null
  customAudioUrl?: string | null
  customMeaning?: string | null
  imageUrl?: string | null
  note?: string
  /**
   * Semantics:
   * - null (không gửi) → giữ nguyên examples hiện tại
   * - [] (mảng rỗng) → RESET: xoá hết và auto-copy lại từ vocabulary.examples
   * - [...] → upsert từng entry (id=null tạo mới, id!=null cập nhật; thiếu → soft-delete)
   */
  examples?: LessonVocabularyExampleUpsertRequest[]
}

export interface LessonVocabularyExampleCreateRequest {
  exampleJp: string
  exampleVi: string
  exampleRomaji?: string
  audioUrl?: string
  orderIndex?: number
  isPrimary?: boolean
}

export interface LessonVocabularyExampleUpsertRequest {
  /** id = null → tạo mới; id != null → cập nhật */
  id?: number | null
  exampleJp: string
  exampleVi: string
  exampleRomaji?: string
  audioUrl?: string
  orderIndex?: number
  isPrimary?: boolean
}

// ============================================================================
// LESSON GRAMMAR ITEM
// ============================================================================
// Type này match với BE LessonGrammarItemResponse (trả về trong lesson.sections[].grammarItems[]).
// Bao gồm title, structure, meaningVi, usageNotes, examples, custom* fields, display* fields...
// → Không cần gọi thêm API khi đã có lesson detail.
export interface LessonGrammarItem {
  id: number
  orderIndex?: number
  note?: string
  grammarId?: number
  title?: string
  structure?: string
  meaningVi?: string
  usageNotes?: string
  jlptLevel?: string
  customMeaning?: string
  customStructure?: string
  displayMeaning?: string
  displayStructure?: string
  examples?: LessonGrammarExample[]
}

export interface LessonGrammarExample {
  id?: number
  orderIndex?: number
  exampleJp?: string
  exampleVi?: string
  exampleRomaji?: string
}

export interface LessonGrammarItemCreateRequest {
  /** ID grammar trong từ điển (BẮT BUỘC theo BE). */
  grammarId: number
  orderIndex?: number
  /** Override fields */
  customMeaning?: string
  customStructure?: string
  note?: string
  /**
   * Semantics:
   * - null (không gửi) HOẶC [] → BE auto-copy tất cả examples từ grammarPoint.examplesJson
   * - [...] → dùng danh sách này (giáo viên kiểm soát hoàn toàn)
   */
  examples?: LessonGrammarExampleCreateRequest[]
}

export interface LessonGrammarItemUpdateRequest {
  orderIndex?: number
  /** 3-state: null = giữ, "" = xoá override, value = update */
  customMeaning?: string | null
  customStructure?: string | null
  note?: string
  /**
   * Semantics:
   * - null (không gửi) → giữ nguyên examples hiện tại
   * - [] → RESET: xoá hết và auto-copy lại từ grammarPoint.examplesJson
   * - [...] → upsert (id=null tạo mới, id!=null cập nhật; thiếu → soft-delete)
   */
  examples?: LessonGrammarExampleUpsertRequest[]
}

export interface LessonGrammarExampleCreateRequest {
  exampleJp: string
  exampleVi: string
  orderIndex?: number
}

export interface LessonGrammarExampleUpsertRequest {
  /** id = null → tạo mới; id != null → cập nhật */
  id?: number | null
  exampleJp: string
  exampleVi: string
  orderIndex?: number
}

// ============================================================================
// PROGRESS
// ============================================================================
export interface CourseProgress {
  courseId: number
  totalLessons: number
  completedLessons: number
  inProgressLessons: number
  notStartedLessons: number
  progressPercent: number
  enrolledAt?: string
  lastAccessedAt?: string
  progress?: LessonProgressItem[]
}

export interface LessonProgressItem {
  lessonId: number
  lessonTitle: string
  lessonOrderIndex: number
  status: ProgressStatus
  bestScore?: number | null
  lastAccessedAt?: string
  completedAt?: string
}

// ============================================================================
// QUIZ (read-only types; quiz UI out of scope)
// ============================================================================
export interface QuizQuestion {
  id: number
  questionText: string
  orderIndex?: number
  options?: QuizOption[]
}

export interface QuizOption {
  id: number
  content: string
  correct?: boolean
}

// ============================================================================
// LESSON KANJI ITEM - related vocab (chỉ là phần phụ, khai báo riêng cho gọn)
// ============================================================================
export interface LessonKanjiRelatedVocab {
  id: number | null // null khi derive tự động từ từ điển
  note?: string
  vocabId: number
  word: string
  hiragana?: string
  meaningVi?: string
}

// ============================================================================
// LESSON VOCABULARY ITEM (example shape BE trả về)
// ============================================================================
export interface LessonVocabularyExampleResponse {
  id?: number
  orderIndex?: number
  exampleJp?: string
  exampleVi?: string
  exampleRomaji?: string
  audioUrl?: string
  isPrimary?: boolean
}

// ============================================================================
// LESSON GRAMMAR ITEM (example shape BE trả về)
// ============================================================================
export interface LessonGrammarExampleResponse {
  id?: number
  orderIndex?: number
  exampleJp?: string
  exampleVi?: string
}

// ============================================================================
// LESSON QUIZ (endpoint /lessons/{id}/quiz)
// ============================================================================
export type QuizQuestionType =
  | 'MULTIPLE_CHOICE'
  | 'MATCHING'
  | 'FILL_IN_THE_BLANK'
  | string

/** Item bên trái của câu hỏi MATCHING. */
export interface QuizMatchingLeftItem {
  key: string
  label: string
}

/** Item bên phải của câu hỏi MATCHING. */
export interface QuizMatchingRightItem {
  key: string
  label: string
}

/**
 * Cấu trúc matchingData trả về từ BE.
 * - left/right: 2 danh sách nhãn để người dùng ghép cặp.
 *   Mảng `right` đã được shuffle sẵn lúc teacher save (mỗi save là 1 thứ tự
 *   khác nhau) → viewer chỉ cần duyệt theo index tự nhiên.
 * - options: danh sách "đáp án đúng" dạng optionText mô tả từng cặp
 *   (vd: "学 = học"). Mỗi option.id được dùng làm selectedOptionId khi nộp bài.
 */
export interface QuizMatchingData {
  left: QuizMatchingLeftItem[]
  right: QuizMatchingRightItem[]
}

export interface QuizOptionApi {
  id: number
  optionKey: number
  optionText: string
  /** MATCHING: key dạng "L_key+R_key" do BE trả về */
  matchKey?: string
}

export interface QuizQuestionApi {
  id: number
  questionText: string
  orderIndex?: number
  questionType?: QuizQuestionType
  matchingData?: QuizMatchingData
  options?: QuizOptionApi[]
}

export interface LessonQuizDetail {
  quizId: number
  lessonId: number
  title?: string
  description?: string
  passingScorePercent?: number
  totalQuestions?: number
  questions: QuizQuestionApi[]
}

/**
 * Option kèm đáp án đúng — chỉ dùng cho trang quản lý (Admin/Teacher).
 * Học viên dùng {@link QuizOptionApi} (ẩn `correct`).
 */
export interface QuizOptionWithAnswer extends QuizOptionApi {
  /** Đáp án đúng (MCQ). */
  correct: boolean
}

/**
 * Câu hỏi kèm đáp án đúng — endpoint quản lý.
 * Endpoint: GET /api/v1/lessons/{id}/quiz/manage
 */
export interface QuizQuestionWithAnswer {
  id: number
  questionText: string
  orderIndex?: number
  questionType?: QuizQuestionType
  explanation?: string
  matchingData?: QuizMatchingData
  /** FILL_IN_THE_BLANK */
  correctTextAnswer?: string
  /** MATCHING: slotKey -> correctText */
  correctMatchingAnswers?: Record<string, string>
  options: QuizOptionWithAnswer[]
}

/**
 * Response cho GET /lessons/{id}/quiz/manage — đề + đáp án đúng.
 */
export interface LessonQuizManageDetail {
  quizId: number
  lessonId: number
  title?: string
  description?: string
  passingScorePercent?: number
  totalQuestions?: number
  questions: QuizQuestionWithAnswer[]
}

export interface QuizSubmitAnswer {
  questionId: number
  /** Dùng cho MULTIPLE_CHOICE */
  selectedOptionId?: number
  /** Dùng cho FILL_IN_THE_BLANK (tự gõ đáp án) */
  textAnswer?: string
  /** Dùng cho MATCHING: Map<targetKey, selectedKey> */
  matchingAnswers?: Record<string, string>
  /** Thời gian làm câu hỏi (ms) */
  responseTimeMs?: number
}

export interface QuizSubmitRequest {
  answers: QuizSubmitAnswer[]
}

export interface QuizDetailItem {
  questionId: number
  correct: boolean
  isCorrect?: boolean
  questionText?: string
  questionType?: QuizQuestionType
  /** MCQ: option học viên chọn */
  selectedOptionId?: number
  selectedOptionText?: string
  /** FILL_IN_THE_BLANK: text học viên gõ */
  userTextAnswer?: string
  /** MATCHING: Map slotKey -> userText */
  userMatchingAnswers?: Record<string, string>
  /** MCQ: option đúng */
  correctOptionId?: number
  correctOptionText?: string
  /** FILL_IN_THE_BLANK: đáp án chuẩn */
  correctTextAnswer?: string
  /** MATCHING: Map slotKey -> correctText */
  correctMatchingAnswers?: Record<string, string>
  explanation?: string
}

export interface QuizSubmitResultItem {
  questionId: number
  correct: boolean
  correctOptionIds?: number[]
  selectedOptionIds?: number[]
  explanation?: string
}

export interface QuizSubmitResponse {
  totalQuestions: number
  correctCount: number
  /** 0-100 */
  scorePercent: number
  passingScorePercent?: number
  passed: boolean
  lessonStatus?: ProgressStatus
  bestScore?: number
  results: QuizSubmitResultItem[]
  newBestScore?: number
  lessonId?: number
}
