import React, { useState, useEffect } from 'react'
import { X, Volume2, BookmarkPlus, Check, Sparkles } from 'lucide-react'
import type { KanjiDetail } from '@/types'
import { dictionaryService } from '@/services'
import { speakJapanese } from '@/utils'
import { KanjiStrokeViewer } from './KanjiStrokeViewer'

interface KanjiDetailModalProps {
  character: string | null
  onClose: () => void
  onSelectVocab?: (vocabId: number) => void
}

export const KanjiDetailModal: React.FC<KanjiDetailModalProps> = ({
  character,
  onClose,
  onSelectVocab,
}) => {
  const [detail, setDetail] = useState<KanjiDetail | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [isAddedToSrs, setIsAddedToSrs] = useState<boolean>(false)

  useEffect(() => {
    if (!character) {
      setDetail(null)
      return
    }

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

  if (!character) return null

  const handleAddToSrs = async () => {
    if (!detail || isAddedToSrs) return
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
      <div className="bg-white rounded-3xl shadow-2xl max-w-3xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        {/* Header Modal */}
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-slate-50">
          <div className="flex items-center gap-3">
            <span className="text-3xl font-extrabold text-gray-900 font-serif">
              {character}
            </span>
            {detail && (
              <span className="px-3 py-1 rounded-lg bg-amber-50 text-amber-700 font-bold uppercase text-sm border border-amber-200">
                {detail.hanViet || 'Hán tự'}
              </span>
            )}
            {detail?.jlptLevel && (
              <span className="px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-700 font-bold text-xs border border-indigo-200">
                {detail.jlptLevel}
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleAddToSrs}
              disabled={isAddedToSrs}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition ${
                isAddedToSrs
                  ? 'bg-emerald-50 text-emerald-600 border border-emerald-200'
                  : 'bg-indigo-600 text-white hover:bg-indigo-700'
              }`}
            >
              {isAddedToSrs ? <Check className="w-4 h-4" /> : <BookmarkPlus className="w-4 h-4" />}
              {isAddedToSrs ? 'Đã lưu SRS' : 'Lưu vào SRS'}
            </button>

            <button
              type="button"
              onClick={onClose}
              className="p-2 text-gray-400 hover:text-gray-700 hover:bg-gray-200/60 rounded-xl transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Body Modal */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {isLoading ? (
            <div className="text-center py-16 text-gray-500 animate-pulse">
              Đang tải chi tiết chữ Hán & thứ tự nét viết...
            </div>
          ) : detail ? (
            <>
              {/* Phần 1: Khung nét viết & Thông số Hán tự */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-indigo-50/40 p-5 rounded-2xl border border-indigo-100">
                {/* Cột trái: Trình diễn nét viết */}
                <div className="flex flex-col items-center justify-center">
                  <KanjiStrokeViewer
                    character={detail.character}
                    strokes={detail.strokes || []}
                    size={170}
                  />
                </div>

                {/* Cột phải: Thông số âm On / Kun / Số nét */}
                <div className="space-y-3.5 text-sm">
                  <div>
                    <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">
                      Ý nghĩa
                    </span>
                    <span className="text-base font-bold text-gray-900">{detail.meaningVi}</span>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div className="bg-white p-2.5 rounded-xl border border-gray-200/80">
                      <span className="text-xs text-gray-400 block">Số nét</span>
                      <span className="text-base font-bold text-indigo-600">
                        {detail.strokeCount || detail.strokes?.length || 0} nét
                      </span>
                    </div>
                    <div className="bg-white p-2.5 rounded-xl border border-gray-200/80">
                      <span className="text-xs text-gray-400 block">Bộ thủ</span>
                      <span className="text-base font-bold text-gray-700">
                        {detail.radicals && detail.radicals.length > 0
                          ? detail.radicals.join(', ')
                          : 'Đang cập nhật'}
                      </span>
                    </div>
                  </div>

                  {/* Âm Onyomi */}
                  <div>
                    <span className="text-xs font-bold text-indigo-700 uppercase tracking-wider block mb-1">
                      Âm On (Onyomi)
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {detail.onyomi && detail.onyomi.length > 0 ? (
                        detail.onyomi.map((on, i) => (
                          <button
                            key={i}
                            type="button"
                            onClick={() => speakJapanese(on)}
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

                  {/* Âm Kunyomi */}
                  <div>
                    <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider block mb-1">
                      Âm Kun (Kunyomi)
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {detail.kunyomi && detail.kunyomi.length > 0 ? (
                        detail.kunyomi.map((kun, i) => (
                          <button
                            key={i}
                            type="button"
                            onClick={() => speakJapanese(kun)}
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

              {/* Phần 2: Từ vựng liên quan gom nhóm theo âm đọc (Mazii-style) */}
              <div>
                <div className="flex items-center gap-2 mb-3">
                  <Sparkles className="w-5 h-5 text-indigo-600" />
                  <h3 className="font-bold text-base text-gray-900">
                    Từ vựng liên quan ghép từ chữ {character}
                  </h3>
                </div>

                {detail.wordsByReading && Object.keys(detail.wordsByReading).length > 0 ? (
                  <div className="space-y-4">
                    {Object.entries(detail.wordsByReading).map(([reading, words]) => (
                      <div key={reading} className="bg-slate-50/80 rounded-2xl p-4 border border-gray-200/80">
                        <div className="flex items-center gap-2 mb-2.5">
                          <span className="px-2 py-0.5 rounded-md bg-indigo-600 text-white font-bold text-xs">
                            Âm: {reading}
                          </span>
                          <span className="text-xs text-gray-500">({words.length} từ ghép)</span>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          {words.map((w, idx) => (
                            <div
                              key={idx}
                              onClick={() => onSelectVocab?.(w.vocabId)}
                              className="bg-white p-3 rounded-xl border border-gray-200/70 hover:border-indigo-400 hover:shadow-xs transition cursor-pointer flex items-center justify-between"
                            >
                              <div>
                                <div className="flex items-baseline gap-2">
                                  <span className="font-bold text-gray-900 text-base">{w.word}</span>
                                  <span className="text-xs text-gray-500">【{w.hiragana}】</span>
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
                                  speakJapanese(w.word)
                                }}
                                className="p-1.5 text-gray-400 hover:text-indigo-600 rounded-lg hover:bg-gray-100 transition shrink-0"
                              >
                                <Volume2 className="w-4 h-4" />
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-6 text-sm text-gray-500 bg-gray-50 rounded-2xl">
                    Chưa có từ ghép liên kết cho chữ Hán này trong từ điển.
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="text-center py-10 text-gray-500">Không tìm thấy thông tin chữ Hán.</div>
          )}
        </div>
      </div>
    </div>
  )
}
