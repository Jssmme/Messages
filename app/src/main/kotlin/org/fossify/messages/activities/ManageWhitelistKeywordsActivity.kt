package org.fossify.messages.activities

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getTempFile
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.underlineText
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ExportResult
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivityManageWhitelistKeywordsBinding
import org.fossify.messages.dialogs.AddWhitelistKeywordDialog
import org.fossify.messages.dialogs.ExportWhitelistKeywordsDialog
import org.fossify.messages.dialogs.ManageWhitelistKeywordsAdapter
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.toArrayList
import org.fossify.messages.helpers.WhitelistKeywordsExporter
import org.fossify.messages.helpers.WhitelistKeywordsImporter
import java.io.FileOutputStream
import java.io.OutputStream

class ManageWhitelistKeywordsActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityManageWhitelistKeywordsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateWhitelistKeywords()
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.manageWhitelistKeywordsList))
        setupMaterialScrollListener(
            scrollingView = binding.manageWhitelistKeywordsList,
            topAppBar = binding.whitelistKeywordsAppbar
        )
        updateTextColors(binding.manageWhitelistKeywordsWrapper)

        binding.manageWhitelistKeywordsPlaceholder2.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                addOrEditWhitelistKeyword()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.whitelistKeywordsAppbar, NavigationIcon.Arrow)
    }

    private fun setupOptionsMenu() {
        binding.whitelistKeywordsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_whitelist_keyword -> {
                    addOrEditWhitelistKeyword()
                    true
                }

                R.id.export_whitelist_keywords -> {
                    tryExportWhitelistNumbers()
                    true
                }

                R.id.import_whitelist_keywords -> {
                    tryImportWhitelistKeywords()
                    true
                }

                else -> false
            }
        }
    }

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            try {
                val outputStream = uri?.let { contentResolver.openOutputStream(it) }
                if (outputStream != null) {
                    exportWhitelistKeywordsTo(outputStream)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            try {
                if (uri != null) {
                    tryImportWhitelistKeywordsFromFile(uri)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

    private fun tryImportWhitelistKeywords() {
        val mimeType = "text/plain"
        try {
            getContent.launch(mimeType)
        } catch (_: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    private fun tryImportWhitelistKeywordsFromFile(uri: Uri) {
        when (uri.scheme) {
            "file" -> importWhitelistKeywords(uri.path!!)
            "content" -> {
                val tempFile = getTempFile("whitelist", "whitelist_keywords.txt")
                if (tempFile == null) {
                    toast(org.fossify.commons.R.string.unknown_error_occurred)
                    return
                }

                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val out = FileOutputStream(tempFile)
                    inputStream!!.copyTo(out)
                    importWhitelistKeywords(tempFile.absolutePath)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }

            else -> toast(org.fossify.commons.R.string.invalid_file_format)
        }
    }

    private fun importWhitelistKeywords(path: String) {
        ensureBackgroundThread {
            val result = WhitelistKeywordsImporter(this).importWhitelistKeywords(path)
            toast(
                when (result) {
                    WhitelistKeywordsImporter.ImportResult.IMPORT_OK -> org.fossify.commons.R.string.importing_successful
                    WhitelistKeywordsImporter.ImportResult.IMPORT_FAIL -> org.fossify.commons.R.string.no_items_found
                }
            )
            updateWhitelistKeywords()
        }
    }

    private fun exportWhitelistKeywordsTo(outputStream: OutputStream?) {
        ensureBackgroundThread {
            val whitelistKeywords = config.whitelistKeywords.toArrayList()
            if (whitelistKeywords.isEmpty()) {
                toast(org.fossify.commons.R.string.no_entries_for_exporting)
            } else {
                WhitelistKeywordsExporter.exportWhitelistKeywords(whitelistKeywords, outputStream) {
                    toast(
                        when (it) {
                            ExportResult.EXPORT_OK -> org.fossify.commons.R.string.exporting_successful
                            else -> org.fossify.commons.R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }

    private fun tryExportWhitelistNumbers() {
        ExportWhitelistKeywordsDialog(
            activity = this,
            path = config.lastWhitelistKeywordExportPath,
            hidePath = true
        ) { file ->
            try {
                createDocument.launch(file.name)
            } catch (_: ActivityNotFoundException) {
                toast(
                    org.fossify.commons.R.string.system_service_disabled,
                    Toast.LENGTH_LONG
                )
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    override fun refreshItems() {
        updateWhitelistKeywords()
    }

    private fun updateWhitelistKeywords() {
        ensureBackgroundThread {
            val whitelistKeywords = config.whitelistKeywords.sorted().toArrayList()
            runOnUiThread {
                ManageWhitelistKeywordsAdapter(
                    activity = this,
                    whitelistKeywords = whitelistKeywords,
                    listener = this,
                    recyclerView = binding.manageWhitelistKeywordsList
                ) {
                    addOrEditWhitelistKeyword(it as String)
                }.apply {
                    binding.manageWhitelistKeywordsList.adapter = this
                }

                binding.manageWhitelistKeywordsPlaceholder.beVisibleIf(whitelistKeywords.isEmpty())
                binding.manageWhitelistKeywordsPlaceholder2.beVisibleIf(whitelistKeywords.isEmpty())
            }
        }
    }

    private fun addOrEditWhitelistKeyword(keyword: String? = null) {
        AddWhitelistKeywordDialog(this, keyword) {
            updateWhitelistKeywords()
        }
    }
}
