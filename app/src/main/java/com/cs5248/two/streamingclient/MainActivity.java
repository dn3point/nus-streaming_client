package com.cs5248.two.streamingclient;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

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
    private PopupWindow uploadVideoPopupWindow;
    private ProgressBar uploadVideoProgressBar;
    private TextView uploadVideoProgressText;
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
        ArrayList<String> filesinfolder = GetFiles(getAppStoragePath(MainActivity.this));
        if (filesinfolder.size() > 0) {

            fileList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, filesinfolder));
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
                String dir = getAppStoragePath(MainActivity.this);
                segmentVideo(dir, videoTitle);
                uploadSingleVideo(dir, videoTitle);

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
                String dir = getAppStoragePath(MainActivity.this);
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
                String dir = getAppStoragePath(MainActivity.this);
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

    private String getAppStoragePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return dir.getAbsolutePath();
    }

    private String getStreamletStoragePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return dir.getAbsolutePath() + "/streamlets";
    }


    private void uploadSingleVideo(String directorypath, String fileName) {
        //ArrayList<String> filesinfolder = GetFiles(directorypath);
        String mainFile = directorypath + "/" + fileName;
        String segmentsPath = directorypath + "/streamlets/" + fileName + "/";
        File f = new File(mainFile);
        mContext = getApplicationContext();
        mainLayout = findViewById(R.id.content);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.uploadvideo_progress_popup, null);
        // Initialize a new instance of popup window
        uploadVideoPopupWindow = new PopupWindow(
                customView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        uploadVideoPopupWindow.setElevation(5.0f);
        uploadVideoPopupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);
        uploadVideoProgressBar = customView.findViewById(R.id.uploadProgress);
        uploadVideoProgressText = customView.findViewById(R.id.uploadTextView);
        UploadFile uploadObj = new UploadFile(uploadVideoProgressBar, uploadVideoProgressText, segmentsPath, f.getName(), "Samsung Galaxy Tab", uploadVideoPopupWindow);
        try {
            String uploadUrl = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("upload_url", "http://server.com/upload.php");
            uploadObj.execute(uploadUrl);
        } catch (Exception e) {
            e.printStackTrace();
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

    private void segmentVideo(String directorypath, String fileName) {
        //Segment the video in splits of 3 seconds
        Log.i("DASH", "Inside segmentVideo()");
        //ArrayList<String> filesinfolder = GetFiles(directorypath);
        String filepath = directorypath + "/" + fileName;
        File f = new File(filepath);
        outputPath = getSegmentFolder(f.getName());
        Log.i("DASH", "Path where segments have to be saved is " + outputPath);

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
        File segmentFolder = new File(getAppStoragePath(MainActivity.this), folderName);
        segmentFolder.mkdirs();

        return segmentFolder.getPath() + "/";
    }
}
