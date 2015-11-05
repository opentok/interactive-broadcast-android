package com.agilityfeat.spotlight.config;

import com.agilityfeat.spotlight.BuildConfig;

public class SpotlightConfig {

    // *** Fill the following variables using your own Project info  ***

    // Replace with the backend URL
    public static final String BACKEND_BASE_URL =
            (BuildConfig.DEBUG) ? "https://chatshow-tesla.herokuapp.com" : "https://spotlight-demos-backend.herokuapp.com";

    public static final String FRONTEND_URL = "https://chatshow.herokuapp.com";
    public static final String SIGNALING_URL = "https://chatshow-signaling.herokuapp.com";
    public static final String DEFAULT_EVENT_IMAGE = "/img/TAB_VIDEO_PREVIEW_LS.jpg";
    public static String INSTANCE_ID = "AAAA1";
    public static String USER_TYPE = "fan";
    private String mUserType = "";


    public void setUserType(String userType) {
        mUserType = userType;
    }

    public String getUserType() {
        return mUserType;
    }

}
