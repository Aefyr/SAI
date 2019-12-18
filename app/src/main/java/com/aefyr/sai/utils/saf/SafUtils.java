package com.aefyr.sai.utils.saf;

import android.net.Uri;
import android.provider.DocumentsContract;

import java.util.List;

public class SafUtils {

    private static final String PATH_TREE = "tree";

    public static String getRootForPath(Uri docUri) {
        String path = DocumentsContract.getTreeDocumentId(docUri);

        int indexOfLastColon = path.lastIndexOf(':');
        if (indexOfLastColon == -1)
            throw new IllegalArgumentException("Given uri does not contain a colon");

        return path.substring(0, indexOfLastColon);
    }

    public static String getPathWithoutRoot(Uri docUri) {
        String path = DocumentsContract.getTreeDocumentId(docUri);

        int indexOfLastColon = path.lastIndexOf(':');
        if (indexOfLastColon == -1)
            throw new IllegalArgumentException("Given uri does not contain a colon");

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

}
