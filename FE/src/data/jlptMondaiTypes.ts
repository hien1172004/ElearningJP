export interface JlptMondaiDefinition {
  code: string
  mondaiNumber: number
  nameVi: string
  skillType: 'VOCABULARY' | 'GRAMMAR' | 'READING' | 'LISTENING'
  sessionType: 'VOCABULARY' | 'GRAMMAR_READING' | 'LISTENING' | 'KNOWLEDGE_READING'
  instructionJp: string
  instructionVi: string
  questionType: 'VOCAB_STANDARD' | 'GRAMMAR_STANDARD' | 'STAR' | 'CLOZE_TEXT' | 'READING_PASSAGE' | 'LISTENING_TASK' | 'LISTENING_STANDARD'
  hasPassage?: boolean
  supportsVisuals?: boolean
  defaultOptionsCount?: number
}

export const JLPT_MONDAI_TYPES: JlptMondaiDefinition[] = [
  // ===== TỪ VỰNG (文字・語彙) =====
  {
    code: 'KANJI_READING',
    mondaiNumber: 1,
    nameVi: 'Mondai 1: Cách đọc Kanji (漢字読み)',
    skillType: 'VOCABULARY',
    sessionType: 'VOCABULARY',
    instructionJp: '____の ことばは どう かきますか。１・２・３・４から いちばん いい ものを ひとつ えらんで ください。',
    instructionVi: 'Từ gạch chân đọc bằng hiragana như thế nào? Chọn đáp án đúng nhất.',
    questionType: 'VOCAB_STANDARD',
  },
  {
    code: 'ORTHOGRAPHY',
    mondaiNumber: 2,
    nameVi: 'Mondai 2: Cách viết Kanji / Hiragana (表記)',
    skillType: 'VOCABULARY',
    sessionType: 'VOCABULARY',
    instructionJp: '____の ことばの 漢字(ひらがな)は どう かきますか。',
    instructionVi: 'Chữ gạch chân viết Kanji (hoặc Hiragana) như thế nào?',
    questionType: 'VOCAB_STANDARD',
  },
  {
    code: 'CONTEXTUAL_USE',
    mondaiNumber: 3,
    nameVi: 'Mondai 3: Điền từ theo ngữ cảnh (文脈規定)',
    skillType: 'VOCABULARY',
    sessionType: 'VOCABULARY',
    instructionJp: '( )に なにを 入れますか。いちばん いい ものを ひとつ えらんで ください。',
    instructionVi: 'Chọn từ thích hợp nhất để điền vào ngữ cảnh câu.',
    questionType: 'VOCAB_STANDARD',
  },
  {
    code: 'SYNONYMS',
    mondaiNumber: 4,
    nameVi: 'Mondai 4: Câu có ý nghĩa gần nhất (類義語)',
    skillType: 'VOCABULARY',
    sessionType: 'VOCABULARY',
    instructionJp: '____の ぶんと だいたい おなじ いみの ぶんが あります。',
    instructionVi: 'Tìm câu có ý nghĩa tương đương nhất với câu cho trước.',
    questionType: 'VOCAB_STANDARD',
  },
  {
    code: 'PROPER_USAGE',
    mondaiNumber: 5,
    nameVi: 'Mondai 5: Cách dùng từ chính xác (用法)',
    skillType: 'VOCABULARY',
    sessionType: 'VOCABULARY',
    instructionJp: 'つぎの ことばの つかいかたで いちばん いい ものを ひとつ えらんで ください。',
    instructionVi: 'Chọn câu sử dụng từ được cho một cách chính xác và tự nhiên nhất.',
    questionType: 'VOCAB_STANDARD',
  },

  // ===== NGỮ PHÁP (文法) =====
  {
    code: 'SENTENTIAL_GRAMMAR',
    mondaiNumber: 1,
    nameVi: 'Mondai 1: Ngữ pháp câu đơn (文法形式の判断)',
    skillType: 'GRAMMAR',
    sessionType: 'GRAMMAR_READING',
    instructionJp: 'つぎの ぶんの ( )に なにを 入れますか。１・２・３・４から いちばん いい ものを ひとつ えらんで ください。',
    instructionVi: 'Chọn dạng ngữ pháp thích hợp điền vào chỗ trống trong câu.',
    questionType: 'GRAMMAR_STANDARD',
  },
  {
    code: 'SENTENCE_COMPOSITION',
    mondaiNumber: 2,
    nameVi: 'Mondai 2: Sắp xếp câu có dấu sao ★ (文の組み立て)',
    skillType: 'GRAMMAR',
    sessionType: 'GRAMMAR_READING',
    instructionJp: 'つぎの ぶんの ★に 入る ものは どれですか。１・２・３・４から いちばん いい ものを ひとつ えらんで ください。',
    instructionVi: 'Sắp xếp 4 từ/cụm từ vào 4 vị trí gạch dưới. Chọn đáp án nằm ở vị trí dấu sao ★.',
    questionType: 'STAR',
  },
  {
    code: 'TEXT_GRAMMAR',
    mondaiNumber: 3,
    nameVi: 'Mondai 3: Ngữ pháp đoạn văn / Đục lỗ (文章の文法)',
    skillType: 'GRAMMAR',
    sessionType: 'GRAMMAR_READING',
    instructionJp: 'つぎの 文章を 読んで、文章全体の 内容を 考えて (19)から(23)の 中に 入る 最もよい ものを えらびなさい。',
    instructionVi: 'Đọc đoạn văn và chọn liên từ/ngữ pháp thích hợp nhất cho các chỗ trống số (19), (20)...',
    questionType: 'CLOZE_TEXT',
    hasPassage: true,
  },

  // ===== ĐỌC HIỂU (読解) =====
  {
    code: 'SHORT_PASSAGE',
    mondaiNumber: 4,
    nameVi: 'Mondai 4: Đọc hiểu văn ngắn (短文読解)',
    skillType: 'READING',
    sessionType: 'GRAMMAR_READING',
    instructionJp: 'つぎの 文章を 読んで、質問に こたえて ください。答えは １・２・３・４から ひとつ えらびなさい。',
    instructionVi: 'Đọc đoạn văn ngắn và trả lời câu hỏi đọc hiểu.',
    questionType: 'READING_PASSAGE',
    hasPassage: true,
  },
  {
    code: 'MEDIUM_PASSAGE',
    mondaiNumber: 5,
    nameVi: 'Mondai 5: Đọc hiểu văn trung (中文読解 - 1 bài nhiều câu)',
    skillType: 'READING',
    sessionType: 'GRAMMAR_READING',
    instructionJp: 'つぎの 文章を 読んで、後の 問いに こたえて ください。',
    instructionVi: 'Đọc bài văn vừa (400-500 chữ) và trả lời các câu hỏi liên kết.',
    questionType: 'READING_PASSAGE',
    hasPassage: true,
  },
  {
    code: 'LONG_PASSAGE',
    mondaiNumber: 6,
    nameVi: 'Mondai 6: Đọc hiểu văn dài (長文読解)',
    skillType: 'READING',
    sessionType: 'GRAMMAR_READING',
    instructionJp: 'つぎの 文章を 読んで、後の 問いに こたえなさい。',
    instructionVi: 'Đọc bài văn dài (800-1000 chữ) và trả lời chuỗi câu hỏi.',
    questionType: 'READING_PASSAGE',
    hasPassage: true,
  },
  {
    code: 'INFORMATION_RETRIEVAL',
    mondaiNumber: 7,
    nameVi: 'Mondai 7: Tìm kiếm thông tin / Tờ rơi (情報検索)',
    skillType: 'READING',
    sessionType: 'GRAMMAR_READING',
    instructionJp: '右の（下の）案内を 見て、質問に こたえて ください。',
    instructionVi: 'Quan sát tờ rơi, bảng thông báo hoặc thư điện tử để tìm thông tin chính xác.',
    questionType: 'READING_PASSAGE',
    hasPassage: true,
    supportsVisuals: true,
  },

  // ===== NGHE HIỂU (聴解) =====
  {
    code: 'TASK_BASED',
    mondaiNumber: 1,
    nameVi: 'Mondai 1: Hiểu nhiệm vụ (課題理解 - Có tranh vẽ & hành động)',
    skillType: 'LISTENING',
    sessionType: 'LISTENING',
    instructionJp: 'まず しつもんを きいて ください。それから はなしを きいて、問題用紙の 1から4の なかから、いちばん いい ものを ひとつ えらんで ください。',
    instructionVi: 'Nghe câu hỏi trước, sau đó nghe hội thoại và chọn việc nhân vật sẽ làm tiếp theo.',
    questionType: 'LISTENING_TASK',
    supportsVisuals: true,
  },
  {
    code: 'KEY_POINT',
    mondaiNumber: 2,
    nameVi: 'Mondai 2: Hiểu điểm mấu chốt (ポイント理解)',
    skillType: 'LISTENING',
    sessionType: 'LISTENING',
    instructionJp: 'まず しつもんを きいて ください。そのあと 問題用紙を みて ください。読む 時間が あります。それから はなしを きいて...',
    instructionVi: 'Nghe câu hỏi, đọc trước các đáp án trên đề, sau đó nghe và chọn điểm then chốt.',
    questionType: 'LISTENING_STANDARD',
    supportsVisuals: true,
  },
  {
    code: 'GENERAL_OUTLINE',
    mondaiNumber: 3,
    nameVi: 'Mondai 3: Hiểu khái quát (概要理解)',
    skillType: 'LISTENING',
    sessionType: 'LISTENING',
    instructionJp: '問題用紙に 何も 印刷されて いません。まず はなしを きいて ください。それから、しつもんを きいて...',
    instructionVi: 'Đề không in sẵn câu hỏi. Nghe toàn bộ chủ đề/nội dung khái quát và chọn đáp án.',
    questionType: 'LISTENING_STANDARD',
  },
  {
    code: 'UTTERANCE_EXPRESSIONS',
    mondaiNumber: 4,
    nameVi: 'Mondai 4: Diễn đạt bằng lời nói (発話表現 - Tranh mũi tên)',
    skillType: 'LISTENING',
    sessionType: 'LISTENING',
    instructionJp: 'えを 見ながら、しつもんを きいて ください。やじるし（➡）の ひとは なんと いいますか。',
    instructionVi: 'Nhìn tranh tình huống. Người có mũi tên chỉ vào sẽ nói câu gì trong tình huống đó?',
    questionType: 'LISTENING_TASK',
    supportsVisuals: true,
    defaultOptionsCount: 3,
  },
  {
    code: 'QUICK_RESPONSE',
    mondaiNumber: 5,
    nameVi: 'Mondai 5: Phản xạ nhanh (即時応答 - 3 đáp án)',
    skillType: 'LISTENING',
    sessionType: 'LISTENING',
    instructionJp: '問題用紙に 何も 印刷されて いません。短い 文を きいて、返事を ひとつ えらんで ください。',
    instructionVi: 'Nghe câu thoại ngắn và chọn câu đáp lại phù hợp nhất trong 3 lựa chọn (1, 2, 3).',
    questionType: 'LISTENING_STANDARD',
    defaultOptionsCount: 3,
  },
]

export const getMondaisBySessionType = (sessionType?: string, jlptLevel?: string): JlptMondaiDefinition[] => {
  if (!sessionType) return []

  let list: JlptMondaiDefinition[] = []
  if (sessionType === 'KNOWLEDGE_READING') {
    list = JLPT_MONDAI_TYPES.filter((m) => m.sessionType === 'VOCABULARY' || m.sessionType === 'GRAMMAR_READING')
  } else {
    list = JLPT_MONDAI_TYPES.filter((m) => m.sessionType === sessionType)
  }

  // Tùy biến danh sách Mondai theo cấp độ JLPT chuẩn:
  if (sessionType === 'LISTENING') {
    if (jlptLevel === 'N5' || jlptLevel === 'N4') {
      // N4 & N5 KHÔNG có Mondai 3: Hiểu khái quát (GENERAL_OUTLINE)
      // Cấu trúc chuẩn N4/N5 chỉ gồm 4 Mondai:
      // Mondai 1: TASK_BASED (Hiểu nhiệm vụ - Tranh vẽ & hành động)
      // Mondai 2: KEY_POINT (Hiểu điểm mấu chốt)
      // Mondai 3: UTTERANCE_EXPRESSIONS (Diễn đạt bằng lời nói - Tranh mũi tên)
      // Mondai 4: QUICK_RESPONSE (Phản xạ nhanh - 3 đáp án)
      list = list
        .filter((m) => m.code !== 'GENERAL_OUTLINE')
        .map((m) => {
          if (m.code === 'UTTERANCE_EXPRESSIONS') {
            return {
              ...m,
              mondaiNumber: 3,
              nameVi: 'Mondai 3: Diễn đạt bằng lời nói (発話表現 - Tranh mũi tên)',
            }
          }
          if (m.code === 'QUICK_RESPONSE') {
            return {
              ...m,
              mondaiNumber: 4,
              nameVi: 'Mondai 4: Phản xạ nhanh (即時応答 - 3 đáp án)',
            }
          }
          return m
        })
    } else if (jlptLevel === 'N1' || jlptLevel === 'N2') {
      // N1 & N2 KHÔNG có UTTERANCE_EXPRESSIONS (Tranh mũi tên), có INTEGRATED_LISTENING
      list = list
        .filter((m) => m.code !== 'UTTERANCE_EXPRESSIONS')
        .map((m) => {
          if (m.code === 'QUICK_RESPONSE') {
            return {
              ...m,
              mondaiNumber: 4,
              nameVi: 'Mondai 4: Phản xạ nhanh (即時応答 - 3 đáp án)',
            }
          }
          return m
        })
      list.push({
        code: 'INTEGRATED_LISTENING',
        mondaiNumber: 5,
        nameVi: 'Mondai 5: Nghe hiểu tổng hợp (統合理解)',
        skillType: 'LISTENING',
        sessionType: 'LISTENING',
        instructionJp: '問題用紙に 何も 印刷されて いません。まず はなしを きいて ください。',
        instructionVi: 'Nghe đoạn hội thoại hoặc thảo luận dài tổng hợp và trả lời các câu hỏi liên kết.',
        questionType: 'LISTENING_STANDARD',
      })
    }
  }

  if (jlptLevel === 'N5' || jlptLevel === 'N4') {
    if (sessionType === 'GRAMMAR_READING' || sessionType === 'KNOWLEDGE_READING') {
      // N4 & N5 KHÔNG có bài đọc dài (LONG_PASSAGE), Thông tin tìm kiếm là Mondai 6
      list = list
        .filter((m) => m.code !== 'LONG_PASSAGE')
        .map((m) => {
          if (m.code === 'INFORMATION_RETRIEVAL') {
            return {
              ...m,
              mondaiNumber: 6,
              nameVi: 'Mondai 6: Tìm kiếm thông tin / Tờ rơi (情報検索)',
            }
          }
          return m
        })
    }

    if (jlptLevel === 'N5' && (sessionType === 'VOCABULARY' || sessionType === 'KNOWLEDGE_READING')) {
      // N5 Từ vựng KHÔNG có Mondai 5 (PROPER_USAGE)
      list = list.filter((m) => m.code !== 'PROPER_USAGE')
    }
  }

  return list
}

export const getMondaiByCode = (code?: string): JlptMondaiDefinition | undefined => {
  if (!code) return undefined
  return JLPT_MONDAI_TYPES.find((m) => m.code.toUpperCase() === code.toUpperCase())
}
