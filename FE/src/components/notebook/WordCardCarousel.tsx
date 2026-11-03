import React, { useState, useEffect, useRef } from 'react'
import { Button, Tooltip } from 'antd'
import {
  SoundOutlined,
  LeftOutlined,
  RightOutlined,
  SwapOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  RetweetOutlined,
} from '@ant-design/icons'
import type { NotebookWord } from '@/types'

interface WordCardCarouselProps {
  words: NotebookWord[]
  onShuffle?: () => void
}

export const WordCardCarousel: React.FC<WordCardCarouselProps> = ({
  words,
  onShuffle,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isFlipped, setIsFlipped] = useState(false)
  const [isPlaying, setIsPlaying] = useState(false)
  const timerRef = useRef<number | null>(null)

  const currentWord = words[currentIndex]

  // Đổi từ thì reset trạng thái lật
  useEffect(() => {
    setIsFlipped(false)
  }, [currentIndex])

  const handleNext = () => {
    if (words.length === 0) return
    setCurrentIndex((prev) => (prev + 1) % words.length)
  }

  const handlePrev = () => {
    if (words.length === 0) return
    setCurrentIndex((prev) => (prev - 1 + words.length) % words.length)
  }

  const playSpeech = (text: string) => {
    if (!('speechSynthesis' in window)) return
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'ja-JP'
    utterance.rate = 0.9
    window.speechSynthesis.speak(utterance)
  }

  const handlePlayAudio = (e?: React.MouseEvent) => {
    e?.stopPropagation()
    if (!currentWord) return
    if (currentWord.audioUrl) {
      const audio = new Audio(currentWord.audioUrl)
      audio.play().catch(() => playSpeech(currentWord.word))
    } else {
      playSpeech(currentWord.word)
    }
  }

  // Tự động chuyển thẻ (Autoplay)
  useEffect(() => {
    if (isPlaying && words.length > 0) {
      timerRef.current = setInterval(() => {
        setCurrentIndex((prev) => (prev + 1) % words.length)
      }, 3500)
    } else if (timerRef.current) {
      clearInterval(timerRef.current)
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [isPlaying, words.length])

  if (!words || words.length === 0) {
    return null
  }

  return (
    <div className="relative mb-6">
      {/* Container thẻ preview */}
      <div className="max-w-xl mx-auto">
        <div
          onClick={() => setIsFlipped(!isFlipped)}
          className="relative min-h-[170px] bg-gradient-to-br from-slate-50 to-blue-50/40 rounded-2xl border border-blue-100/60 p-6 shadow-xs hover:shadow-md transition-all cursor-pointer flex flex-col justify-between select-none"
        >
          {/* Header thẻ: vị trí hiện tại & nút xáo trộn / autoplay */}
          <div className="flex items-center justify-between text-xs text-gray-400">
            <span className="font-medium bg-white/80 px-2.5 py-0.5 rounded-full border border-gray-100 shadow-2xs">
              {currentIndex + 1} / {words.length}
            </span>

            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
              <Tooltip title={isPlaying ? 'Dừng phát tự động' : 'Tự động chạy'}>
                <Button
                  type="text"
                  size="small"
                  shape="circle"
                  icon={
                    isPlaying ? (
                      <PauseCircleOutlined className="text-blue-600 text-base" />
                    ) : (
                      <PlayCircleOutlined className="text-gray-500 hover:text-blue-600 text-base" />
                    )
                  }
                  onClick={() => setIsPlaying(!isPlaying)}
                />
              </Tooltip>

              {onShuffle && (
                <Tooltip title="Xáo trộn danh sách thẻ">
                  <Button
                    type="text"
                    size="small"
                    shape="circle"
                    icon={<SwapOutlined className="text-gray-500 hover:text-blue-600 text-base" />}
                    onClick={onShuffle}
                  />
                </Tooltip>
              )}
            </div>
          </div>

          {/* Nội dung chính thẻ: Mặt trước (Từ + Đọc + Loa) / Mặt sau (Nghĩa + Hán Việt) */}
          <div className="text-center py-2">
            {!isFlipped ? (
              <div className="space-y-1">
                <div className="text-3xl font-bold text-gray-900 tracking-wide">
                  {currentWord.word}
                </div>
                {currentWord.reading && (
                  <div className="text-sm text-gray-500 font-medium">
                    {currentWord.reading}
                  </div>
                )}
                {(currentWord.sinoVietnamese || currentWord.hanViet) && (
                  <div className="text-xs text-emerald-600 font-medium">
                    [{currentWord.sinoVietnamese || currentWord.hanViet}]
                  </div>
                )}
                <div className="pt-2">
                  <Button
                    type="text"
                    shape="circle"
                    icon={<SoundOutlined className="text-blue-500 text-lg" />}
                    onClick={handlePlayAudio}
                    aria-label="Phát âm"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-2 py-1">
                <div className="text-xs font-semibold text-blue-500 uppercase tracking-wider">
                  Ý nghĩa
                </div>
                <div className="text-xl font-bold text-gray-800">
                  {currentWord.meaning || 'Chưa cập nhật nghĩa'}
                </div>
                {currentWord.partOfSpeech && (
                  <div className="text-xs text-gray-400 italic">
                    Từ loại: {currentWord.partOfSpeech}
                  </div>
                )}
                {currentWord.userNote && (
                  <div className="text-xs text-amber-700 bg-amber-50 px-3 py-1 rounded inline-block">
                    Ghi chú: {currentWord.userNote}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Footer thẻ: Hướng dẫn lật & Nút điều hướng */}
          <div className="flex items-center justify-between pt-2 border-t border-gray-100/60">
            <Button
              type="text"
              size="small"
              shape="circle"
              icon={<LeftOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                handlePrev()
              }}
              disabled={words.length <= 1}
            />

            <span className="text-[11px] text-gray-400 flex items-center gap-1">
              <RetweetOutlined /> Nhấp vào thẻ để lật xem {isFlipped ? 'từ vựng' : 'nghĩa'}
            </span>

            <Button
              type="text"
              size="small"
              shape="circle"
              icon={<RightOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                handleNext()
              }}
              disabled={words.length <= 1}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
export default WordCardCarousel
