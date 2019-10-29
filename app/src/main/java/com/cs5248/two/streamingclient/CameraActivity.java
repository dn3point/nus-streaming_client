package com.cs5248.two.streamingclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class CameraActivity extends Activity {
    public static final String LOG_TAG = CameraActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new VideoRecordFragment());
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(LOG_TAG, "Quit recording and back to video listing");
        finish();
    }
}
