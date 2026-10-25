import React, { useState, useEffect } from 'react'
import {
  ChevronDown,
  Info,
  Crown,
  Loader2,
} from 'lucide-react'
import { examService } from '@/services'
import type { LeaderboardData, JlptLevel } from '@/types'

// Bảng màu avatar phong phú cho danh sách xếp hạng
const AVATAR_BG_COLORS = [
  'bg-emerald-600',
  'bg-blue-600',
  'bg-indigo-600',
  'bg-purple-600',
  'bg-rose-600',
  'bg-amber-600',
  'bg-teal-600',
  'bg-cyan-600',
]

interface LeaderboardWidgetProps {
  initialLevel?: JlptLevel
  className?: string
}

export const LeaderboardWidget: React.FC<LeaderboardWidgetProps> = ({
  initialLevel = 'N5',
  className = '',
}) => {
  // Bộ lọc
  const [selectedLevel, setSelectedLevel] = useState<JlptLevel>(initialLevel)
  const [selectedTimeframe, setSelectedTimeframe] = useState<'WEEK' | 'MONTH' | 'ALL'>('WEEK')

  // Dữ liệu
  const [data, setData] = useState<LeaderboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const levels: JlptLevel[] = ['N5', 'N4', 'N3', 'N2', 'N1']

  useEffect(() => {
    if (initialLevel && levels.includes(initialLevel)) {
      setSelectedLevel(initialLevel)
    }
  }, [initialLevel])

  useEffect(() => {
    fetchLeaderboard()
  }, [selectedLevel, selectedTimeframe])

  const fetchLeaderboard = async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await examService.getLeaderboard({
        level: selectedLevel,
        timeframe: selectedTimeframe,
      })
      if (res.data) {
        setData(res.data)
      }
    } catch (err: any) {
      console.error('Failed to load leaderboard', err)
      setError(err?.response?.data?.message || 'Không thể tải bảng xếp hạng lúc này.')
    } finally {
      setLoading(false)
    }
  }

  const rankings = data?.rankings || []
  const top1 = rankings[0]
  const top2 = rankings[1]
  const top3 = rankings[2]
  const otherRankings = rankings.slice(3)
  const myRank = data?.myRank

  return (
    <div
      className={`bg-white rounded-3xl border border-slate-200/90 shadow-sm p-4 sm:p-5 space-y-4 ${className}`}
    >
      {/* 1. HEADER: TIÊU ĐỀ + NHÃN JLPT */}
      <div className="flex items-center justify-between pb-1">
        <h2 className="text-lg sm:text-xl font-black text-slate-900 tracking-tight">
          Bảng xếp hạng
        </h2>

        {/* Nhãn JLPT */}
        <span className="px-3 py-1 bg-slate-100 text-slate-800 text-xs font-bold rounded-xl border border-slate-200">
          JLPT
        </span>
      </div>

      {/* 2. DÃY NÚT CHỌN CẤP ĐỘ JLPT (N5 -> N1) VÀ DROPDOWN THỜI GIAN */}
      <div className="flex items-center justify-between gap-1.5 border-b border-slate-100 pb-3">
        {/* Các cấp độ N5, N4, N3, N2, N1 */}
        <div className="flex items-center gap-1 sm:gap-1.5">
          {levels.map((lvl) => {
            const isActive = selectedLevel === lvl
            return (
              <button
                key={lvl}
                type="button"
                onClick={() => setSelectedLevel(lvl)}
                className={`px-2.5 sm:px-3 py-1 rounded-xl text-xs font-bold transition cursor-pointer ${
                  isActive
                    ? 'bg-blue-600 text-white shadow-xs font-black'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                {lvl}
              </button>
            )
          })}
        </div>

        {/* Pulldown Thời gian (Tuần / Tháng / Tất cả) */}
        <div className="relative inline-flex items-center">
          <select
            value={selectedTimeframe}
            onChange={(e: any) => setSelectedTimeframe(e.target.value)}
            className="appearance-none bg-slate-100 hover:bg-slate-200/80 text-slate-800 text-xs font-bold rounded-xl pl-3 pr-7 py-1 border border-slate-200/80 cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition"
          >
            <option value="WEEK">Tuần</option>
            <option value="MONTH">Tháng</option>
            <option value="ALL">Tất cả</option>
          </select>
          <ChevronDown className="w-3.5 h-3.5 text-slate-500 absolute right-2 pointer-events-none" />
        </div>
      </div>

      {/* Thông báo lỗi nếu có */}
      {error && (
        <div className="p-2.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl">
          {error}
        </div>
      )}

      {/* 3. KHUNG THÔNG BÁO THỨ HẠNG CỦA HỌC VIÊN */}
      {loading ? (
        <div className="py-3 flex items-center justify-center gap-2 text-xs font-semibold text-slate-400">
          <Loader2 className="w-4 h-4 text-blue-600 animate-spin" />
          <span>Đang cập nhật thứ hạng...</span>
        </div>
      ) : myRank ? (
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200/90 rounded-2xl p-3 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-black text-xs shrink-0 shadow-2xs">
              #{myRank.rank}
            </div>
            <div className="truncate">
              <p className="font-bold text-blue-950 text-xs truncate">
                Thứ hạng: <span className="text-blue-600 font-black">Top #{myRank.rank}</span>
              </p>
              <p className="text-[10px] text-blue-800/80">
                {selectedLevel} • {Math.round((myRank.timeSpentSeconds || 0) / 60)} phút
              </p>
            </div>
          </div>
          <div className="text-right shrink-0 pl-2">
            <span className="text-base font-black text-blue-950 font-mono">
              {myRank.score}
            </span>
            <span className="text-[11px] text-blue-700 font-semibold">/{myRank.maxScore || 180}</span>
          </div>
        </div>
      ) : (
        <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-3 flex items-center gap-2 text-xs text-slate-600 leading-tight">
          <Info className="w-4 h-4 text-slate-400 shrink-0" />
          <span>Bạn cần hoàn thành một bài thi để vào bảng xếp hạng</span>
        </div>
      )}

      {/* 4. BỤC VINH DANH TOP 3 & DANH SÁCH THỨ HẠNG */}
      {loading ? (
        <div className="py-16 text-center text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin mx-auto text-blue-600 mb-2" />
          <p className="text-xs font-semibold">Đang tải bảng xếp hạng...</p>
        </div>
      ) : rankings.length === 0 ? (
        <div className="py-12 px-4 text-center bg-slate-50 rounded-2xl border border-dashed border-slate-200">
          <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto mb-2 text-slate-400">
            <Crown className="w-6 h-6" />
          </div>
          <p className="text-xs font-bold text-slate-700 mb-1">Chưa có lượt thi nào</p>
          <p className="text-[11px] text-slate-500 max-w-xs mx-auto">
            Chưa có thí sinh nào hoàn thành bài thi {selectedLevel} trong khoảng thời gian này.
            Hãy làm bài thi ngay để ghi danh lên đầu bảng!
          </p>
        </div>
      ) : (
        <>
          {/* BỤC VINH DANH TOP 3 (PODIUM 02 - 01 - 03) */}
          <div className="grid grid-cols-3 gap-2 items-end pt-3 pb-1">
            {/* VỊ TRÍ HẠNG 2 (BÊN TRÁI - BẠC / TÍM NHẠT) */}
            <div className="bg-gradient-to-b from-[#c3ccdf] via-[#e2e8f0] to-[#f8fafc] border border-slate-300/80 rounded-2xl p-2 flex flex-col justify-between items-center text-center shadow-2xs h-48 sm:h-52">
              {/* Huy hiệu 02 */}
              <div className="w-6 h-6 rounded-full bg-white text-slate-800 font-black text-[11px] border border-slate-300 shadow-2xs flex items-center justify-center shrink-0">
                02
              </div>

              {/* Avatar thí sinh Top 2 */}
              <div className="my-auto flex flex-col items-center">
                <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-full ring-3 ring-white shadow-xs overflow-hidden flex items-center justify-center font-bold text-white text-base sm:text-lg bg-purple-600">
                  {top2?.avatarUrl ? (
                    <img src={top2.avatarUrl} alt={top2.fullName} className="w-full h-full object-cover" />
                  ) : (
                    <span>{top2 ? top2.fullName.charAt(0).toUpperCase() : '-'}</span>
                  )}
                </div>
              </div>

              {/* Thẻ trắng chứa tên & điểm */}
              <div className="w-full bg-white/95 rounded-xl py-1.5 px-1 text-center shadow-2xs">
                {top2 ? (
                  <>
                    <p className="text-[10px] sm:text-[11px] font-bold text-slate-800 truncate" title={top2.fullName}>
                      {top2.fullName}
                    </p>
                    <p className="text-xs sm:text-sm font-black text-slate-900 font-mono">
                      {top2.score}
                    </p>
                  </>
                ) : (
                  <>
                    <p className="text-[10px] text-slate-400 font-medium">Chưa có</p>
                    <p className="text-xs text-slate-400 font-mono font-bold">-</p>
                  </>
                )}
              </div>
            </div>

            {/* VỊ TRÍ HẠNG 1 (Ở GIỮA - VÀNG HOÀNG GIA - CAO NHẤT & HUY CHƯƠNG) */}
            <div className="bg-gradient-to-b from-[#f59e0b] via-[#fbbf24] to-[#fef3c7] border-2 border-amber-400 rounded-2xl p-2 flex flex-col justify-between items-center text-center shadow-md h-56 sm:h-60 -translate-y-2 relative">
              {/* Huy hiệu 01 */}
              <div className="w-7 h-7 rounded-full bg-white text-amber-950 font-black text-xs border-2 border-amber-400 shadow-2xs flex items-center justify-center shrink-0">
                01
              </div>

              {/* Avatar thí sinh Top 1 với Huy chương Rosette Vàng sang trọng */}
              <div className="my-auto flex flex-col items-center relative">
                <div className="relative p-0.5 rounded-full bg-gradient-to-tr from-amber-500 to-yellow-300 shadow-sm">
                  <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-full ring-3 ring-white shadow-xs overflow-hidden flex items-center justify-center font-black text-white text-lg sm:text-xl bg-blue-600">
                    {top1?.avatarUrl ? (
                      <img src={top1.avatarUrl} alt={top1.fullName} className="w-full h-full object-cover" />
                    ) : (
                      <span>{top1 ? top1.fullName.charAt(0).toUpperCase() : '-'}</span>
                    )}
                  </div>
                  {/* Huy hiệu vương miện nhỏ đính kèm góc dưới */}
                  <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-amber-400 border border-white text-amber-950 flex items-center justify-center shadow-2xs">
                    <Crown className="w-3 h-3 fill-amber-950" />
                  </div>
                </div>
              </div>

              {/* Thẻ trắng chứa tên & điểm */}
              <div className="w-full bg-white/95 rounded-xl py-2 px-1 text-center shadow-xs">
                {top1 ? (
                  <>
                    <p className="text-[11px] sm:text-xs font-black text-slate-900 truncate" title={top1.fullName}>
                      {top1.fullName}
                    </p>
                    <p className="text-sm sm:text-base font-black text-amber-900 font-mono">
                      {top1.score}
                    </p>
                  </>
                ) : (
                  <>
                    <p className="text-[10px] text-slate-400 font-medium">Chưa có</p>
                    <p className="text-xs text-slate-400 font-mono font-bold">-</p>
                  </>
                )}
              </div>
            </div>

            {/* VỊ TRÍ HẠNG 3 (BÊN PHẢI - ĐỒNG / CAM) */}
            <div className="bg-gradient-to-b from-[#e39a62] via-[#fed7aa] to-[#fff7ed] border border-orange-300/80 rounded-2xl p-2 flex flex-col justify-between items-center text-center shadow-2xs h-44 sm:h-48">
              {/* Huy hiệu 03 */}
              <div className="w-6 h-6 rounded-full bg-white text-slate-800 font-black text-[11px] border border-slate-300 shadow-2xs flex items-center justify-center shrink-0">
                03
              </div>

              {/* Avatar thí sinh Top 3 */}
              <div className="my-auto flex flex-col items-center">
                <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-full ring-3 ring-white shadow-xs overflow-hidden flex items-center justify-center font-bold text-white text-base sm:text-lg bg-orange-600">
                  {top3?.avatarUrl ? (
                    <img src={top3.avatarUrl} alt={top3.fullName} className="w-full h-full object-cover" />
                  ) : (
                    <span>{top3 ? top3.fullName.charAt(0).toUpperCase() : '-'}</span>
                  )}
                </div>
              </div>

              {/* Thẻ trắng chứa tên & điểm */}
              <div className="w-full bg-white/90 rounded-xl py-1.5 px-1 text-center shadow-2xs">
                {top3 ? (
                  <>
                    <p className="text-[10px] sm:text-[11px] font-bold text-slate-800 truncate" title={top3.fullName}>
                      {top3.fullName}
                    </p>
                    <p className="text-xs sm:text-sm font-black text-slate-900 font-mono">
                      {top3.score}
                    </p>
                  </>
                ) : (
                  <>
                    <p className="text-[10px] text-slate-400 font-medium">Chưa có</p>
                    <p className="text-xs text-slate-400 font-mono font-bold">-</p>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* 5. DANH SÁCH THỨ HẠNG TỪ HẠNG 4 TRỞ ĐI */}
          {otherRankings.length > 0 && (
            <div className="pt-1">
              <div className="max-h-[320px] overflow-y-auto divide-y divide-slate-100 pr-1">
                {otherRankings.map((item, idx) => {
                  const colorBg = AVATAR_BG_COLORS[idx % AVATAR_BG_COLORS.length]
                  const isUser = item.isCurrentUser

                  return (
                    <div
                      key={item.userId || idx}
                      className={`flex items-center justify-between py-2 px-1.5 sm:px-2 rounded-xl transition ${
                        isUser
                          ? 'bg-blue-50/80 border border-blue-200/80 font-bold'
                          : 'hover:bg-slate-50'
                      }`}
                    >
                      {/* Cột trái: Số hạng + Avatar + Họ tên */}
                      <div className="flex items-center gap-2.5 sm:gap-3 min-w-0">
                        {/* Số hạng (4, 5, 6, 7...) */}
                        <span className="w-5 text-center text-xs font-bold text-slate-500 font-mono shrink-0">
                          {item.rank}
                        </span>

                        {/* Avatar */}
                        <div className="w-8 h-8 rounded-full overflow-hidden shrink-0 shadow-2xs flex items-center justify-center font-bold text-white text-xs">
                          {item.avatarUrl ? (
                            <img
                              src={item.avatarUrl}
                              alt={item.fullName}
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                e.currentTarget.style.display = 'none'
                              }}
                            />
                          ) : null}
                          <div className={`w-full h-full ${colorBg} flex items-center justify-center`}>
                            {item.fullName.charAt(0).toUpperCase()}
                          </div>
                        </div>

                        {/* Tên thí sinh */}
                        <div className="truncate">
                          <span className={`text-xs truncate block ${isUser ? 'font-black text-blue-950' : 'font-semibold text-slate-800'}`}>
                            {item.fullName}
                          </span>
                          {isUser && (
                            <span className="text-[9px] text-blue-600 font-bold bg-blue-100/80 px-1 py-0.2 rounded">
                              Bạn
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Cột phải: Điểm số */}
                      <div className="pl-2 shrink-0">
                        <span className="text-sm sm:text-base font-black text-slate-900 font-mono">
                          {item.score}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default LeaderboardWidget
