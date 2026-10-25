import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Volume2, BookOpen } from 'lucide-react'
import { playSmartAudio } from '@/utils'
import { useGrammarItemDetail } from '@/hooks'
import type { LessonGrammarItem } from '@/types'
import { PaginationFooter } from './KanjiStudyView'

interface GrammarStudyViewProps {
  /** Danh sách grammar items đã load sẵn từ lesson.sections[].grammarItems[]. */
  items: LessonGrammarItem[]
  /** Được gọi 1 lần khi user đã mở tới trang cuối cùng. */
  onSectionDone?: () => void
}

/**
 * Chế độ học ngữ pháp "phân trang":
 * - Mỗi trang = 1 mẫu ngữ pháp.
 * - Trên: title + cấu trúc + nghĩa.
 * - Giữa: thông tin chi tiết chia 2 cột.
 * - Dưới: ví dụ minh họa.
 * - Có nút Next/Prev và phân trang "1/N" ở dưới.
 */
export const GrammarStudyView: React.FC<GrammarStudyViewProps> = ({ items, onSectionDone }) => {
  // Sắp xếp theo orderIndex
  const sortedItems = useMemo(
    () => [...items].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)),
    [items],
  )
  const [page, setPage] = useState(0)
  const total = sortedItems.length

  // Reset page khi items đổi
  useEffect(() => {
    setPage(0)
  }, [items])

  // Báo "đã xem xong" khi user mở tới trang cuối (chỉ phát 1 lần)
  const reportedDoneRef = useRef(false)
  useEffect(() => {
    if (total > 0 && page >= total - 1 && !reportedDoneRef.current) {
      reportedDoneRef.current = true
      onSectionDone?.()
    }
    if (page === 0 && total > 0) {
      reportedDoneRef.current = false
    }
  }, [page, total, onSectionDone])

  if (total === 0) {
    return (
      <div className="text-center py-16 text-sm text-gray-400">
        Bài học này chưa có ngữ pháp nào.
      </div>
    )
  }

  const safePage = Math.max(0, Math.min(page, total - 1))
  const currentItem = sortedItems[safePage]

  return (
    <div className="space-y-6">
      <GrammarStudyCard item={currentItem} />

      {/* Phân trang */}
      <PaginationFooter
        page={safePage}
        total={total}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => Math.min(total - 1, p + 1))}
      />
    </div>
  )
}

interface GrammarStudyCardProps {
  item: LessonGrammarItem
}

const GrammarStudyCard: React.FC<GrammarStudyCardProps> = ({ item }) => {
  // Nếu item chưa có examples (BE chưa truyền bundle vào mapper / DB rỗng) →
  // tự gọi endpoint riêng /lesson-grammar-items/grammar-items/{id} để lấy.
  const needFetch = !item.examples || item.examples.length === 0
  const { data: detail } = useGrammarItemDetail(needFetch ? item.id : undefined)
  const examples = item.examples?.length
    ? item.examples
    : (detail?.examples ?? [])

  const displayStructure = item.displayStructure ?? item.structure ?? ''
  const displayMeaning = item.displayMeaning ?? item.meaningVi ?? ''

  return (
    <>
      {/* Phần trên: title + cấu trúc + nghĩa */}
      <div className="bg-gradient-to-br from-rose-50 to-pink-50 rounded-md border-2 border-rose-200/80 p-8 md:p-10 shadow-sm">
        <div className="space-y-4 text-center">
          {item.title && (
            <h3 className="text-2xl md:text-3xl font-extrabold text-rose-800">
              {item.title}
            </h3>
          )}
          {displayStructure && (
            <div className="inline-block bg-white border-2 border-rose-200 rounded-md px-5 py-3 shadow-inner">
              <p className="text-lg md:text-xl font-bold text-gray-900 font-mono">
                {displayStructure}
              </p>
            </div>
          )}
          {displayMeaning && (
            <p className="text-base md:text-lg text-gray-700 font-medium mx-auto">
              {displayMeaning}
            </p>
          )}
        </div>
      </div>

      {/* Thông số ngữ pháp — chia 2 cột:
          Cột trái: cấp độ JLPT + nghĩa
          Cột phải: cách dùng + ghi chú */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 space-y-3">
          <InfoRow
            label="Cấp độ JLPT"
            value={item.jlptLevel ? `JLPT ${item.jlptLevel}` : '—'}
          />
          <InfoRow label="Cấu trúc" value={displayStructure || '—'} />
          <InfoRow label="Nghĩa" value={displayMeaning || '—'} />
          <InfoRow label="Ghi chú" value={item.note || '—'} />
        </div>
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 space-y-3">
          <InfoRow label="Cách dùng" value={item.usageNotes || '—'} />
        </div>
      </div>

      {/* Ví dụ */}
      {examples.length > 0 && (
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 md:p-6 space-y-3">
          <h4 className="font-bold text-base text-gray-900 flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            Ví dụ ({examples.length})
          </h4>
          <div className="space-y-2">
            {examples.map((ex, idx) => (
              <div
                key={ex.id ?? idx}
                className="flex items-start gap-3 p-3 rounded-md border border-gray-200 hover:border-rose-300 hover:bg-rose-50/30 transition"
              >
                <span className="text-lg font-bold text-rose-500 w-5 shrink-0 pt-0.5">
                  {idx + 1}.
                </span>
                <div className="flex-1 min-w-0">
                  <p className="text-xl font-semibold text-gray-900">{ex.exampleJp}</p>
                  {ex.exampleRomaji && (
                    <p className="text-lg text-gray-500 italic mt-0.5">
                      {ex.exampleRomaji}
                    </p>
                  )}
                  {ex.exampleVi && (
                    <p className="text-lg text-rose-950 font-medium mt-1">
                      {ex.exampleVi}
                    </p>
                  )}
                </div>
                {ex.exampleJp && (
                  <button
                    type="button"
                    onClick={() => playSmartAudio({ text: ex.exampleJp! })}
                    className="p-2 rounded bg-rose-50 text-rose-600 hover:bg-rose-100 transition shrink-0"
                    title="Nghe phát âm"
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
  )
}

const InfoRow: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div className="flex items-start gap-3">
    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide min-w-[100px]">
      {label}
    </span>
    <span className="text-sm text-gray-900 font-medium flex-1">{value}</span>
  </div>
)
