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
class ForkStealthSettingsActivity : BaseFragment() {

    private var ghostModeCell: TextCheckCell? = null
    private var stealthPrivateCell: TextCheckCell? = null
    private var stealthGroupsCell: TextCheckCell? = null
    private var deletedMessagesCell: TextCheckCell? = null
    private var editHistoryCell: TextCheckCell? = null
    private var showEditInMenuCell: TextCheckCell? = null
    private var archiveEnabledCell: TextCheckCell? = null
    private var openArchiveCell: TextSettingsCell? = null

    override fun createView(context: Context): View {
        actionBar.setTitle(LocaleController.getString(R.string.ForkStealthTitle))
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
                LocaleController.getString(R.string.ForkGhostModeMaster),
                ForkStealthSettings.ghostModeEnabled(),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.ghostModeEnabled()
                ForkStealthSettings.setGhostModeEnabled(enabled)
                isChecked = enabled
                updateChildToggles()
            }
        }
        ghostSection.addView(ghostModeCell)

        stealthPrivateCell = TextCheckCell(context).apply {
            setTextAndCheck(
                LocaleController.getString(R.string.ForkStealthPrivate),
                ForkStealthSettings.stealthReadPrivate(),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.stealthReadPrivate()
                ForkStealthSettings.setStealthReadPrivate(enabled)
                isChecked = enabled
            }
        }
        ghostSection.addView(stealthPrivateCell)

        stealthGroupsCell = TextCheckCell(context).apply {
            setTextAndCheck(
                LocaleController.getString(R.string.ForkStealthGroups),
                ForkStealthSettings.stealthReadGroups(),
                false
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.stealthReadGroups()
                ForkStealthSettings.setStealthReadGroups(enabled)
                isChecked = enabled
            }
        }
        ghostSection.addView(stealthGroupsCell)

        ForkSettingsLayout.addSection(page, ghostSection)
        val ghostInfo = TextInfoPrivacyCell(context).apply {
            text = LocaleController.getString(R.string.ForkGhostModeInfo)
        }
        page.addView(ghostInfo)

        // --- Message Preservation & Edits ---
        val preserveSection = ForkSettingsLayout.section(context)
        deletedMessagesCell = TextCheckCell(context).apply {
            setTextAndCheck(
                LocaleController.getString(R.string.ForkDeletedMessagesKeep),
                ForkStealthSettings.deletedMessagesEnabled(),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.deletedMessagesEnabled()
                ForkStealthSettings.setDeletedMessagesEnabled(enabled)
                isChecked = enabled
            }
        }
        preserveSection.addView(deletedMessagesCell)

        editHistoryCell = TextCheckCell(context).apply {
            setTextAndCheck(
                LocaleController.getString(R.string.ForkEditHistorySave),
                ForkStealthSettings.editHistoryEnabled(),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.editHistoryEnabled()
                ForkStealthSettings.setEditHistoryEnabled(enabled)
                isChecked = enabled
                showEditInMenuCell?.visibility = if (enabled) View.VISIBLE else View.GONE
            }
        }
        preserveSection.addView(editHistoryCell)

        showEditInMenuCell = TextCheckCell(context).apply {
            setTextAndCheck(
                LocaleController.getString(R.string.ForkEditHistoryShowInMenu),
                ForkStealthSettings.showEditInMenu(),
                false
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.showEditInMenu()
                ForkStealthSettings.setShowEditInMenu(enabled)
                isChecked = enabled
            }
        }
        preserveSection.addView(showEditInMenuCell)

        ForkSettingsLayout.addSection(page, preserveSection)
        val preserveInfo = TextInfoPrivacyCell(context).apply {
            text = LocaleController.getString(R.string.ForkDeletedMessagesInfo)
        }
        page.addView(preserveInfo)

        // --- Berry Archive Section ---
        val archiveSection = ForkSettingsLayout.section(context)
        archiveEnabledCell = TextCheckCell(context).apply {
            setTextAndCheck(
                LocaleController.getString(R.string.ForkArchiveEnableToggle),
                ForkStealthSettings.archiveEnabled(),
                true
            )
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                val enabled = !ForkStealthSettings.archiveEnabled()
                ForkStealthSettings.setArchiveEnabled(enabled)
                isChecked = enabled
            }
        }
        archiveSection.addView(archiveEnabledCell)

        openArchiveCell = TextSettingsCell(context).apply {
            setTextAndValue(
                LocaleController.getString(R.string.ForkArchiveOpenTitle),
                LocaleController.getString(R.string.ForkArchiveOpenSubtitle),
                false
            )
            setIcon(R.drawable.msg_archive)
            ForkSettingsLayout.interactive(this)
            setOnClickListener {
                presentFragment(ForkArchiveActivity())
            }
        }
        archiveSection.addView(openArchiveCell)

        ForkSettingsLayout.addSection(page, archiveSection)
        val archiveInfo = TextInfoPrivacyCell(context).apply {
            text = LocaleController.getString(R.string.ForkArchiveInfo)
        }
        page.addView(archiveInfo)

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page)
        }
        fragmentView = ForkFloatingHeader.wrap(
            context,
            actionBar,
            scroll,
            LocaleController.getString(R.string.ForkStealthTitle)
        ) { finishFragment() }

        updateChildToggles()
        return fragmentView
    }

    private fun updateChildToggles() {
        val ghost = ForkStealthSettings.ghostModeEnabled()
        stealthPrivateCell?.isEnabled = ghost
        stealthGroupsCell?.isEnabled = ghost
        stealthPrivateCell?.alpha = if (ghost) 1.0f else 0.5f
        stealthGroupsCell?.alpha = if (ghost) 1.0f else 0.5f
        showEditInMenuCell?.visibility = if (ForkStealthSettings.editHistoryEnabled()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        ghostModeCell?.let {
            it.isChecked = ForkStealthSettings.ghostModeEnabled()
            stealthPrivateCell?.isChecked = ForkStealthSettings.stealthReadPrivate()
            stealthGroupsCell?.isChecked = ForkStealthSettings.stealthReadGroups()
            deletedMessagesCell?.isChecked = ForkStealthSettings.deletedMessagesEnabled()
            editHistoryCell?.isChecked = ForkStealthSettings.editHistoryEnabled()
            showEditInMenuCell?.isChecked = ForkStealthSettings.showEditInMenu()
            archiveEnabledCell?.isChecked = ForkStealthSettings.archiveEnabled()
            updateChildToggles()
        }
    }
}
