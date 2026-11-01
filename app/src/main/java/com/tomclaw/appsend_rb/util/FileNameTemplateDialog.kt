package com.tomclaw.appsend_rb.util

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.DialogTemplateBinding
import com.tomclaw.appsend_rb.domain.TOKEN_CODE
import com.tomclaw.appsend_rb.domain.TOKEN_NAME
import com.tomclaw.appsend_rb.domain.TOKEN_PACKAGE
import com.tomclaw.appsend_rb.domain.TOKEN_VERSION
import kotlin.math.max
import kotlin.math.min

fun showFileNameTemplateDialog(context: Context, template: String, onSave: (String) -> Unit) {
    val binding = DialogTemplateBinding.inflate(LayoutInflater.from(context))
    binding.input.setText(template)
    binding.input.setSelection(template.length)
    listOf(
        binding.tokenName to TOKEN_NAME,
        binding.tokenPackage to TOKEN_PACKAGE,
        binding.tokenVersion to TOKEN_VERSION,
        binding.tokenCode to TOKEN_CODE,
    ).forEach { (chip, token) ->
        chip.setOnClickListener { binding.input.replaceSelection(token) }
    }
    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.export_file_name)
        .setView(binding.root)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ -> onSave(binding.input.text.toString()) }
        .show()
    val save = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    binding.input.doAfterTextChanged { save.isEnabled = !it.isNullOrBlank() }
}

private fun EditText.replaceSelection(replacement: String) {
    text.replace(min(selectionStart, selectionEnd), max(selectionStart, selectionEnd), replacement)
}
