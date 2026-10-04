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
        public static final String CHANGE_AVATAR = "/change-avatar" ;
        public static final String LOCK = "/lock";
        public static final String UNLOCK = "/unlock";
        public static final String STAFF = "/staff";
    }
}