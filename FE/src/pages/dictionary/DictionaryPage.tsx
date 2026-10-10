import React, { useState, useEffect, useRef } from 'react'
import {
  Search,
  X,
  Sparkles,
  Filter,
  ChevronRight,
  BookOpen,
  Clock,
  Trash2,
  ArrowRight,
  Flame,
  Award,
} from 'lucide-react'
import type {
  JlptLevel,
  VocabularySummary,
  KanjiSummary,
  GrammarSummary,
  PageResponse,
  DictionaryUnifiedSearch,
} from '@/types'
import { dictionaryService } from '@/services'
import {
  VocabDetailPanel,
  KanjiDetailPanel,
  GrammarDetailPanel,
  KanaMatrixTab,
} from '@/components/dictionary'

type TabType = 'vocab' | 'kanji' | 'grammar' | 'kana'

interface SearchHistoryItem {
  id: string
  keyword: string
  tab: TabType
  title: string
  subtitle?: string
  meaning?: string
  timestamp: number
}

const STORAGE_KEY_HISTORY = 'elearning_dictionary_history'

export const DictionaryPage: React.FC = () => {
  const [keyword, setKeyword] = useState<string>('')
  const [debouncedKeyword, setDebouncedKeyword] = useState<string>('')
  const [activeTab, setActiveTab] = useState<TabType>('vocab')
  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | undefined>(undefined)

  // Autocomplete Suggestions Dropdown
  const [suggestions, setSuggestions] = useState<DictionaryUnifiedSearch | null>(null)
  const [isDropdownOpen, setIsDropdownOpen] = useState<boolean>(false)
  const searchContainerRef = useRef<HTMLDivElement>(null)

  // Lịch sử tra cứu gần đây từ localStorage
  const [history, setHistory] = useState<SearchHistoryItem[]>([])

  // Mục đang được chọn để hiển thị chi tiết ở Cột Phải (chuẩn Mazii)
  const [selectedVocabId, setSelectedVocabId] = useState<number | null>(null)
  const [selectedKanjiChar, setSelectedKanjiChar] = useState<string | null>(null)
  const [selectedGrammarId, setSelectedGrammarId] = useState<number | null>(null)

  // Dữ liệu phân trang cho Tab Từ vựng
  const [vocabPage, setVocabPage] = useState<PageResponse<VocabularySummary> | null>(null)
  const [vocabPageIndex, setVocabPageIndex] = useState<number>(0)
  const [isLoadingVocab, setIsLoadingVocab] = useState<boolean>(false)

  // Dữ liệu phân trang cho Tab Hán tự
  const [kanjiPage, setKanjiPage] = useState<PageResponse<KanjiSummary> | null>(null)
  const [kanjiPageIndex, setKanjiPageIndex] = useState<number>(0)
  const [isLoadingKanji, setIsLoadingKanji] = useState<boolean>(false)

  // Dữ liệu phân trang cho Tab Ngữ pháp
  const [grammarPage, setGrammarPage] = useState<PageResponse<GrammarSummary> | null>(null)
  const [grammarPageIndex, setGrammarPageIndex] = useState<number>(0)
  const [isLoadingGrammar, setIsLoadingGrammar] = useState<boolean>(false)

  // Đọc lịch sử từ localStorage khi khởi động
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY_HISTORY)
      if (raw) {
        setHistory(JSON.parse(raw))
      }
    } catch {
      // Bỏ qua lỗi parse
    }
  }, [])

  // Đóng dropdown khi click ra ngoài ô tìm kiếm
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(e.target as Node)) {
        setIsDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Lưu một mục vào lịch sử
  const saveToHistory = (item: Omit<SearchHistoryItem, 'id' | 'timestamp'>) => {
    const newItem: SearchHistoryItem = {
      ...item,
      id: `${item.tab}_${item.keyword}_${Date.now()}`,
      timestamp: Date.now(),
    }
    setHistory((prev) => {
      const filtered = prev.filter((h) => !(h.tab === item.tab && h.keyword === item.keyword))
      const updated = [newItem, ...filtered].slice(0, 12)
      try {
        localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(updated))
      } catch {
        // Bỏ qua
      }
      return updated
    })
  }

  // Xoá một mục trong lịch sử
  const removeHistoryItem = (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    setHistory((prev) => {
      const updated = prev.filter((item) => item.id !== id)
      try {
        localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(updated))
      } catch {
        // Bỏ qua
      }
      return updated
    })
  }

  // Xoá toàn bộ lịch sử
  const clearAllHistory = () => {
    setHistory([])
    try {
      localStorage.removeItem(STORAGE_KEY_HISTORY)
    } catch {
      // Bỏ qua
    }
  }

  // Debounce input tìm kiếm 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      const trimmed = keyword.trim()
      setDebouncedKeyword(trimmed)

      // Nếu có từ khóa, gọi gợi ý tìm kiếm tức thì (Unified Search) cho Dropdown
      if (trimmed.length > 0) {
        dictionaryService
          .searchUnified(trimmed, selectedLevel)
          .then((res) => {
            if (res.data) {
              setSuggestions(res.data)
              setIsDropdownOpen(true)
            }
          })
          .catch(() => {})
      } else {
        setSuggestions(null)
        setIsDropdownOpen(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [keyword, selectedLevel])

  // Reset trang về 0 khi đổi từ khóa, cấp độ JLPT hoặc đổi Tab
  useEffect(() => {
    setVocabPageIndex(0)
    setKanjiPageIndex(0)
    setGrammarPageIndex(0)
  }, [debouncedKeyword, selectedLevel, activeTab])

  // Kiểm tra xem người dùng đã thực hiện tra cứu chưa
  const hasSearched = debouncedKeyword.length > 0 || selectedLevel !== undefined

  // 1. Fetch danh sách Từ vựng khi có từ khóa hoặc bộ lọc JLPT
  useEffect(() => {
    if (activeTab !== 'vocab' || !hasSearched) {
      if (!hasSearched) setVocabPage(null)
      return
    }

    let isMounted = true
    setIsLoadingVocab(true)

    dictionaryService
      .searchVocabularies({
        keyword: debouncedKeyword,
        level: selectedLevel,
        page: vocabPageIndex,
        size: 15,
      })
      .then((res) => {
        if (isMounted && res.data) {
          setVocabPage(res.data)
          const items = res.data.items ?? []
          if (items.length > 0) {
            setSelectedVocabId(items[0].id)
            if (debouncedKeyword) {
              saveToHistory({
                tab: 'vocab',
                keyword: debouncedKeyword,
                title: items[0].word,
                subtitle: items[0].hiragana,
                meaning: items[0].meaningVi,
              })
            }
          } else {
            setSelectedVocabId(null)
          }
        }
      })
      .catch((err) => console.error('Lỗi tìm kiếm từ vựng:', err))
      .finally(() => {
        if (isMounted) setIsLoadingVocab(false)
      })

    return () => {
      isMounted = false
    }
  }, [debouncedKeyword, selectedLevel, activeTab, vocabPageIndex, hasSearched])

  // 2. Fetch danh sách Hán tự khi có từ khóa hoặc bộ lọc JLPT
  useEffect(() => {
    if (activeTab !== 'kanji' || !hasSearched) {
      if (!hasSearched) setKanjiPage(null)
      return
    }

    let isMounted = true
    setIsLoadingKanji(true)

    dictionaryService
      .searchKanjis({
        keyword: debouncedKeyword,
        level: selectedLevel,
        page: kanjiPageIndex,
        size: 15,
      })
      .then((res) => {
        if (isMounted && res.data) {
          setKanjiPage(res.data)
          const items = res.data.items ?? []
          if (items.length > 0) {
            setSelectedKanjiChar(items[0].character)
            if (debouncedKeyword) {
              saveToHistory({
                tab: 'kanji',
                keyword: debouncedKeyword,
                title: items[0].character,
                subtitle: items[0].hanViet,
                meaning: items[0].meaningVi,
              })
            }
          } else {
            setSelectedKanjiChar(null)
          }
        }
      })
      .catch((err) => console.error('Lỗi tìm kiếm Hán tự:', err))
      .finally(() => {
        if (isMounted) setIsLoadingKanji(false)
      })

    return () => {
      isMounted = false
    }
  }, [debouncedKeyword, selectedLevel, activeTab, kanjiPageIndex, hasSearched])

  // 3. Fetch danh sách Ngữ pháp khi có từ khóa hoặc bộ lọc JLPT
  useEffect(() => {
    if (activeTab !== 'grammar' || !hasSearched) {
      if (!hasSearched) setGrammarPage(null)
      return
    }

    let isMounted = true
    setIsLoadingGrammar(true)

    dictionaryService
      .searchGrammars({
        keyword: debouncedKeyword,
        level: selectedLevel,
        page: grammarPageIndex,
        size: 15,
      })
      .then((res) => {
        if (isMounted && res.data) {
          setGrammarPage(res.data)
          const items = res.data.items ?? []
          if (items.length > 0) {
            setSelectedGrammarId(items[0].id)
            if (debouncedKeyword) {
              saveToHistory({
                tab: 'grammar',
                keyword: debouncedKeyword,
                title: items[0].title,
                meaning: items[0].meaningVi,
              })
            }
          } else {
            setSelectedGrammarId(null)
          }
        }
      })
      .catch((err) => console.error('Lỗi tìm kiếm ngữ pháp:', err))
      .finally(() => {
        if (isMounted) setIsLoadingGrammar(false)
      })

    return () => {
      isMounted = false
    }
  }, [debouncedKeyword, selectedLevel, activeTab, grammarPageIndex, hasSearched])

  // Cấu hình placeholder và gợi ý nhanh theo tab
  const tabConfig = {
    vocab: {
      placeholder: 'Tra từ vựng tiếng Nhật, romaji, nghĩa (VD: taberu, 食べる, ăn, gakkou)...',
      suggestions: ['食べる', '日本', '学校', '行く', '先生', '友達', '勉強'],
      badge: 'Từ vựng tiếng Nhật',
    },
    kanji: {
      placeholder: 'Tra Hán tự, âm Hán Việt, romaji, bộ thủ, nghĩa (VD: 日, 月, THỰC, mặt trời)...',
      suggestions: ['日', '月', '水', '火', '木', '金', '土', '学'],
      badge: 'Chữ Hán (Kanji)',
    },
    grammar: {
      placeholder: 'Tra mẫu ngữ pháp JLPT (VD: 〜てもいい, 〜てはいけない, koto ga aru)...',
      suggestions: ['〜てもいい', '〜てはいけない', '〜たことがある', '〜ほうがいい'],
      badge: 'Ngữ pháp JLPT',
    },
    kana: {
      placeholder: '',
      suggestions: [],
      badge: 'Bảng chữ cái tương tác âm thanh',
    },
  }[activeTab]

  // Xử lý khi chọn một mục từ gợi ý Dropdown
  const handleSelectSuggestion = (type: TabType, query: string, targetId?: number, char?: string) => {
    setIsDropdownOpen(false)
    setKeyword(query)
    setActiveTab(type)
    if (type === 'vocab' && targetId) setSelectedVocabId(targetId)
    if (type === 'kanji' && char) setSelectedKanjiChar(char)
    if (type === 'grammar' && targetId) setSelectedGrammarId(targetId)
  }

  // Xử lý chọn mục từ lịch sử tra cứu
  const handleSelectHistory = (item: SearchHistoryItem) => {
    setActiveTab(item.tab)
    setKeyword(item.keyword)
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-16">
      {/* =========================================================================
          1. HEADER TRA CỨU MAZII: Ô TÌM KIẾM TẬP TRUNG + DROPDOWN GỢI Ý TỨC THÌ
         ========================================================================= */}
      <div className="bg-gradient-to-br from-indigo-700 via-indigo-800 to-slate-900 rounded-3xl p-6 md:p-8 text-white shadow-xl relative">
        <div className="max-w-3xl mx-auto space-y-4 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 text-indigo-200 text-xs font-semibold backdrop-blur-md">
            <Sparkles className="w-4 h-4 text-amber-300" />
            Tra từ điển thông minh chuẩn Mazii: <span className="text-white font-bold">{tabConfig.badge}</span>
          </div>

          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
            Tra cứu Từ điển Tiếng Nhật
          </h1>

          {/* Ô tìm kiếm lớn & Dropdown Autocomplete */}
          {activeTab !== 'kana' && (
            <div ref={searchContainerRef} className="relative mt-4">
              <div className="relative flex items-center bg-white rounded-2xl shadow-lg overflow-hidden border-2 border-white/20 focus-within:border-indigo-400 focus-within:ring-4 focus-within:ring-indigo-300/30 transition">
                <Search className="w-5 h-5 text-gray-400 ml-4 shrink-0" />
                <input
                  type="text"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  onFocus={() => {
                    if (suggestions && keyword.trim().length > 0) setIsDropdownOpen(true)
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      setIsDropdownOpen(false)
                    }
                  }}
                  placeholder={tabConfig.placeholder}
                  className="w-full py-3.5 px-3 text-gray-900 text-base outline-hidden placeholder:text-gray-400 font-medium"
                />
                {keyword && (
                  <button
                    type="button"
                    onClick={() => {
                      setKeyword('')
                      setIsDropdownOpen(false)
                    }}
                    className="p-2 mr-1 text-gray-400 hover:text-gray-600 rounded-xl hover:bg-gray-100 transition"
                    title="Xoá từ khoá"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setIsDropdownOpen(false)}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold px-5 py-3.5 mr-1 rounded-xl text-sm transition shrink-0"
                >
                  Tìm kiếm
                </button>
              </div>

              {/* MAZII LIVE SUGGESTIONS DROPDOWN (Gợi ý tự động ngay khi gõ) */}
              {isDropdownOpen && suggestions && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-2xl shadow-2xl border border-gray-200 text-left overflow-hidden z-50 max-h-[460px] overflow-y-auto animate-in fade-in slide-in-from-top-2 duration-150">
                  {/* Nhóm Từ vựng */}
                  {suggestions.vocabularies && suggestions.vocabularies.length > 0 && (
                    <div className="p-2 border-b border-gray-100">
                      <div className="px-3 py-1.5 text-[11px] font-bold text-indigo-600 uppercase tracking-wider flex items-center gap-1.5">
                        <span>📖 Từ vựng ({suggestions.vocabularies.length})</span>
                      </div>
                      <div className="space-y-1">
                        {suggestions.vocabularies.map((v) => (
                          <div
                            key={v.id}
                            onClick={() => handleSelectSuggestion('vocab', v.word, v.id)}
                            className="px-3 py-2 rounded-xl hover:bg-indigo-50/80 cursor-pointer flex items-center justify-between gap-2 transition"
                          >
                            <div className="flex items-baseline gap-2">
                              <span className="font-extrabold text-gray-900 text-base">{v.word}</span>
                              <span className="text-xs text-gray-500">【{v.hiragana}】</span>
                              {v.hanViet && (
                                <span className="text-[10px] font-bold text-amber-700 uppercase bg-amber-50 px-1.5 py-0.5 rounded">
                                  {v.hanViet}
                                </span>
                              )}
                            </div>
                            <span className="text-xs text-gray-600 line-clamp-1 max-w-[200px]">
                              {v.meaningVi}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Nhóm Chữ Hán */}
                  {suggestions.kanjis && suggestions.kanjis.length > 0 && (
                    <div className="p-2 border-b border-gray-100">
                      <div className="px-3 py-1.5 text-[11px] font-bold text-amber-600 uppercase tracking-wider flex items-center gap-1.5">
                        <span>🈸 Chữ Hán ({suggestions.kanjis.length})</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-1">
                        {suggestions.kanjis.map((k) => (
                          <div
                            key={k.id}
                            onClick={() => handleSelectSuggestion('kanji', k.character, undefined, k.character)}
                            className="px-3 py-2 rounded-xl hover:bg-amber-50/80 cursor-pointer flex items-center gap-3 transition"
                          >
                            <div className="w-9 h-9 rounded-lg bg-amber-100 text-amber-900 font-serif font-bold text-lg flex items-center justify-center shrink-0">
                              {k.character}
                            </div>
                            <div>
                              <div className="flex items-center gap-1.5">
                                <span className="font-bold text-xs text-amber-800 uppercase">
                                  {k.hanViet || 'Hán tự'}
                                </span>
                                <span className="text-[10px] text-gray-400">{k.strokeCount} nét</span>
                              </div>
                              <p className="text-xs text-gray-600 line-clamp-1">{k.meaningVi}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Nhóm Ngữ pháp */}
                  {suggestions.grammars && suggestions.grammars.length > 0 && (
                    <div className="p-2">
                      <div className="px-3 py-1.5 text-[11px] font-bold text-purple-600 uppercase tracking-wider flex items-center gap-1.5">
                        <span>📚 Ngữ pháp ({suggestions.grammars.length})</span>
                      </div>
                      <div className="space-y-1">
                        {suggestions.grammars.map((g) => (
                          <div
                            key={g.id}
                            onClick={() => handleSelectSuggestion('grammar', g.title, g.id)}
                            className="px-3 py-2 rounded-xl hover:bg-purple-50/80 cursor-pointer flex items-center justify-between gap-2 transition"
                          >
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-sm text-gray-900">{g.title}</span>
                              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-purple-100 text-purple-700">
                                {g.jlptLevel}
                              </span>
                            </div>
                            <span className="text-xs text-gray-600 line-clamp-1 max-w-[200px]">
                              {g.meaningVi}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Không tìm thấy mục nào */}
                  {(!suggestions.vocabularies?.length &&
                    !suggestions.kanjis?.length &&
                    !suggestions.grammars?.length) && (
                    <div className="p-6 text-center text-xs text-gray-500">
                      Không tìm thấy gợi ý nào cho từ khóa &quot;{keyword}&quot;. Bấm Enter để tìm kiếm đầy đủ.
                    </div>
                  )}
                </div>
              )}

              {/* Gợi ý từ khóa mẫu nhanh */}
              {tabConfig.suggestions.length > 0 && (
                <div className="flex items-center justify-center gap-1.5 flex-wrap pt-2.5 text-xs text-indigo-200">
                  <span className="font-medium">Gợi ý tra nhanh:</span>
                  {tabConfig.suggestions.map((tag) => (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => setKeyword(tag)}
                      className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 hover:text-white transition backdrop-blur-xs font-semibold"
                    >
                      {tag}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* =========================================================================
          2. THANH CHUYỂN TAB & BỘ LỌC CẤP ĐỘ JLPT
         ========================================================================= */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-3 rounded-2xl border border-gray-200 shadow-xs sticky top-16 z-30 backdrop-blur-md">
        {/* 4 Tab điều hướng chuyên biệt */}
        <div className="flex items-center gap-1 overflow-x-auto w-full sm:w-auto p-1 bg-gray-100 rounded-xl">
          <button
            type="button"
            onClick={() => setActiveTab('vocab')}
            className={`px-4 py-2 rounded-lg text-xs md:text-sm font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'vocab'
                ? 'bg-white text-indigo-600 shadow-xs'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            📖 Từ vựng
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('kanji')}
            className={`px-4 py-2 rounded-lg text-xs md:text-sm font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'kanji'
                ? 'bg-white text-indigo-600 shadow-xs'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            🈸 Chữ Hán
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('grammar')}
            className={`px-4 py-2 rounded-lg text-xs md:text-sm font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'grammar'
                ? 'bg-white text-indigo-600 shadow-xs'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            📚 Ngữ pháp
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('kana')}
            className={`px-4 py-2 rounded-lg text-xs md:text-sm font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
              activeTab === 'kana'
                ? 'bg-white text-indigo-600 shadow-xs'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            🌸 Bảng chữ cái
          </button>
        </div>

        {/* Bộ lọc JLPT */}
        {activeTab !== 'kana' && (
          <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto">
            <span className="text-xs text-gray-400 font-semibold flex items-center gap-1 pl-2">
              <Filter className="w-3.5 h-3.5" /> JLPT:
            </span>
            <button
              type="button"
              onClick={() => setSelectedLevel(undefined)}
              className={`px-2.5 py-1 rounded-lg text-xs font-bold transition ${
                selectedLevel === undefined
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              All
            </button>
            {(['N5', 'N4', 'N3', 'N2', 'N1'] as JlptLevel[]).map((lvl) => (
              <button
                key={lvl}
                type="button"
                onClick={() => setSelectedLevel(selectedLevel === lvl ? undefined : lvl)}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition ${
                  selectedLevel === lvl
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {lvl}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* =========================================================================
          3. NỘI DUNG CHÍNH: PHÂN CHIA THEO TRẠNG THÁI (KANA / ĐANG TÌM / CHƯA TÌM)
         ========================================================================= */}

      {/* TRƯỜNG HỢP 1: TAB BẢNG CHỮ CÁI KANA */}
      {activeTab === 'kana' ? (
        <KanaMatrixTab />
      ) : hasSearched ? (
        /* TRƯỜNG HỢP 2: ĐÃ CÓ TỪ KHÓA TÌM KIẾM -> BỐ CỤC 2 CỘT CHUẨN MAZII */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* 👈 CỘT TRÁI (DANH SÁCH KẾT QUẢ KHỚP TỪ KHÓA - 4 CỘT) */}
          <div className="lg:col-span-5 xl:col-span-4 bg-white rounded-3xl border border-gray-200/80 shadow-xs p-4 space-y-3">
            {/* Header danh sách */}
            <div className="flex items-center justify-between px-2 pb-2 border-b border-gray-100 text-xs font-bold text-gray-500 uppercase tracking-wider">
              <span>Kết quả tìm kiếm</span>
              <span className="text-indigo-600">
                {activeTab === 'vocab' && `${vocabPage?.totalElements ?? 0} từ`}
                {activeTab === 'kanji' && `${kanjiPage?.totalElements ?? 0} chữ`}
                {activeTab === 'grammar' && `${grammarPage?.totalElements ?? 0} mẫu`}
              </span>
            </div>

            {/* DANH SÁCH TỪ VỰNG */}
            {activeTab === 'vocab' && (
              <div className="space-y-2 max-h-[70vh] overflow-y-auto pr-1">
                {isLoadingVocab ? (
                  <div className="py-12 text-center text-sm text-gray-400 animate-pulse">
                    Đang tìm kiếm từ vựng...
                  </div>
                ) : (vocabPage?.items?.length ?? 0) > 0 ? (
                  (vocabPage?.items ?? []).map((v) => {
                    const isSelected = selectedVocabId === v.id
                    return (
                      <div
                        key={v.id}
                        onClick={() => setSelectedVocabId(v.id)}
                        className={`p-3.5 rounded-2xl border transition cursor-pointer select-none ${
                          isSelected
                            ? 'bg-indigo-50/90 border-indigo-500 shadow-xs ring-2 ring-indigo-200'
                            : 'bg-white border-gray-200/80 hover:border-indigo-300 hover:bg-slate-50/60'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2 mb-1">
                          <div className="flex items-baseline gap-2">
                            <span className="font-extrabold text-gray-900 text-lg font-sans">
                              {v.word}
                            </span>
                            <span className="text-xs text-gray-500">【{v.hiragana}】</span>
                          </div>
                          {v.jlptLevel && (
                            <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-md bg-indigo-100 text-indigo-700">
                              {v.jlptLevel}
                            </span>
                          )}
                        </div>

                        <div className="flex items-center justify-between gap-2">
                          <p className="text-xs text-gray-700 line-clamp-1 font-medium">
                            {v.meaningVi}
                          </p>
                          {v.hanViet && (
                            <span className="text-[10px] font-bold text-amber-600 uppercase shrink-0">
                              {v.hanViet}
                            </span>
                          )}
                        </div>
                      </div>
                    )
                  })
                ) : (
                  <div className="py-12 text-center text-xs text-gray-400 space-y-2">
                    <p>Không tìm thấy từ vựng nào khớp với &quot;{debouncedKeyword}&quot;.</p>
                    <p className="text-[11px] text-gray-400">Hãy thử gõ bằng Romaji, Hiragana hoặc Tiếng Việt.</p>
                  </div>
                )}
              </div>
            )}

            {/* DANH SÁCH CHỮ HÁN */}
            {activeTab === 'kanji' && (
              <div className="space-y-2 max-h-[70vh] overflow-y-auto pr-1">
                {isLoadingKanji ? (
                  <div className="py-12 text-center text-sm text-gray-400 animate-pulse">
                    Đang tìm kiếm chữ Hán...
                  </div>
                ) : (kanjiPage?.items?.length ?? 0) > 0 ? (
                  (kanjiPage?.items ?? []).map((k) => {
                    const isSelected = selectedKanjiChar === k.character
                    return (
                      <div
                        key={k.id}
                        onClick={() => setSelectedKanjiChar(k.character)}
                        className={`p-3 rounded-2xl border transition cursor-pointer flex items-center justify-between gap-3 ${
                          isSelected
                            ? 'bg-indigo-50/90 border-indigo-500 shadow-xs ring-2 ring-indigo-200'
                            : 'bg-white border-gray-200/80 hover:border-indigo-300 hover:bg-slate-50/60'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-xl bg-amber-100/60 border border-amber-200 flex items-center justify-center text-2xl font-bold text-gray-900 font-serif shrink-0">
                            {k.character}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-extrabold text-amber-700 text-xs uppercase">
                                {k.hanViet || 'Hán tự'}
                              </span>
                              {k.jlptLevel && (
                                <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-md bg-gray-100 text-gray-600">
                                  {k.jlptLevel}
                                </span>
                              )}
                            </div>
                            <p className="text-xs text-gray-600 line-clamp-1 mt-0.5 font-medium">
                              {k.meaningVi}
                            </p>
                            <span className="text-[10px] text-gray-400 block mt-0.5">
                              {k.strokeCount} nét
                            </span>
                          </div>
                        </div>

                        <ChevronRight className="w-4 h-4 text-gray-400 shrink-0" />
                      </div>
                    )
                  })
                ) : (
                  <div className="py-12 text-center text-xs text-gray-400 space-y-2">
                    <p>Không tìm thấy chữ Hán nào khớp với &quot;{debouncedKeyword}&quot;.</p>
                  </div>
                )}
              </div>
            )}

            {/* DANH SÁCH NGỮ PHÁP */}
            {activeTab === 'grammar' && (
              <div className="space-y-2 max-h-[70vh] overflow-y-auto pr-1">
                {isLoadingGrammar ? (
                  <div className="py-12 text-center text-sm text-gray-400 animate-pulse">
                    Đang tìm kiếm ngữ pháp...
                  </div>
                ) : (grammarPage?.items?.length ?? 0) > 0 ? (
                  (grammarPage?.items ?? []).map((g) => {
                    const isSelected = selectedGrammarId === g.id
                    return (
                      <div
                        key={g.id}
                        onClick={() => setSelectedGrammarId(g.id)}
                        className={`p-3.5 rounded-2xl border transition cursor-pointer ${
                          isSelected
                            ? 'bg-purple-50/90 border-purple-500 shadow-xs ring-2 ring-purple-200'
                            : 'bg-white border-gray-200/80 hover:border-purple-300 hover:bg-slate-50/60'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2 mb-1">
                          <span className="font-extrabold text-gray-900 text-base font-sans line-clamp-1">
                            {g.title}
                          </span>
                          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-700 shrink-0">
                            {g.jlptLevel}
                          </span>
                        </div>
                        <p className="text-xs text-gray-600 line-clamp-1">{g.meaningVi}</p>
                      </div>
                    )
                  })
                ) : (
                  <div className="py-12 text-center text-xs text-gray-400 space-y-2">
                    <p>Không tìm thấy điểm ngữ pháp nào khớp với &quot;{debouncedKeyword}&quot;.</p>
                  </div>
                )}
              </div>
            )}

            {/* Phân trang cột trái */}
            <div className="pt-2 border-t border-gray-100 flex items-center justify-between text-xs">
              {activeTab === 'vocab' && vocabPage && vocabPage.totalPages > 1 && (
                <>
                  <button
                    type="button"
                    disabled={vocabPageIndex === 0}
                    onClick={() => setVocabPageIndex((p) => p - 1)}
                    className="px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
                  >
                    Trước
                  </button>
                  <span className="text-gray-400">
                    Trang {vocabPageIndex + 1} / {vocabPage.totalPages}
                  </span>
                  <button
                    type="button"
                    disabled={vocabPageIndex >= vocabPage.totalPages - 1}
                    onClick={() => setVocabPageIndex((p) => p + 1)}
                    className="px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
                  >
                    Sau
                  </button>
                </>
              )}

              {activeTab === 'kanji' && kanjiPage && kanjiPage.totalPages > 1 && (
                <>
                  <button
                    type="button"
                    disabled={kanjiPageIndex === 0}
                    onClick={() => setKanjiPageIndex((p) => p - 1)}
                    className="px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
                  >
                    Trước
                  </button>
                  <span className="text-gray-400">
                    Trang {kanjiPageIndex + 1} / {kanjiPage.totalPages}
                  </span>
                  <button
                    type="button"
                    disabled={kanjiPageIndex >= kanjiPage.totalPages - 1}
                    onClick={() => setKanjiPageIndex((p) => p + 1)}
                    className="px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
                  >
                    Sau
                  </button>
                </>
              )}

              {activeTab === 'grammar' && grammarPage && grammarPage.totalPages > 1 && (
                <>
                  <button
                    type="button"
                    disabled={grammarPageIndex === 0}
                    onClick={() => setGrammarPageIndex((p) => p - 1)}
                    className="px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
                  >
                    Trước
                  </button>
                  <span className="text-gray-400">
                    Trang {grammarPageIndex + 1} / {grammarPage.totalPages}
                  </span>
                  <button
                    type="button"
                    disabled={grammarPageIndex >= grammarPage.totalPages - 1}
                    onClick={() => setGrammarPageIndex((p) => p + 1)}
                    className="px-2.5 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
                  >
                    Sau
                  </button>
                </>
              )}
            </div>
          </div>

          {/* 👉 CỘT PHẢI (CHI TIẾT CHUYÊN SÂU TOÀN DIỆN KIỂU MAZII - 8 CỘT) */}
          <div className="lg:col-span-7 xl:col-span-8">
            {/* 1. Chi tiết từ vựng */}
            {activeTab === 'vocab' && (
              selectedVocabId ? (
                <VocabDetailPanel
                  vocabId={selectedVocabId}
                  onSelectKanji={(char) => {
                    setActiveTab('kanji')
                    setKeyword(char)
                  }}
                />
              ) : (
                <div className="bg-white rounded-3xl p-16 border border-gray-200 text-center text-gray-400 space-y-3">
                  <BookOpen className="w-12 h-12 mx-auto text-gray-300" />
                  <p className="font-medium text-base">Chọn một từ vựng bên trái để xem giải nghĩa chi tiết.</p>
                </div>
              )
            )}

            {/* 2. Chi tiết chữ Hán */}
            {activeTab === 'kanji' && (
              selectedKanjiChar ? (
                <KanjiDetailPanel
                  character={selectedKanjiChar}
                  onSelectVocab={(vId) => {
                    setActiveTab('vocab')
                    setSelectedVocabId(vId)
                  }}
                />
              ) : (
                <div className="bg-white rounded-3xl p-16 border border-gray-200 text-center text-gray-400 space-y-3">
                  <Sparkles className="w-12 h-12 mx-auto text-gray-300" />
                  <p className="font-medium text-base">Chọn một chữ Hán bên trái để xem nét vẽ và từ ghép.</p>
                </div>
              )
            )}

            {/* 3. Chi tiết ngữ pháp */}
            {activeTab === 'grammar' && (
              selectedGrammarId ? (
                <GrammarDetailPanel grammarId={selectedGrammarId} />
              ) : (
                <div className="bg-white rounded-3xl p-16 border border-gray-200 text-center text-gray-400 space-y-3">
                  <BookOpen className="w-12 h-12 mx-auto text-gray-300" />
                  <p className="font-medium text-base">Chọn một mẫu ngữ pháp bên trái để xem cấu trúc và ví dụ.</p>
                </div>
              )
            )}
          </div>
        </div>
      ) : (
        /* =========================================================================
            TRƯỜNG HỢP 3: MÀN HÌNH CHÍNH MAZII KHI CHƯA TRA CỨU (DISCOVERY HUB)
           ========================================================================= */
        <div className="space-y-6">
          {/* 1. Lịch sử tra cứu gần đây (Recent Searches) */}
          {history.length > 0 && (
            <div className="bg-white rounded-3xl p-6 border border-gray-200/80 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm font-bold text-gray-800">
                  <Clock className="w-4 h-4 text-indigo-600" />
                  <span>Lịch sử tra cứu gần đây</span>
                  <span className="text-xs text-gray-400 font-normal">({history.length})</span>
                </div>
                <button
                  type="button"
                  onClick={clearAllHistory}
                  className="text-xs text-gray-400 hover:text-rose-500 font-semibold flex items-center gap-1 transition"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  Xoá lịch sử
                </button>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2.5">
                {history.map((item) => (
                  <div
                    key={item.id}
                    onClick={() => handleSelectHistory(item)}
                    className="p-3 rounded-2xl border border-gray-200/80 hover:border-indigo-400 hover:bg-indigo-50/50 cursor-pointer group transition relative flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-1">
                        <span className="font-extrabold text-sm text-gray-900 group-hover:text-indigo-600 transition truncate">
                          {item.title}
                        </span>
                        <button
                          type="button"
                          onClick={(e) => removeHistoryItem(item.id, e)}
                          className="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-rose-500 p-0.5 rounded transition"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                      {item.subtitle && (
                        <p className="text-[11px] text-gray-500 truncate">【{item.subtitle}】</p>
                      )}
                      {item.meaning && (
                        <p className="text-xs text-gray-600 truncate mt-1">{item.meaning}</p>
                      )}
                    </div>
                    <div className="pt-2 flex items-center justify-between text-[10px] text-gray-400">
                      <span className="uppercase font-semibold text-indigo-600">
                        {item.tab === 'vocab' ? 'Từ vựng' : item.tab === 'kanji' ? 'Hán tự' : 'Ngữ pháp'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 2. Khám phá nổi bật & Lối tắt nhanh */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Thẻ 1: Khám phá Chữ Hán tiêu biểu */}
            <div className="bg-gradient-to-br from-amber-50 to-orange-50 rounded-3xl p-6 border border-amber-200/60 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-amber-700 flex items-center gap-1.5">
                  <Flame className="w-4 h-4 text-amber-600" /> Chữ Hán hôm nay
                </span>
                <span className="text-xs font-bold px-2 py-0.5 rounded bg-amber-200 text-amber-900">
                  JLPT N5
                </span>
              </div>
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-white shadow-xs border border-amber-200 flex items-center justify-center text-4xl font-serif font-bold text-gray-900">
                  日
                </div>
                <div>
                  <h4 className="font-extrabold text-amber-900 text-lg uppercase">NHẬT</h4>
                  <p className="text-xs text-gray-600">Mặt trời, ngày, Nhật Bản</p>
                  <p className="text-[11px] text-gray-400 mt-1">4 nét • On: ニチ, ジツ • Kun: ひ, -び</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  setActiveTab('kanji')
                  setKeyword('日')
                }}
                className="w-full py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs transition flex items-center justify-center gap-1.5 shadow-xs"
              >
                <span>Xem thứ tự nét vẽ & Từ ghép</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Thẻ 2: Khám phá Từ vựng thông dụng */}
            <div className="bg-gradient-to-br from-indigo-50 to-blue-50 rounded-3xl p-6 border border-indigo-200/60 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-700 flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-indigo-600" /> Từ vựng thông dụng
                </span>
                <span className="text-xs font-bold px-2 py-0.5 rounded bg-indigo-200 text-indigo-900">
                  JLPT N5
                </span>
              </div>
              <div className="space-y-1">
                <div className="flex items-baseline gap-2">
                  <h4 className="font-extrabold text-indigo-950 text-2xl font-sans">食べる</h4>
                  <span className="text-sm text-gray-500">【たべる】</span>
                </div>
                <p className="text-xs text-amber-700 font-bold uppercase">THỰC</p>
                <p className="text-xs text-gray-600 font-medium">Ăn (động từ nhóm 2)</p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setActiveTab('vocab')
                  setKeyword('食べる')
                }}
                className="w-full py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs transition flex items-center justify-center gap-1.5 shadow-xs"
              >
                <span>Xem chi tiết & Ví dụ câu</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Thẻ 3: Khám phá Bảng chữ cái tương tác */}
            <div className="bg-gradient-to-br from-pink-50 to-rose-50 rounded-3xl p-6 border border-pink-200/60 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-pink-700 flex items-center gap-1.5">
                  <Award className="w-4 h-4 text-pink-600" /> Bảng chữ cái tương tác
                </span>
                <span className="text-xs font-bold px-2 py-0.5 rounded bg-pink-200 text-pink-900">
                  50 âm
                </span>
              </div>
              <div className="space-y-1">
                <h4 className="font-extrabold text-pink-950 text-xl">Hiragana & Katakana</h4>
                <p className="text-xs text-gray-600">
                  Phát âm chuẩn từng chữ, nút đọc tuần tự cả hàng và tập viết nét SVG trực quan.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActiveTab('kana')}
                className="w-full py-2.5 rounded-xl bg-pink-600 hover:bg-pink-700 text-white font-bold text-xs transition flex items-center justify-center gap-1.5 shadow-xs"
              >
                <span>Mở bảng chữ cái</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* 3. Lối tắt tìm kiếm theo cấp độ JLPT */}
          <div className="bg-white rounded-3xl p-6 border border-gray-200/80 shadow-xs space-y-3">
            <h3 className="text-sm font-bold text-gray-800 flex items-center gap-2">
              <Filter className="w-4 h-4 text-indigo-600" />
              Khám phá từ vựng theo Cấp độ JLPT
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              {(
                [
                  { level: 'N5', label: 'Căn bản (Sơ cấp 1)', color: 'border-emerald-200 bg-emerald-50 text-emerald-800' },
                  { level: 'N4', label: 'Sơ cấp 2', color: 'border-blue-200 bg-blue-50 text-blue-800' },
                  { level: 'N3', label: 'Trung cấp', color: 'border-indigo-200 bg-indigo-50 text-indigo-800' },
                  { level: 'N2', label: 'Trung cao cấp', color: 'border-purple-200 bg-purple-50 text-purple-800' },
                  { level: 'N1', label: 'Cao cấp (Thành thạo)', color: 'border-rose-200 bg-rose-50 text-rose-800' },
                ] as const
              ).map((item) => (
                <div
                  key={item.level}
                  onClick={() => {
                    setActiveTab('vocab')
                    setSelectedLevel(item.level as JlptLevel)
                  }}
                  className={`p-4 rounded-2xl border transition cursor-pointer hover:shadow-md ${item.color} flex flex-col justify-between`}
                >
                  <span className="font-extrabold text-xl">{item.level}</span>
                  <span className="text-xs font-medium mt-1 opacity-90">{item.label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
