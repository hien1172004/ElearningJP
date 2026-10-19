import React from 'react'
import { Volume2, BookOpen } from 'lucide-react'
import { playSmartAudio } from '@/utils'
import type { LessonSection } from '@/types'

interface VocabSectionViewProps {
  section: LessonSection
}

export const VocabSectionView: React.FC<VocabSectionViewProps> = ({ section }) => {
  const items = section.vocabularyItems ?? []
  return (
    <div className="space-y-3">
      <h3 className="font-bold text-base text-emerald-800 flex items-center gap-2">
        <span className="w-7 h-7 rounded bg-emerald-100 text-emerald-700 flex items-center justify-center text-sm font-bold">
          {section.orderIndex}
        </span>
        {section.title}
      </h3>
      {items.length === 0 && (
        <p className="text-sm text-gray-400 py-4 text-center">Chưa có từ vựng nào trong section này.</p>
      )}
      <div className="space-y-2">
        {items.map((item) => (
          <div key={item.id} className="bg-white rounded-md border border-gray-200/80 p-4 hover:border-emerald-300 hover:shadow-sm transition">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-baseline gap-3">
                  {item.word && (
                    <span className="text-2xl font-bold text-gray-900">{item.word}</span>
                  )}
                  {item.hiragana && (
                    <span className="text-base text-gray-500">【{item.hiragana}】</span>
                  )}
                </div>
                {item.meaningVi && (
                  <p className="mt-1.5 text-base font-semibold text-indigo-950">{item.meaningVi}</p>
                )}
                {item.note && (
                  <p className="mt-1 text-xs text-gray-500">{item.note}</p>
                )}
                {(item.examples ?? []).length > 0 && (
                  <div className="mt-3 space-y-1.5">
                    {item.examples?.slice(0, 2).map((ex, idx) => (
                      <div key={idx} className="flex items-start gap-2 bg-slate-50/70 p-2.5 rounded-md text-sm">
                        <BookOpen className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
                        <div>
                          <p className="font-medium text-gray-800">{ex.exampleJp}</p>
                          <p className="text-xs text-gray-500 mt-0.5">{ex.exampleVi}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              <button
                type="button"
                onClick={() => playSmartAudio({ text: item.word ?? item.hiragana ?? '' })}
                className="p-2.5 rounded-md bg-indigo-50 text-indigo-600 hover:bg-indigo-100 hover:scale-105 active:scale-95 transition shrink-0"
                title="Nghe phát âm"
              >
                <Volume2 className="w-5 h-5" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
