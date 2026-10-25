import React, { useEffect, useRef, useState, useCallback } from 'react'
import {
  X,
  Camera as CameraIcon,
  VideoOff,
  Sparkles,
  RefreshCw,
  CheckCircle2,
  RotateCcw,
  Trash2,
  PenTool,
  Brain,
  Loader2,
  Layers,
  ArrowRight,
  Search,
  Volume2,
  VolumeX,
  Delete,
  Plus,
  MousePointer,
} from 'lucide-react'
import { aiService, type PredictionItem } from '@/services'

export interface Point {
  x: number
  y: number
}

export interface NormalizedLandmark {
  x: number
  y: number
  z: number
  visibility?: number
}

export interface Results {
  image: HTMLVideoElement | HTMLCanvasElement | HTMLImageElement
  multiHandLandmarks?: NormalizedLandmark[][]
  multiHandWorldLandmarks?: NormalizedLandmark[][]
  multiHandedness?: Array<{ index: number; score: number; label: string; displayName?: string }>
}

// Cặp chỉ số các khớp nối xương bàn tay chuẩn của MediaPipe
const DEFAULT_HAND_CONNECTIONS: Array<[number, number]> = [
  [0, 1], [1, 2], [2, 3], [3, 4],       // Ngón cái (Thumb)
  [0, 5], [5, 6], [6, 7], [7, 8],       // Ngón trỏ (Index)
  [5, 9], [9, 10], [10, 11], [11, 12],  // Ngón giữa (Middle)
  [9, 13], [13, 14], [14, 15], [15, 16],// Ngón áp út (Ring)
  [13, 17], [0, 17], [17, 18], [18, 19], [19, 20] // Ngón út (Pinky)
]

export type AirGestureType = 'NONE' | 'PINCH' | 'PEACE_UNDO' | 'PALM_CLEAR' | 'THUMBS_SEARCH'

// Helper tính khoảng cách Euclidean giữa 2 landmarks
const getDistance = (p1: NormalizedLandmark, p2: NormalizedLandmark) =>
  Math.hypot(p1.x - p2.x, p1.y - p2.y)

// Kiểm tra 1 ngón tay có đang duỗi thẳng hay không
const isFingerExtended = (
  tip: NormalizedLandmark,
  pip: NormalizedLandmark,
  _mcp: NormalizedLandmark,
  wrist: NormalizedLandmark
) => {
  const distTipWrist = getDistance(tip, wrist)
  const distPipWrist = getDistance(pip, wrist)
  return distTipWrist > distPipWrist && tip.y < pip.y + 0.04
}

// Nhận diện cử chỉ tay tĩnh chuẩn xác từ 21 điểm mốc
const detectStaticGesture = (
  lm: NormalizedLandmark[],
  isPinching: boolean
): AirGestureType => {
  if (isPinching) return 'PINCH'

  const wrist = lm[0]
  const thumbTip = lm[4]
  const thumbMcp = lm[2]
  const indexTip = lm[8]
  const indexPip = lm[6]
  const indexMcp = lm[5]
  const middleTip = lm[12]
  const middlePip = lm[10]
  const middleMcp = lm[9]
  const ringTip = lm[16]
  const ringPip = lm[14]
  const ringMcp = lm[13]
  const pinkyTip = lm[20]
  const pinkyPip = lm[18]
  const pinkyMcp = lm[17]

  const indexExt = isFingerExtended(indexTip, indexPip, indexMcp, wrist)
  const middleExt = isFingerExtended(middleTip, middlePip, middleMcp, wrist)
  const ringExt = isFingerExtended(ringTip, ringPip, ringMcp, wrist)
  const pinkyExt = isFingerExtended(pinkyTip, pinkyPip, pinkyMcp, wrist)

  // 1. CỬ CHỈ CHỮ V ✌️ (UNDO): Ngón trỏ + ngón giữa duỗi, áp út + út gập
  if (indexExt && middleExt && !ringExt && !pinkyExt) {
    if (getDistance(indexTip, middleTip) > 0.035 && indexTip.y < wrist.y && middleTip.y < wrist.y) {
      return 'PEACE_UNDO'
    }
  }

  // 2. CỬ CHỈ XÒE BÀN TAY ✋ (CLEAR ALL): Cả 4 ngón chính đều duỗi rộng
  if (indexExt && middleExt && ringExt && pinkyExt) {
    if (getDistance(thumbTip, pinkyTip) > 0.14) {
      return 'PALM_CLEAR'
    }
  }

  // 3. CỬ CHỈ NGÓN CÁI 👍 (THUMBS UP / SEARCH): Ngón cái chỉ lên trên, 4 ngón kia gập
  if (!indexExt && !middleExt && !ringExt && !pinkyExt) {
    if (thumbTip.y < thumbMcp.y && thumbTip.y < indexMcp.y - 0.02) {
      return 'THUMBS_SEARCH'
    }
  }

  return 'NONE'
}

// Helper phát âm thanh phản hồi xúc giác qua Web Audio API (Zero-Dependency)
const playFeedbackSound = (type: 'ting' | 'swoosh' | 'pop', enabled = true) => {
  if (!enabled) return
  try {
    const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext
    if (!AudioContextClass) return
    const ctx = new AudioContextClass()

    if (type === 'ting') {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = 'sine'
      osc.frequency.setValueAtTime(880, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(1320, ctx.currentTime + 0.15)
      gain.gain.setValueAtTime(0.2, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start()
      osc.stop(ctx.currentTime + 0.35)
    } else if (type === 'swoosh') {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = 'triangle'
      osc.frequency.setValueAtTime(400, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(140, ctx.currentTime + 0.25)
      gain.gain.setValueAtTime(0.25, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.25)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start()
      osc.stop(ctx.currentTime + 0.25)
    } else if (type === 'pop') {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.type = 'sine'
      osc.frequency.setValueAtTime(520, ctx.currentTime)
      osc.frequency.exponentialRampToValueAtTime(650, ctx.currentTime + 0.08)
      gain.gain.setValueAtTime(0.18, ctx.currentTime)
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.12)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start()
      osc.stop(ctx.currentTime + 0.12)
    }
  } catch {}
}

interface KanjiAirWritingModalProps {
  isOpen: boolean
  onClose: () => void
  onSelectCharacter?: (char: string) => void
}

// Hàm tải script động từ CDN của Google
const loadScript = (src: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.crossOrigin = 'anonymous'
    script.onload = () => resolve()
    script.onerror = (err) => reject(err)
    document.head.appendChild(script)
  })
}

export const KanjiAirWritingModal: React.FC<KanjiAirWritingModalProps> = ({
  isOpen,
  onClose,
  onSelectCharacter,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const handsRef = useRef<any>(null)
  const isComponentMounted = useRef<boolean>(true)
  const animationFrameId = useRef<number | null>(null)
  const isProcessingFrame = useRef<boolean>(false)

  // Trạng thái hệ thống
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const [handDetected, setHandDetected] = useState<boolean>(false)
  const [fps, setFps] = useState<number>(0)
  const [showSkeleton, setShowSkeleton] = useState<boolean>(true)
  const [retryCount, setRetryCount] = useState<number>(0)
  const [isSimulationMode, setIsSimulationMode] = useState<boolean>(false)

  // --- QUẢN LÝ NÉT VẼ & CỬ CHỈ (PHASE 2) ---
  const strokesRef = useRef<Point[][]>([]) // Lưu các nét vẽ hoàn chỉnh
  const currentStrokeRef = useRef<Point[]>([]) // Nét đang vẽ
  const isPinchingRef = useRef<boolean>(false) // Trạng thái chụm ngón (Hysteresis)
  const lastSmoothedPointRef = useRef<Point | null>(null) // Điểm mượt cuối cùng (1-Euro / EMA)

  const [isDrawing, setIsDrawing] = useState<boolean>(false)
  const [strokesCount, setStrokesCount] = useState<number>(0)

  // --- KẾT NỐI AI NHẬN DIỆN THỜI GIAN THỰC (PHASE 3) ---
  const [predictions, setPredictions] = useState<PredictionItem[]>([])
  const [topChar, setTopChar] = useState<string | null>(null)
  const [isPredicting, setIsPredicting] = useState<boolean>(false)
  const [predictionError, setPredictionError] = useState<string | null>(null)
  const predictionTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const schedulePredictionRef = useRef<() => void>(() => {})

  // --- ĐIỀU KHIỂN KHÔNG CHẠM & GHÉP CỤM TỪ (PHASE 4) ---
  const [wordBuffer, setWordBuffer] = useState<string[]>([])
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true)
  const [gestureOverlay, setGestureOverlay] = useState<{
    type: AirGestureType
    progress: number
    label: string
  } | null>(null)
  const [hoverTargetKey, setHoverTargetKey] = useState<string | null>(null)

  // Refs phục vụ vòng lặp Animation Frame (Zero Stale Closure)
  const wordBufferRef = useRef<string[]>([])
  const predictionsRef = useRef<PredictionItem[]>([])
  const topCharRef = useRef<string | null>(null)
  const soundEnabledRef = useRef<boolean>(true)

  const gestureTypeRef = useRef<AirGestureType>('NONE')
  const gestureStartTimeRef = useRef<number | null>(null)
  const gestureCooldownRef = useRef<number>(0)

  const hoverStartTimeRef = useRef<number | null>(null)
  const hoverTargetRef = useRef<string | null>(null)
  const hoverCooldownRef = useRef<number>(0)

  // Đồng bộ state vào refs
  useEffect(() => {
    wordBufferRef.current = wordBuffer
  }, [wordBuffer])

  useEffect(() => {
    predictionsRef.current = predictions
  }, [predictions])

  useEffect(() => {
    topCharRef.current = topChar
  }, [topChar])

  useEffect(() => {
    soundEnabledRef.current = soundEnabled
  }, [soundEnabled])

  // Đo FPS
  const frameCountRef = useRef<number>(0)
  const lastTimeRef = useRef<number>(performance.now())

  // 3.1. Offscreen Canvas chuyển đổi mảng nét vẽ thành ảnh Base64 và gọi AI Model
  const predictStrokes = useCallback(async () => {
    if (strokesRef.current.length === 0) {
      setPredictions([])
      setTopChar(null)
      return
    }

    const canvas = canvasRef.current
    const width = canvas?.width || 640
    const height = canvas?.height || 480

    try {
      setIsPredicting(true)
      setPredictionError(null)

      // Tạo Canvas ảo với nền đen, nét vẽ trắng độ dày 14px chuẩn cho mô hình AI CNN
      const offscreen = document.createElement('canvas')
      offscreen.width = width
      offscreen.height = height
      const offCtx = offscreen.getContext('2d')
      if (!offCtx) return

      offCtx.fillStyle = '#000000'
      offCtx.fillRect(0, 0, width, height)

      offCtx.strokeStyle = '#ffffff'
      offCtx.lineWidth = 14
      offCtx.lineCap = 'round'
      offCtx.lineJoin = 'round'

      for (const stroke of strokesRef.current) {
        if (stroke.length < 2) continue
        offCtx.beginPath()
        offCtx.moveTo(stroke[0].x, stroke[0].y)
        for (let i = 1; i < stroke.length; i++) {
          offCtx.lineTo(stroke[i].x, stroke[i].y)
        }
        offCtx.stroke()
      }

      const base64Data = offscreen.toDataURL('image/png')
      const response = await aiService.predictBase64(base64Data, 5)

      if (response.success && response.predictions && response.predictions.length > 0) {
        setPredictions(response.predictions)
        setTopChar(response.top_char || response.predictions[0].char)
      } else {
        setPredictions([])
        setTopChar(null)
        if (response.message) {
          setPredictionError(response.message)
        }
      }
    } catch (err: any) {
      console.error('Lỗi khi AI nhận diện nét vẽ:', err)
      setPredictionError('Không thể kết nối máy chủ AI (Port 8000)')
    } finally {
      setIsPredicting(false)
    }
  }, [])

  // 3.2. Debounce 150ms sau khi nhấc bút (PEN_UP) để dự đoán mượt mà
  const schedulePrediction = useCallback(() => {
    if (predictionTimerRef.current) {
      clearTimeout(predictionTimerRef.current)
    }
    predictionTimerRef.current = setTimeout(() => {
      predictStrokes()
    }, 150)
  }, [predictStrokes])

  useEffect(() => {
    schedulePredictionRef.current = schedulePrediction
  }, [schedulePrediction])

  // Vẽ Canvas trong chế độ chuột mô phỏng (khi không có camera)
  const redrawSimulationCanvas = useCallback((cursorPoint: Point | null = null) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    if (canvas.width !== 640 || canvas.height !== 480) {
      canvas.width = 640
      canvas.height = 480
    }

    ctx.save()
    ctx.fillStyle = '#0b0f19'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    // Khung ô vuông thư pháp
    ctx.strokeStyle = 'rgba(51, 65, 85, 0.4)'
    ctx.lineWidth = 1
    ctx.strokeRect(40, 20, 560, 440)

    ctx.setLineDash([6, 6])
    ctx.beginPath()
    ctx.moveTo(canvas.width / 2, 20)
    ctx.lineTo(canvas.width / 2, 460)
    ctx.moveTo(40, canvas.height / 2)
    ctx.lineTo(600, canvas.height / 2)
    ctx.stroke()
    ctx.setLineDash([])

    // Hàm vẽ nét đa tầng neon
    const drawStroke = (stroke: Point[], isCurrent: boolean) => {
      if (stroke.length < 2) return
      // Outer glow
      ctx.strokeStyle = isCurrent ? 'rgba(56, 189, 248, 0.45)' : 'rgba(99, 102, 241, 0.4)'
      ctx.lineWidth = isCurrent ? 14 : 12
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.beginPath()
      ctx.moveTo(stroke[0].x, stroke[0].y)
      for (let i = 1; i < stroke.length; i++) ctx.lineTo(stroke[i].x, stroke[i].y)
      ctx.stroke()

      // Core neon
      ctx.strokeStyle = isCurrent ? '#38bdf8' : '#818cf8'
      ctx.lineWidth = isCurrent ? 8 : 7
      ctx.beginPath()
      ctx.moveTo(stroke[0].x, stroke[0].y)
      for (let i = 1; i < stroke.length; i++) ctx.lineTo(stroke[i].x, stroke[i].y)
      ctx.stroke()

      // Inner white
      ctx.strokeStyle = '#ffffff'
      ctx.lineWidth = 2.5
      ctx.beginPath()
      ctx.moveTo(stroke[0].x, stroke[0].y)
      for (let i = 1; i < stroke.length; i++) ctx.lineTo(stroke[i].x, stroke[i].y)
      ctx.stroke()
    }

    for (const s of strokesRef.current) drawStroke(s, false)
    if (currentStrokeRef.current.length > 0) drawStroke(currentStrokeRef.current, true)

    if (cursorPoint) {
      const activeColor = isPinchingRef.current ? '#38bdf8' : '#22c55e'
      ctx.fillStyle = activeColor
      ctx.beginPath()
      ctx.arc(cursorPoint.x, cursorPoint.y, isPinchingRef.current ? 8 : 6, 0, 2 * Math.PI)
      ctx.fill()
    }
    ctx.restore()
  }, [])

  // Hàm hoàn tác nét cuối (Undo)
  const handleUndo = useCallback(() => {
    if (strokesRef.current.length > 0) {
      strokesRef.current.pop()
      setStrokesCount(strokesRef.current.length)
      if (isSimulationMode) redrawSimulationCanvas()
      if (strokesRef.current.length > 0) {
        schedulePredictionRef.current()
      } else {
        if (predictionTimerRef.current) {
          clearTimeout(predictionTimerRef.current)
          predictionTimerRef.current = null
        }
        setPredictions([])
        setTopChar(null)
        setPredictionError(null)
      }
    }
  }, [isSimulationMode, redrawSimulationCanvas])

  // Hàm xóa sạch toàn bộ nét vẽ (Clear All)
  const handleClearAll = useCallback(() => {
    if (predictionTimerRef.current) {
      clearTimeout(predictionTimerRef.current)
      predictionTimerRef.current = null
    }
    strokesRef.current = []
    currentStrokeRef.current = []
    lastSmoothedPointRef.current = null
    isPinchingRef.current = false
    setIsDrawing(false)
    setStrokesCount(0)
    setPredictions([])
    setTopChar(null)
    setPredictionError(null)
    if (isSimulationMode) redrawSimulationCanvas()
  }, [isSimulationMode, redrawSimulationCanvas])

  // Thao tác vẽ chuột trong chế độ mô phỏng
  const handleCanvasMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isSimulationMode) return
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const x = ((e.clientX - rect.left) / rect.width) * canvas.width
    const y = ((e.clientY - rect.top) / rect.height) * canvas.height
    isPinchingRef.current = true
    setIsDrawing(true)
    currentStrokeRef.current = [{ x, y }]
    redrawSimulationCanvas({ x, y })
  }

  const handleCanvasMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isSimulationMode) return
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const x = ((e.clientX - rect.left) / rect.width) * canvas.width
    const y = ((e.clientY - rect.top) / rect.height) * canvas.height
    if (isPinchingRef.current) {
      currentStrokeRef.current.push({ x, y })
    }
    redrawSimulationCanvas({ x, y })
  }

  const handleCanvasMouseUp = () => {
    if (!isSimulationMode) return
    if (isPinchingRef.current) {
      isPinchingRef.current = false
      setIsDrawing(false)
      if (currentStrokeRef.current.length >= 2) {
        strokesRef.current.push([...currentStrokeRef.current])
        setStrokesCount(strokesRef.current.length)
        schedulePredictionRef.current()
      }
      currentStrokeRef.current = []
      redrawSimulationCanvas(null)
    }
  }

  const startSimulationMode = () => {
    setIsSimulationMode(true)
    setCameraError(null)
    setIsLoading(false)
    setHandDetected(true)
    setTimeout(() => redrawSimulationCanvas(null), 50)
  }

  // 4.4. Thêm ký tự vào cụm từ ghép (Word Building) & dọn sạch bảng vẽ cho ký tự kế tiếp
  const handleAppendToWordBuffer = useCallback(
    (char: string) => {
      playFeedbackSound('ting', soundEnabledRef.current)
      setWordBuffer((prev) => [...prev, char])
      handleClearAll()
    },
    [handleClearAll]
  )

  // Xóa ký tự cuối khỏi cụm từ đang ghép (Backspace)
  const handleRemoveLastChar = useCallback(() => {
    playFeedbackSound('pop', soundEnabledRef.current)
    setWordBuffer((prev) => prev.slice(0, -1))
  }, [])

  // Xóa sạch toàn bộ cụm từ đang ghép
  const handleClearWordBuffer = useCallback(() => {
    playFeedbackSound('swoosh', soundEnabledRef.current)
    setWordBuffer([])
  }, [])

  // 4.5. Tra cứu từ điển: Tra cụm từ đang ghép hoặc ký tự hiện tại
  const handleSearchFinal = useCallback(
    (specificTerm?: string) => {
      playFeedbackSound('ting', soundEnabledRef.current)
      const term =
        specificTerm ||
        (wordBufferRef.current.length > 0
          ? wordBufferRef.current.join('')
          : topCharRef.current || (predictionsRef.current[0] ? predictionsRef.current[0].char : ''))

      if (term && onSelectCharacter) {
        onSelectCharacter(term)
      }
      onClose()
    },
    [onSelectCharacter, onClose]
  )

  // Đăng ký ref để loop animation frame luôn gọi bản mới nhất mà không gây re-render
  const handleUndoRef = useRef(handleUndo)
  const handleClearAllRef = useRef(handleClearAll)
  const handleAppendToWordBufferRef = useRef(handleAppendToWordBuffer)
  const handleSearchFinalRef = useRef(handleSearchFinal)

  useEffect(() => {
    handleUndoRef.current = handleUndo
  }, [handleUndo])

  useEffect(() => {
    handleClearAllRef.current = handleClearAll
  }, [handleClearAll])

  useEffect(() => {
    handleAppendToWordBufferRef.current = handleAppendToWordBuffer
  }, [handleAppendToWordBuffer])

  useEffect(() => {
    handleSearchFinalRef.current = handleSearchFinal
  }, [handleSearchFinal])

  // Xử lý khi nhận kết quả từ MediaPipe Hands
  const onResults = useCallback(
    (results: Results) => {
      const canvas = canvasRef.current
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      if (!ctx) return

      // Cập nhật FPS
      frameCountRef.current += 1
      const now = performance.now()
      if (now - lastTimeRef.current >= 1000) {
        setFps(Math.round((frameCountRef.current * 1000) / (now - lastTimeRef.current)))
        frameCountRef.current = 0
        lastTimeRef.current = now
      }

      // Đảm bảo canvas cùng độ phân giải với video đầu vào
      if (results.image && (canvas.width !== results.image.width || canvas.height !== results.image.height)) {
        canvas.width = results.image.width
        canvas.height = results.image.height
      }

      ctx.save()
      ctx.clearRect(0, 0, canvas.width, canvas.height)

      // 1. Vẽ khung hình Video lật gương (Mirrored) để tự nhiên như soi gương
      if (results.image) {
        ctx.scale(-1, 1)
        ctx.drawImage(results.image, -canvas.width, 0, canvas.width, canvas.height)
        ctx.restore()
      }

      // Lớp phủ tối nhẹ để làm nổi bật nét vẽ dạ quang
      ctx.fillStyle = 'rgba(15, 23, 42, 0.35)'
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      // 2. VẼ TẤT CẢ CÁC NÉT ĐÃ HOÀN THÀNH (Strokes)
      const drawStroke = (stroke: Point[], isCurrent: boolean) => {
        if (stroke.length < 2) return

        // Lớp hào quang ngoài (Outer Glow)
        ctx.strokeStyle = isCurrent ? 'rgba(56, 189, 248, 0.45)' : 'rgba(99, 102, 241, 0.4)'
        ctx.lineWidth = isCurrent ? 14 : 12
        ctx.lineCap = 'round'
        ctx.lineJoin = 'round'
        ctx.beginPath()
        ctx.moveTo(stroke[0].x, stroke[0].y)
        for (let i = 1; i < stroke.length; i++) {
          ctx.lineTo(stroke[i].x, stroke[i].y)
        }
        ctx.stroke()

        // Lớp mực chính (Core Neon Stroke)
        ctx.strokeStyle = isCurrent ? '#38bdf8' : '#818cf8' // Cyan khi đang vẽ, Indigo cho nét cũ
        ctx.lineWidth = isCurrent ? 8 : 7
        ctx.beginPath()
        ctx.moveTo(stroke[0].x, stroke[0].y)
        for (let i = 1; i < stroke.length; i++) {
          ctx.lineTo(stroke[i].x, stroke[i].y)
        }
        ctx.stroke()

        // Lớp tâm trắng (Inner White Core tạo nét bút lông calligraphy)
        ctx.strokeStyle = '#ffffff'
        ctx.lineWidth = 2.5
        ctx.beginPath()
        ctx.moveTo(stroke[0].x, stroke[0].y)
        for (let i = 1; i < stroke.length; i++) {
          ctx.lineTo(stroke[i].x, stroke[i].y)
        }
        ctx.stroke()
      }

      // Vẽ các nét đã xong
      for (const stroke of strokesRef.current) {
        drawStroke(stroke, false)
      }

      // Vẽ nét hiện tại đang vẽ
      if (currentStrokeRef.current.length > 0) {
        drawStroke(currentStrokeRef.current, true)
      }

      // 3. XỬ LÝ NHẬN DIỆN BÀN TAY & CỬ CHỈ CHỤM NGÓN (PINCH)
      if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
        setHandDetected(true)
        const landmarks = results.multiHandLandmarks[0]

        // 3.1. Vẽ khung xương bàn tay (Skeleton) nếu được bật
        if (showSkeleton) {
          ctx.strokeStyle = 'rgba(99, 102, 241, 0.3)'
          ctx.lineWidth = 1.5
          for (const [start, end] of DEFAULT_HAND_CONNECTIONS) {
            const p1 = landmarks[start]
            const p2 = landmarks[end]
            if (!p1 || !p2) continue
            const x1 = (1 - p1.x) * canvas.width
            const y1 = p1.y * canvas.height
            const x2 = (1 - p2.x) * canvas.width
            const y2 = p2.y * canvas.height

            ctx.beginPath()
            ctx.moveTo(x1, y1)
            ctx.lineTo(x2, y2)
            ctx.stroke()
          }

          // Vẽ các khớp nhỏ
          for (const lm of landmarks) {
            const x = (1 - lm.x) * canvas.width
            const y = lm.y * canvas.height
            ctx.fillStyle = 'rgba(199, 210, 254, 0.5)'
            ctx.beginPath()
            ctx.arc(x, y, 2.5, 0, 2 * Math.PI)
            ctx.fill()
          }
        }

        // 3.2. Lấy tọa độ đầu ngón trỏ (8) và đầu ngón cái (4)
        const indexTip = landmarks[8]
        const thumbTip = landmarks[4]

        if (indexTip && thumbTip) {
          const rawIndexX = (1 - indexTip.x) * canvas.width
          const rawIndexY = indexTip.y * canvas.height
          const rawThumbX = (1 - thumbTip.x) * canvas.width
          const rawThumbY = thumbTip.y * canvas.height

          // Tính khoảng cách Euclidean giữa đầu ngón cái và ngón trỏ
          const pinchDistance = Math.hypot(rawIndexX - rawThumbX, rawIndexY - rawThumbY)

          // 3.3. ÁP DỤNG BỘ LỌC CHỐNG RUNG TAY (Exponential Moving Average - EMA)
          // Alpha = 0.65: phản hồi tức thì và triệt tiêu hoàn toàn rung cơ học
          let smoothedX = rawIndexX
          let smoothedY = rawIndexY
          if (lastSmoothedPointRef.current) {
            const alpha = 0.65
            smoothedX = alpha * rawIndexX + (1 - alpha) * lastSmoothedPointRef.current.x
            smoothedY = alpha * rawIndexY + (1 - alpha) * lastSmoothedPointRef.current.y
          }
          const smoothedPoint: Point = { x: smoothedX, y: smoothedY }
          lastSmoothedPointRef.current = smoothedPoint

          // 3.4. THUẬT TOÁN HYSTERESIS PHÂN ĐỊNH PEN_DOWN VÀ PEN_UP
          // Ngưỡng chụm ngón: < 40px bắt đầu vẽ; > 52px nhấc bút kết thúc nét
          const PINCH_THRESHOLD_DOWN = 40
          const PINCH_THRESHOLD_UP = 52

          if (!isPinchingRef.current && pinchDistance < PINCH_THRESHOLD_DOWN) {
            // BẮT ĐẦU VẼ NÉT MỚI (Pen Down)
            isPinchingRef.current = true
            setIsDrawing(true)
            currentStrokeRef.current = [smoothedPoint]
          } else if (isPinchingRef.current && pinchDistance > PINCH_THRESHOLD_UP) {
            // KẾT THÚC 1 NÉT VẼ (Pen Up)
            isPinchingRef.current = false
            setIsDrawing(false)
            if (currentStrokeRef.current.length >= 2) {
              strokesRef.current.push([...currentStrokeRef.current])
              setStrokesCount(strokesRef.current.length)
              schedulePredictionRef.current()
            }
            currentStrokeRef.current = []
          } else if (isPinchingRef.current) {
            // ĐANG TRONG QUÁ TRÌNH VẼ NÉT
            const lastPt = currentStrokeRef.current[currentStrokeRef.current.length - 1]
            // Chỉ thêm điểm nếu di chuyển tối thiểu 1.5px để tránh trùng điểm gây gợn nét
            if (!lastPt || Math.hypot(smoothedX - lastPt.x, smoothedY - lastPt.y) >= 1.5) {
              currentStrokeRef.current.push(smoothedPoint)
            }
          }

          // 3.5. ĐƯỜNG NỐI ĐO KHOẢNG CÁCH CHỤM NGÓN (Visual Distance Gauge)
          if (pinchDistance < 90) {
            ctx.strokeStyle = isPinchingRef.current
              ? 'rgba(56, 189, 248, 0.9)'
              : 'rgba(255, 255, 255, 0.4)'
            ctx.lineWidth = isPinchingRef.current ? 2.5 : 1
            ctx.setLineDash(isPinchingRef.current ? [] : [4, 4])
            ctx.beginPath()
            ctx.moveTo(rawIndexX, rawIndexY)
            ctx.lineTo(rawThumbX, rawThumbY)
            ctx.stroke()
            ctx.setLineDash([])
          }

          // 3.6. RENDER CON TRỎ LASER ẢO TẠI ĐẦU NGÓN TRỎ
          const pointerX = smoothedX
          const pointerY = smoothedY
          const activeColor = isPinchingRef.current ? '#38bdf8' : '#22c55e'
          const glowColor = isPinchingRef.current ? 'rgba(56, 189, 248, ' : 'rgba(34, 197, 94, '

          // Hào quang tỏa sáng (Outer Glow)
          const rad = isPinchingRef.current ? 24 : 18
          const gradient = ctx.createRadialGradient(pointerX, pointerY, 2, pointerX, pointerY, rad)
          gradient.addColorStop(0, glowColor + '0.85)')
          gradient.addColorStop(0.5, glowColor + '0.3)')
          gradient.addColorStop(1, glowColor + '0)')

          ctx.fillStyle = gradient
          ctx.beginPath()
          ctx.arc(pointerX, pointerY, rad, 0, 2 * Math.PI)
          ctx.fill()

          // Vòng tròn trung tâm
          ctx.strokeStyle = '#ffffff'
          ctx.lineWidth = 2.5
          ctx.beginPath()
          ctx.arc(pointerX, pointerY, isPinchingRef.current ? 9 : 7, 0, 2 * Math.PI)
          ctx.stroke()

          // Điểm tâm phát sáng
          ctx.fillStyle = activeColor
          ctx.beginPath()
          ctx.arc(pointerX, pointerY, 4, 0, 2 * Math.PI)
          ctx.fill()

          // Tag nhãn trạng thái bay theo ngón tay
          ctx.font = 'bold 11px system-ui, -apple-system, sans-serif'
          const statusText = isPinchingRef.current ? '✍️ ĐANG VẼ NÉT' : 'Di chuyển'
          const textWidth = ctx.measureText(statusText).width
          ctx.fillStyle = isPinchingRef.current ? 'rgba(14, 116, 144, 0.85)' : 'rgba(15, 23, 42, 0.75)'
          ctx.beginPath()
          ctx.roundRect(pointerX - textWidth / 2 - 8, pointerY - 32, textWidth + 16, 20, 6)
          ctx.fill()

          ctx.fillStyle = '#ffffff'
          ctx.textAlign = 'center'
          ctx.fillText(statusText, pointerX, pointerY - 18)

          // =========================================================================
          // 4. CỬ CHỈ ĐIỀU KHIỂN KHÔNG CHẠM & AIR HOVER / DWELL CLICK (PHASE 4)
          // =========================================================================
          const now = performance.now()

          // 4.1. VẼ CÁC NÚT TƯƠNG TÁC KHÔNG CHẠM TRÊN CANVAS (Touchless HUD)
          // Nút Undo góc trên trái
          const undoBtnRect = { x: 16, y: 16, w: 100, h: 32 }
          ctx.fillStyle =
            hoverTargetRef.current === 'CANVAS_UNDO'
              ? 'rgba(56, 189, 248, 0.35)'
              : 'rgba(30, 41, 59, 0.7)'
          ctx.strokeStyle =
            hoverTargetRef.current === 'CANVAS_UNDO' ? '#38bdf8' : 'rgba(148, 163, 184, 0.4)'
          ctx.lineWidth = hoverTargetRef.current === 'CANVAS_UNDO' ? 2 : 1
          ctx.beginPath()
          ctx.roundRect(undoBtnRect.x, undoBtnRect.y, undoBtnRect.w, undoBtnRect.h, 10)
          ctx.fill()
          ctx.stroke()
          ctx.fillStyle = '#f8fafc'
          ctx.font = 'bold 11px system-ui, sans-serif'
          ctx.textAlign = 'center'
          ctx.fillText('✌️ Hoàn tác', undoBtnRect.x + undoBtnRect.w / 2, undoBtnRect.y + 20)

          // Nút Clear All góc trên trái
          const clearBtnRect = { x: 124, y: 16, w: 100, h: 32 }
          ctx.fillStyle =
            hoverTargetRef.current === 'CANVAS_CLEAR'
              ? 'rgba(244, 63, 94, 0.35)'
              : 'rgba(30, 41, 59, 0.7)'
          ctx.strokeStyle =
            hoverTargetRef.current === 'CANVAS_CLEAR' ? '#f43f5e' : 'rgba(148, 163, 184, 0.4)'
          ctx.lineWidth = hoverTargetRef.current === 'CANVAS_CLEAR' ? 2 : 1
          ctx.beginPath()
          ctx.roundRect(clearBtnRect.x, clearBtnRect.y, clearBtnRect.w, clearBtnRect.h, 10)
          ctx.fill()
          ctx.stroke()
          ctx.fillStyle = '#f8fafc'
          ctx.fillText('✋ Xóa bảng', clearBtnRect.x + clearBtnRect.w / 2, clearBtnRect.y + 20)

          // Vẽ Top 3 thẻ gợi ý AI trên góc trên phải Canvas (để Air Hover chọn nhanh)
          const preds = predictionsRef.current
          const candidateRects: Array<{ x: number; y: number; w: number; h: number; char: string; key: string }> = []
          if (preds && preds.length > 0) {
            const cardW = 125
            const cardH = 36
            const cardX = canvas.width - cardW - 16
            for (let i = 0; i < Math.min(3, preds.length); i++) {
              const cardY = 16 + i * 44
              const isHovered = hoverTargetRef.current === `CANVAS_CANDIDATE_${i}`
              ctx.fillStyle = isHovered
                ? 'rgba(56, 189, 248, 0.35)'
                : i === 0
                ? 'rgba(14, 116, 144, 0.65)'
                : 'rgba(30, 41, 59, 0.7)'
              ctx.strokeStyle = isHovered ? '#38bdf8' : i === 0 ? '#38bdf8' : 'rgba(148, 163, 184, 0.35)'
              ctx.lineWidth = isHovered ? 2 : 1
              ctx.beginPath()
              ctx.roundRect(cardX, cardY, cardW, cardH, 10)
              ctx.fill()
              ctx.stroke()

              // Chữ Kanji và Tỷ lệ %
              ctx.fillStyle = '#ffffff'
              ctx.font = 'bold 16px serif'
              ctx.textAlign = 'left'
              ctx.fillText(preds[i].char, cardX + 10, cardY + 24)

              ctx.font = 'bold 10px system-ui, sans-serif'
              ctx.fillStyle = isHovered ? '#38bdf8' : '#94a3b8'
              ctx.textAlign = 'right'
              ctx.fillText(`${preds[i].percentage.toFixed(0)}%`, cardX + cardW - 10, cardY + 23)

              candidateRects.push({
                x: cardX,
                y: cardY,
                w: cardW,
                h: cardH,
                char: preds[i].char,
                key: `CANVAS_CANDIDATE_${i}`,
              })
            }
          }

          // 4.2. XỬ LÝ CỬ CHỈ TAY TĨNH (✌️ Undo, ✋ Clear, 👍 Search)
          if (!isPinchingRef.current) {
            const staticGesture = detectStaticGesture(landmarks, false)

            if (now < gestureCooldownRef.current) {
              gestureTypeRef.current = 'NONE'
              gestureStartTimeRef.current = null
              setGestureOverlay(null)
            } else if (staticGesture !== 'NONE' && staticGesture !== 'PINCH') {
              if (gestureTypeRef.current !== staticGesture) {
                gestureTypeRef.current = staticGesture
                gestureStartTimeRef.current = now
              }

              const elapsed = now - (gestureStartTimeRef.current || now)
              const targetDuration =
                staticGesture === 'PEACE_UNDO' ? 600 : staticGesture === 'PALM_CLEAR' ? 1200 : 1000

              const progress = Math.min(1, elapsed / targetDuration)
              const label =
                staticGesture === 'PEACE_UNDO'
                  ? '✌️ Giữ 0.6s để Hoàn tác nét'
                  : staticGesture === 'PALM_CLEAR'
                  ? '✋ Giữ 1.2s để Xóa toàn bộ bảng'
                  : '👍 Giữ 1.0s để Tra từ điển'

              setGestureOverlay({ type: staticGesture, progress, label })

              if (progress >= 1.0) {
                if (staticGesture === 'PEACE_UNDO') {
                  handleUndoRef.current()
                  playFeedbackSound('swoosh', soundEnabledRef.current)
                  gestureCooldownRef.current = now + 900
                } else if (staticGesture === 'PALM_CLEAR') {
                  handleClearAllRef.current()
                  playFeedbackSound('swoosh', soundEnabledRef.current)
                  gestureCooldownRef.current = now + 1200
                } else if (staticGesture === 'THUMBS_SEARCH') {
                  playFeedbackSound('ting', soundEnabledRef.current)
                  handleSearchFinalRef.current()
                  gestureCooldownRef.current = now + 2000
                }
                gestureTypeRef.current = 'NONE'
                gestureStartTimeRef.current = null
                setGestureOverlay(null)
              }
            } else {
              if (gestureTypeRef.current !== 'NONE') {
                gestureTypeRef.current = 'NONE'
                gestureStartTimeRef.current = null
                setGestureOverlay(null)
              }

              // 4.3. XỬ LÝ AIR HOVER / DWELL CLICK (Dừng ngón trỏ 0.8s để chọn)
              let hoveredKey: string | null = null
              let hoveredAction: (() => void) | null = null

              // Kiểm tra hover nút Undo
              if (
                pointerX >= undoBtnRect.x &&
                pointerX <= undoBtnRect.x + undoBtnRect.w &&
                pointerY >= undoBtnRect.y &&
                pointerY <= undoBtnRect.y + undoBtnRect.h
              ) {
                hoveredKey = 'CANVAS_UNDO'
                hoveredAction = () => {
                  handleUndoRef.current()
                  playFeedbackSound('swoosh', soundEnabledRef.current)
                }
              }
              // Kiểm tra hover nút Clear
              else if (
                pointerX >= clearBtnRect.x &&
                pointerX <= clearBtnRect.x + clearBtnRect.w &&
                pointerY >= clearBtnRect.y &&
                pointerY <= clearBtnRect.y + clearBtnRect.h
              ) {
                hoveredKey = 'CANVAS_CLEAR'
                hoveredAction = () => {
                  handleClearAllRef.current()
                  playFeedbackSound('swoosh', soundEnabledRef.current)
                }
              } else {
                // Kiểm tra hover các thẻ gợi ý
                for (const c of candidateRects) {
                  if (
                    pointerX >= c.x &&
                    pointerX <= c.x + c.w &&
                    pointerY >= c.y &&
                    pointerY <= c.y + c.h
                  ) {
                    hoveredKey = c.key
                    hoveredAction = () => {
                      handleAppendToWordBufferRef.current(c.char)
                    }
                    break
                  }
                }
              }

              if (hoveredKey && hoveredAction) {
                if (hoverTargetRef.current !== hoveredKey) {
                  hoverTargetRef.current = hoveredKey
                  hoverStartTimeRef.current = now
                  setHoverTargetKey(hoveredKey)
                }

                const dwellElapsed = now - (hoverStartTimeRef.current || now)
                const dwellProgress = Math.min(1, dwellElapsed / 800) // 0.8s để kích hoạt

                // Render vòng tròn đếm ngược Dwell xung quanh con trỏ laser
                if (dwellProgress > 0) {
                  ctx.strokeStyle = '#38bdf8'
                  ctx.lineWidth = 3.5
                  ctx.beginPath()
                  ctx.arc(
                    pointerX,
                    pointerY,
                    22,
                    -Math.PI / 2,
                    -Math.PI / 2 + dwellProgress * 2 * Math.PI
                  )
                  ctx.stroke()
                }

                if (dwellProgress >= 1.0 && now >= hoverCooldownRef.current) {
                  hoveredAction()
                  hoverCooldownRef.current = now + 1200
                  hoverTargetRef.current = null
                  hoverStartTimeRef.current = null
                  setHoverTargetKey(null)
                }
              } else {
                hoverTargetRef.current = null
                hoverStartTimeRef.current = null
                setHoverTargetKey(null)
              }
            }
          } else {
            // Đang vẽ nét -> Tạm ngắt cử chỉ và Dwell
            gestureTypeRef.current = 'NONE'
            gestureStartTimeRef.current = null
            setGestureOverlay(null)
            hoverTargetRef.current = null
            hoverStartTimeRef.current = null
            setHoverTargetKey(null)
          }
        }
      } else {
        setHandDetected(false)
        lastSmoothedPointRef.current = null
        gestureTypeRef.current = 'NONE'
        gestureStartTimeRef.current = null
        setGestureOverlay(null)
        hoverTargetRef.current = null
        hoverStartTimeRef.current = null
        setHoverTargetKey(null)

        if (isPinchingRef.current) {
          isPinchingRef.current = false
          setIsDrawing(false)
          if (currentStrokeRef.current.length >= 2) {
            strokesRef.current.push([...currentStrokeRef.current])
            setStrokesCount(strokesRef.current.length)
            schedulePredictionRef.current()
          }
          currentStrokeRef.current = []
        }
      }
    },
    [showSkeleton]
  )

  // Khởi tạo MediaPipe Hands và Camera Stream (Resilient Fallback)
  useEffect(() => {
    if (!isOpen) return
    isComponentMounted.current = true

    // Nếu đang trong chế độ mô phỏng vẽ bằng chuột (fallback khi không có camera)
    if (isSimulationMode) {
      setIsLoading(false)
      setCameraError(null)
      setHandDetected(true)
      setTimeout(() => redrawSimulationCanvas(null), 50)
      return
    }

    setIsLoading(true)
    setCameraError(null)

    // Reset nét vẽ, từ ghép & gợi ý AI khi mở modal
    strokesRef.current = []
    currentStrokeRef.current = []
    lastSmoothedPointRef.current = null
    isPinchingRef.current = false
    setIsDrawing(false)
    setStrokesCount(0)
    setPredictions([])
    setTopChar(null)
    setPredictionError(null)
    setWordBuffer([])
    setGestureOverlay(null)
    setHoverTargetKey(null)
    if (predictionTimerRef.current) {
      clearTimeout(predictionTimerRef.current)
      predictionTimerRef.current = null
    }

    let handsInstance: any = null
    let localStream: MediaStream | null = null

    const initMediaPipe = async () => {
      try {
        const videoElement = videoRef.current
        if (!videoElement) return

        // 1. Tải script MediaPipe từ CDN nếu chưa có
        await loadScript('https://cdn.jsdelivr.net/npm/@mediapipe/hands/hands.js')

        const HandsClass = (window as any).Hands
        if (!HandsClass) {
          throw new Error('Không thể nạp thư viện MediaPipe từ CDN')
        }

        // 2. Khởi tạo MediaPipe Hands
        handsInstance = new HandsClass({
          locateFile: (file: string) => {
            return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`
          },
        })

        handsInstance.setOptions({
          maxNumHands: 1, // Tối ưu: chỉ theo dõi 1 bàn tay viết chữ
          modelComplexity: 1,
          minDetectionConfidence: 0.5,
          minTrackingConfidence: 0.5,
        })

        handsInstance.onResults(onResults)
        handsRef.current = handsInstance

        // 3. Khởi tạo luồng Camera linh hoạt (Hỗ trợ cả laptop webcam, USB cam, virtual cam)
        try {
          localStream = await navigator.mediaDevices.getUserMedia({
            video: {
              width: { ideal: 640 },
              height: { ideal: 480 },
              facingMode: 'user',
            },
          })
        } catch {
          // Fallback nếu máy tính không hỗ trợ ràng buộc facingMode
          localStream = await navigator.mediaDevices.getUserMedia({ video: true })
        }

        videoElement.srcObject = localStream
        await videoElement.play()

        // 4. Vòng lặp animation frame xử lý hình ảnh mượt mà (Không phụ thuộc Camera class)
        const processFrame = async () => {
          if (!isComponentMounted.current) return

          if (
            videoElement &&
            videoElement.readyState >= 2 &&
            handsRef.current &&
            !isProcessingFrame.current
          ) {
            isProcessingFrame.current = true
            try {
              await handsRef.current.send({ image: videoElement })
            } catch (frameErr) {
              console.warn('Frame processing dropped:', frameErr)
            } finally {
              isProcessingFrame.current = false
            }
          }

          if (isComponentMounted.current) {
            animationFrameId.current = requestAnimationFrame(processFrame)
          }
        }

        animationFrameId.current = requestAnimationFrame(processFrame)

        if (isComponentMounted.current) {
          setIsLoading(false)
        }
      } catch (err: any) {
        console.error('Lỗi khi mở camera hoặc MediaPipe:', err)
        if (isComponentMounted.current) {
          setIsLoading(false)
          setCameraError(
            err.name === 'NotAllowedError'
              ? 'Trình duyệt bị từ chối quyền truy cập Webcam. Vui lòng bấm vào icon ổ khóa/cài đặt trên thanh địa chỉ URL để Cấp quyền Camera.'
              : err.name === 'NotFoundError'
              ? 'Không tìm thấy thiết bị Webcam nào kết nối với máy tính của bạn. Trên laptop GIGABYTE / MSI / ASUS, bạn hãy bấm phím nóng Fn + F10 (hoặc mở phần mềm Control Center bật Camera) để cấp nguồn cho Webcam.'
              : 'Không thể kết nối đến Webcam hoặc nạp mô hình MediaPipe. Hãy kiểm tra lại kết nối mạng và thiết bị camera.'
          )
        }
      }
    }

    initMediaPipe()

    // Dọn dẹp tài nguyên khi đóng Modal (Chống rò rỉ bộ nhớ & giải phóng ngay Camera)
    return () => {
      isComponentMounted.current = false
      if (predictionTimerRef.current) {
        clearTimeout(predictionTimerRef.current)
        predictionTimerRef.current = null
      }
      if (animationFrameId.current) {
        cancelAnimationFrame(animationFrameId.current)
        animationFrameId.current = null
      }
      if (handsInstance) {
        try {
          handsInstance.close()
        } catch {}
      }
      if (localStream) {
        localStream.getTracks().forEach((track) => track.stop())
        localStream = null
      }
      if (videoRef.current && videoRef.current.srcObject) {
        const stream = videoRef.current.srcObject as MediaStream
        stream.getTracks().forEach((track) => track.stop())
        videoRef.current.srcObject = null
      }
      if (canvasRef.current) {
        const ctx = canvasRef.current.getContext('2d')
        if (ctx) ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height)
      }
    }
  }, [isOpen, onResults, retryCount, isSimulationMode, redrawSimulationCanvas])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div
        className="relative bg-slate-900 border border-slate-700/80 rounded-3xl shadow-2xl max-w-5xl w-full overflow-hidden flex flex-col"
        style={{ maxHeight: '94vh' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* HEADER */}
        <div className="flex items-center justify-between px-6 py-3.5 bg-slate-800/90 border-b border-slate-700">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <CameraIcon className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-bold text-white tracking-wide">
                  Viết chữ Hán trên không (Air Writing)
                </h3>
                <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {isSimulationMode ? 'Chế độ Chuột mô phỏng' : 'Air Touch & Word Building'}
                </span>
                {isSimulationMode && (
                  <button
                    onClick={() => {
                      setIsSimulationMode(false)
                      setIsLoading(true)
                      setRetryCount((c) => c + 1)
                    }}
                    className="px-2.5 py-0.5 text-xs font-medium rounded-full bg-indigo-600/30 hover:bg-indigo-600/40 text-indigo-200 border border-indigo-500/40 transition cursor-pointer flex items-center gap-1"
                    title="Chuyển về kết nối Camera Webcam"
                  >
                    <CameraIcon className="w-3 h-3" />
                    <span>Dùng Camera</span>
                  </button>
                )}
              </div>
              <p className="text-xs text-slate-400">
                Điều khiển 100% không chạm (✌️ Undo, ✋ Xóa bảng, 👍 Tra từ) • Ghép cụm từ Kanji
              </p>
            </div>
          </div>

          {/* CÁC HUY HIỆU TRẠNG THÁI */}
          <div className="flex items-center gap-2">
            {/* Nút bật/tắt âm thanh phản hồi */}
            <button
              onClick={() => setSoundEnabled((prev) => !prev)}
              className={`p-2 rounded-xl border text-xs font-medium transition cursor-pointer ${
                soundEnabled
                  ? 'bg-indigo-600/20 border-indigo-500/40 text-indigo-300 hover:bg-indigo-600/30'
                  : 'bg-slate-800 border-slate-700 text-slate-400 hover:bg-slate-750'
              }`}
              title={soundEnabled ? 'Tắt âm thanh phản hồi' : 'Bật âm thanh phản hồi'}
            >
              {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>

            {/* Trạng thái Bút vẽ (Pen Status) */}
            <div
              className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border transition ${
                isDrawing
                  ? 'bg-sky-500/20 text-sky-300 border-sky-400/50 animate-pulse'
                  : 'bg-slate-800 text-slate-400 border-slate-700'
              }`}
            >
              <PenTool className="w-3.5 h-3.5" />
              <span>{isDrawing ? 'Hạ bút' : 'Nhấc bút'}</span>
            </div>

            {/* Trạng thái Bàn tay (Hand Detected) */}
            <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs text-slate-300">
              <span
                className={`w-2 h-2 rounded-full ${
                  handDetected ? 'bg-emerald-400' : 'bg-slate-500'
                }`}
              />
              <span>{handDetected ? 'Đã thấy tay' : 'Chưa thấy tay'}</span>
            </div>

            {/* FPS Badge */}
            <div className="hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs font-mono text-slate-400">
              <span
                className={`w-2 h-2 rounded-full ${
                  fps > 25 ? 'bg-emerald-400' : fps > 15 ? 'bg-amber-400' : 'bg-red-400'
                }`}
              />
              <span>{fps} FPS</span>
            </div>

            {/* Nút đóng */}
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl transition cursor-pointer ml-1"
              title="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* 4.4. WORD BUILDING: THANH GHÉP CỤM TỪ KANJI */}
        <div className="px-6 py-2.5 bg-slate-950/90 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span className="text-xs font-semibold text-slate-300 flex items-center gap-1">
              <Layers className="w-3.5 h-3.5 text-sky-400" />
              <span>Cụm từ đang ghép:</span>
            </span>
            {wordBuffer.length === 0 ? (
              <span className="text-xs text-slate-500 italic">
                (Chưa ghép chữ nào — Nhấp &quot;+ Ghép&quot; hoặc Air Hover để ghép thành từ hoàn chỉnh)
              </span>
            ) : (
              <div className="flex items-center gap-1.5">
                {wordBuffer.map((char, index) => (
                  <span
                    key={index}
                    className="inline-flex items-center justify-center w-8 h-8 rounded-xl bg-indigo-500/20 border border-indigo-400/40 text-lg font-bold font-serif text-sky-200 shadow-sm"
                  >
                    {char}
                  </span>
                ))}
                <span className="text-sm font-bold text-sky-400 ml-1">
                  = &ldquo;{wordBuffer.join('')}&rdquo;
                </span>
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            {wordBuffer.length > 0 && (
              <>
                <button
                  onClick={handleRemoveLastChar}
                  className="px-2.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium flex items-center gap-1 transition cursor-pointer"
                  title="Xóa ký tự vừa ghép (Backspace)"
                >
                  <Delete className="w-3.5 h-3.5" />
                  <span>Xóa chữ</span>
                </button>
                <button
                  onClick={handleClearWordBuffer}
                  className="px-2.5 py-1.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/25 text-xs font-medium flex items-center gap-1 transition cursor-pointer"
                  title="Xóa toàn bộ cụm từ đang ghép"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Xóa cụm</span>
                </button>
              </>
            )}

            <button
              onClick={() => handleSearchFinal()}
              disabled={wordBuffer.length === 0 && !topChar && predictions.length === 0}
              className="px-4 py-1.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-semibold text-xs flex items-center gap-1.5 shadow-md shadow-indigo-500/25 transition disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
              title="Tra cứu từ điển (Hoặc giơ ngón cái 👍 giữ 1.0s)"
            >
              <Search className="w-3.5 h-3.5" />
              <span>
                Tra từ điển {wordBuffer.length > 0 ? `(${wordBuffer.join('')})` : topChar ? `(${topChar})` : ''}
              </span>
            </button>
          </div>
        </div>

        {/* BODY - KHUNG VIEWPORT CAMERA VÀ SIDEBAR GỢI Ý AI */}
        <div className="relative flex-1 bg-black flex flex-col md:flex-row overflow-hidden min-h-[440px] sm:min-h-[500px]">
          {/* CỘT TRÁI: CAMERA VIEWPORT & CANVAS NÉT VẼ */}
          <div className="relative flex-1 bg-black flex items-center justify-center overflow-hidden min-h-[360px] sm:min-h-[460px]">
            {/* Thẻ Video ẩn để MediaPipe đọc frame */}
            <video
              ref={videoRef}
              playsInline
              autoPlay
              muted
              className="hidden"
            />

            {/* Canvas chính hiển thị hình ảnh gương hoặc bảng vẽ mô phỏng */}
            <canvas
              ref={canvasRef}
              className={`w-full h-full object-contain ${
                isSimulationMode ? 'cursor-crosshair' : ''
              }`}
              onMouseDown={handleCanvasMouseDown}
              onMouseMove={handleCanvasMouseMove}
              onMouseUp={handleCanvasMouseUp}
              onMouseLeave={handleCanvasMouseUp}
            />

            {/* Màn hình Loading */}
            {isLoading && !isSimulationMode && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-950/90 text-white z-10 gap-3">
                <div className="w-12 h-12 rounded-full border-3 border-indigo-500/30 border-t-indigo-500 animate-spin" />
                <p className="text-sm font-medium text-slate-200">
                  Đang khởi tạo Camera & MediaPipe Hands...
                </p>
              </div>
            )}

            {/* Màn hình Báo lỗi Camera (Hỗ trợ hướng dẫn phím cứng Fn + F10 & Chế độ chuột mô phỏng) */}
            {cameraError && !isSimulationMode && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-950/95 text-white z-20 p-6 text-center gap-3">
                <div className="w-12 h-12 rounded-full bg-red-500/10 border border-red-500/30 flex items-center justify-center text-red-400">
                  <VideoOff className="w-6 h-6" />
                </div>
                <h4 className="text-base font-semibold text-red-400">
                  Lỗi truy cập Camera (Mã 0xA00F4244)
                </h4>
                <p className="text-sm text-slate-300 max-w-md font-medium">{cameraError}</p>

                {/* Hộp hướng dẫn phần cứng cho máy tính GIGABYTE / Laptop */}
                <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 text-xs text-slate-300 max-w-lg text-left space-y-2 mt-1">
                  <div className="font-semibold text-white flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-sky-400" />
                    <span>Cách kích hoạt Camera trên laptop:</span>
                  </div>
                  <ul className="list-disc pl-5 space-y-1.5 text-slate-400">
                    <li>
                      <strong className="text-sky-300">Phím tắt bàn phím:</strong> Nhấn tổ hợp phím{' '}
                      <kbd className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-sky-200 font-mono font-bold">
                        Fn + F10
                      </kbd>{' '}
                      để cấp nguồn cho Camera (màn hình sẽ hiện thông báo <em>Camera ON</em>).
                    </li>
                    <li>
                      <strong className="text-sky-300">GIGABYTE Control Center:</strong> Vào tab{' '}
                      <em>Device Settings</em> gạt mục <em>Camera</em> sang <strong>ON</strong>.
                    </li>
                    <li>
                      <strong className="text-sky-300">Chưa tiện cắm Camera?</strong> Bấm nút{' '}
                      <strong>Chế độ Chuột mô phỏng</strong> bên dưới để test ngay AI nhận diện 3.170 lớp!
                    </li>
                  </ul>
                </div>

                <div className="flex items-center gap-3 mt-1.5">
                  <button
                    onClick={() => {
                      setCameraError(null)
                      setIsLoading(true)
                      setRetryCount((prev) => prev + 1)
                    }}
                    className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold flex items-center gap-2 transition cursor-pointer"
                  >
                    <RefreshCw className="w-3.5 h-3.5" /> Thử lại kết nối Camera
                  </button>

                  <button
                    onClick={startSimulationMode}
                    className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white text-xs font-semibold flex items-center gap-2 transition cursor-pointer shadow-md shadow-emerald-900/20"
                  >
                    <MousePointer className="w-3.5 h-3.5" /> Chế độ Chuột mô phỏng
                  </button>
                </div>
              </div>
            )}

            {/* 4.1 & 4.2 & 4.5: BANNER PHẢN HỒI CỬ CHỈ ĐANG GIỮ (GESTURE HOLD HUD) */}
            {gestureOverlay && (
              <div className="absolute top-4 left-1/2 -translate-x-1/2 bg-slate-900/95 backdrop-blur-md border border-sky-400/60 rounded-2xl px-5 py-3 shadow-2xl flex flex-col items-center gap-2 z-30 animate-in fade-in zoom-in-95 duration-150">
                <div className="flex items-center gap-2 text-white font-bold text-sm tracking-wide">
                  <span>{gestureOverlay.label}</span>
                </div>
                <div className="w-56 bg-slate-800 h-2.5 rounded-full overflow-hidden border border-slate-700">
                  <div
                    className="h-full bg-gradient-to-r from-sky-400 via-indigo-500 to-emerald-400 transition-all duration-75"
                    style={{ width: `${Math.round(gestureOverlay.progress * 100)}%` }}
                  />
                </div>
              </div>
            )}

            {/* HƯỚNG DẪN CỬ CHỈ KHÔNG CHẠM NHANH DƯỚI ĐÁY KHUNG CAMERA */}
            <div className="absolute bottom-4 left-4 right-4 bg-slate-900/80 backdrop-blur-md border border-slate-700/60 rounded-2xl px-4 py-2 text-xs text-slate-300 flex flex-wrap items-center justify-between gap-2 shadow-lg pointer-events-none">
              <div className="flex items-center gap-3">
                <span className="flex items-center gap-1 font-semibold text-white">
                  <Sparkles className="w-3.5 h-3.5 text-sky-400" /> Cử chỉ:
                </span>
                <span className="text-sky-300">✌️ Chữ V (Undo 0.6s)</span>
                <span className="text-rose-300">✋ Xòe tay (Xóa 1.2s)</span>
                <span className="text-emerald-300">👍 Ngón cái (Tra từ 1.0s)</span>
                <span className="text-amber-300">🎯 Dừng ngón 0.8s (Chọn thẻ)</span>
              </div>
              {hoverTargetKey && (
                <div className="text-[11px] font-semibold text-sky-400 animate-pulse">
                  Đang dừng trên: {hoverTargetKey}
                </div>
              )}
            </div>

            {/* BADGE BÁO ĐANG DỰ ĐOÁN NÉT VẼ */}
            {isPredicting && (
              <div className="absolute top-14 left-4 bg-indigo-950/85 backdrop-blur-md border border-indigo-500/40 rounded-xl px-3 py-1.5 text-xs text-indigo-200 flex items-center gap-2 shadow-lg animate-pulse">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-sky-400" />
                <span>AI đang phân tích nét vẽ...</span>
              </div>
            )}
          </div>

          {/* CỘT PHẢI: SIDEBAR HIỂN THỊ DANH SÁCH ỨNG VIÊN TOP-K AI */}
          <div className="w-full md:w-80 bg-slate-900/95 border-t md:border-t-0 md:border-l border-slate-800 flex flex-col justify-between shrink-0">
            {/* Thanh tiêu đề Sidebar */}
            <div className="px-4 py-3 bg-slate-800/60 border-b border-slate-700/80 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Brain className="w-4 h-4 text-sky-400" />
                <span className="text-xs font-bold text-white uppercase tracking-wider">
                  Gợi ý AI thời gian thực
                </span>
              </div>
              {isPredicting ? (
                <span className="flex items-center gap-1.5 text-xs text-sky-400 font-medium">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang đoán...</span>
                </span>
              ) : topChar ? (
                <span className="text-xs font-semibold text-sky-400 bg-sky-500/10 px-2 py-0.5 rounded border border-sky-500/20">
                  Khớp: {topChar}
                </span>
              ) : (
                <span className="text-[11px] font-semibold text-slate-400">
                  {predictions.length > 0 ? `Top ${predictions.length}` : 'Chờ nét vẽ'}
                </span>
              )}
            </div>

            {/* Danh sách các ứng viên ký tự */}
            <div className="flex-1 overflow-y-auto p-3.5 space-y-2.5 max-h-[300px] md:max-h-[460px]">
              {/* Trạng thái chưa có nét vẽ nào */}
              {strokesCount === 0 && (
                <div className="h-full min-h-[220px] flex flex-col items-center justify-center p-4 text-center text-slate-400">
                  <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-2.5">
                    <PenTool className="w-6 h-6" />
                  </div>
                  <p className="text-sm font-semibold text-slate-200">Bắt đầu vẽ nét</p>
                  <p className="text-xs text-slate-400 max-w-[210px] mt-1 leading-relaxed">
                    Chụm ngón tay trước camera để vẽ chữ Kanji. AI sẽ tự động phân tích và đưa ra gợi ý tức thì!
                  </p>
                </div>
              )}

              {/* Trạng thái đang tải nét đầu tiên */}
              {strokesCount > 0 && isPredicting && predictions.length === 0 && (
                <div className="h-full min-h-[220px] flex flex-col items-center justify-center p-4 text-center text-slate-400">
                  <Loader2 className="w-8 h-8 text-sky-400 animate-spin mb-2.5" />
                  <p className="text-sm font-semibold text-slate-200">Đang đối sánh với 3.170 lớp AI...</p>
                  <p className="text-xs text-slate-400 mt-1">Chuẩn hóa khung giấy ETL9G</p>
                </div>
              )}

              {/* Báo lỗi kết nối AI nếu có */}
              {predictionError && (
                <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-xs text-rose-300">
                  {predictionError}
                </div>
              )}

              {/* Render danh sách Top 5 ứng viên */}
              {predictions.length > 0 && (
                <>
                  {/* Ký tự khớp nhất (Top 1) */}
                  <div className="p-3.5 rounded-2xl bg-gradient-to-br from-indigo-950/60 via-slate-800/90 to-slate-900 border border-sky-500/60 hover:border-sky-400 shadow-lg shadow-sky-950/20 transition group">
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300 border border-sky-400/30 text-[10px] font-bold flex items-center gap-1">
                        <Sparkles className="w-3 h-3 text-sky-400" /> Khớp nhất
                      </span>
                      <span className="text-[11px] font-mono text-slate-400">
                        {predictions[0].unicode_hex || ''}
                      </span>
                    </div>

                    <div className="flex items-center justify-between mt-1">
                      <span className="text-4xl font-serif font-bold text-white group-hover:text-sky-300 transition">
                        {predictions[0].char}
                      </span>
                      <div className="text-right">
                        <span className="text-base font-bold text-sky-400">
                          {predictions[0].percentage.toFixed(1)}%
                        </span>
                        <div className="text-[10px] text-slate-400">độ tin cậy</div>
                      </div>
                    </div>

                    <div className="w-full bg-slate-700/60 h-2 rounded-full overflow-hidden mt-2">
                      <div
                        className="bg-gradient-to-r from-sky-400 to-indigo-500 h-full rounded-full transition-all duration-300"
                        style={{
                          width: `${Math.min(100, Math.max(5, predictions[0].percentage))}%`,
                        }}
                      />
                    </div>

                    <div className="mt-3 flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => handleAppendToWordBuffer(predictions[0].char)}
                        className="flex-1 py-1.5 px-3 rounded-xl bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 border border-sky-400/35 text-xs font-semibold flex items-center justify-center gap-1.5 transition cursor-pointer"
                        title="Thêm vào cụm từ đang ghép và xóa bảng để vẽ tiếp"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>Ghép từ</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => handleSearchFinal(predictions[0].char)}
                        className="py-1.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-medium flex items-center justify-center gap-1 transition cursor-pointer"
                        title="Tra cứu trực tiếp chữ này vào từ điển"
                      >
                        <span>Tra ngay</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Các ứng viên thay thế (Top 2 đến Top 5) */}
                  {predictions.slice(1).map((item, idx) => (
                    <div
                      key={item.char + idx}
                      className="flex items-center justify-between p-2.5 rounded-xl bg-slate-800/60 hover:bg-slate-800 border border-slate-700/60 hover:border-slate-600 transition group"
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-5 text-center text-xs font-bold text-slate-500 group-hover:text-slate-400">
                          #{idx + 2}
                        </span>
                        <span className="text-2xl font-serif font-bold text-slate-200 group-hover:text-white transition">
                          {item.char}
                        </span>
                        <div className="flex flex-col">
                          <span className="text-[10px] font-mono text-slate-400">
                            {item.unicode_hex || ''}
                          </span>
                          <div className="w-16 bg-slate-700 h-1 rounded-full overflow-hidden mt-1">
                            <div
                              className="bg-indigo-400 h-full rounded-full"
                              style={{ width: `${item.percentage}%` }}
                            />
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={() => handleAppendToWordBuffer(item.char)}
                          className="px-2 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 border border-sky-400/30 text-[11px] font-semibold flex items-center gap-0.5 transition cursor-pointer"
                          title="Ghép vào cụm từ"
                        >
                          <Plus className="w-3 h-3" />
                          <span>Ghép</span>
                        </button>
                        <button
                          onClick={() => handleSearchFinal(item.char)}
                          className="p-1.5 rounded-lg bg-slate-750 hover:bg-slate-700 text-slate-300 hover:text-white transition cursor-pointer"
                          title="Tra ngay"
                        >
                          <ArrowRight className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </>
              )}
            </div>

            {/* Thông tin footer của Sidebar */}
            <div className="p-3 bg-slate-900 border-t border-slate-800 text-[11px] text-slate-400 flex flex-col gap-1">
              <div className="flex items-center justify-between text-slate-300 font-medium">
                <span className="flex items-center gap-1">
                  <Layers className="w-3 h-3 text-indigo-400" />
                  <span>Deep Residual CNN</span>
                </span>
                <span className="text-[10px] text-indigo-400 font-semibold">3.170 lớp nhãn</span>
              </div>
              <p className="text-[10px] text-slate-500">
                100% Không chạm: ✌️ Undo • ✋ Xóa • 👍 Tra từ • Dừng 0.8s
              </p>
            </div>
          </div>
        </div>

        {/* FOOTER ĐIỀU KHIỂN & CÁC NÚT THAO TÁC NHANH */}
        <div className="px-6 py-3 bg-slate-800/90 border-t border-slate-700/80 flex items-center justify-between gap-3">
          {/* Nhóm nút Hoàn tác & Xóa tất cả */}
          <div className="flex items-center gap-2">
            <button
              onClick={handleUndo}
              disabled={strokesCount === 0}
              className="px-3.5 py-1.5 rounded-xl bg-slate-700/80 hover:bg-slate-700 text-slate-200 disabled:opacity-40 disabled:cursor-not-allowed text-xs font-medium flex items-center gap-1.5 transition cursor-pointer"
              title="Xóa nét vẽ vừa rồi (Hoặc giơ 2 ngón tay chữ V ✌️)"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Hoàn tác (✌️)</span>
            </button>

            <button
              onClick={handleClearAll}
              disabled={strokesCount === 0}
              className="px-3.5 py-1.5 rounded-xl bg-rose-500/15 hover:bg-rose-500/25 border border-rose-500/30 text-rose-300 disabled:opacity-40 disabled:cursor-not-allowed text-xs font-medium flex items-center gap-1.5 transition cursor-pointer"
              title="Xóa toàn bộ các nét (Hoặc xòe bàn tay ✋)"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Xóa tất cả (✋)</span>
            </button>
          </div>

          {/* Checkbox ẩn/hiện xương tay & Huy hiệu Phase 4 */}
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-xs text-slate-400 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={showSkeleton}
                onChange={(e) => setShowSkeleton(e.target.checked)}
                className="rounded border-slate-600 text-indigo-600 focus:ring-indigo-500 bg-slate-700 w-3.5 h-3.5"
              />
              <span>Khung xương tay</span>
            </label>

            <div className="text-xs text-emerald-400 flex items-center gap-1 font-medium">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Phase 4: Không chạm 100%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
