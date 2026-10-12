package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.readtag.ReadTagPhase
import de.syss.MifareClassicTool.ui.readtag.ReadTagScreen
import de.syss.MifareClassicTool.ui.readtag.ReadTagUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadTagComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testReadFailureAndCancelDoNotStartAnotherRead() {
        var cancels = 0
        val state = ReadTagUiState(
            phase = ReadTagPhase.FAILED,
            message = "Error: Tag removed while reading",
            canCancel = true
        )

        composeTestRule.setContent {
            MctTheme {
                ReadTagScreen(
                    state = state,
                    onBackClick = {},
                    onCancelClick = { cancels++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("read_tag_message")
            .assertTextContains("Tag removed")
        composeTestRule.onNodeWithTag("btn_read_tag_cancel").assertIsDisplayed().performClick()
        assertEquals(1, cancels)
    }
}
