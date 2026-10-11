package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.importexport.DumpFormatChoice
import de.syss.MifareClassicTool.ui.importexport.ImportExportScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportExportComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testImportExportScreenStructure() {
        composeTestRule.setContent {
            MctTheme {
                ImportExportScreen(
                    onBackClick = {},
                    onImportDumpFormatSelected = {},
                    onExportDumpClick = {},
                    onImportKeysFormatSelected = {},
                    onExportKeysClick = {},
                    onBackupAllClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("card_dump_operations").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_key_operations").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_backup_operations").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_instructions").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_import_dump").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_export_dump").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_import_keys").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_export_keys").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_backup_all").assertIsDisplayed()
    }

    @Test
    fun testImportDumpFormatDialogSelection() {
        var selectedFormat: DumpFormatChoice? = null

        composeTestRule.setContent {
            MctTheme {
                ImportExportScreen(
                    onBackClick = {},
                    onImportDumpFormatSelected = { selectedFormat = it },
                    onExportDumpClick = {},
                    onImportKeysFormatSelected = {},
                    onExportKeysClick = {},
                    onBackupAllClick = {}
                )
            }
        }

        // Open dump format dialog
        composeTestRule.onNodeWithTag("btn_import_dump").performClick()

        // Format items visible
        composeTestRule.onNodeWithTag("format_dump_JSON").assertIsDisplayed()
        composeTestRule.onNodeWithTag("format_dump_MCT").assertIsDisplayed()

        // Select JSON
        composeTestRule.onNodeWithTag("format_dump_JSON").performClick()
        assertEquals(DumpFormatChoice.JSON, selectedFormat)
    }
}
