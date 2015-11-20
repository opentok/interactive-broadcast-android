package com.agilityfeat.spotlight.events;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.ImageView;

import com.agilityfeat.spotlight.config.SpotlightConfig;
import com.squareup.picasso.Picasso;

public class EventUtils {

    private static final String LOG_TAG = EventUtils.class.getSimpleName();

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
        if(image.equals("null") || image.equals("")) image = SpotlightConfig.DEFAULT_EVENT_IMAGE;
        image = SpotlightConfig.FRONTEND_URL + image;
        Log.i(LOG_TAG, "loadEventImage.." + image);
        Picasso.with(context).load(image).fit().centerCrop().into(imgView);
    }

    public static Typeface getFont(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.ttf");
    }
}