package com.tomclaw.appsend_rb;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.widget.Toast;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.tomclaw.appsend_rb.core.PleaseWaitTask;
import com.tomclaw.appsend_rb.core.TaskExecutor;
import com.tomclaw.appsend_rb.screen.apps.OutputWrapperImpl;

/**
 * Created by Solkin on 12.01.2015.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preference myPref = findPreference(getString(R.string.pref_clear_cache));
        myPref.setOnPreferenceClickListener(preference -> {
            Context context = getActivity();
            if (context == null) {
                return true;
            }
            OutputWrapperImpl outputWrapper = new OutputWrapperImpl(
                    context.getApplicationContext(),
                    context.getContentResolver()
            );
            TaskExecutor.getInstance().execute(new PleaseWaitTask(context) {
                @Override
                public void executeBackground() {
                    outputWrapper.clearExports();
                }

                @Override
                public void onSuccessMain() {
                    Context context = getWeakObject();
                    if (context != null) {
                        Toast.makeText(context, R.string.cache_cleared_successfully, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailMain(Throwable ex) {
                    Context context = getWeakObject();
                    if (context != null) {
                        Toast.makeText(context, R.string.cache_clearing_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return true;
        });
    }
}
