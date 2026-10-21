package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.uidlog.UidLogItem
import de.syss.MifareClassicTool.ui.uidlog.UidLogScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UidLogComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testUidLogEmptyState() {
        composeTestRule.setContent {
            MctTheme {
                UidLogScreen(
                    logEntries = emptyList(),
                    onBackClick = {},
                    onShareClick = {},
                    onClearClick = {},
                    onCopyUid = {},
                    onCopyAll = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_logs_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_logs").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_clear_logs").assertDoesNotExist()
    }

    @Test
    fun testUidLogWithEntriesAndClearDialog() {
        var copiedUid = ""
        var clearConfirmed = false

        val sampleLogs = listOf(
            UidLogItem(
                timestamp = "2026-10-11_21-30-00",
                uid = "0BDC504F",
                rawLine = "2026-10-11_21-30-00: 0BDC504F"
            ),
            UidLogItem(
                timestamp = "2026-10-11_21-00-00",
                uid = "A1B2C3D4",
                rawLine = "2026-10-11_21-00-00: A1B2C3D4"
            )
        )

        composeTestRule.setContent {
            MctTheme {
                UidLogScreen(
                    logEntries = sampleLogs,
                    onBackClick = {},
                    onShareClick = {},
                    onClearClick = { clearConfirmed = true },
                    onCopyUid = { uid -> copiedUid = uid },
                    onCopyAll = {}
                )
            }
        }

        // List and items
        composeTestRule.onNodeWithTag("list_uid_logs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_log_count").assertTextContains("2", substring = true)
        composeTestRule.onNodeWithTag("log_item_0BDC504F").assertIsDisplayed()

        // Copy item click
        composeTestRule.onNodeWithTag("log_item_0BDC504F").performClick()
        assertEquals("0BDC504F", copiedUid)

        // Clear logs dialog interaction
        composeTestRule.onNodeWithTag("btn_clear_logs").performClick()
        composeTestRule.onNodeWithTag("btn_confirm_clear").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_confirm_clear").performClick()
        assertTrue(clearConfirmed)
    }
}
