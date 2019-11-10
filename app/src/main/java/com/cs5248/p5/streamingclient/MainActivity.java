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
import android.widget.ProgressBar;
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
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    ListView fileList;
    private TextView videoPopUpTitle;
    private TextView videoPopupPlay;
    private TextView videoPopupUpload;
    private TextView videoPopupDelete;
    private String outputPath;
    private PopupWindow videoPopupWindow;
    private PopupWindow splitVideoPopupWindow;
    private ProgressBar splitVideoProgressBar;
    private TextView splitVideoProgressText;
    private LinearLayout mainLayout;
    private Context mContext;
    private TextView mainTitleText;
    private FloatingActionButton fab;

    @Override
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
        mContext = getApplicationContext();
        ListAllVideos();
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
        ListAllVideos();
    }


    public void ListAllVideos() {
        mainTitleText = findViewById(R.id.title_txt_view);
        fileList = findViewById(R.id.video_list_view);
        ArrayList<String> filesinfolder = GetFiles(FileUtils.getStoragePath(MainActivity.this));
        if (filesinfolder.size() > 0) {

            fileList.setAdapter(new ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_list_item_1, filesinfolder));
            mainTitleText.setText("List of Videos");
            fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    String itemName = parent.getAdapter().getItem(position).toString();
                    System.out.println(itemName + "is clicked");
                    showVideoPopup(itemName);
                }
            });
        } else {
            mainTitleText.setText("No video files");
            fileList.setAdapter(null);
        }
    }

    public void openCamera() {
        Intent intent = new Intent(this, VideoActivity.class);
        startActivity(intent);
    }

    public void showVideoPopup(String videoName) {

        mContext = getApplicationContext();
        mainLayout = findViewById(R.id.content);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.popup_window, null);
        // Initialize a new instance of popup window
        videoPopupWindow = new PopupWindow(
                customView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        videoPopupWindow.setElevation(5.0f);

        ImageButton closeButton = customView.findViewById(R.id.ib_close);

        // Set a click listener for the popup window close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss the popup window
                videoPopupWindow.dismiss();
            }
        });

        videoPopupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);
        videoPopUpTitle = customView.findViewById(R.id.videopopup_title);
        videoPopUpTitle.setText(videoName);
        //set listeners for play, upload and delete buttons
        videoPopupUpload = customView.findViewById(R.id.videopopup_upload);
        videoPopupUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPopupWindow.dismiss();
                String videoTitle = videoPopUpTitle.getText().toString();
                Toast.makeText(MainActivity.this, "Splitting and uploading the video : " + videoTitle, Toast.LENGTH_LONG).show();
                //Call the function that splits into segments and uploads
                String dir = FileUtils.getStoragePath(MainActivity.this);
                segmentVideo(dir, videoTitle);
                uploadVideo(dir, videoTitle);

            }
        });

        videoPopupDelete = customView.findViewById(R.id.videopopup_delete);
        videoPopupDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPopupWindow.dismiss();
                String videoTitle = videoPopUpTitle.getText().toString();
                Toast.makeText(MainActivity.this, "Deleting the video : " + videoTitle, Toast.LENGTH_LONG).show();
                //Call the function that splits into segments and uploads
                String dir = FileUtils.getStoragePath(MainActivity.this);
                deleteVideo(dir, videoTitle);
            }
        });

        videoPopupPlay = customView.findViewById(R.id.videopopup_play);
        videoPopupPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPopupWindow.dismiss();
                String videoTitle = videoPopUpTitle.getText().toString();
                Toast.makeText(MainActivity.this, "Playing the video : " + videoTitle, Toast.LENGTH_LONG).show();
                //Call the function that splits into segments and uploads
                String dir = FileUtils.getStoragePath(MainActivity.this);
                playVideo(dir, videoTitle);
            }
        });

    }

    public ArrayList<String> GetFiles(String directorypath) {
        System.out.println("The directory path is " + directorypath);
        ArrayList<String> Myfiles = new ArrayList<String>();
        File f = new File(directorypath);
        File[] files = f.listFiles();
        if (files.length == 0) {
            return Myfiles;
        } else {
            for (int i = 0; i < files.length; i++) {
                System.out.println("File name is " + files[i].getName());
                String name = files[i].getName();
                if (name.contains(".mp4")) {
                    Myfiles.add(name);
                }
            }
        }
        return Myfiles;
    }

    private void uploadVideo(String baseDir, String videoName) {
        String segmentsPath = baseDir + "/streamlets/" + videoName + "/";
        UploadFile uploadObj = new UploadFile(mContext,
                videoName.substring(0, videoName.lastIndexOf('.')),
                segmentsPath);
        try {
            String uploadUrl = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("upload_url",
                    "http://http://monterosa.d2.comp.nus.edu.sg/~CS5248T5/post-video.php");
            uploadObj.execute(uploadUrl);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Toast.makeText(mContext, "Upload " + videoName + "failed. Please try again.",
                    Toast.LENGTH_SHORT).show();

        }
    }

    private void deleteVideo(String directorypath, String fileName) {
        String filepath = directorypath + "/" + fileName;
        File f = new File(filepath);
        f.delete();
        File dir = new File(directorypath + "/streamlets/" + fileName);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }
        dir.delete();
        ListAllVideos();
    }

    private void playVideo(String directorypath, String fileName) {
        String filepath = directorypath + "/" + fileName;
        File f = new File(filepath);
        Context mContext = getApplicationContext();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.setDataAndType(Uri.fromFile(f), "video/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri apkURI = FileProvider.getUriForFile(
                mContext,
                mContext.getApplicationContext()
                        .getPackageName() + ".provider", f);
        intent.setDataAndType(apkURI, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);

    }

    private void segmentVideo(String baseDir, String videoName) {
        //Segment the video in splits of 3 seconds
        Log.d(LOG_TAG, "Inside segmentVideo()");
        //ArrayList<String> filesinfolder = getFiles(directorypath);
        String filepath = baseDir + "/" + videoName;
        File f = new File(filepath);
        outputPath = getSegmentFolder(f.getName());
        Log.d(LOG_TAG, "Path where segments have to be saved is " + outputPath);

        //open a popup for the progress bar and then pass it to the segment function
        mContext = getApplicationContext();
        mainLayout = findViewById(R.id.content);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.splitvideo_progress_popup, null);
        // Initialize a new instance of popup window
        splitVideoPopupWindow = new PopupWindow(
                customView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        splitVideoPopupWindow.setElevation(5.0f);
        splitVideoPopupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);
        splitVideoProgressBar = customView.findViewById(R.id.segmentProgress);
        splitVideoProgressText = customView.findViewById(R.id.segmentTextView);

        CreateVideoSegments obj = new CreateVideoSegments(splitVideoProgressBar, splitVideoProgressText, splitVideoPopupWindow);
        try {
            obj.execute(filepath, outputPath, "3.0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSegmentFolder(String innerFolderName) {
        String folderName = "streamlets/" + innerFolderName;
        File segmentFolder = new File(FileUtils.getStoragePath(MainActivity.this), folderName);
        segmentFolder.mkdirs();

        return segmentFolder.getPath() + "/";
    }
}
