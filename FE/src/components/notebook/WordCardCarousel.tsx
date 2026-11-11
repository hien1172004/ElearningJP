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
          className="relative min-h-[180px] bg-surface rounded-2xl border border-suna/90 p-6 shadow-xs hover:shadow-sm transition-all cursor-pointer flex flex-col justify-between select-none"
        >
          {/* Header thẻ: vị trí hiện tại & nút xáo trộn / autoplay */}
          <div className="flex items-center justify-between text-xs text-nibi">
            <span className="font-medium bg-slate-50 text-nibi px-2.5 py-0.5 rounded-full border border-suna shadow-2xs">
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
                      <PauseCircleOutlined className="text-ai text-base" />
                    ) : (
                      <PlayCircleOutlined className="text-nibi hover:text-ai text-base" />
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
                    icon={<SwapOutlined className="text-nibi hover:text-ai text-base" />}
                    onClick={onShuffle}
                  />
                </Tooltip>
              )}
            </div>
          </div>

          {/* Nội dung chính thẻ: Mặt trước (Từ + Đọc + Loa) / Mặt sau (Nghĩa + Hán Việt) */}
          <div className="text-center py-3">
            {!isFlipped ? (
              <div className="space-y-1.5">
                <div className="text-3xl sm:text-4xl font-bold text-sumi tracking-wide font-sans">
                  {currentWord.word}
                </div>
                {currentWord.reading && (
                  <div className="text-sm text-nibi font-medium">
                    {currentWord.reading}
                  </div>
                )}
                {(currentWord.sinoVietnamese || currentWord.hanViet) && (
                  <div className="inline-block text-xs text-ai font-medium bg-kamenozoki/60 px-2.5 py-0.5 rounded-full border border-ai/15">
                    [{currentWord.sinoVietnamese || currentWord.hanViet}]
                  </div>
                )}
                <div className="pt-1">
                  <Button
                    type="text"
                    shape="circle"
                    icon={<SoundOutlined className="text-ai text-lg" />}
                    onClick={handlePlayAudio}
                    aria-label="Phát âm"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-2 py-1">
                <div className="text-xs font-semibold text-ai uppercase tracking-wider">
                  Ý nghĩa
                </div>
                <div className="text-xl sm:text-2xl font-bold text-sumi">
                  {currentWord.meaning || 'Chưa cập nhật nghĩa'}
                </div>
                {currentWord.partOfSpeech && (
                  <div className="text-xs text-nibi italic">
                    Từ loại: {currentWord.partOfSpeech}
                  </div>
                )}
                {currentWord.userNote && (
                  <div className="text-xs text-sumi bg-kinari/80 border border-suna px-3 py-1 rounded-md inline-block">
                    Ghi chú: {currentWord.userNote}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Footer thẻ: Hướng dẫn lật & Nút điều hướng */}
          <div className="flex items-center justify-between pt-3 border-t border-suna/60">
            <Button
              type="text"
              size="small"
              shape="circle"
              icon={<LeftOutlined className="text-nibi hover:text-ai" />}
              onClick={(e) => {
                e.stopPropagation()
                handlePrev()
              }}
              disabled={words.length <= 1}
            />

            <span className="text-[11px] text-nibi flex items-center gap-1.5">
              <RetweetOutlined className="text-ai" /> Nhấp vào thẻ để lật xem {isFlipped ? 'từ vựng' : 'nghĩa'}
            </span>

            <Button
              type="text"
              size="small"
              shape="circle"
              icon={<RightOutlined className="text-nibi hover:text-ai" />}
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
