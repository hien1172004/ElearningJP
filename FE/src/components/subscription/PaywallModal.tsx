import React, { useEffect } from 'react'
import { Modal } from 'antd'
import { useNavigate } from 'react-router-dom'
import { Crown, Sparkles, CheckCircle2, Zap, ArrowRight, ShieldCheck, X } from 'lucide-react'
import { useSubscriptionStore } from '@/stores/useSubscriptionStore'
import { PAYWALL_TRIGGER_EVENT, type PaywallTriggerDetail } from '@/utils/paywallEvents'

export const PaywallModal: React.FC = () => {
  const navigate = useNavigate()
  const { isPaywallOpen, paywallReason, openPaywall, closePaywall } = useSubscriptionStore()

  useEffect(() => {
    const handleTrigger = (e: Event) => {
      const customEvent = e as CustomEvent<PaywallTriggerDetail>
      openPaywall(customEvent.detail?.reason)
    }

    window.addEventListener(PAYWALL_TRIGGER_EVENT, handleTrigger)
    return () => window.removeEventListener(PAYWALL_TRIGGER_EVENT, handleTrigger)
  }, [openPaywall])

  const handleGoToPricing = () => {
    closePaywall()
    navigate('/pricing')
  }

  return (
    <Modal
      open={isPaywallOpen}
      onCancel={closePaywall}
      footer={null}
      centered
      width={520}
      closeIcon={null}
      className="paywall-modal-modern [&_.ant-modal-content]:!p-0 [&_.ant-modal-content]:!rounded-3xl [&_.ant-modal-content]:!overflow-hidden"
    >
      <div className="bg-surface text-sumi flex flex-col">
        {/* Header Hero Banner */}
        <div className="relative bg-gradient-to-r from-[#161a23] via-[#1f2637] to-[#161a23] text-white px-6 py-6 text-center border-b border-white/10">
          <button
            type="button"
            onClick={closePaywall}
            className="absolute top-4 right-4 w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-slate-300 hover:text-white transition flex items-center justify-center cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-4 h-4" />
          </button>

          <div className="relative inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-tr from-amber-400 via-yellow-400 to-amber-500 text-slate-950 shadow-lg shadow-amber-500/30 mb-3 animate-bounce">
            <Crown className="w-9 h-9 fill-slate-950" />
            <Sparkles className="w-5 h-5 absolute -top-1.5 -right-1.5 text-white animate-spin" />
          </div>

          <h3 className="text-2xl font-black tracking-tight text-white">
            Nâng cấp ElearningJP PRO 👑
          </h3>

          <p className="text-xs text-slate-300 max-w-sm mx-auto mt-1.5 leading-relaxed">
            {paywallReason || 'Tính năng này yêu cầu hội viên PRO hoặc bạn đã sử dụng hết lượt miễn phí trong ngày.'}
          </p>
        </div>

        {/* Feature List */}
        <div className="p-6 sm:p-7 space-y-4">
          <div className="bg-kinari/70 dark:bg-slate-900/60 border border-suna rounded-2xl p-4 space-y-3">
            <div className="flex items-start gap-3">
              <div className="w-7 h-7 rounded-lg bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0 mt-0.5">
                <CheckCircle2 className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs sm:text-sm font-bold text-sumi">
                  Toàn quyền mở khóa khóa học &amp; đề thi JLPT
                </p>
                <p className="text-[11px] text-nibi leading-relaxed">
                  Truy cập toàn bộ đề thi thử độc quyền và bài giảng chuyên sâu N5 – N1.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="w-7 h-7 rounded-lg bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0 mt-0.5">
                <CheckCircle2 className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs sm:text-sm font-bold text-sumi">
                  Luyện nói AI Speaking không giới hạn
                </p>
                <p className="text-[11px] text-nibi leading-relaxed">
                  Không lo hết token, trò chuyện phản xạ tiếng Nhật tự nhiên 24/7.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="w-7 h-7 rounded-lg bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0 mt-0.5">
                <CheckCircle2 className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs sm:text-sm font-bold text-sumi">
                  Xuất phiếu luyện viết PDF Kanji không giới hạn
                </p>
                <p className="text-[11px] text-nibi leading-relaxed">
                  In trọn bộ danh sách chữ Hán tuỳ thích chuẩn nét bút và ô ly chữ điền.
                </p>
              </div>
            </div>
          </div>

          {/* Action CTAs */}
          <div className="space-y-2 pt-2">
            <button
              type="button"
              onClick={handleGoToPricing}
              className="w-full inline-flex items-center justify-center gap-2 py-3.5 px-4 rounded-xl bg-gradient-to-r from-amber-500 via-amber-400 to-yellow-500 hover:from-amber-600 hover:to-yellow-600 text-slate-950 font-black text-sm tracking-wide shadow-lg shadow-amber-500/30 active:scale-[0.99] transition cursor-pointer"
            >
              <Zap className="w-4 h-4 fill-slate-950" />
              <span>Xem bảng giá &amp; Nâng cấp PRO ngay</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={closePaywall}
              className="w-full text-xs text-nibi hover:text-sumi py-2 transition cursor-pointer text-center font-medium"
            >
              Để sau, tôi sẽ nâng cấp sau
            </button>
          </div>

          <div className="flex items-center justify-center gap-1.5 text-[11px] text-nibi pt-1">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
            <span>Thanh toán VietQR PayOS • Kích hoạt tự động sau 3 giây</span>
          </div>
        </div>
      </div>
    </Modal>
  )
}
