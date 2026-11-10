package org.telegram.ui

import android.view.View
import org.telegram.messenger.R
import org.telegram.messenger.fork.panels.ForkAnimationSettings
import org.telegram.messenger.fork.panels.ForkDockSettings
import org.telegram.messenger.fork.panels.ForkPanelSettings
import org.telegram.messenger.fork.panels.PanelPolicy
import org.telegram.messenger.fork.performance.PerformancePolicy
import org.telegram.messenger.fork.performance.PerformanceSettings
import org.telegram.messenger.fork.stealth.ForkStealthSettings
import org.telegram.messenger.fork.text.ForkTranslationSettings
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.fork.settings.ForkSettingsSection

/**
 * Builds the Berrygram blocks that live inside the Telegram settings screens.
 *
 * The whole application uses a single settings system. A Berrygram feature is
 * therefore not a separate menu any more, it is a block inside the Telegram
 * screen that owns the feature. Every block is created here, so all screens
 * use the same rows, the same wording and the same behaviour.
 */
object ForkSettingsBlocks {

    /** Ghost Mode, message preservation, edit history and archive privacy. */
    @JvmStatic
    fun privacy(fragment: BaseFragment, account: Int): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)

        section.addHeader(R.string.ForkSectionGhostMode)
        val ghost = section.addToggle(R.string.ForkGhostModeMaster, ForkStealthSettings.ghostModeEnabled(account)) { enabled ->
            ForkStealthSettings.setGhostModeEnabled(account, enabled)
        }
        val privateRead = section.addToggle(R.string.ForkStealthPrivate, ForkStealthSettings.stealthReadPrivate(account)) { enabled ->
            ForkStealthSettings.setStealthReadPrivate(account, enabled)
        }
        val groupRead = section.addToggle(R.string.ForkStealthGroups, ForkStealthSettings.stealthReadGroups(account)) { enabled ->
            ForkStealthSettings.setStealthReadGroups(account, enabled)
        }
        section.addInfo(R.string.ForkGhostModeInfo)

        section.addHeader(R.string.ForkSectionPrivacy)
        section.addToggle(R.string.ForkDeletedMessagesKeep, ForkStealthSettings.deletedMessagesEnabled(account)) { enabled ->
            ForkStealthSettings.setDeletedMessagesEnabled(account, enabled)
        }
        val editHistory = section.addToggle(R.string.ForkEditHistorySave, ForkStealthSettings.editHistoryEnabled(account)) { enabled ->
            ForkStealthSettings.setEditHistoryEnabled(account, enabled)
        }
        val showEditInMenu = section.addToggle(R.string.ForkEditHistoryShowInMenu, ForkStealthSettings.showEditInMenu(account)) { enabled ->
            ForkStealthSettings.setShowEditInMenu(account, enabled)
        }
        section.addInfo(R.string.ForkDeletedMessagesInfo)

        section.addHeader(R.string.ForkSectionArchive)
        section.addToggle(R.string.ForkArchiveEnableToggle, ForkStealthSettings.archiveEnabled(account)) { enabled ->
            ForkStealthSettings.setArchiveEnabled(account, enabled)
        }
        lateinit var retention: org.telegram.ui.Cells.TextSettingsCell
        retention = section.addRow(R.string.ForkArchiveRetention, ForkLocale.get(retentionLabel(account))) { anchor ->
            showRetention(fragment, account, anchor, retention)
        }
        section.addInfo(R.string.ForkArchiveRetentionInfo)

        // The sub rows of Ghost Mode and the edit history depend on their parent
        // switch, the state is applied again when the screen is shown.
        section.refreshState {
            val master = ForkStealthSettings.ghostModeEnabled(account)
            ghost.isChecked = master
            privateRead.isChecked = ForkStealthSettings.stealthReadPrivate(account)
            groupRead.isChecked = ForkStealthSettings.stealthReadGroups(account)
            privateRead.isEnabled = master
            groupRead.isEnabled = master
            privateRead.alpha = if (master) 1f else 0.5f
            groupRead.alpha = if (master) 1f else 0.5f
            editHistory.isChecked = ForkStealthSettings.editHistoryEnabled(account)
            showEditInMenu.isChecked = ForkStealthSettings.showEditInMenu(account)
            showEditInMenu.visibility = if (ForkStealthSettings.editHistoryEnabled(account)) View.VISIBLE else View.GONE
            retention.setTextAndValue(ForkLocale.get(R.string.ForkArchiveRetention), ForkLocale.get(retentionLabel(account)), false)
        }
        return section
    }

    /** Graphics controls beside Telegram's theme and bubble settings. */
    @JvmStatic
    fun graphics(fragment: BaseFragment): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)

        section.addHeader(R.string.ForkSectionEffects)
        lateinit var style: org.telegram.ui.Cells.TextSettingsCell
        style = section.addRow(R.string.ForkGraphicsStyle, styleLabel()) { anchor ->
            showStyle(fragment, anchor, style)
        }
        lateinit var radius: org.telegram.ui.Cells.TextSettingsCell
        radius = section.addRow(R.string.ForkAppearanceRadius, ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp())) { anchor ->
            showRadius(fragment, anchor, radius)
        }
        val animations = section.addToggle(R.string.ForkAdvancedAnimations, ForkAnimationSettings.fluidPopupsEnabled()) { enabled ->
            ForkAnimationSettings.setFluidPopupsEnabled(enabled)
        }
        section.addInfo(R.string.ForkGraphicsInfoUnified)

        section.refreshState {
            style.setTextAndValue(ForkLocale.get(R.string.ForkGraphicsStyle), styleLabel(), false)
            radius.setTextAndValue(
                ForkLocale.get(R.string.ForkAppearanceRadius),
                ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()),
                false
            )
            animations.isChecked = ForkAnimationSettings.fluidPopupsEnabled()
        }
        return section
    }

    /** Reactions, message menu, gestures and media controls. */
    @JvmStatic
    fun chatSettings(fragment: BaseFragment): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)

        section.addHeader(R.string.ForkSectionMessages)
        section.addDetailRow(R.string.ForkReactionTitle, R.string.ForkUnifiedReactionsSubtitle, R.drawable.msg_reactions) {
            fragment.presentFragment(ForkReactionSettingsActivity())
        }
        section.addDetailRow(R.string.ForkMenuEditorTitle, R.string.ForkUnifiedMenuSubtitle, R.drawable.msg_settings) {
            fragment.presentFragment(ForkMessageMenuSettingsActivity())
        }
        section.addDetailRow(R.string.ForkCustomTextTitle, R.string.ForkTextSubtitle, R.drawable.msg_edit) {
            fragment.presentFragment(ForkCustomTextActivity())
        }

        section.addHeader(R.string.ForkSectionMedia)
        section.addDetailRow(R.string.ForkGestureTitle, R.string.ForkGestureDoubleTap, R.drawable.menu_reply) {
            fragment.presentFragment(ForkGestureSettingsActivity())
        }
        section.addDetailRow(R.string.ForkVideoTitle, R.string.ForkVideoSubtitle, R.drawable.msg_video) {
            fragment.presentFragment(ForkVideoSettingsActivity())
        }
        section.addDetailRow(R.string.ForkVoiceTitle, R.string.ForkVoiceSubtitle, R.drawable.msg_filled_data_voice) {
            fragment.presentFragment(ForkVoiceSettingsActivity())
        }

        return section
    }

    /** Notification diagnostics inside the Telegram notification settings. */
    @JvmStatic
    fun notifications(fragment: BaseFragment): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)
        section.addHeader(R.string.ForkSectionDiagnostics)
        section.addDetailRow(R.string.ForkUnifiedDiagnosticsTitle, R.string.ForkUnifiedDiagnosticsInfo, R.drawable.settings_sounds) {
            fragment.presentFragment(ForkNotificationDiagnosticsActivity())
        }
        return section
    }

    /** Archive browser, import and cleanup inside the data settings. */
    @JvmStatic
    fun data(fragment: BaseFragment, account: Int): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)
        section.addHeader(R.string.ForkSectionArchive)
        section.addDetailRow(R.string.ForkArchiveOpenTitle, R.string.ForkArchiveOpenSubtitle, R.drawable.msg_archive) {
            fragment.presentFragment(ForkArchiveActivity())
        }
        section.addRow(R.string.ForkArchiveStorage, archiveSizeLabel(account))
        section.addInfo(R.string.ForkArchiveInfo)
        return section
    }

    /** The size of the local archive of the account. */
    private fun archiveSizeLabel(account: Int): String {
        val bytes = org.telegram.messenger.fork.storage.ForkArchiveManager.archiveSizeBytes(account)
        if (bytes <= 0L) {
            return ForkLocale.get(R.string.ForkArchiveEmpty)
        }
        val megabytes = bytes / (1024.0 * 1024.0)
        return ForkLocale.get(
            R.string.ForkArchiveStorageValue,
            String.format(java.util.Locale.US, "%.1f MB", megabytes)
        )
    }

    /** Translation options inside the language settings. */
    @JvmStatic
    fun language(fragment: BaseFragment, account: Int): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)
        val settings = ForkTranslationSettings(account)
        section.addHeader(R.string.ForkSectionTranslation)
        val auto = section.addToggle(R.string.ForkTranslationAutoTranslate, settings.autoTranslateEnabled) { enabled ->
            settings.autoTranslateEnabled = enabled
            settings.save()
        }
        lateinit var target: org.telegram.ui.Cells.TextSettingsCell
        target = section.addRow(R.string.ForkTranslationTargetLanguage, ForkLocale.get(R.string.ForkTranslationAutomatic)) { anchor ->
            showTranslationTarget(fragment, settings, anchor, target)
        }
        section.addInfo(R.string.ForkTranslationInfo)
        section.refreshState {
            auto.isChecked = settings.autoTranslateEnabled
            target.setTextAndValue(
                ForkLocale.get(R.string.ForkTranslationTargetLanguage),
                currentTranslationLabel(settings),
                false
            )
        }
        return section
    }

    /** Device analyzer and performance profile inside the power saving screen. */
    @JvmStatic
    fun performance(fragment: BaseFragment): ForkSettingsSection {
        val section = ForkSettingsSection(fragment.context)
        section.addHeader(R.string.ForkSectionPerformance)
        lateinit var mode: org.telegram.ui.Cells.TextSettingsCell
        mode = section.addRow(R.string.ForkPerformanceConfigMode, configModeLabel()) { anchor ->
            showConfigMode(fragment, anchor, mode)
        }
        lateinit var level: org.telegram.ui.Cells.TextSettingsCell
        level = section.addRow(R.string.ForkPerformanceManualLevel, levelLabel()) { anchor ->
            showManualLevel(fragment, anchor, level)
        }
        val miniDock = section.addToggle(R.string.ForkMiniDock, ForkDockSettings.miniDockEnabled()) { enabled ->
            ForkDockSettings.setMiniDockEnabled(enabled)
        }
        section.addInfo(R.string.ForkPerformanceInfoUnified)
        section.refreshState {
            mode.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceConfigMode), configModeLabel(), false)
            level.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceManualLevel), levelLabel(), false)
            level.visibility = if (PerformancePolicy.CONFIG_MANUAL.equals(PerformanceSettings.configMode())) View.VISIBLE else View.GONE
            miniDock.isChecked = ForkDockSettings.miniDockEnabled()
        }
        return section
    }

    /* ----- helpers ----- */

    private fun styleLabel(): String {
        return when (ForkPanelSettings.style()) {
            PanelPolicy.STYLE_BLUR -> ForkLocale.get(R.string.ForkGraphicsBlur)
            PanelPolicy.STYLE_NONE -> ForkLocale.get(R.string.ForkGraphicsNone)
            else -> ForkLocale.get(R.string.ForkGraphicsGlass)
        }
    }

    private fun showStyle(fragment: BaseFragment, anchor: View, cell: org.telegram.ui.Cells.TextSettingsCell) {
        val modes = arrayOf(PanelPolicy.STYLE_GLASS, PanelPolicy.STYLE_BLUR, PanelPolicy.STYLE_NONE)
        val labels = arrayOf(
            ForkLocale.get(R.string.ForkGraphicsGlass),
            ForkLocale.get(R.string.ForkGraphicsBlur),
            ForkLocale.get(R.string.ForkGraphicsNone)
        )
        val current = modes.indexOf(ForkPanelSettings.style()).let { if (it < 0) 0 else it }
        ForkChoicePopup.show(fragment, anchor, labels, null, current) { index ->
            val chosen = modes[index]
            ForkPanelSettings.setStyle(chosen)
            if (PanelPolicy.STYLE_GLASS.equals(chosen)) {
                org.telegram.messenger.LiteMode.setAllFlags(
                    org.telegram.messenger.LiteMode.getValue(true) or org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS
                )
            } else {
                org.telegram.messenger.LiteMode.toggleFlag(org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS, false)
            }
            cell.setTextAndValue(ForkLocale.get(R.string.ForkGraphicsStyle), styleLabel(), false)
        }
    }

    private fun showRadius(fragment: BaseFragment, anchor: View, cell: org.telegram.ui.Cells.TextSettingsCell) {
        val values = IntArray(21)
        val labels = Array(values.size) { index -> "${index + 12}" }
        val current = (ForkPanelSettings.radiusDp() - 12).coerceIn(0, values.size - 1)
        ForkChoicePopup.show(fragment, anchor, labels, null, current) { index ->
            ForkPanelSettings.setRadiusDp(index + 12)
            cell.setTextAndValue(
                ForkLocale.get(R.string.ForkAppearanceRadius),
                ForkLocale.get(R.string.ForkAppearanceRadiusValue, ForkPanelSettings.radiusDp()),
                false
            )
        }
    }

    private fun retentionLabel(account: Int): Int {
        return when (ForkStealthSettings.retentionDays(account)) {
            7 -> R.string.ForkArchiveRetention7
            30 -> R.string.ForkArchiveRetention30
            90 -> R.string.ForkArchiveRetention90
            else -> R.string.ForkArchiveRetentionForever
        }
    }

    private fun showRetention(fragment: BaseFragment, account: Int, anchor: View, cell: org.telegram.ui.Cells.TextSettingsCell) {
        val options = ForkStealthSettings.RETENTION_OPTIONS
        val labels = arrayOf(
            ForkLocale.get(R.string.ForkArchiveRetention7),
            ForkLocale.get(R.string.ForkArchiveRetention30),
            ForkLocale.get(R.string.ForkArchiveRetention90),
            ForkLocale.get(R.string.ForkArchiveRetentionForever)
        )
        val current = options.indexOf(ForkStealthSettings.retentionDays(account)).let { if (it < 0) options.size - 1 else it }
        ForkChoicePopup.show(fragment, anchor, labels, null, current) { index ->
            val days = options.getOrNull(index) ?: return@show
            ForkStealthSettings.setRetentionDays(account, days)
            cell.setTextAndValue(ForkLocale.get(R.string.ForkArchiveRetention), ForkLocale.get(retentionLabel(account)), false)
            org.telegram.messenger.Utilities.globalQueue.postRunnable {
                org.telegram.messenger.fork.storage.ForkStorageMaintenance.cleanupAccount(account, true)
            }
        }
    }

    private fun currentTranslationLabel(settings: ForkTranslationSettings): String {
        val code = settings.targetLanguage
        if (code.isEmpty()) {
            return ForkLocale.get(R.string.ForkTranslationAutomatic)
        }
        val match = settings.supportedLanguages().firstOrNull { it.first == code }
        return match?.second?.takeIf { it.isNotEmpty() } ?: code
    }

    private fun showTranslationTarget(fragment: BaseFragment, settings: ForkTranslationSettings, anchor: View, cell: org.telegram.ui.Cells.TextSettingsCell) {
        val languages = settings.supportedLanguages()
        if (languages.isEmpty()) {
            return
        }
        val labels = Array(languages.size) { index -> languages[index].second }
        val current = languages.indexOfFirst { it.first == settings.targetLanguage }
        ForkChoicePopup.show(fragment, anchor, labels, null, if (current < 0) 0 else current) { index ->
            val language = languages.getOrNull(index) ?: return@show
            settings.targetLanguage = language.first
            settings.save()
            cell.setTextAndValue(
                ForkLocale.get(R.string.ForkTranslationTargetLanguage),
                currentTranslationLabel(settings),
                false
            )
        }
    }

    private fun configModeLabel(): String {
        return if (PerformancePolicy.CONFIG_MANUAL.equals(PerformanceSettings.configMode())) {
            ForkLocale.get(R.string.ForkPerformanceManualMode)
        } else {
            ForkLocale.get(R.string.ForkPerformanceAutoMode)
        }
    }

    private fun levelLabel(): String {
        return when (PerformanceSettings.manualLevel()) {
            PerformancePolicy.LEVEL_LOW -> ForkLocale.get(R.string.ForkPerformanceLevelLow)
            PerformancePolicy.LEVEL_HIGH -> ForkLocale.get(R.string.ForkPerformanceLevelHigh)
            else -> ForkLocale.get(R.string.ForkPerformanceLevelMedium)
        }
    }

    private fun showConfigMode(fragment: BaseFragment, anchor: View, cell: org.telegram.ui.Cells.TextSettingsCell) {
        val modes = arrayOf(PerformancePolicy.CONFIG_AUTO, PerformancePolicy.CONFIG_MANUAL)
        val labels = arrayOf(
            ForkLocale.get(R.string.ForkPerformanceAutoMode),
            ForkLocale.get(R.string.ForkPerformanceManualMode)
        )
        val current = modes.indexOf(PerformancePolicy.normalizeConfigMode(PerformanceSettings.configMode())).let { if (it < 0) 0 else it }
        ForkChoicePopup.show(fragment, anchor, labels, null, current) { index ->
            PerformanceSettings.setConfigMode(modes[index])
            cell.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceConfigMode), configModeLabel(), false)
        }
    }

    private fun showManualLevel(fragment: BaseFragment, anchor: View, cell: org.telegram.ui.Cells.TextSettingsCell) {
        val levels = arrayOf(PerformancePolicy.LEVEL_LOW, PerformancePolicy.LEVEL_MEDIUM, PerformancePolicy.LEVEL_HIGH)
        val labels = arrayOf(
            ForkLocale.get(R.string.ForkPerformanceLevelLow),
            ForkLocale.get(R.string.ForkPerformanceLevelMedium),
            ForkLocale.get(R.string.ForkPerformanceLevelHigh)
        )
        val current = levels.indexOf(PerformancePolicy.normalizeManualLevel(PerformanceSettings.manualLevel())).let { if (it < 0) 1 else it }
        ForkChoicePopup.show(fragment, anchor, labels, null, current) { index ->
            PerformanceSettings.setManualLevel(levels[index])
            cell.setTextAndValue(ForkLocale.get(R.string.ForkPerformanceManualLevel), levelLabel(), false)
        }
    }
}
