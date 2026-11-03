/**
 * Web Push Notification Types
 * Định nghĩa cấu trúc dữ liệu cho Web Push Notification (VAPID)
 */

/** Kết quả subscribe Web Push — khớp payload POST /api/v1/push/subscribe ở backend. */
export interface PushSubscriptionPayload {
  endpoint: string
  p256dh: string
  auth: string
}
