package com.tomclaw.appteka.util;

import android.app.Activity;

import com.jaeger.library.StatusBarUtil;
import com.tomclaw.appteka.R;

/**
 * Created by ivsolkin on 21.09.16.
 */

public class ThemeHelper {

    public static boolean updateTheme(Activity activity) {
        boolean isDarkTheme = PreferenceHelper.isDarkTheme(activity);
        activity.setTheme(isDarkTheme ? R.style.AppThemeBlack : R.style.AppTheme);
        return isDarkTheme;
    }

    public static void updateStatusBar(Activity activity) {
        int color = ColorHelper.getAttributedColor(activity, R.attr.toolbar_background);
        StatusBarUtil.setColor(activity, color);
    }
}
