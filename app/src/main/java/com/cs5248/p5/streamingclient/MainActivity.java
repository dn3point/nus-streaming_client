package com.cs5248.p5.streamingclient;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.cs5248.p5.streamingclient.util.FileUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String SEGMENT_FOLDER = "segments";

    private TextView popupTitleTextView;
    private PopupWindow popupWindow;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        FloatingActionButton fab = findViewById(R.id.btn_capture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, VideoActivity.class);
                startActivity(intent);
            }
        });
        listVideos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        listVideos();
    }

    public void listVideos() {
        TextView mainTitleText = findViewById(R.id.title_txt_view);
        ListView videoListView = findViewById(R.id.video_list_view);
        List<String> videoFileList = FileUtils.getMP4FileNameArrayList(
                FileUtils.getStoragePath(mContext));
        if (!videoFileList.isEmpty()) {
            videoListView.setAdapter(new ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_list_item_1, videoFileList));
            mainTitleText.setText(R.string.title_list);
            videoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    String itemName = parent.getAdapter().getItem(position).toString();
                    showPopupWindow(itemName);
                }
            });
        } else {
            mainTitleText.setText(R.string.title_empty);
            videoListView.setAdapter(null);
        }
    }

    public void showPopupWindow(final String itemName) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popup_window, null);
        popupWindow = new PopupWindow(
                view,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        float ele = 5;
        popupWindow.setElevation(ele);
        LinearLayout mainLayout = findViewById(R.id.content);
        popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);
        popupTitleTextView = view.findViewById(R.id.popup_txt_title);
        popupTitleTextView.setText(itemName);

        // Close popup
        ImageButton closeButton = view.findViewById(R.id.btn_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        // Play video
        TextView videoPopupPlay = view.findViewById(R.id.popup_txt_play_video);
        videoPopupPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                playVideo(itemName);
            }
        });


        // Upload video
        TextView videoPopupUpload = view.findViewById(R.id.popup_txt_upload_video);
        videoPopupUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                segmentVideo(itemName);
                uploadVideo(itemName);

            }
        });

        // Delete video
        TextView videoPopupDelete = view.findViewById(R.id.popup_txt_delete_video);
        videoPopupDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                deleteVideo(itemName);
            }
        });

    }

    private void uploadVideo(String videoName) {
        String videoSegmentFolder = FileUtils.getStoragePath(mContext)
                + "/" + SEGMENT_FOLDER + "/" + videoName + "/";
        UploadVideoTask uploadVideoTask = new UploadVideoTask(mContext,
                videoName.substring(0, videoName.lastIndexOf('.')),
                videoSegmentFolder);
        try {
            String uploadUrl = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("upload_url", getString(R.string.default_upload_url));
            uploadVideoTask.execute(uploadUrl);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(mContext, "Upload " + videoName + "failed. Please try again.",
                    Toast.LENGTH_SHORT).show();

        }
    }

    private void deleteVideo(String videoName) {
        new File(FileUtils.getStoragePath(mContext) + "/" + videoName).delete();
        File videoSegmentFolder = new File(FileUtils.getStoragePath(mContext)
                + "/" + SEGMENT_FOLDER + "/" + videoName);

        try {
            if (videoSegmentFolder.isDirectory()) {
                String[] videos = videoSegmentFolder.list();
                for (String video : videos) {
                    File currVideo = new File(videoSegmentFolder.getPath(), video);
                    currVideo.delete();
                }
                videoSegmentFolder.delete();
            }
            Toast.makeText(mContext,
                    "Deleted video " + videoName + " and its segments.",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(mContext,
                    "Delete video " + videoName + " and its segments failed.",
                    Toast.LENGTH_SHORT).show();
        } finally {
            listVideos();
        }
    }

    private void playVideo(String videoName) {
        File video = new File(FileUtils.getStoragePath(mContext) + "/" + videoName);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri apkURI = FileProvider.getUriForFile(
                mContext,
                mContext.getApplicationContext()
                        .getPackageName() + ".provider", video);
        intent.setDataAndType(apkURI, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);
    }

    private void segmentVideo(String videoName) {
        String outputPath = getSegmentFolder(videoName);
        String videoPath = FileUtils.getStoragePath(mContext) + "/" + videoName;
        Log.d(LOG_TAG, "Segment path: " + outputPath);

        SegmentVideoTask segmentVideoTask = new SegmentVideoTask(mContext);
        try {
            segmentVideoTask.execute(videoPath, outputPath,
                    videoName.substring(0, videoName.lastIndexOf('.')));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(mContext,
                    "Segment video " + videoName + " failed.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String getSegmentFolder(String innerFolderName) {
        String folderName = SEGMENT_FOLDER + "/" + innerFolderName;
        File segmentFolder = new File(FileUtils.getStoragePath(mContext), folderName);
        segmentFolder.mkdirs();

        return segmentFolder.getPath() + "/";
    }
}
