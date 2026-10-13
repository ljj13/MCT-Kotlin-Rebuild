package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.writetag.WriteTagShell
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pure-Compose test of the P15-B.1 Material 3 shell. No NFC, no native form,
 * no write path is exercised — only the display-only shell is verified.
 */
@RunWith(AndroidJUnit4::class)
class WriteTagShellComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testShellRendersTopBarAndSafetyNotice() {
        var backClicks = 0
        composeTestRule.setContent {
            MctTheme {
                WriteTagShell(onBackClick = { backClicks++ })
            }
        }

        composeTestRule.onNodeWithTag("write_tag_shell").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back")
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, backClicks)
        composeTestRule.onNodeWithTag("write_tag_safety_notice")
            .assertTextContains("Writing may overwrite existing data")
    }
}
