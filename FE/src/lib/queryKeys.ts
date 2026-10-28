/**
 * Tập trung các cache key cho React Query.
 * Mục đích: tránh typo khi gọi useQuery({ queryKey: ['me'] }) ở nhiều nơi.
 */
export const QUERY_KEYS = {
  me: ['me'] as const,
  course: {
    list: (params: Record<string, unknown>) => ['courses', 'list', params] as const,
    detail: (id: number | string) => ['courses', 'detail', id] as const,
  },
  lesson: {
    detail: (id: number | string) => ['lessons', 'detail', id] as const,
    quiz: (id: number | string) => ['lessons', 'quiz', id] as const,
    quizManage: (id: number | string) => ['lessons', 'quiz', 'manage', id] as const,
    kanjiItem: (id: number | string) => ['lessons', 'kanji-item', id] as const,
    vocabItem: (id: number | string) => ['lessons', 'vocab-item', id] as const,
    grammarItem: (id: number | string) => ['lessons', 'grammar-item', id] as const,
  },
  srs: {
    due: (params: Record<string, unknown>) => ['srs', 'due', params] as const,
    stats: ['srs', 'stats'] as const,
  },
  progress: {
    summary: (params: Record<string, unknown>) =>
      ['progress', 'summary', params] as const,
    calendar: (month?: string) =>
      ['progress', 'calendar', month ?? 'current'] as const,
    chart: (params: Record<string, unknown>) =>
      ['progress', 'chart', params] as const,
    topLookups: (params: Record<string, unknown>) =>
      ['progress', 'top-lookups', params] as const,
    leaderboard: (week?: string) =>
      ['progress', 'leaderboard', week ?? 'current'] as const,
    myRank: (week?: string) =>
      ['progress', 'my-rank', week ?? 'current'] as const,
  },
  dictionary: {
    unified: (params: Record<string, unknown>) =>
      ['dictionary', 'unified', params] as const,
    vocab: (id: number) => ['dictionary', 'vocab', id] as const,
    kanji: (character: string) => ['dictionary', 'kanji', character] as const,
    grammar: (id: number) => ['dictionary', 'grammar', id] as const,
    kana: (type: string) => ['dictionary', 'kana', type] as const,
  },
  comment: {
    list: (targetType: string, targetId: number | string, page: number) =>
      ['comments', 'list', targetType, targetId, page] as const,
    replies: (commentId: number | string, page: number) =>
      ['comments', 'replies', commentId, page] as const,
  },
  notification: {
    /** Key gốc — invalidate `all` sẽ quét cả list/unread/recent. */
    all: ['notifications'] as const,
    list: (page: number) => ['notifications', 'list', page] as const,
    unreadCount: ['notifications', 'unread-count'] as const,
    recent: ['notifications', 'recent'] as const,
  },
}
