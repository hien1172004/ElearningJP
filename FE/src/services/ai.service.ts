import axios from 'axios'

const AI_API_URL = import.meta.env.VITE_AI_API_URL || 'http://localhost:8000'

export interface PredictionItem {
  char: string
  unicode_hex?: string
  confidence: number
  percentage: number
}

export interface PredictionResponse {
  success: boolean
  top_char: string | null
  predictions: PredictionItem[]
  message?: string
}

export interface StrokePoint {
  x: number
  y: number
  t?: number
}

export interface ReferenceStroke {
  stroke_number: number
  svg_path_data?: string
  sample_points?: number[][]
}

export interface StrokeAssessmentRequest {
  character: string
  user_strokes: StrokePoint[][]
  canvas_width?: number
  canvas_height?: number
  reference_strokes?: ReferenceStroke[]
}

export interface StrokeEvaluationDetail {
  stroke_number: number
  user_stroke_index: number
  order_correct: boolean
  direction_score: number
  is_direction_correct: boolean
  is_reversed: boolean
  shape_score?: number
  feedback: string
}

export interface StrokeAssessmentResponse {
  success: boolean
  character: string
  total_strokes_expected: number
  total_strokes_received: number
  is_stroke_count_correct: boolean
  is_all_correct: boolean
  overall_score: number
  feedback: string
  stroke_details: StrokeEvaluationDetail[]
}

export const aiService = {
  /**
   * Gửi ảnh nét vẽ dạng Blob qua FormData sang AI Server
   */
  async predictBlob(blob: Blob, topK = 10): Promise<PredictionResponse> {
    const formData = new FormData()
    formData.append('file', blob, 'canvas.png')

    const response = await axios.post<PredictionResponse>(
      `${AI_API_URL}/api/v1/predict?top_k=${topK}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        timeout: 5000,
      }
    )
    return response.data
  },

  /**
   * Gửi ảnh nét vẽ dạng chuỗi Base64 DataURL sang AI Server
   */
  async predictBase64(base64DataUrl: string, topK = 10): Promise<PredictionResponse> {
    const response = await axios.post<PredictionResponse>(
      `${AI_API_URL}/api/v1/predict-base64`,
      {
        image: base64DataUrl,
        top_k: topK,
      },
      {
        headers: {
          'Content-Type': 'application/json',
        },
        timeout: 5000,
      }
    )
    return response.data
  },

  /**
   * Đánh giá thứ tự nét và chiều nét viết Kanji/Kana
   */
  async evaluateStrokes(request: StrokeAssessmentRequest): Promise<StrokeAssessmentResponse> {
    const response = await axios.post<StrokeAssessmentResponse>(
      `${AI_API_URL}/api/v1/evaluate-strokes`,
      request,
      {
        headers: {
          'Content-Type': 'application/json',
        },
        timeout: 5000,
      }
    )
    return response.data
  },

  /**
   * Lấy thông tin nét vẽ chuẩn (KanjiVG) của một ký tự bất kỳ trong 2,353 chữ
   */
  async getKanjiStrokes(character: string): Promise<{
    character: string
    char_type: string
    stroke_count: number
    han_viet?: string
    meaning_vi?: string
    meaning_en?: string
    jlpt_level?: string
    strokes: ReferenceStroke[]
  }> {
    const response = await axios.get(
      `${AI_API_URL}/api/v1/kanji-strokes/${encodeURIComponent(character)}`,
      { timeout: 4000 }
    )
    return response.data
  },

  /**
   * Lấy danh sách các ký tự theo cấp độ JLPT hoặc Kana
   */
  async getKanjiList(level?: string, limit = 200): Promise<{
    total: number
    items: Array<{
      character: string
      char_type: string
      stroke_count: number
      han_viet?: string
      meaning_vi?: string
      jlpt_level?: string
    }>
  }> {
    const params = new URLSearchParams()
    if (level) params.append('level', level)
    params.append('limit', limit.toString())

    const response = await axios.get(
      `${AI_API_URL}/api/v1/kanji-list?${params.toString()}`,
      { timeout: 4000 }
    )
    return response.data
  },

  /**
   * Kiểm tra tình trạng hoạt động của AI Service
   */
  async checkHealth(): Promise<boolean> {
    try {
      const res = await axios.get(`${AI_API_URL}/health`, { timeout: 2000 })
      return res.status === 200
    } catch {
      return false
    }
  },
}
