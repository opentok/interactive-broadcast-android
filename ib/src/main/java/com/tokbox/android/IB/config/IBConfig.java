package com.tokbox.android.IB.config;

public class IBConfig {

    // *** Fill the following variables using your own Project info  ***

    // Replace with the backend URL
    public static String BACKEND_BASE_URL = "https://tokbox-ib-staging-tesla.herokuapp.com";

    // Replace with the signaling URL
    public static String SIGNALING_URL = "";
    public static String FRONTEND_URL = "";
    public static String DEFAULT_EVENT_IMAGE = "";
    public static String INSTANCE_ID = "";
    public static String ADMIN_ID = "";
    public static String USER_TYPE = "";
    public static String USER_NAME = "Anonymous";
    private String mUserType = "";

    // For internal use only. Please do not modify or remove this code.
    public static final String LOG_CLIENT_VERSION = "android-ib-1.0.3";
    public static final String LOG_COMPONENTID = "iBS";


    public void setUserType(String userType) {
        mUserType = userType;
    }

    public String getUserType() {
        return mUserType;
    }

}
