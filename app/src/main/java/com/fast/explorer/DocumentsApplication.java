/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fast.explorer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatDelegate;
import android.text.format.DateUtils;

import com.fast.explorer.misc.AnalyticsManager;
import com.fast.explorer.misc.ContentProviderClientCompat;
import com.fast.explorer.misc.CrashReportingManager;
import com.fast.explorer.misc.RootsCache;
import com.fast.explorer.misc.SAFManager;
import com.fast.explorer.misc.ThumbnailCache;
import com.fast.explorer.misc.Utils;
import com.fast.explorer.setting.SettingsActivity;

public class DocumentsApplication extends AppFlavour {
	private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
    private static DocumentsApplication sInstance;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private RootsCache mRoots;
    private ArrayMap<Integer, Long> mSizes = new ArrayMap<Integer, Long>();
    private SAFManager mSAFManager;
    private Point mThumbnailsSize;
    private ThumbnailCache mThumbnailCache;
    private static boolean isTelevision;

    public static RootsCache getRootsCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mRoots;
    }

    public static ArrayMap<Integer, Long> getFolderSizes() {
        return getInstance().mSizes;
    }

    public static SAFManager getSAFManager(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mSAFManager;
    }

    public static ThumbnailCache getThumbnailCache(Context context) {
        final DocumentsApplication app = (DocumentsApplication) context.getApplicationContext();
        return app.mThumbnailCache;
    }

    public static ThumbnailCache getThumbnailsCache(Context context, Point size) {
        return getThumbnailCache(context);
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
    	final ContentProviderClient client = ContentProviderClientCompat.acquireUnstableContentProviderClient(resolver, authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        ContentProviderClientCompat.setDetectNotResponding(client, PROVIDER_ANR_TIMEOUT);
        return client;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(!BuildConfig.DEBUG) {
            AnalyticsManager.intialize(getApplicationContext());
        }
        sInstance = this;
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
//        CloudRail.setAppKey(BuildConfig.LICENSE_KEY);
        CrashReportingManager.enable(getApplicationContext(), true);

        mRoots = new RootsCache(this);
        mRoots.updateAsync();

        mSAFManager = new SAFManager(this);

        mThumbnailCache = new ThumbnailCache(memoryClassBytes / 4);

        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        packageFilter.addDataScheme("package");
        registerReceiver(mCacheReceiver, packageFilter);

        final IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
        registerReceiver(mCacheReceiver, localeFilter);

        isTelevision = Utils.isTelevision(this);
        if(isTelevision && Integer.valueOf(SettingsActivity.getThemeStyle()) != AppCompatDelegate.MODE_NIGHT_YES){
            SettingsActivity.setThemeStyle(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static synchronized DocumentsApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mThumbnailCache.onTrimMemory(level);
    }

    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Uri data = intent.getData();
            if (data != null) {
                final String authority = data.getAuthority();
                mRoots.updateAuthorityAsync(authority);
            } else {
                mRoots.updateAsync();
            }
        }
    };

    public static boolean isTelevision() {
        return isTelevision;
    }
}
