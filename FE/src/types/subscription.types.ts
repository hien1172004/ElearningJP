export interface SubscriptionPlan {
  id: string // 'FREE' | 'PRO_1M' | 'PRO_1Y' | 'PRO_LIFETIME'
  name: string
  priceVnd: number
  originalPriceVnd?: number
  durationDays?: number
  monthlyTokens: number
  maxPracticeWords: number
  isActive: boolean
}

export interface UserSubscription {
  planId: string
  planName: string
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED'
  isPro: boolean
  startAt: string
  endAt?: string
}

export interface UserTokenWallet {
  trialBalance: number
  dailyFreeBalance: number
  subscriptionBalance: number
  totalAvailableTokens: number
  lastDailyReset: string
}

export interface PaymentOrder {
  id: string
  orderCode: number
  planId: string
  planName: string
  originalAmountVnd: number
  discountAmountVnd: number
  amountVnd: number
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'EXPIRED'
  checkoutUrl?: string
  qrCode?: string
  couponCode?: string
  createdAt: string
  paidAt?: string
}

export interface ApplyCouponResponse {
  isValid: boolean
  code: string
  discountAmountVnd: number
  finalAmountVnd: number
  message: string
}

export interface CreatePaymentOrderRequest {
  planId: string
  couponCode?: string
}

export interface ApplyCouponRequest {
  code: string
  planId: string
}

