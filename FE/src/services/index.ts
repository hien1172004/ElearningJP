export * from './api/axiosClient'
export * from './auth.service'
export * from './user.service'
export * from './dictionary.service'
export * from './ai.service'
export * from './course.service'
export * from './exam.service'
export * from './comment.service'
export * from './notification.service'
// Đặt tên cụ thể để tránh đụng `progressService` của course.service.ts
// (service này gọi /progress/lessons/* — lesson progress).
export { progressService as learningProgressService } from './progress.service'
