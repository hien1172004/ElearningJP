import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BookOpen, Sparkles, Trophy, Search } from 'lucide-react'
import { Card } from 'antd'
import { Button as AntdButton } from 'antd'

export const HomePage: React.FC = () => {
  const navigate = useNavigate();

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
          onClick={() => navigate('/dictionary')}
        >
          Tra từ điển &amp; Chữ cái
        </AntdButton>
        <button
          type="button"
          onClick={() => navigate('/courses')}
          className="px-6 py-3.5 rounded-md bg-white text-gray-800 font-bold border border-gray-200 hover:bg-gray-50 active:scale-98 transition shadow-2xs"
        >
          Bắt đầu học ngay
        </button>
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

        <Card className="!h-full">
          <div className="w-12 h-12 rounded-md bg-purple-50 text-purple-600 flex items-center justify-center mb-4">
            <BookOpen className="w-6 h-6" />
          </div>
          <h3 className="font-bold text-lg text-gray-900">Giáo trình bài bản</h3>
          <p className="text-gray-600 text-sm mt-1.5">
            Từ vựng, ngữ pháp, chữ Hán chia theo lộ trình từng cấp độ JLPT từ N5
            đến N1.
          </p>
        </Card>

        <Card className="!h-full">
          <div className="w-12 h-12 rounded-md bg-amber-50 text-amber-600 flex items-center justify-center mb-4">
            <Sparkles className="w-6 h-6" />
          </div>
          <h3 className="font-bold text-lg text-gray-900">Flashcard SRS</h3>
          <p className="text-gray-600 text-sm mt-1.5">
            Thuật toán ghi nhớ ngắt quãng SM-2 giúp học sâu nhớ lâu, tự động đồng
            bộ từ Quiz.
          </p>
        </Card>

        <Card className="!h-full">
          <div className="w-12 h-12 rounded-md bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4">
            <Trophy className="w-6 h-6" />
          </div>
          <h3 className="font-bold text-lg text-gray-900">Thi thử JLPT chuẩn</h3>
          <p className="text-gray-600 text-sm mt-1.5">
            Đề thi mô phỏng thực tế với bảng điểm quy đổi Scaled Score chuẩn xác.
          </p>
        </Card>
      </div>
    </div>
  )
}
