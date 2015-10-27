package com.agilityfeat.spotlight.config;

import com.agilityfeat.spotlight.BuildConfig;

public class SpotlightConfig {

    // *** Fill the following variables using your own Project info  ***

    // Replace with the backend URL
    public static final String BACKEND_BASE_URL =
            (BuildConfig.DEBUG) ? "https://chatshow-tesla.herokuapp.com" : "https://spotlight-demos-backend.herokuapp.com";

    public static final String FRONTEND_URL = "https://chatshow.herokuapp.com";
}
