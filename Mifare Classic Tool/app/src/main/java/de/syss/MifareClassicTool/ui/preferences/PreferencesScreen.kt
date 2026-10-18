package de.syss.MifareClassicTool.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctActionButton
import de.syss.MifareClassicTool.ui.components.MctSelectionDialog
import de.syss.MifareClassicTool.ui.components.MctSettingGroup
import de.syss.MifareClassicTool.ui.components.MctSettingItem
import de.syss.MifareClassicTool.ui.components.MctSwitchItem
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class PreferencesUiState(
    val autoReconnect: Boolean = false,
    val autoCopyUid: Boolean = false,
    val uidFormat: Int = 0, // 0: Hex, 1: DecBE, 2: DecLE
    val saveLastUsedKeyFiles: Boolean = true,
    val useCustomSectorCount: Boolean = false,
    val customSectorCount: String = "16",
    val useRetryAuthentication: Boolean = false,
    val retryAuthenticationCount: String = "1",
    val autostartIfCardDetected: Boolean = true,
    val customAppLanguage: Int = 0,
    val customAppTheme: Int = 2,
    val languageOptions: List<String> = emptyList(),
    val themeOptions: List<String> = emptyList()
)

@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    onStateChange: (PreferencesUiState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onShowAutoReconnectInfo: () -> Unit,
    onShowCustomSectorCountInfo: () -> Unit,
    onShowRetryAuthenticationInfo: () -> Unit
) {
    var themeDialogOpen by remember { mutableStateOf(false) }
    var languageDialogOpen by remember { mutableStateOf(false) }
    var uidFormatDialogOpen by remember { mutableStateOf(false) }

    val uidFormatOptions = listOf(
        stringResource(R.string.text_hex),
        stringResource(R.string.text_dec_be),
        stringResource(R.string.text_dec_le)
    )

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_preferences),
                onBackClick = onCancel
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: General & Convenience (日常使用与字典便利)
            MctSettingGroup(
                title = stringResource(R.string.pref_group_daily_and_keys),
                modifier = Modifier.testTag("card_convenience_settings")
            ) {
                MctSwitchItem(
                    title = stringResource(R.string.action_autostart_if_tag_detected),
                    checked = state.autostartIfCardDetected,
                    onCheckedChange = { onStateChange(state.copy(autostartIfCardDetected = it)) },
                    testTag = "cb_autostart"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                MctSwitchItem(
                    title = stringResource(R.string.action_save_last_key_files),
                    checked = state.saveLastUsedKeyFiles,
                    onCheckedChange = { onStateChange(state.copy(saveLastUsedKeyFiles = it)) },
                    testTag = "cb_save_last_keys"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                MctSwitchItem(
                    title = stringResource(R.string.action_auto_copy_uid),
                    checked = state.autoCopyUid,
                    onCheckedChange = { onStateChange(state.copy(autoCopyUid = it)) },
                    testTag = "cb_auto_copy_uid"
                )
            }

            // Group 2: RF Communication & Authentication (读写通信与认证设置)
            MctSettingGroup(
                title = stringResource(R.string.pref_group_rf_and_auth),
                modifier = Modifier.testTag("card_rf_settings")
            ) {
                // Auto Reconnect
                MctSettingItem(
                    title = stringResource(R.string.action_auto_reconnect),
                    onClick = { onStateChange(state.copy(autoReconnect = !state.autoReconnect)) },
                    testTag = "cb_auto_reconnect",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShowAutoReconnectInfo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "ⓘ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Switch(
                                checked = state.autoReconnect,
                                onCheckedChange = { onStateChange(state.copy(autoReconnect = it)) }
                            )
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Custom Sector Count
                MctSettingItem(
                    title = stringResource(R.string.action_use_custom_sector_count),
                    onClick = { onStateChange(state.copy(useCustomSectorCount = !state.useCustomSectorCount)) },
                    testTag = "cb_use_custom_sector_count",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShowCustomSectorCountInfo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "ⓘ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Switch(
                                checked = state.useCustomSectorCount,
                                onCheckedChange = { onStateChange(state.copy(useCustomSectorCount = it)) }
                            )
                        }
                    }
                )

                if (state.useCustomSectorCount) {
                    OutlinedTextField(
                        value = state.customSectorCount,
                        onValueChange = { onStateChange(state.copy(customSectorCount = it)) },
                        label = { Text(stringResource(R.string.pref_custom_sector_count_label)) },
                        placeholder = { Text(stringResource(R.string.hint_custom_sector_count)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("input_custom_sector_count"),
                        singleLine = true
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Retry Authentication
                MctSettingItem(
                    title = stringResource(R.string.action_retry_authentication),
                    onClick = { onStateChange(state.copy(useRetryAuthentication = !state.useRetryAuthentication)) },
                    testTag = "cb_use_retry_auth",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onShowRetryAuthenticationInfo,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "ⓘ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Switch(
                                checked = state.useRetryAuthentication,
                                onCheckedChange = { onStateChange(state.copy(useRetryAuthentication = it)) }
                            )
                        }
                    }
                )

                if (state.useRetryAuthentication) {
                    OutlinedTextField(
                        value = state.retryAuthenticationCount,
                        onValueChange = { onStateChange(state.copy(retryAuthenticationCount = it)) },
                        label = { Text(stringResource(R.string.pref_retry_auth_count_label)) },
                        placeholder = { Text(stringResource(R.string.hint_custom_retry_authentication_count)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("input_retry_auth_count"),
                        singleLine = true
                    )
                }
            }

            // Group 3: Display & Language (外观主题与语言)
            MctSettingGroup(
                title = stringResource(R.string.pref_group_display_and_ui),
                modifier = Modifier.testTag("card_ui_settings")
            ) {
                // Theme Selection Item
                val currentThemeText = state.themeOptions.getOrElse(state.customAppTheme) { "Follow System" }
                MctSettingItem(
                    title = stringResource(R.string.text_choose_theme),
                    valueText = currentThemeText,
                    onClick = { themeDialogOpen = true },
                    testTag = "dropdown_theme"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Language Selection Item
                val currentLangText = state.languageOptions.getOrElse(state.customAppLanguage) {
                    stringResource(R.string.text_language_follow_system)
                }
                MctSettingItem(
                    title = stringResource(R.string.text_choose_language),
                    valueText = currentLangText,
                    onClick = { languageDialogOpen = true },
                    testTag = "dropdown_language"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // UID Format Selection Item (clean vertical modal dialog, solves horizontal squeezing!)
                val currentUidFormatText = uidFormatOptions.getOrElse(state.uidFormat) { uidFormatOptions[0] }
                MctSettingItem(
                    title = stringResource(R.string.pref_uid_format_title),
                    valueText = currentUidFormatText,
                    onClick = { uidFormatDialogOpen = true },
                    testTag = "setting_uid_format"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Cancel & Save)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_cancel")
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("btn_save")
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Selection Dialogs
        if (themeDialogOpen) {
            MctSelectionDialog(
                title = stringResource(R.string.pref_theme_dialog_title),
                options = state.themeOptions,
                selectedIndex = state.customAppTheme,
                onSelect = { onStateChange(state.copy(customAppTheme = it)) },
                onDismissRequest = { themeDialogOpen = false }
            )
        }

        if (languageDialogOpen) {
            MctSelectionDialog(
                title = stringResource(R.string.pref_language_dialog_title),
                options = state.languageOptions,
                selectedIndex = state.customAppLanguage,
                onSelect = { onStateChange(state.copy(customAppLanguage = it)) },
                onDismissRequest = { languageDialogOpen = false }
            )
        }

        if (uidFormatDialogOpen) {
            MctSelectionDialog(
                title = stringResource(R.string.pref_uid_format_dialog_title),
                options = uidFormatOptions,
                selectedIndex = state.uidFormat,
                onSelect = { onStateChange(state.copy(uidFormat = it)) },
                onDismissRequest = { uidFormatDialogOpen = false }
            )
        }
    }
}
