package com.opentok.android.spotlightsample.events;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.opentok.android.spotlight.CelebrityHostActivity;
import com.opentok.android.spotlight.FanActivity;
import com.opentok.android.spotlight.config.SpotlightConfig;
import com.opentok.android.spotlight.events.EventUtils;
import com.opentok.android.spotlightsample.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class EventAdapter extends ArrayAdapter<JSONObject> {

    private static final String LOG_TAG = EventAdapter.class.getSimpleName();

    private List<JSONObject> eventList = new ArrayList<JSONObject>();
    ViewHolder holder;
    private Context mContext;
    private Typeface mFont;

    private class ViewHolder {
        public TextView name, date_status;
        public ImageView event_img;
        public Button join_event;
    }

    public EventAdapter(Context context, int resource, List<JSONObject> entities) {
        super(context, resource, entities);
        this.eventList = entities;
        this.mContext = context;

        mFont = EventUtils.getFont(context);
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
            if(event.getString("status").equals("N")) {
                //holder.join_event.setVisibility(View.GONE);

                if(!event.getString("date_time_start").equals("null")){
                    holder.date_status.setText(event.getString("date_time_start"));
                    Date date = new Date();
                    //msg time
                    SimpleDateFormat ft = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
                    SimpleDateFormat ft2 = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    try {
                        date = ft.parse(event.getString("date_time_start").replace(".0", ""));
                    }
                    catch(ParseException pe) {
                        Log.e(LOG_TAG, pe.getMessage());
                    }

                    holder.date_status.setText(ft2.format(date).toString().toUpperCase());
                }
                holder.join_event.setText("Not started");
            } else {
                holder.join_event.setText("Join event");
                holder.join_event.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showEvent(pos);
                    }
                });
            }


        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        //Set fonts
        holder.name.setTypeface(mFont);
        holder.date_status.setTypeface(mFont);
        holder.join_event.setTypeface(mFont);


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
        localBundle.putString("event_index", Integer.toString(event_index));
        localIntent.putExtras(localBundle);
        mContext.startActivity(localIntent);
    }

}
