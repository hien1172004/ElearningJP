import React, { useState, useEffect } from 'react'
import { Volume2, BookmarkPlus, Check, BookOpen, Sparkles } from 'lucide-react'
import type { VocabularyDetail } from '@/types'
import { dictionaryService } from '@/services'
import { playSmartAudio } from '@/utils'

interface VocabDetailPanelProps {
  vocabId: number
  onSelectKanji?: (character: string) => void
}

export const VocabDetailPanel: React.FC<VocabDetailPanelProps> = ({ vocabId, onSelectKanji }) => {
  const [detail, setDetail] = useState<VocabularyDetail | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [isAddedToSrs, setIsAddedToSrs] = useState<boolean>(false)

  useEffect(() => {
    let isMounted = true
    setIsLoading(true)
    setIsAddedToSrs(false)

    dictionaryService
      .getVocabularyDetail(vocabId)
      .then((res) => {
        if (isMounted && res.data) {
          setDetail(res.data)
        }
      })
      .catch((err) => console.error('Lỗi tải chi tiết từ vựng:', err))
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [vocabId])

  if (isLoading) {
    return (
      <div className="bg-white rounded-3xl p-12 border border-gray-200/80 shadow-xs text-center text-gray-500 animate-pulse space-y-3">
        <div className="w-10 h-10 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="text-sm font-medium">Đang tải chi tiết từ vựng kiểu Mazii...</p>
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="bg-white rounded-3xl p-10 border border-gray-200 text-center text-gray-500">
        Không tìm thấy thông tin chi tiết từ vựng.
      </div>
    )
  }

  const handlePlayAudio = () => {
    playSmartAudio({
      text: detail.word || detail.hiragana,
      audioUrl: detail.audioUrl,
    })
  }

  const handleAddToSrs = async () => {
    if (isAddedToSrs) return
    try {
      await dictionaryService.addToSrs('VOCABULARY', detail.id)
      setIsAddedToSrs(true)
    } catch (err: any) {
      if (err?.response?.data?.code === 3002) {
        setIsAddedToSrs(true)
      }
    }
  }

  return (
    <div className="bg-white rounded-3xl border border-gray-200/80 shadow-sm p-6 md:p-8 space-y-6">
      {/* 1. Header từ vựng kiểu Mazii: Từ to bản, Furigana, Hán-Việt, Nút nghe, Nút lưu */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 border-b border-gray-100 pb-6">
        <div className="space-y-2">
          {/* Badges: JLPT, Hán Việt, Từ loại */}
          <div className="flex items-center gap-2 flex-wrap">
            {detail.jlptLevel && (
              <span className="px-2.5 py-1 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200/80">
                JLPT {detail.jlptLevel}
              </span>
            )}
            {detail.hanViet && (
              <span className="px-3 py-1 text-xs font-extrabold rounded-lg bg-amber-50 text-amber-700 border border-amber-200 uppercase tracking-wide">
                {detail.hanViet}
              </span>
            )}
            {detail.partOfSpeech && (
              <span className="px-2.5 py-1 text-xs font-medium rounded-lg bg-gray-100 text-gray-600">
                {detail.partOfSpeech}
              </span>
            )}
          </div>

          {/* Mặt chữ chính & Cách đọc */}
          <div className="flex items-baseline gap-3 flex-wrap">
            <h2 className="text-4xl md:text-5xl font-black text-gray-900 tracking-tight font-sans">
              {detail.word}
            </h2>
            <span className="text-2xl font-bold text-gray-500 font-sans">
              【{detail.hiragana}】
            </span>
            {detail.romaji && (
              <span className="text-sm font-mono text-gray-400">/{detail.romaji}/</span>
            )}
          </div>

          {/* Nghĩa tiếng Việt chính */}
          <div className="pt-1">
            <p className="text-xl font-bold text-indigo-950">{detail.meaningVi}</p>
            {detail.meaningEn && (
              <p className="text-sm text-gray-500 mt-0.5">Tiếng Anh: {detail.meaningEn}</p>
            )}
          </div>
        </div>

        {/* Nút hành động nổi bật */}
        <div className="flex items-center gap-2.5 shrink-0 self-start">
          <button
            type="button"
            onClick={handlePlayAudio}
            className="p-3.5 rounded-2xl bg-indigo-600 text-white hover:bg-indigo-700 hover:scale-105 active:scale-95 transition shadow-sm flex items-center gap-2 font-semibold text-sm"
            title="Nghe phát âm chuẩn"
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

      {/* 2. Mục Hán tự cấu thành (Đặc trưng Mazii - Tra chéo tức thì) */}
      {detail.kanjiComponents && detail.kanjiComponents.length > 0 && (
        <div className="bg-slate-50/80 rounded-2xl p-4 border border-gray-200/80 space-y-3">
          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
            <Sparkles className="w-4 h-4 text-amber-500" /> Chữ Hán cấu thành (Bấm để xem nét vẽ)
          </h4>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2.5">
            {detail.kanjiComponents.map((k) => (
              <button
                key={k.id}
                type="button"
                onClick={() => onSelectKanji?.(k.character)}
                className="flex items-center gap-3 p-3 bg-white rounded-xl border border-gray-200/80 hover:border-indigo-400 hover:bg-indigo-50/40 hover:shadow-xs transition text-left group"
              >
                <div className="w-12 h-12 rounded-lg bg-indigo-50/80 border border-indigo-200/60 flex items-center justify-center text-2xl font-bold text-gray-900 font-serif group-hover:scale-105 group-hover:text-indigo-600 transition shrink-0">
                  {k.character}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="font-extrabold text-amber-700 text-xs uppercase block">
                      {k.hanViet || 'Hán tự'}
                    </span>
                    {k.jlptLevel && (
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-sm bg-gray-100 text-gray-600">
                        {k.jlptLevel}
                      </span>
                    )}
                  </div>
                  <p className="text-xs font-medium text-gray-600 truncate mt-0.5">{k.meaningVi}</p>
                  <span className="text-[10px] text-gray-400 block mt-0.5">{k.strokeCount} nét</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 3. Các tầng nghĩa chi tiết (Senses) */}
      {detail.senses && detail.senses.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
            <BookOpen className="w-4 h-4 text-indigo-600" /> Giải nghĩa chi tiết ({detail.senses.length})
          </h4>

          <div className="space-y-2.5">
            {detail.senses.map((sense, idx) => (
              <div key={idx} className="bg-white p-4 rounded-2xl border border-gray-200/80 space-y-1">
                <div className="flex items-baseline gap-2.5">
                  <span className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 text-xs font-bold flex items-center justify-center shrink-0">
                    {sense.senseNo || idx + 1}
                  </span>
                  <p className="text-base font-bold text-gray-900">{sense.meaningVi}</p>
                  {sense.partOfSpeech && (
                    <span className="text-xs text-gray-400 font-normal">({sense.partOfSpeech})</span>
                  )}
                </div>
                {sense.usageNotes && (
                  <p className="text-xs text-gray-500 pl-8.5">{sense.usageNotes}</p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 4. Danh sách câu ví dụ minh họa song ngữ */}
      {((detail.examples && detail.examples.length > 0) || detail.exampleJp) && (
        <div className="space-y-3">
          <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
            <BookOpen className="w-4 h-4 text-emerald-600" /> Câu ví dụ song ngữ Nhật - Việt
          </h4>

          <div className="space-y-2.5">
            {detail.examples && detail.examples.length > 0 ? (
              detail.examples.map((ex, idx) => (
                <div
                  key={idx}
                  className="bg-slate-50/60 p-4 rounded-2xl border border-gray-200/70 flex items-start justify-between gap-3 hover:bg-indigo-50/30 transition group"
                >
                  <div className="space-y-1">
                    <p className="text-base font-bold text-gray-900 group-hover:text-indigo-600 transition">
                      {ex.exampleJp}
                    </p>
                    {ex.exampleRomaji && (
                      <p className="text-xs font-mono text-gray-400">{ex.exampleRomaji}</p>
                    )}
                    <p className="text-sm text-gray-700">{ex.exampleVi}</p>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      playSmartAudio({
                        text: ex.exampleJp,
                        audioUrl: ex.audioUrl,
                      })
                    }
                    className="p-2 text-gray-400 hover:text-indigo-600 hover:bg-white rounded-xl transition shrink-0"
                    title="Nghe phát âm câu ví dụ"
                  >
                    <Volume2 className="w-5 h-5" />
                  </button>
                </div>
              ))
            ) : (
              <div className="bg-slate-50/60 p-4 rounded-2xl border border-gray-200/70 flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <p className="text-base font-bold text-gray-900">{detail.exampleJp}</p>
                  <p className="text-sm text-gray-700">{detail.exampleVi}</p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    playSmartAudio({
                      text: detail.exampleJp || '',
                    })
                  }
                  className="p-2 text-gray-400 hover:text-indigo-600 hover:bg-white rounded-xl transition shrink-0"
                  title="Nghe câu ví dụ"
                >
                  <Volume2 className="w-5 h-5" />
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
