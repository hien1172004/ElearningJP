import React, { useState, useEffect, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
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
  Languages,
  PenTool,
  Camera,
  Brush,
  BookText,
  PencilLine,
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
import { CommentSection } from '@/components/comment'
import { StudyTimerTracker } from '@/components/common'
import { useRecordLookup } from '@/hooks/useRecordLookup'
import { parseCommentFocus, parseDictionaryTargetId } from '@/utils/commentFocus'
import { JLPT_LEVEL_BADGE, JLPT_LEVEL_TEXT } from '@/utils/jlpt'
import {
  VocabDetailPanel,
  KanjiDetailPanel,
  GrammarDetailPanel,
  KanaMatrixTab,
  KanjiHandwritingModal,
  KanjiAirWritingModal,
  KanjiPracticeModal,
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

/** Query param mà các trang khác dùng để mở sẵn từ khóa (VD: từ đã tra ở trang Tiến độ). */
const QP_KEYWORD = 'q'
const QP_TAB = 'tab'

const TAB_FROM_PARAM: Record<string, TabType> = {
  vocab: 'vocab',
  VOCAB: 'vocab',
  kanji: 'kanji',
  KANJI: 'kanji',
  grammar: 'grammar',
  GRAMMAR: 'grammar',
}

/**
 * Mục cần chọn sẵn khi mở từ URL (`?tab&q&id=` — link từ thông báo bình luận).
 * Từ vựng / ngữ pháp chọn theo id; chữ Hán chọn theo ký tự (`q`) vì panel chi tiết đi theo ký tự.
 */
interface PendingSelection {
  tab: TabType
  keyword: string
  id?: number
  char?: string
}

function readUrlSelection(params: URLSearchParams): PendingSelection | null {
  const id = parseDictionaryTargetId(params)
  const tab = TAB_FROM_PARAM[params.get(QP_TAB) ?? '']
  const keyword = (params.get(QP_KEYWORD) ?? '').trim()
  if (!id || !tab || !keyword) return null
  return tab === 'kanji' ? { tab, keyword, char: keyword } : { tab, keyword, id }
}

/**
 * Lấy (và xoá) mục chọn sẵn khi kết quả tìm của `tab` về. Chỉ dùng khi từ khóa vẫn là từ khóa
 * trong URL — user đã gõ từ khác thì bỏ, để không chọn nhầm ở lần tìm sau.
 */
function takePendingSelection(
  ref: React.MutableRefObject<PendingSelection | null>,
  tab: TabType,
  keyword: string,
): PendingSelection | null {
  const pending = ref.current
  if (!pending || pending.tab !== tab) return null
  ref.current = null
  return pending.keyword === keyword ? pending : null
}

export const DictionaryPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  // Từ khóa mở sẵn từ URL (được set một lần khi vào trang).
  const initialKeyword = searchParams.get(QP_KEYWORD) ?? ''
  const initialTab = TAB_FROM_PARAM[searchParams.get(QP_TAB) ?? ''] ?? 'vocab'

  // Mở thẳng tới bình luận từ thông báo (xem utils/commentFocus).
  const commentFocus = parseCommentFocus(searchParams)
  const focusTab = TAB_FROM_PARAM[searchParams.get(QP_TAB) ?? '']
  const focusTargetId = parseDictionaryTargetId(searchParams)
  /** Chỉ truyền focus cho khối bình luận đúng tab + đúng mục mà link trỏ tới. */
  const focusFor = (tab: TabType, targetId: number | null) =>
    commentFocus && focusTab === tab && focusTargetId === targetId ? commentFocus : null
  const pendingSelectRef = useRef<PendingSelection | null>(readUrlSelection(searchParams))

  const [keyword, setKeyword] = useState<string>(initialKeyword)
  const [debouncedKeyword, setDebouncedKeyword] = useState<string>('')
  const [activeTab, setActiveTab] = useState<TabType>(initialTab)
  const [selectedLevel, setSelectedLevel] = useState<JlptLevel | undefined>(undefined)
  const [isHandwritingOpen, setIsHandwritingOpen] = useState<boolean>(false)
  const [isPracticeOpen, setIsPracticeOpen] = useState<boolean>(false)
  const [isAirWritingOpen, setIsAirWritingOpen] = useState<boolean>(false)

  // Autocomplete Suggestions Dropdown
  const [suggestions, setSuggestions] = useState<DictionaryUnifiedSearch | null>(null)
  const [isDropdownOpen, setIsDropdownOpen] = useState<boolean>(false)
  const searchContainerRef = useRef<HTMLDivElement>(null)
  /** true khi từ khóa đến từ URL → không tự mở dropdown gợi ý. */
  const suppressDropdownRef = useRef(true)

  // Lịch sử tra cứu gần đây từ localStorage
  const [history, setHistory] = useState<SearchHistoryItem[]>([])

  // Mục đang được chọn để hiển thị chi tiết ở Cột Phải (chuẩn Mazii)
  const [selectedVocabId, setSelectedVocabId] = useState<number | null>(null)
  const [selectedKanjiChar, setSelectedKanjiChar] = useState<string | null>(null)
  /**
   * Id của chữ Hán (writing_characters.id).
   * BE dùng id này làm targetId cho bình luận KANJI, nên cần resolve
   * riêng từ KanjiDetailPanel (state hiện tại chỉ giữ ký tự).
   */
  const [selectedKanjiId, setSelectedKanjiId] = useState<number | null>(null)
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

  // Đồng bộ lại khi URL có từ khóa mới (điều hướng từ trang khác sang).
  useEffect(() => {
    const q = searchParams.get(QP_KEYWORD)
    if (q) {
      // Đến từ URL (VD: bấm 1 từ ở trang Tiến độ) → không tự mở dropdown.
      suppressDropdownRef.current = true
      setIsDropdownOpen(false)
      if (q !== keyword) setKeyword(q)
    }
    const t = TAB_FROM_PARAM[searchParams.get(QP_TAB) ?? '']
    if (t) setActiveTab(t)
    // Link có `id` → chọn luôn mục đó (kể cả khi từ khóa không đổi nên không tìm lại).
    const pending = readUrlSelection(searchParams)
    pendingSelectRef.current = pending
    if (pending?.tab === 'vocab' && pending.id) setSelectedVocabId(pending.id)
    if (pending?.tab === 'grammar' && pending.id) setSelectedGrammarId(pending.id)
    if (pending?.tab === 'kanji' && pending.char) setSelectedKanjiChar(pending.char)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

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
              // Dropdown gợi ý chỉ mở khi user TỰ gõ tay trong ô tìm kiếm.
              // Khi mở sẵn từ URL (mọi từ khác điều hướng tới) thì để kết quả hiện thẳng.
              if (!suppressDropdownRef.current) {
                setIsDropdownOpen(true)
              }
            }
          })
          .catch(() => { })
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
          const pending = takePendingSelection(pendingSelectRef, 'vocab', debouncedKeyword)
          if (items.length > 0) {
            setSelectedVocabId(pending?.id ?? items[0].id)
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
            setSelectedVocabId(pending?.id ?? null)
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
          const pending = takePendingSelection(pendingSelectRef, 'kanji', debouncedKeyword)
          if (items.length > 0) {
            setSelectedKanjiChar(pending?.char ?? items[0].character)
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
            setSelectedKanjiChar(pending?.char ?? null)
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
          const pending = takePendingSelection(pendingSelectRef, 'grammar', debouncedKeyword)
          if (items.length > 0) {
            setSelectedGrammarId(pending?.id ?? items[0].id)
            if (debouncedKeyword) {
              saveToHistory({
                tab: 'grammar',
                keyword: debouncedKeyword,
                title: items[0].title,
                meaning: items[0].meaningVi,
              })
            }
          } else {
            setSelectedGrammarId(pending?.id ?? null)
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

  // Mỗi lần đổi chữ Hán → reset id đã resolve (id mới sẽ được set lại
  // bởi callback onIdResolved của KanjiDetailPanel).
  useEffect(() => {
    setSelectedKanjiId(null)
  }, [selectedKanjiChar])

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

  // =========================================================================
  // Ghi nhận hoạt động học tập (Bước 2 & 3)
  // =========================================================================

  // Đếm thời gian học khi user đang ở trang tra từ điển.
  // <StudyTimerTracker /> render null — chỉ bật hook.
  const recordLookup = useRecordLookup()

  // Mỗi lần mở chi tiết 1 từ vựng / chữ Hán / ngữ pháp → ghi 1 LƯỢT TRA.
  // BE tự phân biệt "từ MỚI" (lần đầu) với "từ cũ" (đã học rồi) — mỗi lần tra
  // đều được ghi, không dedupe ở client.
  useEffect(() => {
    if (selectedVocabId) {
      recordLookup({ targetType: 'VOCAB', targetId: selectedVocabId })
    }
  }, [selectedVocabId, recordLookup])

  useEffect(() => {
    if (selectedKanjiChar) {
      recordLookup({ targetType: 'KANJI', sourceRef: selectedKanjiChar })
    }
  }, [selectedKanjiChar, recordLookup])

  useEffect(() => {
    if (selectedGrammarId) {
      recordLookup({ targetType: 'GRAMMAR', targetId: selectedGrammarId })
    }
  }, [selectedGrammarId, recordLookup])

  return (
    <div className="space-y-6 pb-16">
      {/* Đếm thời gian học — component ẩn, không ảnh hưởng layout */}
      <StudyTimerTracker />
      {/* =========================================================================
          1. THANH TRA CỨU: Ô TÌM KIẾM + CÔNG CỤ AI + TAB + LỌC JLPT (một khối gọn, không banner)
         ========================================================================= */}
      {/* Lớp bọc sticky sát TopBar, nền cùng màu trang + pt-2: giữ khoảng hở nhưng không để nội dung cuộn lọt qua */}
      <div className="sticky-below-topbar z-20 bg-kinari -mt-2 pt-2">
      <div className="bg-surface rounded-xl border border-suna shadow-xs p-3 sm:p-4 space-y-3">
        {/* Ô tìm kiếm & Dropdown Autocomplete */}
        {activeTab !== 'kana' && (
          <div ref={searchContainerRef} className="relative">
            <div className="flex items-center gap-2">
              <div className="flex-1 min-w-0 flex items-center h-11 bg-surface rounded-lg border border-suna focus-within:border-indigo-600 focus-within:ring-2 focus-within:ring-indigo-600/15 transition">
                <Search className="w-[18px] h-[18px] text-nibi ml-3 shrink-0" />
                <input
                  type="text"
                  value={keyword}
                  onChange={(e) => {
                    // Gõ tay → cho phép dropdown gợi ý hoạt động trở lại.
                    suppressDropdownRef.current = false
                    setKeyword(e.target.value)
                  }}
                  onFocus={() => {
                    if (suggestions && keyword.trim().length > 0) {
                      suppressDropdownRef.current = false
                      setIsDropdownOpen(true)
                    }
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      setIsDropdownOpen(false)
                    }
                  }}
                  placeholder={tabConfig.placeholder}
                  className="flex-1 min-w-0 h-full px-2.5 bg-transparent text-sumi text-[15px] outline-hidden placeholder:text-gray-400"
                />
                {keyword && (
                  <button
                    type="button"
                    onClick={() => {
                      setKeyword('')
                      setIsDropdownOpen(false)
                    }}
                    className="p-1.5 text-gray-400 hover:text-sumi rounded-md hover:bg-gray-100 transition shrink-0"
                    title="Xoá từ khoá"
                    aria-label="Xoá từ khoá"
                  >
                    <X className="w-4 h-4" />
                  </button>
                )}
                <span className="w-px h-5 bg-suna mx-1.5 shrink-0" aria-hidden="true" />
                <div className="flex items-center gap-1 pr-1.5 shrink-0">
                  <button
                    type="button"
                    onClick={() => setIsHandwritingOpen(true)}
                    className="w-8 h-8 shrink-0 rounded-md border border-suna bg-surface text-nibi hover:text-indigo-600 hover:border-indigo-300 hover:bg-kamenozoki transition flex items-center justify-center cursor-pointer"
                    title="Nhận dạng chữ viết tay Kanji (AI)"
                    aria-label="Nhận dạng chữ viết tay Kanji (AI)"
                  >
                    <PenTool className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsAirWritingOpen(true)}
                    className="w-8 h-8 shrink-0 rounded-md border border-suna bg-surface text-nibi hover:text-indigo-600 hover:border-indigo-300 hover:bg-kamenozoki transition flex items-center justify-center cursor-pointer"
                    title="Viết chữ Hán trên không qua Camera & AI"
                    aria-label="Viết chữ Hán trên không qua Camera & AI"
                  >
                    <Camera className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsPracticeOpen(true)}
                    className="w-8 h-8 shrink-0 rounded-md border border-suna bg-surface text-nibi hover:text-indigo-600 hover:border-indigo-300 hover:bg-kamenozoki transition flex items-center justify-center cursor-pointer"
                    title="Luyện viết & Chấm điểm nét vẽ AI"
                    aria-label="Luyện viết & Chấm điểm nét vẽ AI"
                  >
                    <PencilLine className="w-4 h-4" />
                  </button>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsDropdownOpen(false)}
                className="h-11 px-5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold transition shrink-0"
              >
                Tìm kiếm
              </button>
            </div>

            {/* Dropdown gợi ý tự động ngay khi gõ */}
            {isDropdownOpen && suggestions && (
              <div className="absolute top-full left-0 right-0 mt-2 bg-surface rounded-md shadow-2xl border border-gray-200 text-left overflow-hidden z-50 max-h-[460px] overflow-y-auto overscroll-contain animate-in fade-in slide-in-from-top-2 duration-150">
                {/* Nhóm Từ vựng */}
                {suggestions.vocabularies && suggestions.vocabularies.length > 0 && (
                  <div className="p-2 border-b border-gray-100">
                    <div className="px-3 py-1.5 text-[11px] font-bold text-indigo-600 uppercase tracking-wider flex items-center gap-1.5">
                      <BookOpen className="w-3.5 h-3.5" />
                      <span>Từ vựng ({suggestions.vocabularies.length})</span>
                    </div>
                    <div className="space-y-1">
                      {suggestions.vocabularies.map((v) => (
                        <div
                          key={v.id}
                          onClick={() => handleSelectSuggestion('vocab', v.word, v.id)}
                          className="px-3 py-2 rounded-md hover:bg-indigo-50/80 cursor-pointer flex items-center justify-between gap-2 transition"
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
                    <div className="px-3 py-1.5 text-[11px] font-bold text-indigo-600 uppercase tracking-wider flex items-center gap-1.5">
                      <Brush className="w-3.5 h-3.5" />
                      <span>Chữ Hán ({suggestions.kanjis.length})</span>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-1">
                      {suggestions.kanjis.map((k) => (
                        <div
                          key={k.id}
                          onClick={() => handleSelectSuggestion('kanji', k.character, undefined, k.character)}
                          className="px-3 py-2 rounded-md hover:bg-amber-50/80 cursor-pointer flex items-center gap-3 transition"
                        >
                          <div className="w-9 h-9 rounded bg-amber-100 text-amber-900 font-serif font-bold text-lg flex items-center justify-center shrink-0">
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
                    <div className="px-3 py-1.5 text-[11px] font-bold text-indigo-600 uppercase tracking-wider flex items-center gap-1.5">
                      <BookText className="w-3.5 h-3.5" />
                      <span>Ngữ pháp ({suggestions.grammars.length})</span>
                    </div>
                    <div className="space-y-1">
                      {suggestions.grammars.map((g) => (
                        <div
                          key={g.id}
                          onClick={() => handleSelectSuggestion('grammar', g.title, g.id)}
                          className="px-3 py-2 rounded-md hover:bg-purple-50/80 cursor-pointer flex items-center justify-between gap-2 transition"
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

          </div>
        )}

        {/* Tab điều hướng + Bộ lọc JLPT */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-1 overflow-x-auto">
            {(
              [
                { key: 'vocab', label: 'Từ vựng' },
                { key: 'kanji', label: 'Chữ Hán' },
                { key: 'grammar', label: 'Ngữ pháp' },
                { key: 'kana', label: 'Bảng chữ cái' },
              ] as const
            ).map(({ key, label }) => (
              <button
                key={key}
                type="button"
                onClick={() => setActiveTab(key)}
                className={`px-3 py-1.5 rounded-md text-xs transition flex items-center gap-1.5 whitespace-nowrap ${activeTab === key
                  ? 'bg-kamenozoki text-indigo-600 font-semibold'
                  : 'text-nibi font-medium hover:text-sumi hover:bg-gray-50'
                  }`}
              >
                {label}
              </button>
            ))}
          </div>

          {activeTab !== 'kana' && (
            <div className="flex items-center gap-1 overflow-x-auto">
              <span className="text-xs text-nibi font-semibold flex items-center gap-1 pr-1">
                <Filter className="w-3.5 h-3.5" /> JLPT
              </span>
              <button
                type="button"
                onClick={() => setSelectedLevel(undefined)}
                className={`px-2.5 py-1 rounded-md text-xs font-semibold transition whitespace-nowrap ${selectedLevel === undefined
                  ? 'bg-indigo-600 text-white'
                  : 'text-nibi hover:bg-kamenozoki hover:text-indigo-600'
                  }`}
              >
                Tất cả
              </button>
              {(['N5', 'N4', 'N3', 'N2', 'N1'] as JlptLevel[]).map((lvl) => (
                <button
                  key={lvl}
                  type="button"
                  onClick={() => setSelectedLevel(selectedLevel === lvl ? undefined : lvl)}
                  className={`px-2.5 py-1 rounded-md text-xs font-semibold transition ${selectedLevel === lvl
                    ? 'bg-indigo-600 text-white'
                    : 'text-nibi hover:bg-kamenozoki hover:text-indigo-600'
                    }`}
                >
                  {lvl}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
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
          <div className="lg:col-span-5 xl:col-span-4 bg-surface rounded-md border border-gray-200/80 shadow-xs p-4 space-y-3">
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
              <div className="space-y-2 max-h-[70vh] overflow-y-auto overscroll-contain pr-1">
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
                        className={`p-3.5 rounded-md border transition cursor-pointer select-none ${isSelected
                          ? 'bg-indigo-50/90 border-indigo-500 shadow-xs ring-2 ring-indigo-200'
                          : 'bg-surface border-gray-200/80 hover:border-indigo-300 hover:bg-slate-50/60'
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
              <div className="space-y-2 max-h-[70vh] overflow-y-auto overscroll-contain pr-1">
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
                        className={`p-3 rounded-md border transition cursor-pointer flex items-center justify-between gap-3 ${isSelected
                          ? 'bg-indigo-50/90 border-indigo-500 shadow-xs ring-2 ring-indigo-200'
                          : 'bg-surface border-gray-200/80 hover:border-indigo-300 hover:bg-slate-50/60'
                          }`}
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-md bg-amber-100/60 border border-amber-200 flex items-center justify-center text-2xl font-bold text-gray-900 font-serif shrink-0">
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
              <div className="space-y-2 max-h-[70vh] overflow-y-auto overscroll-contain pr-1">
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
                        className={`p-3.5 rounded-md border transition cursor-pointer ${isSelected
                          ? 'bg-purple-50/90 border-purple-500 shadow-xs ring-2 ring-purple-200'
                          : 'bg-surface border-gray-200/80 hover:border-purple-300 hover:bg-slate-50/60'
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
                    className="px-2.5 py-1.5 rounded bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
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
                    className="px-2.5 py-1.5 rounded bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
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
                    className="px-2.5 py-1.5 rounded bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
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
                    className="px-2.5 py-1.5 rounded bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
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
                    className="px-2.5 py-1.5 rounded bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
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
                    className="px-2.5 py-1.5 rounded bg-gray-100 hover:bg-gray-200 font-semibold disabled:opacity-30"
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
                    setIsDropdownOpen(false)
                    setActiveTab('kanji')
                    setKeyword(char)
                    window.scrollTo({ top: 0, behavior: 'smooth' })
                  }}
                />
              ) : (
                <div className="bg-surface rounded-md p-16 border border-gray-200 text-center text-gray-400 space-y-3">
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
                  onSelectVocab={(vId, word) => {
                    setIsDropdownOpen(false)
                    setActiveTab('vocab')
                    setKeyword(word)
                    setSelectedVocabId(vId)
                    window.scrollTo({ top: 0, behavior: 'smooth' })
                  }}
                  onIdResolved={(id) => setSelectedKanjiId(id)}
                />
              ) : (
                <div className="bg-surface rounded-md p-16 border border-gray-200 text-center text-gray-400 space-y-3">
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
                <div className="bg-surface rounded-md p-16 border border-gray-200 text-center text-gray-400 space-y-3">
                  <BookOpen className="w-12 h-12 mx-auto text-gray-300" />
                  <p className="font-medium text-base">Chọn một mẫu ngữ pháp bên trái để xem cấu trúc và ví dụ.</p>
                </div>
              )
            )}

            {/* ===============================================================
                4. BÌNH LUẬN (đặt ngay dưới panel chi tiết)
               =============================================================== */}
            {activeTab === 'vocab' && selectedVocabId && (
              <div className="mt-6">
                <CommentSection
                  targetType="VOCABULARY"
                  targetId={selectedVocabId}
                  focus={focusFor('vocab', selectedVocabId)}
                />
              </div>
            )}

            {activeTab === 'kanji' && selectedKanjiId && (
              <div className="mt-6">
                <CommentSection
                  targetType="KANJI"
                  targetId={selectedKanjiId}
                  focus={focusFor('kanji', selectedKanjiId)}
                />
              </div>
            )}

            {activeTab === 'grammar' && selectedGrammarId && (
              <div className="mt-6">
                <CommentSection
                  targetType="GRAMMAR"
                  targetId={selectedGrammarId}
                  focus={focusFor('grammar', selectedGrammarId)}
                />
              </div>
            )}
          </div>
        </div>
      ) : (
        /* =========================================================================
            TRƯỜNG HỢP 3: MÀN HÌNH CHÍNH KHI CHƯA TRA CỨU (DISCOVERY HUB) — gộp 1 khối 2 cột
           ========================================================================= */
        <div className="bg-surface rounded-xl border border-suna shadow-xs grid grid-cols-1 lg:grid-cols-12">
          {/* ===== CỘT TRÁI: Lịch sử + Gợi ý nhanh + Cấp độ JLPT ===== */}
          <div className="lg:col-span-8 p-4 sm:p-5 space-y-5 min-w-0">
            {/* 1. Lịch sử tra cứu gần đây */}
            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="flex items-center gap-2 text-sm font-bold text-sumi">
                  <Clock className="w-4 h-4 text-indigo-600" />
                  Lịch sử tra cứu gần đây
                  {history.length > 0 && (
                    <span className="text-xs text-gray-400 font-normal">({history.length})</span>
                  )}
                </h3>
                {history.length > 0 && (
                  <button
                    type="button"
                    onClick={clearAllHistory}
                    className="text-xs text-nibi hover:text-shu font-semibold flex items-center gap-1 transition"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    Xoá lịch sử
                  </button>
                )}
              </div>

              {history.length > 0 ? (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 xl:grid-cols-5 gap-2">
                  {history.map((item) => (
                    <div
                      key={item.id}
                      onClick={() => handleSelectHistory(item)}
                      className="group relative min-w-0 p-2.5 rounded-md border border-suna hover:border-indigo-300 hover:bg-kamenozoki/50 cursor-pointer transition"
                    >
                      <div className="flex items-start justify-between gap-1">
                        <span className="font-bold text-sm text-sumi group-hover:text-indigo-600 transition truncate">
                          {item.title}
                        </span>
                        <button
                          type="button"
                          onClick={(e) => removeHistoryItem(item.id, e)}
                          className="opacity-0 group-hover:opacity-100 -mr-1 text-gray-400 hover:text-shu p-0.5 rounded transition shrink-0"
                          aria-label="Xoá khỏi lịch sử"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </div>
                      {item.subtitle && (
                        <p className="text-[11px] text-nibi truncate">【{item.subtitle}】</p>
                      )}
                      {item.meaning && (
                        <p className="text-xs text-gray-600 truncate mt-0.5">{item.meaning}</p>
                      )}
                      <p className="mt-1 text-[10px] uppercase font-semibold tracking-wide text-indigo-600">
                        {item.tab === 'vocab' ? 'Từ vựng' : item.tab === 'kanji' ? 'Hán tự' : 'Ngữ pháp'}
                      </p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-nibi">
                  Chưa có lịch sử. Các từ bạn tra sẽ hiện ở đây để mở lại nhanh.
                </p>
              )}
            </section>

            {/* 2. Gợi ý tra nhanh (chuyển từ banner cũ xuống) */}
            {tabConfig.suggestions.length > 0 && (
              <section className="flex items-center gap-1.5 flex-wrap">
                <span className="text-xs font-semibold text-nibi mr-1">Gợi ý tra nhanh:</span>
                {tabConfig.suggestions.map((tag) => (
                  <button
                    key={tag}
                    type="button"
                    onClick={() => setKeyword(tag)}
                    className="px-2.5 py-1 rounded-md border border-suna bg-kinari text-sm font-medium text-sumi hover:border-indigo-300 hover:text-indigo-600 transition"
                  >
                    {tag}
                  </button>
                ))}
              </section>
            )}

            {/* 3. Khám phá từ vựng theo cấp độ JLPT */}
            <section className="space-y-3 pt-4 border-t border-suna">
              <h3 className="flex items-center gap-2 text-sm font-bold text-sumi">
                <Filter className="w-4 h-4 text-indigo-600" />
                Khám phá từ vựng theo cấp độ JLPT
              </h3>
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                {(
                  [
                    { level: 'N5', label: 'Căn bản (Sơ cấp 1)' },
                    { level: 'N4', label: 'Sơ cấp 2' },
                    { level: 'N3', label: 'Trung cấp' },
                    { level: 'N2', label: 'Trung cao cấp' },
                    { level: 'N1', label: 'Cao cấp (Thành thạo)' },
                  ] as const
                ).map((item) => (
                  <button
                    key={item.level}
                    type="button"
                    onClick={() => {
                      setActiveTab('vocab')
                      setSelectedLevel(item.level as JlptLevel)
                    }}
                    className="text-left p-3 rounded-md border border-suna hover:border-indigo-300 hover:bg-kamenozoki/50 transition cursor-pointer"
                  >
                    <span className={`block font-extrabold text-lg leading-none ${JLPT_LEVEL_TEXT[item.level]}`}>
                      {item.level}
                    </span>
                    <span className="block text-xs text-nibi mt-1.5">{item.label}</span>
                  </button>
                ))}
              </div>
            </section>
          </div>

          {/* ===== CỘT PHẢI: Khám phá nổi bật — 3 mục cùng khung, khác độ đậm màu chàm Ai ===== */}
          <aside className="lg:col-span-4 p-4 sm:p-5 space-y-3 border-t lg:border-t-0 lg:border-l border-suna bg-kinari/50 rounded-b-xl lg:rounded-bl-none lg:rounded-r-xl">
            <h3 className="flex items-center gap-2 text-sm font-bold text-sumi">
              <Sparkles className="w-4 h-4 text-indigo-600" />
              Khám phá nổi bật
            </h3>

            {/* Chữ Hán hôm nay */}
            <button
              type="button"
              onClick={() => {
                setActiveTab('kanji')
                setKeyword('日')
              }}
              className="group w-full text-left flex items-center gap-3 p-3 rounded-md bg-surface border border-suna hover:bg-kamenozoki/40 hover:shadow-xs transition cursor-pointer"
            >
              <span className="w-12 h-12 shrink-0 rounded-md bg-kamenozoki text-indigo-800 font-serif text-2xl font-bold flex items-center justify-center">
                日
              </span>
              <span className="flex-1 min-w-0">
                <span className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-nibi">
                  <Brush className="w-3.5 h-3.5" /> Chữ Hán hôm nay
                  <span className={`ml-auto text-[10px] px-1.5 py-0.5 rounded ${JLPT_LEVEL_BADGE.N5}`}>N5</span>
                </span>
                <span className="block mt-0.5 font-bold text-sumi truncate">
                  NHẬT <span className="font-normal text-xs text-nibi">· Mặt trời, ngày, Nhật Bản</span>
                </span>
                <span className="block text-[11px] text-gray-400 truncate">4 nét • On: ニチ, ジツ • Kun: ひ, -び</span>
                <span className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-indigo-800 group-hover:underline">
                  Xem thứ tự nét vẽ & Từ ghép <ArrowRight className="w-3 h-3" />
                </span>
              </span>
            </button>

            {/* Từ vựng thông dụng */}
            <button
              type="button"
              onClick={() => {
                setActiveTab('vocab')
                setKeyword('食べる')
              }}
              className="group w-full text-left flex items-center gap-3 p-3 rounded-md bg-surface border border-suna hover:bg-kamenozoki/40 hover:shadow-xs transition cursor-pointer"
            >
              <span className="w-12 h-12 shrink-0 rounded-md bg-kamenozoki text-indigo-600 font-serif text-2xl font-bold flex items-center justify-center">
                食
              </span>
              <span className="flex-1 min-w-0">
                <span className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-nibi">
                  <BookOpen className="w-3.5 h-3.5" /> Từ vựng thông dụng
                  <span className={`ml-auto text-[10px] px-1.5 py-0.5 rounded ${JLPT_LEVEL_BADGE.N5}`}>N5</span>
                </span>
                <span className="block mt-0.5 font-bold text-sumi truncate">
                  食べる <span className="font-normal text-xs text-nibi">【たべる】</span>
                </span>
                <span className="block text-[11px] text-gray-400 truncate">THỰC · Ăn (động từ nhóm 2)</span>
                <span className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 group-hover:underline">
                  Xem chi tiết & Ví dụ câu <ArrowRight className="w-3 h-3" />
                </span>
              </span>
            </button>

            {/* Bảng chữ cái tương tác */}
            <button
              type="button"
              onClick={() => setActiveTab('kana')}
              className="group w-full text-left flex items-center gap-3 p-3 rounded-md bg-surface border border-suna hover:bg-kamenozoki/40 hover:shadow-xs transition cursor-pointer"
            >
              <span className="w-12 h-12 shrink-0 rounded-md bg-kamenozoki text-indigo-500 font-serif text-2xl font-bold flex items-center justify-center">
                あ
              </span>
              <span className="flex-1 min-w-0">
                <span className="flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-nibi">
                  <Languages className="w-3.5 h-3.5" /> Bảng chữ cái
                  <span className="ml-auto text-[10px] px-1.5 py-0.5 rounded bg-surface text-nibi border border-suna">50 âm</span>
                </span>
                <span className="block mt-0.5 font-bold text-sumi truncate">Hiragana & Katakana</span>
                <span className="block text-[11px] text-gray-400 line-clamp-2">
                  Phát âm chuẩn từng chữ, đọc tuần tự cả hàng và tập viết nét trực quan.
                </span>
                <span className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-indigo-500 group-hover:underline">
                  Mở bảng chữ cái <ArrowRight className="w-3 h-3" />
                </span>
              </span>
            </button>
          </aside>
        </div>
      )}

      {/* Modal nhận dạng chữ viết tay Kanji bằng AI */}
      {/* Modal Viết chữ Hán trên không qua Camera */}
      <KanjiAirWritingModal
        isOpen={isAirWritingOpen}
        onClose={() => setIsAirWritingOpen(false)}
        onSelectCharacter={(char) => {
          setKeyword(char)
          if (char.length > 1) {
            setActiveTab('vocab')
          } else {
            setActiveTab('kanji')
          }
          setIsDropdownOpen(false)
        }}
      />

      <KanjiHandwritingModal
        isOpen={isHandwritingOpen}
        onClose={() => setIsHandwritingOpen(false)}
        onSelectCharacter={(char) => {
          setKeyword(char)
          setActiveTab('kanji')
          setIsDropdownOpen(false)
        }}
      />

      {/* Modal Luyện viết & Chấm điểm nét AI */}
      <KanjiPracticeModal
        isOpen={isPracticeOpen}
        onClose={() => setIsPracticeOpen(false)}
        character={selectedKanjiChar || (keyword.trim().length === 1 ? keyword.trim() : '木')}
      />
    </div>
  )
}
