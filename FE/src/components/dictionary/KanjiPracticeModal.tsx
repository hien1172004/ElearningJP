import React, { useRef, useState, useEffect, useCallback } from 'react'
import {
  X,
  RotateCcw,
  Trash2,
  Sparkles,
  Loader2,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertTriangle,
  Award,
  ChevronRight,
  HelpCircle,
  Search,
} from 'lucide-react'
import type { CharacterStroke } from '@/types'
import {
  aiService,
  type StrokePoint,
  type StrokeAssessmentResponse,
} from '@/services'

const PRESET_KANJI: Record<string, { hanViet: string; meaningVi: string; strokes: CharacterStroke[] }> = {
  '二': {
    hanViet: 'NHỊ',
    meaningVi: 'Số 2, hai',
    strokes: [
      { strokeNumber: 1, svgPathData: 'M30,35 c1.5,0.2 4,0.3 6,0.2 c15,-1 30,-3 42,-3 c2,0 3.5,0.1 5,0.3' },
      { strokeNumber: 2, svgPathData: 'M15,75 c2,0.4 5,0.5 7.5,0.4 c25,-1.2 48,-3.5 62,-3.6 c3,0 5,0.2 6.5,0.4' },
    ],
  },
  '木': {
    hanViet: 'MỘC',
    meaningVi: 'Cây, gỗ',
    strokes: [
      { strokeNumber: 1, svgPathData: 'M13.25,38.22c2.25,0.41,4.52,0.49,6.75,0.25c15.12-1.6,44.75-4.84,62.25-5.97c2.47-0.16,4.88-0.08,7.25,0.47' },
      { strokeNumber: 2, svgPathData: 'M52.5,14.5c1.25,1.25,1.75,3,1.75,4.75c0,4.75-0.12,68.75-0.12,71.5c0,2.75-0.12,4.5-0.12,6.5' },
      { strokeNumber: 3, svgPathData: 'M51.75,39.5C44,53.25,29.5,72,16.25,79.5' },
      { strokeNumber: 4, svgPathData: 'M54.5,40.75c5.38,6.88,23.38,25.62,31.75,33.5c2.14,2.02,4.62,3.88,7.25,5' },
    ],
  },
  '日': {
    hanViet: 'NHẬT',
    meaningVi: 'Mặt trời, ngày',
    strokes: [
      { strokeNumber: 1, svgPathData: 'M29.75,19.25c0.88,0.88,1.25,2.25,1.25,3.75c0,11.5-0.12,48.5-0.12,69.5' },
      { strokeNumber: 2, svgPathData: 'M32.25,21.75c3.25-0.25,36.5-3.5,39.5-3.75c2.5-0.21,4.25,1.25,4.25,3.75c0,11.25-0.12,47.75-0.12,68.25' },
      { strokeNumber: 3, svgPathData: 'M32.25,53.25c8.5-0.75,33-3,42.5-3.25' },
      { strokeNumber: 4, svgPathData: 'M32.25,89.5c8.5-0.5,31.5-2.25,42.5-2.5' },
    ],
  },
  '一': {
    hanViet: 'NHẤT',
    meaningVi: 'Số 1, một',
    strokes: [
      { strokeNumber: 1, svgPathData: 'M12,54 c3,0.6 6,0.7 9.5,0.5 c20,-1.5 50,-5 68,-5.2 c3.5,0 5.7,0.2 7.5,0.5' },
    ],
  },
  '人': {
    hanViet: 'NHÂN',
    meaningVi: 'Người',
    strokes: [
      { strokeNumber: 1, svgPathData: 'M51.75,17.25c0.25,1.75,0.12,3.88-0.38,5.88C47.75,40.75,35.25,70.25,15.75,86' },
      { strokeNumber: 2, svgPathData: 'M47.75,44.75c5.62,9.38,22.12,31.25,31.62,39.62c2.25,2,4.88,3.62,7.38,4.88' },
    ],
  },
  '大': {
    hanViet: 'ĐẠI',
    meaningVi: 'To lớn, lớn',
    strokes: [
      { strokeNumber: 1, svgPathData: 'M16.25,43.75c2.5,0.5,5.25,0.62,7.88,0.38c16.62-1.62,45.88-4.88,61.12-5.75c2.62-0.12,5.25,0,7.75,0.62' },
      { strokeNumber: 2, svgPathData: 'M48.75,14.5c1.12,1.12,1.38,2.75,1.38,4.38c0,23.5-6.62,56.75-34.88,71.88' },
      { strokeNumber: 3, svgPathData: 'M50.25,45.25c6.5,9.5,23.38,32.12,34.12,40.62c2.38,1.88,4.88,3.38,7.62,4.38' },
    ],
  },
}

interface KanjiPracticeModalProps {
  isOpen: boolean
  onClose: () => void
  character?: string
  meaningVi?: string
  hanViet?: string
  strokes?: CharacterStroke[]
}

export const KanjiPracticeModal: React.FC<KanjiPracticeModalProps> = ({
  isOpen,
  onClose,
  character: propChar,
  meaningVi: propMeaning,
  hanViet: propHanViet,
  strokes: propStrokes,
}) => {
  const [selectedChar, setSelectedChar] = useState<string>(propChar || '木')
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const [isDrawing, setIsDrawing] = useState(false)
  const [userStrokes, setUserStrokes] = useState<StrokePoint[][]>([])
  const [currentStroke, setCurrentStroke] = useState<StrokePoint[]>([])
  const [showGhostGuide, setShowGhostGuide] = useState<boolean>(true)
  const [isEvaluating, setIsEvaluating] = useState<boolean>(false)
  const [assessment, setAssessment] = useState<StrokeAssessmentResponse | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Hỗ trợ gõ chữ bất kỳ & chọn theo cấp độ JLPT
  const [customInputChar, setCustomInputChar] = useState<string>('')
  const [isLoadingChar, setIsLoadingChar] = useState<boolean>(false)
  const [selectedLevel, setSelectedLevel] = useState<string>('N5')
  const [levelChars, setLevelChars] = useState<string[]>([])
  const [customCharData, setCustomCharData] = useState<{
    character: string
    han_viet?: string
    meaning_vi?: string
    strokes: CharacterStroke[]
  } | null>(null)

  // Đồng bộ khi propChar thay đổi từ bên ngoài
  useEffect(() => {
    if (propChar) {
      setSelectedChar(propChar)
      setCustomCharData(null)
      setUserStrokes([])
      setCurrentStroke([])
      setAssessment(null)
      setErrorMessage(null)
    }
  }, [propChar])

  // Tải danh sách chữ theo cấp độ JLPT / Kana
  useEffect(() => {
    if (isOpen) {
      aiService
        .getKanjiList(selectedLevel, 80)
        .then((res) => {
          if (res.items) {
            setLevelChars(res.items.map((it) => it.character))
          }
        })
        .catch(() => {})
    }
  }, [isOpen, selectedLevel])

  // Hàm tải dữ liệu nét của một chữ bất kỳ trong 2,353 chữ
  const loadAnyCharacter = async (charToLoad: string) => {
    const trimmed = charToLoad.trim()
    if (!trimmed) return
    const char = trimmed[0]
    setIsLoadingChar(true)
    setErrorMessage(null)

    try {
      const data = await aiService.getKanjiStrokes(char)
      const formattedStrokes: CharacterStroke[] = data.strokes.map((s) => ({
        strokeNumber: s.stroke_number,
        svgPathData: s.svg_path_data || '',
        samplePointsJson: s.sample_points,
      }))

      setCustomCharData({
        character: data.character,
        han_viet: data.han_viet,
        meaning_vi: data.meaning_vi,
        strokes: formattedStrokes,
      })
      setSelectedChar(data.character)
      setCustomInputChar('')
      setUserStrokes([])
      setCurrentStroke([])
      setAssessment(null)
    } catch {
      if (PRESET_KANJI[char]) {
        setSelectedChar(char)
        setCustomCharData(null)
        setCustomInputChar('')
        setUserStrokes([])
        setCurrentStroke([])
        setAssessment(null)
      } else {
        setErrorMessage(`Chưa có dữ liệu nét chuẩn cho ký tự '${char}'. Hãy thử ký tự khác.`)
      }
    } finally {
      setIsLoadingChar(false)
    }
  }

  // Lựa chọn chữ hiện tại (ưu tiên customCharData, rồi đến selectedChar)
  const currentChar = customCharData
    ? customCharData.character
    : selectedChar || propChar || '木'

  const activePreset = PRESET_KANJI[currentChar]

  const currentMeaning = customCharData
    ? customCharData.meaning_vi
    : (selectedChar === propChar && propMeaning ? propMeaning : activePreset?.meaningVi || '')

  const currentHanViet = customCharData
    ? customCharData.han_viet
    : (selectedChar === propChar && propHanViet ? propHanViet : activePreset?.hanViet || '')

  const activeStrokes = customCharData
    ? customCharData.strokes
    : (selectedChar === propChar && propStrokes && propStrokes.length > 0
        ? propStrokes
        : activePreset?.strokes || [])

  const handleSelectChar = (char: string) => {
    loadAnyCharacter(char)
  }

  const CANVAS_SIZE = 320

  // 1. Vẽ canvas: Lưới chữ Mễ + Nét vẽ người dùng (giữ nền trong suốt để thấy nét mờ SVG)
  const redrawCanvas = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Xóa sạch canvas dạng trong suốt (KHÔNG dùng fillRect trắng để không đè lên SVG nét mờ)
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // Ô lưới chữ Mễ (Quadrant + Diagonal lines)
    ctx.save()
    ctx.strokeStyle = '#e2e8f0'
    ctx.lineWidth = 1.2
    ctx.setLineDash([5, 5])

    // Đường kẻ ngang & dọc
    ctx.beginPath()
    ctx.moveTo(canvas.width / 2, 0)
    ctx.lineTo(canvas.width / 2, canvas.height)
    ctx.moveTo(0, canvas.height / 2)
    ctx.lineTo(canvas.width, canvas.height / 2)

    // Đường chéo 2 góc
    ctx.moveTo(0, 0)
    ctx.lineTo(canvas.width, canvas.height)
    ctx.moveTo(canvas.width, 0)
    ctx.lineTo(0, canvas.height)
    ctx.stroke()
    ctx.restore()

    // Cấu hình nét mực của người dùng
    ctx.strokeStyle = '#1e1b4b' // Màu mực chàm đậm (indigo-950)
    ctx.lineWidth = 8
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'

    const allStrokes = [...userStrokes, currentStroke]
    for (const stroke of allStrokes) {
      if (stroke.length === 0) continue
      ctx.beginPath()
      ctx.moveTo(stroke[0].x, stroke[0].y)
      for (let i = 1; i < stroke.length; i++) {
        ctx.lineTo(stroke[i].x, stroke[i].y)
      }
      ctx.stroke()
    }
  }, [userStrokes, currentStroke])

  useEffect(() => {
    if (isOpen) {
      redrawCanvas()
    }
  }, [isOpen, redrawCanvas])

  // Reset khi mở modal hoặc đổi chữ
  useEffect(() => {
    if (isOpen) {
      setUserStrokes([])
      setCurrentStroke([])
      setAssessment(null)
      setErrorMessage(null)
    }
  }, [isOpen, currentChar])

  // 2. Bắt tọa độ chuột / cảm ứng
  const getCoordinates = (e: React.MouseEvent | React.TouchEvent): StrokePoint | null => {
    const canvas = canvasRef.current
    if (!canvas) return null
    const rect = canvas.getBoundingClientRect()
    const scaleX = canvas.width / rect.width
    const scaleY = canvas.height / rect.height

    if ('touches' in e) {
      if (e.touches.length === 0) return null
      return {
        x: (e.touches[0].clientX - rect.left) * scaleX,
        y: (e.touches[0].clientY - rect.top) * scaleY,
        t: Date.now(),
      }
    } else {
      return {
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY,
        t: Date.now(),
      }
    }
  }

  const handleStart = (e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault()
    const point = getCoordinates(e)
    if (!point) return
    setIsDrawing(true)
    setCurrentStroke([point])
  }

  const handleMove = (e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault()
    if (!isDrawing) return
    const point = getCoordinates(e)
    if (!point) return
    setCurrentStroke((prev) => [...prev, point])
  }

  const handleEnd = (e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault()
    if (!isDrawing) return
    setIsDrawing(false)

    if (currentStroke.length > 1) {
      setUserStrokes((prev) => [...prev, currentStroke])
    }
    setCurrentStroke([])
  }

  // 3. Các nút điều khiển nét
  const handleUndo = () => {
    if (userStrokes.length > 0) {
      setUserStrokes((prev) => prev.slice(0, prev.length - 1))
      setAssessment(null)
    }
  }

  const handleClear = () => {
    setUserStrokes([])
    setCurrentStroke([])
    setAssessment(null)
    setErrorMessage(null)
  }

  // 4. Gửi chấm điểm nét vẽ sang AI Service
  const handleEvaluate = async () => {
    if (userStrokes.length === 0) {
      setErrorMessage('Vui lòng vẽ chữ trước khi bấm chấm điểm.')
      return
    }

    setIsEvaluating(true)
    setErrorMessage(null)

    try {
      // Chuyển đổi dữ liệu nét chuẩn từ KanjiVG
      const refStrokes = activeStrokes.map((s) => ({
        stroke_number: s.strokeNumber,
        svg_path_data: s.svgPathData,
        sample_points: s.samplePointsJson,
      }))

      const response = await aiService.evaluateStrokes({
        character: currentChar,
        user_strokes: userStrokes,
        canvas_width: CANVAS_SIZE,
        canvas_height: CANVAS_SIZE,
        reference_strokes: refStrokes,
      })

      if (response.success) {
        setAssessment(response)
      } else {
        setErrorMessage(response.feedback || 'Không thể đánh giá nét vẽ.')
      }
    } catch (err: any) {
      setErrorMessage(
        err?.response?.data?.detail ||
          'Không thể kết nối đến máy chủ AI (Port 8000). Vui lòng kiểm tra lại dịch vụ AI.'
      )
    } finally {
      setIsEvaluating(false)
    }
  }

  if (!isOpen) return null

  const expectedStrokes = activeStrokes.length

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl shadow-2xl border border-gray-100 w-full max-w-4xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header Modal */}
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-bold text-xl font-serif shadow-sm shadow-indigo-200">
              {currentChar}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-black text-gray-900">Luyện viết & Chấm điểm nét vẽ AI</h3>
                {currentHanViet && (
                  <span className="px-2 py-0.5 rounded-md bg-amber-100 text-amber-800 text-xs font-bold uppercase">
                    {currentHanViet}
                  </span>
                )}
              </div>
              <p className="text-xs text-gray-500">
                {currentMeaning ? `Nghĩa: ${currentMeaning} • ` : ''}Chuẩn: <span className="font-bold text-indigo-600">{expectedStrokes} nét</span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-wrap justify-end">
            {/* Quick Kanji pills */}
            <div className="flex items-center gap-1 bg-white p-1 rounded-xl border border-gray-200 text-xs shadow-2xs">
              <span className="text-[11px] font-semibold text-gray-400 px-1">Chữ mẫu:</span>
              {Object.keys(PRESET_KANJI).map((char) => (
                <button
                  key={char}
                  type="button"
                  onClick={() => handleSelectChar(char)}
                  className={`px-2.5 py-1 rounded-lg font-black transition cursor-pointer text-sm ${
                    currentChar === char
                      ? 'bg-indigo-600 text-white shadow-xs'
                      : 'text-gray-700 hover:bg-indigo-50 hover:text-indigo-600'
                  }`}
                >
                  {char}
                </button>
              ))}
            </div>

            <button
              type="button"
              onClick={onClose}
              className="p-2 text-gray-400 hover:text-gray-700 hover:bg-gray-200/60 rounded-xl transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Thanh công cụ: Ô nhập ký tự bất kỳ & Chọn theo cấp độ JLPT */}
        <div className="px-6 py-2.5 bg-slate-50/80 border-b border-gray-100 flex flex-col gap-2">
          {/* Ô nhập bất kỳ ký tự nào */}
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={customInputChar}
                onChange={(e) => setCustomInputChar(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') loadAnyCharacter(customInputChar)
                }}
                placeholder="Gõ hoặc dán bất kỳ chữ Kanji / Kana nào (ví dụ: 学, 桜, 愛, 山, 川, あ, ア)..."
                className="w-full pl-8 pr-3 py-1.5 bg-white border border-gray-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-indigo-500 shadow-2xs"
              />
              <Search className="w-3.5 h-3.5 text-gray-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
            </div>
            <button
              type="button"
              onClick={() => loadAnyCharacter(customInputChar)}
              disabled={!customInputChar.trim() || isLoadingChar}
              className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 cursor-pointer disabled:opacity-40 shrink-0 shadow-xs"
            >
              {isLoadingChar ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
              )}
              <span>Tải chữ</span>
            </button>
          </div>

          {/* Dải chọn nhanh theo cấp độ JLPT / Kana */}
          <div className="flex items-center gap-2 overflow-x-auto py-0.5 text-xs">
            <div className="flex items-center gap-1 shrink-0 font-bold text-gray-500">
              {['N5', 'N4', 'N3', 'N2', 'N1', 'KANA'].map((lvl) => (
                <button
                  key={lvl}
                  type="button"
                  onClick={() => setSelectedLevel(lvl)}
                  className={`px-2 py-0.5 rounded-lg text-[11px] font-bold transition cursor-pointer ${
                    selectedLevel === lvl
                      ? 'bg-amber-500 text-white shadow-2xs'
                      : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {lvl}
                </button>
              ))}
            </div>

            <div className="h-4 w-px bg-gray-200 shrink-0" />

            <div className="flex items-center gap-1 overflow-x-auto py-0.5">
              {levelChars.slice(0, 40).map((char) => (
                <button
                  key={char}
                  type="button"
                  onClick={() => loadAnyCharacter(char)}
                  className={`w-7 h-7 rounded-lg font-serif font-black flex items-center justify-center transition cursor-pointer shrink-0 text-sm ${
                    currentChar === char
                      ? 'bg-indigo-600 text-white shadow-xs scale-105'
                      : 'bg-white border border-gray-200/90 text-gray-800 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-300'
                  }`}
                >
                  {char}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Thân Modal: Khung vẽ & Bảng kết quả */}
        <div className="p-6 overflow-y-auto flex-1 grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Cột trái: Khung Canvas tương tác (7 cột) */}
          <div className="lg:col-span-7 flex flex-col items-center">
            {/* Thanh trạng thái nét & Nút chuyển nét gợi ý */}
            <div className="w-full flex items-center justify-between mb-3 px-1 text-xs font-medium">
              <span className="text-gray-600">
                Số nét đã vẽ:{' '}
                <strong
                  className={`font-black text-sm ${
                    userStrokes.length === expectedStrokes
                      ? 'text-emerald-600'
                      : userStrokes.length > expectedStrokes
                      ? 'text-rose-600'
                      : 'text-indigo-600'
                  }`}
                >
                  {userStrokes.length}
                </strong>{' '}
                / {expectedStrokes} nét
              </span>

              <button
                type="button"
                onClick={() => setShowGhostGuide(!showGhostGuide)}
                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border transition ${
                  showGhostGuide
                    ? 'bg-indigo-50 border-indigo-200 text-indigo-700 font-semibold'
                    : 'bg-gray-50 border-gray-200 text-gray-500 hover:bg-gray-100'
                }`}
              >
                {showGhostGuide ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                <span>{showGhostGuide ? 'Hiện nét mờ' : 'Ẩn nét mờ'}</span>
              </button>
            </div>

            {/* Container Canvas + Lớp nét gợi ý SVG */}
            <div
              className="relative bg-white rounded-3xl border-2 border-indigo-200/80 shadow-md overflow-hidden select-none touch-none flex items-center justify-center"
              style={{ width: CANVAS_SIZE, height: CANVAS_SIZE }}
            >
              {/* Nét mờ KanjiVG làm mẫu nền (Ghost guide) */}
              {showGhostGuide && activeStrokes && activeStrokes.length > 0 && (
                <svg
                  viewBox="0 0 109 109"
                  className="absolute inset-0 w-full h-full pointer-events-none z-0"
                  style={{ strokeLinecap: 'round', strokeLinejoin: 'round' }}
                >
                  {activeStrokes.map((s, idx) => (
                    <path
                      key={idx}
                      d={s.svgPathData}
                      fill="none"
                      stroke="#818cf8"
                      strokeWidth="6"
                      opacity="0.45"
                    />
                  ))}
                </svg>
              )}

              {/* Canvas viết tay */}
              <canvas
                ref={canvasRef}
                width={CANVAS_SIZE}
                height={CANVAS_SIZE}
                className="w-full h-full cursor-crosshair relative z-10"
                onMouseDown={handleStart}
                onMouseMove={handleMove}
                onMouseUp={handleEnd}
                onMouseLeave={handleEnd}
                onTouchStart={handleStart}
                onTouchMove={handleMove}
                onTouchEnd={handleEnd}
              />
            </div>

            {/* Thanh công cụ vẽ: Xóa nét, Xóa hết, Chấm điểm */}
            <div className="flex items-center gap-2 mt-4 w-full justify-center">
              <button
                type="button"
                onClick={handleUndo}
                disabled={userStrokes.length === 0}
                className="px-3.5 py-2.5 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-gray-700 text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-40 cursor-pointer shadow-2xs"
              >
                <RotateCcw className="w-4 h-4 text-gray-500" />
                <span>Hoàn tác nét</span>
              </button>

              <button
                type="button"
                onClick={handleClear}
                disabled={userStrokes.length === 0}
                className="px-3.5 py-2.5 rounded-xl border border-gray-200 bg-white hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 text-gray-700 text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-40 cursor-pointer shadow-2xs"
              >
                <Trash2 className="w-4 h-4 text-gray-400" />
                <span>Viết lại</span>
              </button>

              <button
                type="button"
                onClick={handleEvaluate}
                disabled={userStrokes.length === 0 || isEvaluating}
                className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white text-xs font-bold flex items-center gap-2 transition shadow-sm disabled:opacity-50 cursor-pointer"
              >
                {isEvaluating ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang chấm điểm...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4 text-amber-300" />
                    <span>Chấm điểm nét</span>
                  </>
                )}
              </button>
            </div>

            {errorMessage && (
              <div className="mt-3 p-3 text-xs bg-rose-50 border border-rose-200 text-rose-700 rounded-xl w-full text-center">
                {errorMessage}
              </div>
            )}
          </div>

          {/* Cột phải: Bảng kết quả chấm điểm & Nhận xét sư phạm (5 cột) */}
          <div className="lg:col-span-5 flex flex-col space-y-4">
            {assessment ? (
              <div className="bg-slate-50/80 rounded-2xl p-5 border border-gray-200 flex flex-col space-y-4 h-full">
                {/* Thẻ điểm tổng thể */}
                <div
                  className={`p-4 rounded-2xl border text-center transition ${
                    assessment.overall_score >= 80
                      ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                      : assessment.overall_score >= 50
                      ? 'bg-amber-50 border-amber-200 text-amber-900'
                      : 'bg-rose-50 border-rose-200 text-rose-900'
                  }`}
                >
                  <div className="flex items-center justify-center gap-2 mb-1">
                    <Award
                      className={`w-6 h-6 ${
                        assessment.overall_score >= 80
                          ? 'text-emerald-600'
                          : assessment.overall_score >= 50
                          ? 'text-amber-600'
                          : 'text-rose-600'
                      }`}
                    />
                    <span className="text-3xl font-black">{assessment.overall_score}</span>
                    <span className="text-xs font-bold text-gray-500">/ 100 điểm</span>
                  </div>

                  <p className="text-xs font-medium mt-1">{assessment.feedback}</p>
                </div>

                {/* Danh sách phân tích từng nét */}
                <div className="space-y-2 flex-1 overflow-y-auto max-h-72 pr-1">
                  <span className="text-xs font-bold text-gray-700 uppercase tracking-wider block">
                    Chi tiết từng nét ({assessment.stroke_details.length} nét):
                  </span>

                  {assessment.stroke_details.map((detail) => (
                    <div
                      key={detail.stroke_number}
                      className={`p-3 rounded-xl border text-xs space-y-1 transition ${
                        detail.order_correct && detail.is_direction_correct
                          ? 'bg-white border-emerald-200'
                          : detail.is_reversed
                          ? 'bg-rose-50/70 border-rose-200'
                          : 'bg-amber-50/70 border-amber-200'
                      }`}
                    >
                      <div className="flex items-center justify-between font-bold">
                        <span className="flex items-center gap-1.5">
                          {detail.order_correct && detail.is_direction_correct ? (
                            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                          ) : (
                            <AlertTriangle
                              className={`w-4 h-4 shrink-0 ${
                                detail.is_reversed ? 'text-rose-600' : 'text-amber-600'
                              }`}
                            />
                          )}
                          <span>Nét {detail.stroke_number}</span>
                        </span>

                        <span
                          className={`px-2 py-0.5 rounded text-[10px] ${
                            detail.order_correct && detail.is_direction_correct
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-rose-100 text-rose-800'
                          }`}
                        >
                          {detail.is_reversed
                            ? 'Ngược chiều'
                            : !detail.order_correct
                            ? 'Sai thứ tự'
                            : 'Chuẩn'}
                        </span>
                      </div>

                      <p className="text-gray-600 leading-relaxed">{detail.feedback}</p>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              /* Trạng thái ban đầu: Hướng dẫn luyện viết */
              <div className="bg-slate-50/70 rounded-2xl p-5 border border-dashed border-gray-300 flex flex-col justify-center items-center text-center space-y-3 h-full min-h-[280px]">
                <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
                  <HelpCircle className="w-6 h-6" />
                </div>
                <div>
                  <h4 className="text-sm font-bold text-gray-800 mb-1">Quy tắc luyện viết Kanji</h4>
                  <p className="text-xs text-gray-500 max-w-xs leading-relaxed">
                    Vẽ từng nét theo đúng thứ tự (từ trên xuống dưới, từ trái sang phải, ngang trước sổ sau).
                  </p>
                </div>
                <div className="text-left w-full max-w-xs space-y-1.5 text-xs text-gray-600 bg-white p-3 rounded-xl border border-gray-200/80">
                  <div className="flex items-center gap-2">
                    <ChevronRight className="w-3.5 h-3.5 text-indigo-500" />
                    <span>Nét ngang: viết từ trái sang phải</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <ChevronRight className="w-3.5 h-3.5 text-indigo-500" />
                    <span>Nét sổ: viết từ trên xuống dưới</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <ChevronRight className="w-3.5 h-3.5 text-indigo-500" />
                    <span>Ngoài trước trong sau, vào trước đóng sau</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
