package com.opentok.android;

import java.net.MalformedURLException;
import java.net.URL;

public class OpenTokConfig {


    static {
        System.loadLibrary("opentok");
    }

    public static void setAPIRootURL(String apiRootURL) throws MalformedURLException {
        setAPIRootURL(apiRootURL, true);
    }

    public static void setAPIRootURL(String apiRootURL, boolean rumorSSL) throws MalformedURLException {
        URL url = new URL(apiRootURL);
        boolean ssl = false;
        int port = url.getPort();
        if("https".equals(url.getProtocol())) {
            ssl = true;
            if(port == -1) {
                port = 443;
            }
        } else if("http".equals(url.getProtocol())) {
            ssl = false;
            if(port == -1) {
                port = 80;
            }
        }

        setAPIRootURLNative(url.getHost(), ssl, port, rumorSSL);
    }

    public static native void enableVP8HWDecoder(boolean vp8hwdecoder);

    public static long[] getSubscriberVideoStreams(SubscriberKit subscriber){
        return getSubscriberVideoStreamsNative(subscriber);
    }

    public static String getSubscriberStat(SubscriberKit subscriber, long stream, String key) {
        return getSubscriberStatNative(subscriber, stream, key);
    }

    protected static native void setAPIRootURLNative(String host, boolean ssl, int port, boolean rumorSSL);
    protected static native long[] getSubscriberAudioStreamsNative(SubscriberKit subscriber);
    protected static native long[] getSubscriberVideoStreamsNative(SubscriberKit subscriber);
    protected static native String getSubscriberStatNative(SubscriberKit subscriber, long stream, String key);
    protected static native String getSubscriberInfoStatsNative(SubscriberKit subscriber);
}
