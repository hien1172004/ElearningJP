import React from 'react'
import { useStudyTimer } from '@/hooks/useStudyTimer'

/**
 * StudyTimerTracker — component "vô hình" bật đếm thời gian học.
 *
 * Render ra `null` (không chiếm không gian), chỉ gọi hook để theo dõi.
 *
 * ```tsx
 * // Trong LessonViewerPage / DictionaryPage
 * <StudyTimerTracker />
 * ```
 *
 * @param intervalMs chu kỳ gửi heartbeat (ms). Mặc định 30_000.
 * @param idleMs     sau thời gian không tương tác này thì coi là idle. Mặc định 60_000.
 * @param enabled    bật/tắt. Mặc định `true`.
 */
export interface StudyTimerTrackerProps {
  intervalMs?: number
  idleMs?: number
  enabled?: boolean
}

export const StudyTimerTracker: React.FC<StudyTimerTrackerProps> = ({
  intervalMs,
  idleMs,
  enabled = true,
}) => {
  useStudyTimer({ enabled, intervalMs, idleMs })
  return null
}

export default StudyTimerTracker