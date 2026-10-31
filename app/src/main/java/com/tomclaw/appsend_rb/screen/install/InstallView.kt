package com.tomclaw.appsend_rb.screen.install

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ActivityInstallBinding
import com.tomclaw.appsend_rb.domain.InstallFailure
import com.tomclaw.appsend_rb.domain.InstallIssue
import com.tomclaw.appsend_rb.util.AppIcons
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface InstallView {

    fun render(model: InstallViewModel)

    fun installClicks(): Observable<Unit>

    fun replaceClicks(): Observable<Unit>

    fun openClicks(): Observable<Unit>

    fun dismissClicks(): Observable<Unit>

}

class InstallViewImpl(
    private val binding: ActivityInstallBinding,
    lifecycleOwner: LifecycleOwner,
    backDispatcher: OnBackPressedDispatcher,
) : InstallView {

    private val context = binding.root.context
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.app_icon_size_large)

    private val installRelay = PublishRelay.create<Unit>()
    private val replaceRelay = PublishRelay.create<Unit>()
    private val openRelay = PublishRelay.create<Unit>()
    private val dismissRelay = PublishRelay.create<Unit>()

    init {
        backDispatcher.addCallback(lifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismissRelay.accept(Unit)
        })
        binding.sheetContent.applySystemBarInsets(bottom = true)
        binding.scrim.setOnClickListener { dismissRelay.accept(Unit) }
        binding.dismissButton.setOnClickListener { dismissRelay.accept(Unit) }
        binding.installButton.setOnClickListener { installRelay.accept(Unit) }
        binding.replaceButton.setOnClickListener { replaceRelay.accept(Unit) }
        binding.openButton.setOnClickListener { openRelay.accept(Unit) }
        listOf(binding.deviceRow, binding.signatureRow, binding.androidRow, binding.filesRow)
            .forEachIndexed { index, row ->
                row.root.updateAppearance(index, CHECK_ROW_COUNT)
                row.chevron.isVisible = false
                row.icon.setImageResource(R.drawable.ic_check)
            }
        binding.deviceRow.title.setText(R.string.install_check_device)
        binding.signatureRow.title.setText(R.string.install_check_signature)
        binding.androidRow.title.setText(R.string.install_check_android)
        binding.filesRow.title.setText(R.string.install_check_files)
        binding.filesRow.icon.setImageResource(R.drawable.ic_file)
    }

    override fun render(model: InstallViewModel) {
        reset()
        when (model) {
            InstallViewModel.Loading -> renderLoading()
            InstallViewModel.Unreadable -> renderUnreadable()
            is InstallViewModel.Ready -> renderReady(model)
            is InstallViewModel.Blocked -> renderBlocked(model)
            is InstallViewModel.Installing -> renderInstalling(model)
            is InstallViewModel.Installed -> renderInstalled(model)
            is InstallViewModel.Failed -> renderFailed(model)
        }
    }

    private fun reset() {
        binding.loading.isVisible = false
        binding.header.isVisible = true
        binding.checks.isVisible = false
        binding.notice.isVisible = false
        binding.problem.isVisible = false
        binding.progress.isVisible = false
        binding.installButton.isVisible = false
        binding.replaceButton.isVisible = false
        binding.openButton.isVisible = false
        binding.dismissButton.isVisible = true
        binding.dismissButton.setText(R.string.cancel)
    }

    private fun renderHeader(packageName: String?, title: String, subtitle: String) {
        binding.title.text = title
        binding.subtitle.text = subtitle
        binding.icon.setImageResource(R.drawable.ic_file)
        packageName?.let {
            AppIcons.load(context, it, iconSize) { _, bitmap -> binding.icon.setImageBitmap(bitmap) }
        }
    }

    private fun renderLoading() {
        binding.header.isVisible = false
        binding.loading.isVisible = true
    }

    private fun renderUnreadable() {
        binding.header.isVisible = false
        showProblem(R.string.install_unreadable_title, context.getString(R.string.install_unreadable_message))
        binding.dismissButton.setText(R.string.done)
    }

    private fun renderReady(model: InstallViewModel.Ready) {
        renderHeader(
            model.packageName,
            context.getString(
                if (model.update) R.string.install_title_update else R.string.install_title_new,
                model.label,
            ),
            if (model.fromVersion != null) {
                context.getString(R.string.install_version_change, model.fromVersion, model.toVersion, model.size)
            } else {
                context.getString(R.string.install_version, model.toVersion, model.size)
            },
        )
        binding.checks.isVisible = true
        binding.deviceRow.subtitle.setText(R.string.install_check_device_ok)
        binding.signatureRow.root.isVisible = model.signatureChecked
        binding.signatureRow.subtitle.setText(R.string.install_check_signature_ok)
        binding.androidRow.subtitle.text = context.getString(R.string.install_check_android_value, model.minSdk)
        binding.filesRow.subtitle.text = if (model.splitCount == 0) {
            context.getString(R.string.install_check_files_base)
        } else {
            context.resources.getQuantityString(
                R.plurals.install_check_files_value,
                model.splitCount,
                model.splitCount,
            )
        }
        binding.notice.isVisible = model.needsPermission
        binding.installButton.isVisible = true
        binding.installButton.setText(
            if (model.needsPermission) R.string.install_allow_and_install else R.string.install
        )
    }

    private fun renderBlocked(model: InstallViewModel.Blocked) {
        renderHeader(
            model.packageName,
            model.label,
            if (model.installedVersion != null) {
                context.getString(R.string.install_versions_compare, model.fileVersion, model.installedVersion)
            } else {
                context.getString(R.string.install_file_version, model.fileVersion)
            },
        )
        when (model.issue) {
            InstallIssue.SIGNATURE_MISMATCH -> showProblem(
                R.string.install_issue_signature_title,
                context.getString(R.string.install_issue_signature_message),
            )
            InstallIssue.DOWNGRADE -> showProblem(
                R.string.install_issue_downgrade_title,
                context.getString(R.string.install_issue_downgrade_message),
            )
            InstallIssue.ANDROID_TOO_OLD -> showProblem(
                R.string.install_issue_android_title,
                context.getString(R.string.install_issue_android_message),
            )
            InstallIssue.WRONG_ARCHITECTURE -> showProblem(
                R.string.install_issue_device_title,
                context.getString(R.string.install_issue_device_message),
            )
        }
        binding.replaceButton.isVisible = model.canReplace
        if (!model.canReplace) binding.dismissButton.setText(R.string.done)
    }

    private fun renderInstalling(model: InstallViewModel.Installing) {
        renderHeader(
            model.packageName,
            context.getString(R.string.install_in_progress, model.label),
            context.getString(
                when (model.stage) {
                    InstallStage.WRITING -> R.string.install_writing
                    InstallStage.CONFIRMING -> R.string.install_awaiting_confirmation
                    InstallStage.APPLYING -> R.string.install_applying
                }
            ),
        )
        binding.progress.isVisible = true
        if (model.stage == InstallStage.APPLYING) {
            // The system reports no progress of its own, and a full bar would look frozen.
            // Setting a progress here would turn the indicator determinate again.
            binding.progress.isIndeterminate = true
            binding.dismissButton.setText(R.string.install_hide)
        } else {
            binding.progress.isIndeterminate = false
            binding.progress.setProgressCompat(model.percent, true)
        }
    }

    private fun renderInstalled(model: InstallViewModel.Installed) {
        renderHeader(
            model.packageName,
            context.getString(R.string.install_done, model.label),
            context.getString(R.string.install_file_version, model.version),
        )
        binding.openButton.isVisible = true
        binding.dismissButton.setText(R.string.done)
    }

    private fun renderFailed(model: InstallViewModel.Failed) {
        renderHeader(model.packageName, model.label, context.getString(R.string.install_failed))
        showProblem(
            R.string.install_failed,
            context.getString(
                when (model.failure) {
                    InstallFailure.BLOCKED -> R.string.install_failure_blocked
                    InstallFailure.CONFLICT -> R.string.install_failure_conflict
                    InstallFailure.INCOMPATIBLE -> R.string.install_failure_incompatible
                    InstallFailure.INVALID -> R.string.install_failure_invalid
                    InstallFailure.STORAGE -> R.string.install_failure_storage
                    InstallFailure.ABORTED, InstallFailure.UNKNOWN -> R.string.install_failure_unknown
                }
            ),
        )
        binding.dismissButton.setText(R.string.done)
    }

    private fun showProblem(title: Int, message: String) {
        binding.problem.isVisible = true
        binding.problemTitle.setText(title)
        binding.problemMessage.text = message
    }

    override fun installClicks(): Observable<Unit> = installRelay

    override fun replaceClicks(): Observable<Unit> = replaceRelay

    override fun openClicks(): Observable<Unit> = openRelay

    override fun dismissClicks(): Observable<Unit> = dismissRelay

}

private const val CHECK_ROW_COUNT = 4
