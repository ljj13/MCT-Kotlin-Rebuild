package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.cloneuid.CloneUidScreen
import de.syss.MifareClassicTool.ui.cloneuid.CloneUidUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloneUidToolComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCloneUidToolStructure() {
        val state = CloneUidUiState(
            uidInput = "",
            block0RestInput = "080400475955D141103607",
            writeKeyInput = "FFFFFFFFFFFF",
            useKeyB = false,
            autoCalcSakAtqa = false,
            showOptions = false,
            block0Complete = null,
            statusLogs = emptyList()
        )

        composeTestRule.setContent {
            MctTheme {
                CloneUidScreen(
                    state = state,
                    onStateChange = {},
                    onBackClick = {},
                    onPasteUid = {},
                    onRandomUid = {},
                    onCalculateBlock0 = {},
                    onShowInfo = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("card_clone_notice").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_target_uid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_block0_log").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_uid").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_calc_block0").assertIsEnabled()
    }

    @Test
    fun testCloneUidToolInputAndCalculate() {
        var calculated = false
        var currentState = CloneUidUiState(
            uidInput = "0BDC504F",
            block0RestInput = "080400475955D141103607",
            writeKeyInput = "FFFFFFFFFFFF",
            useKeyB = false,
            autoCalcSakAtqa = false,
            showOptions = true,
            block0Complete = "0BDC504FC8080400475955D141103607",
            statusLogs = listOf("Block 0 generated: 0BDC504FC8080400475955D141103607")
        )

        composeTestRule.setContent {
            MctTheme {
                CloneUidScreen(
                    state = currentState,
                    onStateChange = { currentState = it },
                    onBackClick = {},
                    onPasteUid = {},
                    onRandomUid = {},
                    onCalculateBlock0 = { calculated = true },
                    onShowInfo = {}
                )
            }
        }

        // Advanced options inputs visible when showOptions = true
        composeTestRule.onNodeWithTag("input_block0_rest").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_info_block0_rest").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_write_key").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_info_write_key").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rb_key_a").assertIsDisplayed()
        composeTestRule.onNodeWithTag("rb_key_b").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cb_sak_atqa").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_info_sak_atqa").assertIsDisplayed()

        // Block 0 complete preview is visible
        composeTestRule.onNodeWithTag("text_block0_complete").assertIsDisplayed()

        // Calculate click
        composeTestRule.onNodeWithTag("btn_calc_block0").performClick()
        assertTrue(calculated)
    }
}
