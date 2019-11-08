package com.cs5248.two.streamingclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class VideoActivity extends Activity {
    private static final String LOG_TAG = VideoActivity.class.getSimpleName();

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
        Log.d(LOG_TAG, "Back button is pressed");
        finish();
    }

}
