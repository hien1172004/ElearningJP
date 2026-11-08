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
//   GET  /lesson-sections/{sectionId}/{kind}-items/export           (xuất dữ liệu section, cùng định dạng mẫu)
// ============================================================================

export interface SectionImportApi {
  /** Tải file mẫu Excel về máy (kèm Bearer token). */
  downloadTemplate: () => Promise<void>
  preview: (sectionId: number, file: File) => Promise<ApiResponse<SectionImportPreviewResponse>>
  commit: (sectionId: number, file: File) => Promise<ApiResponse<SectionImportCommitResponse>>
  /** Xuất dữ liệu section ra file Excel (cùng định dạng mẫu import) và tải về máy. */
  exportSection: (sectionId: number, fileName: string) => Promise<void>
}

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

const toFormData = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return formData
}

/** Lưu dữ liệu blob (axios interceptor đã trả thẳng `response.data`) thành file .xlsx. */
const saveBlob = (data: unknown, fileName: string) => {
  const blob = new Blob([data as BlobPart], { type: XLSX_MIME })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

const createImportApi = (kind: 'kanji' | 'vocabulary' | 'grammar', templateFileName: string): SectionImportApi => ({
  downloadTemplate: async () => {
    const res = await axiosClient.get(`/lesson-sections/${kind}-items/import/template`, {
      responseType: 'blob',
    })
    saveBlob(res, templateFileName)
  },
  exportSection: async (sectionId, fileName) => {
    const res = await axiosClient.get(`/lesson-sections/${sectionId}/${kind}-items/export`, {
      responseType: 'blob',
    })
    saveBlob(res, fileName)
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
