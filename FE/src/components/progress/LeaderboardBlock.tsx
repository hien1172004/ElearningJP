import React, { useMemo, useState } from 'react'
import { Avatar, Button, Tooltip, message } from 'antd'
import {
  ChevronLeft,
  ChevronRight,
  Medal,
  RefreshCw,
  Trophy,
  Users,
} from 'lucide-react'
import { useLeaderboard, useMyRank, useRefreshLeaderboard } from '@/hooks/useLeaderboard'
import { useAuthStore } from '@/stores/useAuthStore'
import { cn } from '@/utils/cn'
import { formatCompactNumber } from '@/utils/progress'
import {
  currentWeekKey,
  formatWeekLabel,
  isCurrentWeek,
  shiftWeekKey,
} from '@/utils/week'
import type { LeaderboardEntry } from '@/types'

/**
 * LeaderboardBlock — Khối 5: bảng xếp hạng tuần.
 *
 * Hiển thị:
 * - Bộ chọn tuần (‹ / ›), disable "›" ở tuần hiện tại.
 * - Bảng top 5: hạng (vàng/bạch/đồng cho top 3), avatar, tên, điểm, lần tra, XP.
 * - Card "Hạng của tôi" bên dưới.
 * - Nút "Reload" (có cooldown 30s ở BE → 429 được xử lý riêng).
 *
 * ```tsx
 * <LeaderboardBlock />
 * ```
 */

/** Số tuần tối đa người dùng có thể lùi về (khớp retention 8 tuần của BE). */
const MAX_WEEKS_BACK = 7

// --------------------------------------------------------------------------
// Style helpers
// --------------------------------------------------------------------------

/** Style của huy chương cho top 3. */
function medalStyle(rank: number): string {
  switch (rank) {
    case 1:
      return 'bg-amber-100 text-amber-700 border-amber-300'
    case 2:
      return 'bg-slate-100 text-slate-600 border-slate-300'
    case 3:
      return 'bg-orange-100 text-orange-700 border-orange-300'
    default:
      return 'bg-gray-50 text-gray-500 border-gray-200'
  }
}

// --------------------------------------------------------------------------
// Loading skeleton
// --------------------------------------------------------------------------
const SkeletonBlock: React.FC = () => (
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-4">
    <div className="animate-pulse space-y-2.5">
      <div className="h-5 w-44 bg-gray-200 rounded" />
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="h-9 bg-gray-100 rounded" />
      ))}
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Error state
// --------------------------------------------------------------------------
const ErrorBlock: React.FC<{ message?: string; onRetry?: () => void }> = ({
  message: msg,
  onRetry,
}) => (
  <div className="bg-white rounded-md border border-rose-200 shadow-xs p-4">
    <div className="flex flex-col sm:flex-row items-center gap-3 text-center sm:text-left">
      <span className="w-10 h-10 rounded-md bg-rose-50 text-rose-500 flex items-center justify-center shrink-0">
        <Trophy className="w-5 h-5" />
      </span>
      <div className="flex-1">
        <p className="font-bold text-gray-900 text-sm">
          Không tải được bảng xếp hạng
        </p>
        <p className="text-xs text-gray-500 mt-0.5">
          {msg ?? 'Có lỗi xảy ra. Vui lòng thử lại sau.'}
        </p>
      </div>
      {onRetry && (
        <Button size="small" icon={<RefreshCw className="w-3.5 h-3.5" />} onClick={onRetry}>
          Thử lại
        </Button>
      )}
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Empty state — chưa ai có điểm trong tuần
// --------------------------------------------------------------------------
const EmptyBlock: React.FC<{ isCurrentWeek: boolean }> = ({ isCurrentWeek: isCurrent }) => (
  <div className="py-6 text-center">
    <span className="inline-flex w-11 h-11 rounded-full bg-indigo-50 text-indigo-400 items-center justify-center">
      <Trophy className="w-5 h-5" />
    </span>
    <p className="font-bold text-gray-900 text-sm mt-2">
      {isCurrent ? 'Chưa có ai ghi điểm tuần này' : 'Tuần này không có dữ liệu'}
    </p>
    <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
      {isCurrent
        ? 'Hãy là người đầu tiên! Tra từ điển hoặc hoàn thành một bài học để ghi điểm.'
        : 'Bạn có thể chuyển sang tuần khác để xem bảng xếp hạng.'}
    </p>
  </div>
)

// --------------------------------------------------------------------------
// Row của bảng
// --------------------------------------------------------------------------
interface RowProps {
  entry: LeaderboardEntry
  isMe: boolean
}

const LeaderboardRow: React.FC<RowProps> = ({ entry, isMe }) => (
  <div
    className={cn(
      'flex items-center gap-2.5 px-2 py-1.5 rounded-md transition-colors',
      isMe
        ? 'bg-indigo-50 ring-1 ring-indigo-200 border-l-4 border-indigo-500'
        : 'hover:bg-gray-50',
    )}
  >
    {/* Hạng */}
    <span
      className={cn(
        'w-6 h-6 shrink-0 rounded border flex items-center justify-center text-[10px] font-extrabold tabular-nums',
        medalStyle(entry.rank),
      )}
    >
      {entry.rank}
    </span>

    {/* Avatar + tên */}
    <Avatar
      size={28}
      src={entry.avatarUrl ?? undefined}
      className="shrink-0 bg-indigo-100 text-indigo-600 text-xs"
    >
      {entry.displayName?.charAt(0)?.toUpperCase() ?? '?'}
    </Avatar>

    <div className="min-w-0 flex-1">
      <p className="text-xs font-semibold text-gray-900 truncate">
        {entry.displayName ?? `User #${entry.userId}`}
        {isMe && (
          <span className="ml-1.5 text-[9px] font-bold text-indigo-600 bg-indigo-100 px-1 py-0.5 rounded">
            BẠN
          </span>
        )}
      </p>
      {/* <p className="text-[10px] text-gray-500 tabular-nums">
        {formatCompactNumber(entry.totalLookups)} lần tra ·{' '}
        {formatCompactNumber(entry.totalXp)} XP
      </p> */}
    </div>

    {/* Điểm */}
    <div className="text-right shrink-0">
      <p className="text-xs font-extrabold text-gray-900 tabular-nums">
        {formatCompactNumber(entry.score)}
      </p>
      <p className="text-[9px] text-gray-400 font-medium uppercase tracking-wide">
        điểm
      </p>
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Card "Hạng của tôi"
// --------------------------------------------------------------------------
interface MyRankCardProps {
  rank: number | null
  score: number | null
  totalPlayers: number | null
  inTop: boolean
}

const MyRankCard: React.FC<MyRankCardProps> = ({
  rank,
  score,
  totalPlayers,
  inTop,
}) => (
  <div className="flex flex-wrap items-center justify-between gap-2 mt-3 px-3 py-2 rounded-md bg-gradient-to-r from-indigo-50 to-sky-50 border border-indigo-100">
    <div className="flex items-center gap-2">
      <span className="w-8 h-8 rounded-md bg-indigo-600 text-white flex items-center justify-center shrink-0">
        <Medal className="w-4 h-4" />
      </span>
      <div>
        <p className="text-[10px] font-semibold text-gray-500 uppercase tracking-wide">
          Hạng của tôi
        </p>
        <p className="text-xs font-bold text-gray-900">
          {rank != null ? (
            <>
              Hạng #{rank}
              {totalPlayers != null && (
                <span className="font-medium text-gray-500">
                  {' '}
                  / {formatCompactNumber(totalPlayers)} người
                </span>
              )}
            </>
          ) : (
            'Chưa có điểm trong tuần này'
          )}
        </p>
      </div>
    </div>

    <div className="text-right">
      <p className="text-base font-extrabold text-indigo-700 tabular-nums">
        {formatCompactNumber(score ?? 0)}
      </p>
      <p className="text-[9px] text-gray-500 font-medium uppercase tracking-wide">
        điểm {inTop && '· trong top 5'}
      </p>
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Main block
// --------------------------------------------------------------------------
export const LeaderboardBlock: React.FC = () => {
  const currentUser = useAuthStore((s) => s.user)
  const myUserId = currentUser?.id ?? null

  // Tuần đang xem. Mặc định = tuần hiện tại.
  const [week, setWeek] = useState<string>(() => currentWeekKey())

  const {
    data: board,
    isLoading,
    isError,
    error,
    refetch,
  } = useLeaderboard(week)
  const { data: myRank } = useMyRank(week)
  const refresh = useRefreshLeaderboard()

  const viewingCurrentWeek = isCurrentWeek(week)

  /** Tuần hiện tại — để disable nút "›" và giới hạn lùi. */
  const thisWeek = useMemo(() => currentWeekKey(), [])

  // ------------------------------------------------------------------
  // Chuyển tuần
  // ------------------------------------------------------------------
  const goPrev = () => setWeek((w) => shiftWeekKey(w, -1))
  const goNext = () => {
    setWeek((w) => {
      const next = shiftWeekKey(w, 1)
      // Không cho đi quá tuần hiện tại
      return next > thisWeek ? w : next
    })
  }

  /** Có thể lùi thêm tuần nữa không (giới hạn retention 8 tuần của BE). */
  const canGoPrev = useMemo(() => {
    const prev = shiftWeekKey(week, -1)
    // Đếm số tuần lùi từ tuần hiện tại
    let cursor = thisWeek
    let count = 0
    while (cursor > prev && count < 20) {
      cursor = shiftWeekKey(cursor, -1)
      count += 1
    }
    return count <= MAX_WEEKS_BACK
  }, [week, thisWeek])

  // ------------------------------------------------------------------
  // Reload
  // ------------------------------------------------------------------
  const handleRefresh = () => {
    refresh.mutate(
      { week },
      {
        onError: (err) => {
          // BE có cooldown 30s → 429. Hiện thông báo rõ ràng thay vì lỗi chung.
          if (err?.response?.status === 429) {
            void message.warning(
              'Vui lòng đợi vài giây rồi thử lại (giới hạn 1 lần / 30 giây).',
            )
            return
          }
          void message.error('Không tải lại được bảng xếp hạng. Vui lòng thử lại.')
        },
      },
    )
  }

  // ------------------------------------------------------------------
  // Render
  // ------------------------------------------------------------------
  if (isLoading && !board) return <SkeletonBlock />
  if (isError && !board) {
    return <ErrorBlock message={error?.message} onRetry={() => void refetch()} />
  }

  const entries = board?.entries ?? []

  return (
    <section className="space-y-3" aria-label="Bảng xếp hạng tuần">
      {/* ---------- Header + bộ chọn tuần ---------- */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
            <Trophy className="w-4 h-4 text-amber-500" />
            Bảng xếp hạng tuần
          </h2>
          <p className="text-xs text-gray-500 mt-0.5 flex items-center gap-1.5">
            {formatWeekLabel(week)}
            {board?.totalPlayers != null && (
              <span className="text-gray-400">
                · {formatCompactNumber(board.totalPlayers)} người có điểm
              </span>
            )}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* Chọn tuần */}
          <div className="flex items-center gap-1 bg-white rounded-md border border-gray-200/80 shadow-xs p-0.5">
            <Tooltip title="Tuần trước">
              <Button
                type="text"
                size="small"
                onClick={goPrev}
                disabled={!canGoPrev}
                icon={<ChevronLeft className="w-3.5 h-3.5" />}
                aria-label="Tuần trước"
              />
            </Tooltip>
            <span className="text-[11px] font-semibold text-gray-700 px-1 tabular-nums min-w-[58px] text-center">
              {week}
            </span>
            <Tooltip title="Tuần sau">
              <Button
                type="text"
                size="small"
                onClick={goNext}
                disabled={viewingCurrentWeek}
                icon={<ChevronRight className="w-3.5 h-3.5" />}
                aria-label="Tuần sau"
              />
            </Tooltip>
          </div>

          {/* Reload */}
          <Tooltip title="Tải lại bảng xếp hạng từ dữ liệu gốc">
            <Button
              size="small"
              icon={<RefreshCw className="w-3.5 h-3.5" />}
              onClick={handleRefresh}
              loading={refresh.isPending}
              aria-label="Tải lại bảng xếp hạng"
            />
          </Tooltip>
        </div>
      </div>

      {/* ---------- Bảng + hạng của tôi ---------- */}
      <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5">
        {entries.length === 0 ? (
          <EmptyBlock isCurrentWeek={viewingCurrentWeek} />
        ) : (
          <>
            {/* Header cột */}
            <div className="flex items-center gap-2.5 px-2 pb-1.5 mb-0.5 border-b border-gray-100">
              <span className="w-6 shrink-0 text-[9px] font-bold text-gray-400 uppercase tracking-wide">
                #
              </span>
              <span className="w-[28px] shrink-0" />
              <span className="flex-1 text-[9px] font-bold text-gray-400 uppercase tracking-wide">
                Người học
              </span>
              <span className="text-right text-[9px] font-bold text-gray-400 uppercase tracking-wide">
                Điểm
              </span>
            </div>

            <div className="space-y-0.5">
              {entries.map((entry) => (
                <LeaderboardRow
                  key={entry.userId}
                  entry={entry}
                  isMe={myUserId != null && entry.userId === myUserId}
                />
              ))}
            </div>

            <MyRankCard
              rank={myRank?.rank ?? null}
              score={myRank?.score ?? null}
              totalPlayers={myRank?.totalPlayers ?? null}
              inTop={myRank?.inTop ?? false}
            />
          </>
        )}
      </div>

      {/* ---------- Chú thích ---------- */}
      <p className="text-[10px] text-gray-400 flex items-start gap-1">
        <Users className="w-3 h-3 shrink-0 mt-px" />
        <span>
          Điểm = số lần tra từ đã tính điểm + số bài học hoàn thành. Cùng điểm sẽ
          cùng hạng.
        </span>
      </p>
    </section>
  )
}

export default LeaderboardBlock
