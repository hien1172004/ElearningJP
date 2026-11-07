import type { JlptLevel } from './dictionary.types'
import type {
  SampleDialogueLine,
  ScenarioGrammar,
  ScenarioTask,
  ScenarioVocab,
} from './speaking.types'

/** Khớp `AdminScenarioResponse` ở BE (GET /admin/scenarios). */
export interface AdminScenario {
  id: number
  topicId: string
  jlptLevel: JlptLevel
  title: string
  description: string
  userRole: string
  aiRole: string
  tasks: ScenarioTask[]
  sampleDialogue: SampleDialogueLine[]
  vocabulary: ScenarioVocab[]
  grammar: ScenarioGrammar[]
  tips: string[]
  isPremium: boolean
  isLessonOnly?: boolean
  thumbnailUrl?: string | null
  /** null = kịch bản hệ thống hoặc do khách chưa đăng nhập tạo */
  createdById?: number | null
  createdByName?: string | null
  createdByEmail?: string | null
  createdAt?: string
  updatedAt?: string
}

export type ScenarioSpeaker = 'AI' | 'USER'

/** Khớp `AdminScenarioRequest` ở BE (POST/PUT /admin/scenarios). */
export interface AdminScenarioPayload {
  topicId: string
  jlptLevel: JlptLevel
  title: string
  description: string
  userRole: string
  aiRole: string
  tasks: { text: string }[]
  sampleDialogue: { speaker: ScenarioSpeaker; text: string; vi?: string }[]
  vocabulary: { word: string; reading?: string; meaning: string }[]
  grammar: { pattern: string; meaning: string; example?: string; exampleMeaning?: string }[]
  tips: string[]
  isPremium: boolean
  isLessonOnly?: boolean
  thumbnailUrl?: string
}

export interface AdminScenarioListParams {
  keyword?: string
  topicId?: string
  jlptLevel?: JlptLevel
  page?: number
  size?: number
}
