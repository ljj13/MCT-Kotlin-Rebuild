package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.filechooser.FileChooserItem
import de.syss.MifareClassicTool.ui.filechooser.FileChooserScreen
import de.syss.MifareClassicTool.ui.filechooser.FileChooserUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileChooserComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFileChooserEmptyState() {
        val state = FileChooserUiState(
            title = "Choose Key File",
            chooserText = "Please select a key file",
            buttonText = "Select File",
            allowNewFile = true,
            files = emptyList(),
            selectedFileName = null
        )

        composeTestRule.setContent {
            MctTheme {
                FileChooserScreen(
                    state = state,
                    onSelectFile = {},
                    onChooseClick = {},
                    onCancel = {},
                    onCreateNewFile = {},
                    onDeleteSelectedFile = {},
                    onImportExportClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_files_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_new_file").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back").assertIsDisplayed()
    }

    @Test
    fun testFileChooserWithFilesAndSelection() {
        var selected = "std.keys"
        val sampleFiles = listOf(
            FileChooserItem(
                file = File("/dummy/std.keys"),
                name = "std.keys",
                sizeText = "128 B",
                dateText = "2026-10-11 12:00"
            ),
            FileChooserItem(
                file = File("/dummy/extended-std.keys"),
                name = "extended-std.keys",
                sizeText = "1 KB",
                dateText = "2026-10-11 12:05"
            )
        )

        val state = FileChooserUiState(
            title = "Choose Key File",
            chooserText = "Please select a key file",
            buttonText = "Select Key File",
            allowNewFile = true,
            files = sampleFiles,
            selectedFileName = selected
        )

        composeTestRule.setContent {
            MctTheme {
                FileChooserScreen(
                    state = state,
                    onSelectFile = { selected = it },
                    onChooseClick = {},
                    onCancel = {},
                    onCreateNewFile = {},
                    onDeleteSelectedFile = {},
                    onImportExportClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("list_files").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_item_std.keys").assertIsDisplayed()
        composeTestRule.onNodeWithTag("file_item_extended-std.keys").assertIsDisplayed()

        // Select second file
        composeTestRule.onNodeWithTag("file_item_extended-std.keys").performClick()
        assertEquals("extended-std.keys", selected)

        // Action button is enabled
        composeTestRule.onNodeWithTag("btn_choose").assertIsEnabled()
    }
}
