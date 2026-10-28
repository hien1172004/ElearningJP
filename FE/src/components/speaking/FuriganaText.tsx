import React, { useState, useEffect } from 'react'
import { speakingService } from '@/services/speaking.service'

interface FuriganaTextProps {
  text: string
  rubyHtml?: string
  romaji?: string
  size?: 'sm' | 'md' | 'lg'
  showRomaji?: boolean
  showFurigana?: boolean
  className?: string
}

// In-memory cache cho furigana để không gọi lại API cho cùng một câu
const furiganaCache = new Map<string, { rubyHtml: string; romaji: string }>()

export const FuriganaText: React.FC<FuriganaTextProps> = ({
  text,
  rubyHtml: initialRubyHtml,
  romaji: initialRomaji,
  size = 'md',
  showRomaji = true,
  showFurigana = true,
  className = '',
}) => {
  const [rubyHtml, setRubyHtml] = useState<string>(initialRubyHtml || furiganaCache.get(text)?.rubyHtml || '')
  const [romaji, setRomaji] = useState<string>(initialRomaji || furiganaCache.get(text)?.romaji || '')

  useEffect(() => {
    if (initialRubyHtml) {
      setRubyHtml(initialRubyHtml)
      if (initialRomaji) setRomaji(initialRomaji)
      return
    }

    if (!text || text.trim().length === 0) return

    // Kiểm tra cache
    if (furiganaCache.has(text)) {
      const cached = furiganaCache.get(text)!
      setRubyHtml(cached.rubyHtml)
      setRomaji(cached.romaji)
      return
    }

    let isMounted = true
    speakingService
      .getFurigana(text)
      .then((res) => {
        if (!isMounted) return
        furiganaCache.set(text, { rubyHtml: res.rubyHtml, romaji: res.romaji })
        setRubyHtml(res.rubyHtml)
        setRomaji(res.romaji)
      })
      .catch((err) => {
        console.warn('Lỗi lấy furigana cho câu:', text, err)
      })

    return () => {
      isMounted = false
    }
  }, [text, initialRubyHtml, initialRomaji])

  const fontSizeClasses = {
    sm: 'text-sm leading-relaxed',
    md: 'text-base sm:text-lg leading-loose',
    lg: 'text-xl sm:text-2xl leading-loose font-medium',
  }

  const romajiSizeClasses = {
    sm: 'text-xs',
    md: 'text-xs sm:text-sm',
    lg: 'text-sm sm:text-base',
  }

  return (
    <div className={`japanese-text-container select-text ${className}`}>
      {showFurigana && rubyHtml ? (
        <span
          className={`japanese-ruby inline-block font-sans ${fontSizeClasses[size]}`}
          dangerouslySetInnerHTML={{ __html: rubyHtml }}
        />
      ) : (
        <span className={`inline-block font-sans ${fontSizeClasses[size]}`}>{text}</span>
      )}

      {showRomaji && romaji && (
        <div className={`text-slate-500 font-mono tracking-wide mt-0.5 ${romajiSizeClasses[size]}`}>
          {romaji}
        </div>
      )}
    </div>
  )
}
