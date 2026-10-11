package de.syss.MifareClassicTool.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctSectionCard
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

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
            // 1. NFC Communication & Security
            MctSectionCard(
                title = "读写通信与认证设置",
                modifier = Modifier.testTag("card_rf_settings")
            ) {
                // Auto Reconnect
                PreferenceCheckboxWithInfo(
                    text = stringResource(R.string.action_auto_reconnect),
                    checked = state.autoReconnect,
                    onCheckedChange = { onStateChange(state.copy(autoReconnect = it)) },
                    onInfoClick = onShowAutoReconnectInfo,
                    testTag = "cb_auto_reconnect"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Custom Sector Count
                PreferenceCheckboxWithInfo(
                    text = stringResource(R.string.action_use_custom_sector_count),
                    checked = state.useCustomSectorCount,
                    onCheckedChange = { onStateChange(state.copy(useCustomSectorCount = it)) },
                    onInfoClick = onShowCustomSectorCountInfo,
                    testTag = "cb_use_custom_sector_count"
                )
                if (state.useCustomSectorCount) {
                    OutlinedTextField(
                        value = state.customSectorCount,
                        onValueChange = { onStateChange(state.copy(customSectorCount = it)) },
                        label = { Text("扇区数量 (1-40)") },
                        placeholder = { Text(stringResource(R.string.hint_custom_sector_count)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("input_custom_sector_count"),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Retry Authentication
                PreferenceCheckboxWithInfo(
                    text = stringResource(R.string.action_retry_authentication),
                    checked = state.useRetryAuthentication,
                    onCheckedChange = { onStateChange(state.copy(useRetryAuthentication = it)) },
                    onInfoClick = onShowRetryAuthenticationInfo,
                    testTag = "cb_use_retry_auth"
                )
                if (state.useRetryAuthentication) {
                    OutlinedTextField(
                        value = state.retryAuthenticationCount,
                        onValueChange = { onStateChange(state.copy(retryAuthenticationCount = it)) },
                        label = { Text("重试次数 (1-1000)") },
                        placeholder = { Text(stringResource(R.string.hint_custom_retry_authentication_count)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("input_retry_auth_count"),
                        singleLine = true
                    )
                }
            }

            // 2. Convenience & Keys
            MctSectionCard(
                title = "日常使用与字典便利",
                modifier = Modifier.testTag("card_convenience_settings")
            ) {
                // Autostart if card detected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStateChange(state.copy(autostartIfCardDetected = !state.autostartIfCardDetected)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.autostartIfCardDetected,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("cb_autostart")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_autostart_if_tag_detected),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Save Last Used Key Files
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStateChange(state.copy(saveLastUsedKeyFiles = !state.saveLastUsedKeyFiles)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.saveLastUsedKeyFiles,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("cb_save_last_keys")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_save_last_key_files),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Auto copy UID
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStateChange(state.copy(autoCopyUid = !state.autoCopyUid)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.autoCopyUid,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("cb_auto_copy_uid")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_auto_copy_uid),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // UID Format radio row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 4.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val formats = listOf(
                        Pair(0, stringResource(R.string.text_hex)),
                        Pair(1, stringResource(R.string.text_dec_be)),
                        Pair(2, stringResource(R.string.text_dec_le))
                    )
                    formats.forEach { (index, title) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .selectable(
                                    selected = (state.uidFormat == index),
                                    enabled = state.autoCopyUid,
                                    role = Role.RadioButton,
                                    onClick = { onStateChange(state.copy(uidFormat = index)) }
                                )
                                .testTag("rb_uid_format_$index")
                        ) {
                            RadioButton(
                                selected = (state.uidFormat == index),
                                enabled = state.autoCopyUid,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.autoCopyUid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }

            // 3. Theme & Language
            MctSectionCard(
                title = "外观主题与语言",
                modifier = Modifier.testTag("card_ui_settings")
            ) {
                // Theme
                Text(
                    text = stringResource(R.string.text_choose_theme),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val currentThemeText = state.themeOptions.getOrElse(state.customAppTheme) { "Follow System" }
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.OutlinedCard(
                        onClick = { themeExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dropdown_theme")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentThemeText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(text = "▼", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (themeExpanded) {
                        androidx.compose.material3.DropdownMenu(
                            expanded = true,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            state.themeOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        onStateChange(state.copy(customAppTheme = index))
                                        themeExpanded = false
                                    },
                                    modifier = Modifier.testTag("theme_option_$index")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language
                Text(
                    text = stringResource(R.string.text_choose_language),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val currentLangText = state.languageOptions.getOrElse(state.customAppLanguage) { "English" }
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.OutlinedCard(
                        onClick = { languageExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dropdown_language")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLangText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(text = "▼", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (languageExpanded) {
                        androidx.compose.material3.DropdownMenu(
                            expanded = true,
                            onDismissRequest = { languageExpanded = false }
                        ) {
                            state.languageOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        onStateChange(state.copy(customAppLanguage = index))
                                        languageExpanded = false
                                    },
                                    modifier = Modifier.testTag("lang_option_$index")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Save and Cancel Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_cancel")
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_save")
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PreferenceCheckboxWithInfo(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.testTag(testTag)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.testTag("${testTag}_info")
        ) {
            Text(
                text = "ⓘ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
