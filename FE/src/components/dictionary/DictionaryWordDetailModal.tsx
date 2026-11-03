import React, { useState, useEffect } from 'react'
import { Modal, Tooltip, message } from 'antd'
import {
  CloseOutlined,
  SoundOutlined,
  PlusOutlined,
  CopyOutlined,
  ThunderboltOutlined,
  PictureOutlined,
  AudioOutlined,
  CheckOutlined,
  RightOutlined,
} from '@ant-design/icons'
import type {
  VocabularyDetail,
  DictionaryUnifiedSearch,
  KanjiDetail,
  GrammarSummary,
} from '@/types'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'
import { KanjiDetailPanel } from './KanjiDetailPanel'
import { GrammarDetailPanel } from './GrammarDetailPanel'
import { AddWordToNotebookModal, type AddWordTargetData } from '@/components/notebook/AddWordToNotebookModal'

interface DictionaryWordDetailModalProps {
  open: boolean
  onClose: () => void
  word: string
  vocabId?: number
  defaultTab?: 'vocab' | 'kanji' | 'grammar'
}

type TabType = 'vocab' | 'kanji' | 'grammar'

const TABS: { key: TabType; label: string }[] = [
  { key: 'vocab', label: 'Từ vựng' },
  { key: 'kanji', label: 'Hán tự' },
  { key: 'grammar', label: 'Ngữ pháp' },
]

export const DictionaryWordDetailModal: React.FC<DictionaryWordDetailModalProps> = ({
  open,
  onClose,
  word,
  vocabId,
  defaultTab = 'vocab',
}) => {
  const [activeTab, setActiveTab] = useState<TabType>(defaultTab)
  const [vocabDetail, setVocabDetail] = useState<VocabularyDetail | null>(null)
  const [unifiedResults, setUnifiedResults] = useState<DictionaryUnifiedSearch | null>(null)
  const [kanjiList, setKanjiList] = useState<KanjiDetail[]>([])
  const [selectedKanjiChar, setSelectedKanjiChar] = useState<string | null>(null)
  const [grammarList, setGrammarList] = useState<GrammarSummary[]>([])
  const [selectedGrammarId, setSelectedGrammarId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [isAddToNotebookOpen, setIsAddToNotebookOpen] = useState<boolean>(false)
  const [copied, setCopied] = useState(false)

  // Khởi tạo tab phù hợp khi mở modal
  useEffect(() => {
    if (!open) return

    if (defaultTab && defaultTab !== 'vocab') {
      setActiveTab(defaultTab)
    } else {
      const trimmed = (word || '').trim()
      const isGrammarPattern = trimmed.startsWith('〜') || trimmed.startsWith('~')
      const isSingleKanji = trimmed.length === 1 && /[\u4e00-\u9faf]/.test(trimmed) && !vocabId

      if (isGrammarPattern) {
        setActiveTab('grammar')
      } else if (isSingleKanji) {
        setActiveTab('kanji')
      } else {
        setActiveTab('vocab')
      }
    }
    setCopied(false)
  }, [open, defaultTab, word, vocabId])

  // Tải thông tin từ vựng, chữ Hán và ngữ pháp
  useEffect(() => {
    if (!open || !word) return

    let isMounted = true
    setIsLoading(true)

    const fetchAll = async () => {
      try {
        // 1. Tìm kiếm tổng hợp Mazii Unified Search
        const searchRes = await dictionaryService.searchUnified(word)
        if (!isMounted) return

        const searchData: DictionaryUnifiedSearch = searchRes.data || {
          keyword: word,
          vocabularies: [],
          kanjis: [],
          grammars: [],
        }
        setUnifiedResults(searchData)

        // 2. Tải chi tiết từ vựng
        let targetId = vocabId
        if (!targetId && searchData.vocabularies && searchData.vocabularies.length > 0) {
          const exactMatch = searchData.vocabularies.find((v) => v.word === word)
          targetId = exactMatch ? exactMatch.id : searchData.vocabularies[0].id
        }

        let resolvedWord = word
        if (targetId) {
          const detailRes = await dictionaryService.getVocabularyDetail(targetId)
          if (isMounted && detailRes.data) {
            setVocabDetail(detailRes.data)
            resolvedWord = detailRes.data.word || word
          }
        } else {
          setVocabDetail({
            id: 0,
            word,
            hiragana: '',
            meaningVi: '',
          })
        }

        // 3. Tải chi tiết các chữ Kanji (loại bỏ trùng lặp, hỗ trợ cả khi từ khóa nhập là Kana)
        let kanjiChars = Array.from(new Set(Array.from(resolvedWord).filter((char) => /[\u4e00-\u9faf]/.test(char))))
        if (kanjiChars.length === 0 && searchData.kanjis && searchData.kanjis.length > 0) {
          kanjiChars = Array.from(new Set(searchData.kanjis.map((k) => k.character)))
        }

        if (kanjiChars.length > 0) {
          const kanjiPromises = kanjiChars.map((ch) =>
            dictionaryService
              .getKanjiDetail(ch)
              .then((r) => r.data)
              .catch(() => null)
          )
          const results = await Promise.all(kanjiPromises)
          if (isMounted) {
            const valid = results.filter((k): k is KanjiDetail => k !== null)
            setKanjiList(valid)
            setSelectedKanjiChar(valid[0]?.character || kanjiChars[0] || null)
          }
        } else {
          if (isMounted) {
            setKanjiList([])
            setSelectedKanjiChar(null)
          }
        }

        // 4. Tải danh sách điểm ngữ pháp tương ứng
        let grammars: GrammarSummary[] = searchData.grammars || []
        if (grammars.length === 0) {
          try {
            const gRes = await dictionaryService.searchGrammars({ keyword: word, size: 10 })
            if (gRes.data?.items) {
              grammars = gRes.data.items
            }
          } catch {
            // Bỏ qua lỗi tìm ngữ pháp
          }
        }
        if (isMounted) {
          setGrammarList(grammars)
          setSelectedGrammarId(grammars.length > 0 ? grammars[0].id : null)
        }
      } catch (err) {
        console.error('Lỗi tải dữ liệu tra cứu từ vựng:', err)
      } finally {
        if (isMounted) setIsLoading(false)
      }
    }

    fetchAll()

    return () => {
      isMounted = false
    }
  }, [open, word, vocabId])

  if (!open) return null

  const handleCopyWord = () => {
    if (!word) return
    navigator.clipboard.writeText(word)
    setCopied(true)
    message.success('Đã sao chép vào bộ nhớ tạm')
    setTimeout(() => setCopied(false), 2000)
  }

  const handlePlayWordAudio = (textToPlay?: string) => {
    playSmartAudio({
      text: textToPlay || vocabDetail?.word || word,
      audioUrl: vocabDetail?.audioUrl,
    })
  }

  // Tải chi tiết một từ vựng khác khi người dùng chọn ở cột kết quả
  const handleSelectOtherVocab = async (newVocabId: number) => {
    try {
      setIsLoading(true)
      const res = await dictionaryService.getVocabularyDetail(newVocabId)
      if (res.data) {
        setVocabDetail(res.data)
        const chars = Array.from(
          new Set(Array.from(res.data.word).filter((char) => /[\u4e00-\u9faf]/.test(char)))
        )
        if (chars.length > 0) {
          const kanjiPromises = chars.map((ch) =>
            dictionaryService.getKanjiDetail(ch).then((r) => r.data).catch(() => null)
          )
          const results = await Promise.all(kanjiPromises)
          const valid = results.filter((k): k is KanjiDetail => k !== null)
          setKanjiList(valid)
          setSelectedKanjiChar(valid[0]?.character || chars[0] || null)
        } else {
          setKanjiList([])
          setSelectedKanjiChar(null)
        }
      }
    } catch (err) {
      console.error('Lỗi khi tải chi tiết từ vựng:', err)
    } finally {
      setIsLoading(false)
    }
  }

  const currentWord = vocabDetail?.word || word

  const addWordData: AddWordTargetData = {
    word: currentWord,
    reading: vocabDetail?.hiragana || '',
    sinoVietnamese: vocabDetail?.hanViet || '',
    meaning: vocabDetail?.meaningVi || '',
    partOfSpeech: vocabDetail?.partOfSpeech || 'Từ vựng',
    audioUrl: vocabDetail?.audioUrl,
  }

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      width={1140}
      centered
      className="max-w-[95vw]"
      closeIcon={<CloseOutlined className="text-gray-500 hover:text-gray-800 text-base" />}
      styles={{
        body: {
          padding: 0,
        },
      }}
    >
      <div className="flex flex-col max-h-[88vh]">
        {/* 1. Header: Tiêu đề "Chi tiết từ [từ]" */}
        <div className="px-6 pt-4 pb-2.5 border-b border-gray-100 flex items-center justify-between shrink-0 bg-white">
          <h2 className="text-lg font-bold text-gray-900 m-0">
            Chi tiết từ <span className="text-blue-600">{currentWord}</span>
          </h2>
        </div>

        {/* 2. Thanh 3 Tabs chuẩn Từ điển (Đã loại bỏ Mẫu câu và Nhật - Nhật) */}
        <div className="px-6 py-2 border-b border-gray-100 flex items-center gap-1 overflow-x-auto bg-gray-50/70 shrink-0">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.key
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => setActiveTab(tab.key)}
                className={`px-4 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer select-none ${
                  isActive
                    ? 'bg-white text-blue-600 shadow-2xs font-bold border border-gray-200/60'
                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100/80'
                }`}
              >
                {tab.label}
              </button>
            )
          })}
        </div>

        {/* 3. Vùng nội dung cuộn được */}
        <div className="flex-1 overflow-y-auto p-5 md:p-6 bg-slate-50/50">
          {isLoading ? (
            <div className="py-20 text-center space-y-3">
              <div className="w-10 h-10 border-3 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto" />
              <p className="text-sm font-medium text-gray-500">Đang tra cứu từ điển...</p>
            </div>
          ) : (
            <>
              {/* TAB 1: TỪ VỰNG (2 Cột chuẩn Mazii) */}
              {activeTab === 'vocab' && (
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
                  {/* Cột trái (7/12): Chi tiết từ vựng chính */}
                  <div className="lg:col-span-7 bg-white rounded-2xl p-6 border border-gray-200/80 shadow-2xs space-y-5">
                    {/* Hàng 1: Mặt chữ to, các nút thao tác */}
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <h1 className="text-4xl md:text-5xl font-black text-blue-600 tracking-tight font-sans">
                          {vocabDetail?.word || word}
                        </h1>

                        <div className="flex items-center gap-3 text-sm text-gray-600 font-semibold mt-1">
                          {vocabDetail?.hiragana && <span>「 {vocabDetail.hiragana} 」</span>}
                          {vocabDetail?.hanViet && (
                            <span className="text-gray-400 font-bold uppercase tracking-wider">
                              「 {vocabDetail.hanViet} 」
                            </span>
                          )}
                        </div>

                        {vocabDetail?.romaji && (
                          <div className="flex items-center gap-1.5 text-xs text-gray-500 font-mono mt-0.5">
                            <button
                              type="button"
                              onClick={() => handlePlayWordAudio(vocabDetail.hiragana || word)}
                              className="text-gray-400 hover:text-blue-600 transition-colors cursor-pointer"
                              title="Nghe cách đọc"
                            >
                              <SoundOutlined />
                            </button>
                            <span>{vocabDetail.romaji}</span>
                          </div>
                        )}
                      </div>

                      {/* Nút thao tác: [+ Sổ tay], [Sao chép], [Loa phát âm] */}
                      <div className="flex items-center gap-1.5 shrink-0">
                        <Tooltip title="Thêm vào sổ tay">
                          <button
                            type="button"
                            onClick={() => setIsAddToNotebookOpen(true)}
                            className="w-9 h-9 rounded-xl bg-gray-50 hover:bg-blue-50 hover:text-blue-600 border border-gray-200 text-gray-600 flex items-center justify-center transition-all cursor-pointer"
                          >
                            <PlusOutlined className="text-sm" />
                          </button>
                        </Tooltip>

                        <Tooltip title={copied ? 'Đã sao chép!' : 'Sao chép từ'}>
                          <button
                            type="button"
                            onClick={handleCopyWord}
                            className="w-9 h-9 rounded-xl bg-gray-50 hover:bg-blue-50 hover:text-blue-600 border border-gray-200 text-gray-600 flex items-center justify-center transition-all cursor-pointer"
                          >
                            {copied ? (
                              <CheckOutlined className="text-emerald-500 text-sm" />
                            ) : (
                              <CopyOutlined className="text-sm" />
                            )}
                          </button>
                        </Tooltip>

                        <Tooltip title="Nghe phát âm chuẩn">
                          <button
                            type="button"
                            onClick={() => handlePlayWordAudio()}
                            className="w-9 h-9 rounded-xl bg-blue-50 hover:bg-blue-100 text-blue-600 border border-blue-200 flex items-center justify-center transition-all cursor-pointer"
                          >
                            <SoundOutlined className="text-base" />
                          </button>
                        </Tooltip>
                      </div>
                    </div>

                    {/* Hàng 2: Các nút tính năng mở rộng phong cách Mazii */}
                    <div className="flex items-center gap-2 flex-wrap pt-1 border-t border-gray-100">
                      <button
                        type="button"
                        onClick={() =>
                          message.info(
                            `Giải nghĩa AI: "${word}" là từ ngữ thường dùng trong giao tiếp tiếng Nhật đời sống và các kỳ thi JLPT.`
                          )
                        }
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-200 text-purple-700 text-xs font-semibold hover:shadow-2xs transition-all cursor-pointer"
                      >
                        <ThunderboltOutlined className="text-purple-600" />
                        <span>Giải nghĩa AI</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => message.info('Hình ảnh minh họa trực quan cho từ vựng')}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-gray-50 border border-gray-200 text-gray-600 text-xs font-medium hover:bg-gray-100 transition-all cursor-pointer"
                      >
                        <PictureOutlined />
                        <span>Ảnh minh họa</span>
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          message.info('Bạn có thể mở chế độ "Luyện nói, viết" trong Sổ tay để thực hành phát âm với AI!')
                        }
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-gray-50 border border-gray-200 text-gray-600 text-xs font-medium hover:bg-gray-100 transition-all cursor-pointer"
                      >
                        <AudioOutlined />
                        <span>Luyện phát âm</span>
                      </button>
                    </div>

                    {/* Hàng 3: Badge JLPT & Từ loại */}
                    <div className="flex items-center gap-2 flex-wrap">
                      {vocabDetail?.jlptLevel && (
                        <span className="px-2.5 py-1 rounded-md bg-blue-50 text-blue-700 text-xs font-bold border border-blue-200">
                          JLPT {vocabDetail.jlptLevel}
                        </span>
                      )}
                      {vocabDetail?.partOfSpeech && (
                        <span className="px-2.5 py-1 rounded-md bg-rose-50 text-rose-600 text-xs font-bold uppercase tracking-wider border border-rose-200">
                          ❖ {vocabDetail.partOfSpeech}
                        </span>
                      )}
                    </div>

                    {/* Hàng 4: Nghĩa tiếng Việt chính */}
                    <div className="space-y-3 pt-2">
                      <div className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full bg-blue-600" />
                        <h3 className="text-lg font-black text-gray-900 m-0">
                          {vocabDetail?.meaningVi || 'Đang cập nhật ý nghĩa...'}
                        </h3>
                      </div>

                      {/* Câu ví dụ đi kèm */}
                      {(vocabDetail?.examples && vocabDetail.examples.length > 0) || vocabDetail?.exampleJp ? (
                        <div className="space-y-2">
                          {(vocabDetail.examples || []).slice(0, 3).map((ex, idx) => (
                            <div
                              key={idx}
                              className="bg-gray-50/80 rounded-xl p-3.5 border border-gray-100 flex items-start justify-between gap-3"
                            >
                              <div className="space-y-1">
                                <div className="text-sm font-bold text-gray-900">{ex.exampleJp}</div>
                                {ex.exampleRomaji && (
                                  <div className="text-xs text-gray-400 font-mono">
                                    {ex.exampleRomaji}
                                  </div>
                                )}
                                <div className="text-xs text-gray-600">{ex.exampleVi}</div>
                              </div>
                              <button
                                type="button"
                                onClick={() =>
                                  playSmartAudio({
                                    text: ex.exampleJp,
                                    audioUrl: ex.audioUrl,
                                  })
                                }
                                className="w-7 h-7 rounded-full bg-white hover:bg-blue-50 text-gray-500 hover:text-blue-600 flex items-center justify-center border border-gray-200 transition-colors shrink-0 cursor-pointer text-xs"
                              >
                                <SoundOutlined />
                              </button>
                            </div>
                          ))}

                          {(!vocabDetail.examples || vocabDetail.examples.length === 0) &&
                            vocabDetail.exampleJp && (
                              <div className="bg-gray-50/80 rounded-xl p-3.5 border border-gray-100 flex items-start justify-between gap-3">
                                <div className="space-y-1">
                                  <div className="text-sm font-bold text-gray-900">
                                    {vocabDetail.exampleJp}
                                  </div>
                                  <div className="text-xs text-gray-600">{vocabDetail.exampleVi}</div>
                                </div>
                                <button
                                  type="button"
                                  onClick={() => playSmartAudio({ text: vocabDetail.exampleJp || '' })}
                                  className="w-7 h-7 rounded-full bg-white hover:bg-blue-50 text-gray-500 hover:text-blue-600 flex items-center justify-center border border-gray-200 transition-colors shrink-0 cursor-pointer text-xs"
                                >
                                  <SoundOutlined />
                                </button>
                              </div>
                            )}
                        </div>
                      ) : null}

                      {/* Các nét nghĩa phụ nếu có */}
                      {vocabDetail?.senses && vocabDetail.senses.length > 0 && (
                        <div className="space-y-2 pt-2 border-t border-gray-100">
                          {vocabDetail.senses.map((sense, sIdx) => (
                            <div key={sIdx} className="space-y-1">
                              <div className="flex items-center gap-2">
                                <span className="w-2 h-2 rounded-full bg-indigo-400" />
                                <span className="text-sm font-bold text-gray-800">
                                  {sense.meaningVi}
                                </span>
                                {sense.partOfSpeech && (
                                  <span className="text-[10px] text-gray-500 font-medium">
                                    ({sense.partOfSpeech})
                                  </span>
                                )}
                              </div>
                              {sense.usageNotes && (
                                <div className="ml-4 text-xs text-gray-500 italic">
                                  {sense.usageNotes}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Cột phải (5/12): Kết quả tra cứu, Các chữ Kanji, Các từ liên quan */}
                  <div className="lg:col-span-5 space-y-4">
                    {/* Card 1: KẾT QUẢ TRA CỨU */}
                    <div className="bg-white rounded-2xl p-4 border border-gray-200/80 shadow-2xs space-y-2">
                      <div className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                        Kết quả tra cứu {currentWord}
                      </div>

                      <div className="space-y-1.5 max-h-48 overflow-y-auto pr-1">
                        {unifiedResults?.vocabularies && unifiedResults.vocabularies.length > 0 ? (
                          unifiedResults.vocabularies.map((v) => {
                            const isCurrent = v.id === vocabDetail?.id
                            return (
                              <div
                                key={v.id}
                                onClick={() => handleSelectOtherVocab(v.id)}
                                className={`p-2.5 rounded-xl border text-xs cursor-pointer transition-all ${
                                  isCurrent
                                    ? 'bg-blue-50/70 border-blue-400 font-semibold'
                                    : 'bg-gray-50/60 border-gray-100 hover:border-blue-200 hover:bg-blue-50/30'
                                }`}
                              >
                                <div className="flex items-center justify-between">
                                  <span className="font-bold text-blue-600">{v.word}</span>
                                  <span className="text-gray-400 text-[11px]">{v.hiragana}</span>
                                </div>
                                <div className="text-gray-600 truncate mt-0.5">{v.meaningVi}</div>
                              </div>
                            )
                          })
                        ) : (
                          <div className="p-2.5 rounded-xl border border-blue-200 bg-blue-50/40 text-xs">
                            <span className="font-bold text-blue-600">{currentWord}</span>
                            <div className="text-gray-600 mt-0.5">
                              {vocabDetail?.meaningVi || 'Đang cập nhật'}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Card 2: CÁC CHỮ KANJI CỦA TỪ */}
                    <div className="bg-white rounded-2xl p-4 border border-gray-200/80 shadow-2xs space-y-3">
                      <div className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                        Các chữ Kanji của {currentWord}
                      </div>

                      <div className="space-y-3">
                        {kanjiList.length > 0 ? (
                          kanjiList.map((k) => (
                            <div
                              key={k.id || k.character}
                              className="p-3 bg-gray-50/70 rounded-xl border border-gray-100 space-y-2"
                            >
                              <div className="flex items-start justify-between">
                                <div className="flex items-center gap-2">
                                  <span className="text-2xl font-black text-gray-900 font-serif">
                                    {k.character}
                                  </span>
                                  {k.onyomi && k.onyomi.length > 0 && (
                                    <span className="text-xs text-gray-600 font-bold">
                                      {k.onyomi.join('・')}
                                    </span>
                                  )}
                                </div>
                                {k.hanViet && (
                                  <span className="text-xs font-bold text-amber-700 uppercase">
                                    {k.hanViet}
                                  </span>
                                )}
                              </div>

                              <div className="text-xs text-gray-600 space-y-0.5">
                                {k.kunyomi && k.kunyomi.length > 0 && (
                                  <div>
                                    <span className="font-semibold text-gray-500">訓:</span>{' '}
                                    {k.kunyomi.join(' ')}
                                  </div>
                                )}
                                {k.onyomi && k.onyomi.length > 0 && (
                                  <div>
                                    <span className="font-semibold text-gray-500">音:</span>{' '}
                                    {k.onyomi.join(' ')}
                                  </div>
                                )}
                                <div className="flex items-center gap-3 text-gray-400 text-[11px] pt-0.5">
                                  <span>Số nét: {k.strokeCount}</span>
                                  {k.jlptLevel && <span>JLPT: {k.jlptLevel}</span>}
                                </div>
                              </div>

                              <div className="pt-1 text-right">
                                <button
                                  type="button"
                                  onClick={() => {
                                    setSelectedKanjiChar(k.character)
                                    setActiveTab('kanji')
                                  }}
                                  className="inline-flex items-center gap-1 text-xs font-semibold text-blue-600 hover:text-blue-800 transition-colors cursor-pointer"
                                >
                                  <span>Xem chi tiết hơn</span>
                                  <RightOutlined className="text-[10px]" />
                                </button>
                              </div>
                            </div>
                          ))
                        ) : (
                          <div className="text-xs text-gray-400 py-1">
                            Từ này không chứa chữ Hán tự (Kanji).
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Card 3: CHỮ HÁN CẤU THÀNH */}
                    <div className="bg-white rounded-2xl p-4 border border-gray-200/80 shadow-2xs space-y-2">
                      <div className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                        Chữ Hán cấu thành của {currentWord}
                      </div>
                      <div className="text-xs text-gray-500 space-y-1.5">
                        {vocabDetail?.kanjiComponents && vocabDetail.kanjiComponents.length > 0 ? (
                          vocabDetail.kanjiComponents.map((comp) => (
                            <div
                              key={comp.id}
                              onClick={() => {
                                setSelectedKanjiChar(comp.character)
                                setActiveTab('kanji')
                              }}
                              className="p-2 bg-gray-50 rounded-lg flex items-center justify-between hover:bg-blue-50/50 cursor-pointer transition-colors"
                            >
                              <span className="font-bold text-gray-800">{comp.character}</span>
                              <span className="text-amber-700 font-semibold">{comp.hanViet}</span>
                              <span className="text-gray-500 truncate max-w-[120px]">
                                {comp.meaningVi}
                              </span>
                            </div>
                          ))
                        ) : (
                          <div className="text-xs text-gray-400 py-1">
                            Xem thêm các dạng cấu tạo từ ở mục Hán tự.
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 2: HÁN TỰ (Trang chữ Hán có từ 2 chữ trở lên thì có chỗ chọn bên cạnh như trang từ điển, chỉ hiển thị 1 chữ) */}
              {activeTab === 'kanji' && (
                <div>
                  {kanjiList.length > 0 ? (
                    kanjiList.length >= 2 ? (
                      <div className="flex flex-col md:flex-row gap-5 items-start">
                        {/* Cột trái: Bộ chọn chữ Hán gọn gàng, sticky top, KHÔNG có thanh cuộn riêng */}
                        <div className="w-full md:w-56 shrink-0 space-y-2 sticky top-0 self-start">
                          <div className="text-[11px] font-bold text-gray-400 uppercase tracking-wider px-1">
                            Chữ Hán ({kanjiList.length})
                          </div>
                          {kanjiList.map((k) => {
                            const isSelected =
                              (selectedKanjiChar || kanjiList[0].character) === k.character
                            return (
                              <div
                                key={k.id || k.character}
                                onClick={() => setSelectedKanjiChar(k.character)}
                                className={`p-2.5 rounded-xl border transition cursor-pointer flex items-center justify-between gap-2.5 ${
                                  isSelected
                                    ? 'bg-blue-50/90 border-blue-500 shadow-2xs ring-1.5 ring-blue-300'
                                    : 'bg-white border-gray-200/90 hover:border-blue-300 hover:bg-slate-50/70'
                                }`}
                              >
                                <div className="flex items-center gap-2.5 min-w-0">
                                  <div className="w-9 h-9 rounded-lg bg-amber-100/70 border border-amber-200 flex items-center justify-center text-lg font-bold text-gray-900 font-serif shrink-0">
                                    {k.character}
                                  </div>
                                  <div className="min-w-0 flex-1">
                                    <div className="flex items-center gap-1.5">
                                      <span className="font-extrabold text-amber-700 text-xs uppercase truncate">
                                        {k.hanViet || 'Hán tự'}
                                      </span>
                                      {k.jlptLevel && (
                                        <span className="text-[9px] font-bold px-1 py-0.2 rounded bg-gray-100 text-gray-600 shrink-0">
                                          {k.jlptLevel}
                                        </span>
                                      )}
                                    </div>
                                    <p className="text-[11px] text-gray-500 truncate mt-0.5 font-medium m-0">
                                      {k.meaningVi}
                                    </p>
                                    <span className="text-[10px] text-gray-400 block mt-0.5">
                                      {k.strokeCount} nét
                                    </span>
                                  </div>
                                </div>
                                <RightOutlined className="text-gray-300 text-[10px] shrink-0" />
                              </div>
                            )
                          })}
                        </div>

                        {/* Cột phải: Chỉ hiển thị 1 chữ Hán được chọn, rộng rãi, KHÔNG có thanh cuộn riêng */}
                        <div className="flex-1 min-w-0">
                          <KanjiDetailPanel
                            character={selectedKanjiChar || kanjiList[0].character}
                            onSelectVocab={(newVocabId) => {
                              handleSelectOtherVocab(newVocabId)
                              setActiveTab('vocab')
                            }}
                          />
                        </div>
                      </div>
                    ) : (
                      /* Nếu chỉ có 1 chữ Hán: hiển thị trực tiếp panel không cần cột chọn */
                      <div className="w-full">
                        <KanjiDetailPanel
                          character={selectedKanjiChar || kanjiList[0].character}
                          onSelectVocab={(newVocabId) => {
                            handleSelectOtherVocab(newVocabId)
                            setActiveTab('vocab')
                          }}
                        />
                      </div>
                    )
                  ) : (
                    <div className="bg-white rounded-2xl p-10 text-center text-gray-500 border border-gray-200 space-y-2">
                      <p className="font-medium">Từ này không chứa ký tự chữ Hán (Kanji).</p>
                      <p className="text-xs text-gray-400">
                        Bạn có thể xem chi tiết nghĩa ở tab &quot;Từ vựng&quot;.
                      </p>
                    </div>
                  )}
                </div>
              )}

              {/* TAB 3: NGỮ PHÁP (Tra cứu vào Ngữ pháp, có chỗ chọn bên cạnh nếu có từ 2 điểm ngữ pháp) */}
              {activeTab === 'grammar' && (
                <div>
                  {grammarList.length > 0 ? (
                    grammarList.length >= 2 ? (
                      <div className="flex flex-col md:flex-row gap-5 items-start">
                        {/* Cột trái: Chỗ chọn cấu trúc ngữ pháp gọn gàng, sticky top */}
                        <div className="w-full md:w-56 shrink-0 space-y-2 sticky top-0 self-start">
                          <div className="text-[11px] font-bold text-gray-400 uppercase tracking-wider px-1">
                            Ngữ pháp liên quan ({grammarList.length})
                          </div>
                          {grammarList.map((g) => {
                            const isSelected =
                              (selectedGrammarId || grammarList[0].id) === g.id
                            return (
                              <div
                                key={g.id}
                                onClick={() => setSelectedGrammarId(g.id)}
                                className={`p-2.5 rounded-xl border transition cursor-pointer ${
                                  isSelected
                                    ? 'bg-purple-50/90 border-purple-500 shadow-xs ring-1.5 ring-purple-200'
                                    : 'bg-white border-gray-200 hover:border-purple-300 hover:bg-slate-50/60'
                                }`}
                              >
                                <div className="flex items-center justify-between gap-1.5 mb-1">
                                  <span className="font-extrabold text-gray-900 text-xs font-sans line-clamp-1">
                                    {g.title}
                                  </span>
                                  {g.jlptLevel && (
                                    <span className="text-[9px] font-bold px-1.5 py-0.2 rounded bg-purple-100 text-purple-700 shrink-0">
                                      {g.jlptLevel}
                                    </span>
                                  )}
                                </div>
                                <p className="text-[11px] text-gray-500 line-clamp-2 m-0">{g.meaningVi}</p>
                              </div>
                            )
                          })}
                        </div>

                        {/* Cột phải: Chỉ hiển thị 1 điểm ngữ pháp được chọn, rộng rãi, KHÔNG thanh cuộn riêng */}
                        <div className="flex-1 min-w-0">
                          <GrammarDetailPanel grammarId={selectedGrammarId || grammarList[0].id} />
                        </div>
                      </div>
                    ) : (
                      /* Nếu chỉ có 1 điểm ngữ pháp: hiển thị trực tiếp */
                      <div className="w-full">
                        <GrammarDetailPanel grammarId={grammarList[0].id} />
                      </div>
                    )
                  ) : (
                    <div className="bg-white rounded-2xl p-10 text-center text-gray-500 border border-gray-200 space-y-2">
                      <p className="font-medium">
                        Không tìm thấy cấu trúc ngữ pháp tương ứng với từ khóa này.
                      </p>
                      <p className="text-xs text-gray-400">
                        Bạn có thể tra cứu nghĩa và các dạng câu ở tab &quot;Từ vựng&quot; hoặc &quot;Hán tự&quot;.
                      </p>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Modal thêm từ vào sổ tay */}
      <AddWordToNotebookModal
        open={isAddToNotebookOpen}
        onClose={() => setIsAddToNotebookOpen(false)}
        wordData={addWordData}
      />
    </Modal>
  )
}
