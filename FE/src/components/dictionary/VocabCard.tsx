import React, { useState } from 'react'
import { Volume2, BookmarkPlus, Check, ChevronDown, ChevronUp, BookOpen } from 'lucide-react'
import type { VocabularySummary, VocabularyDetail } from '@/types'
import { playSmartAudio } from '@/utils'
import { dictionaryService } from '@/services'

interface VocabCardProps {
  vocab: VocabularySummary
  onSelectKanji?: (character: string) => void
}

export const VocabCard: React.FC<VocabCardProps> = ({ vocab, onSelectKanji }) => {
  const [detail, setDetail] = useState<VocabularyDetail | null>(null)
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false)
  const [isAddedToSrs, setIsAddedToSrs] = useState<boolean>(false)
  const [isAddingSrs, setIsAddingSrs] = useState<boolean>(false)

  const handlePlayAudio = (e: React.MouseEvent) => {
    e.stopPropagation()
    playSmartAudio({
      text: vocab.word || vocab.hiragana,
      audioUrl: vocab.audioUrl,
    })
  }

  const handleToggleExpand = async () => {
    if (!isExpanded && !detail) {
      setIsLoadingDetail(true)
      try {
        const res = await dictionaryService.getVocabularyDetail(vocab.id)
        if (res.data) {
          setDetail(res.data)
        }
      } catch (err) {
        console.error('Không thể tải chi tiết từ vựng:', err)
      } finally {
        setIsLoadingDetail(false)
      }
    }
    setIsExpanded((prev) => !prev)
  }

  const handleAddToSrs = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (isAddedToSrs || isAddingSrs) return
    setIsAddingSrs(true)
    try {
      await dictionaryService.addToSrs('VOCABULARY', vocab.id)
      setIsAddedToSrs(true)
    } catch (err: any) {
      // Nếu đã có sẵn trong SRS
      if (err?.response?.data?.code === 3002) {
        setIsAddedToSrs(true)
      } else {
        console.error('Lỗi khi thêm vào SRS:', err)
      }
    } finally {
      setIsAddingSrs(false)
    }
  }

  return (
    <div className="bg-white rounded-2xl border border-gray-200/80 hover:border-indigo-300 hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Phần tóm tắt đầu thẻ */}
      <div className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex-1">
          {/* Hàng badge & cấp độ */}
          <div className="flex items-center gap-2 mb-1.5 flex-wrap">
            {vocab.jlptLevel && (
              <span className="px-2 py-0.5 text-xs font-bold rounded-md bg-indigo-50 text-indigo-600 border border-indigo-200/60">
                {vocab.jlptLevel}
              </span>
            )}
            {vocab.hanViet && (
              <span className="px-2 py-0.5 text-xs font-semibold rounded-md bg-amber-50 text-amber-700 border border-amber-200/60 uppercase">
                {vocab.hanViet}
              </span>
            )}
            {vocab.partOfSpeech && (
              <span className="px-2 py-0.5 text-xs font-medium rounded-md bg-gray-100 text-gray-600">
                {vocab.partOfSpeech}
              </span>
            )}
          </div>

          {/* Từ vựng chính & Hiragana */}
          <div className="flex items-baseline gap-3 flex-wrap">
            <span className="text-2xl font-bold text-gray-900 tracking-tight font-sans">
              {vocab.word}
            </span>
            <span className="text-base text-gray-500 font-medium">
              【{vocab.hiragana}】
            </span>
            {vocab.romaji && (
              <span className="text-xs text-gray-400 font-mono">
                /{vocab.romaji}/
              </span>
            )}
          </div>

          {/* Nghĩa tiếng Việt */}
          <p className="mt-2 text-base font-semibold text-indigo-950">
            {vocab.meaningVi}
          </p>
        </div>

        {/* Nút hành động */}
        <div className="flex items-center gap-2 self-start md:self-center">
          <button
            type="button"
            onClick={handlePlayAudio}
            className="p-2.5 rounded-xl bg-indigo-50 text-indigo-600 hover:bg-indigo-100 hover:scale-105 active:scale-95 transition"
            title="Nghe phát âm"
          >
            <Volume2 className="w-5 h-5" />
          </button>

          <button
            type="button"
            onClick={handleAddToSrs}
            disabled={isAddedToSrs || isAddingSrs}
            className={`px-3 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition ${
              isAddedToSrs
                ? 'bg-emerald-50 text-emerald-600 border border-emerald-200'
                : 'bg-gray-100 hover:bg-indigo-600 hover:text-white text-gray-700'
            }`}
            title="Thêm vào sổ tay SRS"
          >
            {isAddedToSrs ? (
              <>
                <Check className="w-4 h-4" /> Đã lưu SRS
              </>
            ) : (
              <>
                <BookmarkPlus className="w-4 h-4" /> Thêm SRS
              </>
            )}
          </button>

          <button
            type="button"
            onClick={handleToggleExpand}
            className="p-2.5 rounded-xl text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition"
            title={isExpanded ? 'Thu gọn' : 'Xem thêm chi tiết & ví dụ'}
          >
            {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Chi tiết mở rộng (Senses, Examples, Kanji Components) */}
      {isExpanded && (
        <div className="bg-slate-50/70 border-t border-gray-100 p-5 space-y-4">
          {isLoadingDetail ? (
            <div className="text-center py-4 text-sm text-gray-500 animate-pulse">
              Đang tải chi tiết & ví dụ...
            </div>
          ) : (
            <>
              {/* Các chữ Hán cấu thành từ */}
              {detail?.kanjiComponents && detail.kanjiComponents.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                    Chữ Hán cấu thành (Bấm để tra Kanji)
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {detail.kanjiComponents.map((k) => (
                      <button
                        key={k.id}
                        type="button"
                        onClick={() => onSelectKanji?.(k.character)}
                        className="flex items-center gap-2 px-3 py-1.5 bg-white rounded-xl border border-gray-200 hover:border-indigo-400 hover:bg-indigo-50/50 shadow-sm transition group"
                      >
                        <span className="text-lg font-bold text-gray-900 group-hover:text-indigo-600">
                          {k.character}
                        </span>
                        <div className="text-left text-xs">
                          <span className="font-semibold text-amber-600 block uppercase">
                            {k.hanViet || 'Hán tự'}
                          </span>
                          <span className="text-gray-500 truncate max-w-[120px] block">
                            {k.meaningVi}
                          </span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Các tầng nghĩa bổ sung (Senses) */}
              {detail?.senses && detail.senses.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                    Các nghĩa chi tiết ({detail.senses.length})
                  </h4>
                  <div className="space-y-2">
                    {detail.senses.map((sense, idx) => (
                      <div key={idx} className="bg-white p-3 rounded-xl border border-gray-200/80 text-sm">
                        <div className="flex items-baseline gap-2">
                          <span className="w-5 h-5 rounded-full bg-indigo-100 text-indigo-700 text-xs font-bold flex items-center justify-center">
                            {sense.senseNo || idx + 1}
                          </span>
                          <span className="font-semibold text-gray-800">{sense.meaningVi}</span>
                          {sense.partOfSpeech && (
                            <span className="text-xs text-gray-400">({sense.partOfSpeech})</span>
                          )}
                        </div>
                        {sense.usageNotes && (
                          <p className="mt-1 text-xs text-gray-500 pl-7">{sense.usageNotes}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Câu ví dụ song ngữ (Examples) */}
              {((detail?.examples && detail.examples.length > 0) || vocab.exampleJp) && (
                <div>
                  <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4 text-indigo-500" /> Câu ví dụ minh họa
                  </h4>
                  <div className="space-y-2.5">
                    {detail?.examples && detail.examples.length > 0 ? (
                      detail.examples.map((ex, idx) => (
                        <div
                          key={idx}
                          className="bg-white p-3.5 rounded-xl border border-gray-200/80 shadow-xs flex items-start justify-between gap-3 group"
                        >
                          <div>
                            <p className="text-sm font-semibold text-gray-900 group-hover:text-indigo-600 transition">
                              {ex.exampleJp}
                            </p>
                            <p className="text-xs text-gray-600 mt-1">{ex.exampleVi}</p>
                          </div>
                          <button
                            type="button"
                            onClick={() =>
                              playSmartAudio({
                                text: ex.exampleJp,
                                audioUrl: ex.audioUrl,
                              })
                            }
                            className="p-1.5 text-gray-400 hover:text-indigo-600 rounded-lg hover:bg-gray-100 transition shrink-0"
                            title="Nghe câu ví dụ"
                          >
                            <Volume2 className="w-4 h-4" />
                          </button>
                        </div>
                      ))
                    ) : (
                      <div className="bg-white p-3.5 rounded-xl border border-gray-200/80 shadow-xs flex items-start justify-between gap-3">
                        <div>
                          <p className="text-sm font-semibold text-gray-900">{vocab.exampleJp}</p>
                          <p className="text-xs text-gray-600 mt-1">{vocab.exampleVi}</p>
                        </div>
                        <button
                          type="button"
                          onClick={() =>
                            playSmartAudio({
                              text: vocab.exampleJp || '',
                            })
                          }
                          className="p-1.5 text-gray-400 hover:text-indigo-600 rounded-lg hover:bg-gray-100 transition shrink-0"
                          title="Nghe câu ví dụ"
                        >
                          <Volume2 className="w-4 h-4" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
