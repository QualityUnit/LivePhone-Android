package com.qualityunit.android.liveagentphone;

/**
 * Created by rasto on 31.08.16.
 */
public class Const {

    public static class Api {
        public static final String API_POSTFIX = "/api/v3";
    }

    public static class SortDir {
        public static final String ASCENDING = "ASC";
        public static final String DESCENDING = "DESC";
    }

    public static class MemoryKeys {
        public static final String LOGIN_ET_URL = "etUrl";
        public static final String LOGIN_ET_EMAIL = "etEmail";
        public static final String LOGIN_URL_LIST = "urlList";
        public static final String LOGIN_EMAIL_LIST = "emailList";
        public static final String CURRENT_ACCOUNT = "CURRENT_ACCOUNT";
    }

    public static class Push {
        public static final long MAX_INCOMING_CALL_PUSH_DELAY = 5;
        public static final String PUSH_TYPE_INCOMING_CALL = "I";
        public static final String PUSH_TYPE_INIT_CALL = "O";
        public static final String PUSH_TYPE_CANCEL_INIT_CALL = "OC";
    }


}
