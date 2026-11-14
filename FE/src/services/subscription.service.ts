import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  SubscriptionPlan,
  UserSubscription,
  UserTokenWallet,
  PaymentOrder,
  ApplyCouponRequest,
  ApplyCouponResponse,
  CreatePaymentOrderRequest,
} from '@/types'

export const subscriptionService = {
  /**
   * Lấy danh sách các gói cước công khai (FREE, PRO_1M, PRO_1Y, PRO_LIFETIME)
   */
  getPublicPlans: async (): Promise<ApiResponse<SubscriptionPlan[]>> => {
    return axiosClient.get('/subscriptions/plans')
  },

  /**
   * Lấy thông tin gói đăng ký hiện tại của user đang đăng nhập
   */
  getMySubscription: async (): Promise<ApiResponse<UserSubscription>> => {
    return axiosClient.get('/subscriptions/my-subscription')
  },

  /**
   * Lấy số dư ví token AI speaking của user
   */
  getMyWallet: async (): Promise<ApiResponse<UserTokenWallet>> => {
    return axiosClient.get('/subscriptions/my-wallet')
  },

  /**
   * Áp dụng mã giảm giá / khuyến mãi để tính giá sau giảm
   */
  applyCoupon: async (payload: ApplyCouponRequest): Promise<ApiResponse<ApplyCouponResponse>> => {
    return axiosClient.post('/subscriptions/apply-coupon', payload)
  },

  /**
   * Tạo đơn hàng PayOS VietQR nâng cấp PRO
   */
  createPaymentOrder: async (payload: CreatePaymentOrderRequest): Promise<ApiResponse<PaymentOrder>> => {
    return axiosClient.post('/subscriptions/create-order', payload)
  },

  /**
   * Tra cứu trạng thái đơn hàng theo mã orderCode (dùng để polling sau khi quét QR)
   */
  getOrderStatus: async (orderCode: number): Promise<ApiResponse<PaymentOrder>> => {
    return axiosClient.get(`/subscriptions/orders/${orderCode}`)
  },
}

