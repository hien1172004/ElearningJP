import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Layers, Loader2, RefreshCw, Sparkles, Trophy } from 'lucide-react'
import { toast } from 'sonner'
import { SrsFlashcard, SrsRatingButtons, SrsForecastBar } from '@/components/srs'
import { useSrsDue } from '@/hooks/useSrsDue'
import { useSrsReview } from '@/hooks/useSrsReview'
import { cn } from '@/utils/cn'
import type { SrsRating } from '@/types'

/** Các lựa chọn số thẻ mỗi phiên (backend clamp 1..100). */
const LIMIT_OPTIONS = [10, 20, 50, 100] as const

type Phase = 'loading' | 'error' | 'empty' | 'done' | 'reviewing'

/**
 * SrsReviewPage — màn hình ôn tập Flashcard (lật thẻ + 4 nút SM-2).
 *
 * Luồng: GET /srs/due?limit=N → danh sách `data` → lật thẻ → đánh giá → tăng
 * `reviewedIndex` (optimistic) → hết danh sách thì hiện tổng kết + "Ôn tiếp".
 *
 * Hàng đợi được derive trực tiếp từ `data` + `reviewedIndex` (không giữ state
 * queue riêng) nên đổi `limit` hay refetch không gây mất đồng bộ.
 */
export const SrsReviewPage: React.FC = () => {
  const navigate = useNavigate()
  const [limit, setLimit] = useState(20)
  const { data, isLoading, isError, refetch, isFetching } = useSrsDue(limit)
  const review = useSrsReview()

  const [reviewedIndex, setReviewedIndex] = useState(0)
  const [totalXp, setTotalXp] = useState(0)
  const [flipped, setFlipped] = useState(false)
  const [revealedAt, setRevealedAt] = useState<number | null>(null)

  const cards = data ?? []
  const current = reviewedIndex < cards.length ? cards[reviewedIndex] : undefined

  const phase: Phase = isLoading
    ? 'loading'
    : isError
      ? 'error'
      : cards.length === 0
        ? 'empty'
        : reviewedIndex >= cards.length
          ? 'done'
          : 'reviewing'

  const handleLimitChange = (n: number) => {
    if (n === limit) return
    setLimit(n)
    // Reset phiên đồng bộ ngay khi đổi số thẻ (tránh lệch với data cũ/cached).
    setReviewedIndex(0)
    setTotalXp(0)
    setFlipped(false)
    setRevealedAt(null)
  }

  const handleFlip = () => {
    if (phase !== 'reviewing' || !current) return
    setFlipped((f) => !f)
    // Ghi lại mốc thời điểm lật thẻ để đo responseTimeMs.
    setRevealedAt((prev) => (prev == null ? Date.now() : prev))
  }

  const handleRate = (rating: SrsRating) => {
    if (!current || !flipped) return
    const card = current
    const responseTimeMs = revealedAt != null ? Date.now() - revealedAt : undefined

    // Optimistic: chuyển sang thẻ kế tiếp.
    setReviewedIndex((i) => i + 1)
    setFlipped(false)
    setRevealedAt(null)

    review.mutate(
      {
        id: card.srsItemId,
        payload: { rating, responseTimeMs, clientEventId: crypto.randomUUID() },
      },
      {
        onSuccess: (result) => {
          setTotalXp((x) => x + (result?.xpEarned ?? 0))
        },
        onError: () => {
          toast.error('Không thể lưu kết quả ôn tập. Vui lòng thử lại.')
          setReviewedIndex((i) => Math.max(0, i - 1))
        },
      },
    )
  }

  const handleReload = async () => {
    await refetch()
    setReviewedIndex(0)
    setTotalXp(0)
    setFlipped(false)
    setRevealedAt(null)
  }

  const total = cards.length
  const progressPct = total > 0 ? Math.round((reviewedIndex / total) * 100) : 0

  return (
    <div className="max-w-3xl mx-auto pb-16">
      {/* ===== Header ===== */}
      <header className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <span className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <Layers className="w-5 h-5" />
          </span>
          <div>
            <h1 className="text-lg font-bold text-slate-900">Ôn tập SRS</h1>
            <p className="text-xs text-slate-500">Lặp lại ngắt quãng — nhớ lâu hơn</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <Link
            to="/srs/deck"
            className="text-sm font-semibold text-slate-500 hover:text-slate-700 px-3 py-1.5 rounded-lg hover:bg-slate-100 transition"
          >
            Sổ tay
          </Link>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="text-sm font-semibold text-slate-500 hover:text-slate-700 px-3 py-1.5 rounded-lg hover:bg-slate-100 transition"
          >
            Thoát
          </button>
        </div>
      </header>

      {/* ===== Dự báo ôn tập ===== */}
      <div className="mb-5">
        <SrsForecastBar />
      </div>

      {/* ===== Chọn số thẻ mỗi phiên ===== */}
      <div className="flex items-center gap-3 mb-5">
        <span className="text-xs font-semibold text-slate-500">Số thẻ mỗi phiên:</span>
        <div className="flex items-center gap-1 bg-slate-100 rounded-lg p-1">
          {LIMIT_OPTIONS.map((n) => (
            <button
              key={n}
              type="button"
              onClick={() => handleLimitChange(n)}
              className={cn(
                'px-3 py-1 rounded-md text-xs font-semibold transition',
                limit === n
                  ? 'bg-white text-indigo-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-700',
              )}
            >
              {n}
            </button>
          ))}
        </div>
        <span className="text-xs text-slate-400">thẻ</span>
      </div>

      {/* ===== Loading ===== */}
      {phase === 'loading' && (
        <div className="flex flex-col items-center justify-center py-24 text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin" />
          <p className="mt-3 text-sm">Đang tải thẻ ôn tập…</p>
        </div>
      )}

      {/* ===== Error ===== */}
      {phase === 'error' && (
        <div className="flex flex-col items-center justify-center py-24 text-center">
          <p className="text-slate-500">Không thể tải thẻ ôn tập.</p>
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
      {phase === 'empty' && (
        <div className="flex flex-col items-center justify-center py-24 text-center">
          <span className="w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-500 flex items-center justify-center mb-4">
            <Sparkles className="w-8 h-8" />
          </span>
          <h2 className="text-lg font-bold text-slate-900">Không còn thẻ đến hạn</h2>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">
            Hôm nay bạn chưa có thẻ nào cần ôn. Hãy lưu thêm từ vựng, chữ Hán hoặc ngữ pháp vào SRS nhé!
          </p>
          <button
            type="button"
            onClick={() => void handleReload()}
            disabled={isFetching}
            className="mt-6 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold transition disabled:opacity-50"
          >
            <RefreshCw className={isFetching ? 'w-4 h-4 animate-spin' : 'w-4 h-4'} />
            Làm mới
          </button>
        </div>
      )}

      {/* ===== Done ===== */}
      {phase === 'done' && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <span className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-500 flex items-center justify-center mb-4">
            <Trophy className="w-8 h-8" />
          </span>
          <h2 className="text-xl font-bold text-slate-900">Hoàn thành buổi ôn tập!</h2>
          <p className="text-sm text-slate-500 mt-1">
            Bạn đã ôn <span className="font-semibold text-slate-700">{reviewedIndex}</span> thẻ
            {totalXp > 0 && (
              <>
                {' · '}
                <span className="font-semibold text-indigo-600">+{totalXp} XP</span>
              </>
            )}
          </p>
          <div className="flex gap-3 mt-6">
            <button
              type="button"
              onClick={() => void handleReload()}
              disabled={isFetching}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold transition disabled:opacity-50"
            >
              <RefreshCw className={isFetching ? 'w-4 h-4 animate-spin' : 'w-4 h-4'} />
              Ôn tiếp
            </button>
            <Link
              to="/"
              className="inline-flex items-center px-4 py-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 text-sm font-semibold transition"
            >
              Về trang chủ
            </Link>
          </div>
        </div>
      )}

      {/* ===== Reviewing ===== */}
      {phase === 'reviewing' && current && (
        <>
          {/* Progress */}
          <div className="flex items-center gap-3 mb-4">
            <div className="flex-1 h-1.5 rounded-full bg-slate-200 overflow-hidden">
              <div
                className="h-full bg-indigo-600 rounded-full transition-all duration-300"
                style={{ width: `${progressPct}%` }}
              />
            </div>
            <span className="text-xs font-semibold text-slate-500 tabular-nums whitespace-nowrap">
              {reviewedIndex + 1} / {total}
            </span>
          </div>

          <SrsFlashcard item={current} flipped={flipped} onFlip={handleFlip} />

          <div className="mt-6">
            <SrsRatingButtons disabled={!flipped || review.isPending} onRate={handleRate} />
          </div>
        </>
      )}
    </div>
  )
}

export default SrsReviewPage
