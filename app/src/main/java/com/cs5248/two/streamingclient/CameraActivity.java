package com.cs5248.two.streamingclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class CameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, CameraRecordActivity.newInstance())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed. Stopping recording and existing!!");
        finish();
    }

}
