package com.jp.elearningjp.shared.constants;

public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    public static final class Auth {
        public static final String BASE = "/auth";
        public static final String LOGIN = "/login";
        public static final String REGISTER = "/register";
        public static final String LOGOUT = "/logout";
        public static final String REFRESH = "/refresh";
        public static final String INTROSPECT = "/introspect";
        public static final String FORGOT_PASSWORD = "/forgot-password";
        public static final String RESET_PASSWORD = "/reset-password";
        public static final String GOOGLE = "/google";
    }

    public static final class User {
        public static final String BASE = "/users";
        public static final String ME = "/me";
        public static final String CHANGE_PASSWORD = "/change-password";
        public static final String CHANGE_AVATAR = "/change-avatar";
        public static final String LOCK = "/lock";
        public static final String UNLOCK = "/unlock";
        public static final String STAFF = "/staff";
    }

    public static final class Course {
        public static final String BASE = "/courses";
        public static final String BY_ID = "/{id}";
        public static final String BY_SLUG = "/slug/{slug}";
        public static final String BY_LEVEL = "/level/{level}";
        public static final String PUBLISH = "/{id}/publish";
        public static final String UNPUBLISH = "/{id}/unpublish";
        public static final String ARCHIVE = "/{id}/archive";
        public static final String ENROLL = "/{id}/enroll";
        public static final String PROGRESS = "/{id}/progress";
        public static final String LESSONS = "/{courseId}/lessons";
        public static final String LESSON_BY_ID = "/{courseId}/lessons/{lessonId}";
        public static final String LESSON_REORDER = "/{courseId}/lessons/reorder";

        // Relative paths cho LessonController (khi đã map tới LESSONS)
        public static final String LESSON_BY_ID_RELATIVE = "/{lessonId}";
        public static final String LESSON_REORDER_RELATIVE = "/reorder";
    }

    public static final class Lesson {
        public static final String BASE = "/lessons";
        public static final String BY_ID = "/{id}";
        public static final String COMPLETE = "/{id}/complete";
        public static final String QUIZ = "/{id}/quiz";
        public static final String QUIZ_MANAGE = "/{id}/quiz/manage";
        public static final String QUIZ_SUBMIT = "/{id}/quiz/submit";
        public static final String QUIZ_QUESTIONS = "/{id}/quiz/questions";
        public static final String QUIZ_QUESTIONS_REORDER = "/{id}/quiz/questions/reorder";

        // ===== Section endpoints =====
        // Lưu ý: phải có prefix BASE ("/lessons") để path đầy đủ là
        // /api/v1/lessons/{id}/sections — khớp FE + docs/course-lesson-module.md.
        public static final String SECTIONS = "/lessons/{id}/sections";
        /** PUT/DELETE theo id section (không cần lessonId — service tự tra lesson). */
        public static final String SECTION_BY_ID = "/lessons/sections/{sectionId}";
        public static final String SECTIONS_REORDER = "/lessons/{id}/sections/reorder";

        // ===== Lesson Items endpoints (controller riêng, root path: /lesson-sections) =====
        public static final String LESSON_SECTIONS = "/lesson-sections";
        public static final String KANJI_ITEMS = "/{sectionId}/kanji-items";
        public static final String KANJI_ITEM_BY_ID = "/kanji-items/{itemId}";
        public static final String KANJI_ITEMS_REORDER = "/{sectionId}/kanji-items/reorder";
        public static final String VOCABULARY_ITEMS = "/{sectionId}/vocabulary-items";
        public static final String VOCABULARY_ITEM_BY_ID = "/vocabulary-items/{itemId}";
        public static final String VOCABULARY_ITEMS_REORDER = "/{sectionId}/vocabulary-items/reorder";
        public static final String GRAMMAR_ITEMS = "/{sectionId}/grammar-items";
        public static final String GRAMMAR_ITEM_BY_ID = "/grammar-items/{itemId}";
        public static final String GRAMMAR_ITEMS_REORDER = "/{sectionId}/grammar-items/reorder";

        // ===== Lesson examples endpoints (controller riêng) =====
        public static final String LESSON_VOCABULARY_ITEMS = "/lesson-vocabulary-items";
        public static final String LESSON_VOCABULARY_EXAMPLES = "/{vocabItemId}/examples";
        public static final String VOCAB_EXAMPLE_BY_ID = "/examples/{exampleId}";

        public static final String LESSON_GRAMMAR_ITEMS = "/lesson-grammar-items";
        public static final String LESSON_GRAMMAR_EXAMPLES = "/{grammarItemId}/examples";
        public static final String GRAMMAR_EXAMPLE_BY_ID = "/examples/{exampleId}";

        // ===== Kanji items (related vocab do giáo viên chọn từ từ điển hoặc tạo mới) =====
        public static final String LESSON_KANJI_ITEMS = "/lesson-kanji-items";
    }

    public static final class Quiz {
        public static final String BASE = "/quiz";
        public static final String QUESTION_BY_ID = "/questions/{id}";
    }

    public static final class Enrollment {
        public static final String BASE = "/enrollments";
        public static final String ME = "/me";
        public static final String BY_ID = "/{enrollmentId}";
    }

    public static final class Progress {
        public static final String BASE = "/progress";
        public static final String START = "/lessons/{lessonId}/start";
        public static final String COMPLETE = "/lessons/{lessonId}/complete";
        public static final String BY_LESSON = "/lessons/{lessonId}";
        public static final String BY_COURSE = "/courses/{courseId}";
    }

    public static final class Srs {
        public static final String BASE = "/srs";
        public static final String DUE = "/due";
        public static final String ITEMS = "/items";
        public static final String BATCH = "/batch";
        public static final String LESSON_ADD_ALL = "/lessons/{lessonId}/add-all";
        public static final String REVIEW = "/items/{id}/review";
        public static final String STATS = "/stats";
        public static final String FORECAST = "/forecast";
        public static final String SAVED_IDS = "/saved-ids";
    }

    public static final class Dictionary {
        public static final String BASE = "/dictionary";
        public static final String SEARCH = "/search";
        public static final String SEARCH_VOCAB = "/search/vocab";
        public static final String SEARCH_KANJI = "/search/kanji";
        public static final String SEARCH_GRAMMAR = "/search/grammar";
        public static final String VOCAB = "/vocab/{id}";
        public static final String KANJI = "/kanji/{character}";
        public static final String GRAMMAR = "/grammar/{id}";
        public static final String KANA = "/kana";
        public static final String KANA_DETAIL = "/kana/{character}";
        public static final String QUICK_LOOKUP = "/quick-lookup";

        /** Ghi nhận 1 lần tra từ điển (instrumented lookup — cho Progress page). */
        public static final String LOOKUP = "/lookup";
    }

    public static final class Study {
        public static final String BASE = "/study";
        /** Heartbeat thời gian học từ FE. */
        public static final String HEARTBEAT = "/heartbeat";
    }

    /** Trang "Tiến độ học tập" — 5 khối (Mazii-style). */
    public static final class LearningProgress {
        public static final String BASE = "/progress";
        /** Khối 1 — tổng hợp theo kỳ. */
        public static final String SUMMARY = "/summary";
        /** Khối 2 — lịch + streak. */
        public static final String CALENDAR = "/calendar";
        /** Khối 3 — biểu đồ time-series (Bước 6). */
        public static final String CHART = "/chart";
        /** Khối 4 — top 30 từ tra nhiều nhất (Bước 7). */
        public static final String TOP_LOOKUPS = "/top-lookups";
        /** Khối 5 — bảng xếp hạng (Bước 8). */
        public static final String LEADERBOARD = "/leaderboard";
        /** Hạng cá nhân của user hiện tại. */
        public static final String LEADERBOARD_ME = "/leaderboard/me";
        /** Force refresh leaderboard — đọc DB rồi ghi lại Redis Sorted Set. */
        public static final String LEADERBOARD_REFRESH = "/leaderboard/refresh";
        /**
         * Hard refresh leaderboard — XOÁ key Redis rồi dựng lại từ đầu.
         *
         * <p>Tách riêng endpoint (không phải query param {@code ?hard=true}) để
         * {@code @PreAuthorize} cấp quyền theo method — không để quyền bị điều
         * khiển bằng query string.</p>
         */
        public static final String LEADERBOARD_REFRESH_HARD = "/leaderboard/refresh/hard";
        /** Mục tiêu số từ mới mỗi tuần của user hiện tại. */
        public static final String GOAL = "/goal";
    }

    /**
     * Comment endpoints — gắn với target (LESSON / VOCABULARY / KANJI / GRAMMAR)
     * thông qua cặp ({@code targetType}, {@code targetId}).
     *
     * <p>URL theo target: {@code /api/v1/{targetType}/{targetId}/comments}</p>
     */
    public static final class Comment {
        public static final String BY_TARGET = "/{targetType}/{targetId}/comments";
        public static final String BY_ID = "/comments/{id}";
        public static final String LIKE = "/comments/{id}/like";
        public static final String DISLIKE = "/comments/{id}/dislike";
        public static final String REPLIES = "/comments/{id}/replies";
        /** Vị trí comment (target, từ khóa/khóa học, thứ tự) — để mở thẳng từ thông báo. */
        public static final String LOCATION = "/comments/{id}/location";
        /** Báo cáo vi phạm bình luận. */
        public static final String REPORT = "/comments/{id}/report";
    }

    /** Reply endpoints. Reply lồng nhau qua {@code parentReplyId} trong payload. */
    public static final class CommentReply {
        public static final String BY_ID = "/comment-replies/{id}";
        public static final String LIKE = "/comment-replies/{id}/like";
        public static final String DISLIKE = "/comment-replies/{id}/dislike";
        /** Báo cáo vi phạm trả lời. */
        public static final String REPORT = "/comment-replies/{id}/report";
    }

    /** Kiểm duyệt bình luận: ADMIN (mọi bình luận), TEACHER (bình luận trong bài học thuộc khóa của mình). */
    public static final class AdminModeration {
        public static final String BASE = "/admin/moderation";
        public static final String REPORTS = "/reports";
        public static final String COMMENTS = "/comments";
        public static final String COMMENT_HIDE = "/comments/{id}/hide";
        public static final String COMMENT_RESTORE = "/comments/{id}/restore";
        public static final String COMMENT_DISMISS = "/comments/{id}/dismiss-reports";
        public static final String COMMENT_BY_ID = "/comments/{id}";
        public static final String REPLY_HIDE = "/replies/{id}/hide";
        public static final String REPLY_RESTORE = "/replies/{id}/restore";
        public static final String REPLY_DISMISS = "/replies/{id}/dismiss-reports";
        public static final String REPLY_BY_ID = "/replies/{id}";
    }

    /**
     * Thông báo trong ứng dụng (in-app notifications).
     *
     * <p>Snapshot đọc qua REST; bản real-time được đẩy qua WebSocket STOMP tới
     * {@code /user/queue/notifications} (endpoint handshake {@code /ws}).</p>
     */
    public static final class Notification {
        public static final String BASE = "/notifications";
        public static final String UNREAD_COUNT = "/unread-count";
        public static final String RECENT = "/recent";
        public static final String READ = "/{id}/read";
        public static final String READ_ALL = "/read-all";
    }

    public static final class Exam {
        public static final String BASE = "/exams";
        public static final String BY_ID = "/{id}";
        public static final String START = "/{id}/attempts";
    }

    public static final class ExamAttempt {
        public static final String BASE = "/exam-attempts";
        public static final String BY_ID = "/{id}";
        public static final String SAVE_ANSWER = "/{id}/answers";
        public static final String SUBMIT = "/{id}/submit";
        public static final String RESULT = "/{id}/result";
        public static final String REVIEW = "/{id}/review";
        public static final String MY_HISTORY = "/my-history";
    }

    public static final class AdminDashboard {
        public static final String BASE = "/admin/dashboard";
        public static final String OVERVIEW = "/overview";
    }

    public static final class AdminBroadcast {
        public static final String BASE = "/admin/broadcasts";
        public static final String RECIPIENT_COUNT = "/recipient-count";
    }

    /** Theo dõi học viên: ADMIN mọi khóa, TEACHER khóa do mình tạo. */
    public static final class TeacherStudents {
        public static final String BASE = "/teacher/courses/{courseId}/students";
        public static final String BY_USER = "/{userId}";
    }

    public static final class AdminScenario {
        public static final String BASE = "/admin/scenarios";
        public static final String BY_ID = "/{id}";
    }

    public static final class AdminUser {
        public static final String BASE = "/admin/users";
        public static final String BY_ID = "/{id}";
        public static final String STATUS = "/{id}/status";
        public static final String ROLES = "/{id}/roles";
    }

    public static final class AdminExam {
        public static final String BASE = "/admin/exams";
        public static final String BY_ID = "/{id}";
        public static final String AI_EXTRACT = "/ai-extract";
        public static final String PREVIEW = "/{id}/preview";
        public static final String UPDATE_QUESTION = "/{id}/questions/{questionId}";
        public static final String ADD_QUESTION = "/{id}/sections/{sectionId}/questions";
        public static final String DELETE_QUESTION = "/{id}/questions/{questionId}";
        public static final String BY_COURSE = "/by-course/{courseId}";
        public static final String PUBLISH = "/{id}/publish";
    }

    public static final class AiSpeaking {
        public static final String BASE = "/ai/sessions";
        public static final String BY_ID = "/{id}";
        public static final String MESSAGES = "/{id}/messages";
        public static final String TTS = "/ai/tts";
        public static final String TRANSLATE = "/ai/translate";
        public static final String FURIGANA = "/ai/furigana";
    }

    public static final class AiScenario {
        public static final String BASE = "/ai/scenarios";
        public static final String BY_ID = "/{id}";
    }

    public static final class NotebookGroup {
        public static final String BASE = "/notebook-groups";
        public static final String BY_ID = "/{id}";
        public static final String WITH_NOTEBOOKS = "/with-notebooks";
    }

    public static final class Notebook {
        public static final String BASE = "/notebooks";
        public static final String BY_ID = "/{id}";
        public static final String UNGROUPED = "/ungrouped";
        public static final String BY_GROUP = "/group/{groupId}";
        public static final String SEARCH = "/search";
        public static final String MOVE = "/{id}/move";

        // Words & Import/Export
        public static final String WORDS = "/{id}/words";
        public static final String WORD_BY_ID = "/{id}/words/{wordId}";
        public static final String WORDS_BATCH = "/{id}/words/batch";
        public static final String WORDS_SHUFFLE = "/{id}/words/shuffle";
        public static final String WORDS_IMPORT = "/{id}/words/import";
        public static final String EXPORT_EXCEL = "/{id}/export/excel";
        public static final String EXPORT_KANJI_PDF = "/{id}/export/kanji-pdf";
        public static final String EXPORT_KANJI_PDF_PREVIEW = "/{id}/export/kanji-pdf/preview";
        public static final String EXPORT_KANJI_PDF_META = "/{id}/export/kanji-pdf/meta";
        public static final String STATS_OVERVIEW = "/stats/overview";

        // Review SRS & Quiz
        public static final String REVIEW_ITEMS = "/{id}/review";
        public static final String REVIEW_SUBMIT = "/{id}/review/submit";

        // Sharing & Cloning
        public static final String SHARE = "/{id}/share";
        public static final String SHARE_BY_CODE = "/share/{shareCode}";
        public static final String SHARE_TOGGLE = "/{id}/share/toggle";
        public static final String PUBLIC_TOGGLE = "/{id}/public/toggle";
        public static final String CLONE_BY_CODE = "/clone";

        // Community Exploration
        public static final String EXPLORE = "/explore";
        public static final String EXPLORE_BY_ID = "/explore/{id}";
        public static final String EXPLORE_LIKE = "/explore/{id}/like";
        public static final String EXPLORE_FAVORITES = "/explore/favorites";
        public static final String EXPLORE_RECENT = "/explore/recent";
    }
}
