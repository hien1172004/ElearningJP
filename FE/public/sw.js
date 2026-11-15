/* Service Worker cho Web Push Notification (ElearningJP). */

self.addEventListener('push', (event) => {
  let data = { title: 'ElearningJP', body: '', url: '/' }
  try {
    if (event.data) {
      const payload = event.data.json()
      data = { ...data, ...payload }
    }
  } catch (e) {
    // Payload không phải JSON — dùng giá trị mặc định.
  }

  const options = {
    body: data.body,
    icon: '/favicon.svg',
    badge: '/favicon.svg',
    data: { url: data.url || '/' },
  }

  event.waitUntil(self.registration.showNotification(data.title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url || '/'

  event.waitUntil(
    self.clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        const client = clientList.find((c) => 'focus' in c)
        if (!client) return self.clients.openWindow(url)
        // Tab đã mở: chuyển tới đúng trang đích rồi đưa lên trước.
        // navigate() lỗi với tab chưa do SW điều khiển → mở tab mới.
        return client
          .navigate(url)
          .then((c) => (c ?? client).focus())
          .catch(() => self.clients.openWindow(url))
      }),
  )
})
