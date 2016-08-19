package com.tokbox.android.IB.common;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tokbox.android.IB.R;
import com.tokbox.android.IB.events.EventUtils;

public class Notification {

    private Context mContext;
    private RelativeLayout mStatusBar;
    private Handler mHandler = new Handler();
    private Boolean mCanHide = true;

    public enum TYPE {
        BACKSTAGE(R.color.status_backstage, R.string.status_backstage, 0),
        TEMPORARILLY_MUTED(R.color.temporarilly_muted, R.string.temporarilly_muted, 0),
        PRIVATE_CALL(R.color.status_private_call, R.string.status_private_call, 0);
        private int color;
        private int text;
        private int delaySeconds;

        private TYPE(int color, int text, int delaySeconds) {
            this.color = color;
            this.text = text;
            this.delaySeconds = delaySeconds;
        }
    }

    public Notification(Context context, RelativeLayout statusBar){
        this.mContext = context;
        this.mStatusBar = statusBar;
    }

    public void showConnectionLost(){
        showNotification(R.string.connection_lost);
    }
    public void showHlsReconnecting(){
        showNotification(R.string.hls_reconnecting);
    }



    public void showCantPublish(String userType){
        int message = userType.equals("host") ? R.string.cant_publish_host : R.string.cant_publish_celebrity;
        showNotification(message);
    }

    public void showUnableToJoinMessage() {
        showNotification(R.string.user_limit);
    }

    public void showThanksForParticipating() {
        Typeface font = EventUtils.getFont(mContext);
        Toast toast = Toast.makeText(mContext, R.string.thanks_for_participating, Toast.LENGTH_LONG);
        ViewGroup view = (ViewGroup) toast.getView();
        view.setBackgroundColor(ContextCompat.getColor(mContext, R.color.countdown_background_color));
        TextView messageTextView = (TextView) view.getChildAt(0);
        messageTextView.setTextSize(13);
        messageTextView.setTypeface(font);
        toast.show();
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

    public void showNotification(TYPE type){
        //mStatusBar.setBackground();
        TextView statusTextView = (TextView) mStatusBar.getChildAt(0);
        String text = mContext.getResources().getString(type.text);
        if(text.length() > 50) {
            statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        } else {
            statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }
        statusTextView.setText(text);
        statusTextView.setBackgroundColor(ContextCompat.getColor(mContext, type.color));
        mStatusBar.setBackgroundColor(ContextCompat.getColor(mContext, type.color));
        mStatusBar.setVisibility(View.VISIBLE);

        if(type.delaySeconds > 0) {
            mCanHide = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCanHide = true;
                    hide();
                }
            }, type.delaySeconds * 1000);
        }
    }

    public void hide(){
        if(mCanHide) {
            mStatusBar.setVisibility(View.GONE);
        }

    }
}
