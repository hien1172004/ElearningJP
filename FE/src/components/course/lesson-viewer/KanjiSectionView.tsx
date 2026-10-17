import React from 'react'
import { Volume2 } from 'lucide-react'
import { speakJapanese } from '@/utils'
import type { LessonSection } from '@/types'

interface KanjiSectionViewProps {
  section: LessonSection
}

export const KanjiSectionView: React.FC<KanjiSectionViewProps> = ({ section }) => {
  const items = section.kanjiItems ?? []
  return (
    <div className="space-y-3">
      <h3 className="font-bold text-base text-amber-800 flex items-center gap-2">
        <span className="w-7 h-7 rounded bg-amber-100 text-amber-700 flex items-center justify-center text-sm font-bold">
          {section.orderIndex}
        </span>
        {section.title}
      </h3>
      {items.length === 0 && (
        <p className="text-sm text-gray-400 py-4 text-center">Chưa có Kanji nào trong section này.</p>
      )}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        {items.map((item) => (
          <div key={item.id} className="bg-white rounded-md border border-amber-200/80 p-4 text-center hover:shadow-md transition group">
            <div className="w-14 h-14 rounded-md bg-amber-50 border border-amber-200 flex items-center justify-center text-3xl font-bold text-gray-900 font-serif mx-auto group-hover:scale-105 transition">
              {item.character ?? '?'}
            </div>
            {item.note && (
              <p className="text-xs text-gray-600 mt-2 leading-relaxed">{item.note}</p>
            )}
            {item.character && (
              <button
                type="button"
                onClick={() => speakJapanese(item.character!)}
                className="mt-2 p-1.5 rounded bg-amber-50 text-amber-600 hover:bg-amber-100 transition"
                title="Nghe phát âm"
              >
                <Volume2 className="w-4 h-4" />
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
