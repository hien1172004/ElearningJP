import React, { useState, useEffect } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import {
  ShieldCheck,
  Check,
  Copy,
  Clock,
  QrCode,
  Tag,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
} from 'lucide-react'
import { toast } from 'sonner'
import { useAuthStore } from '@/stores/useAuthStore'
import { useAuthModalStore } from '@/stores/useAuthModalStore'
import { useSubscriptionStore } from '@/stores/useSubscriptionStore'
import { subscriptionService } from '@/services/subscription.service'
import type { SubscriptionPlan, PaymentOrder, ApplyCouponResponse } from '@/types'

const FALLBACK_PLANS: SubscriptionPlan[] = [
  {
    id: 'PRO_1M',
    name: 'PRO 1 Tháng',
    priceVnd: 99000,
    originalPriceVnd: 129000,
    durationDays: 30,
    monthlyTokens: 50000,
    maxPracticeWords: 9999,
    isActive: true,
  },
  {
    id: 'PRO_1Y',
    name: 'PRO 1 Năm',
    priceVnd: 699000,
    originalPriceVnd: 1188000,
    durationDays: 365,
    monthlyTokens: 500000,
    maxPracticeWords: 9999,
    isActive: true,
  },
  {
    id: 'PRO_LIFETIME',
    name: 'PRO Trọn Đời',
    priceVnd: 1299000,
    originalPriceVnd: 2499000,
    durationDays: undefined,
    monthlyTokens: 100000,
    maxPracticeWords: 9999,
    isActive: true,
  },
]

export const PricingCheckoutPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const planQuery = searchParams.get('plan') || 'PRO_1Y'

  const [plans, setPlans] = useState<SubscriptionPlan[]>(FALLBACK_PLANS)
  const [selectedPlanId, setSelectedPlanId] = useState<string>(planQuery)
  const [currentStep, setCurrentStep] = useState<1 | 2>(1)

  // Step 1: Coupon
  const [couponCode, setCouponCode] = useState<string>('')
  const [couponResult, setCouponResult] = useState<ApplyCouponResponse | null>(null)
  const [isApplyingCoupon, setIsApplyingCoupon] = useState<boolean>(false)

  // Step 2: PayOS Order & VietQR
  const [isCreatingOrder, setIsCreatingOrder] = useState<boolean>(false)
  const [order, setOrder] = useState<PaymentOrder | null>(null)
  const [copiedField, setCopiedField] = useState<string | null>(null)
  const [timeLeftSeconds, setTimeLeftSeconds] = useState<number>(600)
  const [isPaid, setIsPaid] = useState<boolean>(false)

  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)
  const openLogin = useAuthModalStore((s) => s.openLogin)
  const { refreshAll } = useSubscriptionStore()

  // Fetch public plans
  useEffect(() => {
    const fetchPlans = async () => {
      try {
        const res = await subscriptionService.getPublicPlans()
        if (res.success && res.data) {
          const proPlans = res.data.filter((p) => p.id !== 'FREE')
          setPlans(proPlans)
          if (proPlans.some((p) => p.id === planQuery)) {
            setSelectedPlanId(planQuery)
          } else if (proPlans.length > 0) {
            setSelectedPlanId(proPlans[0].id)
          }
        }
      } catch {
        // Fallback initialized
      }
    }
    void fetchPlans()
  }, [planQuery])

  // Timer countdown for Step 2
  useEffect(() => {
    if (currentStep !== 2 || !order || isPaid) return

    setTimeLeftSeconds(600)
    const interval = setInterval(() => {
      setTimeLeftSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(interval)
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(interval)
  }, [currentStep, order, isPaid])

  // Polling order status for Step 2
  useEffect(() => {
    if (currentStep !== 2 || !order || isPaid) return

    let cancelled = false
    const poll = async () => {
      try {
        const res = await subscriptionService.getOrderStatus(order.orderCode)
        if (cancelled) return
        if (res.success && res.data && res.data.status === 'PAID') {
          setIsPaid(true)
          toast.success('Thanh toán thành công! Gói PRO đã được kích hoạt! 🎉')

          if (user) {
            setUser({ ...user, isPro: true })
          }
          await refreshAll()

          setTimeout(() => {
            navigate('/courses')
          }, 3000)
        }
      } catch {
        // Silent poll error
      }
    }

    const interval = setInterval(poll, 2500)
    void poll()

    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [currentStep, order, isPaid, navigate, refreshAll, user, setUser])

  const selectedPlan = plans.find((p) => p.id === selectedPlanId) || plans[0]

  // Handle coupon with strict validation
  const handleApplyCoupon = async () => {
    const trimmed = couponCode.trim()
    if (!trimmed) {
      toast.error('Vui lòng nhập mã giảm giá')
      return
    }

    setIsApplyingCoupon(true)
    try {
      const res = await subscriptionService.applyCoupon({
        code: trimmed,
        planId: selectedPlan.id,
      })

      if (res.success && res.data) {
        if (res.data.isValid) {
          setCouponResult(res.data)
          toast.success(res.data.message || `Đã áp dụng mã ${res.data.code}`)
        } else {
          setCouponResult(null)
          toast.error(res.data.message || 'Mã giảm giá không hợp lệ hoặc đã hết hạn')
        }
      } else {
        setCouponResult(null)
        toast.error('Mã giảm giá không hợp lệ hoặc đã hết hạn')
      }
    } catch (err: any) {
      setCouponResult(null)
      toast.error(err.response?.data?.message || 'Mã giảm giá không hợp lệ hoặc đã hết hạn')
    } finally {
      setIsApplyingCoupon(false)
    }
  }

  // Handle proceed to Step 2
  const handleProceedToPayment = async () => {
    if (!isAuthenticated) {
      openLogin(`/pricing/checkout?plan=${selectedPlanId}`)
      return
    }

    setIsCreatingOrder(true)
    try {
      const res = await subscriptionService.createPaymentOrder({
        planId: selectedPlan.id,
        couponCode: couponResult?.isValid ? couponResult.code : undefined,
      })

      if (res.success && res.data) {
        setOrder(res.data)
        setCurrentStep(2)
        window.scrollTo({ top: 0, behavior: 'smooth' })
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tạo đơn hàng. Vui lòng thử lại.')
    } finally {
      setIsCreatingOrder(false)
    }
  }

  const copyToClipboard = (text: string, fieldName: string) => {
    navigator.clipboard.writeText(text)
    setCopiedField(fieldName)
    toast.success(`Đã sao chép ${fieldName}!`)
    setTimeout(() => setCopiedField(null), 2000)
  }

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60)
    const s = secs % 60
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  const finalPrice = couponResult?.isValid ? couponResult.finalAmountVnd : selectedPlan.priceVnd
  const paymentDescription = order ? `EJP ${order.orderCode}` : ''
  const qrImageSrc = order?.qrCode?.startsWith('http')
    ? order.qrCode
    : order
      ? `https://img.vietqr.io/image/MB-0348888888-compact2.png?amount=${order.amountVnd}&addInfo=${encodeURIComponent(paymentDescription)}&accountName=ELEARNINGJP`
      : ''

  const getDurationLabel = (id: string) => {
    if (id === 'PRO_LIFETIME') return 'Trọn đời (Vĩnh viễn)'
    if (id === 'PRO_1Y') return '1 Năm (12 tháng)'
    return '1 Tháng (30 ngày)'
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 sm:py-12 space-y-6">
      {/* Navigation Header */}
      <div className="flex items-center justify-between">
        <Link
          to="/pricing"
          className="inline-flex items-center gap-1.5 text-xs sm:text-sm text-nibi hover:text-sumi transition font-medium"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại bảng giá</span>
        </Link>

        {/* Subtle step indicator */}
        <div className="text-xs text-nibi font-medium">
          Bước {currentStep} / 2: {currentStep === 1 ? 'Xác nhận đơn hàng' : 'Quét mã VietQR'}
        </div>
      </div>

      {/* =================================================================== */}
      {/* BƯỚC 1: XÁC NHẬN ĐƠN HÀNG (Thiết kế tập trung, sạch sẽ, không rườm rà) */}
      {/* =================================================================== */}
      {currentStep === 1 && (
        <div className="bg-surface rounded-2xl border border-suna p-6 sm:p-8 shadow-xs space-y-6">
          {/* Header */}
          <div className="border-b border-suna pb-4">
            <h1 className="text-xl sm:text-2xl font-bold text-sumi">
              Xác nhận thông tin gói cước
            </h1>
            <p className="text-xs text-nibi mt-1">
              Kiểm tra thông tin chi tiết và áp dụng ưu đãi trước khi tiến hành thanh toán
            </p>
          </div>

          {/* Chi tiết gói cước */}
          <div className="space-y-4 text-xs sm:text-sm">
            <div className="flex justify-between items-center">
              <span className="text-nibi">Gói cước nâng cấp:</span>
              <div className="flex items-center gap-2">
                <span className="font-bold text-sumi text-base">{selectedPlan.name}</span>
                <Link
                  to="/pricing"
                  className="text-xs text-ai hover:underline font-semibold"
                >
                  (Đổi gói)
                </Link>
              </div>
            </div>

            <div className="flex justify-between items-center">
              <span className="text-nibi">Thời hạn sử dụng:</span>
              <span className="font-semibold text-sumi">
                {getDurationLabel(selectedPlan.id)}
              </span>
            </div>

            <div className="flex justify-between items-center">
              <span className="text-nibi">Giá niêm yết:</span>
              <div className="flex items-center gap-2">
                {selectedPlan.originalPriceVnd && selectedPlan.originalPriceVnd > selectedPlan.priceVnd && (
                  <span className="text-xs text-nibi line-through font-mono">
                    {selectedPlan.originalPriceVnd.toLocaleString('vi-VN')} đ
                  </span>
                )}
                <span className="font-semibold text-sumi font-mono">
                  {selectedPlan.priceVnd.toLocaleString('vi-VN')} đ
                </span>
              </div>
            </div>

            {/* Ô nhập mã giảm giá */}
            <div className="pt-3 border-t border-suna/70">
              <div className="flex justify-between items-center mb-2">
                <span className="font-semibold text-sumi text-xs">Mã giảm giá / Voucher:</span>
                {couponResult?.isValid && (
                  <button
                    type="button"
                    onClick={() => {
                      setCouponResult(null)
                      setCouponCode('')
                    }}
                    className="text-xs text-rose-600 hover:underline cursor-pointer"
                  >
                    Hủy mã
                  </button>
                )}
              </div>

              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Tag className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-nibi" />
                  <input
                    type="text"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        void handleApplyCoupon()
                      }
                    }}
                    placeholder="Nhập mã voucher (ví dụ: ELEARNING2026)"
                    className="w-full bg-kinari/60 border border-suna rounded-xl pl-9 pr-3 py-2 text-xs font-mono font-bold text-sumi uppercase placeholder:text-nibi focus:outline-hidden focus:border-ai transition"
                  />
                </div>
                <button
                  type="button"
                  onClick={handleApplyCoupon}
                  disabled={isApplyingCoupon || !couponCode.trim()}
                  className="px-4 py-2 rounded-xl bg-ai hover:bg-ai/90 disabled:opacity-50 text-white text-xs font-bold transition cursor-pointer"
                >
                  {isApplyingCoupon ? 'Đang kiểm tra...' : 'Áp dụng'}
                </button>
              </div>

              {couponResult?.isValid && (
                <div className="flex justify-between items-center text-xs text-emerald-600 dark:text-emerald-400 font-semibold mt-2.5">
                  <span className="flex items-center gap-1.5">
                    <Check className="w-3.5 h-3.5" />
                    <span>Đã áp dụng mã {couponResult.code}</span>
                  </span>
                  <span className="font-mono">
                    -{couponResult.discountAmountVnd.toLocaleString('vi-VN')} đ
                  </span>
                </div>
              )}
            </div>

            {/* Tổng tiền thanh toán */}
            <div className="pt-4 border-t border-suna flex justify-between items-baseline">
              <span className="text-sm font-bold text-sumi">Tổng thanh toán:</span>
              <div className="text-right">
                <span className="text-2xl sm:text-3xl font-black text-ai dark:text-indigo-300 font-mono tracking-tight">
                  {finalPrice.toLocaleString('vi-VN')}
                </span>
                <span className="text-sm text-ai dark:text-indigo-300 font-bold ml-1">đ</span>
              </div>
            </div>
          </div>

          {/* Phương thức thanh toán */}
          <div className="pt-2">
            <label className="block text-xs font-semibold text-nibi uppercase tracking-wider mb-2">
              Phương thức thanh toán
            </label>
            <div className="p-3.5 rounded-xl border border-ai/50 bg-ai/5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-lg bg-white p-1 border border-suna flex items-center justify-center shrink-0">
                  <QrCode className="w-5 h-5 text-ai" />
                </div>
                <div>
                  <p className="font-bold text-xs sm:text-sm text-sumi">
                    Chuyển khoản VietQR (Napas 247)
                  </p>
                  <p className="text-[11px] text-nibi">
                    Quét mã qua app ngân hàng • Kích hoạt tự động sau 3–5 giây
                  </p>
                </div>
              </div>
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            </div>
          </div>

          {/* Thông tin tài khoản nhận gói PRO */}
          {user && (
            <div className="p-3 rounded-xl bg-kinari/70 border border-suna/70 text-xs text-nibi flex items-center justify-between">
              <span>Tài khoản kích hoạt:</span>
              <strong className="text-sumi">{user.email}</strong>
            </div>
          )}

          {/* Action CTA Button */}
          <div className="pt-2">
            <button
              type="button"
              onClick={handleProceedToPayment}
              disabled={isCreatingOrder}
              className="w-full py-3.5 px-4 rounded-xl bg-ai hover:bg-ai/90 disabled:opacity-50 text-white font-bold text-sm tracking-wide shadow-xs active:scale-[0.99] transition flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>{isCreatingOrder ? 'Đang tạo đơn PayOS...' : 'Tiến hành quét mã VietQR'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <div className="flex items-center justify-center gap-1.5 text-[11px] text-nibi mt-3">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
              <span>Giao dịch bảo mật 100% qua cổng thanh toán PayOS</span>
            </div>
          </div>
        </div>
      )}

      {/* =================================================================== */}
      {/* BƯỚC 2: MÀN HÌNH QUÉT MÃ VIETQR (Chuẩn mực, dễ chuyển khoản) */}
      {/* =================================================================== */}
      {currentStep === 2 && order && (
        <div className="bg-surface rounded-2xl border border-suna p-6 sm:p-8 shadow-xs space-y-6">
          {isPaid ? (
            /* Trạng thái thanh toán thành công */
            <div className="py-8 text-center space-y-4">
              <div className="w-20 h-20 rounded-full bg-emerald-500/15 text-emerald-600 flex items-center justify-center mx-auto shadow-md">
                <CheckCircle2 className="w-12 h-12" />
              </div>
              <h2 className="text-2xl font-bold text-sumi">
                Kích hoạt gói PRO thành công! 🎉
              </h2>
              <p className="text-xs sm:text-sm text-nibi max-w-md mx-auto leading-relaxed">
                Hệ thống đã nhận được tiền và kích hoạt toàn bộ quyền lợi PRO cho tài khoản <strong className="text-sumi">{user?.email}</strong>.
              </p>
              <div className="pt-2 flex justify-center gap-3">
                <Link
                  to="/courses"
                  className="px-5 py-2.5 rounded-xl bg-ai text-white font-bold text-xs hover:bg-ai/90 transition"
                >
                  Bắt đầu học ngay
                </Link>
                <Link
                  to="/exams"
                  className="px-5 py-2.5 rounded-xl bg-kinari border border-suna text-sumi font-bold text-xs hover:bg-surface transition"
                >
                  Luyện thi JLPT
                </Link>
              </div>
            </div>
          ) : (
            /* Khung quét mã VietQR */
            <div className="space-y-6">
              <div className="flex items-center justify-between border-b border-suna pb-4">
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-sumi">
                    Quét mã để kích hoạt ElearningJP PRO
                  </h2>
                  <p className="text-xs text-nibi mt-0.5">
                    Mở app ngân hàng bất kỳ (VCB, MB, Techcombank...) để quét mã thanh toán
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setCurrentStep(1)}
                  className="text-xs text-nibi hover:text-sumi font-medium underline cursor-pointer"
                >
                  Đổi thông tin
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-center">
                {/* Cột QR Code (6 cols) - Kích thước lớn, dễ quét */}
                <div className="md:col-span-6 flex flex-col items-center justify-center">
                  <div className="w-full max-w-[320px] bg-white rounded-2xl p-4 border-2 border-suna shadow-sm relative group">
                    {/* Subtle corner scanner guides */}
                    <div className="absolute top-2.5 left-2.5 w-4 h-4 border-t-2 border-l-2 border-amber-500 rounded-tl-sm pointer-events-none"></div>
                    <div className="absolute top-2.5 right-2.5 w-4 h-4 border-t-2 border-r-2 border-amber-500 rounded-tr-sm pointer-events-none"></div>
                    <div className="absolute bottom-2.5 left-2.5 w-4 h-4 border-b-2 border-l-2 border-amber-500 rounded-bl-sm pointer-events-none"></div>
                    <div className="absolute bottom-2.5 right-2.5 w-4 h-4 border-b-2 border-r-2 border-amber-500 rounded-br-sm pointer-events-none"></div>

                    <img
                      src={qrImageSrc}
                      alt="VietQR PayOS"
                      className="w-full aspect-square object-contain rounded-lg"
                    />
                  </div>

                  {/* Đếm ngược */}
                  <div className="mt-4 inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-500/20">
                    <Clock className="w-3.5 h-3.5 animate-spin text-amber-600 dark:text-amber-400" />
                    <span>Hết hạn sau: <strong className="font-mono text-sm">{formatTime(timeLeftSeconds)}</strong></span>
                  </div>

                  <p className="text-[11px] text-nibi mt-2 text-center">
                    * Mở app ngân hàng bất kỳ để quét mã tự động
                  </p>
                </div>

                {/* Cột Thông tin chuyển khoản (6 cols) */}
                <div className="md:col-span-6 space-y-3.5 text-xs">
                  {/* Số tiền */}
                  <div className="bg-kinari/70 border border-suna rounded-xl p-3.5">
                    <div className="flex justify-between items-center text-nibi mb-1">
                      <span>Số tiền cần chuyển:</span>
                      <button
                        type="button"
                        onClick={() => copyToClipboard(order.amountVnd.toString(), 'Số tiền')}
                        className="text-ai hover:underline font-semibold cursor-pointer"
                      >
                        {copiedField === 'Số tiền' ? 'Đã chép ✓' : 'Sao chép'}
                      </button>
                    </div>
                    <div className="text-2xl font-black text-ai dark:text-indigo-300 font-mono">
                      {order.amountVnd.toLocaleString('vi-VN')} đ
                    </div>
                  </div>

                  {/* Nội dung chuyển khoản (QUAN TRỌNG) */}
                  <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-3.5">
                    <div className="flex justify-between items-center text-amber-700 dark:text-amber-300 font-semibold mb-1">
                      <span>Nội dung chuyển khoản (Bắt buộc):</span>
                      <button
                        type="button"
                        onClick={() => copyToClipboard(paymentDescription, 'Nội dung chuyển khoản')}
                        className="px-2 py-0.5 rounded bg-amber-500 text-white font-bold text-[11px] hover:bg-amber-600 transition cursor-pointer"
                      >
                        {copiedField === 'Nội dung chuyển khoản' ? 'Đã chép ✓' : 'Sao chép mã'}
                      </button>
                    </div>
                    <div className="font-mono text-base font-bold text-sumi bg-surface px-3 py-1.5 rounded-lg border border-amber-500/30 select-all tracking-wider">
                      {paymentDescription}
                    </div>
                    <p className="text-[10px] text-amber-700 dark:text-amber-400 mt-1">
                      * Giữ nguyên nội dung chuyển khoản để hệ thống kích hoạt tức thì.
                    </p>
                  </div>

                  {/* Thông tin tài khoản thụ hưởng */}
                  <div className="bg-kinari/50 border border-suna rounded-xl p-3 space-y-2 text-sumi">
                    <div className="flex justify-between items-center">
                      <span className="text-nibi">Ngân hàng:</span>
                      <span className="font-semibold text-sumi">MB Bank (Quân Đội)</span>
                    </div>

                    <div className="flex justify-between items-center">
                      <span className="text-nibi">Số tài khoản:</span>
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono font-bold text-sumi">0348888888</span>
                        <button
                          type="button"
                          onClick={() => copyToClipboard('0348888888', 'Số tài khoản')}
                          className="text-nibi hover:text-sumi cursor-pointer"
                          title="Sao chép số tài khoản"
                        >
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    <div className="flex justify-between items-center border-t border-suna/60 pt-1.5">
                      <span className="text-nibi">Chủ tài khoản:</span>
                      <span className="font-semibold text-sumi">ELEARNINGJP</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Live listening pulse footer */}
              <div className="pt-4 border-t border-suna flex items-center justify-between text-xs text-nibi">
                <div className="flex items-center gap-2">
                  <span className="relative flex h-2.5 w-2.5">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                  </span>
                  <span>Đang tự động lắng nghe giao dịch qua cổng PayOS...</span>
                </div>

                <div className="flex items-center gap-1 text-[11px]">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Napas 247 an toàn</span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
