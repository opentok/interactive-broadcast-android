package com.agilityfeat.spotlight.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.agilityfeat.spotlight.config.SpotlightConfig;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;


public class SocketCoordinator {

    private static final String LOG_TAG = SocketCoordinator.class.getSimpleName();

    private Socket mSocket;

    {
        try {
            mSocket = IO.socket(SpotlightConfig.SIGNALING_URL);
        } catch (URISyntaxException e) {

        }
    }

    public void connect() {
        mSocket.connect();

        Log.i(LOG_TAG, "connected");
    }

    public void emitJoinRoom(String sessionIdProducer) {
        if(mSocket.connected()) {
            mSocket.emit("joinRoom", sessionIdProducer);
            Log.i(LOG_TAG, "joinRoom emitted");
        } else {
            Log.i(LOG_TAG, "joinRoom not emitted");
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void on(String sessionIdProducer) {
        if(mSocket.connected()) {
            mSocket.emit("joinRoom", sessionIdProducer);
            Log.i(LOG_TAG, "joinRoom emitted");
        } else {
            Log.i(LOG_TAG, "joinRoom not emitted");
        }
    }



    public void SendSnapShot(JSONObject data) {
        mSocket.emit("mySnapshot", data);
    }

    public void disconnect() {
        if(mSocket.connected()) {
            mSocket.disconnect();
            //mSocket.off("new message", onNewMessage);
        }
    }

}

