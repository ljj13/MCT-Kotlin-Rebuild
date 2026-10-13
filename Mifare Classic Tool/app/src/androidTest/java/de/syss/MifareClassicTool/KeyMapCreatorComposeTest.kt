package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.keymap.KeyFileChoice
import de.syss.MifareClassicTool.ui.keymap.KeyMapCreatorScreen
import de.syss.MifareClassicTool.ui.keymap.KeyMapCreatorUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyMapCreatorComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testKeySelectionAndRangeControlsDoNotStartScan() {
        var selected: Pair<String, Boolean>? = null
        var createClicks = 0
        val state = KeyMapCreatorUiState(
            title = "Map Keys",
            sectorRange = "0 - 15",
            keyFiles = listOf(KeyFileChoice("std.keys", false)),
            createButtonText = "Start Mapping and Read Tag",
            statusMessage = "idle"
        )

        composeTestRule.setContent {
            MctTheme {
                KeyMapCreatorScreen(
                    state = state,
                    onBackClick = {},
                    onToggleKeyFile = { name, checked -> selected = name to checked },
                    onSelectAll = {},
                    onSelectNone = {},
                    onChangeRangeClick = {},
                    onRangeFromChange = {},
                    onRangeToChange = {},
                    onSaveRangeDefaultChange = {},
                    onConfirmRange = {},
                    onReadAllSectors = {},
                    onDismissRange = {},
                    onCreateClick = { createClicks++ },
                    onCancelClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("text_sector_range").assertTextContains("0 - 15")
        composeTestRule.onNodeWithTag("key_file_std.keys").assertIsDisplayed()
        composeTestRule.onNode(
            isToggleable() and hasAnyAncestor(hasTestTag("key_file_std.keys")),
            useUnmergedTree = true
        ).performClick()
        composeTestRule.onNodeWithTag("btn_create_key_map").assertIsDisplayed()
        assertEquals("std.keys" to true, selected)
        assertEquals(0, createClicks)
    }
}
