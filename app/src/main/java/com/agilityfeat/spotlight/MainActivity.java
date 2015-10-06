package com.agilityfeat.spotlight;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.agilityfeat.spotlight.model.InstanceApp;
import com.agilityfeat.spotlight.ws.WebServiceCoordinator;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;


/**
 * Main Activity
 */
public class MainActivity extends AppCompatActivity implements WebServiceCoordinator.Listener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private WebServiceCoordinator mWebServiceCoordinator;
    private ProgressDialog mProgress;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    }

    public void getInstanceId() {
        try {
            mWebServiceCoordinator.getInstanceById(BuildConfig.INSTANCE_ID);
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

        Intent intent = new Intent(MainActivity.this, EventListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void showEvent() {
        //Passing the apiData to AudioVideoActivity
        Intent localIntent = new Intent(MainActivity.this, CelebrityHostActivity.class);
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index", "0");
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
                //Check the count of events.
                if(arrEvents.length() > 1) {
                    showEventList();
                } else {
                    showEvent();
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