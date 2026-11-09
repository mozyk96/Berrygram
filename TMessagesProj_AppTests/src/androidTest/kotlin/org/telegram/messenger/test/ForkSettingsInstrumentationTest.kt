package org.telegram.messenger.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.ActionBar.BaseFragment

/**
 * Runtime test of the unified settings.
 *
 * Every Berrygram feature was moved into the Telegram screen that owns it. A
 * relocated entry must never point to a screen that does not exist or that is
 * not a fragment, otherwise the screen crashes when it is opened.
 */
@RunWith(AndroidJUnit4::class)
class ForkSettingsInstrumentationTest {

    private val destinations = listOf(
        "org.telegram.ui.ThemeActivity",
        "org.telegram.ui.PrivacySettingsActivity",
        "org.telegram.ui.NotificationsSettingsActivity",
        "org.telegram.ui.DataSettingsActivity",
        "org.telegram.ui.LanguageSelectActivity",
        "org.telegram.ui.LiteModeSettingsActivity",
        "org.telegram.ui.ForkAboutActivity",
        "org.telegram.ui.ForkArchiveActivity",
        "org.telegram.ui.ForkAppearanceSettingsActivity",
        "org.telegram.ui.ForkMediaSettingsActivity",
        "org.telegram.ui.ForkMessageMenuSettingsActivity",
        "org.telegram.ui.ForkNotificationDiagnosticsActivity",
        "org.telegram.ui.ForkPerformanceSettingsActivity",
        "org.telegram.ui.ForkReactionSettingsActivity",
        "org.telegram.ui.ForkStealthSettingsActivity",
        "org.telegram.ui.ForkTranslationSettingsActivity"
    )

    @Test
    fun everySettingsDestinationIsAFragment() {
        for (name in destinations) {
            val type = Class.forName(name)
            assertTrue("$name is not a fragment", BaseFragment::class.java.isAssignableFrom(type))
            assertNotNull("$name has no public constructor", type.getConstructors().firstOrNull())
        }
    }

    @Test
    fun theUnifiedSettingsMapIsConsistent() {
        val mapping = Class.forName("org.telegram.ui.fork.settings.ForkUnifiedSettingsMapping")
        val hosts = mapping.getMethod("hosts").invoke(null) as List<*>
        val entries = mapping.getMethod("entries").invoke(null) as List<*>
        assertTrue("The unified settings map is empty", hosts.isNotEmpty())
        assertTrue("The unified settings map has no features", entries.isNotEmpty())
        val rootRows = mapping.getMethod("rootRows").invoke(null) as Int
        assertTrue("The settings root has more than the Update Center", rootRows == 1)
    }
}
