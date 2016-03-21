package com.opentok.android.ib.events;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.ImageView;

import com.opentok.android.ib.config.IBConfig;
import com.squareup.picasso.Picasso;

public class EventUtils {

    private static final String LOG_TAG = EventUtils.class.getSimpleName();
    private static final String NON_THIN = "[^iIl1\\.,']";

    public static String getStatusNameById(String statusId) {
        String statusName = "";
        switch(statusId) {
            case "N":
            case "P":
                statusName = "Not started";
                break;
            case "L":
                statusName = "Live";
                break;
            case "C":
                statusName = "Closed";
                break;
        }
        return statusName;
    }

    public static void loadEventImage(Context context, String image, ImageView imgView) {
        if(image.equals("null") || image.equals("")) image = IBConfig.DEFAULT_EVENT_IMAGE;
        image = IBConfig.FRONTEND_URL + image;
        Log.i(LOG_TAG, "loadEventImage.." + image);
        Picasso.with(context).load(image).fit().centerCrop().into(imgView);
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
}