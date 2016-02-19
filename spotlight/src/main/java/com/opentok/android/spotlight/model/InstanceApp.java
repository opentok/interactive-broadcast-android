package com.opentok.android.spotlight.model;


import android.util.Log;

import com.opentok.android.spotlight.config.SpotlightConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InstanceApp {
    private static InstanceApp mInstance = null;

    private JSONObject mData;

    private InstanceApp(){
        mData = null;
    }

    public static InstanceApp getInstance(){
        if(mInstance == null)
        {
            mInstance = new InstanceApp();
        }
        return mInstance;
    }

    public JSONObject getData(){
        return this.mData;
    }

    public JSONArray getEvents(){
        try {
            if(this.mData != null && this.mData.has("events")) {
                return this.mData.getJSONArray("events");
            } else {
                return new JSONArray();
            }
        } catch(JSONException e) {
            Log.e("InstanceApp", e.getMessage());
            return null;
        }
    }

    public Boolean getEnableGetInline(){
        try {
            if(this.mData != null && this.mData.has("enable_getinline")) {
                return this.mData.getBoolean("enable_getinline");
            } else {
                return false;
            }
        } catch(JSONException e) {
            Log.e("InstanceApp", e.getMessage());
            return false;
        }
    }

    public Boolean getEnableAnalytics(){
        try {
            if(this.mData != null && this.mData.has("enable_analytics")) {
                return this.mData.getBoolean("enable_analytics");
            } else {
                return false;
            }
        } catch(JSONException e) {
            Log.e("InstanceApp", e.getMessage());
            return false;
        }
    }



    public JSONObject getEventByIndex(int index){
        try {
            JSONArray events = this.mData.getJSONArray("events");
            return events.getJSONObject(index);
        } catch(JSONException e) {
            return null;
        }
    }

    public void setData(JSONObject value){

        mData = value;
        try {
            SpotlightConfig.FRONTEND_URL = (String)mData.get("frontend_url");
            SpotlightConfig.SIGNALING_URL = (String)mData.get("signaling_url");
            SpotlightConfig.DEFAULT_EVENT_IMAGE = (String)mData.get("default_event_image");
        } catch(JSONException e) {
            Log.e("instanceApp.setData", e.getMessage());
        }
    }
}