import type { JlptLevel } from './dictionary.types'

export type { JlptLevel }
export type ExamType = 'JLPT_OFFICIAL_MOCK' | 'MINI_TEST' | 'RANDOM_GENERATED'
export type AttemptType = 'FULL_EXAM' | 'PRACTICE_SECTION'
export type SessionType = 'VOCABULARY' | 'GRAMMAR_READING' | 'LISTENING' | 'KNOWLEDGE_READING'
export type ScoreGroupCode = 'LANGUAGE_KNOWLEDGE' | 'READING' | 'LANGUAGE_KNOWLEDGE_READING' | 'LISTENING'
export type PassageType = 'TEXT' | 'IMAGE_POSTER' | 'EMAIL' | 'NOTICE'
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'TIMED_OUT'

export interface ExamSummary {
  id: number
  title: string
  examType: ExamType
  jlptLevel: JlptLevel
  totalDurationMinutes: number
  totalScaledMaxScore: number
  overallPassingScore: number
  totalQuestions: number
  published: boolean
  courseId?: number
  status?: string
  passingScore?: number
}

export interface ExamScoreGroup {
  id: number
  groupCode: ScoreGroupCode
  maxScaledScore: number
  minRequiredScore: number
}

export interface ExamSectionSummary {
  id: number
  sessionType: SessionType
  sectionTitle: string
  orderIndex: number
  durationMinutes: number
  audioUrl?: string
  questionCount: number
}

export interface ExamDetail extends ExamSummary {
  scoreGroups: ExamScoreGroup[]
  sections: ExamSectionSummary[]
}

export interface TakingPassage {
  id: number
  title: string
  passageType: PassageType
  contentJp?: string
  imageUrl?: string
  translationVi?: string
}

export interface TakingOption {
  id: number
  optionKey: number
  optionText?: string
  optionImageUrl?: string
}

export interface TakingQuestion {
  id: number
  questionNumber: number
  mondaiCode?: string
  mondaiNameVi?: string
  instructionJp?: string
  instructionVi?: string
  questionText: string
  imageUrl?: string
  audioStartTime?: number
  audioEndTime?: number
  starPosition?: number
  hasVisualOptions: boolean
  passage?: TakingPassage
  options: TakingOption[]
}

export interface TakingSection {
  id: number
  sessionType: SessionType
  sectionTitle: string
  audioUrl?: string
  orderIndex: number
  durationMinutes?: number
  questions: TakingQuestion[]
}

export interface ExamTakingData {
  attemptId: number
  examId: number
  examTitle: string
  jlptLevel: JlptLevel
  attemptType: AttemptType
  sectionId?: number
  targetDurationMinutes: number
  startedAt: string
  sections: TakingSection[]
}

export interface AttemptGroupScore {
  scoreGroupId: number
  groupCode: ScoreGroupCode
  rawCorrectCount: number
  rawTotalCount: number
  scaledScore: number
  maxScaledScore: number
  minRequiredScore: number
  groupPassed: boolean
}

export interface ExamResult {
  attemptId: number
  examId: number
  examTitle: string
  jlptLevel: JlptLevel
  attemptType: AttemptType
  sectionId?: number
  status: AttemptStatus
  startedAt: string
  submittedAt: string
  timeSpentSeconds: number
  totalRawScore: number
  totalScaledScore: number
  totalScaledMaxScore: number
  overallPassingScore: number
  passed: boolean
  hasThresholdFailed: boolean
  groupScores: AttemptGroupScore[]
}

export interface ReviewOption extends TakingOption {
  correct: boolean
}

export interface ReviewStudentAnswer {
  selectedOptionId?: number
  orderedOptionIds?: number[]
  correct: boolean
  pointsEarned: number
  audioPlayCount: number
}

export interface ReviewQuestion extends Omit<TakingQuestion, 'options'> {
  transcriptJp?: string
  transcriptVi?: string
  explanationVi?: string
  options: ReviewOption[]
  studentAnswer?: ReviewStudentAnswer
}

export interface ReviewSection extends Omit<TakingSection, 'questions'> {
  questions: ReviewQuestion[]
}

export interface ExamReviewData extends ExamResult {
  sections: ReviewSection[]
}

export interface ExamAttemptHistory {
  attemptId: number
  examId: number
  examTitle: string
  jlptLevel: JlptLevel
  attemptType: AttemptType
  sectionTitle: string
  status: AttemptStatus
  startedAt: string
  submittedAt: string
  timeSpentSeconds: number
  totalRawScore: number
  totalScaledScore: number
  totalScaledMaxScore: number
  overallPassingScore: number
  passed: boolean
  groupScores: AttemptGroupScore[]
}

// ===== ADMIN PREVIEW & INLINE EDIT INTERFACES =====
export interface AdminTakingOption {
  id?: number
  optionKey: number
  optionText: string
  optionImageUrl?: string
  correct: boolean
}

export interface AdminTakingQuestion {
  id: number
  questionNumber: number
  mondaiCode?: string
  mondaiNameVi?: string
  instructionJp?: string
  instructionVi?: string
  questionText: string
  imageUrl?: string
  audioUrl?: string
  audioStartTime?: number
  audioEndTime?: number
  transcriptJp?: string
  transcriptVi?: string
  explanationVi?: string
  starPosition?: number
  hasVisualOptions: boolean
  passage?: TakingPassage
  options: AdminTakingOption[]
}

export interface AdminTakingSection {
  id: number
  sessionType: SessionType
  sectionTitle: string
  audioUrl?: string
  orderIndex: number
  durationMinutes?: number
  questions: AdminTakingQuestion[]
}

export interface AdminExamPreview {
  id: number
  title: string
  examType: ExamType
  jlptLevel: JlptLevel
  totalDurationMinutes: number
  totalQuestions: number
  published: boolean
  totalAttempts?: number
  inProgressAttempts?: number
  sections: AdminTakingSection[]
}

export interface UpdateQuestionInlinePayload {
  questionText?: string
  imageUrl?: string
  audioUrl?: string
  audioStartTime?: number
  audioEndTime?: number
  transcriptJp?: string
  transcriptVi?: string
  explanationVi?: string
  starPosition?: number
  hasVisualOptions?: boolean
  mondaiCode?: string
  passageTitle?: string
  passageContentJp?: string
  passageTranslationVi?: string
  passageImageUrl?: string
  correctOptionKey?: number
  options?: AdminTakingOption[]
}

export interface CreateQuestionPayload {
  mondaiCode?: string
  questionText: string
  points?: number
  explanationVi?: string
  starPosition?: number
  hasVisualOptions?: boolean
  imageUrl?: string
  audioUrl?: string
  audioStartTime?: number
  audioEndTime?: number
  transcriptJp?: string
  transcriptVi?: string
  passageId?: number
  passageTitle?: string
  passageContentJp?: string
  passageTranslationVi?: string
  passageImageUrl?: string
  options: {
    optionKey: number
    optionText: string
    optionImageUrl?: string
    isCorrect: boolean
  }[]
}

export interface LeaderboardItem {
  rank: number
  userId: number
  fullName: string
  avatarUrl?: string
  score: number
  maxScore: number
  jlptLevel: JlptLevel
  examTitle?: string
  timeSpentSeconds?: number
  submittedAt?: string
  isCurrentUser: boolean
}

export interface LeaderboardData {
  rankings: LeaderboardItem[]
  myRank?: LeaderboardItem | null
  totalParticipants: number
}

