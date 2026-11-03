/**
 * Web Push — tiện ích phía trình duyệt: đăng ký Service Worker và subscribe push.
 */
import type { PushSubscriptionPayload } from '@/types'

/** VAPID public key — phải khớp `app.web-push.vapid-public-key` ở backend. */
export const VAPID_PUBLIC_KEY =
  (import.meta.env.VITE_VAPID_PUBLIC_KEY as string | undefined) ||
  'BFckzKFFMX4TAtW1lVLtLF8dad9YOyI2Eww2JWSeY845Xz869lC5IMTHsXOujNfAzI6dgbULc8bcFGmU5YLeC1c'

export type { PushSubscriptionPayload }

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = atob(base64)
  const buffer = new ArrayBuffer(rawData.length)
  const outputArray = new Uint8Array(buffer)
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i)
  }
  return outputArray
}

function toBase64Url(buffer: ArrayBuffer | null): string {
  if (!buffer) return ''
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i])
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

export function isPushSupported(): boolean {
  return (
    typeof window !== 'undefined' &&
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  )
}

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!isPushSupported()) return null
  try {
    return await navigator.serviceWorker.register('/sw.js')
  } catch {
    return null
  }
}

/**
 * Xin quyền thông báo và subscribe push. Trả về null nếu trình duyệt không hỗ trợ
 * hoặc người dùng từ chối quyền.
 */
export async function subscribeToPush(): Promise<PushSubscriptionPayload | null> {
  const registration = await registerServiceWorker()
  if (!registration) return null

  let permission = Notification.permission
  if (permission === 'default') {
    permission = await Notification.requestPermission()
  }
  if (permission !== 'granted') return null

  const keyBuffer = urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: keyBuffer,
  })

  return {
    endpoint: subscription.endpoint,
    p256dh: toBase64Url(subscription.getKey('p256dh')),
    auth: toBase64Url(subscription.getKey('auth')),
  }
}
