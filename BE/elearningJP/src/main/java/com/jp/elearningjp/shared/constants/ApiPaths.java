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
        public static final String QUIZ_SUBMIT = "/{id}/quiz/submit";
        public static final String QUIZ_QUESTIONS = "/{id}/quiz/questions";

        // ===== Section endpoints =====
        public static final String SECTIONS = "/{id}/sections";
        public static final String SECTION_BY_ID = "/{id}/sections/{sectionId}";

        // ===== Lesson Items endpoints (controller riêng, root path: /lesson-sections) =====
        public static final String LESSON_SECTIONS = "/lesson-sections";
        public static final String KANJI_ITEMS = "/{sectionId}/kanji-items";
        public static final String KANJI_ITEM_BY_ID = "/kanji-items/{itemId}";
        public static final String VOCABULARY_ITEMS = "/{sectionId}/vocabulary-items";
        public static final String VOCABULARY_ITEM_BY_ID = "/vocabulary-items/{itemId}";
        public static final String GRAMMAR_ITEMS = "/{sectionId}/grammar-items";
        public static final String GRAMMAR_ITEM_BY_ID = "/grammar-items/{itemId}";

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
    }

    /** Reply endpoints. Reply lồng nhau qua {@code parentReplyId} trong payload. */
    public static final class CommentReply {
        public static final String BY_ID = "/comment-replies/{id}";
        public static final String LIKE = "/comment-replies/{id}/like";
        public static final String DISLIKE = "/comment-replies/{id}/dislike";
    }
}