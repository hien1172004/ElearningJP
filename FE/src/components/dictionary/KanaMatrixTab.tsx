import React, { useState, useEffect, useRef } from 'react'
import { Volume2, Sparkles, X } from 'lucide-react'
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
  const handleReadRow = (rowRomaji: string, characters: KanaCharacter[]) => {
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
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-gray-200 shadow-xs">
        {/* Chuyển đổi Hiragana / Katakana */}
        <div className="flex items-center gap-1.5 p-1 bg-gray-100 rounded-xl">
          <button
            type="button"
            onClick={() => setCharType('HIRAGANA')}
            className={`px-4 py-2 rounded-lg text-sm font-bold transition ${
              charType === 'HIRAGANA'
                ? 'bg-white text-indigo-600 shadow-xs'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            🌸 Hiragana (Chữ mềm)
          </button>
          <button
            type="button"
            onClick={() => setCharType('KATAKANA')}
            className={`px-4 py-2 rounded-lg text-sm font-bold transition ${
              charType === 'KATAKANA'
                ? 'bg-white text-indigo-600 shadow-xs'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            ⚡ Katakana (Chữ cứng)
          </button>
        </div>

        {/* Lọc: Cơ bản / Âm đục / Âm ghép */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setSection('seion')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
              section === 'seion'
                ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            Âm cơ bản (50 âm)
          </button>
          <button
            type="button"
            onClick={() => setSection('dakuon')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
              section === 'dakuon'
                ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            Âm đục & Bán đục
          </button>
          <button
            type="button"
            onClick={() => setSection('yoon')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
              section === 'yoon'
                ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            Âm ghép (Yōon)
          </button>
        </div>
      </div>

      {/* Lưới Ma trận bảng chữ cái */}
      {isLoading ? (
        <div className="bg-white rounded-3xl p-12 text-center text-gray-500 animate-pulse border border-gray-200">
          Đang tải ma trận bảng chữ cái...
        </div>
      ) : (
        <div className="space-y-4">
          {currentRows.map((row, rIdx) => (
            <div
              key={rIdx}
              className="bg-white rounded-2xl p-4 border border-gray-200/80 shadow-xs flex flex-col md:flex-row md:items-center gap-4 hover:border-indigo-200 transition"
            >
              {/* Tên hàng & Nút đọc cả hàng */}
              <div className="md:w-52 shrink-0 flex items-center justify-between md:justify-start gap-3 border-b md:border-b-0 pb-2 md:pb-0 border-gray-100">
                <div>
                  <h4 className="font-bold text-gray-900 text-sm">{row.rowName}</h4>
                  <p className="text-xs text-gray-400 font-mono">({row.rowRomaji})</p>
                </div>

                <button
                  type="button"
                  onClick={() => handleReadRow(row.rowRomaji, row.characters)}
                  className="px-2.5 py-1.5 rounded-xl bg-indigo-50 text-indigo-600 hover:bg-indigo-600 hover:text-white transition flex items-center gap-1.5 text-xs font-semibold shrink-0 group"
                  title="Đọc phát âm cả hàng này"
                >
                  <Volume2 className="w-4 h-4 group-hover:scale-110 transition" />
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
                      className={`relative aspect-square sm:aspect-4/3 rounded-2xl border p-2 sm:p-3 flex flex-col items-center justify-center cursor-pointer transition-all duration-200 select-none ${
                        isActive
                          ? 'bg-indigo-100/90 border-indigo-600 ring-4 ring-indigo-400/40 scale-105 shadow-md'
                          : 'bg-slate-50/70 border-gray-200/70 hover:bg-white hover:border-indigo-400 hover:shadow-sm hover:scale-[1.02]'
                      }`}
                    >
                      <span className="text-2xl sm:text-3xl font-extrabold text-gray-900 font-sans">
                        {char.character}
                      </span>
                      <span className="text-xs font-medium text-gray-400 font-mono mt-0.5">
                        {char.romaji}
                      </span>

                      {/* Icon âm thanh nhỏ khi hover */}
                      <span className="absolute top-1.5 right-1.5 text-gray-300 opacity-0 group-hover:opacity-100 hover:text-indigo-600">
                        <Volume2 className="w-3 h-3" />
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
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full p-6 animate-in fade-in zoom-in-95 duration-150 relative">
            <button
              type="button"
              onClick={() => setSelectedKana(null)}
              className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-xl transition"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center mb-4">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-bold mb-2">
                <Sparkles className="w-3.5 h-3.5" />
                {selectedKana.charType === 'HIRAGANA' ? 'Chữ Hiragana' : 'Chữ Katakana'}
              </div>
              <h3 className="text-3xl font-extrabold text-gray-900">
                {selectedKana.character}{' '}
                <span className="text-lg font-mono text-gray-400">({selectedKana.romaji})</span>
              </h3>
              <p className="text-xs text-gray-500 mt-1">{selectedKana.meaningVi}</p>
            </div>

            {/* Trình chiếu nét vẽ SVG */}
            <div className="flex justify-center my-4">
              <KanjiStrokeViewer
                character={selectedKana.character}
                strokes={selectedKana.strokes || []}
                size={160}
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
                className="flex-1 py-3 rounded-xl bg-indigo-600 text-white font-semibold flex items-center justify-center gap-2 hover:bg-indigo-700 active:scale-98 transition shadow-sm"
              >
                <Volume2 className="w-5 h-5" /> Nghe phát âm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
