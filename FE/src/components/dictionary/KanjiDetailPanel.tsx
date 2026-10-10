import React, { useState, useEffect } from 'react'
import { Volume2, BookmarkPlus, Check, Sparkles, BookOpen } from 'lucide-react'
import type { KanjiDetail } from '@/types'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'
import { KanjiStrokeViewer } from './KanjiStrokeViewer'

interface KanjiDetailPanelProps {
  character: string
  onSelectVocab?: (vocabId: number) => void
}

export const KanjiDetailPanel: React.FC<KanjiDetailPanelProps> = ({ character, onSelectVocab }) => {
  const [detail, setDetail] = useState<KanjiDetail | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [isAddedToSrs, setIsAddedToSrs] = useState<boolean>(false)
  const [expandedReadings, setExpandedReadings] = useState<Record<string, boolean>>({})

  useEffect(() => {
    let isMounted = true
    setIsLoading(true)
    setIsAddedToSrs(false)

    dictionaryService
      .getKanjiDetail(character)
      .then((res) => {
        if (isMounted && res.data) {
          setDetail(res.data)
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
      <div className="bg-white rounded-3xl p-12 border border-gray-200/80 shadow-xs text-center text-gray-500 animate-pulse space-y-3">
        <div className="w-10 h-10 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm font-medium">Đang tải nét vẽ & từ ghép chữ Hán...</p>
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="bg-white rounded-3xl p-10 border border-gray-200 text-center text-gray-500">
        Không tìm thấy thông tin chữ Hán.
      </div>
    )
  }

  const handleAddToSrs = async () => {
    if (isAddedToSrs) return
    try {
      await dictionaryService.addToSrs('KANJI', detail.id)
      setIsAddedToSrs(true)
    } catch (err: any) {
      if (err?.response?.data?.code === 3002) {
        setIsAddedToSrs(true)
      }
    }
  }

  return (
    <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-6 md:p-8 space-y-6">
      {/* 1. Header Chữ Hán */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 border-b border-gray-100 pb-6">
        <div className="space-y-2">
          <div className="flex items-center gap-2 flex-wrap">
            {detail.jlptLevel && (
              <span className="px-2.5 py-1 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200/80">
                JLPT {detail.jlptLevel}
              </span>
            )}
            {detail.hanViet && (
              <span className="px-3 py-1 text-xs font-extrabold rounded-lg bg-amber-50 text-amber-700 border border-amber-200 uppercase tracking-wide">
                Hán Việt: {detail.hanViet}
              </span>
            )}
          </div>

          <div className="flex items-baseline gap-4">
            <h2 className="text-6xl font-black text-gray-900 font-serif">{detail.character}</h2>
            <div>
              <p className="text-2xl font-bold text-indigo-950">{detail.meaningVi}</p>
              {detail.meaningEn && (
                <p className="text-sm text-gray-500">Tiếng Anh: {detail.meaningEn}</p>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0 self-start">
          <button
            type="button"
            onClick={() => playSmartAudio({ text: detail.character })}
            className="p-3.5 rounded-2xl bg-indigo-600 text-white hover:bg-indigo-700 hover:scale-105 active:scale-95 transition shadow-sm flex items-center gap-2 font-semibold text-sm"
            title="Nghe phát âm"
          >
            <Volume2 className="w-5 h-5" />
            <span className="hidden sm:inline">Phát âm</span>
          </button>

          <button
            type="button"
            onClick={handleAddToSrs}
            disabled={isAddedToSrs}
            className={`px-4 py-3.5 rounded-2xl text-sm font-semibold flex items-center gap-2 transition ${
              isAddedToSrs
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                : 'bg-gray-100 hover:bg-gray-200 text-gray-800'
            }`}
          >
            {isAddedToSrs ? <Check className="w-5 h-5" /> : <BookmarkPlus className="w-5 h-5" />}
            <span>{isAddedToSrs ? 'Đã lưu SRS' : 'Lưu SRS'}</span>
          </button>
        </div>
      </div>

      {/* 2. Khung vẽ nét SVG (Stroke Animation) + Thông số bộ thủ & âm On/Kun */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-indigo-50/40 p-5 rounded-2xl border border-indigo-100">
        <div className="flex flex-col items-center justify-center">
          <KanjiStrokeViewer
            character={detail.character}
            strokes={detail.strokes || []}
            size={180}
          />
        </div>

        <div className="space-y-4 text-sm justify-center flex flex-col">
          <div className="grid grid-cols-2 gap-2.5">
            <div className="bg-white p-3 rounded-xl border border-gray-200/80 shadow-2xs">
              <span className="text-xs text-gray-400 block font-medium">Số nét</span>
              <span className="text-lg font-black text-indigo-600">
                {detail.strokeCount || detail.strokes?.length || 0} nét
              </span>
            </div>
            <div className="bg-white p-3 rounded-xl border border-gray-200/80 shadow-2xs">
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
                    className="px-2.5 py-1 bg-white hover:bg-indigo-50 text-indigo-900 font-bold rounded-lg border border-indigo-200 text-xs flex items-center gap-1 transition"
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
                    className="px-2.5 py-1 bg-white hover:bg-emerald-50 text-emerald-900 font-bold rounded-lg border border-emerald-200 text-xs flex items-center gap-1 transition"
                  >
                    {kun} <Volume2 className="w-3 h-3 text-emerald-400" />
                  </button>
                ))
              ) : (
                <span className="text-xs text-gray-400">Không có</span>
              )}
            </div>
          </div>
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
                <div key={reading} className="bg-slate-50/80 rounded-2xl p-4 border border-gray-200/80">
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
                        onClick={() => onSelectVocab?.(w.vocabId)}
                        className="bg-white p-3 rounded-xl border border-gray-200/80 hover:border-indigo-400 hover:shadow-xs transition cursor-pointer flex items-center justify-between group"
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
                          className="p-1.5 text-gray-400 hover:text-indigo-600 rounded-lg hover:bg-gray-100 transition shrink-0"
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
          <div className="text-center py-6 text-sm text-gray-500 bg-gray-50 rounded-2xl">
            Chưa có từ ghép liên kết cho chữ Hán này.
          </div>
        )}
      </div>
    </div>
  )
}
