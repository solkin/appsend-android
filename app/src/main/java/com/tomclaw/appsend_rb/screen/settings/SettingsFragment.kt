package com.tomclaw.appsend_rb.screen.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors
import com.tomclaw.appsend_rb.BuildConfig
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.applyThemeMode
import com.tomclaw.appsend_rb.databinding.FragmentSettingsBinding
import com.tomclaw.appsend_rb.domain.ThemeMode
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.settings.di.SettingsModule
import com.tomclaw.appsend_rb.util.ExportFolderPicker
import com.tomclaw.appsend_rb.util.openDeveloperPage
import com.tomclaw.appsend_rb.util.openStorePage
import javax.inject.Inject

class SettingsFragment : Fragment(R.layout.fragment_settings), SettingsRouter {

    @Inject
    lateinit var presenter: SettingsPresenter

    private lateinit var folderPicker: ExportFolderPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        requireActivity().application.getComponent()
            .settingsComponent(SettingsModule(AndroidSettingsEnvironment()))
            .inject(this)
        folderPicker = ExportFolderPicker(this, requireContext()) { presenter.onFolderPicked(it) }
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attachView(SettingsViewImpl(FragmentSettingsBinding.bind(view)))
    }

    override fun onStart() {
        super.onStart()
        presenter.attachRouter(this)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) presenter.onShown()
    }

    override fun onStop() {
        presenter.detachRouter()
        super.onStop()
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
    }

    override fun pickFolder() {
        folderPicker.pick()
    }

    override fun openRate() {
        requireContext().openStorePage(requireContext().packageName)
    }

    override fun openDeveloperApps() {
        requireContext().openDeveloperPage()
    }

    override fun restart() {
        requireActivity().recreate()
    }

}

private class AndroidSettingsEnvironment : SettingsEnvironment {

    override fun applyTheme(mode: ThemeMode) = applyThemeMode(mode)

    override fun dynamicColorsAvailable(): Boolean = DynamicColors.isDynamicColorAvailable()

    override fun version(): String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

}
