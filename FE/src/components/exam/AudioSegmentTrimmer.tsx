import React, { useState, useEffect, useRef } from 'react'
import {
  Play,
  Pause,
  RotateCcw,
  FastForward,
  Scissors,
  Volume2,
  VolumeX,
  MapPin,
  Flag,
  Headphones,
} from 'lucide-react'

interface AudioSegmentTrimmerProps {
  audioUrl?: string
  masterAudioUrl?: string
  startTime: number
  endTime: number
  onChangeStartTime: (val: number) => void
  onChangeEndTime: (val: number) => void
  title?: string
}

export const AudioSegmentTrimmer: React.FC<AudioSegmentTrimmerProps> = ({
  audioUrl,
  masterAudioUrl,
  startTime = 0,
  endTime = 30,
  onChangeStartTime,
  onChangeEndTime,
  title,
}) => {
  const effectiveUrl = audioUrl || masterAudioUrl
  const audioRef = useRef<HTMLAudioElement | null>(null)

  const [isPlaying, setIsPlaying] = useState<boolean>(false)
  const [currentTime, setCurrentTime] = useState<number>(0)
  const [duration, setDuration] = useState<number>(0)
  const [isMuted, setIsMuted] = useState<boolean>(false)
  const [playMode, setPlayMode] = useState<'SEGMENT' | 'FREE'>('SEGMENT')

  // Định dạng mm:ss hoặc mm:ss.s
  const formatTime = (sec: number, showDecimals: boolean = false) => {
    if (isNaN(sec) || sec < 0) return '00:00'
    const m = Math.floor(sec / 60)
    const s = Math.floor(sec % 60)
    const ms = Math.floor((sec % 1) * 10)
    if (showDecimals) {
      return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}.${ms}`
    }
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  // Khởi tạo audio metadata
  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return

    const handleLoadedMetadata = () => {
      const dur = audio.duration || 0
      setDuration(dur)
      // Nếu endTime chưa có hoặc lớn hơn duration
      if (!endTime || endTime <= startTime) {
        onChangeEndTime(Math.min(dur || 30, (startTime || 0) + 30))
      }
    }

    const handleTimeUpdate = () => {
      const cur = audio.currentTime
      setCurrentTime(cur)

      // Nếu đang phát ở chế độ SEGMENT thì dừng khi đến endTime
      if (playMode === 'SEGMENT' && endTime > startTime && cur >= endTime) {
        audio.pause()
        setIsPlaying(false)
        audio.currentTime = startTime
      }
    }

    const handleEnded = () => {
      setIsPlaying(false)
      audio.currentTime = startTime
    }

    audio.addEventListener('loadedmetadata', handleLoadedMetadata)
    audio.addEventListener('timeupdate', handleTimeUpdate)
    audio.addEventListener('ended', handleEnded)

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata)
      audio.removeEventListener('timeupdate', handleTimeUpdate)
      audio.removeEventListener('ended', handleEnded)
    }
  }, [effectiveUrl, playMode, startTime, endTime, onChangeEndTime])

  if (!effectiveUrl) {
    return (
      <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-700 text-xs flex items-center gap-2">
        <Volume2 className="w-4 h-4 text-amber-500 shrink-0" />
        <span>Chưa có file âm thanh để điều chỉnh mốc thời gian.</span>
      </div>
    )
  }

  const maxDuration = Math.max(duration, endTime + 10, 60)

  // Bật/tắt phát
  const togglePlaySegment = () => {
    const audio = audioRef.current
    if (!audio) return

    if (isPlaying && playMode === 'SEGMENT') {
      audio.pause()
      setIsPlaying(false)
    } else {
      setPlayMode('SEGMENT')
      audio.currentTime = startTime
      audio.play().catch((err) => console.error('Audio play error:', err))
      setIsPlaying(true)
    }
  }

  const togglePlayFree = () => {
    const audio = audioRef.current
    if (!audio) return

    if (isPlaying && playMode === 'FREE') {
      audio.pause()
      setIsPlaying(false)
    } else {
      setPlayMode('FREE')
      audio.play().catch((err) => console.error('Audio play error:', err))
      setIsPlaying(true)
    }
  }

  // Nhảy tua
  const handleSeek = (newTime: number) => {
    const audio = audioRef.current
    if (!audio) return
    audio.currentTime = newTime
    setCurrentTime(newTime)
  }

  // Điều chỉnh mốc bắt đầu
  const handleStartChange = (val: number) => {
    const validVal = Math.max(0, Math.min(val, endTime - 1))
    onChangeStartTime(Math.round(validVal * 10) / 10)
  }

  // Điều chỉnh mốc kết thúc
  const handleEndChange = (val: number) => {
    const validVal = Math.max(startTime + 1, Math.min(val, maxDuration))
    onChangeEndTime(Math.round(validVal * 10) / 10)
  }

  // Tính phần trăm trên thanh trực quan
  const startPercent = (startTime / maxDuration) * 100
  const endPercent = (endTime / maxDuration) * 100
  const currentPercent = (currentTime / maxDuration) * 100

  return (
    <div className="bg-slate-900 border border-slate-700/80 rounded-2xl p-4 shadow-lg text-white space-y-3.5">
      <audio ref={audioRef} src={effectiveUrl} preload="metadata" />

      {/* Tiêu đề & Thông tin thời lượng */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-indigo-500/20 border border-indigo-400/40 flex items-center justify-center">
            <Scissors className="w-4 h-4 text-indigo-400" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-white flex items-center gap-1.5">
              {title || 'Kéo chỉnh đoạn âm thanh câu hỏi'}
            </h4>
            <p className="text-[10px] text-slate-400">
              Đoạn cắt: <span className="font-mono text-indigo-300 font-bold">{formatTime(startTime, true)}</span> →{' '}
              <span className="font-mono text-purple-300 font-bold">{formatTime(endTime, true)}</span> (Dài{' '}
              <span className="font-semibold text-emerald-400">{Math.max(0, endTime - startTime).toFixed(1)}s</span>)
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-[11px] font-mono text-slate-300 bg-slate-800/80 px-2 py-0.5 rounded-md border border-slate-700">
            {formatTime(currentTime)} / {formatTime(duration)}
          </span>
          <button
            type="button"
            onClick={() => {
              if (audioRef.current) {
                audioRef.current.muted = !isMuted
                setIsMuted(!isMuted)
              }
            }}
            className="p-1 rounded-md text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
            title={isMuted ? 'Bật âm' : 'Tắt tiếng'}
          >
            {isMuted ? <VolumeX className="w-3.5 h-3.5 text-rose-400" /> : <Volume2 className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>

      {/* THANH TIMELINE TRỰC QUAN */}
      <div className="space-y-1.5">
        <div className="relative w-full h-8 bg-slate-950 rounded-xl border border-slate-800 overflow-hidden select-none">
          {/* Vùng chọn của câu hỏi (Highlighted Segment) */}
          <div
            className="absolute top-0 bottom-0 bg-linear-to-r from-indigo-500/40 via-purple-500/40 to-indigo-500/40 border-x-2 border-indigo-400 cursor-pointer transition-all duration-75"
            style={{
              left: `${startPercent}%`,
              width: `${Math.max(1, endPercent - startPercent)}%`,
            }}
            onClick={togglePlaySegment}
            title="Bấm để nghe thử đoạn này"
          />

          {/* Mốc hiện tại của audio khi đang phát */}
          <div
            className="absolute top-0 bottom-0 w-0.5 bg-rose-400 z-10 shadow-sm transition-all duration-75"
            style={{ left: `${currentPercent}%` }}
          >
            <div className="w-2 h-2 rounded-full bg-rose-500 -ml-[3px] -mt-0.5 shadow-xs" />
          </div>

          {/* Vạch kẻ chia thời gian mờ */}
          <div className="absolute inset-0 flex justify-between items-center px-2 pointer-events-none text-[9px] font-mono text-slate-500">
            <span>00:00</span>
            <span>{formatTime(maxDuration / 2)}</span>
            <span>{formatTime(maxDuration)}</span>
          </div>
        </div>

        {/* 2 THANH TRƯỢT KÉO ĐỘC LẬP (START & END) */}
        <div className="space-y-2 pt-1">
          {/* Thanh trượt MỐC BẮT ĐẦU */}
          <div className="bg-slate-800/50 p-2 rounded-xl border border-slate-800 space-y-1">
            <div className="flex items-center justify-between text-[11px]">
              <span className="flex items-center gap-1 font-semibold text-indigo-300">
                <MapPin className="w-3 h-3 text-indigo-400" />
                Mốc Bắt đầu (Start):{' '}
                <span className="font-mono text-white font-bold">{formatTime(startTime, true)}</span> ({startTime}s)
              </span>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => handleStartChange(startTime - 1)}
                  className="px-1.5 py-0.5 text-[10px] bg-slate-700 hover:bg-slate-600 rounded text-slate-200 cursor-pointer"
                  title="Lùi 1 giây"
                >
                  -1s
                </button>
                <button
                  type="button"
                  onClick={() => handleStartChange(startTime + 1)}
                  className="px-1.5 py-0.5 text-[10px] bg-slate-700 hover:bg-slate-600 rounded text-slate-200 cursor-pointer"
                  title="Tiến 1 giây"
                >
                  +1s
                </button>
                <button
                  type="button"
                  onClick={() => handleStartChange(currentTime)}
                  className="px-2 py-0.5 text-[10px] bg-indigo-600/80 hover:bg-indigo-600 text-white font-semibold rounded cursor-pointer flex items-center gap-0.5 shadow-xs transition"
                  title="Gắn mốc bắt đầu đúng vị trí audio đang nghe"
                >
                  <MapPin className="w-2.5 h-2.5" />
                  <span>Gắn điểm đang nghe</span>
                </button>
              </div>
            </div>

            <input
              type="range"
              min={0}
              max={maxDuration}
              step={0.5}
              value={startTime}
              onChange={(e) => handleStartChange(parseFloat(e.target.value))}
              className="w-full h-1.5 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-indigo-500"
            />
          </div>

          {/* Thanh trượt MỐC KẾT THÚC */}
          <div className="bg-slate-800/50 p-2 rounded-xl border border-slate-800 space-y-1">
            <div className="flex items-center justify-between text-[11px]">
              <span className="flex items-center gap-1 font-semibold text-purple-300">
                <Flag className="w-3 h-3 text-purple-400" />
                Mốc Kết thúc (End):{' '}
                <span className="font-mono text-white font-bold">{formatTime(endTime, true)}</span> ({endTime}s)
              </span>
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => handleEndChange(endTime - 1)}
                  className="px-1.5 py-0.5 text-[10px] bg-slate-700 hover:bg-slate-600 rounded text-slate-200 cursor-pointer"
                  title="Lùi 1 giây"
                >
                  -1s
                </button>
                <button
                  type="button"
                  onClick={() => handleEndChange(endTime + 1)}
                  className="px-1.5 py-0.5 text-[10px] bg-slate-700 hover:bg-slate-600 rounded text-slate-200 cursor-pointer"
                  title="Tiến 1 giây"
                >
                  +1s
                </button>
                <button
                  type="button"
                  onClick={() => handleEndChange(currentTime)}
                  className="px-2 py-0.5 text-[10px] bg-purple-600/80 hover:bg-purple-600 text-white font-semibold rounded cursor-pointer flex items-center gap-0.5 shadow-xs transition"
                  title="Gắn mốc kết thúc đúng vị trí audio đang nghe"
                >
                  <Flag className="w-2.5 h-2.5" />
                  <span>Gắn điểm đang nghe</span>
                </button>
              </div>
            </div>

            <input
              type="range"
              min={0}
              max={maxDuration}
              step={0.5}
              value={endTime}
              onChange={(e) => handleEndChange(parseFloat(e.target.value))}
              className="w-full h-1.5 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-purple-500"
            />
          </div>
        </div>
      </div>

      {/* CỤM NÚT ĐIỀU KHIỂN & NGHE THỬ */}
      <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-slate-800">
        <div className="flex items-center gap-2">
          {/* Nút phát thử đoạn cắt */}
          <button
            type="button"
            onClick={togglePlaySegment}
            className="px-3.5 py-1.5 bg-linear-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 active:scale-95 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow-md transition cursor-pointer"
          >
            {isPlaying && playMode === 'SEGMENT' ? (
              <>
                <Pause className="w-3.5 h-3.5 fill-white" />
                <span>Tạm dừng</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-white" />
                <span>Nghe thử đoạn cắt ({Math.max(0, endTime - startTime).toFixed(1)}s)</span>
              </>
            )}
          </button>

          {/* Nút phát tự do toàn bộ file để dò mốc */}
          <button
            type="button"
            onClick={togglePlayFree}
            className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium rounded-xl text-xs flex items-center gap-1 border border-slate-700 transition cursor-pointer"
            title="Phát liên tục từ điểm hiện tại để bạn vừa nghe vừa bấm gắn mốc"
          >
            <Headphones className="w-3.5 h-3.5 text-slate-400" />
            <span>{isPlaying && playMode === 'FREE' ? 'Dừng dò' : 'Nghe tự do để dò mốc'}</span>
          </button>
        </div>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => handleSeek(Math.max(0, currentTime - 5))}
            className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition text-[11px] flex items-center gap-0.5 cursor-pointer"
            title="Lùi 5s"
          >
            <RotateCcw className="w-3 h-3" />
            <span>-5s</span>
          </button>
          <button
            type="button"
            onClick={() => handleSeek(Math.min(maxDuration, currentTime + 5))}
            className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition text-[11px] flex items-center gap-0.5 cursor-pointer"
            title="Tiến 5s"
          >
            <span>+5s</span>
            <FastForward className="w-3 h-3" />
          </button>
        </div>
      </div>
    </div>
  )
}

export default AudioSegmentTrimmer
