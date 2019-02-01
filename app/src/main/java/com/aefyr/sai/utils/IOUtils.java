package com.aefyr.sai.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }

    public static void copyFile(File original, File destination) throws IOException {
        FileInputStream inputStream = new FileInputStream(original);
        FileOutputStream outputStream = new FileOutputStream(destination);

        copyStream(inputStream, outputStream);

        inputStream.close();
        outputStream.close();
    }

    public static void copyFileFromAssets(Context context, String assetFileName, File destination) throws IOException {
        InputStream inputStream = context.getAssets().open(assetFileName);
        FileOutputStream outputStream = new FileOutputStream(destination);

        copyStream(inputStream, outputStream);

        inputStream.close();
        outputStream.close();
    }

}
