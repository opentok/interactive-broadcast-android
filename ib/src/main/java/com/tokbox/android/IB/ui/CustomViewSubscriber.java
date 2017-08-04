package com.tokbox.android.IB.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.tokbox.android.IB.R;

public class CustomViewSubscriber extends RelativeLayout {

    private Context mContext;
    private View rootView;
    private ProgressBar mLoadingSub;
    private RelativeLayout mAvatar;
    private static final String LOG_TAG = CustomViewSubscriber.class.getSimpleName();

    public CustomViewSubscriber(Context context) {
        super(context);
        mContext = context;
        Log.i(LOG_TAG, "init");
        init();
    }

    public CustomViewSubscriber(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }


    private void init() {
        rootView = inflate(mContext, R.layout.subscriber, this);
        mLoadingSub = (ProgressBar) findViewById(R.id.loadingSpinner);
        mAvatar = (RelativeLayout) findViewById(R.id.avatar);
    }

    public void displayAvatar(Boolean show) {
        if(show) {
            mAvatar.setVisibility(View.VISIBLE);
        } else {
            mAvatar.setVisibility(View.GONE);
        }
    }

    public void displaySpinner(Boolean show) {
        if(show) {
            mLoadingSub.setVisibility(View.VISIBLE);
        } else {
            mLoadingSub.setVisibility(View.GONE);
        }
    }

}
