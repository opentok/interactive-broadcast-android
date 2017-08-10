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

    private final static String LOG_TAG = "opentok-config";

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

    public static void setPublisherVGASimulcast(PublisherKit publisher, boolean enable){
        setPublisherVGASimulcastNative(publisher, enable);
    }

    public static void setOTKitLogs(boolean otkitLogs) {
        setOTKitLogsNative(otkitLogs);
    }

    public static void setJNILogs(boolean jniLogs) {
        setJNILogsNative(jniLogs);
    }

    public static void setWebRTCLogs(boolean webrtcLogs) {
        setWebRTCLogsNative(webrtcLogs);
    }

    public static String getPublisherInfoStats(PublisherKit publisher) {
        String publisherStats = getPublisherInfoStatsNative(publisher);

        return publisherStats;
    }

    public static String getSubscriberInfoStats(SubscriberKit subscriber) {
        String subscriberStats = getSubscriberInfoStatsNative(subscriber);

        return subscriberStats;
    }

    public static String getPublisherStat(PublisherKit publisher, String peerId, long stream, String key){
        return getPublisherStatNative(publisher, peerId, stream, key);
    }

    public static long[] getPublisherAudioStreams(PublisherKit publisher, String peerId){
        return getPublisherAudioStreamsNative(publisher, peerId);
    }

    public static long[] getPublisherVideoStreams(PublisherKit publisher, String peerId){
        return getPublisherVideoStreamsNative(publisher, peerId);
    }

    public static String getSubscriberStat(SubscriberKit subscriber, long stream, String key) {
        return getSubscriberStatNative(subscriber, stream, key);
    }

    public static long[] getSubscriberAudioStreams(SubscriberKit subscriber){
        return getSubscriberAudioStreamsNative(subscriber);
    }

    public static long[] getSubscriberVideoStreams(SubscriberKit subscriber){
        return getSubscriberVideoStreamsNative(subscriber);
    }

    public static String getSDKVersion(Session session){
        String sdkVersion = getSDKVersionNative(session);
        return sdkVersion;
    }

    public static void setUseMediaCodecFactories(boolean value) {
        setUseMediaCodecFactoriesNative(value);
    }

    public static void setPreferH264Codec(boolean value) {
        setPreferH264CodecNative(value);
    }

    //Private method, use for testing error conditions only.
    public static void generateArbitraryErrorPublisher(PublisherKit publisher, int errorcode){
        generateArbitraryErrorPublisherNative(publisher, errorcode);
    }

    //Private method, use for testing error conditions only.
    public static void generateArbitraryErrorSubscriber(SubscriberKit subscriber, int errorcode){
        generateArbitraryErrorSubscriberNative(subscriber, errorcode);
    }

    public static native void setProxy(String host, int port);

    protected static native void setAPIRootURLNative(String host, boolean ssl, int port, boolean rumorSSL);
    protected static native void setPublisherVGASimulcastNative(PublisherKit publisher, boolean enable);
    protected static native void setOTKitLogsNative(boolean otkitLogs);
    protected static native void setJNILogsNative(boolean jniLogs);
    protected static native void setWebRTCLogsNative(boolean webrtcLogs);

    protected static native String getPublisherInfoStatsNative(PublisherKit publisher);
    protected static native String getSubscriberInfoStatsNative(SubscriberKit subscriber);
    protected static native String getPublisherStatNative(PublisherKit publisher, String peerId, long stream, String key);
    protected static native long[] getPublisherAudioStreamsNative(PublisherKit publisher, String peerId);
    protected static native long[] getPublisherVideoStreamsNative(PublisherKit publisher, String peerId);
    protected static native long[] getSubscriberAudioStreamsNative(SubscriberKit subscriber);
    protected static native long[] getSubscriberVideoStreamsNative(SubscriberKit subscriber);
    protected static native String getSubscriberStatNative(SubscriberKit subscriber, long stream, String key);
    protected static native String getSDKVersionNative(Session session);
    protected static native void generateArbitraryErrorPublisherNative(PublisherKit publisher, int errorcode);
    protected static native void generateArbitraryErrorSubscriberNative(SubscriberKit subscriber, int errorcode);

    protected static native void setUseMediaCodecFactoriesNative(boolean value);
    protected static native void setPreferH264CodecNative(boolean value);
}
