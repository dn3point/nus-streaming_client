package com.cs5248.p5.streamingclient;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cs5248.p5.streamingclient.util.CollectionUtils;
import com.cs5248.p5.streamingclient.util.FileUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UploadFile extends AsyncTask<String, Integer, Void> {
    private static final String LOG_TAG = UploadFile.class.getSimpleName();
    private static final int MAX_RETRY = 3;

    private String mVideoId;
    private String mSegmentDir;

    private String mCurrSourceFileName;
    private int mNumOfSegments;
    private int mNumOfUploadedSegment;
    private int mNumOfAttempt;

    private Context mContext;

    public UploadFile(Context context, String videoId, String segmentDir) {
        mVideoId = videoId;
        mContext = context;
        mSegmentDir = segmentDir;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mContext,
                "Start to upload " + mVideoId + ".mp4", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int type = values[0];
        switch (type) {
            case 0:
                int segNo = values[1];
                Toast.makeText(mContext,
                        "Uploading segment " + mCurrSourceFileName
                                + ". (" + segNo + "/" + mNumOfSegments + ")",
                        Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(mContext, "Upload failed, retry: " + mNumOfAttempt,
                        Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(mContext, "Can't upload file "
                                + mCurrSourceFileName + ", please try again later.",
                        Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
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
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        return conn;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (CollectionUtils.isEmpty(params)) {
            throw new IllegalArgumentException("Invalid input parameter");
        }
        String uploadUrl = params[0];
        Log.d(LOG_TAG, "Upload URL: " + uploadUrl);
        List<File> segmentList = FileUtils.getFilesInDir(mSegmentDir);
        mNumOfSegments = segmentList.size();
        mNumOfUploadedSegment = 0;
        mNumOfAttempt = 0;
        for (int i = mNumOfUploadedSegment; i < segmentList.size(); i++) {
            File sourceFile = segmentList.get(i);
            mCurrSourceFileName = sourceFile.getName();
            String sourceFilePath = sourceFile.getAbsolutePath();
            Log.d(LOG_TAG, "Uploading segment from " + sourceFilePath);
            publishProgress(0, i + 1);
            try {
                HttpURLConnection conn = setupConnection(uploadUrl, mVideoId,
                        i, sourceFilePath);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String s;
                    while ((s = br.readLine()) != null) {
                        Log.d(LOG_TAG, s);
                    }
                }
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Sent" + mCurrSourceFileName + "to server");
                } else {
                    throw new Exception("Could not upload file " + mCurrSourceFileName);
                }

                mNumOfAttempt = 0;
                mNumOfUploadedSegment++;
            } catch (Exception e) {
                Log.d(LOG_TAG, "Could not upload file " + mCurrSourceFileName);
                i--;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Log.e(LOG_TAG, ie.getMessage());
                }
                mNumOfAttempt++;
                Log.d(LOG_TAG, "Upload attempt is " + mNumOfAttempt);
                publishProgress(1);
                if (mNumOfAttempt == MAX_RETRY) {
                    Log.d(LOG_TAG, "Max retry reached");
                    publishProgress(2);
                    break;
                }
                Log.e(LOG_TAG, e.getMessage());
                continue;
            }
        }
        return null;
    }
}
