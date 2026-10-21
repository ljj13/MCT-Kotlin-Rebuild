package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.diff.DiffBlockItem
import de.syss.MifareClassicTool.ui.diff.DiffSectorItem
import de.syss.MifareClassicTool.ui.diff.DiffToolScreen
import de.syss.MifareClassicTool.ui.diff.DiffToolUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiffToolComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDiffToolEmptyState() {
        val emptyState = DiffToolUiState(
            dump1Name = null,
            dump2Name = null,
            isDump1FromEditor = false,
            hideIdenticalSectors = false,
            diffPercentageText = null,
            sectors = emptyList()
        )

        composeTestRule.setContent {
            MctTheme {
                DiffToolScreen(
                    state = emptyState,
                    onBackClick = {},
                    onChooseDump1Click = {},
                    onChooseDump2Click = {},
                    onHideIdenticalChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("card_empty_diff").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_choose_dump1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_choose_dump2").assertIsDisplayed()
    }

    @Test
    fun testDiffToolWithDumpsAndToggle() {
        var hideToggled = false
        val sampleSectors = listOf(
            DiffSectorItem(
                sectorNumber = 0,
                title = "Sector: 0",
                isIdentical = false,
                diffCount = 8,
                blocks = listOf(
                    DiffBlockItem(
                        blockIndex = 0,
                        blockHeader = "Block: 0",
                        dump1Line = "0BDC504FC80804000102030405060708",
                        dump2Line = "A1B2C3D49C0804000102030405060708",
                        diffIndices = setOf(0, 1, 2, 3, 4, 5, 6, 7),
                        isIdentical = false
                    ),
                    DiffBlockItem(
                        blockIndex = 1,
                        blockHeader = "Block: 1",
                        dump1Line = "00000000000000000000000000000000",
                        dump2Line = "00000000000000000000000000000000",
                        diffIndices = emptySet(),
                        isIdentical = true
                    )
                )
            ),
            DiffSectorItem(
                sectorNumber = 1,
                title = "Sector: 1",
                isIdentical = true,
                diffCount = 0,
                blocks = listOf(
                    DiffBlockItem(
                        blockIndex = 0,
                        blockHeader = "Block: 0",
                        dump1Line = "00000000000000000000000000000000",
                        dump2Line = "00000000000000000000000000000000",
                        diffIndices = emptySet(),
                        isIdentical = true
                    )
                )
            )
        )

        val state = DiffToolUiState(
            dump1Name = "Card1.dump",
            dump2Name = "Card2.dump",
            isDump1FromEditor = false,
            hideIdenticalSectors = false,
            diffPercentageText = "Difference between dumps: 3.12 %",
            sectors = sampleSectors
        )

        composeTestRule.setContent {
            MctTheme {
                DiffToolScreen(
                    state = state,
                    onBackClick = {},
                    onChooseDump1Click = {},
                    onChooseDump2Click = {},
                    onHideIdenticalChanged = { hideToggled = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("card_dump_selection").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_diff_percentage").assertTextContains("3.12 %", substring = true)
        composeTestRule.onNodeWithTag("list_diff_sectors").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sector_card_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sector_card_1").assertIsDisplayed()

        // Toggle checkbox
        composeTestRule.onNodeWithTag("cb_hide_identical").performClick()
        assertTrue(hideToggled)
    }
}
