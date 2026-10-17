import React, { useRef, useState, useEffect, useCallback } from 'react'
import { createPortal } from 'react-dom'
import { X, Trash2, RotateCcw, Search, Sparkles, Loader2, Delete } from 'lucide-react'
import { aiService, type PredictionItem } from '../../services/ai.service'

interface Point {
  x: number
  y: number
}

interface KanjiHandwritingModalProps {
  isOpen: boolean
  onClose: () => void
  onSelectCharacter: (char: string) => void
}

export const KanjiHandwritingModal: React.FC<KanjiHandwritingModalProps> = ({
  isOpen,
  onClose,
  onSelectCharacter,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const [isDrawing, setIsDrawing] = useState(false)
  const [strokes, setStrokes] = useState<Point[][]>([])
  const [currentStroke, setCurrentStroke] = useState<Point[]>([])
  const [predictions, setPredictions] = useState<PredictionItem[]>([])
  const [isPredicting, setIsPredicting] = useState(false)
  const [selectedChars, setSelectedChars] = useState<string[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const CANVAS_SIZE = 480

  // 1. Vẽ lại toàn bộ canvas: Lưới chữ thập + Các nét vẽ
  const redrawCanvas = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Xóa trắng toàn bộ
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    // Vẽ đường chữ thập 4 ô mờ (Grid lines)
    ctx.save()
    ctx.strokeStyle = '#e2e8f0'
    ctx.lineWidth = 1.5
    ctx.setLineDash([6, 6])

    // Đường kẻ dọc chính giữa
    ctx.beginPath()
    ctx.moveTo(canvas.width / 2, 0)
    ctx.lineTo(canvas.width / 2, canvas.height)
    ctx.stroke()

    // Đường kẻ ngang chính giữa
    ctx.beginPath()
    ctx.moveTo(0, canvas.height / 2)
    ctx.lineTo(canvas.width, canvas.height / 2)
    ctx.stroke()
    ctx.restore()

    // Cấu hình nét vẽ người dùng (màu xanh dương đậm chuẩn như ảnh mockup)
    ctx.strokeStyle = '#2563eb'
    ctx.lineWidth = 10
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'

    // Vẽ lại tất cả các nét đã hoàn thành
    const allStrokes = [...strokes, currentStroke]
    for (const stroke of allStrokes) {
      if (stroke.length === 0) continue
      ctx.beginPath()
      ctx.moveTo(stroke[0].x, stroke[0].y)
      for (let i = 1; i < stroke.length; i++) {
        ctx.lineTo(stroke[i].x, stroke[i].y)
      }
      ctx.stroke()
    }
  }, [strokes, currentStroke])

  // Cập nhật lại canvas mỗi khi nét vẽ thay đổi
  useEffect(() => {
    if (isOpen) {
      redrawCanvas()
    }
  }, [isOpen, redrawCanvas])

  // Reset state khi mở Modal
  useEffect(() => {
    if (isOpen) {
      setStrokes([])
      setCurrentStroke([])
      setPredictions([])
      setSelectedChars([])
      setErrorMessage(null)
    }
  }, [isOpen])

  // Khóa scroll body khi modal mở (tránh Antd mask chồng lên khi scroll)
  useEffect(() => {
    if (!isOpen) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [isOpen])

  // 2. Gửi ảnh sang AI Service để nhận diện (Debounce 450ms)
  const triggerPrediction = useCallback(async () => {
    const canvas = canvasRef.current
    if (!canvas || strokes.length === 0) {
      setPredictions([])
      return
    }

    try {
      setIsPredicting(true)
      setErrorMessage(null)

      // Tạo canvas tạm nền đen nét trắng để xuất ảnh đúng chuẩn model AI đã train
      const exportCanvas = document.createElement('canvas')
      exportCanvas.width = canvas.width
      exportCanvas.height = canvas.height
      const exportCtx = exportCanvas.getContext('2d')

      if (exportCtx) {
        exportCtx.fillStyle = '#000000'
        exportCtx.fillRect(0, 0, exportCanvas.width, exportCanvas.height)
        exportCtx.strokeStyle = '#ffffff'
        exportCtx.lineWidth = 12
        exportCtx.lineCap = 'round'
        exportCtx.lineJoin = 'round'

        for (const stroke of strokes) {
          if (stroke.length === 0) continue
          exportCtx.beginPath()
          exportCtx.moveTo(stroke[0].x, stroke[0].y)
          for (let i = 1; i < stroke.length; i++) {
            exportCtx.lineTo(stroke[i].x, stroke[i].y)
          }
          exportCtx.stroke()
        }

        const base64Data = exportCanvas.toDataURL('image/png')
        const response = await aiService.predictBase64(base64Data, 10)

        if (response.success && response.predictions) {
          setPredictions(response.predictions)
        } else {
          setPredictions([])
          if (response.message) setErrorMessage(response.message)
        }
      }
    } catch {
      setErrorMessage('Không thể kết nối đến máy chủ AI (Port 8000). Hãy đảm bảo AI service đang chạy.')
    } finally {
      setIsPredicting(false)
    }
  }, [strokes])

  // Tự động gọi nhận diện sau khi người dùng nhấc bút 450ms
  useEffect(() => {
    if (strokes.length === 0) {
      setPredictions([])
      return
    }

    const timer = setTimeout(() => {
      triggerPrediction()
    }, 450)

    return () => clearTimeout(timer)
  }, [strokes, triggerPrediction])

  // 3. Xử lý sự kiện vẽ Chuột & Cảm ứng (Touch)
  const getCoordinates = (e: React.MouseEvent | React.TouchEvent): Point | null => {
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
      }
    } else {
      return {
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY,
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
    if (!isDrawing) return
    e.preventDefault()
    const point = getCoordinates(e)
    if (!point) return
    setCurrentStroke((prev) => [...prev, point])
  }

  const handleEnd = () => {
    if (!isDrawing) return
    setIsDrawing(false)
    if (currentStroke.length > 0) {
      setStrokes((prev) => [...prev, currentStroke])
      setCurrentStroke([])
    }
  }

  // 4. Các nút thao tác
  const handleClearAll = () => {
    setStrokes([])
    setCurrentStroke([])
    setPredictions([])
    setSelectedChars([])
    setErrorMessage(null)
  }

  const handleUndo = () => {
    setStrokes((prev) => prev.slice(0, prev.length - 1))
    setCurrentStroke([])
  }

  const handleSelectChar = (char: string) => {
    setSelectedChars((prev) => [...prev, char])
  }

  const handleBackspace = () => {
    setSelectedChars((prev) => prev.slice(0, prev.length - 1))
  }

  const handleConfirmSearch = (charToSearch?: string) => {
    const finalKeyword = charToSearch || selectedChars.join('') || (predictions.length > 0 ? predictions[0].char : '')
    if (finalKeyword) {
      onSelectCharacter(finalKeyword)
      onClose()
    }
  }

  if (!isOpen) return null

  return createPortal(
    <div className="fixed inset-0 z-[2000] flex items-center justify-center p-4 sm:p-6 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div
        className="bg-white rounded-md shadow-2xl w-full max-w-6xl border border-gray-100 overflow-hidden transform animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Modal */}
        <div className="flex items-center justify-between px-8 py-5 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-md bg-indigo-50 flex items-center justify-center text-indigo-600">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-xl font-bold text-gray-900">Nhận dạng nét viết tay</h3>
              <p className="text-xs text-gray-500 mt-0.5">
                Vẽ chữ Kanji hoặc Kana vào khung bên trái để AI gợi ý kết quả
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition"
            aria-label="Đóng"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Nội dung Modal: Canvas bên trái, Kết quả gợi ý bên phải */}
        <div className="p-8 grid grid-cols-1 md:grid-cols-12 gap-8 items-start">
          {/* VÙNG VẼ CANVAS (Bên trái - 7 cột) */}
          <div className="md:col-span-7 flex flex-col items-center space-y-5">
            <div className="relative border-2 border-gray-200 rounded-md overflow-hidden shadow-inner bg-white cursor-crosshair touch-none">
              <canvas
                ref={canvasRef}
                width={CANVAS_SIZE}
                height={CANVAS_SIZE}
                onMouseDown={handleStart}
                onMouseMove={handleMove}
                onMouseUp={handleEnd}
                onMouseLeave={handleEnd}
                onTouchStart={handleStart}
                onTouchMove={handleMove}
                onTouchEnd={handleEnd}
                className="block max-w-full h-auto"
                style={{ width: '100%', maxWidth: CANVAS_SIZE, height: 'auto', aspectRatio: '1 / 1' }}
              />

              {/* Loading overlay nhẹ khi AI đang nhận diện */}
              {isPredicting && (
                <div className="absolute top-3 right-3 bg-indigo-600/90 text-white text-xs px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-md backdrop-blur-xs">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang nhận diện...</span>
                </div>
              )}
            </div>

            {/* Thanh công cụ nút bấm dưới Canvas */}
            <div className="flex items-center justify-between w-full gap-2">
              <button
                type="button"
                onClick={handleClearAll}
                disabled={strokes.length === 0}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-md text-sm font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-40 disabled:hover:bg-transparent transition"
              >
                <Trash2 className="w-4 h-4" />
                <span>Xóa tất cả</span>
              </button>

              <button
                type="button"
                onClick={handleUndo}
                disabled={strokes.length === 0}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-md text-sm font-semibold text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:hover:bg-transparent transition"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Hoàn tác</span>
              </button>

              <button
                type="button"
                onClick={() => handleConfirmSearch()}
                disabled={strokes.length === 0 && selectedChars.length === 0}
                className="flex items-center gap-1.5 px-5 py-2.5 rounded-md text-sm font-bold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40 transition shadow-sm"
              >
                <Search className="w-4 h-4" />
                <span>Tra cứu</span>
              </button>
            </div>
          </div>

          {/* VÙNG KẾT QUẢ GỢI Ý (Bên phải - 5 cột) */}
          <div className="md:col-span-5 flex flex-col h-full justify-between space-y-5">
            <div>
              <div className="flex items-center justify-between pb-3 border-b border-gray-100 mb-4">
                <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                  Kết quả gợi ý
                </span>
                {predictions.length > 0 && (
                  <span className="text-[11px] text-indigo-600 font-medium">
                    Top {predictions.length} ký tự
                  </span>
                )}
              </div>

              {/* Lưới các ô chữ gợi ý */}
              {predictions.length > 0 ? (
                <div className="grid grid-cols-4 gap-2.5">
                  {predictions.map((p, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => {
                        handleSelectChar(p.char)
                        // Bấm đúp hoặc bấm trực tiếp để tra cứu ngay
                        handleConfirmSearch(p.char)
                      }}
                      className="group relative flex flex-col items-center justify-center p-3.5 rounded-md border border-gray-200 bg-white hover:border-indigo-500 hover:bg-indigo-50/50 hover:shadow-md transition active:scale-95"
                      title={`Độ tin cậy: ${p.percentage}%`}
                    >
                      <span className="text-2xl font-bold text-gray-900 group-hover:text-indigo-600 transition">
                        {p.char}
                      </span>
                      <span className="text-[10px] text-gray-400 mt-0.5">
                        {p.percentage}%
                      </span>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="py-16 text-center text-gray-400 text-xs">
                  {strokes.length === 0 ? (
                    <p>Hãy vẽ nét chữ Kanji hoặc Kana vào khung bên trái để nhận dạng.</p>
                  ) : isPredicting ? (
                    <div className="flex flex-col items-center gap-2">
                      <Loader2 className="w-5 h-5 text-indigo-600 animate-spin" />
                      <span>Đang phân tích nét vẽ...</span>
                    </div>
                  ) : (
                    <p>Chưa có kết quả dự đoán nào.</p>
                  )}
                </div>
              )}

              {errorMessage && (
                <div className="mt-3 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-xs">
                  {errorMessage}
                </div>
              )}
            </div>

            {/* Ô chữ đã chọn & Nút Backspace */}
            {selectedChars.length > 0 && (
              <div className="pt-4 border-t border-gray-100 flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 border border-gray-200 rounded-md flex-1 overflow-x-auto">
                  <span className="text-xs text-gray-400 font-medium">Đã chọn:</span>
                  <span className="text-base font-extrabold text-indigo-600 tracking-wider">
                    {selectedChars.join('')}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleBackspace}
                  className="p-2.5 rounded-md text-gray-500 hover:text-rose-600 hover:bg-rose-50 border border-gray-200 transition"
                  title="Xóa ký tự cuối"
                >
                  <Delete className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>,
    document.body,
  )
}
