import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  PageResponse,
  CourseSummary,
  CourseDetail,
  CourseCreateRequest,
  CourseUpdateRequest,
  Enrollment,
  LessonSummary,
  LessonDetail,
  LessonCreateRequest,
  LessonUpdateRequest,
  LessonReorderRequest,
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
  LessonVocabularyExampleCreateRequest,
  LessonVocabularyExampleUpsertRequest,
  LessonGrammarItem,
  LessonGrammarItemCreateRequest,
  LessonGrammarItemUpdateRequest,
  LessonGrammarExampleCreateRequest,
  LessonGrammarExampleUpsertRequest,
  LessonQuizDetail,
  QuizSubmitRequest,
  QuizSubmitResponse,
  LessonQuizManageDetail,
  CourseProgress,
  JlptLevel,
} from '@/types'

// ============================================================================
// COURSE
// ============================================================================
export const courseService = {
  // ---- Public ----
  getCourses(params?: {
    jlptLevel?: JlptLevel
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<CourseSummary>>> {
    return axiosClient.get('/courses', { params })
  },

  getCourseById(id: number): Promise<ApiResponse<CourseDetail>> {
    return axiosClient.get(`/courses/${id}`)
  },

  getCourseBySlug(slug: string): Promise<ApiResponse<CourseDetail>> {
    return axiosClient.get(`/courses/slug/${encodeURIComponent(slug)}`)
  },

  getCourseProgress(id: number): Promise<ApiResponse<CourseProgress>> {
    return axiosClient.get(`/courses/${id}/progress`)
  },

  // ---- Admin/Teacher ----
  getAllCoursesAdmin(params?: {
    page?: number
    size?: number
  }): Promise<ApiResponse<PageResponse<CourseSummary>>> {
    return axiosClient.get('/courses/admin', { params })
  },

  createCourse(payload: CourseCreateRequest): Promise<ApiResponse<CourseSummary>> {
    return axiosClient.post('/courses', payload)
  },

  updateCourse(
    id: number,
    payload: CourseUpdateRequest,
  ): Promise<ApiResponse<CourseSummary>> {
    return axiosClient.put(`/courses/${id}`, payload)
  },

  deleteCourse(id: number): Promise<ApiResponse<void>> {
    return axiosClient.delete(`/courses/${id}`)
  },

  publishCourse(
    id: number,
    publish: boolean,
  ): Promise<ApiResponse<CourseSummary>> {
    return axiosClient.patch(`/courses/${id}/publish`, null, {
      params: { publish },
    })
  },
}

// ============================================================================
// ENROLLMENT
// ============================================================================
export const enrollmentService = {
  enroll(courseId: number): Promise<ApiResponse<Enrollment>> {
    return axiosClient.post(`/courses/${courseId}/enroll`)
  },

  getMyEnrollments(): Promise<ApiResponse<Enrollment[]>> {
    return axiosClient.get('/enrollments/me')
  },

  getMyActiveEnrollments(): Promise<ApiResponse<Enrollment[]>> {
    return axiosClient.get('/enrollments/me/active')
  },

  getEnrollmentById(enrollmentId: number): Promise<ApiResponse<Enrollment>> {
    return axiosClient.get(`/enrollments/${enrollmentId}`)
  },

  drop(enrollmentId: number): Promise<ApiResponse<void>> {
    return axiosClient.post(`/enrollments/${enrollmentId}/drop`)
  },

  dropByCourseId(courseId: number): Promise<ApiResponse<void>> {
    return axiosClient.post(`/enrollments/by-course/${courseId}/drop`)
  },
}

// ============================================================================
// LESSON
// ============================================================================
export const lessonService = {
  getLessonsByCourse(courseId: number): Promise<ApiResponse<LessonSummary[]>> {
    return axiosClient.get(`/courses/${courseId}/lessons`)
  },

  getLessonDetail(id: number): Promise<ApiResponse<LessonDetail>> {
    return axiosClient.get(`/lessons/${id}`)
  },

  createLesson(
    courseId: number,
    payload: LessonCreateRequest,
  ): Promise<ApiResponse<LessonDetail>> {
    return axiosClient.post(`/courses/${courseId}/lessons`, payload)
  },

  updateLesson(
    id: number,
    payload: LessonUpdateRequest,
  ): Promise<ApiResponse<LessonDetail>> {
    return axiosClient.put(`/lessons/${id}`, payload)
  },

  deleteLesson(id: number): Promise<ApiResponse<void>> {
    return axiosClient.delete(`/lessons/${id}`)
  },

  reorderLessons(
    courseId: number,
    payload: LessonReorderRequest,
  ): Promise<ApiResponse<void>> {
    return axiosClient.post(`/courses/${courseId}/lessons/reorder`, payload)
  },

  completeLesson(id: number): Promise<ApiResponse<CourseProgress>> {
    return axiosClient.post(`/lessons/${id}/complete`)
  },
}

// ============================================================================
// PROGRESS
// ============================================================================
export const progressService = {
  startLesson(lessonId: number): Promise<ApiResponse<void>> {
    return axiosClient.post(`/progress/lessons/${lessonId}/start`)
  },

  getMyProgressForLesson(lessonId: number): Promise<ApiResponse<any>> {
    return axiosClient.get(`/progress/lessons/${lessonId}`)
  },
}

// ============================================================================
// LESSON SECTIONS
// ============================================================================
export const sectionService = {
  getSections(lessonId: number): Promise<ApiResponse<LessonSection[]>> {
    return axiosClient.get(`/lessons/${lessonId}/sections`)
  },

  createSection(
    lessonId: number,
    payload: LessonSectionCreateRequest,
  ): Promise<ApiResponse<LessonSection>> {
    return axiosClient.post(`/lessons/${lessonId}/sections`, payload)
  },

  updateSection(
    id: number,
    payload: LessonSectionUpdateRequest,
  ): Promise<ApiResponse<LessonSection>> {
    return axiosClient.put(`/lessons/sections/${id}`, payload)
  },

  deleteSection(id: number): Promise<ApiResponse<void>> {
    return axiosClient.delete(`/lessons/sections/${id}`)
  },

  reorderSections(
    lessonId: number,
    payload: LessonSectionReorderRequest,
  ): Promise<ApiResponse<void>> {
    return axiosClient.post(`/lessons/${lessonId}/sections/reorder`, payload)
  },
}

// ============================================================================
// LESSON KANJI ITEMS
// ============================================================================
// Path khớp BE LessonKanjiItemController (section-based):
//   POST   /lesson-sections/{sectionId}/kanji-items
//   PUT    /lesson-kanji-items/kanji-items/{itemId}
export const kanjiItemService = {
  createItem(
    sectionId: number,
    payload: LessonKanjiItemCreateRequest,
  ): Promise<ApiResponse<LessonKanjiItem>> {
    return axiosClient.post(`/lesson-sections/${sectionId}/kanji-items`, payload)
  },

  updateItem(
    id: number,
    payload: LessonKanjiItemUpdateRequest,
  ): Promise<ApiResponse<LessonKanjiItem>> {
    return axiosClient.put(`/lesson-kanji-items/kanji-items/${id}`, payload)
  },

  deleteItem(id: number): Promise<ApiResponse<void>> {
    return axiosClient.delete(`/lesson-kanji-items/kanji-items/${id}`)
  },

  /**
   * Lấy chi tiết 1 kanji item từ BE (kèm related vocabulary đầy đủ).
   * Endpoint: GET /api/v1/lesson-kanji-items/kanji-items/{itemId}
   * Dùng làm fallback khi response tổng bị thiếu relatedVocabulary.
   */
  getItemDetail(id: number): Promise<ApiResponse<LessonKanjiItem>> {
    return axiosClient.get(`/lesson-kanji-items/kanji-items/${id}`)
  },
}

// ============================================================================
// LESSON VOCAB ITEMS
// ============================================================================
// Path khớp BE LessonVocabularyItemController (section-based):
//   POST /lesson-sections/{sectionId}/vocabulary-items
export const vocabItemService = {
  createItem(
    sectionId: number,
    payload: LessonVocabularyItemCreateRequest,
  ): Promise<ApiResponse<LessonVocabularyItem>> {
    return axiosClient.post(`/lesson-sections/${sectionId}/vocabulary-items`, payload)
  },

  updateItem(
    id: number,
    payload: LessonVocabularyItemUpdateRequest,
  ): Promise<ApiResponse<LessonVocabularyItem>> {
    return axiosClient.put(`/lesson-vocabulary-items/vocabulary-items/${id}`, payload)
  },

  deleteItem(id: number): Promise<ApiResponse<void>> {
    return axiosClient.delete(`/lesson-vocabulary-items/vocabulary-items/${id}`)
  },

  addExample(
    itemId: number,
    payload: LessonVocabularyExampleCreateRequest,
  ): Promise<ApiResponse<void>> {
    return axiosClient.post(`/lessons/vocab-items/${itemId}/examples`, payload)
  },

  upsertExample(
    itemId: number,
    payload: LessonVocabularyExampleUpsertRequest,
  ): Promise<ApiResponse<void>> {
    return axiosClient.put(
      `/lessons/vocab-items/${itemId}/examples`,
      payload,
    )
  },

  /**
   * Lấy chi tiết 1 vocab item từ BE (kèm examples đầy đủ).
   * Endpoint: GET /api/v1/lesson-vocabulary-items/vocabulary-items/{itemId}
   * Dùng làm fallback khi response tổng /lessons/{id} bị thiếu examples.
   */
  getItemDetail(id: number): Promise<ApiResponse<LessonVocabularyItem>> {
    return axiosClient.get(`/lesson-vocabulary-items/vocabulary-items/${id}`)
  },
}

// ============================================================================
// LESSON GRAMMAR ITEMS
// ============================================================================
// Path khớp BE LessonGrammarItemController (section-based):
//   POST /lesson-sections/{sectionId}/grammar-items
export const grammarItemService = {
  createItem(
    sectionId: number,
    payload: LessonGrammarItemCreateRequest,
  ): Promise<ApiResponse<LessonGrammarItem>> {
    return axiosClient.post(`/lesson-sections/${sectionId}/grammar-items`, payload)
  },

  updateItem(
    id: number,
    payload: LessonGrammarItemUpdateRequest,
  ): Promise<ApiResponse<LessonGrammarItem>> {
    return axiosClient.put(`/lesson-grammar-items/grammar-items/${id}`, payload)
  },

  deleteItem(id: number): Promise<ApiResponse<void>> {
    return axiosClient.delete(`/lesson-grammar-items/grammar-items/${id}`)
  },

  addExample(
    itemId: number,
    payload: LessonGrammarExampleCreateRequest,
  ): Promise<ApiResponse<void>> {
    return axiosClient.post(
      `/lessons/grammar-items/${itemId}/examples`,
      payload,
    )
  },

  upsertExample(
    itemId: number,
    payload: LessonGrammarExampleUpsertRequest,
  ): Promise<ApiResponse<void>> {
    return axiosClient.put(
      `/lessons/grammar-items/${itemId}/examples`,
      payload,
    )
  },

  /**
   * Lấy chi tiết 1 grammar item từ BE (kèm examples đầy đủ).
   * Endpoint: GET /api/v1/lesson-grammar-items/grammar-items/{itemId}
   * Dùng làm fallback khi response tổng /lessons/{id} bị thiếu examples.
   */
  getItemDetail(id: number): Promise<ApiResponse<LessonGrammarItem>> {
    return axiosClient.get(`/lesson-grammar-items/grammar-items/${id}`)
  },
}

// ============================================================================
// LESSON QUIZ
// ============================================================================
export const quizService = {
  getLessonQuiz(lessonId: number): Promise<ApiResponse<LessonQuizDetail>> {
    return axiosClient.get(`/lessons/${lessonId}/quiz`)
  },

  /**
   * Lấy đề quiz kèm đáp án đúng — chỉ dành cho Admin/Teacher.
   * Endpoint: GET /api/v1/lessons/{id}/quiz/manage
   * (BE sẽ check quyền owner course; teacher khác sẽ bị 403.)
   */
  getLessonQuizManage(lessonId: number): Promise<ApiResponse<LessonQuizManageDetail>> {
    return axiosClient.get(`/lessons/${lessonId}/quiz/manage`)
  },

  submitQuiz(
    lessonId: number,
    payload: QuizSubmitRequest,
  ): Promise<ApiResponse<QuizSubmitResponse>> {
    return axiosClient.post(`/lessons/${lessonId}/quiz/submit`, payload)
  },
}
