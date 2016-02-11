package com.opentok.android.spotlightsample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import com.opentok.android.spotlight.config.SpotlightConfig;
import com.opentok.android.spotlightsample.R;

public class MainActivity extends AppCompatActivity {

    private final String[] permissions = {"android.permission.RECORD_AUDIO", "android.permission.CAMERA"};
    private final int permsRequestCode = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //request Marshmallow camera permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, permsRequestCode);
        }
    }


    public void onStartClicked(View v) {
        RadioButton rdFan = (RadioButton) findViewById(R.id.radioButton);
        RadioButton rdCeleb = (RadioButton) findViewById(R.id.radioButton2);
        RadioButton rdHost = (RadioButton) findViewById(R.id.radioButton3);
        EditText userName = (EditText) findViewById(R.id.editText);

        //Setting the userType
        if(rdFan.isChecked()) {
            SpotlightConfig.USER_TYPE = "fan";
        } else if(rdHost.isChecked()) {
            SpotlightConfig.USER_TYPE = "host";
        } else {
            SpotlightConfig.USER_TYPE = "celebrity";
        }

        //Replace with the instance ID
        SpotlightConfig.INSTANCE_ID = "AAAA1";

        //Replace with the BACKEND URL
        SpotlightConfig.BACKEND_BASE_URL = "https://spotlight-tesla-mlb.herokuapp.com";

        //Setting the userName
        if((!userName.getText().toString().equals(""))) {
            SpotlightConfig.USER_NAME = userName.getText().toString();
        }
        start();
    }

    public void start() {
        Intent localIntent;
        localIntent = new Intent(MainActivity.this, EventListActivity.class);
        startActivity(localIntent);
    }

    @Override
    public void onRequestPermissionsResult ( int permsRequestCode, String[] permissions,
                                             int[] grantResults){
        switch (permsRequestCode) {

            case 200:
                boolean video = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean audio = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }


}


