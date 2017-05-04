package com.tokbox.android.IBSample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import com.tokbox.android.IB.config.IBConfig;

public class MainActivity extends AppCompatActivity {

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    private final int permsRequestCode = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStartClicked(View v) {
        RadioButton rdFan = (RadioButton) findViewById(R.id.radioButton);
        RadioButton rdCeleb = (RadioButton) findViewById(R.id.radioButton2);
        RadioButton rdHost = (RadioButton) findViewById(R.id.radioButton3);
        EditText userName = (EditText) findViewById(R.id.editText);

        //Setting the userType
        if(rdFan.isChecked()) {
            IBConfig.USER_TYPE = "fan";
        } else if(rdHost.isChecked()) {
            IBConfig.USER_TYPE = "host";
        } else {
            IBConfig.USER_TYPE = "celebrity";
        }

        //Replace with the admin ID hashed
        IBConfig.ADMIN_ID = "fBLBS9NPHYUitE3KtVghn4yI9ke2";

        //Replace with the BACKEND URL
        IBConfig.BACKEND_BASE_URL = "http://192.168.0.14:3001";

        //Setting the userName
        if((!userName.getText().toString().equals(""))) {
            IBConfig.USER_NAME = userName.getText().toString();
        }
        start();
    }

    public void start() {
        Intent localIntent;
        localIntent = new Intent(MainActivity.this, EventListActivity.class);
        startActivity(localIntent);
    }




}


