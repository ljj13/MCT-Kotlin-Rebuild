package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.Activities.BccTool
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BccToolComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<BccTool>()

    @Test
    fun testBccCalculationSuccess() {
        // Real card UID 0BDC504F -> BCC should be C8
        composeTestRule.onNodeWithTag("uid_input")
            .performTextInput("0BDC504F")

        composeTestRule.onNodeWithTag("calc_button")
            .performClick()

        composeTestRule.onNodeWithTag("bcc_result")
            .assertTextContains("C8")

        composeTestRule.onNodeWithTag("copy_button")
            .assertIsEnabled()
    }

    @Test
    fun testBccCalculationInvalidLength() {
        composeTestRule.onNodeWithTag("uid_input")
            .performTextInput("1234")

        composeTestRule.onNodeWithTag("calc_button")
            .performClick()

        composeTestRule.onNodeWithTag("copy_button")
            .assertIsNotEnabled()
    }
}
