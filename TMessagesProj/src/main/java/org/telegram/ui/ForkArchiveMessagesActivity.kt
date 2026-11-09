package org.telegram.ui

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
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
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

/**
 * Displays archived messages for a single dialog in Berry Archive.
 * Written in Kotlin.
 */
class ForkArchiveMessagesActivity(private val dialogId: Long) : ForkSettingsFragment() {

    private val messages = mutableListOf<ForkArchiveManager.ArchivedMessage>()
    private var listView: RecyclerListView? = null
    private var adapter: MessagesAdapter? = null
    private var emptyView: EmptyTextProgressView? = null

    override fun createView(context: Context): View {
        val frameLayout = FrameLayout(context).apply {
            setBackgroundColor(Theme.getColor(Theme.key_chat_outBubble))
        }
        fragmentView = frameLayout

        listView = RecyclerListView(context).apply {
            val lm = LinearLayoutManager(context)
            lm.stackFromEnd = true
            layoutManager = lm
            this@ForkArchiveMessagesActivity.adapter = MessagesAdapter(context)
            adapter = this@ForkArchiveMessagesActivity.adapter
        }
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        emptyView = EmptyTextProgressView(context).apply {
            setText(LocaleController.getString(R.string.NoResult))
            showTextView()
        }
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        listView?.emptyView = emptyView

        loadMessages()
        fragmentView = ForkFloatingHeader.wrap(
            context,
            actionBar,
            frameLayout,
            resolveDialogName(dialogId)
        ) { finishFragment() }
        return fragmentView
    }

    private fun loadMessages() {
        ForkArchiveManager.loadMessagesAsync(currentAccount, dialogId) { result ->
            if (fragmentView == null) return@loadMessagesAsync
            messages.clear()
            messages.addAll(result)
            adapter?.notifyDataSetChanged()
            emptyView?.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun resolveDialogName(did: Long): String {
        if (DialogObject.isUserDialog(did)) {
            val user = messagesController.getUser(did)
            if (user != null) return UserObject.getUserName(user)
        } else {
            val chat = messagesController.getChat(-did)
            if (chat != null && chat.title != null) return chat.title
        }
        return "Chat #$did"
    }

    private inner class MessagesAdapter(private val context: Context) : RecyclerListView.SelectionAdapter() {
        override fun isEnabled(holder: RecyclerView.ViewHolder): Boolean = false

        override fun getItemCount(): Int = messages.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val cell = MessageItemCell(context).apply {
                layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            }
            return RecyclerListView.Holder(cell)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = messages[position]
            val cell = holder.itemView as MessageItemCell
            cell.setData(msg.senderName, msg.messageText, LocaleController.formatDateTime(msg.date.toLong(), false))
        }
    }

    private class MessageItemCell(context: Context) : FrameLayout(context) {
        private val senderView: TextView
        private val messageView: TextView
        private val timeView: TextView

        init {
            setPadding(AndroidUtilities.dp(12f), AndroidUtilities.dp(4f), AndroidUtilities.dp(12f), AndroidUtilities.dp(4f))

            val bubble = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(AndroidUtilities.dp(12f), AndroidUtilities.dp(8f), AndroidUtilities.dp(12f), AndroidUtilities.dp(8f))
                background = Theme.createRoundRectDrawable(AndroidUtilities.dp(12f), Theme.getColor(Theme.key_chat_inBubble))
            }

            senderView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                typeface = AndroidUtilities.bold()
                setTextColor(Theme.getColor(Theme.key_chat_inAdminText))
            }
            bubble.addView(senderView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 4f))

            messageView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
                setTextColor(Theme.getColor(Theme.key_chat_messageTextIn))
                setTextIsSelectable(true)
            }
            bubble.addView(messageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

            timeView = TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
                setTextColor(Theme.getColor(Theme.key_chat_inTimeText))
            }
            bubble.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 0f))

            addView(bubble, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat()))
        }

        fun setData(sender: String?, message: String, time: String) {
            if (TextUtils.isEmpty(sender)) {
                senderView.visibility = GONE
            } else {
                senderView.visibility = VISIBLE
                senderView.text = sender
            }
            messageView.text = message
            timeView.text = time
        }
    }
}
