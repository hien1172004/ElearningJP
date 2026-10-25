import React, { useState, useEffect, useRef } from 'react'
import { Sparkles, Check, Trash2, X, Loader2 } from 'lucide-react'

export interface FuriganaPopoverProps {
  isOpen: boolean
  baseText: string
  initialReading?: string
  position?: { top: number; left: number }
  onApply: (reading: string) => void
  onRemove?: () => void
  onClose: () => void
}

export const FuriganaPopover: React.FC<FuriganaPopoverProps> = ({
  isOpen,
  baseText,
  initialReading = '',
  position,
  onApply,
  onRemove,
  onClose,
}) => {
  const [reading, setReading] = useState(initialReading)
  const [loadingSuggestion, setLoadingSuggestion] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const popoverRef = useRef<HTMLDivElement>(null)

  // Tự động gợi ý Hiragana qua endpoint khi mở popover
  useEffect(() => {
    if (!isOpen) return

    setReading(initialReading)

    // Tự động phân tích Hiragana khi mở popover
    if (!initialReading && baseText && /[\u4e00-\u9faf]/.test(baseText)) {
      setLoadingSuggestion(true)

      const resolveReading = async (): Promise<string> => {
        // Chạy song song: Thuật toán từ điển PyKakasi offline (< 10ms) & Kuroshiro client
        // Cả 2 đều dùng từ điển thuật toán cục bộ, 0% Gemini, 0 token
        const fastApiPromise = fetch(`http://127.0.0.1:8000/api/v1/furigana/suggest?text=${encodeURIComponent(baseText)}`)
          .then((res) => res.json())
          .then((data) => (data?.reading ? (data.reading as string) : ''))
          .catch(() => '')

        const clientPromise = import('../utils/kuroshiroService')
          .then(({ kuroshiroService }) => kuroshiroService.suggestReading(baseText))
          .catch(() => '')

        // Bên nào trả về kết quả trước thì lập tức điền ngay (thường mất 5ms - 15ms)
        const firstResult = await Promise.race([
          fastApiPromise.then((r) => (r ? r : new Promise<string>(() => {}))),
          clientPromise.then((r) => (r ? r : new Promise<string>(() => {}))),
          new Promise<string>((resolve) => setTimeout(() => resolve(''), 800)),
        ])

        if (firstResult) return firstResult

        const fallback1 = await fastApiPromise
        if (fallback1) return fallback1
        const fallback2 = await clientPromise
        return fallback2 || ''
      }

      resolveReading()
        .then((suggested) => {
          if (suggested) {
            setReading(suggested)
          }
        })
        .finally(() => {
          setLoadingSuggestion(false)
          setTimeout(() => {
            inputRef.current?.focus()
            inputRef.current?.select()
          }, 50)
        })
    } else {
      setTimeout(() => {
        inputRef.current?.focus()
        inputRef.current?.select()
      }, 50)
    }
  }, [isOpen, baseText, initialReading])

  // Xử lý phím Enter / Escape
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      if (reading.trim()) {
        onApply(reading.trim())
      }
    } else if (e.key === 'Escape') {
      e.preventDefault()
      onClose()
    }
  }

  // Click ngoài popover để đóng
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        onClose()
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div
      ref={popoverRef}
      style={
        position
          ? {
              top: `${Math.max(10, position.top)}px`,
              left: `${Math.max(10, position.left)}px`,
            }
          : undefined
      }
      className={`${
        position ? 'fixed' : 'relative'
      } z-50 p-2.5 bg-white border border-indigo-200/90 rounded-2xl shadow-xl w-72 sm:w-80 space-y-2 animate-in fade-in zoom-in-95 duration-100 select-none`}
    >
      {/* Header nhỏ */}
      <div className="flex items-center justify-between border-b border-indigo-100 pb-1.5">
        <span className="text-[11px] font-bold text-indigo-950 flex items-center gap-1">
          <Sparkles className="w-3 h-3 text-indigo-600" />
          Furigana cho: <span className="font-serif font-black text-indigo-700 bg-indigo-50 px-1.5 py-0.5 rounded border border-indigo-100">{baseText}</span>
        </span>
        <button
          type="button"
          onClick={onClose}
          className="text-slate-400 hover:text-slate-600 p-0.5 rounded"
          title="Đóng (Esc)"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Ô nhập Hiragana duy nhất */}
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={reading}
          onChange={(e) => setReading(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Nhập Hiragana (ví dụ: たいいくかん)..."
          className="w-full pr-8 pl-2.5 py-1.5 border border-indigo-200 rounded-xl text-xs font-serif bg-slate-50 focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
        />
        {loadingSuggestion && (
          <div className="absolute right-2.5 top-1/2 -translate-y-1/2 text-indigo-500">
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          </div>
        )}
      </div>

      {/* Xem trước trực quan & Hành động */}
      <div className="flex items-center justify-between pt-1">
        <div className="text-xs font-serif text-slate-800 flex items-center gap-1">
          {reading ? (
            <ruby className="bg-indigo-50/50 px-1.5 py-0.5 rounded">
              {baseText}
              <rt className="text-[0.6em] text-indigo-600 font-sans">{reading}</rt>
            </ruby>
          ) : (
            <span className="text-[10px] text-slate-400 italic">Nhấn Enter để chèn</span>
          )}
        </div>

        <div className="flex items-center gap-1">
          {onRemove && (
            <button
              type="button"
              onClick={onRemove}
              className="p-1 px-1.5 rounded-lg text-rose-600 hover:bg-rose-50 text-[11px] font-semibold transition cursor-pointer flex items-center gap-0.5"
              title="Xóa Furigana của chữ này"
            >
              <Trash2 className="w-3 h-3" />
              <span>Gỡ</span>
            </button>
          )}

          <button
            type="button"
            onClick={() => {
              if (reading.trim()) onApply(reading.trim())
            }}
            disabled={!reading.trim()}
            className="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-[11px] font-bold shadow-2xs transition disabled:opacity-50 cursor-pointer flex items-center gap-1"
          >
            <Check className="w-3 h-3" />
            <span>Áp dụng</span>
          </button>
        </div>
      </div>
    </div>
  )
}

export default FuriganaPopover
