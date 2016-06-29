package com.tokbox.android.IB.common;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tokbox.android.IB.R;
import com.tokbox.android.IB.events.EventUtils;

public class Notification {

    private Context mContext;

    public Notification(Context context){
        this.mContext = context;
    }

    public void showConnectionLost(){
        showNotification(R.string.connection_lost);
    }

    public void showCantPublish(String userType){
        int message = userType.equals("host") ? R.string.cant_publish_host : R.string.cant_publish_celebrity;
        showNotification(message);
    }

    public void showUnableToJoinMessage() {
        showNotification(R.string.user_limit);
    }

    public void  showNotification(int message){
        Typeface font = EventUtils.getFont(mContext);
        for(int i=0;i<3;i++) {
            Toast toast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
            ViewGroup view = (ViewGroup) toast.getView();
            view.setBackgroundColor(ContextCompat.getColor(mContext, R.color.countdown_background_color));
            view.setPadding(20,10,20,10);
            TextView messageTextView = (TextView) view.getChildAt(0);
            messageTextView.setTextSize(13);
            messageTextView.setTypeface(font);
            toast.show();
        }
    }
}
