package org.telegram.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.messenger.fork.text.ForkTranslationSettings
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextSettingsCell

/**
 * Translation preferences for Berrygram. The settings use Telegram's own
 * translation infrastructure, the screen only exposes the configuration.
 */
class ForkTranslationSettingsActivity : BaseFragment() {

    private var autoTranslateCell: TextCheckCell? = null
    private var targetLanguageCell: TextSettingsCell? = null

    override fun createView(context: Context): View {
        actionBar.setTitle(ForkLocale.get(R.string.ForkUnifiedTranslationTitle))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })

        val page = ForkSettingsLayout.page(context)
        val settings = ForkTranslationSettings(currentAccount)

        autoTranslateCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkTranslationAutoTranslate),
                settings.autoTranslateEnabled,
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !settings.autoTranslateEnabled
                settings.autoTranslateEnabled = enabled
                isChecked = enabled
                settings.save()
                updateTargetLanguageState()
            }
        }
        ForkSettingsLayout.addCard(page, autoTranslateCell)

        targetLanguageCell = TextSettingsCell(context).apply {
            setTextAndValue(
                ForkLocale.get(R.string.ForkTranslationTargetLanguage),
                settings.targetLanguageName(),
                false
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener { showTargetLanguagePicker() }
        }
        ForkSettingsLayout.addCard(page, targetLanguageCell)

        val info = org.telegram.ui.Cells.TextInfoPrivacyCell(context).apply {
            text = ForkLocale.get(R.string.ForkTranslationInfo)
        }
        page.addView(info)

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page)
        }
        fragmentView = ForkFloatingHeader.wrap(context, actionBar, scroll, ForkLocale.get(R.string.ForkUnifiedTranslationTitle)) {
            finishFragment()
        }
        updateTargetLanguageState()
        return fragmentView
    }

    private fun updateTargetLanguageState() {
        val settings = ForkTranslationSettings(currentAccount)
        targetLanguageCell?.isEnabled = settings.autoTranslateEnabled
        targetLanguageCell?.setAlpha(if (settings.autoTranslateEnabled) 1f else 0.5f)
    }

    private fun showTargetLanguagePicker() {
        val settings = ForkTranslationSettings(currentAccount)
        val languages = settings.supportedLanguages()
        if (languages.isEmpty()) {
            return
        }
        val labels = languages.map { it.second }.toTypedArray()
        val selected = languages.indexOfFirst { it.first == settings.targetLanguage }
        ForkChoicePopup.show(this, targetLanguageCell, labels, null, selected) { index ->
            val entry = languages.getOrNull(index) ?: return@show
            settings.targetLanguage = entry.first
            settings.save()
            targetLanguageCell?.setTextAndValue(
                ForkLocale.get(R.string.ForkTranslationTargetLanguage),
                settings.targetLanguageName(),
                false
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateTargetLanguageState()
    }
}
