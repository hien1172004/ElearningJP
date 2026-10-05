import React from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Sparkles, Trophy } from 'lucide-react'

export const HomePage: React.FC = () => {
  return (
    <div className="py-12 flex flex-col items-center text-center">
      <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-600 text-sm font-medium mb-4">
        <Sparkles className="w-4 h-4" />
        Nền tảng học tiếng Nhật thông minh với AI & SRS
      </div>
      <h1 className="text-4xl md:text-5xl font-extrabold text-gray-900 tracking-tight max-w-2xl">
        Chinh phục JLPT từ N5 đến N1 dễ dàng hơn bao giờ hết
      </h1>
      <p className="mt-4 text-lg text-gray-600 max-w-xl">
        Hệ thống học từ vựng Spaced Repetition, luyện viết chữ Hán, thi thử JLPT có chấm điểm chuẩn hóa và trợ lý AI đàm thoại.
      </p>

      <div className="mt-8 flex gap-4">
        <Link
          to="/login"
          className="px-6 py-3 rounded-xl bg-indigo-600 text-white font-semibold hover:bg-indigo-700 transition"
        >
          Bắt đầu học ngay
        </Link>
      </div>

      <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-6 w-full text-left">
        <div className="p-6 bg-white rounded-xl border border-gray-200">
          <BookOpen className="w-8 h-8 text-indigo-600 mb-3" />
          <h3 className="font-semibold text-lg text-gray-900">Giáo trình bài bản</h3>
          <p className="text-gray-600 text-sm mt-1">Từ vựng, ngữ pháp, chữ Hán chia theo lộ trình từng cấp độ JLPT.</p>
        </div>
        <div className="p-6 bg-white rounded-xl border border-gray-200">
          <Sparkles className="w-8 h-8 text-indigo-600 mb-3" />
          <h3 className="font-semibold text-lg text-gray-900">Flashcard SRS</h3>
          <p className="text-gray-600 text-sm mt-1">Thuật toán ghi nhớ ngắt quãng SM-2 giúp học sâu nhớ lâu.</p>
        </div>
        <div className="p-6 bg-white rounded-xl border border-gray-200">
          <Trophy className="w-8 h-8 text-indigo-600 mb-3" />
          <h3 className="font-semibold text-lg text-gray-900">Thi thử JLPT chuẩn</h3>
          <p className="text-gray-600 text-sm mt-1">Đề thi mô phỏng thực tế với bảng điểm quy đổi Scaled Score chuẩn xác.</p>
        </div>
      </div>
    </div>
  )
}
