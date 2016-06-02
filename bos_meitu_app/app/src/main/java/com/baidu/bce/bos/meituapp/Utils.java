package com.baidu.bce.bos.meituapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gavin on 5/15/16.
 */
public class Utils {
    /**
     * Try to return the absolute file path from the given Uri
     * not reliable, android version dependent
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getFileFullPathFromUri(final Context context, final Uri uri) {
        if (null == uri) return null;
        String scheme = uri.getScheme();
        String path = null;
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        path = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        } else {
            path = uri.getPath();
        }
        return path;
    }

    /**
     * use double memory of the stream
     * max length is Iteger.MAX_VALUE
     *
     * @param input
     * @return
     * @throws IOException
     */
    public static byte[] readAllFromStream(InputStream input) throws IOException {
        List<byte[]> result = new ArrayList<byte[]>();
        int bufferSize = 5 * 1024 * 1024;
        long length = 0;
        byte[] ret = null;
        for (;;) {
            byte[] buffer = new byte[bufferSize];
            result.add(buffer);
            int off = 0;
            while (off < bufferSize) {
                int count;
                try {
                    count = input.read(buffer, off, bufferSize - off);
                } catch (IOException e) {
                    throw e;
                }
                if (count < 0) {
                    ret = new byte[(int)length];
                    int index = 0;
                    for (int i = 0; i < length; ++i) {
                        index = i / bufferSize;
                        ret[i] = result.get(index)[i % bufferSize];
                        if (index >= 1) {
                            result.set(index - 1, null);
                        }
                    }
                    result.clear();
                    return ret;
                }
                length += count;
                // cannot larger than Integer.MAX_VALUE
                if (length > Integer.MAX_VALUE) {
                    throw new IndexOutOfBoundsException();
                }
                off += count;
            }
        }
    }
}
