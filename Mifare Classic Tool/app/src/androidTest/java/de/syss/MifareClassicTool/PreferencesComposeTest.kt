package de.syss.MifareClassicTool

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.preferences.PreferencesScreen
import de.syss.MifareClassicTool.ui.preferences.PreferencesUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPreferencesScreenStructure() {
        val state = mutableStateOf(
            PreferencesUiState(
                autoReconnect = false,
                autoCopyUid = false,
                saveLastUsedKeyFiles = true,
                useCustomSectorCount = false,
                languageOptions = listOf("English (en)", "简体中文 (zh)"),
                themeOptions = listOf("Dark", "Light", "Follow System")
            )
        )

        composeTestRule.setContent {
            MctTheme {
                PreferencesScreen(
                    state = state.value,
                    onStateChange = { state.value = it },
                    onSave = {},
                    onCancel = {},
                    onShowAutoReconnectInfo = {},
                    onShowCustomSectorCountInfo = {},
                    onShowRetryAuthenticationInfo = {}
                )
            }
        }

        // Section cards exist in composition
        composeTestRule.onNodeWithTag("card_rf_settings").assertExists()
        composeTestRule.onNodeWithTag("card_convenience_settings").assertExists()
        composeTestRule.onNodeWithTag("card_ui_settings").assertExists()

        // Checkboxes & Buttons
        composeTestRule.onNodeWithTag("cb_auto_reconnect").assertExists()
        composeTestRule.onNodeWithTag("cb_use_custom_sector_count").assertExists()
        composeTestRule.onNodeWithTag("btn_save").assertExists()
        composeTestRule.onNodeWithTag("btn_cancel").assertExists()
    }

    @Test
    fun testToggleCustomSectorCount() {
        val state = mutableStateOf(
            PreferencesUiState(
                useCustomSectorCount = false,
                languageOptions = listOf("English (en)"),
                themeOptions = listOf("Dark")
            )
        )

        composeTestRule.setContent {
            MctTheme {
                PreferencesScreen(
                    state = state.value,
                    onStateChange = { state.value = it },
                    onSave = {},
                    onCancel = {},
                    onShowAutoReconnectInfo = {},
                    onShowCustomSectorCountInfo = {},
                    onShowRetryAuthenticationInfo = {}
                )
            }
        }

        // Initially custom sector count input not shown
        composeTestRule.onNodeWithTag("input_custom_sector_count").assertDoesNotExist()

        // Toggle custom sector count
        composeTestRule.onNodeWithTag("cb_use_custom_sector_count").performClick()
        assertTrue(state.value.useCustomSectorCount)

        // Now custom sector count input is displayed
        composeTestRule.onNodeWithTag("input_custom_sector_count").assertIsDisplayed()
    }
}
