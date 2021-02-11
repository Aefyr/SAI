package com.aefyr.sai.utils.saf;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.List;

public class SafUtils {

    private static final String PATH_TREE = "tree";

    public static String getRootForPath(Uri docUri) {
        String path = DocumentsContract.getTreeDocumentId(docUri);

        int indexOfLastColon = path.lastIndexOf(':');
        if (indexOfLastColon == -1)
            throw new IllegalArgumentException("Given uri does not contain a colon: " + docUri);

        return path.substring(0, indexOfLastColon);
    }

    public static String getPathWithoutRoot(Uri docUri) {
        String path = DocumentsContract.getTreeDocumentId(docUri);

        int indexOfLastColon = path.lastIndexOf(':');
        if (indexOfLastColon == -1)
            throw new IllegalArgumentException("Given uri does not contain a colon: " + docUri);

        return path.substring(indexOfLastColon + 1);
    }

    public static Uri buildChildDocumentUri(Uri directoryUri, String childDisplayName) {
        if (!isTreeUri(directoryUri))
            throw new IllegalArgumentException("directoryUri must be a tree uri");

        String rootPath = getRootForPath(directoryUri);
        String directoryPath = getPathWithoutRoot(directoryUri);

        String childPath = rootPath + ":" + directoryPath + "/" + FileUtils.buildValidFatFilename(childDisplayName);

        return DocumentsContract.buildDocumentUriUsingTree(directoryUri, childPath);
    }

    /**
     * Test if the given URI represents a {@link DocumentsContract.Document} tree.
     **/
    public static boolean isTreeUri(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        return (paths.size() >= 2 && PATH_TREE.equals(paths.get(0)));
    }

    @Nullable
    public static DocumentFile docFileFromSingleUriOrFileUri(Context context, Uri contentUri) {
        if (ContentResolver.SCHEME_FILE.equals(contentUri.getScheme())) {
            String path = contentUri.getPath();
            if (path == null)
                return null;

            File file = new File(path);
            if (file.isDirectory())
                return null;

            return DocumentFile.fromFile(file);
        } else {
            return DocumentFile.fromSingleUri(context, contentUri);
        }
    }

    @Nullable
    public static DocumentFile docFileFromTreeUriOrFileUri(Context context, Uri contentUri) {
        if (ContentResolver.SCHEME_FILE.equals(contentUri.getScheme())) {
            String path = contentUri.getPath();
            if (path == null)
                return null;

            File file = new File(path);
            if (!file.isDirectory())
                return null;

            return DocumentFile.fromFile(file);
        } else {
            return DocumentFile.fromTreeUri(context, contentUri);
        }
    }

    @Nullable
    public static String getFileNameFromContentUri(Context context, Uri contentUri) {
        DocumentFile documentFile = docFileFromSingleUriOrFileUri(context, contentUri);

        if (documentFile == null)
            return null;

        return documentFile.getName();
    }

    /**
     * @param context
     * @param contentUri
     * @return file length or 0 if it's unknown
     */
    @Nullable
    public static long getFileLengthFromContentUri(Context context, Uri contentUri) {
        DocumentFile documentFile = docFileFromSingleUriOrFileUri(context, contentUri);

        if (documentFile == null)
            return 0;

        return documentFile.length();
    }

    public static File parcelFdToFile(ParcelFileDescriptor fd) {
        return new File("/proc/self/fd/" + fd.getFd());
    }

}
