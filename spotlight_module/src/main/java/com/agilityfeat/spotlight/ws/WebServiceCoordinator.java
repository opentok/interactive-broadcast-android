package com.agilityfeat.spotlight.ws;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import com.agilityfeat.spotlight.config.SpotlightConfig;


public class WebServiceCoordinator {

    private static final String BACKEND_BASE_URL = SpotlightConfig.BACKEND_BASE_URL;
    private static final String LOG_TAG = WebServiceCoordinator.class.getSimpleName();

    private final Context context;
    private Listener delegate;

    public WebServiceCoordinator(Context context, Listener delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    public void getInstanceById() throws JSONException {

        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject("{\"instance_id\":\""+ SpotlightConfig.INSTANCE_ID +"\"}");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        this.fetchInstanceAppData(jsonBody, BACKEND_BASE_URL + "/get-instance-by-id");
    }

    public void createCelebrityToken(String celebrity_url) throws JSONException {
        String url = BACKEND_BASE_URL + "/create-token-celebrity/" + celebrity_url;
        createToken(url);
    }

    public void createHostToken(String host_url) throws JSONException {
        String url = BACKEND_BASE_URL + "/create-token-host/" + host_url;
        createToken(url);
    }

    public void createFanToken(String fan_url) throws JSONException {
        String url = BACKEND_BASE_URL + "/create-token-fan/" + fan_url;
        createToken(url);
    }

    public void createFanTokenAnalytics(String fan_url) throws JSONException {
        String url = BACKEND_BASE_URL + "/create-token-fan";
        String user_id = getUserId();


        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("fan_url", fan_url);
            jsonBody.put("user_id", user_id);
            jsonBody.put("os", "Android API " + android.os.Build.VERSION.SDK_INT);
            jsonBody.put("is_mobile", "true");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                delegate.onDataReady(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        });

        jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        reqQueue.add(jor);


    }

    public void createToken(String url) {
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                delegate.onDataReady(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        });

        jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        reqQueue.add(jor);
    }

    public void fetchInstanceAppData(JSONObject jsonBody, String url) {
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                delegate.onDataReady(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        });

        jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        reqQueue.add(jor);
    }

    public void fetchEventData(JSONObject jsonBody, String url) {
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                delegate.onDataReady(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        });

        jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        reqQueue.add(jor);
    }

    public void sendGetInLine(String eventId) throws JSONException {
        String url = BACKEND_BASE_URL + "/metrics/get-inline";
        String user_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);


        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("event_id", eventId);
            jsonBody.put("user_id", user_id);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                delegate.onDataReady(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        });

        jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        reqQueue.add(jor);
    }

    public void leaveEvent(String eventId) throws JSONException {
        String url = BACKEND_BASE_URL + "/metrics/leave-event";
        String user_id = getUserId();


        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("event_id", eventId);
            jsonBody.put("user_id", user_id);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                delegate.onDataReady(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onWebServiceCoordinatorError(error);
            }
        });

        jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        reqQueue.add(jor);
    }

    public static interface Listener {
        void onDataReady(JSONObject jsonData);
        void onWebServiceCoordinatorError(Exception error);
    }

    private String getUserId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}

