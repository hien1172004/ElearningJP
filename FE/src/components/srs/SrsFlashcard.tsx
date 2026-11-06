import React from 'react'
import { RotateCw, Volume2 } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { SrsDueItem, SrsItemType } from '@/types'

const ITEM_TYPE_LABEL: Record<SrsItemType, string> = {
  VOCABULARY: 'Từ vựng',
  CHARACTER: 'Chữ Hán',
  GRAMMAR: 'Ngữ pháp',
}

interface SrsFlashcardProps {
  item: SrsDueItem
  flipped: boolean
  onFlip: () => void
}

/**
 * SrsFlashcard — thẻ lật 3D (front/back).
 *
 * - Mặt trước: từ / chữ Hán / điểm ngữ pháp + cách đọc + nút phát âm.
 * - Mặt sau: nghĩa tiếng Việt + cách đọc + ví dụ song ngữ.
 */
export const SrsFlashcard: React.FC<SrsFlashcardProps> = ({ item, flipped, onFlip }) => {
  const playAudio = () => {
    if (!item.audioUrl) return
    const audio = new Audio(item.audioUrl)
    void audio.play().catch(() => undefined)
  }

  return (
    <div className="w-full">
      <div className="relative w-full max-w-2xl mx-auto [perspective:1400px]">
        <div
          role="button"
          tabIndex={0}
          onClick={onFlip}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault()
              onFlip()
            }
          }}
          aria-label={flipped ? 'Lật lại mặt trước' : 'Lật thẻ để xem nghĩa'}
          className={cn(
            'relative w-full h-[380px] sm:h-[420px] outline-none cursor-pointer',
            '[transform-style:preserve-3d] transition-transform duration-500',
            flipped && '[transform:rotateY(180deg)]',
          )}
        >
          {/* ===== MẶT TRƯỚC ===== */}
          <div className="absolute inset-0 rounded-3xl bg-surface border border-slate-200 shadow-sm p-6 sm:p-8 flex flex-col [backface-visibility:hidden]">
            <div className="flex items-center justify-between">
              <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-semibold">
                {ITEM_TYPE_LABEL[item.itemType]}
              </span>
              {item.audioUrl && (
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    playAudio()
                  }}
                  className="p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
                  aria-label="Phát âm"
                >
                  <Volume2 className="w-5 h-5" />
                </button>
              )}
            </div>

            <div className="flex-1 flex flex-col items-center justify-center gap-3">
              <div className="font-serif text-4xl sm:text-5xl text-slate-900 font-bold text-center">
                {item.frontText}
              </div>
              {item.reading && (
                <div className="text-slate-500 text-base sm:text-lg text-center">
                  {item.reading}
                </div>
              )}
            </div>

            <div className="text-center text-xs text-slate-400">Bấm vào thẻ để lật</div>
          </div>

          {/* ===== MẶT SAU ===== */}
          <div className="absolute inset-0 rounded-3xl bg-surface border border-slate-200 shadow-sm p-6 sm:p-8 flex flex-col [backface-visibility:hidden] [transform:rotateY(180deg)] overflow-y-auto">
            <div className="flex items-center justify-between">
              <span className="inline-flex items-center px-2.5 py-1 rounded-full bg-slate-100 text-slate-500 text-xs font-semibold">
                Nghĩa
              </span>
              <RotateCw className="w-4 h-4 text-slate-400" />
            </div>

            <div className="flex-1 flex flex-col justify-center gap-4 py-4">
              <div className="text-xl sm:text-2xl text-slate-900 font-bold text-center">
                {item.backMeaning}
              </div>
              {item.reading && (
                <div className="text-slate-500 text-sm sm:text-base text-center">
                  {item.reading}
                </div>
              )}
              {item.exampleSentence && (
                <div className="mt-2 p-4 rounded-2xl bg-slate-50 border border-slate-100">
                  <div className="text-slate-700 text-sm sm:text-base">
                    {item.exampleSentence}
                  </div>
                  {item.exampleTranslation && (
                    <div className="text-slate-400 text-xs sm:text-sm mt-1">
                      {item.exampleTranslation}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
