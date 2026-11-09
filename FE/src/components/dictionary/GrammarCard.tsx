import React, { useState } from 'react'
import { BookmarkPlus, Check, ChevronDown, ChevronUp, BookOpen, Volume2, Plus } from 'lucide-react'
import type { GrammarSummary, GrammarDetail } from '@/types'
import { dictionaryService } from '@/services'
import { speakJapanese } from '@/utils'
import { useAddToSrs, useSrsSavedIds } from '@/hooks'
import { AddWordToNotebookModal, type AddWordTargetData } from '@/components/notebook/AddWordToNotebookModal'

interface GrammarCardProps {
  grammar: GrammarSummary
}

export const GrammarCard: React.FC<GrammarCardProps> = ({ grammar }) => {
  const [detail, setDetail] = useState<GrammarDetail | null>(null)
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false)
  const { isSaved } = useSrsSavedIds()
  const isAddedToSrs = isSaved('GRAMMAR', grammar.id)
  const addToSrs = useAddToSrs()
  const [isAddToNotebookOpen, setIsAddToNotebookOpen] = useState<boolean>(false)

  const handleToggleExpand = async () => {
    if (!isExpanded && !detail) {
      setIsLoadingDetail(true)
      try {
        const res = await dictionaryService.getGrammarDetail(grammar.id)
        if (res.data) {
          setDetail(res.data)
        }
      } catch (err) {
        console.error('Không thể tải chi tiết ngữ pháp:', err)
      } finally {
        setIsLoadingDetail(false)
      }
    }
    setIsExpanded((prev) => !prev)
  }

  const handleAddToSrs = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (isAddedToSrs) return
    try {
      await addToSrs.mutateAsync({ type: 'GRAMMAR', id: grammar.id })
    } catch {
      /* ignore */
    }
  }

  const grammarWordData: AddWordTargetData = {
    word: grammar.title,
    reading: grammar.structure || '',
    meaning: grammar.meaningVi || '',
    partOfSpeech: 'Ngữ pháp',
    itemType: 'GRAMMAR',
    grammarId: grammar.id,
  }
  // Parse examples json nếu có
  const parsedExamples: Array<{ jp?: string; vi?: string }> = (() => {
    if (!detail?.examplesJson) return []
    try {
      if (Array.isArray(detail.examplesJson)) return detail.examplesJson
      if (typeof detail.examplesJson === 'string') return JSON.parse(detail.examplesJson)
      return []
    } catch {
      return []
    }
  })()

  return (
    <div className="bg-surface rounded-md border border-gray-200/80 hover:border-indigo-300 hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Header tóm tắt */}
      <div className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex-1 space-y-2">
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-purple-50 text-purple-700 border border-purple-200/60">
              {grammar.jlptLevel}
            </span>
            <span className="text-xl font-bold text-gray-900 font-sans">
              {grammar.title}
            </span>
          </div>

          {/* Cấu trúc ngữ pháp */}
          <div className="inline-block bg-slate-100 px-3 py-1.5 rounded-md border border-slate-200/60 text-sm font-mono font-semibold text-slate-800">
            {grammar.structure}
          </div>

          {/* Nghĩa tiếng Việt */}
          <p className="text-sm font-semibold text-gray-700">
            👉 {grammar.meaningVi}
          </p>
        </div>

        {/* Nút hành động */}
        <div className="flex items-center gap-2 self-start md:self-center">
          <button
            type="button"
            onClick={handleAddToSrs}
            disabled={isAddedToSrs || addToSrs.isPending}
            className={`px-3 py-2 rounded-md text-xs font-semibold flex items-center gap-1.5 transition ${
              isAddedToSrs
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 cursor-default'
                : 'bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 cursor-pointer'
            }`}
            title={isAddedToSrs ? 'Đã thêm vào SRS' : 'Thêm vào SRS để ôn tập'}
          >
            {isAddedToSrs ? <Check className="w-4 h-4" /> : <BookmarkPlus className="w-4 h-4" />}
            <span>{isAddedToSrs ? 'Đã lưu SRS' : 'Lưu SRS'}</span>
          </button>

          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              setIsAddToNotebookOpen(true)
            }}
            className="px-3 py-2 rounded-md text-xs font-semibold flex items-center gap-1.5 transition bg-gray-100 hover:bg-indigo-50 hover:text-indigo-600 text-gray-700 cursor-pointer"
            title="Thêm vào sổ tay"
          >
            <Plus className="w-4 h-4" />
            <span>Sổ tay</span>
          </button>

          <button
            type="button"
            onClick={handleToggleExpand}
            className="p-2.5 rounded-md text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition cursor-pointer"
            title={isExpanded ? 'Thu gọn' : 'Xem chi tiết & ví dụ'}
          >
            {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Chi tiết mở rộng (Usage notes & Examples) */}
      {isExpanded && (
        <div className="bg-slate-50/70 border-t border-gray-100 p-5 space-y-4">
          {isLoadingDetail ? (
            <div className="text-center py-4 text-sm text-gray-500 animate-pulse">
              Đang tải giải thích & câu ví dụ...
            </div>
          ) : (
            <>
              {/* Giải thích cách dùng */}
              {detail?.usageNotes && (
                <div>
                  <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1.5">
                    Giải thích cách dùng & Lưu ý
                  </h4>
                  <div className="bg-surface p-3.5 rounded-md border border-gray-200/80 text-sm text-gray-800 leading-relaxed whitespace-pre-line">
                    {detail.usageNotes}
                  </div>
                </div>
              )}

              {/* Danh sách câu ví dụ mẫu */}
              {parsedExamples.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4 text-purple-600" /> Các câu ví dụ mẫu
                  </h4>
                  <div className="space-y-2.5">
                    {parsedExamples.map((ex, idx) => (
                      <div
                        key={idx}
                        className="bg-surface p-3.5 rounded-md border border-gray-200/80 flex items-start justify-between gap-3 group"
                      >
                        <div>
                          <p className="text-sm font-semibold text-gray-900 group-hover:text-purple-700 transition">
                            {ex.jp}
                          </p>
                          <p className="text-xs text-gray-600 mt-1">{ex.vi}</p>
                        </div>
                        {ex.jp && (
                          <button
                            type="button"
                            onClick={() => speakJapanese(ex.jp || '')}
                            className="p-1.5 text-gray-400 hover:text-purple-600 rounded hover:bg-gray-100 transition shrink-0"
                            title="Nghe câu ví dụ"
                          >
                            <Volume2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

      <AddWordToNotebookModal
        open={isAddToNotebookOpen}
        wordData={grammarWordData}
        onClose={() => setIsAddToNotebookOpen(false)}
      />
    </div>
  )
}
