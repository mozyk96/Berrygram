package org.telegram.messenger.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextSettingsCell
import androidx.test.platform.app.InstrumentationRegistry
import android.view.View
import android.widget.TextView
import org.telegram.messenger.R
import org.telegram.ui.fork.settings.ForkSettingsSection

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
    fun sectionHeadersStartNewDividerGroups() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val section = ForkSettingsSection(context)
            section.addHeader(R.string.ForkSectionEffects)
            section.addToggle(R.string.ForkAdvancedAnimations, false) { }
            section.addInfo(R.string.ForkGraphicsInfoUnified)
            section.addHeader(R.string.ForkSectionMessages)
            section.addToggle(R.string.ForkAdvancedAnimations, false) { }
            assertTrue("Unexpected divider between heading and first row", section.childCount == 5)
        }
    }

    @Test
    fun destinationSubtitleHasRoomBelowTitle() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val section = ForkSettingsSection(context)
            val row = section.addDetailRow(R.string.ForkUnifiedDiagnosticsTitle, R.string.ForkUnifiedDiagnosticsInfo, R.drawable.settings_sounds) { }
            val width = (240 * context.resources.displayMetrics.density).toInt()
            row.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            row.layout(0, 0, width, row.measuredHeight)
            assertTrue("Description overlaps title", row.valueTextView.top >= row.textView.bottom)
            assertTrue("Description is clipped", row.valueTextView.bottom <= row.measuredHeight)
            assertTrue("Description is empty", row.valueTextView.lineCount > 0)
        }
    }

    @Test
    fun longSettingTitleWrapsBesideItsValue() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val cell = TextSettingsCell(context)
            cell.setTextAndValue("A long setting title that must remain readable on a narrow screen", "Value", false)
            cell.setMultilineTitle()
            val width = (240 * context.resources.displayMetrics.density).toInt()
            cell.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            cell.layout(0, 0, width, cell.measuredHeight)
            assertTrue("Long title did not wrap", cell.textView.lineCount > 1)
            assertTrue("Title is clipped", cell.textView.bottom <= cell.measuredHeight)
            assertTrue("Title overlaps value", cell.textView.right <= cell.valueTextView.left)
        }
    }

    @Test
    fun longSwitchTitleWrapsOnNarrowScreen() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val cell = TextCheckCell(context)
            cell.setTextAndCheck("A long setting title that must remain readable on a narrow screen", true, false)
            cell.setMultilineTitle()
            val width = (240 * context.resources.displayMetrics.density).toInt()
            cell.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            cell.layout(0, 0, width, cell.measuredHeight)
            val title = cell.getChildAt(0) as TextView
            assertTrue("Long title did not wrap", title.lineCount > 1)
            assertTrue("Title is clipped", title.bottom <= cell.measuredHeight)
        }
    }

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
