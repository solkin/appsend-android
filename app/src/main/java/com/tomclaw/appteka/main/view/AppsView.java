package com.tomclaw.appteka.main.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ViewFlipper;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.flurry.android.FlurryAgent;
import com.greysonparrelli.permiso.Permiso;
import com.tomclaw.appteka.DonateActivity;
import com.tomclaw.appteka.PermissionsActivity;
import com.tomclaw.appteka.R;
import com.tomclaw.appteka.core.TaskExecutor;
import com.tomclaw.appteka.main.adapter.BaseItemAdapter;
import com.tomclaw.appteka.main.adapter.FilterableItemAdapter;
import com.tomclaw.appteka.main.controller.AppsController;
import com.tomclaw.appteka.main.item.AppItem;
import com.tomclaw.appteka.main.item.BaseItem;
import com.tomclaw.appteka.main.task.ExportApkTask;
import com.tomclaw.appteka.util.ColorHelper;
import com.tomclaw.appteka.util.EdgeChanger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mrapp.android.bottomsheet.BottomSheet;

import static com.tomclaw.appteka.util.IntentHelper.openGooglePlay;

/**
 * Created by ivsolkin on 08.01.17.
 */
public class AppsView extends MainView implements BillingProcessor.IBillingHandler, AppsController.AppsCallback {

    private ViewFlipper viewFlipper;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private FilterableItemAdapter adapter;
    private BillingProcessor bp;

    public AppsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        String licenseKey = context.getString(R.string.license_key);
        bp = new BillingProcessor(context, licenseKey, this);

        viewFlipper = findViewById(R.id.apps_view_switcher);

        findViewById(R.id.button_retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView = findViewById(R.id.apps_list_view);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        recyclerView.setItemAnimator(itemAnimator);
        final int toolbarColor = ColorHelper.getAttributedColor(context, R.attr.toolbar_background);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                EdgeChanger.setEdgeGlowColor(recyclerView, toolbarColor);
            }
        });

        BaseItemAdapter.BaseItemClickListener listener = new BaseItemAdapter.BaseItemClickListener() {
            @Override
            public void onItemClicked(final BaseItem item) {
                boolean donateItem = item.getType() == BaseItem.DONATE_ITEM;
                if (donateItem) {
                    FlurryAgent.logEvent("Chocolate clicked");
                    showDonateDialog();
                } else {
                    final AppItem info = (AppItem) item;
                    checkPermissionsForExtract(info);
                }
            }

            @Override
            public void onActionClicked(BaseItem item, String action) {
            }
        };

        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        adapter = new FilterableItemAdapter(context);
        adapter.setListener(listener);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.apps_view;
    }

    @Override
    public void activate() {
        if (!AppsController.getInstance().isStarted()) {
            refresh();
        }
    }

    @Override
    public void start() {
        AppsController.getInstance().onAttach(this);
    }

    @Override
    public void stop() {
        AppsController.getInstance().onDetach(this);
    }

    @Override
    public void destroy() {
        if (bp != null) {
            bp.release();
        }
    }

    @Override
    public void refresh() {
        AppsController.getInstance().reload(getContext());
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public void filter(String query) {
        adapter.getFilter().filter(query);
    }

    private void showDonateDialog() {
        startActivity(new Intent(getContext(), DonateActivity.class));
    }

    private void checkPermissionsForExtract(final AppItem appItem) {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    // Permission granted!
                    showActionDialog(appItem);
                } else {
                    // Permission denied.
                    Snackbar.make(recyclerView, R.string.permission_denied_message, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                String title = getResources().getString(R.string.app_name);
                String message = getResources().getString(R.string.write_permission_extract);
                Permiso.getInstance().showRationaleInDialog(title, message, null, callback);
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void showActionDialog(final AppItem appItem) {
        BottomSheet.Builder builder = new BottomSheet.Builder(getContext());
        builder.addItem(0, R.string.run_app, R.drawable.run);
        builder.addItem(1, R.string.find_on_gp, R.drawable.google_play);
        builder.addItem(2, R.string.share_apk, R.drawable.share);
        builder.addItem(3, R.string.extract_apk, R.drawable.floppy);
        builder.addItem(4, R.string.required_permissions, R.drawable.lock_open);
        builder.addItem(5, R.string.app_details, R.drawable.settings_box);
        builder.addItem(6, R.string.remove, R.drawable.delete);
        BottomSheet bottomSheet = builder.create();
        bottomSheet.show();
        bottomSheet.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppsView.this.onItemClick(appItem, id);
            }
        });
        bottomSheet.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });
    }

    private void onItemClick(AppItem appItem, long id) {
        switch ((int) id) {
            case 0: {
                FlurryAgent.logEvent("App menu: run");
                PackageManager packageManager = getContext().getPackageManager();
                Intent launchIntent = packageManager.getLaunchIntentForPackage(appItem.getPackageName());
                if (launchIntent == null) {
                    Snackbar.make(recyclerView, R.string.non_launchable_package, Snackbar.LENGTH_LONG).show();
                } else {
                    startActivity(launchIntent);
                }
                break;
            }
            case 1: {
                FlurryAgent.logEvent("App menu: Google Play");
                String packageName = appItem.getPackageName();
                openGooglePlay(getContext(), packageName);
                break;
            }
            case 2: {
                FlurryAgent.logEvent("App menu: share");
                TaskExecutor.getInstance().execute(new ExportApkTask(getContext(), appItem, ExportApkTask.ACTION_SHARE));
                break;
            }
            case 3: {
                FlurryAgent.logEvent("App menu: extract");
                TaskExecutor.getInstance().execute(new ExportApkTask(getContext(), appItem, ExportApkTask.ACTION_EXTRACT));
                break;
            }
            case 4: {
                FlurryAgent.logEvent("App menu: permissions");
                try {
                    PackageInfo packageInfo = appItem.getPackageInfo();
                    List<String> permissions = Arrays.asList(packageInfo.requestedPermissions);
                    Intent intent = new Intent(getContext(), PermissionsActivity.class)
                            .putStringArrayListExtra(PermissionsActivity.EXTRA_PERMISSIONS,
                                    new ArrayList<>(permissions));
                    startActivity(intent);
                } catch (Throwable ex) {
                    Snackbar.make(recyclerView, R.string.unable_to_get_permissions, Snackbar.LENGTH_LONG).show();
                }
                break;
            }
            case 5: {
                FlurryAgent.logEvent("App menu: details");
                setRefreshOnResume();
                final Intent intent = new Intent()
                        .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .addCategory(Intent.CATEGORY_DEFAULT)
                        .setData(Uri.parse("package:" + appItem.getPackageName()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
            case 6: {
                FlurryAgent.logEvent("App menu: remove");
                setRefreshOnResume();
                Uri packageUri = Uri.parse("package:" + appItem.getPackageName());
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
                startActivity(uninstallIntent);
                break;
            }
        }
    }

    private void setAppInfoList(List<BaseItem> appItemList) {
        if (bp.loadOwnedPurchasesFromGoogle() &&
                bp.isPurchased(getContext().getString(R.string.chocolate_id))) {
            for (BaseItem item : appItemList) {
                boolean donateItem = (item.getType() == BaseItem.DONATE_ITEM);
                if (donateItem) {
                    appItemList.remove(item);
                    break;
                }
            }
        }
        adapter.setItemsList(appItemList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
    }

    @Override
    public void onPurchaseHistoryRestored() {
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
    }

    @Override
    public void onBillingInitialized() {
    }

    @Override
    public void onProgress() {
        if (!swipeRefresh.isRefreshing()) {
            viewFlipper.setDisplayedChild(0);
        }
    }

    @Override
    public void onLoaded(List<BaseItem> list) {
        setAppInfoList(list);
        viewFlipper.setDisplayedChild(1);
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onError() {
        viewFlipper.setDisplayedChild(2);
        swipeRefresh.setRefreshing(false);
    }
}
