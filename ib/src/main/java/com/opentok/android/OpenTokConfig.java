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

    public static String getSubscriberStat(SubscriberKit subscriber, long stream, String key) {
        return getSubscriberStatNative(subscriber, stream, key);
    }

    public static long[] getSubscriberVideoStreams(SubscriberKit subscriber){
        return getSubscriberVideoStreamsNative(subscriber);
    }

    public static void setUseMediaCodecFactories(boolean value) {
        setUseMediaCodecFactoriesNative(value);
    }

    protected static native long[] getSubscriberVideoStreamsNative(SubscriberKit subscriber);
    protected static native String getSubscriberStatNative(SubscriberKit subscriber, long stream, String key);
    protected static native void setUseMediaCodecFactoriesNative(boolean value);

}
