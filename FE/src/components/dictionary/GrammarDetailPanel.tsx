import React, { useState, useEffect } from 'react'
import { Volume2, BookmarkPlus, Check, BookOpen } from 'lucide-react'
import type { GrammarDetail } from '@/types'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'

interface GrammarDetailPanelProps {
  grammarId: number
}

export const GrammarDetailPanel: React.FC<GrammarDetailPanelProps> = ({ grammarId }) => {
  const [detail, setDetail] = useState<GrammarDetail | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [isAddedToSrs, setIsAddedToSrs] = useState<boolean>(false)

  useEffect(() => {
    let isMounted = true
    setIsLoading(true)
    setIsAddedToSrs(false)

    dictionaryService
      .getGrammarDetail(grammarId)
      .then((res) => {
        if (isMounted && res.data) {
          setDetail(res.data)
        }
      })
      .catch((err) => console.error('Lỗi tải chi tiết ngữ pháp:', err))
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [grammarId])

  if (isLoading) {
    return (
      <div className="bg-white rounded-3xl p-12 border border-gray-200/80 shadow-xs text-center text-gray-500 animate-pulse space-y-3">
        <div className="w-10 h-10 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm font-medium">Đang tải giải thích cấu trúc ngữ pháp...</p>
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="bg-white rounded-3xl p-10 border border-gray-200 text-center text-gray-500">
        Không tìm thấy thông tin điểm ngữ pháp.
      </div>
    )
  }

  const handleAddToSrs = async () => {
    if (isAddedToSrs) return
    try {
      await dictionaryService.addToSrs('GRAMMAR', detail.id)
      setIsAddedToSrs(true)
    } catch (err: any) {
      if (err?.response?.data?.code === 3002) {
        setIsAddedToSrs(true)
      }
    }
  }

  const parsedExamples: Array<{ jp?: string; vi?: string }> = (() => {
    if (!detail.examplesJson) return []
    try {
      if (Array.isArray(detail.examplesJson)) return detail.examplesJson
      if (typeof detail.examplesJson === 'string') return JSON.parse(detail.examplesJson)
      return []
    } catch {
      return []
    }
  })()

  return (
    <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-6 md:p-8 space-y-6">
      {/* 1. Header điểm ngữ pháp */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 border-b border-gray-100 pb-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2 flex-wrap">
            {detail.jlptLevel && (
              <span className="px-3 py-1 text-xs font-bold rounded-lg bg-purple-50 text-purple-700 border border-purple-200/80">
                JLPT {detail.jlptLevel}
              </span>
            )}
          </div>

          <h2 className="text-3xl md:text-4xl font-extrabold text-gray-900 tracking-tight font-sans">
            {detail.title}
          </h2>

          <div className="pt-1">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">
              Ý nghĩa
            </span>
            <p className="text-xl font-bold text-purple-950 mt-0.5">👉 {detail.meaningVi}</p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleAddToSrs}
          disabled={isAddedToSrs}
          className={`px-4 py-3.5 rounded-2xl text-sm font-semibold flex items-center gap-2 transition shrink-0 self-start ${
            isAddedToSrs
              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
              : 'bg-gray-100 hover:bg-gray-200 text-gray-800'
          }`}
        >
          {isAddedToSrs ? <Check className="w-5 h-5" /> : <BookmarkPlus className="w-5 h-5" />}
          <span>{isAddedToSrs ? 'Đã lưu SRS' : 'Lưu SRS'}</span>
        </button>
      </div>

      {/* 2. Cấu trúc ngữ pháp */}
      <div className="bg-slate-50/80 rounded-2xl p-4 border border-gray-200/80 space-y-2">
        <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider">
          Cấu trúc liên kết
        </h4>
        <div className="bg-white p-3.5 rounded-xl border border-gray-200 text-base font-mono font-bold text-indigo-700">
          {detail.structure}
        </div>
      </div>

      {/* 3. Giải thích cách dùng & Lưu ý */}
      {detail.usageNotes && (
        <div className="space-y-2">
          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider">
            Giải thích chi tiết & Hướng dẫn sử dụng
          </h4>
          <div className="bg-white p-4 rounded-2xl border border-gray-200/80 text-sm text-gray-800 leading-relaxed whitespace-pre-line shadow-2xs">
            {detail.usageNotes}
          </div>
        </div>
      )}

      {/* 4. Các câu ví dụ minh họa */}
      {parsedExamples.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
            <BookOpen className="w-4 h-4 text-purple-600" /> Các mẫu câu ví dụ ({parsedExamples.length})
          </h4>

          <div className="space-y-2.5">
            {parsedExamples.map((ex, idx) => (
              <div
                key={idx}
                className="bg-slate-50/60 p-4 rounded-2xl border border-gray-200/70 flex items-start justify-between gap-3 hover:bg-purple-50/30 transition group"
              >
                <div className="space-y-1">
                  <p className="text-base font-bold text-gray-900 group-hover:text-purple-700 transition">
                    {ex.jp}
                  </p>
                  <p className="text-sm text-gray-700">{ex.vi}</p>
                </div>

                {ex.jp && (
                  <button
                    type="button"
                    onClick={() => playSmartAudio({ text: ex.jp || '' })}
                    className="p-2 text-gray-400 hover:text-purple-600 hover:bg-white rounded-xl transition shrink-0"
                    title="Nghe phát âm"
                  >
                    <Volume2 className="w-5 h-5" />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
