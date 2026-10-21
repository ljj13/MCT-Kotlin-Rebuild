package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.accesscondition.AccessConditionDecoderScreen
import de.syss.MifareClassicTool.ui.accesscondition.BlockAcPermission
import de.syss.MifareClassicTool.ui.accesscondition.DecodedSectorAc
import de.syss.MifareClassicTool.ui.accesscondition.TrailerAcPermission
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessConditionDecoderComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAccessConditionDecoderEmptyState() {
        composeTestRule.setContent {
            MctTheme {
                AccessConditionDecoderScreen(
                    sectors = emptyList(),
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_ac_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back").assertIsDisplayed()
    }

    @Test
    fun testAccessConditionDecoderWithSectors() {
        val sampleSectors = listOf(
            DecodedSectorAc(
                sectorHeader = "Sector: 0",
                isValid = true,
                blocks = listOf(
                    BlockAcPermission("Block: 0", "Key A|B", "Key A|B", "Key A|B", "Key A|B"),
                    BlockAcPermission("Block: 1", "Key A|B", "Key A|B", "Key A|B", "Key A|B"),
                    BlockAcPermission("Block: 2", "Key A|B", "Key A|B", "Key A|B", "Key A|B")
                ),
                trailer = listOf(
                    TrailerAcPermission("Key A:", "Never", "Key A"),
                    TrailerAcPermission("AC Bits:", "Key A", "Never"),
                    TrailerAcPermission("Key B:", "Key A", "Key A")
                )
            )
        )

        composeTestRule.setContent {
            MctTheme {
                AccessConditionDecoderScreen(
                    sectors = sampleSectors,
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("list_decoded_sectors").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sector_card_Sector: 0").assertIsDisplayed()
    }
}
