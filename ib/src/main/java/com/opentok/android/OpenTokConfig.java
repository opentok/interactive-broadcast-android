package com.opentok.android;

public class OpenTokConfig {


    static {
        System.loadLibrary("opentok");
    }

    public static native void enableVP8HWDecoder(boolean vp8hwdecoder);

    public static long[] getSubscriberVideoStreams(SubscriberKit subscriber){
        return getSubscriberVideoStreamsNative(subscriber);
    }

    public static String getSubscriberStat(SubscriberKit subscriber, long stream, String key) {
        return getSubscriberStatNative(subscriber, stream, key);
    }

    protected static native long[] getSubscriberAudioStreamsNative(SubscriberKit subscriber);
    protected static native long[] getSubscriberVideoStreamsNative(SubscriberKit subscriber);
    protected static native String getSubscriberStatNative(SubscriberKit subscriber, long stream, String key);
    protected static native String getSubscriberInfoStatsNative(SubscriberKit subscriber);
}
