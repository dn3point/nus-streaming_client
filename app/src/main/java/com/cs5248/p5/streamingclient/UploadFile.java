package com.cs5248.p5.streamingclient;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.FreeBox;
import com.coremedia.iso.boxes.MovieBox;
import com.cs5248.p5.streamingclient.util.CollectionUtils;
import com.cs5248.p5.streamingclient.util.FileUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UploadFile extends AsyncTask<String, Integer, Void> {
    private static final String LOG_TAG = UploadFile.class.getSimpleName();

    private ProgressBar mUploadProgress;
    private TextView mTextView;
    private int mSegmentsUploaded = 0;
    private int mTotalSegments = 0;
    private String mVideoTitle;
    private String mDirectory;
    private PopupWindow mPopUp;
    private int mUploadAttempt = 0;

    public UploadFile(ProgressBar uploadProgress, TextView textView, String directory,
                      String videoTitle, PopupWindow popUp){
        this.mUploadProgress = uploadProgress;
        this.mTextView = textView;
        this.mVideoTitle = videoTitle;
        this.mDirectory = directory;
        this.mPopUp = popUp;
    }

    public ArrayList<String> getFiles(String directory){
        Log.d(LOG_TAG, "The segment path is " + directory);
        ArrayList<String> Myfiles = new ArrayList<>();
        File f = new File(directory);
        File[] files = f.listFiles();
        if(files.length==0){
            return Myfiles;
        }
        else{
            for(int i=0;i<files.length;i++) {
                System.out.println("File name is " + files[i].getName());
                Myfiles.add(files[i].getAbsolutePath());
            }
        }
        Collections.sort(Myfiles);
        return Myfiles;
    }

    @Override
    protected void onPreExecute() {
        mUploadProgress.setMax(100);
        mTextView.setText("Started to upload segments ...");
    }

    @Override
    protected void onPostExecute(Void result){
        mPopUp.dismiss();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values[0] == -1) {
            mTextView.setText(mSegmentsUploaded + " segments uploaded but failed to load the next segment, trying again, Upload Attempt is "+ mUploadAttempt);
        }
        else if(values[0] == -2) {
            mTextView.setText(mSegmentsUploaded + " segments uploaded, rest failed due to network issues");
        }
        else {
            mUploadProgress.setProgress(values[0]);
            mTextView.setText(mSegmentsUploaded +1  + " segments uploaded");
        }
    }

    private HttpURLConnection setupConnection(
            String uploadUrl, String videoId, int segNum, String segUri)
        throws IOException {
        String fileString = FileUtils.getStringFromFile(segUri);
        String urlParameters = videoId + "\n" + segNum + "\n" + fileString;
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        URL url = new URL(uploadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength ));
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        return conn;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (CollectionUtils.isEmpty(params)) {
            Log.e(LOG_TAG, "Invalid input parameter");
            return null;
        }
        String upLoadServerUri = params[0];
        Log.d(LOG_TAG, "Upload URL: " + upLoadServerUri);
        ArrayList<String> segmentList = getFiles(mDirectory);
        for (int i = mSegmentsUploaded; i < segmentList.size(); i++) {
            String sourceFileUri = segmentList.get(i);
            String sourceFileName = new File(sourceFileUri).getName();
            Log.d(LOG_TAG, "Trying to upload the file " + sourceFileUri);
            try {
                HttpURLConnection conn = setupConnection(
                        upLoadServerUri,
                        mVideoTitle,
                        i,
                        sourceFileUri);

                int serverResponseCode = conn.getResponseCode();
                Log.d(LOG_TAG, conn.getResponseMessage());
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String s;
                    while ((s = br.readLine()) != null) {
                        Log.d(LOG_TAG, s);
                    }
                }
                if (serverResponseCode == 200) {
                    Log.d(LOG_TAG, "Sent"+ sourceFileName + "to server");
                }
                else {
                    throw new Exception("Could not upload file " + sourceFileName);
                }

                mUploadAttempt = 0;
                mSegmentsUploaded++;

            } catch (Exception e) {
                Log.d(LOG_TAG, "Could not upload file " + sourceFileName);
                i--;
                publishProgress(-1);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Log.e(LOG_TAG, ie.getMessage());
                }
                mUploadAttempt++;
                Log.d(LOG_TAG, "Upload attempt is " + mUploadAttempt);
                if (mUploadAttempt == 3) {
                    publishProgress(-2);
                    Log.d(LOG_TAG, "Max upload attempts reached");
                    break;
                }
                Log.e(LOG_TAG, e.getMessage());
                continue;
            }
            if (mSegmentsUploaded <= mTotalSegments){
                publishProgress((mSegmentsUploaded * 100) / (mTotalSegments -1));
            }
        }
        return null;
    }
}
