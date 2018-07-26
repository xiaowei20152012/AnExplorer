package com.fast.explorer.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import java.io.Closeable;

import com.fast.explorer.libcore.io.IoUtils;
import com.fast.explorer.misc.ContentProviderClientCompat;

import static com.fast.explorer.BaseActivity.State.MODE_UNKNOWN;
import static com.fast.explorer.BaseActivity.State.SORT_ORDER_UNKNOWN;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;
    public Exception exception;

    public int mode = MODE_UNKNOWN;
    public int sortOrder = SORT_ORDER_UNKNOWN;

    @Override
    public void close() {
        IoUtils.closeQuietly(cursor);
        ContentProviderClientCompat.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}