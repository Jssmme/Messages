package org.fossify.messages.dialogs

import android.view.*
import androidx.appcompat.widget.PopupMenu
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getPopupMenuTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemManageWhitelistKeywordBinding
import org.fossify.messages.extensions.config

class ManageWhitelistKeywordsAdapter(
    activity: BaseSimpleActivity, var whitelistKeywords: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_whitelist_keywords

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_copy_whitelist_keyword).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_copy_whitelist_keyword -> copyKeywordToClipboard()
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = whitelistKeywords.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = whitelistKeywords.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = whitelistKeywords.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageWhitelistKeywordBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val whitelistKeyword = whitelistKeywords[position]
        holder.bindView(whitelistKeyword, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, whitelistKeyword)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = whitelistKeywords.size

    private fun getSelectedItems() = whitelistKeywords.filter { selectedKeys.contains(it.hashCode()) }

    private fun setupView(view: View, whitelistKeyword: String) {
        ItemManageWhitelistKeywordBinding.bind(view).apply {
            root.setupViewBackground(activity)
            manageWhitelistKeywordHolder.isSelected = selectedKeys.contains(whitelistKeyword.hashCode())
            manageWhitelistKeywordTitle.apply {
                text = whitelistKeyword
                setTextColor(textColor)
            }

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, whitelistKeyword)
            }
        }
    }

    private fun showPopupMenu(view: View, whitelistKeyword: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val whitelistKeywordId = whitelistKeyword.hashCode()
                when (item.itemId) {
                    R.id.cab_copy_whitelist_keyword -> {
                        executeItemMenuOperation(whitelistKeywordId) {
                            copyKeywordToClipboard()
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(whitelistKeywordId) {
                            deleteSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(whitelistKeywordId: Int, callback: () -> Unit) {
        selectedKeys.add(whitelistKeywordId)
        callback()
        selectedKeys.remove(whitelistKeywordId)
    }

    private fun copyKeywordToClipboard() {
        val selectedKeyword = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(selectedKeyword)
        finishActMode()
    }

    private fun deleteSelection() {
        val deleteWhitelistKeywords = HashSet<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deleteWhitelistKeywords.add(it)
            activity.config.removeWhitelistKeyword(it)
        }

        whitelistKeywords.removeAll(deleteWhitelistKeywords)
        removeSelectedItems(positions)
        if (whitelistKeywords.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
