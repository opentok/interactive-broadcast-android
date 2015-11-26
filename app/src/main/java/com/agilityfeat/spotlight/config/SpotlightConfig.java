package com.agilityfeat.spotlight.config;

import com.agilityfeat.spotlight.BuildConfig;

public class SpotlightConfig {

    // *** Fill the following variables using your own Project info  ***

    // Replace with the backend URL
    public static final String BACKEND_BASE_URL =
            (BuildConfig.DEBUG) ? "https://chatshow-tesla.herokuapp.com" : "https://spotlight-demos-backend.herokuapp.com";

    public static String FRONTEND_URL = "";
    public static final String SIGNALING_URL = "https://chatshow-signaling.herokuapp.com";
    public static String DEFAULT_EVENT_IMAGE = "";
    public static String INSTANCE_ID = "AAAA1";
    public static String USER_TYPE = "fan";
    public static String USER_NAME = "anonymous";
    public static String NEWRELIC_TOKEN = "AA7e042e8e5d41efec5f390b3f00cf4943c1a3b79d";
    private String mUserType = "";



    public void setUserType(String userType) {
        mUserType = userType;
    }

    public String getUserType() {
        return mUserType;
    }

}
