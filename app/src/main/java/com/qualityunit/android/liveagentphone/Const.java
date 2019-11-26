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
        public static final long MAX_INCOMING_CALL_PUSH_DELAY = 5000;
        public static final String PUSH_TYPE_INCOMING_CALL = "I";
        public static final String PUSH_TYPE_INIT_CALL = "O";
        public static final String PUSH_TYPE_CANCEL_INIT_CALL = "OC";
    }

    public static class OnlineStatus {
        public static final String STATUS_ONLINE_FLAG = "N";
        public static final String STATUS_OFFLINE_FLAG = "F";
    }

    public static class NotificationId {
        public static final int ONGOING_NOTIFICATION_ID = 1;
        public static final int INCOMING_NOTIFICATION_ID = 2;
        public static final int INIT_CALL_NOTIFICATION_ID = 3;
    }

    public static class ChannelId {
        public static final String SERVICE_CHANNEL_ID = "livephone_service";
        public static final String INCOMING_CHANNEL_ID = "livephone_incoming";
        public static final String MAKE_CALL_CHANNEL_ID = "livephone_make_call";
    }

}
