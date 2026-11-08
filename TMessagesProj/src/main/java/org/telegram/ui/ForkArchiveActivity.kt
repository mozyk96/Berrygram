package org.telegram.ui

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DialogObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.UserObject
import org.telegram.messenger.fork.storage.ForkArchiveManager
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenu
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

/**
 * Berry Archive UI — provides browsing, full-text search, and management
 * of locally preserved chats and messages. Written in Kotlin.
 */
class ForkArchiveActivity : BaseFragment() {

    private var listView: RecyclerListView? = null
    private var adapter: ListAdapter? = null
    private var emptyView: EmptyTextProgressView? = null
    private val dialogs = mutableListOf<ForkArchiveManager.ArchivedDialogSummary>()
    private val searchResults = mutableListOf<ForkArchiveManager.ArchivedMessage>()
    private var isSearchMode = false
    private var currentQuery = ""

    override fun createView(context: Context): View {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(ForkLocale.get(R.string.ForkArchiveTitle))
        actionBar.setAllowOverlayTitle(true)

        val menu: ActionBarMenu = actionBar.createMenu()
        val searchItem = menu.addItem(0, R.drawable.outline_header_search).setIsSearchField(true)
        searchItem.setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            override fun onSearchExpand() {
                isSearchMode = true
                searchResults.clear()
                adapter?.notifyDataSetChanged()
            }

            override fun onSearchCollapse() {
                isSearchMode = false
                currentQuery = ""
                searchResults.clear()
                loadDialogs()
            }

            override fun onTextChanged(editText: EditText) {
                currentQuery = editText.text.toString().trim()
                searchResults.clear()
                if (currentQuery.isNotEmpty()) {
                    searchResults.addAll(ForkArchiveManager.getInstance(currentAccount).search(currentQuery))
                }
                adapter?.notifyDataSetChanged()
                updateEmptyView()
            }
        })

        val otherItem = menu.addItem(1, R.drawable.ic_ab_other)
        otherItem.addSubItem(10, R.drawable.msg_delete, ForkLocale.get(R.string.ForkArchiveClear))

        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                } else if (id == 10) {
                    showClearConfirmation()
                }
            }
        })

        val frameLayout = FrameLayout(context).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        fragmentView = frameLayout

        listView = RecyclerListView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this@ForkArchiveActivity.adapter = ListAdapter(context)
            adapter = this@ForkArchiveActivity.adapter
        }
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        emptyView = EmptyTextProgressView(context).apply {
            setText(ForkLocale.get(R.string.ForkArchiveEmpty))
            showTextView()
        }
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        listView?.emptyView = emptyView

        listView?.setOnItemClickListener { _, position ->
            if (isSearchMode && position < searchResults.size) {
                val msg = searchResults[position]
                presentFragment(ForkArchiveMessagesActivity(msg.dialogId))
            } else if (!isSearchMode && position < dialogs.size) {
                val d = dialogs[position]
                presentFragment(ForkArchiveMessagesActivity(d.dialogId))
            }
        }

        loadDialogs()
        return frameLayout
    }

    private fun loadDialogs() {
        dialogs.clear()
        dialogs.addAll(ForkArchiveManager.getInstance(currentAccount).getArchivedDialogs())
        adapter?.notifyDataSetChanged()
        updateEmptyView()
    }

    private fun updateEmptyView() {
        emptyView?.let { view ->
            if (isSearchMode && currentQuery.isNotEmpty()) {
                view.setText(LocaleController.getString(R.string.NoResult))
            } else {
                view.setText(ForkLocale.get(R.string.ForkArchiveEmpty))
            }
        }
    }

    private fun showClearConfirmation() {
        val activity = parentActivity ?: return
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(ForkLocale.get(R.string.ForkArchiveClear))
        builder.setMessage(ForkLocale.get(R.string.ForkArchiveClearConfirm))
        builder.setPositiveButton(LocaleController.getString(R.string.ClearButton)) { _, _ ->
            ForkArchiveManager.getInstance(currentAccount).clearArchive()
            loadDialogs()
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null)
        showDialog(builder.create())
    }

    private inner class ListAdapter(private val context: Context) : RecyclerListView.SelectionAdapter() {
        override fun isEnabled(holder: RecyclerView.ViewHolder): Boolean = true

        override fun getItemCount(): Int = if (isSearchMode) searchResults.size else dialogs.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = DialogCell(context).apply {
                layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            }
            return RecyclerListView.Holder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val cell = holder.itemView as DialogCell
            if (isSearchMode) {
                val msg = searchResults[position]
                val title = resolveDialogName(msg.dialogId)
                cell.setData(title, msg.messageText, LocaleController.formatDateTime(msg.date.toLong(), false))
            } else {
                val d = dialogs[position]
                val title = resolveDialogName(d.dialogId)
                val countStr = LocaleController.formatPluralString("Messages", d.messageCount)
                cell.setData(title, "$countStr • ${d.lastSnippet ?: ""}", LocaleController.formatDateTime(d.lastDate.toLong(), false))
            }
        }
    }

    private fun resolveDialogName(dialogId: Long): String {
        if (DialogObject.isUserDialog(dialogId)) {
            val user = messagesController.getUser(dialogId)
            if (user != null) return UserObject.getUserName(user)
        } else {
            val chat = messagesController.getChat(-dialogId)
            if (chat != null && chat.title != null) return chat.title
        }
        return "Chat #$dialogId"
    }

    private class DialogCell(context: Context) : FrameLayout(context) {
        private val titleView: TextView
        private val subtitleView: TextView
        private val dateView: TextView

        init {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setPadding(AndroidUtilities.dp(16f), AndroidUtilities.dp(12f), AndroidUtilities.dp(16f), AndroidUtilities.dp(12f))

            val linear = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            titleView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                typeface = AndroidUtilities.bold()
                setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
            }
            topRow.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f))

            dateView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            }
            topRow.addView(dateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

            linear.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

            subtitleView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2))
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
            }
            linear.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 0f))

            addView(linear, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat()))
        }

        fun setData(title: String, subtitle: String, date: String) {
            titleView.text = title
            subtitleView.text = subtitle
            dateView.text = date
        }
    }
}
