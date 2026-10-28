import React, { useState, useEffect, useRef, useCallback } from 'react'
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
  Sliders,
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
  const trackRef = useRef<HTMLDivElement | null>(null)

  const [isPlaying, setIsPlaying] = useState<boolean>(false)
  const [currentTime, setCurrentTime] = useState<number>(0)
  const [duration, setDuration] = useState<number>(0)
  const [isMuted, setIsMuted] = useState<boolean>(false)
  const [playMode, setPlayMode] = useState<'SEGMENT' | 'FREE'>('SEGMENT')

  // Trạng thái kéo chuột trên thanh timeline duy nhất: 'START' | 'END' | 'WINDOW' | null
  const [draggingHandle, setDraggingHandle] = useState<'START' | 'END' | 'WINDOW' | null>(null)
  const dragStartPosRef = useRef<{ clientX: number; initStart: number; initEnd: number }>({
    clientX: 0,
    initStart: 0,
    initEnd: 0,
  })

  // Format mm:ss hoặc mm:ss.s
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
      if (!endTime || endTime <= startTime) {
        onChangeEndTime(Math.min(dur || 30, (startTime || 0) + 30))
      }
    }

    const handleTimeUpdate = () => {
      const cur = audio.currentTime
      setCurrentTime(cur)

      // Dừng lại khi chạm mốc endTime nếu đang ở chế độ nghe thử đoạn cắt
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

  const maxDuration = Math.max(duration, endTime, 60)
  const startPercent = Math.max(0, Math.min(100, (startTime / maxDuration) * 100))
  const endPercent = Math.max(0, Math.min(100, (endTime / maxDuration) * 100))
  const currentPercent = Math.max(0, Math.min(100, (currentTime / maxDuration) * 100))

  const handleSeek = (timeInSec: number) => {
    const clamped = Math.max(0, Math.min(maxDuration, timeInSec))
    setCurrentTime(clamped)
    if (audioRef.current) {
      audioRef.current.currentTime = clamped
    }
  }

  const handleStartChange = (val: number) => {
    const clamped = Math.max(0, Math.min(val, endTime - 0.5))
    onChangeStartTime(Math.round(clamped * 10) / 10)
  }

  const handleEndChange = (val: number) => {
    const clamped = Math.max(startTime + 0.5, Math.min(val, maxDuration))
    onChangeEndTime(Math.round(clamped * 10) / 10)
  }

  const togglePlaySegment = () => {
    const audio = audioRef.current
    if (!audio) return

    if (isPlaying && playMode === 'SEGMENT') {
      audio.pause()
      setIsPlaying(false)
    } else {
      setPlayMode('SEGMENT')
      audio.currentTime = startTime
      audio.play().then(() => setIsPlaying(true)).catch((e) => console.error(e))
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
      audio.play().then(() => setIsPlaying(true)).catch((e) => console.error(e))
    }
  }

  // --- XỬ LÝ KÉO CHUỘT TRÊN THANH TIMELINE DUY NHẤT (DRAG HANDLERS) ---
  const handlePointerDown = (e: React.PointerEvent, handleType: 'START' | 'END' | 'WINDOW') => {
    e.stopPropagation()
    e.preventDefault()
    setDraggingHandle(handleType)
    dragStartPosRef.current = {
      clientX: e.clientX,
      initStart: startTime,
      initEnd: endTime,
    }
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
  }

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!draggingHandle || !trackRef.current) return
    const rect = trackRef.current.getBoundingClientRect()
    if (rect.width <= 0) return

    const deltaX = e.clientX - dragStartPosRef.current.clientX
    const deltaSec = (deltaX / rect.width) * maxDuration

    if (draggingHandle === 'START') {
      const newStart = Math.max(0, Math.min(dragStartPosRef.current.initStart + deltaSec, endTime - 1))
      handleStartChange(newStart)
    } else if (draggingHandle === 'END') {
      const newEnd = Math.max(startTime + 1, Math.min(dragStartPosRef.current.initEnd + deltaSec, maxDuration))
      handleEndChange(newEnd)
    } else if (draggingHandle === 'WINDOW') {
      const segLen = dragStartPosRef.current.initEnd - dragStartPosRef.current.initStart
      let newStart = dragStartPosRef.current.initStart + deltaSec
      let newEnd = dragStartPosRef.current.initEnd + deltaSec

      if (newStart < 0) {
        newStart = 0
        newEnd = segLen
      } else if (newEnd > maxDuration) {
        newEnd = maxDuration
        newStart = maxDuration - segLen
      }
      onChangeStartTime(Math.round(newStart * 10) / 10)
      onChangeEndTime(Math.round(newEnd * 10) / 10)
    }
  }

  const handlePointerUp = (e: React.PointerEvent) => {
    if (draggingHandle) {
      setDraggingHandle(null)
      try {
        ;(e.target as HTMLElement).releasePointerCapture(e.pointerId)
      } catch (_) {}
    }
  }

  // Click vào track để di chuyển Playhead
  const handleTrackClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!trackRef.current) return
    const rect = trackRef.current.getBoundingClientRect()
    const clickX = e.clientX - rect.left
    const percent = Math.max(0, Math.min(1, clickX / rect.width))
    const seekSec = percent * maxDuration
    handleSeek(seekSec)
  }

  if (!effectiveUrl) {
    return (
      <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-amber-700 text-xs flex items-center gap-2">
        <Volume2 className="w-4 h-4 text-amber-500 shrink-0" />
        <span>Chưa có nguồn audio khả dụng để căn chỉnh mốc câu hỏi này.</span>
      </div>
    )
  }

  const segmentDuration = Math.max(0, endTime - startTime)

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-3.5 space-y-3 shadow-lg select-none text-slate-100">
      <audio ref={audioRef} src={effectiveUrl} preload="metadata" />

      {/* HEADER: Tiêu đề + Độ dài + Bộ đếm thời gian */}
      <div className="flex items-center justify-between border-b border-slate-800/80 pb-2.5">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
            <Scissors className="w-3.5 h-3.5" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-white tracking-wide">
              {title || 'Cắt đoạn âm thanh'}
            </h4>
            <p className="text-[11px] text-slate-400 flex items-center gap-1.5 font-mono">
              <span>{formatTime(startTime)}</span>
              <span>→</span>
              <span>{formatTime(endTime)}</span>
              <span className="text-emerald-400 font-bold bg-emerald-950/60 px-1.5 py-0.2 rounded border border-emerald-800/60">
                {segmentDuration.toFixed(1)}s
              </span>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 font-mono text-[11px]">
          <span className="text-slate-300 bg-slate-950 px-2 py-1 rounded-lg border border-slate-800">
            {formatTime(currentTime, true)} / {formatTime(duration || maxDuration)}
          </span>
          <button
            type="button"
            onClick={() => {
              if (audioRef.current) {
                audioRef.current.muted = !isMuted
                setIsMuted(!isMuted)
              }
            }}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
            title={isMuted ? 'Bật âm' : 'Tắt tiếng'}
          >
            {isMuted ? <VolumeX className="w-3.5 h-3.5 text-rose-400" /> : <Volume2 className="w-3.5 h-3.5" />}
          </button>
        </div>
      </div>

      {/* THANH KÉO AUDIO DUY NHẤT (SINGLE TIMELINE WITH DUAL HANDLES) */}
      <div className="space-y-1.5 pt-1">
        <div
          ref={trackRef}
          onClick={handleTrackClick}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          className="relative w-full h-11 bg-slate-950 rounded-xl border border-slate-800 overflow-hidden cursor-pointer touch-none"
        >
          {/* Mô phỏng các vạch sóng âm mờ (Simulated Waveform) */}
          <div className="absolute inset-0 flex items-center justify-between px-2 opacity-20 pointer-events-none">
            {Array.from({ length: 48 }).map((_, i) => (
              <div
                key={i}
                className="w-1 bg-indigo-400 rounded-full"
                style={{ height: `${20 + ((i * 17) % 65)}%` }}
              />
            ))}
          </div>

          {/* VÙNG CHỌN NẰM GIỮA 2 ĐẦU MÚT (HIGHLIGHTED RANGE - KÉO ĐƯỢC CẢ KHỐI) */}
          <div
            className={`absolute top-0 bottom-0 bg-linear-to-r from-indigo-500/35 via-purple-500/35 to-indigo-500/35 border-y border-indigo-400/50 cursor-grab active:cursor-grabbing transition-colors ${
              draggingHandle === 'WINDOW' ? 'bg-indigo-500/50' : ''
            }`}
            style={{
              left: `${startPercent}%`,
              width: `${Math.max(0.5, endPercent - startPercent)}%`,
            }}
            onPointerDown={(e) => handlePointerDown(e, 'WINDOW')}
            title="Kéo vùng này để di chuyển cả đoạn âm thanh"
          >
            <div className="absolute inset-0 flex items-center justify-center text-[10px] font-mono font-bold text-white/70 tracking-wider pointer-events-none">
              {segmentDuration.toFixed(1)}s
            </div>
          </div>

          {/* TAY CẦM TRÁI (START HANDLE - MỐC BẮT ĐẦU) */}
          <div
            className="absolute top-0 bottom-0 w-4 -ml-2 z-20 flex items-center justify-center cursor-ew-resize group"
            style={{ left: `${startPercent}%` }}
            onPointerDown={(e) => handlePointerDown(e, 'START')}
          >
            <div className="w-2.5 h-7 bg-indigo-500 hover:bg-indigo-400 active:scale-110 rounded-md border border-white shadow-md flex items-center justify-center transition-transform">
              <div className="w-0.5 h-3 bg-white/80 rounded" />
            </div>
            {/* Tooltip hiển thị mốc khi kéo */}
            <span className="absolute -top-6 text-[9px] font-mono bg-indigo-600 text-white px-1.5 py-0.5 rounded shadow whitespace-nowrap opacity-0 group-hover:opacity-100 transition pointer-events-none">
              Bắt đầu: {formatTime(startTime, true)}
            </span>
          </div>

          {/* TAY CẦM PHẢI (END HANDLE - MỐC KẾT THÚC) */}
          <div
            className="absolute top-0 bottom-0 w-4 -ml-2 z-20 flex items-center justify-center cursor-ew-resize group"
            style={{ left: `${endPercent}%` }}
            onPointerDown={(e) => handlePointerDown(e, 'END')}
          >
            <div className="w-2.5 h-7 bg-purple-500 hover:bg-purple-400 active:scale-110 rounded-md border border-white shadow-md flex items-center justify-center transition-transform">
              <div className="w-0.5 h-3 bg-white/80 rounded" />
            </div>
            {/* Tooltip hiển thị mốc khi kéo */}
            <span className="absolute -top-6 text-[9px] font-mono bg-purple-600 text-white px-1.5 py-0.5 rounded shadow whitespace-nowrap opacity-0 group-hover:opacity-100 transition pointer-events-none">
              Kết thúc: {formatTime(endTime, true)}
            </span>
          </div>

          {/* PLAYHEAD CON TRỎ PHÁT HIỆN TẠI */}
          <div
            className="absolute top-0 bottom-0 w-0.5 bg-rose-400 z-30 pointer-events-none shadow"
            style={{ left: `${currentPercent}%` }}
          >
            <div className="w-2.5 h-2.5 rounded-full bg-rose-500 -ml-1 -mt-0.5 ring-2 ring-white/50" />
          </div>

          {/* Vạch chia thời gian ở đáy */}
          <div className="absolute bottom-0.5 inset-x-2 flex justify-between text-[8px] font-mono text-slate-500 pointer-events-none">
            <span>00:00</span>
            <span>{formatTime(maxDuration / 2)}</span>
            <span>{formatTime(maxDuration)}</span>
          </div>
        </div>
      </div>

      {/* ĐIỀU KHIỂN TINH CHỈNH 2 ĐẦU TRÊN 1 HÀNG GỌN GÀNG (INTEGRATED CONTROLS ROW) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 bg-slate-950/70 p-2.5 rounded-xl border border-slate-800">
        {/* Cụm mốc BẮT ĐẦU */}
        <div className="flex items-center justify-between gap-2 bg-slate-900/90 px-2.5 py-1.5 rounded-lg border border-slate-800">
          <div className="flex items-center gap-1.5 min-w-0">
            <MapPin className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
            <span className="text-[11px] font-bold text-indigo-300">Start:</span>
            <input
              type="number"
              step="0.5"
              min={0}
              max={endTime - 0.5}
              value={startTime}
              onChange={(e) => handleStartChange(parseFloat(e.target.value) || 0)}
              className="w-16 px-1.5 py-0.5 text-xs font-mono font-bold bg-slate-950 border border-slate-700 rounded text-white text-center focus:border-indigo-500 focus:outline-none"
            />
            <span className="text-[10px] text-slate-400 font-mono hidden xl:inline">
              ({formatTime(startTime, true)})
            </span>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => handleStartChange(startTime - 1)}
              className="px-1.5 py-0.5 text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-200 rounded cursor-pointer"
              title="Lùi 1 giây"
            >
              -1s
            </button>
            <button
              type="button"
              onClick={() => handleStartChange(startTime + 1)}
              className="px-1.5 py-0.5 text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-200 rounded cursor-pointer"
              title="Tiến 1 giây"
            >
              +1s
            </button>
            <button
              type="button"
              onClick={() => handleStartChange(currentTime)}
              className="px-2 py-0.5 text-[10px] bg-indigo-600/80 hover:bg-indigo-600 text-white font-semibold rounded cursor-pointer shadow-xs transition"
              title="Gắn mốc bắt đầu tại vị trí đang nghe"
            >
              Gắn
            </button>
          </div>
        </div>

        {/* Cụm mốc KẾT THÚC */}
        <div className="flex items-center justify-between gap-2 bg-slate-900/90 px-2.5 py-1.5 rounded-lg border border-slate-800">
          <div className="flex items-center gap-1.5 min-w-0">
            <Flag className="w-3.5 h-3.5 text-purple-400 shrink-0" />
            <span className="text-[11px] font-bold text-purple-300">End:</span>
            <input
              type="number"
              step="0.5"
              min={startTime + 0.5}
              max={maxDuration}
              value={endTime}
              onChange={(e) => handleEndChange(parseFloat(e.target.value) || 0)}
              className="w-16 px-1.5 py-0.5 text-xs font-mono font-bold bg-slate-950 border border-slate-700 rounded text-white text-center focus:border-purple-500 focus:outline-none"
            />
            <span className="text-[10px] text-slate-400 font-mono hidden xl:inline">
              ({formatTime(endTime, true)})
            </span>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => handleEndChange(endTime - 1)}
              className="px-1.5 py-0.5 text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-200 rounded cursor-pointer"
              title="Lùi 1 giây"
            >
              -1s
            </button>
            <button
              type="button"
              onClick={() => handleEndChange(endTime + 1)}
              className="px-1.5 py-0.5 text-[10px] bg-slate-800 hover:bg-slate-700 text-slate-200 rounded cursor-pointer"
              title="Tiến 1 giây"
            >
              +1s
            </button>
            <button
              type="button"
              onClick={() => handleEndChange(currentTime)}
              className="px-2 py-0.5 text-[10px] bg-purple-600/80 hover:bg-purple-600 text-white font-semibold rounded cursor-pointer shadow-xs transition"
              title="Gắn mốc kết thúc tại vị trí đang nghe"
            >
              Gắn
            </button>
          </div>
        </div>
      </div>

      {/* CỤM NÚT PHÁT VÀ TUA ĐOẠN ÂM THANH */}
      <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-slate-800/80">
        <div className="flex items-center gap-2">
          {/* Nút phát thử đoạn cắt */}
          <button
            type="button"
            onClick={togglePlaySegment}
            className="px-3.5 py-1.5 bg-linear-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 active:scale-98 text-white font-bold rounded-xl text-xs flex items-center gap-1.5 shadow-md transition cursor-pointer"
          >
            {isPlaying && playMode === 'SEGMENT' ? (
              <>
                <Pause className="w-3.5 h-3.5 fill-white" />
                <span>Tạm dừng</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-white" />
                <span>Nghe thử đoạn cắt ({segmentDuration.toFixed(1)}s)</span>
              </>
            )}
          </button>

          {/* Nút phát tự do toàn bộ file để dò mốc */}
          <button
            type="button"
            onClick={togglePlayFree}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium rounded-xl text-xs flex items-center gap-1 border border-slate-700 transition cursor-pointer"
            title="Phát liên tục từ điểm hiện tại để vừa nghe vừa gắn mốc"
          >
            <Headphones className="w-3.5 h-3.5 text-slate-400" />
            <span>{isPlaying && playMode === 'FREE' ? 'Dừng dò' : 'Nghe tự do'}</span>
          </button>
        </div>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => handleSeek(Math.max(0, currentTime - 5))}
            className="px-2 py-1 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition text-[11px] flex items-center gap-0.5 cursor-pointer"
            title="Lùi 5s"
          >
            <RotateCcw className="w-3 h-3" />
            <span>-5s</span>
          </button>
          <button
            type="button"
            onClick={() => handleSeek(Math.min(maxDuration, currentTime + 5))}
            className="px-2 py-1 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition text-[11px] flex items-center gap-0.5 cursor-pointer"
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
