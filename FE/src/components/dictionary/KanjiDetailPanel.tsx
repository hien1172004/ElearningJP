import React, { useState, useEffect, useRef } from 'react'
import { Tooltip } from 'antd'
import { Volume2, BookmarkPlus, Check, Sparkles, PenTool, Plus, Network } from 'lucide-react'
import type { KanjiDetail } from '@/types'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'
import { useAddToSrs, useSrsSavedIds } from '@/hooks'
import { KanjiStrokeViewer } from './KanjiStrokeViewer'
import { KanjiPracticeModal } from './KanjiPracticeModal'
import { KanjiBreakdownModal } from './KanjiBreakdownModal'
import { AddWordToNotebookModal, type AddWordTargetData } from '@/components/notebook/AddWordToNotebookModal'

interface KanjiDetailPanelProps {
  character: string
  onSelectVocab?: (vocabId: number, word: string) => void
  /**
   * Gọi khi đã resolve xong id của chữ Hán.
   * BE dùng `writing_characters.id` làm targetId cho bình luận KANJI,
   * trong khi state của trang tra cứu chỉ giữ ký tự (character).
   */
  onIdResolved?: (id: number) => void
}

export const KanjiDetailPanel: React.FC<KanjiDetailPanelProps> = ({
  character,
  onSelectVocab,
  onIdResolved,
}) => {
  const [detail, setDetail] = useState<KanjiDetail | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const { isSaved } = useSrsSavedIds()
  const isAddedToSrs = isSaved('KANJI', detail?.id)
  const [isAddToNotebookOpen, setIsAddToNotebookOpen] = useState<boolean>(false)
  const [isPracticeOpen, setIsPracticeOpen] = useState<boolean>(false)
  const [isBreakdownOpen, setIsBreakdownOpen] = useState<boolean>(false)
  const [expandedReadings, setExpandedReadings] = useState<Record<string, boolean>>({})
  const addToSrs = useAddToSrs()

  // Giữ callback trong ref để không tạo vòng lặp render vô hạn khi parent
  // truyền callback mới mỗi lần render.
  const onIdResolvedRef = useRef(onIdResolved)
  useEffect(() => {
    onIdResolvedRef.current = onIdResolved
  }, [onIdResolved])

  useEffect(() => {
    let isMounted = true
    setIsLoading(true)

    dictionaryService
      .getKanjiDetail(character)
      .then((res) => {
        if (isMounted && res.data) {
          setDetail(res.data)
          // Đưa id của chữ Hán ra ngoài (dùng làm targetId cho bình luận).
          onIdResolvedRef.current?.(res.data.id)
        }
      })
      .catch((err) => console.error('Lỗi tải chi tiết chữ Hán:', err))
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [character])

  if (isLoading) {
    return (
      <div className="bg-white rounded-md p-12 border border-gray-200/80 shadow-xs text-center text-gray-500 animate-pulse space-y-3">
        <div className="w-10 h-10 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm font-medium">Đang tải nét vẽ & từ ghép chữ Hán...</p>
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="bg-white rounded-md p-10 border border-gray-200 text-center text-gray-500">
        Không tìm thấy thông tin chữ Hán.
      </div>
    )
  }

  const handleAddToSrs = async () => {
    if (isAddedToSrs || !detail) return
    try {
      await addToSrs.mutateAsync({ type: 'KANJI', id: detail.id })
    } catch {
      /* ignore */
    }
  }

  const kanjiWordData: AddWordTargetData = {
    word: detail.character,
    reading: detail.kunyomi?.[0]?.replace(/\./g, '') || detail.onyomi?.[0] || '',
    sinoVietnamese: detail.hanViet?.split(',')?.[0]?.trim()?.toUpperCase() || '',
    meaning: detail.meaningVi || '',
    partOfSpeech: 'Chữ Hán',
  }

  return (
    <div className="bg-white rounded-md border border-gray-200/80 shadow-sm p-6 md:p-8 space-y-6">
      {/* 1. Header Chữ Hán */}
      <div className="flex flex-col md:flex-row md:items-start justify-between gap-4 border-b border-gray-100 pb-5">
        <div className="space-y-2 flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            {detail.jlptLevel && (
              <span className="px-2.5 py-0.5 text-xs font-bold rounded bg-indigo-50 text-indigo-700 border border-indigo-200/80">
                JLPT {detail.jlptLevel}
              </span>
            )}
            {detail.hanViet && (
              <span className="px-3 py-0.5 text-xs font-extrabold rounded bg-amber-50 text-amber-700 border border-amber-200 uppercase tracking-wide">
                Hán Việt: {detail.hanViet}
              </span>
            )}
          </div>

          <div className="flex items-center gap-4 flex-wrap">
            <h2 className="text-5xl md:text-6xl font-black text-gray-900 font-serif shrink-0 m-0 leading-none">
              {detail.character}
            </h2>
            <div className="flex-1 min-w-[140px]">
              <p className="text-xl md:text-2xl font-bold text-indigo-950 m-0 break-words leading-snug">
                {detail.meaningVi}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0 self-start flex-wrap">
          <button
            type="button"
            onClick={() => playSmartAudio({ text: detail.character })}
            className="px-3.5 py-2 rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 hover:scale-105 active:scale-95 transition shadow-2xs flex items-center gap-1.5 font-semibold text-xs cursor-pointer"
            title="Nghe phát âm"
          >
            <Volume2 className="w-4 h-4" />
            <span>Phát âm</span>
          </button>

          <button
            type="button"
            onClick={() => setIsPracticeOpen(true)}
            className="px-3.5 py-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-white hover:scale-105 active:scale-95 transition shadow-2xs flex items-center gap-1.5 font-semibold text-xs cursor-pointer"
            title="Luyện viết & Chấm điểm nét AI"
          >
            <PenTool className="w-4 h-4" />
            <span>Luyện viết</span>
          </button>

          <button
            type="button"
            onClick={handleAddToSrs}
            disabled={isAddedToSrs || addToSrs.isPending}
            className={`px-3.5 py-2 rounded-xl hover:scale-105 active:scale-95 transition shadow-2xs flex items-center gap-1.5 font-semibold text-xs ${
              isAddedToSrs
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 cursor-default'
                : 'bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 cursor-pointer'
            }`}
            title={isAddedToSrs ? 'Đã thêm vào SRS' : 'Thêm vào SRS để ôn tập'}
          >
            {isAddedToSrs ? <Check className="w-4 h-4 text-emerald-600" /> : <BookmarkPlus className="w-4 h-4 text-rose-600" />}
            <span>{isAddedToSrs ? 'Đã lưu SRS' : 'Lưu SRS'}</span>
          </button>

          <button
            type="button"
            onClick={() => setIsAddToNotebookOpen(true)}
            className="px-3.5 py-2 rounded-xl bg-gray-100 hover:bg-indigo-50 hover:text-indigo-600 text-gray-700 hover:scale-105 active:scale-95 transition shadow-2xs flex items-center gap-1.5 font-semibold text-xs cursor-pointer"
            title="Thêm chữ Hán vào sổ tay"
          >
            <Plus className="w-4 h-4 text-gray-700" />
            <span>Sổ tay</span>
          </button>
        </div>
      </div>

      {/* 2. Khung vẽ nét SVG (Stroke Animation) + Thông số bộ thủ & âm On/Kun */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-indigo-50/40 p-5 rounded-md border border-indigo-100">
        <div className="flex flex-col items-center justify-center">
          <KanjiStrokeViewer
            character={detail.character}
            strokes={detail.strokes || []}
            size={180}
            meaningVi={detail.meaningVi}
            hanViet={detail.hanViet}
            onPractice={() => setIsPracticeOpen(true)}
            showPracticeButton={false}
          />
        </div>

        <div className="space-y-4 text-sm justify-center flex flex-col">
          <div className="grid grid-cols-2 gap-2.5">
            <div className="bg-white p-3 rounded-md border border-gray-200/80 shadow-2xs">
              <span className="text-xs text-gray-400 block font-medium">Số nét</span>
              <span className="text-lg font-black text-indigo-600">
                {detail.strokeCount || detail.strokes?.length || 0} nét
              </span>
            </div>
            <div className="bg-white p-3 rounded-md border border-gray-200/80 shadow-2xs">
              <span className="text-xs text-gray-400 block font-medium">Bộ thủ</span>
              <span className="text-lg font-bold text-gray-800">
                {detail.radicals && detail.radicals.length > 0
                  ? detail.radicals.join(', ')
                  : 'Đang cập nhật'}
              </span>
            </div>
          </div>

          {/* Âm On (Onyomi) */}
          <div>
            <span className="text-xs font-bold text-indigo-700 uppercase tracking-wider block mb-1">
              Âm On (Onyomi - Katakana)
            </span>
            <div className="flex flex-wrap gap-1.5">
              {detail.onyomi && detail.onyomi.length > 0 ? (
                detail.onyomi.map((on, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => playSmartAudio({ text: on })}
                    className="px-2.5 py-1 bg-white hover:bg-indigo-50 text-indigo-900 font-bold rounded border border-indigo-200 text-xs flex items-center gap-1 transition"
                  >
                    {on} <Volume2 className="w-3 h-3 text-indigo-400" />
                  </button>
                ))
              ) : (
                <span className="text-xs text-gray-400">Không có</span>
              )}
            </div>
          </div>

          {/* Âm Kun (Kunyomi) */}
          <div>
            <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider block mb-1">
              Âm Kun (Kunyomi - Hiragana)
            </span>
            <div className="flex flex-wrap gap-1.5">
              {detail.kunyomi && detail.kunyomi.length > 0 ? (
                detail.kunyomi.map((kun, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => playSmartAudio({ text: kun })}
                    className="px-2.5 py-1 bg-white hover:bg-emerald-50 text-emerald-900 font-bold rounded border border-emerald-200 text-xs flex items-center gap-1 transition"
                  >
                    {kun} <Volume2 className="w-3 h-3 text-emerald-400" />
                  </button>
                ))
              ) : (
                <span className="text-xs text-gray-400">Không có</span>
              )}
            </div>
          </div>

          {/* Bộ & Kanji Breakdown (tương tự giao diện Mazii) */}
          {((detail.compDetails && detail.compDetails.length > 0) || (detail.compGraph?.nodes && detail.compGraph.nodes.length > 0)) && (
            <div className="pt-2 border-t border-indigo-100">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                  Bộ
                </span>
                <button
                  type="button"
                  onClick={() => setIsBreakdownOpen(true)}
                  className="inline-flex items-center gap-1.5 text-xs font-bold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                  title="Xem sơ đồ phân rã Kanji Breakdown"
                >
                  <Network className="w-3.5 h-3.5" />
                  <span>Kanji Breakdown</span>
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {detail.compDetails?.map((comp, idx) => {
                  const compTooltip = (
                    <div className="max-w-xs text-left p-1 text-xs leading-relaxed space-y-1">
                      <div className="font-bold text-sm text-amber-300 flex items-center gap-1.5">
                        <span>[{comp.w}]</span>
                        {comp.h && <span>[{comp.h}]</span>}
                      </div>
                      {comp.d ? (
                        <div className="text-gray-100 whitespace-pre-line text-[11px] leading-5 max-h-40 overflow-y-auto pr-1">
                          {comp.d}
                        </div>
                      ) : (
                        <div className="text-gray-300 text-[11px]">
                          {comp.h ? `Thành phần Hán tự: ${comp.h}` : 'Bộ thủ / nét chữ'}
                        </div>
                      )}
                    </div>
                  )
                  return (
                    <Tooltip
                      key={idx}
                      title={compTooltip}
                      placement="top"
                      color="#1e293b"
                      overlayInnerStyle={{ padding: '8px 12px', borderRadius: '8px' }}
                    >
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-white hover:bg-indigo-50 hover:border-indigo-300 text-gray-800 rounded border border-gray-200/90 text-xs shadow-2xs cursor-pointer transition">
                        <span className="font-serif font-bold text-base text-gray-900 leading-none">{comp.w}</span>
                        {comp.h && (
                          <span className="text-[11px] font-extrabold text-amber-700 uppercase tracking-wide">
                            {comp.h}
                          </span>
                        )}
                      </span>
                    </Tooltip>
                  )
                })}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 3. Từ vựng liên quan ghép từ chữ Hán (Gom nhóm theo âm đọc kiểu Mazii) */}
      <div className="space-y-3">
        <h3 className="font-bold text-base text-gray-900 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-indigo-600" />
          Từ vựng ghép chứa chữ {detail.character} (Gom nhóm theo âm đọc)
        </h3>

        {detail.wordsByReading && Object.keys(detail.wordsByReading).length > 0 ? (
          <div className="space-y-3">
            {Object.entries(detail.wordsByReading).map(([reading, words]) => {
              const isExpanded = !!expandedReadings[reading]
              const displayedWords = isExpanded ? words : words.slice(0, 4)

              return (
                <div key={reading} className="bg-slate-50/80 rounded-md p-4 border border-gray-200/80">
                  <div className="flex items-center justify-between mb-2.5">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded-md bg-indigo-600 text-white font-bold text-xs">
                        Âm đọc: {reading}
                      </span>
                      <span className="text-xs text-gray-500 font-medium">({words.length} từ tiêu biểu)</span>
                    </div>

                    {words.length > 4 && (
                      <button
                        type="button"
                        onClick={() =>
                          setExpandedReadings((prev) => ({
                            ...prev,
                            [reading]: !prev[reading],
                          }))
                        }
                        className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold cursor-pointer"
                      >
                        {isExpanded ? 'Thu gọn' : `+ Xem thêm (${words.length - 4})`}
                      </button>
                    )}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {displayedWords.map((w, idx) => (
                      <div
                        key={idx}
                        onClick={() => onSelectVocab?.(w.vocabId, w.word)}
                        className="bg-white p-3 rounded-md border border-gray-200/80 hover:border-indigo-400 hover:shadow-xs transition cursor-pointer flex items-center justify-between group"
                      >
                        <div>
                          <div className="flex items-baseline gap-2">
                            <span className="font-bold text-gray-900 text-base group-hover:text-indigo-600 transition">
                              {w.word}
                            </span>
                            <span className="text-xs text-gray-500 font-sans">【{w.hiragana}】</span>
                            {w.hanViet && (
                              <span className="text-[10px] font-semibold text-amber-600 uppercase">
                                {w.hanViet}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-gray-600 mt-0.5 line-clamp-1">{w.meaningVi}</p>
                        </div>

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation()
                            playSmartAudio({ text: w.word })
                          }}
                          className="p-1.5 text-gray-400 hover:text-indigo-600 rounded hover:bg-gray-100 transition shrink-0"
                        >
                          <Volume2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="text-center py-6 text-sm text-gray-500 bg-gray-50 rounded-md">
            Chưa có từ ghép liên kết cho chữ Hán này.
          </div>
        )}
      </div>

      <KanjiPracticeModal
        isOpen={isPracticeOpen}
        onClose={() => setIsPracticeOpen(false)}
        character={detail.character}
        meaningVi={detail.meaningVi}
        hanViet={detail.hanViet}
        strokes={detail.strokes || []}
      />

      <KanjiBreakdownModal
        open={isBreakdownOpen}
        character={detail.character}
        hanViet={detail.hanViet}
        meaningVi={detail.meaningVi}
        compGraph={detail.compGraph}
        compDetails={detail.compDetails}
        onClose={() => setIsBreakdownOpen(false)}
      />

      <AddWordToNotebookModal
        open={isAddToNotebookOpen}
        wordData={kanjiWordData}
        onClose={() => setIsAddToNotebookOpen(false)}
      />
    </div>
  )
}
