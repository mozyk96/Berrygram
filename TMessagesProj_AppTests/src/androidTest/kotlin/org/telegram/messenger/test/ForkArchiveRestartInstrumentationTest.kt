package org.telegram.messenger.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.fork.storage.ForkArchiveManager

/** Run once with phase=write and again in a fresh app process with phase=read. */
@RunWith(AndroidJUnit4::class)
class ForkArchiveRestartInstrumentationTest {
    @Test fun archivedTextSurvivesProcessRestart() {
        val manager = ForkArchiveManager.getInstance(987655)
        val dialog = 98765501L
        when (InstrumentationRegistry.getArguments().getString("phase") ?: "write") {
            "write" -> assertTrue(manager.archiveMessage(dialog, 1, 1, 1, null, "restart test", false))
            "read" -> assertEquals("restart test", manager.getArchivedMessages(dialog).single { it.messageId == 1 }.messageText)
            else -> throw AssertionError("Supply phase=write or phase=read")
        }
    }
}
