import React from 'react'
import { Link } from 'react-router-dom'
import {
  Crown,
  ShieldCheck,
  BookOpen,
  Award,
  Mic,
  Bookmark,
  Search,
  Sparkles,
} from 'lucide-react'
import { BrandLogo } from '@/components/common/BrandLogo'

export const Footer: React.FC = () => {
  return (
    <footer className="bg-surface border-t border-suna text-sumi transition-colors mt-auto shrink-0">
      <div className="max-w-[1650px] mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-8 lg:gap-12 items-start">
          {/* Brand & Slogan Column (5 Cols) */}
          <div className="lg:col-span-5 space-y-4">
            <BrandLogo size="md" />

            <p className="text-xs sm:text-sm text-nibi leading-relaxed max-w-sm">
              Nền tảng học tiếng Nhật trực tuyến thông minh. Tối ưu hóa lộ trình ôn thi JLPT N5–N1 chuẩn hóa, rèn luyện phản xạ giao tiếp với trợ lý AI và phương pháp lặp lại ngắt quãng SRS.
            </p>

            {/* Feature Pills */}
            <div className="flex flex-wrap gap-2 pt-1 text-[11px] font-semibold text-nibi">
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-kinari border border-suna/80">
                <Sparkles className="w-3 h-3 text-ai" />
                <span>AI Speaking &amp; OCR</span>
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-kinari border border-suna/80">
                <Award className="w-3 h-3 text-emerald-600" />
                <span>Đề thi thử JLPT N5–N1</span>
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-kinari border border-suna/80">
                <ShieldCheck className="w-3 h-3 text-amber-600" />
                <span>PayOS VietQR Napas</span>
              </span>
            </div>
          </div>

          {/* Links Column 1: Học tập & Luyện thi (3 Cols) */}
          <div className="lg:col-span-3 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-nibi">
              Học tập &amp; Luyện thi
            </h4>
            <ul className="space-y-2.5 text-xs sm:text-sm">
              <li>
                <Link
                  to="/courses"
                  className="text-nibi hover:text-ai transition flex items-center gap-1.5"
                >
                  <BookOpen className="w-3.5 h-3.5" />
                  <span>Khóa học chuyên sâu</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/exams"
                  className="text-nibi hover:text-ai transition flex items-center gap-1.5"
                >
                  <Award className="w-3.5 h-3.5" />
                  <span>Kho đề thi thử JLPT</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/speaking"
                  className="text-nibi hover:text-ai transition flex items-center gap-1.5"
                >
                  <Mic className="w-3.5 h-3.5" />
                  <span>Luyện nói AI Speaking</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/dictionary"
                  className="text-nibi hover:text-ai transition flex items-center gap-1.5"
                >
                  <Search className="w-3.5 h-3.5" />
                  <span>Tra cứu từ điển Mazii</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/notebooks"
                  className="text-nibi hover:text-ai transition flex items-center gap-1.5"
                >
                  <Bookmark className="w-3.5 h-3.5" />
                  <span>Sổ tay từ vựng SRS</span>
                </Link>
              </li>
            </ul>
          </div>

          {/* Links Column 2: Hội viên & Hỗ trợ (4 Cols) */}
          <div className="lg:col-span-4 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-nibi">
              Hội viên &amp; Hỗ trợ
            </h4>
            <ul className="space-y-2.5 text-xs sm:text-sm">
              <li>
                <Link
                  to="/pricing"
                  className="text-nibi hover:text-ai transition flex items-center gap-1.5 font-semibold"
                >
                  <Crown className="w-3.5 h-3.5 text-amber-500" />
                  <span className="text-sumi">Bảng giá gói cước PRO</span>
                </Link>
              </li>
              <li>
                <Link
                  to="/pricing/checkout"
                  className="text-nibi hover:text-ai transition"
                >
                  Đăng ký nâng cấp tài khoản
                </Link>
              </li>
              <li>
                <a
                  href="#faq"
                  onClick={() => {
                    if (window.location.pathname !== '/pricing') {
                      window.location.href = '/pricing#faq'
                    }
                  }}
                  className="text-nibi hover:text-ai transition"
                >
                  Câu hỏi thường gặp (FAQ)
                </a>
              </li>
              <li>
                <span className="text-nibi/80 text-xs">
                  Cổng thanh toán tự động VietQR Napas 247 qua PayOS
                </span>
              </li>
            </ul>

            {/* Support Hotline / Email */}
            <div className="pt-2 text-xs text-nibi space-y-1">
              <p>Email hỗ trợ: <a href="mailto:support@elearningjp.vn" className="text-ai hover:underline">support@elearningjp.vn</a></p>
              <p>Thời gian làm việc: 08:00 – 22:00 (Hàng ngày)</p>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-10 pt-6 border-t border-suna/70 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-nibi">
          <p>© 2026 ElearningJP. Bản quyền thuộc về nền tảng học tiếng Nhật ElearningJP.</p>

          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>Giao dịch bảo mật 100% Napas 247</span>
            </span>
            <span>•</span>
            <span className="text-emerald-600 dark:text-emerald-400 font-medium flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>Hệ thống hoạt động 24/7</span>
            </span>
          </div>
        </div>
      </div>
    </footer>
  )
}
