import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles, ChevronDown, RotateCw, Crown } from 'lucide-react'
import { Popover, Tooltip } from 'antd'
import { useAuthStore } from '@/stores/useAuthStore'
import { useSubscriptionStore } from '@/stores/useSubscriptionStore'
import { cn } from '@/utils/cn'

export const TokenBalanceWidget: React.FC = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const user = useAuthStore((s) => s.user)
  const { wallet, isPro, fetchWallet, fetchSubscription } = useSubscriptionStore()
  const [popoverOpen, setPopoverOpen] = useState<boolean>(false)

  useEffect(() => {
    if (isAuthenticated) {
      void fetchWallet()
      void fetchSubscription()
    }
  }, [isAuthenticated, fetchWallet, fetchSubscription])

  // Guest state
  if (!isAuthenticated) {
    return (
      <Link
        to="/pricing"
        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold text-amber-700 dark:text-amber-300 bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 transition"
      >
        <Crown className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
        <span>Gói PRO</span>
      </Link>
    )
  }

  const isUserProAccount = Boolean(isPro || user?.isPro)
  const maxQuota = 3500 // 3000 trial + 500 daily
  const availableTokens = wallet ? wallet.totalAvailableTokens : maxQuota
  const percent = isUserProAccount
    ? 100
    : Math.min(100, Math.max(0, Math.round((availableTokens / maxQuota) * 100)))

  // Popover Card Content (Chuẩn Mazii Image 2)
  const popoverContent = (
    <div className="w-72 p-1 text-sumi">
      {/* Top Header Badge */}
      <div className="flex items-center justify-between">
        <span
          className={cn(
            'px-2.5 py-0.5 rounded-md font-bold text-xs',
            isUserProAccount
              ? 'bg-blue-600 text-white'
              : 'bg-kinari text-sumi border border-suna',
          )}
        >
          {isUserProAccount ? 'Premium' : 'Gói Miễn phí'}
        </span>

        {!isUserProAccount && (
          <Link
            to="/pricing"
            onClick={() => setPopoverOpen(false)}
            className="text-xs text-blue-600 dark:text-blue-400 hover:underline font-semibold"
          >
            Nâng cấp PRO →
          </Link>
        )}
      </div>

      {/* Dashed Separator */}
      <div className="border-b border-dashed border-suna/70 my-3" />

      {/* Token AI Header */}
      <p className="text-xs font-bold text-purple-600 dark:text-purple-400 mb-1">
        Token AI
      </p>

      {/* Percentage & Balance Numbers */}
      <div className="flex items-baseline justify-between">
        <span className="text-2xl font-black text-sumi font-mono">
          {percent}%
        </span>
        <span className="text-xs text-nibi font-medium font-mono">
          {isUserProAccount
            ? 'Không giới hạn Token'
            : `Còn ${availableTokens.toLocaleString()} / ${maxQuota.toLocaleString()} Token`}
        </span>
      </div>

      {/* Progress Bar */}
      <div className="w-full h-2 rounded-full bg-suna mt-2 overflow-hidden">
        <div
          className={cn(
            'h-full rounded-full transition-all duration-300',
            isUserProAccount
              ? 'bg-blue-600 w-full'
              : percent > 20
              ? 'bg-blue-600'
              : 'bg-rose-500',
          )}
          style={{ width: `${percent}%` }}
        />
      </div>

      {/* Daily Reset Countdown info */}
      <div className="mt-3 pt-2.5 border-t border-suna/60 flex items-center justify-between text-[11px] text-nibi">
        <span className="flex items-center gap-1.5">
          <RotateCw className="w-3.5 h-3.5 text-nibi" />
          <span>Đặt lại mỗi ngày lúc 00:00</span>
        </span>
        <span className="font-semibold text-sumi">+500 Token</span>
      </div>

      {/* Upgrade Call to action for Free users */}
      {!isUserProAccount && (
        <Link
          to="/pricing"
          onClick={() => setPopoverOpen(false)}
          className="mt-3 block text-center py-2 px-3 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-600 hover:to-amber-700 text-white font-bold text-xs transition shadow-xs"
        >
          👑 Mở khóa PRO không giới hạn Token
        </Link>
      )}
    </div>
  )

  // TopBar Pill Trigger Button (Chuẩn Mazii)
  return (
    <Popover
      content={popoverContent}
      trigger="click"
      open={popoverOpen}
      onOpenChange={setPopoverOpen}
      placement="bottomRight"
      overlayClassName="token-ai-popover"
    >
      <Tooltip title="Token AI" placement="bottom">
        <button
          type="button"
          className={cn(
            'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold transition border cursor-pointer select-none',
            popoverOpen
              ? 'border-blue-500 bg-blue-50/50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300'
              : 'border-suna bg-surface hover:bg-kinari text-sumi shadow-2xs',
          )}
        >
          {/* Gradient Star Icon */}
          <span className="w-5 h-5 rounded-full bg-gradient-to-tr from-purple-600 via-indigo-600 to-blue-500 flex items-center justify-center shrink-0 shadow-2xs">
            <Sparkles className="w-3 h-3 text-white fill-white" />
          </span>

          <span className="font-mono">{percent}%</span>

          <ChevronDown
            className={cn(
              'w-3.5 h-3.5 text-nibi transition-transform',
              popoverOpen && 'rotate-180',
            )}
          />
        </button>
      </Tooltip>
    </Popover>
  )
}
