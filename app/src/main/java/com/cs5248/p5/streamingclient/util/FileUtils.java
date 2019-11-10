package com.cs5248.p5.streamingclient.util;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileUtils {
    public static String getStoragePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }

    public static String getStringFromFile(String filePath) throws IOException {
        FileInputStream fin = new FileInputStream(filePath);
        byte[] bytes = IOUtils.toByteArray(fin);
        fin.close();
        return new String(bytes);
    }

    public static List<File> getFilesInDir(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files.length == 0) {
            return new ArrayList<>(0);
        } else {
            List<File> results = Arrays.asList(files);
            Collections.sort(results);
            return results;
        }
    }
}
