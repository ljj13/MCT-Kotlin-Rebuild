package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.main.MainMenuScreen
import de.syss.MifareClassicTool.ui.main.NfcHardwareState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainMenuComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAllMainMenuEntriesDisplayed() {
        composeTestRule.setContent {
            MctTheme {
                MainMenuScreen(
                    versionCode = "4.3.1",
                    isEditorOnly = false,
                    nfcState = NfcHardwareState.Ready,
                    onReadTagClick = {},
                    onWriteTagClick = {},
                    onOpenTagDumpEditorClick = {},
                    onOpenKeyEditorClick = {},
                    onHelpClick = {},
                    onPreferencesClick = {},
                    onAboutClick = {},
                    onEnableNfcClick = {},
                    onTagInfoClick = {},
                    onCloneUidClick = {},
                    onDiffToolClick = {},
                    onAccessConditionToolClick = {},
                    onValueBlockToolClick = {},
                    onBccToolClick = {},
                    onDataConversionToolClick = {},
                    onUidLogToolClick = {},
                    onImportExportToolClick = {}
                )
            }
        }

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
        composeTestRule.setContent {
            MctTheme {
                MainMenuScreen(
                    versionCode = "4.3.1",
                    isEditorOnly = false,
                    nfcState = NfcHardwareState.Ready,
                    onReadTagClick = {},
                    onWriteTagClick = {},
                    onOpenTagDumpEditorClick = {},
                    onOpenKeyEditorClick = {},
                    onHelpClick = {},
                    onPreferencesClick = {},
                    onAboutClick = {},
                    onEnableNfcClick = {},
                    onTagInfoClick = {},
                    onCloneUidClick = {},
                    onDiffToolClick = {},
                    onAccessConditionToolClick = {},
                    onValueBlockToolClick = {},
                    onBccToolClick = {},
                    onDataConversionToolClick = {},
                    onUidLogToolClick = {},
                    onImportExportToolClick = {}
                )
            }
        }

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
