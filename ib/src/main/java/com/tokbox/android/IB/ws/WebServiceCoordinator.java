package com.tokbox.android.IB.ws;

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
import com.tokbox.android.IB.config.IBConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 Represents an object to communicate with the backend web service and handle events on behalf of the user.
 */
public class WebServiceCoordinator {

    private static final String BACKEND_BASE_URL = IBConfig.BACKEND_BASE_URL;
    private static final String LOG_TAG = WebServiceCoordinator.class.getSimpleName();

    private final Context context;
    private Listener delegate;
    private Boolean mConnected = false;


    /**
     * Monitors when the instance data is ready or there is an error using the WebServiceCoordinator
     *
     */
    public static interface Listener {
        /**
         * Called when the instance data is ready.
         *
         * @param jsonData  The instance data
         */
        void onDataReady(JSONObject jsonData);
        /**
         * Called when there is an error
         *
         * @param error  The exception error
         */
        void onWebServiceCoordinatorError(Exception error);
    }

    /**
     * Use this constructor to create a WebServiceCoordinator instance.
     *
     * @param context   The Android application context associated with this process.
     * @param delegate  The WebServiceCoordinator delegate
     *
     *
     */
     public WebServiceCoordinator(Context context, Listener delegate) {
        this.context = context;
        this.delegate = delegate;
    }

    /**
     * Whether the Session is connected or not.
     *
     * @return true if the Session is connected; false if it is not.
     */
    public Boolean isConnected() {
        return mConnected;
    }


    /**
     * Returns the InstanceData filtering by ID
     */
    public void getInstanceById() throws JSONException {

        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject("{\"instance_id\":\""+ IBConfig.INSTANCE_ID +"\"}");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        this.fetchInstanceAppData(jsonBody, BACKEND_BASE_URL + "/get-instance-by-id");
    }

    /**
     * Returns the events list filtering by Admin
     */
    public void getEventsByAdmin() throws JSONException {

        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject("{\"id\":\""+ IBConfig.ADMIN_ID +"\"}");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        this.fetchInstanceAppData(jsonBody, BACKEND_BASE_URL + "/get-events-by-admin");
    }

    /**
     * Create the OpenTok token for the Celebrity role
     */
    public void createCelebrityToken(String celebrity_url) throws JSONException {
        String url = "";
        if(IBConfig.ADMIN_ID != "") {
            url = BACKEND_BASE_URL + "/create-token-celebrity/" + IBConfig.ADMIN_ID + "/" + celebrity_url;
        } else {
            url = BACKEND_BASE_URL + "/create-token-celebrity/" + celebrity_url;
        }
        createToken(url);
    }

    /**
     * Create the OpenTok token for the Host role
     */
    public void createHostToken(String host_url) throws JSONException {
        String url = "";
        if(IBConfig.ADMIN_ID != "") {
            url = BACKEND_BASE_URL + "/create-token-host/" + IBConfig.ADMIN_ID + "/" + host_url;
        } else {
            url = BACKEND_BASE_URL + "/create-token-host/" + host_url;
        }
        createToken(url);
    }

    /**
     * Create the OpenTok token for the Fan role
     */
    public void createFanToken(String fan_url) throws JSONException {
        String url = BACKEND_BASE_URL + "/create-token-fan";

        JSONObject jsonBody = getFanParams(fan_url);

        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                mConnected = true;
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

    private JSONObject getFanParams(String fan_url) {
        String user_id = getUserId();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("fan_url", fan_url);
            jsonBody.put("admins_id", IBConfig.ADMIN_ID);
            jsonBody.put("user_id", user_id);
            jsonBody.put("os", "Android API " + Build.VERSION.SDK_INT);
            jsonBody.put("is_mobile", "true");

        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        return jsonBody;
    }

    private void createToken(String url) {
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
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

    private void fetchInstanceAppData(JSONObject jsonBody, String url) {
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

    private void fetchEventData(JSONObject jsonBody, String url) {
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

    private String getUserId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}

