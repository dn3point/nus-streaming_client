package com.cs5248.p5.streamingclient;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class SegmentVideoTask extends AsyncTask<String, Integer, Void> {
    private static final String LOG_TAG = SegmentVideoTask.class.getSimpleName();

    private static final double DURATION = 3.0;
    private Context mContext;


    public SegmentVideoTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        if (params == null || params.length < 3) {
            Log.e(LOG_TAG, "Invalid inputs");
            throw new IllegalArgumentException("Invalid input parameters");
        }

        String videoPath = params[0];
        String output = params[1];
        String videoID = params[2];
        segment(videoPath, output, videoID);

        return null;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mContext, "Start to segment video", Toast.LENGTH_SHORT).show();
    }

    // Correct key frame track
    private double correctTimeToSyncSample(Track track, double endLimit, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        long[] trackSampleDurations = track.getSampleDurations();

        for (int i = 0; i < trackSampleDurations.length; i++) {

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += ((double) trackSampleDurations[i] / (double) track.getTrackMetaData().getTimescale());
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample >= endLimit) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }


    private void segment(String videoPath, String outputPath, String videoID) {
        double start = 0.0;
        double end = DURATION;
        int segNum = 1;
        try {
            while (segmentVideo(start, end, segNum, videoPath, outputPath, videoID)) {
                segNum++;
                start = end;
                end += DURATION;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private boolean segmentVideo(double startTime, double endTime, int segmentNumber,
                                 String videoPath, String outputPath, String videoId)
            throws IOException {
        Movie video = MovieCreator.build(videoPath);
        List<Track> tracks = video.getTracks();
        video.setTracks(new LinkedList<Track>());
        boolean timeCorrected = false;
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    throw new RuntimeException("The startTime has already been corrected.");
                }
                startTime = correctTimeToSyncSample(track, startTime, true);
                endTime = correctTimeToSyncSample(track, endTime, true);
                timeCorrected = true;
            }
        }

        if (startTime == endTime) {
            return false;
        }

        publishProgress(0, segmentNumber);

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = 0;
            long startSample = 0;
            long endSample = -1;
            long[] trackSampleDurations = track.getSampleDurations();
            for (int i = 0; i < trackSampleDurations.length; i++) {
                if (currentTime > lastTime && currentTime <= startTime) {
                    startSample = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    endSample = currentSample;
                }
                lastTime = currentTime;
                currentTime += ((double) trackSampleDurations[i]
                        / (double) track.getTrackMetaData().getTimescale());
                currentSample++;
            }
            Log.d(LOG_TAG, "Start time: " + startTime + ", end time: " + endTime);
            video.addTrack(new CroppedTrack(track, startSample, endSample));
        }
        Container out = new DefaultMp4Builder().build(video);
        String outputFile = outputPath + videoId + "_" + segmentNumber + ".mp4";
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             FileChannel fc = fos.getChannel()) {
            out.writeContainer(fc);
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int type = values[0];
        switch (type) {
            case 0:
                int seqNum = values[1];
                Toast.makeText(mContext, "Creating segment No." + seqNum, Toast.LENGTH_SHORT).show();
        }
    }

}
