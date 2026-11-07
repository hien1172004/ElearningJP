/**
 * Tiện ích dùng chung cho tính năng viết bằng tay qua camera (MediaPipe Hands).
 * Dùng bởi KanjiAirWritingModal (tra chữ) và hook useHandStrokeCapture (luyện viết chấm điểm).
 */

export interface Point {
  x: number
  y: number
}

export interface NormalizedLandmark {
  x: number
  y: number
  z: number
  visibility?: number
}

export interface Results {
  image: HTMLVideoElement | HTMLCanvasElement | HTMLImageElement
  multiHandLandmarks?: NormalizedLandmark[][]
  multiHandWorldLandmarks?: NormalizedLandmark[][]
  multiHandedness?: Array<{ index: number; score: number; label: string; displayName?: string }>
}

export const MEDIAPIPE_HANDS_CDN = 'https://cdn.jsdelivr.net/npm/@mediapipe/hands'

// Khoảng cách Euclidean giữa 2 landmarks
export const getDistance = (p1: NormalizedLandmark, p2: NormalizedLandmark) =>
  Math.hypot(p1.x - p2.x, p1.y - p2.y)

// Tư thế VẼ (chỉ 1 ngón trỏ duỗi) - bản nới: chấp nhận ngón trỏ cong nhẹ, các ngón khác hơi nhích.
export const isStrictPointing = (lm: NormalizedLandmark[]) => {
  const wrist = lm[0]
  const indexExtended = getDistance(lm[8], wrist) > getDistance(lm[6], wrist) * 1.15
  const middleNotExtended = getDistance(lm[12], wrist) < getDistance(lm[10], wrist) * 1.3
  const ringNotExtended = getDistance(lm[16], wrist) < getDistance(lm[14], wrist) * 1.3
  const pinkyNotExtended = getDistance(lm[20], wrist) < getDistance(lm[18], wrist) * 1.3
  return indexExtended && middleNotExtended && ringNotExtended && pinkyNotExtended
}

// Tải script động từ CDN (bỏ qua nếu đã có)
export const loadScript = (src: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.crossOrigin = 'anonymous'
    script.onload = () => resolve()
    script.onerror = (err) => reject(err)
    document.head.appendChild(script)
  })
}

/**
 * Bộ lọc One-Euro (Casiez 2012): làm mượt mạnh khi tay đứng yên (khử rung),
 * giảm độ trễ khi tay di chuyển nhanh. Áp dụng cho từng trục.
 */
class LowPass {
  private y: number | null = null
  filter(x: number, alpha: number) {
    this.y = this.y === null ? x : alpha * x + (1 - alpha) * this.y
    return this.y
  }
  last() {
    return this.y
  }
  reset() {
    this.y = null
  }
}

const smoothingAlpha = (cutoff: number, dt: number) => {
  const tau = 1 / (2 * Math.PI * cutoff)
  return 1 / (1 + tau / dt)
}

export class OneEuroFilter {
  private xFilter = new LowPass()
  private dxFilter = new LowPass()
  private lastTime: number | null = null

  private minCutoff: number
  private beta: number
  private dCutoff: number

  constructor(minCutoff = 1.2, beta = 8, dCutoff = 1.0) {
    this.minCutoff = minCutoff
    this.beta = beta
    this.dCutoff = dCutoff
  }

  filter(value: number, timeMs: number) {
    const dt = this.lastTime === null ? 1 / 30 : Math.max(1e-3, (timeMs - this.lastTime) / 1000)
    this.lastTime = timeMs
    const prev = this.xFilter.last()
    const dx = prev === null ? 0 : (value - prev) / dt
    const edx = this.dxFilter.filter(dx, smoothingAlpha(this.dCutoff, dt))
    const cutoff = this.minCutoff + this.beta * Math.abs(edx)
    return this.xFilter.filter(value, smoothingAlpha(cutoff, dt))
  }

  reset() {
    this.xFilter.reset()
    this.dxFilter.reset()
    this.lastTime = null
  }
}

/**
 * Vùng hoạt động trong khung hình camera (toạ độ chuẩn hoá 0..1, đã lật gương).
 * Người học chỉ cần di tay trong vùng giữa khung hình là phủ hết ô vẽ vuông,
 * không phải với tay ra tận mép camera.
 */
export const ACTIVE_REGION = { x0: 0.2, x1: 0.8, y0: 0.1, y1: 0.9 }

export const mapToBox = (p: Point, size: number): Point => {
  const clamp = (v: number) => Math.max(0, Math.min(1, v))
  const nx = clamp((p.x - ACTIVE_REGION.x0) / (ACTIVE_REGION.x1 - ACTIVE_REGION.x0))
  const ny = clamp((p.y - ACTIVE_REGION.y0) / (ACTIVE_REGION.y1 - ACTIVE_REGION.y0))
  return { x: nx * size, y: ny * size }
}
