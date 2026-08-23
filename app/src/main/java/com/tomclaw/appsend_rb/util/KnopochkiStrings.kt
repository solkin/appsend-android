package com.tomclaw.appsend_rb.util

import android.content.res.Resources
import com.tomclaw.appsend_rb.R
import com.tomclaw.knopochki.core.Strings

/**
 * Localized replacements for the strings Knopochki speaks to accessibility
 * services. The toolkit ships English defaults and has no resources of its own,
 * so without this every locale would hear "Navigation" and "Bottom sheet".
 *
 * Only the members the app can actually reach are overridden: it has no
 * navigation bar and no slider, so their badge/value strings stay on the
 * library defaults rather than shipping translations nothing can utter.
 */
class KnopochkiStrings(private val resources: Resources) : Strings() {

    /**
     * The back arrow of every screen's top bar. Taken from AppCompat, which
     * already translates it into far more locales than this app carries.
     */
    override val navigation: CharSequence
        get() = resources.getString(androidx.appcompat.R.string.abc_action_bar_up_description)

    override val bottomSheet: CharSequence
        get() = resources.getString(R.string.a11y_bottom_sheet)

    override val navigationDrawer: CharSequence
        get() = resources.getString(R.string.a11y_navigation_drawer)
}
