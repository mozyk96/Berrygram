package org.telegram.ui.Components

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.fork.storage.ForkEditHistoryStorage
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.BottomSheet
import org.telegram.ui.ActionBar.Theme

/**
 * BottomSheet displaying the edit revisions of a message in Berrygram.
 * Written in Kotlin.
 */
class ForkEditHistoryBottomSheet(
    context: Context,
    currentAccount: Int,
    dialogId: Long,
    messageId: Int,
    currentText: String?,
    resourcesProvider: Theme.ResourcesProvider?
) : BottomSheet(context, true, resourcesProvider) {

    init {
        setApplyTopPadding(false)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                AndroidUtilities.dp(18f),
                AndroidUtilities.dp(16f),
                AndroidUtilities.dp(18f),
                AndroidUtilities.dp(20f)
            )
        }

        val titleView = TextView(context).apply {
            text = LocaleController.getString(R.string.ForkEditHistoryTitle)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            typeface = AndroidUtilities.bold()
            setTextColor(getThemedColor(Theme.key_dialogTextBlack))
        }
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 16f))

        val scrollView = NestedScrollView(context)
        val revisionsList = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Current version
        addRevisionCard(
            context,
            revisionsList,
            LocaleController.getString(R.string.ForkEditHistoryCurrent),
            currentText ?: "",
            true
        )

        // Previous revisions
        val revisions = ForkEditHistoryStorage.getInstance(currentAccount).getRevisions(dialogId, messageId)
        for (i in revisions.indices.reversed()) {
            val rev = revisions[i]
            val dateStr = LocaleController.formatDateTime(rev.editDate.toLong(), false)
            addRevisionCard(context, revisionsList, dateStr, rev.text, false)
        }

        scrollView.addView(revisionsList)
        container.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        setCustomView(container)
    }

    private fun addRevisionCard(
        context: Context,
        parent: LinearLayout,
        header: String,
        content: String,
        isCurrent: Boolean
    ) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                AndroidUtilities.dp(12f),
                AndroidUtilities.dp(10f),
                AndroidUtilities.dp(12f),
                AndroidUtilities.dp(10f)
            )
            background = Theme.createRoundRectDrawable(
                AndroidUtilities.dp(10f),
                if (isCurrent) (getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader) and 0x1AFFFFFF)
                else (getThemedColor(Theme.key_chat_inBubble) and 0x33FFFFFF)
            )
        }

        val headerView = TextView(context).apply {
            text = header
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            setTextColor(
                getThemedColor(
                    if (isCurrent) Theme.key_windowBackgroundWhiteBlueText
                    else Theme.key_dialogTextGray
                )
            )
        }
        card.addView(headerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 6f))

        val bodyView = TextView(context).apply {
            text = if (TextUtils.isEmpty(content)) "—" else content
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setTextColor(getThemedColor(Theme.key_dialogTextBlack))
            setTextIsSelectable(true)
        }
        card.addView(bodyView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        parent.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 10f))
    }

    companion object {
        @JvmStatic
        fun show(
            fragment: BaseFragment?,
            currentAccount: Int,
            dialogId: Long,
            messageId: Int,
            currentText: String?
        ) {
            val activity = fragment?.parentActivity ?: return
            val sheet = ForkEditHistoryBottomSheet(
                activity,
                currentAccount,
                dialogId,
                messageId,
                currentText,
                fragment.resourceProvider
            )
            sheet.show()
        }
    }
}
