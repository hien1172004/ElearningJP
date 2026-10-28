/**
 * Logic thuần cho MascotProgress (không JSX) — tách khỏi component để test được
 * và để các hằng số điều chỉnh nằm ở 1 chỗ duy nhất.
 */

// --------------------------------------------------------------------------
// Hằng số có thể chỉnh
// --------------------------------------------------------------------------

/**
 * Mốc tỉ lệ để phân biệt "gần như chưa học" với "đang học dở".
 * {@code ratio < MASCOT_DROWNING_THRESHOLD} → state {@code drowning}.
 */
export const MASCOT_DROWNING_THRESHOLD = 0.4

/**
 * Mực nước tối đa (% chiều cao khung) khi {@code ratio = 0}.
 * Nước hạ dần về 0 khi {@code ratio} tiến tới 1.
 */
export const MASCOT_MAX_WATER = 70

/** Thời gian transition mực nước (ms) — khớp với CSS. */
export const MASCOT_WATER_TRANSITION_MS = 800

// --------------------------------------------------------------------------
// State
// --------------------------------------------------------------------------

export type MascotProgressState = 'drowning' | 'standing' | 'fire'

/**
 * Suy ra tỉ lệ tiến độ, đã kẹp trong {@code [0, 1]}.
 *
 * <p>{@code goal <= 0} (hoặc giá trị không hữu hạn) → {@code 0} để tránh
 * chia cho 0.</p>
 */
export function mascotRatio(learned: number, goal: number): number {
  if (!Number.isFinite(learned) || !Number.isFinite(goal)) return 0
  if (goal <= 0) return 0
  const safeLearned = Math.max(0, learned)
  return Math.min(safeLearned / goal, 1)
}

/**
 * Suy ra state của mascot từ tiến độ.
 *
 * <ul>
 *   <li>{@code goal > 0} và {@code learned >= goal} → {@code fire}</li>
 *   <li>{@code ratio < MASCOT_DROWNING_THRESHOLD} → {@code drowning}</li>
 *   <li>còn lại → {@code standing}</li>
 * </ul>
 */
export function resolveMascotState(
  learned: number,
  goal: number,
): MascotProgressState {
  if (goal > 0 && learned >= goal) return 'fire'
  return mascotRatio(learned, goal) < MASCOT_DROWNING_THRESHOLD
    ? 'drowning'
    : 'standing'
}

/**
 * Mực nước (% chiều cao khung) cho state hiện tại.
 *
 * <p>Công thức: {@code MAX_WATER * (1 - ratio)}. State {@code fire} →
 * {@code 0} (nước cạn hẳn).</p>
 */
export function mascotWaterPct(learned: number, goal: number): number {
  if (resolveMascotState(learned, goal) === 'fire') return 0
  return Math.max(0, MASCOT_MAX_WATER * (1 - mascotRatio(learned, goal)))
}
