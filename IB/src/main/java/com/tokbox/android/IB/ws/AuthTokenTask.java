package com.tokbox.android.IB.ws;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.opentok.android.Connection;
import com.tokbox.android.IB.config.IBConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get a JWT from the server
 */
public class AuthTokenTask extends AsyncTask<String, Void, Void> {

    final WebServiceCoordinator.AuthenticationCallback callback;
    private static final String LOG_TAG = AuthTokenTask.class.getSimpleName();
    private final Context context;

    AuthTokenTask(WebServiceCoordinator.AuthenticationCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        IBConfig.AUTH_TOKEN = "";
    }

    @Override
    protected Void doInBackground(String... params)
    {
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody = new JSONObject(params[1]);
        } catch (JSONException ex) {
            Log.d(LOG_TAG, ex.getMessage());
        }

        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.POST, params[0], jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    IBConfig.AUTH_TOKEN = response.getString("token");
                    callback.onLoginSuccess();
                } catch (JSONException ex) {
                    Log.d(LOG_TAG, ex.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onLoginError(error);
        }
        });
        reqQueue.add(jor);
        return null;
    }
}
