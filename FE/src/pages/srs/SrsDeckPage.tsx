import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  BookMarked,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Loader2,
  RefreshCw,
  Zap,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import { SrsForecastBar } from '@/components/srs'
import { useSrsItems } from '@/hooks/useSrsItems'
import type { SrsStatus } from '@/types'

const PAGE_SIZE = 20

const STATUS_FILTERS: Array<{ value: SrsStatus | undefined; label: string }> = [
  { value: undefined, label: 'Tất cả' },
  { value: 'LEARNING', label: 'Đang học' },
  { value: 'REVIEWING', label: 'Đang ôn' },
  { value: 'MASTERED', label: 'Thành thạo' },
]

const ITEM_TYPE_LABEL: Record<string, string> = {
  VOCABULARY: 'Từ vựng',
  CHARACTER: 'Chữ Hán',
  GRAMMAR: 'Ngữ pháp',
}

const STATUS_META: Record<SrsStatus, { label: string; className: string }> = {
  LEARNING: { label: 'Đang học', className: 'bg-amber-50 text-amber-700 border-amber-200' },
  REVIEWING: { label: 'Đang ôn', className: 'bg-sky-50 text-sky-700 border-sky-200' },
  MASTERED: { label: 'Thành thạo', className: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
}

const formatDate = (iso: string): string => {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
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

  const items = data?.items ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <div className="max-w-4xl mx-auto pb-16">
      {/* ===== Header ===== */}
      <header className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div className="flex items-center gap-3">
          <span className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <BookMarked className="w-5 h-5" />
          </span>
          <div>
            <h1 className="text-lg font-bold text-slate-900">Sổ tay SRS</h1>
            <p className="text-xs text-slate-500">{totalElements} thẻ trong sổ tay ôn tập</p>
          </div>
        </div>
        <Link
          to="/srs"
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold transition"
        >
          <Zap className="w-4 h-4" />
          Ôn ngay
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
            onClick={() => {
              setStatus(f.value)
              setPage(0)
            }}
            className={cn(
              'px-3.5 py-1.5 rounded-full text-xs font-semibold transition border',
              status === f.value
                ? 'bg-indigo-600 text-white border-indigo-600'
                : 'bg-white text-slate-600 border-slate-200 hover:border-indigo-300',
            )}
          >
            {f.label}
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
          <p className="text-slate-500">Chưa có thẻ nào trong sổ tay.</p>
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
          {items.map((item) => (
            <li
              key={item.srsItemId}
              className="flex items-center gap-4 p-4 rounded-2xl bg-white border border-slate-200 hover:border-indigo-300 transition"
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
              </div>

              <div className="hidden sm:flex flex-col items-end gap-1 shrink-0">
                <span
                  className={cn(
                    'px-2 py-0.5 rounded-md text-[10px] font-semibold border',
                    STATUS_META[item.status].className,
                  )}
                >
                  {STATUS_META[item.status].label}
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] text-slate-400">
                  <CalendarDays className="w-3 h-3" />
                  {formatDate(item.nextReviewAt)}
                </span>
              </div>
            </li>
          ))}
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
