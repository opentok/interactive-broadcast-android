package com.tokbox.android.IB;


import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import com.tokbox.android.IB.events.ActiveBroadcast;
import com.tokbox.android.IB.events.ActiveFan;
import com.tokbox.android.IB.events.EventProperties;
import com.tokbox.android.IB.events.EventRole;
import com.tokbox.android.IB.events.EventStatus;
import com.tokbox.android.IB.events.EventUtils;
import com.tokbox.android.IB.events.PrivateCall;
import com.tokbox.android.IB.model.InstanceApp;
import com.tokbox.android.IB.services.ClearNotificationService;
import com.tokbox.android.IB.ws.WebServiceCoordinator;
import com.tokbox.android.IB.common.Notification;

import org.json.JSONException;
import org.json.JSONObject;

import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import com.tokbox.android.IB.logging.OTKAction;
import com.tokbox.android.IB.logging.OTKVariation;

import com.tokbox.android.IB.ui.CustomViewSubscriber;

import java.util.UUID;

import static com.tokbox.android.IB.common.Notification.TYPE.TEMPORARILLY_MUTED;


public class CelebrityHostActivity extends AppCompatActivity implements WebServiceCoordinator.Listener,
        Session.SessionListener, Session.ConnectionListener, Session.ReconnectionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener,
        Session.SignalListener,Subscriber.VideoListener,
        TextChatFragment.TextChatListener{

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    private final int permsRequestCode = 200;
    private static final String LOG_TAG = CelebrityHostActivity.class.getSimpleName();
    private JSONObject mEvent;
    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private Session mSession;
    private WebServiceCoordinator mWebServiceCoordinator;
    private Notification mNotification;
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
    private TextView mWarningAlert;
    private ImageButton mChatButton;
    private Button mLiveButton;
    private ImageView mEventImageEnd;
    private ImageButton mUnreadCircle;

    private Boolean mAllowPublish = true;

    private Handler mHandler = new Handler();
    private CustomViewSubscriber mPublisherViewContainer;
    private CustomViewSubscriber mSubscriberViewContainer;
    private CustomViewSubscriber mSubscriberFanViewContainer;
    private FrameLayout mFragmentContainer;
    private RelativeLayout mStatusBar;

    private ProgressDialog mReconnectionsDialog;

    // Spinning wheel for loading subscriber view
    private boolean resumeHasRun = false;
    private boolean mIsBound = false;
    private boolean mUserIsCelebrity;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private ServiceConnection mConnection;

    private TextChatFragment mTextChatFragment;
    private FragmentTransaction mFragmentTransaction;
    private boolean msgError = false;
    private int mUnreadMessages = 0;

    private OTKAnalyticsData mOnStageAnalyticsData;
    private OTKAnalytics mOnStageAnalytics;

    private String mLogSource;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;


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

        setContentView(R.layout.activity_celebrity_host);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        // Make sure we're connected to firebase
        mDatabase.goOnline();

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mUserIsCelebrity = IBConfig.USER_TYPE.equals(EventRole.CELEBRITY);

        initLayoutWidgets();

        //Get the selected event from the instance
        getSelectedEvent(savedInstanceState);

        //Request a token and the event data
        requestEventData();

        //Set event name and images
        setEventUI();

        //Disable HWDEC
        OpenTokConfig.enableVP8HWDecoder(false);

        mNotification = new Notification(this, mStatusBar);
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
        mPublisherViewContainer = (CustomViewSubscriber) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (CustomViewSubscriber) findViewById(R.id.subscriberview);
        mSubscriberFanViewContainer = (CustomViewSubscriber) findViewById(R.id.subscriberviewfan);

        mTextUnreadMessages = (TextView) findViewById(R.id.unread_messages);
        mChatButton = (ImageButton) findViewById(R.id.chat_button);
        mLiveButton = (Button) findViewById(R.id.live_button);
        mEventName = (TextView) findViewById(R.id.event_name);
        mEventStatus = (TextView) findViewById(R.id.event_status);
        mGoLiveStatus = (TextView) findViewById(R.id.go_live_status);
        mGoLiveNumber = (TextView) findViewById(R.id.go_live_number);
        mStatusBar = (RelativeLayout) findViewById(R.id.status_bar);
        mUserStatus = (TextView) findViewById(R.id.user_status);
        mFragmentContainer = (FrameLayout) findViewById(R.id.fragment_textchat_container);
        mEventImageEnd = (ImageView) findViewById(R.id.event_image_end);
        mUnreadCircle = (ImageButton) findViewById(R.id.unread_circle);
        mWarningAlert = (TextView) findViewById(R.id.quality_warning);
    }

    private void getSelectedEvent (Bundle savedInstanceState) {
        mPublisherViewContainer.displaySpinner(true);
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

    private void setEventUI() {
        try {
            updateEventName(mEvent.getString(EventProperties.NAME), EventUtils.getStatusNameById(mEvent.getString(EventProperties.STATUS)));
            EventUtils.loadEventImage(this, mEvent.has(EventProperties.END_IMAGE) ? mEvent.getJSONObject(EventProperties.END_IMAGE).getString("url") : "", mEventImageEnd);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - getInstanceById", e);
        }
    }

    private void requestEventData() {
        try {
            mWebServiceCoordinator.createToken(mUserIsCelebrity ? mEvent.getString(EventProperties.CELEBRITY_URL) : mEvent.getString(EventProperties.HOST_URL));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - getInstanceById", e);
        }
    }

    private void updateEventName() {
        try {
            mEventName.setText(EventUtils.ellipsize(mEvent.getString(EventProperties.NAME), 40));
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
        String status = EventStatus.NOT_STARTED;
        try {
            status = mEvent.getString(EventProperties.STATUS);
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
        // Store the event data
        try {
            mEvent = new JSONObject(results.toString());
        } catch (JSONException e) {
            Log.i(LOG_TAG, e.getMessage());
        }

        // Once the authToken is ready, let's sign in to firebase anonymously
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            connectToPresence();
                        } else {
                            Log.w(LOG_TAG, "signInAnonymously", task.getException());
                            Toast.makeText(CelebrityHostActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    private void connectToPresence() {
        try {
            final DatabaseReference myRef = mDatabase.getReference("activeBroadcasts/" + mEvent.getString(EventProperties.ADMIN_ID) + "/" + mEvent.getString(EventProperties.FAN_URL));
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ActiveBroadcast activeBroadcast = dataSnapshot.getValue(ActiveBroadcast.class);
                    if(mUserIsCelebrity && activeBroadcast.getCelebrityActive() ||
                            !mUserIsCelebrity && activeBroadcast.getHostActive()) {
                        mNotification.showCantPublish(IBConfig.USER_TYPE);
                    } else {
                        createPresenceRecord();
                        initEvent();
                        monitorPrivateCall();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(LOG_TAG, databaseError.getMessage());
                }
            });
        } catch (JSONException e) {
            return;
        }
    }

    private void createPresenceRecord(){
        try {
            DatabaseReference myRef = mDatabase.getReference("activeBroadcasts/" + mEvent.getString(EventProperties.ADMIN_ID) + "/" + mEvent.getString(EventProperties.FAN_URL) + "/" + IBConfig.USER_TYPE + "Active");
            myRef.setValue(true);
            myRef.onDisconnect().removeValue();
        } catch (JSONException e) {
            Log.i(LOG_TAG, e.getMessage());
        }
    }

    private void monitorPrivateCall() {
        // Listen for updates in inPrivateCall and isBackstage
        ValueEventListener updateListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PrivateCall privateCall = dataSnapshot.getValue(PrivateCall.class);
                if (privateCall != null) {
                    if (privateCall.getIsWith().equals(IBConfig.USER_TYPE)) {
                        startPrivateCall();
                    } else if (!privateCall.getIsWith().toLowerCase().equals("activeFan")) {
                        mNotification.showNotification(TEMPORARILLY_MUTED);
                    }
                } else {
                    endPrivateCall();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };
        try {
            DatabaseReference myRef = mDatabase.getReference("activeBroadcasts/" + mEvent.getString(EventProperties.ADMIN_ID) + "/" + mEvent.getString(EventProperties.FAN_URL) + "/privateCall");
            myRef.addValueEventListener(updateListener);
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    private void initEvent() {
        JSONObject objSource = new JSONObject();
        try {
            mApiKey = mEvent.getString("apiKey");
            mToken = mEvent.getString("stageToken");
            mSessionId = mEvent.getString("stageSessionId");

            //Set the LogSource
            objSource.put("app", getApplicationContext().getApplicationInfo().packageName);
            objSource.put("account", mEvent.getString(EventProperties.ADMIN_ID));
            objSource.put("event-id", mEvent.getString(EventProperties.ID));

            mLogSource = objSource.toString();

            updateEventName();

            //request Marshmallow camera permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, permsRequestCode);
            } else {
                sessionConnect();
            }


        } catch(JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
            //@TODO: Do something when this error happens
        }
    }

    @Override
    public void onWebServiceCoordinatorError(Exception error) {
        Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
        mNotification.show(R.string.connection_lost);
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
            disconnectSession();
        }
    }

    @Override
    public void onDestroy() {
        mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }

        disconnectSession();

        super.onDestroy();
        finish();
    }

    @Override
    public void onBackPressed() {

        if(mFragmentContainer.getVisibility() == View.VISIBLE) {
            toggleChat();
        }else {
            disconnectSession();

            if (mIsBound) {
                unbindService(mConnection);
                mIsBound = false;
            }
            mNotificationManager.cancel(ClearNotificationService.NOTIFICATION_ID);
            mDatabase.goOffline();
            super.onBackPressed();
        }
    }

    private void disconnectSession() {
        if (mSession != null) {
            if(mUserIsCelebrity) {
                addLogEvent(OTKAction.CELEBRITY_DISCONNECTS_ONSTAGE, OTKVariation.ATTEMPT);
            } else {
                addLogEvent(OTKAction.HOST_DISCONNECTS_ONSTAGE, OTKVariation.ATTEMPT);
            }
            mSession.disconnect();
            mSession = null;
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
            mSession.setReconnectionListener(this);
            mSession.connect(mToken);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Connected to the session.");

        //Init the analytics logging for onstage
        String source = getPackageName();
        SharedPreferences prefs = getSharedPreferences("opentok", Context.MODE_PRIVATE);
        String guidIB = prefs.getString("guidIB", null);
        if (null == guidIB) {
            guidIB = UUID.randomUUID().toString();
            prefs.edit().putString("guidIB", guidIB).commit();
        }
        mOnStageAnalyticsData = new com.tokbox.android.logging.OTKAnalyticsData.Builder(IBConfig.LOG_CLIENT_VERSION, mLogSource, IBConfig.LOG_COMPONENTID, guidIB).build();
        mOnStageAnalytics = new com.tokbox.android.logging.OTKAnalytics(mOnStageAnalyticsData);
        mOnStageAnalyticsData.setSessionId(session.getSessionId());
        mOnStageAnalyticsData.setConnectionId(session.getConnection().getConnectionId());
        mOnStageAnalyticsData.setPartnerId(mApiKey);
        mOnStageAnalytics.setData(mOnStageAnalyticsData);


        //Logging
        if(mUserIsCelebrity) {
            addLogEvent(OTKAction.CELEBRITY_CONNECTS_ONSTAGE, OTKVariation.SUCCESS);
        } else {
            addLogEvent(OTKAction.HOST_CONNECTS_ONSTAGE, OTKVariation.SUCCESS);
        }


        if (mPublisher == null) {
            mPublisher = new Publisher(CelebrityHostActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            attachPublisherView();
            doPublish();
        }
        //loading text-chat ui component
        loadTextChatFragment();
    }

    /* Reconnections events */
    @Override
    public void onReconnecting(Session session) {
        Log.i(LOG_TAG, "Session is reconnecting");
        if (mReconnectionsDialog == null) {
            mReconnectionsDialog = new ProgressDialog(this);
            mReconnectionsDialog.setTitle(getString(R.string.session_reconnecting_title));
            mReconnectionsDialog.setMessage(getString(R.string.session_reconnecting));
            mReconnectionsDialog.show();
        }
    }

    @Override
    public void onReconnected(Session session) {
        Log.i(LOG_TAG, "Session has been reconnected");
        if (mReconnectionsDialog != null) {
            mReconnectionsDialog.dismiss();
            mReconnectionsDialog = null;
        }
    }
    private void doPublish(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mAllowPublish) {
                    if(mUserIsCelebrity) {
                        addLogEvent(OTKAction.CELEBRITY_PUBLISHES_ONSTAGE, OTKVariation.ATTEMPT);
                    } else {
                        addLogEvent(OTKAction.HOST_PUBLISHES_ONSTAGE, OTKVariation.ATTEMPT);
                    }
                    mSession.publish(mPublisher);
                } else {
                    mNotification.showCantPublish(IBConfig.USER_TYPE);
                    cleanViews();
                    mPublisherViewContainer.displayAvatar(false);
                    mChatButton.setVisibility(View.GONE);

                }
            }
        }, 1000);

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
            Long tsLong = System.currentTimeMillis() / 1000;
            String ts = tsLong.toString();
            try {
                JSONObject message = new JSONObject();
                message.put("text", msg.getText());
                message.put("fromType", IBConfig.USER_TYPE);
                message.put("timestamp", ts);
                mSession.sendSignal("chatMessage", message.toString(), mProducerConnection);
            } catch (JSONException ex) {
                Log.e(LOG_TAG, ex.getMessage());
            }

        }
        return msgError;
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Disconnected from the session.");
        if(mUserIsCelebrity) {
            addLogEvent(OTKAction.CELEBRITY_DISCONNECTS_ONSTAGE, OTKVariation.SUCCESS);
        } else {
            addLogEvent(OTKAction.HOST_DISCONNECTS_ONSTAGE, OTKVariation.SUCCESS);
        }

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
            mSubscriberViewContainer.displaySpinner(true);
        }
        else {
            enableAudioOnlyView(mSubscriber.getStream().getConnection().getConnectionId(), true);
        }
    }

    private void subscribeFanToStream(Stream stream) {
        Log.i(LOG_TAG, "subscribeFanToStream");
        mSubscriberFan = new Subscriber(CelebrityHostActivity.this, stream);
        mSubscriberFan.setVideoListener(this);
        mSession.subscribe(mSubscriberFan);

        if (stream.hasVideo()) {
            // start loading spinning
            mSubscriberFanViewContainer.displaySpinner(true);
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
        String error = exception.getErrorCode().toString();
        if(mUserIsCelebrity) {
            addLogEvent(OTKAction.CELEBRITY_DISCONNECTS_ONSTAGE, OTKVariation.ERROR);
        } else {
            addLogEvent(OTKAction.HOST_DISCONNECTS_ONSTAGE, OTKVariation.ERROR);
        }
        if(error.equals("ConnectionDropped") || error.equals("ConnectionFailed")) {
            cleanViews();
            mNotification.show(R.string.connection_lost);
        }
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "onStreamReceived:" + stream.getConnection().getData());
        switch(EventUtils.getUserType(stream.getConnection().getData())) {
            case EventRole.FAN:
                if (mFanStream == null) {
                    if(mUserIsCelebrity) {
                        addLogEvent(OTKAction.CELEBRITY_SUBSCRIBES_FAN, OTKVariation.ATTEMPT);
                    } else {
                        addLogEvent(OTKAction.HOST_SUBSCRIBES_FAN, OTKVariation.ATTEMPT);
                    }
                    subscribeFanToStream(stream);
                    mFanStream = stream;
                    updateViewsWidth();
                }
                break;
            case EventRole.CELEBRITY:
                Log.i(LOG_TAG, "Celebrity!");
                if(mUserIsCelebrity) mAllowPublish = false;
                if (mCelebirtyStream == null && !mUserIsCelebrity) {
                    addLogEvent(OTKAction.HOST_SUBSCRIBES_CELEBRITY, OTKVariation.ATTEMPT);
                    subscribeToStream(stream);
                    mCelebirtyStream = stream;
                    updateViewsWidth();
                }
                break;
            case EventRole.HOST:
                if(!mUserIsCelebrity) mAllowPublish = false;
                if (mHostStream == null && mUserIsCelebrity) {
                    addLogEvent(OTKAction.CELEBRITY_SUBSCRIBES_HOST, OTKVariation.ATTEMPT);
                    subscribeToStream(stream);
                    mHostStream = stream;
                    updateViewsWidth();
                }
                break;
            case EventRole.PRODUCER:
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
        switch(EventUtils.getUserType(stream.getConnection().getData())) {
            case EventRole.FAN:
                if(mFanStream != null && mFanStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    unsubscribeFanFromStream(stream);
                    mFanStream = null;
                    updateViewsWidth();
                }
                break;
            case EventRole.CELEBRITY:
                if(mCelebirtyStream != null && mCelebirtyStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    unsubscribeFromStream(stream);
                    mCelebirtyStream = null;
                    updateViewsWidth();
                }
                break;
            case EventRole.HOST:
                if(mHostStream!= null && mHostStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    unsubscribeFromStream(stream);
                    mHostStream = null;
                    updateViewsWidth();
                }
            case EventRole.PRODUCER:
                if(mProducerStream != null && mProducerStream.getConnection().getConnectionId().equals(streamConnectionId)) {
                    mProducerStream = null;
                    Log.i(LOG_TAG, "producer stream out");
                }
                break;
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {

        if(mUserIsCelebrity) {
           addLogEvent(OTKAction.CELEBRITY_PUBLISHES_ONSTAGE, OTKVariation.SUCCESS);
        } else {
            addLogEvent(OTKAction.HOST_PUBLISHES_ONSTAGE, OTKVariation.SUCCESS);
        }

        // stop loading spinning
        mPublisherViewContainer.displaySpinner(false);
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
        if(mUserIsCelebrity) {
            addLogEvent(OTKAction.CELEBRITY_UNPUBLISHES_ONSTAGE, OTKVariation.SUCCESS);
        } else {
            addLogEvent(OTKAction.HOST_UNPUBLISHES_ONSTAGE, OTKVariation.SUCCESS);
        }
    }

    @Override
    public void onError(PublisherKit publisher, OpentokError exception) {
        Log.i(LOG_TAG, "Publisher exception: " + exception.getMessage());
        if(mUserIsCelebrity) {
            addLogEvent(OTKAction.CELEBRITY_PUBLISHES_ONSTAGE, OTKVariation.ERROR);
        } else {
            addLogEvent(OTKAction.HOST_PUBLISHES_ONSTAGE, OTKVariation.ERROR);
        }
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriber) {
        String userType = EventUtils.getUserType(subscriber.getStream().getConnection().getData());
        if(userType.equals(EventRole.FAN)) {

            if(mUserIsCelebrity) {
                addLogEvent(OTKAction.CELEBRITY_SUBSCRIBES_FAN, OTKVariation.SUCCESS);
            } else {
                addLogEvent(OTKAction.HOST_SUBSCRIBES_FAN, OTKVariation.SUCCESS);
            }


            // stop loading spinning
            mSubscriberFanViewContainer.displayAvatar(false);
            attachSubscriberFanView();
        } else {

            if(mUserIsCelebrity) {
                addLogEvent(OTKAction.CELEBRITY_SUBSCRIBES_HOST, OTKVariation.SUCCESS);
            } else {
                addLogEvent(OTKAction.HOST_SUBSCRIBES_CELEBRITY, OTKVariation.SUCCESS);
            }

            // stop loading spinning
            mSubscriberViewContainer.displaySpinner(false);
            attachSubscriberView();
        }

    }

    private void enableAudioOnlyView(String subscriberConnectionId, boolean show) {
        String hostCeleb = mSubscriber != null ? mSubscriber.getStream().getConnection().getConnectionId() : "";
        String fan = mSubscriberFan != null ? mSubscriberFan.getStream().getConnection().getConnectionId() : "";
        if(subscriberConnectionId.equals(hostCeleb)) {
            if (show) {
                mSubscriber.getView().setVisibility(View.GONE);
                mSubscriberViewContainer.displayAvatar(true);
            }
            else {
                mSubscriber.getView().setVisibility(View.VISIBLE);
                mSubscriberViewContainer.displayAvatar(false);
            }
        }
        if(subscriberConnectionId.equals(fan)) {
            if (show) {
                mSubscriberFan.getView().setVisibility(View.GONE);
                mSubscriberFanViewContainer.displayAvatar(true);
            }
            else {
                mSubscriberFan.getView().setVisibility(View.VISIBLE);
                mSubscriberFanViewContainer.displayAvatar(false);
            }
        }
    }
    @Override
    public void onVideoDisabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG,
                "Video disabled:" + reason);
        enableAudioOnlyView(subscriber.getStream().getConnection().getConnectionId(), true);
        if (reason.equals("quality")) {
            mWarningAlert.setBackgroundResource(R.color.quality_alert);
            mWarningAlert.setTextColor(Color.WHITE);
            mWarningAlert.bringToFront();
            mWarningAlert.setVisibility(View.VISIBLE);
            mWarningAlert.postDelayed(new Runnable() {
                public void run() {
                    mWarningAlert.setVisibility(View.GONE);
                }
            }, 7000);
        }
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriber, String reason) {
        Log.i(LOG_TAG, "Video enabled:" + reason);
        enableAudioOnlyView(subscriber.getStream().getConnection().getConnectionId(), false);
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "Video may be disabled soon due to network quality degradation. Add UI handling here." + subscriber.getStream().getConnection().getData());
        mWarningAlert.setBackgroundResource(R.color.quality_warning);
        mWarningAlert.setTextColor(CelebrityHostActivity.this.getResources().getColor(R.color.warning_text));
        mWarningAlert.bringToFront();
        mWarningAlert.setVisibility(View.VISIBLE);
        mWarningAlert.postDelayed(new Runnable() {
            public void run() {
                mWarningAlert.setVisibility(View.GONE);
            }
        }, 7000);
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
        Log.i(LOG_TAG, "Video may no longer be disabled as stream quality improved. Add UI handling here.");
    }

    /* Subscriber Listener methods */

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        //Log.i(LOG_TAG, "Subscriber Connected");
        String userType = EventUtils.getUserType(subscriberKit.getStream().getConnection().getData());
        if(userType.equals(EventRole.FAN)) {
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
        String userType = EventUtils.getUserType(connection.getData());
        if(type != null) {
            if (userType.equals(EventRole.PRODUCER)) {
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
                }
            }
            else {
                if (!userType.equals(EventRole.FAN) && !userType.equals(EventRole.HOST)
                        && !userType.equals(EventRole.CELEBRITY))
                    Log.i(LOG_TAG, "Got a signal from an unexpected origin. Ignoring");
            }
        }
        //TODO: onChangeVolumen

    }

    private void startPrivateCall() {
        if(mSubscriber != null) mSubscriber.setSubscribeToAudio(false);
        subscribeProducer();
    }

    private void endPrivateCall() {
        if(mSubscriber != null) mSubscriber.setSubscribeToAudio(true);
        if(mSubscriberProducer != null) {
            mSession.unsubscribe(mSubscriberProducer);
            mSubscriberProducer = null;
        }
        mNotification.hide();
    }

    private void subscribeProducer() {
        if(mProducerStream != null) {
            mSubscriberProducer = new Subscriber(CelebrityHostActivity.this, mProducerStream);
            mSession.subscribe(mSubscriberProducer);
            mNotification.showNotification(Notification.TYPE.PRIVATE_CALL);
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

    private void handleNewMessage(String data, Connection connection) {
        String text = "";
        try {
            text = new JSONObject(data).getString("text");
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Could not parse malformed JSON: \"" + data + "\"");
        }

        ChatMessage msg = null;
        msg = new ChatMessage(connection.getConnectionId(), "Producer", text);
        // Add the new ChatMessage to the text-chat component
        if (mTextChatFragment != null ) {
            mTextChatFragment.addMessage(msg);
            if (mFragmentContainer.getVisibility() != View.VISIBLE) {
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
            mPublisherViewContainer.displayAvatar(false);
        } else {
            mPublisher.getView().setVisibility(View.GONE);
            mPublisherViewContainer.displayAvatar(true);
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
            mEvent.put("status", EventStatus.LIVE);
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
        mSubscriberFanViewContainer.displayAvatar(false);
        mSubscriberViewContainer.displayAvatar(false);
        mPublisherViewContainer.displayAvatar(false);

        //Unpublish
        if(mUserIsCelebrity) {
            addLogEvent(OTKAction.CELEBRITY_UNPUBLISHES_ONSTAGE, OTKVariation.ATTEMPT);
        } else {
            addLogEvent(OTKAction.HOST_UNPUBLISHES_ONSTAGE, OTKVariation.ATTEMPT);
        }
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
                disconnectSession();
            }
        }, 10000);

        try {
            //Change status
            mEvent.put("status", EventStatus.CLOSED);
        } catch (JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        //Update event name and Status.
        updateEventName();
    }


    /* Connection Listener methods */
    @Override
    public void onConnectionCreated(Session session, Connection connection) {
        if(connection.getData() != null && EventUtils.getUserType(connection.getData()).equals(EventRole.PRODUCER)) {

            mProducerConnection = connection;
            mChatButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection)
    {
        if(EventUtils.getUserType(connection.getData()).equals(EventRole.PRODUCER)) {
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
            sessionConnect();

        } else if(grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
            Log.i(LOG_TAG, "Permission denied");
            boolean audio = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
            boolean video = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1]);

            if(audio || video){
                //user denied without Never ask again, just show rationale explanation
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Denied");
                builder.setMessage("Without this permission the app is unable to publish. Are you sure you want to deny this permission?");
                builder.setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onBackPressed();
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
}

