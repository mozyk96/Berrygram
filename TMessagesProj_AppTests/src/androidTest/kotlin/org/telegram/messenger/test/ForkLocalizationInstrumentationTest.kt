package org.telegram.messenger.test

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ForkLocale

/**
 * Runtime test of the Berrygram localization.
 *
 * The static checks of the resource files reported a missing runtime resource
 * as a successful check, the application showed "LOC_ERR:null" on the device.
 * This test resolves the resources of the running application, so a resource
 * that is not packaged can no longer pass.
 */
@RunWith(AndroidJUnit4::class)
class ForkLocalizationInstrumentationTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** The fork strings that are shown in the settings of the application. */
    private val visibleForkStrings = listOf(
        "ForkSectionAppearance",
        "ForkSectionEffects",
        "ForkSectionMessages",
        "ForkSectionMedia",
        "ForkSectionGhostMode",
        "ForkSectionPrivacy",
        "ForkSectionArchive",
        "ForkSectionDiagnostics",
        "ForkSectionTranslation",
        "ForkSectionPerformance",
        "ForkSectionUpdates",
        "ForkGhostModeMaster",
        "ForkStealthPrivate",
        "ForkStealthGroups",
        "ForkDeletedMessagesKeep",
        "ForkEditHistorySave",
        "ForkEditHistoryShowInMenu",
        "ForkArchiveEnableToggle",
        "ForkArchiveRetention",
        "ForkArchiveRetention7",
        "ForkArchiveRetention30",
        "ForkArchiveRetention90",
        "ForkArchiveRetentionForever",
        "ForkGraphicsStyle",
        "ForkGraphicsGlass",
        "ForkGraphicsBlur",
        "ForkGraphicsNone",
        "ForkAppearanceRadius",
        "ForkAppearanceRadiusValue",
        "ForkAdvancedAnimations",
        "ForkReactionTitle",
        "ForkMenuEditorTitle",
        "ForkCustomTextTitle",
        "ForkGestureTitle",
        "ForkVideoTitle",
        "ForkVoiceTitle",
        "ForkUnifiedDiagnosticsTitle",
        "ForkArchiveOpenTitle",
        "ForkArchiveCreate",
        "ForkArchiveUpdate",
        "ForkTranslationAutoTranslate",
        "ForkTranslationTargetLanguage",
        "ForkTranslationAutomatic",
        "ForkPerformanceConfigMode",
        "ForkPerformanceAutoMode",
        "ForkPerformanceManualMode",
        "ForkPerformanceManualLevel",
        "ForkPerformanceLevelLow",
        "ForkPerformanceLevelMedium",
        "ForkPerformanceLevelHigh",
        "ForkUnifiedUpdateTitle",
        "ForkUnifiedUpdateInfo"
    )

    @Test
    fun everyVisibleForkStringIsPackaged() {
        val resources = context.resources
        val packageName = context.packageName
        for (name in visibleForkStrings) {
            val id = resources.getIdentifier(name, "string", packageName)
            assertTrue("The resource $name is not packaged", id != 0)
            val value = resources.getString(id)
            assertTrue("The resource $name is empty", value.isNotEmpty())
        }
    }

    @Test
    fun forkStringsNeverResolveToLocErr() {
        // The resolver of Telegram only knows the upstream language packs, the
        // fork strings have to be read from the packaged resources.
        val value = LocaleController.getString(R.string.ForkPerformanceOn)
        assertFalse("The resolver reports LOC_ERR: $value", value.startsWith("LOC_ERR"))
        assertNotEquals("LOC_ERR:null", value)

        val manual = LocaleController.getString(R.string.ForkPerformanceManualMode)
        assertFalse("The resolver reports LOC_ERR: $manual", manual.startsWith("LOC_ERR"))

        val auto = LocaleController.getString(R.string.ForkPerformanceAutoMode)
        assertFalse("The resolver reports LOC_ERR: $auto", auto.startsWith("LOC_ERR"))
    }

    @Test
    fun forkLocaleReadsThePackagedResource() {
        val value = ForkLocale.get(R.string.ForkPerformanceOn)
        assertTrue("The fork locale returned an empty label", value.isNotEmpty())
        assertFalse("The fork locale reported LOC_ERR: $value", value.startsWith("LOC_ERR"))
    }
}
