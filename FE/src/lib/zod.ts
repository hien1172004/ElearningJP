import { z } from 'zod'

/**
 * Cấu hình thông báo lỗi zod mặc định bằng tiếng Việt cho toàn ứng dụng.
 *
 * Thứ tự ưu tiên của zod v4: thông báo khai báo trong schema (vd `.min(1, '...')`,
 * `z.string({ error: '...' })`) > `customError` bên dưới > `localeError` (bộ tiếng Việt của zod).
 * Vì vậy chỉ những chỗ schema KHÔNG khai báo thông báo mới rơi xuống đây — tránh hiện
 * thông báo kỹ thuật kiểu "Invalid input: expected string, received undefined".
 *
 * Nạp 1 lần ở `main.tsx` (import side-effect).
 */
z.config({
  ...z.locales.vi(),
  customError: (issue) => {
    // Trường bắt buộc chưa được nhập / chưa được chọn
    if (issue.code === 'invalid_type' && (issue.input === undefined || issue.input === null || issue.input === '')) {
      return 'Vui lòng nhập đầy đủ các trường bắt buộc'
    }
    // Sai kiểu dữ liệu (vd: nhập chữ vào ô số) — tránh thông báo kỹ thuật "mong đợi số, nhận được string"
    if (issue.code === 'invalid_type') {
      return 'Giá trị không hợp lệ'
    }
    if (issue.code === 'too_small' && issue.origin === 'string' && issue.minimum === 1) {
      return 'Không được để trống'
    }
    if (issue.code === 'too_big' && issue.origin === 'string') {
      return `Tối đa ${issue.maximum} ký tự`
    }
    return undefined // dùng bộ thông báo tiếng Việt của zod
  },
})
