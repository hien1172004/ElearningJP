export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  code?: number
}

export interface PageResponse<T> {
  page: number
  size: number
  totalElements: number
  totalPages: number
  items?: T[]
  content?: T[]
}
