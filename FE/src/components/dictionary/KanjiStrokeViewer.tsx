import React, { useState, useEffect, useRef } from 'react'
import { Play, Pause, RotateCcw, ChevronLeft, ChevronRight, PenTool } from 'lucide-react'
import type { CharacterStroke } from '@/types'
import { KanjiPracticeModal } from './KanjiPracticeModal'

// Bảng màu đa sắc cho các nét vẽ tương tự hình ảnh tham chiếu
const STROKE_COLORS = [
  '#2563eb', // 1: Xanh dương đậm
  '#dc2626', // 2: Đỏ cam
  '#1e293b', // 3: Đen xám đậm
  '#16a34a', // 4: Xanh lá tươi
  '#d97706', // 5: Vàng cam hổ phách
  '#9333ea', // 6: Tím violet
  '#ec4899', // 7: Hồng sen
  '#0284c7', // 8: Xanh da trời
  '#e11d48', // 9: Đỏ hồng san hô
  '#059669', // 10: Xanh ngọc lục bảo
  '#10b981', // 11: Xanh lá mạ
  '#ca8a04', // 12: Vàng nghệ
  '#8b5cf6', // 13: Tím oải hương
  '#f43f5e', // 14: Hồng thắm
  '#0ea5e9', // 15: Xanh lam sáng
  '#6366f1', // 16: Chàm indigo
  '#14b8a6', // 17: Xanh mòng két
  '#f59e0b', // 18: Hổ phách sáng
  '#a855f7', // 19: Tím phong lan
  '#475569', // 20: Xám đá phiến
]

/**
 * Trích xuất tọa độ điểm bắt đầu từ lệnh M (MoveTo) trong chuỗi SVG path
 */
function getStrokeStartPoint(svgPath: string): { x: number; y: number } {
  if (!svgPath) return { x: 50, y: 50 }
  // Tìm lệnh M/m đầu tiên, ví dụ: "M13.25,38.22..." hoặc "M 13.25 38.22..."
  const match = svgPath.match(/[Mm]\s*([+-]?\d*\.?\d+)[,\s]+([+-]?\d*\.?\d+)/)
  if (match) {
    const x = parseFloat(match[1])
    const y = parseFloat(match[2])
    if (!isNaN(x) && !isNaN(y)) {
      return { x, y }
    }
  }
  return { x: 50, y: 50 }
}

interface KanjiStrokeViewerProps {
  character: string
  strokes: CharacterStroke[]
  size?: number
}

export const KanjiStrokeViewer: React.FC<KanjiStrokeViewerProps> = ({
  character,
  strokes,
  size = 200,
}) => {
  const [currentStrokeIndex, setCurrentStrokeIndex] = useState<number>(strokes.length)
  const [isPlaying, setIsPlaying] = useState<boolean>(false)
  const [isPracticeOpen, setIsPracticeOpen] = useState<boolean>(false)
  const [showNumbers, setShowNumbers] = useState<boolean>(true)
  const [useMultiColors, setUseMultiColors] = useState<boolean>(true)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Reset khi đổi ký tự
  useEffect(() => {
    setCurrentStrokeIndex(strokes.length)
    setIsPlaying(false)
    if (timerRef.current) clearInterval(timerRef.current)
  }, [character, strokes])

  // Logic phát tự động tuần tự
  useEffect(() => {
    if (isPlaying) {
      timerRef.current = setInterval(() => {
        setCurrentStrokeIndex((prev) => {
          if (prev >= strokes.length) {
            setIsPlaying(false)
            return strokes.length
          }
          return prev + 1
        })
      }, 700)
    } else {
      if (timerRef.current) clearInterval(timerRef.current)
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [isPlaying, strokes.length])

  const handlePlay = () => {
    if (currentStrokeIndex >= strokes.length) {
      setCurrentStrokeIndex(0)
    }
    setIsPlaying(true)
  }

  const handlePause = () => {
    setIsPlaying(false)
  }

  const handleReset = () => {
    setIsPlaying(false)
    setCurrentStrokeIndex(0)
  }

  const handleShowAll = () => {
    setIsPlaying(false)
    setCurrentStrokeIndex(strokes.length)
  }

  const handlePrev = () => {
    setIsPlaying(false)
    setCurrentStrokeIndex((prev) => Math.max(0, prev - 1))
  }

  const handleNext = () => {
    setIsPlaying(false)
    setCurrentStrokeIndex((prev) => Math.min(strokes.length, prev + 1))
  }

  return (
    <div className="flex flex-col items-center select-none">
      {/* Khung vẽ chữ với ô lưới chữ Mễ (quadrant grid) */}
      <div
        className="relative bg-white rounded-md border-2 border-indigo-100 shadow-sm overflow-hidden flex items-center justify-center transition-all duration-200"

        style={{ width: size, height: size }}
      >
        {/* Nút reset/vẽ lại góc trên bên phải giống hình mẫu tham chiếu */}
        <button
          type="button"
          onClick={handlePlay}
          title="Chạy mô phỏng lại từ đầu"
          className="absolute top-2 right-2 z-20 p-1.5 bg-white/90 hover:bg-indigo-50 border border-gray-200/80 rounded-xl text-gray-500 hover:text-indigo-600 shadow-2xs hover:scale-105 active:scale-95 transition cursor-pointer"
        >
          <RotateCcw className="w-4 h-4" />
        </button>

        {/* Đường kẻ chia ô chữ Mễ (dashed grid) */}
        <div className="absolute inset-0 pointer-events-none">
          {/* Trục ngang & Trục dọc */}
          <div className="w-full h-1/2 border-b border-dashed border-indigo-300/60" />
          <div className="absolute top-0 bottom-0 left-1/2 border-r border-dashed border-indigo-300/60" />
          {/* Đường chéo mờ */}
          <svg className="absolute inset-0 w-full h-full stroke-indigo-200/40 stroke-dasharray-4">
            <line x1="0" y1="0" x2="100%" y2="100%" strokeDasharray="3 3" strokeWidth="1" />
            <line x1="100%" y1="0" x2="0" y2="100%" strokeDasharray="3 3" strokeWidth="1" />
          </svg>
        </div>

        {strokes && strokes.length > 0 ? (
          <svg
            viewBox="0 0 109 109"
            className="w-full h-full relative z-10"
            style={{ strokeLinecap: 'round', strokeLinejoin: 'round' }}
          >
            {/* 1. Nét nền xám mờ chỉ dẫn vị trí hoàn chỉnh của chữ */}
            {strokes.map((stroke, index) => (
              <path
                key={`bg-${index}`}
                d={stroke.svgPathData}
                fill="none"
                stroke="#f1f5f9"
                strokeWidth="7"
                strokeOpacity="0.8"
              />
            ))}

            {/* 2. Các nét đã vẽ (màu sắc sống động phân biệt từng nét) */}
            {strokes.slice(0, currentStrokeIndex).map((stroke, index) => {
              const strokeColor = useMultiColors
                ? STROKE_COLORS[index % STROKE_COLORS.length]
                : index === currentStrokeIndex - 1
                  ? '#dc2626'
                  : '#1e293b'

              const isLatestStroke = index === currentStrokeIndex - 1

              return (
                <path
                  key={`stroke-${index}`}
                  d={stroke.svgPathData}
                  fill="none"
                  stroke={strokeColor}
                  strokeWidth="6"
                  className={isLatestStroke ? 'transition-all duration-300 drop-shadow-xs' : 'transition-all duration-300'}
                />
              )
            })}

            {/* 3. Hiển thị số thứ tự nét vẽ tại tọa độ bắt đầu của từng nét */}
            {showNumbers &&
              strokes.slice(0, currentStrokeIndex).map((stroke, index) => {
                const strokeColor = useMultiColors
                  ? STROKE_COLORS[index % STROKE_COLORS.length]
                  : '#475569'

                const startPoint = getStrokeStartPoint(stroke.svgPathData)

                // Căn chỉnh vị trí số để không bị che lấp nét mực
                const posX = Math.max(6, Math.min(103, startPoint.x - 4))
                const posY = Math.max(8, Math.min(103, startPoint.y - 3))

                return (
                  <g key={`number-${index}`} className="animate-in fade-in zoom-in-75 duration-200">
                    {/* Viền sáng mờ phía sau số để đọc rõ ràng trên mọi nền */}
                    <text
                      x={posX}
                      y={posY}
                      fill="#ffffff"
                      stroke="#ffffff"
                      strokeWidth="2.5"
                      strokeLinejoin="round"
                      fontSize="9"
                      fontWeight="bold"
                      fontFamily="sans-serif"
                      textAnchor="middle"
                      dominantBaseline="central"
                    >
                      {index + 1}
                    </text>
                    {/* Chữ số thứ tự đồng màu với nét */}
                    <text
                      x={posX}
                      y={posY}
                      fill={strokeColor}
                      fontSize="9"
                      fontWeight="800"
                      fontFamily="sans-serif"
                      textAnchor="middle"
                      dominantBaseline="central"
                    >
                      {index + 1}
                    </text>
                  </g>
                )
              })}
          </svg>
        ) : (
          <span className="text-6xl font-bold text-gray-800 font-serif">{character}</span>
        )}
      </div>

      {/* Thông tin số nét & Tùy chọn hiển thị */}
      <div className="mt-3 flex flex-col items-center gap-2 w-full max-w-xs">
        <div className="flex items-center justify-between w-full px-1 text-xs">
          <div className="font-medium text-gray-500">
            Nét vẽ: <span className="text-indigo-600 font-extrabold">{currentStrokeIndex}</span> / {strokes.length}
          </div>
          {/* Nút bật/tắt số thứ tự và chế độ màu */}
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setShowNumbers((prev) => !prev)}
              className={`px-2 py-0.5 rounded-md font-bold transition text-[11px] cursor-pointer ${showNumbers
                  ? 'bg-indigo-100 text-indigo-700'
                  : 'bg-gray-100 text-gray-400 hover:text-gray-600'
                }`}
              title="Bật/Tắt hiển thị số thứ tự nét"
            >
              Số STT
            </button>
            <button
              type="button"
              onClick={() => setUseMultiColors((prev) => !prev)}
              className={`px-2 py-0.5 rounded-md font-bold transition text-[11px] cursor-pointer ${useMultiColors
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-gray-100 text-gray-400 hover:text-gray-600'
                }`}
              title="Bật/Tắt chế độ đa sắc màu phân biệt nét"
            >
              Đa sắc
            </button>
          </div>
        </div>

        {strokes && strokes.length > 0 && (
          <div className="flex items-center gap-1.5 bg-gray-100 p-1.5 rounded-md border border-gray-200">
            <button
              type="button"
              onClick={handleReset}
              title="Về nét đầu"
              className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-white rounded transition"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handlePrev}
              disabled={currentStrokeIndex <= 0}
              title="Nét trước"
              className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-white rounded transition disabled:opacity-30"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            {isPlaying ? (
              <button
                type="button"
                onClick={handlePause}
                title="Tạm dừng"
                className="p-1.5 bg-amber-500 text-white rounded hover:bg-amber-600 transition shadow-sm"
              >
                <Pause className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={handlePlay}
                title="Tự động vẽ"
                className="p-1.5 bg-indigo-600 text-white rounded hover:bg-indigo-700 transition shadow-sm"
              >
                <Play className="w-4 h-4" />
              </button>
            )}
            <button
              type="button"
              onClick={handleNext}
              disabled={currentStrokeIndex >= strokes.length}
              title="Nét tiếp"
              className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-white rounded transition disabled:opacity-30"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handleShowAll}
              title="Hiện đầy đủ"
              className="px-2 py-1 text-xs font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded transition"
            >
              Đầy đủ
            </button>
          </div>
        )}

        {strokes && strokes.length > 0 && (
          <button
            type="button"
            onClick={() => setIsPracticeOpen(true)}
            className="mt-2 w-full py-2 px-3 bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 text-white rounded-md text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm active:scale-95 transition cursor-pointer"
          >
            <PenTool className="w-3.5 h-3.5 text-amber-300" />
            <span>Thực hành viết & Chấm điểm AI</span>
          </button>
        )}
      </div>

      <KanjiPracticeModal
        isOpen={isPracticeOpen}
        onClose={() => setIsPracticeOpen(false)}
        character={character}
        strokes={strokes}
      />
    </div>
  )
}
