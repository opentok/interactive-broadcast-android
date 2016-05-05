package com.tokbox.android.IB.config;

public class IBConfig {

    // *** Fill the following variables using your own Project info  ***

    // Replace with the backend URL
    public static String BACKEND_BASE_URL = "https://chatshow-tesla.herokuapp.com";

    // Replace with the signaling URL
    public static String SIGNALING_URL = "";
    public static String FRONTEND_URL = "";
    public static String DEFAULT_EVENT_IMAGE = "";
    public static String INSTANCE_ID = "";
    public static String USER_TYPE = "";
    public static String USER_NAME = "Anonymous";
    private String mUserType = "";

    // For internal use only. Please do not modify or remove this code.
    public static final String LOG_CLIENT_VERSION = "android-vsol-1.0.0";
    public static final String LOG_SOURCE = "ib";

    public void setUserType(String userType) {
        mUserType = userType;
    }

    public String getUserType() {
        return mUserType;
    }

}
