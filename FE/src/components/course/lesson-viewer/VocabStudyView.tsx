import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Volume2, BookOpen } from 'lucide-react'
import { playSmartAudio } from '@/utils'
import { useVocabItemDetail } from '@/hooks'
import type { LessonVocabularyItem } from '@/types'
import { PaginationFooter } from './KanjiStudyView'

interface VocabStudyViewProps {
  /** Danh sách vocab items đã load sẵn từ lesson.sections[].vocabularyItems[]. */
  items: LessonVocabularyItem[]
  /** Được gọi 1 lần khi user đã mở tới trang cuối cùng. */
  onSectionDone?: () => void
}

/**
 * Chế độ học từ vựng "phân trang":
 * - Mỗi trang = 1 từ vựng.
 * - Trên: ô từ (lớn) + hiragana + romaji.
 * - Giữa: nghĩa + Hán Việt + loại từ.
 * - Dưới: thông tin chi tiết (JLPT, romaji, nghĩa EN, ghi chú).
 * - Cuối: ví dụ minh họa.
 * - Có nút Next/Prev và phân trang "1/N" ở dưới.
 */
export const VocabStudyView: React.FC<VocabStudyViewProps> = ({ items, onSectionDone }) => {
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
        Bài học này chưa có từ vựng nào.
      </div>
    )
  }

  const safePage = Math.max(0, Math.min(page, total - 1))
  const currentItem = sortedItems[safePage]

  return (
    <div className="space-y-6">
      <VocabStudyCard item={currentItem} />

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

interface VocabStudyCardProps {
  item: LessonVocabularyItem
}

const VocabStudyCard: React.FC<VocabStudyCardProps> = ({ item }) => {
  // Nếu item chưa có examples (BE chưa truyền bundle vào mapper / DB rỗng) →
  // tự gọi endpoint riêng /lesson-vocabulary-items/vocabulary-items/{id} để lấy.
  const needFetch = !item.examples || item.examples.length === 0
  const { data: detail } = useVocabItemDetail(needFetch ? item.id : undefined)
  const examples = item.examples?.length
    ? item.examples
    : (detail?.examples ?? [])

  const displayWord = item.word ?? ''
  const displayHiragana = item.displayHiragana ?? item.hiragana ?? ''
  const displayRomaji = item.displayRomaji ?? item.romaji ?? ''
  const displayMeaning = item.displayMeaning ?? item.meaningVi ?? ''

  return (
    <>
      {/* Phần trên: từ vựng + nghĩa */}
      <div className="bg-gradient-to-br from-emerald-50 to-teal-50 rounded-md border-2 border-emerald-200/80 p-8 md:p-10 shadow-sm">
        <div className="flex flex-col sm:flex-row items-center gap-6 md:gap-8">
          <div className="w-40 h-40 md:w-48 md:h-48 shrink-0 rounded-md bg-white border-2 border-emerald-300 flex flex-col items-center justify-center shadow-inner px-3 text-center">
            <span className="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">
              {displayWord || '?'}
            </span>
            {displayHiragana && (
              <span className="text-base text-gray-500 mt-2">【{displayHiragana}】</span>
            )}
            {displayRomaji && (
              <span className="text-xs text-gray-400 mt-1 italic">{displayRomaji}</span>
            )}
          </div>
          <div className="flex-1 text-center sm:text-left space-y-2">
            {displayMeaning && (
              <p className="text-xl md:text-2xl font-extrabold text-emerald-800 leading-snug">
                {displayMeaning}
              </p>
            )}
            {/* {item.hanViet && (
              <p className="text-base text-gray-600">Hán Việt: {item.hanViet}</p>
            )} */}
            {/* {item.partOfSpeech && (
              <p className="text-xs text-gray-500">Loại từ: {item.partOfSpeech}</p>
            )} */}
            {displayWord && (
              <button
                type="button"
                onClick={() => playSmartAudio({ text: displayWord })}
                className="mt-3 inline-flex items-center gap-2 px-4 py-2 rounded-md bg-emerald-200 text-emerald-800 hover:bg-emerald-300 transition text-sm font-semibold"
                title="Nghe phát âm"
              >
                <Volume2 className="w-4 h-4" />
                Phát âm
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Thông số từ vựng — chia 2 cột:
          Cột trái: JLPT + Hán Việt + loại từ
          Cột phải: hiragana + romaji + ghi chú */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 space-y-3">
          <InfoRow
            label="Cấp độ JLPT"
            value={item.jlptLevel ? `JLPT ${item.jlptLevel}` : '—'}
          />
          <InfoRow label="Hán Việt" value={item.hanViet || '—'} />
          <InfoRow label="Loại từ" value={item.partOfSpeech || '—'} />
          <InfoRow label="Nghĩa (EN)" value={item.meaningEn || '—'} />
          <InfoRow label="Ghi chú" value={item.note || '—'} />

        </div>
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 space-y-3">
          <h4 className="font-bold text-base text-gray-900 flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            Cách viết
          </h4>
          <InfoRow label="Hiragana" value={displayHiragana || '—'} />
          <InfoRow label="Romaji" value={displayRomaji || '—'} />
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
                className="flex items-start gap-3 p-3 rounded-md border border-gray-200 hover:border-indigo-300 hover:bg-indigo-50/30 transition"
              >
                <span className="text-lg font-bold text-indigo-500 w-5 shrink-0 pt-0.5">
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
                    <p className="text-lg text-indigo-950 font-medium mt-1">
                      {ex.exampleVi}
                    </p>
                  )}
                </div>
                {ex.exampleJp && (
                  <button
                    type="button"
                    onClick={() => playSmartAudio({ text: ex.exampleJp! })}
                    className="p-2 rounded bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition shrink-0"
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
