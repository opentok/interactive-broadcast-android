package com.agilityfeat.spotlight;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.agilityfeat.spotlight.config.SpotlightConfig;

public class SelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);
    }


    public void onStartClicked(View v) {
        RadioButton rdFan = (RadioButton) findViewById(R.id.radioButton);
        RadioButton rdCeleb = (RadioButton) findViewById(R.id.radioButton2);
        RadioButton rdHost = (RadioButton) findViewById(R.id.radioButton3);

        if(rdFan.isChecked()) {
            SpotlightConfig.USER_TYPE = "fan";
        } else if(rdHost.isChecked()) {
            SpotlightConfig.USER_TYPE = "host";
        } else {
            SpotlightConfig.USER_TYPE = "celebrity";
        }

        start();
    }

    public void start() {
        Intent localIntent;
        localIntent = new Intent(SelectActivity.this, MainActivity.class);
        startActivity(localIntent);
        Log.i("selector", SpotlightConfig.USER_TYPE);
    }

}


