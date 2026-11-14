import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Crown,
  Check,
  ArrowRight,
  ShieldCheck,
  Infinity as InfinityIcon,
  Sparkles,
} from 'lucide-react'
import { useAuthStore } from '@/stores/useAuthStore'
import { useAuthModalStore } from '@/stores/useAuthModalStore'
import { useSubscriptionStore } from '@/stores/useSubscriptionStore'
import { subscriptionService } from '@/services/subscription.service'
import type { SubscriptionPlan } from '@/types'
import { cn } from '@/utils/cn'

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

const FAQS = [
  {
    question: 'Hệ thống có tự động gia hạn và trừ tiền thẻ hàng tháng không?',
    answer:
      'Hoàn toàn không. ElearningJP sử dụng phương thức chuyển khoản ngân hàng qua mã VietQR Napas 247 cho từng kỳ thanh toán. Chúng tôi không lưu thông tin thẻ tín dụng và không bao giờ tự động trừ tiền của bạn.',
  },
  {
    question: 'Quyền lợi giữa các gói 1 tháng, 1 năm và trọn đời có gì khác nhau?',
    answer:
      'Quyền lợi học tập của cả 3 gói PRO là hoàn toàn giống nhau 100%: đều mở khóa toàn bộ đề thi JLPT N5–N1, không giới hạn bài giảng giảng viên, không giới hạn token AI Speaking và xuất file PDF Kanji. Sự khác biệt duy nhất là thời hạn sử dụng gói.',
  },
  {
    question: 'Sau khi quét mã chuyển khoản bao lâu thì tài khoản được kích hoạt?',
    answer:
      'Hệ thống kết nối trực tiếp cổng thanh toán PayOS để lắng nghe giao dịch 24/7. Ngay khi giao dịch ngân hàng thành công, tài khoản của bạn sẽ được kích hoạt tự động sau 3–5 giây mà không cần chờ duyệt thủ công.',
  },
  {
    question: 'Tài khoản miễn phí (Free Tier) có những giới hạn nào?',
    answer:
      'Tài khoản Free được tặng 3.000 token AI Speaking khởi tạo và 500 token mỗi ngày, xuất tối đa 10 chữ Kanji/phiếu luyện viết, và học thử các bài học/đề thi mẫu. Nâng cấp PRO giúp bạn gỡ bỏ toàn bộ các giới hạn này.',
  },
]

export const PricingPage: React.FC = () => {
  const navigate = useNavigate()
  const [plans, setPlans] = useState<SubscriptionPlan[]>(FALLBACK_PLANS)
  const [openFaqIndex, setOpenFaqIndex] = useState<number | null>(null)

  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)
  const { isPro } = useSubscriptionStore()

  useEffect(() => {
    const fetchPlans = async () => {
      try {
        const res = await subscriptionService.getPublicPlans()
        if (res.success && res.data) {
          const proPlans = res.data.filter((p) => p.id !== 'FREE')
          if (proPlans.length > 0) setPlans(proPlans)
        }
      } catch {
        // Fallback initialized
      }
    }
    void fetchPlans()
  }, [])

  const handleSelectPlan = (planId: string) => {
    if (!isAuthenticated) {
      openLogin(`/pricing/checkout?plan=${planId}`)
      return
    }
    navigate(`/pricing/checkout?plan=${planId}`)
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-10 sm:py-14 space-y-16">
      {/* =================================================================== */}
      {/* 1. HERO HEADER — Thiết kế trang nhã phong cách Nhật Bản */}
      {/* =================================================================== */}
      <div className="text-center max-w-3xl mx-auto space-y-4">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-ai/10 text-ai dark:bg-ai/20 dark:text-indigo-300 text-xs font-semibold">
          <Crown className="w-3.5 h-3.5" />
          <span>Hội viên ElearningJP PRO</span>
        </div>

        <h1 className="text-3xl sm:text-4xl md:text-5xl font-black text-sumi tracking-tight leading-tight">
          Đầu tư cho hành trình chinh phục tiếng Nhật
        </h1>

        <p className="text-sm sm:text-base text-nibi max-w-2xl mx-auto leading-relaxed">
          Mở khóa toàn diện kho đề thi thử chuẩn hóa JLPT, bài giảng chuyên sâu do giảng viên phát hành và luyện nói phản xạ không giới hạn cùng trợ lý AI.
        </p>

        {isPro && (
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border border-emerald-500/20 text-xs font-semibold mt-2">
            <Sparkles className="w-4 h-4 text-emerald-600" />
            <span>Tài khoản của bạn hiện đang là Hội viên PRO cao cấp</span>
          </div>
        )}
      </div>

      {/* =================================================================== */}
      {/* 2. THREE PRO PLAN CARDS (Dữ liệu động 100% từ Database) */}
      {/* =================================================================== */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-8 items-stretch max-w-5xl mx-auto">
        {plans.map((plan) => {
          const isYearly = plan.id === 'PRO_1Y'
          const isLifetime = plan.id === 'PRO_LIFETIME'
          const hasDiscount = plan.originalPriceVnd && plan.originalPriceVnd > plan.priceVnd
          const discountPercent = hasDiscount
            ? Math.round(((plan.originalPriceVnd! - plan.priceVnd) / plan.originalPriceVnd!) * 100)
            : 0

          // Calculate sub-rate
          const subRateText = isYearly
            ? `~${Math.round(plan.priceVnd / 12).toLocaleString('vi-VN')} đ / tháng`
            : isLifetime
            ? 'Đóng 1 lần • Sở hữu vĩnh viễn'
            : 'Gia hạn linh hoạt từng tháng'

          return (
            <div
              key={plan.id}
              className={cn(
                'relative flex flex-col justify-between rounded-2xl p-6 sm:p-8 transition-all bg-surface',
                'border',
                isYearly
                  ? 'border-ai shadow-lg ring-1 ring-ai/30 dark:ring-ai/50 md:-translate-y-2'
                  : 'border-suna shadow-xs hover:border-ai/40 hover:shadow-md',
              )}
            >
              {/* Highlight badge for popular plan */}
              {isYearly && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full bg-ai text-white text-[11px] font-bold tracking-wide shadow-xs uppercase">
                  Được chọn nhiều nhất
                </div>
              )}
              {isLifetime && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full bg-amber-500 text-slate-950 text-[11px] font-black tracking-wide shadow-xs uppercase">
                  Tiết kiệm trọn đời
                </div>
              )}

              <div>
                {/* Plan Header */}
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-xl font-bold text-sumi">{plan.name}</h3>
                  {hasDiscount && (
                    <span className="px-2 py-0.5 rounded-md text-[11px] font-bold bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20">
                      Tiết kiệm {discountPercent}%
                    </span>
                  )}
                </div>

                <p className="text-xs text-nibi min-h-[36px] leading-relaxed mb-6">
                  {isYearly
                    ? 'Lộ trình tối ưu nhất để bứt phá từ N5 lên N2 trong 1 năm học.'
                    : isLifetime
                    ? 'Đóng học phí một lần duy nhất, đồng hành vĩnh viễn không lo gia hạn.'
                    : 'Trải nghiệm toàn bộ tính năng PRO hoặc ôn thi cấp tốc nước rút.'}
                </p>

                {/* Price Presentation */}
                <div className="pb-6 mb-6 border-b border-suna/70">
                  <div className="flex items-baseline gap-1.5">
                    <span className="text-3xl sm:text-4xl font-extrabold text-sumi font-mono tracking-tight">
                      {plan.priceVnd.toLocaleString('vi-VN')}
                    </span>
                    <span className="text-xs font-bold text-nibi">đ</span>
                    <span className="text-xs text-nibi ml-1">
                      {isLifetime ? '/ trọn đời' : isYearly ? '/ năm' : '/ tháng'}
                    </span>
                  </div>

                  {hasDiscount && (
                    <div className="text-xs text-nibi/80 line-through mt-1 font-mono">
                      {plan.originalPriceVnd!.toLocaleString('vi-VN')} đ
                    </div>
                  )}

                  <p className="text-xs text-ai dark:text-indigo-300 font-medium mt-2">
                    {subRateText}
                  </p>
                </div>

                {/* Core Perks List */}
                <ul className="space-y-3 text-xs text-sumi">
                  <li className="flex items-start gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                    <span>Mở khóa 100% kho đề thi thử JLPT N5–N1</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                    <span>Mở khóa toàn bộ khóa học chuyên sâu</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                    <span>Luyện nói AI Speaking không giới hạn token</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                    <span>Xuất file PDF Kanji luyện viết không giới hạn</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                    <span>AI Canvas OCR nhận diện chữ viết tay miễn phí</span>
                  </li>
                  <li className="flex items-start gap-2.5">
                    <Check className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                    <span>Huy hiệu PRO vương miện trên hồ sơ cá nhân</span>
                  </li>
                </ul>
              </div>

              {/* Action Button */}
              <div className="pt-8 mt-6">
                <button
                  type="button"
                  onClick={() => handleSelectPlan(plan.id)}
                  className={cn(
                    'w-full py-3 px-4 rounded-xl font-bold text-xs sm:text-sm transition flex items-center justify-center gap-2 cursor-pointer shadow-xs',
                    isYearly
                      ? 'bg-ai hover:bg-ai/90 text-white shadow-md'
                      : isLifetime
                      ? 'bg-amber-500 hover:bg-amber-600 text-slate-950 font-black'
                      : 'bg-kinari hover:bg-surface border border-suna text-sumi',
                  )}
                >
                  <span>Nâng cấp ngay</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )
        })}
      </div>

      {/* =================================================================== */}
      {/* 3. QUY TRÌNH THANH TOÁN 3 BƯỚC MINH BẠCH (Gọn gàng, tinh tế) */}
      {/* =================================================================== */}
      <div className="bg-surface rounded-2xl border border-suna p-6 sm:p-8 max-w-4xl mx-auto shadow-2xs">
        <h3 className="text-center text-sm font-bold uppercase tracking-wider text-nibi mb-6">
          Quy trình thanh toán PayOS VietQR tự động
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 text-center">
          <div className="space-y-2">
            <div className="w-10 h-10 rounded-xl bg-ai/10 text-ai dark:bg-ai/20 dark:text-indigo-300 font-bold text-sm flex items-center justify-center mx-auto">
              1
            </div>
            <h4 className="text-xs sm:text-sm font-bold text-sumi">Chọn gói &amp; Thông tin</h4>
            <p className="text-[11px] text-nibi leading-relaxed">
              Chọn thời hạn phù hợp, nhập mã voucher giảm giá (nếu có) và xác nhận tài khoản.
            </p>
          </div>

          <div className="space-y-2">
            <div className="w-10 h-10 rounded-xl bg-ai/10 text-ai dark:bg-ai/20 dark:text-indigo-300 font-bold text-sm flex items-center justify-center mx-auto">
              2
            </div>
            <h4 className="text-xs sm:text-sm font-bold text-sumi">Quét mã VietQR</h4>
            <p className="text-[11px] text-nibi leading-relaxed">
              Mở ứng dụng ngân hàng bất kỳ (VCB, MB, Techcombank, v.v.) và quét mã QR Napas 247.
            </p>
          </div>

          <div className="space-y-2">
            <div className="w-10 h-10 rounded-xl bg-ai/10 text-ai dark:bg-ai/20 dark:text-indigo-300 font-bold text-sm flex items-center justify-center mx-auto">
              3
            </div>
            <h4 className="text-xs sm:text-sm font-bold text-sumi">Kích hoạt sau 3 giây</h4>
            <p className="text-[11px] text-nibi leading-relaxed">
              Hệ thống tự động kích hoạt tài khoản PRO ngay tức thì mà không cần chờ duyệt thủ công.
            </p>
          </div>
        </div>
      </div>

      {/* =================================================================== */}
      {/* 4. BẢNG SO SÁNH QUYỀN LỢI CHI TIẾT */}
      {/* =================================================================== */}
      <div className="bg-surface rounded-2xl border border-suna shadow-2xs p-6 sm:p-8 max-w-4xl mx-auto space-y-6">
        <div className="text-center max-w-md mx-auto space-y-1">
          <h2 className="text-xl sm:text-2xl font-bold text-sumi">
            So sánh quyền lợi chi tiết
          </h2>
          <p className="text-xs text-nibi">
            Xem sự khác biệt giữa tài khoản Miễn phí và Hội viên PRO
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-suna text-xs font-bold uppercase tracking-wider text-nibi">
                <th className="py-3 px-4">Tính năng &amp; Tài nguyên</th>
                <th className="py-3 px-4 text-center w-36 text-nibi">Miễn phí</th>
                <th className="py-3 px-4 text-center w-44 text-ai dark:text-indigo-300 bg-ai/5 rounded-t-xl">
                  Hội viên PRO 👑
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-suna/70 text-xs sm:text-sm">
              <tr>
                <td className="py-3.5 px-4 font-medium text-sumi">
                  <div className="font-semibold">Kho đề thi thử chuẩn hóa JLPT (N5 – N1)</div>
                  <div className="text-[11px] text-nibi">Bấm giờ chuẩn thi thật, chấm điểm đậu/trượt tức thì</div>
                </td>
                <td className="py-3.5 px-4 text-center text-nibi">Chỉ làm đề mẫu</td>
                <td className="py-3.5 px-4 text-center font-bold text-emerald-600 dark:text-emerald-400 bg-ai/5">
                  Mở khóa 100% toàn bộ
                </td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 font-medium text-sumi">
                  <div className="font-semibold">Khóa học chuyên sâu do Giảng viên phát hành</div>
                  <div className="text-[11px] text-nibi">Lộ trình bài giảng video, từ vựng, ngữ pháp, kanji</div>
                </td>
                <td className="py-3.5 px-4 text-center text-nibi">Chỉ học bài mở đầu</td>
                <td className="py-3.5 px-4 text-center font-bold text-emerald-600 dark:text-emerald-400 bg-ai/5">
                  Mở khóa toàn bộ bài học
                </td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 font-medium text-sumi">
                  <div className="font-semibold">Trợ lý AI Luyện nói Speaking &amp; Giao tiếp</div>
                  <div className="text-[11px] text-nibi">Luyện nói phản xạ tình huống thực tế, sửa lỗi phát âm</div>
                </td>
                <td className="py-3.5 px-4 text-center text-nibi">
                  3.000 token dùng thử
                  <br />
                  <span className="text-[10px] text-nibi/80">+500 token/ngày</span>
                </td>
                <td className="py-3.5 px-4 text-center font-bold text-emerald-600 dark:text-emerald-400 bg-ai/5">
                  <div className="flex items-center justify-center gap-1">
                    <InfinityIcon className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                    <span>Không giới hạn</span>
                  </div>
                </td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 font-medium text-sumi">
                  <div className="font-semibold">Xuất phiếu luyện viết chữ Hán (Practice Sheet)</div>
                  <div className="text-[11px] text-nibi">Tải file PDF ô ly chữ điền chuẩn nét bút để in ra tập viết</div>
                </td>
                <td className="py-3.5 px-4 text-center text-nibi">Tối đa 10 chữ / phiếu</td>
                <td className="py-3.5 px-4 text-center font-bold text-emerald-600 dark:text-emerald-400 bg-ai/5">
                  Không giới hạn số chữ
                </td>
              </tr>
              <tr>
                <td className="py-3.5 px-4 font-medium text-sumi">
                  <div className="font-semibold">Nhận diện chữ viết tay AI (Canvas OCR)</div>
                  <div className="text-[11px] text-nibi">Vẽ chữ trực tiếp trên màn hình để tra cứu nghĩa và bộ thủ</div>
                </td>
                <td className="py-3.5 px-4 text-center font-bold text-emerald-600 dark:text-emerald-400">100% Miễn phí</td>
                <td className="py-3.5 px-4 text-center font-bold text-emerald-600 dark:text-emerald-400 bg-ai/5">
                  100% Miễn phí
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* =================================================================== */}
      {/* 5. CÂU HỎI THƯỜNG GẶP (FAQ ACCORDION) */}
      {/* =================================================================== */}
      <div className="max-w-3xl mx-auto space-y-6">
        <div className="text-center space-y-1">
          <h2 className="text-xl sm:text-2xl font-bold text-sumi">
            Câu hỏi thường gặp
          </h2>
          <p className="text-xs text-nibi">
            Giải đáp những thắc mắc phổ biến nhất của học viên về tài khoản PRO
          </p>
        </div>

        <div className="space-y-3">
          {FAQS.map((faq, index) => {
            const isOpen = openFaqIndex === index
            return (
              <div
                key={index}
                className="bg-surface rounded-xl border border-suna overflow-hidden transition"
              >
                <button
                  type="button"
                  onClick={() => setOpenFaqIndex(isOpen ? null : index)}
                  className="w-full py-4 px-5 text-left text-xs sm:text-sm font-bold text-sumi flex items-center justify-between gap-3 hover:text-ai transition cursor-pointer"
                >
                  <span>{faq.question}</span>
                  <span className={cn('text-nibi transition-transform', isOpen && 'rotate-180')}>
                    ▼
                  </span>
                </button>
                {isOpen && (
                  <div className="px-5 pb-4 pt-1 text-xs text-nibi leading-relaxed border-t border-suna/60">
                    {faq.answer}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Bottom Reassurance */}
      <div className="flex items-center justify-center gap-2 text-xs text-nibi pt-4">
        <ShieldCheck className="w-4 h-4 text-emerald-600" />
        <span>Thanh toán an toàn qua cổng PayOS VietQR • Cam kết không phát sinh phụ phí</span>
      </div>
    </div>
  )
}
