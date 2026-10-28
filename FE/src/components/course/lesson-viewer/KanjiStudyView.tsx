import React, { useState, useEffect, useMemo, useRef } from 'react'
import { Volume2, ChevronLeft, ChevronRight, BookOpen, SpeakerIcon, PenTool, Loader2 } from 'lucide-react'
import { speakJapanese } from '@/utils'
import { useKanjiItemDetail, useKanjiDetail } from '@/hooks'
import { KanjiPracticeModal } from '@/components/dictionary'
import type { LessonKanjiItem } from '@/types'

interface KanjiStudyViewProps {
  /** Danh sách kanji items đã load sẵn từ lesson.sections[].kanjiItems[]. */
  items: LessonKanjiItem[]
  /** Được gọi 1 lần khi user đã mở tới trang cuối cùng. */
  onSectionDone?: () => void
}

/**
 * Chế độ học chữ Hán "phân trang":
 * - Mỗi trang = 1 chữ Hán.
 * - Trên: ô chữ Hán (lớn) + Hán Việt.
 * - Giữa: thông số (nghĩa, âm on/kun, số nét, JLPT...).
 * - Dưới: related vocabulary (từ liên quan).
 * - Có nút Next/Prev và phân trang "1/N" ở dưới.
 */
export const KanjiStudyView: React.FC<KanjiStudyViewProps> = ({ items, onSectionDone }) => {
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
    // Reset khi items đổi (bài học khác)
    if (page === 0 && total > 0) {
      reportedDoneRef.current = false
    }
  }, [page, total, onSectionDone])

  if (total === 0) {
    return (
      <div className="text-center py-16 text-sm text-gray-400">
        Bài học này chưa có chữ Hán nào.
      </div>
    )
  }

  const safePage = Math.max(0, Math.min(page, total - 1))
  const currentItem = sortedItems[safePage]

  return (
    <div className="space-y-6">
      {/* `key` theo item.id → mỗi chữ Hán là 1 instance mới, tránh giữ trạng thái
          (modal đang mở, nét vẽ cũ) của chữ trước khi chuyển trang. */}
      <KanjiStudyCard key={currentItem.id} item={currentItem} />

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

interface KanjiStudyCardProps {
  item: LessonKanjiItem
}

const KanjiStudyCard: React.FC<KanjiStudyCardProps> = ({ item }) => {
  // Nếu item chưa có relatedVocabulary (BE chưa truyền bundle vào mapper / DB rỗng) →
  // tự gọi endpoint riêng /lesson-kanji-items/kanji-items/{id} để lấy.
  const needFetch = !item.relatedVocabulary || item.relatedVocabulary.length === 0
  const { data: detail } = useKanjiItemDetail(needFetch ? item.id : undefined)
  const relatedVocabs = item.relatedVocabulary?.length
    ? item.relatedVocabulary
    : (detail?.relatedVocabulary ?? [])

  // ===== Luyện viết AI (modal KanjiPracticeModal) =====
  const [isPracticeOpen, setIsPracticeOpen] = useState<boolean>(false)
  // `LessonKanjiItem` không chứa field `strokes` → gọi endpoint từ điển để lấy nét chuẩn.
  // Fetch ngay khi thẻ hiển thị (mỗi trang chỉ có 1 chữ Hán) để khi user bấm nút thì modal
  // mở lên đã có sẵn nét chuẩn, không nhấp nháy "0 nét". React Query cache 5 phút.
  const { data: kanjiDetail, isLoading: isKanjiLoading } = useKanjiDetail(item.character)

  return (
    <>
      {/* Phần trên: ô chữ Hán + Hán Việt */}
      <div className="bg-gradient-to-br from-amber-50 to-orange-50 rounded-md border-2 border-amber-200/80 p-8 md:p-10 shadow-sm">
        <div className="flex flex-col sm:flex-row items-center gap-6 md:gap-8">
          <div className="w-40 h-40 md:w-48 md:h-48 shrink-0 rounded-md bg-white border-2 border-amber-300 flex items-center justify-center shadow-inner">
            <span className="text-7xl md:text-8xl font-bold text-gray-900 font-serif leading-none">
              {item.character ?? '?'}
            </span>
          </div>
          <div className="flex-1 text-center sm:text-left space-y-2">
            {item.hanViet && (
              <p className="text-2xl md:text-3xl font-extrabold text-amber-800">
                {item.hanViet}
              </p>
            )}
            {item.meaningVi && (
              <p className="text-base md:text-lg text-gray-700 font-medium">
                {item.meaningVi}
              </p>
            )}
            {item.meaningEn && (
              <p className="text-sm text-gray-500 italic">{item.meaningEn}</p>
            )}
            {item.character && (
              <div className="mt-3 flex flex-wrap items-center gap-2 justify-center sm:justify-start">
                <button
                  type="button"
                  onClick={() => speakJapanese(item.character!)}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-md bg-amber-200 text-amber-800 hover:bg-amber-300 transition text-sm font-semibold"
                  title="Nghe phát âm"
                >
                  <Volume2 className="w-4 h-4" />
                  Phát âm
                </button>

                {/* Nút luyện viết AI — mở modal luyện viết & chấm điểm nét (giống trang từ điển) */}
                <button
                  type="button"
                  onClick={() => setIsPracticeOpen(true)}
                  disabled={isKanjiLoading}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-md bg-amber-500 hover:bg-amber-600 active:scale-95 transition text-sm font-semibold text-white shadow-xs cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
                  title="Luyện viết & Chấm điểm nét AI"
                >
                  {isKanjiLoading ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <PenTool className="w-4 h-4" />
                  )}
                  Luyện viết AI
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Thông số chữ Hán — không có tiêu đề, chia 2 cột:
          Cột trái: cấp độ JLPT + số nét + ghi chú
          Cột phải: onyomi + kunyomi + bộ thủ */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 space-y-3">
          <InfoRow
            label="Cấp độ JLPT"
            value={item.jlptLevel ? `${item.jlptLevel}` : '—'}
          />
          <InfoRow
            label="Số nét"
            value={item.strokeCount != null ? String(item.strokeCount) : '—'}
          />
          <InfoRow
            label="Bộ thủ"
            value={item.radicals && item.radicals.length > 0 ? item.radicals.join(', ') : '—'}
          />
          <InfoRow label="Ghi chú" value={item.note || '—'} />
        </div>
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 space-y-3">
          <h4 className="font-bold text-base text-gray-900 flex items-center gap-2">
            <SpeakerIcon className="w-5 h-5 text-indigo-600" />
            Cách đọc
          </h4>
          <InfoRow
            label="On'yomi"
            value={item.onyomi && item.onyomi.length > 0 ? item.onyomi.join(' , ') : '—'}
          />
          <InfoRow
            label="Kun'yomi"
            value={item.kunyomi && item.kunyomi.length > 0 ? item.kunyomi.join(' , ') : '—'}
          />

        </div>
      </div>

      {/* Từ vựng liên quan */}
      {relatedVocabs.length > 0 && (
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-5 md:p-6 space-y-3">
          <h4 className="font-bold text-base text-gray-900 flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            Từ vựng liên quan ({relatedVocabs.length})
          </h4>
          <div className="space-y-2">
            {relatedVocabs.map((v) => (
              <div
                key={`${v.vocabId}-${v.id ?? ''}`}
                className="flex items-center justify-between gap-4 p-3 rounded-md border border-gray-200 hover:border-indigo-300 hover:bg-indigo-50/30 transition"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-baseline gap-2 flex-wrap">
                    <span className="text-xl font-bold text-gray-900">{v.word}</span>
                    {v.hiragana && (
                      <span className="text-lg text-gray-500">【{v.hiragana}】</span>
                    )}
                  </div>
                  {v.meaningVi && (
                    <p className="text-lg text-indigo-950 font-medium mt-0.5">{v.meaningVi}</p>
                  )}
                </div>
                {v.word && (
                  <button
                    type="button"
                    onClick={() => speakJapanese(v.word!)}
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

      {/* Modal luyện viết & chấm điểm nét AI — cùng component với trang từ điển.
          `strokes` lấy từ từ điển (BE) vì LessonKanjiItem không chứa nét vẽ. */}
      {item.character && (
        <KanjiPracticeModal
          isOpen={isPracticeOpen}
          onClose={() => setIsPracticeOpen(false)}
          character={item.character}
          meaningVi={item.meaningVi}
          hanViet={item.hanViet}
          strokes={kanjiDetail?.strokes ?? []}
        />
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

interface PaginationFooterProps {
  page: number
  total: number
  onPrev: () => void
  onNext: () => void
}
export const PaginationFooter: React.FC<PaginationFooterProps> = ({
  page,
  total,
  onPrev,
  onNext,
}) => {
  return (
    <div className="flex items-center justify-between gap-3 pt-2">
      <button
        type="button"
        onClick={onPrev}
        disabled={page <= 0}
        className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-md border border-gray-200 bg-white text-sm font-semibold text-gray-700 hover:border-indigo-300 hover:text-indigo-600 transition disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:border-gray-200 disabled:hover:text-gray-700"
      >
        <ChevronLeft className="w-4 h-4" />
        Quay lại
      </button>
      <span className="text-sm font-semibold text-gray-600 tabular-nums">
        {page + 1} / {total}
      </span>
      <button
        type="button"
        onClick={onNext}
        disabled={page >= total - 1}
        className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-md bg-indigo-600 text-white text-sm font-semibold hover:bg-indigo-700 transition disabled:opacity-40 disabled:cursor-not-allowed"
      >
        Tiếp theo
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  )
}
