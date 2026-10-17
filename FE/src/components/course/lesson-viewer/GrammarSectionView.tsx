import React from 'react'
import { BookOpen } from 'lucide-react'
import type { LessonSection } from '@/types'

interface GrammarSectionViewProps {
  section: LessonSection
}

export const GrammarSectionView: React.FC<GrammarSectionViewProps> = ({ section }) => {
  const items = section.grammarItems ?? []
  return (
    <div className="space-y-3">
      <h3 className="font-bold text-base text-purple-800 flex items-center gap-2">
        <span className="w-7 h-7 rounded bg-purple-100 text-purple-700 flex items-center justify-center text-sm font-bold">
          {section.orderIndex}
        </span>
        {section.title}
      </h3>
      {items.length === 0 && (
        <p className="text-sm text-gray-400 py-4 text-center">Chưa có ngữ pháp nào trong section này.</p>
      )}
      <div className="space-y-3">
        {items.map((item) => (
          <div key={item.id} className="bg-white rounded-md border border-gray-200/80 p-5 hover:border-purple-300 hover:shadow-sm transition">
            <div className="flex items-baseline gap-3 flex-wrap">
              {item.title && (
                <span className="text-2xl font-bold text-gray-900 font-sans">{item.title}</span>
              )}
              {item.structure && (
                <span className="px-2.5 py-0.5 rounded bg-purple-50 text-purple-700 text-sm font-semibold border border-purple-200">
                  {item.structure}
                </span>
              )}
            </div>
            {item.meaningVi && (
              <p className="mt-2 text-base font-semibold text-indigo-950">{item.meaningVi}</p>
            )}
            {item.note && (
              <p className="mt-1 text-sm text-gray-500">{item.note}</p>
            )}
            {(item.examples ?? []).length > 0 && (
              <div className="mt-4 space-y-2">
                <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
                  <BookOpen className="w-4 h-4 text-purple-500" /> Ví dụ minh hoạ
                </h4>
                {(item.examples ?? []).slice(0, 3).map((ex, idx) => (
                  <div key={idx} className="flex items-start gap-3 bg-slate-50/70 p-3.5 rounded-md text-sm">
                    <div className="flex-1">
                      <p className="font-semibold text-gray-900">{ex.exampleJp}</p>
                      {ex.exampleRomaji && <p className="text-xs text-gray-400 mt-0.5">{ex.exampleRomaji}</p>}
                      {ex.exampleVi && <p className="text-sm text-gray-600 mt-1">{ex.exampleVi}</p>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
