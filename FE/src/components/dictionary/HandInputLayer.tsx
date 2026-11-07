import React from 'react'
import { Loader2, MousePointer, VideoOff } from 'lucide-react'
import { useHandStrokeCapture } from '@/hooks/useHandStrokeCapture'
import { mapToBox, type Point } from '@/lib/handTracking'

interface HandInputLayerProps {
  /** Kích thước ô vẽ vuông (px) mà lớp này phủ lên */
  size: number
  /** Toạ độ đã quy về pixel trong ô vẽ (0..size) */
  onStrokeStart: (p: Point) => void
  onStrokeMove: (p: Point) => void
  onStrokeEnd: () => void
  /** Khi camera lỗi, người học bấm để quay về dùng chuột */
  onFallbackToMouse?: () => void
}

/**
 * Lớp phủ nhập nét bằng tay: hiển thị hình camera mờ phía sau ô vẽ, con trỏ đầu ngón trỏ,
 * và chuyển cử chỉ tay thành sự kiện nét vẽ theo toạ độ ô vẽ.
 * Tự tách khỏi component cha để việc cập nhật con trỏ ~30fps không render lại cả modal.
 */
export const HandInputLayer: React.FC<HandInputLayerProps> = ({
  size,
  onStrokeStart,
  onStrokeMove,
  onStrokeEnd,
  onFallbackToMouse,
}) => {
  const { videoRef, status, error, cursor } = useHandStrokeCapture({
    enabled: true,
    onStrokeStart: (p) => onStrokeStart(mapToBox(p, size)),
    onStrokeMove: (p) => onStrokeMove(mapToBox(p, size)),
    onStrokeEnd,
  })

  const cursorPx = cursor ? mapToBox(cursor, size) : null

  return (
    <>
      {/* Hình camera (lật gương) làm nền mờ để người học thấy tay mình */}
      <video
        ref={videoRef}
        muted
        playsInline
        className="absolute inset-0 w-full h-full object-cover pointer-events-none z-0 opacity-25 -scale-x-100"
      />

      {/* Con trỏ đầu ngón trỏ */}
      {cursorPx && (
        <div
          className={`absolute z-30 pointer-events-none rounded-full border-2 -translate-x-1/2 -translate-y-1/2 transition-[width,height] duration-75 ${
            cursor?.penDown
              ? 'w-4 h-4 bg-sky-500 border-white shadow-[0_0_10px_rgba(14,165,233,0.8)]'
              : 'w-5 h-5 bg-emerald-400/40 border-emerald-500'
          }`}
          style={{ left: cursorPx.x, top: cursorPx.y }}
        />
      )}

      {/* Trạng thái */}
      <div className="absolute z-30 top-2 left-2 pointer-events-none">
        {status === 'ready' && (
          <span
            className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
              cursor?.penDown ? 'bg-sky-600 text-white' : cursor ? 'bg-slate-800/75 text-white' : 'bg-amber-500 text-white'
            }`}
          >
            {cursor?.penDown ? '✍️ Đang vẽ' : cursor ? '✋ Di chuyển' : 'Đưa tay vào camera'}
          </span>
        )}
      </div>

      {status === 'loading' && (
        <div className="absolute inset-0 z-40 bg-surface/80 flex flex-col items-center justify-center gap-2 text-indigo-600">
          <Loader2 className="w-7 h-7 animate-spin" />
          <span className="text-xs font-semibold text-gray-600">Đang mở camera & MediaPipe...</span>
        </div>
      )}

      {status === 'error' && (
        <div
          className="absolute inset-0 z-40 bg-surface/95 p-4 flex flex-col items-center justify-center text-center gap-2"
          style={{ width: size, height: size }}
        >
          <VideoOff className="w-8 h-8 text-rose-500" />
          <p className="text-xs font-semibold text-gray-700">{error}</p>
          {onFallbackToMouse && (
            <button
              type="button"
              onClick={onFallbackToMouse}
              className="mt-1 px-3 py-1.5 rounded-md bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer"
            >
              <MousePointer className="w-3.5 h-3.5" />
              Dùng chuột
            </button>
          )}
        </div>
      )}
    </>
  )
}

/** Dòng hướng dẫn cử chỉ, đặt dưới ô vẽ khi ở chế độ bằng tay */
export const HandInputHint: React.FC = () => (
  <p className="mt-2 text-[11px] text-gray-500 text-center leading-relaxed">
    ☝️ Duỗi <b>1 ngón trỏ</b> để hạ bút • ✋ Mở bàn tay để nhấc bút • Giữ <b>Space</b> để vẽ nếu nhận diện chưa ổn
  </p>
)
