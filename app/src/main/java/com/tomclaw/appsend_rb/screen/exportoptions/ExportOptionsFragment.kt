package com.tomclaw.appsend_rb.screen.exportoptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tomclaw.appsend_rb.databinding.FragmentExportOptionsBinding
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.exportoptions.di.ExportOptionsModule
import com.tomclaw.appsend_rb.screen.exportprogress.createExportProgressIntent
import com.tomclaw.appsend_rb.util.ExportFolderPicker
import com.tomclaw.appsend_rb.util.StorageAccessRequester
import com.tomclaw.appsend_rb.util.openAppSystemSettings
import javax.inject.Inject

class ExportOptionsFragment : BottomSheetDialogFragment(), ExportOptionsRouter {

    @Inject
    lateinit var presenter: ExportOptionsPresenter

    private val storageAccess = StorageAccessRequester(this, ::shouldShowRequestPermissionRationale) {
        presenter.onStorageAccess(it)
    }
    private lateinit var folderPicker: ExportFolderPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        val packages = requireArguments().getStringArrayList(ARG_PACKAGES).orEmpty()
        requireActivity().application.getComponent()
            .exportOptionsComponent(
                ExportOptionsModule(
                    requireContext(),
                    packages,
                    savedInstanceState?.let {
                        BundleCompat.getParcelable(it, KEY_STATE, ExportOptionsState::class.java)
                    },
                )
            )
            .inject(this)
        folderPicker = ExportFolderPicker(this, requireContext()) { presenter.onFolderPicked(it) }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = FragmentExportOptionsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attachView(ExportOptionsViewImpl(FragmentExportOptionsBinding.bind(view)))
    }

    override fun onStart() {
        super.onStart()
        presenter.attachRouter(this)
    }

    override fun onStop() {
        presenter.detachRouter()
        super.onStop()
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_STATE, presenter.saveState())
    }

    override fun pickFolder() {
        folderPicker.pick()
    }

    override fun requestStorageAccess() {
        storageAccess.request()
    }

    override fun openOwnSettings() {
        requireContext().openAppSystemSettings(requireContext().packageName)
    }

    override fun openExportProgress() {
        startActivity(createExportProgressIntent(requireContext()))
    }

    override fun leave() {
        dismissAllowingStateLoss()
    }

    companion object {

        const val TAG = "export_options"

        fun newInstance(packages: List<String>) = ExportOptionsFragment().apply {
            arguments = bundleOf(ARG_PACKAGES to ArrayList(packages))
        }

    }

}

private const val ARG_PACKAGES = "packages"
private const val KEY_STATE = "export_options_state"
