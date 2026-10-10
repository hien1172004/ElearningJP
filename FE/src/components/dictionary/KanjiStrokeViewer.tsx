import React, { useState, useEffect, useRef } from 'react'
import { Play, Pause, RotateCcw, ChevronLeft, ChevronRight } from 'lucide-react'
import type { CharacterStroke } from '@/types'

interface KanjiStrokeViewerProps {
  character: string
  strokes: CharacterStroke[]
  size?: number
}

export const KanjiStrokeViewer: React.FC<KanjiStrokeViewerProps> = ({
  character,
  strokes,
  size = 180,
}) => {
  const [currentStrokeIndex, setCurrentStrokeIndex] = useState<number>(strokes.length)
  const [isPlaying, setIsPlaying] = useState<boolean>(false)
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
    <div className="flex flex-col items-center">
      {/* Khung vẽ chữ với ô lưới chữ Mễ (quadrant grid) */}
      <div
        className="relative bg-white rounded-2xl border-2 border-indigo-100 shadow-inner overflow-hidden flex items-center justify-center"
        style={{ width: size, height: size }}
      >
        {/* Đường kẻ chia ô mờ */}
        <div className="absolute inset-0 pointer-events-none">
          <div className="w-full h-1/2 border-b border-dashed border-indigo-200/60" />
          <div className="absolute top-0 bottom-0 left-1/2 border-r border-dashed border-indigo-200/60" />
        </div>

        {strokes && strokes.length > 0 ? (
          <svg
            viewBox="0 0 109 109"
            className="w-full h-full relative z-10"
            style={{ strokeLinecap: 'round', strokeLinejoin: 'round' }}
          >
            {/* Nét nền mờ chỉ dẫn nếu đang phát */}
            {strokes.map((stroke, index) => (
              <path
                key={`bg-${index}`}
                d={stroke.svgPathData}
                fill="none"
                stroke="#e2e8f0"
                strokeWidth="5"
              />
            ))}

            {/* Các nét đã vẽ */}
            {strokes.slice(0, currentStrokeIndex).map((stroke, index) => {
              const isCurrentStroke = index === currentStrokeIndex - 1
              return (
                <path
                  key={`stroke-${index}`}
                  d={stroke.svgPathData}
                  fill="none"
                  stroke={isCurrentStroke ? '#dc2626' : '#1e293b'}
                  strokeWidth="5"
                  className="transition-all duration-300"
                />
              )
            })}
          </svg>
        ) : (
          <span className="text-6xl font-bold text-gray-800 font-serif">{character}</span>
        )}
      </div>

      {/* Thông tin số nét & Thanh điều khiển */}
      <div className="mt-3 flex flex-col items-center gap-2 w-full max-w-xs">
        <div className="text-xs font-medium text-gray-500">
          Nét vẽ: <span className="text-indigo-600 font-bold">{currentStrokeIndex}</span> / {strokes.length}
        </div>

        {strokes && strokes.length > 0 && (
          <div className="flex items-center gap-1.5 bg-gray-100 p-1.5 rounded-xl border border-gray-200">
            <button
              type="button"
              onClick={handleReset}
              title="Về nét đầu"
              className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-white rounded-lg transition"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handlePrev}
              disabled={currentStrokeIndex <= 0}
              title="Nét trước"
              className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-white rounded-lg transition disabled:opacity-30"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            {isPlaying ? (
              <button
                type="button"
                onClick={handlePause}
                title="Tạm dừng"
                className="p-1.5 bg-amber-500 text-white rounded-lg hover:bg-amber-600 transition shadow-sm"
              >
                <Pause className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={handlePlay}
                title="Tự động vẽ"
                className="p-1.5 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition shadow-sm"
              >
                <Play className="w-4 h-4" />
              </button>
            )}
            <button
              type="button"
              onClick={handleNext}
              disabled={currentStrokeIndex >= strokes.length}
              title="Nét tiếp"
              className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-white rounded-lg transition disabled:opacity-30"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handleShowAll}
              title="Hiện đầy đủ"
              className="px-2 py-1 text-xs font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition"
            >
              Đầy đủ
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
