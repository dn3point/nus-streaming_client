package com.cs5248.two.streamingclient;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import static com.cs5248.two.streamingclient.Constant.SERVER_URL;
import static com.cs5248.two.streamingclient.Constant.UPLOAD_ENDPOINT;

public class VideoSegmentUploadTask extends AsyncTask<String, Void, Boolean> {

    private static final String LOG_TAG = VideoSegmentUploadTask.class.getSimpleName();

    @Override
    protected Boolean doInBackground(String... strings) {
        boolean result = false;
        if (strings == null || strings.length < 3) {
            Log.e(LOG_TAG, "No file path");
            return false;
        }
        String filePath = strings[0];
        String videoId = strings[1];
        String seqNo = strings[2];
        File video = new File(filePath);
        try {
            URL url = new URL(SERVER_URL + UPLOAD_ENDPOINT);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            String boundary = UUID.randomUUID().toString();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream request = new DataOutputStream(connection.getOutputStream());

            request.writeBytes("--" + boundary + "\r\n");
            request.writeBytes("Content-Disposition: form-data; name=\"seqNo\"\r\n\r\n");
            request.writeBytes(seqNo + "\r\n");
            request.writeBytes("--" + boundary + "\r\n");
            request.writeBytes("Content-Disposition: form-data; name=\"videoId\"\r\n\r\n");
            request.writeBytes(videoId + "\r\n");
            request.writeBytes("--" + boundary + "\r\n");
            request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + video.getName() + "\"\r\n\r\n");
            request.write(getFileContent(video));
            request.writeBytes("\r\n");

            request.writeBytes("--" + boundary + "--\r\n");
            request.flush();
            int respCode = connection.getResponseCode();

            switch (respCode) {
                case 200:
                    result = true;
                    break;
                default:
                    Log.w(LOG_TAG, "response code is: " + respCode);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            return result;
        }
    }

    private byte[] getFileContent(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            return bytes;
        }
    }
}
