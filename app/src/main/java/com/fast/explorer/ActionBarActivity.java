package com.fast.explorer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.fast.explorer.misc.AnalyticsManager;
import com.fast.explorer.misc.Utils;

/**
 * Created by HaKr on 18-Oct-14.
 */
public abstract class ActionBarActivity extends AppCompatActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Utils.changeThemeStyle(getDelegate());
        super.onCreate(savedInstanceState);
    }

    @Override
    public ActionBar getSupportActionBar() {
        return super.getSupportActionBar();
    }

    @Override
    public void recreate() {
        Utils.changeThemeStyle(getDelegate());
        super.recreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsManager.setCurrentScreen(this, getTag());
    }

    public abstract String getTag();
}
