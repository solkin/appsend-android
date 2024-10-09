package com.tomclaw.appsend_rb;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.Constants;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.material.snackbar.Snackbar;
import com.jaeger.library.StatusBarUtil;
import com.tomclaw.appsend_rb.util.ThemeHelper;

/**
 * Created by ivsolkin on 06.09.16.
 */
public class DonateActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    private BillingProcessor bp;
    private View rootView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.updateTheme(this);
        super.onCreate(savedInstanceState);

        String licenseKey = getString(R.string.license_key);
        bp = new BillingProcessor(this, licenseKey, this);

        setContentView(R.layout.donate);
        ThemeHelper.updateStatusBar(this);

        rootView = findViewById(R.id.root_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.chocolate_color));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        findViewById(R.id.donate_button).setOnClickListener(view -> onChocolateClicked());

        int color = getResources().getColor(R.color.chocolate_color);
        StatusBarUtil.setColor(this, color);
    }

    private void onChocolateClicked() {
        String chocolateId = getString(R.string.chocolate_id);
        bp.purchase(this, chocolateId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!bp.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        if (bp != null) {
            bp.release();
        }
        super.onDestroy();
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        Toast.makeText(this, R.string.thank_you, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        finish();
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        if (errorCode != Constants.BILLING_RESPONSE_RESULT_USER_CANCELED) {
            Snackbar.make(rootView, R.string.purchase_error, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBillingInitialized() {
    }
}
