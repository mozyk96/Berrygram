package org.telegram.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DialogObject
import org.telegram.messenger.R
import org.telegram.messenger.UserObject
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper

/**
 * Lets the owner choose the chats whose history is imported into the Berry
 * archive. A single chat, several chats or every eligible chat can be chosen.
 */
class ForkArchiveChatPickerActivity : BaseFragment() {

    private val chats = mutableListOf<Long>()
    private val selected = LinkedHashSet<Long>()
    private var listView: RecyclerView? = null
    private var emptyView: EmptyTextProgressView? = null

    interface Delegate {
        fun onChatsPicked(dialogIds: List<Long>)
    }

    private var delegate: Delegate? = null

    fun setDelegate(delegate: Delegate?): ForkArchiveChatPickerActivity {
        this.delegate = delegate
        return this
    }

    override fun createView(context: Context): View {
        actionBar.setTitle(ForkLocale.get(R.string.ForkArchiveImportTitle))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })

        val frame = FrameLayout(context)
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))

        val list = RecyclerView(context)
        list.layoutManager = LinearLayoutManager(context)
        list.setAdapter(ChatAdapter())
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                if (position == 0) {
                    outRect.top = AndroidUtilities.dp(12f).toInt()
                }
            }
        })
        frame.addView(list, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        listView = list

        emptyView = EmptyTextProgressView(context).apply {
            setText(ForkLocale.get(R.string.ForkArchiveNoChats))
            showTextView()
        }
        frame.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        emptyView?.visibility = View.GONE

        loadChats()
        fragmentView = frame
        return frame
    }

    private fun loadChats() {
        val controller = org.telegram.messenger.MessagesController.getInstance(currentAccount)
        chats.clear()
        val dialogs = controller.dialogs_dict
        for (index in 0 until dialogs.size()) {
            val dialog = dialogs.valueAt(index) ?: continue
            if (dialog.id == 0L) continue
            if (DialogObject.isEncryptedDialog(dialog.id)) {
                // Secret chats are never imported.
                continue
            }
            chats.add(dialog.id)
        }
        listView?.adapter?.notifyDataSetChanged()
        emptyView?.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun title(dialogId: Long): String {
        val controller = org.telegram.messenger.MessagesController.getInstance(currentAccount)
        return try {
            if (dialogId > 0) {
                val user = controller.getUser(dialogId)
                if (user != null) UserObject.getUserName(user) else ""
            } else {
                val chat = controller.getChat(-dialogId)
                chat?.title ?: ""
            }
        } catch (error: Throwable) {
            ""
        }
    }

    private inner class ChatAdapter : RecyclerView.Adapter<Holder>() {
        override fun getItemCount(): Int = chats.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val row = TextCheckCell(parent.context)
            return Holder(row)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cell = holder.itemView as TextCheckCell
            if (position == 0) {
                cell.setTextAndCheck(
                    ForkLocale.get(R.string.ForkArchiveSelectAll),
                    chats.isNotEmpty() && selected.size == chats.size,
                    false
                )
                cell.setOnClickListener {
                    if (selected.size == chats.size) {
                        selected.clear()
                    } else {
                        selected.clear()
                        selected.addAll(chats)
                    }
                    notifyDataSetChanged()
                }
                return
            }
            val dialogId = chats[position - 1]
            cell.setTextAndCheck(title(dialogId), selected.contains(dialogId), false)
            cell.setOnClickListener {
                if (!selected.add(dialogId)) {
                    selected.remove(dialogId)
                }
                notifyDataSetChanged()
            }
        }

        override fun onViewRecycled(holder: Holder) {
            super.onViewRecycled(holder)
        }
    }

    private class Holder(view: View) : RecyclerView.ViewHolder(view)

    /** Starts the import with the current selection. */
    fun startImport(mode: org.telegram.messenger.fork.storage.ForkArchiveImporter.Mode) {
        val ids = if (selected.isEmpty()) chats.toList() else selected.toList()
        delegate?.onChatsPicked(ids)
        finishFragment()
    }

    fun selectedCount(): Int = selected.size
}
