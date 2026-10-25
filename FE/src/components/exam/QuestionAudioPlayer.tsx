import React, { useState, useEffect, useRef } from 'react'
import { Play, Pause, RotateCcw, Volume2, VolumeX, FastForward } from 'lucide-react'

interface QuestionAudioPlayerProps {
  audioUrl?: string
  masterAudioUrl?: string
  startTime?: number
  endTime?: number
  title?: string
}

export const QuestionAudioPlayer: React.FC<QuestionAudioPlayerProps> = ({
  audioUrl,
  masterAudioUrl,
  startTime = 0,
  endTime,
  title,
}) => {
  const effectiveUrl = audioUrl || masterAudioUrl
  const audioRef = useRef<HTMLAudioElement | null>(null)

  const [isPlaying, setIsPlaying] = useState<boolean>(false)
  const [currentTime, setCurrentTime] = useState<number>(0)
  const [duration, setDuration] = useState<number>(0)
  const [isMuted, setIsMuted] = useState<boolean>(false)

  // Segment boundaries: CHỈ là segment nếu không có audioUrl riêng (phải dùng masterAudioUrl ca nghe), hoặc audioUrl trùng với masterAudioUrl
  const isSegment = Boolean(
    masterAudioUrl && (!audioUrl || audioUrl === masterAudioUrl)
  )
  const segStart = isSegment ? (startTime || 0) : 0

  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return

    const handleLoadedMetadata = () => {
      setDuration(audio.duration || 0)
      if (isSegment) {
        audio.currentTime = segStart
        setCurrentTime(0)
      }
    }

    const computedEnd = endTime && endTime > segStart ? endTime : (segStart + 60)
    const handleTimeUpdate = () => {
      if (isSegment && audio.currentTime >= computedEnd) {
        audio.pause()
        setIsPlaying(false)
        audio.currentTime = segStart
        setCurrentTime(0)
        return
      }
      const relTime = isSegment ? Math.max(0, audio.currentTime - segStart) : audio.currentTime
      setCurrentTime(relTime)
    }

    const handleEnded = () => {
      setIsPlaying(false)
      setCurrentTime(0)
      if (isSegment) audio.currentTime = segStart
    }

    audio.addEventListener('loadedmetadata', handleLoadedMetadata)
    audio.addEventListener('timeupdate', handleTimeUpdate)
    audio.addEventListener('ended', handleEnded)

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata)
      audio.removeEventListener('timeupdate', handleTimeUpdate)
      audio.removeEventListener('ended', handleEnded)
    }
  }, [effectiveUrl, isSegment, segStart, endTime])

  if (!effectiveUrl) {
    return (
      <div className="p-2.5 rounded-xl bg-slate-100 text-slate-500 text-xs italic flex items-center gap-2">
        <Volume2 className="w-4 h-4 text-slate-400" />
        <span>Chưa có file âm thanh cho câu hỏi này.</span>
      </div>
    )
  }

  const computedEnd = isSegment ? (endTime && endTime > segStart ? endTime : (segStart + 60)) : duration
  const effectiveDuration = isSegment ? Math.max(0, computedEnd - segStart) : duration

  const togglePlay = () => {
    const audio = audioRef.current
    if (!audio) return

    if (isPlaying) {
      audio.pause()
      setIsPlaying(false)
    } else {
      if (isSegment && (audio.currentTime < segStart || (endTime && audio.currentTime >= endTime))) {
        audio.currentTime = segStart
      }
      audio.play().catch((err) => console.error('Audio play error:', err))
      setIsPlaying(true)
    }
  }

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const audio = audioRef.current
    if (!audio) return
    const targetRel = parseFloat(e.target.value)
    setCurrentTime(targetRel)
    audio.currentTime = isSegment ? segStart + targetRel : targetRel
  }

  const handleSkip = (seconds: number) => {
    const audio = audioRef.current
    if (!audio) return
    const newRel = Math.min(Math.max(0, currentTime + seconds), effectiveDuration)
    setCurrentTime(newRel)
    audio.currentTime = isSegment ? segStart + newRel : newRel
  }

  const formatTime = (sec: number) => {
    if (isNaN(sec) || sec < 0) return '00:00'
    const m = Math.floor(sec / 60)
    const s = Math.floor(sec % 60)
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  return (
    <div className="bg-linear-to-r from-indigo-50 to-purple-50 dark:from-indigo-950/40 dark:to-purple-950/40 border border-indigo-200 dark:border-indigo-800 rounded-2xl p-3 shadow-xs space-y-2">
      <audio ref={audioRef} src={effectiveUrl} preload="metadata" />

      <div className="flex items-center justify-between text-xs">
        <div className="flex items-center gap-1.5 font-bold text-indigo-900 dark:text-indigo-200">
          <Volume2 className="w-4 h-4 text-indigo-600 animate-pulse" />
          <span>{title || (audioUrl ? 'Audio riêng câu hỏi' : `Đoạn cắt ca nghe (${segStart}s - ${endTime || 0}s)`)}</span>
        </div>
        <div className="font-mono text-[11px] font-semibold text-indigo-700 dark:text-indigo-300">
          {formatTime(currentTime)} / {formatTime(effectiveDuration)}
        </div>
      </div>

      {/* Progress & Seekbar */}
      <div className="flex items-center gap-2">
        <input
          type="range"
          min={0}
          max={effectiveDuration || 100}
          step={0.1}
          value={currentTime}
          onChange={handleSeek}
          className="w-full h-1.5 bg-indigo-200 dark:bg-indigo-900 rounded-lg appearance-none cursor-pointer accent-indigo-600"
        />
      </div>

      {/* Playback Controls */}
      <div className="flex items-center justify-between pt-1">
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => handleSkip(-5)}
            className="p-1.5 rounded-lg text-slate-600 hover:text-indigo-600 hover:bg-white/80 transition cursor-pointer text-[10px] font-bold flex items-center gap-0.5"
            title="Lùi lại 5 giây"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>-5s</span>
          </button>

          <button
            type="button"
            onClick={togglePlay}
            className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow-sm transition cursor-pointer"
          >
            {isPlaying ? (
              <>
                <Pause className="w-3.5 h-3.5 fill-white" />
                <span>Tạm dừng</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-white" />
                <span>Bắt đầu nghe</span>
              </>
            )}
          </button>

          <button
            type="button"
            onClick={() => handleSkip(5)}
            className="p-1.5 rounded-lg text-slate-600 hover:text-indigo-600 hover:bg-white/80 transition cursor-pointer text-[10px] font-bold flex items-center gap-0.5"
            title="Tua tới 5 giây"
          >
            <span>+5s</span>
            <FastForward className="w-3.5 h-3.5" />
          </button>
        </div>

        <button
          type="button"
          onClick={() => {
            if (audioRef.current) {
              audioRef.current.muted = !isMuted
              setIsMuted(!isMuted)
            }
          }}
          className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 transition cursor-pointer"
          title={isMuted ? 'Bật âm' : 'Tắt tiếng'}
        >
          {isMuted ? <VolumeX className="w-4 h-4 text-rose-500" /> : <Volume2 className="w-4 h-4" />}
        </button>
      </div>
    </div>
  )
}
export default QuestionAudioPlayer
