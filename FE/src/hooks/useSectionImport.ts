import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import type { SectionImportApi } from '@/services/sectionImport.service'
import type { ApiResponse } from '@/types'
import { invalidateLesson } from './useSectionMutations'

const getErrorMessage = (err: unknown, fallback: string) =>
  (err as AxiosError<ApiResponse<unknown>>)?.response?.data?.message ?? fallback

/**
 * Mutation cho luồng import Excel của 1 section: tải mẫu → xem trước → ghi.
 * `api` là service riêng của loại section (kanjiImportService / vocabularyImportService / grammarImportService).
 */
export function useSectionImport(api: SectionImportApi, lessonId: number, sectionId: number) {
  const qc = useQueryClient()

  const downloadTemplate = useMutation({
    mutationFn: () => api.downloadTemplate(),
    onError: (e) => toast.error('Tải file mẫu thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') }),
  })

  const preview = useMutation({
    mutationFn: (file: File) => api.preview(sectionId, file).then((res) => res.data),
    onError: (e) => toast.error('Không đọc được file Excel', { description: getErrorMessage(e, 'Lỗi không xác định') }),
  })

  const commit = useMutation({
    mutationFn: (file: File) => api.commit(sectionId, file).then((res) => res.data),
    onSuccess: () => invalidateLesson(qc, lessonId),
    onError: (e) => toast.error('Import thất bại', { description: getErrorMessage(e, 'Lỗi không xác định') }),
  })

  return { downloadTemplate, preview, commit }
}
