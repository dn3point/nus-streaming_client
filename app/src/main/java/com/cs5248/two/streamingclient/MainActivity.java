package com.cs5248.two.streamingclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private ImageButton mImageButton;
    private SurfaceHolder mSurfaceHolder;

    private Camera mCamera;
    private Camera.Parameters mParameters;
    private VideoEncoder mVideoEncoder;

    public static final int REQ_CODE = 10001;

    public static final int WIDTH = 1080;
    public static final int HEIGHT = 720;
    public static final int FPS = 30;
    public static final int BIT_RATE = (int) (30 * Math.pow(10, 6));

    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<>(10);

    private static String[] PERMISSIONS = {
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.cameraView);
        mImageButton = findViewById(R.id.record_button);


        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQ_CODE);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    showAlert();
                }
                break;
            default:
                break;
        }
    }

    private void showAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Cannot operate without Camera permission")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    private void init() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera = getBackCamera();
        startCamera(mCamera);
        mVideoEncoder = new VideoEncoder(WIDTH, HEIGHT, FPS, BIT_RATE);
        mVideoEncoder.startEncoderThread();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    private Camera getBackCamera() {
        Camera c = null;
        try {
            c = Camera.open(0);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return c;
    }

    private void startCamera(Camera mCamera) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                mParameters = mCamera.getParameters();
                mParameters.setPreviewFormat(ImageFormat.NV21);
                mParameters.setPreviewSize(WIDTH, HEIGHT);
                mCamera.setParameters(mParameters);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopStream(View view) {
        stop();
    }

    private void stop() {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mVideoEncoder.stopThread();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
        // TODO Auto-generated method stub
        putYUVData(data);
    }

    private void putYUVData(byte[] buffer) {
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
        }
        YUVQueue.add(buffer);
    }

    private boolean checkPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
