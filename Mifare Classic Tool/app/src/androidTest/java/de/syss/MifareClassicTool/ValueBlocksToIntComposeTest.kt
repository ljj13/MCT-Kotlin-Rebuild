package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.valueblock.DecodedValueBlockItem
import de.syss.MifareClassicTool.ui.valueblock.ValueBlocksToIntScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ValueBlocksToIntComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testValueBlocksToIntEmptyState() {
        composeTestRule.setContent {
            MctTheme {
                ValueBlocksToIntScreen(
                    items = emptyList(),
                    onBackClick = {},
                    onCopyInt = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_vb_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back").assertIsDisplayed()
    }

    @Test
    fun testValueBlocksToIntWithData() {
        var copiedVal = ""
        val sampleItems = listOf(
            DecodedValueBlockItem(
                posLabel = "Sector: 1, Block: 0",
                hexOrig = "01000000",
                intResolved = "1",
                rawHex = "01000000FEFFFFFF0100000000FF00FF"
            ),
            DecodedValueBlockItem(
                posLabel = "Sector: 1, Block: 1",
                hexOrig = "A7010000",
                intResolved = "423",
                rawHex = "A701000058FEFFFFA701000001FE01FE"
            )
        )

        composeTestRule.setContent {
            MctTheme {
                ValueBlocksToIntScreen(
                    items = sampleItems,
                    onBackClick = {},
                    onCopyInt = { copiedVal = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("list_value_blocks").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_vb_Sector: 1, Block: 0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_resolved_int_Sector: 1, Block: 0").assertTextContains("1")
        composeTestRule.onNodeWithTag("text_resolved_int_Sector: 1, Block: 1").assertTextContains("423")

        composeTestRule.onNodeWithTag("btn_copy_Sector: 1, Block: 1").performClick()
        assertEquals("423", copiedVal)
    }
}
