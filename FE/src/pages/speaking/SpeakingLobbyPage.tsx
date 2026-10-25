import React, { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Sparkles,
  Clock,
  X,
  ArrowUpRight,
  Search,
  SlidersHorizontal,
  Dice5,
  AlertTriangle,
  Loader2,
} from 'lucide-react'
import { speakingService } from '@/services/speaking.service'
import { useAuthStore } from '@/stores'
import { MaziikoMascot } from '@/components/speaking/MaziikoMascot'
import type { AiScenario, AiSession, JlptLevel } from '@/types/speaking.types'

// Danh sách các chủ đề hội thoại mô phỏng Mazii
const TOPIC_CATEGORIES = [
  {
    id: 'COMMUNITY',
    name: 'Khám phá',
    count: 'Cộng đồng',
    image: 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=600&auto=format&fit=crop&q=80',
  },
  {
    id: 'FRIENDSHIP_CHATTING',
    name: 'Kết bạn & Trò chuyện',
    count: '14 hội thoại',
    image: 'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=600&auto=format&fit=crop&q=80',
  },
  {
    id: 'LIVING_IN_JAPAN',
    name: 'Sống ở Nhật',
    count: '10 hội thoại',
    image: 'https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=600&auto=format&fit=crop&q=80',
  },
  {
    id: 'TRAVEL_TRANSPORT',
    name: 'Đi lại & Du lịch',
    count: '3 hội thoại',
    image: 'https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=600&auto=format&fit=crop&q=80',
  },
  {
    id: 'HELP_EMERGENCY',
    name: 'Lúc cần giúp đỡ',
    count: '5 hội thoại',
    image: 'https://images.unsplash.com/photo-1513407030348-c983a97b98d8?w=600&auto=format&fit=crop&q=80',
  },
  {
    id: 'DINING_SHOPPING',
    name: 'Đi ăn & Mua sắm',
    count: '8 hội thoại',
    image: 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600&auto=format&fit=crop&q=80',
  },
]

// Các mẫu tình huống ngẫu nhiên thú vị cho nút "🎲 Ngẫu nhiên"
const RANDOM_PRESETS = [
  {
    myRole: 'Khách hàng',
    aiRole: 'Nhân viên quán ramen',
    description: 'Tôi muốn gọi một bát mì ramen shoyu ít dầu mỡ, thêm một phần trứng luộc ajitama và gyoza.',
    level: 'N5' as JlptLevel,
  },
  {
    myRole: 'Du khách',
    aiRole: 'Nhân viên ga tàu Shinjuku',
    description: 'Tôi bị lạc đường và muốn hỏi cách đi tàu tuyến Yamanote đến Akihabara nhanh nhất.',
    level: 'N4' as JlptLevel,
  },
  {
    myRole: 'Sinh viên mới',
    aiRole: 'Tiền bối câu lạc bộ',
    description: 'Tôi muốn hỏi về lịch sinh hoạt của câu lạc bộ bóng đá và thủ tục đăng ký tham gia.',
    level: 'N3' as JlptLevel,
  },
  {
    myRole: 'Người đi thuê nhà',
    aiRole: 'Môi giới bất động sản',
    description: 'Tôi muốn tìm một căn hộ 1LDK gần ga với mức giá khoảng 7 vạn yên, hỏi về tiền cọc và tiền lễ.',
    level: 'N2' as JlptLevel,
  },
  {
    myRole: 'Khách hàng',
    aiRole: 'Quản lý cửa hàng điện tử',
    description: 'Chiếc tai nghe vừa mua hôm qua bị rè một bên, tôi muốn giải thích vấn đề và yêu cầu đổi mới.',
    level: 'N1' as JlptLevel,
  },
  {
    myRole: 'Bệnh nhân',
    aiRole: 'Bác sĩ phòng khám',
    description: 'Tôi bị sốt và đau họng từ tối qua, muốn mô tả triệu chứng và xin đơn thuốc uống.',
    level: 'N4' as JlptLevel,
  },
  {
    myRole: 'Nhân viên',
    aiRole: 'Trưởng phòng người Nhật',
    description: 'Tôi muốn báo cáo tiến độ dự án tuần này và xin ý kiến về phương án tổ chức cuộc họp khách hàng.',
    level: 'N3' as JlptLevel,
  },
  {
    myRole: 'Bạn học',
    aiRole: 'Bạn cùng lớp',
    description: 'Hỏi bài tập về nhà hôm qua và rủ bạn cùng ra thư viện ôn thi tiếng Nhật.',
    level: 'N5' as JlptLevel,
  },
]


// Màu sắc badge JLPT chuẩn Mazii
const getJlptBadgeStyle = (level: string) => {
  switch (level) {
    case 'N5':
      return 'bg-emerald-600 text-white'
    case 'N4':
      return 'bg-blue-600 text-white'
    case 'N3':
      return 'bg-amber-500 text-white'
    case 'N2':
      return 'bg-indigo-600 text-white'
    case 'N1':
      return 'bg-purple-600 text-white'
    default:
      return 'bg-slate-600 text-white'
  }
}

export const SpeakingLobbyPage: React.FC = () => {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)

  const [scenarios, setScenarios] = useState<AiScenario[]>([])
  const [historySessions, setHistorySessions] = useState<AiSession[]>([])

  // Drawer Khám phá & Chủ đề state
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)
  const [selectedDrawerTopic, setSelectedDrawerTopic] = useState<string>('COMMUNITY')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [filterLevel, setFilterLevel] = useState<JlptLevel | 'ALL'>('ALL')
  const [showFilterOptions, setShowFilterOptions] = useState(false)

  // Modal Tạo tình huống (Screenshot 1)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [customMyRole, setCustomMyRole] = useState('')
  const [customAiRole, setCustomAiRole] = useState('')
  const [customDescription, setCustomDescription] = useState('')
  const [customLevel, setCustomLevel] = useState<JlptLevel>('N5')
  const [isCreatingCustom, setIsCreatingCustom] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  // Tải danh sách kịch bản & lịch sử
  const fetchScenarios = () => {
    speakingService
      .getScenarios()
      .then((data) => setScenarios(data))
      .catch((err) => console.error('Lỗi tải kịch bản:', err))
  }

  useEffect(() => {
    fetchScenarios()

    speakingService
      .getUserSessions(0, 10)
      .then((res) => setHistorySessions(res.content || []))
      .catch((err) => console.error('Lỗi tải lịch sử:', err))
  }, [])

  // Xóa một phiên lịch sử
  const handleDeleteSession = async (e: React.MouseEvent, id: number) => {
    e.stopPropagation()
    try {
      await speakingService.deleteSession(id)
      setHistorySessions((prev) => prev.filter((s) => s.id !== id))
    } catch (err) {
      console.error('Lỗi xóa session:', err)
    }
  }

  // Xóa tất cả lịch sử
  const handleDeleteAllSessions = async () => {
    if (!window.confirm('Bạn có chắc muốn xóa tất cả lịch sử luyện tập?')) return
    try {
      await speakingService.deleteAllSessions()
      setHistorySessions([])
    } catch (err) {
      console.error('Lỗi xóa tất cả lịch sử:', err)
    }
  }

  // Chọn kịch bản để xem chi tiết / bắt đầu
  const handleOpenScenario = (scenario: AiScenario) => {
    navigate(`/speaking/scenario/${scenario.id}`)
  }

  // Bắt đầu ngẫu nhiên một mẫu cho Modal "Tạo tình huống"
  const handlePickRandomPreset = () => {
    const randomItem = RANDOM_PRESETS[Math.floor(Math.random() * RANDOM_PRESETS.length)]
    setCustomMyRole(randomItem.myRole)
    setCustomAiRole(randomItem.aiRole)
    setCustomDescription(randomItem.description)
    setCustomLevel(randomItem.level)
    setCreateError(null)
  }

  // Submit tạo tình huống tùy chỉnh và bắt đầu nói ngay
  const handleCreateCustomScenario = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!customMyRole.trim()) {
      setCreateError('Vui lòng nhập vai của bạn!')
      return
    }
    if (!customAiRole.trim()) {
      setCreateError('Vui lòng nhập vai của AI!')
      return
    }
    if (!customDescription.trim()) {
      setCreateError('Vui lòng nhập mô tả tình huống!')
      return
    }

    try {
      setIsCreatingCustom(true)
      setCreateError(null)

      // 1. Tạo kịch bản mới trên Backend
      const newScenario = await speakingService.createCustomScenario({
        userRole: customMyRole.trim(),
        aiRole: customAiRole.trim(),
        description: customDescription.trim(),
        jlptLevel: customLevel,
        title: customDescription.trim().length > 30
          ? customDescription.trim().substring(0, 30) + '...'
          : customDescription.trim(),
      })

      // 2. Khởi tạo phiên luyện nói với kịch bản vừa tạo
      const session = await speakingService.createSession({
        mode: 'SCENARIO',
        scenarioId: newScenario.id,
      })

      setShowCreateModal(false)
      // Navigate thẳng vào phòng luyện nói
      navigate(`/speaking/session/${session.id}`)
    } catch (err: any) {
      console.error('Lỗi tạo tình huống tùy chỉnh:', err)
      setCreateError(err.response?.data?.message || 'Không thể tạo tình huống. Vui lòng thử lại!')
    } finally {
      setIsCreatingCustom(false)
    }
  }

  // Lọc kịch bản hiển thị trong Drawer
  const filteredDrawerScenarios = useMemo(() => {
    return scenarios.filter((s) => {
      // 1. Lọc theo tab chủ đề: Nếu tab là COMMUNITY thì lấy các bài topicId === 'COMMUNITY' hoặc không thuộc danh mục tĩnh
      const matchTopic =
        selectedDrawerTopic === 'COMMUNITY'
          ? s.topicId === 'COMMUNITY' || !TOPIC_CATEGORIES.some((c) => c.id !== 'COMMUNITY' && c.id === s.topicId)
          : s.topicId === selectedDrawerTopic

      if (!matchTopic) return false

      // 2. Lọc theo cấp độ JLPT
      if (filterLevel !== 'ALL' && s.jlptLevel !== filterLevel) {
        return false
      }

      // 3. Lọc theo từ khóa tìm kiếm
      if (searchKeyword.trim()) {
        const q = searchKeyword.trim().toLowerCase()
        const matchTitle = (s.title || '').toLowerCase().includes(q)
        const matchDesc = (s.description || '').toLowerCase().includes(q)
        const matchRoles = (s.userRole || '').toLowerCase().includes(q) || (s.aiRole || '').toLowerCase().includes(q)
        return matchTitle || matchDesc || matchRoles
      }

      return true
    })
  }, [scenarios, selectedDrawerTopic, filterLevel, searchKeyword])

  const username = user?.fullName || 'bạn'

  // Mở Drawer chủ đề
  const handleOpenDrawer = (topicId = 'COMMUNITY') => {
    setSelectedDrawerTopic(topicId)
    setIsDrawerOpen(true)
  }

  return (
    <div className="min-h-screen bg-[#F0F2F5] text-slate-800">
      {/* ==================================================================== */}
      {/* Main Container 2 cột: Cột trái (Nội dung) + Cột phải (Lịch sử) */}
      {/* ==================================================================== */}
      <div className="max-w-[1550px] mx-auto px-4 sm:px-8 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* ================================================================ */}
          {/* CỘT TRÁI (8/12 cột): Luyện nói AI */}
          {/* ================================================================ */}
          <div className="lg:col-span-8 space-y-5">
            {/* 1. Greeting Card chào buổi tối */}
            <div className="relative bg-white rounded-2xl p-6 sm:p-8 border border-slate-200/80 shadow-xs flex flex-col items-center justify-center text-center">
              <div className="mb-4">
                <MaziikoMascot size={84} showAura={true} showBadge={true} />
              </div>
              <h2 className="text-lg sm:text-xl font-bold text-[#1F51FF] mb-1">
                Chào {username}!
              </h2>
              <p className="text-xs sm:text-sm text-slate-500 font-medium">
                Hôm nay luyện nói tiếng Nhật cùng Maziiko nhé?
              </p>
            </div>

            {/* 2. Hai nút bấm chính chuẩn giao diện Screenshot 2: "✨ Tạo tình huống" & "💬 Trò chuyện tự do" */}
            <div className="flex items-center justify-center gap-3 p-1">
              <button
                type="button"
                onClick={() => setShowCreateModal(true)}
                className="flex-1 py-3 px-6 rounded-full font-bold text-sm sm:text-base flex items-center justify-center gap-2 transition-all bg-linear-to-r from-purple-600 via-indigo-600 to-blue-500 text-white shadow-md shadow-indigo-500/25 hover:opacity-95 active:scale-[0.99] cursor-pointer"
              >
                <Sparkles className="w-4 h-4 fill-current" />
                <span>Tạo tình huống</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/speaking/free')}
                className="flex-1 py-3 px-6 rounded-full font-bold text-sm sm:text-base flex items-center justify-center gap-2 transition-all bg-white text-slate-600 border border-slate-200 hover:bg-slate-50 active:scale-[0.99] cursor-pointer"
              >
                <span className="w-4 h-4 rounded-full bg-purple-600 text-white text-[10px] flex items-center justify-center font-bold">
                  AI
                </span>
                <span>Trò chuyện tự do</span>
              </button>
            </div>

            {/* ============================================================== */}
            {/* 3. Banner Khám phá (Click mở Drawer cộng đồng chuẩn Screenshot 2) */}
            {/* ============================================================== */}
            <div
              onClick={() => handleOpenDrawer('COMMUNITY')}
              className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs cursor-pointer hover:border-indigo-300 hover:shadow-sm transition group"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-bold text-sm sm:text-base text-slate-800 group-hover:text-indigo-600 transition">
                  Khám phá
                </span>
                <ArrowUpRight className="w-4 h-4 text-slate-400 group-hover:text-indigo-600 transition" />
              </div>
              <div className="p-2.5 rounded-xl bg-purple-50/70 border border-purple-100 flex items-center justify-between text-xs">
                <div className="flex items-center gap-1.5">
                  <div className="flex -space-x-1.5">
                    <span className="w-6 h-6 rounded-full bg-purple-600 text-white text-[10px] font-bold flex items-center justify-center border-2 border-white">
                      Y
                    </span>
                    <span className="w-6 h-6 rounded-full bg-indigo-600 text-white text-[10px] font-bold flex items-center justify-center border-2 border-white">
                      H
                    </span>
                    <span className="w-6 h-6 rounded-full bg-amber-500 text-white text-[10px] font-bold flex items-center justify-center border-2 border-white">
                      T
                    </span>
                    <span className="w-6 h-6 rounded-full bg-blue-500 text-white text-[10px] font-bold flex items-center justify-center border-2 border-white">
                      Z
                    </span>
                  </div>
                  <span className="text-purple-900 font-semibold ml-1">+</span>
                </div>
                <span className="text-purple-900 font-medium">417 đang luyện tập</span>
              </div>
            </div>

            {/* ============================================================== */}
            {/* 4. Hội thoại theo chủ đề */}
            {/* ============================================================== */}
            <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs">
              <h3 className="font-bold text-base text-slate-800 mb-4 text-left">
                Hội thoại theo chủ đề
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {TOPIC_CATEGORIES.filter((t) => t.id !== 'COMMUNITY').map((topic) => (
                  <div
                    key={topic.id}
                    onClick={() => handleOpenDrawer(topic.id)}
                    className="rounded-xl overflow-hidden border border-slate-200 hover:border-indigo-400 hover:shadow-md transition-all cursor-pointer group bg-slate-50 text-center"
                  >
                    <div className="h-32 w-full overflow-hidden bg-slate-200">
                      <img
                        src={topic.image}
                        alt={topic.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                    </div>
                    <div className="p-3">
                      <h4 className="font-bold text-sm text-slate-800 group-hover:text-indigo-600 transition-colors">
                        {topic.name}
                      </h4>
                      <span className="text-xs text-slate-400 font-medium">
                        {topic.count}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* ================================================================ */}
          {/* CỘT PHẢI (4/12 cột): Lịch sử luyện tập */}
          {/* ================================================================ */}
          <div className="lg:col-span-4">
            <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs sticky top-20 text-left">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
                <span className="font-bold text-sm text-slate-800">Lịch sử luyện tập</span>
                {historySessions.length > 0 && (
                  <button
                    onClick={handleDeleteAllSessions}
                    className="text-xs text-slate-400 hover:text-rose-500 font-medium transition"
                  >
                    Xóa tất cả
                  </button>
                )}
              </div>

              {historySessions.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">
                  <Clock className="w-6 h-6 mx-auto mb-1 text-slate-300" />
                  <span>Chưa có buổi luyện nói nào</span>
                </div>
              ) : (
                <div className="space-y-1 divide-y divide-slate-100">
                  {historySessions.map((session) => (
                    <div
                      key={session.id}
                      onClick={() => navigate(`/speaking/session/${session.id}`)}
                      className="pt-2.5 pb-2 cursor-pointer hover:bg-slate-50 p-2 rounded-xl transition group"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <h4 className="font-semibold text-xs sm:text-sm text-slate-800 group-hover:text-indigo-600 transition truncate max-w-[200px]">
                            {session.title}
                          </h4>
                          <span
                            className={`text-[11px] font-medium ${
                              session.mode === 'SCENARIO' ? 'text-blue-600' : 'text-emerald-600'
                            }`}
                          >
                            {session.mode === 'SCENARIO' ? 'Tình huống có sẵn' : 'Trò chuyện tự do'}
                          </span>
                        </div>

                        <div className="text-right flex flex-col items-end">
                          <button
                            onClick={(e) => handleDeleteSession(e, session.id)}
                            className="text-slate-300 hover:text-rose-500 p-0.5 rounded transition"
                            title="Xóa"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                          <span className="text-[10px] text-slate-400 mt-1">
                            {session.createdAt
                              ? new Date(session.createdAt).toISOString().split('T')[0]
                              : ''}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ==================================================================== */}
      {/* MODAL: TẠO TÌNH HUỐNG (Mô phỏng 1:1 Screenshot 1)                     */}
      {/* ==================================================================== */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden animate-scale-up border border-slate-100">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="text-lg font-bold text-slate-800">Tạo tình huống</h3>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body / Form */}
            <form onSubmit={handleCreateCustomScenario} className="p-6 space-y-4 text-left">
              {createError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
                  {createError}
                </div>
              )}

              {/* 1. Vai của tôi + Nút [🎲 Ngẫu nhiên] */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs sm:text-sm font-bold text-slate-700">
                    Vai của tôi
                  </label>
                  <button
                    type="button"
                    onClick={handlePickRandomPreset}
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg border border-slate-200 hover:border-indigo-400 hover:bg-indigo-50/50 text-slate-700 hover:text-indigo-600 text-xs font-semibold transition"
                    title="Điền ngẫu nhiên gợi ý thú vị"
                  >
                    <Dice5 className="w-3.5 h-3.5" />
                    <span>Ngẫu nhiên</span>
                  </button>
                </div>
                <input
                  type="text"
                  value={customMyRole}
                  onChange={(e) => setCustomMyRole(e.target.value)}
                  placeholder="Bạn muốn đóng vai gì"
                  maxLength={100}
                  className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none text-sm text-slate-800 placeholder:text-slate-400 transition"
                />
              </div>

              {/* 2. Vai của AI */}
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-700 mb-1.5">
                  Vai của AI
                </label>
                <input
                  type="text"
                  value={customAiRole}
                  onChange={(e) => setCustomAiRole(e.target.value)}
                  placeholder="Bạn muốn AI đóng vai gì"
                  maxLength={100}
                  className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none text-sm text-slate-800 placeholder:text-slate-400 transition"
                />
              </div>

              {/* 3. Mô tả (max 200 ký tự) */}
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-700 mb-1.5">
                  Mô tả
                </label>
                <div className="relative">
                  <textarea
                    value={customDescription}
                    onChange={(e) => {
                      if (e.target.value.length <= 200) {
                        setCustomDescription(e.target.value)
                      }
                    }}
                    rows={4}
                    placeholder="Mô tả tình huống giữa bạn và AI? Bạn muốn luyện tập gì?"
                    className="w-full px-4 py-3 rounded-2xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none text-sm text-slate-800 placeholder:text-slate-400 resize-none transition"
                  />
                  <span className="absolute bottom-3 right-3 text-[11px] text-slate-400 font-medium">
                    {customDescription.length}/200
                  </span>
                </div>
              </div>

              {/* 4. Trình độ (N5 - N1) */}
              <div>
                <label className="block text-xs sm:text-sm font-bold text-slate-700 mb-2">
                  Trình độ
                </label>
                <div className="flex items-center gap-2">
                  {(['N5', 'N4', 'N3', 'N2', 'N1'] as JlptLevel[]).map((lvl) => {
                    const isSelected = customLevel === lvl
                    return (
                      <button
                        key={lvl}
                        type="button"
                        onClick={() => setCustomLevel(lvl)}
                        className={`flex-1 py-2 rounded-xl text-xs sm:text-sm font-bold transition-all ${
                          isSelected
                            ? 'bg-blue-600 text-white shadow-xs scale-102'
                            : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                        }`}
                      >
                        {lvl}
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* 5. Nút submit Tạo tình huống */}
              <div className="pt-3">
                <button
                  type="submit"
                  disabled={isCreatingCustom}
                  className="w-full py-3.5 rounded-full font-bold text-sm sm:text-base flex items-center justify-center gap-2 transition-all bg-linear-to-r from-purple-600 via-indigo-600 to-blue-500 text-white shadow-lg shadow-indigo-500/25 hover:opacity-95 active:scale-[0.99] disabled:opacity-50 cursor-pointer"
                >
                  {isCreatingCustom ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      <span>Đang khởi tạo tình huống...</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4 fill-current" />
                      <span>Tạo tình huống</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* DRAWER: DANH SÁCH KHÁM PHÁ & CHỦ ĐỀ (Mô phỏng 1:1 Screenshot 2)     */}
      {/* ==================================================================== */}
      {isDrawerOpen && (
        <div className="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-xs animate-fade-in">
          {/* Backdrop click to close */}
          <div className="flex-1" onClick={() => setIsDrawerOpen(false)} />

          <div className="w-full max-w-md bg-white h-full shadow-2xl flex flex-col overflow-hidden animate-slide-left">
            {/* Header Tabs: Khám phá | Kết bạn & Trò chuyện | Sống ở Nhật... | X */}
            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 bg-white">
              <div className="flex items-center gap-4 overflow-x-auto text-xs font-semibold scrollbar-none pr-2">
                {TOPIC_CATEGORIES.map((topic) => (
                  <button
                    key={topic.id}
                    onClick={() => {
                      setSelectedDrawerTopic(topic.id)
                      setSearchKeyword('')
                    }}
                    className={`pb-1 whitespace-nowrap transition border-b-2 ${
                      selectedDrawerTopic === topic.id
                        ? 'border-blue-600 text-blue-600 font-bold'
                        : 'border-transparent text-slate-500 hover:text-slate-800'
                    }`}
                  >
                    {topic.name}
                  </button>
                ))}
              </div>

              <button
                type="button"
                onClick={() => setIsDrawerOpen(false)}
                className="p-1 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition shrink-0 ml-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Thanh Search & Filter chuẩn Screenshot 2 */}
            <div className="p-3 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center gap-2">
                <div className="flex-1 relative flex items-center">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3" />
                  <input
                    type="text"
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                    placeholder="Tìm kiếm"
                    className="w-full pl-9 pr-3 py-2 bg-white rounded-xl border border-slate-200 outline-none text-xs sm:text-sm text-slate-700 placeholder:text-slate-400 focus:border-indigo-500 transition"
                  />
                  {searchKeyword && (
                    <button
                      type="button"
                      onClick={() => setSearchKeyword('')}
                      className="absolute right-2.5 text-slate-400 hover:text-slate-600"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                <button
                  type="button"
                  onClick={() => setShowFilterOptions((prev) => !prev)}
                  className={`p-2 rounded-xl border transition ${
                    filterLevel !== 'ALL' || showFilterOptions
                      ? 'bg-indigo-50 border-indigo-300 text-indigo-600'
                      : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                  title="Bộ lọc cấp độ"
                >
                  <SlidersHorizontal className="w-4 h-4" />
                </button>
              </div>

              {/* Bộ lọc cấp độ JLPT nếu mở */}
              {showFilterOptions && (
                <div className="flex items-center gap-1.5 mt-2.5 pt-2 border-t border-slate-200/70 text-xs">
                  <span className="text-slate-500 font-medium mr-1">Cấp độ:</span>
                  {(['ALL', 'N5', 'N4', 'N3', 'N2', 'N1'] as const).map((lvl) => (
                    <button
                      key={lvl}
                      type="button"
                      onClick={() => setFilterLevel(lvl)}
                      className={`px-2 py-0.5 rounded-md font-semibold transition ${
                        filterLevel === lvl
                          ? 'bg-blue-600 text-white'
                          : 'bg-slate-200/70 text-slate-600 hover:bg-slate-200'
                      }`}
                    >
                      {lvl === 'ALL' ? 'Tất cả' : lvl}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Danh sách các card kịch bản chuẩn Screenshot 2 */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 text-left">
              {filteredDrawerScenarios.length === 0 ? (
                <div className="text-center py-12 text-slate-400 text-xs">
                  {searchKeyword
                    ? 'Không tìm thấy tình huống nào phù hợp.'
                    : 'Chưa có tình huống nào trong mục này.'}
                </div>
              ) : (
                filteredDrawerScenarios.map((item, idx) => {
                  // Giả lập tác giả & view ngẫu nhiên chuẩn cộng đồng nếu không có trong entity
                  const authorList = ['Đạt Trần', 'ánh ngọc', 'Cuong Nguyenvan', 'Mai Phương', 'Huy Hoàng']
                  const authorName = authorList[item.id ? item.id % authorList.length : idx % authorList.length]
                  const viewCount = item.id ? (item.id * 3) % 24 : 1
                  const dateStr = item.createdAt
                    ? new Date(item.createdAt).toISOString().split('T')[0]
                    : '25-10-2026'

                  return (
                    <div
                      key={item.id}
                      onClick={() => handleOpenScenario(item)}
                      className="p-4 rounded-2xl border border-slate-200/80 hover:border-indigo-400 hover:shadow-xs transition bg-white cursor-pointer group space-y-2"
                    >
                      {/* Hàng 1: Tiêu đề + Badge JLPT + Icon Flag ⚠️ */}
                      <div className="flex items-start justify-between gap-2">
                        <h4 className="font-bold text-sm text-slate-800 group-hover:text-indigo-600 transition line-clamp-1 flex-1">
                          {item.title}
                        </h4>

                        <div className="flex items-center gap-1.5 shrink-0">
                          <span
                            className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${getJlptBadgeStyle(
                              item.jlptLevel
                            )}`}
                          >
                            {item.jlptLevel}
                          </span>
                          <span
                            className="text-slate-400 hover:text-amber-600 p-0.5 transition"
                            title="Báo cáo nội dung"
                            onClick={(e) => {
                              e.stopPropagation()
                              alert('Đã gửi báo cáo tình huống này đến ban quản trị.')
                            }}
                          >
                            <AlertTriangle className="w-3.5 h-3.5" />
                          </span>
                        </div>
                      </div>

                      {/* Hàng 2: Mô tả ngắn */}
                      <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed font-normal">
                        {item.description}
                      </p>

                      {/* Hàng 3: Ngày tạo · Lượt xem · Avatar + Tên tác giả */}
                      <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1">
                        <span>
                          {dateStr} · {viewCount} lượt xem
                        </span>

                        <div className="flex items-center gap-1.5 text-slate-600 font-medium">
                          <div className="w-4 h-4 rounded-full bg-slate-200 flex items-center justify-center overflow-hidden text-[9px] font-bold text-slate-700">
                            {authorName.charAt(0).toUpperCase()}
                          </div>
                          <span className="truncate max-w-[100px]">{authorName}</span>
                        </div>
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
