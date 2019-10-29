package com.cs5248.two.streamingclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private String outputPath;
    private FrameLayout mainLayout;
    private Context context;
    private Activity mActivity;
    private ListView fileListView;
    private FloatingActionButton fab;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.btn_capture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
        listVideos();
    }

    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void listVideos() {
        fileListView = findViewById(R.id.fileListView);
        ArrayList<String> videoList = getVideos(getFileFolder(this));
        if (videoList.isEmpty()) {
            fileListView.setAdapter(null);
        } else {
            fileListView.setAdapter(new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, videoList));
            fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String itemName = parent.getAdapter().getItem(position).toString();
                    Log.i(LOG_TAG, "Click:" + itemName);
                    Toast.makeText(
                            MainActivity.this, "Click:" + itemName, Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    private ArrayList<String> getVideos(String folderPath) {
        Log.i(LOG_TAG, "Video folder path: " + folderPath);
        ArrayList<String> videoList = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            Log.i(LOG_TAG, "File: " + fileName);
            if (fileName.endsWith(".mp4")) {
                videoList.add(fileName);
            }
        }
        return videoList;
    }

    private String getFileFolder(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }
}
