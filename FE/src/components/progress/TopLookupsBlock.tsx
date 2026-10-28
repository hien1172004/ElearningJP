import React from 'react'
import { Button } from 'antd'
import { BookOpen, ChevronRight, Flame, Search, Sparkles } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useTopLookups } from '@/hooks/useTopLookups'
import type { TopLookup } from '@/types'

/**
 * TopLookupsBlock — Khối 4: danh sách "Từ đã tra nhiều nhất".
 *
 * Gộp chung cả 3 loại đối tượng tra cứu (từ vựng / Hán tự / ngữ pháp) vào một
 * danh sách chip xếp theo số lần tra giảm dần — phản ánh đúng thói quen tra cứu.
 *
 * <b>Phạm vi: toàn bộ lịch sử</b> (không giới hạn 30 ngày) — chỉ 30 từ đứng đầu
 * mới được hiển thị.
 *
 * Mỗi chip là 1 nút bấm → mở từ điển tra lại đúng từ đó.
 *
 * ```tsx
 * <TopLookupsBlock />
 * ```
 */

// --------------------------------------------------------------------------
// Loading / Empty
// --------------------------------------------------------------------------
const SkeletonBlock: React.FC = () => (
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-4">
    <div className="animate-pulse space-y-2.5">
      <div className="h-5 w-44 bg-gray-200 rounded" />
      <div className="flex flex-wrap gap-1.5">
        {Array.from({ length: 14 }).map((_, i) => (
          <div
            key={i}
            className="h-7 bg-gray-100 rounded-md"
            style={{ width: `${52 + ((i * 37) % 58)}px` }}
          />
        ))}
      </div>
    </div>
  </div>
)

const EmptyBlock: React.FC = () => (
  <div className="py-6 text-center">
    <span className="inline-flex w-11 h-11 rounded-full bg-indigo-50 text-indigo-400 items-center justify-center">
      <Search className="w-5 h-5" />
    </span>
    <p className="font-bold text-gray-900 text-sm mt-2">Chưa có lượt tra nào</p>
    <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
      Hãy tra vài từ trong từ điển — những từ bạn tra nhiều nhất sẽ hiện ở đây.
    </p>  </div>
)

// --------------------------------------------------------------------------
// 1 chip từ
// --------------------------------------------------------------------------
const LookupChip: React.FC<{
  entry: TopLookup
  onOpen: (entry: TopLookup) => void
}> = ({ entry, onOpen }) => (
  <button
    type="button"
    onClick={() => onOpen(entry)}
    title={`Tra cứu "${entry.label}" trong từ điển`}
    className="group inline-flex items-center gap-1 px-2.5 py-1.5 rounded-md border border-gray-200 bg-gray-50/70 hover:border-indigo-300 hover:bg-indigo-50 hover:text-indigo-700 transition-colors max-w-full"
  >
    <span className="text-sm font-bold text-gray-900 group-hover:text-indigo-700 transition-colors truncate">
      {entry.label}
    </span>
    <ChevronRight className="w-3 h-3 text-gray-300 group-hover:text-indigo-400 shrink-0 transition-colors" />
  </button>
)

// --------------------------------------------------------------------------
// Main block
// --------------------------------------------------------------------------
export const TopLookupsBlock: React.FC = () => {
  const navigate = useNavigate()
  const { data, isLoading, isError, error, refetch } = useTopLookups({ limit: 30 })

  if (isLoading && !data) return <SkeletonBlock />
  if (isError && !data) {
    return (
      <div className="bg-white rounded-md border border-rose-200 shadow-xs p-4">
        <div className="flex flex-col sm:flex-row items-center gap-3 text-center sm:text-left">
          <span className="w-10 h-10 rounded-md bg-rose-50 text-rose-500 flex items-center justify-center shrink-0">
            <Search className="w-5 h-5" />
          </span>
          <div className="flex-1">
            <p className="font-bold text-gray-900 text-sm">
              Không tải được bảng từ đã tra
            </p>
            <p className="text-xs text-gray-500 mt-0.5">
              {error?.message ?? 'Có lỗi xảy ra. Vui lòng thử lại sau.'}
            </p>
          </div>
          <Button size="small" onClick={() => void refetch()}>Thử lại</Button>
        </div>
      </div>
    )
  }

  const entries = data ?? []

  /**
   * Mở từ điển tra đúng từ đã bấm.
   * - VOCAB  → tab từ vựng, tìm theo từ viết
   * - KANJI  → tab chữ Hán, tìm theo ký tự
   * - GRAMMAR→ tab ngữ pháp, tìm theo tên mẫu
   */
  const handleOpen = (entry: TopLookup) => {
    const tab =
      entry.targetType === 'KANJI'
        ? 'kanji'
        : entry.targetType === 'GRAMMAR'
          ? 'grammar'
          : 'vocab'

    const query = entry.label.trim()
    if (!query) return

    navigate(`/dictionary?q=${encodeURIComponent(query)}&tab=${tab}`)
  }

  return (
    <section className="space-y-3" aria-label="Từ đã tra nhiều nhất">
      {/* ---------- Header ---------- */}
      <div>
        <h2 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
          <Flame className="w-4 h-4 text-amber-500" />
          Top từ tra nhiều nhất
        </h2>
        <p className="text-xs text-gray-500 mt-0.5">
          Top {entries.length} từ được tra nhiều nhất · bấm từ để tra lại
        </p>
      </div>

      {/* ---------- Chip + nút tra cứu ---------- */}
      <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5">
        {entries.length === 0 ? (
          <EmptyBlock />
        ) : (
          // Flow ngang → tự xuống hàng khi đầy; mũi tên chỉ tới 1 từ mỗi chip.
          <div className="flex flex-wrap gap-1.5 max-h-[380px] overflow-y-auto pr-0.5">
            {entries.map((entry) => (
              <LookupChip
                key={`${entry.targetType}-${entry.targetId ?? entry.kanjiCharacter}`}
                entry={entry}
                onOpen={handleOpen}
              />
            ))}
          </div>
        )}

        {/* ---------- Nút hành động ---------- */}
        <div className="mt-3 pt-3 border-t border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <p className="text-[10px] text-gray-400 flex items-start gap-1">
            <Sparkles className="w-3 h-3 shrink-0 mt-px" />
            <span>
              Bấm vào một từ để mở từ điển tra lại từ đó.
            </span>
          </p>
          <Button
            type="primary"
            size="small"
            icon={<BookOpen className="w-3.5 h-3.5" />}
            onClick={() => navigate('/dictionary')}
            className="!rounded-md !font-semibold shrink-0"
          >
            Tra cứu ngay
          </Button>
        </div>
      </div>
    </section>
  )
}

export default TopLookupsBlock