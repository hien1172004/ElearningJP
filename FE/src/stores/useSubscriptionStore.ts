import { create } from 'zustand'
import { subscriptionService } from '@/services/subscription.service'
import type { UserSubscription, UserTokenWallet } from '@/types'

interface SubscriptionState {
  subscription: UserSubscription | null
  wallet: UserTokenWallet | null
  isPro: boolean
  isLoading: boolean
  isPaywallOpen: boolean
  paywallReason: string

  openPaywall: (reason?: string) => void
  closePaywall: () => void
  fetchSubscription: () => Promise<void>
  fetchWallet: () => Promise<void>
  refreshAll: () => Promise<void>
  setWallet: (wallet: UserTokenWallet | null) => void
}

export const useSubscriptionStore = create<SubscriptionState>((set, get) => ({
  subscription: null,
  wallet: null,
  isPro: false,
  isLoading: false,
  isPaywallOpen: false,
  paywallReason: '',

  openPaywall: (reason?: string) => {
    set({
      isPaywallOpen: true,
      paywallReason: reason || 'Tính năng này dành riêng cho hội viên PRO hoặc đã hết lượt dùng thử.',
    })
  },

  closePaywall: () => {
    set({ isPaywallOpen: false, paywallReason: '' })
  },

  fetchSubscription: async () => {
    try {
      const res = await subscriptionService.getMySubscription()
      if (res.success && res.data) {
        set({
          subscription: res.data,
          isPro: res.data.isPro,
        })
      }
    } catch {
      // User might be guest or not authenticated
    }
  },

  fetchWallet: async () => {
    try {
      const res = await subscriptionService.getMyWallet()
      if (res.success && res.data) {
        set({ wallet: res.data })
      }
    } catch {
      // ignore
    }
  },

  refreshAll: async () => {
    set({ isLoading: true })
    try {
      await Promise.allSettled([get().fetchSubscription(), get().fetchWallet()])
    } finally {
      set({ isLoading: false })
    }
  },

  setWallet: (wallet) => {
    set({ wallet })
  },
}))

