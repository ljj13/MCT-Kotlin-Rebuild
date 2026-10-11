package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.help.HelpAndInfoScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpAndInfoComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHelpAndInfoScreenDisplayed() {
        var backClicked = false
        composeTestRule.setContent {
            MctTheme {
                HelpAndInfoScreen(
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("top_bar_back").assertIsDisplayed()
        composeTestRule.onNodeWithTag("help_webview").assertIsDisplayed()
    }
}
