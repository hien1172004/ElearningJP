import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  SectionImportCommitResponse,
  SectionImportPreviewResponse,
} from '@/types'

// ============================================================================
// Import item section từ Excel — mỗi loại section 1 mẫu & 1 nhóm endpoint riêng:
//   GET  /lesson-sections/{kind}-items/import/template
//   POST /lesson-sections/{sectionId}/{kind}-items/import/preview   (multipart, không ghi DB)
//   POST /lesson-sections/{sectionId}/{kind}-items/import           (multipart, ghi DB)
// ============================================================================

export interface SectionImportApi {
  /** Tải file mẫu Excel về máy (kèm Bearer token). */
  downloadTemplate: () => Promise<void>
  preview: (sectionId: number, file: File) => Promise<ApiResponse<SectionImportPreviewResponse>>
  commit: (sectionId: number, file: File) => Promise<ApiResponse<SectionImportCommitResponse>>
}

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

const toFormData = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return formData
}

const createImportApi = (kind: 'kanji' | 'vocabulary' | 'grammar', templateFileName: string): SectionImportApi => ({
  downloadTemplate: async () => {
    const res = await axiosClient.get(`/lesson-sections/${kind}-items/import/template`, {
      responseType: 'blob',
    })
    const blob = new Blob([res as unknown as BlobPart], { type: XLSX_MIME })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = templateFileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  },
  preview: (sectionId, file) =>
    axiosClient.post(`/lesson-sections/${sectionId}/${kind}-items/import/preview`, toFormData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  commit: (sectionId, file) =>
    axiosClient.post(`/lesson-sections/${sectionId}/${kind}-items/import`, toFormData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
})

export const kanjiImportService = createImportApi('kanji', 'mau_import_kanji.xlsx')
export const vocabularyImportService = createImportApi('vocabulary', 'mau_import_tu_vung.xlsx')
export const grammarImportService = createImportApi('grammar', 'mau_import_ngu_phap.xlsx')
