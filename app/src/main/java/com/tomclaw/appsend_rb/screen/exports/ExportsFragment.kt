package com.tomclaw.appsend_rb.screen.exports

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.FragmentExportsBinding
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.exports.di.ExportsModule
import com.tomclaw.appsend_rb.screen.install.createInstallIntent
import com.tomclaw.appsend_rb.screen.main.BottomBarHost
import com.tomclaw.appsend_rb.util.ExportFolderPicker
import com.tomclaw.appsend_rb.util.shareFiles
import javax.inject.Inject

class ExportsFragment : Fragment(R.layout.fragment_exports), ExportsRouter {

    @Inject
    lateinit var presenter: ExportsPresenter

    private lateinit var folderPicker: ExportFolderPicker
    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { presenter.onPackageFilePicked(it.toString()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val state = savedInstanceState?.let { BundleCompat.getParcelable(it, KEY_STATE, ExportsState::class.java) }
        requireActivity().application.getComponent()
            .exportsComponent(ExportsModule(requireContext(), state))
            .inject(this)
        folderPicker = ExportFolderPicker(this, requireContext()) { presenter.onFolderPicked(it) }
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attachView(
            ExportsViewImpl(
                FragmentExportsBinding.bind(view),
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
        if (hidden) presenter.onHidden() else presenter.onResume()
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

    override fun shareFiles(files: List<ExportedFile>) {
        requireContext().shareFiles(files)
    }

    override fun install(uri: String) {
        startActivity(createInstallIntent(requireContext(), uri))
    }

    override fun pickPackageFile() {
        filePicker.launch(arrayOf("*/*"))
    }

    override fun pickFolder() {
        folderPicker.pick()
    }

}

private const val KEY_STATE = "exports_state"
