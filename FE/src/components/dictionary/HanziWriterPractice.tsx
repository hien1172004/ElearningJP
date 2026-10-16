import React, { useEffect, useRef, useState } from 'react'
import HanziWriter from 'hanzi-writer'
import { Play, RotateCcw, Eye, EyeOff, Award, AlertCircle, CheckCircle2, Sparkles, HelpCircle } from 'lucide-react'

interface HanziWriterPracticeProps {
  character: string
  size?: number
  onCompleteQuiz?: (summary: { character: string; totalMistakes: number; accuracyScore: number }) => void
}

export const HanziWriterPractice: React.FC<HanziWriterPracticeProps> = ({
  character,
  size = 320,
  onCompleteQuiz,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const writerRef = useRef<HanziWriter | null>(null)

  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [showOutline, setShowOutline] = useState<boolean>(true)
  const [mistakesCount, setMistakesCount] = useState<number>(0)
  const [currentStroke, setCurrentStroke] = useState<number>(1)
  const [totalStrokes, setTotalStrokes] = useState<number>(0)
  const [quizStatus, setQuizStatus] = useState<'idle' | 'in_progress' | 'completed'>('idle')
  const [quizSummary, setQuizSummary] = useState<{
    totalMistakes: number
    accuracyScore: number
  } | null>(null)

  // Khởi tạo HanziWriter với nạp dữ liệu chuẩn Kanji Nhật Bản (@k1low/hanzi-writer-data-jp)
  useEffect(() => {
    if (!containerRef.current || !character) return

    setIsLoading(true)
    setLoadError(null)
    setQuizStatus('idle')
    setQuizSummary(null)
    setMistakesCount(0)
    setCurrentStroke(1)

    // Xóa nội dung container cũ
    containerRef.current.innerHTML = ''

    let isCancelled = false

    try {
      const writer = HanziWriter.create(containerRef.current, character, {
        width: size,
        height: size,
        padding: 20,
        showOutline: true,
        strokeAnimationSpeed: 1.2,
        strokeHighlightSpeed: 1.5,
        strokeColor: '#1e1b4b', // Mực chàm đậm
        outlineColor: '#cbd5e1', // Nét viền mờ chỉ dẫn
        drawingColor: '#2563eb', // Nét người dùng vẽ: Xanh dương nổi bật
        drawingWidth: 7,
        highlightColor: '#10b981', // Màu sáng khi vẽ đúng nét: Xanh ngọc
        highlightOnComplete: true,
        showHintAfterMisses: 2, // Tự động gợi ý nét nếu viết sai 2 lần
        charDataLoader: (char, onLoaded, onError) => {
          // 1. Thử tải dữ liệu nét chuẩn Kanji Nhật Bản
          fetch(`https://unpkg.com/@k1low/hanzi-writer-data-jp@latest/${char}.json`)
            .then((res) => {
              if (res.ok) return res.json()
              // 2. Nếu không có ở gói JP, fallback sang dữ liệu gốc của HanziWriter (MakeMeAHanzi)
              return fetch(`https://cdn.jsdelivr.net/npm/hanzi-writer-data@latest/${char}.json`).then((r) => {
                if (!r.ok) throw new Error(`Không tìm thấy dữ liệu nét cho chữ '${char}'`)
                return r.json()
              })
            })
            .then((data) => {
              if (!isCancelled) {
                setTotalStrokes(data.strokes?.length || 0)
                setIsLoading(false)
                onLoaded(data)
              }
            })
            .catch((err) => {
              if (!isCancelled) {
                setIsLoading(false)
                setLoadError(`Ký tự '${char}' chưa có dữ liệu nét trong thư viện.`)
                onError(err)
              }
            })
        },
      })

      writerRef.current = writer
    } catch (err: any) {
      setIsLoading(false)
      setLoadError('Không thể khởi tạo bộ luyện viết.')
    }

    return () => {
      isCancelled = true
      if (writerRef.current) {
        try {
          writerRef.current.destroy()
        } catch {}
      }
    }
  }, [character, size])

  // Bắt đầu chế độ làm bài kiểm tra nét vẽ (Quiz Mode)
  const startPracticeQuiz = () => {
    if (!writerRef.current) return
    setQuizStatus('in_progress')
    setQuizSummary(null)
    setMistakesCount(0)
    setCurrentStroke(1)

    writerRef.current.quiz({
      showOutline: showOutline,
      showHintAfterMisses: 2,
      onMistake: (strokeData) => {
        setMistakesCount(strokeData.totalMistakes)
      },
      onCorrectStroke: (strokeData) => {
        setCurrentStroke(strokeData.strokeNum + 2) // chuyển sang nét tiếp theo
        setMistakesCount(strokeData.totalMistakes)
      },
      onComplete: (summary) => {
        const strokeTotal = totalStrokes || 1
        // Công thức tính điểm: Mỗi lần sai trừ bớt điểm
        const score = Math.max(0, Math.round(((strokeTotal) / (strokeTotal + summary.totalMistakes)) * 100))
        setQuizStatus('completed')
        setQuizSummary({
          totalMistakes: summary.totalMistakes,
          accuracyScore: score,
        })
        if (onCompleteQuiz) {
          onCompleteQuiz({
            character: summary.character,
            totalMistakes: summary.totalMistakes,
            accuracyScore: score,
          })
        }
      },
    })
  }

  // Chạy hiệu ứng viết mẫu toàn bộ chữ
  const handleAnimate = () => {
    if (!writerRef.current) return
    writerRef.current.cancelQuiz()
    setQuizStatus('idle')
    writerRef.current.animateCharacter()
  }

  // Làm mới / Xóa viết lại
  const handleReset = () => {
    if (!writerRef.current) return
    writerRef.current.cancelQuiz()
    startPracticeQuiz()
  }

  // Bật/tắt nét mờ chỉ dẫn
  const handleToggleOutline = () => {
    if (!writerRef.current) return
    const nextState = !showOutline
    setShowOutline(nextState)
    if (nextState) {
      writerRef.current.showOutline()
    } else {
      writerRef.current.hideOutline()
    }
  }

  return (
    <div className="flex flex-col items-center select-none w-full">
      {/* Khung trạng thái nét khi đang luyện tập */}
      <div className="w-full flex items-center justify-between mb-3 px-1 text-xs">
        <div className="flex items-center gap-2">
          {quizStatus === 'in_progress' ? (
            <span className="font-semibold text-gray-700">
              Đang viết nét: <strong className="text-indigo-600 font-extrabold text-sm">{Math.min(currentStroke, totalStrokes)}</strong> / {totalStrokes}
            </span>
          ) : quizStatus === 'completed' ? (
            <span className="text-emerald-600 font-bold flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4" /> Hoàn thành bài viết!
            </span>
          ) : (
            <span className="text-gray-500 font-medium">Sẵn sàng luyện viết ({totalStrokes} nét)</span>
          )}

          {quizStatus === 'in_progress' && (
            <span className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-700 font-bold border border-amber-200 text-[11px]">
              Sai: {mistakesCount} lần
            </span>
          )}
        </div>

        <button
          type="button"
          onClick={handleToggleOutline}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border transition text-xs ${
            showOutline
              ? 'bg-indigo-50 border-indigo-200 text-indigo-700 font-semibold'
              : 'bg-gray-50 border-gray-200 text-gray-500 hover:bg-gray-100'
          }`}
        >
          {showOutline ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
          <span>{showOutline ? 'Hiện nét mờ' : 'Ẩn nét mờ'}</span>
        </button>
      </div>

      {/* Vùng Canvas chứa HanziWriter có nền lưới chữ Mễ */}
      <div
        className="relative bg-white rounded-3xl border-2 border-indigo-200/80 shadow-md overflow-hidden flex items-center justify-center transition-all duration-200"
        style={{ width: size, height: size }}
      >
        {/* Lưới chữ Mễ (Dashed grid) */}
        <div className="absolute inset-0 pointer-events-none z-0">
          <div className="w-full h-1/2 border-b border-dashed border-indigo-200/70" />
          <div className="absolute top-0 bottom-0 left-1/2 border-r border-dashed border-indigo-200/70" />
          <svg className="absolute inset-0 w-full h-full stroke-indigo-100 stroke-dasharray-4">
            <line x1="0" y1="0" x2="100%" y2="100%" strokeDasharray="3 3" strokeWidth="1" />
            <line x1="100%" y1="0" x2="0" y2="100%" strokeDasharray="3 3" strokeWidth="1" />
          </svg>
        </div>

        {/* DOM element được HanziWriter gắn SVG vào */}
        <div ref={containerRef} className="relative z-10 w-full h-full cursor-crosshair" />

        {/* Loading Spinner */}
        {isLoading && (
          <div className="absolute inset-0 z-20 bg-white/80 backdrop-blur-2xs flex flex-col items-center justify-center gap-2 text-indigo-600">
            <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin" />
            <span className="text-xs font-semibold text-gray-600">Đang nạp nét chữ...</span>
          </div>
        )}

        {/* Thông báo lỗi nếu ký tự không có trong dữ liệu */}
        {loadError && (
          <div className="absolute inset-0 z-20 bg-white/95 p-4 flex flex-col items-center justify-center text-center gap-2">
            <AlertCircle className="w-8 h-8 text-amber-500" />
            <p className="text-xs font-bold text-gray-800">{loadError}</p>
            <p className="text-[11px] text-gray-500">Bạn có thể chuyển sang tab Canvas AI tự do để thực hành.</p>
          </div>
        )}
      </div>

      {/* Bảng tổng kết điểm khi hoàn thành Quiz */}
      {quizSummary && (
        <div className="mt-4 p-4 rounded-2xl bg-gradient-to-br from-indigo-50 to-emerald-50 border border-emerald-200 w-full max-w-sm animate-in fade-in zoom-in-95 duration-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Award className="w-6 h-6 text-amber-500" />
              <div>
                <h4 className="text-sm font-extrabold text-gray-900">Điểm độ chuẩn xác:</h4>
                <p className="text-xs text-gray-500">Số nét sai: {quizSummary.totalMistakes} lần</p>
              </div>
            </div>
            <div className="text-2xl font-black text-emerald-600">
              {quizSummary.accuracyScore}%
            </div>
          </div>
        </div>
      )}

      {/* Thanh điều khiển nút bấm */}
      <div className="flex items-center gap-2 mt-4 w-full justify-center flex-wrap">
        <button
          type="button"
          onClick={handleAnimate}
          disabled={isLoading || !!loadError}
          className="px-3.5 py-2 rounded-xl border border-gray-200 bg-white hover:bg-indigo-50 hover:text-indigo-600 text-gray-700 text-xs font-bold flex items-center gap-1.5 transition cursor-pointer shadow-2xs disabled:opacity-40"
        >
          <Play className="w-3.5 h-3.5" />
          <span>Xem viết mẫu</span>
        </button>

        <button
          type="button"
          onClick={handleReset}
          disabled={isLoading || !!loadError}
          className="px-3.5 py-2 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-gray-700 text-xs font-bold flex items-center gap-1.5 transition cursor-pointer shadow-2xs disabled:opacity-40"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Viết lại</span>
        </button>

        <button
          type="button"
          onClick={startPracticeQuiz}
          disabled={isLoading || !!loadError || quizStatus === 'in_progress'}
          className="px-5 py-2 bg-gradient-to-r from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 text-white rounded-xl text-xs font-black flex items-center gap-1.5 shadow-sm active:scale-95 transition cursor-pointer disabled:opacity-50"
        >
          <Sparkles className="w-3.5 h-3.5 text-amber-300" />
          <span>{quizStatus === 'in_progress' ? 'Đang trong bài luyện' : 'Bắt đầu luyện vẽ'}</span>
        </button>
      </div>

      <div className="mt-3 flex items-center gap-1 text-[11px] text-gray-400">
        <HelpCircle className="w-3.5 h-3.5" />
        <span>Gợi ý: Cầm chuột hoặc bút viết từng nét theo đúng chiều từ trái qua phải, trên xuống dưới.</span>
      </div>
    </div>
  )
}
