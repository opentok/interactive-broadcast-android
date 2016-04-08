package com.tokbox.android.IB;


import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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
import com.tokbox.android.IB.services.ClearNotificationService;
import com.tokbox.android.IB.ws.WebServiceCoordinator;

import org.json.JSONException;
import org.json.JSONObject;


public class CelebrityHostActivity extends AppCompatActivity implements WebServiceCoordinator.Listener,

        Session.SessionListener, Session.ConnectionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener,
        Session.SignalListener,Subscriber.VideoListener,
        TextChatFragment.TextChatListener{

    private static final String LOG_TAG = CelebrityHostActivity.class.getSimpleName();
    private JSONObject mEvent;
    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private Session mSession;
    private WebServiceCoordinator mWebServiceCoordinator;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private Subscriber mSubscriberFan;
    private Subscriber mSubscriberProducer;
    private Stream mCelebirtyStream;
    private Stream mFanStream;
    private Stream mHostStream;
    private Stream mProducerStream;
    private Connection mProducerConnection;
    private TextView mTextUnreadMessages;
    private TextView mEventName;
    private TextView mEventStatus;
    private TextView mGoLiveStatus;
    private TextView mGoLiveNumber;
    private TextView mUserStatus;
    private ImageButton mChatButton;
    private Button mLiveButton;
    private ImageView mEventImageEnd;
    private ImageButton mUnreadCircle;
    private RelativeLayout mAvatarHostCelebrity;
    private RelativeLayout mAvatarFan;
    private RelativeLayout mAvatarPublisher;


    private Handler mHandler = new Handler();
    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberViewContainer;
    private RelativeLayout mSubscriberFanViewContainer;
    private FrameLayout mFragmentContainer;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSub;
    private ProgressBar mLoadingSubPublisher;
    private ProgressBar mLoadingSubFan;
    private boolean resumeHasRun = false;
    private boolean mIsBound = false;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;

    private TextChatFragment mTextChatFragment;
    private FragmentTransaction mFragmentTransaction;
    private boolean msgError = false;

    private int mUnreadMessages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_celebrity_host);


        //Hide the bar
        getSupportActionBar().hide();

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        initLayoutWidgets();

        //Get the event
        requestEventData(savedInstanceState);

        //Disable HWDEC
        OpenTokConfig.enableVP8HWDecoder(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_celebrity_host, menu);
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
        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
        mSubscriberFanViewContainer = (RelativeLayout) findViewById(R.id.subscriberviewfan);

        mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);
        mLoadingSubPublisher = (ProgressBar) findViewById(R.id.loadingSpinnerPublisher);
        mLoadingSubFan = (ProgressBar) findViewById(R.id.loadingSpinnerFan);
        mTextUnreadMessages = (TextView) findViewById(R.id.unread_messages);
        mChatButton = (ImageButton) findViewById(R.id.chat_button);
        mLiveButton = (Button) findViewById(R.id.live_button);
        mEventName = (TextView) findViewById(R.id.event_name);
        mEventStatus = (TextView) findViewById(R.id.event_status);
        mGoLiveStatus = (TextView) findViewById(R.id.go_live_status);
        mGoLiveNumber = (TextView) findViewById(R.id.go_live_number);
        mUserStatus = (TextView) findViewById(R.id.user_status);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_textchat_container);
        mEventImageEnd = (ImageView) findViewById(R.id.event_image_end);
        mUnreadCircle = (ImageButton) findViewById(R.id.unread_circle);
        mAvatarPublisher = (RelativeLayout) findViewById(R.id.avatar_publisher);
        mAvatarFan = (RelativeLayout) findViewById(R.id.avatar_fan);
        mAvatarHostCelebrity = (RelativeLayout) findViewById(R.id.avatar_hostceleb);
    }

    private void requestEventData (Bundle savedInstanceState) {
        mLoadingSubPublisher.setVisibility(View.VISIBLE);
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
            EventUtils.loadEventImage(this, event.getString("event_image_end"), mEventImageEnd);
            if(IBConfig.USER_TYPE == "celebrity") {
                mWebServiceCoordinator.createCelebrityToken(event.getString("celebrity_url"));
            } else {
                mWebServiceCoordinator.createHostToken(event.getString("host_url"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - getInstanceById", e);
        }

    }

    private void updateEventName() {
        try {
            mEventName.setText(EventUtils.ellipsize(mEvent.getString("event_name"), 40));
            mEventStatus.setText("(" + getEventStatusName() + ")");
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    private String getEventStatusName() {
        return EventUtils.getStatusNameById(getEventStatus());
    }

    private void updateEventName(String event_name, String status) {
        mEventName.setText(EventUtils.ellipsize(event_name,40));
        mEventStatus.setText("(" + status + ")");
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
        Toast.makeText(getApplicationContext(),"Unable to connect to the server. Please try in a few minutes.", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onPause() {
        super.onPause();

        if (mSession != null) {
            mSession.onPause();

            if (mSubscriber != null) {
                mSubscriberViewContainer.removeView(mSubscriber.getView());
            }

            if (mSubscriberFan != null) {
                mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            }
        }

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getTitle())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(getResources().getString(R.string.notification));

        Intent notificationIntent = new Intent(this, CelebrityHostActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        mNotifyBuilder.setContentIntent(intent);
        if (mConnection == null) {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    ((ClearNotificationService.ClearBinder) binder).service.startService(new Intent(CelebrityHostActivity.this, ClearNotificationService.class));
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
            bindService(new Intent(CelebrityHostActivity.this,
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

        if (isFinishing()) {
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            if (mSession != null) {
                mSession.disconnect();
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

        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {

        if(mFragmentContainer.getVisibility() == View.VISIBLE) {
            toggleChat();
        }else {
            if (mSession != null) {
                mSession.disconnect();
            }

            if (mIsBound) {
                unbindService(mConnection);
                mIsBound = false;
            }
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);

            super.onBackPressed();
        }
    }

    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriber != null) {
                    attachSubscriberView();
                }
                if (mSubscriberFan != null) {
                    attachSubscriberFanView();
                }
            }
        }, 500);
    }

    public void updateViewsWidth() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                int streams = 1;
                if (mFanStream != null) streams++;
                if (mCelebirtyStream != null || mHostStream != null) streams++;

                RelativeLayout.LayoutParams publisher_head_params = (RelativeLayout.LayoutParams) mPublisherViewContainer.getLayoutParams();
                publisher_head_params.width = screenWidth(CelebrityHostActivity.this) / streams;
                mPublisherViewContainer.setLayoutParams(publisher_head_params);

                RelativeLayout.LayoutParams subscriber_head_params = (RelativeLayout.LayoutParams) mSubscriberViewContainer.getLayoutParams();
                subscriber_head_params.width = (mCelebirtyStream != null || mHostStream != null) ? screenWidth(CelebrityHostActivity.this) / streams : 1;
                mSubscriberViewContainer.setLayoutParams(subscriber_head_params);

                RelativeLayout.LayoutParams subscriberfan_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                subscriberfan_head_params.width = (mFanStream != null) ? screenWidth(CelebrityHostActivity.this) / streams : 1;
                mSubscriberFanViewContainer.setLayoutParams(subscriberfan_head_params);
            }
        });
    }



    private void sessionConnect() {
        if (mSession == null) {
            mSession = new Session(CelebrityHostActivity.this,
                    mApiKey, mSessionId);
            mSession.setSessionListener(this);
            mSession.setSignalListener(this);
            mSession.setConnectionListener(this);
            mSession.connect(mToken);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Connected to the session.");
        if (mPublisher == null) {
            mPublisher = new Publisher(CelebrityHostActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            attachPublisherView();
            mSession.publish(mPublisher);
        }
        //loading text-chat ui component
        loadTextChatFragment();
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
            mTextChatFragment.setSenderInfo(mSession.getConnection().getConnectionId(), "Me");

            mFragmentTransaction.add(containerId, mTextChatFragment, "TextChatFragment").commit();
            mFragmentContainer.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onMessageReadyToSend(ChatMessage msg) {
        Log.d(LOG_TAG, "TextChat listener: onMessageReadyToSend: " + msg.getText());

        if (mSession != null && mProducerConnection != null) {
            String message = "{\"message\":{\"to\":{\"connectionId\":\"" + mProducerConnection.getConnectionId()+"\"}, \"message\":\""+msg.getText()+"\"}}";
            mSession.sendSignal("chatMessage", message, mProducerConnection);
        }
        return msgError;
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Disconnected from the session.");
        cleanViews();
    }

    public void cleanViews() {
        if (mPublisher != null) {
            mPublisherViewContainer.removeView(mPublisher.getView());
        }

        if (mSubscriber != null) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
        }

        if (mSubscriberFan != null) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        }

        mPublisher = null;
        mSubscriber = null;
        mSubscriberFan = null;
        mCelebirtyStream = null;
        mFanStream = null;
        mHostStream = null;
        mSession = null;
    }

    private void subscribeToStream(Stream stream) {
        Log.i(LOG_TAG, "subscribeToStream");
        Log.i(LOG_TAG, "Subscriber is null? = " + ((mSubscriber==null) ? "Yes" : "No"));
        mSubscriber = new Subscriber(CelebrityHostActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);

        if (stream.hasVideo()) {
            // start loading spinning
            mLoadingSub.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeFanToStream(Stream stream) {
        Log.i(LOG_TAG, "subscribeFanToStream");
        mSubscriberFan = new Subscriber(CelebrityHostActivity.this, stream);
        mSubscriberFan.setVideoListener(this);
        mSession.subscribe(mSubscriberFan);

        if (stream.hasVideo()) {
            // start loading spinning
            mLoadingSubFan.setVisibility(View.VISIBLE);
        }
    }

    private void unsubscribeFromStream(Stream stream) {
        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSession.unsubscribe(mSubscriber);
            mSubscriber = null;
        }
    }

    private void unsubscribeFanFromStream(Stream stream) {
        if (mSubscriberFan.getStream().equals(stream)) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            mSession.unsubscribe(mSubscriberFan);
            mSubscriberFan = null;
        }
    }

    private void attachSubscriberView() {
        mSubscriberViewContainer.removeView(mSubscriber.getView());
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        mSubscriberViewContainer.addView(mSubscriber.getView());
    }

    private void attachSubscriberFanView() {
        mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
        mSubscriberFan.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        mSubscriberFanViewContainer.addView(mSubscriberFan.getView());
    }

    private void attachPublisherView() {
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
        mPublisherViewContainer.addView(mPublisher.getView());
    }

    @Override
    public void onError(Session session, OpentokError exception) {
        Log.i(LOG_TAG, "Session exception: " + exception.getMessage());
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "onStreamReceived:" + stream.getConnection().getData());
        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if (mFanStream == null) {
                    subscribeFanToStream(stream);
                    mFanStream = stream;
                    updateViewsWidth();
                }
                break;
            case "usertype=celebrity":
                Log.i(LOG_TAG, "Celebrity!");
                if (mCelebirtyStream == null) {
                    subscribeToStream(stream);
                    mCelebirtyStream = stream;
                    updateViewsWidth();
                }
                break;
            case "usertype=host":
                if (mHostStream == null) {
                    subscribeToStream(stream);
                    mHostStream = stream;
                    updateViewsWidth();
                }
                break;
            case "usertype=producer":
                if(mProducerStream == null && session.getSessionId().equals(mSessionId)){
                    Log.i(LOG_TAG, "producer stream in");
                    mProducerStream = stream;
                }
                break;
        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "New stream dropped:" + stream.getConnection().getData());
        String streamConnectionId = stream.getConnection().getConnectionId();
        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if(mFanStream != null && mFanStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    unsubscribeFanFromStream(stream);
                    mFanStream = null;
                    updateViewsWidth();
                }
                break;
            case "usertype=celebrity":
                if(mCelebirtyStream != null && mCelebirtyStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    unsubscribeFromStream(stream);
                    mCelebirtyStream = null;
                    updateViewsWidth();
                }
                break;
            case "usertype=host":
                if(mHostStream!= null && mHostStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    unsubscribeFromStream(stream);
                    mHostStream = null;
                    updateViewsWidth();
                }
            case "usertype=producer":
                if(mProducerStream != null && mProducerStream.getConnection().getConnectionId().equals(streamConnectionId)) {
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
        updateViewsWidth();
    }

    public static int screenWidth(Context ctx) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisher, Stream stream) {
        Log.i(LOG_TAG, "Publisher destroyed");
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOG_TAG, "Publisher exception: " + exception.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        if(subscriber.getStream().getConnection().getData().equals("usertype=fan")) {
            // stop loading spinning
            mLoadingSubFan.setVisibility(View.GONE);
            attachSubscriberFanView();
        } else {
            // stop loading spinning
            mLoadingSub.setVisibility(View.GONE);
            attachSubscriberView();
        }

    }


    private void showAvatar(String subscriberConnectionId) {

        String hostCeleb = mSubscriber != null ? mSubscriber.getStream().getConnection().getConnectionId() : "";
        String fan = mSubscriberFan != null ? mSubscriberFan.getStream().getConnection().getConnectionId() : "";
        if(subscriberConnectionId.equals(hostCeleb)) {
            mSubscriber.getView().setVisibility(View.GONE);
            mAvatarHostCelebrity.setVisibility(View.VISIBLE);
        }
        if(subscriberConnectionId.equals(fan)) {
            mSubscriberFan.getView().setVisibility(View.GONE);
            mAvatarFan.setVisibility(View.VISIBLE);
        }
    }

    private void hideAvatar(String subscriberConnectionId) {

        String hostCeleb = mSubscriber != null ? mSubscriber.getStream().getConnection().getConnectionId() : "";
        String fan = mSubscriberFan != null ? mSubscriberFan.getStream().getConnection().getConnectionId() : "";
        if(subscriberConnectionId.equals(hostCeleb)) {
            mSubscriber.getView().setVisibility(View.VISIBLE);
            mAvatarHostCelebrity.setVisibility(View.GONE);
        }
        if(subscriberConnectionId.equals(fan)) {
            mSubscriberFan.getView().setVisibility(View.VISIBLE);
            mAvatarFan.setVisibility(View.GONE);
        }
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG,
                "Video disabled:" + reason);
        showAvatar(subscriber.getStream().getConnection().getConnectionId());
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG, "Video enabled:" + reason);
        hideAvatar(subscriber.getStream().getConnection().getConnectionId());
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriber) {

        Log.i(LOG_TAG, "Video may be disabled soon due to network quality degradation. Add UI handling here." + subscriber.getStream().getConnection().getData());
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "Video may no longer be disabled as stream quality improved. Add UI handling here.");
        hideAvatar(subscriber.getStream().getConnection().getConnectionId());
    }

    /* Subscriber Listener methods */

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        //Log.i(LOG_TAG, "Subscriber Connected");
        if(subscriberKit.getStream().getConnection().getData() == "usertype=fan") {
            mSubscriberFanViewContainer.addView(mSubscriberFan.getView());
        } else {
            mSubscriberViewContainer.addView(mSubscriber.getView());
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
                case "newBackstageFan":
                    newBackstageFan();
                    break;
                case "privateCall":
                    startPrivateCall(data);
                    break;
                case "endPrivateCall":
                    endPrivateCall();
                    break;

            }
        }
        //TODO: onChangeVolumen

    }

    private void startPrivateCall(String data) {
        String connectionId = "";
        try {
            connectionId = new JSONObject(data).getString("callWith");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }
        if(mSubscriber != null) mSubscriber.setSubscribeToAudio(false);
        if(mPublisher.getStream().getConnection().getConnectionId().equals(connectionId)) {
            subscribeProducer();
        }
    }

    private void endPrivateCall() {
        if(mSubscriber != null) mSubscriber.setSubscribeToAudio(true);
        if(mSubscriberProducer != null) {
            mSession.unsubscribe(mSubscriberProducer);
            mSubscriberProducer = null;
        }
    }

    private void subscribeProducer() {
        if(mProducerStream != null) {
            mSubscriberProducer = new Subscriber(CelebrityHostActivity.this, mProducerStream);
            mSession.subscribe(mSubscriberProducer);
        }
    }

    private void newBackstageFan() {
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
    }

    public void handleNewMessage(String data, Connection connection) {
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
        android.view.ViewGroup.LayoutParams params = mUnreadCircle.getLayoutParams();
        Resources r = getResources();
        int newWidth = 0;
        newWidth = mUnreadMessages > 9 ? 35:27;
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newWidth, r.getDisplayMetrics());
        mUnreadCircle.setLayoutParams(params);
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

        if(video.equals("on")) {
            mPublisher.getView().setVisibility(View.VISIBLE);
            mAvatarPublisher.setVisibility(View.GONE);
        } else {
            mPublisher.getView().setVisibility(View.GONE);
            mAvatarPublisher.setVisibility(View.VISIBLE);
        }

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
            showCountDown();
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    private void showCountDown() {
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
                mLiveButton.setVisibility(View.VISIBLE);
            }
        }, 5000);
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

    public void finishEvent() {
        //Show Event Image end
        mEventImageEnd.setVisibility(View.VISIBLE);
        mLiveButton.setVisibility(View.GONE);

        //Hide subscriber containters
        mSubscriberViewContainer.setVisibility(View.GONE);
        mSubscriberFanViewContainer.setVisibility(View.GONE);

        //Hide avatars
        mAvatarFan.setVisibility(View.GONE);
        mAvatarHostCelebrity.setVisibility(View.GONE);
        mAvatarPublisher.setVisibility(View.GONE);

        //Unpublish
        mSession.unpublish(mPublisher);

        //Hide chat
        hideChat();

        //Remove publisher
        mPublisherViewContainer.removeView(mPublisher.getView());
        mPublisher.destroy();

        updateViewsWidth();

        //Hide chat
        mChatButton.setVisibility(View.GONE);
        //mFragmentContainer.setVisibility(View.GONE);

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
            mChatButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection)
    {
        if(connection.getData().equals("usertype=producer")) {
            mProducerConnection = null;
            mChatButton.setVisibility(View.GONE);
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
            Log.i(LOG_TAG, "Open chat!");
            mFragmentContainer.setVisibility(View.VISIBLE);
            mUnreadMessages = 0;
            refreshUnreadMessages();
        }
    }

    @Override
    public void hideChat() {
        mFragmentContainer.setVisibility(View.GONE);
        if(mSession == null) {
            mChatButton.setVisibility(View.GONE);
        }
    }


}
