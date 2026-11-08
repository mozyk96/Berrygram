package org.telegram.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Cells.TextSettingsCell

/**
 * Groups the remaining Berrygram customisation screens: camera and video, voice,
 * gestures, reactions, message menu and custom text. The screen exists so the
 * unified settings never have to grow a long list of single purpose rows.
 */
class ForkMediaSettingsActivity : BaseFragment() {

    override fun createView(context: Context): View {
        actionBar.setTitle(ForkLocale.get(R.string.ForkUnifiedMediaTitle))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })

        val page = ForkSettingsLayout.page(context)
        addRow(page, context, R.string.ForkVideoTitle, R.string.ForkVideoSubtitle) { presentFragment(ForkVideoSettingsActivity()) }
        addRow(page, context, R.string.ForkVoiceTitle, R.string.ForkVoiceSubtitle) { presentFragment(ForkVoiceSettingsActivity()) }
        addRow(page, context, R.string.ForkGestureTitle, R.string.ForkGestureDoubleTap) { presentFragment(ForkGestureSettingsActivity()) }
        addRow(page, context, R.string.ForkReactionTitle, R.string.ForkUnifiedReactionsSubtitle) { presentFragment(ForkReactionSettingsActivity()) }
        addRow(page, context, R.string.ForkMenuEditorTitle, R.string.ForkUnifiedMenuSubtitle) { presentFragment(ForkMessageMenuSettingsActivity()) }
        addRow(page, context, R.string.ForkCustomTextTitle, R.string.ForkTextSubtitle) { presentFragment(ForkCustomTextActivity()) }

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page)
        }
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkUnifiedMediaTitle)) {
            finishFragment()
        }
        return fragmentView
    }

    private fun addRow(
        page: LinearLayout,
        context: Context,
        titleRes: Int,
        subtitleRes: Int,
        action: () -> Unit
    ) {
        val row = TextSettingsCell(context)
        row.setTextAndValue(ForkLocale.get(titleRes), ForkLocale.get(subtitleRes), false)
        ForkSettingsLayout.interactive(row)
        row.setOnClickListener { action() }
        ForkSettingsLayout.addCard(page, row)
    }
}
