package com.agilityfeat.spotlight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.GridView;
import android.widget.Toast;

import com.agilityfeat.spotlight.config.SpotlightConfig;
import com.agilityfeat.spotlight.events.EventAdapter;
import com.agilityfeat.spotlight.model.InstanceApp;
import com.agilityfeat.spotlight.ws.WebServiceCoordinator;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;


/**
 * Main Activity
 */
public class MainActivity extends AppCompatActivity implements WebServiceCoordinator.Listener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private WebServiceCoordinator mWebServiceCoordinator;
    private ProgressDialog mProgress;
    private Handler mHandler = new Handler();
    private ArrayList<JSONObject> mEventList = new ArrayList<JSONObject>();
    private EventAdapter mEventAdapter;
    private GridView mListActivities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(LOG_TAG, "usert type = " + SpotlightConfig.USER_TYPE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebServiceCoordinator = new WebServiceCoordinator(this, this);

        //start the progress bar
        startLoadingAnimation();

        getInstanceId();
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
            getInstanceId();
        }

    }

    public void getInstanceId() {
        try {
            mWebServiceCoordinator.getInstanceById(SpotlightConfig.INSTANCE_ID);
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
        if(mProgress.isShowing()){
            mProgress.dismiss();
        }
    }

    public void showEventList() {
        Log.i(LOG_TAG, "starting event list app");

        mListActivities = (GridView) findViewById(R.id.gridView);
        JSONArray arrEvents = InstanceApp.getInstance().getEvents();
        mEventList.clear();
        try {
            for (int i=0; i<arrEvents.length(); i++) {
                mEventList.add(arrEvents.getJSONObject(i));
            }
        } catch(JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }

        mEventAdapter = new EventAdapter(this, R.layout.event_item, mEventList);

        mListActivities.setAdapter(mEventAdapter);
    }

    public void showEvent() {
        //Passing the apiData to AudioVideoActivity
        Intent localIntent;
        if(SpotlightConfig.USER_TYPE == "fan") {
            localIntent = new Intent(MainActivity.this, FanActivity.class);
        } else {
            localIntent = new Intent(MainActivity.this, CelebrityHostActivity.class);
        }
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index", "0");
        localIntent.putExtras(localBundle);
        startActivity(localIntent);
    }

    public void showEvent(int event_index) {
        //Passing the apiData to AudioVideoActivity
        Intent localIntent;
        if(SpotlightConfig.USER_TYPE == "fan") {
            localIntent = new Intent(MainActivity.this, FanActivity.class);
        } else {
            localIntent = new Intent(MainActivity.this, CelebrityHostActivity.class);
        }
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index",Integer.toString(event_index));
        localIntent.putExtras(localBundle);
        startActivity(localIntent);
    }

    /**
     * Web Service Coordinator delegate methods
     */
    @Override
    public void onDataReady(JSONObject instanceAppData) {
        //Set instanceApp Data
        InstanceApp.getInstance().setData(instanceAppData);

        Boolean bSuccess = false;
        JSONArray arrEvents = new JSONArray();
        try {
            bSuccess = (Boolean)instanceAppData.get("success");
            if(instanceAppData.has("events")) {
                arrEvents = instanceAppData.getJSONArray("events");
            }

            if(bSuccess) {
                SpotlightConfig.FRONTEND_URL = (String)instanceAppData.get("frontend_url");
                SpotlightConfig.DEFAULT_EVENT_IMAGE = (String)instanceAppData.get("default_event_image");
                //Check the count of events.
                if(arrEvents.length() > 1) {
                    showEventList();
                } else {
                    if(arrEvents.length() == 1) {
                        showEvent();
                    } else {
                        Toast.makeText(getApplicationContext(),"No events were found", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(),"Invalid instance ID", Toast.LENGTH_SHORT).show();
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
                getInstanceId();
            }
        }, 500);
    }

}