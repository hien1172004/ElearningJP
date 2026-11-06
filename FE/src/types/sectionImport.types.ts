import type { LessonSectionType } from './course.types'

/** Trạng thái 1 dòng Excel sau khi BE kiểm tra. */
export type ImportRowStatus = 'OK' | 'WARNING' | 'ERROR'

/**
 * Kết quả kiểm tra 1 dòng trong file import.
 * `data` gồm giá trị các cột (theo tên cột trong mẫu) + giá trị tra từ điển
 * (vd `dictMeaning`, `vocabId`, `linkedRow`, `exampleCount`…).
 */
export interface ImportRowResult {
  sheet: string
  rowNumber: number
  status: ImportRowStatus
  data: Record<string, unknown>
  messages: string[]
}

/** Kết quả xem trước: itemRows = sheet chính, childRows = sheet phụ (TuLienQuan / ViDu). */
export interface SectionImportPreviewResponse {
  sectionType: LessonSectionType
  totalItems: number
  totalChildren: number
  warningRows: number
  errorRows: number
  canCommit: boolean
  itemRows: ImportRowResult[]
  childRows: ImportRowResult[]
}

export interface SectionImportCommitResponse {
  importedItems: number
  importedChildren: number
}
