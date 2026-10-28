export type JlptLevel = 'N5' | 'N4' | 'N3' | 'N2' | 'N1'

export type AiPracticeMode = 'SCENARIO' | 'FREE'

export type AiMessageRole = 'USER' | 'AI'

export interface ScenarioTask {
  id: number
  text: string
}

export interface SampleDialogueLine {
  speaker?: string
  role?: string
  text?: string
  ja?: string
  vi?: string
  meaning?: string
}

export interface ScenarioVocab {
  word: string
  reading?: string
  meaning: string
}

export interface ScenarioGrammar {
  pattern: string
  meaning: string
  example?: string
  exampleMeaning?: string
  explanation?: string
}

export interface AiScenario {
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
  thumbnailUrl?: string
  createdAt?: string
}

export interface GrammarCorrection {
  original: string
  fixed: string
  note_vi?: string
}

export interface AiMessage {
  id?: number
  sessionId?: number
  role: AiMessageRole
  textJa: string
  textVi?: string
  audioUrl?: string
  correction?: GrammarCorrection | null
  createdAt?: string
  isStreaming?: boolean
}

export interface AiSession {
  id: number
  userId: number
  scenario?: AiScenario | null
  mode: AiPracticeMode
  title: string
  completedTasks: number[]
  messages: AiMessage[]
  messageCount?: number
  createdAt?: string
}

export interface AiSpeakingStreamEvent {
  type: 'CHUNK' | 'COMPLETE' | 'ERROR'
  chunk?: string
  response?: {
    reply_ja: string
    reply_vi?: string
    replyVi?: string
    correction?: GrammarCorrection | null
    completed_task_ids?: number[]
    tokensUsed?: number
  }
  userMessageId?: number
  aiMessageId?: number
  errorMessage?: string
}

export interface FuriganaToken {
  surface: string
  furigana?: string | null
  romaji: string
  isKanji: boolean
}

export interface FuriganaResponse {
  original: string
  rubyHtml: string
  romaji: string
  tokens: FuriganaToken[]
}

export interface TtsResponse {
  audioUrl: string
  hash: string
  cached: boolean
}

export interface TranslateResponse {
  messageId?: number
  originalText: string
  translatedText: string
  textVi?: string
}

export interface CreateSessionRequest {
  mode: AiPracticeMode
  scenarioId?: number
  topic?: string
  jlptLevel?: JlptLevel
}

export interface SendMessageRequest {
  message: string
}
