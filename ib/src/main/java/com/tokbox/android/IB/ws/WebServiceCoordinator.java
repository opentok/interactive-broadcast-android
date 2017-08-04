package com.tokbox.android.IB.ws;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.tokbox.android.IB.config.IBConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 Represents an object to communicate with the backend web service and handle events on behalf of the user.
 */
public class WebServiceCoordinator {

    private static final String BACKEND_BASE_URL = IBConfig.BACKEND_BASE_URL;
    private static final String BACKEND_API_PREFIX = "/api/";
    private static final String LOG_TAG = WebServiceCoordinator.class.getSimpleName();

    private final Context context;
    private Listener delegate;
    private Boolean mConnected = false;


    /**
     * Monitors when the instance data is ready or there is an error using the WebServiceCoordinator
     *
     */
    public interface Listener {
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
     * Interface for the Authentication process
     *
     */
    public interface AuthenticationCallback {
         void onLoginSuccess();
         void onLoginError(Exception error);
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
     * Returns the events list filtering by Admin
     */
    public void getEventsByAdmin() {
        this.fetchInstanceAppData("event/get-events-by-admin?adminId=" + IBConfig.ADMIN_ID);
    }


    /**
     * Create the OpenTok token for the Fan role
     */
    public void createFanToken(String fan_url) {
        String url = BACKEND_BASE_URL + "/create-token-fan";

        JSONObject jsonBody = getFanParams(fan_url);

        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i(LOG_TAG, response.toString());
                mConnected = true;
                //delegate.onDataReady(response);
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

    /**
     * Create the OpenTok token for every role
     */
    public void createToken(String userUrl) {

        final String url = "event/create-token-" + IBConfig.USER_TYPE;
        final JSONObject jsonBody = new JSONObject();
        String authUrl = buildUrl("auth/token-" + IBConfig.USER_TYPE);

        try {
            jsonBody.put(IBConfig.USER_TYPE + "Url", userUrl);
            jsonBody.put("adminId", IBConfig.ADMIN_ID);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        getAuthToken(authUrl, jsonBody.toString(), new AuthenticationCallback() {

            @Override
            public void onLoginError(Exception error) {
                Log.d(LOG_TAG, error.getMessage());
            }

            @Override
            public void onLoginSuccess() {
                RequestQueue reqQueue = Volley.newRequestQueue(context);
                JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, buildUrl(url), jsonBody, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        delegate.onDataReady(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        delegate.onWebServiceCoordinatorError(error);
                    }
                }
                )
                {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String>  params = new HashMap<String, String>();
                        params.put("Content-Type", "application/json");
                        params.put("Authorization", "Bearer " + IBConfig.AUTH_TOKEN);
                        return params;
                    }
                };

                jor.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                reqQueue.add(jor);
            }
        });
    }

    /**
     * Create the OpenTok token for every role
     */
    public void createAuthToken(String userUrl) {

        final String url = "event/create-token-" + IBConfig.USER_TYPE;
        final JSONObject jsonBody = new JSONObject();
        String authUrl = buildUrl("auth/token-" + IBConfig.USER_TYPE);

        try {
            jsonBody.put(IBConfig.USER_TYPE + "Url", userUrl);
            jsonBody.put("adminId", IBConfig.ADMIN_ID);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unexpected JSON exception", e);
        }

        getAuthToken(authUrl, jsonBody.toString(), new AuthenticationCallback() {

            @Override
            public void onLoginError(Exception error) {
                delegate.onWebServiceCoordinatorError(error);
            }

            @Override
            public void onLoginSuccess() {
                delegate.onDataReady(null);
            }
        });
    }

    // Get a JWT from the server
    private void getAuthToken(String stringUrl, String jsonBody, AuthenticationCallback callback) {
        new AuthTokenTask(callback, context).execute(stringUrl, jsonBody);
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

    private void fetchInstanceAppData(String url) {
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JsonArrayRequest jor = new JsonArrayRequest(Request.Method.GET, buildUrl(url), null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Log.i(LOG_TAG, response.toString());
                JSONObject result = new JSONObject();
                try {
                    result.put("events", response);
                } catch (JSONException ex ) {
                    Log.d(LOG_TAG, ex.getMessage());
                }
                delegate.onDataReady(result);
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
                //delegate.onDataReady(response);
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

    private String buildUrl (String url) {
        return BACKEND_BASE_URL + BACKEND_API_PREFIX + url;
    }
}