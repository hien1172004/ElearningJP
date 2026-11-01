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

export const flattenNestedRuby = (text: string): string => {
  if (!text || !text.includes('<ruby>')) return text
  let res = text.replace(
    /<ruby>\s*([^<]+?)\s*<rt>\s*<ruby>(?:[\s\S]*?<rt>([^<]+)<\/rt>|[\s\S]*?<rb>([^<]+)<\/rb>|([^<]+))[\s\S]*?<\/ruby>\s*<\/rt>\s*<\/ruby>/gi,
    (_match, base, rtFuri, rbFuri, rawFuri) => {
      const furi = (rtFuri || rbFuri || rawFuri || '').trim()
      return `<ruby>${base.trim()}<rt>${furi}</rt></ruby>`
    }
  )
  res = res.replace(
    /<ruby>\s*<rb>\s*([^<]+?)\s*<\/rb>\s*<rt>\s*([^<]+?)\s*<\/rt>\s*<\/ruby>/gi,
    (_m, b, f) => `<ruby>${b.trim()}<rt>${f.trim()}</rt></ruby>`
  )
  return res
}

/**
 * Làm sạch chuỗi tiếng Nhật:
 * 1. Chuyển đổi literal `\n` thành ký tự xuống dòng thực tế `\n`
 * 2. Loại bỏ dấu cách trắng nhân tạo vô lý giữa các ký tự Kanji, Hiragana, Katakana và dấu câu
 * 3. Chuẩn hóa thẻ <ruby> Furigana không bị lồng nhau (gây xếp chồng chữ hiển thị)
 */
export const cleanJapaneseText = (text?: string | null): string => {
  if (!text) return ''
  let cleaned = text.replace(/\\n/g, '\n')
  cleaned = flattenNestedRuby(cleaned)
  // Loại bỏ khoảng trắng giữa các chữ tiếng Nhật (Kana, Kanji)
  cleaned = cleaned.replace(/([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])[\s\u3000]+([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])/g, '$1$2')
  // Loại bỏ khoảng trắng trước/sau dấu câu tiếng Nhật
  cleaned = cleaned.replace(/([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])[\s\u3000]+([。、？！])/g, '$1$2')
  cleaned = cleaned.replace(/([。、？！])[\s\u3000]+([\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff])/g, '$1$2')
  return cleaned
}

/**
 * Format các số chỗ trống 【 21 】, [21] thành thẻ badge HTML nổi bật
 */
export const formatJapaneseBlanksHtml = (text?: string | null): string => {
  if (!text) return ''
  const cleaned = text.replace(/\\n/g, '\n')
  return cleaned.replace(
    /([【\[]\s*(\d+)\s*[】\]])/g,
    '<span data-blank-num="$2" class="jlpt-blank-pill inline-flex items-center justify-center font-bold px-2 py-0.5 mx-1 text-sm rounded border border-black bg-white text-black hover:bg-gray-100 transition cursor-pointer select-none font-mono">【 $2 】</span>'
  )
}

/**
 * Chuyển đổi và định dạng toàn bộ bài đọc tiếng Nhật (văn xuôi, bảng biểu Markdown, tờ rơi, thông báo)
 * thành HTML hiển thị đẹp mắt, rõ ràng và dễ nhìn nhất cho người học.
 */
export const formatJapanesePassageHtml = (rawText?: string | null): string => {
  if (!rawText) return ''
  const text = rawText.replace(/\\n/g, '\n').trim()
  if (!text) return ''

  // Nếu đã chứa mã HTML table hoặc div class phức tạp thì chỉ format blanks
  if (text.includes('<table') || text.includes('<div class=')) {
    return formatJapaneseBlanksHtml(text)
  }

  const lines = text.split('\n')
  const resultBlocks: string[] = []
  let tableBuffer: string[] = []

  const flushTable = () => {
    if (tableBuffer.length === 0) return
    const rows = tableBuffer
      .map(r => r.trim())
      .filter(r => r.startsWith('|') && r.endsWith('|'))

    if (rows.length >= 2) {
      const parseCells = (row: string) =>
        row.slice(1, -1).split('|').map(c => c.trim())

      const hasSeparator = rows.some(r => /^[\|\s\-:]+$/.test(r))
      let headerCells: string[] = []
      let bodyRows: string[][] = []

      if (hasSeparator) {
        const sepIdx = rows.findIndex(r => /^[\|\s\-:]+$/.test(r))
        if (sepIdx > 0) {
          headerCells = parseCells(rows[0])
          bodyRows = rows.slice(sepIdx + 1).map(parseCells)
        } else {
          bodyRows = rows.map(parseCells)
        }
      } else {
        headerCells = parseCells(rows[0])
        bodyRows = rows.slice(1).map(parseCells)
      }

      let tableHtml = '<div class="my-3.5 overflow-x-auto rounded-lg border border-gray-400 bg-white">'
      tableHtml += '<table class="w-full text-left border-collapse text-xs md:text-sm font-serif text-black">'
      if (headerCells.length > 0) {
        tableHtml += '<thead class="bg-gray-100 text-black font-bold border-b border-gray-400"><tr>'
        headerCells.forEach(cell => {
          tableHtml += `<th class="p-2.5 md:p-3 border-r border-gray-300 last:border-r-0 whitespace-nowrap md:whitespace-normal">${formatJapaneseBlanksHtml(cleanJapaneseText(cell))}</th>`
        })
        tableHtml += '</tr></thead>'
      }
      if (bodyRows.length > 0) {
        tableHtml += '<tbody class="divide-y divide-gray-200">'
        bodyRows.forEach((row) => {
          tableHtml += '<tr>'
          row.forEach(cell => {
            tableHtml += `<td class="p-2.5 md:p-3 border-r border-gray-200 last:border-r-0 leading-relaxed">${formatJapaneseBlanksHtml(cleanJapaneseText(cell))}</td>`
          })
          tableHtml += '</tr>'
        })
        tableHtml += '</tbody>'
      }
      tableHtml += '</table></div>'
      resultBlocks.push(tableHtml)
    } else {
      tableBuffer.forEach(l => {
        resultBlocks.push(`<p class="leading-relaxed my-1 font-serif text-slate-800 dark:text-slate-200">${formatJapaneseBlanksHtml(cleanJapaneseText(l))}</p>`)
      })
    }
    tableBuffer = []
  }

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i]
    const line = rawLine.trim()

    // Kiểm tra dòng bảng Markdown
    if (line.startsWith('|') && line.endsWith('|')) {
      tableBuffer.push(line)
      continue
    } else if (tableBuffer.length > 0) {
      flushTable()
    }

    if (!line) {
      resultBlocks.push('<div class="h-2.5"></div>')
      continue
    }

    // Tiêu đề Markdown: ## hoặc ###
    if (line.startsWith('## ') || line.startsWith('### ')) {
      const headingText = line.replace(/^#{2,3}\s*/, '').trim()
      resultBlocks.push(
        `<h4 class="my-3 font-bold text-base md:text-lg text-black font-serif border-l-2 border-black pl-2.5">
          <span>${cleanJapaneseText(headingText)}</span>
        </h4>`
      )
      continue
    }

    // Tiêu đề chính độc lập trong ngoặc vuông to: ví dụ 【公園で遊ぶときの注意】
    if (/^【[^】\n]{3,}】$/.test(line)) {
      resultBlocks.push(
        `<div class="my-4 text-center">
          <span class="inline-block text-lg md:text-xl font-bold font-serif text-black border-b border-black pb-0.5">
            ${cleanJapaneseText(line)}
          </span>
        </div>`
      )
      continue
    }

    // Chú thích / Lưu ý đặc biệt: ※ hoặc 【注意】 hoặc （注） hoặc (注)
    if (line.startsWith('※') || line.startsWith('【注意】') || line.startsWith('（注）') || line.startsWith('(注)')) {
      resultBlocks.push(
        `<div class="my-3 p-3 rounded-lg border border-gray-400 bg-white text-black text-sm md:text-base leading-relaxed font-serif">
          <div class="leading-relaxed flex-1">${formatJapaneseBlanksHtml(cleanJapaneseText(line))}</div>
        </div>`
      )
      continue
    }

    // Dòng gạch đầu dòng: ・ hoặc • hoặc - hoặc *
    if (line.startsWith('・') || line.startsWith('•') || line.startsWith('- ') || line.startsWith('* ')) {
      const bulletContent = line.replace(/^[・•\-\*]\s*/, '').trim()
      resultBlocks.push(
        `<div class="my-1.5 flex items-start gap-2 text-sm md:text-base text-black font-serif">
          <span class="text-black font-bold shrink-0 leading-relaxed">・</span>
          <div class="leading-relaxed flex-1">${formatJapaneseBlanksHtml(cleanJapaneseText(bulletContent))}</div>
        </div>`
      )
      continue
    }

    // Dòng văn xuôi thông thường
    resultBlocks.push(
      `<p class="leading-[2.2] md:leading-[2.4] text-base md:text-[17px] text-black font-serif my-2.5 tracking-wide">
        ${formatJapaneseBlanksHtml(cleanJapaneseText(line))}
      </p>`
    )
  }

  if (tableBuffer.length > 0) {
    flushTable()
  }

  return resultBlocks.join('')
}

/**
 * Làm sạch và định dạng nội dung câu hỏi tiếng Nhật:
 * 1. Loại bỏ chỉ dẫn Mondai bị trùng lặp ở đầu nội dung câu hỏi nếu có.
 * 2. Tự động chuyển đổi ngoặc vuông 【từ】 thành gạch chân <u>từ</u> (trừ ô số như 【 21 】).
 * 3. Tự động nhận diện câu thoại đối đáp (A「...」 B「...」 hoặc 山田「...」 田中「...」):
 *    Tách thành 2 hàng riêng biệt với thẻ block để hiển thị rõ ràng, chuyên nghiệp.
 */
export const cleanQuestionPrompt = (questionText?: string | null, instructionJp?: string | null): string => {
  if (!questionText) return ''
  let cleaned = questionText.replace(/\\n/g, '\n').trim()
  if (instructionJp) {
    const cleanInstr = cleanJapaneseText(instructionJp).trim()
    if (cleanInstr && cleaned.startsWith(cleanInstr)) {
      cleaned = cleaned.substring(cleanInstr.length).trim()
    }
  }

  // Chuyển 【từ】 thành <u>từ</u> (trừ ô số như 【 21 】 của bài đục lỗ)
  cleaned = cleaned.replace(/【(?!\s*\d+\s*】)(.*?)】/g, '<u>$1</u>')

  // Loại bỏ hoàn toàn thẻ ruby/furigana trên từ gạch chân <u> (tránh lộ đáp án ở câu hỏi đọc chữ Hán)
  cleaned = cleaned.replace(/<u>\s*<ruby>(.*?)(?:<rt>.*?<\/rt>)?<\/ruby>\s*<\/u>/gi, '<u>$1</u>')
  cleaned = cleaned.replace(/<ruby>\s*<u>(.*?)<\/u>(?:<rt>.*?<\/rt>)?<\/ruby>/gi, '<u>$1</u>')

  // Tách lượt hội thoại giữa các nhân vật thành từng dòng riêng biệt
  // Trường hợp 1: Kết thúc bằng 」 theo sau là tên/nhân vật + 「 hoặc dấu hai chấm (ví dụ: A「...」 B「...」 hoặc 山田「...」 田中「...」)
  cleaned = cleaned.replace(/(」)\s*([A-Za-z0-9\u3040-\u30ff\u4e00-\u9faf]{1,10}[「：:])/g, '$1\n$2')
  // Trường hợp 2: Kết thúc bằng dấu câu 。/？/? theo sau là tên nhân vật + 「 hoặc dấu hai chấm
  cleaned = cleaned.replace(/([。？！\?!])\s*([A-Za-z0-9\u3040-\u30ff\u4e00-\u9faf]{1,8}[「：:])/g, '$1\n$2')

  const rawLines = cleaned.split('\n').map((l) => l.trim()).filter(Boolean)
  if (rawLines.length > 1) {
    return rawLines
      .map((line) => `<div class="block leading-relaxed my-0.5">${cleanJapaneseText(line)}</div>`)
      .join('')
  }

  return cleanJapaneseText(cleaned)
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

/**
 * Kiểm tra xem câu hỏi nghe có phải là câu hỏi thuần chữ (không có tranh vẽ / không cần ảnh) hay không.
 * Các trường hợp thuần chữ:
 * - Mondai 2 (KEY_POINT) và Mondai 4 (QUICK_RESPONSE) trong JLPT KHÔNG BAO GIỜ có tranh.
 * - Các câu hỏi có >= 2 lựa chọn là chữ tiếng Nhật thực tế (không phải nhãn placeholder "Lựa chọn 1", "1", "A"...)
 *   và không phải Mondai 3 (UTTERANCE_EXPRESSIONS - tranh mũi tên tình huống).
 */
export const isQuestionTextOnly = (q: any): boolean => {
  if (!q) return false
  const mCode = (q.mondai_code || q.mondaiCode || '').toUpperCase()
  if (['KEY_POINT', 'QUICK_RESPONSE'].includes(mCode)) return true
  if (mCode === 'UTTERANCE_EXPRESSIONS') return false

  // Nếu câu hỏi đã được gắn ảnh tình huống (imageUrl) hoặc có tranh đáp án (hasVisualOptions)
  // thì tuyệt đối KHÔNG PHẢI là câu hỏi thuần chữ!
  if (q.imageUrl || q.image_url || q.hasVisualOptions || q.has_visual_options) {
    return false
  }

  // Nếu câu hỏi có từ khóa hình ảnh (như 写真, 絵, 地図...) thì không phải thuần chữ
  const qText = q.question_text || q.questionText || ''
  if (/写真|絵|地図|図|グラフ/.test(qText)) {
    return false
  }

  const opts = q.options || []
  const meaningfulOpts = opts.filter((o: any) => {
    const t = (o.option_text || o.optionText || '').trim()
    if (!t) return false
    // Nhãn tranh (như "アイ", "アエ", "ア", "イ", "A", "1"...) không phải là nội dung chữ thực tế
    if (/^[ア-ンA-Za-z0-9\s・、,/\-()（）①-④]+$/.test(t)) {
      return false
    }
    return (
      t.length > 1 &&
      !t.startsWith('Lựa chọn') &&
      !t.startsWith('Đáp án') &&
      !t.startsWith('Option') &&
      !t.startsWith('Phương án') &&
      !t.startsWith('選択肢') &&
      !/^\(?\d+\)?$/.test(t)
    )
  })

  return meaningfulOpts.length >= 2
}
/*
 * Format thời gian tương đối — dùng cho bình luận, hoạt động gần đây.
 *
 * - `< 60 giây`      → "vừa xong"
 * - `< 60 phút`      → "5 phút trước"
 * - `< 24 giờ`       → "2 giờ trước"
 * - `< 7 ngày`       → "3 ngày trước"
 * - còn lại          → "dd/MM/yyyy"
 *
 * @param value chuỗi ISO hoặc Date
 */
export const formatRelativeTime = (value: string | Date | null | undefined): string => {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const diffMs = Date.now() - date.getTime()
  // Tương lai (lệch múi giờ) → coi như "vừa xong"
  if (diffMs < 0) return 'vừa xong'

  const seconds = Math.floor(diffMs / 1000)
  if (seconds < 60) return 'vừa xong'

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} phút trước`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} giờ trước`

  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} ngày trước`

  return formatDate(date)
}

/**
 * Làm sạch tiêu đề bài đọc hiểu:
 * 1. Loại bỏ các thẻ HTML như <ruby>, <rt>, <rb>, <p>, <span>... (tránh lộ cú pháp raw HTML ra ngoài UI)
 * 2. Nhận diện nếu tiêu đề bị gán nhầm câu chỉ dẫn đề thi Mondai (vd: "もんだい５ つぎの ぶんしょうを 読んで...")
 * 3. Nhận diện nếu tiêu đề bị gán nhầm câu dẫn bối cảnh (vd: "(3) 森さんの机の上に...")
 */
export const cleanPassageTitle = (rawTitle?: string | null, fallback: string = 'Đoạn văn đọc hiểu'): string => {
  if (!rawTitle) return fallback
  let cleaned = String(rawTitle).trim()
  if (!cleaned) return fallback

  // Lột bỏ thẻ <rt>...</rt> trước để chỉ giữ lại chữ Kanji/Kana gốc nếu có
  cleaned = cleaned.replace(/<rt>[\s\S]*?<\/rt>/gi, '')
  // Lột bỏ toàn bộ thẻ HTML còn lại
  cleaned = cleaned.replace(/<[^>]+>/g, '').trim()
  // Lột khoảng trắng thừa
  cleaned = cleaned.replace(/\s+/g, ' ').trim()

  if (!cleaned) return fallback

  // Kiểm tra nếu là câu chỉ dẫn đề bài (Mondai instruction)
  const isInstruction = /(?:もんだい|問題)\s*\d+|つぎの\s*(?:ぶんしょう|文章)|しつもんに\s*(?:こたえて|かたえて|答えて)|いちばん\s*いい\s*ものを|えらんで\s*ください|何を入れますか/i.test(cleaned)
  if (isInstruction) {
    return fallback
  }

  // Kiểm tra nếu là câu dẫn bối cảnh bắt đầu bằng số thứ tự (1), (2), (3)...
  const numMatch = cleaned.match(/^[（\(]\s*([0-9０-９\d]+)\s*[）\)]/)
  if (numMatch && (cleaned.length >= 15 || /[。？！]/.test(cleaned))) {
    return `Bài đọc (${numMatch[1]})`
  }

  // Nếu quá dài (> 45 ký tự) và có các từ chỉ câu văn thông thường, fallback về tên chuẩn
  if (cleaned.length > 30 && (/[。、]/.test(cleaned) || /(?:あります|います|でした|ました|メモ|手紙)/.test(cleaned))) {
    return fallback
  }

  return cleaned || fallback
}

/**
 * Đảm bảo nội dung bài đọc có câu dẫn tình huống (nếu câu dẫn bị lưu nhầm vào title trong các đề cũ)
 */
export const ensurePassageContentWithIntro = (contentJp?: string | null, rawTitle?: string | null): string => {
  if (!contentJp) return ''
  if (!rawTitle) return contentJp

  const cleanTitle = rawTitle.replace(/<rt>[\s\S]*?<\/rt>/gi, '').replace(/<[^>]+>/g, '').trim()
  const isInstruction = /(?:もんだい|問題)\s*\d+|つぎの\s*(?:ぶんしょう|文章)|しつもんに\s*(?:こたえて|かたえて|答えて)|いちばん\s*いい\s*ものを|えらんで\s*ください|何を入れますか/i.test(cleanTitle)
  if (isInstruction) {
    return contentJp
  }

  const numMatch = cleanTitle.match(/^[（\(]\s*([0-9０-９\d]+)\s*[）\)]/)
  const isContextIntro = (numMatch && (cleanTitle.length >= 15 || /[。？！]/.test(cleanTitle))) ||
    (cleanTitle.length >= 18 && (cleanTitle.includes('あります') || cleanTitle.includes('います') || cleanTitle.includes('でした') || cleanTitle.includes('ました') || cleanTitle.includes('メモ') || cleanTitle.includes('手紙')))

  if (isContextIntro) {
    const cleanContent = contentJp.replace(/<[^>]+>/g, '')
    const coreIntro = cleanTitle.replace(/^[（\(]\s*[0-9０-９\d]+\s*[）\)]\s*/, '').trim()
    if (coreIntro && !cleanContent.includes(coreIntro)) {
      return `${rawTitle}\n\n${contentJp}`
    }
  }

  return contentJp
}
