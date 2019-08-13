package com.opentok.android;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class contains API's, which are only used Internally.
 */

public class OpenTokConfig {

    static {
        System.loadLibrary("opentok");
    }

    public static double defaultFrameRate = 30.0;

}
