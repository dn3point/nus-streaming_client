package com.cs5248.two.streamingclient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.legacy.app.FragmentCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class VideoRecordFragment extends Fragment
    implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {
    private static final String LOG_TAG = VideoRecordFragment.class.getSimpleName();

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    public static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    private CameraView mCameraView;
    private Button mRecordButton;

    private boolean mIsRecording;
    private MediaRecorder mMediaRecorder;
    private String mVideoAbsolutePath;
    private CameraDevice mCamera;
    private Size mCameraSize;
    private Size mVideoSize;
    private CameraCaptureSession mCameraSession;
    private CaptureRequest.Builder mCaptureBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundHandlerThread;
    private Integer mSensorOrientation;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCamera = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mCameraView) {
                configureTransform(mCameraView.getWidth(), mCameraView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCamera = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_record:
                if (mIsRecording) {
                    stopRecord();
                } else {
                    startRecord();
                }
                break;
        }
    }

    private void startRecord() {
        if (mCamera == null || !mCameraView.isAvailable() || mCameraSize == null) {
            return;
        }
        try {
            closeCameraSession();
            setupMediaRecorder();
            SurfaceTexture texture = mCameraView.getSurfaceTexture();
            if (texture != null) {
                texture.setDefaultBufferSize(mCameraSize.getWidth(), mCameraView.getHeight());
                mCaptureBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                List<Surface> surfaces = new ArrayList<>();

                Surface previewSurface = new Surface(texture);
                surfaces.add(previewSurface);
                mCaptureBuilder.addTarget(previewSurface);

                Surface recordSurface = mMediaRecorder.getSurface();
                surfaces.add(recordSurface);
                mCaptureBuilder.addTarget(recordSurface);

                mCamera.createCaptureSession(surfaces,
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCameraSession = session;
                                updatePreview();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mRecordButton.setText(R.string.btn_stop);
                                        mIsRecording = true;
                                        mMediaRecorder.start();
                                    }
                                });
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                if (getActivity() == null) {
                                    Toast.makeText(
                                            getActivity(),
                                            "Capture failed",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }, mBackgroundHandler);
            }
        } catch (CameraAccessException | IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void setupMediaRecorder() throws IOException {
        if (getActivity() == null) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mVideoAbsolutePath == null || mVideoAbsolutePath.isEmpty()) {
            mVideoAbsolutePath = getVideoPath(getActivity());
        }
        mMediaRecorder.setOutputFile(mVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(3500000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rot = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rot));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rot));
                break;
        }
        mMediaRecorder.prepare();
    }

    private String getVideoPath(Context context) {
        File dir = context.getExternalFilesDir(null);
        return dir == null ? "" : dir.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
    }

    private void stopRecord() {
        mIsRecording = false;
        mRecordButton.setText(R.string.btn_record);
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        if (getActivity() != null) {
            Toast.makeText(
                    getActivity(),
                    "Save video to: " + mVideoAbsolutePath,
                    Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "Save video to: " + mVideoAbsolutePath);
        }
        mVideoAbsolutePath = null;
        startPreview();
    }

    private void startPreview() {
        if (mCamera == null || !mCameraView.isAvailable() || mCameraSize == null) {
            return;
        }
        try {
            closeCameraSession();
            SurfaceTexture texture = mCameraView.getSurfaceTexture();
            if (texture != null) {
                texture.setDefaultBufferSize(mCameraSize.getWidth(), mCameraView.getHeight());
                mCaptureBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                Surface previewSurface = new Surface(texture);
                mCaptureBuilder.addTarget(previewSurface);

                mCamera.createCaptureSession(Collections.singletonList(previewSurface),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCameraSession = session;
                                updatePreview();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                if (getActivity() != null) {
                                    Toast.makeText(
                                            getActivity(),
                                            "Capture failed",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void updatePreview() {
        if (mCamera == null) {
            return;
        }
        try {
            mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mCameraSession.setRepeatingRequest(
                    mCaptureBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void closeCameraSession() {
        if (mCameraSession != null) {
            mCameraSession.close();
            mCameraSession = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.video_recorder, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mCameraView = view.findViewById(R.id.camera_view);
        mRecordButton = view.findViewById(R.id.btn_record);
        mRecordButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mCameraView.isAvailable()) {
            openCamera(mCameraView.getWidth(), mCameraView.getHeight());
        } else {
            mCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closeCameraSession();
            if (mCamera != null) {
                mCamera.close();
                mCamera = null;
            }
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage());
            throw new RuntimeException("Interrupt when lock camera");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        if (!checkPermissions(VIDEO_PERMISSIONS)) {
            askPermissions();
            return;
        }
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = cameraManager.getCameraIdList()[0];

            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            mVideoSize = selectVideoSize(map.getOutputSizes(MediaRecorder.class));
            mCameraSize = selectCameraSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mCameraView.setRatio(mCameraSize.getWidth(), mCameraView.getHeight());
            } else {
                mCameraView.setRatio(mCameraSize.getHeight(), mCameraView.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            cameraManager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "No camera access", Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, e.getMessage());
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage());
            throw new RuntimeException("Interrupt when lock camera");
        }
    }

    private void configureTransform(int width, int height) {
        Activity activity = getActivity();
        if (mCameraView == null || mCameraSize == null || activity == null) {
            return;
        }
        int rot = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, mCameraSize.getHeight(), mCameraSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rot || Surface.ROTATION_270 == rot) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) height / mCameraSize.getHeight(),
                    (float) width / mCameraSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rot - 2), centerX, centerY);
        }
        mCameraView.setTransform(matrix);
    }

    private Size selectCameraSize(Size[] outputSizes, int width, int height, Size videoSize) {
        List<Size> sizeList = new ArrayList<>();
        int w = videoSize.getWidth();
        int h = videoSize.getHeight();
        for (Size option: outputSizes) {
            if (option.getHeight() == option.getWidth() * h / w &&
                option.getWidth() >= width &&
                option.getHeight() >= height) {
                sizeList.add(option);
            }
        }

        if (!sizeList.isEmpty()) {
            return Collections.min(sizeList, new CompareSizesByArea());
        } else {
            Log.e(LOG_TAG, "No suitable camera size");
            return outputSizes[0];
        }
    }

    private Size selectVideoSize(Size[] outputSizes) {
        for (Size size: outputSizes) {
            if (size.getWidth() == size.getHeight() *4 / 3 && size.getWidth() <= 720) {
                return size;
            }
        }
        Log.e(LOG_TAG, "No suitable size");
        return outputSizes[outputSizes.length - 1];
    }

    private void askPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPermissions(String[] permissions) {
        for (String permission: permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("CameraBackground");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }

    public static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
