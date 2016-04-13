package com.tokbox.android.IB.network;

import android.print.PrintAttributes;
import android.util.Log;
import android.widget.Toast;

import com.opentok.android.OpenTokConfig;
import com.opentok.android.SubscriberKit;
import com.opentok.android.VideoUtils;

public class NetworkTest {
    private static final String LOG_TAG = "network-test";

    private static final int TIME_WINDOW = 3; //3 seconds
    public static final int TIME_VIDEO_TEST = 15; //time interval to check the video quality in seconds

    private MOSQuality mQuality = MOSQuality.Good;
    private double mVideoPLRatio = 0.0;
    private long mVideoBw = 0;
    private long mPrevVideoPacketsLost = 0;
    private long mPrevVideoPacketsRcvd = 0;
    private double mPrevVideoTimestamp = 0;
    private long mPrevVideoBytes = 0;
    private long mStartTestTime = 0;

    private long mPacketsReceivedAudio = 0;
    private long mPacketsLostAudio = 0;


    private SubscriberKit mTestSubscriber;
    private NetworkTestListener mListener;

    private VideoUtils.Size videoResolution;
    private double videoFrameRate;

    private boolean mAudioOnly = false;

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

    public static interface NetworkTestListener {
        void onVideoQualityUpdated(String connectionId, MOSQuality quality);
        void onAudioQualityUpdated(String connectionId, MOSQuality quality);
    }

    /*public void setNetworkTestListener (NetworkTestListener listener){
        mListener = listener;
    }*/

    public void startNetworkTest(SubscriberKit subscriber, NetworkTestListener listener){
        mTestSubscriber = subscriber;
        mListener = listener;

        if(subscriber.getSubscribeToVideo()) {
            startVideoNetworkTest();
        }
        else {
            startAudioNetworkTest();
        }
    }

    public void updateTest(boolean video){

        if (video) {
            mAudioOnly = false;
            startVideoNetworkTest();
        }
        else {
            mAudioOnly = true;
            startAudioNetworkTest();
        }
    }

    private void startVideoNetworkTest(){

        mTestSubscriber.setVideoStatsListener(new SubscriberKit.VideoStatsListener() {

            @Override
            public void onVideoStats(SubscriberKit subscriber,
                                     SubscriberKit.SubscriberVideoStats stats) {

                //check video quality
                checkVideoQuality(stats);

                //check quality of the video call after TIME_VIDEO_TEST seconds
                if (((System.currentTimeMillis() / 1000 - mStartTestTime) > TIME_VIDEO_TEST)) {
                    NetworkTest.MOSQuality quality = getMOSQuality();
                    mListener.onVideoQualityUpdated(mTestSubscriber.getStream().getConnection().getConnectionId(), quality);
                }
            }
        });

        mTestSubscriber.setAudioStatsListener(null);
    }

    private void startAudioNetworkTest() {
        mTestSubscriber.setVideoStatsListener(null);

        mTestSubscriber.setAudioStatsListener(new SubscriberKit.AudioStatsListener() {
            @Override
            public void onAudioStats(SubscriberKit subscriberKit, SubscriberKit.SubscriberAudioStats stats) {

                checkAudioStats(stats);

                //check quality of the audio call after TIME_VIDEO_TEST seconds
                if (((System.currentTimeMillis() / 1000 - mStartTestTime) > TIME_VIDEO_TEST)) {
                    NetworkTest.MOSQuality quality = getMOSQuality();
                    mListener.onAudioQualityUpdated(mTestSubscriber.getStream().getConnection().getConnectionId(), quality);
                }
            }
        });
    }

    private void checkVideoQuality(SubscriberKit.SubscriberVideoStats stats) {

        if (mStartTestTime == 0) {
            mStartTestTime = System.currentTimeMillis() / 1000;
        }

        checkVideoStats(stats);

    }

    private void checkAudioQuality(SubscriberKit.SubscriberAudioStats stats) {

        if (mStartTestTime == 0) {
            mStartTestTime = System.currentTimeMillis() / 1000;
        }

        checkAudioStats(stats);

    }

    public MOSQuality getMOSQuality () {

        if (!mAudioOnly) {
            //get resolution and frameRate
            this.getPCStats();

            //check values
            if (videoResolution.equals(1280,720)){
                if (videoFrameRate == 30) { //aprox
                    checkHDand30fps();
                }
                else {
                    if(videoFrameRate == 15) {
                        checkHDand15fps();
                    }
                    else {
                        if(videoFrameRate == 7) {
                            checkHDand7fps();
                        }
                    }
                }
            }
            else {
                if (videoResolution.equals(640,480)){
                    if (videoFrameRate == 30) { //aprox
                        checkVGAand30fps();
                    }
                    else {
                        if(videoFrameRate == 15) {
                            checkVGAand15fps();
                        }
                        else {
                            if(videoFrameRate == 7) {
                                checkVGAand7fps();
                            }
                        }
                    }
                }
                else {
                    if (videoResolution.equals(320,240)){
                        if (videoFrameRate == 30) { //aprox
                            checkQVGAand30fps();
                        }
                        else {
                            if(videoFrameRate == 15) {
                                checkQVGAand15fps();
                            }
                            else {
                                if(videoFrameRate == 7) {
                                    checkQVGAand7fps();
                                }
                            }
                        }
                    }
                }
            }
        }

        else {
            //audio quality

        }

        Log.i(LOG_TAG, "MOS QUALITY: "+mQuality.toString());
        mStartTestTime = System.currentTimeMillis() / 1000;

        return mQuality;
    }

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

            Log.i(LOG_TAG, "RESOLUTION: " + mTestSubscriber.getStream().getVideoWidth() + "x" + mTestSubscriber.getStream().getVideoHeight());

            Log.i(LOG_TAG, "Video bandwidth (bps): " + mVideoBw + " Video Bytes received: " + stats.videoBytesReceived + " Video packet lost: " + stats.videoPacketsLost + " Video packet loss ratio: " + mVideoPLRatio);

        }
    }

    private void checkAudioStats(SubscriberKit.SubscriberAudioStats stats) {
        if (mPacketsReceivedAudio != 0) {
            long pl = stats.audioPacketsLost - mPacketsLostAudio;
            long pr = stats.audioPacketsReceived - mPacketsReceivedAudio;
            long pt = pl + pr;
            if (pt > 0) {
                double ratio = (double) pl / (double) pt;
                Log.d("QualityStatsSampleApp", "Packet loss ratio = " + ratio);
            }
        }
        mPacketsLostAudio = stats.audioPacketsLost;
        mPacketsReceivedAudio = stats.audioPacketsReceived;
    }

    private void getPCStats() {

        //getting PC stats from Google
        long[] videoStreams = OpenTokConfig.getSubscriberVideoStreams(mTestSubscriber);

        if (videoStreams != null && videoStreams.length > 0) {
            String frameRate = OpenTokConfig.getSubscriberStat(mTestSubscriber, videoStreams[0], "googFrameRateReceived");
            String videoWidth = OpenTokConfig.getSubscriberStat(mTestSubscriber, videoStreams[0], "googFrameWidthReceived");
            String videoHeight = OpenTokConfig.getSubscriberStat(mTestSubscriber, videoStreams[0], "googFrameHeightReceived");

            videoFrameRate = Double.valueOf(frameRate);
            videoResolution = new VideoUtils.Size(Integer.valueOf(videoWidth), Integer.valueOf(videoHeight));

            Log.i(LOG_TAG, "FRAMERATE: " + videoFrameRate);
            Log.i(LOG_TAG, "WIDTH: " + videoWidth);
            Log.i(LOG_TAG, "HEIGHT: " + videoHeight);
        }
    }

    private void checkQVGAand30fps(){
        if (mVideoBw <= 100000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 100000 && mVideoBw <= 120000 && mVideoPLRatio < 0.1) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 120000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 120000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 200000 && mVideoBw <= 300000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 200000 && mVideoBw <= 300000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 300000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 300000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkQVGAand15fps(){
        if (mVideoBw <= 100000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 100000 && mVideoBw <= 120000 && mVideoPLRatio < 0.1) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 120000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 120000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 200000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 200000 && mVideoBw <= 150000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 200000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 200000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void checkQVGAand7fps(){

    }

    private void checkVGAand30fps(){
        if (mVideoBw <= 120000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 120000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 150000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 250000 && mVideoBw <= 600000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 250000 && mVideoBw <= 600000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 600000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 600000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkVGAand15fps(){
        if (mVideoBw <= 75000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 75000 && mVideoBw <= 120000 && mVideoPLRatio < 0.1) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 120000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 120000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 200000 && mVideoBw <= 400000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 200000 && mVideoBw <= 400000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 400000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 400000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkVGAand7fps(){
        if (mVideoBw <= 50000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 50000 && mVideoBw <= 100000 && mVideoPLRatio < 0.1) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 100000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 100000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 150000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 150000 && mVideoBw <= 200000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 200000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 200000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkHDand30fps(){
        if (mVideoBw <= 250000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 250000 && mVideoBw <= 350000 && mVideoPLRatio < 0.1 ) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 350000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 350000 && mVideoBw <= 600000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 600000 && mVideoBw <= 1000000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 600000 && mVideoBw <= 1000000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 1000000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 1000000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void checkHDand15fps(){
        if (mVideoBw <= 150000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1 ) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 250000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 250000 && mVideoBw <= 350000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 350000 && mVideoBw <= 800000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 350000 && mVideoBw <= 800000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 800000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 800000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkHDand7fps(){
        if (mVideoBw <= 120000) {
            mQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 120000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1 ) {
                mQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 150000 || mVideoPLRatio > 0.1) {
                    mQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1) {
                        mQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 250000 && mVideoBw <= 400000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 250000 && mVideoBw <= 400000 && mVideoPLRatio < 0.02) {
                                mQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 400000 && mVideoPLRatio > 0.005) {
                                    mQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 400000 && mVideoPLRatio < 0.005) {
                                        mQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}