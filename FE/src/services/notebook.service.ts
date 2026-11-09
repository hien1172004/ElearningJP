import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  PageResponse,
  Notebook,
  NotebookGroup,
  NotebookGroupWithNotebooks,
  CreateNotebookRequest,
  UpdateNotebookRequest,
  CreateNotebookGroupRequest,
  UpdateNotebookGroupRequest,
  NotebookSearchParams,
  NotebookWord,
  CreateNotebookWordRequest,
  UpdateNotebookWordRequest,
  ExcelImportResultResponse,
  NotebookStatsResponse,
  KanjiPdfMetaResponse,
  NotebookItemType,
  SaveLessonItemsRequest,
  SaveLessonItemsResponse,
  NotebookSavedIds,
} from '@/types'

export const notebookService = {
  // Sổ tay (Notebooks)
  createNotebook: async (data: CreateNotebookRequest): Promise<ApiResponse<Notebook>> => {
    return axiosClient.post('/notebooks', data)
  },

  getMyNotebooks: async (): Promise<ApiResponse<Notebook[]>> => {
    return axiosClient.get('/notebooks')
  },

  getMyUngroupedNotebooks: async (): Promise<ApiResponse<Notebook[]>> => {
    return axiosClient.get('/notebooks/ungrouped')
  },

  getMyNotebooksByGroup: async (groupId: number): Promise<ApiResponse<Notebook[]>> => {
    return axiosClient.get(`/notebooks/group/${groupId}`)
  },

  searchMyNotebooks: async (params?: NotebookSearchParams): Promise<ApiResponse<PageResponse<Notebook>>> => {
    return axiosClient.get('/notebooks/search', { params })
  },

  getNotebookDetail: async (id: number): Promise<ApiResponse<Notebook>> => {
    return axiosClient.get(`/notebooks/${id}`)
  },

  updateNotebook: async (id: number, data: UpdateNotebookRequest): Promise<ApiResponse<Notebook>> => {
    return axiosClient.put(`/notebooks/${id}`, data)
  },

  deleteNotebook: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/notebooks/${id}`)
  },

  moveToGroup: async (id: number, targetGroupId?: number | null): Promise<ApiResponse<Notebook>> => {
    return axiosClient.patch(`/notebooks/${id}/move`, null, {
      params: { targetGroupId },
    })
  },

  // Nhóm sổ tay (Notebook Groups)
  createGroup: async (data: CreateNotebookGroupRequest): Promise<ApiResponse<NotebookGroup>> => {
    return axiosClient.post('/notebook-groups', data)
  },

  getMyGroups: async (): Promise<ApiResponse<NotebookGroup[]>> => {
    return axiosClient.get('/notebook-groups')
  },

  getMyGroupsWithNotebooks: async (): Promise<ApiResponse<NotebookGroupWithNotebooks[]>> => {
    return axiosClient.get('/notebook-groups/with-notebooks')
  },

  getGroupById: async (id: number): Promise<ApiResponse<NotebookGroup>> => {
    return axiosClient.get(`/notebook-groups/${id}`)
  },

  updateGroup: async (id: number, data: UpdateNotebookGroupRequest): Promise<ApiResponse<NotebookGroup>> => {
    return axiosClient.put(`/notebook-groups/${id}`, data)
  },

  deleteGroup: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/notebook-groups/${id}`)
  },

  // Từ vựng trong sổ tay (Words)
  getWords: async (
    notebookId: number,
    params?: { keyword?: string; page?: number; size?: number; sort?: string; itemType?: NotebookItemType }
  ): Promise<ApiResponse<PageResponse<NotebookWord>>> => {
    return axiosClient.get(`/notebooks/${notebookId}/words`, { params })
  },

  getShuffledWords: async (notebookId: number): Promise<ApiResponse<NotebookWord[]>> => {
    return axiosClient.get(`/notebooks/${notebookId}/words/shuffle`)
  },

  addWord: async (notebookId: number, data: CreateNotebookWordRequest): Promise<ApiResponse<NotebookWord>> => {
    return axiosClient.post(`/notebooks/${notebookId}/words`, data)
  },

  updateWord: async (
    notebookId: number,
    wordId: number,
    data: UpdateNotebookWordRequest
  ): Promise<ApiResponse<NotebookWord>> => {
    return axiosClient.put(`/notebooks/${notebookId}/words/${wordId}`, data)
  },

  deleteWord: async (notebookId: number, wordId: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/notebooks/${notebookId}/words/${wordId}`)
  },

  batchDeleteWords: async (notebookId: number, wordIds: number[]): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/notebooks/${notebookId}/words/batch`, { data: wordIds })
  },

  // Nhập / Xuất dữ liệu
  importWordsFromExcel: async (
    notebookId: number,
    file: File
  ): Promise<ApiResponse<ExcelImportResultResponse>> => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosClient.post(`/notebooks/${notebookId}/words/import`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  // Tải file an toàn kèm Bearer Token
  downloadExcel: async (notebookId: number, notebookName: string = 'notebook'): Promise<void> => {
    const res = await axiosClient.get(`/notebooks/${notebookId}/export/excel`, {
      responseType: 'blob',
    })
    const blob = new Blob([res as unknown as BlobPart], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = `${notebookName}_tu_vung.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(downloadUrl)
  },

  downloadKanjiPdf: async (notebookId: number, notebookName: string = 'notebook'): Promise<void> => {
    const res = await axiosClient.get(`/notebooks/${notebookId}/export/kanji-pdf`, {
      responseType: 'blob',
    })
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/pdf' })
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = `${notebookName}_tap_viet_kanji.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(downloadUrl)
  },

  getKanjiPdfMeta: async (notebookId: number): Promise<ApiResponse<KanjiPdfMetaResponse>> => {
    return axiosClient.get(`/notebooks/${notebookId}/export/kanji-pdf/meta`)
  },

  fetchKanjiPdfPreviewUrl: async (notebookId: number, page: number = 1): Promise<string> => {
    const res = await axiosClient.get(`/notebooks/${notebookId}/export/kanji-pdf/preview`, {
      params: { page, _t: Date.now() },
      responseType: 'blob',
    })
    const blob = new Blob([res as unknown as BlobPart], { type: 'image/png' })
    return window.URL.createObjectURL(blob)
  },

  getStatsOverview: async (): Promise<ApiResponse<NotebookStatsResponse>> => {
    return axiosClient.get('/notebooks/stats/overview')
  },

  getReviewItems: async (
    notebookId: number,
    limit?: number,
    types?: NotebookItemType[]
  ): Promise<ApiResponse<import('@/types').NotebookReviewItem[]>> => {
    // Spring nhận Set<Enum> dạng "types=VOCAB,KANJI"
    return axiosClient.get(`/notebooks/${notebookId}/review`, {
      params: { limit, types: types && types.length > 0 ? types.join(',') : undefined },
    })
  },

  // Lưu kiến thức bài học vào sổ tay & trạng thái "đã lưu"
  saveLessonItems: async (data: SaveLessonItemsRequest): Promise<ApiResponse<SaveLessonItemsResponse>> => {
    return axiosClient.post('/notebooks/lesson-items', data)
  },

  getSavedIds: async (): Promise<ApiResponse<NotebookSavedIds>> => {
    return axiosClient.get('/notebooks/saved-ids')
  },

  submitReview: async (
    notebookId: number,
    data: import('@/types').NotebookReviewSubmitRequest
  ): Promise<ApiResponse<import('@/types').NotebookReviewSubmitResponse>> => {
    return axiosClient.post(`/notebooks/${notebookId}/review/submit`, data)
  },

  // Chia sẻ & Nhân bản (Share & Clone)
  toggleSharing: async (
    notebookId: number,
    enabled: boolean
  ): Promise<ApiResponse<Notebook>> => {
    return axiosClient.patch(`/notebooks/${notebookId}/share/toggle`, null, {
      params: { enabled },
    })
  },

  togglePublic: async (
    notebookId: number,
    isPublic: boolean
  ): Promise<ApiResponse<Notebook>> => {
    return axiosClient.patch(`/notebooks/${notebookId}/public/toggle`, null, {
      params: { isPublic },
    })
  },

  getNotebookByShareCode: async (shareCode: string): Promise<ApiResponse<Notebook>> => {
    return axiosClient.get(`/notebooks/share/${shareCode}`)
  },

  cloneNotebookByShareCode: async (
    shareCode: string,
    targetGroupId?: number | null
  ): Promise<ApiResponse<Notebook>> => {
    return axiosClient.post('/notebooks/clone', null, {
      params: { shareCode, targetGroupId },
    })
  },

  // Khám phá Cộng đồng (Community Exploration)
  exploreNotebooks: async (params?: {
    keyword?: string
    sort?: string
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<Notebook>>> => {
    return axiosClient.get('/notebooks/explore', { params })
  },

  viewPublicNotebookDetail: async (id: number): Promise<ApiResponse<Notebook>> => {
    return axiosClient.get(`/notebooks/explore/${id}`)
  },

  toggleLikeNotebook: async (id: number): Promise<ApiResponse<boolean>> => {
    return axiosClient.post(`/notebooks/explore/${id}/like`)
  },

  getFavoriteNotebooks: async (
    page: number = 0,
    size: number = 12
  ): Promise<ApiResponse<PageResponse<Notebook>>> => {
    return axiosClient.get('/notebooks/explore/favorites', { params: { page, size } })
  },

  getRecentlyViewedNotebooks: async (
    page: number = 0,
    size: number = 12
  ): Promise<ApiResponse<PageResponse<Notebook>>> => {
    return axiosClient.get('/notebooks/explore/recent', { params: { page, size } })
  },
}
