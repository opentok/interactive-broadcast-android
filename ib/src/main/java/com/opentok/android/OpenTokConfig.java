package com.opentok.android;

public class OpenTokConfig {


    static {
        System.loadLibrary("opentok");
    }

    public static native void enableVP8HWDecoder(boolean vp8hwdecoder);
}
