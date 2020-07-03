package com.aefyr.sai.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.zip.CRC32;

public class IOUtils {
    private static final String TAG = "IOUtils";

    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }

    public static void copyFile(File original, File destination) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(original); FileOutputStream outputStream = new FileOutputStream(destination)) {
            copyStream(inputStream, outputStream);
        }
    }

    public static void copyFileFromAssets(Context context, String assetFileName, File destination) throws IOException {
        try (InputStream inputStream = context.getAssets().open(assetFileName); FileOutputStream outputStream = new FileOutputStream(destination)) {
            copyStream(inputStream, outputStream);
        }
    }

    public static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files)
                    deleteRecursively(child);
            }
        }
        f.delete();
    }

    public static long calculateFileCrc32(File file) throws IOException {
        return calculateCrc32(new FileInputStream(file));
    }

    public static long calculateBytesCrc32(byte[] bytes) throws IOException {
        return calculateCrc32(new ByteArrayInputStream(bytes));
    }

    public static long calculateCrc32(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            CRC32 crc32 = new CRC32();
            byte[] buffer = new byte[1024 * 1024];
            int read;

            while ((read = in.read(buffer)) > 0)
                crc32.update(buffer, 0, read);

            return crc32.getValue();
        }
    }

    public static Thread writeStreamToStringBuilder(StringBuilder builder, InputStream inputStream) {
        Thread t = new Thread(() -> {
            try {
                char[] buf = new char[1024];
                int len;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while ((len = reader.read(buf)) > 0)
                    builder.append(buf, 0, len);

                reader.close();
            } catch (Exception e) {
                Log.wtf(TAG, e);
            }
        });
        t.start();
        return t;
    }

    /**
     * Read contents of input stream to a byte array and close it
     *
     * @param inputStream
     * @return contents of input stream
     * @throws IOException
     */
    public static byte[] readStream(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            return readStreamNoClose(in);
        }
    }

    public static String readStream(InputStream inputStream, Charset charset) throws IOException {
        return new String(readStream(inputStream), charset);
    }

    /**
     * Read contents of input stream to a byte array, but don't close the stream
     *
     * @param inputStream
     * @return contents of input stream
     * @throws IOException
     */
    public static byte[] readStreamNoClose(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copyStream(inputStream, buffer);
        return buffer.toByteArray();
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (Exception e) {
            Log.w(TAG, String.format("Unable to close %s", closeable.getClass().getCanonicalName()), e);
        }
    }

    /**
     * Hashes stream content using passed {@link MessageDigest}, closes the stream and returns digest bytes
     *
     * @param inputStream
     * @param messageDigest
     * @return
     * @throws IOException
     */
    public static byte[] hashStream(InputStream inputStream, MessageDigest messageDigest) throws IOException {
        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);) {
            byte[] buffer = new byte[1024 * 64];
            int read;
            while ((read = digestInputStream.read(buffer)) > 0) {
                //Do nothing
            }

            return messageDigest.digest();
        }
    }

    public static byte[] hashString(String s, MessageDigest messageDigest) throws IOException {
        return hashStream(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), messageDigest);
    }

    public static byte[] readFile(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return readStream(in);
        }
    }


}
