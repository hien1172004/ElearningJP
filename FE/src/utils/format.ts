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

/**
 * Làm sạch chuỗi tiếng Nhật:
 * 1. Chuyển đổi literal `\n` thành ký tự xuống dòng thực tế `\n`
 * 2. Loại bỏ dấu cách trắng nhân tạo vô lý giữa các ký tự Kanji, Hiragana, Katakana và dấu câu
 */
export const cleanJapaneseText = (text?: string | null): string => {
  if (!text) return ''
  let cleaned = text.replace(/\\n/g, '\n')
  // Loại bỏ khoảng trắng giữa các chữ tiếng Nhật (Kana, Kanji)
  cleaned = cleaned.replace(/([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])[\s\u3000]+([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])/g, '$1$2')
  // Loại bỏ khoảng trắng trước/sau dấu câu tiếng Nhật
  cleaned = cleaned.replace(/([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])[\s\u3000]+([。、？！])/g, '$1$2')
  cleaned = cleaned.replace(/([。、？！])[\s\u3000]+([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])/g, '$1$2')
  return cleaned
}

/**
 * Loại bỏ chỉ dẫn Mondai bị trùng lặp ở đầu nội dung câu hỏi nếu có
 */
export const cleanQuestionPrompt = (questionText?: string | null, instructionJp?: string | null): string => {
  if (!questionText) return ''
  let cleaned = cleanJapaneseText(questionText).trim()
  if (instructionJp) {
    const cleanInstr = cleanJapaneseText(instructionJp).trim()
    if (cleanInstr && cleaned.startsWith(cleanInstr)) {
      cleaned = cleaned.substring(cleanInstr.length).trim()
    }
  }
  return cleaned
}

/**
 * Bản đồ chuyển đổi Mondai Code kỹ thuật sang Tiêu đề chuẩn JLPT
 */
const MONDAI_MAP: Record<string, { jp: string; vi: string }> = {
  KANJI_READING: { jp: '問題 1: 漢字読み', vi: 'Cách đọc chữ Hán' },
  ORTHOGRAPHY: { jp: '問題 2: 表記', vi: 'Cách viết chữ Hán / Hiragana' },
  WORD_FORMATION: { jp: '問題 3: 語形成', vi: 'Cấu tạo từ vựng' },
  CONTEXTUAL_USE: { jp: '問題 3: 文脈規定', vi: 'Từ vựng theo ngữ cảnh' },
  SYNONYMS: { jp: '問題 4: 言い換え類義', vi: 'Từ gần nghĩa / Cách diễn đạt tương đương' },
  PROPER_USAGE: { jp: '問題 5: 用法', vi: 'Cách dùng từ trong câu' },
  SENTENTIAL_GRAMMAR: { jp: '問題 1: 文法形式の判断', vi: 'Ngữ pháp câu' },
  SENTENCE_COMPOSITION: { jp: '問題 2: 文の組み立て（★）', vi: 'Sắp xếp câu tìm vị trí dấu sao' },
  TEXT_GRAMMAR: { jp: '問題 3: 文章の文法', vi: 'Ngữ pháp đoạn văn (Điền vào chỗ trống)' },
  SHORT_PASSAGE: { jp: '問題 4: 短文読解', vi: 'Đọc hiểu đoạn văn ngắn' },
  MEDIUM_PASSAGE: { jp: '問題 5: 中文読解', vi: 'Đọc hiểu đoạn văn trung' },
  LONG_PASSAGE: { jp: '問題 6: 長文読解', vi: 'Đọc hiểu đoạn văn dài' },
  COMPARISON_INTEGRATED: { jp: '問題 7: 統合理解（比較）', vi: 'Đọc hiểu so sánh đối chiếu (Nhiều văn bản A & B)' },
  THEMATIC_LONG: { jp: '問題 8: 主張理解（長文）', vi: 'Đọc hiểu nắm bắt chủ kiến tác giả' },
  INFORMATION_RETRIEVAL: { jp: '問題: 情報検索', vi: 'Tìm kiếm thông tin (Tờ rơi / Bảng biểu)' },
  TASK_BASED: { jp: '問題 1: 課題理解', vi: 'Hiểu nhiệm vụ hành động' },
  KEY_POINT: { jp: '問題 2: ポイント理解', vi: 'Hiểu điểm mấu chốt' },
  GENERAL_OUTLINE: { jp: '問題 3: 概要理解', vi: 'Hiểu khái quát nội dung' },
  UTTERANCE_EXPRESSIONS: { jp: '問題 4: 発話表現', vi: 'Diễn đạt lời nói (Tranh có mũi tên)' },
  QUICK_RESPONSE: { jp: '問題 4: 即時応答', vi: 'Phản xạ nhanh câu hỏi - đáp' },
  INTEGRATED_COMPREHENSION: { jp: '問題 5: 統合理解（聴解）', vi: 'Nghe hiểu tổng hợp thảo luận nhiều người' },
}

export const formatMondaiDisplay = (code?: string, nameVi?: string) => {
  if (code && MONDAI_MAP[code]) {
    return MONDAI_MAP[code]
  }
  return {
    jp: '問題',
    vi: nameVi || 'Bài thi',
  }
}

export const formatSectionName = (sessionType?: string, orderIndex?: number, customTitle?: string) => {
  if (sessionType === 'VOCABULARY') {
    return `Phần ${orderIndex || 1}: Kiến thức ngôn ngữ (Chữ Hán - Từ vựng)`
  }
  if (sessionType === 'GRAMMAR_READING') {
    return `Phần ${orderIndex || 2}: Kiến thức ngôn ngữ (Ngữ pháp) & Đọc hiểu`
  }
  if (sessionType === 'LISTENING') {
    return `Phần ${orderIndex || 3}: Nghe hiểu (聴解)`
  }
  if (sessionType === 'KNOWLEDGE_READING') {
    return `Phần ${orderIndex || 1}: Kiến thức ngôn ngữ (Từ vựng, Ngữ pháp) & Đọc hiểu`
  }
  return customTitle || `Phần ${orderIndex || 1}`
}
