package com.tokbox.android.IB.network;

import android.print.PrintAttributes;
import android.util.Log;
import android.widget.Toast;

import com.opentok.android.OpenTokConfig;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.VideoUtils;

public class NetworkTest {
    private static final String LOG_TAG = NetworkTest.class.getName();

    private static final int TIME_WINDOW = 3; //3 seconds
    public static final int TIME_VIDEO_TEST = 15; //time interval to check the video quality in seconds

    private MOSQuality mVideoQuality = MOSQuality.Good;
    private double mVideoPLRatio = 0.0;
    private long mVideoBw = 0;
    private long mPrevVideoPacketsLost = 0;
    private long mPrevVideoPacketsRcvd = 0;
    private double mPrevVideoTimestamp = 0;
    private long mPrevVideoBytes = 0;
    private long mStartTestTime = 0;

    private long mPrevAudioPacketsLost = 0;
    private long mPrevAudioPacketsRcvd = 0;
    private double mPrevAudioTimestamp = 0;
    private long mPrevAudioBytes = 0;


    private double mAudioPLRatio = 0.0;
    private long mAudioBw = 0;
    private MOSQuality mAudioQuality = MOSQuality.Good;

    private SubscriberKit mTestSubscriber;
    private NetworkTestListener mListener;

    private VideoUtils.Size videoResolution;
    private double videoFrameRate = 0.0;

    private boolean mAudioOnly = false;

    public enum MOSQuality {

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

    public interface NetworkTestListener {
        void onVideoQualityUpdated(String connectionId, MOSQuality quality);
        void onAudioQualityUpdated(String connectionId, MOSQuality quality);
    }

    public void startNetworkTest(SubscriberKit subscriber){
        mTestSubscriber = subscriber;
        Log.i(LOG_TAG, "Start network test "+mTestSubscriber.getStream().getConnection().getConnectionId());

        if(subscriber.getSubscribeToVideo()) {
            startVideoNetworkTest();
        }
        else {
            mAudioOnly = true;
            startAudioNetworkTest();
        }
    }

    public void stopNetworkTest(){
        Log.i(LOG_TAG, "Stop network test");
        mVideoQuality = MOSQuality.Good;
        mVideoPLRatio = 0.0;
        mVideoBw = 0;
        mPrevVideoPacketsLost = 0;
        mPrevVideoPacketsRcvd = 0;
        mPrevVideoTimestamp = 0;
        mPrevVideoBytes = 0;
        mStartTestTime = 0;

        mAudioQuality = MOSQuality.Good;

        mAudioPLRatio = 0.0;
        mAudioBw = 0;
        mPrevAudioPacketsLost = 0;
        mPrevAudioPacketsRcvd = 0;
        mAudioOnly = false;

        /*TODO: review at platform level:
        mTestSubscriber.setVideoStatsListener(null);
        mTestSubscriber.setAudioStatsListener(null);
        mTestSubscriber = null;*/
    }

    public void setNetworkTestListener (NetworkTestListener listener){
      mListener = listener;
    }

    public void updateTest(boolean video){
        if (video) {
            mAudioOnly = false;
            startVideoNetworkTest();
        } else {
            mAudioOnly = true;
            startAudioNetworkTest();
        }
    }

    private void startVideoNetworkTest(){
        Log.i(LOG_TAG, "Start video network test");
        mTestSubscriber.setAudioStatsListener(null);
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
    }

    private void startAudioNetworkTest() {
        Log.i(LOG_TAG, "Start audio network test");
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
            if (videoResolution != null && videoResolution.equals(1280,720)){
                if (videoFrameRate <= 30 && videoFrameRate >=23) {
                    checkHDand30fps();
                }
                else {
                    if(videoFrameRate <= 15 && videoFrameRate >=11) {
                        checkHDand15fps();
                    }
                    else {
                        if(videoFrameRate <= 7 && videoFrameRate >=4) {
                            checkHDand7fps();
                        }
                    }
                }
            }
            else {
                if (videoResolution != null && videoResolution.equals(640,480)){
                    if (videoFrameRate <= 30 && videoFrameRate >=23) {
                        checkVGAand30fps();
                    }
                    else {
                        if(videoFrameRate <= 15 && videoFrameRate >=11) {
                            checkVGAand15fps();
                        }
                        else {
                            if(videoFrameRate <= 7 && videoFrameRate >=4) {
                                checkVGAand7fps();
                            }
                        }
                    }
                }
                else {
                    if (videoResolution != null && videoResolution.equals(320,240)){
                        if (videoFrameRate <= 30 && videoFrameRate >=23) {
                            checkQVGAand30fps();
                        }
                        else {
                            if(videoFrameRate <= 15 && videoFrameRate >=11) {
                                checkQVGAand15fps();
                            }
                            else {
                                if(videoFrameRate <= 7 && videoFrameRate >=4) {
                                    checkQVGAand7fps();
                                }
                            }
                        }
                    }
                }
            }
            Log.i(LOG_TAG, "Video quality: "+mVideoQuality);
        }
        else {
            //audio quality
            checkAudioQuality();
            Log.i(LOG_TAG, "Audio quality: "+mAudioQuality);
        }

        //restart time
        mStartTestTime = System.currentTimeMillis() / 1000;

        return mVideoQuality;
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

            Log.i(LOG_TAG, "Video bandwidth (bps): " + mVideoBw + " Video Bytes received: " + stats.videoBytesReceived + " Video packet lost: " + stats.videoPacketsLost + " Video packet loss ratio: " + mVideoPLRatio);

        }
    }

    private void checkAudioStats(SubscriberKit.SubscriberAudioStats stats) {
        Log.i(LOG_TAG, "Check audio stats");

        double audioTimestamp = stats.timeStamp / 1000;

        //initialize values
        if (mPrevAudioTimestamp == 0) {
            mPrevAudioTimestamp = audioTimestamp;
            mPrevAudioBytes = stats.audioBytesReceived;
        }

        if (audioTimestamp - mPrevAudioTimestamp >= TIME_WINDOW) {
            //calculate audio packets lost ratio
            if (mPrevAudioPacketsRcvd != 0) {
                long pl = stats.audioPacketsLost - mPrevAudioPacketsLost;
                long pr = stats.audioPacketsReceived - mPrevAudioPacketsRcvd;
                long pt = pl + pr;

                if (pt > 0) {
                    mAudioPLRatio = (double) pl / (double) pt;
                }
            }
            mPrevAudioPacketsLost = stats.audioPacketsLost;
            mPrevAudioPacketsRcvd = stats.audioPacketsReceived;

            //calculate audio bandwidth
            mAudioBw = (long) ((8 * (stats.audioBytesReceived - mPrevAudioBytes)) / (audioTimestamp - mPrevAudioTimestamp));

            mPrevAudioTimestamp = audioTimestamp;
            mPrevAudioBytes = stats.audioBytesReceived;

            Log.i(LOG_TAG, "Audio bandwidth (bps): " + mAudioBw + " Audio Bytes received: " + stats.audioBytesReceived + " Audio packet lost: " + stats.audioPacketsLost + " Audio packet loss ratio: " + mAudioPLRatio);

        }

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
        Log.i(LOG_TAG, "Check QVGA and 30fps");

        if (mVideoBw <= 100000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 100000 && mVideoBw <= 120000 && mVideoPLRatio < 0.1) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 120000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 120000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 200000 && mVideoBw <= 300000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 200000 && mVideoBw <= 300000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 300000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 300000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check QVGA and 15fps");

        if (mVideoBw <= 100000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 100000 && mVideoBw <= 120000 && mVideoPLRatio < 0.1) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 120000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 120000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 200000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 200000 && mVideoBw <= 150000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 200000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 200000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check QVGA and 7fps");

        if (mVideoBw <= 50000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 50000 && mVideoBw <= 75000 && mVideoPLRatio < 0.1) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 75000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 75000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 150000 && mVideoBw <= 100000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 150000 && mVideoBw <= 100000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 150000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 150000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkVGAand30fps(){
        Log.i(LOG_TAG, "Check VGA and 30fps");

        if (mVideoBw <= 120000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 120000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 150000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 250000 && mVideoBw <= 600000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 250000 && mVideoBw <= 600000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 600000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 600000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check VGA and 15fps");
        if (mVideoBw <= 75000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 75000 && mVideoBw <= 120000 && mVideoPLRatio < 0.1) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 120000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 120000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 200000 && mVideoBw <= 400000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 200000 && mVideoBw <= 400000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 400000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 400000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check VGA and 7fps");
        if (mVideoBw <= 50000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 50000 && mVideoBw <= 100000 && mVideoPLRatio < 0.1) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 100000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 100000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 150000 && mVideoBw <= 200000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 150000 && mVideoBw <= 200000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 200000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 200000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check HD and 30fps");
        if (mVideoBw <= 250000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 250000 && mVideoBw <= 350000 && mVideoPLRatio < 0.1 ) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 350000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 350000 && mVideoBw <= 600000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 600000 && mVideoBw <= 1000000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 600000 && mVideoBw <= 1000000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 1000000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 1000000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check HD and 15fps");

        if (mVideoBw <= 150000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1 ) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 250000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 250000 && mVideoBw <= 350000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 350000 && mVideoBw <= 800000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 350000 && mVideoBw <= 800000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 800000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 800000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
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
        Log.i(LOG_TAG, "Check HD and 7fps");
        if (mVideoBw <= 120000) {
            mVideoQuality = MOSQuality.Bad;
        }
        else {
            if (mVideoBw > 120000 && mVideoBw <= 150000 && mVideoPLRatio < 0.1 ) {
                mVideoQuality = MOSQuality.Poor;
            }
            else {
                if (mVideoBw <= 150000 || mVideoPLRatio > 0.1) {
                    mVideoQuality = MOSQuality.Poor;
                } else {
                    if (mVideoBw > 150000 && mVideoBw <= 250000 && mVideoPLRatio < 0.1) {
                        mVideoQuality = MOSQuality.Fair;
                    } else {
                        if (mVideoBw > 250000 && mVideoBw <= 400000 && mVideoPLRatio < 0.1 && mVideoPLRatio > 0.02) {
                            mVideoQuality = MOSQuality.Fair;
                        } else {
                            if (mVideoBw > 250000 && mVideoBw <= 400000 && mVideoPLRatio < 0.02) {
                                mVideoQuality = MOSQuality.Good;
                            } else {
                                if (mVideoBw > 400000 && mVideoPLRatio > 0.005) {
                                    mVideoQuality = MOSQuality.Good;
                                } else {
                                    if (mVideoBw > 400000 && mVideoPLRatio < 0.005) {
                                        mVideoQuality = MOSQuality.Excellent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkAudioQuality(){
        if (mAudioBw <= 25000 && mVideoPLRatio > 0.05) {
            mAudioQuality = MOSQuality.Bad;
        }
        else {
            if(mAudioBw > 25000 && mAudioBw <= 30000 && mVideoPLRatio < 0.05) {
                mAudioQuality = MOSQuality.Good;
            }
            else {
                if(mAudioBw > 30000 && mAudioBw < 0.005) {
                    mAudioQuality = MOSQuality.Excellent;
                }
            }
        }
    }
}