package com.tokbox.android.IB.network;

import android.util.Log;

import com.opentok.android.SubscriberKit;

public class NetworkTest {
    private static final String LOG_TAG = "network-test";

    private static final int TIME_WINDOW = 3; //3 seconds
    public static final int TIME_VIDEO_TEST = 15; //time interval to check the video quality in seconds

    public static enum MOSQuality {

        Excellent(5),

        Good(4),

        Fair(3),

        Poor(2),

        Bad(1);

        private int mosQuality;

        MOSQuality(int quality) {
            this.mosQuality = quality;
        }

        public int getMosQuality() {
            return mosQuality;
        }

        static MOSQuality fromQuality(int qualityId) {
            for (MOSQuality quality : MOSQuality.values()) {
                if (quality.getMosQuality() == qualityId) {
                    return quality;
                }
            }
            throw new IllegalArgumentException("unknown quality " + qualityId);
        }
    };

    private MOSQuality mQuality = MOSQuality.Good;
    private double mVideoPLRatio = 0.0;
    private long mVideoBw = 0;
    private long mPrevVideoPacketsLost = 0;
    private long mPrevVideoPacketsRcvd = 0;
    private double mPrevVideoTimestamp = 0;
    private long mPrevVideoBytes = 0;
    private long mStartTestTime = 0;


    private void checkVideoStats(SubscriberKit.SubscriberVideoStats stats) {
        double videoTimestamp = stats.timeStamp / 1000;

        //initialize values
        if (mPrevVideoTimestamp == 0) {
            mPrevVideoTimestamp = videoTimestamp;
            mPrevVideoBytes = stats.videoBytesReceived;
        }

        if (videoTimestamp - mPrevVideoTimestamp >= TIME_WINDOW) {
            //calculate video packets lost ratio
            if (mPrevVideoPacketsRcvd != 0) {
                long pl = stats.videoPacketsLost - mPrevVideoPacketsLost;
                long pr = stats.videoPacketsReceived - mPrevVideoPacketsRcvd;
                long pt = pl + pr;

                if (pt > 0) {
                    mVideoPLRatio = (double) pl / (double) pt;
                }
            }

            mPrevVideoPacketsLost = stats.videoPacketsLost;
            mPrevVideoPacketsRcvd = stats.videoPacketsReceived;

            //calculate video bandwidth
            mVideoBw = (long) ((8 * (stats.videoBytesReceived - mPrevVideoBytes)) / (videoTimestamp - mPrevVideoTimestamp));

            mPrevVideoTimestamp = videoTimestamp;
            mPrevVideoBytes = stats.videoBytesReceived;

            Log.i(LOG_TAG, "Video bandwidth (bps): " + mVideoBw + " Video Bytes received: " + stats.videoBytesReceived + " Video packet lost: " + stats.videoPacketsLost + " Video packet loss ratio: " + mVideoPLRatio);

        }
    }

    public void checkVideoQuality(SubscriberKit.SubscriberVideoStats stats) {

        if (mStartTestTime == 0) {
            mStartTestTime = System.currentTimeMillis() / 1000;
        }

        checkVideoStats(stats);

    }

    public MOSQuality getMOSQuality () {
        if ( mVideoBw <= 150000 || mVideoPLRatio > 0.1 ) {
            mQuality = MOSQuality.Poor;
        }
        else {
            if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1 ) {
                mQuality = MOSQuality.Fair;
            }
            else {
                if (mVideoBw > 250000 && mVideoBw <= 600000 && mVideoPLRatio < 0.01 && mVideoPLRatio > 0.02 ) {
                    mQuality = MOSQuality.Fair;
                }
                else {
                    if (mVideoBw > 250000 && mVideoBw <= 600000 && mVideoPLRatio < 0.02 ) {
                        mQuality = MOSQuality.Good;
                    }
                    else {
                        if (mVideoBw > 600000 && mVideoPLRatio > 0.005 ) {
                            mQuality = MOSQuality.Good;
                        }
                        else {
                            if (mVideoBw > 600000 && mVideoPLRatio < 0.005 ) {
                                mQuality = MOSQuality.Excellent;
                            }
                        }
                    }
                }
            }
        }
        Log.i(LOG_TAG, "MOS QUALITY: "+mQuality.toString());
        mStartTestTime = System.currentTimeMillis() / 1000 + 45;

        return mQuality;
    }

    public long getStartTestTime() {
        return mStartTestTime;
    }

}