import React, { useState } from 'react'
import { Volume2, BookmarkPlus, Check, ChevronDown, ChevronUp, BookOpen, Plus } from 'lucide-react'
import type { VocabularySummary, VocabularyDetail } from '@/types'
import { playSmartAudio } from '@/utils'
import { dictionaryService } from '@/services'
import { useAddToSrs, useSrsSavedIds } from '@/hooks'
import { AddWordToNotebookModal, type AddWordTargetData } from '@/components/notebook/AddWordToNotebookModal'

interface VocabCardProps {
  vocab: VocabularySummary
  onSelectKanji?: (character: string) => void
}

export const VocabCard: React.FC<VocabCardProps> = ({ vocab, onSelectKanji }) => {
  const [detail, setDetail] = useState<VocabularyDetail | null>(null)
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const [isLoadingDetail, setIsLoadingDetail] = useState<boolean>(false)
  const { isSaved } = useSrsSavedIds()
  const isAddedToSrs = isSaved('VOCABULARY', vocab.id)
  const [isAddingSrs, setIsAddingSrs] = useState<boolean>(false)
  const addToSrs = useAddToSrs()
  const [isAddToNotebookOpen, setIsAddToNotebookOpen] = useState<boolean>(false)

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
      await addToSrs.mutateAsync({ type: 'VOCABULARY', id: vocab.id })
    } catch {
      /* ignore */
    } finally {
      setIsAddingSrs(false)
    }
  }

  const vocabWordData: AddWordTargetData = {
    word: vocab.word || vocab.hiragana,
    reading: vocab.hiragana || '',
    meaning: vocab.meaningVi || '',
    sinoVietnamese: vocab.hanViet || '',
    partOfSpeech: vocab.partOfSpeech || 'Từ vựng',
    audioUrl: vocab.audioUrl,
  }

  return (
    <div className="bg-white rounded-md border border-gray-200/80 hover:border-indigo-300 hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Phần tóm tắt đầu thẻ */}
      <div className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex-1">
          {/* Hàng badge & cấp độ */}
          <div className="flex items-center gap-2 mb-1.5 flex-wrap">
            {vocab.jlptLevel && (
              <span className="px-2.5 py-0.5 text-xs font-bold rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200/60">
                {vocab.jlptLevel}
              </span>
            )}
            {vocab.partOfSpeech && (
              <span className="px-2 py-0.5 text-xs font-medium rounded-md bg-gray-100 text-gray-600">
                {vocab.partOfSpeech}
              </span>
            )}
            {vocab.hanViet && (
              <span className="text-xs font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded border border-amber-200/60 uppercase">
                {vocab.hanViet}
              </span>
            )}
          </div>

          {/* Từ viết & cách đọc */}
          <div className="flex items-baseline gap-3">
            <h3 className="text-2xl font-black text-gray-900 font-sans tracking-wide">
              {vocab.word}
            </h3>
            <span className="text-sm font-medium text-gray-500 font-sans">
              【{vocab.hiragana}】
            </span>
            {vocab.romaji && (
              <span className="text-xs text-gray-400 font-mono hidden sm:inline">
                /{vocab.romaji}/
              </span>
            )}
          </div>

          {/* Nghĩa tiếng Việt ngắn */}
          <p className="text-sm text-gray-700 font-medium mt-1 line-clamp-1">
            👉 {vocab.meaningVi}
          </p>
        </div>

        {/* Nút hành động */}
        <div className="flex items-center gap-2 self-start md:self-center">
          <button
            type="button"
            onClick={handlePlayAudio}
            className="p-2.5 text-indigo-600 bg-indigo-50 hover:bg-indigo-600 hover:text-white rounded-md transition duration-150 cursor-pointer shadow-2xs"
            title="Nghe phát âm"
          >
            <Volume2 className="w-5 h-5" />
          </button>

          <button
            type="button"
            onClick={handleAddToSrs}
            disabled={isAddedToSrs || isAddingSrs}
            className={`px-3 py-2 rounded-md text-xs font-semibold flex items-center gap-1.5 transition ${
              isAddedToSrs
                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 cursor-default'
                : 'bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 cursor-pointer'
            }`}
            title={isAddedToSrs ? 'Đã thêm vào SRS' : 'Thêm vào SRS để ôn tập'}
          >
            {isAddedToSrs ? <Check className="w-4 h-4" /> : <BookmarkPlus className="w-4 h-4" />}
            <span>{isAddedToSrs ? 'Đã lưu SRS' : 'Lưu SRS'}</span>
          </button>

          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              setIsAddToNotebookOpen(true)
            }}
            className="px-3 py-2 rounded-md text-xs font-semibold flex items-center gap-1.5 transition bg-gray-100 hover:bg-indigo-50 hover:text-indigo-600 text-gray-700 cursor-pointer"
            title="Thêm vào sổ tay"
          >
            <Plus className="w-4 h-4" />
            <span>Sổ tay</span>
          </button>

          <button
            type="button"
            onClick={handleToggleExpand}
            className="p-2.5 rounded-md text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition cursor-pointer"
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
              {/* Ý nghĩa chi tiết */}
              {detail?.senses && detail.senses.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
                    Các tầng ý nghĩa
                  </h4>
                  <div className="space-y-2">
                    {detail.senses.map((sense, idx) => (
                      <div
                        key={idx}
                        className="bg-white p-3 rounded-md border border-gray-200/80 text-sm shadow-2xs"
                      >
                        <div className="font-semibold text-gray-800">
                          {idx + 1}. {sense.meaningVi}
                        </div>
                        {sense.meaningEn && (
                          <div className="text-xs text-gray-400 mt-0.5">
                            EN: {sense.meaningEn}
                          </div>
                        )}
                        {sense.usageNotes && (
                          <div className="text-xs text-gray-600 mt-1 italic">
                            {sense.usageNotes}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Chữ Hán cấu thành */}
              {detail?.kanjiComponents && detail.kanjiComponents.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
                    Chữ Hán cấu thành
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {detail.kanjiComponents.map((k) => (
                      <button
                        key={k.id}
                        type="button"
                        onClick={() => onSelectKanji?.(k.character)}
                        className="px-3 py-1.5 bg-white border border-gray-200/90 rounded-md text-xs font-bold text-indigo-700 hover:bg-indigo-50 hover:border-indigo-300 transition flex items-center gap-1.5 shadow-2xs cursor-pointer"
                      >
                        <span className="text-base font-serif font-black">{k.character}</span>
                        {k.hanViet && (
                          <span className="text-[10px] text-amber-700 uppercase font-sans">
                            {k.hanViet}
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Câu ví dụ mẫu */}
              {((detail?.examples && detail.examples.length > 0) ||
                ('exampleJp' in vocab && Boolean(vocab.exampleJp))) && (
                <div>
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4 text-indigo-600" /> Câu ví dụ mẫu
                  </h4>
                  <div className="space-y-2.5">
                    {detail?.examples && detail.examples.length > 0 ? (
                      detail.examples.map((ex, idx) => (
                        <div
                          key={idx}
                          className="bg-white p-3.5 rounded-md border border-gray-200/80 shadow-2xs flex items-start justify-between gap-3 group"
                        >
                          <div>
                            <p className="text-sm font-semibold text-gray-900 group-hover:text-indigo-600 transition">
                              {ex.exampleJp}
                            </p>
                            <p className="text-xs text-gray-600 mt-1">{ex.exampleVi}</p>
                            {ex.exampleRomaji && (
                              <p className="text-[11px] text-gray-400 font-mono mt-0.5">
                                {ex.exampleRomaji}
                              </p>
                            )}
                          </div>
                          <button
                            type="button"
                            onClick={() =>
                              playSmartAudio({
                                text: ex.exampleJp,
                                audioUrl: ex.audioUrl,
                              })
                            }
                            className="p-1.5 text-gray-400 hover:text-indigo-600 rounded hover:bg-gray-100 transition shrink-0 cursor-pointer"
                            title="Nghe câu ví dụ"
                          >
                            <Volume2 className="w-4 h-4" />
                          </button>
                        </div>
                      ))
                    ) : (
                      <div className="bg-white p-3.5 rounded-md border border-gray-200/80 shadow-2xs flex items-start justify-between gap-3">
                        <div>
                          <p className="text-sm font-semibold text-gray-900">
                            {'exampleJp' in vocab ? (vocab.exampleJp as string) : ''}
                          </p>
                          <p className="text-xs text-gray-600 mt-1">
                            {'exampleVi' in vocab ? (vocab.exampleVi as string) : ''}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() =>
                            playSmartAudio({
                              text:
                                'exampleJp' in vocab
                                  ? (vocab.exampleJp as string) || ''
                                  : '',
                            })
                          }
                          className="p-1.5 text-gray-400 hover:text-indigo-600 rounded hover:bg-gray-100 transition shrink-0 cursor-pointer"
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

      <AddWordToNotebookModal
        open={isAddToNotebookOpen}
        wordData={vocabWordData}
        onClose={() => setIsAddToNotebookOpen(false)}
      />
    </div>
  )
}
