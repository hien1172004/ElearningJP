import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCw,
  RotateCcw,
  Trash2,
  Zap,
} from 'lucide-react'
import { toast } from 'sonner'
import { cn } from '@/utils/cn'
import { BackButton } from '@/components/common'
import { SrsForecastBar } from '@/components/srs'
import { useSrsItems } from '@/hooks/useSrsItems'
import { useSrsStats } from '@/hooks/useSrsStats'
import { useArchiveSrsItem, useRestoreSrsItem } from '@/hooks/useSrsItemActions'
import type { SrsDueItem, SrsStats, SrsStatus } from '@/types'

const PAGE_SIZE = 20

const STATUS_FILTERS: Array<{
  value: SrsStatus | undefined
  label: string
  count: (s: SrsStats) => number
}> = [
  { value: undefined, label: 'Tất cả', count: (s) => s.totalItems },
  { value: 'NEW', label: 'Mới', count: (s) => s.newCount },
  { value: 'LEARNING', label: 'Đang học', count: (s) => s.learningCount },
  { value: 'RELEARNING', label: 'Học lại', count: (s) => s.relearningCount },
  { value: 'REVIEWING', label: 'Đang ôn', count: (s) => s.reviewingCount },
  { value: 'MASTERED', label: 'Thành thạo', count: (s) => s.masteredCount },
  { value: 'ARCHIVED', label: 'Lưu trữ', count: (s) => s.archivedCount },
]

const ITEM_TYPE_LABEL: Record<string, string> = {
  VOCABULARY: 'Từ vựng',
  CHARACTER: 'Chữ Hán',
  GRAMMAR: 'Ngữ pháp',
}

const STATUS_META: Record<SrsStatus, { label: string; hint: string; className: string }> = {
  NEW: {
    label: 'Mới',
    hint: 'Chưa ôn lần nào. Mỗi ngày chỉ đưa một số thẻ mới vào phiên ôn.',
    className: 'bg-violet-50 text-violet-700 border-violet-200',
  },
  LEARNING: {
    label: 'Đang học',
    hint: 'Đang học lần đầu, khoảng ôn dưới 6 ngày.',
    className: 'bg-amber-50 text-amber-700 border-amber-200',
  },
  RELEARNING: {
    label: 'Học lại',
    hint: 'Đã từng nhớ nhưng bị quên, đang học lại.',
    className: 'bg-rose-50 text-rose-700 border-rose-200',
  },
  REVIEWING: {
    label: 'Đang ôn',
    hint: 'Đã nhớ, khoảng ôn từ 6 ngày và đang giãn dần.',
    className: 'bg-sky-50 text-sky-700 border-sky-200',
  },
  MASTERED: {
    label: 'Thành thạo',
    hint: 'Nhớ lâu dài, khoảng ôn từ 21 ngày.',
    className: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  },
  ARCHIVED: {
    label: 'Lưu trữ',
    hint: 'Đã xoá hoặc quá hạn ôn hơn 30 ngày. Khôi phục để học lại từ đầu.',
    className: 'bg-slate-100 text-slate-500 border-slate-200',
  },
}

/** Mốc "Thành thạo" (ngày) để vẽ thanh tiến độ. */
const MASTERED_DAYS = 21

const DAY_MS = 24 * 60 * 60 * 1000

/** "Ôn lại: 06/11 (sau 6 ngày)" / "Đến hạn hôm nay" / "Quá hạn 3 ngày". */
const describeNextReview = (item: SrsDueItem): string => {
  if (item.status === 'ARCHIVED') return 'Đã lưu trữ'
  if (item.status === 'NEW') return 'Chờ học'
  const d = new Date(item.nextReviewAt)
  if (Number.isNaN(d.getTime())) return '—'
  const startOfToday = new Date()
  startOfToday.setHours(0, 0, 0, 0)
  const target = new Date(d)
  target.setHours(0, 0, 0, 0)
  const diffDays = Math.round((target.getTime() - startOfToday.getTime()) / DAY_MS)
  if (diffDays < 0) return `Quá hạn ${-diffDays} ngày`
  if (diffDays === 0) return 'Đến hạn hôm nay'
  const label = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
  return `Ôn lại: ${label} (sau ${diffDays} ngày)`
}

/**
 * SrsDeckPage — "Sổ tay SRS": liệt kê toàn bộ thẻ của user (phân trang + lọc trạng thái).
 * Route: `/srs/deck` (protected).
 */
export const SrsDeckPage: React.FC = () => {
  const [status, setStatus] = useState<SrsStatus | undefined>(undefined)
  const [page, setPage] = useState(0)

  const { data, isLoading, isError, refetch, isFetching } = useSrsItems({
    status,
    page,
    size: PAGE_SIZE,
  })

  const { data: stats } = useSrsStats()
  const archive = useArchiveSrsItem()
  const restore = useRestoreSrsItem()

  const handleArchive = (item: SrsDueItem) => {
    if (!window.confirm(`Xoá "${item.frontText}" khỏi sổ ôn? Bạn có thể khôi phục trong tab Lưu trữ.`)) return
    archive.mutate(item.srsItemId, {
      onSuccess: () => toast.success('Đã chuyển thẻ vào Lưu trữ'),
      onError: () => toast.error('Không thể xoá thẻ'),
    })
  }

  const handleRestore = (item: SrsDueItem) => {
    restore.mutate(item.srsItemId, {
      onSuccess: () => toast.success('Đã khôi phục thẻ, học lại từ đầu'),
      onError: () => toast.error('Không thể khôi phục thẻ'),
    })
  }

  const items = data?.items ?? []
  const totalPages = data?.totalPages ?? 0

  return (
    <div className="max-w-4xl mx-auto pb-16">
      {/* ===== Header ===== */}
      <header className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <BackButton to="/srs" />
        <Link
          to="/srs"
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold transition"
        >
          <Zap className="w-4 h-4" />
          Ôn ngay
          {stats && stats.dueTodayCount > 0 && (
            <span className="px-1.5 py-0.5 rounded-md bg-surface/20 text-xs tabular-nums">
              {stats.dueTodayCount}
            </span>
          )}
        </Link>
      </header>

      {/* ===== Dự báo ôn tập ===== */}
      <div className="mb-5">
        <SrsForecastBar />
      </div>

      {/* ===== Filter tabs ===== */}
      <div className="flex gap-2 mb-5 flex-wrap">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.label}
            type="button"
            title={f.value ? STATUS_META[f.value].hint : 'Tất cả thẻ đang ôn (không gồm lưu trữ)'}
            onClick={() => {
              setStatus(f.value)
              setPage(0)
            }}
            className={cn(
              'px-3.5 py-1.5 rounded-full text-xs font-semibold transition border',
              status === f.value
                ? 'bg-indigo-600 text-white border-indigo-600'
                : 'bg-surface text-slate-600 border-slate-200 hover:border-indigo-300',
            )}
          >
            {f.label}
            {stats && <span className="ml-1.5 tabular-nums opacity-70">{f.count(stats)}</span>}
          </button>
        ))}
      </div>

      {/* ===== Loading ===== */}
      {isLoading && (
        <div className="flex flex-col items-center justify-center py-24 text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin" />
          <p className="mt-3 text-sm">Đang tải sổ tay…</p>
        </div>
      )}

      {/* ===== Error ===== */}
      {isError && (
        <div className="flex flex-col items-center justify-center py-24 text-center">
          <p className="text-slate-500">Không thể tải sổ tay SRS.</p>
          <button
            type="button"
            onClick={() => void refetch()}
            className="mt-4 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold transition"
          >
            <RefreshCw className="w-4 h-4" />
            Thử lại
          </button>
        </div>
      )}

      {/* ===== Empty ===== */}
      {!isLoading && !isError && items.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="text-slate-500">
            {status === 'ARCHIVED' ? 'Không có thẻ nào trong Lưu trữ.' : 'Chưa có thẻ nào trong sổ tay.'}
          </p>
          <Link
            to="/dictionary"
            className="mt-3 text-sm font-semibold text-indigo-600 hover:text-indigo-700"
          >
            Tra từ điển để lưu thẻ →
          </Link>
        </div>
      )}

      {/* ===== List ===== */}
      {items.length > 0 && (
        <ul className="space-y-3">
          {items.map((item) => {
            const meta = STATUS_META[item.status] ?? STATUS_META.LEARNING
            const progress = Math.min(100, Math.round((item.intervalDays / MASTERED_DAYS) * 100))
            const archived = item.status === 'ARCHIVED'
            return (
            <li
              key={item.srsItemId}
              className="flex items-center gap-4 p-4 rounded-2xl bg-surface border border-slate-200 hover:border-indigo-300 transition"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-serif text-lg font-bold text-slate-900 truncate">
                    {item.frontText}
                  </span>
                  <span className="shrink-0 px-2 py-0.5 rounded-md bg-slate-100 text-slate-500 text-[10px] font-semibold">
                    {ITEM_TYPE_LABEL[item.itemType] ?? item.itemType}
                  </span>
                </div>
                {item.reading && (
                  <div className="text-xs text-slate-500 truncate">{item.reading}</div>
                )}
                {!archived && item.status !== 'NEW' && (
                  <div
                    className="mt-2 h-1 w-full max-w-[200px] rounded-full bg-slate-100 overflow-hidden"
                    title={`Khoảng ôn ${item.intervalDays} ngày / ${MASTERED_DAYS} ngày để thành thạo`}
                  >
                    <div className="h-full rounded-full bg-emerald-400" style={{ width: `${progress}%` }} />
                  </div>
                )}
              </div>

              <div className="hidden sm:flex flex-col items-end gap-1 shrink-0">
                <span
                  className={cn(
                    'px-2 py-0.5 rounded-md text-[10px] font-semibold border',
                    meta.className,
                  )}
                  title={meta.hint}
                >
                  {meta.label}
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] text-slate-400">
                  <CalendarDays className="w-3 h-3" />
                  {describeNextReview(item)}
                </span>
              </div>

              {archived ? (
                <button
                  type="button"
                  onClick={() => handleRestore(item)}
                  disabled={restore.isPending}
                  title="Khôi phục để học lại từ đầu"
                  className="shrink-0 inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-600 hover:border-indigo-300 hover:text-indigo-600 transition disabled:opacity-50"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  Khôi phục
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => handleArchive(item)}
                  disabled={archive.isPending}
                  title="Xoá khỏi sổ ôn (chuyển vào Lưu trữ)"
                  aria-label="Xoá thẻ"
                  className="shrink-0 p-2 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition disabled:opacity-50"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </li>
            )
          })}
        </ul>
      )}

      {/* ===== Pagination ===== */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 mt-6">
          <button
            type="button"
            disabled={page <= 0 || isFetching}
            onClick={() => setPage((p) => p - 1)}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-slate-200 text-slate-600 text-sm font-semibold transition hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="w-4 h-4" />
            Trước
          </button>
          <span className="text-sm text-slate-500 font-medium tabular-nums">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1 || isFetching}
            onClick={() => setPage((p) => p + 1)}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-slate-200 text-slate-600 text-sm font-semibold transition hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            Sau
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  )
}

export default SrsDeckPage
