package com.tomclaw.appsend_rb.screen.apps

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.FragmentAppsBinding
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
import com.tomclaw.appsend_rb.screen.details.createAppDetailsIntent
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsFragment
import com.tomclaw.appsend_rb.screen.exportprogress.createExportProgressIntent
import com.tomclaw.appsend_rb.screen.main.BottomBarHost
import com.tomclaw.appsend_rb.util.requestUninstall
import javax.inject.Inject

class AppsFragment : Fragment(R.layout.fragment_apps), AppsRouter {

    @Inject
    lateinit var presenter: AppsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        val state = savedInstanceState?.let { BundleCompat.getParcelable(it, KEY_STATE, AppsState::class.java) }
        requireActivity().application.getComponent()
            .appsComponent(AppsModule(requireContext(), state))
            .inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attachView(
            AppsViewImpl(
                FragmentAppsBinding.bind(view),
                viewLifecycleOwner,
                requireActivity().onBackPressedDispatcher,
                activity as? BottomBarHost,
            )
        )
    }

    override fun onStart() {
        super.onStart()
        presenter.attachRouter(this)
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) presenter.onHidden()
    }

    override fun onStop() {
        presenter.detachRouter()
        super.onStop()
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_STATE, presenter.saveState())
    }

    override fun openDetails(packageName: String) {
        startActivity(createAppDetailsIntent(requireContext(), packageName))
    }

    override fun openExportOptions(packages: List<String>) {
        ExportOptionsFragment.newInstance(packages).show(parentFragmentManager, ExportOptionsFragment.TAG)
    }

    override fun openExportProgress() {
        startActivity(createExportProgressIntent(requireContext()))
    }

    override fun uninstall(packageName: String) {
        requireContext().requestUninstall(packageName)
    }

}

private const val KEY_STATE = "apps_state"
