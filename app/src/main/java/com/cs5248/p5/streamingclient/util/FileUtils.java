package com.cs5248.p5.streamingclient.util;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtils {
    public static String getStoragePath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath();
    }

    private static String getBits(byte b) {
        String result = "";
        for(int i = 0; i < 8; i++)
            result += (b & (1 << i)) == 0 ? "0" : "1";
        return result;
    }

    public static String convertFileToBinaryString(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileData = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(fileData);
        fis.close();
        StringBuilder content = new StringBuilder();
        for (byte b : fileData) {
            content.append(getBits(b));
        }
        return content.toString();
    }
    public static String getStringFromFile (String filePath) throws IOException {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        byte[] bytes = IOUtils.toByteArray(fin);
        fin.close();
        return new String(bytes);
    }
}
