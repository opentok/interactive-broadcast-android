package com.tokbox.android.IB;


import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpenTokConfig;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.tokbox.android.IB.chat.ChatMessage;
import com.tokbox.android.IB.chat.TextChatFragment;
import com.tokbox.android.IB.config.IBConfig;
import com.tokbox.android.IB.events.EventUtils;
import com.tokbox.android.IB.model.InstanceApp;
import com.tokbox.android.IB.network.NetworkTest;
import com.tokbox.android.IB.services.ClearNotificationService;
import com.tokbox.android.IB.socket.SocketCoordinator;
import com.tokbox.android.IB.video.CustomVideoRenderer;
import com.tokbox.android.IB.ws.WebServiceCoordinator;
import com.tokbox.android.IB.common.Notification;

import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import com.tokbox.android.IB.logging.OTKAction;
import com.tokbox.android.IB.logging.OTKVariation;

import com.tokbox.android.IB.ui.CustomViewSubscriber;

import java.util.UUID;

public class FanActivity extends AppCompatActivity implements WebServiceCoordinator.Listener,

        Session.SessionListener, Session.ConnectionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener,
        Session.SignalListener,Subscriber.VideoListener,
        TextChatFragment.TextChatListener, NetworkTest.NetworkTestListener{

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    private final int permsRequestCode = 200;


    private static final String LOG_TAG = FanActivity.class.getSimpleName();

    private boolean mNewFanSignalAckd = false;
    private int mUnreadMessages = 0;
    private boolean mTestingOnStage = false;
    private boolean mOnstageMuted = false;
    private boolean mConnectionError = false;
    private boolean mSubscribingError = false;
    private boolean mInitializated = false;
    private boolean mHls = false;
    private boolean mOnBackstage = false;

    private JSONObject mEvent;
    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private String mBackstageSessionId;
    private String mBackstageToken;
    private String mBackstageConnectionId;
    private String mBroadcastUrl;
    private Session mSession;
    private Session mBackstageSession;

    private NetworkTest mTest;
    private String mTestQuality = "";

    private WebServiceCoordinator mWebServiceCoordinator;
    private Notification mNotification;
    private SocketCoordinator mSocket;
    private Publisher mPublisher;
    private Subscriber mSubscriberHost;
    private Subscriber mSubscriberCelebrity;
    private Subscriber mSubscriberFan;
    private Subscriber mSubscriberProducer;
    private Subscriber mSubscriberProducerOnstage;
    private Subscriber mTestSubscriber;
    private Stream mCelebrityStream;
    private Stream mFanStream;
    private Stream mHostStream;
    private Stream mProducerStream;
    private Stream mProducerStreamOnstage;
    private Connection mProducerConnection;

    private TextView mEventName;
    private TextView mEventStatus;
    private TextView mGoLiveStatus;
    private TextView mTextUnreadMessages;
    private TextView mLiveButton;
    private TextView mCameraPreview1;
    private TextView mCameraPreview2;
    private ImageButton mChatButton;
    private ImageView mEventImage;
    private ImageView mEventImageEnd;
    private ImageView mCircleLiveButton;
    private Button mGetInLine;
    private ImageButton mUnreadCircle;
    private RelativeLayout mVideoViewLayout;
    private ProgressBar mVideoViewProgressBar;
    private VideoView mVideoView;
    private RelativeLayout mAvatarPublisher;


    private Handler mHandler = new Handler();
    private RelativeLayout mPublisherViewContainer;
    private CustomViewSubscriber mSubscriberHostViewContainer;
    private CustomViewSubscriber mSubscriberCelebrityViewContainer;
    private CustomViewSubscriber mSubscriberFanViewContainer;
    private RelativeLayout mPublisherSpinnerLayout;
    private RelativeLayout mGoLiveView;
    private FrameLayout mFragmentContainer;
    private RelativeLayout mStatusBar;


    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSubPublisher;
    private boolean resumeHasRun = false;
    private boolean mIsBound = false;
    private boolean mIsOnPause = false;
    private boolean mUserIsOnstage = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;
    private CustomVideoRenderer mCustomVideoRenderer;
    private boolean mAudioOnlyFan;

    private TextChatFragment mTextChatFragment;
    private FragmentTransaction mFragmentTransaction;
    private boolean msgError = false;

    //Logging
    private OTKAnalyticsData mOnStageAnalyticsData;
    private OTKAnalytics mOnStageAnalytics;

    private String mLogSource;

    private JSONObject mBroadcastData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Creates the action bar
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        //Hide the bar
        ActionBar actionBar = getSupportActionBar();

        if ( actionBar != null ){
            actionBar.hide();
        }

        setContentView(R.layout.activity_fan);

        initLayoutWidgets();

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification(this, mStatusBar);

        //Get the selected event from the instance
        getSelectedEvent(savedInstanceState);

        mAudioOnlyFan = false;



        //Connect to socket
        initSocket();

        //Set event name and images
        setEventUI();

        setupFonts();

        //Disable HWDEC
        OpenTokConfig.enableVP8HWDecoder(false);


    }

    private void initSocket() {
        mSocket = new SocketCoordinator();
        mSocket.getSocket().on(Socket.EVENT_CONNECT,onSocketConnected);
        mSocket.getSocket().on(Socket.EVENT_CONNECT_ERROR,onSocketConnectError);
        mSocket.getSocket().on(Socket.EVENT_CONNECT_TIMEOUT,onSocketConnectError);
        mSocket.getSocket().on("changeStatus", onChangeStatus);
        if(mSocket.getSocket().connected()) {
            init();
        } else {
            mSocket.connect();
        }
    }

    private void init() {

        //The event must be on preshow or live.
        String status = getEventStatus();
        if(status.equals("N") || status.equals("C")) return;

        //If the event is already initializated and the fan was able to join to interactive, don't do anything.
        if(mInitializated && mHls==false) return;

        //If the event is already initializated and the fan was watching HLS, resume the broadcast
        if(mInitializated && mHls) {
            resumeBroadcast();
            try {
                mSocket.emitJoinBroadcast("broadcast" + mBroadcastData.getString("broadcastId"));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "unexpected JSON exception - emitJoinBroadcast", e);
            }
            mSocket.getSocket().on("eventGoLive", onBroadcastGoLive);
            mSocket.getSocket().on("eventEnded", onBroadcastEnd);
            return;
        }

        mInitializated = true;
        //Emit the presence signal
        //@TODO we need a way to know if the user limit is enabled or disabled
        if(true) {
            try {
                mSocket.emitJoinInteractive(mEvent.getString("stage_sessionid"));
                mSocket.getSocket().on("ableToJoin", onAbleToJoin);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "unexpected JSON exception - emitJoinInteractive", e);
            }
        } else {
            //Get the event
            requestEventData(mEvent);
        }
    }

    private Emitter.Listener onSocketConnected = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mConnectionError = false;
            init();
        }
    };

    private Emitter.Listener onChangeStatus = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            String id;
            String newStatus;
            try {
                id = data.getString("id");
                newStatus = data.getString("newStatus");
                Log.i(LOG_TAG, mEvent.getString("event_name"));
                if(newStatus.equals("P") && id.equals(mEvent.getString("id"))) {
                    setEventStatus("P");
                    init();
                }

            } catch (JSONException e) {
                return;
            }

        }
    };

    private Emitter.Listener onSocketConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  if(!mConnectionError) mNotification.show(R.string.hls_reconnecting);
                                  mConnectionError = true;
                              }
                          });
            Log.e(LOG_TAG, "Failed to connect to the signaling server");
        }
    };


    private void setEventUI(){
        if(mEvent == null) return;
        try {
            updateEventName(mEvent.getString("event_name"), EventUtils.getStatusNameById(mEvent.getString("status")));
            EventUtils.loadEventImage(this, mEvent.getString("event_image"), mEventImage);
            EventUtils.loadEventImage(this, mEvent.getString("event_image_end"), mEventImageEnd);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - updateEventName", e);
        }
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
        mSubscriberHostViewContainer = (CustomViewSubscriber) findViewById(R.id.subscriberHostView);
        mSubscriberCelebrityViewContainer = (CustomViewSubscriber) findViewById(R.id.subscriberCelebrityView);
        mSubscriberFanViewContainer = (CustomViewSubscriber) findViewById(R.id.subscriberFanView);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_textchat_container);

        mLoadingSubPublisher = (ProgressBar) findViewById(R.id.loadingSpinnerPublisher);
        mPublisherSpinnerLayout = (RelativeLayout) findViewById(R.id.publisher_spinner_layout);
        mEventName = (TextView) findViewById(R.id.event_name);
        mEventStatus = (TextView) findViewById(R.id.event_status);
        mGoLiveStatus = (TextView) findViewById(R.id.go_live_status);
        mGoLiveView = (RelativeLayout) findViewById(R.id.goLiveView);
        mStatusBar = (RelativeLayout) findViewById(R.id.status_bar);
        mTextUnreadMessages = (TextView) findViewById(R.id.unread_messages);
        mLiveButton = (TextView) findViewById(R.id.live_button);
        mCameraPreview1 = (TextView) findViewById(R.id.camera_preview_1);
        mCameraPreview2 = (TextView) findViewById(R.id.camera_preview_2);
        mEventImageEnd = (ImageView) findViewById(R.id.event_image_end);
        mEventImage = (ImageView) findViewById(R.id.event_image);
        mCircleLiveButton = (ImageView) findViewById(R.id.circle_live_button);
        mChatButton = (ImageButton) findViewById(R.id.chat_button);
        mGetInLine = (Button) findViewById(R.id.btn_getinline);
        mUnreadCircle = (ImageButton) findViewById(R.id.unread_circle);
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoViewProgressBar = (ProgressBar) findViewById(R.id.videoViewProgressBar);
        mVideoViewLayout = (RelativeLayout) findViewById(R.id.videoViewLayout);
        mAvatarPublisher = (RelativeLayout) findViewById(R.id.avatarPublisher);
    }

    private void setupFonts() {
        Typeface font = EventUtils.getFont(this);
        mEventName.setTypeface(font);
        mGoLiveStatus.setTypeface(font);
        mEventStatus.setTypeface(font);
        mGetInLine.setTypeface(font);
        mTextUnreadMessages.setTypeface(font);
        mCameraPreview1.setTypeface(font);
        mCameraPreview2.setTypeface(font);
        ((TextView)mStatusBar.getChildAt(0)).setTypeface(font);
    }

    private void getSelectedEvent (Bundle savedInstanceState) {
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
            if(savedInstanceState.getSerializable("event_index") == null) return;
            event_index = Integer.parseInt((String) savedInstanceState.getSerializable("event_index"));
        }
        mEvent = InstanceApp.getInstance().getEventByIndex(event_index);
    }

    private void requestEventData (JSONObject event) {

        if(InstanceApp.getInstance().getEnableGetInline()) {
            setVisibilityGetInLine(View.VISIBLE);
        }

        try {
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
        mConnectionError = false;
        JSONObject objSource = new JSONObject();
        try {
            mEvent = results.getJSONObject("event");
            mApiKey = results.getString("apiKey");
            mToken = results.getString("tokenHost");
            mSessionId = results.getString("sessionIdHost");
            mBackstageToken = results.getString("tokenProducer");
            mBackstageSessionId = results.getString("sessionIdProducer");

            //Set the LogSource:
            mLogSource = getApplicationContext().getApplicationInfo().packageName + "-" +
                         mEvent.getString("admins_name") + "-" +
                         mEvent.getString("id");

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

        if(!mConnectionError) mNotification.show(R.string.connection_lost);
        mGetInLine.setVisibility(View.GONE);
    }


    @Override
    public void onPause() {
        super.onPause();

        mIsOnPause = true;

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
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(getResources().getString(R.string.notification));

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
                    try {
                        ((ClearNotificationService.ClearBinder) binder).service.startService(new Intent(FanActivity.this, ClearNotificationService.class));
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mNotificationManager.notify(ClearNotificationService.NOTIFICATION_ID, mNotifyBuilder.build());
                    } catch (Exception ex) {
                        Log.e(LOG_TAG, "onServiceConnected Error ---> " + ex.getMessage());
                    }

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

        mIsOnPause = false;

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
        if(mTextChatFragment == null) {
           loadTextChatFragment();
        }
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);

        reloadInterface();

        //Resumes the video
        resumeBroadcast();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        if (isFinishing()) {
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            disconnectOnStageSession();
            disconnectBackstageSession();
        }
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        disconnectOnStageSession();
        disconnectBackstageSession();
        disconnectSocket();
        super.onDestroy();
        finish();
    }


    @Override
    public void onBackPressed() {

        if(mFragmentContainer.getVisibility() == View.VISIBLE) {
            toggleChat();
        }else {
            disconnectOnStageSession();
            disconnectBackstageSession();
            disconnectSocket();
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            if (mIsBound) {
                unbindService(mConnection);
                mIsBound = false;
            }

            super.onBackPressed();
        }
    }

    private void disconnectSocket() {
        if(mSocket != null) {
            mSocket.disconnect();
            mSocket.getSocket().off("eventGoLive", onBroadcastGoLive);
            mSocket.getSocket().off("eventEnded", onBroadcastEnd);
            mSocket.getSocket().off("ableToJoin", onAbleToJoin);
            mSocket.getSocket().off(Socket.EVENT_CONNECT, onSocketConnected);
            mSocket.getSocket().off(Socket.EVENT_CONNECT_ERROR,onSocketConnectError);
            mSocket.getSocket().off(Socket.EVENT_CONNECT_TIMEOUT,onSocketConnectError);
            mSocket = null;
        }
    }

    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriberHost != null) {
                    attachSubscriberHostView();
                }
                if (mSubscriberCelebrity != null) {
                    attachSubscriberCelebrityView();
                }
                if (mSubscriberFan != null) {
                    attachSubscriberFanView();
                }
                if(mSession != null) {
                    updateViewsWidth();
                }
            }
        }, 500);
    }

    private void disconnectOnStageSession() {
        if (mSession != null) {
            //Logging
            addLogEvent(OTKAction.FAN_DISCONNECTS_ONSTAGE, OTKVariation.ATTEMPT);
            mSession.disconnect();
            mSession = null;
        }
    }

    private void disconnectBackstageSession() {
        if (mBackstageSession != null) {
            if(mTestingOnStage) {
                this.stopTestingConnectionQuality();
            } else {
                if ( mTest!= null ) {
                    mTest.stopNetworkTest();
                    mTest = null;
                }
                mTestSubscriber = null;
            }
            //Logging
            addLogEvent(OTKAction.FAN_DISCONNECTS_BACKSTAGE, OTKVariation.ATTEMPT);
            mBackstageSession.disconnect();
            mBackstageSession = null;
        }
    }

    private void updateViewsWidth() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                int streams = 0;
                if (mFanStream != null || mUserIsOnstage) streams++;
                if (mCelebrityStream != null) streams++;
                if (mHostStream != null) streams++;

                if (streams > 0) {
                    mEventImage.setVisibility(View.GONE);

                    RelativeLayout.LayoutParams celebrity_head_params = (RelativeLayout.LayoutParams) mSubscriberCelebrityViewContainer.getLayoutParams();
                    celebrity_head_params.width = (mCelebrityStream != null) ? screenWidth(FanActivity.this) / streams : 1;
                    mSubscriberCelebrityViewContainer.setLayoutParams(celebrity_head_params);

                    RelativeLayout.LayoutParams host_head_params = (RelativeLayout.LayoutParams) mSubscriberHostViewContainer.getLayoutParams();
                    host_head_params.width = (mHostStream != null) ? screenWidth(FanActivity.this) / streams : 1;
                    mSubscriberHostViewContainer.setLayoutParams(host_head_params);


                    if (mUserIsOnstage) {

                        if (mAudioOnlyFan) {
                            mSubscriberFanViewContainer.displayAvatar(true);
                        } else {
                            mPublisher.getView().setVisibility(View.VISIBLE);
                        }
                        //copy layoutparams from fan container
                        RelativeLayout.LayoutParams publisher_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                        publisher_head_params.width = screenWidth(FanActivity.this) / streams;
                        mSubscriberFanViewContainer.setLayoutParams(publisher_head_params);
                        mSubscriberFanViewContainer.setVisibility(View.VISIBLE);
                    } else {
                        RelativeLayout.LayoutParams fan_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                        fan_head_params.width = (mFanStream != null) ? screenWidth(FanActivity.this) / streams : 1;
                        mSubscriberFanViewContainer.setLayoutParams(fan_head_params);
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

            //Logging
            addLogEvent(OTKAction.FAN_CONNECTS_BACKSTAGE, OTKVariation.ATTEMPT);

            mBackstageSession.connect(mBackstageToken);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Connected to the session");
        mConnectionError = false;

        // stop loading spinning
        //mPublisherSpinnerLayout.setVisibility(View.GONE);

        //Start publishing in backstage session
        if(session.getSessionId().equals(mBackstageSessionId)) {

            //Logging
            addLogEvent(OTKAction.FAN_CONNECTS_BACKSTAGE, OTKVariation.SUCCESS);

            //Logging
            addLogEvent(OTKAction.FAN_PUBLISHES_BACKSTAGE, OTKVariation.ATTEMPT);
            mBackstageSession.publish(mPublisher);

            setVisibilityGetInLine(View.VISIBLE);

            //loading text-chat ui component
            loadTextChatFragment();

        } else {

            //Init the analytics logging for onstage
            String source = getPackageName();
            SharedPreferences prefs = getSharedPreferences("opentok", Context.MODE_PRIVATE);
            String guidIB = prefs.getString("guidIB", null);
            if (null == guidIB) {
                guidIB = UUID.randomUUID().toString();
                prefs.edit().putString("guidIB", guidIB).commit();
            }
            mOnStageAnalyticsData = new OTKAnalyticsData.Builder(IBConfig.LOG_CLIENT_VERSION, mLogSource, IBConfig.LOG_COMPONENTID, guidIB).build();
            mOnStageAnalytics = new OTKAnalytics(mOnStageAnalyticsData);
            mOnStageAnalyticsData.setSessionId(session.getSessionId());
            mOnStageAnalyticsData.setConnectionId(session.getConnection().getConnectionId());
            mOnStageAnalyticsData.setPartnerId(mApiKey);
            mOnStageAnalytics.setData(mOnStageAnalyticsData);

            //Logging
            addLogEvent(OTKAction.FAN_CONNECTS_ONSTAGE, OTKVariation.SUCCESS);

            mGetInLine.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Disconnected from the session.");
        if(session.getSessionId().equals(mSessionId)) {
            //Logging
            addLogEvent(OTKAction.FAN_DISCONNECTS_ONSTAGE, OTKVariation.SUCCESS);
            cleanViews();
            mUserIsOnstage = false;

        } else {
            //Logging
            addLogEvent(OTKAction.FAN_DISCONNECTS_BACKSTAGE, OTKVariation.SUCCESS);
            leaveLine();

        }
    }

    private void leaveLine() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String status = getEventStatus();
                mNotification.hide();
                if (mBackstageSession != null) {
                    //Logging
                    addLogEvent(OTKAction.FAN_UNPUBLISHES_BACKSTAGE, OTKVariation.ATTEMPT);
                    mBackstageSession.unpublish(mPublisher);
                    disconnectBackstageSession();
                }
                if (mPublisher != null) {
                    mPublisherViewContainer.setVisibility(View.GONE);
                    mPublisher.getView().setVisibility(View.GONE);
                    mPublisherViewContainer.removeView(mPublisher.getView());
                    mPublisher = null;
                }
                //Hide chat stuff
                hideChat();
                mGetInLine.setText(getResources().getString(R.string.get_inline));
                mGetInLine.setBackground(getResources().getDrawable(R.drawable.get_in_line_button));
                mPublisherSpinnerLayout.setVisibility(View.GONE);
                mNewFanSignalAckd = false;

                if (!status.equals("C")) {
                    setVisibilityGetInLine(View.VISIBLE);
                } else {
                    setVisibilityGetInLine(View.GONE);
                }

            }
        });

    }

    public void cleanViews() {
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriberFan != null) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        }

        if (mUserIsOnstage) {
            mSubscriberFanViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriberHost != null) {
            mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
        }

        if (mSubscriberCelebrity != null) {
            mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
        }
    }

    private void subscribeHostToStream(Stream stream) {
        //Logging
        addLogEvent(OTKAction.FAN_SUBSCRIBES_HOST, OTKVariation.ATTEMPT);

        mSubscriberHost = new Subscriber(FanActivity.this, stream);
        mSubscriberHost.setVideoListener(this);
        mSession.subscribe(mSubscriberHost);
        if(mOnstageMuted) mSubscriberHost.setSubscribeToAudio(false);
        if (stream.hasVideo()) {
            // start loading spinning
            mSubscriberHostViewContainer.displaySpinner(true);
        }
        else {
            enableAudioOnlyView( mSubscriberHost.getStream().getConnection().getConnectionId(), true);
        }
    }

    private void subscribeCelebrityToStream(Stream stream) {
        //Logging
        addLogEvent(OTKAction.FAN_SUBSCRIBES_CELEBRITY, OTKVariation.ATTEMPT);

        mSubscriberCelebrity = new Subscriber(FanActivity.this, stream);
        mSubscriberCelebrity.setVideoListener(this);
        mSession.subscribe(mSubscriberCelebrity);
        if(mOnstageMuted) mSubscriberCelebrity.setSubscribeToAudio(false);
        if (stream.hasVideo()) {
            // start loading spinning
            mSubscriberCelebrityViewContainer.displaySpinner(true);
        }
        else {
            enableAudioOnlyView( mSubscriberCelebrity.getStream().getConnection().getConnectionId(), true);
        }
    }

    private void subscribeFanToStream(Stream stream) {
        //Logging
        addLogEvent(OTKAction.FAN_SUBSCRIBES_FAN, OTKVariation.ATTEMPT);

        mSubscriberFan = new Subscriber(FanActivity.this, stream);
        mSubscriberFan.setVideoListener(this);
        mSession.subscribe(mSubscriberFan);
        if(mOnstageMuted) mSubscriberFan.setSubscribeToAudio(false);
        if (stream.hasVideo()) {
            // start loading spinning
            mSubscriberFanViewContainer.displaySpinner(true);
        }
        else {
            enableAudioOnlyView( mSubscriberFan.getStream().getConnection().getConnectionId(), true);
        }
    }

    private void subscribeProducer() {
        if(mProducerStream != null) {
            showPublisher();
            muteOnstage(true);
            addLogEvent(OTKAction.FAN_SUBSCRIBES_PRODUCER, OTKVariation.ATTEMPT);
            mSubscriberProducer = new Subscriber(FanActivity.this, mProducerStream);
            mBackstageSession.subscribe(mSubscriberProducer);
            mNotification.showNotification(Notification.TYPE.PRIVATE_CALL);
        }
    }

    private void unSubscribeProducer() {
        if (mProducerStream!= null && mSubscriberProducer != null) {
            muteOnstage(false);
            addLogEvent(OTKAction.FAN_UNSUBSCRIBES_PRODUCER, OTKVariation.ATTEMPT);
            mBackstageSession.unsubscribe(mSubscriberProducer);
            mSubscriberProducer = null;
            if(mOnBackstage) {
                mNotification.showNotification(Notification.TYPE.BACKSTAGE);
            } else {
                mNotification.hide();
                hidePublisher();
            }

        }

    }

    private void startPrivateCall(String data) {
        String callWith = "";
        try {
            callWith = new JSONObject(data).getString("callWith");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }

        if(mProducerStreamOnstage != null && mPublisher != null && mPublisher.getStream().getConnection().getConnectionId().equals(callWith)) {
            addLogEvent(OTKAction.FAN_SUBSCRIBES_PRODUCER, OTKVariation.ATTEMPT);
            mSubscriberProducerOnstage = new Subscriber(FanActivity.this, mProducerStreamOnstage);
            mSession.subscribe(mSubscriberProducerOnstage);
            mNotification.showNotification(Notification.TYPE.PRIVATE_CALL);
        } else {
            if(mUserIsOnstage) mNotification.showNotification(Notification.TYPE.TEMPORARILLY_MUTED);
        }
        muteOnstage(true);
    }

    private void endPrivateCall() {
        muteOnstage(false);
        if(mUserIsOnstage) mNotification.hide();
        if (mProducerStreamOnstage != null && mSubscriberProducerOnstage != null) {
            mSession.unsubscribe(mSubscriberProducerOnstage);
            mSubscriberProducerOnstage = null;
            mNotification.hide();
        }

    }

    private void muteOnstage(Boolean mute){
        mOnstageMuted = mute;
        if(mSubscriberHost != null) mSubscriberHost.setSubscribeToAudio(!mute);
        if(mSubscriberFan != null) mSubscriberFan.setSubscribeToAudio(!mute);
        if(mSubscriberCelebrity != null) mSubscriberCelebrity.setSubscribeToAudio(!mute);
    }

    private void unsubscribeHostFromStream(Stream stream) {
        if (mSubscriberHost != null && mSubscriberHost.getStream().equals(stream)) {
            mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
            //mSession.unsubscribe(mSubscriberHost);
            mSubscriberHost = null;
            mSubscriberHostViewContainer.displaySpinner(false);
        }
    }

    private void unsubscribeCelebrityFromStream(Stream stream) {
        if (mSubscriberCelebrity != null && mSubscriberCelebrity.getStream().equals(stream)) {
            mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
            //mSession.unsubscribe(mSubscriberCelebrity);
            mSubscriberCelebrity = null;
            mSubscriberCelebrityViewContainer.displaySpinner(false);
        }
    }

    private void unsubscribeFanFromStream(Stream stream) {
        if (mSubscriberCelebrity != null && mSubscriberFan.getStream().equals(stream)) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            //mSession.unsubscribe(mSubscriberFan);
            mSubscriberFan = null;
            mSubscriberFanViewContainer.displaySpinner(false);
        }
    }

    private void attachSubscriberHostView() {
        mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
        mSubscriberHost.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        ((GLSurfaceView)mSubscriberHost.getView()).setZOrderMediaOverlay(false);
        mSubscriberHostViewContainer.addView(mSubscriberHost.getView());
    }

    private void attachSubscriberCelebrityView() {
        mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
        mSubscriberCelebrity.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        ((GLSurfaceView)mSubscriberCelebrity.getView()).setZOrderMediaOverlay(false);
        mSubscriberCelebrityViewContainer.addView(mSubscriberCelebrity.getView());
    }

    private void attachSubscriberFanView() {
        mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        mSubscriberFan.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        ((GLSurfaceView)mSubscriberFan.getView()).setZOrderMediaOverlay(false);
        mSubscriberFanViewContainer.addView(mSubscriberFan.getView());
    }

    private void attachPublisherView() {
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        mPublisherViewContainer.addView(mPublisher.getView());
    }

    private void attachPublisherViewToFanView() {
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        mSubscriberFanViewContainer.addView(mPublisher.getView());
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        String error = exception.getErrorCode().toString();
        Log.i(LOG_TAG, "Session exception: " + exception.getMessage() + " Code: " + exception.getErrorCode());

        //Logging
        if(session.getSessionId().equals(mSessionId)) {
            addLogEvent(OTKAction.FAN_DISCONNECTS_ONSTAGE, OTKVariation.ERROR);
        } else {
            addLogEvent(OTKAction.FAN_DISCONNECTS_BACKSTAGE, OTKVariation.ERROR);
        }

        if(error.equals("ConnectionDropped") || error.equals("ConnectionFailed")) {
            if(mUserIsOnstage) {
                mLiveButton.setVisibility(View.GONE);
                mCircleLiveButton.setVisibility(View.GONE);
            }
            cleanViews();
            if(!mConnectionError) mNotification.show(R.string.connection_lost);
            mGetInLine.setVisibility(View.GONE);
            mEventImage.setVisibility(View.VISIBLE);
            mConnectionError = true;
            restartOpentokObjects();
            sendWarningSignal();

        }
    }

    private void restartOpentokObjects() {
        disconnectOnStageSession();
        disconnectBackstageSession();
        mUserIsOnstage = false;
        mPublisher  = null;
        mSubscriberHost  = null;
        mSubscriberCelebrity  = null;
        mSubscriberFan  = null;
        mSubscriberProducer  = null;
        mSubscriberProducerOnstage  = null;
        mTestSubscriber  = null;
        mCelebrityStream  = null;
        mFanStream  = null;
        mHostStream  = null;
        mProducerStream  = null;
        mProducerStreamOnstage  = null;
        mProducerConnection = null;

        mAudioOnlyFan = false;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        String status = getEventStatus();
        Log.i(LOG_TAG, "onStreamReceived/" + status);
        Boolean bIsOnStage = session.getSessionId().equals(mSessionId);
        Log.i(LOG_TAG, "Video height:" + String.valueOf(stream.getVideoHeight()));
        Log.i(LOG_TAG, "Video width:" + String.valueOf(stream.getVideoWidth()));
        Log.i(LOG_TAG, "Video type:" + String.valueOf(stream.getStreamVideoType().name()));

        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if (mFanStream == null && bIsOnStage) {
                    mFanStream = stream;
                    if(status.equals("L")) {
                        subscribeFanToStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=celebrity":
                if (mCelebrityStream == null) {
                    mCelebrityStream = stream;
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
                if(mProducerStream == null  && !bIsOnStage){
                    mProducerStream = stream;
                }
                if(mProducerStreamOnstage == null  && bIsOnStage) {
                    mProducerStreamOnstage = stream;
                }
                break;

        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        String status = getEventStatus();
        String streamConnectionId = stream.getConnection().getConnectionId();
        Boolean bIsOnStage = session.getSessionId().equals(mSessionId);
        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if(mFanStream != null && bIsOnStage && mFanStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    mFanStream = null;
                    if(status.equals("L") || status.equals("C")) {
                        unsubscribeFanFromStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=celebrity":
                if(mCelebrityStream != null && mCelebrityStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    mCelebrityStream = null;
                    if(status.equals("L") || status.equals("C") || mUserIsOnstage) {
                        unsubscribeCelebrityFromStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=host":
                if(mHostStream != null && mHostStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    mHostStream = null;
                    if(status.equals("L") || status.equals("C") || mUserIsOnstage) {
                        unsubscribeHostFromStream(stream);
                        updateViewsWidth();
                    }
                }
            case "usertype=producer":
                if(!bIsOnStage) {
                    if(mProducerStream != null && mProducerStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                        addLogEvent(OTKAction.FAN_UNSUBSCRIBES_PRODUCER, OTKVariation.SUCCESS);
                        unSubscribeProducer();
                        mProducerStream = null;
                    }
                } else {
                    if(mProducerStreamOnstage != null && mProducerStreamOnstage.getConnection().getConnectionId().equals(streamConnectionId)) {
                        addLogEvent(OTKAction.FAN_UNSUBSCRIBES_PRODUCER, OTKVariation.SUCCESS);
                        endPrivateCall();
                        mProducerStreamOnstage = null;
                    }
                }
                break;
        }
        if(!stream.hasVideo()) {
            enableAudioOnlyView(stream.getConnection().getConnectionId(), false);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {
        mLoadingSubPublisher.setVisibility(View.GONE);

        if (stream.getSession().getSessionId().equals(mBackstageSessionId)) {
            Log.i(LOG_TAG, "publisher Video height:" + String.valueOf(stream.getVideoHeight()));
            Log.i(LOG_TAG, "publisher Video width:" + String.valueOf(stream.getVideoWidth()));
            Log.i(LOG_TAG, "publisher Video type:" + String.valueOf(stream.getStreamVideoType().name()));

            //Logging
            addLogEvent(OTKAction.FAN_PUBLISHES_BACKSTAGE, OTKVariation.SUCCESS);

            mBackstageConnectionId = mPublisher.getStream().getConnection().getConnectionId();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "Setting setSaveScreenshot true");
                    mCustomVideoRenderer.setSaveScreenshot(true);
                }
            }, 5000);
        } else {
            //Logging
            addLogEvent(OTKAction.FAN_PUBLISHES_ONSTAGE, OTKVariation.SUCCESS);
        }

        if(mHostStream != null && (getEventStatus().equals("L") || mUserIsOnstage)) {
            Log.i("NetworkTest", "mtestinghost");
            mTestingOnStage = true;
            testStreamConnectionQuality(mHostStream);
        } else if(mCelebrityStream != null  && (getEventStatus().equals("L") || mUserIsOnstage)) {
            Log.i("NetworkTest", "mCelebritytest");
            mTestingOnStage = true;
            testStreamConnectionQuality(mCelebrityStream);
        } else {
            Log.i("NetworkTest", "mFanTest");
            mTestingOnStage = false;
            testStreamConnectionQuality(stream);
        }
    }

    private void showPublisher() {
        if(mPublisher != null) {
            mPublisherViewContainer.clearAnimation();
            mPublisherSpinnerLayout.clearAnimation();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPublisherViewContainer.setAlpha(1f);
                    mPublisherViewContainer.setVisibility(View.VISIBLE);
                    mPublisherSpinnerLayout.setVisibility(View.VISIBLE);
                    //if(mAvatarPublisher.getVisibility() == View.GONE) {
                        mPublisher.getView().setVisibility(View.VISIBLE);
                    //}

                }
            }, 500);

        }
    }

    private void hidePublisher() {
        if(mPublisher != null && mPublisherSpinnerLayout.getVisibility() != View.GONE) {

            AlphaAnimation animation1 = new AlphaAnimation(1f, 0f);
            animation1.setDuration(2000);
            mPublisherSpinnerLayout.startAnimation(animation1);
            mPublisherViewContainer.startAnimation(animation1);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!mOnBackstage) {
                        mPublisher.getView().setVisibility(View.GONE);
                        mPublisherSpinnerLayout.setVisibility(View.GONE);
                        mPublisherViewContainer.setVisibility(View.GONE);
                    }
                }
            }, 1000);
        }

    }

    private void stopTestingConnectionQuality() {

        if(mTestSubscriber == null) return;
        if(!mTestingOnStage) { //if autoselfsubscribe
            if(mTestSubscriber.getSession().getSessionId().equals(mSessionId)) {
                if(mSession != null) mSession.unsubscribe(mTestSubscriber);
            } else {
                if(mBackstageSession != null) mBackstageSession.unsubscribe(mTestSubscriber);
            }

        }
        if ( mTest!= null ) {
            mTest.stopNetworkTest();
            mTest = null;
        }
        mTestSubscriber = null;
    }

    private void testStreamConnectionQuality(Stream stream) {

        if(!mTestingOnStage) {
            autoselfSubscribe(stream);
            if(stream.getSession().getSessionId().equals(mBackstageSessionId)) {
                mBackstageSession.subscribe(mTestSubscriber);
            } else {
                mSession.subscribe(mTestSubscriber);
            }
        } else {
            if(mSubscriberHost != null && stream.getConnection().getConnectionId() == mSubscriberHost.getStream().getConnection().getConnectionId() && mSubscriberHost.getStream().hasVideo()) {
                mTestSubscriber = mSubscriberHost;
            } else {
                if (mSubscriberCelebrity != null && stream.getConnection().getConnectionId() == mSubscriberCelebrity.getStream().getConnection().getConnectionId() && mSubscriberCelebrity.getStream().hasVideo()) {
                    mTestSubscriber = mSubscriberCelebrity;
                }
                else{
                    autoselfSubscribe(stream);
                }
            }
        }

        mTest = new NetworkTest();
        mTest.setNetworkTestListener(this);
        mTest.startNetworkTest(mTestSubscriber);
    }


    private void autoselfSubscribe(Stream stream){
        mTestSubscriber = new Subscriber(FanActivity.this, stream);
        mTestSubscriber.setSubscriberListener(this);
        mTestSubscriber.setSubscribeToAudio(false);
    }

    public void sendQualityUpdate(String connectionId, String quality) {
        if (mBackstageSession != null && mProducerConnection != null) {
            String msg = "{\"connectionId\":\"" + connectionId + "\", \"quality\":\"" + quality + "\"}";
            mBackstageSession.sendSignal("qualityUpdate", msg);
        }
    }

    public void sendWarningSignal() {
        if (mBackstageSession != null && mProducerConnection != null) {
            String connectionId = mPublisher != null ? mPublisher.getStream().getConnection().getConnectionId() : "";
            String msg = "{\"connectionId\":\"" + connectionId + "\", \"connected\":\"false\", \"subscribing\":\"false\"}";
            mBackstageSession.sendSignal("warning", msg);
            Log.i(LOG_TAG, "sendWarningSignal sent =>" + msg);
        }
    }


    private static int screenWidth(Context ctx) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels + 1;
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {

        if(publisher.getSession().getSessionId().equals(mSessionId)) {
            addLogEvent(OTKAction.FAN_UNPUBLISHES_ONSTAGE, OTKVariation.SUCCESS);
        } else {
            addLogEvent(OTKAction.FAN_UNPUBLISHES_BACKSTAGE, OTKVariation.SUCCESS);
        }
        Log.i(LOG_TAG, "Publisher destroyed");
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOG_TAG, "Publisher exception: " + exception.getMessage());
        //Logging
        if(publisher.getSession().getSessionId().equals(mSessionId)) {
            addLogEvent(OTKAction.FAN_PUBLISHES_ONSTAGE, OTKVariation.ERROR);
        } else {
            addLogEvent(OTKAction.FAN_PUBLISHES_BACKSTAGE, OTKVariation.ERROR);
        }

    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "First frame received");
        Log.i(LOG_TAG, "onVideoDataReceived " + subscriber.getStream().getConnection().getData());
        if(subscriber.getStream().getConnection().getData().equals("usertype=fan")) {

            //Logging
            addLogEvent(OTKAction.FAN_SUBSCRIBES_FAN, OTKVariation.SUCCESS);

            // stop loading spinning
            mSubscriberFanViewContainer.displaySpinner(false);
            attachSubscriberFanView();
        } else if(subscriber.getStream().getConnection().getData().equals("usertype=host")) {

            //Logging
            addLogEvent(OTKAction.FAN_SUBSCRIBES_HOST, OTKVariation.SUCCESS);

            // stop loading spinning
            mSubscriberHostViewContainer.displaySpinner(false);
            attachSubscriberHostView();


        } else if(subscriber.getStream().getConnection().getData().equals("usertype=celebrity")) {

            //Logging
            addLogEvent(OTKAction.FAN_SUBSCRIBES_CELEBRITY, OTKVariation.SUCCESS);

            // stop loading spinning
            mSubscriberCelebrityViewContainer.displaySpinner(false);
            attachSubscriberCelebrityView();
        } else if(subscriber.getStream().getConnection().getData().equals("usertype=producer")) {
            Boolean bIsOnStage = subscriber.getSession().getSessionId().equals(mSessionId);
            if(!bIsOnStage) {
                addLogEvent(OTKAction.FAN_SUBSCRIBES_PRODUCER, OTKVariation.SUCCESS);
            } else {
                addLogEvent(OTKAction.FAN_SUBSCRIBES_PRODUCER, OTKVariation.SUCCESS);
            }

        }
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG,
                "Video disabled:" + reason);
        enableAudioOnlyView(subscriber.getStream().getConnection().getConnectionId(), true);

        if (mTestSubscriber != null && mTestSubscriber.getStream().getConnection().getConnectionId() == subscriber.getStream().getConnection().getConnectionId()) {
            mTest.updateTest(true);
        }
    }

    private void enableAudioOnlyView(String subscriberConnectionId, boolean show) {
        String host = mHostStream != null ? mHostStream.getConnection().getConnectionId() : "";
        String celebrity = mCelebrityStream != null ? mCelebrityStream.getConnection().getConnectionId() : "";
        String fan = mFanStream != null ? mFanStream.getConnection().getConnectionId() : "";
        if(subscriberConnectionId.equals(host)) {
            if (show) {
                mSubscriberHost.getView().setVisibility(View.GONE);
                mSubscriberHostViewContainer.displayAvatar(true);
            }
            else {
                mSubscriberHost.getView().setVisibility(View.VISIBLE);
                mSubscriberHostViewContainer.displayAvatar(false);
            }
        }
        if(subscriberConnectionId.equals(celebrity)) {
            if (show) {
                mSubscriberCelebrity.getView().setVisibility(View.GONE);
                mSubscriberCelebrityViewContainer.displayAvatar(true);
            }
            else {
                mSubscriberCelebrity.getView().setVisibility(View.VISIBLE);
                mSubscriberCelebrityViewContainer.displayAvatar(false);
            }
        }
        if(subscriberConnectionId.equals(fan)) {
            if (show) {
                mSubscriberFan.getView().setVisibility(View.GONE);
                mSubscriberFanViewContainer.displayAvatar(true);
                mAudioOnlyFan = true;
            }
            else {
                mSubscriberFan.getView().setVisibility(View.VISIBLE);
                mSubscriberFanViewContainer.displayAvatar(false);
                mAudioOnlyFan = false;
            }
        }
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG, "Video enabled:" + reason);
        enableAudioOnlyView(subscriber.getStream().getConnection().getConnectionId(), false);

        if (mTestSubscriber != null && mTestSubscriber.getStream().getConnection().getConnectionId() == subscriber.getStream().getConnection().getConnectionId()) {
            mTest.updateTest(false);
        }
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

        if(!subscriberKit.getStream().hasVideo()) {
            enableAudioOnlyView(subscriberKit.getStream().getConnection().getConnectionId(), true);
        }
    }

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber Disconnected");
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        Log.e(LOG_TAG, "SubscriberKit opentokError.getMessage() ---> " + opentokError.getMessage());
        Log.e(LOG_TAG, "SubscriberKit opentokError.getErrorCode() ---> " + opentokError.getErrorCode());
        Log.e(LOG_TAG, "SubscriberKit opentokError.getErrorDomain() ---> " + opentokError.getErrorDomain());

        //Preventing a crash when the producer kicks the fan from the active fan list
        if(opentokError.getErrorCode() == OpentokError.ErrorCode.SessionSubscriberNotFound) return;

        try {
            addLogEvent(OTKAction.FAN_SUBSCRIBES_CELEBRITY, OTKVariation.ERROR);
            mSubscribingError = true;
            sendWarningSignal();
            //if(!mConnectionError) mNotification.showConnectionLost();
        } catch(Exception ex) {
            Log.e(LOG_TAG, "Catching error SubscriberKit");
        }

    }

    /* Signal Listener methods */
    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {

        Log.i(LOG_TAG, "New signal:" + type);
        if(type != null) {
            //Check the origin of the signal
            if (connection.getData().equals("usertype=producer")) {
                switch (type) {
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
                    case "joinHostNow":
                        joinHostNow();
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
                    case "privateCall":
                        startPrivateCall(data);
                        break;
                    case "endPrivateCall":
                        endPrivateCall();
                        break;
                }
            }
            else {
                if (!connection.getData().equals("usertype=fan") && !connection.getData().equals("usertype=host")
                        && !connection.getData().equals("usertype=celebrity"))
                    Log.i(LOG_TAG, "Got a signal from an unexpected origin. Ignoring");
            }
        }

        //TODO: onChangeVolumen
    }

    private void ackNewFanSignal() {
        mNewFanSignalAckd = true;

        if(mCustomVideoRenderer.getSnapshot() != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    if(mPublisher == null) return;
                    String status = getEventStatus();
                    String connectionId = mBackstageConnectionId;
                    String sessionId = mBackstageSessionId;
                    String snapshot = mCustomVideoRenderer.getSnapshot();
                    Log.i(LOG_TAG, "sending snapshot : " + snapshot);
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("connectionId", connectionId);
                        obj.put("sessionId", sessionId);
                        obj.put("snapshot", snapshot);
                    } catch (JSONException ex) {
                        Log.e(LOG_TAG, "ackNewFanSignal error " + ex.getMessage());
                    }
                    mSocket.SendSnapShot(obj);

                    //Send warning signal
                    if(mSession == null || (status.equals("L") && mSubscribingError)) {
                        Log.i(LOG_TAG, "send warning signal");
                        sendWarningSignal();
                    }
                }
            });
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ackNewFanSignal();
                }
            }, 1000);
        }


    }

    private void joinBackstage() {
        mOnBackstage = true;
        mPublisher.setPublishVideo(true);
        showPublisher();
        mNotification.showNotification(Notification.TYPE.BACKSTAGE);
    }

    private void disconnectBackstage() {
        mOnBackstage = false;
        hidePublisher();
        mNotification.hide();
    }

    private void connectWithOnstage() {
        mOnBackstage = false;
        hidePublisher();
        mNotification.hide();
        //End the private call
        if (mProducerStream!= null && mSubscriberProducer != null) {
            muteOnstage(false);
            mBackstageSession.unsubscribe(mSubscriberProducer);
            mSubscriberProducer = null;
        }

        //Hidding leave line button
        setVisibilityGetInLine(View.GONE);
        //Logging
        addLogEvent(OTKAction.FAN_UNPUBLISHES_BACKSTAGE, OTKVariation.ATTEMPT);
        mBackstageSession.unpublish(mPublisher);
        mGoLiveView.setVisibility(View.VISIBLE);

        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.destroy();

        mPublisher = new Publisher(FanActivity.this, "publisher");
        mPublisher.setPublisherListener(this);
    }

    private void joinHostNow() {

        Log.i(LOG_TAG, "joinHostNow!");
        publishOnStage();
    }

    private void publishOnStage(){
        if(mSession != null && mPublisher != null) {
            mSession.publish(mPublisher);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLiveButton.setVisibility(View.VISIBLE);
                    mCircleLiveButton.setVisibility(View.VISIBLE);
                    mUserIsOnstage = true;
                    attachPublisherViewToFanView();
                    if (mHostStream != null && mSubscriberHost == null)
                        subscribeHostToStream(mHostStream);
                    if (mCelebrityStream != null && mSubscriberCelebrity == null)
                        subscribeCelebrityToStream(mCelebrityStream);
                    updateViewsWidth();
                    AlphaAnimation animation1 = new AlphaAnimation(0.8f, 0f);
                    animation1.setDuration(500);
                    animation1.setFillAfter(true);
                    mGoLiveView.startAnimation(animation1);
                }
            }, 2000);
        } else {
            mGoLiveView.setVisibility(View.GONE);
        }

    }

    private void disconnectFromOnstage() {
        mUserIsOnstage = false;
        mSubscriberFanViewContainer.displayAvatar(false);

        //Unpublish
        addLogEvent(OTKAction.FAN_UNPUBLISHES_ONSTAGE, OTKVariation.ATTEMPT);
        mSession.unpublish(mPublisher);

        //Hide publisher
        mPublisherSpinnerLayout.setVisibility(View.GONE);
        mPublisherViewContainer.setVisibility(View.GONE);
        mPublisher.getView().setVisibility(View.GONE);


        //Hide chat
        hideChat();

        //Disconnect from backstage
        disconnectBackstageSession();

        //Remove publisher
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.destroy();

        updateViewsWidth();
        mLiveButton.setVisibility(View.GONE);
        mCircleLiveButton.setVisibility(View.GONE);

        mNotification.show(R.string.thanks_for_participating);

        mGoLiveView.clearAnimation();
        mGoLiveView.setAlpha(1f);
        mGoLiveView.setVisibility(View.GONE);
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
        if(mTextChatFragment != null ){
            mTextChatFragment.addMessage(msg);
            if(mFragmentContainer.getVisibility() != View.VISIBLE) {
                mUnreadMessages++;
                refreshUnreadMessages();
                mChatButton.setVisibility(View.VISIBLE);
            }
        }

    }

    private void refreshUnreadMessages() {
        if(mUnreadMessages > 0) {
            mTextUnreadMessages.setVisibility(View.VISIBLE);
            mUnreadCircle.setVisibility(View.VISIBLE);
            setUnreadCircleWidth();
        } else {
            mTextUnreadMessages.setVisibility(View.GONE);
            mUnreadCircle.setVisibility(View.GONE);
        }
        mTextUnreadMessages.setText(Integer.toString(mUnreadMessages));
    }

    private void setUnreadCircleWidth() {
        ViewGroup.LayoutParams params = mUnreadCircle.getLayoutParams();
        Resources r = getResources();
        int newWidth = 0;
        newWidth = mUnreadMessages > 9 ? 35:27;
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newWidth, r.getDisplayMetrics());
        mUnreadCircle.setLayoutParams(params);
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
        if(mUserIsOnstage) {
            if(video.equals("on")) {
                mPublisher.getView().setVisibility(View.VISIBLE);
                mSubscriberFanViewContainer.displayAvatar(false);
                mAudioOnlyFan = false;
            } else {
                mPublisher.getView().setVisibility(View.GONE);
                mSubscriberFanViewContainer.displayAvatar(true);
                mAudioOnlyFan = true;
            }
        } else {
            if(mPublisherViewContainer.getVisibility() == View.VISIBLE) {
                if(video.equals("on")) {
                    mPublisher.getView().setVisibility(View.VISIBLE);
                    mAvatarPublisher.setVisibility(View.GONE);
                } else {
                    mPublisher.getView().setVisibility(View.GONE);
                    mAvatarPublisher.setVisibility(View.VISIBLE);
                }
            }
        }

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
        setEventStatus("L");
        updateEventName();
        if(!mUserIsOnstage) {
            if (mFanStream != null) subscribeFanToStream(mFanStream);
            if (mHostStream != null) subscribeHostToStream(mHostStream);
            if (mCelebrityStream != null) subscribeCelebrityToStream(mCelebrityStream);
            updateViewsWidth();
        }
    }

    private void updateEventName() {
        try {
            mEventName.setText(EventUtils.ellipsize(mEvent.getString("event_name"), 20));
            mEventStatus.setText("(" + getEventStatusName() + ")");
        } catch (JSONException ex) {
            Log.e(LOG_TAG, "updateEventName ---> " + ex.getMessage());
        }
    }

    private void updateEventName(String event_name, String status) {
        mEventName.setText(EventUtils.ellipsize(event_name,20));
        mEventStatus.setText("(" + status +")");
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

    private void setEventStatus(String status) {
        try {
            mEvent.put("status", status);
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }



    private void finishEvent() {

        //Show Event Image end
        mEventImage.setVisibility(View.GONE);
        mEventImageEnd.setVisibility(View.VISIBLE);

        //Hide subscriber containters
        mSubscriberCelebrityViewContainer.setVisibility(View.GONE);
        mSubscriberFanViewContainer.setVisibility(View.GONE);
        mSubscriberHostViewContainer.setVisibility(View.GONE);
        mSubscriberFanViewContainer.displayAvatar(false);
        mSubscriberCelebrityViewContainer.displayAvatar(false);
        mSubscriberHostViewContainer.displayAvatar(false);

        //Hide chat
        mChatButton.setVisibility(View.GONE);
        mFragmentContainer.setVisibility(View.GONE);

        //Hide getinline
        setVisibilityGetInLine(View.GONE);

        if(mUserIsOnstage) {
            disconnectFromOnstage();
        } else {
            //Disconnect the onbackstage session
            disconnectBackstageSession();
        }

        //Disconnect from onstage session
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                disconnectOnStageSession();
            }
        }, 10000);

        setEventStatus("C");

        //Update event name and Status.
        updateEventName();
    }


    /* Connection Listener methods */
    @Override
    public void onConnectionCreated(Session session, Connection connection) {
        if(mProducerConnection == null && connection.getData().equals("usertype=producer") && session.getSessionId().equals(mBackstageSessionId)) {
            mProducerConnection = connection;
        }
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection)
    {
        if(mProducerConnection != null &&
                mProducerConnection.getConnectionId().equals(connection.getConnectionId()) &&
                connection.getData().equals("usertype=producer") &&
                session.getSessionId().equals(mBackstageSessionId)) {

            mProducerConnection = null;
            mNewFanSignalAckd = false;
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

    private Boolean isInLine() {
        return !mGetInLine.getText().equals(getResources().getString(R.string.get_inline));
    }

    public void onGetInLineClicked(View v) {
        if(mGetInLine.getVisibility() == View.GONE) return;
        if(!isInLine()){
            //request Marshmallow camera permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, permsRequestCode);
            }
            else {
                initGetInline();
            }
        } else {
            leaveLine();
        }
    }

    public void setVisibilityGetInLine(int visibility) {
        mGetInLine.setVisibility(visibility);
    }

    public void initGetInline() {
        mOnBackstage = false;
        setVisibilityGetInLine(View.GONE);
        mGetInLine.setText(getResources().getString(R.string.leave_line));
        mGetInLine.setBackground(getResources().getDrawable(R.drawable.leave_line_button));
        if(mBackstageSessionId != null) {

            //Send socket signal
            mSocket.emitJoinRoom(mBackstageSessionId);

            if (mPublisher == null) {

                Log.i(LOG_TAG, "init publisher");
                mPublisher = new Publisher(FanActivity.this, "publisher");
                mPublisher.setPublisherListener(this);
                // use an external custom video renderer
                mCustomVideoRenderer = new CustomVideoRenderer(this);
                mCustomVideoRenderer.setSaveScreenshot(false);
                mPublisher.setRenderer(mCustomVideoRenderer);
                attachPublisherView();
            }

            backstageSessionConnect();
        } else {
            //Recalling after 0.5 secs
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initGetInline();
                }
            }, 500);
        }

    }

    private void sendNewFanSignal() {

        if(mProducerConnection != null && mBackstageSession != null){
            if(!mNewFanSignalAckd) {
                mNewFanSignalAckd = true;
                String userName = IBConfig.USER_NAME;
                String user_id = mWebServiceCoordinator.getUserId();
                String msg = "{\"user\":{\"user_id\":\"" + user_id + "\",\"mobile\":\"true\",\"os\":\"Android\",\"username\":\"" + userName + "\", \"quality\":\"" + mTestQuality + "\"}}";
                mBackstageSession.sendSignal("newFan", msg, mProducerConnection);
            }
        }
    }

    // Initialize a TextChatFragment instance and add it to the UI
    private void loadTextChatFragment(){
        if(mBackstageSession == null || mIsOnPause || mBackstageSession.getConnection() == null) return;
        int containerId = R.id.fragment_textchat_container;
        mFragmentTransaction = getFragmentManager().beginTransaction();
        mTextChatFragment = (TextChatFragment)this.getFragmentManager().findFragmentByTag("TextChatFragment");
        if (mTextChatFragment == null) {
            mTextChatFragment = new TextChatFragment();
            mTextChatFragment.setMaxTextLength(1050);
            mTextChatFragment.setTextChatListener(this);
            mTextChatFragment.setSenderInfo(mBackstageSession.getConnection().getConnectionId(), IBConfig.USER_NAME);

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
            mUnreadCircle.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoQualityUpdated(String connectionId, NetworkTest.MOSQuality quality) {
        if ( mTestSubscriber != null ) {
            Log.i(LOG_TAG, "Video quality sent to the producer: " + getIBQuality(quality));
            sendQualityUpdate(mBackstageConnectionId, getIBQuality(quality));
        }
    }

    @Override
    public void onAudioQualityUpdated(String connectionId, NetworkTest.MOSQuality quality) {
        if (mTestSubscriber != null) {
            //to send quality update to the producer
            Log.i(LOG_TAG, "Audio quality sent to the producer: " + quality.toString());
            //TODO: audio quality to the producer
            //sendQualityUpdate(mTestSubscriber.getStream().getConnection().getConnectionId(), quality.toString());
        }
    }

    private String getIBQuality(NetworkTest.MOSQuality quality){
        String qualityStr = null;

        if (quality.equals(NetworkTest.MOSQuality.Bad) || quality.equals(NetworkTest.MOSQuality.Poor)) {
            qualityStr = "Poor";
        }
        else {
            if (quality.equals(NetworkTest.MOSQuality.Good) || quality.equals(NetworkTest.MOSQuality.Fair)){
                qualityStr = "Good";
            }
            else {
                if (quality.equals(NetworkTest.MOSQuality.Excellent)){
                    qualityStr = "Great";
                }
            }
        }
        return qualityStr;
    }

    private void addLogEvent(String action, String variation){
        if ( mOnStageAnalytics != null ) {
            mOnStageAnalytics.logEvent(action, variation);
        }
    }

    @Override
    public void onRequestPermissionsResult (final int permsRequestCode, final String[] permissions,
                                            int[] grantResults){
        switch (permsRequestCode) {
            case 200:
                boolean video = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean audio = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Permission granted");
            initGetInline();

        } else if(grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
            Log.i(LOG_TAG, "Permission denied");
            boolean audio = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
            boolean video = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1]);

            if(audio || video){
                //user denied without Never ask again, just show rationale explanation
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Denied");
                builder.setMessage("Without this permission the app is unable to get inline.Are you sure you want to deny this permission?");
                builder.setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("RE-TRY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(permissions, permsRequestCode);
                        }
                    }});
                builder.show();
            }else{
                Log.i(LOG_TAG, "user has denied with `Never Ask Again`, go to settings");
                //user has denied with `Never Ask Again`, go to settings
                promptSettings();
            }
        }
    }

    private void promptSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String mDeniedNeverAskTitle = "Unable to get inline";
        String mDeniedNeverAskMsg = "You have denied the permission to get inline. Please go to app settings and allow permission";
        builder.setTitle(mDeniedNeverAskTitle);
        builder.setMessage(mDeniedNeverAskMsg);
        builder.setPositiveButton("go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goToSettings();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(myAppSettings);
    }

    private Emitter.Listener onAbleToJoin = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Boolean ableToJoin;
                    Boolean eventLive;
                    String broadcastId;
                    try {
                        ableToJoin = data.getBoolean("ableToJoin");
                        if(ableToJoin) {
                            Log.i(LOG_TAG, "able to join to interactive!");
                            requestEventData(mEvent);
                        } else {
                            Log.i(LOG_TAG, "not able to join to interactive.");
                            if(data.get("broadcastData") != JSONObject.NULL){
                                mBroadcastData = data.getJSONObject("broadcastData");
                                mHls = true;
                                mSocket.emitJoinBroadcast("broadcast" + mBroadcastData.getString("broadcastId"));
                                mSocket.getSocket().on("eventGoLive", onBroadcastGoLive);
                                mSocket.getSocket().on("eventEnded", onBroadcastEnd);
                                mBroadcastUrl = mBroadcastData.getString("broadcastUrl");
                                eventLive = mBroadcastData.getBoolean("eventLive");
                                if(eventLive) {
                                    startBroadcast();
                                }
                            } else {
                                mNotification.show(R.string.user_limit);
                            }

                        }
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private Emitter.Listener onBroadcastGoLive = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setEventStatus("L");
                            updateEventName();
                            startBroadcast();
                        }
                    }, 15 * 1000);
                }
            });
        }
    };

    private Emitter.Listener onBroadcastEnd = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Show Event Image end
                            mEventImage.setVisibility(View.GONE);
                            mEventImageEnd.setVisibility(View.VISIBLE);
                            mVideoView.stopPlayback();
                            mVideoViewLayout.setVisibility(View.GONE);
                            setEventStatus("C");
                            //Update event name and Status.
                            updateEventName();
                        }
                    }, 15 * 1000);
                }
            });
        }
    };
    
    private void startBroadcast() {
        if(!mBroadcastUrl.equals("")) {
            mEventImage.setVisibility(View.GONE);
            mVideoViewLayout.setVisibility(View.VISIBLE);
            mVideoView.setVideoURI(Uri.parse(mBroadcastUrl));
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return false;
                }

            });

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                @Override
                public void onPrepared(MediaPlayer m) {
                    mHls = true;
                    mVideoViewProgressBar.setVisibility(View.GONE);
                }
            });
            mVideoView.start();
        }
    }

    private void resumeBroadcast() {
        if(mVideoViewLayout.getVisibility() == View.VISIBLE) {
            mVideoView.start();
        }
    }
}