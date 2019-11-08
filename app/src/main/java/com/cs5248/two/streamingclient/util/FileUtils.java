package com.cs5248.two.streamingclient.util;

import android.content.Context;

public class FileUtils {
    public static String getStoragePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }
}
