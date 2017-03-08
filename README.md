![logo](tokbox-logo.png)

------

# OpenTok Interactive Broadcast Solution for Android

This document describes how to create a OpenTok Interactive Broadcast Solution mobile app for Android. 

You will learn how to set up the API calls to use the admin ID for the backend account, set up the role and name of the mobile participant, and connect the participant with a specified event.

This guide has the following sections:

* [Prerequisites](#prerequisites): A checklist of everything you need to get started.
* [Quickstart](#quickstart): A step by step tutorial to help you develop a basic Interactive Broadcast Solution application.

_**NOTE:** The **Interactive Broadcast Solution** only supports landscape orientation on mobile devices._


## Prerequisites

To be prepared to develop your first Interactive Broadcast Solution mobile app:

1. Install [Android Studio](http://developer.android.com/intl/es/sdk/index.html).
2. Review the [OpenTok Android SDK Requirements](https://tokbox.com/developer/sdks/android/#developerandclientrequirements).

## Quickstart

To get up and running quickly with your app, go through the following steps in the tutorial provided below:

1. [Create a new Android Studio Project](#create-a-new-android-studio-project)
2. [Add the OpenTok Interactive Broadcast Solution library](#addlibrary)
3. [Add the OpenTok Android SDK](#add-the-opentok-android-sdk)
4. [Configure the Interactive Broadcast Solution user](#configure-the-interactive-broadcast-solution-user)
5. [Create a web service coordinator](#create-a-web-service-coordinator)
6. [Implement the web service coordinator listener interface](#implement-the-web-service-coordinator-listener-interface)

View the [Complete code example](#complete-code-example).

### Create a new Android Studio Project

1. Configure a new project.
2. Edit **AndroidManifest.xml** and add the following permissions:

    ```java
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    ```

    **NOTE**: If you are using Android Marshmallow or later, you must manage the permissions for the microphone and camera at runtime:

    ```java
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    requestPermissions(permissions, permsRequestCode);
    ```

    For more information, see [Requesting Permissions at Run Time](https://developer.android.com/training/permissions/requesting.html).


<h3 id=addlibrary> Add the OpenTok Interactive Broadcast Solution library</h3>

To installing the OpenTok Interactive Broadcast Solution library:

#### Downloading and Installing the AAR File

1.  Download the [OpenTok Interactive Broadcast Solution AAR](https://s3.amazonaws.com/artifact.tokbox.com/solution/rel/ibs/android/opentok-ib-android-1.0.4.zip).
1. Extract the **opentok-ib-android-1.0.4.aar** file.
1. Right-click the app name, select **Open Module Settings**, and click **+**.
1. Select **Import .JAR/.AAR Package** and click  **Next**.
1. Browse to the **OpenTok Interactive Broadcast Solution AAR** and click **Finish**.
1. Right-click the app name and select Open Module Settings.
1. Select the app module and Dependencies tab.
1. Click + to add a dependency, select Module Dependency, select the module name for your Interactive Broadcasting Solution AAR, and click OK.


_**NOTE**: Since some dependencies are not transitive, it is necessary to explicitly reference them._

<ol>

<li>Modify the <b>build.gradle</b> for your activity and add the following code snippet to the section labeled <b>dependencies</b>: <br/>

<code>

    compile 'com.squareup.picasso:picasso:2.5.2'


    compile 'com.android.volley:volley:1.0.0'

    
    compile('com.github.nkzawa:socket.io-client:0.4.1') {
        exclude group: 'org.json', module: 'json'
    }

    
</code>

</li>

</ol>


### Configure the Interactive Broadcast Solution user

Now you are ready to add the Interactive Broadcast Solution instance details to your app. These will include the Admin ID and Backend Base URL you retrieved earlier (see [Prerequisites](#prerequisites)).

1. Ensure that you have the following import statements in MainActivity.java:

    ```java
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
    ```

2. In MainActivity.java, add the following detail to your `onStartClicked()` method:


    ```java
    /* Replace with your instance ID  */
    IBConfig.ADMIN_ID = "";

    /* Replace with the username.  */
    IBConfig.BACKEND_BASE_URL= "";
    ```

3. Set the ADMIN ID, Backend Base URL, user type and username in the code you just added:

   - The ADMIN ID is unique to your account. It is used to authorize your code to use the library and make requests to the backend, which is hosted at the location identified by the Backend Base URL. You can use your ADMIN ID for multiple events.
   - The Backend Base URL is the endpoint to the web service hosting the events, and should be provided by TokBox.
   - The user type must be assigned. You can hard code such values or set up UI controls such as radio buttons. One of the following values for the user type must be assigned: `fan`, `celebrity`, or `host`. There should only be one celebrity and host per event.
   - The Username is optional, and is displayed in chats with the producer and when fans get in line. You can set up manual entry, for example, with an editable text field.




### Create a web service coordinator

Now that you have configured the user, you can set up the code required for the user to join an event and interact using the Interactive Broadcast Solution service. To communicate with the backend web service and handle events on behalf of the user, you will use a `WebServiceCoordinator` object.


1. Add the following local variables to your `EventListActivity` class:

   ```java
   private WebServiceCoordinator mWebServiceCoordinator;
   private static final String LOG_TAG = EventListActivity.class.getSimpleName();
   ```


2. Add the following statement to your `EventListActivity.onCreate()` method:

   ```java
   private onCreate()
   {
      mWebServiceCoordinator = new WebServiceCoordinator(this, this);
   }
   ```


### Implement the web service coordinator listener interface

The `WebServiceCoordinator.Listener` interface supports the `WebServiceCoordinator` object you just created by listening for events and responding to errors. The interface requires that you implement two methods:

 * An `onDataReady()` method that responds to events.
 * An `onWebServiceCoordinatorError()` method that handles errors.

1. Modify the `EventListActivity` class declaration to implement the `WebServiceCoordinator.Listener` interface, which will enable your `MainActivity` class to implement the methods that respond to events and errors:

    ```java
    public class EventListActivity extends AppCompatActivity
            implements WebServiceCoordinator.Listener {
       . . .
    }
    ```


2.  In your `EventListActivity` class, implement an `onDataReady()` method that listens for events from the Interactive Broadcast Solution server. Here is an example:


    ```java
    /**
    * Web Service Coordinator delegate methods
    */
    /**
     * Web Service Coordinator delegate methods
     */
    @Override
    public void onDataReady(JSONObject instanceAppData) {
        //Set instanceApp Data
        InstanceApp.getInstance().setData(instanceAppData);

        Boolean bSuccess = false;
        mArrEvents = new JSONArray();
        try {
            bSuccess = (Boolean)instanceAppData.get("success");
            if(instanceAppData.has("events")) {
                mArrEvents = instanceAppData.getJSONArray("events");
            }

            if(bSuccess) {
                //init socket
                initSocket();

                //Check the count of events.
                if(mArrEvents.length() > 1) {
                    showEventList();
                } else {
                    if(mArrEvents.length() == 1) {
                        showEvent();
                    } else {
                        Toast.makeText(getApplicationContext(),"No events were found", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Log.e(LOG_TAG, "Invalid instance ID");
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "parsing instanceAppData error", e);
        } finally {
            stopLoadingAnimation();
        }
    }
    ```


3. Implement an `onWebServiceCoordinatorError()` method in your `EventListActivity` class. Here is an example:

    ```java
    @Override
    public void onWebServiceCoordinatorError(Exception error) {
        Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
        Toast.makeText(getApplicationContext(),"Unable to connect to the server. Trying again in 5 seconds..", Toast.LENGTH_LONG).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getEventsByAdmin();
            }
        }, 5000);
    }
    ```


4. Note that the methods to display the events are not shown in this example. You can implement your own versions of these, and you can view the complete sample app by extracting it from the .zip file provided by TokBox.


## Complete code example

You have completed the task of setting up a fully working example that uses the OpenTok Interactive Broadcast Solution! You can add processing for events and errors, and begin using your program.
