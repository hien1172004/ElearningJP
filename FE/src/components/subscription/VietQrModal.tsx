import React, { useState, useEffect } from 'react'
import { Modal } from 'antd'
import {
  Copy,
  Check,
  Clock,
  ShieldCheck,
  CheckCircle2,
  X,
  CreditCard,
  Building2,
  Sparkles,
  Zap,
} from 'lucide-react'
import { toast } from 'sonner'
import type { PaymentOrder } from '@/types'
import { subscriptionService } from '@/services/subscription.service'
import { useSubscriptionStore } from '@/stores/useSubscriptionStore'
import { useAuthStore } from '@/stores/useAuthStore'

interface VietQrModalProps {
  order: PaymentOrder | null
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export const VietQrModal: React.FC<VietQrModalProps> = ({
  order,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [copiedField, setCopiedField] = useState<string | null>(null)
  const [timeLeftSeconds, setTimeLeftSeconds] = useState<number>(600) // 10 minutes default
  const [isPaid, setIsPaid] = useState<boolean>(false)
  const refreshAll = useSubscriptionStore((s) => s.refreshAll)
  const user = useAuthStore((s) => s.user)
  const setUser = useAuthStore((s) => s.setUser)

  // Timer countdown
  useEffect(() => {
    if (!isOpen || !order || isPaid) return

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
  }, [isOpen, order, isPaid])

  // Polling order status every 2.5 seconds
  useEffect(() => {
    if (!isOpen || !order || isPaid) return

    let cancelled = false

    const poll = async () => {
      try {
        const res = await subscriptionService.getOrderStatus(order.orderCode)
        if (cancelled) return
        if (res.success && res.data && res.data.status === 'PAID') {
          setIsPaid(true)
          toast.success('Thanh toán thành công! Chào mừng bạn đến với ElearningJP PRO! 🎉')

          // Update user state and store
          if (user) {
            setUser({ ...user, isPro: true })
          }
          await refreshAll()

          setTimeout(() => {
            onSuccess()
            onClose()
          }, 2000)
        }
      } catch {
        // Polling error silently handled
      }
    }

    const interval = setInterval(poll, 2500)
    void poll()

    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [isOpen, order, isPaid, onSuccess, onClose, refreshAll, user, setUser])

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

  if (!order) return null

  const paymentDescription = `EJP ${order.orderCode}`
  const qrImageSrc = order.qrCode?.startsWith('http')
    ? order.qrCode
    : `https://img.vietqr.io/image/MB-0348888888-compact2.png?amount=${order.amountVnd}&addInfo=${encodeURIComponent(paymentDescription)}&accountName=ELEARNINGJP`

  return (
    <Modal
      open={isOpen}
      onCancel={onClose}
      footer={null}
      centered
      width={740}
      closeIcon={null}
      className="vietqr-modal-modern [&_.ant-modal-content]:!p-0 [&_.ant-modal-content]:!rounded-3xl [&_.ant-modal-content]:!overflow-hidden"
    >
      <div className="bg-surface text-sumi flex flex-col">
        {/* Header Banner - FinTech Modern */}
        <div className="relative bg-gradient-to-r from-[#141822] via-[#1c2333] to-[#141822] text-white px-6 py-5 border-b border-white/10">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30 text-xs font-bold tracking-wide uppercase">
                <Zap className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                <span>VietQR Napas 247</span>
              </span>
              <span className="hidden sm:inline-flex items-center gap-1 text-[11px] text-slate-300 font-medium">
                <Sparkles className="w-3 h-3 text-amber-400" />
                <span>Tự động kích hoạt sau 3 giây</span>
              </span>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-slate-300 hover:text-white transition flex items-center justify-center cursor-pointer"
              aria-label="Đóng"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="mt-3">
            <h3 className="text-xl sm:text-2xl font-black tracking-tight text-white flex items-center gap-2">
              <span>Kích hoạt ElearningJP PRO</span>
              <span className="text-amber-400 text-lg">👑</span>
            </h3>
            <p className="text-xs text-slate-300 mt-1">
              Quét mã QR bằng ứng dụng ngân hàng bất kỳ (Vietcombank, MB, Techcombank, Momo, v.v.)
            </p>
          </div>
        </div>

        {isPaid ? (
          /* Payment Success State */
          <div className="py-14 px-6 text-center space-y-4">
            <div className="relative inline-flex items-center justify-center w-24 h-24 rounded-full bg-emerald-500/15 text-emerald-500 shadow-xl border border-emerald-500/30 animate-bounce">
              <CheckCircle2 className="w-14 h-14" />
              <Sparkles className="w-6 h-6 absolute top-0 right-0 text-amber-400 animate-spin" />
            </div>
            <h3 className="text-2xl sm:text-3xl font-black text-sumi">
              Thanh toán thành công! 🎉
            </h3>
            <p className="text-sm text-nibi max-w-md mx-auto leading-relaxed">
              Hệ thống đã nhận được tiền và kích hoạt toàn bộ quyền lợi <strong className="text-amber-600 dark:text-amber-400">ElearningJP PRO</strong> cho tài khoản của bạn.
            </p>
            <div className="pt-2">
              <span className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-bold text-xs">
                <Check className="w-4 h-4" /> Đang chuyển hướng bạn đến kho học tập...
              </span>
            </div>
          </div>
        ) : (
          /* Main Payment Body */
          <div>
            <div className="p-6 sm:p-7 grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
              {/* Left Column: QR Code Display (6 Cols) */}
              <div className="md:col-span-6 flex flex-col items-center justify-center">
                <div className="w-full max-w-[310px] bg-white rounded-2xl p-4 border-2 border-suna shadow-md relative group">
                  {/* Subtle corner scanner guides */}
                  <div className="absolute top-2 left-2 w-4 h-4 border-t-2 border-l-2 border-amber-500 rounded-tl-sm pointer-events-none"></div>
                  <div className="absolute top-2 right-2 w-4 h-4 border-t-2 border-r-2 border-amber-500 rounded-tr-sm pointer-events-none"></div>
                  <div className="absolute bottom-2 left-2 w-4 h-4 border-b-2 border-l-2 border-amber-500 rounded-bl-sm pointer-events-none"></div>
                  <div className="absolute bottom-2 right-2 w-4 h-4 border-b-2 border-r-2 border-amber-500 rounded-br-sm pointer-events-none"></div>

                  <img
                    src={qrImageSrc}
                    alt="VietQR PayOS"
                    className="w-full aspect-square object-contain rounded-lg"
                  />
                </div>

                {/* Countdown Badge */}
                <div className="mt-3.5 inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-700 dark:text-amber-300 border border-amber-500/30">
                  <Clock className="w-3.5 h-3.5 animate-spin text-amber-600 dark:text-amber-400" />
                  <span>
                    Mã hết hạn sau: <strong className="font-mono text-sm">{formatTime(timeLeftSeconds)}</strong>
                  </span>
                </div>

                <p className="text-[11px] text-nibi mt-2 text-center">
                  * Chụp màn hình hoặc quét trực tiếp từ app ngân hàng
                </p>
              </div>

              {/* Right Column: Transaction Details (6 Cols) */}
              <div className="md:col-span-6 space-y-3.5 text-xs">
                {/* Số tiền cần thanh toán */}
                <div className="bg-kinari/70 border border-suna rounded-2xl p-3.5 sm:p-4">
                  <div className="flex items-center justify-between text-nibi font-semibold mb-1">
                    <span>SỐ TIỀN CẦN CHUYỂN</span>
                    <button
                      type="button"
                      onClick={() => copyToClipboard(order.amountVnd.toString(), 'Số tiền')}
                      className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400 hover:text-amber-700 font-bold transition cursor-pointer"
                    >
                      {copiedField === 'Số tiền' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{copiedField === 'Số tiền' ? 'Đã chép' : 'Sao chép'}</span>
                    </button>
                  </div>
                  <div className="text-2xl sm:text-3xl font-black text-amber-600 dark:text-amber-400 font-mono tracking-tight">
                    {order.amountVnd.toLocaleString('vi-VN')} <span className="text-lg">đ</span>
                  </div>
                </div>

                {/* Nội dung chuyển khoản (CRITICAL) */}
                <div className="bg-amber-500/10 border-2 border-amber-500/40 rounded-2xl p-3.5 sm:p-4 relative">
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="font-bold text-amber-800 dark:text-amber-300 uppercase tracking-wide flex items-center gap-1.5 text-[11px]">
                      <Zap className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                      <span>Nội dung chuyển khoản (Bắt buộc)</span>
                    </span>
                    <button
                      type="button"
                      onClick={() => copyToClipboard(paymentDescription, 'Nội dung chuyển khoản')}
                      className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-amber-500 hover:bg-amber-600 text-white font-bold transition shadow-xs cursor-pointer text-xs"
                    >
                      {copiedField === 'Nội dung chuyển khoản' ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{copiedField === 'Nội dung chuyển khoản' ? 'Đã chép ✓' : 'Sao chép'}</span>
                    </button>
                  </div>

                  <div className="font-mono text-base sm:text-lg font-black text-sumi bg-surface px-3 py-2 rounded-xl border border-amber-500/30 select-all tracking-wider text-center shadow-xs">
                    {paymentDescription}
                  </div>

                  <p className="text-[10px] text-amber-700 dark:text-amber-300 mt-1.5 font-medium leading-relaxed">
                    ⚠️ Vui lòng giữ nguyên nội dung chuyển khoản để hệ thống tự động ghi nhận và kích hoạt ngay sau 3 giây.
                  </p>
                </div>

                {/* Ngân hàng & STK */}
                <div className="bg-kinari/50 border border-suna rounded-2xl p-3.5 space-y-2.5 text-xs text-sumi">
                  <div className="flex justify-between items-center">
                    <span className="text-nibi flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-nibi" />
                      <span>Ngân hàng thụ hưởng:</span>
                    </span>
                    <span className="font-bold text-sumi">MB Bank (Quân Đội)</span>
                  </div>

                  <div className="flex justify-between items-center">
                    <span className="text-nibi flex items-center gap-1.5">
                      <CreditCard className="w-3.5 h-3.5 text-nibi" />
                      <span>Số tài khoản:</span>
                    </span>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-sumi text-sm tracking-wide">0348888888</span>
                      <button
                        type="button"
                        onClick={() => copyToClipboard('0348888888', 'Số tài khoản')}
                        className="text-nibi hover:text-sumi transition cursor-pointer"
                        title="Sao chép số tài khoản"
                      >
                        {copiedField === 'Số tài khoản' ? (
                          <Check className="w-3.5 h-3.5 text-emerald-500" />
                        ) : (
                          <Copy className="w-3.5 h-3.5" />
                        )}
                      </button>
                    </div>
                  </div>

                  <div className="flex justify-between items-center border-t border-suna/60 pt-2">
                    <span className="text-nibi">Chủ tài khoản:</span>
                    <span className="font-bold text-sumi tracking-wide">ELEARNINGJP</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Bottom Live Listening Footer */}
            <div className="px-6 py-3.5 bg-kinari border-t border-suna flex flex-wrap items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2.5">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500"></span>
                </span>
                <span className="font-medium text-sumi">
                  Hệ thống đang tự động lắng nghe giao dịch qua PayOS...
                </span>
              </div>

              <div className="flex items-center gap-1.5 text-nibi font-medium">
                <ShieldCheck className="w-4 h-4 text-emerald-500" />
                <span>Bảo mật 100% Napas 247</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}
