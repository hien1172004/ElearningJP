import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { notebookService } from '@/services/notebook.service'
import type {
  Notebook,
  NotebookGroupWithNotebooks,
  CreateNotebookRequest,
  UpdateNotebookRequest,
  CreateNotebookGroupRequest,
  UpdateNotebookGroupRequest,
  NotebookSearchParams,
  NotebookWord,
  CreateNotebookWordRequest,
  UpdateNotebookWordRequest,
  PageResponse,
  NotebookStatsResponse,
} from '@/types'

export const NOTEBOOK_QUERY_KEYS = {
  all: ['notebooks'] as const,
  myList: () => [...NOTEBOOK_QUERY_KEYS.all, 'my-list'] as const,
  ungrouped: () => [...NOTEBOOK_QUERY_KEYS.all, 'ungrouped'] as const,
  byGroup: (groupId: number) => [...NOTEBOOK_QUERY_KEYS.all, 'by-group', groupId] as const,
  search: (params?: NotebookSearchParams) => [...NOTEBOOK_QUERY_KEYS.all, 'search', params] as const,
  detail: (id: number) => [...NOTEBOOK_QUERY_KEYS.all, 'detail', id] as const,
  groups: ['notebook-groups'] as const,
  groupList: () => [...NOTEBOOK_QUERY_KEYS.groups, 'list'] as const,
  groupsWithNotebooks: () => [...NOTEBOOK_QUERY_KEYS.groups, 'with-notebooks'] as const,
  words: (notebookId: number, params?: { keyword?: string; page?: number; size?: number; sort?: string }) =>
    params !== undefined
      ? ([...NOTEBOOK_QUERY_KEYS.all, 'words', notebookId, params] as const)
      : ([...NOTEBOOK_QUERY_KEYS.all, 'words', notebookId] as const),
  shuffledWords: (notebookId: number) => [...NOTEBOOK_QUERY_KEYS.all, 'shuffled-words', notebookId] as const,
  statsOverview: () => [...NOTEBOOK_QUERY_KEYS.all, 'stats-overview'] as const,
}

// Hook lấy danh sách nhóm phẳng
export function useMyGroups() {
  return useQuery({
    queryKey: NOTEBOOK_QUERY_KEYS.groupList(),
    queryFn: async () => {
      const res = await notebookService.getMyGroups()
      return res.data ?? []
    },
  })
}

// Hook lấy danh sách toàn bộ sổ tay của tôi
export function useMyNotebooks() {
  return useQuery<Notebook[]>({
    queryKey: NOTEBOOK_QUERY_KEYS.myList(),
    queryFn: async () => {
      const res = await notebookService.getMyNotebooks()
      return res.data ?? []
    },
  })
}

// Hook lấy danh sách sổ tay chưa phân nhóm
export function useMyUngroupedNotebooks() {
  return useQuery<Notebook[]>({
    queryKey: NOTEBOOK_QUERY_KEYS.ungrouped(),
    queryFn: async () => {
      const res = await notebookService.getMyUngroupedNotebooks()
      return res.data ?? []
    },
  })
}

// Hook lấy danh sách nhóm kèm sổ tay
export function useMyGroupsWithNotebooks() {
  return useQuery<NotebookGroupWithNotebooks[]>({
    queryKey: NOTEBOOK_QUERY_KEYS.groupsWithNotebooks(),
    queryFn: async () => {
      const res = await notebookService.getMyGroupsWithNotebooks()
      return res.data ?? []
    },
  })
}

// Hook tìm kiếm sổ tay có phân trang
export function useSearchMyNotebooks(params?: NotebookSearchParams) {
  return useQuery({
    queryKey: NOTEBOOK_QUERY_KEYS.search(params),
    queryFn: async () => {
      const res = await notebookService.searchMyNotebooks(params)
      return res.data
    },
  })
}

// Hook chi tiết sổ tay
export function useNotebookDetail(id?: number) {
  return useQuery<Notebook | null>({
    queryKey: NOTEBOOK_QUERY_KEYS.detail(id ?? 0),
    queryFn: async () => {
      if (!id) return null
      const res = await notebookService.getNotebookDetail(id)
      return res.data ?? null
    },
    enabled: !!id,
  })
}

// Hook lấy danh sách từ vựng trong sổ tay có phân trang & tìm kiếm
export function useNotebookWords(
  notebookId?: number,
  params?: { keyword?: string; page?: number; size?: number; sort?: string }
) {
  return useQuery<PageResponse<NotebookWord>>({
    queryKey: NOTEBOOK_QUERY_KEYS.words(notebookId ?? 0, params),
    queryFn: async () => {
      if (!notebookId) {
        return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
      }
      const res = await notebookService.getWords(notebookId, params)
      return res.data ?? { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    },
    enabled: !!notebookId,
  })
}

// Hook lấy danh sách từ xáo trộn cho chế độ lướt thẻ preview
export function useNotebookShuffledWords(notebookId?: number) {
  return useQuery<NotebookWord[]>({
    queryKey: NOTEBOOK_QUERY_KEYS.shuffledWords(notebookId ?? 0),
    queryFn: async () => {
      if (!notebookId) return []
      const res = await notebookService.getShuffledWords(notebookId)
      return res.data ?? []
    },
    enabled: !!notebookId,
  })
}

// Hook thống kê tổng quan SRS
export function useNotebookStatsOverview() {
  return useQuery<NotebookStatsResponse | null>({
    queryKey: NOTEBOOK_QUERY_KEYS.statsOverview(),
    queryFn: async () => {
      const res = await notebookService.getStatsOverview()
      return res.data ?? null
    },
  })
}

// Hook mutations thao tác sổ tay và nhóm
export function useNotebookMutations() {
  const queryClient = useQueryClient()

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.all })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.groups })
  }

  const createNotebook = useMutation({
    mutationFn: (data: CreateNotebookRequest) => notebookService.createNotebook(data),
    onSuccess: invalidateAll,
  })

  const updateNotebook = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateNotebookRequest }) =>
      notebookService.updateNotebook(id, data),
    onSuccess: invalidateAll,
  })

  const deleteNotebook = useMutation({
    mutationFn: (id: number) => notebookService.deleteNotebook(id),
    onSuccess: invalidateAll,
  })

  const moveToGroup = useMutation({
    mutationFn: ({ id, targetGroupId }: { id: number; targetGroupId?: number | null }) =>
      notebookService.moveToGroup(id, targetGroupId),
    onSuccess: invalidateAll,
  })

  const createGroup = useMutation({
    mutationFn: (data: CreateNotebookGroupRequest) => notebookService.createGroup(data),
    onSuccess: invalidateAll,
  })

  const updateGroup = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateNotebookGroupRequest }) =>
      notebookService.updateGroup(id, data),
    onSuccess: invalidateAll,
  })

  const deleteGroup = useMutation({
    mutationFn: (id: number) => notebookService.deleteGroup(id),
    onSuccess: invalidateAll,
  })

  return {
    createNotebook,
    updateNotebook,
    deleteNotebook,
    moveToGroup,
    createGroup,
    updateGroup,
    deleteGroup,
  }
}

// Hook mutations thao tác từ vựng trong sổ tay
export function useNotebookWordMutations(notebookId: number) {
  const queryClient = useQueryClient()

  const invalidateWords = () => {
    queryClient.invalidateQueries({ queryKey: [...NOTEBOOK_QUERY_KEYS.all, 'words', notebookId] })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.shuffledWords(notebookId) })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.detail(notebookId) })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.statsOverview() })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.myList() })
  }

  const addWord = useMutation({
    mutationFn: (data: CreateNotebookWordRequest) => notebookService.addWord(notebookId, data),
    onSuccess: invalidateWords,
  })

  const updateWord = useMutation({
    mutationFn: ({ wordId, data }: { wordId: number; data: UpdateNotebookWordRequest }) =>
      notebookService.updateWord(notebookId, wordId, data),
    onSuccess: invalidateWords,
  })

  const deleteWord = useMutation({
    mutationFn: (wordId: number) => notebookService.deleteWord(notebookId, wordId),
    onSuccess: invalidateWords,
  })

  const batchDeleteWords = useMutation({
    mutationFn: (wordIds: number[]) => notebookService.batchDeleteWords(notebookId, wordIds),
    onSuccess: invalidateWords,
  })

  const importWordsFromExcel = useMutation({
    mutationFn: (file: File) => notebookService.importWordsFromExcel(notebookId, file),
    onSuccess: invalidateWords,
  })

  return {
    addWord,
    updateWord,
    deleteWord,
    batchDeleteWords,
    importWordsFromExcel,
  }
}

// Hook lấy danh sách từ cần ôn tập SRS
export function useNotebookReviewItems(notebookId?: number, limit?: number) {
  return useQuery<import('@/types').NotebookReviewItem[]>({
    queryKey: ['notebook-review-items', notebookId ?? 0, limit],
    queryFn: async () => {
      if (notebookId === undefined || notebookId === null || notebookId < 0) return []
      const res = await notebookService.getReviewItems(notebookId, limit)
      return res.data ?? []
    },
    enabled: notebookId !== undefined && notebookId !== null && notebookId >= 0,
  })
}

// Hook gửi đánh giá SRS
export function useSubmitReviewMutation(notebookId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: import('@/types').NotebookReviewSubmitRequest) =>
      notebookService.submitReview(notebookId, data),
    onSuccess: () => {
      if (notebookId > 0) {
        queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.words(notebookId) })
      } else {
        queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.all })
      }
      queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.statsOverview() })
      queryClient.invalidateQueries({ queryKey: ['notebook-review-items'] })
    },
  })
}

// Hooks Khám phá Sổ tay Cộng đồng
export function useExploreNotebooks(params?: {
  keyword?: string
  sort?: string
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: ['notebooks-explore', params],
    queryFn: async () => {
      const res = await notebookService.exploreNotebooks(params)
      return res.data
    },
  })
}

export function useFavoriteNotebooks(page: number = 0, size: number = 12) {
  return useQuery({
    queryKey: ['notebooks-favorites', page, size],
    queryFn: async () => {
      const res = await notebookService.getFavoriteNotebooks(page, size)
      return res.data
    },
  })
}

export function useRecentlyViewedNotebooks(page: number = 0, size: number = 12) {
  return useQuery({
    queryKey: ['notebooks-recent', page, size],
    queryFn: async () => {
      const res = await notebookService.getRecentlyViewedNotebooks(page, size)
      return res.data
    },
  })
}

export function useCommunityNotebookMutations() {
  const queryClient = useQueryClient()

  const invalidateCommunity = () => {
    queryClient.invalidateQueries({ queryKey: ['notebooks-explore'] })
    queryClient.invalidateQueries({ queryKey: ['notebooks-favorites'] })
    queryClient.invalidateQueries({ queryKey: ['notebooks-recent'] })
    queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.myList() })
  }

  const toggleLike = useMutation({
    mutationFn: (id: number) => notebookService.toggleLikeNotebook(id),
    onSuccess: invalidateCommunity,
  })

  const cloneNotebook = useMutation({
    mutationFn: ({ shareCode, targetGroupId }: { shareCode: string; targetGroupId?: number | null }) =>
      notebookService.cloneNotebookByShareCode(shareCode, targetGroupId),
    onSuccess: () => {
      invalidateCommunity()
      queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.all })
    },
  })

  const toggleSharing = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      notebookService.toggleSharing(id, enabled),
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.detail(vars.id) })
      queryClient.invalidateQueries({ queryKey: NOTEBOOK_QUERY_KEYS.myList() })
    },
  })

  return {
    toggleLike,
    cloneNotebook,
    toggleSharing,
  }
}
