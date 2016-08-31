![logo](tokbox-logo.png)

# OpenTok Interactive Broadcast Solution for Android<br/>Version 1.0

This document describes how to create a OpenTok Interactive Broadcast Solution mobile app for Android. You will learn how to set up the API calls to use the instance ID for the backend account, set up the role and name of the mobile participant, and connect the participant with a specified event.

This guide has the following sections:

* [Prerequisites](#prerequisites): A checklist of everything you need to get started.
* [Quickstart](#quickstart): A step by step tutorial to help you develop a basic Interactive Broadcast Solution application.
* [Complete code example](#complete-code-example): This is the complete code example that you will develop in this tutorial. You can skip the tutorial and use this example to get started quickly with your own application development.

_**NOTE:** The **Interactive Broadcast Solution** only supports landscape orientation on mobile devices._

_**IMPORTANT:** In order to deploy the OpenTok Interactive Broadcast Solution, your web domain must use HTTPS._


## Prerequisites

To be prepared to develop your first Interactive Broadcast Solution mobile app:

1. Install [Android Studio](http://developer.android.com/intl/es/sdk/index.html).
2. Review the [OpenTok Android SDK Requirements](https://tokbox.com/developer/sdks/android/#developerandclientrequirements).
3. Your app will need a **Session ID**, **Token**, and **API Key**, which you can get at the [OpenTok Developer Dashboard](https://dashboard.tokbox.com/).

_**NOTE**: The OpenTok Developer Dashboard allows you to quickly run this sample program. For production deployment, you must generate the **Session ID** and **Token** values using one of the [OpenTok Server SDKs](https://tokbox.com/developer/sdks/server/)._

## Quickstart

To get up and running quickly with your app, go through the following steps in the tutorial provided below:

1. [Importing the Android Studio Project](#importing-the-android-studio-project)
2. [Add the OpenTok Interactive Broadcast Solution library](#addlibrary)
3. [Add the OpenTok Android SDK](#add-the-opentok-android-sdk)
4. [Configure the Interactive Broadcast Solution user](#configure-the-interactive-broadcast-solution-user)
5. [Create a web service coordinator](#create-a-web-service-coordinator)
6. [Implement the web service coordinator listener interface](#implement-the-web-service-coordinator-listener-interface)

View the [Complete code example](#complete-code-example).

### Importing the Android Studio Project

In Android Studio, configure a new project.

1. Specify your **Application Name**, **Company Domain**, and **Project Location**.
2. Using the **Phone and Tablet** platform, select **API 16** as the **Minimum SDK**.
3. Add a **Blank Activity**. You can use the default values and click **Finish**.
4. Edit **AndroidManifest.xml** and add the following permissions:

    ```
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

<ol>

<li>Modify the <b>build.gradle</b> for your solution and add the following code snippet to the section labeled <b>repositories</b>:

<code>
maven { url  "http://tokbox.bintray.com/maven" }
</code>

</li>

<li>Modify the <b>build.gradle</b> for your activity and add the following code snippet to the section labeled <b>dependencies</b>: 


<code>
compile 'com.mcxiaoke.volley:library:1.0.19'
</code>

</li>

</ol>

  _**NOTE**: Since dependencies are transitive with Maven, it is not necessary to explicitly reference the TokBox Common Accelerator Session Pack and the Annotations Kit with this option._



### Add the OpenTok Android SDK

To add the OpenTok Android SDK to your project:

<ol>

<li>Modify the <b>build.gradle</b> for your solution and add the following code snippet to the section labeled <b>repositories</b>:

<code>
maven { url  "http://tokbox.bintray.com/maven" }
</code>

</li>

<li>Modify the <b>build.gradle</b> for your activity and add the following code snippet to the section labeled <b>dependencies</b>: 


<code>
compile com.opentok.android:opentok-android-sdk:2.8.+'
</code>

</li>

</ol>

For more information, see [Creating your own app using the OpenTok Android SDK](https://tokbox.com/developer/sdks/android/#creating-your-own-app-using-the-opentok-android-sdk).


### Configure the Interactive Broadcast Solution user

Now you are ready to add the Interactive Broadcast Solution user detail to your app. These will include the Instance ID and Backend Base URL you retrieved earlier (see [Prerequisites](#prerequisites)).

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
        IBConfig.INSTANCE_ID = "";

        /* Replace with the username.  */
        IBConfig.BACKEND_BASE_URL= "";
        ```

3. Set the Instance ID, Backend Base URL, user type and username in the code you just added:

   - The Instance ID is unique to your account. It is used to authorize your code to use the library and make requests to the backend, which is hosted at the location identified by the Backend Base URL. You can use your Instance ID for multiple events.
   - The Backend Base URL is the endpoint to the web service hosting the events, and should be provided by TokBox.
   - The user type must be assigned. You can hard code such values or set up UI controls such as radio buttons. One of the following values for the user type must be assigned: `fan`, `celebrity`, or `host`. There should only be one celebrity and host per event.
   - The Username is optional, and is displayed in chats with the producer and when fans get in line. You can set up manual entry, for example, with an editable text field.




### Create a web service coordinator

Now that you have configured the user, you can set up the code required for the user to join an event and interact using the Interactive Broadcast Solution service. To communicate with the backend web service and handle events on behalf of the user, you will use a `WebServiceCoordinator` object.


1. Add the following local variables to your `EventListActivity` class:

   ```java
   private WebServiceCoordinator mWebServiceCoordinator;
   private static final String LOG_TAG = "Event Log";
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
                Toast.makeText(getApplicationContext(),"Invalid instance ID", Toast.LENGTH_SHORT).show();
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
                getInstanceId();
            }
        }, 5000);
    }

    ```


4. Note that the methods to display the events are not shown in this example. You can implement your own versions of these, and you can view the complete sample app by extracting it from the .zip file provided by TokBox.


## Complete code example

You have completed the task of setting up a fully working example that uses the OpenTok Interactive Broadcast Solution! You can add processing for events and errors, and begin using your program.


## Additional information

For information on how to set up archiving on an Interactive Broadcast (IB) instance, click <a href="./ARCHIVING.md">here</a>.
