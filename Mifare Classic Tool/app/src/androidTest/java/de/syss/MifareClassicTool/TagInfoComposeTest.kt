package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.taginfo.TagInfoData
import de.syss.MifareClassicTool.ui.taginfo.TagInfoScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagInfoComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTagInfoEmptyState() {
        composeTestRule.setContent {
            MctTheme {
                TagInfoScreen(
                    tagInfo = null,
                    mfcSupport = 0,
                    onBackClick = {},
                    onReadMoreClick = {},
                    onCopyUid = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("no_tag_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back").assertIsDisplayed()
    }

    @Test
    fun testTagInfoWithMfcTag() {
        var copiedUid = ""
        val mockData = TagInfoData(
            uid = "0BDC504F (4 byte)",
            uidRaw = "0BDC504F",
            rfTech = "ISO/IEC 14443 Type A",
            atqa = "0400",
            sak = "08",
            ats = "-",
            tagType = "NXP MIFARE Classic 1k",
            isTagTypeGuessed = true,
            memSize = "1024",
            blockSize = "16",
            sectorCount = "16",
            blockCount = "64"
        )

        composeTestRule.setContent {
            MctTheme {
                TagInfoScreen(
                    tagInfo = mockData,
                    mfcSupport = 0,
                    onBackClick = {},
                    onReadMoreClick = {},
                    onCopyUid = { uid -> copiedUid = uid }
                )
            }
        }

        composeTestRule.onNodeWithTag("card_generic_info").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_uid_val").assertTextContains("0BDC504F", substring = true)
        composeTestRule.onNodeWithTag("card_mfc_info").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_copy_uid").performClick()
        assertEquals("0BDC504F", copiedUid)
    }
}
