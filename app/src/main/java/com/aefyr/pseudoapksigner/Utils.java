package com.aefyr.pseudoapksigner;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

public class Utils {

    public static byte[] getFileHash(File file, String hashingAlgorithm) throws Exception {
        return getFileHash(new FileInputStream(file), hashingAlgorithm);
    }

    static byte[] getFileHash(InputStream fileInputStream, String hashingAlgorithm) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(hashingAlgorithm);

        byte[] buffer = new byte[1024 * 1024];

        int read;
        while ((read = fileInputStream.read(buffer)) > 0)
            messageDigest.update(buffer, 0, read);

        fileInputStream.close();

        return messageDigest.digest();
    }

    static byte[] hash(byte[] bytes, String hashingAlgorithm) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(hashingAlgorithm);
        messageDigest.update(bytes);
        return messageDigest.digest();
    }

    static String base64Encode(byte[] bytes) {
        return Base64.encodeToString(bytes, 0);
    }

    static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }
}
