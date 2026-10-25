import { axiosClient } from './api/axiosClient'
import { storage } from '@/utils/storage'
import type { PageResponse } from '@/types/api.types'
import type {
  AiScenario,
  AiSession,
  AiSpeakingStreamEvent,
  CreateSessionRequest,
  FuriganaResponse,
  JlptLevel,
  TranslateResponse,
  TtsResponse,
} from '@/types/speaking.types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const speakingService = {
  /**
   * Tạo phiên luyện nói mới (Kịch bản hoặc Tự do)
   */
  async createSession(data: CreateSessionRequest): Promise<AiSession> {
    const res: any = await axiosClient.post('/ai/sessions', data)
    return res.data ?? res
  },

  /**
   * Lấy chi tiết phiên luyện nói và toàn bộ lịch sử tin nhắn
   */
  async getSession(id: number): Promise<AiSession> {
    const res: any = await axiosClient.get(`/ai/sessions/${id}`)
    return res.data ?? res
  },

  /**
   * Danh sách các phiên luyện nói của người dùng (phân trang)
   */
  async getUserSessions(page = 0, size = 10): Promise<PageResponse<AiSession>> {
    const res: any = await axiosClient.get('/ai/sessions', {
      params: { page, size, sort: 'createdAt,desc' },
    })
    return res.data ?? res
  },

  /**
   * Xóa mềm một phiên luyện nói
   */
  async deleteSession(id: number): Promise<void> {
    await axiosClient.delete(`/ai/sessions/${id}`)
  },

  /**
   * Xóa toàn bộ lịch sử luyện nói của người dùng
   */
  async deleteAllSessions(): Promise<void> {
    await axiosClient.delete('/ai/sessions')
  },

  /**
   * Danh sách tình huống luyện nói (có lọc theo chủ đề và cấp độ JLPT)
   */
  async getScenarios(topicId?: string, jlptLevel?: JlptLevel): Promise<AiScenario[]> {
    const res: any = await axiosClient.get('/ai/scenarios', {
      params: { topicId, jlptLevel },
    })
    return res.data ?? res ?? []
  },

  /**
   * Chi tiết một kịch bản luyện nói theo ID
   */
  async getScenarioById(id: number): Promise<AiScenario> {
    const res: any = await axiosClient.get(`/ai/scenarios/${id}`)
    return res.data ?? res
  },

  /**
   * Tạo tình huống tùy chỉnh do người dùng nhập
   */
  async createCustomScenario(data: {
    userRole: string
    aiRole: string
    description: string
    jlptLevel: JlptLevel
    title?: string
  }): Promise<AiScenario> {
    const res: any = await axiosClient.post('/ai/scenarios', data)
    return res.data ?? res
  },

  /**
   * Phân tích hình thái câu tiếng Nhật qua Kuromoji sinh Furigana, Romaji và thẻ <ruby>
   */
  async getFurigana(text: string): Promise<FuriganaResponse> {
    const res: any = await axiosClient.post('/ai/furigana', { text })
    return res.data ?? res
  },

  /**
   * Tạo âm thanh phát âm TTS và lấy URL file audio
   */
  async generateTts(text: string, voice = 'ja', messageId?: number): Promise<TtsResponse> {
    const res: any = await axiosClient.post('/ai/tts', {
      text,
      voice,
      messageId,
    })
    return res.data ?? res
  },

  /**
   * Dịch câu tiếng Nhật sang tiếng Việt và lưu lại vào tin nhắn
   */
  async translate(text?: string, messageId?: number): Promise<TranslateResponse> {
    const res: any = await axiosClient.post('/ai/translate', {
      text,
      messageId,
    })
    return res.data ?? res
  },

  /**
   * Gửi câu thoại của học viên và nhận phản hồi streaming qua Server-Sent Events (SSE)
   */
  sendStreamMessage(
    sessionId: number,
    message: string,
    callbacks: {
      onChunk: (chunk: string) => void
      onComplete: (event: AiSpeakingStreamEvent) => void
      onError: (err: Error) => void
    },
  ): () => void {
    const controller = new AbortController()
    const token = storage.getAccessToken()

    ;(async () => {
      try {
        const url = `${API_BASE_URL}/ai/sessions/${sessionId}/messages`
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify({ message }),
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`Lỗi kết nối máy chủ: ${response.status} ${response.statusText}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
          throw new Error('Trình duyệt không hỗ trợ đọc luồng dữ liệu (ReadableStream)')
        }

        const decoder = new TextDecoder('utf-8')
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          // Giữ lại phần chưa kết thúc dòng ở cuối buffer
          buffer = lines.pop() || ''

          for (const line of lines) {
            const trimmed = line.trim()
            if (trimmed.startsWith('data:')) {
              const jsonStr = trimmed.replace(/^data:\s*/, '')
              if (!jsonStr) continue
              try {
                const event: AiSpeakingStreamEvent = JSON.parse(jsonStr)
                if (event.type === 'CHUNK' && event.chunk) {
                  callbacks.onChunk(event.chunk)
                } else if (event.type === 'COMPLETE') {
                  callbacks.onComplete(event)
                } else if (event.type === 'ERROR') {
                  callbacks.onError(new Error(event.errorMessage || 'Lỗi không xác định từ AI'))
                }
              } catch (parseError) {
                console.warn('Lỗi parse SSE event JSON:', jsonStr, parseError)
              }
            }
          }
        }
      } catch (err: unknown) {
        if ((err as Error).name === 'AbortError') {
          return // User cancelled
        }
        callbacks.onError(err instanceof Error ? err : new Error(String(err)))
      }
    })()

    // Trả về hàm hủy kết nối
    return () => controller.abort()
  },
}
