package com.tokbox.android.IB.config;

public class IBConfig {

    // *** Fill the following variables using your own Project info  ***

    // Replace with the backend URL
    public static String BACKEND_BASE_URL = "https://ibs-dev-server.herokuapp.com";

    // Replace with the signaling URL
    public static String ADMIN_ID = "";
    public static String USER_TYPE = "";
    public static String USER_NAME = "Anonymous";
    public static String AUTH_TOKEN = "";
    private String mUserType = "";

    // For internal use only. Please do not modify or remove this code.
    public static final String LOG_CLIENT_VERSION = "android-ib-2.0.1";
    public static final String LOG_COMPONENTID = "iBS";


    public void setUserType(String userType) {
        mUserType = userType;
    }

    public String getUserType() {
        return mUserType;
    }

}
