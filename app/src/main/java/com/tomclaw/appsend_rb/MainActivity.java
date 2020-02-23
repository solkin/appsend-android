package com.tomclaw.appsend_rb;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.greysonparrelli.permiso.PermisoActivity;
import com.kobakei.ratethisapp.RateThisApp;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.tomclaw.appsend_rb.main.view.AppsView;
import com.tomclaw.appsend_rb.main.view.MainView;
import com.tomclaw.appsend_rb.util.PreferenceHelper;
import com.tomclaw.appsend_rb.util.ThemeHelper;

public class MainActivity extends PermisoActivity implements MainView.ActivityCallback {

    private static final int REQUEST_UPDATE_SETTINGS = 6;
    private static final String REFRESH_ON_RESUME = "refresh_on_resume";
    private static final String APP_IDENTIFIER_KEY = "com.microsoft.appcenter.android.appIdentifier";

    private AppsView appsView;
    private SearchView.OnQueryTextListener onQueryTextListener;
    private SearchView.OnCloseListener onCloseListener;
    private boolean isRefreshOnResume = false;
    private boolean isDarkTheme;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        isDarkTheme = ThemeHelper.updateTheme(this);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isRefreshOnResume = savedInstanceState.getBoolean(REFRESH_ON_RESUME, false);
        }

        setContentView(R.layout.main);
        ThemeHelper.updateStatusBar(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.drawable.ic_logo_ab);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        appsView = findViewById(R.id.apps_view);

        onQueryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (appsView.isFilterable()) {
                    appsView.filter(newText);
                }
                return false;
            }
        };
        onCloseListener = () -> {
            if (appsView.isFilterable()) {
                appsView.filter("");
            }
            return false;
        };

        if (savedInstanceState == null) {
            // Custom criteria: 7 days and 10 launches
            RateThisApp.Config config = new RateThisApp.Config(7, 10);
            // Custom title ,message and buttons names
            config.setTitle(R.string.rate_title);
            config.setMessage(R.string.rate_message);
            config.setYesButtonText(R.string.yes_rate);
            config.setNoButtonText(R.string.no_thanks);
            config.setCancelButtonText(R.string.rate_cancel);
            RateThisApp.init(config);

            // Monitor launch times and interval from installation
            RateThisApp.onStart(this);
            // If the criteria is satisfied, "Rate this app" dialog will be shown
            RateThisApp.showRateDialogIfNeeded(this);
        }

        register(getApplication());

        appsView.activate();
    }

    private void updateList() {
        appsView.refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        appsView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        appsView.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        appsView.destroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(REFRESH_ON_RESUME, isRefreshOnResume);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appsView.activate(this);
        if (isRefreshOnResume) {
            updateList();
            isRefreshOnResume = false;
        }
        if (isDarkTheme != PreferenceHelper.isDarkTheme(this)) {
            Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        @MenuRes int menuRes;
        if (appsView == null || !appsView.isFilterable()) {
            menuRes = R.menu.main_no_search_menu;
        } else {
            menuRes = R.menu.main_menu;
        }
        getMenuInflater().inflate(menuRes, menu);
        MenuItem searchMenu = menu.findItem(R.id.menu_search);
        if (searchMenu != null) {
            SearchView searchView = (SearchView) searchMenu.getActionView();
            searchView.setQueryHint(menu.findItem(R.id.menu_search).getTitle());
            searchView.setOnQueryTextListener(onQueryTextListener);
            searchView.setOnCloseListener(onCloseListener);
            searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    onCloseListener.onClose();
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                break;
            }
            case R.id.refresh: {
                updateList();
                break;
            }
            case R.id.settings: {
                showSettings();
                break;
            }
            case R.id.info: {
                showInfo();
                break;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UPDATE_SETTINGS) {
            if (resultCode == SettingsActivity.RESULT_UPDATE) {
                updateList();
            }
        }
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_UPDATE_SETTINGS);
    }

    private void showInfo() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public void setRefreshOnResume() {
        isRefreshOnResume = true;
    }

    private static void register(Application application) {
        String appIdentifier = getAppIdentifier(application.getApplicationContext());
        if (appIdentifier == null || appIdentifier.length() == 0) {
            throw new IllegalArgumentException("AppCenter app identifier was not configured correctly in manifest or build configuration.");
        }
        register(application, appIdentifier);
    }

    private static void register(Application application, String appIdentifier) {
        AppCenter.start(application, appIdentifier, Analytics.class, Crashes.class);
    }

    public static String getAppIdentifier(Context context) {
        String appIdentifier = getManifestString(context, APP_IDENTIFIER_KEY);
        if (TextUtils.isEmpty(appIdentifier)) {
            throw new IllegalArgumentException("AppCenter app identifier was not configured correctly in manifest or build configuration.");
        }
        return appIdentifier;
    }

    public static String getManifestString(Context context, String key) {
        return getBundle(context).getString(key);
    }

    private static Bundle getBundle(Context context) {
        Bundle bundle;
        try {
            bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        return bundle;
    }

}
