import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import {
  sectionService,
  kanjiItemService,
  vocabItemService,
  grammarItemService,
} from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  ApiResponse,
  LessonSection,
  LessonSectionCreateRequest,
  LessonSectionUpdateRequest,
  LessonSectionReorderRequest,
  LessonKanjiItem,
  LessonKanjiItemCreateRequest,
  LessonKanjiItemUpdateRequest,
  LessonVocabularyItem,
  LessonVocabularyItemCreateRequest,
  LessonVocabularyItemUpdateRequest,
  LessonGrammarItem,
  LessonGrammarItemCreateRequest,
  LessonGrammarItemUpdateRequest,
} from '@/types'

// ============================================================================
// Helpers
// ============================================================================
function getErrorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<ApiResponse<unknown>>)?.response?.data?.message ?? fallback
}

/**
 * Invalidate toàn bộ cache liên quan đến 1 lesson.
 * - Lesson detail (chứa sections + items nested)
 * - Tất cả item-detail cache (kanji/vocab/grammar) theo prefix để catch cả những item
 *   vừa được tạo/xoá.
 *
 * Trong react-query v5, `refetchType` đã bị loại bỏ khỏi InvalidateOptions.
 * Để refetch ngay cả các query đang inactive, ta gọi thêm `refetchQueries`
 * sau khi invalidate.
 */
function invalidateLesson(qc: ReturnType<typeof useQueryClient>, lessonId: number) {
  // 1. Mark stale tất cả cache liên quan
  qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
  qc.invalidateQueries({ queryKey: ['lessons', 'kanji-item'] })
  qc.invalidateQueries({ queryKey: ['lessons', 'vocab-item'] })
  qc.invalidateQueries({ queryKey: ['lessons', 'grammar-item'] })

  // 2. Refetch ngay lesson detail (UI đang mở) — refetchQueries với type 'active'
  //    → chỉ fetch các component đang subscribe, không spam các cache inactive
  qc.refetchQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
}

/**
 * Update trực tiếp cache của 1 item-detail (kanji/vocab/grammar)
 * → UI đang mở modal xem chi tiết sẽ thấy data mới ngay không cần refetch.
 */
function setItemCache(
  qc: ReturnType<typeof useQueryClient>,
  kind: 'kanji-item' | 'vocab-item' | 'grammar-item',
  item: LessonKanjiItem | LessonVocabularyItem | LessonGrammarItem | null,
) {
  if (!item) return
  if (kind === 'kanji-item') {
    qc.setQueryData(QUERY_KEYS.lesson.kanjiItem(item.id), item)
  } else if (kind === 'vocab-item') {
    qc.setQueryData(QUERY_KEYS.lesson.vocabItem(item.id), item)
  } else if (kind === 'grammar-item') {
    qc.setQueryData(QUERY_KEYS.lesson.grammarItem(item.id), item)
  }
}

function removeItemCache(
  qc: ReturnType<typeof useQueryClient>,
  kind: 'kanji-item' | 'vocab-item' | 'grammar-item',
  id: number,
) {
  if (kind === 'kanji-item') {
    qc.removeQueries({ queryKey: QUERY_KEYS.lesson.kanjiItem(id) })
  } else if (kind === 'vocab-item') {
    qc.removeQueries({ queryKey: QUERY_KEYS.lesson.vocabItem(id) })
  } else if (kind === 'grammar-item') {
    qc.removeQueries({ queryKey: QUERY_KEYS.lesson.grammarItem(id) })
  }
}

// ============================================================================
// SECTION CRUD (lesson-level)
// ============================================================================
export function useSectionMutations(lessonId: number) {
  const qc = useQueryClient()
  const invalidate = () => invalidateLesson(qc, lessonId)

  const createSection = useMutation({
    mutationFn: async (payload: LessonSectionCreateRequest) =>
      sectionService.createSection(lessonId, payload),
    onSuccess: (res) => {
      toast.success('Đã thêm section mới')
      // Update cache trực tiếp: BE trả về section mới trong res.data
      const newSection: LessonSection | undefined = (res as any)?.data
      if (newSection?.id) {
        qc.setQueryData<LessonSection[] | undefined>(
          ['sections', 'lesson', lessonId],
          (old) => (old ? [...old, newSection] : [newSection]),
        )
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Thêm section thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const updateSection = useMutation({
    mutationFn: async (params: { id: number; payload: LessonSectionUpdateRequest }) =>
      sectionService.updateSection(params.id, params.payload),
    onSuccess: (res, vars) => {
      toast.success('Đã cập nhật section')
      const updated: LessonSection | undefined = (res as any)?.data
      if (updated?.id) {
        // Patch trực tiếp vào cache sections (nếu có)
        qc.setQueryData<LessonSection[] | undefined>(
          ['sections', 'lesson', lessonId],
          (old) => old?.map((s) => (s.id === updated.id ? { ...s, ...updated } : s)),
        )
      }
      void vars
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Cập nhật thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const deleteSection = useMutation({
    mutationFn: async (id: number) => sectionService.deleteSection(id),
    onSuccess: (_res, id) => {
      toast.success('Đã xoá section')
      // Xoá khỏi cache sections
      qc.setQueryData<LessonSection[] | undefined>(
        ['sections', 'lesson', lessonId],
        (old) => old?.filter((s) => s.id !== id),
      )
      // Xoá tất cả item cache thuộc section này (vì chúng bị cascade delete)
      qc.removeQueries({ queryKey: ['lessons', 'kanji-item'], type: 'active' as any })
      qc.removeQueries({ queryKey: ['lessons', 'vocab-item'], type: 'active' as any })
      qc.removeQueries({ queryKey: ['lessons', 'grammar-item'], type: 'active' as any })
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Xoá thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const reorderSections = useMutation({
    mutationFn: async (payload: LessonSectionReorderRequest) =>
      sectionService.reorderSections(lessonId, payload),
    onSuccess: () => {
      toast.success('Đã cập nhật thứ tự section')
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Sắp xếp thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  return {
    createSection,
    updateSection,
    deleteSection,
    reorderSections,
  }
}

// ============================================================================
// KANJI ITEM mutations (section-level)
// ============================================================================
export function useKanjiItemMutations(lessonId: number, sectionId: number) {
  const qc = useQueryClient()
  const invalidate = () => invalidateLesson(qc, lessonId)

  const createKanjiItem = useMutation({
    mutationFn: async (payload: LessonKanjiItemCreateRequest) =>
      kanjiItemService.createItem(sectionId, payload),
    onSuccess: (res) => {
      toast.success('Đã thêm Kanji')
      // BE trả về kanji item đầy đủ (kèm relatedVocabulary) trong res.data
      const newItem: LessonKanjiItem | undefined = (res as any)?.data
      if (newItem?.id) {
        setItemCache(qc, 'kanji-item', newItem)
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Thêm Kanji thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const updateKanjiItem = useMutation({
    mutationFn: async (params: { id: number; payload: LessonKanjiItemUpdateRequest }) =>
      kanjiItemService.updateItem(params.id, params.payload),
    onSuccess: (res, vars) => {
      toast.success('Đã cập nhật Kanji')
      // Update cache detail của item này ngay (BE trả về item mới kèm relatedVocabulary)
      const updated: LessonKanjiItem | undefined = (res as any)?.data
      if (updated?.id) {
        setItemCache(qc, 'kanji-item', updated)
      } else if (vars?.id) {
        // Nếu BE không trả data, refetch cache detail
        qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.kanjiItem(vars.id) })
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Cập nhật Kanji thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const deleteKanjiItem = useMutation({
    mutationFn: async (id: number) => kanjiItemService.deleteItem(id),
    onSuccess: (_res, id) => {
      toast.success('Đã xoá Kanji')
      // Xoá cache detail của item này
      removeItemCache(qc, 'kanji-item', id)
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Xoá Kanji thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  return { createKanjiItem, updateKanjiItem, deleteKanjiItem }
}

// ============================================================================
// VOCAB ITEM mutations (section-level)
// ============================================================================
export function useVocabItemMutations(lessonId: number, sectionId: number) {
  const qc = useQueryClient()
  const invalidate = () => invalidateLesson(qc, lessonId)

  const createVocabItem = useMutation({
    mutationFn: async (payload: LessonVocabularyItemCreateRequest) =>
      vocabItemService.createItem(sectionId, payload),
    onSuccess: (res) => {
      toast.success('Đã thêm từ vựng')
      const newItem: LessonVocabularyItem | undefined = (res as any)?.data
      if (newItem?.id) {
        setItemCache(qc, 'vocab-item', newItem)
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Thêm từ vựng thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const updateVocabItem = useMutation({
    mutationFn: async (params: { id: number; payload: LessonVocabularyItemUpdateRequest }) =>
      vocabItemService.updateItem(params.id, params.payload),
    onSuccess: (res, vars) => {
      toast.success('Đã cập nhật từ vựng')
      const updated: LessonVocabularyItem | undefined = (res as any)?.data
      if (updated?.id) {
        setItemCache(qc, 'vocab-item', updated)
      } else if (vars?.id) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.vocabItem(vars.id) })
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Cập nhật từ vựng thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const deleteVocabItem = useMutation({
    mutationFn: async (id: number) => vocabItemService.deleteItem(id),
    onSuccess: (_res, id) => {
      toast.success('Đã xoá từ vựng')
      removeItemCache(qc, 'vocab-item', id)
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Xoá từ vựng thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  return { createVocabItem, updateVocabItem, deleteVocabItem }
}

// ============================================================================
// GRAMMAR ITEM mutations (section-level)
// ============================================================================
export function useGrammarItemMutations(lessonId: number, sectionId: number) {
  const qc = useQueryClient()
  const invalidate = () => invalidateLesson(qc, lessonId)

  const createGrammarItem = useMutation({
    mutationFn: async (payload: LessonGrammarItemCreateRequest) =>
      grammarItemService.createItem(sectionId, payload),
    onSuccess: (res) => {
      toast.success('Đã thêm ngữ pháp')
      const newItem: LessonGrammarItem | undefined = (res as any)?.data
      if (newItem?.id) {
        setItemCache(qc, 'grammar-item', newItem)
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Thêm ngữ pháp thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const updateGrammarItem = useMutation({
    mutationFn: async (params: { id: number; payload: LessonGrammarItemUpdateRequest }) =>
      grammarItemService.updateItem(params.id, params.payload),
    onSuccess: (res, vars) => {
      toast.success('Đã cập nhật ngữ pháp')
      const updated: LessonGrammarItem | undefined = (res as any)?.data
      if (updated?.id) {
        setItemCache(qc, 'grammar-item', updated)
      } else if (vars?.id) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.grammarItem(vars.id) })
      }
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Cập nhật ngữ pháp thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  const deleteGrammarItem = useMutation({
    mutationFn: async (id: number) => grammarItemService.deleteItem(id),
    onSuccess: (_res, id) => {
      toast.success('Đã xoá ngữ pháp')
      removeItemCache(qc, 'grammar-item', id)
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Xoá ngữ pháp thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') })
    },
  })

  return { createGrammarItem, updateGrammarItem, deleteGrammarItem }
}