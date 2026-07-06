package org.fossify.messages.adapters

import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.getTextSize
import org.fossify.commons.extensions.highlightTextPart
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.ItemSearchResultBinding
import org.fossify.messages.extensions.deleteMessage
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.SearchResult

class SearchResultsAdapter(
    activity: SimpleActivity,
    var searchResults: ArrayList<SearchResult>,
    recyclerView: MyRecyclerView,
    highlightText: String,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private var fontSize = activity.getTextSize()
    private var textToHighlight = highlightText

    override fun getActionMenuId() = R.menu.cab_search_results

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_select_all).isVisible = selectedKeys.isNotEmpty()
            findItem(R.id.cab_deselect_all).isVisible = selectedKeys.isNotEmpty()
            findItem(R.id.cab_delete).isVisible = selectedKeys.isNotEmpty()
        }
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_select_all -> selectAll()
            R.id.cab_deselect_all -> {
                selectedKeys.clear()
                notifyDataSetChanged()
                finishActMode()
            }
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = searchResults.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = searchResults.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = searchResults.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchResult = searchResults[position]
        holder.bindView(searchResult, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, searchResult)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = searchResults.size

    fun updateItems(newItems: ArrayList<SearchResult>, highlightText: String = "") {
        if (newItems.hashCode() != searchResults.hashCode()) {
            searchResults = newItems.clone() as ArrayList<SearchResult>
            textToHighlight = highlightText
            notifyDataSetChanged()
        } else if (textToHighlight != highlightText) {
            textToHighlight = highlightText
            notifyDataSetChanged()
        }
    }

    private fun askConfirmDelete() {
        val selectedItems = getSelectedItems()
        val messageItems = selectedItems.filter { it.messageId != -1L }
        if (messageItems.isEmpty()) {
            finishActMode()
            return
        }

        val itemsCnt = messageItems.size
        val items = resources.getQuantityString(R.plurals.delete_messages, itemsCnt, itemsCnt)
        val question = String.format(
            resources.getString(R.string.delete_search_results_confirmation),
            items
        )

        // First confirmation
        ConfirmationDialog(activity, question) {
            // Second confirmation
            ConfirmationDialog(
                activity,
                resources.getString(R.string.delete_search_results_final_confirmation)
            ) {
                ensureBackgroundThread {
                    messageItems.forEach { result ->
                        activity.deleteMessage(result.messageId, result.isMMS)
                    }
                    activity.runOnUiThread {
                        refreshMessages()
                        refreshConversations()
                        finishActMode()
                        // Remove deleted items from the list
                        val remaining = searchResults.filter { it !in messageItems } as ArrayList<SearchResult>
                        updateItems(remaining, textToHighlight)
                    }
                }
            }
        }
    }

    private fun getSelectedItems(): ArrayList<SearchResult> {
        return searchResults.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<SearchResult>
    }

    private fun setupView(view: View, searchResult: SearchResult) {
        ItemSearchResultBinding.bind(view).apply {
            searchResultHolder.isSelected = selectedKeys.contains(searchResult.hashCode())

            searchResultTitle.apply {
                text = searchResult.title.highlightTextPart(textToHighlight, properPrimaryColor)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            searchResultSnippet.apply {
                text = searchResult.snippet.highlightTextPart(textToHighlight, properPrimaryColor)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            searchResultDate.apply {
                text = searchResult.date
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }

            SimpleContactsHelper(activity).loadContactImage(
                searchResult.photoUri, searchResultImage, searchResult.title
            )
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            val binding = ItemSearchResultBinding.bind(holder.itemView)
            Glide.with(activity).clear(binding.searchResultImage)
        }
    }
}
