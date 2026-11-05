import React, { useState, useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { Link, useNavigate } from 'react-router-dom'
import { Loader2, RefreshCw, Sparkles, Trophy, RotateCcw } from 'lucide-react'
import { toast } from 'sonner'
import { SrsFlashcard, SrsRatingButtons, SrsForecastBar } from '@/components/srs'
import { useSrsDue } from '@/hooks/useSrsDue'
import { useSrsReview } from '@/hooks/useSrsReview'
import { cn } from '@/utils/cn'
import type { SrsRating, SrsDueItem } from '@/types'

/** Các lựa chọn số thẻ mỗi phiên (backend clamp 1..100). */
const LIMIT_OPTIONS = [10, 20, 50, 100] as const

type Phase = 'loading' | 'error' | 'empty' | 'done' | 'reviewing'

/**
 * SrsReviewPage — màn hình ôn tập Flashcard (lật thẻ + 4 nút SM-2 Anki loop).
 *
 * Chuẩn Anki (Intra-session queue):
 *  - Khi bấm "Lại" (Again): Thẻ được đưa về cuối hàng đợi của phiên để học lại.
 *  - Khi bấm "Khó" (Hard), "Tốt" (Good), "Dễ" (Easy): Thẻ được tính là đã thuộc và kết thúc phiên.
 *  - Phiên chỉ kết thúc khi tất cả thẻ đều đã được nhớ thành công.
 */
export const SrsReviewPage: React.FC = () => {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [limit, setLimit] = useState(20)
  const { data, isLoading, isError, refetch, isFetching } = useSrsDue(limit)
  const review = useSrsReview()

  // Hàng đợi phiên học xoay vòng (Anki-style)
  const [queue, setQueue] = useState<SrsDueItem[]>([])
  const [initialTotal, setInitialTotal] = useState(0)
  const [completedCount, setCompletedCount] = useState(0)

  const [totalXp, setTotalXp] = useState(0)
  const [flipped, setFlipped] = useState(false)
  const [revealedAt, setRevealedAt] = useState<number | null>(null)

  // Khởi tạo hàng đợi khi dữ liệu được tải về lần đầu
  useEffect(() => {
    if (data && data.length > 0 && queue.length === 0 && completedCount === 0) {
      setQueue(data)
      setInitialTotal(data.length)
    }
  }, [data, queue.length, completedCount])

  const current = queue.length > 0 ? queue[0] : undefined

  const phase: Phase = isLoading
    ? 'loading'
    : isError
      ? 'error'
      : (data?.length === 0 && queue.length === 0 && completedCount === 0)
        ? 'empty'
        : (queue.length === 0 && completedCount > 0)
          ? 'done'
          : queue.length > 0
            ? 'reviewing'
            : 'empty'

  const handleLimitChange = (n: number) => {
    if (n === limit) return
    setLimit(n)
    setQueue([])
    setInitialTotal(0)
    setCompletedCount(0)
    setTotalXp(0)
    setFlipped(false)
    setRevealedAt(null)
  }

  const handleFlip = () => {
    if (phase !== 'reviewing' || !current) return
    setFlipped((f) => !f)
    setRevealedAt((prev) => (prev == null ? Date.now() : prev))
  }

  const handleRate = (rating: SrsRating) => {
    if (!current || !flipped) return
    const card = current
    const responseTimeMs = revealedAt != null ? Date.now() - revealedAt : undefined

    setFlipped(false)
    setRevealedAt(null)

    if (rating === 'AGAIN') {
      // Chuẩn Anki: Đẩy thẻ quên về cuối hàng đợi của phiên học
      setQueue((prev) => [...prev.slice(1), card])
      toast.info('Thẻ sẽ xuất hiện lại ở cuối phiên để bạn ôn lại', {
        icon: <RotateCcw className="w-4 h-4 text-rose-500" />,
      })
    } else {
      // Đã nhớ được (Hard, Good, Easy): Đánh dấu hoàn thành trong phiên
      setQueue((prev) => prev.slice(1))
      setCompletedCount((c) => c + 1)
    }

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
        },
      },
    )
  }

  const handleReload = async () => {
    void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.srs.forecast })
    const res = await refetch()
    const newCards = res.data ?? []
    setQueue(newCards)
    setInitialTotal(newCards.length)
    setCompletedCount(0)
    setTotalXp(0)
    setFlipped(false)
    setRevealedAt(null)
  }

  const progressPct =
    initialTotal > 0
      ? Math.min(100, Math.round((completedCount / initialTotal) * 100))
      : 0

  return (
    <div className="max-w-3xl mx-auto pb-16">
      {/* ===== Thanh điều khiển: dự báo ôn tập + Sổ tay/Thoát + số thẻ mỗi phiên ===== */}
      <section className="bg-white rounded-xl border border-suna shadow-xs p-4 mb-6 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <SrsForecastBar />
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
        </div>

        {/* Chọn số thẻ mỗi phiên */}
        <div className="flex items-center gap-3">
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
      </section>

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
            Bạn đã hoàn thành <span className="font-semibold text-slate-700">{completedCount}</span> thẻ trong phiên học
            {totalXp > 0 && (
              <>
                {' · '}
                {/* <span className="font-semibold text-indigo-600">+{totalXp} XP</span> */}
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
              Đã thuộc {completedCount}/{initialTotal} (còn {queue.length})
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