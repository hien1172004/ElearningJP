import { axiosClient } from './api/axiosClient'
import { storage } from '@/utils/storage'
import type {
  ApiResponse,
  DictionaryLookupPayload,
  DictionaryLookupResult,
  LeaderboardResponse,
  MyRank,
  ProgressCalendar,
  ProgressSummary,
  StudyHeartbeatPayload,
  StudyHeartbeatResult,
  TopLookup,
} from '@/types'

/**
 * Service cho Progress page (tiến độ học tập).
 *
 * <p>Hai endpoint "instrumentation" (ghi sự kiện) được thiết kế
 * <b>fire-and-forget</b>: lỗi network KHÔNG được làm hỏng UX của
 * người dùng đang học. Vì vậy chúng không dùng React Query.</p>
 */
export const progressService = {
  // ----------------------------------------------------------------
  // BƯỚC 4 — Khoi 1: tong hop theo ky
  // ----------------------------------------------------------------
  /**
   * Tổng hợp tiến độ học tập.
   *
   * @param from ngày bắt đầu `yyyy-MM-dd` (bỏ trống → BE tự default 30 ngày)
   * @param to   ngày kết thúc `yyyy-MM-dd` (bỏ trống → hôm nay)
   */
  getSummary: async (params?: {
    from?: string
    to?: string
  }): Promise<ApiResponse<ProgressSummary>> => {
    return axiosClient.get('/progress/summary', { params })
  },

  // ----------------------------------------------------------------
  // Khối 2 — Lịch học + streak
  // ----------------------------------------------------------------
  /**
   * Lịch học + chuỗi streak theo tháng.
   *
   * @param month tháng `yyyy-MM` (bỏ trống → tháng hiện tại)
   */
  getCalendar: async (month?: string): Promise<ApiResponse<ProgressCalendar>> => {
    return axiosClient.get('/progress/calendar', {
      params: month ? { month } : undefined,
    })
  },

  // ----------------------------------------------------------------
  // Khối 4 — Bảng từ đã tra nhiều nhất
  // ----------------------------------------------------------------
  /**
   * Top N từ/đối tượng được tra cứu nhiều nhất.
   *
   * <p>Mặc định xếp hạng <b>toàn bộ lịch sử</b> — bỏ trống `from` và `to`.</p>
   *
   * @param limit số dòng tối đa (bỏ trống → 30)
   */
  getTopLookups: async (params?: {
    from?: string
    to?: string
    limit?: number
  }): Promise<ApiResponse<TopLookup[]>> => {
    return axiosClient.get('/progress/top-lookups', { params })
  },

  // ----------------------------------------------------------------
  // KHỐI 5 — Bảng xếp hạng tuần
  // ----------------------------------------------------------------
  /**
   * Lấy bảng xếp hạng tuần.
   *
   * @param week tuần ISO `YYYY-'W'ww` (bỏ trống → tuần hiện tại)
   */
  getLeaderboard: async (
    week?: string,
  ): Promise<ApiResponse<LeaderboardResponse>> => {
    return axiosClient.get('/progress/leaderboard', {
      params: week ? { week } : undefined,
    })
  },

  /**
   * Lấy hạng cá nhân trong tuần.
   *
   * @param week tuần ISO `YYYY-'W'ww` (bỏ trống → tuần hiện tại)
   */
  getMyRank: async (week?: string): Promise<ApiResponse<MyRank>> => {
    return axiosClient.get('/progress/leaderboard/me', {
      params: week ? { week } : undefined,
    })
  },

  /**
   * Refresh bảng xếp hạng tuần (bù cho trường hợp ZSET bị lỡ).
   *
   * <p><b>Không sửa điểm giảm</b> — vì BE dùng `ZADD GT` (chỉ nâng điểm).
   * Muốn sửa điểm giảm → endpoint `refresh/hard` (chỉ ADMIN).</p>
   *
   * <p>BE có cooldown 30s → gọi lại trong 30s sẽ nhận HTTP 429.</p>
   */
  refreshLeaderboard: async (
    week?: string,
  ): Promise<ApiResponse<LeaderboardResponse>> => {
    return axiosClient.post('/progress/leaderboard/refresh', null, {
      params: week ? { week } : undefined,
    })
  },

  // ----------------------------------------------------------------
  // BƯỚC 2 — Ghi nhận 1 lần tra từ điển
  // ----------------------------------------------------------------
  /**
   * Ghi nhận người dùng đã tra 1 từ → +1 "từ đã học".
   * Định nghĩa nghiệp vụ: 1 lần tra = 1 từ đã học.
   */
  recordLookup: async (
    payload: DictionaryLookupPayload,
  ): Promise<ApiResponse<DictionaryLookupResult>> => {
    return axiosClient.post('/dictionary/lookup', payload)
  },

  // ----------------------------------------------------------------
  // BƯỚC 3 — Heartbeat thời gian học
  // ----------------------------------------------------------------
  /**
   * Gửi heartbeat bằng `fetch` (bình thường).
   *
   * <p>Được gọi khi tab đóng trước `sendBeacon`, và mỗi 30s khi là leader.</p>
   */
  sendHeartbeat: async (
    payload: StudyHeartbeatPayload,
  ): Promise<ApiResponse<StudyHeartbeatResult>> => {
    return axiosClient.post('/study/heartbeat', payload)
  },

  /**
   * Gửi heartbeat khi user đóng tab, dùng `fetch(..., { keepalive: true })`.
   *
   * <p>Vì sao không dùng `navigator.sendBeacon`?</p>
   * <ul>
   *   <li>`sendBeacon` KHÔNG cho phép set custom header → không gửi được
   *       `Authorization: Bearer`, mà BE chỉ đọc token từ header.</li>
   *   <li>`fetch` với `keepalive: true` cho phép set header và vẫn được
   *       gửi khi page đang unload, tối đa 64KB body — payload ta gửi rất nhỏ.</li>
   * </ul>
   *
   * <p>Bỏ qua cả response (fire-and-forget) — lỗi ở đây không quan trọng.</p>
   *
   * @returns true nếu request đã được khởi tạo thành công
   */
  sendHeartbeatOnUnload: (payload: StudyHeartbeatPayload): boolean => {
    if (typeof window === 'undefined' || typeof fetch === 'undefined') {
      return false
    }
    const base =
      import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'
    const token = storage.getAccessToken()
    if (!token) return false

    try {
      void fetch(`${base}/study/heartbeat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
        keepalive: true,
      }).catch(() => undefined)
      return true
    } catch {
      return false
    }
  },
}