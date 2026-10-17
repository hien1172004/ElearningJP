export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('vi-VN').format(num)
}

export const formatDate = (date: string | Date): string => {
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(date))
}

/**
 * Chuyển chuỗi tiếng Việt / Nhật / có dấu → slug URL-friendly.
 * Ví dụ: "Bài 1: Số tự nhiên" → "bai-1-so-tu-nhien"
 *        "日本語の 수업" → "nihongo-no-shu-ke"
 *        "Ngữ pháp ①" → "ngu-phap-1"
 */
export const toSlug = (text: string): string => {
  return text
    .toLowerCase()
    // Thay chữ có dấu tiếng Việt → không dấu
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    // Bỏ ký tự không phải chữ cái / số / khoảng trắng
    .replace(/[^a-z0-9\s\u3040-\u30ff\u4e00-\u9faf-]/g, '')
    // Thay khoảng trắng / gạch dưới / gạch ngang nhiều → 1 gạch ngang
    .replace(/[\s_]+/g, '-')
    .replace(/-+/g, '-')
    // Bỏ gạch ngang đầu/cuối
    .replace(/^-+|-+$/g, '')
}
