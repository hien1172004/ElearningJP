import { useBodyScrollLock } from '@/hooks/useBodyScrollLock'
import React, { useState, useEffect, useRef } from 'react'
import { Volume2, BookOpen, X } from 'lucide-react'
import type { KanaMatrix, KanaCharacter, CharacterType } from '@/types'
import { dictionaryService } from '@/services'
import { playSmartAudio, stopSpeaking } from '@/utils'
import { KanjiStrokeViewer } from './KanjiStrokeViewer'

export const KanaMatrixTab: React.FC = () => {
  const [charType, setCharType] = useState<CharacterType>('HIRAGANA')
  const [section, setSection] = useState<'seion' | 'dakuon' | 'yoon'>('seion')
  const [matrixData, setMatrixData] = useState<KanaMatrix | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)

  // Trạng thái ký tự đang được click để xem nét viết modal
  const [selectedKana, setSelectedKana] = useState<KanaCharacter | null>(null)
  // Khoá cuộn trang phía sau khi lớp phủ tự dựng đang mở
  useBodyScrollLock(!!selectedKana)

  // Trạng thái chữ đang được đọc tự động (phục vụ highlight ô chữ khi đọc cả hàng)
  const [activeReadingChar, setActiveReadingChar] = useState<string | null>(null)
  const readingQueueRef = useRef<{ cancel: boolean }>({ cancel: false })

  useEffect(() => {
    let isMounted = true
    setIsLoading(true)
    dictionaryService
      .getKanaMatrix(charType)
      .then((res) => {
        if (isMounted && res.data) {
          setMatrixData(res.data)
        }
      })
      .catch((err) => console.error('Lỗi tải bảng chữ cái:', err))
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
      readingQueueRef.current.cancel = true
      stopSpeaking()
    }
  }, [charType])

  // Click vào từng chữ cái: Phát âm ngay và mở chi tiết
  const handleCharClick = (kana: KanaCharacter) => {
    readingQueueRef.current.cancel = true
    stopSpeaking()
    setActiveReadingChar(kana.character)
    playSmartAudio({
      text: kana.character,
      audioUrl: kana.audioUrl,
      onEnd: () => setActiveReadingChar(null),
    })
    setSelectedKana(kana)
  }

  // Đọc tuần tự cả hàng (ví dụ: a -> i -> u -> e -> o) kèm highlight từng ô
  const handleReadRow = (_rowRomaji: string, characters: KanaCharacter[]) => {
    readingQueueRef.current.cancel = true
    stopSpeaking()

    if (!characters || characters.length === 0) return

    const currentToken = { cancel: false }
    readingQueueRef.current = currentToken

    let index = 0

    const playNext = () => {
      if (currentToken.cancel || index >= characters.length) {
        setActiveReadingChar(null)
        return
      }

      const currentChar = characters[index]
      setActiveReadingChar(currentChar.character)

      playSmartAudio({
        text: currentChar.character,
        audioUrl: currentChar.audioUrl,
        onEnd: () => {
          if (!currentToken.cancel) {
            index++
            // Nghỉ 400ms giữa các chữ trong hàng để nghe rõ
            setTimeout(playNext, 400)
          }
        },
      })
    }

    playNext()
  }

  const currentRows = matrixData
    ? section === 'seion'
      ? matrixData.seion
      : section === 'dakuon'
      ? matrixData.dakuon
      : matrixData.yoon
    : []

  return (
    <div className="space-y-6">
      {/* Thanh công cụ chọn loại chữ và nhóm âm */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-surface p-3.5 rounded-2xl border border-suna/90 shadow-xs">
        {/* Chuyển đổi Hiragana / Katakana */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100/80 rounded-xl">
          <button
            type="button"
            onClick={() => setCharType('HIRAGANA')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              charType === 'HIRAGANA'
                ? 'bg-surface text-ai shadow-2xs font-extrabold'
                : 'text-nibi hover:text-sumi'
            }`}
          >
            🌸 Hiragana (Chữ mềm)
          </button>
          <button
            type="button"
            onClick={() => setCharType('KATAKANA')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              charType === 'KATAKANA'
                ? 'bg-surface text-ai shadow-2xs font-extrabold'
                : 'text-nibi hover:text-sumi'
            }`}
          >
            ⚡ Katakana (Chữ cứng)
          </button>
        </div>

        {/* Lọc: Cơ bản / Âm đục / Âm ghép */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <button
            type="button"
            onClick={() => setSection('seion')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer ${
              section === 'seion'
                ? 'bg-ai text-white shadow-2xs font-bold'
                : 'text-nibi hover:text-ai hover:bg-kamenozoki/50'
            }`}
          >
            Âm cơ bản (50 âm)
          </button>
          <button
            type="button"
            onClick={() => setSection('dakuon')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer ${
              section === 'dakuon'
                ? 'bg-ai text-white shadow-2xs font-bold'
                : 'text-nibi hover:text-ai hover:bg-kamenozoki/50'
            }`}
          >
            Âm đục & Bán đục
          </button>
          <button
            type="button"
            onClick={() => setSection('yoon')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer ${
              section === 'yoon'
                ? 'bg-ai text-white shadow-2xs font-bold'
                : 'text-nibi hover:text-ai hover:bg-kamenozoki/50'
            }`}
          >
            Âm ghép (Yōon)
          </button>
        </div>
      </div>

      {/* Lưới Ma trận bảng chữ cái */}
      {isLoading ? (
        <div className="bg-surface rounded-2xl p-12 text-center text-nibi animate-pulse border border-suna">
          Đang tải ma trận bảng chữ cái...
        </div>
      ) : (
        <div className="space-y-4">
          {currentRows.map((row, rIdx) => (
            <div
              key={rIdx}
              className="bg-surface rounded-2xl p-5 border border-suna/90 shadow-xs flex flex-col md:flex-row md:items-center gap-4 hover:border-ai/30 transition-all duration-200"
            >
              {/* Tên hàng & Nút đọc cả hàng */}
              <div className="md:w-52 shrink-0 flex items-center justify-between md:justify-start gap-3 border-b md:border-b-0 pb-3 md:pb-0 border-gray-100">
                <div>
                  <h4 className="font-bold text-sumi text-sm">{row.rowName}</h4>
                  <p className="text-xs text-nibi font-mono">({row.rowRomaji})</p>
                </div>

                <button
                  type="button"
                  onClick={() => handleReadRow(row.rowRomaji, row.characters)}
                  className="px-2.5 py-1.5 rounded-lg bg-kamenozoki/70 text-ai hover:bg-ai hover:text-white border border-ai/20 transition-all flex items-center gap-1.5 text-xs font-medium shrink-0 group cursor-pointer shadow-2xs"
                  title="Đọc phát âm cả hàng này"
                >
                  <Volume2 className="w-3.5 h-3.5 group-hover:scale-110 transition-transform" />
                  <span>Đọc hàng</span>
                </button>
              </div>

              {/* Các ô chữ trong hàng */}
              <div className="flex-1 grid grid-cols-5 gap-2.5 sm:gap-3">
                {row.characters.map((char) => {
                  const isActive = activeReadingChar === char.character
                  return (
                    <div
                      key={char.id}
                      onClick={() => handleCharClick(char)}
                      className={`relative aspect-square sm:aspect-4/3 rounded-xl border p-2 sm:p-3 flex flex-col items-center justify-center cursor-pointer transition-all duration-200 select-none group ${
                        isActive
                          ? 'bg-kamenozoki border-ai ring-2 ring-ai/30 scale-105 shadow-sm'
                          : 'bg-kinari/60 border-suna/80 hover:bg-surface hover:border-ai/50 hover:shadow-xs hover:scale-[1.02]'
                      }`}
                    >
                      <span className="text-2xl sm:text-3xl font-extrabold text-sumi font-sans">
                        {char.character}
                      </span>
                      <span className="text-xs font-medium text-nibi font-mono mt-0.5">
                        {char.romaji}
                      </span>

                      {/* Icon âm thanh nhỏ khi hover */}
                      <span className="absolute top-1.5 right-1.5 text-nibi/40 opacity-0 group-hover:opacity-100 hover:text-ai transition-opacity">
                        <Volume2 className="w-3.5 h-3.5" />
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal xem chi tiết và thứ tự nét viết khi bấm vào 1 chữ cái */}
      {selectedKana && (
        <div className="fixed inset-0 overscroll-contain z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-surface rounded-2xl shadow-xl max-w-md w-full p-6 animate-in fade-in zoom-in-95 duration-150 relative border border-suna">
            <button
              type="button"
              onClick={() => setSelectedKana(null)}
              className="absolute top-4 right-4 p-2 text-nibi hover:text-sumi hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center mb-4">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-kamenozoki text-ai text-xs font-semibold mb-2 border border-ai/20">
                <BookOpen className="w-3.5 h-3.5" />
                {selectedKana.charType === 'HIRAGANA' ? 'Chữ Hiragana' : 'Chữ Katakana'}
              </div>
              <h3 className="text-3xl font-extrabold text-sumi">
                {selectedKana.character}{' '}
                <span className="text-lg font-mono text-nibi">({selectedKana.romaji})</span>
              </h3>
              <p className="text-xs text-nibi mt-1">{selectedKana.meaningVi}</p>
            </div>

            {/* Trình chiếu nét vẽ SVG */}
            <div className="flex justify-center my-4">
              <KanjiStrokeViewer
                character={selectedKana.character}
                strokes={selectedKana.strokes || []}
                size={160}
                showPracticeButton={false}
              />
            </div>

            {/* Nút nghe lại */}
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={() =>
                  playSmartAudio({
                    text: selectedKana.character,
                    audioUrl: selectedKana.audioUrl,
                  })
                }
                className="flex-1 py-2.5 rounded-lg bg-ai text-white font-medium text-xs flex items-center justify-center gap-2 hover:bg-ai/90 active:scale-98 transition shadow-2xs cursor-pointer"
              >
                <Volume2 className="w-4 h-4" /> Nghe phát âm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
