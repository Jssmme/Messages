package org.fossify.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value
import org.fossify.messages.databinding.DialogAddWhitelistKeywordBinding
import org.fossify.messages.extensions.config

class AddWhitelistKeywordDialog(val activity: BaseSimpleActivity, private val originalKeyword: String? = null, val callback: () -> Unit) {
    init {
        val binding = DialogAddWhitelistKeywordBinding.inflate(activity.layoutInflater).apply {
            if (originalKeyword != null) {
                addWhitelistKeywordEdittext.setText(originalKeyword)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addWhitelistKeywordEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newWhitelistKeyword = binding.addWhitelistKeywordEdittext.value
                        if (originalKeyword != null && newWhitelistKeyword != originalKeyword) {
                            activity.config.removeWhitelistKeyword(originalKeyword)
                        }

                        if (newWhitelistKeyword.isNotEmpty()) {
                            activity.config.addWhitelistKeyword(newWhitelistKeyword)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
