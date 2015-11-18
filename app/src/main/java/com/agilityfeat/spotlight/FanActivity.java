package com.agilityfeat.spotlight;


import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.agilityfeat.spotlight.config.SpotlightConfig;
import com.agilityfeat.spotlight.events.EventUtils;
import com.agilityfeat.spotlight.model.InstanceApp;
import com.agilityfeat.spotlight.video.CustomVideoRenderer;
import com.agilityfeat.spotlight.socket.SocketCoordinator;
import com.agilityfeat.spotlight.ws.WebServiceCoordinator;
import com.agilityfeat.spotlight.services.ClearNotificationService;


import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;


import org.json.JSONException;
import org.json.JSONObject;

import com.agilityfeat.spotlight.chat.ChatMessage;
import com.agilityfeat.spotlight.chat.TextChatFragment;



public class FanActivity extends AppCompatActivity implements WebServiceCoordinator.Listener,

        Session.SessionListener, Session.ConnectionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener,
        Session.SignalListener,Subscriber.VideoListener,
        TextChatFragment.TextChatListener{

    private static final String LOG_TAG = FanActivity.class.getSimpleName();
    private static final int TIME_WINDOW = 3; //3 seconds
    private static final int TIME_VIDEO_TEST = 10; //time interval to check the video quality in seconds

    //Test call vars
    private String mQuality = "";
    private double mVideoPLRatio = 0.0;
    private long mVideoBw = 0;
    private double mAudioPLRatio = 0.0;
    private long mAudioBw = 0;
    private long mPrevVideoPacketsLost = 0;
    private long mPrevVideoPacketsRcvd = 0;
    private double mPrevVideoTimestamp = 0;
    private long mPrevVideoBytes = 0;
    private long mPrevAudioPacketsLost = 0;
    private long mPrevAudioPacketsRcvd = 0;
    private double mPrevAudioTimestamp = 0;
    private long mPrevAudioBytes = 0;
    private long mStartTestTime = 0;
    private boolean audioOnly = false;
    private boolean mNewFanSignalAckd = false;
    private int mUnreadMessages = 0;


    private JSONObject mEvent;
    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private String mBackstageSessionId;
    private String mBackstageToken;
    private Session mSession;
    private Session mBackstageSession;
    private WebServiceCoordinator mWebServiceCoordinator;
    private SocketCoordinator mSocket;
    private Publisher mPublisher;
    private Subscriber mSubscriberHost;
    private Subscriber mSubscriberCelebrity;
    private Subscriber mSubscriberFan;
    private Subscriber mSubscriberProducer;
    private Subscriber mSelfSubscriber;
    private Stream mCelebirtyStream;
    private Stream mFanStream;
    private Stream mHostStream;
    private Stream mProducerStream;
    private Connection mProducerConnection;

    private TextView mEventName;
    private TextView mEventStatus;
    private TextView mUserStatus;
    private TextView mGoLiveStatus;
    private TextView mGoLiveNumber;
    private TextView mTextoUnreadMessages;
    private ImageButton mChatButton;
    private ImageView mEventImage;
    private ImageView mEventImageEnd;
    private Button mGetInLine;
    private Button mLiveButton;

    private Handler mHandler = new Handler();
    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberHostViewContainer;
    private RelativeLayout mSubscriberCelebrityViewContainer;
    private RelativeLayout mSubscriberFanViewContainer;
    private FrameLayout mFragmentContainer;


    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSubCelebrity;
    private ProgressBar mLoadingSubHost;
    private ProgressBar mLoadingSubPublisher;
    private ProgressBar mLoadingSubFan;
    private boolean resumeHasRun = false;
    private boolean mIsBound = false;
    private boolean mUserIsOnstage = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;
    private CustomVideoRenderer mCustomVideoRenderer;

    private TextChatFragment mTextChatFragment;
    private FragmentTransaction mFragmentTransaction;
    private boolean msgError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fan);

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mSocket = new SocketCoordinator();

        initLayoutWidgets();

        //Get the event
        requestEventData(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_celebrity_host, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initLayoutWidgets() {
        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherView);
        mSubscriberHostViewContainer = (RelativeLayout) findViewById(R.id.subscriberHostView);
        mSubscriberCelebrityViewContainer = (RelativeLayout) findViewById(R.id.subscriberCelebrityView);
        mSubscriberFanViewContainer = (RelativeLayout) findViewById(R.id.subscriberFanView);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_textchat_container);

        mLoadingSubCelebrity = (ProgressBar) findViewById(R.id.loadingSpinnerCelebrity);
        mLoadingSubHost = (ProgressBar) findViewById(R.id.loadingSpinnerHost);
        mLoadingSubFan = (ProgressBar) findViewById(R.id.loadingSpinnerFan);
        mLoadingSubPublisher = (ProgressBar) findViewById(R.id.loadingSpinnerPublisher);
        mEventName = (TextView) findViewById(R.id.event_name);
        mEventStatus = (TextView) findViewById(R.id.event_status);
        mUserStatus = (TextView) findViewById(R.id.user_status);
        mGoLiveStatus = (TextView) findViewById(R.id.go_live_status);
        mGoLiveNumber = (TextView) findViewById(R.id.go_live_number);
        mTextoUnreadMessages = (TextView) findViewById(R.id.unread_messages);
        mEventImageEnd = (ImageView) findViewById(R.id.event_image_end);
        mEventImage = (ImageView) findViewById(R.id.event_image);
        mChatButton = (ImageButton) findViewById(R.id.chat_button);
        mGetInLine = (Button) findViewById(R.id.btn_getinline);
        mLiveButton = (Button) findViewById(R.id.live_button);
    }

    private void requestEventData (Bundle savedInstanceState) {
        int event_index = 0;
        //Parse data from activity_join
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                event_index = Integer.parseInt(extras.getString("event_index"));
            } else {
                Log.i(LOG_TAG, "NO EXTRAS");
                //@TODO: Handle no extras
            }
        } else {
            event_index = Integer.parseInt((String) savedInstanceState.getSerializable("event_index"));
        }

        JSONObject event = InstanceApp.getInstance().getEventByIndex(event_index);
        try {
            updateEventName(event.getString("event_name"), EventUtils.getStatusNameById(event.getString("status")));
            EventUtils.loadEventImage(this, event.getString("event_image"), mEventImage);
            EventUtils.loadEventImage(this, event.getString("event_image_end"), mEventImageEnd);
            mWebServiceCoordinator.createFanToken(event.getString("fan_url"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - getInstanceById", e);
        }

    }


    /**
     * Web Service Coordinator delegate methods
     */
    @Override
    public void onDataReady(JSONObject results) {
        try {
            mEvent = results.getJSONObject("event");
            mApiKey = results.getString("apiKey");
            mToken = results.getString("tokenHost");
            mSessionId = results.getString("sessionIdHost");
            mBackstageToken = results.getString("tokenProducer");
            mBackstageSessionId = results.getString("sessionIdProducer");

            updateEventName();
            sessionConnect();

        } catch(JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
            //@TODO: Do something when this error happens
        }
    }

    @Override
    public void onWebServiceCoordinatorError(Exception error) {
        Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
        Toast.makeText(getApplicationContext(), "Unable to connect to the server. Please try in a few minutes.", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();

            if (mSubscriberHost != null) {
                mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
            }

            if (mSubscriberCelebrity != null) {
                mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
            }

            if (mSubscriberFan != null) {
                mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            }
        }

        if(mBackstageSession != null) {
            mBackstageSession.onPause();
        }

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setContentText(getResources().getString(R.string.notification));
        //.setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

        Intent notificationIntent = new Intent(this, FanActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        if (mConnection == null) {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    ((ClearNotificationService.ClearBinder) binder).service.startService(new Intent(FanActivity.this, ClearNotificationService.class));
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotificationManager.notify(ClearNotificationService.NOTIFICATION_ID, mNotifyBuilder.build());
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mConnection = null;
                }

            };
        }

        if (!mIsBound) {
            bindService(new Intent(FanActivity.this,
                            ClearNotificationService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;
            startService(notificationIntent);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (!resumeHasRun) {
            resumeHasRun = true;
            return;
        } else {
            if (mSession != null) {
                mSession.onResume();
            }

            if (mBackstageSession != null) {
                mBackstageSession.onResume();
            }
        }
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);

        reloadInterface();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        if (isFinishing()) {
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);

            mSocket.disconnect();

            if (mSession != null) {
                mSession.disconnect();
            }

            if (mBackstageSession != null) {
                mBackstageSession.disconnect();
            }

        }
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        if (mSession != null) {
            mSession.disconnect();
        }

        if (mBackstageSession != null) {
            mBackstageSession.disconnect();
        }

        super.onDestroy();
        finish();
    }


    @Override
    public void onBackPressed() {

        if(mFragmentContainer.getVisibility() == View.VISIBLE) {
            toggleChat();
        }else {
            mSocket.disconnect();
            if (mSession != null) {
                mSession.disconnect();
            }

            if (mBackstageSession != null) {
                mBackstageSession.disconnect();
            }

            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            if (mIsBound) {
                unbindService(mConnection);
                mIsBound = false;
            }

            super.onBackPressed();
        }
    }

    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriberHost != null) {
                    attachSubscriberHostView(mSubscriberHost);
                }
                if (mSubscriberCelebrity != null) {
                    attachSubscriberCelebrityView(mSubscriberCelebrity);
                }
                if (mSubscriberFan != null) {
                    attachSubscriberFanView(mSubscriberFan);
                }
            }
        }, 500);
    }

    public void updateViewsWidth() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                int streams = 0;
                if (mFanStream != null || mUserIsOnstage) streams++;
                if (mCelebirtyStream != null) streams++;
                if (mHostStream != null) streams++;

                if (streams > 0) {
                    Log.i(LOG_TAG, "more streams than 0");
                    mEventImage.setVisibility(View.GONE);

                    RelativeLayout.LayoutParams celebrity_head_params = (RelativeLayout.LayoutParams) mSubscriberCelebrityViewContainer.getLayoutParams();
                    celebrity_head_params.width = (mCelebirtyStream != null) ? screenWidth(FanActivity.this) / streams : 1;
                    mSubscriberCelebrityViewContainer.setLayoutParams(celebrity_head_params);

                    RelativeLayout.LayoutParams host_head_params = (RelativeLayout.LayoutParams) mSubscriberHostViewContainer.getLayoutParams();
                    host_head_params.width = (mHostStream != null) ? screenWidth(FanActivity.this) / streams : 1;
                    mSubscriberHostViewContainer.setLayoutParams(host_head_params);

                    RelativeLayout.LayoutParams fan_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                    fan_head_params.width = (mFanStream != null) ? screenWidth(FanActivity.this) / streams : 1;
                    mSubscriberFanViewContainer.setLayoutParams(fan_head_params);

                    if (mUserIsOnstage) {
                        //copy layoutparams from fan container
                        RelativeLayout.LayoutParams publisher_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                        publisher_head_params.width = screenWidth(FanActivity.this) / streams;
                        mPublisherViewContainer.setLayoutParams(publisher_head_params);
                    }


                } else {
                    mEventImage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(FanActivity.this,
                    mApiKey, mSessionId);
            mSession.setSessionListener(this);
            mSession.setSignalListener(this);
            mSession.setConnectionListener(this);
            mSession.connect(mToken);
        }
    }

    private void backstageSessionConnect() {
        if (mBackstageSession == null) {
            mBackstageSession = new Session(FanActivity.this,
                    mApiKey, mBackstageSessionId);
            mBackstageSession.setSessionListener(this);
            mBackstageSession.setSignalListener(this);
            mBackstageSession.setConnectionListener(this);
            mBackstageSession.connect(mBackstageToken);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Connected to the session");
        mGetInLine.setVisibility(View.VISIBLE);
        // stop loading spinning
        mLoadingSubPublisher.setVisibility(View.GONE);

        //Start publishing in backstage session
        if(session.getSessionId().equals(mBackstageSessionId)) {
            mPublisher.setAudioFallbackEnabled(false);
            mBackstageSession.publish(mPublisher);
            setUserStatus(R.string.status_inline);
            mGetInLine.setText(getResources().getString(R.string.leave_line));
            mGetInLine.setBackground(getResources().getDrawable(R.drawable.leave_line_button));
            mGetInLine.setVisibility(View.VISIBLE);

            //loading text-chat ui component
            loadTextChatFragment();

        }
    }

    private void setUserStatus(int status) {
        if(status != R.string.status_onstage) {
            mUserStatus.setText(getResources().getString(status));
            mUserStatus.setVisibility(View.VISIBLE);
            AlphaAnimation animation1 = new AlphaAnimation(0f, 0.8f);
            animation1.setDuration(1000);
            animation1.setFillAfter(true);
            mUserStatus.startAnimation(animation1);


            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlphaAnimation animation1 = new AlphaAnimation(0.8f, 0f);
                    animation1.setDuration(1000);
                    animation1.setFillAfter(true);
                    mUserStatus.startAnimation(animation1);

                }
            }, 3000);
        } else {
            //Going live on 3..2..1
            mGoLiveStatus.setVisibility(View.VISIBLE);
            mGoLiveNumber.setVisibility(View.VISIBLE);
            startCountDown(6);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    AlphaAnimation animation1 = new AlphaAnimation(0.8f, 0f);
                    animation1.setDuration(500);
                    animation1.setFillAfter(true);
                    AlphaAnimation animation2 = new AlphaAnimation(0.8f, 0f);
                    animation2.setDuration(500);
                    animation2.setFillAfter(true);
                    mGoLiveStatus.startAnimation(animation1);
                    mGoLiveNumber.startAnimation(animation2);
                    publishOnStage();
                }
            }, 5000);
        }
    }

    private void startCountDown(final int number) {

        mGoLiveNumber.setText(String.valueOf(number - 1));
        if((number-1)>1) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCountDown(number - 1);
                }
            }, 1000);
        }

    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Disconnected from the session.");
        if(session.getSessionId().equals(mSessionId)) {
            cleanViews();
        } else {
            //TODO: Hide Get Inline button on forceDisconnect event
            leaveLine();
        }
    }

    private void leaveLine() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mLoadingSubPublisher.setVisibility(View.GONE);
                if (mPublisher != null) {
                    mPublisherViewContainer.removeView(mPublisher.getView());
                    mPublisher = null;
                }
                if (mBackstageSession != null) {
                    mBackstageSession.unpublish(mPublisher);
                    mBackstageSession.disconnect();
                    mBackstageSession = null;
                }
                //Hide chat stuff
                hideChat();
                mGetInLine.setText(getResources().getString(R.string.get_inline));
                mGetInLine.setBackground(getResources().getDrawable(R.drawable.get_in_line_button));
                mNewFanSignalAckd = false;
            }
        }, 100);

    }

    public void cleanViews() {
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriberFan != null) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        }

        if (mSubscriberHost != null) {
            mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
        }

        if (mSubscriberCelebrity != null) {
            mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
        }
    }

    private void subscribeHostToStream(Stream stream) {

        mSubscriberHost = new Subscriber(FanActivity.this, stream);
        mSubscriberHost.setVideoListener(this);
        mSession.subscribe(mSubscriberHost);

        if (stream.hasVideo()) {
            // start loading spinning
            mLoadingSubHost.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeCelebrityToStream(Stream stream) {

        mSubscriberCelebrity = new Subscriber(FanActivity.this, stream);
        mSubscriberCelebrity.setVideoListener(this);
        mSession.subscribe(mSubscriberCelebrity);

        if (stream.hasVideo()) {
            // start loading spinning
            mLoadingSubCelebrity.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeFanToStream(Stream stream) {

        mSubscriberFan = new Subscriber(FanActivity.this, stream);
        mSubscriberFan.setVideoListener(this);
        mSession.subscribe(mSubscriberFan);

        if (stream.hasVideo()) {
            // start loading spinning
            mLoadingSubFan.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeProducer() {
        if(mProducerStream != null) {
            mSubscriberProducer = new Subscriber(FanActivity.this, mProducerStream);
            mBackstageSession.subscribe(mSubscriberProducer);
            setUserStatus(R.string.status_incall);
        }
    }

    private void unSubscribeProducer() {
        if (mProducerStream!= null && mSubscriberProducer != null) {
            mBackstageSession.unsubscribe(mSubscriberProducer);
            mSubscriberProducer = null;
            setUserStatus(R.string.status_inline);
        }
    }

    private void unsubscribeHostFromStream(Stream stream) {
        if (mSubscriberHost.getStream().equals(stream)) {
            mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
            mSession.unsubscribe(mSubscriberHost);
            mSubscriberHost = null;
            mLoadingSubHost.setVisibility(View.GONE);
        }
    }

    private void unsubscribeCelebrityFromStream(Stream stream) {
        if (mSubscriberCelebrity.getStream().equals(stream)) {
            mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
            mSession.unsubscribe(mSubscriberCelebrity);
            mSubscriberCelebrity = null;
            mLoadingSubCelebrity.setVisibility(View.GONE);
        }
    }

    private void unsubscribeFanFromStream(Stream stream) {
        if (mSubscriberFan.getStream().equals(stream)) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            mSession.unsubscribe(mSubscriberFan);
            mSubscriberFan = null;
            mLoadingSubFan.setVisibility(View.GONE);
        }
    }

    private void attachSubscriberHostView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
        ((GLSurfaceView)mSubscriberHost.getView()).setZOrderMediaOverlay(false);
        mSubscriberHostViewContainer.addView(mSubscriberHost.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachSubscriberCelebrityView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
        ((GLSurfaceView)mSubscriberCelebrity.getView()).setZOrderMediaOverlay(false);
        mSubscriberCelebrityViewContainer.addView(mSubscriberCelebrity.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachSubscriberFanView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        ((GLSurfaceView)mSubscriberFan.getView()).setZOrderMediaOverlay(false);
        mSubscriberFanViewContainer.addView(mSubscriberFan.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachPublisherView(Publisher publisher) {

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisherViewContainer.addView(mPublisher.getView(), layoutParams);
        publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        Log.i(LOG_TAG, "Session exception: " + exception.getMessage());
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        String status = getEventStatus();
        Log.i(LOG_TAG, "onStreamReceived/" + status);
        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if (mFanStream == null && session.getSessionId().equals(mSessionId)) {
                    mFanStream = stream;
                    if(status.equals("L")) {
                        subscribeFanToStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=celebrity":
                if (mCelebirtyStream == null) {
                    mCelebirtyStream = stream;
                    if(status.equals("L") || mUserIsOnstage) {
                        subscribeCelebrityToStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=host":
                if (mHostStream == null) {
                    mHostStream = stream;
                    if(status.equals("L") || mUserIsOnstage) {

                        subscribeHostToStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=producer":
                if(mProducerStream == null){
                    Log.i(LOG_TAG, "producer stream in");
                    mProducerStream = stream;
                }
                break;
        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        String status = getEventStatus();

        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if(mFanStream != null && session.getSessionId().equals(mSessionId) && mFanStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    mFanStream = null;
                    if(status.equals("L") || status.equals("C")) {
                        unsubscribeFanFromStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=celebrity":
                if(mCelebirtyStream != null && mCelebirtyStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {

                    mCelebirtyStream = null;
                    if(status.equals("L") || status.equals("C")) {
                        unsubscribeCelebrityFromStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=host":
                Log.i(LOG_TAG, "drop host");
                if(mHostStream != null && mHostStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    Log.i(LOG_TAG, "drop host ok ");
                    mHostStream = null;
                    if(status.equals("L") || status.equals("C")) {
                        unsubscribeHostFromStream(stream);
                        updateViewsWidth();
                    }
                }
            case "usertype=producer":
                if(mProducerStream != null && mProducerStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    mProducerStream = null;
                    Log.i(LOG_TAG, "producer stream out");
                }
                break;
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {
        Log.i(LOG_TAG, "mSelfSubscriber=" + String.valueOf(mSelfSubscriber == null));
        Log.i(LOG_TAG, "mQuality=" + String.valueOf(mQuality.equals("")));
        if (mQuality.equals("")) {
            subscribeToSelfStream(stream);
        }
    }

    private void subscribeToSelfStream(Stream stream) {
        mSelfSubscriber = new Subscriber(FanActivity.this, stream);

        mSelfSubscriber.setSubscriberListener(this);
        mSelfSubscriber.setSubscribeToAudio(false);
        mBackstageSession.subscribe(mSelfSubscriber);

        mSelfSubscriber.setVideoStatsListener(new SubscriberKit.VideoStatsListener() {

            @Override
            public void onVideoStats(SubscriberKit subscriber,
                                     SubscriberKit.SubscriberVideoStats stats) {

                if (mStartTestTime == 0) {
                    mStartTestTime = System.currentTimeMillis() / 1000;
                }
                checkVideoStats(stats);

                //check quality of the video call after TIME_VIDEO_TEST seconds
                if (((System.currentTimeMillis() / 1000 - mStartTestTime) > TIME_VIDEO_TEST) && !audioOnly) {
                    checkVideoQuality();
                }
            }

        });

        mSelfSubscriber.setAudioStatsListener(new SubscriberKit.AudioStatsListener() {
            @Override
            public void onAudioStats(SubscriberKit subscriber, SubscriberKit.SubscriberAudioStats stats) {

                checkAudioStats(stats);

            }
        });
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

    private void checkVideoQuality() {
        if (mBackstageSession != null) {
            Log.i(LOG_TAG, "Check video quality stats data");
            if (mVideoBw < 150000 || mVideoPLRatio > 0.03) {
                /*mPublisher.setPublishVideo(false);
                mSelfSubscriber.setSubscribeToVideo(false);
                mSelfSubscriber.setVideoStatsListener(null);
                audioOnly = true;*/
                mQuality = "Poor";
            } else if (mVideoBw > 350 * 1000) {
                mQuality = "Great";
            } else {
                mQuality = "Good";
            }
            //mSelfSubscriber.setVideoStatsListener(null);
            Log.i(LOG_TAG, "Publisher quality is " + mQuality);
            mBackstageSession.unsubscribe(mSelfSubscriber);
            mSelfSubscriber = null;
        }
    }


    private static int screenWidth(Context ctx) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
        //mPublisher = null;
        Log.i(LOG_TAG, "Publisher destroyed");
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOG_TAG, "Publisher exception: " + exception.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "First frame received");
        Log.i(LOG_TAG, "onVideoDataReceived " + subscriber.getStream().getConnection().getData());
        if(subscriber.getStream().getConnection().getData().equals("usertype=fan")) {
            // stop loading spinning
            mLoadingSubFan.setVisibility(View.GONE);
            attachSubscriberFanView(mSubscriberFan);
        } else if(subscriber.getStream().getConnection().getData().equals("usertype=host")) {
            // stop loading spinning
            mLoadingSubHost.setVisibility(View.GONE);
            attachSubscriberHostView(mSubscriberHost);
        } else if(subscriber.getStream().getConnection().getData().equals("usertype=celebrity")) {
            // stop loading spinning
            mLoadingSubCelebrity.setVisibility(View.GONE);
            attachSubscriberCelebrityView(mSubscriberCelebrity);
        }

    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG,
                "Video disabled:" + reason);
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG, "Video enabled:" + reason);
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "Video may be disabled soon due to network quality degradation. Add UI handling here.");
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "Video may no longer be disabled as stream quality improved. Add UI handling here.");
    }

    /* Subscriber Listener methods */

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        //Log.i(LOG_TAG, "Subscriber Connected");
        if(subscriberKit.getStream().getConnection().getData() == "usertype=fan") {
            mSubscriberFanViewContainer.addView(mSubscriberFan.getView());
        } else if(subscriberKit.getStream().getConnection().getData() == "usertype=host") {
            mSubscriberHostViewContainer.addView(mSubscriberHost.getView());
        } else if(subscriberKit.getStream().getConnection().getData() == "usertype=celebrity") {
            mSubscriberCelebrityViewContainer.addView(mSubscriberCelebrity.getView());
        }
    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber Disconnected");
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        Log.e(LOG_TAG, opentokError.getMessage());
    }

    /* Signal Listener methods */
    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {


        if(type != null) {
            switch(type) {
                case "chatMessage":
                    handleNewMessage(data, connection);
                    break;
                case "videoOnOff":
                    videoOnOff(data);
                    break;
                case "muteAudio":
                    muteAudio(data);
                    break;
                case "startEvent":
                    startEvent();
                    break;
                case "goLive":
                    goLive();
                    break;
                case "finishEvent":
                    finishEvent();
                    break;
                //backstage
                case "resendNewFanSignal":
                    if (!mNewFanSignalAckd) sendNewFanSignal();
                    break;
                case "joinProducer":
                    subscribeProducer();
                    break;
                case "disconnectProducer":
                    unSubscribeProducer();
                    break;
                case "joinHost":
                    connectWithOnstage();
                    break;
                case "closeChat":
                    hideChat();
                    mChatButton.setVisibility(View.GONE);
                    break;

                case "disconnect":
                    disconnectFromOnstage();
                    break;
                case "joinBackstage":
                    joinBackstage();
                    break;
                case "disconnectBackstage":
                    disconnectBackstage();
                    break;
                case "newFanAck":
                    ackNewFanSignal();
                    break;
                case "producerLeaving":
                    mNewFanSignalAckd = false;
                    break;
            }
        }
        //TODO: onChangeVolumen


    }

    private void ackNewFanSignal() {
        mNewFanSignalAckd = true;

        String connectionId = mPublisher.getStream().getConnection().getConnectionId();
        String sessionId = mBackstageSessionId;
        String snapshot = mCustomVideoRenderer.getSnapshot();

        Log.i(LOG_TAG, "sending snapshot : " + snapshot);
        JSONObject obj = new JSONObject();
        try {
            obj.put("connectionId", connectionId);
            obj.put("sessionId", sessionId);
            obj.put("snapshot", snapshot);
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        mSocket.SendSnapShot(obj);
    }

    private void joinBackstage() {
        setUserStatus(R.string.status_backstage);
    }

    private void disconnectBackstage() {
        setUserStatus(R.string.status_inline);
    }

    private void connectWithOnstage() {
        //Hidding leave line button
        mGetInLine.setVisibility(View.GONE);
        setUserStatus(R.string.status_onstage);
    }

    private void publishOnStage(){
        mLiveButton.setVisibility(View.VISIBLE);
        mUserIsOnstage = true;
        mBackstageSession.unpublish(mPublisher);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mSession.publish(mPublisher);
                if (mHostStream != null) subscribeHostToStream(mHostStream);
                if (mCelebirtyStream != null) subscribeCelebrityToStream(mCelebirtyStream);
                updateViewsWidth();
            }
        }, 500);
    }

    private void disconnectFromOnstage() {
        mUserIsOnstage = false;
        //Hide Get in line button
        mGetInLine.setVisibility(View.GONE);

        //Unpublish
        mSession.unpublish(mPublisher);

        //Hide chat
        hideChat();
        mLoadingSubPublisher.setVisibility(View.GONE);

        //Disconnect from backstage
        if(mBackstageSession!=null) mBackstageSession.disconnect();

        //Remove publisher
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.destroy();

        updateViewsWidth();
        mLiveButton.setVisibility(View.GONE);
        Toast.makeText(getApplicationContext(), "Thank you for participating, you are no longer sharing video/voice. You can continue to watch the session at your leisure.", Toast.LENGTH_LONG).show();

    }



    private void handleNewMessage(String data, Connection connection) {
        String text = "";
        try {
            text = new JSONObject(data)
                    .getJSONObject("message")
                    .getString("message");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }

        ChatMessage msg = null;
        msg = new ChatMessage(connection.getConnectionId(), "Producer", text);
        // Add the new ChatMessage to the text-chat component
        mTextChatFragment.addMessage(msg);
        if(mFragmentContainer.getVisibility() != View.VISIBLE) {
            mUnreadMessages++;
            refreshUnreadMessages();
            mChatButton.setVisibility(View.VISIBLE);
        }



    }

    private void refreshUnreadMessages() {
        if(mUnreadMessages > 0) {
            mTextoUnreadMessages.setVisibility(View.VISIBLE);
        } else {
            mTextoUnreadMessages.setVisibility(View.GONE);
        }
        mTextoUnreadMessages.setText(Integer.toString(mUnreadMessages));
    }

    private void videoOnOff(String data){
        String video="";
        try {
            video = new JSONObject(data)
                    .getString("video");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }
        mPublisher.setPublishVideo(video.equals("on"));
    }

    private void muteAudio(String data){
        String mute="";
        try {
            mute = new JSONObject(data)
                    .getString("mute");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }
        mPublisher.setPublishAudio(!mute.equals("on"));
    }

    public void startEvent(){
        mNewFanSignalAckd = false;
    }

    public void goLive(){
        try {
            mEvent.put("status", "L");
            updateEventName();
            if(!mUserIsOnstage) {
                if (mFanStream != null) subscribeFanToStream(mFanStream);
                if (mHostStream != null) subscribeHostToStream(mHostStream);
                if (mCelebirtyStream != null) subscribeCelebrityToStream(mCelebirtyStream);
                updateViewsWidth();
            }
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    private void updateEventName() {
        try {
            mEventName.setText(mEvent.getString("event_name"));
            mEventStatus.setText(getEventStatusName().toUpperCase());
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    private void updateEventName(String event_name, String status) {
        mEventName.setText(event_name);
        mEventStatus.setText(status.toUpperCase());
    }

    private String getEventStatusName() {
        return EventUtils.getStatusNameById(getEventStatus());
    }

    private String getEventStatus() {
        String status = "N";
        try {
            status = mEvent.getString("status");
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        return status;
    }



    private void finishEvent() {

        //Show Event Image end
        mEventImage.setVisibility(View.GONE);
        mEventImageEnd.setVisibility(View.VISIBLE);

        //Hide subscriber containters
        mSubscriberCelebrityViewContainer.setVisibility(View.GONE);
        mSubscriberFanViewContainer.setVisibility(View.GONE);
        mSubscriberHostViewContainer.setVisibility(View.GONE);

        //Hide chat
        mChatButton.setVisibility(View.GONE);
        mFragmentContainer.setVisibility(View.GONE);

        //Hide getinline
        mGetInLine.setVisibility(View.GONE);

        if(mUserIsOnstage) {
            disconnectFromOnstage();
        } else {
            //Disconnect the onbackstage session
            if (mBackstageSession != null) mBackstageSession.disconnect();
        }

        //Disconnect from onstage session
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSession != null) mSession.disconnect();
            }
        }, 10000);

        try {
            //Change status
            mEvent.put("status", "C");
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        //Update event name and Status.
        updateEventName();
    }


    /* Connection Listener methods */
    @Override
    public void onConnectionCreated(Session session, Connection connection) {
        if(connection.getData().equals("usertype=producer")) {
            mProducerConnection = connection;
        }
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection)
    {
        if(connection.getData().equals("usertype=producer")) {
            mProducerConnection = null;

        }
    }

    /* Chat methods */
    public void onChatButtonClicked(View v) {
        toggleChat();
    }

    private void toggleChat() {
        if(mFragmentContainer.getVisibility() == View.VISIBLE) {
            hideChat();
        } else {
            mFragmentContainer.setVisibility(View.VISIBLE);
            mUnreadMessages = 0;
            refreshUnreadMessages();
        }
    }

    public void onGetInLineClicked(View v) {
        if(mGetInLine.getVisibility() == View.GONE) return;
        if(mGetInLine.getText().equals(getResources().getString(R.string.get_inline))){
            initGetInline();
        } else {
            leaveLine();

        }
    }

    public void initGetInline() {
        mGetInLine.setVisibility(View.GONE);
        mPublisherViewContainer.setVisibility(View.VISIBLE);
        mLoadingSubPublisher.setVisibility(View.VISIBLE);

        mSocket.connect();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSocket.emitJoinRoom(mBackstageSessionId);
            }
        }, 2000);

        if (mPublisher == null) {

            Log.i(LOG_TAG, "init publisher");
            mPublisher = new Publisher(FanActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            // use an external custom video renderer
            mCustomVideoRenderer = new CustomVideoRenderer(this);
            mPublisher.setRenderer(mCustomVideoRenderer);
            attachPublisherView(mPublisher);
        }


        backstageSessionConnect();
    }

    private void sendNewFanSignal() {

            if(mProducerConnection != null && mBackstageSession != null){
                if(!mQuality.equals("") && !mNewFanSignalAckd) {
                    mNewFanSignalAckd = true;
                    String userName = SpotlightConfig.USER_NAME;
                    String msg = "{\"user\":{\"username\":\"" + userName + "\", \"quality\":\"" + mQuality + "\"}}";
                    mBackstageSession.sendSignal("newFan", msg, mProducerConnection);
                } else {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendNewFanSignal();
                        }
                    }, 500);
                }
            }
    }

    // Initialize a TextChatFragment instance and add it to the UI
    private void loadTextChatFragment(){
        int containerId = R.id.fragment_textchat_container;
        mFragmentTransaction = getFragmentManager().beginTransaction();
        mTextChatFragment = (TextChatFragment)this.getFragmentManager().findFragmentByTag("TextChatFragment");
        if (mTextChatFragment == null) {
            mTextChatFragment = new TextChatFragment();
            mTextChatFragment.setMaxTextLength(1050);
            mTextChatFragment.setTextChatListener(this);
            mTextChatFragment.setSenderInfo(mBackstageSession.getConnection().getConnectionId(), SpotlightConfig.USER_NAME);

            mFragmentTransaction.add(containerId, mTextChatFragment, "TextChatFragment").commit();
            mFragmentContainer.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onMessageReadyToSend(ChatMessage msg) {
        Log.d(LOG_TAG, "TextChat listener: onMessageReadyToSend: " + msg.getText());

        if (mBackstageSession != null && mProducerConnection != null) {
            String message = "{\"message\":{\"to\":{\"connectionId\":\"" + mProducerConnection.getConnectionId()+"\"}, \"message\":\""+msg.getText()+"\"}}";
            mBackstageSession.sendSignal("chatMessage", message, mProducerConnection);
        }
        return msgError;
    }

    @Override
    public void hideChat() {
        mFragmentContainer.setVisibility(View.GONE);
        if(mBackstageSession == null) {
            mChatButton.setVisibility(View.GONE);
        }
    }
}

