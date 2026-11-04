/**
 * Danh sách chủ đề kịch bản luyện nói cho trang quản lý.
 * Mã chủ đề phải khớp `TOPIC_CATEGORIES` trong `pages/speaking/SpeakingLobbyPage.tsx`
 * (sảnh luyện nói lọc kịch bản theo các mã này). Thêm chủ đề mới thì sửa cả hai nơi.
 */
export const SCENARIO_TOPICS = [
  { id: 'COMMUNITY', name: 'Khám phá (cộng đồng)' },
  { id: 'FRIENDSHIP_CHATTING', name: 'Kết bạn & Trò chuyện' },
  { id: 'LIVING_IN_JAPAN', name: 'Sống ở Nhật' },
  { id: 'TRAVEL_TRANSPORT', name: 'Đi lại & Du lịch' },
  { id: 'HELP_EMERGENCY', name: 'Lúc cần giúp đỡ' },
  { id: 'DINING_SHOPPING', name: 'Đi ăn & Mua sắm' },
] as const

export const scenarioTopicName = (topicId: string): string =>
  SCENARIO_TOPICS.find((t) => t.id === topicId)?.name ?? topicId
