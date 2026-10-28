import React from 'react'
import { cn } from '@/utils/cn'
import {
  mascotWaterPct,
  resolveMascotState,
  type MascotProgressState,
} from '@/utils/mascotProgress'
import styles from './MascotProgress.module.css'

/**
 * MascotProgress — mèo mascot có hiệu ứng nước, phản ánh tiến độ học trong ngày.
 *
 * <p><b>3 lớp</b> trong khung tròn {@code overflow-hidden}:</p>
 * <ol>
 *   <li>Nền gradient theo state (xanh đậm → xanh vừa → đỏ cam).</li>
 *   <li>Ảnh mèo SVG (đổi theo state).</li>
 *   <li>Lớp nước <b>đè lên mèo</b> — thân mèo trông như chìm dưới nước.</li>
 * </ol>
 *
 * <p><b>Công thức mực nước:</b> {@code water% = MAX_WATER * (1 - ratio)} —
 * càng học nhiều nước càng hạ. Đạt/vượt mục tiêu thì cạn nước (state
 * {@code fire}).</p>
 *
 * <p><b>Hiệu năng:</b> chỉ animate {@code transform}/{@code opacity} — mực nước
 * hạ bằng {@code translateY} trên lớp nước full-height, không animate
 * {@code height} (tránh reflow). Toàn bộ animation tắt khi
 * {@code prefers-reduced-motion: reduce}.</p>
 *
 * <p><b>Số liệu vẫn hiển thị bằng chữ</b> ở UI (cạnh mascot), không chỉ dựa
 * vào màu/hình. Container có {@code role="img"} + {@code aria-label}
 * dạng "Đã học 12/20 từ".</p>
 *
 * ```tsx
 * <MascotProgress learned={12} goal={20} size={160} />
 * ```
 */

export type { MascotProgressState }

// --------------------------------------------------------------------------
// Map state → asset / class / màu
// --------------------------------------------------------------------------

/** Đường dẫn asset trong `FE/public` → dùng qua URL tuyệt đối. */
const STATE_SRC: Record<MascotProgressState, string> = {
  drowning: '/mascot/drowning.svg',
  standing: '/mascot/standing.svg',
  fire: '/mascot/fire.svg',
}

const STATE_BG_CLASS: Record<MascotProgressState, string> = {
  drowning: styles.bgDrowning,
  standing: styles.bgStanding,
  fire: styles.bgFire,
}

const STATE_WATER_CLASS: Record<MascotProgressState, string> = {
  drowning: styles.waterBodyDrowning,
  standing: styles.waterBodyStanding,
  fire: styles.waterBodyFire,
}

/** Màu mép sóng (sáng hơn màu nước để thấy rõ đường bờ). */
const STATE_SURFACE_COLOR: Record<MascotProgressState, string> = {
  drowning: '#93c5fd',
  standing: '#bae6fd',
  fire: '#fdba74',
}

// --------------------------------------------------------------------------
// Bong bóng
// --------------------------------------------------------------------------

/** 4 bong bóng nhỏ nổi lên trong nước (kích thước tính theo `size`). */
const BUBBLES = [
  { left: '18%', sizeRatio: 0.056, duration: 5.2, delay: 0 },
  { left: '41%', sizeRatio: 0.038, duration: 4.1, delay: 1.2 },
  { left: '63%', sizeRatio: 0.069, duration: 6, delay: 0.5 },
  { left: '81%', sizeRatio: 0.044, duration: 4.8, delay: 2.1 },
] as const

/** Một chu kỳ sóng lặp 8 lần trên viewBox rộng 200 → path lặp liền mạch. */
const WAVE_PATH =
  'M0 8 q12.5 -8 25 0 t25 0 t25 0 t25 0 t25 0 t25 0 t25 0 t25 0 V16 H0 Z'

// --------------------------------------------------------------------------
// Props
// --------------------------------------------------------------------------

export interface MascotProgressProps {
  /** Số mục MỚI đã học trong ngày. */
  learned: number
  /** Mục tiêu số mục mỗi ngày. */
  goal: number
  /** Đường kính khung (px). */
  size?: number
  className?: string
}

// --------------------------------------------------------------------------
// Component
// --------------------------------------------------------------------------

export const MascotProgress: React.FC<MascotProgressProps> = ({
  learned,
  goal,
  size = 160,
  className,
}) => {
  const state = resolveMascotState(learned, goal)
  const waterPct = mascotWaterPct(learned, goal)

  // Lớp nước cao full khung → dịch xuống để phần lộ ra đúng `waterPct`.
  const waterOffsetPct = 100 - waterPct

  // Bong bóng chỉ nổi trong khoảng nước đang có (không vượt mặt nước).
  const bubbleRisePx = Math.round(size * (waterPct / 100) * 0.62)

  return (
    <div
      className={cn(styles.frame, className)}
      style={{ width: size, height: size }}
      role="img"
      aria-label={`Đã học ${learned}/${goal} từ`}
    >
      {/* ---------- Lớp 1: nền gradient ---------- */}
      <div className={cn(styles.bg, STATE_BG_CLASS[state])} aria-hidden="true" />

      {/* ---------- Lớp 2: ảnh mèo ---------- */}
      <div className={styles.catLayer} aria-hidden="true">
        <img
          // `key` buộc remount khi đổi state → ảnh mới fade vào.
          key={state}
          src={STATE_SRC[state]}
          alt=""
          draggable={false}
          className={cn(styles.cat, state === 'drowning' && styles.catBobble)}
        />
      </div>

      {/* ---------- Lớp 3: nước (đè lên mèo) ---------- */}
      <div
        className={cn(styles.water, state === 'fire' && styles.waterHidden)}
        style={{ transform: `translate3d(0, ${waterOffsetPct}%, 0)` }}
        aria-hidden="true"
      >
        <div className={cn(styles.waterBody, STATE_WATER_CLASS[state])}>
          {BUBBLES.map((bubble) => (
            <span
              key={bubble.left}
              className={styles.bubble}
              style={{
                left: bubble.left,
                width: Math.max(4, Math.round(size * bubble.sizeRatio)),
                height: Math.max(4, Math.round(size * bubble.sizeRatio)),
                // Biến CSS (không phải thuộc tính layout) → set mỗi render.
                ['--bubble-duration' as string]: `${bubble.duration}s`,
                ['--bubble-delay' as string]: `${bubble.delay}s`,
                ['--rise' as string]: `${bubbleRisePx}px`,
              }}
            />
          ))}
        </div>

        {/* Mép nước: 2 lớp sóng trôi ngược chiều, tốc độ khác nhau. */}
        <div className={styles.waveLayer}>
          <svg
            className={cn(styles.wave, styles.waveSlow)}
            viewBox="0 0 200 16"
            preserveAspectRatio="none"
            focusable="false"
          >
            <path d={WAVE_PATH} fill={STATE_SURFACE_COLOR[state]} />
          </svg>
          <svg
            className={cn(styles.wave, styles.waveFast)}
            viewBox="0 0 200 16"
            preserveAspectRatio="none"
            focusable="false"
          >
            <path d={WAVE_PATH} fill={STATE_SURFACE_COLOR[state]} />
          </svg>
        </div>
      </div>
    </div>
  )
}

export default MascotProgress
