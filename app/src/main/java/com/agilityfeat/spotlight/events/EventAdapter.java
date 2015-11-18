package com.agilityfeat.spotlight.events;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.agilityfeat.spotlight.CelebrityHostActivity;
import com.agilityfeat.spotlight.FanActivity;
import com.agilityfeat.spotlight.R;
import com.agilityfeat.spotlight.config.SpotlightConfig;

import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.List;


public class EventAdapter extends ArrayAdapter<JSONObject> {

    private static final String LOG_TAG = EventAdapter.class.getSimpleName();

    private List<JSONObject> eventList = new ArrayList<JSONObject>();
    ViewHolder holder;
    private Context mContext;

    private class ViewHolder {
        public TextView name, date_status;
        public ImageView event_img;
        public Button join_event;
    }

    public EventAdapter(Context context, int resource, List<JSONObject> entities) {
        super(context, resource, entities);
        this.eventList = entities;
        this.mContext = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        final JSONObject event = this.eventList.get(position);
        final int pos = position;
        holder = new ViewHolder();
        if (convertView == null) {
            holder = new ViewHolder();

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.event_item, parent, false);
            holder.date_status = (TextView) convertView.findViewById(R.id.date_status);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.event_img = (ImageView) convertView.findViewById(R.id.event_img);
            holder.join_event = (Button) convertView.findViewById(R.id.join_btn);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        try {
            if(holder.name.getText().equals("")) {
                EventUtils.loadEventImage(getContext(), event.getString("event_image"), holder.event_img);
            }
            holder.name.setText(event.getString("event_name"));
            holder.date_status.setText(EventUtils.getStatusNameById(event.getString("status")));
            holder.join_event.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    showEvent(pos);
                }
            });

        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        return convertView;
    }

    public void showEvent(int event_index) {
        //Passing the apiData to AudioVideoActivity
        Intent localIntent;
        if(SpotlightConfig.USER_TYPE == "fan") {
            localIntent = new Intent(mContext, FanActivity.class);
        } else {
            localIntent = new Intent(mContext, CelebrityHostActivity.class);
        }
        Bundle localBundle = new Bundle();
        localBundle.putString("event_index",Integer.toString(event_index));
        localIntent.putExtras(localBundle);
        mContext.startActivity(localIntent);
    }

}
