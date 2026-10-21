package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.keyeditor.KeyEditorScreen
import de.syss.MifareClassicTool.ui.keyeditor.KeyEditorUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyEditorComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testKeyEditorStructureAndContent() {
        val testContent = "# Sample key file\nFFFFFFFFFFFF\nA0B1C2D3E4F5\n"
        val state = KeyEditorUiState(
            fileName = "std.keys",
            hasChanges = false,
            initialContent = testContent,
            lineCount = 4,
            validKeyCount = 2,
            showSaveDialog = false,
            saveFileNameInput = "std.keys",
            showExitConfirmDialog = false
        )

        composeTestRule.setContent {
            MctTheme {
                KeyEditorScreen(
                    state = state,
                    onStateChange = {},
                    editTextRef = {},
                    onContentChanged = {},
                    onBackClick = {},
                    onSaveClick = {},
                    onConfirmSave = {},
                    onShareClick = {},
                    onRemoveDuplicatesClick = {},
                    onExportKeysClick = {},
                    onDiscardAndExit = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_key_save").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_key_share").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_key_more").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_key_editor_stats").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_key_editor_stats").assertTextContains("std.keys")
        composeTestRule.onNodeWithTag("card_key_editor_stats").assertTextContains("2 个密钥")
        composeTestRule.onNodeWithTag("card_key_editor_content").assertIsDisplayed()
    }

    @Test
    fun testKeyEditorDialogsAndMenus() {
        var saveConfirmed = false
        var confirmedName = ""
        var exitDiscarded = false

        val stateWithSaveDialog = KeyEditorUiState(
            fileName = "my_custom.keys",
            hasChanges = true,
            initialContent = "FFFFFFFFFFFF\n",
            lineCount = 2,
            validKeyCount = 1,
            showSaveDialog = true,
            saveFileNameInput = "my_custom.keys",
            showExitConfirmDialog = false
        )

        composeTestRule.setContent {
            MctTheme {
                KeyEditorScreen(
                    state = stateWithSaveDialog,
                    onStateChange = {},
                    editTextRef = {},
                    onContentChanged = {},
                    onBackClick = {},
                    onSaveClick = {},
                    onConfirmSave = {
                        saveConfirmed = true
                        confirmedName = it
                    },
                    onShareClick = {},
                    onRemoveDuplicatesClick = {},
                    onExportKeysClick = {},
                    onDiscardAndExit = { exitDiscarded = true }
                )
            }
        }

        // Save dialog is displayed
        composeTestRule.onNodeWithTag("input_save_file_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_confirm_save").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_cancel_save").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_confirm_save").performClick()
        assertTrue(saveConfirmed)
        org.junit.Assert.assertEquals("my_custom.keys", confirmedName)
    }
}
