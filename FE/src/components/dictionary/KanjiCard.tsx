import React from 'react'
import { Eye, Volume2 } from 'lucide-react'
import type { KanjiSummary } from '@/types'
import { speakJapanese } from '@/utils'

interface KanjiCardProps {
  kanji: KanjiSummary
  onOpenDetail: (character: string) => void
}

export const KanjiCard: React.FC<KanjiCardProps> = ({ kanji, onOpenDetail }) => {
  return (
    <div
      onClick={() => onOpenDetail(kanji.character)}
      className="bg-white rounded-2xl border border-gray-200/80 hover:border-indigo-400 hover:shadow-md p-5 transition-all duration-200 cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4 group"
    >
      <div className="flex items-start gap-4">
        {/* Khung chữ Hán nổi bật */}
        <div className="w-16 h-16 rounded-2xl bg-indigo-50/60 border border-indigo-200 flex items-center justify-center text-3xl font-bold text-gray-900 font-serif group-hover:scale-105 group-hover:text-indigo-600 transition shrink-0">
          {kanji.character}
        </div>

        <div className="space-y-1">
          {/* Badge cấp độ & Hán Việt */}
          <div className="flex items-center gap-2 flex-wrap">
            {kanji.hanViet && (
              <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-amber-50 text-amber-700 border border-amber-200/60 uppercase">
                {kanji.hanViet}
              </span>
            )}
            {kanji.jlptLevel && (
              <span className="px-2 py-0.5 text-xs font-bold rounded-md bg-indigo-50 text-indigo-600 border border-indigo-200/60">
                {kanji.jlptLevel}
              </span>
            )}
            <span className="text-xs text-gray-400">
              {kanji.strokeCount} nét
            </span>
          </div>

          {/* Nghĩa tiếng Việt */}
          <p className="text-base font-bold text-gray-900 group-hover:text-indigo-600 transition">
            {kanji.meaningVi}
          </p>

          {/* Âm On & Âm Kun */}
          <div className="flex items-center gap-4 text-xs text-gray-500 pt-1 flex-wrap">
            {kanji.onyomi && kanji.onyomi.length > 0 && (
              <div className="flex items-center gap-1">
                <span className="font-semibold text-indigo-700">On:</span>
                <span>{kanji.onyomi.slice(0, 3).join(', ')}</span>
              </div>
            )}
            {kanji.kunyomi && kanji.kunyomi.length > 0 && (
              <div className="flex items-center gap-1">
                <span className="font-semibold text-emerald-700">Kun:</span>
                <span>{kanji.kunyomi.slice(0, 3).join(', ')}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Nút hành động */}
      <div className="flex items-center gap-2 self-end md:self-center">
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            speakJapanese(kanji.character)
          }}
          className="p-2.5 rounded-xl bg-gray-50 text-gray-600 hover:text-indigo-600 hover:bg-indigo-50 transition"
          title="Nghe phát âm"
        >
          <Volume2 className="w-5 h-5" />
        </button>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            onOpenDetail(kanji.character)
          }}
          className="px-3.5 py-2 rounded-xl bg-indigo-50 text-indigo-600 hover:bg-indigo-600 hover:text-white font-semibold text-xs flex items-center gap-1.5 transition"
        >
          <Eye className="w-4 h-4" /> Chi tiết & Nét vẽ
        </button>
      </div>
    </div>
  )
}
