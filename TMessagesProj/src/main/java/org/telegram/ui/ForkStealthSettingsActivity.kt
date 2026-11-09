package org.telegram.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.fork.stealth.ForkStealthSettings
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Cells.TextSettingsCell

/**
 * Privacy & Stealth Settings for Berrygram 1.4.0.
 * Controls Ghost Mode, invisible reading, deleted messages preservation,
 * edit history revisions, and local chat archive. Written in Kotlin.
 */
class ForkStealthSettingsActivity : ForkSettingsFragment() {

    private var ghostModeCell: TextCheckCell? = null
    private var stealthPrivateCell: TextCheckCell? = null
    private var stealthGroupsCell: TextCheckCell? = null
    private var deletedMessagesCell: TextCheckCell? = null
    private var editHistoryCell: TextCheckCell? = null
    private var showEditInMenuCell: TextCheckCell? = null
    private var archiveEnabledCell: TextCheckCell? = null
    private var openArchiveCell: TextSettingsCell? = null
    private var retentionCell: TextSettingsCell? = null

    override fun createView(context: Context): View {
        actionBar.setTitle(ForkLocale.get(R.string.ForkStealthTitle))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })

        val page = ForkSettingsLayout.page(context)

        // --- Ghost Mode Section ---
        val ghostSection = ForkSettingsLayout.section(context)
        ghostModeCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkGhostModeMaster),
                ForkStealthSettings.ghostModeEnabled(currentAccount),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.ghostModeEnabled(currentAccount)
                ForkStealthSettings.setGhostModeEnabled(currentAccount, enabled)
                isChecked = enabled
                updateChildToggles()
            }
        }
        ghostSection.addView(ghostModeCell)

        stealthPrivateCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkStealthPrivate),
                ForkStealthSettings.stealthReadPrivate(currentAccount),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.stealthReadPrivate(currentAccount)
                ForkStealthSettings.setStealthReadPrivate(currentAccount, enabled)
                isChecked = enabled
            }
        }
        ghostSection.addView(stealthPrivateCell)

        stealthGroupsCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkStealthGroups),
                ForkStealthSettings.stealthReadGroups(currentAccount),
                false
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.stealthReadGroups(currentAccount)
                ForkStealthSettings.setStealthReadGroups(currentAccount, enabled)
                isChecked = enabled
            }
        }
        ghostSection.addView(stealthGroupsCell)

        ForkSettingsLayout.addSection(page, ghostSection)
        val ghostInfo = TextInfoPrivacyCell(context).apply {
            text = ForkLocale.get(R.string.ForkGhostModeInfo)
        }
        page.addView(ghostInfo)

        // --- Message Preservation & Edits ---
        val preserveSection = ForkSettingsLayout.section(context)
        deletedMessagesCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkDeletedMessagesKeep),
                ForkStealthSettings.deletedMessagesEnabled(currentAccount),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.deletedMessagesEnabled(currentAccount)
                ForkStealthSettings.setDeletedMessagesEnabled(currentAccount, enabled)
                isChecked = enabled
            }
        }
        preserveSection.addView(deletedMessagesCell)

        editHistoryCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkEditHistorySave),
                ForkStealthSettings.editHistoryEnabled(currentAccount),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.editHistoryEnabled(currentAccount)
                ForkStealthSettings.setEditHistoryEnabled(currentAccount, enabled)
                isChecked = enabled
                showEditInMenuCell?.visibility = if (enabled) View.VISIBLE else View.GONE
            }
        }
        preserveSection.addView(editHistoryCell)

        showEditInMenuCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkEditHistoryShowInMenu),
                ForkStealthSettings.showEditInMenu(currentAccount),
                false
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.showEditInMenu(currentAccount)
                ForkStealthSettings.setShowEditInMenu(currentAccount, enabled)
                isChecked = enabled
            }
        }
        preserveSection.addView(showEditInMenuCell)

        ForkSettingsLayout.addSection(page, preserveSection)
        val preserveInfo = TextInfoPrivacyCell(context).apply {
            text = ForkLocale.get(R.string.ForkDeletedMessagesInfo)
        }
        page.addView(preserveInfo)

        // --- Berry Archive Section ---
        val archiveSection = ForkSettingsLayout.section(context)
        archiveEnabledCell = TextCheckCell(context).apply {
            setTextAndCheck(
                ForkLocale.get(R.string.ForkArchiveEnableToggle),
                ForkStealthSettings.archiveEnabled(currentAccount),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.archiveEnabled(currentAccount)
                ForkStealthSettings.setArchiveEnabled(currentAccount, enabled)
                isChecked = enabled
            }
        }
        archiveSection.addView(archiveEnabledCell)

        openArchiveCell = TextSettingsCell(context).apply {
            setTextAndValue(
                ForkLocale.get(R.string.ForkArchiveOpenTitle),
                ForkLocale.get(R.string.ForkArchiveOpenSubtitle),
                false
            )
            setIcon(R.drawable.msg_archive)
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                presentFragment(ForkArchiveActivity())
            }
        }
        archiveSection.addView(openArchiveCell)

        retentionCell = TextSettingsCell(context).apply {
            setTextAndValue(
                ForkLocale.get(R.string.ForkArchiveRetention),
                ForkLocale.get(retentionLabel()),
                false
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                showRetentionPicker()
            }
        }
        archiveSection.addView(retentionCell)

        ForkSettingsLayout.addSection(page, archiveSection)
        val archiveInfo = TextInfoPrivacyCell(context).apply {
            text = ForkLocale.get(R.string.ForkArchiveInfo)
        }
        page.addView(archiveInfo)
        val retentionInfo = TextInfoPrivacyCell(context).apply {
            text = ForkLocale.get(R.string.ForkArchiveRetentionInfo)
        }
        page.addView(retentionInfo)

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page)
        }
        fragmentView = ForkFloatingHeader.wrap(
            context,
            actionBar,
            scroll,
            ForkLocale.get(R.string.ForkStealthTitle)
        ) { finishFragment() }

        updateChildToggles()
        return fragmentView
    }

    private fun retentionLabel(): Int {
        return when (ForkStealthSettings.retentionDays(currentAccount)) {
            7 -> R.string.ForkArchiveRetention7
            30 -> R.string.ForkArchiveRetention30
            90 -> R.string.ForkArchiveRetention90
            else -> R.string.ForkArchiveRetentionForever
        }
    }

    private fun showRetentionPicker() {
        val options = ForkStealthSettings.RETENTION_OPTIONS
        val labels = arrayOf(
            ForkLocale.get(R.string.ForkArchiveRetention7),
            ForkLocale.get(R.string.ForkArchiveRetention30),
            ForkLocale.get(R.string.ForkArchiveRetention90),
            ForkLocale.get(R.string.ForkArchiveRetentionForever)
        )
        val selected = options.indexOf(ForkStealthSettings.retentionDays(currentAccount))
        ForkChoicePopup.show(this, retentionCell, labels, null, selected) { index ->
            val days = options.getOrNull(index)
            if (days != null) {
                ForkStealthSettings.setRetentionDays(currentAccount, days)
                retentionCell?.setTextAndValue(
                    ForkLocale.get(R.string.ForkArchiveRetention),
                    ForkLocale.get(retentionLabel()),
                    false
                )
                org.telegram.messenger.Utilities.globalQueue.postRunnable {
                    org.telegram.messenger.fork.storage.ForkStorageMaintenance.cleanupAccount(currentAccount, true)
                }
            }
        }
    }

    private fun updateChildToggles() {        val ghost = ForkStealthSettings.ghostModeEnabled(currentAccount)
        stealthPrivateCell?.isEnabled = ghost
        stealthGroupsCell?.isEnabled = ghost
        stealthPrivateCell?.alpha = if (ghost) 1.0f else 0.5f
        stealthGroupsCell?.alpha = if (ghost) 1.0f else 0.5f
        showEditInMenuCell?.visibility = if (ForkStealthSettings.editHistoryEnabled(currentAccount)) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        ghostModeCell?.let {
            it.isChecked = ForkStealthSettings.ghostModeEnabled(currentAccount)
            stealthPrivateCell?.isChecked = ForkStealthSettings.stealthReadPrivate(currentAccount)
            stealthGroupsCell?.isChecked = ForkStealthSettings.stealthReadGroups(currentAccount)
            deletedMessagesCell?.isChecked = ForkStealthSettings.deletedMessagesEnabled(currentAccount)
            editHistoryCell?.isChecked = ForkStealthSettings.editHistoryEnabled(currentAccount)
            showEditInMenuCell?.isChecked = ForkStealthSettings.showEditInMenu(currentAccount)
            archiveEnabledCell?.isChecked = ForkStealthSettings.archiveEnabled(currentAccount)
            updateChildToggles()
        }
    }
}
