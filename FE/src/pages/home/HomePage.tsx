import React from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Sparkles, Trophy, Search, LogIn } from 'lucide-react'
import { Card } from 'antd'
import { Button as AntdButton } from 'antd'
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
      <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-indigo-50 text-indigo-700 text-sm font-semibold mb-4 border border-indigo-200/60 shadow-2xs">
        <Sparkles className="w-4 h-4 text-amber-500" />
        Nền tảng học tiếng Nhật thông minh với AI &amp; SRS
      </div>
      <h1 className="text-4xl md:text-5xl font-extrabold text-gray-900 tracking-tight max-w-2xl">
        Chinh phục JLPT từ N5 đến N1 dễ dàng hơn bao giờ hết
      </h1>
      <p className="mt-4 text-lg text-gray-600 max-w-xl">
        Hệ thống từ điển chuyên sâu kiểu Mazii, học từ vựng Spaced Repetition
        (SRS), luyện viết chữ Hán SVG, và thi thử JLPT chuẩn hóa.
      </p>

      <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
        <AntdButton
          type="primary"
          size="large"
          icon={<Search className="w-5 h-5" />}
          className="!h-12 !px-7 !rounded-md !font-bold"
          onClick={() => (window.location.href = '/dictionary')}
        >
          Tra từ điển &amp; Chữ cái
        </AntdButton>
        <Link
          to="/login"
          className="px-6 py-3.5 rounded-md bg-white text-gray-800 font-bold border border-gray-200 hover:bg-gray-50 active:scale-98 transition shadow-2xs"
        >
          Bắt đầu học ngay
        </Link>
      </div>

      <div className="mt-16 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 w-full text-left">
        <Link to="/dictionary" className="block group">
          <Card className="!h-full hover:!border-indigo-400 hover:!shadow-md transition group-hover:!border-indigo-400">
            <div className="w-12 h-12 rounded-md bg-indigo-50 text-indigo-600 flex items-center justify-center mb-4 group-hover:scale-110 transition">
              <Search className="w-6 h-6" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 group-hover:text-indigo-600 transition">
              Từ điển chuyên sâu
            </h3>
            <p className="text-gray-600 text-sm mt-1.5">
              Tra cứu Chữ Hán, Từ vựng, Ngữ pháp, kèm nét vẽ SVG hoạt ảnh và
              phát âm chuẩn.
            </p>
          </Card>
        </Link>

        <Link to="/courses" className="block group">
          <Card className="!h-full hover:!border-purple-400 hover:!shadow-md transition group-hover:!border-purple-400">
            <div className="w-12 h-12 rounded-md bg-purple-50 text-purple-600 flex items-center justify-center mb-4 group-hover:scale-110 transition">
              <BookOpen className="w-6 h-6" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 group-hover:text-purple-600 transition">
              Giáo trình bài bản
            </h3>
            <p className="text-gray-600 text-sm mt-1.5">
              Từ vựng, ngữ pháp, chữ Hán chia theo lộ trình từng cấp độ JLPT từ N5
              đến N1.
            </p>
          </Card>
        </Link>

        {/* /srs cần đăng nhập: khách bấm → mở modal đăng nhập (xong quay lại /srs) thay vì bị đá sang /login. */}
        <Link to={SRS_PATH} onClick={handleSrsClick} className="block group">
          <Card className="!h-full hover:!border-amber-400 hover:!shadow-md transition group-hover:!border-amber-400">
            <div className="w-12 h-12 rounded-md bg-amber-50 text-amber-600 flex items-center justify-center mb-4 group-hover:scale-110 transition">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 group-hover:text-amber-600 transition">
              Flashcard SRS
            </h3>
            <p className="text-gray-600 text-sm mt-1.5">
              Thuật toán ghi nhớ ngắt quãng SM-2 giúp học sâu nhớ lâu, tự động đồng
              bộ từ Quiz.
            </p>
            {!isAuthenticated && (
              <p className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-amber-700">
                <LogIn className="w-3.5 h-3.5" />
                Đăng nhập để ôn tập
              </p>
            )}
          </Card>
        </Link>

        <Link to="/exams" className="block group">
          <Card className="!h-full hover:!border-emerald-400 hover:!shadow-md transition group-hover:!border-emerald-400">
            <div className="w-12 h-12 rounded-md bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4 group-hover:scale-110 transition">
              <Trophy className="w-6 h-6" />
            </div>
            <h3 className="font-bold text-lg text-gray-900 group-hover:text-emerald-600 transition">
              Thi thử JLPT chuẩn
            </h3>
            <p className="text-gray-600 text-sm mt-1.5">
              Đề thi mô phỏng thực tế với bảng điểm quy đổi Scaled Score chuẩn xác.
            </p>
          </Card>
        </Link>
      </div>
    </div>
  )
}
