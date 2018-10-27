package com.tomclaw.appteka;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.greysonparrelli.permiso.PermisoActivity;
import com.kobakei.ratethisapp.RateThisApp;
import com.tomclaw.appteka.main.view.AppsView;
import com.tomclaw.appteka.main.view.MainView;
import com.tomclaw.appteka.util.PreferenceHelper;
import com.tomclaw.appteka.util.ThemeHelper;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.metrics.MetricsManager;

import androidx.annotation.MenuRes;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends PermisoActivity implements MainView.ActivityCallback {

    private static final int REQUEST_UPDATE_SETTINGS = 6;
    private static final String REFRESH_ON_RESUME = "refresh_on_resume";

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
        getSupportActionBar().setIcon(R.drawable.ic_logo_ab);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

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
        onCloseListener = new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if (appsView.isFilterable()) {
                    appsView.filter("");
                }
                return false;
            }
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

        checkForCrashes();
        MetricsManager.register(getApplication());

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
    protected void onSaveInstanceState(Bundle outState) {
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

    private void checkForCrashes() {
        CrashManager.register(this);
    }

    @Override
    public void setRefreshOnResume() {
        isRefreshOnResume = true;
    }

}
