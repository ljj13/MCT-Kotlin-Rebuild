package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.Activities.MainMenu
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainMenuComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainMenu>()

    @Test
    fun testAllMainMenuEntriesDisplayed() {
        // Core entries
        composeTestRule.onNodeWithTag("nfc_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_read_tag").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_write_tag").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_edit_dump").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_edit_keys").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_tools").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_help").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_preferences").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_about").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_version_footer").assertIsDisplayed()
    }

    @Test
    fun testToolsDialogOpensAndCloses() {
        // Open tools dialog
        composeTestRule.onNodeWithTag("btn_tools").performClick()

        // Verify sub-tools are listed
        composeTestRule.onNodeWithTag("tool_tag_info").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tool_bcc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tool_ac").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tool_vb").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_close_tools").assertIsDisplayed()

        // Close dialog
        composeTestRule.onNodeWithTag("btn_close_tools").performClick()
        composeTestRule.onNodeWithTag("btn_tools").assertIsDisplayed()
    }
}
