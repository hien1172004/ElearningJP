import { useEffect, useRef, useState } from 'react'
import {
  isStrictPointing,
  loadScript,
  MEDIAPIPE_HANDS_CDN,
  OneEuroFilter,
  type Point,
  type Results,
} from '@/lib/handTracking'

export type HandCaptureStatus = 'idle' | 'loading' | 'ready' | 'error'

export interface HandCursor extends Point {
  /** true khi đang hạ bút (tư thế 1 ngón trỏ hoặc giữ phím Space) */
  penDown: boolean
}

interface UseHandStrokeCaptureOptions {
  enabled: boolean
  /** Toạ độ chuẩn hoá 0..1, đã lật gương (trái người dùng = trái màn hình) */
  onStrokeStart?: (p: Point) => void
  onStrokeMove?: (p: Point) => void
  onStrokeEnd?: () => void
}

// Số frame liên tiếp cần để xác nhận hạ bút / nhấc bút (hysteresis chống đứt hoặc dính nét)
const PEN_DOWN_FRAMES = 2
const PEN_UP_FRAMES = 3
// Bỏ qua điểm quá sát điểm trước (chuẩn hoá) để nét không bị gợn
const MIN_POINT_DIST = 0.003

/**
 * Hook bật camera + MediaPipe Hands, theo dõi đầu ngón trỏ và phát sự kiện nét vẽ
 * (bắt đầu / di chuyển / kết thúc). Dùng chung cho các chế độ luyện viết bằng tay.
 *
 * Cử chỉ: duỗi 1 ngón trỏ = hạ bút; mở bàn tay / nắm tay = nhấc bút.
 * Dự phòng: giữ phím Space để hạ bút khi nhận diện tư thế không ổn định.
 */
export function useHandStrokeCapture({
  enabled,
  onStrokeStart,
  onStrokeMove,
  onStrokeEnd,
}: UseHandStrokeCaptureOptions) {
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const [status, setStatus] = useState<HandCaptureStatus>('idle')
  const [error, setError] = useState<string | null>(null)
  const [cursor, setCursor] = useState<HandCursor | null>(null)

  // Giữ callback mới nhất mà không phải khởi tạo lại camera
  const callbacksRef = useRef({ onStrokeStart, onStrokeMove, onStrokeEnd })
  callbacksRef.current = { onStrokeStart, onStrokeMove, onStrokeEnd }

  useEffect(() => {
    if (!enabled) {
      setStatus('idle')
      setCursor(null)
      return
    }

    let cancelled = false
    let hands: any = null
    let stream: MediaStream | null = null
    let rafId: number | null = null
    let processing = false

    const fx = new OneEuroFilter()
    const fy = new OneEuroFilter()
    let penDown = false
    let downFrames = 0
    let upFrames = 0
    let lastPoint: Point | null = null
    let spaceHeld = false
    let videoEl: HTMLVideoElement | null = null

    const endStroke = () => {
      if (!penDown) return
      penDown = false
      lastPoint = null
      callbacksRef.current.onStrokeEnd?.()
    }

    const onKeyDown = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement | null)?.tagName
      if (e.code === 'Space' && tag !== 'INPUT' && tag !== 'TEXTAREA') {
        e.preventDefault()
        spaceHeld = true
      }
    }
    const onKeyUp = (e: KeyboardEvent) => {
      if (e.code === 'Space') spaceHeld = false
    }
    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keyup', onKeyUp)

    const onResults = (results: Results) => {
      if (cancelled) return
      const lm = results.multiHandLandmarks?.[0]
      if (!lm || !lm[8]) {
        // Mất bàn tay: kết thúc nét đang vẽ
        endStroke()
        fx.reset()
        fy.reset()
        downFrames = upFrames = 0
        setCursor(null)
        return
      }

      const now = performance.now()
      const p: Point = { x: fx.filter(1 - lm[8].x, now), y: fy.filter(lm[8].y, now) }
      const drawingPose = spaceHeld || isStrictPointing(lm)

      if (drawingPose) {
        downFrames++
        upFrames = 0
      } else {
        upFrames++
        downFrames = 0
      }

      if (!penDown && downFrames >= PEN_DOWN_FRAMES) {
        penDown = true
        lastPoint = p
        callbacksRef.current.onStrokeStart?.(p)
      } else if (penDown && upFrames >= PEN_UP_FRAMES) {
        endStroke()
      } else if (penDown && lastPoint) {
        if (Math.hypot(p.x - lastPoint.x, p.y - lastPoint.y) >= MIN_POINT_DIST) {
          lastPoint = p
          callbacksRef.current.onStrokeMove?.(p)
        }
      }

      setCursor({ ...p, penDown })
    }

    const init = async () => {
      setStatus('loading')
      setError(null)
      try {
        await loadScript(`${MEDIAPIPE_HANDS_CDN}/hands.js`)
        const HandsClass = (window as any).Hands
        if (!HandsClass) throw new Error('Không thể nạp thư viện MediaPipe từ CDN')
        if (cancelled) return

        hands = new HandsClass({ locateFile: (file: string) => `${MEDIAPIPE_HANDS_CDN}/${file}` })
        hands.setOptions({
          maxNumHands: 1,
          modelComplexity: 1,
          minDetectionConfidence: 0.5,
          minTrackingConfidence: 0.5,
        })
        hands.onResults(onResults)

        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
          })
        } catch (e: any) {
          if (e?.name === 'NotAllowedError') throw e
          stream = await navigator.mediaDevices.getUserMedia({ video: true })
        }
        if (cancelled) return

        const video = videoRef.current
        if (!video) throw new Error('Không tìm thấy phần tử video')
        videoEl = video
        video.srcObject = stream
        await video.play()

        const loop = async () => {
          if (cancelled) return
          if (video.readyState >= 2 && !processing) {
            processing = true
            try {
              await hands.send({ image: video })
            } catch {
              // bỏ frame lỗi
            } finally {
              processing = false
            }
          }
          if (!cancelled) rafId = requestAnimationFrame(loop)
        }
        rafId = requestAnimationFrame(loop)
        if (!cancelled) setStatus('ready')
      } catch (err: any) {
        if (cancelled) return
        console.error('Lỗi khởi tạo camera / MediaPipe:', err)
        setStatus('error')
        setError(
          err?.name === 'NotAllowedError'
            ? 'Trình duyệt chưa được cấp quyền Camera. Hãy bấm biểu tượng ổ khoá trên thanh địa chỉ để cho phép.'
            : err?.name === 'NotFoundError'
              ? 'Không tìm thấy webcam trên thiết bị.'
              : 'Không thể mở webcam hoặc nạp MediaPipe. Hãy kiểm tra kết nối mạng và thiết bị camera.',
        )
      }
    }

    init()

    return () => {
      cancelled = true
      window.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('keyup', onKeyUp)
      if (penDown) callbacksRef.current.onStrokeEnd?.()
      if (rafId) cancelAnimationFrame(rafId)
      try {
        hands?.close()
      } catch {
        // bỏ qua
      }
      stream?.getTracks().forEach((t) => t.stop())
      if (videoEl) videoEl.srcObject = null
      setCursor(null)
    }
  }, [enabled])

  return { videoRef, status, error, cursor }
}
