/**
 * Tập trung các cache key cho React Query.
 * Mục đích: tránh typo khi gọi useQuery({ queryKey: ['me'] }) ở nhiều nơi.
 */
export const QUERY_KEYS = {
  me: ['me'] as const,
  course: {
    list: (params: Record<string, unknown>) => ['courses', 'list', params] as const,
    detailAll: ['courses', 'detail'] as const,
    detail: (id: number | string) => ['courses', 'detail', id] as const,
  },
  enrollment: {
    all: ['enrollments'] as const,
    mine: (activeOnly: boolean) =>
      ['enrollments', 'me', activeOnly ? 'active' : 'all'] as const,
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
    items: (params: Record<string, unknown>) => ['srs', 'items', params] as const,
    forecast: ['srs', 'forecast'] as const,
    savedIds: ['srs', 'saved-ids'] as const,
  },
  progress: {
    all: ['progress'] as const,
    goal: ['progress', 'goal'] as const,
    summaryAll: ['progress', 'summary'] as const,
    calendarAll: ['progress', 'calendar'] as const,
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
    /** Key gốc — mọi trang bình luận của 1 target (dùng để invalidate / setQueriesData). */
    listByTarget: (targetType: string, targetId: number | string) =>
      ['comments', 'list', targetType, targetId] as const,
    list: (targetType: string, targetId: number | string, page: number) =>
      ['comments', 'list', targetType, targetId, page] as const,
    /** Key khi query bình luận bị tắt (thiếu targetId). */
    listDisabled: ['comments', 'list', 'disabled'] as const,
    /** Key gốc — mọi trang reply của 1 bình luận. */
    repliesByComment: (commentId: number | string) =>
      ['comments', 'replies', commentId] as const,
    replies: (commentId: number | string, page: number) =>
      ['comments', 'replies', commentId, page] as const,
    /** Key khi query reply bị tắt (chưa mở khối trả lời). */
    repliesDisabled: ['comments', 'replies', 'disabled'] as const,
    /** Vị trí bình luận (mở thẳng từ thông báo). */
    location: (commentId: number, replyId: number | null) =>
      ['comments', 'location', commentId, replyId] as const,
  },
  notification: {
    /** Key gốc — invalidate `all` sẽ quét cả list/unread/recent. */
    all: ['notifications'] as const,
    list: (page: number) => ['notifications', 'list', page] as const,
    /** Danh sách cuộn vô hạn trong dropdown chuông. */
    feedAll: ['notifications', 'feed'] as const,
    feed: (size: number) => ['notifications', 'feed', size] as const,
    unreadCount: ['notifications', 'unread-count'] as const,
    recent: ['notifications', 'recent'] as const,
  },
}
