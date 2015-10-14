package com.agilityfeat.spotlight;


import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.agilityfeat.spotlight.config.SpotlightConfig;
import com.agilityfeat.spotlight.model.InstanceApp;
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
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;



public class FanActivity extends AppCompatActivity implements WebServiceCoordinator.Listener,

        Session.SessionListener, Session.ConnectionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener,
        Session.SignalListener,Subscriber.VideoListener{

    private static final String LOG_TAG = FanActivity.class.getSimpleName();
    private JSONObject mEvent;
    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private String mBackstageSessionId;
    private String mBackstageToken;
    private Session mSession;
    private Session mBackstageSession;
    private WebServiceCoordinator mWebServiceCoordinator;
    private Publisher mPublisher;
    private Subscriber mSubscriberHost;
    private Subscriber mSubscriberCelebrity;
    private Subscriber mSubscriberFan;
    private Subscriber mSubscriberProducer;
    private Stream mCelebirtyStream;
    private Stream mFanStream;
    private Stream mHostStream;
    private Stream mProducerStream;
    private Connection mProducerConnection;
    private ScrollView mScroller;
    private RelativeLayout mMessageBox;
    private EditText mMessageEditText;
    private TextView mMessageView;
    private TextView mEventName;
    private ImageButton mChatButton;
    private ImageView mEventImage;
    private ImageView mEventImageEnd;
    private Button mGetInLine;
    private EditText mUsername;

    private Handler mHandler = new Handler();
    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberHostViewContainer;
    private RelativeLayout mSubscriberCelebrityViewContainer;
    private RelativeLayout mSubscriberFanViewContainer;
    private RelativeLayout mGetInLineView;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSubCelebrity;
    private ProgressBar mLoadingSubHost;
    private ProgressBar mLoadingSubPublisher;
    private ProgressBar mLoadingSubFan;
    private boolean resumeHasRun = false;
    private boolean mIsBound = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fan);

        //Hide the bar
        //getSupportActionBar().hide();

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
        mGetInLineView = (RelativeLayout) findViewById(R.id.get_inline_view);

        mMessageBox = (RelativeLayout) findViewById(R.id.messagebox);
        mLoadingSubCelebrity = (ProgressBar) findViewById(R.id.loadingSpinnerCelebrity);
        mLoadingSubHost = (ProgressBar) findViewById(R.id.loadingSpinnerHost);
        mLoadingSubFan = (ProgressBar) findViewById(R.id.loadingSpinnerFan);
        mLoadingSubPublisher = (ProgressBar) findViewById(R.id.loadingSpinnerPublisher);
        mScroller = (ScrollView) findViewById(R.id.scroller);
        mMessageEditText = (EditText) findViewById(R.id.message);
        mMessageView = (TextView) findViewById(R.id.messageView);
        mEventName = (TextView) findViewById(R.id.event_name);
        mEventImageEnd = (ImageView) findViewById(R.id.event_image_end);
        mEventImage = (ImageView) findViewById(R.id.event_image);
        mChatButton = (ImageButton) findViewById(R.id.chat_button);
        mGetInLine = (Button) findViewById(R.id.btn_getinline);
        mUsername = (EditText) findViewById(R.id.user_name);
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
            updateEventName(event.getString("event_name"), getStatusNameById(event.getString("status")));
            loadEventImage(event.getString("event_image"), mEventImage);
            loadEventImage(event.getString("event_image_end"), mEventImageEnd);
            mWebServiceCoordinator.createFanToken(event.getString("fan_url"));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - getInstanceById", e);
        }

    }

    public void loadEventImage(String image, ImageView imgView) {
        image = image.equals("") ? SpotlightConfig.eventImageDefault : SpotlightConfig.FRONTEND_URL + image;
        Picasso.with(this).load(image).into(imgView);
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
            mBackstageSession.onPause();

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

        if(mScroller.getVisibility() == View.VISIBLE) {
            toggleChat();
        }else {
            if (mSession != null) {
                mSession.disconnect();
            }

            if (mBackstageSession != null) {
                mBackstageSession.disconnect();
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
                if (mFanStream != null) streams++;
                if (mCelebirtyStream != null) streams++;
                if (mHostStream != null) streams++;

                if (streams > 0) {
                    Log.i(LOG_TAG, "more streams than 0");
                    mEventImage.setVisibility(View.GONE);

                    RelativeLayout.LayoutParams celebrity_head_params = (RelativeLayout.LayoutParams) mSubscriberCelebrityViewContainer.getLayoutParams();
                    celebrity_head_params.width = screenWidth(FanActivity.this) / streams;
                    mSubscriberCelebrityViewContainer.setLayoutParams(celebrity_head_params);

                    RelativeLayout.LayoutParams host_head_params = (RelativeLayout.LayoutParams) mSubscriberHostViewContainer.getLayoutParams();
                    host_head_params.width = screenWidth(FanActivity.this) / streams;
                    mSubscriberHostViewContainer.setLayoutParams(host_head_params);

                    RelativeLayout.LayoutParams fan_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                    fan_head_params.width = screenWidth(FanActivity.this) / streams;
                    mSubscriberFanViewContainer.setLayoutParams(fan_head_params);


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

        //Start publishing in backstage session
        if(session.getSessionId().equals(mBackstageSessionId)) {
            mBackstageSession.publish(mPublisher);

        }
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Disconnected from the session.");
        if(session.getSessionId().equals(mSessionId)) {
            cleanViews();
        }
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

        mPublisher = null;
        mSubscriberCelebrity = null;
        mSubscriberHost = null;
        mSubscriberFan = null;
        mCelebirtyStream = null;
        mFanStream = null;
        mHostStream = null;
        mSession = null;
    }

    private void subscribeHostToStream(Stream stream) {

        mSubscriberHost = new Subscriber(FanActivity.this, stream);
        mSubscriberHost.setVideoListener(this);
        mSession.subscribe(mSubscriberHost);

        if (mSubscriberHost.getSubscribeToVideo()) {
            // start loading spinning
            mLoadingSubHost.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeCelebrityToStream(Stream stream) {

        mSubscriberCelebrity = new Subscriber(FanActivity.this, stream);
        mSubscriberCelebrity.setVideoListener(this);
        mSession.subscribe(mSubscriberCelebrity);

        if (mSubscriberCelebrity.getSubscribeToVideo()) {
            // start loading spinning
            mLoadingSubCelebrity.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeFanToStream(Stream stream) {

        mSubscriberFan = new Subscriber(FanActivity.this, stream);
        mSubscriberFan.setVideoListener(this);
        mSession.subscribe(mSubscriberFan);

        if (mSubscriberFan.getSubscribeToVideo()) {
            // start loading spinning
            mLoadingSubFan.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeProducer() {
        if(mProducerStream != null) {
            mSubscriberProducer = new Subscriber(FanActivity.this, mProducerStream);
            mBackstageSession.subscribe(mSubscriberProducer);
        }
    }

    private void unSubscribeProducer() {
        if (mProducerStream!= null && mSubscriberProducer != null) {
            mBackstageSession.unsubscribe(mSubscriberProducer);
            mSubscriberProducer = null;
        }
    }

    private void unsubscribeHostFromStream(Stream stream) {
        if (mSubscriberHost.getStream().equals(stream)) {
            mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
            mSubscriberHost = null;
            mLoadingSubHost.setVisibility(View.GONE);
        }
    }

    private void unsubscribeCelebirtyFromStream(Stream stream) {
        if (mSubscriberCelebrity.getStream().equals(stream)) {
            mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
            mSubscriberCelebrity = null;
            mLoadingSubCelebrity.setVisibility(View.GONE);
        }
    }

    private void unsubscribeFanFromStream(Stream stream) {
        if (mSubscriberFan.getStream().equals(stream)) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            mSubscriberFan = null;
            mLoadingSubFan.setVisibility(View.GONE);
        }
    }

    private void attachSubscriberHostView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberHostViewContainer.removeView(mSubscriberHost.getView());
        mSubscriberHostViewContainer.addView(mSubscriberHost.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachSubscriberCelebrityView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberCelebrityViewContainer.removeView(mSubscriberCelebrity.getView());
        mSubscriberCelebrityViewContainer.addView(mSubscriberCelebrity.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachSubscriberFanView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        mSubscriberFanViewContainer.addView(mSubscriberFan.getView(), layoutParams);
        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void attachPublisherView(Publisher publisher) {

        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        mPublisherViewContainer.addView(mPublisher.getView(), layoutParams);
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
                if (mFanStream == null) {
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
                    if(status.equals("L")) {
                        subscribeCelebrityToStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=host":
                if (mHostStream == null) {
                    mHostStream = stream;
                    if(status.equals("L")) {

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
                if(mFanStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    mFanStream = null;
                    if(status.equals("L")) {
                        unsubscribeFanFromStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=celebrity":
                if(mCelebirtyStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {

                    mCelebirtyStream = null;
                    if(status.equals("L")) {
                        unsubscribeCelebirtyFromStream(stream);
                        updateViewsWidth();
                    }
                }
                break;
            case "usertype=host":
                if(mHostStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    mHostStream = null;
                    if(status.equals("L")) {
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
        // stop loading spinning
        mLoadingSubPublisher.setVisibility(View.GONE);
        mPublisherViewContainer.bringToFront();
        //updateViewsWidth();
    }

    public static int screenWidth(Context ctx) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {

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
                case "goLive":
                    goLive();
                    break;
                case "finishEvent":
                    finishEvent();
                    break;
                //backstage
                case "resendNewFanSignal":
                    sendNewFanSignal();
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
                case "disconnect":
                    disconnectFromOnstage();
                    break;

            }
        }
        //TODO: onChangeVolumen


    }

    public void connectWithOnstage() {
        //mUserIsOnstage = true;
        mBackstageSession.unpublish(mPublisher);
        mSession.publish(mPublisher);

        //show publisher in fan container
        if(mFanStream != null) subscribeFanToStream(mFanStream);
        if(mHostStream != null) subscribeHostToStream(mHostStream);
        if(mCelebirtyStream != null) subscribeCelebrityToStream(mCelebirtyStream);
        updateViewsWidth();
    }

    public void disconnectFromOnstage() {
        //mUserIsOnstage = false;

        //Unpublish
        mSession.unpublish(mPublisher);
        //Hide chat
        mScroller.setVisibility(View.GONE);
        mMessageBox.setVisibility(View.GONE);
        //Disconnect from backstage
        mBackstageSession.disconnect();

        //Remove publisher
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.destroy();

        updateViewsWidth();

        Toast.makeText(getApplicationContext(), "Thank you for participating, you are no longer sharing video/voice. You can continue to watch the session at your leisure.", Toast.LENGTH_LONG).show();

    }

    public void handleNewMessage(String data, Connection connection) {
        mChatButton.setVisibility(View.VISIBLE);
        String mycid = mSession.getConnection().getConnectionId();
        String cid = connection.getConnectionId();
        String who = "";
        if (!cid.equals(mycid)) {
            String message = "";
            try {
                message = new JSONObject(data)
                        .getJSONObject("message")
                        .getString("message");
            } catch (Throwable t) {
                Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
            }
            presentMessage("Producer", message);
        }
    }

    public void videoOnOff(String data){
        String video="";
        try {
            video = new JSONObject(data)
                    .getString("video");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }
        mPublisher.setPublishVideo(video.equals("on"));

    }

    public void muteAudio(String data){
        String mute="";
        try {
            mute = new JSONObject(data)
                    .getString("mute");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }
        mPublisher.setPublishAudio(!mute.equals("on"));
    }

    public void goLive(){
        try {
            mEvent.put("status", "L");
            updateEventName();

            if(mFanStream != null) subscribeFanToStream(mFanStream);
            if(mHostStream != null) subscribeHostToStream(mHostStream);
            if(mCelebirtyStream != null) subscribeCelebrityToStream(mCelebirtyStream);
            updateViewsWidth();

        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    public void updateEventName() {
        try {
            mEventName.setText(mEvent.getString("event_name") + ": " + getEventStatusName());
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    public void updateEventName(String event_name, String status) {
        mEventName.setText(event_name + ": " + status);
    }

    public String getEventStatusName() {
        return getStatusNameById(getEventStatus());
    }

    public String getEventStatus() {
        String status = "N";
        try {
            status = mEvent.getString("status");
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        return status;
    }

    public String getStatusNameById(String statusId) {
        String statusName = "";
        switch(statusId) {
            case "N":
            case "P":
                statusName = "Preshow";
                break;
            case "L":
                statusName = "Live";
                break;
            case "C":
                statusName = "Closed";
                break;
        }
        return statusName;
    }

    public void finishEvent() {
        //Disconnect the session
        if (mSession != null) {
            mSession.disconnect();
        }

        if (mBackstageSession != null) {
            mBackstageSession.disconnect();
        }
        //Hide chat
        mScroller.setVisibility(View.GONE);
        mMessageBox.setVisibility(View.GONE);
        mChatButton.setVisibility(View.GONE);

        //Hide getinline
        mGetInLine.setVisibility(View.GONE);

        try {
            //Change status
            mEvent.put("status", "C");
            //Show Event Image
            if(!mEvent.getString("event_image_end").equals("")) {
                mEventImageEnd.setVisibility(View.VISIBLE);
            } else {
                mEventImage.setVisibility(View.VISIBLE);
            }
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

    public void toggleChat() {
        if(mScroller.getVisibility() == View.VISIBLE) {
            mScroller.setVisibility(View.GONE);
            mMessageBox.setVisibility(View.GONE);
        } else {
            mScroller.setVisibility(View.VISIBLE);
            mMessageBox.setVisibility(View.VISIBLE);
        }
    }

    public void onClickSend(View v) {
        if (mMessageEditText.getText().toString().compareTo("") == 0) {
            Log.i(LOG_TAG, "Cannot Send - Empty String Message");
        } else {
            Log.i(LOG_TAG, "Sending a chat message");
            sendChatMessage(mMessageEditText.getText().toString());
            mMessageEditText.setText("");
        }
    }

    public void sendChatMessage(String message) {
        sendSignal("chatMessage", message);
        presentMessage("Me", message);
    }

    public void sendSignal(String type, String msg) {
        if(mProducerConnection != null) {
            msg = "{\"message\":{\"to\":{\"connectionId\":\"" + mProducerConnection.getConnectionId()+"\"}, \"message\":\""+msg+"\"}}";
            mBackstageSession.sendSignal(type, msg,mProducerConnection);
        }

    }

    private void presentMessage(String who, String message) {
        presentText("\n" + who + ": " + message);
    }

    private void presentText(String message) {
        mMessageView.setText(mMessageView.getText() + message);
        mScroller.post(new Runnable() {
            @Override
            public void run() {
                int totalHeight = mMessageView.getHeight();
                mScroller.smoothScrollTo(0, totalHeight);
            }
        });
    }


    public void onGetInLineClicked(View v) {
        //if(mGetInLine.getText().equals(R.string.get_inline)){
            mGetInLineView.setVisibility(View.VISIBLE);
            if (mPublisher == null) {
                Log.i(LOG_TAG, "init publisher");
                mPublisher = new Publisher(FanActivity.this, "publisher");
                mPublisher.setPublisherListener(this);
                attachPublisherView(mPublisher);
            }
        //}
    }

    public void initGetInline(View v) {
        mGetInLineView.setVisibility(View.GONE);
        mLoadingSubPublisher.setVisibility(View.VISIBLE);
        backstageSessionConnect();
    }

    public void cancelGetInline(View v) {
        mGetInLineView.setVisibility(View.GONE);
    }

    public void sendNewFanSignal() {

        if(mProducerConnection != null){
            //TODO: add quality test
            String msg = "{\"user\":{\"username\":\"" + mUsername.getText() +"\", \"quality\":\"" + "Great" + "\"}}";
            mBackstageSession.sendSignal("newFan", msg,mProducerConnection);
        }



    }
}

