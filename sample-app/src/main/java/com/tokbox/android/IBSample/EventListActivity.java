package com.tokbox.android.IBSample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.tokbox.android.IB.CelebrityHostActivity;
import com.tokbox.android.IB.FanActivity;
import com.tokbox.android.IB.config.IBConfig;
import com.tokbox.android.IB.events.EventUtils;
import com.tokbox.android.IB.model.InstanceApp;
import com.tokbox.android.IB.socket.SocketCoordinator;
import com.tokbox.android.IB.ws.WebServiceCoordinator;
import com.tokbox.android.IBSample.events.EventAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Main Activity
 */
public class EventListActivity extends AppCompatActivity implements WebServiceCoordinator.Listener {

    private static final String LOG_TAG = EventListActivity.class.getSimpleName();
    private WebServiceCoordinator mWebServiceCoordinator;
    private ProgressDialog mProgress;
    private Handler mHandler = new Handler();
    private ArrayList<JSONObject> mEventList = new ArrayList<JSONObject>();
    private EventAdapter mEventAdapter;
    private GridView mListActivities;
    private TextView mEventListTitle;
    private SocketCoordinator mSocket;
    private JSONArray mArrEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(IBConfig.ADMIN_ID == null || IBConfig.ADMIN_ID.equals("")) {
            goToMainActivity();
        }
        setContentView(R.layout.event_list_activity);

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);

        //Set fonts
        mEventListTitle = (TextView) findViewById(R.id.event_list_title);
        Typeface font = EventUtils.getFont(this);
        mEventListTitle.setTypeface(font);

        //start the progress bar
        startLoadingAnimation();

        //
        getEventsByAdmin();
    }

    private void initSocket() {
        //Init socket
        mSocket = new SocketCoordinator();
        mSocket.connect();
        mSocket.getSocket().on("changeStatus", onChangeStatus);
    }

    private Emitter.Listener onChangeStatus = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            String id;
            String newStatus;
            try {
                id = data.getString("id");
                newStatus = data.getString("newStatus");
                Log.i(LOG_TAG, "change" + newStatus);

                for (int i=0; i<mArrEvents.length(); i++) {
                    if(mArrEvents.getJSONObject(i).getString("id").equals(id)) {
                        mArrEvents.getJSONObject(i).put("status", newStatus);
                        break;
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showEventList();
                    }
                });


            } catch (JSONException e) {
                return;
            }

        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LOG_TAG, "Back from the event");
        goToMainActivity();
    }

    private void goToMainActivity() {
        Intent localIntent;
        localIntent = new Intent(EventListActivity.this, MainActivity.class);
        startActivity(localIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();

        if(mListActivities != null) {
            mListActivities.setAdapter(null);
            //start the progress bar
            startLoadingAnimation();
            getEventsByAdmin();
        }

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mSocket != null) {
            mSocket.getSocket().off("changeStatus", onChangeStatus);
        }


    }

    public void getEventsByAdmin() {
        try {
            mWebServiceCoordinator.getEventsByAdmin();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception - getInstanceById", e);
        }
    }

    public void startLoadingAnimation() {
        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Loading");
        mProgress.setMessage("Wait while loading...");
        mProgress.setCancelable(false);
        mProgress.show();
    }

    public void stopLoadingAnimation() {
        if(mProgress != null && mProgress.isShowing()){
            mProgress.dismiss();
        }
    }

    public void showEventList() {
        Log.i(LOG_TAG, "starting event list app");

        mListActivities = (GridView) findViewById(R.id.gridView);
        mEventList.clear();
        try {
            for (int i=0; i<mArrEvents.length(); i++) {
                if(!mArrEvents.getJSONObject(i).getString("status").equals("C")) {
                    mEventList.add(mArrEvents.getJSONObject(i));
                }
            }
        } catch(JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }

        mEventAdapter = new EventAdapter(this, R.layout.event_item, mEventList);

        mListActivities.setAdapter(mEventAdapter);
    }

    public void showEvent() {
        //mSocket.disconnect();
        //Passing the apiData to AudioVideoActivity
        Intent localIntent;
        if(IBConfig.USER_TYPE == "fan") {
            localIntent = new Intent(EventListActivity.this, FanActivity.class);
        } else {
            localIntent = new Intent(EventListActivity.this, CelebrityHostActivity.class);
        }

        Bundle localBundle = new Bundle();
        localBundle.putString("event_index", "0");
        localIntent.putExtras(localBundle);
        startActivityForResult(localIntent, 0);
    }

    public void showEvent(int event_index) {
        mSocket.disconnect();
        //Passing the apiData to AudioVideoActivity
        Intent localIntent;
        if(IBConfig.USER_TYPE == "fan") {
            localIntent = new Intent(EventListActivity.this, FanActivity.class);
        } else {
            localIntent = new Intent(EventListActivity.this, CelebrityHostActivity.class);
        }
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index", Integer.toString(event_index));
        localIntent.putExtras(localBundle);
        startActivity(localIntent);
        finish();
    }

    /**
     * Web Service Coordinator delegate methods
     */
    @Override
    public void onDataReady(JSONObject instanceAppData) {
        //Set instanceApp Data
        InstanceApp.getInstance().setData(instanceAppData);

        Boolean bSuccess = false;
        mArrEvents = new JSONArray();
        try {
            bSuccess = (Boolean)instanceAppData.get("success");
            if(instanceAppData.has("events")) {
                mArrEvents = instanceAppData.getJSONArray("events");
            }

            if(bSuccess) {
                //init socket
                initSocket();

                //Check the count of events.
                if(mArrEvents.length() > 1) {
                    showEventList();
                } else {
                    if(mArrEvents.length() == 1) {
                        showEvent();
                    } else {
                        Toast.makeText(getApplicationContext(),"No events were found", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Log.e(LOG_TAG, "Invalid instance ID");
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "parsing instanceAppData error", e);
        } finally {
            stopLoadingAnimation();
        }

    }

    @Override
    public void onWebServiceCoordinatorError(Exception error) {
        Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
        Toast.makeText(getApplicationContext(),"Unable to connect to the server. Trying again in 5 seconds..", Toast.LENGTH_LONG).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getEventsByAdmin();
            }
        }, 5000);
    }

}