package com.cs5248.two.streamingclient;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cs5248.two.streamingclient.util.CollectionUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

public class UploadFile extends AsyncTask<String, Integer, Void> {
    private static final String LOG_TAG = UploadFile.class.getSimpleName();

    private ProgressBar mUploadProgress;
    private TextView mTextView;
    private int mSegmentsUploaded = 0;
    private int mTotalSegments = 0;
    private String mVideoTitle;
    private String mDeviceId;
    private String mDirectory;
    private PopupWindow mPopUp;
    private int mUploadAttempt = 0;

    public UploadFile(ProgressBar uploadProgress, TextView textView, String directory,
                      String videoTitle, String deviceId, PopupWindow popUp){
        this.mUploadProgress = uploadProgress;
        this.mTextView = textView;
        this.mVideoTitle = videoTitle;
        this.mDeviceId = deviceId;
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

    @Override
    protected Void doInBackground(String... params) {
        if (CollectionUtils.isEmpty(params)) {
            Log.e(LOG_TAG, "Invalid input parameter");
            return null;
        }
        String upLoadServerUri = params[0];
        Log.d(LOG_TAG, "Upload URL: " + upLoadServerUri);
        ArrayList<String> segmentList = getFiles(mDirectory);
        String totalStreamlets = Integer.toString(segmentList.size());
        mTotalSegments = segmentList.size();
        for (int i = mSegmentsUploaded; i < segmentList.size(); i++) {

            try {
                String sourceFileUri = segmentList.get(i);
                Log.d(LOG_TAG, "Trying to upload the file " + sourceFileUri);
                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = new File(sourceFileUri);
                Log.d(LOG_TAG, "Device id is " + mDeviceId);
                String streamletNo = Integer.toString(i);

                if (sourceFile.isFile()) {

                    try {
                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(
                                sourceFile);
                        URL url = new URL(upLoadServerUri);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("fileToUpload", sourceFileUri);
                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                        //send other params
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"videoID\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(mVideoTitle);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"mDeviceId\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(mDeviceId);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"seqNUM\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(streamletNo);
                        dos.writeBytes(lineEnd); //to add multiple parameters write Content-Disposition: form-data; name=\"your parameter name\"" + crlf again and keep repeating till here :)
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"totalStreamlets\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(totalStreamlets);//your parameter value
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();

                        if (serverResponseCode == 200) {
                            System.out.println("Sent"+ sourceFile.getName() + "to server");
                        }
                        else{
                            throw new Exception("Could not upload file " + sourceFile.getName());
                        }

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();
                        mUploadAttempt = 0;
                        mSegmentsUploaded++;

                    } catch (Exception e) {
                        Log.i("DASH","Could not upload file " + sourceFile.getName() );
                        i--;
                        publishProgress(-1);
                        Thread.sleep(5000);
                        mUploadAttempt++;
                        System.out.println("Upload attempt is " + mUploadAttempt);
                        if (mUploadAttempt == 3) {
                            publishProgress(-2);
                            System.out.println("Max upload attempts reached");
                            break;
                        }
                        // dialog.dismiss();
                        e.printStackTrace();
                        continue;
                    }
                }

                if ( mSegmentsUploaded <= mTotalSegments){
                    publishProgress((mSegmentsUploaded * 100) / (mTotalSegments -1));
                }
            } catch (Exception ex) {
                // dialog.dismiss();

                ex.printStackTrace();
            }
        }
        return null;
    }
}
