package org.fossify.messages.helpers

import android.app.Activity
import org.fossify.commons.extensions.showErrorToast
import org.fossify.messages.extensions.config

import java.io.File

class WhitelistKeywordsImporter(
    private val activity: Activity,
) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK
    }

    fun importWhitelistKeywords(path: String): ImportResult {
        return try {
            val inputStream = File(path).inputStream()
            val keywords = inputStream.bufferedReader().use {
                val content = it.readText().trimEnd().split(WHITELIST_KEYWORDS_EXPORT_DELIMITER)
                content
            }
            if (keywords.isNotEmpty()) {
                keywords.forEach { keyword: String ->
                    activity.config.addWhitelistKeyword(keyword)
                }
                ImportResult.IMPORT_OK
            } else {
                ImportResult.IMPORT_FAIL
            }

        } catch (e: Exception) {
            activity.showErrorToast(e)
            ImportResult.IMPORT_FAIL
        }
    }
}
