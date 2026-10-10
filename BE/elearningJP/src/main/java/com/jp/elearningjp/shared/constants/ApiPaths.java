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
    }

    public static final class Enrollment {
        public static final String BASE = "/enrollments";
        public static final String ME = "/me";
        public static final String BY_ID = "/{enrollmentId}";
        public static final String DROP = "/{enrollmentId}/drop";
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
}