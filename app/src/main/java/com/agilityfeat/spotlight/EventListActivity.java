package com.agilityfeat.spotlight;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import java.util.ArrayList;

import com.agilityfeat.spotlight.events.EventAdapter;
import com.agilityfeat.spotlight.config.SpotlightConfig;
import com.agilityfeat.spotlight.model.InstanceApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EventListActivity extends AppCompatActivity {
    private static final String LOG_TAG = EventListActivity.class.getSimpleName();
    private ArrayList<JSONObject> mEventList = new ArrayList<JSONObject>();
    private EventAdapter mEventAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        final GridView listActivities = (GridView) findViewById(R.id.gridView);
        JSONArray arrEvents = InstanceApp.getInstance().getEvents();
        try {
            for (int i=0; i<arrEvents.length(); i++) {
                mEventList.add(arrEvents.getJSONObject(i));
            }
        } catch(JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }

        mEventAdapter = new EventAdapter(this, R.layout.event_item, mEventList);

        listActivities.setAdapter(mEventAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_celebrity_host, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showEvent(int event_index) {
        //Passing the apiData to AudioVideoActivity
        Intent localIntent;
        if(SpotlightConfig.USER_TYPE == "fan") {
            localIntent = new Intent(EventListActivity.this, FanActivity.class);
        } else {
            localIntent = new Intent(EventListActivity.this, CelebrityHostActivity.class);
        }
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index",Integer.toString(event_index));
        localIntent.putExtras(localBundle);
        startActivity(localIntent);
    }
}


