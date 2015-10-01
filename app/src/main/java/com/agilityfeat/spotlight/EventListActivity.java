package com.agilityfeat.spotlight;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;

import com.agilityfeat.spotlight.model.InstanceApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EventListActivity extends AppCompatActivity {
    private static final String LOG_TAG = EventListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        final ListView listActivities = (ListView) findViewById(R.id.listview);
        JSONArray arrEvents = InstanceApp.getInstance().getEvents();
        ArrayList<String> stringArrayList = new ArrayList<String>();
        try {
            for (int i=0; i<arrEvents.length(); i++) {
                JSONObject event = arrEvents.getJSONObject(i);
                stringArrayList.add(event.getString("event_name"));
            }
        } catch(JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }

        //if you want your array
        String [] eventNames = stringArrayList.toArray(new String[stringArrayList.size()]);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, eventNames);
        listActivities.setAdapter(adapter);

        listActivities.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position,
                                    long id) {
                showEvent(position);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event_list, menu);
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
        Intent localIntent = new Intent(EventListActivity.this, CelebrityHostActivity.class);
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index",Integer.toString(event_index));
        localIntent.putExtras(localBundle);
        startActivity(localIntent);
    }
}


