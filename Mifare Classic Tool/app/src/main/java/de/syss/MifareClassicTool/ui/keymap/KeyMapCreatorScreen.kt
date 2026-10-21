package de.syss.MifareClassicTool.ui.keymap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctActionButton
import de.syss.MifareClassicTool.ui.components.MctMonospaceField
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class KeyFileChoice(
    val name: String,
    val checked: Boolean
)

data class KeyMapCreatorUiState(
    val title: String = "",
    val sectorRange: String = "",
    val sectorChooserEnabled: Boolean = true,
    val keyFiles: List<KeyFileChoice> = emptyList(),
    val progress: Int = 0,
    val progressMax: Int = 1,
    val scanning: Boolean = false,
    val createEnabled: Boolean = true,
    val cancelEnabled: Boolean = true,
    val createButtonText: String = "",
    val statusMessage: String = "",
    val showRangeDialog: Boolean = false,
    val rangeFrom: String = "",
    val rangeTo: String = "",
    val saveRangeAsDefault: Boolean = false
)

@Composable
fun KeyMapCreatorScreen(
    state: KeyMapCreatorUiState,
    onBackClick: () -> Unit,
    onToggleKeyFile: (String, Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onChangeRangeClick: () -> Unit,
    onRangeFromChange: (String) -> Unit,
    onRangeToChange: (String) -> Unit,
    onSaveRangeDefaultChange: (Boolean) -> Unit,
    onConfirmRange: () -> Unit,
    onReadAllSectors: () -> Unit,
    onDismissRange: () -> Unit,
    onCreateClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MctTopAppBar(
                title = state.title.ifBlank { stringResource(R.string.action_create_key_map) },
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("key_map_creator_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MctSectionCard(title = stringResource(R.string.text_sector_range)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.sectorRange,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("text_sector_range")
                    )
                    OutlinedButton(
                        onClick = onChangeRangeClick,
                        enabled = state.sectorChooserEnabled && !state.scanning,
                        modifier = Modifier.testTag("btn_change_sector_range")
                    ) {
                        Text(stringResource(R.string.action_change))
                    }
                }
            }

            MctSectionCard(
                title = stringResource(R.string.text_choose_key_files),
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSelectAll,
                        enabled = !state.scanning,
                        modifier = Modifier.testTag("btn_select_all_keys")
                    ) {
                        Text(stringResource(R.string.action_select_all))
                    }
                    OutlinedButton(
                        onClick = onSelectNone,
                        enabled = !state.scanning,
                        modifier = Modifier.testTag("btn_select_none_keys")
                    ) {
                        Text(stringResource(R.string.action_select_none))
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_key_files")
                ) {
                    items(state.keyFiles, key = { it.name }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("key_file_${file.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = file.checked,
                                onCheckedChange = { checked -> onToggleKeyFile(file.name, checked) },
                                enabled = !state.scanning
                            )
                            Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            MctSectionCard(title = stringResource(R.string.text_key_map_progress)) {
                LinearProgressIndicator(
                    progress = { if (state.progressMax <= 0) 0f else state.progress.toFloat() / state.progressMax },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_key_map")
                )
                if (state.statusMessage.isNotBlank()) {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("text_key_map_status")
                    )
                }
            }

            MctActionButton(
                text = state.createButtonText.ifBlank { stringResource(R.string.action_create_key_map) },
                onClick = onCreateClick,
                enabled = state.createEnabled && !state.scanning,
                testTag = "btn_create_key_map"
            )
            MctActionButton(
                text = stringResource(R.string.action_cancel),
                onClick = onCancelClick,
                enabled = state.cancelEnabled,
                testTag = "btn_cancel_key_map"
            )
        }
    }

    if (state.showRangeDialog) {
        AlertDialog(
            onDismissRequest = onDismissRange,
            title = { Text(stringResource(R.string.dialog_mapping_range_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dialog_mapping_range))
                    MctMonospaceField(
                        value = state.rangeFrom,
                        onValueChange = onRangeFromChange,
                        placeholder = stringResource(R.string.text_from),
                        testTag = "field_range_from",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    MctMonospaceField(
                        value = state.rangeTo,
                        onValueChange = onRangeToChange,
                        placeholder = stringResource(R.string.text_to),
                        testTag = "field_range_to",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.saveRangeAsDefault,
                            onCheckedChange = onSaveRangeDefaultChange,
                            modifier = Modifier.testTag("check_save_range_default")
                        )
                        Text(stringResource(R.string.action_save_as_default))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmRange, modifier = Modifier.testTag("btn_confirm_range")) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onReadAllSectors, modifier = Modifier.testTag("btn_read_all_sectors")) {
                        Text(stringResource(R.string.action_read_all_sectors))
                    }
                    TextButton(onClick = onDismissRange) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }
}
