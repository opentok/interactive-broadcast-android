package com.tokbox.android.IB.events;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.ImageView;

import com.tokbox.android.IB.R;
import com.tokbox.android.IB.config.IBConfig;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class EventUtils {

    private static final String LOG_TAG = EventUtils.class.getSimpleName();
    private static final String NON_THIN = "[^iIl1\\.,']";

    public static String getStatusNameById(String statusId) {
        String statusName = "";
        switch(statusId) {
            case "notStarted":
            case "preshow":
                statusName = "Not started";
                break;
            case "live":
                statusName = "Live";
                break;
            case "closed":
                statusName = "Closed";
                break;
        }
        return statusName;
    }

    public static void loadEventImage(Context context, String image, ImageView imgView) {
        if(image.equals("")) {
            /* Load the default img */
            Picasso.with(context).load(R.drawable.default_event_img).fit().centerCrop().into(imgView);
        } else {
            /* Load the img from firebase */
            Picasso.with(context).load(image).fit().centerCrop().into(imgView);
        }
    }

    public static Typeface getFont(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.ttf");
    }

    private static int textWidth(String str) {
        return (int) (str.length() - str.replaceAll(NON_THIN, "").length() / 2);
    }

    public static String ellipsize(String text, int max) {

        if (textWidth(text) <= max)
            return text;

        // Start by chopping off at the word before max
        // This is an over-approximation due to thin-characters...
        int end = text.lastIndexOf(' ', max - 3);

        // Just one long word. Chop it off.
        if (end == -1)
            return text.substring(0, max-3) + "...";

        // Step forward as long as textWidth allows.
        int newEnd = end;
        do {
            end = newEnd;
            newEnd = text.indexOf(' ', end + 1);

            // No more spaces.
            if (newEnd == -1)
                newEnd = text.length();

        } while (textWidth(text.substring(0, newEnd) + "...") < max);

        return text.substring(0, end) + "...";
    }

    public static String getUserType(String connectionData) {
        String userType = "";
        try {
            JSONObject data = new JSONObject(connectionData);
            userType = data.getString("userType");
        } catch (JSONException ex) {
            Log.d(LOG_TAG, ex.getMessage());
        } finally {
            return userType;
        }

    }
}