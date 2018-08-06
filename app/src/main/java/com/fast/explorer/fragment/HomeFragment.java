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

package com.fast.explorer.fragment;

import android.app.ActivityManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fast.explorer.BaseActivity;
import com.fast.explorer.DocumentsActivity;
import com.fast.explorer.DocumentsApplication;
import com.fast.explorer.R;
import com.fast.explorer.adapter.RecentsAdapter;
import com.fast.explorer.adapter.ShortcutsAdapter;
import com.fast.explorer.cursor.LimitCursorWrapper;
import com.fast.explorer.loader.RecentLoader;
import com.fast.explorer.misc.AnalyticsManager;
import com.fast.explorer.misc.AsyncTask;
import com.fast.explorer.misc.CrashReportingManager;
import com.fast.explorer.misc.IconHelper;
import com.fast.explorer.misc.IconUtils;
import com.fast.explorer.misc.RootsCache;
import com.fast.explorer.misc.Utils;
import com.fast.explorer.model.DirectoryResult;
import com.fast.explorer.model.DocumentInfo;
import com.fast.explorer.model.RootInfo;
import com.fast.explorer.provider.AppsProvider;
import com.fast.explorer.setting.SettingsActivity;
import com.fast.explorer.ui.HomeItem;
import com.fast.explorer.ui.MaterialProgressDialog;

import java.util.ArrayList;
import java.util.List;

import static com.fast.explorer.BaseActivity.State.MODE_GRID;
import static com.fast.explorer.DocumentsApplication.isTelevision;
import static com.fast.explorer.misc.AnalyticsManager.FILE_TYPE;
import static com.fast.explorer.provider.AppsProvider.getRunningAppProcessInfo;

/**
 * Display home.
 */
public class HomeFragment extends Fragment {
    public static final String TAG = "HomeFragment";
    private static final int MAX_RECENT_COUNT = isTelevision() ? 20 : 10;

    private final int mLoaderId = 42;
    private HomeItem storageStats;
    private HomeItem memoryStats;
    private RootsCache roots;
    private RecyclerView mRecentsRecycler;
    private RecyclerView mShortcutsRecycler;
    private RecentsAdapter mRecentsAdapter;
    private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;
    private View recents_container;
    private TextView recents;
    private ShortcutsAdapter mShortcutsAdapter;
    private RootInfo mHomeRoot;
    private HomeItem secondayStorageStats;
    private HomeItem usbStorageStats;
    private BaseActivity mActivity;
    private IconHelper mIconHelper;

    public static void show(FragmentManager fm) {
        final HomeFragment fragment = new HomeFragment();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment, TAG);
        ft.commitAllowingStateLoss();
    }

    public static HomeFragment get(FragmentManager fm) {
        return (HomeFragment) fm.findFragmentByTag(TAG);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storageStats = (HomeItem) view.findViewById(R.id.storage_stats);
        secondayStorageStats = (HomeItem) view.findViewById(R.id.seconday_storage_stats);
        usbStorageStats = (HomeItem) view.findViewById(R.id.usb_storage_stats);
        memoryStats = (HomeItem) view.findViewById(R.id.memory_stats);
        recents = (TextView) view.findViewById(R.id.recents);
        recents_container = view.findViewById(R.id.recents_container);

        mShortcutsRecycler = (RecyclerView) view.findViewById(R.id.shortcuts_recycler);
        mRecentsRecycler = (RecyclerView) view.findViewById(R.id.recents_recycler);

        mActivity = ((BaseActivity) getActivity());
        mIconHelper = new IconHelper(mActivity, MODE_GRID);

        roots = DocumentsApplication.getRootsCache(getActivity());
        mHomeRoot = roots.getHomeRoot();
        showRecents();
        showData();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    public void showData() {
        updateUI();
        showStorage();
        showOtherStorage();
        showMemory(0);
        showShortcuts();
        getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
    }

    private void updateUI() {
        mIconHelper.setThumbnailsEnabled(mActivity.getDisplayState().showThumbnail);
        recents_container.setVisibility(SettingsActivity.getDisplayRecentMedia() ? View.VISIBLE : View.GONE);
        roots = DocumentsApplication.getRootsCache(getActivity());
        int accentColor = SettingsActivity.getAccentColor();
        recents.setTextColor(accentColor);
        storageStats.updateColor();
        memoryStats.updateColor();
        secondayStorageStats.updateColor();
        usbStorageStats.updateColor();
    }

    public void reloadData() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showData();
            }
        }, 500);
    }

    private void showStorage() {
        final RootInfo primaryRoot = roots.getPrimaryRoot();
        if (null != primaryRoot) {
            storageStats.setVisibility(View.VISIBLE);
            storageStats.setInfo(primaryRoot);
            storageStats.setAction(R.drawable.ic_analyze, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
                    if (Utils.isIntentAvailable(getActivity(), intent)) {
                        getActivity().startActivity(intent);
                    } else {
                        ((DocumentsActivity) getActivity()).showInfo("Coming Soon!");
                    }
                    Bundle params = new Bundle();
                    AnalyticsManager.logEvent("storage_analyze", params);
                }
            });
            storageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(primaryRoot);
                }
            });
            animateProgress(storageStats, primaryRoot);
        } else {
            storageStats.setVisibility(View.GONE);
        }
    }


    private void showOtherStorage() {
        final RootInfo secondaryRoot = roots.getSecondaryRoot();
        if (null != secondaryRoot) {
            secondayStorageStats.setVisibility(View.VISIBLE);
            secondayStorageStats.setInfo(secondaryRoot);
            secondayStorageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(secondaryRoot);
                }
            });
            animateProgress(secondayStorageStats, secondaryRoot);
        } else {
            secondayStorageStats.setVisibility(View.GONE);
        }

        final RootInfo usbRoot = roots.getUSBRoot();
        if (null != usbRoot) {
            usbStorageStats.setVisibility(View.VISIBLE);
            usbStorageStats.setInfo(usbRoot);
            usbStorageStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(usbRoot);
                }
            });
            animateProgress(usbStorageStats, usbRoot);
        } else {
            usbStorageStats.setVisibility(View.GONE);
        }
    }

    private void showMemory(long currentAvailableBytes) {

        final RootInfo processRoot = roots.getProcessRoot();
        if (null != processRoot) {
            memoryStats.setVisibility(View.VISIBLE);
            memoryStats.setInfo(processRoot);
            memoryStats.setAction(R.drawable.ic_clean, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new OperationTask(processRoot).execute();
                    Bundle params = new Bundle();
                    AnalyticsManager.logEvent("process_clean", params);
                }
            });
            memoryStats.setCardListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openRoot(processRoot);
                }
            });
            if (currentAvailableBytes != 0) {
                long availableBytes = processRoot.availableBytes - currentAvailableBytes;
                String summaryText = availableBytes <= 0 ? "Already cleaned up!" :
                        getActivity().getString(R.string.root_available_bytes,
                                Formatter.formatFileSize(getActivity(), availableBytes));
                ((DocumentsActivity) getActivity()).showInfo(summaryText);
            }

            animateProgress(memoryStats, processRoot);
        }
    }

    private void showShortcuts() {
        ArrayList<RootInfo> data = roots.getShortcutsInfo();
        mShortcutsAdapter = new ShortcutsAdapter(getActivity(), data);
        mShortcutsAdapter.setOnItemClickListener(new ShortcutsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ShortcutsAdapter.ViewHolder item, int position) {
                openRoot(mShortcutsAdapter.getItem(position));
            }
        });
        mShortcutsRecycler.setAdapter(mShortcutsAdapter);
    }

    private void showRecents() {
        final RootInfo root = roots.getRecentsRoot();
        recents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRoot(root);
            }
        });

        mRecentsAdapter = new RecentsAdapter(getActivity(), null, mIconHelper);
        mRecentsAdapter.setOnItemClickListener(new RecentsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecentsAdapter.ViewHolder item, int position) {
                openDocument(item.mDocumentInfo);
            }
        });
        mRecentsRecycler.setAdapter(mRecentsAdapter);
        LinearSnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(mRecentsRecycler);

        final BaseActivity.State state = getDisplayState(this);
        mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {

//            @Override
//            public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
//                final RootsCache roots = DocumentsApplication.getRootsCache(getActivity());
//                return new RecentLoader(getActivity(), roots, state);
//            }

            @NonNull
            @Override
            public android.support.v4.content.Loader<DirectoryResult> onCreateLoader(int id, @Nullable Bundle args) {
                final RootsCache roots = DocumentsApplication.getRootsCache(getActivity());
                return new RecentLoader(getActivity(), roots, state);
            }

            @Override
            public void onLoadFinished(@NonNull android.support.v4.content.Loader<DirectoryResult> loader, DirectoryResult result) {
                if (!isAdded()) {
                    return;
                }
                if (null == result.cursor || (null != result.cursor && result.cursor.getCount() == 0)) {
                    recents_container.setVisibility(View.GONE);
                } else {
                    //recents_container.setVisibility(View.VISIBLE);
                    mRecentsAdapter.swapCursor(new LimitCursorWrapper(result.cursor, MAX_RECENT_COUNT));
                }
            }

            @Override
            public void onLoaderReset(@NonNull android.support.v4.content.Loader<DirectoryResult> loader) {
                mRecentsAdapter.swapCursor(null);

            }

//            @Override
//            public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
//                if (!isAdded()) {
//                    return;
//                }
//                if (null == result.cursor || (null != result.cursor && result.cursor.getCount() == 0)) {
//                    recents_container.setVisibility(View.GONE);
//                } else {
//                    //recents_container.setVisibility(View.VISIBLE);
//                    mRecentsAdapter.swapCursor(new LimitCursorWrapper(result.cursor, MAX_RECENT_COUNT));
//                }
//            }
//
//            @Override
//            public void onLoaderReset(Loader<DirectoryResult> loader) {
//                mRecentsAdapter.swapCursor(null);
//            }
        };
        getLoaderManager().restartLoader(mLoaderId, null, mCallbacks);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class OperationTask extends AsyncTask<Void, Void, Boolean> {

        private MaterialProgressDialog progressDialog;
        private RootInfo root;
        private long currentAvailableBytes;

        public OperationTask(RootInfo root) {
            progressDialog = new MaterialProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setColor(SettingsActivity.getAccentColor());
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Cleaning up RAM...");
            this.root = root;
            currentAvailableBytes = root.availableBytes;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            cleanupMemory(getActivity());
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Utils.isActivityAlive(getActivity())) {
                return;
            }
            AppsProvider.notifyDocumentsChanged(getActivity(), root.rootId);
            AppsProvider.notifyRootsChanged(getActivity());
            RootsCache.updateRoots(getActivity(), AppsProvider.AUTHORITY);
            roots = DocumentsApplication.getRootsCache(getActivity());
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showMemory(currentAvailableBytes);
                    progressDialog.dismiss();
                }
            }, 500);
        }
    }

    private void animateProgress(final HomeItem item, RootInfo root) {
        try {
            final double percent = (((root.totalBytes - root.availableBytes) / (double) root.totalBytes) * 100);
            item.setProgress(0);
            if (Utils.isActivityAlive(getActivity())) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (item.getProgress() >= (int) percent) {
                            item.setProgress((int) percent);
                        } else {
                            item.setProgress((int) percent);
                        }
                    }
                });
            }
        } catch (Exception e) {
            item.setVisibility(View.GONE);
            CrashReportingManager.logException(e);
        }
    }

    private static BaseActivity.State getDisplayState(Fragment fragment) {
        return ((BaseActivity) fragment.getActivity()).getDisplayState();
    }

    private void openRoot(RootInfo rootInfo) {
        DocumentsActivity activity = ((DocumentsActivity) getActivity());
        activity.onRootPicked(rootInfo, mHomeRoot);
        AnalyticsManager.logEvent("open_shortcuts", rootInfo, new Bundle());
    }

    public void cleanupMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcessesList = getRunningAppProcessInfo(context);
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcessesList) {
            activityManager.killBackgroundProcesses(processInfo.processName);
        }
    }

    private void openDocument(DocumentInfo doc) {
        ((BaseActivity) getActivity()).onDocumentPicked(doc);
        Bundle params = new Bundle();
        String type = IconUtils.getTypeNameFromMimeType(doc.mimeType);
        params.putString(FILE_TYPE, type);
        AnalyticsManager.logEvent("open_image_recent", params);
    }
}