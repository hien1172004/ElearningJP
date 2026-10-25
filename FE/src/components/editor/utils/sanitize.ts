import DOMPurify from 'dompurify'

// Cấu hình sanitize whitelist chuẩn JLPT:
// Chỉ cho phép: p, br, strong, b, u, ruby, rt, span[data-speaker]
const SANITIZE_CONFIG: DOMPurify.Config = {
  ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'u', 'ruby', 'rt', 'span'],
  ALLOWED_ATTR: ['data-speaker', 'class'],
}

/**
 * Sanitize chuỗi HTML trước khi lưu vào DB hoặc truyền ra ngoài
 */
export function sanitizeJapaneseHtml(html: string): string {
  if (!html) return ''
  return DOMPurify.sanitize(html, SANITIZE_CONFIG).trim()
}

/**
 * Chuẩn hóa dữ liệu cũ (Legacy):
 * Dữ liệu cũ có thể lưu text thô xuống dòng bằng \n, hoặc chứa thẻ <b>, <u>, <ruby> chưa bọc trong <p>.
 * Hàm này chuẩn hóa về cấu trúc HTML hợp lệ cho TipTap.
 */
export function normalizeLegacy(raw: string): string {
  if (!raw) return '<p></p>'
  
  // Nếu đã bọc <p>, sanitize rồi trả về
  if (raw.trim().startsWith('<p>') || raw.trim().startsWith('<div>')) {
    return sanitizeJapaneseHtml(raw)
  }

  // Chuyển \n thành các đoạn <p>
  const paragraphs = raw.split(/\r?\n/).map(line => {
    const trimmed = line.trim()
    return trimmed ? `<p>${trimmed}</p>` : '<p></p>'
  })

  return sanitizeJapaneseHtml(paragraphs.join(''))
}
