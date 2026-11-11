import React from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Compass, Layers, Trophy, Search, LogIn } from 'lucide-react'
import { useAuthStore } from '@/stores/useAuthStore'
import { useAuthModalStore } from '@/stores/useAuthModalStore'

/** Trang ôn tập SRS — nằm trong ProtectedRoute. */
const SRS_PATH = '/srs'

export const HomePage: React.FC = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)

  const handleSrsClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (isAuthenticated) return
    e.preventDefault()
    openLogin(SRS_PATH)
  }

  return (
    <div className="py-12 flex flex-col items-center text-center">
      {/* Pill Badge */}
      <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-kamenozoki text-ai text-xs font-semibold mb-5 border border-ai/20 shadow-2xs">
        <Compass className="w-3.5 h-3.5 text-ai" />
        Nền tảng học tiếng Nhật thông minh với AI &amp; SRS
      </div>

      {/* Hero Title */}
      <h1 className="text-4xl md:text-5xl font-extrabold text-sumi tracking-tight max-w-2xl leading-tight">
        Chinh phục JLPT từ N5 đến N1 dễ dàng hơn bao giờ hết
      </h1>
      <p className="mt-4 text-base sm:text-lg text-nibi max-w-xl leading-relaxed">
        Hệ thống từ điển chuyên sâu kiểu Mazii, học từ vựng Spaced Repetition
        (SRS), luyện viết chữ Hán SVG, và thi thử JLPT chuẩn hóa.
      </p>

      {/* Action Buttons */}
      <div className="mt-8 flex flex-wrap items-center justify-center gap-3.5">
        <Link
          to="/dictionary"
          className="h-11 px-6 rounded-lg !bg-[#2e5c8a] hover:!bg-[#254b70] !text-white font-medium text-sm inline-flex items-center gap-2 shadow-sm transition-all cursor-pointer select-none border border-[#2e5c8a]"
        >
          <Search className="w-4 h-4 !text-white" />
          <span className="!text-white font-semibold">Tra từ điển &amp; Chữ cái</span>
        </Link>
        <Link
          to={isAuthenticated ? '/courses' : '/login'}
          className="h-11 px-6 rounded-lg bg-surface hover:bg-slate-50 text-sumi font-medium text-sm border border-suna hover:border-ai/50 inline-flex items-center gap-2 transition-all shadow-2xs cursor-pointer select-none"
        >
          <BookOpen className="w-4 h-4 text-ai" />
          <span className="text-sumi">{isAuthenticated ? 'Vào bài học ngay' : 'Bắt đầu học ngay'}</span>
        </Link>
      </div>

      {/* 4 Feature Cards */}
      <div className="mt-16 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 w-full text-left">
        {/* Card 1: Từ điển */}
        <Link to="/dictionary" className="block group">
          <div className="h-full bg-surface rounded-2xl p-6 border border-suna/90 hover:border-ai/40 shadow-xs hover:shadow-sm transition-all duration-200 flex flex-col justify-between">
            <div>
              <div className="w-12 h-12 rounded-xl bg-kamenozoki text-ai flex items-center justify-center mb-4 group-hover:scale-105 transition-transform border border-ai/15 shadow-2xs">
                <Search className="w-6 h-6" />
              </div>
              <h3 className="font-bold text-base text-sumi group-hover:text-ai transition-colors">
                Từ điển chuyên sâu
              </h3>
              <p className="text-nibi text-xs mt-2 leading-relaxed">
                Tra cứu Chữ Hán, Từ vựng, Ngữ pháp, kèm nét vẽ SVG hoạt ảnh và phát âm chuẩn.
              </p>
            </div>
            <span className="mt-5 text-xs font-semibold text-ai inline-flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Khám phá ngay &rarr;
            </span>
          </div>
        </Link>

        {/* Card 2: Giáo trình */}
        <Link to="/courses" className="block group">
          <div className="h-full bg-surface rounded-2xl p-6 border border-suna/90 hover:border-ai/40 shadow-xs hover:shadow-sm transition-all duration-200 flex flex-col justify-between">
            <div>
              <div className="w-12 h-12 rounded-xl bg-kamenozoki text-ai flex items-center justify-center mb-4 group-hover:scale-105 transition-transform border border-ai/15 shadow-2xs">
                <BookOpen className="w-6 h-6" />
              </div>
              <h3 className="font-bold text-base text-sumi group-hover:text-ai transition-colors">
                Giáo trình bài bản
              </h3>
              <p className="text-nibi text-xs mt-2 leading-relaxed">
                Từ vựng, ngữ pháp, chữ Hán chia theo lộ trình từng cấp độ JLPT từ N5 đến N1.
              </p>
            </div>
            <span className="mt-5 text-xs font-semibold text-ai inline-flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Xem khóa học &rarr;
            </span>
          </div>
        </Link>

        {/* Card 3: Flashcard SRS */}
        <Link to={SRS_PATH} onClick={handleSrsClick} className="block group">
          <div className="h-full bg-surface rounded-2xl p-6 border border-suna/90 hover:border-ai/40 shadow-xs hover:shadow-sm transition-all duration-200 flex flex-col justify-between">
            <div>
              <div className="w-12 h-12 rounded-xl bg-kamenozoki text-ai flex items-center justify-center mb-4 group-hover:scale-105 transition-transform border border-ai/15 shadow-2xs">
                <Layers className="w-6 h-6" />
              </div>
              <h3 className="font-bold text-base text-sumi group-hover:text-ai transition-colors">
                Flashcard SRS
              </h3>
              <p className="text-nibi text-xs mt-2 leading-relaxed">
                Thuật toán ghi nhớ ngắt quãng SM-2 giúp học sâu nhớ lâu, tự động đồng bộ từ Quiz.
              </p>
              {!isAuthenticated && (
                <p className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                  <LogIn className="w-3.5 h-3.5" />
                  Đăng nhập để ôn tập
                </p>
              )}
            </div>
            <span className="mt-5 text-xs font-semibold text-ai inline-flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Bắt đầu ôn tập &rarr;
            </span>
          </div>
        </Link>

        {/* Card 4: Thi thử JLPT */}
        <Link to="/exams" className="block group">
          <div className="h-full bg-surface rounded-2xl p-6 border border-suna/90 hover:border-ai/40 shadow-xs hover:shadow-sm transition-all duration-200 flex flex-col justify-between">
            <div>
              <div className="w-12 h-12 rounded-xl bg-kamenozoki text-ai flex items-center justify-center mb-4 group-hover:scale-105 transition-transform border border-ai/15 shadow-2xs">
                <Trophy className="w-6 h-6" />
              </div>
              <h3 className="font-bold text-base text-sumi group-hover:text-ai transition-colors">
                Thi thử JLPT chuẩn
              </h3>
              <p className="text-nibi text-xs mt-2 leading-relaxed">
                Đề thi mô phỏng thực tế với bảng điểm quy đổi Scaled Score chuẩn xác từ N5 đến N1.
              </p>
            </div>
            <span className="mt-5 text-xs font-semibold text-ai inline-flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Vào phòng thi &rarr;
            </span>
          </div>
        </Link>
      </div>
    </div>
  )
}
