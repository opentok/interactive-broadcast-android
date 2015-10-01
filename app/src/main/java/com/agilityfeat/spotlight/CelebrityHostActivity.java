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
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONException;
import org.json.JSONObject;



public class CelebrityHostActivity extends AppCompatActivity implements WebServiceCoordinator.Listener,

        Session.SessionListener, Session.ConnectionListener, PublisherKit.PublisherListener, SubscriberKit.SubscriberListener,
        Session.SignalListener,Subscriber.VideoListener{

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
    private Stream mCelebirtyStream;
    private Stream mFanStream;
    private Stream mHostStream;
    private ScrollView mScroller;
    private RelativeLayout mMessageBox;
    private EditText mMessageEditText;
    private TextView mMessageView;

    private Handler mHandler = new Handler();
    private RelativeLayout mPublisherViewContainer;
    private RelativeLayout mSubscriberViewContainer;
    private RelativeLayout mSubscriberFanViewContainer;

    // Spinning wheel for loading subscriber view
    private ProgressBar mLoadingSub;
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
        setContentView(R.layout.activity_celebrity_host);

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);

        mPublisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mSubscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
        mSubscriberFanViewContainer = (RelativeLayout) findViewById(R.id.subscriberviewfan);
        mMessageBox = (RelativeLayout) findViewById(R.id.messagebox);
        mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);
        mLoadingSubPublisher = (ProgressBar) findViewById(R.id.loadingSpinnerPublisher);
        mLoadingSubFan = (ProgressBar) findViewById(R.id.loadingSpinnerFan);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mScroller = (ScrollView) findViewById(R.id.scroller);
        mMessageEditText = (EditText) findViewById(R.id.message);
        mMessageView = (TextView) findViewById(R.id.messageView);

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
            if(BuildConfig.USER_TYPE == "celebrity") {
                mWebServiceCoordinator.createCelebrityToken(event.getString("celebrity_url"));
            } else {
                mWebServiceCoordinator.createHostToken(event.getString("host_url"));
            }
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
                .setContentText(getResources().getString(R.string.notification));
                //.setSmallIcon(R.drawable.ic_launcher).setOngoing(true);

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

        if(mScroller.getVisibility() == View.VISIBLE) {
            toggleChat();
        }else {
            if (mSession != null) {
                mSession.disconnect();
            }

            super.onBackPressed();
        }
    }

    public void reloadInterface() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSubscriber != null) {
                    attachSubscriberView(mSubscriber);
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

                int streams = 1;
                if (mFanStream != null) streams++;
                if (mCelebirtyStream != null || mHostStream != null) streams++;

                RelativeLayout.LayoutParams publisher_head_params = (RelativeLayout.LayoutParams) mPublisherViewContainer.getLayoutParams();
                publisher_head_params.width = screenWidth(CelebrityHostActivity.this) / streams;
                mPublisherViewContainer.setLayoutParams(publisher_head_params);

                RelativeLayout.LayoutParams subscriber_head_params = (RelativeLayout.LayoutParams) mSubscriberViewContainer.getLayoutParams();
                subscriber_head_params.width = screenWidth(CelebrityHostActivity.this) / streams;
                mSubscriberViewContainer.setLayoutParams(subscriber_head_params);

                RelativeLayout.LayoutParams subscriberfan_head_params = (RelativeLayout.LayoutParams) mSubscriberFanViewContainer.getLayoutParams();
                subscriberfan_head_params.width = screenWidth(CelebrityHostActivity.this) / streams;
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
            attachPublisherView(mPublisher);
            mSession.publish(mPublisher);
        }
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Disconnected from the session.");
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

        mSubscriber = new Subscriber(CelebrityHostActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);

        if (mSubscriber.getSubscribeToVideo()) {
            // start loading spinning
            mLoadingSub.setVisibility(View.VISIBLE);
        }
    }

    private void subscribeFanToStream(Stream stream) {

        mSubscriberFan = new Subscriber(CelebrityHostActivity.this, stream);
        mSubscriberFan.setVideoListener(this);
        mSession.subscribe(mSubscriberFan);

        if (mSubscriberFan.getSubscribeToVideo()) {
            // start loading spinning
            mLoadingSubFan.setVisibility(View.VISIBLE);
        }
    }

    private void unsubscribeFromStream(Stream stream) {
        if (mSubscriber.getStream().equals(stream)) {
            mSubscriberViewContainer.removeView(mSubscriber.getView());
            mSubscriber = null;
        }
    }

    private void unsubscribeFanFromStream(Stream stream) {
        if (mSubscriberFan.getStream().equals(stream)) {
            mSubscriberFanViewContainer.removeView(mSubscriberFan.getView());
            mSubscriberFan = null;
        }
    }

    private void attachSubscriberView(Subscriber subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                getResources().getDisplayMetrics().widthPixels, getResources()
                .getDisplayMetrics().heightPixels);
        mSubscriberViewContainer.removeView(mSubscriber.getView());
        mSubscriberViewContainer.addView(mSubscriber.getView(), layoutParams);
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

        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if (mFanStream == null) {
                    subscribeFanToStream(stream);
                    mFanStream = stream;
                    updateViewsWidth();
                }
                break;
            case "usertype=celebrity":
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
        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        switch(stream.getConnection().getData()) {
            case "usertype=fan":
                if(mFanStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    unsubscribeFanFromStream(stream);
                    mFanStream = null;
                    updateViewsWidth();
                }
                break;
            case "usertype=celebrity":
                if(mCelebirtyStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    unsubscribeFromStream(stream);
                    mCelebirtyStream = null;
                    updateViewsWidth();
                }
                break;
            case "usertype=host":
                if(mHostStream.getConnection().getConnectionId() == stream.getConnection().getConnectionId()) {
                    unsubscribeFromStream(stream);
                    mHostStream = null;
                    updateViewsWidth();
                }
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
        } else {
            // stop loading spinning
            mLoadingSub.setVisibility(View.GONE);
            attachSubscriberView(mSubscriber);
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
        if (type != null && "chatMessage".equals(type)) {
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
    }

    /* Connection Listener methods */
    @Override
    public void onConnectionCreated(Session session, Connection connection) {

    }

    @Override
    public void onConnectionDestroyed(Session session, Connection connection)
    {

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
        message
        sendSignal("chatMessage", message);
        presentMessage("Me", message);
    }

    public void sendSignal(String type, String msg) {
        mSession.sendSignal(type, msg);
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


}
