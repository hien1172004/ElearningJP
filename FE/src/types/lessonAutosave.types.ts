/** Một bản chưa lưu của người dùng hiện tại trong bài — BE: LessonAutosaveResponse. */
export interface LessonAutosave {
  /** Do FE đặt, vd "vocab:345", "vocab:new:12", "quiz:new". */
  formKey: string
  content: unknown
  updatedAt?: string | null
}
