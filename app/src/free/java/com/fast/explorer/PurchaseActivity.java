package com.fast.explorer;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


import com.fast.explorer.misc.SystemBarTintManager;
import com.fast.explorer.misc.Utils;
import com.fast.explorer.setting.SettingsActivity;
import needle.Needle;
import needle.UiRelatedTask;

import static com.fast.explorer.DocumentsActivity.getStatusBarHeight;

public class PurchaseActivity extends ActionBarActivity {

    public static final String TAG = PurchaseActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mToolbar.setTitleTextAppearance(this, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
        if(Utils.hasKitKat() && !Utils.hasLollipop()) {
            mToolbar.setPadding(0, getStatusBarHeight(this), 0, 0);
        }
        int color = SettingsActivity.getPrimaryColor();
        mToolbar.setBackgroundColor(color);
        appBarLayout.setBackgroundColor(color);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.support_app));
        setUpDefaultStatusBar();

        initControls();
        DocumentsApplication.getInstance().initializeBilling();
    }

    private void initControls() {

        Button restoreButton = (Button) findViewById(R.id.restore_button);
        Button purchaseButton = (Button) findViewById(R.id.purchase_button);
        restoreButton.setEnabled(true);
        purchaseButton.setEnabled(true);

        if(Utils.isTelevision(this)){
            restoreButton.setVisibility(View.GONE);
        }

        purchaseButton.setTextColor(SettingsActivity.getAccentColor());

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restorePurchase();
            }
        });

        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Utils.isTelevision(PurchaseActivity.this)){
                    Intent intentMarketAll = new Intent("android.intent.action.VIEW");
                    intentMarketAll.setData(Utils.getAppProStoreUri());
                    if(Utils.isIntentAvailable(PurchaseActivity.this, intentMarketAll)) {
                        startActivity(intentMarketAll);
                    }
                } else {
                    if(DocumentsApplication.isPurchased()){
                        Utils.showSnackBar(PurchaseActivity.this, R.string.thank_you);
                    } else {
                        DocumentsApplication.getInstance().purchase(PurchaseActivity.this, DocumentsApplication.getPurchaseId());
                    }
                }
            }
        });
    }

    @Override
    public String getTag() {
        return TAG;
    }

    private void restorePurchase() {
        Utils.showSnackBar(this, R.string.restoring_purchase);
        Needle.onBackgroundThread().execute(new UiRelatedTask<Boolean>(){
            @Override
            protected Boolean doWork() {
                DocumentsApplication.getInstance().loadOwnedPurchasesFromGoogle();
                DocumentsApplication.getInstance().onPurchaseHistoryRestored();
                return true;
            }

            @Override
            protected void thenDoUiRelatedWork(Boolean aBoolean) {
                onPurchaseRestored();
            }
        });
    }

    public void onPurchaseRestored(){
        if (DocumentsApplication.isPurchased()) {
            Utils.showSnackBar(this, R.string.restored_previous_purchase_please_restart);
        } else {
            Utils.showSnackBar(this, R.string.could_not_restore_purchase);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!DocumentsApplication.getInstance().handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        DocumentsApplication.getInstance().releaseBillingProcessor();
        super.onDestroy();
    }

    public void setUpDefaultStatusBar() {
        int color = Utils.getStatusBarColor(SettingsActivity.getPrimaryColor());
        if(Utils.hasLollipop()){
            getWindow().setStatusBarColor(color);
        }
        else if(Utils.hasKitKat()){
            SystemBarTintManager systemBarTintManager = new SystemBarTintManager(this);
            systemBarTintManager.setTintColor(Utils.getStatusBarColor(color));
            systemBarTintManager.setStatusBarTintEnabled(true);
        }
    }
}